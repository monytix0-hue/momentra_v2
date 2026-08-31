package com.example.momentra.data.api

/**
 * Maps HTTP/API failures to machine-readable codes.
 * Do not parse English error strings for control flow.
 */
sealed class ApiResultException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Unauthenticated(message: String = "UNAUTHENTICATED") : ApiResultException(message)
    class Forbidden(val code: String = "GOVERNANCE_DENIED", message: String = "FORBIDDEN") : ApiResultException(message)
    class NotFound(message: String = "NOT_FOUND") : ApiResultException(message)
    class Conflict(val code: String, message: String) : ApiResultException(message)
    class Validation(val code: String = "VALIDATION_FAILED", message: String) : ApiResultException(message)
    class RateLimited(message: String = "RATE_LIMITED") : ApiResultException(message)
    class Server(message: String = "INFRASTRUCTURE_UNAVAILABLE") : ApiResultException(message)
    class Network(message: String = "NETWORK_UNAVAILABLE", cause: Throwable? = null) : ApiResultException(message, cause)
}

fun mapHttpFailure(code: Int, errorCode: String?, message: String?): ApiResultException =
    when (code) {
        401 -> ApiResultException.Unauthenticated(errorCode ?: "UNAUTHORIZED")
        403 -> ApiResultException.Forbidden(errorCode ?: "GOVERNANCE_DENIED", message ?: "FORBIDDEN")
        404 -> ApiResultException.NotFound(errorCode ?: "RESOURCE_NOT_FOUND")
        409 -> ApiResultException.Conflict(errorCode ?: "CONFLICT", message ?: "Conflict")
        422, 400 -> ApiResultException.Validation(errorCode ?: "VALIDATION_FAILED", message ?: "Validation failed")
        429 -> ApiResultException.RateLimited(errorCode ?: "RATE_LIMITED")
        in 500..599 -> ApiResultException.Server(errorCode ?: "INFRASTRUCTURE_UNAVAILABLE")
        else -> ApiResultException.Server(message ?: "Unexpected status $code")
    }
