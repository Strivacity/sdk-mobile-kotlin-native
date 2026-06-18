package com.strivacity.android.native_sdk.render

import FakeLogging
import android.app.Activity
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.PlatformError
import com.strivacity.android.native_sdk.UnsupportedFeatureError
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.buildAssertController
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.buildEnrollController
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.fakeAuthenticationJson
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.fakeRegistrationJson
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.finalizeScreen
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.nonPasskeyScreen
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.passkeyEnrollScreen
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.passkeyEnrollScreenWithoutHostedUrl
import com.strivacity.android.native_sdk.render.PasskeyTestFixtures.passkeyLoginScreen
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.util.FeatureDetection
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
import org.mockito.kotlin.never
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

    private fun buildEnrollController(
        activityContext: Activity?,
        featureDetection: FeatureDetection = PasskeyTestFixtures.PasskeySupportedFeatureDetection(),
        enroller: suspend (
            Activity,
            androidx.credentials.CreatePublicKeyCredentialRequest,
        ) -> CreatePublicKeyCredentialResponse,
    ) = buildEnrollController(
        mockNativeSDK,
        mockLoginHandlerService,
        fakeLogging,
        activityContext,
        featureDetection,
        enroller,
    )

    private fun buildAssertController(
        activityContext: Activity?,
        featureDetection: FeatureDetection = PasskeyTestFixtures.PasskeySupportedFeatureDetection(),
        asserter: suspend (Activity, androidx.credentials.GetCredentialRequest) -> GetCredentialResponse,
    ) = buildAssertController(
        mockNativeSDK,
        mockLoginHandlerService,
        fakeLogging,
        activityContext,
        featureDetection,
        asserter,
    )

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

    // region Scenario 6 – unsupported device behavior

    @Test
    fun initialize_shouldThrowUnsupportedFeatureError_whenPasskeyUnsupportedAndNoHostedUrl() {
        val controller =
            buildEnrollController(
                activityContext = activity,
                featureDetection = PasskeyTestFixtures.PasskeyUnsupportedFeatureDetection(),
            ) { _, _ -> throw UnsupportedOperationException() }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyEnrollScreenWithoutHostedUrl())

            try {
                controller.initialize()
                fail("Expected UnsupportedFeatureError to be thrown")
            } catch (e: UnsupportedFeatureError) {
                assertTrue(
                    "Error message should mention Passkey/WebAuthn",
                    e.message?.contains("Passkey/WebAuthn") == true,
                )
            }
        }

        runBlocking {
            // Should not attempt to finalize or continue flow
            verify(mockNativeSDK, never()).continueFlow(any())
        }
    }

    @Test
    fun initialize_shouldTriggerFallback_whenPasskeyUnsupportedAndHostedUrlAvailable() {
        var fallbackTriggered = false
        var fallbackUrl: String? = null

        val controller =
            LoginController(
                nativeSDK = mockNativeSDK,
                loginHandlerService = mockLoginHandlerService,
                oidcParams =
                    com.strivacity.android.native_sdk.service.OidcParams(
                        onSuccess = {},
                        onError = {},
                        shouldVerifyIdTokenClaims = true,
                    ),
                fallbackHandler = { uri ->
                    fallbackTriggered = true
                    fallbackUrl = uri
                },
                logging = fakeLogging,
                credentialManagerProvider =
                    PasskeyTestFixtures.FakeCredentialManagerProvider(
                        activityContext = activity,
                    ),
                featureDetection = PasskeyTestFixtures.PasskeyUnsupportedFeatureDetection(),
            )

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyEnrollScreen())

            controller.initialize()
        }

        assertTrue("Fallback should have been triggered", fallbackTriggered)
        assertTrue(
            "Fallback URL should match hostedUrl",
            fallbackUrl == "https://example.com/hosted",
        )
        assertTrue(
            "Warning about unsupported feature should appear in logs",
            fakeLogging.logMessages.contains("Passkey/WebAuthn Widget"),
        )
    }

    @Test
    fun initialize_shouldSucceed_whenPasskeyUnsupportedButNoPasskeyForms() {
        val controller =
            buildEnrollController(
                activityContext = activity,
                featureDetection = PasskeyTestFixtures.PasskeyUnsupportedFeatureDetection(),
            ) { _, _ -> throw UnsupportedOperationException() }

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(nonPasskeyScreen())

            // Should not throw
            controller.initialize()
        }

        assertNotNull("Screen should be loaded successfully", controller.screen.value)
    }

    @Test
    @Config(sdk = [28])
    fun initialize_shouldTriggerFallback_whenRunningOnApi28WithPasskeyForm() {
        // On API 28, FeatureDetection should naturally return false
        var fallbackTriggered = false

        val controller =
            LoginController(
                nativeSDK = mockNativeSDK,
                loginHandlerService = mockLoginHandlerService,
                oidcParams =
                    com.strivacity.android.native_sdk.service.OidcParams(
                        onSuccess = {},
                        onError = {},
                        shouldVerifyIdTokenClaims = true,
                    ),
                fallbackHandler = { fallbackTriggered = true },
                logging = fakeLogging,
                credentialManagerProvider =
                    PasskeyTestFixtures.FakeCredentialManagerProvider(
                        activityContext = activity,
                    ),
            )

        runBlocking {
            whenever(mockLoginHandlerService.initCall()).thenReturn(passkeyLoginScreen())

            controller.initialize()
        }

        assertTrue(
            "Fallback should be triggered on API 28 with passkey form",
            fallbackTriggered,
        )
    }

    // endregion
}
