package com.strivacity.android.app

import android.credentials.CreateCredentialException
import android.credentials.GetCredentialException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Popup
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strivacity.android.native_sdk.render.LoginController
import com.strivacity.android.native_sdk.render.models.CheckboxWidget
import com.strivacity.android.native_sdk.render.models.DateWidget
import com.strivacity.android.native_sdk.render.models.InputWidget
import com.strivacity.android.native_sdk.render.models.MultiSelectWidget
import com.strivacity.android.native_sdk.render.models.PasscodeWidget
import com.strivacity.android.native_sdk.render.models.PasswordWidget
import com.strivacity.android.native_sdk.render.models.PhoneWidget
import com.strivacity.android.native_sdk.render.models.Screen
import com.strivacity.android.native_sdk.render.models.SelectWidget
import com.strivacity.android.native_sdk.render.models.StaticWidget
import com.strivacity.android.native_sdk.render.models.SubmitWidget
import com.strivacity.android.native_sdk.render.models.WebauthnWidget
import com.strivacity.android.native_sdk.render.models.Widget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Composable
fun Widget(
    loginController: LoginController,
    screen: Screen,
    widget: Widget,
    formId: String,
    widgetId: String
) {
    when (widget) {
        is CheckboxWidget -> CheckboxWidget(loginController, screen, widget, formId, widgetId)
        is DateWidget -> DateWidget(loginController, screen, widget, formId, widgetId)
        is InputWidget -> InputWidget(loginController, screen, widget, formId, widgetId)
        is MultiSelectWidget -> MultiSelectWidget(loginController, screen, widget, formId, widgetId)
        is PasscodeWidget -> PasscodeWidget(loginController, screen, widget, formId, widgetId)
        is PasswordWidget -> PasswordWidget(loginController, screen, widget, formId, widgetId)
        is PhoneWidget -> PhoneWidget(loginController, screen, widget, formId, widgetId)
        is SelectWidget -> SelectWidget(loginController, screen, widget, formId, widgetId)
        is StaticWidget -> StaticWidget(loginController, screen, widget, formId, widgetId)
        is SubmitWidget -> SubmitWidget(loginController, screen, widget, formId, widgetId)
        is WebauthnWidget -> WebauthnWidget(loginController, screen, widget, formId, widgetId)
        else -> loginController.triggerFallback()
    }

    val messages by loginController.messages.collectAsState()

    val errorMessage = messages?.errorMessageForWidget(formId, widgetId)
    if (errorMessage != null) {
        Text(errorMessage, color = Color.Red)
    }
}

@Composable
fun StaticWidget(
    loginController: LoginController,
    screen: Screen,
    widget: StaticWidget,
    formId: String,
    widgetId: String
) {
    TextWithType(loginController, widget.render.type, widget.value)
}

@Composable
fun InputWidget(
    loginController: LoginController,
    screen: Screen,
    widget: InputWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget<String?>(formId, widgetId, null)
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    val keyboardOptions = when (widget.inputmode) {
        "email" -> KeyboardOptions(keyboardType = KeyboardType.Email)
        else -> KeyboardOptions()
    }

    val modifier = when (widget.autocomplete) {
        "username" -> Modifier.semantics { contentType = ContentType.Username }
        else -> Modifier.semantics {}
    }

    TextField(
        value = value ?: "",
        onValueChange = { stateForWidget.value = it },
        label = { Text(widget.label) },
        enabled = !processing,
        keyboardOptions = keyboardOptions,
        modifier = modifier
    )
}

@Composable
fun SubmitWidget(
    loginController: LoginController,
    screen: Screen,
    widget: SubmitWidget,
    formId: String,
    widgetId: String
) {
    val coroutineScope = rememberCoroutineScope()

    val processing by loginController.processing.collectAsState()

    val onClick: () -> Unit = { coroutineScope.launch { loginController.submit(formId) } }
    when (widget.render.type) {
        in "button" -> Button(onClick = onClick, enabled = !processing) { Text(widget.label) }
        in "link" -> TextButton(onClick = onClick, enabled = !processing) { Text(widget.label) }
        else -> loginController.triggerFallback()
    }
}

