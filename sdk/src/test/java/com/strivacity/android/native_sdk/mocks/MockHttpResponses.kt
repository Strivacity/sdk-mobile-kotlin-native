package com.strivacity.android.native_sdk.mocks

import TokenResponseBuilder
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.headers
import io.ktor.http.set

fun MockRequestHandleScope.respondFlowRedirect(request: HttpRequestData): HttpResponseData? =
    with(request.url.encodedPath) {
      when {
        startsWith("/oauth2/auth") ->
            respond(
                "",
                HttpStatusCode.Found,
                headers {
                  set(
                      HttpHeaders.Location,
                      "test-scheme://my-test-app/redirUrl?session_id=test-session-id",
                  )
                },
            )

        else -> null
      }
    }

fun MockRequestHandleScope.respondFlowOAuthError(request: HttpRequestData): HttpResponseData? =
    with(request.url.encodedPath) {
      when {
        startsWith("/oauth2/auth") ->
            respond(
                "",
                HttpStatusCode.Found,
                headers {
                  set(
                      HttpHeaders.Location,
                      "https://localhost/oauth2/error?error=test-error&error_description=SomeTestErrorDescription",
                  )
                },
            )
        startsWith("/oauth2/error") ->
            respond(
                "",
                HttpStatusCode.OK,
            )

        else -> null
      }
    }

fun MockRequestHandleScope.respondPostLoginRedirect(
    withCode: String? = null,
    withState: String? = null,
    request: HttpRequestData,
): HttpResponseData? =
    with(request.url.encodedPath) {
      when {
        startsWith("/provider/oauth2/v1/finish") -> {
          val redirectUrl =
              URLBuilder("test-scheme://my-test-app/redirUrl")
                  .apply {
                    set {
                      withCode?.let { parameters["code"] = it }
                      withState?.let { parameters["state"] = it }
                    }
                  }
                  .build()
                  .toString()
          respond(
              "",
              HttpStatusCode.Found,
              headers {
                set(
                    HttpHeaders.Location,
                    redirectUrl,
                )
              },
          )
        }
        else -> null
      }
    }

internal fun MockRequestHandleScope.respondTokenExchange200(
    tokenResponseBuilder: TokenResponseBuilder.() -> Unit,
    request: HttpRequestData,
): HttpResponseData? =
    with(request.url.encodedPath) {
      when {
        startsWith("/oauth2/token") -> {
          val tokenResp = TokenResponseBuilder()
          tokenResp.tokenResponseBuilder()
          respond(
              tokenResp.buildAsString(),
              HttpStatusCode.OK,
              headers { set(HttpHeaders.ContentType, "application/json") },
          )
        }
        else -> null
      }
    }

fun MockRequestHandleScope.respondTokenExchange500(
    tokenResponseBuilder: () -> String,
    request: HttpRequestData,
): HttpResponseData? =
    with(request.url.encodedPath) {
      when {
        startsWith("/oauth2/token") -> {
          respond("", HttpStatusCode.InternalServerError)
        }
        else -> null
      }
    }

fun MockRequestHandleScope.respondTokenExchangeException(
    tokenResponseBuilder: () -> String,
    request: HttpRequestData,
): HttpResponseData? =
    with(request.url.encodedPath) {
      when {
        startsWith("/oauth2/token") -> {
          throw Exception("Test exception")
        }
        else -> null
      }
    }

fun MockRequestHandleScope.respondInit200(
    request: HttpRequestData,
): HttpResponseData? =
    with(request.url.encodedPath) {
      when {
        startsWith("/flow/api/v1/init") -> {
          respond(
              fakeInitResponsePayload,
              HttpStatusCode.OK,
              headers { set(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
          )
        }
        else -> null
      }
    }

const val fakeInitResponsePayload =
    """
{
  "screen": "identification",
  "hostedUrl": "https://localhost/provider/flow?challenge=2198d62e8e12aed-3839-4e46-b1bb-3f7262d09d63&redirect_uri=test-scheme%3A%2F%2Fmy-test-app%2FredirUrl",
  "forms": [
    {
      "type": "form",
      "id": "identifier",
      "widgets": [
        {
          "type": "static",
          "id": "section-title",
          "value": "Sign in"
        },
        {
          "type": "input",
          "id": "identifier",
          "label": "Email address",
          "value": null,
          "readonly": false,
          "autocomplete": "username",
          "inputmode": "email",
          "validator": {
            "minLength": null,
            "maxLength": null,
            "regex": null,
            "required": true
          }
        },
        {
          "type": "submit",
          "id": "submit",
          "label": "Continue"
        }
      ]
    },
    {
      "type": "form",
      "id": "additionalActions/registration",
      "widgets": [
        {
          "type": "static",
          "id": "dont-have-an-account",
          "value": "Don't have an account?"
        },
        {
          "type": "submit",
          "id": "submit",
          "label": "Sign up"
        }
      ]
    }
  ]
}
"""
