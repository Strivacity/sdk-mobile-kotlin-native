package com.strivacity.android.native_sdk.service

import com.strivacity.android.native_sdk.Logging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.protocolWithAuthority
import io.ktor.serialization.kotlinx.json.json
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal class HttpService(
    private val logging: Logging,
    clientEngine: HttpClientEngine = Android.create(),
) {
  private val client =
      HttpClient(clientEngine) {
        install(ContentNegotiation) {
          json(
              Json {
                ignoreUnknownKeys = true
                explicitNulls = false
              }
          )
        }
        install(HttpCookies)
        install(LoggingClientPlugin) { this@install.logging = this@HttpService.logging }
      }

  suspend fun get(
      url: Url,
      acceptHeader: ContentType = ContentType.Application.Json,
  ): HttpResponse {
    return client.get(url) { accept(acceptHeader) }
  }

  suspend fun post(
      url: Url,
      session: String,
      body: JsonObject? = null,
      acceptHeader: ContentType = ContentType.Application.Json,
  ): HttpResponse {
    return client.post(url) {
      accept(acceptHeader)
      contentType(ContentType.Application.Json)
      setBody(body)
      headers.apply {
        append("Authorization", "Bearer $session")
        // TODO: this could be configured by integrator
        append("Accept-Language", Locale.getDefault().language)
      }
    }
  }

  suspend fun post(
      url: Url,
      acceptHeader: ContentType = ContentType.Application.Json
  ): HttpResponse {
    return client.post(url) {
      accept(acceptHeader)
      contentType(ContentType.Application.Json)
      setBody(body)
      headers.apply { append("Accept-Language", Locale.getDefault().language) }
    }
  }

  suspend fun postForm(
      url: String,
      body: Parameters,
      acceptHeader: ContentType = ContentType.Application.Json,
  ): HttpResponse {
    return client.submitForm(url = url, formParameters = body) { accept(acceptHeader) }
  }
}

private val LoggingClientPlugin =
    createClientPlugin("SDKLoggingClientPlugin", ::LoggingClientPluginConfig) {
      val logging = pluginConfig.logging
      val redirects =
          setOf(
              HttpStatusCode.MovedPermanently,
              HttpStatusCode.Found,
              HttpStatusCode.SeeOther,
              HttpStatusCode.TemporaryRedirect,
              HttpStatusCode.PermanentRedirect,
          )

      onRequest { request, _ ->
        logging.debug("HTTP Request: ${request.method.value} ${request.url.encodedPath}")
      }

      onResponse { response ->
        val locationHeader = response.headers[HttpHeaders.Location]
        val shouldPrintLocation =
            response.status in redirects && locationHeader != null && locationHeader.isNotBlank()
        if (shouldPrintLocation) {
          val locationUrl = Url(locationHeader)
          logging.debug(
              "HTTP Response: ${response.status.value} for ${response.request.method.value} ${response.request.url.encodedPath}" +
                  "Redirecting: ${locationUrl.protocolWithAuthority}${locationUrl.encodedPath}"
          )
        } else {
          logging.debug(
              "HTTP Response: ${response.status.value} for ${response.request.method.value} ${response.request.url.encodedPath}"
          )
        }

        val eventIdHeader = response.headers["X-Event-ID"]
        if (!eventIdHeader.isNullOrBlank()) {
          logging.debug("X-Event-ID: $eventIdHeader")
        }
      }
    }

internal class LoggingClientPluginConfig {
  lateinit var logging: Logging
}
