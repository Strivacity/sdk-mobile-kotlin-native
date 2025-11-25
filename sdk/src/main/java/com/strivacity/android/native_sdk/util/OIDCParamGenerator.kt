package com.strivacity.android.native_sdk.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

internal object OIDCParamGenerator {
  fun generateRandomString(byteLengths: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(byteLengths)
    random.nextBytes(bytes)
    // migrate to Kotlin.io.encoding once 2.2 is used
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  @Throws(NoSuchAlgorithmException::class)
  fun generateCodeChallenge(codeVerifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(codeVerifier.toByteArray(StandardCharsets.UTF_8))
    // migrate to Kotlin.io.encoding once 2.2 is used
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
  }
}
