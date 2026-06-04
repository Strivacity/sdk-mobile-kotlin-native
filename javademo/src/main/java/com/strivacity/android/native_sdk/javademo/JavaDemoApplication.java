package com.strivacity.android.native_sdk.javademo;

import android.app.Application;
import com.strivacity.android.native_sdk.SdkMode;
import com.strivacity.android.native_sdk.Storage;
import com.strivacity.android.native_sdk.compat.NativeSDKJava;
import com.strivacity.android.native_sdk.javademo.service.SharedPreferenceStorage;

public class JavaDemoApplication extends Application {
    private NativeSDKJava nativeSDK;

    @Override
    public void onCreate() {
        super.onCreate();

        Storage storage =
                new SharedPreferenceStorage(getSharedPreferences("session", MODE_PRIVATE));

        nativeSDK =
                new NativeSDKJava.Builder(
                                "https://example.org",
                                "",
                                "com.strivacity.android.nativesdk.javademo://oauth/login-callback",
                                "com.strivacity.android.nativesdk.javademo://oauth/logout-callback",
                                storage)
                        .mode(SdkMode.AndroidMinimal)
                        .build();
    }

    public NativeSDKJava getNativeSDK() {
        return nativeSDK;
    }
}
