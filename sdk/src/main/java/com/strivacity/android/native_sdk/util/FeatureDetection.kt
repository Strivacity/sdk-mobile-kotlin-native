package com.strivacity.android.native_sdk.util

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

interface FeatureDetection {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun isPasskeySupported(): Boolean

    companion object {
        val instance: FeatureDetection = DefaultFeatureDetection()
    }
}

internal class DefaultFeatureDetection : FeatureDetection {
    @SuppressLint("AnnotateVersionCheck")
    override fun isPasskeySupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}
