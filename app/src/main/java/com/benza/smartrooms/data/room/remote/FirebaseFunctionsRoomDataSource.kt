package com.benza.smartrooms.data.room.remote

import com.benza.smartrooms.data.room.model.CreateRoomInvitationRequest
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.QuizKind
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Cloud Functions room wrapper for quiz generation actions.
 */
internal class FirebaseFunctionsRoomDataSource(
    private val functions: FirebaseFunctions,
) {
    /**
     * Calls the backend function that generates and stores a quiz for the supplied room.
     */
    internal suspend fun generateQuiz(request: GenerateQuizRequest) {
        functions
            .getHttpsCallable(GENERATE_QUIZ_FUNCTION)
            .withQuizGenerationTimeout()
            .call(
                mapOf(
                    CLIENT_REQUEST_ID_FIELD to request.clientRequestId,
                    ROOM_ID_FIELD to request.roomId,
                    QUIZ_TITLE_FIELD to request.title,
                    QUIZ_KIND_FIELD to request.quizKind.toBackendValue(),
                    QUIZ_TOPIC_FIELD to request.topic,
                    VOCABULARY_WORDS_FIELD to request.vocabularyWords,
                    CEFR_LEVEL_FIELD to request.cefrLevel,
                    QUESTION_COUNT_FIELD to request.questionCount,
                    QUESTION_TYPE_FIELD to request.questionType.toBackendValue(),
                    QUESTION_TIME_LIMIT_SECONDS_FIELD to request.questionTimeLimitSeconds,
                ),
            ).await()
    }

    /**
     * Calls the backend function that retries generation for an existing failed quiz.
     */
    internal suspend fun retryQuizGeneration(
        roomId: String,
        quizId: String,
    ) {
        functions
            .getHttpsCallable(RETRY_QUIZ_FUNCTION)
            .withQuizGenerationTimeout()
            .call(
                mapOf(
                    ROOM_ID_FIELD to roomId,
                    QUIZ_ID_FIELD to quizId,
                ),
            ).await()
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
                    INVITEE_ID_FIELD to request.inviteeId,
                ),
            ).await()
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

    /**
     * Calls the backend function that removes one user from the selected room.
     */
    internal suspend fun removeRoomMember(
        roomId: String,
        targetUserId: String,
    ) {
        functions
            .getHttpsCallable(REMOVE_ROOM_MEMBER_FUNCTION)
            .call(
                mapOf(
                    ROOM_ID_FIELD to roomId,
                    TARGET_USER_ID_FIELD to targetUserId,
                ),
            ).await()
    }
}

private fun QuestionType.toBackendValue(): String =
    when (this) {
        QuestionType.MULTIPLE_CHOICE -> "multiple_choice"
        QuestionType.FILL_IN_BLANK -> "fill_in_blank"
        QuestionType.MIXED -> "mixed"
        QuestionType.WORD_SCRAMBLE -> "word_scramble"
    }

private fun QuizKind.toBackendValue(): String =
    when (this) {
        QuizKind.GRAMMAR -> "grammar"
        QuizKind.VOCABULARY -> "vocabulary"
    }

private fun com.google.firebase.functions.HttpsCallableReference.withQuizGenerationTimeout() =
    apply { setTimeout(QUIZ_GENERATION_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS) }

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener(FIREBASE_TASK_EXECUTOR) { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
            }
        }
    }

private val FIREBASE_TASK_EXECUTOR = Dispatchers.IO.asExecutor()

private const val GENERATE_QUIZ_FUNCTION = "generateQuizForRoom"
private const val RETRY_QUIZ_FUNCTION = "retryQuizForRoom"
private const val QUIZ_GENERATION_TIMEOUT_SECONDS = 300
private const val SEND_ROOM_INVITATION_FUNCTION = "sendRoomInvitation"
private const val ACCEPT_ROOM_INVITATION_FUNCTION = "acceptRoomInvitation"
private const val REJECT_ROOM_INVITATION_FUNCTION = "rejectRoomInvitation"
private const val REMOVE_ROOM_MEMBER_FUNCTION = "removeRoomMember"
private const val CLIENT_REQUEST_ID_FIELD = "clientRequestId"
private const val ROOM_ID_FIELD = "roomId"
private const val QUIZ_ID_FIELD = "quizId"
private const val QUIZ_TITLE_FIELD = "title"
private const val INVITEE_ID_FIELD = "inviteeId"
private const val INVITATION_ID_FIELD = "invitationId"
private const val TARGET_USER_ID_FIELD = "targetUserId"
private const val QUIZ_KIND_FIELD = "quizKind"
private const val QUIZ_TOPIC_FIELD = "topic"
private const val VOCABULARY_WORDS_FIELD = "vocabularyWords"
private const val CEFR_LEVEL_FIELD = "cefrLevel"
private const val QUESTION_COUNT_FIELD = "questionCount"
private const val QUESTION_TYPE_FIELD = "questionType"
private const val QUESTION_TIME_LIMIT_SECONDS_FIELD = "questionTimeLimitSeconds"
