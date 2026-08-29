package com.benza.smartrooms.data.room.model

/**
 * Minimal generated quiz model shown in the room details screen.
 */
internal data class RoomQuizSummary(
    val id: String,
    val clientRequestId: String = "",
    val title: String,
    val quizKind: QuizKind,
    val topic: String,
    val vocabularyWords: List<String>,
    val cefrLevel: String,
    val questionType: QuestionType,
    val questionCount: Int,
    val hasTimer: Boolean = false,
    val maxScore: Int =
        RoomQuizScoring.maxScore(
            questionCount = questionCount,
            timedQuestionCount = if (hasTimer) questionCount else 0,
        ),
    val status: RoomQuizStatus,
    val failureReason: String? = null,
    val createdAtEpochMillis: Long,
)
