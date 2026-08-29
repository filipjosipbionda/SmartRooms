package com.benza.smartrooms.data.room.model

/**
 * Typed quiz question model used by the quiz-solving screen.
 */
internal sealed interface RoomQuizQuestion {
    val id: String
    val prompt: String
    val explanation: String
    val timeLimitSeconds: Int?
    val type: QuestionType

    data class MultipleChoice(
        override val id: String,
        override val prompt: String,
        override val explanation: String,
        override val timeLimitSeconds: Int?,
        val options: List<String>,
        val correctOptionIndex: Int,
    ) : RoomQuizQuestion {
        override val type: QuestionType = QuestionType.MULTIPLE_CHOICE
    }

    data class FillInBlank(
        override val id: String,
        override val prompt: String,
        override val explanation: String,
        override val timeLimitSeconds: Int?,
        val answerText: String,
    ) : RoomQuizQuestion {
        override val type: QuestionType = QuestionType.FILL_IN_BLANK
    }

    data class WordScramble(
        override val id: String,
        override val prompt: String,
        override val explanation: String,
        override val timeLimitSeconds: Int?,
        val answerWord: String,
        val shuffledLetters: List<String>,
    ) : RoomQuizQuestion {
        override val type: QuestionType = QuestionType.WORD_SCRAMBLE
    }
}
