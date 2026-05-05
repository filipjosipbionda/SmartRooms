package com.benza.smartrooms.data.room.model

/**
 * Lifecycle state for a generated quiz document in Firestore.
 */
internal enum class RoomQuizStatus {
    GENERATING,
    REVIEW,
    READY,
    FAILED,
}
