package com.strivacity.android.native_sdk

interface Storage {
  fun set(key: String, value: String): Boolean

  fun get(key: String): String?

  fun delete(key: String)
}
