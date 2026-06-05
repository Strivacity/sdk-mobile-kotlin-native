package com.strivacity.android.native_sdk.render

import android.app.Activity
import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import java.lang.ref.WeakReference

/**
 * Provides credential operations (passkey enrollment and assertion) backed by the Android
 * [CredentialManager] API. Abstracting this behind an interface makes the operations testable
 * without a real device credential store.
 */
interface CredentialManagerProvider {
    /**
     * Returns the [Activity] context that credential operations must be invoked on, or `null` if
     * the context is unavailable or has been garbage-collected.
     */
    fun activityContext(): Activity?

    /** Enrolls a new passkey / WebAuthn credential. */
    suspend fun createCredential(
        context: Activity,
        request: CreatePublicKeyCredentialRequest,
    ): CreatePublicKeyCredentialResponse

    /** Asserts an existing passkey / WebAuthn credential. */
    suspend fun getCredential(
        context: Activity,
        request: GetCredentialRequest,
    ): GetCredentialResponse
}

/** Default production implementation that delegates to [CredentialManager]. */
class DefaultCredentialManagerProvider(
    private val context: WeakReference<Context>,
) : CredentialManagerProvider {
    override fun activityContext(): Activity? = context.get() as? Activity

    override suspend fun createCredential(
        context: Activity,
        request: CreatePublicKeyCredentialRequest,
    ): CreatePublicKeyCredentialResponse =
        CredentialManager.create(context).createCredential(context, request)
            as CreatePublicKeyCredentialResponse

    override suspend fun getCredential(
        context: Activity,
        request: GetCredentialRequest,
    ): GetCredentialResponse = CredentialManager.create(context).getCredential(context, request)
}
