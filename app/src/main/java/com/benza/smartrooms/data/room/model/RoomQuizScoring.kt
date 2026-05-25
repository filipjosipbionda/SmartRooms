package com.benza.smartrooms.data.room.model

import kotlin.math.roundToInt

internal object RoomQuizScoring {
    const val VERSION = 1

    private const val STANDARD_QUESTION_POINTS = 100
    private const val TIMED_CORRECT_POINTS = 100
    private const val TIMED_SPEED_BONUS_POINTS = 100

    fun maxScore(question: RoomQuizQuestion): Int =
        if (question.timeLimitSeconds == null) {
            STANDARD_QUESTION_POINTS
        } else {
            timedQuestionMaxScore()
        }

    fun maxScore(
        questionCount: Int,
        timedQuestionCount: Int,
    ): Int {
        val timedCount = timedQuestionCount.coerceIn(0, questionCount)
        val standardCount = questionCount - timedCount
        return standardCount * STANDARD_QUESTION_POINTS + timedCount * timedQuestionMaxScore()
    }

    fun score(
        question: RoomQuizQuestion,
        isCorrect: Boolean,
        remainingTimeSeconds: Int?,
        isTimedOut: Boolean,
    ): Int {
        if (!isCorrect || isTimedOut) return 0

        val timeLimitSeconds = question.timeLimitSeconds ?: return STANDARD_QUESTION_POINTS
        val remainingSeconds =
            remainingTimeSeconds
                ?.coerceIn(0, timeLimitSeconds)
                ?: 0
        val speedBonus =
            (remainingSeconds.toFloat() / timeLimitSeconds.toFloat() * TIMED_SPEED_BONUS_POINTS)
                .roundToInt()
        return TIMED_CORRECT_POINTS + speedBonus
    }

    private fun timedQuestionMaxScore(): Int = TIMED_CORRECT_POINTS + TIMED_SPEED_BONUS_POINTS
}
