package com.strivacity.android.headlessdemo

import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.strivacity.android.headlessdemo.login.GenericResultView
import com.strivacity.android.headlessdemo.login.IdentificationView
import com.strivacity.android.headlessdemo.login.MFAEnrollChallengeView
import com.strivacity.android.headlessdemo.login.MFAEnrollStartView
import com.strivacity.android.headlessdemo.login.MFAEnrollTargetSelectView
import com.strivacity.android.headlessdemo.login.MFAMethodView
import com.strivacity.android.headlessdemo.login.MFAPasscode
import com.strivacity.android.headlessdemo.login.PasswordView
import com.strivacity.android.headlessdemo.login.RegistrationView
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.HeadlessAdapterDelegate
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.render.models.GlobalMessages
import com.strivacity.android.native_sdk.render.models.Screen
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(nativeSDK: NativeSDK) {

  val loginScreenModel by remember { mutableStateOf(LoginScreenModel()) }

  val headlessAdapter by remember { mutableStateOf(HeadlessAdapter(nativeSDK, loginScreenModel)) }

  val coroutineScope = rememberCoroutineScope()
  var showToast by remember { mutableStateOf(AtomicBoolean(false)) }
  LaunchedEffect(Unit) {
    coroutineScope.launch {
      headlessAdapter.messages().collectLatest {
        showToast.set(headlessAdapter.messages().value is GlobalMessages)
      }
    }
  }

  LaunchedEffect(Unit) { headlessAdapter.initialize() }

  val screen by loginScreenModel.screen.collectAsState()

  if (screen == null) {
    Text("Loading")
  } else {
    val messages by headlessAdapter.messages().collectAsState()
    if (messages is GlobalMessages && showToast.get()) {
      Toast.makeText(
              LocalContext.current, (messages as GlobalMessages).global.text, Toast.LENGTH_SHORT)
          .show()
      showToast.set(false)
    }

    when (screen!!.screen) {
      "identification" -> {
        IdentificationView(screen!!, headlessAdapter)
      }
      "password" -> {
        PasswordView(screen!!, headlessAdapter)
      }
      "registration" -> {
        RegistrationView(screen!!, headlessAdapter)
      }
      "mfaEnrollStart" -> {
        MFAEnrollStartView(screen!!, headlessAdapter)
      }
      "mfaEnrollTargetSelect" -> {
        MFAEnrollTargetSelectView(screen!!, headlessAdapter)
      }
      "mfaEnrollChallenge" -> {
        MFAEnrollChallengeView(screen!!, headlessAdapter)
      }
      "genericResult" -> {
        GenericResultView(screen!!, headlessAdapter)
      }
      "mfaMethod" -> {
        MFAMethodView(screen!!, headlessAdapter)
      }
      "mfaPasscode" -> {
        MFAPasscode(screen!!, headlessAdapter)
      }
      else -> {
        Text("Unknown screen")
      }
    }
  }
}

class LoginScreenModel : HeadlessAdapterDelegate {
  private val _screen = MutableStateFlow<Screen?>(null)
  val screen: StateFlow<Screen?> = _screen

  override fun renderScreen(screen: Screen) {
    _screen.value = screen
  }

  override fun refreshScreen(screen: Screen) {
    _screen.value = screen
  }
}
