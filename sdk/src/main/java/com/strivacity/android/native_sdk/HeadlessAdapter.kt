package com.strivacity.android.native_sdk

import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.render.models.Messages
import com.strivacity.android.native_sdk.render.models.Screen
import java.util.Objects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HeadlessAdapter {
  private val scope = CoroutineScope(Dispatchers.Main)

  private val nativeSDK: NativeSDK
  private val loginController: LoginController

  private val delegate: HeadlessAdapterDelegate

  constructor(nativeSDK: NativeSDK, delegate: HeadlessAdapterDelegate) {
    this.nativeSDK = nativeSDK
    this.delegate = delegate

    if (nativeSDK.loginController == null) {
      error("No login in progress")
    }

    loginController = nativeSDK.loginController!!

    scope.launch {
      var oldScreen = loginController.screen.value
      loginController.screen.collectLatest { _ ->
        if (!nativeSDK.session.loginInProgress.value) {
          return@collectLatest
        }
        var currentScreen = oldScreen
        val newScreen = loginController.screen.value
        oldScreen = newScreen

        if (Objects.equals(currentScreen?.screen, newScreen?.screen) &&
            Objects.equals(currentScreen?.forms, newScreen?.forms) &&
            Objects.equals(currentScreen?.layout, newScreen?.layout) &&
            !Objects.equals(currentScreen?.messages, newScreen?.messages)) {
          delegate.refreshScreen(getScreen())
          return@collectLatest
        }

        delegate.renderScreen(getScreen())
      }
    }
  }

  fun initialize() {
    delegate.renderScreen(getScreen())
  }

  fun getScreen(): Screen {
    if (loginController.screen.value == null) {
      error("Screen not set")
    }

    return loginController.screen.value!!
  }

  suspend fun submit(formId: String, body: Map<String, Any>) =
      withContext(Dispatchers.IO) { loginController.submit(formId, body) }

  fun messages(): StateFlow<Messages?> {
    return loginController.messages
  }
}

interface HeadlessAdapterDelegate {
  fun renderScreen(screen: Screen)

  fun refreshScreen(screen: Screen)
}
