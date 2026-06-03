package com.strivacity.android.native_sdk.compat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.strivacity.android.native_sdk.DefaultLogging
import com.strivacity.android.native_sdk.Error
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.HeadlessAdapterDelegate
import com.strivacity.android.native_sdk.Logging
import com.strivacity.android.native_sdk.LoginParameters
import com.strivacity.android.native_sdk.NativeSDK
import com.strivacity.android.native_sdk.NetworkConfiguration
import com.strivacity.android.native_sdk.SdkMode
import com.strivacity.android.native_sdk.Storage
import com.strivacity.android.native_sdk.render.FallbackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/** Callback invoked when the native flow cannot continue and the hosted login page should be opened. */
fun interface FallbackHandlerCompat {
    fun onFallback(uriToLoad: String)
}

/** Callback invoked when a login or entry flow completes successfully. */
fun interface NativeSDKOnSuccess {
    fun onSuccess()
}

/** Callback invoked when a login or entry flow fails. */
fun interface NativeSDKOnError {
    fun onError(error: Error)
}

private fun FallbackHandlerCompat.toFallbackHandler(): FallbackHandler = { uri -> onFallback(uri) }

class NativeSDKJava private constructor(
    private val delegate: NativeSDK,
    private val dispatcher: CoroutineContext = Dispatchers.Default,
) {
    /** Emits `true` while an active login flow is in progress, `false` otherwise. */
    val loginInProgressLiveData: LiveData<Boolean> = delegate.session.loginInProgress.asLiveData()

    /** Emits the current [com.strivacity.android.native_sdk.Profile], or `null` when signed out. */
    val profileLiveData = delegate.session.profile.asLiveData()

    private val activeSetupJob = AtomicReference<Job?>(null)

    /**
     * Creates a [HeadlessAdapterJava] bound to this SDK instance.
     *
     * Must be called after a login flow has started (i.e. after [loginInProgressLiveData] emits
     * `true`). The returned adapter drives the headless UI by forwarding screen events to the
     * provided [adapterDelegate].
     */
    fun createHeadlessAdapter(adapterDelegate: HeadlessAdapterDelegate): HeadlessAdapterJava =
        HeadlessAdapterJava(delegate, adapterDelegate)

    /**
     * Restores a previously persisted session from [Storage] and refreshes tokens if needed.
     *
     * Call this on app start before checking [isAuthenticated] or [getAccessToken].
     */
    fun initializeSession(): CompletableFuture<Void> =
        launch { delegate.initializeSession() }.thenApply { null }

    /**
     * Starts the login flow.
     *
     * The SDK drives the headless UI via [HeadlessAdapterJava] callbacks. If the native flow cannot
     * continue, [fallbackHandler] is invoked with a URL to open in a browser. Provide a [context]
     * to enable Passkey authentication.
     */
    @JvmOverloads
    fun login(
        fallbackHandler: FallbackHandlerCompat,
        onSuccess: NativeSDKOnSuccess,
        onError: NativeSDKOnError,
        loginParameters: LoginParameters? = null,
        context: Context? = null,
    ) {
        val job =
            CoroutineScope(dispatcher).launch {
                delegate.login(
                    fallbackHandler = fallbackHandler.toFallbackHandler(),
                    onSuccess = { onSuccess.onSuccess() },
                    onError = { err -> onError.onError(err) },
                    loginParameters = loginParameters,
                    context = context,
                )
            }
        activeSetupJob.set(job)
        job.invokeOnCompletion { activeSetupJob.compareAndSet(job, null) }
    }

    /**
     * Resumes an existing flow from a deep-link entry URI.
     *
     * Use this when the app is launched via a challenge URI (e.g. from an email link). Behaves like
     * [login] once the session is established. Provide a [context] to enable Passkey authentication.
     */
    fun entry(
        uri: Uri?,
        fallbackHandler: FallbackHandlerCompat,
        onSuccess: NativeSDKOnSuccess,
        onError: NativeSDKOnError,
        context: Context? = null,
    ) {
        val job =
            CoroutineScope(dispatcher).launch {
                delegate.entry(
                    uri = uri,
                    fallbackHandler = fallbackHandler.toFallbackHandler(),
                    onSuccess = { onSuccess.onSuccess() },
                    onError = { err -> onError.onError(err) },
                    context = context,
                )
            }
        activeSetupJob.set(job)
        job.invokeOnCompletion { activeSetupJob.compareAndSet(job, null) }
    }

    /**
     * Returns `true` if the user has a valid (or successfully refreshed) session.
     *
     * Triggers a token refresh if the access token is expired or about to expire.
     */
    fun isAuthenticated(): CompletableFuture<Boolean> = launch { delegate.isAuthenticated() }

    /**
     * Returns the current access token, refreshing it first if expired.
     *
     * Returns `null` if the user is not authenticated.
     */
    fun getAccessToken(): CompletableFuture<String?> = launch { delegate.getAccessToken() }

    /** Returns `true` if the SDK is waiting for a redirect callback URI via [continueFlow]. */
    fun isRedirectExpected(): Boolean = delegate.isRedirectExpected()

    /**
     * Delivers the redirect callback URI to the SDK after the hosted flow completes.
     *
     * Pass `null` to signal that the user cancelled the hosted flow.
     */
    fun continueFlow(uri: String?): CompletableFuture<Void> =
        launch { delegate.continueFlow(uri) }.thenApply { null }

    /**
     * Cancels the active login flow and releases its resources.
     *
     * Optionally propagates an [Error] to the original [NativeSDKOnError] callback. If no flow is
     * active this is a no-op.
     */
    @JvmOverloads
    fun cancelFlow(error: Error? = null) {
        activeSetupJob.getAndSet(null)?.cancel()
        delegate.cancelFlow(error)
    }

    /**
     * Signs the user out by clearing the local session and calling the OIDC end-session endpoint.
     *
     * The future resolves once the logout request completes. Local session data is always cleared
     * even if the remote call fails.
     */
    fun logout(): CompletableFuture<Void> = launch { delegate.logout() }.thenApply { null }

    /**
     * Revokes the active refresh or access token and clears the local session.
     *
     * Prefer this over [logout] when you need to invalidate the token server-side without triggering
     * an OIDC end-session redirect.
     */
    fun revoke(): CompletableFuture<Void> = launch { delegate.revoke() }.thenApply { null }

    private fun <R> launch(block: suspend CoroutineScope.() -> R): CompletableFuture<R> =
        CoroutineScope(dispatcher).future(block = block)

    /**
     * Fluent builder for [NativeSDKJava].
     *
     * The five required parameters must be supplied at construction time. Optional parameters can
     * be customised via the fluent setters before calling [build]. Calling [build] multiple times
     * produces independent [NativeSDKJava] instances.
     */
    class Builder(
        private val issuer: String,
        private val clientId: String,
        private val redirectURI: String,
        private val postLogoutURI: String,
        private val storage: Storage,
    ) {
        private var mode: SdkMode = SdkMode.Android
        private var logging: Logging = DefaultLogging()
        private var networkConfiguration: NetworkConfiguration = NetworkConfiguration()

        /** Sets the [SdkMode] used when communicating with the Strivacity backend.
         * Defaults to [SdkMode.Android].
         */
        fun mode(mode: SdkMode): Builder = apply { this.mode = mode }

        /** Replaces the default [DefaultLogging] with a custom [Logging] implementation.
         * Useful for routing SDK log output to an existing logging framework.
         */
        fun logging(logging: Logging): Builder = apply { this.logging = logging }

        /** Overrides the [NetworkConfiguration] used for all HTTP requests made by the SDK,
         * including user-agent and custom `x-sty-*` request headers.
         */
        fun networkConfiguration(config: NetworkConfiguration): Builder =
            apply {
                this.networkConfiguration = config
            }

        /** Constructs and returns the configured [NativeSDKJava] instance. */
        fun build(): NativeSDKJava =
            NativeSDKJava(
                NativeSDK(
                    issuer = issuer,
                    clientId = clientId,
                    redirectURI = redirectURI,
                    postLogoutURI = postLogoutURI,
                    storage = storage,
                    mode = mode,
                    logging = logging,
                    networkConfiguration = networkConfiguration,
                ),
            )
    }
}

