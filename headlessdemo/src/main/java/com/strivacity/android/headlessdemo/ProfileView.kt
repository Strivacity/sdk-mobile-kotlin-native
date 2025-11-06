package com.strivacity.android.headlessdemo

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.native_sdk.NativeSDK
import kotlinx.coroutines.launch

@Composable
fun ProfileView(nativeSDK: NativeSDK) {
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  var accessToken by remember { mutableStateOf("") }

  val profile by nativeSDK.session.profile.collectAsState()

  Column(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
    Text("Welcome, ${profile?.claims?.get("email") ?: "N/A"}")
    Text("")
    Text("accessToken", fontWeight = FontWeight.W600)
    Text(accessToken)
    Text("")
    Text("claims", fontWeight = FontWeight.W600)
    profile!!.idToken.split(".")[1].let {
      val decoded = String(android.util.Base64.decode(it, android.util.Base64.DEFAULT))
      Text(formatString(decoded))
    }

    Text("")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
      Button(
          colors = ButtonDefaults.buttonColors(containerColor = StrivacityPrimary),
          onClick = { coroutineScope.launch { nativeSDK.logout() } }) {
            Text("Logout")
          }
    }
  }

  LaunchedEffect(Unit) {
    coroutineScope.launch {
      try {
        accessToken = nativeSDK.getAccessToken() ?: ""
      } catch (e: Throwable) {
        Toast.makeText(context, "Failed to fetch access_token ${e.message}", Toast.LENGTH_SHORT)
            .show()
      }
    }
  }
}

fun formatString(text: String): String {
  val json = StringBuilder()
  var indentString = ""

  for (i in 0..<text.length) {
    val letter = text[i]
    when (letter) {
      '{',
      '[' -> {
        json.append("\n" + indentString + letter + "\n")
        indentString = indentString + "\t"
        json.append(indentString)
      }

      '}',
      ']' -> {
        indentString = indentString.replaceFirst("\t".toRegex(), "")
        json.append("\n" + indentString + letter)
      }

      ',' -> json.append(letter.toString() + "\n" + indentString)
      else -> json.append(letter)
    }
  }

  return json.toString()
}
