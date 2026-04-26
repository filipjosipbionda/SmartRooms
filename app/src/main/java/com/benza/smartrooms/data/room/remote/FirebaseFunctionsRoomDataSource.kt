package com.benza.smartrooms.data.room.remote

import com.benza.smartrooms.data.room.model.CreateRoomInvitationRequest
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
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

    /**
     * Calls the backend function that validates ownership and creates a pending invitation.
     */
    internal suspend fun createRoomInvitation(request: CreateRoomInvitationRequest) {
        functions
            .getHttpsCallable(SEND_ROOM_INVITATION_FUNCTION)
            .call(
                mapOf(
                    ROOM_ID_FIELD to request.roomId,
                    INVITEE_ID_FIELD to request.inviteeId
                )
            )
            .await()
    }

    /**
     * Calls the backend function that accepts a pending room invitation.
     */
    internal suspend fun acceptRoomInvitation(invitationId: String) {
        functions
            .getHttpsCallable(ACCEPT_ROOM_INVITATION_FUNCTION)
            .call(mapOf(INVITATION_ID_FIELD to invitationId))
            .await()
    }

    /**
     * Calls the backend function that rejects a pending room invitation.
     */
    internal suspend fun rejectRoomInvitation(invitationId: String) {
        functions
            .getHttpsCallable(REJECT_ROOM_INVITATION_FUNCTION)
            .call(mapOf(INVITATION_ID_FIELD to invitationId))
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
        addOnCompleteListener(FIREBASE_TASK_EXECUTOR) { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
            }
        }
    }
}

private val FIREBASE_TASK_EXECUTOR = Dispatchers.IO.asExecutor()

private const val GENERATE_QUIZ_FUNCTION = "generateQuizForRoom"
private const val SEND_ROOM_INVITATION_FUNCTION = "sendRoomInvitation"
private const val ACCEPT_ROOM_INVITATION_FUNCTION = "acceptRoomInvitation"
private const val REJECT_ROOM_INVITATION_FUNCTION = "rejectRoomInvitation"
private const val ROOM_ID_FIELD = "roomId"
private const val INVITEE_ID_FIELD = "inviteeId"
private const val INVITATION_ID_FIELD = "invitationId"
private const val CEFR_LEVEL_FIELD = "cefrLevel"
private const val QUESTION_COUNT_FIELD = "questionCount"
private const val QUESTION_TYPE_FIELD = "questionType"
