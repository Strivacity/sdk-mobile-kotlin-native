package com.strivacity.android.app

import android.content.SharedPreferences
import androidx.core.content.edit
import com.strivacity.android.native_sdk.Storage

class SharedPreferenceStorage(private val sharedPreferences: SharedPreferences) : Storage {
  override fun set(key: String, value: String): Boolean {
    sharedPreferences.edit { putString(key, value) }
    return true
  }

  override fun get(key: String): String? {
    return sharedPreferences.getString(key, null)
  }

  override fun delete(key: String) {
    sharedPreferences.edit { remove(key) }
  }
}
