package com.benza.smartrooms.data.userprofile.model

/**
 * Result wrapper for user profile operations.
 */
internal sealed interface UserProfileOperationResult<out T> {
    data class Success<T>(
        val data: T,
    ) : UserProfileOperationResult<T>

    data class Error(
        val messageRes: Int,
    ) : UserProfileOperationResult<Nothing>
}
