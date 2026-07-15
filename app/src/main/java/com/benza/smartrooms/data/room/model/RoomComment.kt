package com.benza.smartrooms.data.room.model

/**
 * One comment attached to a room announcement.
 */
internal data class RoomComment(
    val id: String,
    val roomId: String,
    val announcementId: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val message: String,
    val createdAtEpochMillis: Long,
)

/**
 * Denormalized preview stored on the parent announcement for lightweight feed rendering.
 */
internal data class RoomCommentPreview(
    val id: String,
    val authorName: String,
    val message: String,
    val createdAtEpochMillis: Long,
)
