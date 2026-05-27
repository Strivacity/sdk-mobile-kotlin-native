package com.strivacity.android.native_sdk.javademo.service;

import android.content.SharedPreferences;
import com.strivacity.android.native_sdk.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SharedPreferenceStorage implements Storage {

    private final SharedPreferences sharedPreferences;

    public SharedPreferenceStorage(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public boolean set(@NotNull String key, @NotNull String value) {
        return sharedPreferences.edit().putString(key, value).commit();
    }

    @Override
    public @Nullable String get(@NotNull String key) {
        return sharedPreferences.getString(key, null);
    }

    @Override
    public void delete(@NotNull String key) {
        sharedPreferences.edit().remove(key).apply();
    }
}
