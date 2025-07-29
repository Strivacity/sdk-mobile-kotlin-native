package com.strivacity.android.native_sdk

sealed class Error : Throwable() {}

class OidcError(val error: String, val errorDescription: String?) : Error()

class HostedFlowCanceledError() : Error()

class UnknownError(exception: Exception) : Error()

class InvalidCallbackError(reason: String) : Error()

class HttpError(statusCode: Int) : Error()

class SessionExpiredError : Error()
