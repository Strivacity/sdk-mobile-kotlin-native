package com.strivacity.android.native_sdk.render

import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.SessionExpiredError
import com.strivacity.android.native_sdk.render.models.FormWidget
import com.strivacity.android.native_sdk.render.models.Messages
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.service.OidcParams
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class LoginController
internal constructor(
    private val nativeSDK: NativeSDK,
    private val loginHandlerService: LoginHandlerService,
    internal val oidcParams: OidcParams,
    private val context: WeakReference<Context>
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
      withContext(Dispatchers.IO) { updateScreen(loginHandlerService.initCall()) }

  suspend fun submit(formId: String) =
      withContext(Dispatchers.IO) {
        val body =
            when (val map = forms.value[formId]) {
              null -> mapOf()
              else -> unfoldMap(map)
            }

        submit(formId, body)
      }

  internal suspend fun submit(formId: String, body: Map<String, Any>) =
      withContext(Dispatchers.IO) {
        _processing.value = true

        try {
          updateScreen(loginHandlerService.submitForm(formId, body))
        } catch (e: SessionExpiredError) {
          nativeSDK.cancelFlow(e)
        } catch (e: Exception) {
          Log.d("LoginController", "Failed to load screen", e)
          triggerFallback()
        }
      }

  fun <T> stateForWidget(formId: String, widgetId: String, defaultValue: T): MutableStateFlow<T> {
    @Suppress("UNCHECKED_CAST")
    return forms.value
        .computeIfAbsent(formId, { mutableMapOf() })
        .computeIfAbsent(widgetId, { MutableStateFlow(defaultValue) }) as MutableStateFlow<T>
  }

  private suspend fun updateScreen(screen: Screen) {
    _processing.value = false

    if (screen.finalizeUrl != null) {
      nativeSDK.continueFlow(screen.finalizeUrl)
      return
    }

    if (screen.hostedUrl != null && screen.forms == null && screen.messages == null) {
      triggerFallback(screen.hostedUrl)
      return
    }

    if (screen.forms != null) {
      this._screen.value = screen
      this._messages.value = screen.messages
      updateFormValues(screen.forms)
    } else {
      this._screen.value = this._screen.value?.copy(messages = screen.messages)
      this._messages.value = screen.messages
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
    val hostedUrl =
        _screen.value?.hostedUrl ?: throw IllegalStateException("Hosted url not available")
    triggerFallback(hostedUrl)
  }

  private fun triggerFallback(uri: String) {
    val ctx = context.get() ?: throw IllegalStateException("Context is no longer available")

    isRedirectExpected = true
    val customTabsIntent = CustomTabsIntent.Builder().build()
    customTabsIntent.launchUrl(ctx, uri.toUri())
  }
}