@Composable
fun WebauthnWidget(
    loginController: LoginController,
    screen: Screen,
    widget: WebauthnWidget,
    formId: String,
    widgetId: String
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager: CredentialManager = CredentialManager.create(context)

    val processing by loginController.processing.collectAsState()
    val stateForWidget = loginController.stateForWidget<Map<String, Any>?>(formId, widgetId, null)

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun signUpWithPasskey(metadata: WebauthnWidget.WebauthnMetadata): Map<String, Any> {
        try {

            val request = CreatePublicKeyCredentialRequest(
                Gson().toJson(metadata.creationOptions)
            )

            val result = credentialManager.createCredential(
                request = request, context = context
            ) as CreatePublicKeyCredentialResponse

            return parseJson(result.registrationResponseJson) as Map<String, Any>
        } catch (e: CreateCredentialException) {
            println("Passkey creation failed: ${e.message}")
        }

        return mapOf()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun signInWithPasskey(metadata: WebauthnWidget.WebauthnMetadata): Map<String, Any> {

        try {

            val option = GetPublicKeyCredentialOption(
                requestJson = Gson().toJson(metadata.assertionOptions)
            )

            val result = credentialManager.getCredential(
                request = GetCredentialRequest(
                    listOf(option)
                ), context = context
            )

            val credential = result.credential as PublicKeyCredential
            return parseJson(
                credential.authenticationResponseJson,
            ) as Map<String, Any>
        } catch (e: GetCredentialException) {
            println("Passkey authentication failed: ${e.message}")
        }
        return mapOf()

    }

    val onClick: () -> Unit = {
        coroutineScope.launch {
            val credential: Map<String, Any> = if (widget.metadata.creationOptions != null) {
                signUpWithPasskey(widget.metadata)
            } else {
                signInWithPasskey(widget.metadata)
            }
            stateForWidget.value = credential
            loginController.submit(formId)
        }
    }
    when (widget.render.type) {
        in "button" -> Button(onClick = onClick, enabled = !processing) { Text(widget.label) }
        else -> loginController.triggerFallback()
    }
}

fun parseJson(json: String): Any? {
    val trimmed = json.trim()
    return when {
        trimmed.startsWith("{") -> {
            val inner = trimmed.removePrefix("{").removeSuffix("}")
            val pairs = splitTopLevel(inner, ',')
            pairs.associate { p ->
                val (k, v) = p.split(":", limit = 2)
                k.trim().removeSurrounding("\"") to parseJson(v)
            }
        }

        trimmed.startsWith("[") -> {
            val inner = trimmed.removePrefix("[").removeSuffix("]")
            splitTopLevel(inner, ',').map { parseJson(it) }
        }

        trimmed.startsWith("\"") -> trimmed.removeSurrounding("\"")
        trimmed == "null" -> null
        trimmed == "true" || trimmed == "false" -> trimmed.toBoolean()
        trimmed.toDoubleOrNull() != null -> trimmed.toDouble()
        else -> trimmed
    }
}

fun splitTopLevel(s: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    val current = StringBuilder()
    for (ch in s) {
        when (ch) {
            '{', '[' -> {
                depth++; current.append(ch)
            }

            '}', ']' -> {
                depth--; current.append(ch)
            }

            delimiter -> if (depth == 0) {
                result.add(current.toString())
                current.clear()
            } else current.append(ch)

            else -> current.append(ch)
        }
    }
    if (current.isNotEmpty()) result.add(current.toString())
    return result.filter { it.isNotBlank() }
}


@Composable
fun CheckboxWidget(
    loginController: LoginController,
    screen: Screen,
    widget: CheckboxWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget(formId, widgetId, false)
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        when (widget.render.type) {
            in "checkboxShown" -> Checkbox(
                value, { stateForWidget.value = it }, enabled = !processing
            )

            in "checkboxHidden" -> {
                LaunchedEffect(Unit) { stateForWidget.value = true }
            }

            else -> loginController.triggerFallback()
        }

        TextWithType(loginController, widget.render.labelType, widget.label)
    }
}

