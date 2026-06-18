package com.strivacity.android.native_sdk.render

import FakeLogging
import android.app.Activity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.render.models.AssertionOptions
import com.strivacity.android.native_sdk.render.models.EnrollOptions
import com.strivacity.android.native_sdk.render.models.FormWidget
import com.strivacity.android.native_sdk.render.models.PasskeyEnrollWidget
import com.strivacity.android.native_sdk.render.models.PasskeyLoginWidget
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.service.LoginHandlerService
import com.strivacity.android.native_sdk.service.OidcParams
import com.strivacity.android.native_sdk.util.FeatureDetection

/**
 * Shared test fixtures and helpers for passkey-related tests.
 */
internal object PasskeyTestFixtures {
    /**
     * Test implementation that always returns true for passkey support.
     */
    class PasskeySupportedFeatureDetection : FeatureDetection {
        override fun isPasskeySupported(): Boolean = true
    }

    /**
     * Test implementation that always returns false for passkey support.
     */
    class PasskeyUnsupportedFeatureDetection : FeatureDetection {
        override fun isPasskeySupported(): Boolean = false
    }

    /**
     * Minimal [CredentialManagerProvider] stub that allows tests to control the activity context
     * and delegate credential operations to simple lambdas.
     */
    internal class FakeCredentialManagerProvider(
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

    internal val fakeEnrollOptions =
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

    internal val fakeAssertionOptions =
        AssertionOptions(
            allowCredentials = emptyList(),
            challenge = "Y2hhbGxlbmdl",
            rpId = "example.com",
            userVerification = "required",
            timeout = null,
        )

    // Realistic enough JSON shapes to satisfy Json.parseToJsonElement in the controller
    @Suppress("ktlint:standard:property-naming")
    internal const val fakeRegistrationJson =
        """{"id":"abc","rawId":"abc","type":"public-key","response":{"clientDataJSON":"","attestationObject":""}}"""

    @Suppress("ktlint:standard:property-naming")
    internal const val fakeAuthenticationJson =
        """{"id":"abc","rawId":"abc","type":"public-key","response":{"clientDataJSON":"","authenticatorData":"","signature":"","userHandle":""}}"""

    internal fun passkeyEnrollScreen() =
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

    internal fun passkeyLoginScreen() =
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

    internal fun passkeyEnrollScreenWithoutHostedUrl() = passkeyEnrollScreen().copy(hostedUrl = null)

    internal fun passkeyLoginScreenWithoutHostedUrl() = passkeyLoginScreen().copy(hostedUrl = null)

    internal fun nonPasskeyScreen() =
        Screen(
            screen = "username-password",
            branding = null,
            hostedUrl = null,
            finalizeUrl = null,
            forms =
                listOf(
                    FormWidget(
                        id = "login",
                        widgets =
                            listOf(
                                com.strivacity.android.native_sdk.render.models.InputWidget(
                                    id = "username",
                                    label = "Username",
                                    value = null,
                                    readonly = false,
                                    autocomplete = null,
                                    inputmode = null,
                                    validator =
                                        com.strivacity.android.native_sdk.render.models.InputWidget.Validator(
                                            minLength = null,
                                            maxLength = null,
                                            regex = null,
                                            required = false,
                                        ),
                                    render = null,
                                ),
                            ),
                    ),
                ),
            layout = null,
            messages = null,
        )

    internal fun finalizeScreen() =
        Screen(
            screen = null,
            branding = null,
            hostedUrl = null,
            finalizeUrl = "https://example.com/finalize",
            forms = null,
            layout = null,
            messages = null,
        )

    internal fun buildEnrollController(
        mockNativeSDK: NativeSDK,
        mockLoginHandlerService: LoginHandlerService,
        fakeLogging: FakeLogging,
        activityContext: Activity?,
        featureDetection: FeatureDetection = PasskeySupportedFeatureDetection(),
        enroller: suspend (
            Activity,
            CreatePublicKeyCredentialRequest,
        ) -> CreatePublicKeyCredentialResponse,
    ): LoginController =
        LoginController(
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
            featureDetection = featureDetection,
        )

    internal fun buildAssertController(
        mockNativeSDK: NativeSDK,
        mockLoginHandlerService: LoginHandlerService,
        fakeLogging: FakeLogging,
        activityContext: Activity?,
        featureDetection: FeatureDetection = PasskeySupportedFeatureDetection(),
        asserter: suspend (Activity, GetCredentialRequest) -> GetCredentialResponse,
    ): LoginController =
        LoginController(
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
            featureDetection = featureDetection,
        )
}
