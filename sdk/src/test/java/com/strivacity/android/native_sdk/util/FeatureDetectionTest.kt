package com.strivacity.android.native_sdk.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FeatureDetectionTest {
    @Test
    @Config(sdk = [28])
    fun isPasskeySupported_shouldReturnFalse_onApi28() {
        val featureDetection = DefaultFeatureDetection()
        assertFalse(
            "Passkey support should be false on API 28 (below Q)",
            featureDetection.isPasskeySupported(),
        )
    }

    @Test
    @Config(sdk = [29])
    fun isPasskeySupported_shouldReturnTrue_onApi29() {
        val featureDetection = DefaultFeatureDetection()
        assertTrue(
            "Passkey support should be true on API 29 (Q)",
            featureDetection.isPasskeySupported(),
        )
    }

    @Test
    @Config(sdk = [30])
    fun isPasskeySupported_shouldReturnTrue_onApi30() {
        val featureDetection = DefaultFeatureDetection()
        assertTrue(
            "Passkey support should be true on API 30",
            featureDetection.isPasskeySupported(),
        )
    }

    @Test
    @Config(sdk = [34])
    fun isPasskeySupported_shouldReturnTrue_onApi34AndAbove() {
        val featureDetection = DefaultFeatureDetection()
        assertTrue(
            "Passkey support should be true on API 34+ (Upside Down Cake and above)",
            featureDetection.isPasskeySupported(),
        )
    }
}
