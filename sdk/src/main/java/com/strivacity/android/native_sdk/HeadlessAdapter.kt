package com.strivacity.android.native_sdk

import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.render.models.Messages
import com.strivacity.android.native_sdk.render.models.Screen
import java.util.Objects
import kotlinx.coroutines.flow.StateFlow

class HeadlessAdapter {
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

  suspend fun submit(formId: String, body: Map<String, Any>) {
    val currentScreen = loginController.screen.value
    loginController.submit(formId, body)

    if (!nativeSDK.session.loginInProgress.value) {
      return
    }

    val newScreen = loginController.screen.value

    if (Objects.equals(currentScreen?.screen, newScreen?.screen) &&
        Objects.equals(currentScreen?.forms, newScreen?.forms) &&
        Objects.equals(currentScreen?.layout, newScreen?.layout) &&
        Objects.equals(currentScreen?.messages, newScreen?.messages)) {
      delegate.refreshScreen(getScreen())
      return
    }

    delegate.refreshScreen(getScreen())
  }

  fun messages(): StateFlow<Messages?> {
    return loginController.messages
  }
}

interface HeadlessAdapterDelegate {
  fun renderScreen(screen: Screen)

  fun refreshScreen(screen: Screen)
}
