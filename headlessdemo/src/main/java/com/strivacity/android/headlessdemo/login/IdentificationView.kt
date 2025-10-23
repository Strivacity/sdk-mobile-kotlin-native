package com.strivacity.android.headlessdemo.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.render.models.Screen
import kotlinx.coroutines.launch

@Composable
fun IdentificationView(screen: Screen, headlessAdapter: HeadlessAdapter) {
  val messages by headlessAdapter.messages().collectAsState()

  val coroutineScope = rememberCoroutineScope()

  var identifier by remember { mutableStateOf("") }

  Column {
    Text("Sign in")

    TextField(
        value = identifier, onValueChange = { identifier = it }, label = { Text("Email address") })

    val errorMessage = messages?.errorMessageForWidget("identifier", "identifier")
    if (errorMessage != null) {
      Text(errorMessage, color = Color.Red)
    }

    Button(
        onClick = {
          coroutineScope.launch {
            headlessAdapter.submit("identifier", mapOf("identifier" to identifier))
          }
        }) {
          Text("Continue")
        }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Don't have an account?")
      TextButton(
          onClick = {
            coroutineScope.launch {
              headlessAdapter.submit("additionalActions/registration", mapOf())
            }
          }) {
            Text("Sign up")
          }
    }
  }
}
