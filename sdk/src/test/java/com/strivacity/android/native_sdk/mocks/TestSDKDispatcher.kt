package com.strivacity.android.native_sdk.mocks

import com.strivacity.android.native_sdk.SDKDispatchers
import kotlinx.coroutines.CoroutineDispatcher

class TestSDKDispatcher(val testDispatcher: CoroutineDispatcher) : SDKDispatchers {
  override val IO: CoroutineDispatcher
    get() = testDispatcher

  override val Default: CoroutineDispatcher
    get() = testDispatcher

  override val Main: CoroutineDispatcher
    get() = testDispatcher
}
