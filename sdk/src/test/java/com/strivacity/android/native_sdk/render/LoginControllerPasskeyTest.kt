package com.strivacity.android.native_sdk.render

import FakeLogging
import android.app.Activity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.PlatformError
import com.strivacity.android.native_sdk.render.models.AssertionOptions
import com.strivacity.android.native_sdk.render.models.EnrollOptions
import com.strivacity.android.native_sdk.render.models.FormWidget
import com.strivacity.android.native_sdk.render.models.PasskeyEnrollWidget
import com.strivacity.android.native_sdk.render.models.PasskeyLoginWidget
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.service.OidcParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
internal class LoginControllerPasskeyTest {
    private lateinit var fakeLogging: FakeLogging
    private lateinit var mockNativeSDK: NativeSDK
    private lateinit var mockLoginHandlerService: LoginHandlerService
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        fakeLogging = FakeLogging()
        mockNativeSDK = mock()
        mockLoginHandlerService = mock()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
    }

    // region Helpers

    /**
     * Minimal [CredentialManagerProvider] stub that allows tests to control the activity context
     * and delegate credential operations to simple lambdas.
     */
    private class FakeCredentialManagerProvider(
        private val activityContext: Activity?,
        private val enroller: suspend (
            Activity,
            CreatePublicKeyCredentialRequest,
        ) -> CreatePublicKeyCredentialResponse = { _, _ -> throw UnsupportedOperationException() },
        private val asserter: suspend (
            Activity,
            GetCredentialRequest,
        ) -> GetCredentialResponse = { _, _ -> throw UnsupportedOperationException() },
    ) : CredentialManagerProvider {
        override fun activityContext(): Activity? = activityContext

        override suspend fun createCredential(
            context: Activity,
            request: CreatePublicKeyCredentialRequest,
        ): CreatePublicKeyCredentialResponse = enroller(context, request)

        override suspend fun getCredential(
            context: Activity,
            request: GetCredentialRequest,
        ): GetCredentialResponse = asserter(context, request)
    }

    private fun buildEnrollController(
        activityContext: Activity?,
        enroller: suspend (
            Activity,
            CreatePublicKeyCredentialRequest,
        ) -> CreatePublicKeyCredentialResponse,
    ) = LoginController(
        nativeSDK = mockNativeSDK,
        loginHandlerService = mockLoginHandlerService,
        oidcParams = OidcParams(onSuccess = {}, onError = {}, shouldVerifyIdTokenClaims = true),
        fallbackHandler = {},
        logging = fakeLogging,
        credentialManagerProvider =
            FakeCredentialManagerProvider(
                activityContext = activityContext,
                enroller = enroller,
            ),
    )

    private fun buildAssertController(
        activityContext: Activity?,
        asserter: suspend (Activity, GetCredentialRequest) -> GetCredentialResponse,
    ) = LoginController(
        nativeSDK = mockNativeSDK,
        loginHandlerService = mockLoginHandlerService,
        oidcParams = OidcParams(onSuccess = {}, onError = {}, shouldVerifyIdTokenClaims = true),
        fallbackHandler = {},
        logging = fakeLogging,
        credentialManagerProvider =
            FakeCredentialManagerProvider(
                activityContext = activityContext,
                asserter = asserter,
            ),
    )

    private val fakeEnrollOptions =
        EnrollOptions(
            rp = EnrollOptions.Rp(id = "example.com", name = "Example"),
            user = EnrollOptions.User(id = "dXNlcjE=", name = "User One", displayName = "User One"),
            challenge = "Y2hhbGxlbmdl",
            pubKeyCredParams = listOf(EnrollOptions.PubKeyCredParam(type = "public-key", alg = -7)),
            excludeCredentials = emptyList(),
            authenticatorSelection =
                EnrollOptions.AuthenticatorSelection(
                    authenticatorAttachment = null,
                    requireResidentKey = null,
                    residentKey = "preferred",
                    userVerification = "required",
                ),
            attestation = "none",
        )

    private val fakeAssertionOptions =
        AssertionOptions(
            allowCredentials = emptyList(),
            challenge = "Y2hhbGxlbmdl",
            rpId = "example.com",
            userVerification = "required",
            timeout = null,
        )

    private fun passkeyEnrollScreen() =
        Screen(
            screen = "passkey-enroll",
            branding = null,
            hostedUrl = "https://example.com/hosted",
            finalizeUrl = null,
            forms =
                listOf(
                    FormWidget(
                        id = "passkeyEnroll",
                        widgets =
                            listOf(
                                PasskeyEnrollWidget(
                                    id = "passkeyEnroll.credential",
                                    label = "Register passkey",
                                    render = null,
                                    enrollOptions = fakeEnrollOptions,
                                ),
                            ),
                    ),
                ),
            layout = null,
            messages = null,
        )

    private fun passkeyLoginScreen() =
        Screen(
            screen = "passkey-login",
            branding = null,
            hostedUrl = "https://example.com/hosted",
            finalizeUrl = null,
            forms =
                listOf(
                    FormWidget(
                        id = "passkey",
                        widgets =
                            listOf(
                                PasskeyLoginWidget(
                                    id = "passkey.credential",
                                    label = "Sign in with passkey",
                                    render = null,
                                    assertionOptions = fakeAssertionOptions,
                                ),
                            ),
                    ),
                ),
            layout = null,
            messages = null,
        )

    private fun finalizeScreen() =
        Screen(
            screen = null,
            branding = null,
            hostedUrl = null,
            finalizeUrl = "https://example.com/finalize",
            forms = null,
            layout = null,
            messages = null,
        )

    // Realistic enough JSON shapes to satisfy Json.parseToJsonElement in the controller
    private val fakeRegistrationJson =
        """{"id":"abc","rawId":"abc","type":"public-key","response":{"clientDataJSON":"","attestationObject":""}}"""
    private val fakeAuthenticationJson =
        """{"id":"abc","rawId":"abc","type":"public-key","response":{"clientDataJSON":"","authenticatorData":"","signature":"","userHandle":""}}"""

    // endregion

    // region Scenario 1 – passkeyEnroll happy path

    @Test
    fun passkeyEnroll_shouldPerformEnrollment_andStoreRegistrationResponse() {
        val createResponse =
            CreatePublicKeyCredentialResponse(
                registrationResponseJson = fakeRegistrationJson,
            )
        val controller = buildEnrollController(activity) { _, _ -> createResponse }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyEnrollScreen())
            whenever(mockLoginHandlerService.submitForm(any(), any())).thenReturn(finalizeScreen())

            controller.initialize()
            controller.submit("passkeyEnroll", body = mapOf("target" to "TestDevice"))
        }

        val storedValue =
            controller
                .stateForWidget(
                    "passkeyEnroll",
                    "passkeyEnroll.credential",
                    Json.parseToJsonElement("{}"),
                ).value
        assertNotNull(
            "Expected registration response to be stored in widget state",
            storedValue,
        )

        val expectedCredential = Json.parseToJsonElement(fakeRegistrationJson)
        val expectedPayload =
            mapOf(
                "passkeyEnroll" to mapOf("credential" to expectedCredential),
                "target" to "TestDevice",
            )
        runBlocking {
            verify(mockLoginHandlerService).submitForm(
                eq("passkeyEnroll"),
                eq(expectedPayload),
            )
        }
    }

    @Test
    fun passkeyEnroll_shouldRecoverAndEnrollSuccessfully_whenPromptIsCancelledOnFirstAttempt() {
        var callCount = 0
        val createResponse =
            CreatePublicKeyCredentialResponse(
                registrationResponseJson = fakeRegistrationJson,
            )
        // First call simulates the user dismissing the system prompt; second call succeeds.
        val controller =
            buildEnrollController(activity) { _, _ ->
                callCount++
                if (callCount == 1) throw CreateCredentialCancellationException()
                createResponse
            }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyEnrollScreen())
            whenever(mockLoginHandlerService.submitForm(any(), any())).thenReturn(finalizeScreen())

            controller.initialize()

            // First attempt – user cancels the system prompt.
            try {
                controller.submit("passkeyEnroll")
                fail("Expected PlatformError on first attempt")
            } catch (e: PlatformError) {
                // The PlatformError must wrap the original cancellation exception so that
                // callers can distinguish a user-initiated dismissal from other failures.
                assertTrue(
                    "Expected PlatformError.cause to be CreateCredentialCancellationException",
                    e.cause is CreateCredentialCancellationException,
                )
            }

            // Internal state should be unaffected: the enroll screen must still be active.
            assertNotNull(
                "Screen should still be the enroll screen after a cancelled prompt",
                controller.screen.value,
            )

            // Second attempt – user successfully completes enrollment.
            controller.submit("passkeyEnroll", body = mapOf("target" to "TestDevice"))
        }

        // The registration response must be stored in widget state after the successful attempt.
        val storedValue =
            controller
                .stateForWidget(
                    "passkeyEnroll",
                    "passkeyEnroll.credential",
                    Json.parseToJsonElement("{}"),
                ).value
        assertNotNull(
            "Expected registration response to be stored in widget state after retry",
            storedValue,
        )

        val expectedCredential = Json.parseToJsonElement(fakeRegistrationJson)
        val expectedPayload =
            mapOf(
                "passkeyEnroll" to mapOf("credential" to expectedCredential),
                "target" to "TestDevice",
            )
        runBlocking {
            // submitForm must be called exactly once – only from the successful second attempt.
            verify(mockLoginHandlerService).submitForm(
                eq("passkeyEnroll"),
                eq(expectedPayload),
            )
            // After submission the controller must have forwarded the finalize URL.
            verify(mockNativeSDK).continueFlow(eq("https://example.com/finalize"))
        }
    }

    // endregion

    // region Scenario 2 – passkey assertion happy path

    @Test
    fun passkeyAssertion_shouldPerformAssertion_andStoreAuthenticationResponse() {
        val getResponse =
            GetCredentialResponse(
                credential =
                    PublicKeyCredential(
                        authenticationResponseJson = fakeAuthenticationJson,
                    ),
            )
        val controller = buildAssertController(activity) { _, _ -> getResponse }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyLoginScreen())
            whenever(mockLoginHandlerService.submitForm(any(), any())).thenReturn(finalizeScreen())

            controller.initialize()
            controller.submit("passkey")
        }

        val storedValue =
            controller
                .stateForWidget("passkey", "passkey.credential", Json.parseToJsonElement("{}"))
                .value
        assertNotNull(
            "Expected authentication response to be stored in widget state",
            storedValue,
        )
    }

    // endregion

    // region Scenario 3 – exceptions are wrapped and re-thrown as PlatformError

    @Test
    fun passkeyEnroll_shouldThrowPlatformError_whenCreateCredentialThrows() {
        val controller =
            buildEnrollController(activity) { _, _ ->
                throw CreateCredentialCancellationException()
            }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyEnrollScreen())
            controller.initialize()

            try {
                controller.submit("passkeyEnroll")
                fail("Expected PlatformError to be thrown")
            } catch (_: PlatformError) {
                // expected
            }
        }
    }

    @Test
    fun passkeyAssertion_shouldThrowPlatformError_whenGetCredentialThrows() {
        val controller =
            buildAssertController(activity) { _, _ ->
                throw GetCredentialCancellationException()
            }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyLoginScreen())
            controller.initialize()

            try {
                controller.submit("passkey")
                fail("Expected PlatformError to be thrown")
            } catch (_: PlatformError) {
                // expected
            }
        }
    }

    // endregion

    // region Scenario 4 – non-main thread invocation logs a warning

    @Test
    fun passkeyEnroll_shouldLogNonMainThreadWarning_whenInvokedFromIODispatcher() {
        val createResponse =
            CreatePublicKeyCredentialResponse(
                registrationResponseJson = fakeRegistrationJson,
            )
        val controller = buildEnrollController(activity) { _, _ -> createResponse }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyEnrollScreen())
            whenever(mockLoginHandlerService.submitForm(any(), any())).thenReturn(finalizeScreen())

            controller.initialize()

            withContext(Dispatchers.IO) {
                controller.submit("passkeyEnroll")
            }
        }

        assertTrue(
            "Expected non-main-thread warning to appear in logs",
            fakeLogging.logMessages.contains("should be invoked from Main thread"),
        )
    }

    @Test
    fun passkeyAssertion_shouldLogNonMainThreadWarning_whenInvokedFromIODispatcher() {
        val getResponse =
            GetCredentialResponse(
                credential =
                    PublicKeyCredential(
                        authenticationResponseJson = fakeAuthenticationJson,
                    ),
            )
        val controller = buildAssertController(activity) { _, _ -> getResponse }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyLoginScreen())
            whenever(mockLoginHandlerService.submitForm(any(), any())).thenReturn(finalizeScreen())

            controller.initialize()

            withContext(Dispatchers.IO) {
                controller.submit("passkey")
            }
        }

        assertTrue(
            "Expected non-main-thread warning to appear in logs",
            fakeLogging.logMessages.contains("should be invoked from Main thread"),
        )
    }

    // endregion

    // region Scenario 5 – null context throws IllegalArgumentException

    @Test
    fun passkeyEnroll_shouldThrowIllegalArgumentException_whenContextIsNull() {
        val controller =
            buildEnrollController(
                activityContext = null,
            ) { _, _ -> throw UnsupportedOperationException() }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyEnrollScreen())
            controller.initialize()

            try {
                controller.submit("passkeyEnroll")
                fail("Expected IllegalArgumentException to be thrown")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun passkeyAssertion_shouldThrowIllegalArgumentException_whenContextIsNull() {
        val controller =
            buildAssertController(
                activityContext = null,
            ) { _, _ -> throw UnsupportedOperationException() }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyLoginScreen())
            controller.initialize()

            try {
                controller.submit("passkey")
                fail("Expected IllegalArgumentException to be thrown")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }

    // endregion
}
