package com.benza.smartrooms.data.room.model

/**
 * Metadata for one uploaded attachment shared inside a room announcement.
 */
internal data class RoomAnnouncementAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val downloadUrl: String,
)
