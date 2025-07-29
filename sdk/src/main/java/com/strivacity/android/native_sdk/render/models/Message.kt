package com.strivacity.android.native_sdk.render.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable data class Message(val type: String, val text: String)

@Serializable(with = MessagesSerializer::class)
sealed class Messages {
  fun errorMessageForWidget(formId: String, widgetId: String): String? {
    return when (this) {
      is FormMessages -> this.form[formId]?.get(widgetId)?.text
      is GlobalMessages -> null
    }
  }
}

@Serializable data class GlobalMessages(val global: Message) : Messages()

@Serializable data class FormMessages(val form: Map<String, Map<String, Message>>) : Messages()

object MessagesSerializer : JsonContentPolymorphicSerializer<Messages>(Messages::class) {
  override fun selectDeserializer(element: JsonElement) =
      when {
        "global" in element.jsonObject -> GlobalMessages.serializer()
        else -> FormMessagesSerializer
      }
}

object FormMessagesSerializer : KSerializer<FormMessages> {
  override val descriptor: SerialDescriptor
    get() =
        MapSerializer(String.serializer(), MapSerializer(String.serializer(), Message.serializer()))
            .descriptor

  override fun deserialize(decoder: Decoder): FormMessages {
    return FormMessages(
        MapSerializer(String.serializer(), MapSerializer(String.serializer(), Message.serializer()))
            .deserialize(decoder))
  }

  override fun serialize(encoder: Encoder, value: FormMessages) {
    return MapSerializer(
            String.serializer(), MapSerializer(String.serializer(), Message.serializer()))
        .serialize(encoder, value.form)
  }
}
