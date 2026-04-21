package com.benza.smartrooms.data.room.model

/**
 * Result wrapper for room operations that either return domain data or a UI message.
 */
internal sealed interface RoomOperationResult<out T> {
    /**
     * Successful room operation carrying the requested payload.
     */
    data class Success<T>(val data: T) : RoomOperationResult<T>

    /**
     * Failed room operation represented by a user-facing string resource id.
     */
    data class Error(val messageRes: Int) : RoomOperationResult<Nothing>
}
