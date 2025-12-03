package com.strivacity.android.native_sdk

/**
 * Base class for all errors in the SDK.
 */
sealed class Error(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {
  constructor(message: String) : this(message, null)
}

class OidcError(val error: String, val errorDescription: String?) : Error()

class HostedFlowCanceledError() : Error()

class UnknownError(val exception: Exception) : Error()

class InvalidCallbackError(val reason: String) : Error()

class HttpError(val statusCode: Int) : Error(message = "HTTP Error with status code $statusCode") {}

class SessionExpiredError : Error()
