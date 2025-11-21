package com.strivacity.android.native_sdk

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface SDKDispatchers {
  val IO: CoroutineDispatcher
  val Default: CoroutineDispatcher
  val Main: CoroutineDispatcher
}

object DefaultSDKDispatchers : SDKDispatchers {
  override val IO: CoroutineDispatcher
    get() = Dispatchers.IO

  override val Default: CoroutineDispatcher
    get() = Dispatchers.Default

  override val Main: CoroutineDispatcher
    get() = Dispatchers.Main
}
