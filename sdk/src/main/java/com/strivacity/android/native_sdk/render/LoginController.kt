package com.strivacity.android.native_sdk.render

import com.strivacity.android.native_sdk.Logging
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.SessionExpiredError
import com.strivacity.android.native_sdk.render.models.FormWidget
import com.strivacity.android.native_sdk.render.models.Messages
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.service.OidcParams
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

typealias FallbackHandler = (uriToLoad: String) -> Unit

class LoginController
internal constructor(
    private val nativeSDK: NativeSDK,
    private val loginHandlerService: LoginHandlerService,
    internal val oidcParams: OidcParams,
    private val fallbackHandler: FallbackHandler,
    private val logging: Logging,
) {
  private val _screen = MutableStateFlow<Screen?>(null)
  val screen: StateFlow<Screen?> = _screen

  private val _messages = MutableStateFlow<Messages?>(null)
  val messages: StateFlow<Messages?> = _messages

  private val _processing = MutableStateFlow(false)
  val processing: StateFlow<Boolean> = _processing

  private val forms =
      MutableStateFlow(mutableMapOf<String, MutableMap<String, MutableStateFlow<*>>>())

  internal var isRedirectExpected = false

  suspend fun initialize() =
      withContext(Dispatchers.IO) {
        logging.debug("LoginController: Initializing login flow")
        try {
          updateScreen(loginHandlerService.initCall())
          logging.debug("LoginController: Login flow initialized successfully")
        } catch (e: Exception) {
          logging.error("LoginController: Failed to initialize login flow", e)
          throw e
        }
      }

  suspend fun submit(formId: String) =
      withContext(Dispatchers.IO) {
        val body =
            when (val map = forms.value[formId]) {
              null -> {
                logging.warn("LoginController: No form data found for formId: $formId")
                mapOf()
              }
              else -> unfoldMap(map)
            }

        submit(formId, body)
      }

  internal suspend fun submit(formId: String, body: Map<String, Any>) =
      withContext(Dispatchers.IO) {
        logging.debug(
            "LoginController: Submitting form `$formId` on screen `${screen.value?.screen ?: "unknown"}`"
        )
        _processing.value = true

        try {
          updateScreen(loginHandlerService.submitForm(formId, body))
        } catch (e: SessionExpiredError) {
          logging.warn("LoginController: Session expired during form submission: $formId")
          nativeSDK.cancelFlow(e)
        } catch (e: Exception) {
          logging.warn(
              "LoginController: Failed to submit form: `$formId` on screen `${screen.value?.screen ?: "unknown"}`",
              e,
          )
          logging.warn(e.stackTraceToString())
          triggerFallback()
        }
      }

  fun <T> stateForWidget(formId: String, widgetId: String, defaultValue: T): MutableStateFlow<T> {
    @Suppress("UNCHECKED_CAST")
    return forms.value
        .computeIfAbsent(formId) { mutableMapOf() }
        .computeIfAbsent(widgetId) { MutableStateFlow(defaultValue) } as MutableStateFlow<T>
  }

  private suspend fun updateScreen(screen: Screen) {
    _processing.value = false
    if (screen.finalizeUrl != null) {
      logging.debug("LoginController: Finalizing flow")
      nativeSDK.continueFlow(screen.finalizeUrl)
      return
    }

    if (screen.hostedUrl != null && screen.forms == null && screen.messages == null) {
      logging.debug("LoginController: Triggering cloud-triggered fallback")
      triggerFallback(screen.hostedUrl)
      return
    }

    if (screen.forms != null) {
      logging.info(
          "LoginController: Loading screen `${screen.screen ?: "unknown"}`"
      )
      logging.debug("LoginController: forms displayed: ${screen.forms.map { (id, _) -> id }}")
      this._screen.value = screen
      this._messages.value = screen.messages
      updateFormValues(screen.forms)
    } else {
      val updatedScreen = this._screen.value?.copy(messages = screen.messages)
      this._screen.value = updatedScreen
      this._messages.value = screen.messages
      logging.info(
          "LoginController: Updating screen `${updatedScreen?.screen ?: "unknown"}` messages only"
      )
    }
  }

  private fun updateFormValues(formWidgets: List<FormWidget>) {
    val forms = mutableMapOf<String, MutableMap<String, MutableStateFlow<*>>>()

    formWidgets.forEach { formWidget ->
      formWidget.widgets.forEach { widget ->
        val value = widget.value()
        if (value != null) {
          forms.computeIfAbsent(formWidget.id, { mutableMapOf() })[widget.id] =
              MutableStateFlow(value)
        }
      }
    }

    this.forms.value = forms
  }

  private fun unfoldMap(flattenedMap: Map<String, MutableStateFlow<*>>): Map<String, Any> {
    val unfoldedMap = mutableMapOf<String, Any>()

    for ((path, value) in flattenedMap) {
      val keys = path.split('.')
      var currentMap = unfoldedMap

      for (i in keys.indices) {
        val key = keys[i]
        if (i == keys.size - 1) {
          if (value.value != null) {
            currentMap[key] = value.value as Any
          }
        } else {
          @Suppress("UNCHECKED_CAST")
          currentMap =
              currentMap.getOrPut(key) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        }
      }
    }
    return unfoldedMap
  }

  fun triggerFallback() {
    val hostedUrl = _screen.value?.hostedUrl
    if (hostedUrl == null) {
      logging.error(
          "LoginController: Cannot trigger fallback from screen `${screen.value?.screen}` - hosted URL not available"
      )
      throw IllegalStateException("Hosted url not available")
    }
    logging.warn("LoginController: Triggering fallback to hosted URL: ${hostedUrl.encodeURLPath()}")
    triggerFallback(hostedUrl)
  }

  private fun triggerFallback(uri: String) {
    logging.warn("LoginController: Triggering fallback to hosted page")
    isRedirectExpected = true
    fallbackHandler(uri)
  }
}
