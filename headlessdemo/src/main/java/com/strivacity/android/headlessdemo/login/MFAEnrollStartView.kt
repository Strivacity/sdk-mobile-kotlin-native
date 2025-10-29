package com.strivacity.android.headlessdemo.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.StaticWidget
import kotlinx.coroutines.launch

@Composable
fun MFAEnrollStartView(screen: Screen, headlessAdapter: HeadlessAdapter) {
  val messages by headlessAdapter.messages().collectAsState()

  val coroutineScope = rememberCoroutineScope()

  val identifierWidget =
      screen.forms?.find { it.id == "reset" }?.widgets?.find { it.id == "identifier" }

  val identifier =
      when (identifierWidget) {
        is StaticWidget -> {
          identifierWidget.value
        }
        else -> ""
      }

  var email by remember { mutableStateOf(false) }
  var phone by remember { mutableStateOf(false) }

  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(35.dp)) {
        Text(
            "Enroll the following authentication methods",
            fontSize = 24.sp,
            fontWeight = FontWeight.W600)

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

        Text("Choose at least one method", modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Checkbox(email, onCheckedChange = { email = it })
          Text("Email address")
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Checkbox(phone, onCheckedChange = { phone = it })
          Text("Phone number")
        }
        val errorMessage = messages?.errorMessageForWidget("mfaEnrollStart", "optional")
        if (errorMessage != null) {
          Text(errorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrivacityPrimary),
            onClick = {
              coroutineScope.launch {
                val optionals = mutableListOf<String>()
                if (email) {
                  optionals.add("email")
                }
                if (phone) {
                  optionals.add("phone")
                }

                headlessAdapter.submit("mfaEnrollStart", mapOf("optional" to optionals))
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
