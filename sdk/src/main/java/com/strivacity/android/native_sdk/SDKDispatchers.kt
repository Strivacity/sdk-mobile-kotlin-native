package com.strivacity.android.native_sdk

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Provides coroutine dispatchers for different types of operations.
 *
 * By default, the SDK uses [DefaultSDKDispatchers], which maps to standard Kotlin coroutines
 * dispatchers.
 */
interface SDKDispatchers {
    @Suppress("ktlint:standard:property-naming")
    val IO: CoroutineDispatcher

    @Suppress("ktlint:standard:property-naming")
    val Default: CoroutineDispatcher

    @Suppress("ktlint:standard:property-naming")
    val Main: CoroutineDispatcher
}

/** Default implementation of [SDKDispatchers] using standard Kotlin coroutines dispatchers. */
object DefaultSDKDispatchers : SDKDispatchers {
    override val IO: CoroutineDispatcher
        get() = Dispatchers.IO

    override val Default: CoroutineDispatcher
        get() = Dispatchers.Default

    override val Main: CoroutineDispatcher
        get() = Dispatchers.Main
}
