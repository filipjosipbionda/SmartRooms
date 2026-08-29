package com.benza.smartrooms.data.room.model

/**
 * Payload used to create a comment under a room announcement.
 */
internal data class CreateRoomCommentRequest(
    val roomId: String,
    val announcementId: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val message: String,
)
