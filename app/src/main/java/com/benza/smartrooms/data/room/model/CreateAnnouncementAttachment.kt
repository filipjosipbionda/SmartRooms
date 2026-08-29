package com.benza.smartrooms.data.room.model

/**
 * Local attachment selected for a room announcement before upload.
 */
internal data class CreateAnnouncementAttachment(
    val uriString: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
)
