package com.benza.smartrooms.data.room.model

/**
 * Minimal room announcement model shown in the room feed.
 */
internal data class RoomAnnouncement(
    val id: String,
    val title: String,
    val message: String,
    val authorName: String,
    val createdAtEpochMillis: Long
)
