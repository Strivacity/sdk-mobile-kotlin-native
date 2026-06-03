package com.strivacity.android.native_sdk.service

import FakeLogging
import com.strivacity.android.native_sdk.HttpError
import com.strivacity.android.native_sdk.NetworkConfiguration
import com.strivacity.android.native_sdk.SessionExpiredError
import com.strivacity.android.native_sdk.mocks.FAKE_INIT_RESPONSE_PAYLOAD
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class LoginHandlerServiceTest {
    @Test
    fun initCall_shouldThrowSessionExpiredError_whenStatusIs403() {
        val service =
            HttpService(
                logging = FakeLogging(),
                networkConfiguration = NetworkConfiguration(),
                MockEngine { respond("", HttpStatusCode.Forbidden) },
                languageTag = "en-US",
            )
        val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")

        assertThrows(SessionExpiredError::class.java) { runBlocking { handlerService.initCall() } }
    }

    @Test
    fun initCall_shouldThrowHttpError_whenStatusIs500() {
        val service =
            HttpService(
                logging = FakeLogging(),
                networkConfiguration = NetworkConfiguration(),
                MockEngine { respond("", HttpStatusCode.InternalServerError) },
                languageTag = "en-US",
            )
        val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")
        assertThrows(HttpError::class.java) { runBlocking { handlerService.initCall() } }
    }

    @Test
    fun submitForm_shouldReturnBody() =
        runTest {
            val service =
                HttpService(
                    logging = FakeLogging(),
                    networkConfiguration = NetworkConfiguration(),
                    MockEngine {
                        respond(
                            FAKE_INIT_RESPONSE_PAYLOAD,
                            HttpStatusCode.OK,
                            headers { set(HttpHeaders.ContentType, "application/json") },
                        )
                    },
                    languageTag = "en-US",
                )
            val handlerService =
                LoginHandlerService(service, "https://localhost/", "test-session-id")
            val screen = handlerService.submitForm("test-form-id", body = mapOf())
        }

    @Test
    fun submitForm_shouldThrowSessionExpiredError_whenStatusIs403() =
        runTest {
            val service =
                HttpService(
                    logging = FakeLogging(),
                    networkConfiguration = NetworkConfiguration(),
                    MockEngine {
                        respond(
                            "",
                            HttpStatusCode.Forbidden,
                        )
                    },
                    languageTag = "en-US",
                )
            val handlerService =
                LoginHandlerService(service, "https://localhost/", "test-session-id")
            assertThrows(SessionExpiredError::class.java) {
                runBlocking { handlerService.submitForm("test-form-id", body = mapOf()) }
            }
        }

    @Test
    fun submitForm_shouldThrowHttpError_whenStatusIs500() {
        val service =
            HttpService(
                logging = FakeLogging(),
                networkConfiguration = NetworkConfiguration(),
                MockEngine {
                    respond(
                        "",
                        HttpStatusCode.InternalServerError,
                    )
                },
                languageTag = "en-US",
            )
        val handlerService = LoginHandlerService(service, "https://localhost/", "test-session-id")
        assertThrows(HttpError::class.java) {
            runBlocking { handlerService.submitForm("test-form-id", body = mapOf()) }
        }
    }
}
