package com.strivacity.android.native_sdk

import java.util.Arrays
import java.util.function.Predicate

/**
 * Base class for all errors in the SDK.
 */
sealed class Error(
    message: String? = null,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    constructor(message: String) : this(message, null)
}

open class OidcError(
    val error: String,
    val errorDescription: String?,
) : Error()

class TokenRefreshOidcError(
    error: String,
    errorDescription: String?,
) : OidcError(error, errorDescription) {
    override val message: String =
        "Token refresh failed — error: $error, description: \"${errorDescription ?: ""}\", session cleared"
}

class HostedFlowCanceledError : Error()

class UnknownError(
    val exception: Exception,
) : Error()

class InvalidCallbackError(
    val reason: String,
) : Error()

class HttpError(
    val statusCode: Int,
) : Error(message = "HTTP Error with status code $statusCode")

class SessionExpiredError : Error()

/**
 * General error class for exceptions thrown by Platform.
 * Example: Passkey enrollment error
 *
 * [cause] can be used to analyze the exact reason for the error raised
 */
class PlatformError(
    message: String,
    cause: Throwable? = null,
) : Error(message, cause)

class WorkflowError(
    error: String,
    errorDescription: String?,
) : OidcError(error, errorDescription) {
    enum class WorkflowErrorId(
        private val id: String,
    ) {
        MAGIC_LINK_EXPIRED("magicLinkExpired"),
        CLIENT_MISMATCH("clientMismatch"),
        INVALID_REDIRECT_URI("invalidRedirectUri"),
        ;

        companion object {
            fun valueOfId(id: String): WorkflowErrorId? =
                Arrays
                    .stream(entries.toTypedArray())
                    .filter(byId(id))
                    .findFirst()
                    .orElse(null)

            /**
             * Matcher used to filter stream by WorkflowError id
             * @param idParam id to match against
             * @return predicate that returns true if workflowError id matches, false otherwise
             */
            private fun byId(idParam: String): Predicate<WorkflowErrorId> =
                Predicate { workflowError: WorkflowErrorId ->
                    workflowError.id == idParam
                }
        }
    }
}
