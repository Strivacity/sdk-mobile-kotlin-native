package com.strivacity.android.native_sdk

import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.render.models.Messages
import com.strivacity.android.native_sdk.render.models.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class HeadlessAdapter {
  protected val scope: CoroutineScope

  private val nativeSDK: NativeSDK
  protected val loginController: LoginController

  private val delegate: HeadlessAdapterDelegate

  /**
   * A [StateFlow] that emits the current processing state of the login flow.
   *
   * Emits `true` when a login operation (form submission, authentication request, etc.) is in progress.
   * Emits `false` when no operation is actively being processed.
   */
  @get:JvmSynthetic
  val isProcessing: StateFlow<Boolean>
    get()= loginController.processing

  constructor(
      nativeSDK: NativeSDK,
      delegate: HeadlessAdapterDelegate,
      scope: CoroutineScope = MainScope()
  ) {
    this.nativeSDK = nativeSDK
    this.delegate = delegate
    this.scope = scope

    if (nativeSDK.loginController == null) {
      throw IllegalStateException("No login in progress")
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

        if (currentScreen?.screen == newScreen?.screen &&
            currentScreen?.forms == newScreen?.forms &&
            currentScreen?.layout == newScreen?.layout &&
            currentScreen?.messages != newScreen?.messages) {
          delegate.refreshScreen(getScreen())
          return@collectLatest
        }

        delegate.renderScreen(getScreen())
      }
    }
  }

  fun dispose() = scope.cancel()

  fun initialize() {
    delegate.renderScreen(getScreen())
  }

  fun getScreen(): Screen {
    if (loginController.screen.value == null) {
      throw IllegalStateException("Screen not set")
    }

    return loginController.screen.value!!
  }

  @JvmSynthetic
  suspend fun submit(formId: String, body: Map<String, Any> = mapOf()) =
      loginController.submit(formId, body)


  fun messages(): StateFlow<Messages?> {
    return loginController.messages
  }

  fun triggerFallback() {
    loginController.triggerFallback()
  }

}

interface HeadlessAdapterDelegate {
  fun renderScreen(screen: Screen)

  fun refreshScreen(screen: Screen)
}
