package com.benza.smartrooms.data.room.remote

import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Cloud Functions room wrapper for quiz generation actions.
 */
internal class FirebaseFunctionsRoomDataSource(
    private val functions: FirebaseFunctions
) {
    /**
     * Calls the backend function that generates and stores a quiz for the supplied room.
     */
    internal suspend fun generateQuiz(request: GenerateQuizRequest) {
        functions
            .getHttpsCallable(GENERATE_QUIZ_FUNCTION)
            .call(
                mapOf(
                    ROOM_ID_FIELD to request.roomId,
                    CEFR_LEVEL_FIELD to request.cefrLevel,
                    QUESTION_COUNT_FIELD to request.questionCount,
                    QUESTION_TYPE_FIELD to request.questionType.toBackendValue()
                )
            )
            .await()
    }
}

private fun QuestionType.toBackendValue(): String {
    return when (this) {
        QuestionType.MULTIPLE_CHOICE -> "multiple_choice"
        QuestionType.FILL_IN_BLANK -> "fill_in_blank"
    }
}

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
            }
        }
    }
}

private const val GENERATE_QUIZ_FUNCTION = "generateQuizForRoom"
private const val ROOM_ID_FIELD = "roomId"
private const val CEFR_LEVEL_FIELD = "cefrLevel"
private const val QUESTION_COUNT_FIELD = "questionCount"
private const val QUESTION_TYPE_FIELD = "questionType"
