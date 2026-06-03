package com.strivacity.android.native_sdk.service

import FakeLogging
import com.strivacity.android.native_sdk.NetworkConfiguration
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.toMap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HttpServiceTest {
    @Test
    fun customHeaders_areAttachedToGetPostRequest() {
        val capturedHeaders = mutableListOf<Map<String, List<String>>>()

        val service =
            HttpService(
                logging = FakeLogging(),
                networkConfiguration =
                    NetworkConfiguration(customRequestHeaders = mapOf("x-sty-foo" to "bar")),
                MockEngine { request ->
                    capturedHeaders.add(request.headers.toMap())
                    respond("", HttpStatusCode.OK)
                },
                languageTag = "en-US",
            )

        runBlocking { service.get(Url("https://localhost/test")) }
        assertEquals("bar", capturedHeaders.first()["x-sty-foo"]?.first())
        capturedHeaders.clear()

        runBlocking { service.post(Url("https://localhost/test"), session = "test-session") }
        assertEquals("bar", capturedHeaders.first()["x-sty-foo"]?.first())
    }

    @Test
    fun noCustomHeaders_getRequest_doesNotContainExtraHeaders() {
        val capturedHeaders = mutableListOf<Map<String, List<String>>>()

        val service =
            HttpService(
                logging = FakeLogging(),
                networkConfiguration = NetworkConfiguration(),
                MockEngine { request ->
                    capturedHeaders.add(request.headers.toMap())
                    respond("", HttpStatusCode.OK)
                },
                languageTag = "en-US",
            )

        runBlocking { service.get(Url("https://localhost/test")) }

        assertNull(capturedHeaders.first()["x-sty-foo"])
    }
}
