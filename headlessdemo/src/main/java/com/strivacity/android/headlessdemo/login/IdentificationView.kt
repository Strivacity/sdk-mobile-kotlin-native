package com.strivacity.android.headlessdemo.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.headlessdemo.ui.theme.StrivacitySecondary
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.render.models.PasskeyLoginWidget
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.SubmitWidget
import kotlinx.coroutines.launch

@Composable
fun IdentificationView(screen: Screen, headlessAdapter: HeadlessAdapter) {
  val messages by headlessAdapter.messages().collectAsState()

  val coroutineScope = rememberCoroutineScope()

  var identifier by remember { mutableStateOf("") }

  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(35.dp)) {
        Text("Sign in", fontSize = 24.sp, fontWeight = FontWeight.W600)

        TextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth())

        val errorMessage = messages?.errorMessageForWidget("identifier", "identifier")
        if (errorMessage != null) {
          Text(errorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrivacityPrimary),
            onClick = {
              coroutineScope.launch {
                headlessAdapter.submit("identifier", mapOf("identifier" to identifier))
              }
            }) {
              Text("Continue")
            }

        Text("OR")

        val externalLogins = screen.forms?.filter { it.id.startsWith("externalLoginProvider") }
        externalLogins?.forEach {
          it.let {
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = StrivacitySecondary, contentColor = Color.Black),
                onClick = { coroutineScope.launch { headlessAdapter.submit(it.id, mapOf()) } }) {
                  Text((it.widgets[0] as SubmitWidget).label)
                }
          }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center) {
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
