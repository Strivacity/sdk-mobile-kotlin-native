package com.strivacity.android.app

import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.strivacity.android.app.ui.theme.SdkmobilekotlinnativeTheme
import com.strivacity.android.native_sdk.Error
import com.strivacity.android.native_sdk.HostedFlowCanceledError
import com.strivacity.android.native_sdk.LoginParameters
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.OidcError
import com.strivacity.android.native_sdk.SessionExpiredError
import com.strivacity.android.native_sdk.WorkflowError
import com.strivacity.android.native_sdk.render.FallbackHandler
import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.render.models.GlobalMessages
import java.lang.ref.WeakReference
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val ENTRY_URL: String = "https://example.org/entry"

  private val WORKFLOW_ERROR_ID_TO_MESSAGE: Map<WorkflowError.WorkflowErrorId, String> =
      mapOf(
          WorkflowError.WorkflowErrorId.MAGIC_LINK_EXPIRED to "Your link has expired",
          WorkflowError.WorkflowErrorId.CLIENT_MISMATCH to
              "An unexpected error occurred, please try again (001)",
          WorkflowError.WorkflowErrorId.INVALID_REDIRECT_URI to
              "An unexpected error occurred, please try again (002)")

  private var nativeSDK: NativeSDK? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    nativeSDK =
        NativeSDK(
            "https://example.org",
            "",
            "android://native-flow",
            "android://native-flow",
            SharedPreferenceStorage(
                this@MainActivity.getSharedPreferences("kotlin-demo", Context.MODE_PRIVATE)))

    enableEdgeToEdge()
    setContent { SdkmobilekotlinnativeTheme { Main() } }
  }

  override fun onResume() {
    super.onResume()

    if (intent.dataString?.startsWith(ENTRY_URL) == true) {
      entry(intent.data)
      intent.data = null
    }
  }

  private fun entry(uri: Uri?) {
    lifecycleScope.launch {
      val context = this@MainActivity
      val customTabsHandler: FallbackHandler = { uri ->
        run {
          val customTabsIntent = CustomTabsIntent.Builder().build()
          customTabsIntent.launchUrl(context, uri.toUri())
        }
      }

      nativeSDK?.entry(
          uri,
          customTabsHandler,
          {},
          { e ->
            var mappedErrorMessage: String? = null
            if (e is WorkflowError) {
              val idValue: String = e.error
              val errorId: WorkflowError.WorkflowErrorId? =
                  WorkflowError.WorkflowErrorId.valueOfId(idValue)
              mappedErrorMessage = WORKFLOW_ERROR_ID_TO_MESSAGE[errorId]
            }

            val toastText = mappedErrorMessage ?: "Something bad happened, please try again"
            lifecycleScope.launch { Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show() }
          })
      intent.data = null
    }
  }

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
    super.onNewIntent(intent, caller)
    setIntent(intent)
  }

  @Composable
  fun Main() {
    Scaffold(
        modifier = Modifier.fillMaxSize(), floatingActionButton = { CancelFAB(nativeSDK!!) }) {
            innerPadding ->
          Box(
              modifier = Modifier.fillMaxSize().padding(innerPadding),
              contentAlignment = Alignment.Center) {
                Login(nativeSDK!!)
              }
        }
  }
}

@Composable
fun CancelFAB(nativeSDK: NativeSDK) {
  val loginInProgress by nativeSDK.session.loginInProgress.collectAsState()

  if (loginInProgress) {
    FloatingActionButton(
        onClick = { nativeSDK.cancelFlow() },
    ) {
      Icon(Icons.Filled.Close, "Cancel login flow")
    }
  }
}

