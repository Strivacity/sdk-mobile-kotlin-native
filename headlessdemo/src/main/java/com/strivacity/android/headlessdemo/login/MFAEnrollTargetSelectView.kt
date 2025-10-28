package com.strivacity.android.headlessdemo.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.headlessdemo.ui.theme.StrivacitySecondary
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.render.models.GlobalMessages
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.SelectWidget
import com.strivacity.android.native_sdk.render.models.StaticWidget
import com.strivacity.android.native_sdk.render.models.SubmitWidget
import kotlinx.coroutines.launch

@Composable
fun MFAEnrollTargetSelectView(screen: Screen, headlessAdapter: HeadlessAdapter) {
  val messages by headlessAdapter.messages().collectAsState()

  val coroutineScope = rememberCoroutineScope()

  val mfaEnrollTargetSelectFormWidgets =
      screen.forms?.find { it.id == "mfaEnrollTargetSelect" }?.widgets
  val targetWidget = mfaEnrollTargetSelectFormWidgets?.find { it.id == "target" }
  var targetInitialValue = ""
  if (targetWidget is SelectWidget) {
    targetInitialValue = targetWidget.value ?: ""
  }

  var target by remember { mutableStateOf(targetInitialValue) }

  var globalShowed by remember { mutableStateOf(false) }
  if (messages is GlobalMessages) {
    if (!globalShowed) {
      globalShowed = true
      Toast.makeText(
              LocalContext.current, (messages as GlobalMessages).global.text, Toast.LENGTH_SHORT)
          .show()
    }
  }

  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(35.dp)) {
        Text(
            (mfaEnrollTargetSelectFormWidgets?.find { it.id == "section-title" } as StaticWidget?)
                ?.value ?: "Verify your authenticator",
            fontSize = 24.sp,
            fontWeight = FontWeight.W600)

        TextField(
            value = target,
            onValueChange = { target = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth())
        val targetErrorMessage = messages?.errorMessageForWidget("mfaEnrollTargetSelect", "target")
        if (targetErrorMessage != null) {
          Text(targetErrorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrivacityPrimary),
            onClick = {
              coroutineScope.launch {
                globalShowed = false
                headlessAdapter.submit(
                    "mfaEnrollTargetSelect", mapOf("target" to target, "method" to "passcode"))
              }
            }) {
              Text("Verify")
            }

        (screen.forms
                ?.find { it.id == "additionalActions/selectDifferentMethod" }
                ?.widgets
                ?.find { it.id == "submit" } as SubmitWidget?)
            ?.let {
              Button(
                  modifier = Modifier.fillMaxWidth(),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = StrivacitySecondary, contentColor = Color.Black),
                  onClick = {
                    coroutineScope.launch {
                      globalShowed = false
                      headlessAdapter.submit("additionalActions/selectDifferentMethod", mapOf())
                    }
                  }) {
                    Text(it.label)
                  }
            }

        TextButton(
            onClick = {
              coroutineScope.launch {
                globalShowed = false
                headlessAdapter.submit("reset", mapOf())
              }
            }) {
              Text("Back to login")
            }
      }
}
