package com.strivacity.android.native_sdk.service

import com.strivacity.android.native_sdk.HttpError
import com.strivacity.android.native_sdk.SessionExpiredError
import com.strivacity.android.native_sdk.render.models.Screen
import io.ktor.client.call.body
import io.ktor.http.URLBuilder
import io.ktor.http.path
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class LoginHandlerService(
    private val httpService: HttpService,
    private val issuer: String,
    private val sessionId: String
) {

  suspend fun initCall(): Screen {
    val httpResponse =
        httpService.post(URLBuilder(issuer).apply { path("/flow/api/v1/init") }.build(), sessionId)

    if (httpResponse.status.value != 200 && httpResponse.status.value != 400) {
      if (httpResponse.status.value == 403) {
        throw SessionExpiredError()
      }

      throw HttpError(statusCode = httpResponse.status.value)
    }

    return httpResponse.body()
  }

  suspend fun submitForm(formId: String, body: Map<String, Any>): Screen {
    val httpResponse =
        httpService.post(
            URLBuilder(issuer).apply { path("/flow/api/v1/form/$formId") }.build(),
            sessionId,
            body.toJsonElement())

    if (httpResponse.status.value != 200 && httpResponse.status.value != 400) {
      if (httpResponse.status.value == 403) {
        throw SessionExpiredError()
      }

      throw HttpError(statusCode = httpResponse.status.value)
    }

    return httpResponse.body()
  }
}

private fun List<*>.toJsonElement(): JsonElement {
  val list: MutableList<JsonElement> = mutableListOf()
  this.forEach { value ->
    when (value) {
      null -> list.add(JsonNull)
      is Map<*, *> -> list.add(value.toJsonElement())
      is List<*> -> list.add(value.toJsonElement())
      is Boolean -> list.add(JsonPrimitive(value))
      is Number -> list.add(JsonPrimitive(value))
      is String -> list.add(JsonPrimitive(value))
      is Enum<*> -> list.add(JsonPrimitive(value.toString()))
      else -> error("Can't serialize unknown collection type: $value")
    }
  }
  return JsonArray(list)
}

private fun Map<*, *>.toJsonElement(): JsonObject {
  val map: MutableMap<String, JsonElement> = mutableMapOf()
  this.forEach { (key, value) ->
    key as String
    when (value) {
      null -> map[key] = JsonNull
      is Map<*, *> -> map[key] = value.toJsonElement()
      is List<*> -> map[key] = value.toJsonElement()
      is Boolean -> map[key] = JsonPrimitive(value)
      is Number -> map[key] = JsonPrimitive(value)
      is String -> map[key] = JsonPrimitive(value)
      is Enum<*> -> map[key] = JsonPrimitive(value.toString())
      else -> error("Can't serialize unknown type: $value")
    }
  }
  return JsonObject(map)
}
