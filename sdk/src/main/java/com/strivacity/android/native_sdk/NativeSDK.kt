package com.strivacity.android.native_sdk

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.strivacity.android.native_sdk.render.FallbackHandler
import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.service.HttpService
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.service.OIDCHandlerService
import com.strivacity.android.native_sdk.service.OidcParams
import com.strivacity.android.native_sdk.service.TokenExchangeParams
import com.strivacity.android.native_sdk.service.TokenRefreshParams
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.path
import java.lang.ref.WeakReference
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NativeSDK
internal constructor(
    private val issuer: String,
    private val clientId: String,
    private val redirectURI: String,
    private val postLogoutURI: String,
    val session: Session,
    private val  mode: SdkMode = SdkMode.Android,
    private val dispatchers: SDKDispatchers = DefaultSDKDispatchers,
    private val clock: Clock = Clock.systemUTC(),
    private val logging: Logging = DefaultLogging(),
    private val httpService: HttpService = HttpService(logging = logging),
    private val oidcHandlerService: OIDCHandlerService =
        OIDCHandlerService(httpService = httpService, logging = logging),
) {

  constructor(
      issuer: String,
      clientId: String,
      redirectURI: String,
      postLogoutURI: String,
      storage: Storage,
      mode: SdkMode = SdkMode.Android,
      logging: Logging = DefaultLogging(),
  ) : this(
      issuer = issuer,
      clientId = clientId,
      redirectURI = redirectURI,
      postLogoutURI = postLogoutURI,
      session = Session(storage, logging),
      mode = mode,
      logging = logging,
  )

  var loginController: LoginController? = null

  /** JSON Key name for error mnemonic in response object */
  private val ERROR_KEY: String = "error"

  /** JSON Key name for error description in response object */
  private val ERROR_DESCRIPTION_KEY: String = "error_description"

  internal val tokenRefreshMutex = Mutex()

  suspend fun initializeSession() =
      withContext(dispatchers.IO) {
        logging.info("NativeSDK: Initializing session")
        try {
          session.load()
          val refreshed = refreshTokensIfNeeded()
          if (refreshed) {
            logging.debug("NativeSDK: Session initialized and tokens refreshed")
          } else {
            logging.debug("NativeSDK: Session initialized")
          }
        } catch (e: Exception) {
          logging.error("NativeSDK: Failed to initialize session", e)
          throw e
        }
      }

  suspend fun login(
      fallbackHandler: FallbackHandler,
      onSuccess: () -> Unit,
      onError: (Error) -> Unit,
      loginParameters: LoginParameters? = null,
  ) =
      withContext(dispatchers.IO) {
        logging.info("NativeSDK: Starting login flow")
        val oidcParams = OidcParams(onSuccess, onError)

        val url =
            URLBuilder(issuer)
                .apply {
                  path("/oauth2/auth")
                  parameters.append("response_type", "code")
                  parameters.append("client_id", clientId)
                  parameters.append("redirect_uri", redirectURI)
                  parameters.append("state", oidcParams.state)
                  parameters.append("nonce", oidcParams.nonce)
                  parameters.append("code_challenge", oidcParams.codeChallenge)
                  parameters.append("code_challenge_method", "S256")
                  parameters.append("sdk", mode.value)

                  val scopes = loginParameters?.scopes ?: listOf("openid", "profile")
                  parameters.append("scope", scopes.joinToString(separator = " "))
                  logging.debug("NativeSDK: Login scopes: ${parameters["scope"]}")

                  loginParameters?.let {
                    it.loginHint?.let { hint ->
                      logging.debug("NativeSDK: Login hint provided")
                      parameters.append("login_hint", hint)
                    }
                    it.acrValue?.let { acr ->
                      logging.debug("NativeSDK: ACR value: ${loginParameters.acrValue}")
                      parameters.append("acr_values", acr)
                    }
                    it.prompt?.let { prompt ->
                      logging.debug("NativeSDK: Prompt: ${loginParameters.prompt}")
                      parameters.append("prompt", prompt)
                    }
                    it.audiences
                        ?.filter { aud -> aud.isNotBlank() }
                        ?.takeIf { audiences -> audiences.isNotEmpty() }
                        ?.let { audiences ->
                          logging.debug("NativeSDK: Audiences: $audiences")
                          parameters.append("audience", audiences.joinToString(" "))
                        }
                  }
                }
                .build()

        try {
          val parameters = oidcHandlerService.handleCall(url)

          val sessionId = parameters["session_id"]
          if (sessionId == null) {
            continueFlow(oidcParams, parameters)
            return@withContext
          }

          val loginHandlerService = LoginHandlerService(httpService, issuer, sessionId)
          val loginController =
              LoginController(
                  this@NativeSDK,
                  loginHandlerService,
                  oidcParams,
                  fallbackHandler,
                  logging,
              )

          loginController.initialize()
          this@NativeSDK.loginController = loginController

          session.setLoginInProgress(true)
          logging.info("NativeSDK: Login flow started")
        } catch (e: Exception) {
          logging.error("NativeSDK: Login flow failed ${e.message}", e)
          onError(UnknownError(e))
        }
      }

  @Deprecated("Use login with fallbackHandler instead. This call will be removed in future version")
  suspend fun login(
      context: WeakReference<Context>,
      onSuccess: () -> Unit,
      onError: (Error) -> Unit,
      loginParameters: LoginParameters? = null,
  ) {
    val customTabsHandler: FallbackHandler = { uri ->
      run {
        val ctx = context.get() ?: throw IllegalStateException("Context is no longer available")

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(ctx, uri.toUri())
      }
    }
    return login(
        fallbackHandler = customTabsHandler,
        onSuccess = onSuccess,
        onError = onError,
        loginParameters = loginParameters,
    )
  }

  suspend fun entry(
      uri: Uri?,
      fallbackHandler: FallbackHandler,
      onSuccess: () -> Unit,
      onError: (Error) -> Unit
  ) {
    withContext(dispatchers.IO) {
      cleanup()

      logging.info("NativeSDK: Starting workflow")
      val oidcParams = OidcParams(onSuccess, onError)

      if (uri == null) {
        onError(UnknownError(RuntimeException("Entry URI is null")))
        return@withContext
      }

      val challenge = uri.getQueryParameter("challenge")
      if (challenge == null || challenge.trim().isEmpty()) {
        onError(UnknownError(RuntimeException("Entry challenge parameter is missing")))
        return@withContext
      }

      val url =
          URLBuilder(issuer)
              .apply {
                path("/provider/flow/entry")
                parameters.append("challenge", challenge)
                parameters.append("client_id", clientId)
                parameters.append("redirect_uri", redirectURI)
              }
              .build()

      val response = httpService.get(url, acceptHeader = ContentType.Text.Html)
      if (response.status == HttpStatusCode.BadRequest) {
        val body = response.body<Map<String, String>>()

        if (!body.containsKey(ERROR_KEY)) {
          onError(
              UnknownError(
                  RuntimeException(String.format("Workflow error: %s is null", ERROR_KEY))))
          return@withContext
        }

        val error = body[ERROR_KEY].toString()
        val errorDescription = body[ERROR_DESCRIPTION_KEY]
        onError(WorkflowError(error, errorDescription))
        return@withContext
      }

      if (response.status == HttpStatusCode.InternalServerError) {
        logging.debug("Ensure that authentication client has entry URL configured.")
        onError(
            UnknownError(RuntimeException("Server failed to answer - 500 status code received")))
        return@withContext
      }

      val locationHeader = response.headers["location"]
      if (locationHeader == null) {
        onError(
            UnknownError(RuntimeException("Expected to find Location header but it was not found")))
        return@withContext
      }

      val parameters: Parameters = URLBuilder(locationHeader).build().parameters
      val sessionId = parameters["session_id"]
      if (sessionId == null) {
        onError(
            UnknownError(RuntimeException("Failed to start session: session_id missing or blank")))
        return@withContext
      }

      val loginHandlerService = LoginHandlerService(httpService, issuer, sessionId)
      val loginController =
          LoginController(
              this@NativeSDK,
              loginHandlerService,
              oidcParams,
              fallbackHandler,
              logging,
          )

      loginController.initialize()
      this@NativeSDK.loginController = loginController

      session.setLoginInProgress(true)
      logging.info("NativeSDK: Login flow started")

      onSuccess()
    }
  }

  suspend fun isAuthenticated(): Boolean =
      withContext(dispatchers.IO) {
        val refreshed = refreshTokensIfNeeded()
        val authenticated = session.profile.value != null
        if (refreshed) {
          logging.debug(
              "NativeSDK: Authentication check - tokens refreshed, authenticated: $authenticated"
          )
        } else {
          logging.debug("NativeSDK: Authentication check - authenticated: $authenticated")
        }
        return@withContext authenticated
      }

  suspend fun getAccessToken(): String? =
      withContext(dispatchers.IO) {
        val refreshed = refreshTokensIfNeeded()
        val profile = session.profile.value
        if (profile == null) {
          logging.debug("NativeSDK: Access token requested but no profile available")
          return@withContext null
        }
        if (refreshed) {
          logging.debug("NativeSDK: Access token retrieved after token refresh")
        } else {
          logging.debug("NativeSDK: Access token retrieved")
        }
        return@withContext profile.tokenResponse.accessToken
      }

  fun isRedirectExpected(): Boolean {
    return loginController?.isRedirectExpected ?: false
  }

  suspend fun continueFlow(uri: String?) =
      withContext(dispatchers.IO) {
        val oidcParams = loginController?.oidcParams
        if (oidcParams == null) {
          logging.warn("NativeSDK: continueFlow called but no login controller available")
          return@withContext
        }

        if (uri == null) {
          logging.debug("NativeSDK: Flow canceled")
          cancelFlow(HostedFlowCanceledError())
          return@withContext
        }

        try {
          val url = URLBuilder(uri).build()
          logging.debug("NativeSDK: Continuing flow with ${url.encodedPath}")
          val parameters = oidcHandlerService.handleCall(url)
          continueFlow(oidcParams, parameters)
        } catch (e: Exception) {
          logging.debug("NativeSDK: Failed to continue flow", e)
          cleanup()
          oidcParams.onError(UnknownError(e))
        }
      }

  fun cancelFlow(error: Error? = null) {
    val loginController = loginController
    if (loginController == null) {
      logging.warn("NativeSDK: cancelFlow called but no login controller available")
      return
    }

    cleanup()
    if (error != null) {
      logging.debug("NativeSDK: Canceling login flow with error", error)
      loginController.oidcParams.onError(error)
    } else {
      logging.warn("NativeSDK: Canceling login flow")
    }
  }

  suspend fun logout(): Unit =
      withContext(dispatchers.IO) {
        logging.debug("NativeSDK: Starting logout")
        val idToken = session.profile.value?.tokenResponse?.idToken

        session.clear()

        logging.debug("NativeSDK: waiting 1s before submitting logout request")

        if (idToken == null) {
          logging.info("NativeSDK: User logged out (no id_token_hint available)")
          return@withContext
        }

        val url =
            URLBuilder(issuer)
                .apply {
                  path("/oauth2/sessions/logout")
                  parameters.append("id_token_hint", idToken)
                  parameters.append("post_logout_redirect_uri", postLogoutURI)
                }
                .build()

        try {
          logging.debug("NativeSDK: submitting logout request")
          oidcHandlerService.handleCall(url)
          logging.info("NativeSDK: User logged out")
        } catch (e: Error) {
          logging.warn("NativeSDK: Failed to call logout endpoint $e", e)
        }
      }

  suspend fun revoke() =
      withContext(dispatchers.IO) {
        val refreshToken = session.profile.value?.tokenResponse?.refreshToken
        val accessToken = session.profile.value?.tokenResponse?.accessToken

        if (refreshToken == null && accessToken == null) {
          // guard statement to return early if there is nothing to revoke
          session.clear()
          return@withContext
        }

        try {
          val token = refreshToken ?: accessToken!!
          val typeHint = if (refreshToken != null) "refresh_token" else "access_token"
          oidcHandlerService.revokeToken(
              issuer,
              token = token,
              typeHint = typeHint,
              clientId = clientId,
          )
        } catch (e: Error) {
          logging.debug("Failed to call revoke endpoint", e)
        } finally {
          session.clear()
        }
      }

  private suspend fun continueFlow(oidcParams: OidcParams, parameters: Parameters) {
    val sessionId = parameters["session_id"]
    if (sessionId != null) {
      try {
        loginController?.initialize()
      } catch (e: Exception) {
        logging.debug("NativeSDK: Failed to re-initialize LoginController", e)
        cleanup()
        oidcParams.onError(UnknownError(e))
      }

      return
    }

    val error = parameters["error"]
    val errorDescription = parameters["error_description"]
    if (error != null && errorDescription != null) {
      logging.debug("NativeSDK: OIDC error received - error: $error $errorDescription")
      session.clear()
      cleanup()
      oidcParams.onError(OidcError(error, errorDescription))
      return
    }

    val state = parameters["state"]
    if (state != oidcParams.state) {
      logging.error("NativeSDK: State validation failed")
      cleanup()
      oidcParams.onError(InvalidCallbackError("State param did not matched expected value"))
      return
    }

    val code = parameters["code"]
    if (code == null) {
      logging.error("NativeSDK: Authorization code missing from callback")
      throw IllegalStateException("Code missing from response")
    }

    logging.debug("NativeSDK: Authorization code received, exchanging for tokens")
    try {
      val tokenResponse =
          oidcHandlerService.tokenExchange(
              URLBuilder(issuer).apply { path("/oauth2/token") }.toString(),
              TokenExchangeParams(code, oidcParams.codeVerifier, redirectURI, clientId),
          )

      logging.debug("NativeSDK: Token exchange successful, validating claims")
      val claims = extractClaims(tokenResponse)

      val responseNonce = claims["nonce"] as? String
      if (responseNonce == null || oidcParams.nonce != responseNonce) {
        logging.error("NativeSDK: Nonce validation failed")
        cleanup()
        oidcParams.onError(InvalidCallbackError("Nonce param did not matched expected value"))
        return
      }

      val responseIssuer = claims["iss"] as? String
      val normalizedIssuer = if (issuer.endsWith("/")) issuer else "$issuer/"
      if (responseIssuer == null || normalizedIssuer != responseIssuer) {
        logging.error("NativeSDK: Issuer validation failed")
        cleanup()
        oidcParams.onError(InvalidCallbackError("Issuer param did not matched expected value"))
        return
      }

      val responseAudience = claims["aud"] as? List<*>
      if (responseAudience == null || !responseAudience.contains(clientId)) {
        logging.error("NativeSDK: Audience validation failed")
        cleanup()
        oidcParams.onError(InvalidCallbackError("Audience param did not matched expected value"))
        return
      }

      logging.debug("NativeSDK: All token validations passed, updating session")
      session.update(tokenResponse)

      cleanup()
      logging.info("NativeSDK: User signed in successfully")
      oidcParams.onSuccess()
    } catch (e: Exception) {
      logging.debug("NativeSDK: Token exchange or validation failed", e)
      cleanup()
      oidcParams.onError(UnknownError(e))
    }
  }

  /**
   * Check if access token should be refreshed and if so, attempt to do so
   *
   * @return Boolean `true` if access token was refreshed, `false` otherwise
   */
  internal suspend fun refreshTokensIfNeeded(): Boolean =
      tokenRefreshMutex.withLock {
        val accessTokenExpiresAt = session.profile.value?.accessTokenExpiresAt
        if (accessTokenExpiresAt == null ||
            accessTokenExpiresAt.isAfter(Instant.now(clock).plus(1, ChronoUnit.MINUTES))) {
          return false
        }

        logging.debug("NativeSDK: Access token expired or expiring soon, attempting refresh")
        val refreshToken = session.profile.value?.tokenResponse?.refreshToken
        if (refreshToken == null) {
          logging.warn("NativeSDK: No refresh token available, clearing session")
          session.clear()
          return false
        }

        try {
          val tokenResponse =
              oidcHandlerService.tokenRefresh(
                  URLBuilder(issuer).apply { path("/oauth2/token") }.toString(),
                  TokenRefreshParams(refreshToken, clientId),
              )

          session.update(tokenResponse)
          logging.debug("NativeSDK: Token refresh successful")
          return true
        } catch (e: HttpError) {
          if (e.statusCode in listOf(401, 403)) {
            logging.warn(
                "NativeSDK: Token refresh failed with status ${e.statusCode}, clearing session"
            )
            session.clear()
            return false
          }
          logging.error("NativeSDK: Token refresh failed with status ${e.statusCode}", e)
          throw e
        } catch (e: Exception) {
          logging.error("NativeSDK: Token refresh failed", e)
          throw e
        }
      }

  private fun cleanup() {
    session.setLoginInProgress(false)
    loginController = null
  }
}

data class LoginParameters(
    val prompt: String? = null,
    val loginHint: String? = null,
    val acrValue: String? = null,
    val scopes: List<String>? = null,
    val audiences: List<String>? = null,
)

enum class SdkMode(val value: String) {
  Android("android"),
  AndroidMinimal("android-minimal"),
}
