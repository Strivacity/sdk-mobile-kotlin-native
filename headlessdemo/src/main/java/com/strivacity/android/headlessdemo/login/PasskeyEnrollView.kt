package com.strivacity.android.headlessdemo.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strivacity.android.headlessdemo.ui.theme.StrivacityPrimary
import com.strivacity.android.native_sdk.HeadlessAdapter
import com.strivacity.android.native_sdk.PlatformError
import com.strivacity.android.native_sdk.render.models.Screen
import kotlinx.coroutines.launch

@Composable
fun PasskeyEnrollView(
    screen: Screen,
    headlessAdapter: HeadlessAdapter,
) {
    val messages by headlessAdapter.messages().collectAsState()

    val coroutineScope = rememberCoroutineScope()

    var target by remember {
        mutableStateOf(
            screen.forms!!
                .first { it.id == "passkeyEnroll" }
                .widgets
                .first { it.id == "target" }
                .value() as String,
        )
    }

    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(35.dp),
    ) {
        Text("Create a passkey", fontSize = 24.sp, fontWeight = FontWeight.W600)

        Text("When you are ready, create a passkey using the button below")

        TextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("Device name") },
            modifier = Modifier.fillMaxWidth(),
        )

        val errorMessage = messages?.errorMessageForWidget("passkeyEnroll", "target")
        if (errorMessage != null) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrivacityPrimary),
            onClick = {
                coroutineScope.launch {
                    try {
                        headlessAdapter.submit("passkeyEnroll", mapOf("target" to target))
                    } catch (ex: PlatformError) {
                        Toast
                            .makeText(
                                context,
                                ex.cause?.message ?: "Unknown issue",
                                Toast.LENGTH_SHORT,
                            ).show()
                    } catch (ex: Throwable) {
                        Toast
                            .makeText(
                                context,
                                ex.message ?: "Unknown issue",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            },
        ) {
            Text("Continue")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch { headlessAdapter.submit("additionalActions/skip", mapOf()) }
            },
        ) {
            Text("Skip for now")
        }
    }
}
