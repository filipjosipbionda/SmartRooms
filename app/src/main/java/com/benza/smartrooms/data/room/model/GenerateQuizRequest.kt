package com.benza.smartrooms.data.room.model

/**
 * Payload used to request server-side quiz generation for a room.
 */
internal data class GenerateQuizRequest(
    val roomId: String,
    val cefrLevel: String,
    val questionCount: Int,
    val questionType: QuestionType
)
