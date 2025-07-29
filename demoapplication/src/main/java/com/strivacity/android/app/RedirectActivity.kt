package com.strivacity.android.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class RedirectActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val data = intent.data

    val intent = Intent(this, MainActivity::class.java)
    intent.setData(data)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    startActivity(intent)

    finish()
  }
}