@Composable
fun PasswordWidget(
    loginController: LoginController,
    screen: Screen,
    widget: PasswordWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget(formId, widgetId, "")
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    TextField(
        value = value,
        onValueChange = { stateForWidget.value = it },
        label = { Text(widget.label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = !processing
    )
}

@Composable
fun PasscodeWidget(
    loginController: LoginController,
    screen: Screen,
    widget: PasscodeWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget(formId, widgetId, "")
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    TextField(
        value = value,
        onValueChange = { stateForWidget.value = it },
        label = { Text(widget.label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = !processing,
        modifier = Modifier.semantics { contentType = ContentType.SmsOtpCode })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectWidget(
    loginController: LoginController,
    screen: Screen,
    widget: SelectWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget<String?>(formId, widgetId, null)
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    when (widget.render.type) {
        "radio" -> {
            Column(modifier = Modifier.selectableGroup()) {
                if (widget.label != null) {
                    Row { Text(widget.label!!) }
                }

                widget.options.forEach {
                    SelectWidgetRadioOption(loginController, screen, widget, it, formId, widgetId)
                }
            }
        }

        "dropdown" -> {
            Column {
                val options = flattenOptions(widget.options)
                var expanded by remember { mutableStateOf(false) }
                var label by remember { mutableStateOf<String?>(null) }

                if (value != null) {
                    label = options.find { it.value == value }?.label
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    TextField(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
                        value = label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(widget.label ?: "") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded,
                                modifier = Modifier.menuAnchor(MenuAnchorType.SecondaryEditable),
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label ?: "",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    if (option.value != null) {
                                        stateForWidget.value = option.value!!
                                        label = option.label
                                    }
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        }

        else -> loginController.triggerFallback()
    }
}

fun flattenOptions(from: List<SelectWidget.Option>): MutableList<SelectWidget.Option> {
    val list = mutableListOf<SelectWidget.Option>()

    from.forEach {
        list.add(it)

        if (it.type == "group" && it.options != null) {
            list.addAll(flattenOptions(it.options!!))
        }
    }

    return list
}

@Composable
fun SelectWidgetRadioOption(
    loginController: LoginController,
    screen: Screen,
    widget: SelectWidget,
    widgetOption: SelectWidget.Option,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget(formId, widgetId, "")
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    when (widgetOption.type) {
        "group" -> {
            if (widgetOption.label != null) {
                Row { Text(widgetOption.label!!) }
            }
            widgetOption.options?.forEach {
                SelectWidgetRadioOption(loginController, screen, widget, it, formId, widgetId)
            }
        }

        "item" -> {
            Row(
                modifier = Modifier.selectable(
                    selected = (widgetOption.value == value),
                    onClick = { stateForWidget.value = widgetOption.value!! },
                    role = Role.RadioButton
                ), verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (widgetOption.value == value), onClick = null, enabled = !processing
                )
                Text(text = widgetOption.label ?: "")
            }
        }

        else -> loginController.triggerFallback()
    }
}

@Composable
fun MultiSelectWidget(
    loginController: LoginController,
    screen: Screen,
    widget: MultiSelectWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget =
        loginController.stateForWidget<MutableList<String>>(formId, widgetId, mutableListOf())
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    Column(modifier = Modifier.selectableGroup()) {
        Row { Text(widget.label) }

        widget.options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    value.contains(option.value), {
                        val list = stateForWidget.value.toMutableList()

                        if (it) {
                            list.add(option.value)
                        } else {
                            list.remove(option.value)
                        }

                        stateForWidget.value = list
                    }, enabled = !processing
                )
                Text(option.label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateWidget(
    loginController: LoginController,
    screen: Screen,
    widget: DateWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget<String?>(formId, widgetId, null)
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    when (widget.render.type) {
        "native" -> {
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState()

            LaunchedEffect(Unit) {
                if (value != null) {
                    datePickerState.selectedDateMillis = convertDateToMillis(value!!)
                }
            }

            Box {
                TextField(
                    value = value ?: "",
                    onValueChange = {},
                    label = { Text(widget.label ?: "") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { showDatePicker = !showDatePicker }, enabled = !processing
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                        }
                    })

                if (showDatePicker) {
                    Popup(
                        onDismissRequest = {
                            showDatePicker = false
                            stateForWidget.value =
                                datePickerState.selectedDateMillis?.let { convertMillisToDate(it) }
                        }, alignment = Alignment.TopStart
                    ) {
                        Box { DatePicker(state = datePickerState, showModeToggle = false) }
                    }
                }
            }
        }

        "fieldSet" -> {
            var year by remember { mutableStateOf("") }
            var month by remember { mutableStateOf("") }
            var day by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                if (value != null) {
                    val split = value!!.split("-")
                    if (split.size == 3) {
                        year = split[0]
                        month = split[1]
                        day = split[2]
                    }
                }
            }

            val afterValueChange = {
                if (year == "" || month == "" || day == "") {
                    stateForWidget.value = null
                } else {
                    try {
                        val milis = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                        stateForWidget.value = convertMillisToDate(milis)
                    } catch (e: Exception) {
                        stateForWidget.value = null
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row { Text(widget.label ?: "") }

                Row {
                    TextField(
                        value = year, onValueChange = {
                        year = it
                        afterValueChange()
                    }, label = { Text("Year") }, modifier = Modifier.fillMaxSize(0.3f)
                    )

                    TextField(
                        value = month, onValueChange = {
                        month = it
                        afterValueChange()
                    }, label = { Text("Month") }, modifier = Modifier.fillMaxSize(0.3f)
                    )

                    TextField(
                        value = day, onValueChange = {
                        day = it
                        afterValueChange()
                    }, label = { Text("Day") }, modifier = Modifier.fillMaxSize(0.3f)
                    )
                }
            }
        }

        else -> loginController.triggerFallback()
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}

fun convertDateToMillis(date: String): Long? {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.parse(date)?.toInstant()?.toEpochMilli()
}

@Composable
fun PhoneWidget(
    loginController: LoginController,
    screen: Screen,
    widget: PhoneWidget,
    formId: String,
    widgetId: String
) {
    val stateForWidget = loginController.stateForWidget<String?>(formId, widgetId, null)
    val value by stateForWidget.collectAsState()

    val processing by loginController.processing.collectAsState()

    TextField(
        value = value ?: "",
        onValueChange = { stateForWidget.value = it },
        label = { Text(widget.label) },
        enabled = !processing,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.semantics { contentType = ContentType.PhoneNumber })
}

@Composable
fun TextWithType(loginController: LoginController, type: String, value: String) {
    when (type) {
        in "text" -> Text(value)
        in "html" -> Text(
            AnnotatedString.fromHtml(
                value, linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            )
        )

        else -> loginController.triggerFallback()
    }
}
