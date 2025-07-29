package com.strivacity.android.native_sdk

import android.content.Context
import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.service.HttpService
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.service.OIDCHandlerService
import com.strivacity.android.native_sdk.service.OidcParams
import com.strivacity.android.native_sdk.service.TokenExchangeParams
import com.strivacity.android.native_sdk.service.TokenRefreshParams
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.path
import java.time.Instant

class NativeSDK(
    private val issuer: String,
    private val clientId: String,
    private val redirectURI: String,
    private val postLogoutURI: String,
    storage: Storage
) {
  val session: Session = Session(storage)
  var loginController: LoginController? = null

  private val httpService = HttpService()
  private val oidcHandlerService = OIDCHandlerService(httpService)

  suspend fun initializeSession() {
    session.load()
    refreshTokensIfNeeded()
  }

  suspend fun login(
      context: Context,
      onSuccess: () -> Unit,
      onError: (Error) -> Unit,
      loginParameters: LoginParameters? = null
  ) {
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

              val scopes = loginParameters?.scopes ?: listOf("openid", "profile")
              parameters.append("scope", scopes.joinToString(separator = " "))

              if (loginParameters?.loginHint != null) {
                parameters.append("login_hint", loginParameters.loginHint)
              }

              if (loginParameters?.acrValue != null) {
                parameters.append("acr_values", loginParameters.acrValue)
              }

              if (loginParameters?.prompt != null) {
                parameters.append("prompt", loginParameters.prompt)
              }
            }
            .build()

    try {
      val parameters = oidcHandlerService.handleCall(url)

      val sessionId = parameters["session_id"]
      if (sessionId == null) {
        continueFlow(oidcParams, parameters)
        return
      }

      val loginHandlerService = LoginHandlerService(httpService, issuer, sessionId)
      val loginController = LoginController(this, loginHandlerService, oidcParams, context)

      loginController.initialize()
      this.loginController = loginController

      session.setLoginInProgress(true)
    } catch (e: Exception) {
      onError(UnknownError(e))
    }
  }

  suspend fun isAuthenticated(): Boolean {
    refreshTokensIfNeeded()
    return session.profile.value != null
  }

  suspend fun getAccessToken(): String? {
    refreshTokensIfNeeded()
    return session.profile.value?.tokenResponse?.accessToken
  }

  fun isRedirectExpected(): Boolean {
    return loginController?.isRedirectExpected ?: false
  }

  suspend fun continueFlow(uri: String?) {
    val oidcParams = loginController?.oidcParams ?: return

    if (uri == null) {
      cancelFlow(HostedFlowCanceledError())
      return
    }

    try {
      val parameters = oidcHandlerService.handleCall(URLBuilder(uri).build())
      continueFlow(oidcParams, parameters)
    } catch (e: Exception) {
      cleanup()
      oidcParams.onError(UnknownError(e))
    }
  }

  fun cancelFlow(error: Error? = null) {
    val loginController = loginController ?: return

    cleanup()
    if (error != null) {
      loginController.oidcParams.onError(error)
    }
  }

  suspend fun logout() {
    val idToken = session.profile.value?.tokenResponse?.idToken

    session.clear()

    if (idToken == null) {
      return
    }

    val url =
        URLBuilder(issuer)
            .apply {
              path("/oauth2/sessions/logout")
              parameters.append("id_token_hint", idToken)
              parameters.append("post_logout_redirect_uri", postLogoutURI)
            }
            .build()

    oidcHandlerService.handleCall(url)
  }

  private suspend fun continueFlow(oidcParams: OidcParams, parameters: Parameters) {
    val sessionId = parameters["session_id"]
    if (sessionId != null) {
      try {
        loginController?.initialize()
      } catch (e: Exception) {
        cleanup()
        oidcParams.onError(UnknownError(e))
      }

      return
    }

    val error = parameters["error"]
    val errorDescription = parameters["error_description"]
    if (error != null && errorDescription != null) {
      session.clear()
      cleanup()
      oidcParams.onError(OidcError(error, errorDescription))
      return
    }

    val state = parameters["state"]
    if (state != oidcParams.state) {
      cleanup()
      oidcParams.onError(InvalidCallbackError("State param did not matched expected value"))
      return
    }

    val code = parameters["code"] ?: error("Code missing from response")

    try {
      val tokenResponse =
          oidcHandlerService.tokenExchange(
              URLBuilder(issuer).apply { path("/oauth2/token") }.toString(),
              TokenExchangeParams(code, oidcParams.codeVerifier, redirectURI, clientId))

      if (oidcParams.nonce != extractClaims(tokenResponse)["nonce"] as String?) {
        cleanup()
        oidcParams.onError(InvalidCallbackError("Nonce param did not matched expected value"))
        return
      }

      session.update(tokenResponse)

      cleanup()
      oidcParams.onSuccess()
    } catch (e: Exception) {
      cleanup()
      oidcParams.onError(UnknownError(e))
    }
  }

  private suspend fun refreshTokensIfNeeded() {
    val accessTokenExpiresAt = session.profile.value?.accessTokenExpiresAt
    if (accessTokenExpiresAt == null || accessTokenExpiresAt.isAfter(Instant.now())) {
      return
    }

    val refreshToken = session.profile.value?.tokenResponse?.refreshToken
    if (refreshToken == null) {
      session.clear()
      return
    }

    try {
      val tokenResponse =
          oidcHandlerService.tokenRefresh(
              URLBuilder(issuer).apply { path("/oauth2/token") }.toString(),
              TokenRefreshParams(refreshToken, clientId))

      session.update(tokenResponse)
    } catch (e: Exception) {
      session.clear()
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
    val scopes: List<String>? = null
)
