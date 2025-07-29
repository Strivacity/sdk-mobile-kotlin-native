package com.strivacity.android.native_sdk.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

internal object OIDCParamGenerator {
  fun generateRandomString(byteLengths: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(byteLengths)
    random.nextBytes(bytes)

    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  @Throws(NoSuchAlgorithmException::class)
  fun generateCodeChallenge(codeVerifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(codeVerifier.toByteArray(StandardCharsets.UTF_8))

    return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }
}
