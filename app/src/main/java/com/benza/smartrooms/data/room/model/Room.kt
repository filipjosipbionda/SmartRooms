package com.benza.smartrooms.data.room.model

/**
 * Minimal room model shown on the authenticated home screen.
 */
internal data class Room(
    val id: String,
    val name: String,
    val topic: String,
    val participantCount: Int,
    val unansweredQuizCount: Int,
    val ownerId: String,
    val memberIds: List<String> = emptyList(),
    val collaboratorIds: List<String> = emptyList(),
    val createdAtEpochMillis: Long
)
