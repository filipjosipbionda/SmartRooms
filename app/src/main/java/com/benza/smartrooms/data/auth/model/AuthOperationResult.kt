package com.benza.smartrooms.data.auth.model

/**
 * Result wrapper for auth operations that either return domain data or a string resource error.
 */
internal sealed interface AuthOperationResult<out T> {
    /**
     * Successful auth result carrying the requested domain payload.
     */
    data class Success<T>(
        val data: T,
    ) : AuthOperationResult<T>

    /**
     * Failed auth result represented by a user-facing string resource id.
     */
    data class Error(
        val messageRes: Int,
    ) : AuthOperationResult<Nothing>
}