@Composable
fun Login(nativeSDK: NativeSDK) {
  val coroutineScope = rememberCoroutineScope()
  val loginInProgress by nativeSDK.session.loginInProgress.collectAsState()
  val profile by nativeSDK.session.profile.collectAsState()

  var error by remember { mutableStateOf(null as Error?) }
  var loading by remember { mutableStateOf(true) }

  val context = LocalContext.current

  DisposableEffect(Unit) {
    val activity = context as? ComponentActivity
    val lifecycle = activity?.lifecycle
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME && nativeSDK.isRedirectExpected()) {
        coroutineScope.launch {
          val uri =
              when (activity?.intent?.data) {
                null -> null
                else -> activity.intent?.data.toString()
              }

          try {
            nativeSDK.continueFlow(uri)
          } catch (e: Error) {
            error = e
          }
        }
      }
    }

    lifecycle?.addObserver(observer)

    onDispose { lifecycle?.removeObserver(observer) }
  }

  if (loading) {
    Text("Loading...")
  } else {
    if (profile != null) {
      Column {
        Text("Hello ${profile!!.claims["given_name"]}")
        Button(onClick = { coroutineScope.launch { nativeSDK.logout() } }) { Text("Logout") }
        Button(onClick = { coroutineScope.launch { nativeSDK.revoke() } }) {Text("Revoke")}
        Button(
            onClick = {
              coroutineScope.launch {
                try {
                  val accessToken = nativeSDK.getAccessToken()
                  // println(accessToken) // uncomment to fetch access token from the log during
                  // development
                  Toast.makeText(context, accessToken, Toast.LENGTH_LONG).show()
                } catch (e: Throwable) {
                  Toast.makeText(
                          context, "Unable to fetch access token ${e.message}", Toast.LENGTH_LONG)
                      .show()
                }
              }
            }) {
              Text("Get Access Token")
            }

        Button(
            onClick = {
              coroutineScope.launch {
                val idToken = profile!!.idToken
                println(idToken)
                Toast.makeText(context, idToken, Toast.LENGTH_LONG).show()
              }
            }) {
              Text("Get ID Token")
            }
      }
    } else if (loginInProgress) {
      LoginView(nativeSDK.loginController!!)
    } else {

      var audiences by remember { mutableStateOf("") }

      Column {
        CustomAudienceInput(audiences = audiences, onAudiencesChanges = { audiences = it })
        Button(
            onClick = {
              coroutineScope.launch {
                error = null
                try {
                  nativeSDK.login(
                      WeakReference(context),
                      {},
                      { error = it },
                      LoginParameters(
                          scopes = listOf("openid", "profile", "offline"),
                          audiences = audiences.split(" ")))
                } catch (e: Error) {
                  error = e
                }
              }
            }) {
              Text("Login")
            }

        when (error) {
          null -> {} // no error

          is OidcError ->
              Text(
                  (error as OidcError).errorDescription ?: (error as OidcError).error,
                  color = Color.Red)

          is HostedFlowCanceledError -> Text("Hosted flow canceled", color = Color.Red)
          is SessionExpiredError -> Text("Session expired", color = Color.Red)

          else -> Text("N/A", color = Color.Red)
        }
      }
    }
  }

  LaunchedEffect(Unit) {
    coroutineScope.launch {
      try {
        nativeSDK.initializeSession()
      } catch (e: Throwable) {
        Toast.makeText(context, "Failed to initialize ${e.message}", Toast.LENGTH_SHORT).show()
      }
      loading = false
    }
  }
}

@Composable
fun LoginView(loginController: LoginController) {
  val screen by loginController.screen.collectAsState()
  val layout = screen?.layout
  Layout(loginController, screen!!, layout!!)

  val messages by loginController.messages.collectAsState()
  when (messages) {
    is GlobalMessages -> {
      Toast.makeText(
              LocalContext.current, (messages as GlobalMessages).global.text, Toast.LENGTH_LONG)
          .show()
    }
    else -> {}
  }
}

@Composable
fun CustomAudienceInput(audiences: String, onAudiencesChanges: (String) -> Unit) {
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current

  OutlinedTextField(
      value = audiences,
      onValueChange = onAudiencesChanges,
      label = { Text("Custom audiences") },
      supportingText = { Text("Optional values separated by space") },
      trailingIcon = {
        IconButton(
            onClick = {
              try {
                uriHandler.openUri(
                    "https://docs.strivacity.com/docs/oauth2-oidc-properties-setup#allowed-custom-audiences")
              } catch (ex: IllegalArgumentException) {
                Log.e("LOGIN", "Could not open documentation U", ex)
                Toast.makeText(context, "Could not open documentation", Toast.LENGTH_SHORT).show()
              }
            }) {
              Icon(
                  imageVector = Icons.Outlined.Info,
                  contentDescription = "Documentation about Strivacity Custom Audiences")
            }
      })
}
