package com.strivacity.android.native_sdk

import android.util.Log
import com.strivacity.android.native_sdk.service.TokenResponse
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

internal typealias JSON = Map<String, Any?>

private const val PROFILE = "profile"

class Session(private val storage: Storage) {
  private val _loginInProgress = MutableStateFlow(false)
  val loginInProgress: StateFlow<Boolean> = _loginInProgress

  private val _profile = MutableStateFlow<Profile?>(null)
  val profile: StateFlow<Profile?> = _profile

  /** Tag used for logging */
  private val TAG = Session::class.simpleName

  fun setLoginInProgress(value: Boolean) {
    _loginInProgress.value = value
  }

  internal fun update(tokenResponse: TokenResponse) {
    _loginInProgress.value = false
    _profile.value = Profile(tokenResponse)

    storage.set(PROFILE, Json.encodeToString(_profile.value))
  }

  internal fun load() {
    try {
      val profileContent = storage.get(PROFILE)

      if (profileContent != null) {
        _profile.value = Json.decodeFromString(profileContent)
      }
    } catch (e: Exception) {
      Log.d(TAG, "Failed to parse profileContent", e)
    }
  }

  internal fun clear() {
    _loginInProgress.value = false
    _profile.value = null

    storage.delete(PROFILE)
  }
}

@Serializable
class Profile
internal constructor(
    internal val tokenResponse: TokenResponse,
    @Serializable(with = EpochMillisInstantSerializer::class)
    internal val accessTokenExpiresAt: Instant,
    @Transient val claims: JSON = extractClaims(tokenResponse),
) {
  internal constructor(
      tokenResponse: TokenResponse
  ) : this(tokenResponse, Instant.now().plusSeconds(tokenResponse.expiresIn.toLong()))

  val idToken: String
    get() {
      return tokenResponse.idToken
    }
}

internal fun extractClaims(tokenResponse: TokenResponse): Map<String, Any?> {
  val idToken = tokenResponse.idToken
  val parts = idToken.split(".")

  return Json.decodeFromString(
      MapStringAnySerializer,
      String(Base64.getUrlDecoder().decode(parts[1])),
  )
}

object MapStringAnySerializer : KSerializer<JSON> {
  override val descriptor: SerialDescriptor =
      MapSerializer(String.serializer(), JsonElement.serializer()).descriptor

  override fun deserialize(decoder: Decoder): JSON {
    val input = decoder as? JsonDecoder ?: error("JsonDecoder expected")
    val jsonObject = input.decodeJsonElement().jsonObject

    @Suppress("UNCHECKED_CAST")
    return jsonElementToAny(jsonObject) as JSON
  }

  private fun jsonElementToAny(value: JsonElement): Any? {
    return when {
      value is JsonPrimitive && value.isString -> value.content
      value is JsonPrimitive && value.booleanOrNull != null -> value.boolean
      value is JsonPrimitive && value.longOrNull != null -> value.long
      value is JsonPrimitive && value.doubleOrNull != null -> value.double
      value is JsonObject -> value.mapValues { jsonElementToAny(it.value) }
      value is JsonArray -> value.map { jsonElementToAny(it) }
      else -> null
    }
  }

  override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
    val jsonEncoder = encoder as? JsonEncoder ?: error("JsonEncoder expected")
    val jsonMap = buildJsonObject {
      value.forEach { (k, v) -> put(k, Json.encodeToJsonElement(v)) }
    }
    jsonEncoder.encodeJsonElement(jsonMap)
  }
}

private object EpochMillisInstantSerializer : KSerializer<Instant> {
  override val descriptor =
      kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
          "java.time.Instant.epochmillis",
          kotlinx.serialization.descriptors.PrimitiveKind.LONG,
      )

  override fun serialize(encoder: Encoder, value: Instant) {
    encoder.encodeLong(value.toEpochMilli())
  }

  override fun deserialize(decoder: Decoder): Instant {
    return Instant.ofEpochMilli(decoder.decodeLong())
  }
}
