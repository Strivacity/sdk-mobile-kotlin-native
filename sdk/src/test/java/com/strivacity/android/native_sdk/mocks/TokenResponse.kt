import com.strivacity.android.native_sdk.service.TokenResponse
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import kotlinx.serialization.json.Json

internal class TokenResponseBuilder(
    var nonce: String = "YigFv8tTlOJRsSGGSURZiw",
    var iss: String = "https://localhost/",
    var aud: String = "[\"test_client\"]",
) {
  fun createAsTokenResponse(now: Instant = Instant.now()): TokenResponse =
      decode(buildAsString(now))

  fun buildAsString(now: Instant = Instant.now()): String =
      """
{
    "access_token": "test_access_token",
    "id_token": "${fakeIdTokenJwt(now)}",
    "refresh_token": "${Random.nextInt(1000000, 9999999).toString(36)}",
    "expires_in": 3600
}
"""

  fun decode(encoded: String): TokenResponse = Json.decodeFromString(encoded)

  private fun fakeIdToken(now: Instant): String {
    return """
{
  "at_hash": "access_token_hash",
  "aud": $aud,
  "auth_time": 1762940826,
  "client_id": "test_client",
  "email": null,
  "exp": ${now.plus(1.hours.toJavaDuration()).epochSecond},
  "iat": ${now.epochSecond},
  "iss": "$iss",
  "jti": "2e1456b8-3c1b-4b38-b401-cee4acfaeeee",
  "nonce": "$nonce",
  "rat": ${now.epochSecond},
  "sid": "0d4f4af2-64e5-492d-b7f2-69d09fc14aaa",
  "sub": "aaaaaaaa-b8cd-4edc-8e8e-c0c0d5ec3aaa",
  "user_id": "aaaaaaaa-b8cd-4edc-8e8e-c0c0d5ec3aaa"
}
"""
  }

  private fun fakeIdTokenJwt(now: Instant): String {
    val header =
        "eyJhbGciOiJSUzI1NiIsImtpZCI6InByaXZhdGU6cGhhREJMWDhhalQ5clNQYkdwRk1fT0FjYjhrQlpDVlVleWtKSjlwVWFoOCIsInR5cCI6IkpXVCJ9"
    val payload = encodeJwtPart(fakeIdToken(now))
    val signature = "fake_sig"
    return "$header.$payload.$signature"
  }
}
