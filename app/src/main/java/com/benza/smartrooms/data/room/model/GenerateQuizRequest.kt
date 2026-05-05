package com.benza.smartrooms.data.room.model

/**
 * Payload used to request server-side quiz generation for a room.
 */
internal data class GenerateQuizRequest(
    val clientRequestId: String,
    val roomId: String,
    val title: String,
    val quizKind: QuizKind,
    val topic: String,
    val vocabularyWords: List<String>,
    val cefrLevel: String,
    val questionCount: Int,
    val questionType: QuestionType,
    val questionTimeLimitSeconds: Int?,
)
