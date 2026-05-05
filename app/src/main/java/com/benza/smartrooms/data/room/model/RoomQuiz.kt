package com.benza.smartrooms.data.room.model

/**
 * Full generated quiz model including all questions required for solving.
 */
internal data class RoomQuiz(
    val id: String,
    val title: String,
    val quizKind: QuizKind,
    val topic: String,
    val vocabularyWords: List<String>,
    val cefrLevel: String,
    val questionType: QuestionType,
    val status: RoomQuizStatus,
    val failureReason: String? = null,
    val questions: List<RoomQuizQuestion>,
    val createdAtEpochMillis: Long,
) {
    val questionCount: Int
        get() = questions.size
}
