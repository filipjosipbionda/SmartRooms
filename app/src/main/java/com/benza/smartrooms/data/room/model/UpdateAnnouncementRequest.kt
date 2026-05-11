package com.benza.smartrooms.data.room.model

/**
 * Payload used to update an existing room announcement and its attachment set.
 */
internal data class UpdateAnnouncementRequest(
    val roomId: String,
    val announcementId: String,
    val title: String,
    val message: String,
    val existingAttachments: List<RoomAnnouncementAttachment>,
    val newAttachments: List<CreateAnnouncementAttachment> = emptyList(),
    val removedAttachments: List<RoomAnnouncementAttachment> = emptyList(),
)
