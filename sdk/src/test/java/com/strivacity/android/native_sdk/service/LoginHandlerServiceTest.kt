package com.strivacity.android.native_sdk.service

import com.strivacity.android.native_sdk.HttpError
import com.strivacity.android.native_sdk.SessionExpiredError
import com.strivacity.android.native_sdk.mocks.fakeInitResponsePayload
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginHandlerServiceTest {
  @Test
  fun initCall_shouldThrowSessionExpiredError_whenStatusIs403() {
    val service = HttpService(MockEngine { respond("", HttpStatusCode.Forbidden) })
    val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")

    assertThrows(SessionExpiredError::class.java) { runBlocking { handlerService.initCall() } }
  }

  @Test
  fun initCall_shouldThrowHttpError_whenStatusIs500() = runTest {
    val service = HttpService(MockEngine { respond("", HttpStatusCode.InternalServerError) })
    val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")
    lateinit var error: Throwable
    try {
      handlerService.initCall()
    } catch (e: Throwable) {
      error = e
    }
    assertTrue("Is a SessionExpiredError", error is HttpError)
  }

  @Test
  fun submitForm_shouldReturnBody() = runTest {
    val service =
        HttpService(
            MockEngine {
              respond(
                  fakeInitResponsePayload,
                  HttpStatusCode.OK,
                  headers { set(HttpHeaders.ContentType, "application/json") },
              )
            }
        )
    val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")
    val screen = handlerService.submitForm("test-form-id", body = mapOf())
  }

  @Test
  fun submitForm_shouldThrowSessionExpiredError_whenStatusIs403() = runTest {
    val service =
        HttpService(
            MockEngine {
              respond(
                  "",
                  HttpStatusCode.Forbidden,
              )
            }
        )
    val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")
    lateinit var error: Throwable
    try {
      handlerService.submitForm("test-form-id", body = mapOf())
    } catch (e: Throwable) {
      error = e
    }
    assertTrue("Is a SessionExpiredError", error is SessionExpiredError)
  }

  @Test
  fun submitForm_shouldThrowHttpError_whenStatusIs500() = runTest {
    val service =
        HttpService(
            MockEngine {
              respond(
                  "",
                  HttpStatusCode.InternalServerError,
              )
            }
        )
    val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")
    lateinit var error: Throwable
    try {
      handlerService.submitForm("test-form-id", body = mapOf())
    } catch (e: Throwable) {
      error = e
    }
    assertTrue("Is a HttpError", error is HttpError)
  }
}
