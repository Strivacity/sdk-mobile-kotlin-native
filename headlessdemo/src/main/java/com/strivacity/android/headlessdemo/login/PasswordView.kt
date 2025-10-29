package com.strivacity.android.headlessdemo.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.StaticWidget
import kotlinx.coroutines.launch

@Composable
fun PasswordView(screen: Screen, headlessAdapter: HeadlessAdapter) {
  val messages by headlessAdapter.messages().collectAsState()

  val coroutineScope = rememberCoroutineScope()

  var password by remember { mutableStateOf("") }
  var keepMeLoggedIn by remember {
    mutableStateOf(
        screen.forms
            ?.find { it.id == "password" }
            ?.widgets
            ?.find { it.id == "keepMeLoggedIn" }
            ?.value() as Boolean? ?: false)
  }

  val identifierWidget =
      screen.forms?.find { it.id == "reset" }?.widgets?.find { it.id == "identifier" }

  val identifier =
      when (identifierWidget) {
        is StaticWidget -> {
          identifierWidget.value
        }
        else -> ""
      }

  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(35.dp)) {
        Text("Enter password", fontSize = 24.sp, fontWeight = FontWeight.W600)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center) {
              Text(identifier)
              TextButton(
                  onClick = {
                    coroutineScope.launch { headlessAdapter.submit("reset", mapOf()) }
                  }) {
                    Text("Not you?")
                  }
            }

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Enter your password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth())
        val errorMessage = messages?.errorMessageForWidget("password", "password")
        if (errorMessage != null) {
          Text(errorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically) {
              Checkbox(keepMeLoggedIn, { keepMeLoggedIn = it })
              Text("Keep me logged in")
            }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrivacityPrimary),
            onClick = {
              coroutineScope.launch {
                headlessAdapter.submit(
                    "password", mapOf("password" to password, "keepMeLoggedIn" to keepMeLoggedIn))
              }
            }) {
              Text("Continue")
            }

        TextButton(
            onClick = { coroutineScope.launch { headlessAdapter.submit("reset", mapOf()) } }) {
              Text("Back to login")
            }
      }
}
