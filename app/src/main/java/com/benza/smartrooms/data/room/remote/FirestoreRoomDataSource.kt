package com.benza.smartrooms.data.room.remote

import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.QuizKind
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.RoomInvitation
import com.benza.smartrooms.data.room.model.RoomInvitationAccess
import com.benza.smartrooms.data.room.model.RoomQuiz
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firestore room wrapper that exposes suspend and Flow-friendly primitives.
 */
internal class FirestoreRoomDataSource(
    private val firestore: FirebaseFirestore,
) {
    /**
     * Observes all rooms owned by the supplied user.
     */
    internal fun observeRooms(ownerId: String): Flow<List<Room>> =
        callbackFlow {
            val registration =
                firestore
                    .collection(ROOMS_COLLECTION)
                    .whereEqualTo(OWNER_ID_FIELD, ownerId)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val rooms =
                            snapshot
                                ?.documents
                                .orEmpty()
                                .mapNotNull(DocumentSnapshot::toRoom)
                                .sortedByDescending(Room::createdAtEpochMillis)

                        trySend(rooms)
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Observes a single room document.
     */
    internal fun observeRoom(roomId: String): Flow<Room> =
        callbackFlow {
            val registration =
                firestore
                    .collection(ROOMS_COLLECTION)
                    .document(roomId)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val room = snapshot?.toRoom()
                        if (room != null) {
                            trySend(room)
                        }
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Observes rooms where the supplied user is listed in memberIds.
     */
    internal fun observeMemberRooms(userId: String): Flow<List<Room>> =
        callbackFlow {
            val registration =
                firestore
                    .collection(ROOMS_COLLECTION)
                    .whereArrayContains(MEMBER_IDS_FIELD, userId)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val rooms =
                            snapshot
                                ?.documents
                                .orEmpty()
                                .mapNotNull(DocumentSnapshot::toRoom)
                                .sortedByDescending(Room::createdAtEpochMillis)

                        trySend(rooms)
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Observes rooms where the supplied professor is listed in collaboratorIds.
     */
    internal fun observeCollaboratingRooms(userId: String): Flow<List<Room>> =
        callbackFlow {
            val registration =
                firestore
                    .collection(ROOMS_COLLECTION)
                    .whereArrayContains(COLLABORATOR_IDS_FIELD, userId)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val rooms =
                            snapshot
                                ?.documents
                                .orEmpty()
                                .mapNotNull(DocumentSnapshot::toRoom)
                                .sortedByDescending(Room::createdAtEpochMillis)

                        trySend(rooms)
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Observes pending invitations for the supplied user.
     */
    internal fun observePendingRoomInvitations(userId: String): Flow<List<RoomInvitation>> =
        callbackFlow {
            val registration =
                firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ROOM_INVITATIONS_COLLECTION)
                    .whereEqualTo(INVITATION_STATUS_FIELD, PENDING_INVITATION_STATUS)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val invitations =
                            snapshot
                                ?.documents
                                .orEmpty()
                                .mapNotNull(DocumentSnapshot::toRoomInvitation)
                                .sortedByDescending(RoomInvitation::createdAtEpochMillis)

                        trySend(invitations)
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Observes generated quizzes inside the supplied room.
     */
    internal fun observeQuizzes(roomId: String): Flow<List<RoomQuizSummary>> =
        callbackFlow {
            val registration =
                firestore
                    .collection(ROOMS_COLLECTION)
                    .document(roomId)
                    .collection(QUIZZES_COLLECTION)
                    .orderBy(QUIZ_CREATED_AT_EPOCH_FIELD, Query.Direction.DESCENDING)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val quizzes =
                            snapshot
                                ?.documents
                                .orEmpty()
                                .mapNotNull(DocumentSnapshot::toQuizSummary)

                        trySend(quizzes)
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Observes one generated quiz including all typed questions.
     */
    internal fun observeQuiz(
        roomId: String,
        quizId: String,
    ): Flow<RoomQuiz> =
        callbackFlow {
            val registration =
                firestore
                    .collection(ROOMS_COLLECTION)
                    .document(roomId)
                    .collection(QUIZZES_COLLECTION)
                    .document(quizId)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        if (snapshot == null || !snapshot.exists()) {
                            close(IllegalStateException("Quiz not found"))
                            return@addSnapshotListener
                        }

                        val quiz = snapshot.toQuiz()
                        if (quiz != null) {
                            trySend(quiz)
                        }
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Observes announcements inside the supplied room.
     */
    internal fun observeAnnouncements(roomId: String): Flow<List<RoomAnnouncement>> =
        callbackFlow {
            val registration =
                firestore
                    .collection(ROOMS_COLLECTION)
                    .document(roomId)
                    .collection(ANNOUNCEMENTS_COLLECTION)
                    .orderBy(ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD, Query.Direction.DESCENDING)
                    .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val announcements =
                            snapshot
                                ?.documents
                                .orEmpty()
                                .mapNotNull(DocumentSnapshot::toAnnouncement)

                        trySend(announcements)
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Creates a new room document in Firestore.
     */
    internal suspend fun createRoom(request: CreateRoomRequest) {
        val roomDocument = firestore.collection(ROOMS_COLLECTION).document()
        val now = System.currentTimeMillis()

        roomDocument
            .set(
                mapOf(
                    ID_FIELD to roomDocument.id,
                    NAME_FIELD to request.name,
                    TOPIC_FIELD to request.topic,
                    CEFR_LEVEL_FIELD to request.cefrLevel,
                    PARTICIPANT_COUNT_FIELD to 1,
                    UNANSWERED_QUIZ_COUNT_FIELD to 0,
                    OWNER_ID_FIELD to request.ownerId,
                    OWNER_NAME_FIELD to request.ownerName,
                    MEMBER_IDS_FIELD to listOf(request.ownerId),
                    COLLABORATOR_IDS_FIELD to emptyList<String>(),
                    CREATED_AT_FIELD to now,
                ),
            ).await()
    }

    /**
     * Creates a new room announcement document in Firestore.
     */
    internal suspend fun createAnnouncement(request: CreateAnnouncementRequest) {
        val announcementDocument =
            firestore
                .collection(ROOMS_COLLECTION)
                .document(request.roomId)
                .collection(ANNOUNCEMENTS_COLLECTION)
                .document()

        announcementDocument
            .set(
                mapOf(
                    ID_FIELD to announcementDocument.id,
                    ANNOUNCEMENT_TITLE_FIELD to request.title,
                    ANNOUNCEMENT_MESSAGE_FIELD to request.message,
                    ANNOUNCEMENT_AUTHOR_ID_FIELD to request.authorId,
                    ANNOUNCEMENT_AUTHOR_NAME_FIELD to request.authorName,
                    ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD to System.currentTimeMillis(),
                ),
            ).await()
    }

    /**
     * Publishes a reviewed quiz so it becomes available for solving.
     */
    internal suspend fun publishQuiz(
        roomId: String,
        quizId: String,
    ) {
        firestore
            .collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(QUIZZES_COLLECTION)
            .document(quizId)
            .update(
                mapOf(
                    QUIZ_STATUS_FIELD to READY_QUIZ_STATUS,
                    UPDATED_AT_FIELD to System.currentTimeMillis(),
                ),
            ).await()
    }

    /**
     * Deletes one quiz document from the selected room.
     */
    internal suspend fun deleteQuiz(
        roomId: String,
        quizId: String,
    ) {
        firestore
            .collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(QUIZZES_COLLECTION)
            .document(quizId)
            .delete()
            .await()
    }

    /**
     * Deletes multiple quiz documents from the selected room in one batch.
     */
    internal suspend fun deleteQuizzes(
        roomId: String,
        quizIds: Collection<String>,
    ) {
        if (quizIds.isEmpty()) return
        val batch = firestore.batch()
        quizIds.forEach { quizId ->
            batch.delete(
                firestore
                    .collection(ROOMS_COLLECTION)
                    .document(roomId)
                    .collection(QUIZZES_COLLECTION)
                    .document(quizId),
            )
        }
        batch.commit().await()
    }

    /**
     * Replaces one question inside the selected quiz document.
     */
    internal suspend fun updateQuizQuestion(
        roomId: String,
        quizId: String,
        question: RoomQuizQuestion,
    ) {
        val quizDocument =
            firestore
                .collection(ROOMS_COLLECTION)
                .document(roomId)
                .collection(QUIZZES_COLLECTION)
                .document(quizId)
        val snapshot = quizDocument.get().await()
        val updatedQuestions =
            snapshot
                .getQuestionList(QUESTIONS_FIELD)
                .map { it.toMutableMap<Any?, Any?>() }
                .toMutableList()
        val targetIndex = updatedQuestions.indexOfFirst { (it[ID_FIELD] as? String) == question.id }

        if (targetIndex == -1) {
            throw IllegalStateException("Question not found")
        }

        updatedQuestions[targetIndex] = question.toFirestoreMap().toMutableMap()
        quizDocument
            .update(
                mapOf(
                    QUESTIONS_FIELD to updatedQuestions,
                    QUIZ_QUESTION_COUNT_FIELD to updatedQuestions.size,
                    UPDATED_AT_FIELD to System.currentTimeMillis(),
                ),
            ).await()
    }

    /**
     * Deletes one question from the selected quiz document.
     */
    internal suspend fun deleteQuizQuestion(
        roomId: String,
        quizId: String,
        questionId: String,
    ) {
        val quizDocument =
            firestore
                .collection(ROOMS_COLLECTION)
                .document(roomId)
                .collection(QUIZZES_COLLECTION)
                .document(quizId)
        val snapshot = quizDocument.get().await()
        val updatedQuestions =
            snapshot
                .getQuestionList(QUESTIONS_FIELD)
                .map { it.toMutableMap<Any?, Any?>() }
                .filterNot { (it[ID_FIELD] as? String) == questionId }

        quizDocument
            .update(
                mapOf(
                    QUESTIONS_FIELD to updatedQuestions,
                    QUIZ_QUESTION_COUNT_FIELD to updatedQuestions.size,
                    UPDATED_AT_FIELD to System.currentTimeMillis(),
                ),
            ).await()
    }
}

private fun DocumentSnapshot.toQuizSummary(): RoomQuizSummary? {
    val title = getString(QUIZ_TITLE_FIELD) ?: return null
    val quizKind = getString(QUIZ_KIND_FIELD).toQuizKind()
    val topic = getString(QUIZ_TOPIC_FIELD).orEmpty().ifBlank { DEFAULT_TOPIC }
    val questionCount = (getLong(QUIZ_QUESTION_COUNT_FIELD) ?: 0L).toInt()
    val hasGeneratedContent = getQuestionList(QUESTIONS_FIELD).isNotEmpty()
    val status = getString(QUIZ_STATUS_FIELD).toQuizStatus()

    return RoomQuizSummary(
        id = id,
        clientRequestId = getString(QUIZ_CLIENT_REQUEST_ID_FIELD).orEmpty(),
        title =
            title.normalizeQuizTitle(
                hasGeneratedContent = hasGeneratedContent,
                quizKind = quizKind,
                topic = topic,
            ),
        quizKind = quizKind,
        topic = topic,
        vocabularyWords = getStringList(QUIZ_VOCABULARY_WORDS_FIELD),
        cefrLevel = getString(CEFR_LEVEL_FIELD).orEmpty(),
        questionType = getString(QUESTION_TYPE_FIELD).toQuestionType(),
        questionCount = questionCount,
        status = status.normalizeQuizStatus(hasGeneratedContent),
        failureReason = getString(QUIZ_FAILURE_REASON_FIELD)?.trim()?.takeIf(String::isNotBlank),
        createdAtEpochMillis = (
            getTimestamp(QUIZ_CREATED_AT_FIELD)?.toDate()?.time
                ?: getLong(QUIZ_CREATED_AT_EPOCH_FIELD)
                ?: 0L
        ),
    )
}

private fun DocumentSnapshot.toQuiz(): RoomQuiz? {
    val title = getString(QUIZ_TITLE_FIELD) ?: return null
    val questionType = getString(QUESTION_TYPE_FIELD).toQuestionType()
    val quizKind = getString(QUIZ_KIND_FIELD).toQuizKind()
    val topic = getString(QUIZ_TOPIC_FIELD).orEmpty().ifBlank { DEFAULT_TOPIC }
    val questions =
        getQuestionList(QUESTIONS_FIELD)
            .mapIndexedNotNull { index, question ->
                question.toQuizQuestion(
                    fallbackType = questionType,
                    fallbackId = "q${index + 1}",
                )
            }
    val hasGeneratedContent = questions.isNotEmpty()

    return RoomQuiz(
        id = id,
        title =
            title.normalizeQuizTitle(
                hasGeneratedContent = hasGeneratedContent,
                quizKind = quizKind,
                topic = topic,
            ),
        quizKind = quizKind,
        topic = topic,
        vocabularyWords = getStringList(QUIZ_VOCABULARY_WORDS_FIELD),
        cefrLevel = getString(CEFR_LEVEL_FIELD).orEmpty(),
        questionType = questionType,
        status = getString(QUIZ_STATUS_FIELD).toQuizStatus().normalizeQuizStatus(hasGeneratedContent),
        failureReason = getString(QUIZ_FAILURE_REASON_FIELD)?.trim()?.takeIf(String::isNotBlank),
        questions = questions,
        createdAtEpochMillis = (
            getTimestamp(QUIZ_CREATED_AT_FIELD)?.toDate()?.time
                ?: getLong(QUIZ_CREATED_AT_EPOCH_FIELD)
                ?: 0L
        ),
    )
}

private fun DocumentSnapshot.toAnnouncement(): RoomAnnouncement? {
    val title = getString(ANNOUNCEMENT_TITLE_FIELD) ?: return null
    val message = getString(ANNOUNCEMENT_MESSAGE_FIELD) ?: return null

    return RoomAnnouncement(
        id = id,
        title = title,
        message = message,
        authorName =
            getString(ANNOUNCEMENT_AUTHOR_NAME_FIELD).orEmpty().ifBlank {
                DEFAULT_ANNOUNCEMENT_AUTHOR
            },
        createdAtEpochMillis = (
            getTimestamp(ANNOUNCEMENT_CREATED_AT_FIELD)?.toDate()?.time
                ?: getLong(ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD)
                ?: 0L
        ),
    )
}

private fun DocumentSnapshot.toRoomInvitation(): RoomInvitation? {
    val roomId = getString(ROOM_ID_FIELD) ?: return null

    return RoomInvitation(
        id = id,
        roomId = roomId,
        roomName = getString(ROOM_NAME_FIELD).orEmpty().ifBlank { DEFAULT_ROOM_NAME },
        inviterName = getString(INVITER_NAME_FIELD).orEmpty().ifBlank { DEFAULT_INVITER_NAME },
        access = getString(INVITATION_ACCESS_FIELD).toRoomInvitationAccess(),
        createdAtEpochMillis = getLong(CREATED_AT_FIELD) ?: 0L,
    )
}

private fun String?.toQuestionType(default: QuestionType = QuestionType.MULTIPLE_CHOICE): QuestionType =
    when (this) {
        "fill_in_blank" -> QuestionType.FILL_IN_BLANK
        "multiple_choice" -> QuestionType.MULTIPLE_CHOICE
        "word_scramble" -> QuestionType.WORD_SCRAMBLE
        else -> default
    }

private fun String?.toQuizKind(): QuizKind =
    when (this) {
        "vocabulary" -> QuizKind.VOCABULARY
        else -> QuizKind.GRAMMAR
    }

private fun String?.toRoomInvitationAccess(): RoomInvitationAccess =
    when (this) {
        "collaborator" -> RoomInvitationAccess.COLLABORATOR
        else -> RoomInvitationAccess.MEMBER
    }

private fun String?.toQuizStatus(): RoomQuizStatus =
    when (this) {
        "generating" -> RoomQuizStatus.GENERATING
        "review" -> RoomQuizStatus.REVIEW
        "failed" -> RoomQuizStatus.FAILED
        else -> RoomQuizStatus.READY
    }

private fun RoomQuizStatus.normalizeQuizStatus(hasGeneratedContent: Boolean): RoomQuizStatus =
    if (hasGeneratedContent && (this == RoomQuizStatus.GENERATING || this == RoomQuizStatus.FAILED)) {
        RoomQuizStatus.REVIEW
    } else {
        this
    }

private fun String.normalizeQuizTitle(
    hasGeneratedContent: Boolean,
    quizKind: QuizKind,
    topic: String,
): String {
    val normalizedTitle = ifBlank { DEFAULT_GENERATING_QUIZ_TITLE }
    return normalizedTitle
}

private fun DocumentSnapshot.toRoom(): Room? {
    val name = getString(NAME_FIELD) ?: return null

    return Room(
        id = id,
        name = name,
        topic = getString(TOPIC_FIELD).orEmpty().ifBlank { DEFAULT_TOPIC },
        cefrLevel = getString(CEFR_LEVEL_FIELD).orEmpty(),
        participantCount = (getLong(PARTICIPANT_COUNT_FIELD) ?: 0L).toInt(),
        unansweredQuizCount =
            (
                getLong(UNANSWERED_QUIZ_COUNT_FIELD)
                    ?: getLong(LEGACY_LIVE_QUIZ_COUNT_FIELD)
                    ?: 0L
            ).toInt(),
        ownerId = getString(OWNER_ID_FIELD).orEmpty(),
        ownerName = getString(OWNER_NAME_FIELD).orEmpty().ifBlank { DEFAULT_INVITER_NAME },
        memberIds = getStringList(MEMBER_IDS_FIELD),
        collaboratorIds = getStringList(COLLABORATOR_IDS_FIELD),
        createdAtEpochMillis = getLong(CREATED_AT_FIELD) ?: 0L,
    )
}

private fun DocumentSnapshot.getStringList(field: String): List<String> =
    get(field)
        .let { it as? List<*> }
        .orEmpty()
        .filterIsInstance<String>()

private fun DocumentSnapshot.getQuestionList(field: String): List<Map<*, *>> =
    get(field)
        .let { it as? List<*> }
        .orEmpty()
        .filterIsInstance<Map<*, *>>()

private fun Map<*, *>.toQuizQuestion(
    fallbackType: QuestionType,
    fallbackId: String,
): RoomQuizQuestion? {
    val prompt = getStringValue(QUIZ_QUESTION_PROMPT_FIELD) ?: return null
    val explanation = getStringValue(QUIZ_QUESTION_EXPLANATION_FIELD) ?: return null
    val timeLimitSeconds = getIntValue(QUIZ_QUESTION_TIME_LIMIT_SECONDS_FIELD)
    val questionType = getStringValue(QUIZ_QUESTION_TYPE_FIELD).toQuestionType(default = fallbackType)
    val questionId = getStringValue(ID_FIELD).orEmpty().ifBlank { fallbackId }

    return when (questionType) {
        QuestionType.MULTIPLE_CHOICE -> {
            val options = getStringListValue(QUIZ_QUESTION_OPTIONS_FIELD)
            val correctOptionIndex = getIntValue(QUIZ_QUESTION_CORRECT_OPTION_INDEX_FIELD)
            if (options.isEmpty() || correctOptionIndex == null) {
                null
            } else {
                RoomQuizQuestion.MultipleChoice(
                    id = questionId,
                    prompt = prompt,
                    explanation = explanation,
                    timeLimitSeconds = timeLimitSeconds,
                    options = options,
                    correctOptionIndex = correctOptionIndex,
                )
            }
        }

        QuestionType.FILL_IN_BLANK -> {
            val answerText = getStringValue(QUIZ_QUESTION_ANSWER_TEXT_FIELD) ?: return null
            RoomQuizQuestion.FillInBlank(
                id = questionId,
                prompt = prompt,
                explanation = explanation,
                timeLimitSeconds = timeLimitSeconds,
                answerText = answerText,
            )
        }

        QuestionType.WORD_SCRAMBLE -> {
            val answerWord = getStringValue(QUIZ_QUESTION_ANSWER_WORD_FIELD) ?: return null
            val shuffledLetters = getStringListValue(QUIZ_QUESTION_SHUFFLED_LETTERS_FIELD)
            if (shuffledLetters.isEmpty()) {
                null
            } else {
                RoomQuizQuestion.WordScramble(
                    id = questionId,
                    prompt = prompt,
                    explanation = explanation,
                    timeLimitSeconds = timeLimitSeconds,
                    answerWord = answerWord,
                    shuffledLetters = shuffledLetters,
                )
            }
        }
    }
}

private fun Map<*, *>.getStringValue(field: String): String? = this[field] as? String

private fun Map<*, *>.getStringListValue(field: String): List<String> =
    (this[field] as? List<*>).orEmpty().filterIsInstance<String>()

private fun Map<*, *>.getIntValue(field: String): Int? = (this[field] as? Number)?.toInt()

private fun RoomQuizQuestion.toFirestoreMap(): Map<String, Any> =
    when (this) {
        is RoomQuizQuestion.MultipleChoice ->
            mapOf(
                ID_FIELD to id,
                QUIZ_QUESTION_TYPE_FIELD to "multiple_choice",
                QUIZ_QUESTION_PROMPT_FIELD to prompt,
                QUIZ_QUESTION_OPTIONS_FIELD to options,
                QUIZ_QUESTION_CORRECT_OPTION_INDEX_FIELD to correctOptionIndex,
                QUIZ_QUESTION_EXPLANATION_FIELD to explanation,
            ).withOptionalQuestionTimeLimit(timeLimitSeconds)

        is RoomQuizQuestion.FillInBlank ->
            mapOf(
                ID_FIELD to id,
                QUIZ_QUESTION_TYPE_FIELD to "fill_in_blank",
                QUIZ_QUESTION_PROMPT_FIELD to prompt,
                QUIZ_QUESTION_ANSWER_TEXT_FIELD to answerText,
                QUIZ_QUESTION_EXPLANATION_FIELD to explanation,
            ).withOptionalQuestionTimeLimit(timeLimitSeconds)

        is RoomQuizQuestion.WordScramble ->
            mapOf(
                ID_FIELD to id,
                QUIZ_QUESTION_TYPE_FIELD to "word_scramble",
                QUIZ_QUESTION_PROMPT_FIELD to prompt,
                QUIZ_QUESTION_ANSWER_WORD_FIELD to answerWord,
                QUIZ_QUESTION_SHUFFLED_LETTERS_FIELD to shuffledLetters,
                QUIZ_QUESTION_EXPLANATION_FIELD to explanation,
            ).withOptionalQuestionTimeLimit(timeLimitSeconds)
    }

private fun Map<String, Any>.withOptionalQuestionTimeLimit(timeLimitSeconds: Int?): Map<String, Any> =
    if (timeLimitSeconds == null) this else this + (QUIZ_QUESTION_TIME_LIMIT_SECONDS_FIELD to timeLimitSeconds)

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

private val FIRESTORE_CALLBACK_EXECUTOR = Dispatchers.IO.asExecutor()
private val FIREBASE_TASK_EXECUTOR = Dispatchers.IO.asExecutor()

private const val USERS_COLLECTION = "users"
private const val ROOMS_COLLECTION = "rooms"
private const val ROOM_INVITATIONS_COLLECTION = "roomInvitations"
private const val QUIZZES_COLLECTION = "quizzes"
private const val ANNOUNCEMENTS_COLLECTION = "announcements"
private const val ID_FIELD = "id"
private const val ROOM_ID_FIELD = "roomId"
private const val ROOM_NAME_FIELD = "roomName"
private const val NAME_FIELD = "name"
private const val TOPIC_FIELD = "topic"
private const val PARTICIPANT_COUNT_FIELD = "participantCount"
private const val UNANSWERED_QUIZ_COUNT_FIELD = "unansweredQuizCount"
private const val LEGACY_LIVE_QUIZ_COUNT_FIELD = "liveQuizCount"
private const val OWNER_ID_FIELD = "ownerId"
private const val OWNER_NAME_FIELD = "ownerName"
private const val MEMBER_IDS_FIELD = "memberIds"
private const val COLLABORATOR_IDS_FIELD = "collaboratorIds"
private const val INVITER_ID_FIELD = "inviterId"
private const val INVITER_NAME_FIELD = "inviterName"
private const val INVITEE_ID_FIELD = "inviteeId"
private const val INVITEE_EMAIL_FIELD = "inviteeEmail"
private const val INVITEE_DISPLAY_NAME_FIELD = "inviteeDisplayName"
private const val INVITATION_ACCESS_FIELD = "access"
private const val INVITATION_STATUS_FIELD = "status"
private const val PENDING_INVITATION_STATUS = "pending"
private const val READY_QUIZ_STATUS = "ready"
private const val CREATED_AT_FIELD = "createdAtEpochMillis"
private const val UPDATED_AT_FIELD = "updatedAtEpochMillis"
private const val QUIZ_CREATED_AT_FIELD = "createdAt"
private const val QUIZ_CREATED_AT_EPOCH_FIELD = "createdAtEpochMillis"
private const val DEFAULT_TOPIC = "AI"
private const val DEFAULT_ROOM_NAME = "Untitled room"
private const val DEFAULT_INVITER_NAME = "Room owner"
private const val DEFAULT_GENERATING_QUIZ_TITLE = "Generating quiz..."
private const val QUIZ_TITLE_FIELD = "title"
private const val QUIZ_CLIENT_REQUEST_ID_FIELD = "clientRequestId"
private const val QUIZ_KIND_FIELD = "quizKind"
private const val QUIZ_TOPIC_FIELD = "topic"
private const val QUIZ_VOCABULARY_WORDS_FIELD = "vocabularyWords"
private const val CEFR_LEVEL_FIELD = "cefrLevel"
private const val QUESTION_TYPE_FIELD = "questionType"
private const val QUIZ_STATUS_FIELD = "status"
private const val QUIZ_FAILURE_REASON_FIELD = "failureReason"
private const val QUESTIONS_FIELD = "questions"
private const val QUIZ_QUESTION_TYPE_FIELD = "type"
private const val QUIZ_QUESTION_PROMPT_FIELD = "prompt"
private const val QUIZ_QUESTION_OPTIONS_FIELD = "options"
private const val QUIZ_QUESTION_CORRECT_OPTION_INDEX_FIELD = "correctOptionIndex"
private const val QUIZ_QUESTION_ANSWER_TEXT_FIELD = "answerText"
private const val QUIZ_QUESTION_ANSWER_WORD_FIELD = "answerWord"
private const val QUIZ_QUESTION_SHUFFLED_LETTERS_FIELD = "shuffledLetters"
private const val QUIZ_QUESTION_EXPLANATION_FIELD = "explanation"
private const val QUIZ_QUESTION_TIME_LIMIT_SECONDS_FIELD = "timeLimitSeconds"
private const val QUIZ_QUESTION_COUNT_FIELD = "questionCount"
private const val ANNOUNCEMENT_TITLE_FIELD = "title"
private const val ANNOUNCEMENT_MESSAGE_FIELD = "message"
private const val ANNOUNCEMENT_AUTHOR_ID_FIELD = "authorId"
private const val ANNOUNCEMENT_AUTHOR_NAME_FIELD = "authorName"
private const val ANNOUNCEMENT_CREATED_AT_FIELD = "createdAt"
private const val ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD = "createdAtEpochMillis"
private const val DEFAULT_ANNOUNCEMENT_AUTHOR = "Teacher"
