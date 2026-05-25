package com.benza.smartrooms.data.room.model

/**
 * Completed quiz attempt for one user.
 */
internal data class RoomQuizResult(
    val roomId: String,
    val quizId: String,
    val userId: String,
    val scoringVersion: Int,
    val answeredQuestionCount: Int,
    val questionCount: Int,
    val score: Int,
    val maxScore: Int,
    val questionResults: List<RoomQuizQuestionResult>,
    val completedAtEpochMillis: Long,
)

internal data class RoomQuizQuestionResult(
    val questionId: String,
    val isAnswered: Boolean,
    val isCorrect: Boolean,
    val score: Int,
    val maxScore: Int,
    val timeLimitSeconds: Int?,
    val remainingTimeSeconds: Int?,
)
