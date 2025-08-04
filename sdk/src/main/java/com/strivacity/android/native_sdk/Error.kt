package com.strivacity.android.native_sdk

sealed class Error : Throwable() {}

class OidcError(val error: String, val errorDescription: String?) : Error()

class HostedFlowCanceledError() : Error()

class UnknownError(val exception: Exception) : Error()

class InvalidCallbackError(val reason: String) : Error()

class HttpError(val statusCode: Int) : Error()

class SessionExpiredError : Error()
