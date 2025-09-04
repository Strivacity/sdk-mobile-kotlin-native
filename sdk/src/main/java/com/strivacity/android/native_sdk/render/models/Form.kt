package com.strivacity.android.native_sdk.render.models

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.webauthn.AuthenticatorSelectionCriteria
import androidx.credentials.webauthn.PublicKeyCredentialCreationOptions
import androidx.credentials.webauthn.PublicKeyCredentialDescriptor
import androidx.credentials.webauthn.PublicKeyCredentialParameters
import androidx.credentials.webauthn.PublicKeyCredentialRequestOptions
import androidx.credentials.webauthn.PublicKeyCredentialRpEntity
import androidx.credentials.webauthn.PublicKeyCredentialUserEntity
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Widget {
    abstract val id: String

    open fun value(): Any? {
        return null
    }
}

@Serializable
@SerialName("form")
data class FormWidget(override val id: String, val widgets: List<Widget>) : Widget() {}

@Serializable
@SerialName("submit")
data class SubmitWidget(override val id: String, val label: String, val render: Render) : Widget() {
    @Serializable
    data class Render(
        val type: String, val textColor: String?, val bgColor: String?, val hint: SubmitWidgetHint?
    ) {
        @Serializable
        data class SubmitWidgetHint(val icon: String?, val variant: String?)
    }
}

@Serializable
@SerialName("webauthn")
data class WebauthnWidget(
    override val id: String, val label: String, val render: Render, val metadata: WebauthnMetadata
) : Widget() {
    @Serializable
    data class Render(
        val type: String,
        val textColor: String?,
        val bgColor: String?,
        val hint: WebauthnWidgetHint?
    ) {
        @Serializable
        data class WebauthnWidgetHint(val icon: String?, val variant: String?)
    }

    @Serializable
    data class WebauthnMetadata(
        val assertionOptions: Assertion?,
        val creationOptions: Creation?
    ) {
        @Serializable
        data class Assertion(
            val challenge: String,
            val timeout: Long,
            val rpId: String,
            val userVerification: String
        )

        @Serializable
        data class Creation(
            val rp: PublicKeyCredentialRpEntity,
            val user: PublicKeyCredentialUserEntity,
            val challenge: String,
            val pubKeyCredParams: List<PublicKeyCredentialParameters>,

            var excludeCredentials: List<PublicKeyCredentialDescriptor>,
            var authenticatorSelection: AuthenticatorSelectionCriteria,
            var attestation: String
        ) {
            @Serializable
            data class PublicKeyCredentialRpEntity(val name: String, val id: String)
            @Serializable
            data class PublicKeyCredentialUserEntity(
                val name: String,
                val id: String,
                val displayName: String
            )

            @Serializable
            data class PublicKeyCredentialParameters(val type: String, val alg: Long)
            @Serializable
            data class PublicKeyCredentialDescriptor(
                val type: String,
                val id: String,
                val transports: List<String>
            )

            @Serializable
            data class AuthenticatorSelectionCriteria(
                val authenticatorAttachment: String,
                val residentKey: String,
                val requireResidentKey: Boolean = false,
                val userVerification: String = "preferred"
            )

        }

    }
}


@Serializable
@SerialName("static")
data class StaticWidget(override val id: String, val value: String, val render: Render) : Widget() {
    @Serializable
    data class Render(val type: String)
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
    val validator: Validator
) : Widget() {

    override fun value(): String? {
        return value
    }

    @Serializable
    data class Validator(
        val minLength: Int?, val maxLength: Int?, val regex: String?, val required: Boolean
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
    val render: Render,
) : Widget() {
    override fun value(): Boolean {
        return value
    }

    @Serializable
    data class Validator(val required: Boolean)

    @Serializable
    data class Render(val type: String, val labelType: String)
}

@Serializable
@SerialName("password")
data class PasswordWidget(
    override val id: String,
    val label: String,
    val qualityIndicator: Boolean,
    val validator: Validator?
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
    val render: Render,
    val options: List<Option>,
    val validator: Validator
) : Widget() {
    override fun value(): String? {
        return value
    }

    @Serializable
    data class Validator(val required: Boolean)

    @Serializable
    data class Render(val type: String)

    @Serializable
    data class Option(
        val type: String, val label: String?, val value: String?, val options: List<Option>?
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
    val validator: Validator?
) : Widget() {
    override fun value(): List<String?> {
        return value
    }

    @Serializable
    data class Validator(val minSelectable: Int, val maxSelectable: Int)

    @Serializable
    data class Option(
        val type: String, val label: String, val value: String, val options: List<Option>?
    )
}

@Serializable
@SerialName("passcode")
data class PasscodeWidget(
    override val id: String,
    val label: String,
    val validator: Validator?,
) : Widget() {
    @Serializable
    data class Validator(val length: Int?)
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
    override fun value(): String? {
        return value
    }

    @Serializable
    data class Validator(val required: Boolean?)
}

@Serializable
@SerialName("date")
data class DateWidget(
    override val id: String,
    val label: String?,
    val placeholder: String?,
    val readonly: Boolean,
    val value: String?,
    val render: Render,
    val validator: Validator?
) : Widget() {
    override fun value(): String? {
        return value
    }

    @Serializable
    data class Render(val type: String)

    @Serializable
    data class Validator(val required: Boolean?, val notBefore: String?, val notAfter: String?)
}
