package com.strivacity.android.native_sdk.render.models

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Widget {
    abstract val id: String

    open fun value(): Any? = null
}

@Serializable
@SerialName("form")
data class FormWidget(
    override val id: String,
    val widgets: List<Widget>,
) : Widget()

@Serializable
@SerialName("submit")
data class SubmitWidget(
    override val id: String,
    val label: String,
    val render: Render?,
) : Widget() {
    @Serializable
    data class Render(
        val type: String,
        val textColor: String?,
        val bgColor: String?,
        val hint: SubmitWidgetHint?,
    ) {
        @Serializable data class SubmitWidgetHint(
            val icon: String?,
            val variant: String?,
        )
    }
}

@Serializable
@SerialName("close")
data class CloseWidget(
    override val id: String,
    val label: String,
    val render: Render?,
) : Widget() {
    @Serializable
    data class Render(
        val type: String,
        val textColor: String?,
        val bgColor: String?,
        val hint: CloseWidgetHint?,
    ) {
        @Serializable data class CloseWidgetHint(
            val icon: String?,
            val variant: String?,
        )
    }
}

@Serializable
@SerialName("static")
data class StaticWidget(
    override val id: String,
    val value: String,
    val render: Render?,
) : Widget() {
    @Serializable data class Render(
        val type: String,
    )
}

@Serializable
@SerialName("input")
data class InputWidget(
    override val id: String,
    val label: String,
    val value: String?,
    val readonly: Boolean,
    val autocomplete: String?,
    val inputmode: String?,
    val validator: Validator,
    val render: Render?,
) : Widget() {
    override fun value(): String? = value

    @Serializable
    data class Validator(
        val minLength: Int?,
        val maxLength: Int?,
        val regex: String?,
        val required: Boolean,
    )

    @Serializable
    data class Render(
        val autocompleteHint: String?,
    )
}

@Serializable
@SerialName("checkbox")
data class CheckboxWidget(
    override val id: String,
    val label: String,
    val value: Boolean,
    val readonly: Boolean,
    val validator: Validator?,
    val render: Render?,
) : Widget() {
    override fun value(): Boolean = value

    @Serializable data class Validator(
        val required: Boolean,
    )

    @Serializable data class Render(
        val type: String,
        val labelType: String,
    )
}

@Serializable
@SerialName("password")
data class PasswordWidget(
    override val id: String,
    val label: String,
    val qualityIndicator: Boolean,
    val validator: Validator?,
) : Widget() {
    @Serializable
    data class Validator(
        val minLength: Int?,
        val maxNumericCharacterSequences: Int?,
        val maxRepeatedCharacters: Int?,
        val mustContain: List<String>?,
    )
}

@Serializable
@SerialName("select")
data class SelectWidget(
    override val id: String,
    val label: String?,
    val value: String?,
    val readonly: Boolean,
    val render: Render?,
    val options: List<Option>,
    val validator: Validator,
) : Widget() {
    override fun value(): String? = value

    @Serializable data class Validator(
        val required: Boolean,
    )

    @Serializable data class Render(
        val type: String,
    )

    @Serializable
    data class Option(
        val type: String,
        val label: String?,
        val value: String?,
        val options: List<Option>?,
    )
}

@Serializable
@SerialName("multiSelect")
data class MultiSelectWidget(
    override val id: String,
    val label: String,
    val value: List<String?>,
    val readonly: Boolean,
    val options: List<Option>,
    val validator: Validator?,
) : Widget() {
    override fun value(): List<String?> = value

    @Serializable data class Validator(
        val minSelectable: Int,
        val maxSelectable: Int,
    )

    @Serializable
    data class Option(
        val type: String,
        val label: String,
        val value: String,
        val options: List<Option>?,
    )
}

@Serializable
@SerialName("passcode")
data class PasscodeWidget(
    override val id: String,
    val label: String,
    val validator: Validator?,
) : Widget() {
    @Serializable data class Validator(
        val length: Int?,
    )
}

@Serializable
@SerialName("phone")
data class PhoneWidget(
    override val id: String,
    val label: String,
    val readonly: Boolean,
    val value: String?,
    val validator: Validator?,
) : Widget() {
    override fun value(): String? = value

    @Serializable data class Validator(
        val required: Boolean?,
    )
}

