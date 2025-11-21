package com.strivacity.android.native_sdk.mocks

import TokenResponseBuilder
import com.strivacity.android.native_sdk.Profile
import com.strivacity.android.native_sdk.Storage
import com.strivacity.android.native_sdk.TestStorage
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration
import kotlinx.serialization.json.Json

/** Sets up storage as in a "clean" state where there is no access token, refresh token */
fun Storage.missingAccessToken() {
  delete("profile")
}

fun Storage.expiredAccessToken() {
  val tokenResponse =
      TokenResponseBuilder()
          .createAsTokenResponse(now = Instant.now().minus(1.days.toJavaDuration()))
  val profile = Profile(tokenResponse)
  storeProfile(profile)
}

fun Storage.validAccessToken() {
  val tokenResponse = TokenResponseBuilder().createAsTokenResponse()
  val profile = Profile(tokenResponse)
  storeProfile(profile)
}

fun Storage.withMissingRefreshToken() {
  val tokenResponse =
      TokenResponseBuilder()
          .createAsTokenResponse(now = Instant.now().minus(1.days.toJavaDuration()))
  val responseWithoutRefreshToken = tokenResponse.copy(refreshToken = null)
  val profile = Profile(responseWithoutRefreshToken)
  storeProfile(profile)
}

fun Storage.storeProfile(profile: Profile) {
  this.set("profile", Json.encodeToString(profile))
}

fun buildTestStorage() = buildTestStorage {}

fun buildTestStorage(block: TestStorage.() -> Unit): Storage {
  val storage = TestStorage()
  storage.block()
  return storage
}
