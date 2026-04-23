package com.benza.smartrooms.data.room.model

/**
 * Payload used to create a room announcement stored in Firestore.
 */
internal data class CreateAnnouncementRequest(
    val roomId: String,
    val authorId: String,
    val authorName: String,
    val title: String,
    val message: String
)