@Serializable
@SerialName("date")
data class DateWidget(
    override val id: String,
    val label: String?,
    val placeholder: String?,
    val readonly: Boolean,
    val value: String?,
    val render: Render?,
    val validator: Validator?,
) : Widget() {
    override fun value(): String? = value

    @Serializable data class Render(
        val type: String,
    )

    @Serializable
    data class Validator(
        val required: Boolean?,
        val notBefore: String?,
        val notAfter: String?,
    )
}

@Serializable
@SerialName("passkeyLogin")
data class PasskeyLoginWidget(
    override val id: String,
    val label: String,
    val render: Render?,
    override val assertionOptions: AssertionOptions,
) : Widget(),
    WithAssertionOptions<PasskeyLoginWidget> {
    @Transient
    override val widget = this

    @Serializable
    data class Render(
        val type: String,
        val hint: PasskeyLoginWidgetHint?,
    ) {
        @Serializable
        data class PasskeyLoginWidgetHint(
            val variant: String?,
        )
    }
}

@Serializable
@SerialName("passkeyEnroll")
data class PasskeyEnrollWidget(
    override val id: String,
    val label: String,
    val render: Render?,
    override val enrollOptions: EnrollOptions,
) : Widget(),
    WithEnrollmentOptions<PasskeyEnrollWidget> {
    @Transient
    override val widget = this

    @Serializable
    data class Render(
        val type: String,
        val hint: PasskeyLoginWidgetHint?,
        val notification: PasskeyLoginWidgetNotification?,
    ) {
        @Serializable
        data class PasskeyLoginWidgetHint(
            val variant: String?,
        )

        @Serializable
        data class PasskeyLoginWidgetNotification(
            val cancelled: String?,
        )
    }
}

@Serializable
data class EnrollOptions(
    val rp: Rp,
    val user: User,
    val challenge: String,
    val pubKeyCredParams: List<PubKeyCredParam>,
    val excludeCredentials: List<ExcludeCredential>,
    val authenticatorSelection: AuthenticatorSelection,
    val attestation: String,
) {
    @Serializable data class Rp(
        val id: String,
        val name: String,
    )

    @Serializable data class User(
        val id: String,
        val name: String = "",
        val displayName: String,
    )

    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class PubKeyCredParam(
        val type: String,
        val alg: Int,
    )

    @Serializable data class ExcludeCredential(
        val id: String,
        val type: String?,
        val transports: List<String>?,
    )

    @Serializable
    data class AuthenticatorSelection(
        val authenticatorAttachment: String?,
        val requireResidentKey: Boolean?,
        val residentKey: String?,
        val userVerification: String?,
    )
}

@Serializable
data class AssertionOptions(
    val allowCredentials: List<AllowCredential>,
    val challenge: String,
    val rpId: String,
    val userVerification: String,
    val timeout: Int?,
) {
    @Serializable
    data class AllowCredential(
        val id: String,
        val type: String?,
        val transports: List<String>?,
    )
}

@Serializable
@SerialName("webauthnLogin")
data class WebauthnLoginWidget(
    override val id: String,
    val label: String,
    val render: Render?,
    override val assertionOptions: AssertionOptions,
    val authenticatorType: String,
) : Widget(),
    WithAssertionOptions<WebauthnLoginWidget> {
    @Transient
    override val widget = this

    @Serializable
    data class Render(
        val type: String,
        val hint: WebauthnLoginWidgetHint?,
        val notification: WebauthnLoginWidgetNotification?,
    ) {
        @Serializable
        data class WebauthnLoginWidgetHint(
            val variant: String?,
        )

        @Serializable
        data class WebauthnLoginWidgetNotification(
            val cancelled: String?,
        )
    }
}

@Serializable
@SerialName("webauthnEnroll")
data class WebauthnEnrollWidget(
    override val id: String,
    val label: String,
    val render: Render?,
    override val enrollOptions: EnrollOptions,
    val authenticatorType: String,
) : Widget(),
    WithEnrollmentOptions<WebauthnEnrollWidget> {
    @Transient
    override val widget = this

    @Serializable
    data class Render(
        val type: String,
        val hint: WebauthnEnrollWidgetHint?,
        val notification: WebauthnEnrollWidgetNotification?,
    ) {
        @Serializable data class WebauthnEnrollWidgetHint(
            val variant: String?,
        )

        @Serializable data class WebauthnEnrollWidgetNotification(
            val cancelled: String?,
        )
    }
}

interface WithEnrollmentOptions<out T> where T : Widget {
    val enrollOptions: EnrollOptions

    @Transient
    val widget: T
}

interface WithAssertionOptions<out T> where T : Widget {
    val assertionOptions: AssertionOptions

    @Transient
    val widget: T
}
