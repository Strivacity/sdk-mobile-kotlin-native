package com.strivacity.android.native_sdk.render

import android.os.Looper
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.strivacity.android.native_sdk.Logging
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.PlatformError
import com.strivacity.android.native_sdk.SessionExpiredError
import com.strivacity.android.native_sdk.render.models.AssertionOptions
import com.strivacity.android.native_sdk.render.models.EnrollOptions
import com.strivacity.android.native_sdk.render.models.FormWidget
import com.strivacity.android.native_sdk.render.models.Messages
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.Widget
import com.strivacity.android.native_sdk.render.models.WithAssertionOptions
import com.strivacity.android.native_sdk.render.models.WithEnrollmentOptions
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.service.OidcParams
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

typealias FallbackHandler = (uriToLoad: String) -> Unit

class LoginController
    internal constructor(
        private val nativeSDK: NativeSDK,
        private val loginHandlerService: LoginHandlerService,
        internal val oidcParams: OidcParams,
        private val fallbackHandler: FallbackHandler,
        private val logging: Logging,
        private val credentialManagerProvider: CredentialManagerProvider,
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

        fun close() {
            nativeSDK.cancelFlow()
        }

        private fun findEnrollmentWidget(formId: String): WithEnrollmentOptions<Widget> {
            val enrollWidget =
                screen.value
                    ?.forms
                    ?.firstOrNull { form -> form.id == formId }
                    ?.widgets
                    ?.filterIsInstance<WithEnrollmentOptions<Widget>>()
                    ?.firstOrNull()
            require(enrollWidget != null) {
                "No enrollment widget found in form with id: $formId"
            }
            return enrollWidget
        }

        private fun findAssertionWidget(formId: String): WithAssertionOptions<Widget> {
            val assertionWidget =
                screen.value
                    ?.forms
                    ?.firstOrNull { form -> form.id == formId }
                    ?.widgets
                    ?.filterIsInstance<WithAssertionOptions<Widget>>()
                    ?.firstOrNull()
            require(assertionWidget != null) {
                "No assertion widget found in form with id: $formId"
            }
            return assertionWidget
        }

        suspend fun submit(
            formId: String,
            body: Map<String, Any> = mapOf(),
        ) {
            logging.debug(
                "LoginController: Submitting form `$formId` on screen " +
                    "`${screen.value?.screen ?: "unknown"}`",
            )
            _processing.value = true
            try {
                when (formId) {
                    "passkey",
                    "mfaWebAuthnAssertion",
                    -> {
                        val assertionWidget = findAssertionWidget(formId)
                        val response = assertPasskeyOrWebauthn(assertionWidget.assertionOptions)
                        val credential = response.credential as PublicKeyCredential
                        val decodedCredential =
                            Json.parseToJsonElement(
                                credential.authenticationResponseJson,
                            )
                        stateForWidget(
                            formId = formId,
                            widgetId = assertionWidget.widget.id,
                            decodedCredential,
                        ).value = decodedCredential
                    }

                    "passkeyEnroll",
                    "mfaEnrollWebAuthn",
                    -> {
                        val enrollWidget = findEnrollmentWidget(formId)
                        val response = enrollPasskeyOrWebauthn(enrollWidget.enrollOptions)
                        val decodedResponse =
                            Json.parseToJsonElement(
                                response.registrationResponseJson,
                            )
                        stateForWidget(
                            formId = formId,
                            widgetId = enrollWidget.widget.id,
                            decodedResponse,
                        ).value = decodedResponse
                    }
                }
            } finally {
                _processing.value = false
            }
            withContext(Dispatchers.IO) {
                val payload =
                    when (val formValues = forms.value[formId]) {
                        null -> body

                        // merge form values with body - override default values from form values
                        // with body values
                        else -> unfoldMap(formValues) + body
                    }
                doSubmit(formId, payload)
            }
        }

        private suspend fun doSubmit(
            formId: String,
            payload: Map<String, Any>,
        ) {
            try {
                updateScreen(loginHandlerService.submitForm(formId, payload))
            } catch (e: SessionExpiredError) {
                logging.warn("LoginController: Session expired during form submission: $formId")
                nativeSDK.cancelFlow(e)
            } catch (e: Exception) {
                logging.warn(
                    "LoginController: Failed to submit form: `$formId` on screen " +
                        "`${screen.value?.screen ?: "unknown"}`",
                    e,
                )
                logging.warn(e.stackTraceToString())
                triggerFallback()
            } finally {
                _processing.value = false
            }
        }

        fun <T> stateForWidget(
            formId: String,
            widgetId: String,
            defaultValue: T,
        ): MutableStateFlow<T> {
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
                    "LoginController: Loading screen `${screen.screen ?: "unknown"}`",
                )
                logging.debug(
                    "LoginController: forms displayed: ${screen.forms.map { (id, _) ->
                        id
                    }}",
                )
                this._screen.value = screen
                this._messages.value = screen.messages
                updateFormValues(screen.forms)
            } else {
                val updatedScreen = this._screen.value?.copy(messages = screen.messages)
                this._screen.value = updatedScreen
                this._messages.value = screen.messages
                logging.info(
                    "LoginController: Updating screen " +
                        "`${updatedScreen?.screen ?: "unknown"}` messages only",
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
                            currentMap.getOrPut(
                                key,
                            ) {
                                mutableMapOf<String, Any>()
                            } as MutableMap<String, Any>
                    }
                }
            }
            return unfoldedMap
        }

        fun triggerFallback() {
            val hostedUrl = _screen.value?.hostedUrl
            if (hostedUrl == null) {
                logging.error(
                    "LoginController: Cannot trigger fallback from screen " +
                        "`${screen.value?.screen}` - hosted URL not available",
                )
                throw IllegalStateException("Hosted url not available")
            }
            logging.warn(
                "LoginController: Triggering fallback to hosted URL: ${hostedUrl.encodeURLPath()}",
            )
            triggerFallback(hostedUrl)
        }

        private fun triggerFallback(uri: String) {
            logging.warn("LoginController: Triggering fallback to hosted page")
            isRedirectExpected = true
            fallbackHandler(uri)
        }

        private suspend fun enrollPasskeyOrWebauthn(
            enrollOptions: EnrollOptions,
        ): androidx.credentials.CreatePublicKeyCredentialResponse {
            logging.debug("LoginController: Passkey enrollment in progress")
            val activityContext = credentialManagerProvider.activityContext()
            require(activityContext != null) {
                "For Passkey/WebAuthn enrollment, providing Activity context to login/entry is mandatory"
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                logging.warn(
                    "Passkey/WebAuthn related operations should be invoked from Main thread",
                )
            }
            try {
                val requestJson = jsonConverter.encodeToString(enrollOptions)
                val request = CreatePublicKeyCredentialRequest(requestJson = requestJson)
                return credentialManagerProvider.createCredential(activityContext, request)
            } catch (e: CreateCredentialException) {
                logging.warn("LoginController: Enrollment failed", e)
                throw PlatformError("Could not perform passkey enrollment", cause = e)
            }
        }

        private suspend fun assertPasskeyOrWebauthn(
            assertionOptions: AssertionOptions,
        ): androidx.credentials.GetCredentialResponse {
            logging.debug("LoginController: Passkey login in progress")
            val activityContext = credentialManagerProvider.activityContext()
            require(activityContext != null) {
                "For Passkey/WebAuthn assertion support, providing Activity context " +
                    "to login/entry is mandatory"
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                logging.warn(
                    "Passkey/WebAuthn related operations should be invoked from Main thread",
                )
            }
            try {
                val getPublicKeyCredentialOption =
                    GetPublicKeyCredentialOption(
                        requestJson = jsonConverter.encodeToString(assertionOptions),
                    )
                val getCredentialRequest =
                    GetCredentialRequest(listOf(getPublicKeyCredentialOption))
                return credentialManagerProvider.getCredential(
                    activityContext,
                    getCredentialRequest,
                )
            } catch (e: GetCredentialException) {
                logging.warn("LoginController: Assertion failed", e)
                throw PlatformError("Could not perform login with passkey", cause = e)
            }
        }

        /**
         * Dedicated JSON serializer for WebAuthn/passkey request payloads.
         *
         * `explicitNulls = false` is required here so nullable properties are omitted from the serialized
         * JSON instead of being emitted as `"field": null`. The WebAuthn request JSON consumed by Android
         * credential APIs expects absent optional fields to stay absent, and changing this back to the
         * default serializer configuration can produce non-compliant payloads.
         */
        private val jsonConverter: Json by lazy {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }
    }
