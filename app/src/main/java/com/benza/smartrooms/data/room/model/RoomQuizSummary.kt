package com.benza.smartrooms.data.room.model

/**
 * Minimal generated quiz model shown in the room details screen.
 */
internal data class RoomQuizSummary(
    val id: String,
    val title: String,
    val cefrLevel: String,
    val questionType: QuestionType,
    val questionCount: Int,
    val createdAtEpochMillis: Long
)
