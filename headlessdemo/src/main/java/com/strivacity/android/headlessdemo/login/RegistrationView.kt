package com.strivacity.android.headlessdemo.login

import android.widget.Toast
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.headlessdemo.ui.theme.StrivacitySecondary
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.render.models.GlobalMessages
import com.strivacity.android.native_sdk.render.models.InputWidget
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.SubmitWidget
import kotlinx.coroutines.launch

@Composable
fun RegistrationView(screen: Screen, headlessAdapter: HeadlessAdapter) {
  val messages by headlessAdapter.messages().collectAsState()

  val coroutineScope = rememberCoroutineScope()

  val focusManager = LocalFocusManager.current
  val emailFocusRequester = remember { FocusRequester() }
  val passwordFocusRequester = remember { FocusRequester() }
  val passwordConfirmationFocusRequester = remember { FocusRequester() }

  var email by remember {
    mutableStateOf(
        (screen.forms?.find { it.id == "registration" }?.widgets?.find { it.id == "email" }
                as InputWidget?)
            ?.value ?: "")
  }

  var password by remember { mutableStateOf("") }

  var passwordConfirmation by remember { mutableStateOf("") }

  var keepMeLoggedIn by remember { mutableStateOf(false) }

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
        Text("Sign up", fontSize = 24.sp, fontWeight = FontWeight.W600)

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier =
                Modifier.fillMaxWidth().focusRequester(emailFocusRequester).onPreviewKeyEvent {
                  if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                    passwordFocusRequester.requestFocus()
                    true
                  } else {
                    false
                  }
                })
        val emailErrorMessage = messages?.errorMessageForWidget("registration", "email")
        if (emailErrorMessage != null) {
          Text(emailErrorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier =
                Modifier.fillMaxWidth().focusRequester(passwordFocusRequester).onPreviewKeyEvent {
                  if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                    passwordConfirmationFocusRequester.requestFocus()
                    true
                  } else {
                    false
                  }
                })
        val passwordErrorMessage = messages?.errorMessageForWidget("registration", "password")
        if (passwordErrorMessage != null) {
          Text(passwordErrorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }

        TextField(
            value = passwordConfirmation,
            onValueChange = { passwordConfirmation = it },
            label = { Text("Re-type password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier =
                Modifier.fillMaxWidth()
                    .focusRequester(passwordConfirmationFocusRequester)
                    .onPreviewKeyEvent {
                      if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                      } else {
                        false
                      }
                    })
        val passwordConfirmationErrorMessage =
            messages?.errorMessageForWidget("registration", "passwordConfirmation")
        if (passwordConfirmationErrorMessage != null) {
          Text(
              passwordConfirmationErrorMessage,
              color = Color.Red,
              modifier = Modifier.fillMaxWidth())
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
                globalShowed = false
                headlessAdapter.submit(
                    "registration",
                    mapOf(
                        "email" to email,
                        "password" to password,
                        "passwordConfirmation" to passwordConfirmation,
                        "keepMeLoggedIn" to keepMeLoggedIn))
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
                onClick = {
                  coroutineScope.launch {
                    globalShowed = false
                    headlessAdapter.submit(it.id, mapOf())
                  }
                }) {
                  Text((it.widgets[0] as SubmitWidget).label)
                }
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

  // uncomment this to focus on email field when registration loads
  //    LaunchedEffect(Unit) {
  //        emailFocusRequester.requestFocus()
  //    }
}
