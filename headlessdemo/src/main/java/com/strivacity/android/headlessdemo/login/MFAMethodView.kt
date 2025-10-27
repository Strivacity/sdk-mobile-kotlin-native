package com.strivacity.android.headlessdemo.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.render.models.GlobalMessages
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.SelectWidget
import com.strivacity.android.native_sdk.render.models.StaticWidget
import kotlinx.coroutines.launch

@Composable
fun MFAMethodView(screen: Screen, headlessAdapter: HeadlessAdapter) {
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

  var target by remember { mutableStateOf("") }

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
        Text("Choose a multi-factor method", fontSize = 24.sp, fontWeight = FontWeight.W600)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              Text(identifier)
              TextButton(
                  onClick = {
                    coroutineScope.launch { headlessAdapter.submit("reset", mapOf()) }
                  }) {
                    Text("Not you?")
                  }
            }

        Column(modifier = Modifier.selectableGroup().fillMaxWidth()) {
          (screen.forms?.find { it.id == "mfaMethod" }?.widgets?.find { it.id == "id" }
                  as SelectWidget?)
              ?.options
              ?.forEach {
                Text(it.label!!)
                it.options?.forEach { it: SelectWidget.Option ->
                  it.let {
                    Row(
                        Modifier.selectable(
                            selected = it.value == target,
                            onClick = { target = it.value!! },
                            role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically) {
                          RadioButton(selected = target == it.value, onClick = null)
                          Text(text = it.label!!)
                        }
                  }
                }
              }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrivacityPrimary),
            onClick = {
              coroutineScope.launch {
                globalShowed = false
                headlessAdapter.submit("mfaMethod", mapOf("id" to target))
              }
            }) {
              Text("Continue")
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