/**
 * Java-friendly wrapper around [HeadlessAdapter].
 *
 * Exposes [isProcessing] as [LiveData] and submit as a callback-based [submit], avoiding
 * the need for coroutines in Java callers. Obtain an instance via
 * [NativeSDKJava.createHeadlessAdapter] rather than constructing directly.
 */
class HeadlessAdapterJava internal constructor(
    nativeSDK: NativeSDK,
    delegate: HeadlessAdapterDelegate,
    scope: CoroutineScope = MainScope(),
) : HeadlessAdapter(nativeSDK, delegate, scope) {
    /**
     * [LiveData] mirror of [isProcessing]. Emits `true` while a form submission or authentication
     * request is in flight, `false` once it settles.
     */
    val isProcessingLiveData: LiveData<Boolean>
        get() = isProcessing.asLiveData()

    /**
     * Submits a form action asynchronously and invokes [callback] on the calling thread pool when done.
     *
     * [callback] receives `null` on success or the thrown [Throwable] on failure. The caller is
     * responsible for updating UI state in response.
     */
    fun submit(
        formId: String,
        body: Map<String, Any>,
        callback: SubmitCallback,
    ) {
        scope.launch {
            try {
                loginController.submit(formId, body)
                callback.onSubmit(null)
            } catch (ex: Throwable) {
                callback.onSubmit(ex)
            }
        }
    }

    /** Callback interface for [submit] results. */
    fun interface SubmitCallback {
        fun onSubmit(throwable: Throwable?)
    }
}
