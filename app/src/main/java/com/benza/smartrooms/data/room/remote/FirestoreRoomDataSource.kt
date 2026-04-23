package com.benza.smartrooms.data.room.remote

import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Firestore room wrapper that exposes suspend and Flow-friendly primitives.
 */
internal class FirestoreRoomDataSource(
    private val firestore: FirebaseFirestore
) {
    /**
     * Observes all rooms owned by the supplied user.
     */
    internal fun observeRooms(ownerId: String): Flow<List<Room>> = callbackFlow {
        val registration = firestore.collection(ROOMS_COLLECTION)
            .whereEqualTo(OWNER_ID_FIELD, ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents.orEmpty()
                    .mapNotNull(DocumentSnapshot::toRoom)
                    .sortedByDescending(Room::createdAtEpochMillis)

                trySend(rooms)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observes rooms where the supplied user is listed in memberIds.
     */
    internal fun observeMemberRooms(userId: String): Flow<List<Room>> = callbackFlow {
        val registration = firestore.collection(ROOMS_COLLECTION)
            .whereArrayContains(MEMBER_IDS_FIELD, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents.orEmpty()
                    .mapNotNull(DocumentSnapshot::toRoom)
                    .sortedByDescending(Room::createdAtEpochMillis)

                trySend(rooms)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observes rooms where the supplied professor is listed in collaboratorIds.
     */
    internal fun observeCollaboratingRooms(userId: String): Flow<List<Room>> = callbackFlow {
        val registration = firestore.collection(ROOMS_COLLECTION)
            .whereArrayContains(COLLABORATOR_IDS_FIELD, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents.orEmpty()
                    .mapNotNull(DocumentSnapshot::toRoom)
                    .sortedByDescending(Room::createdAtEpochMillis)

                trySend(rooms)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observes generated quizzes inside the supplied room.
     */
    internal fun observeQuizzes(roomId: String): Flow<List<RoomQuizSummary>> = callbackFlow {
        val registration = firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(QUIZZES_COLLECTION)
            .orderBy(QUIZ_CREATED_AT_FIELD, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val quizzes = snapshot?.documents.orEmpty()
                    .mapNotNull(DocumentSnapshot::toQuizSummary)

                trySend(quizzes)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observes announcements inside the supplied room.
     */
    internal fun observeAnnouncements(roomId: String): Flow<List<RoomAnnouncement>> = callbackFlow {
        val registration = firestore.collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(ANNOUNCEMENTS_COLLECTION)
            .orderBy(ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val announcements = snapshot?.documents.orEmpty()
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

        roomDocument.set(
            mapOf(
                ID_FIELD to roomDocument.id,
                NAME_FIELD to request.name,
                TOPIC_FIELD to request.topic,
                PARTICIPANT_COUNT_FIELD to 1,
                UNANSWERED_QUIZ_COUNT_FIELD to 0,
                OWNER_ID_FIELD to request.ownerId,
                OWNER_NAME_FIELD to request.ownerName,
                MEMBER_IDS_FIELD to listOf(request.ownerId),
                COLLABORATOR_IDS_FIELD to emptyList<String>(),
                CREATED_AT_FIELD to now
            )
        ).await()
    }

    /**
     * Creates a new room announcement document in Firestore.
     */
    internal suspend fun createAnnouncement(request: CreateAnnouncementRequest) {
        val announcementDocument = firestore.collection(ROOMS_COLLECTION)
            .document(request.roomId)
            .collection(ANNOUNCEMENTS_COLLECTION)
            .document()

        announcementDocument.set(
            mapOf(
                ID_FIELD to announcementDocument.id,
                ANNOUNCEMENT_TITLE_FIELD to request.title,
                ANNOUNCEMENT_MESSAGE_FIELD to request.message,
                ANNOUNCEMENT_AUTHOR_ID_FIELD to request.authorId,
                ANNOUNCEMENT_AUTHOR_NAME_FIELD to request.authorName,
                ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD to System.currentTimeMillis()
            )
        ).await()
    }
}

private fun DocumentSnapshot.toQuizSummary(): RoomQuizSummary? {
    val title = getString(QUIZ_TITLE_FIELD) ?: return null

    return RoomQuizSummary(
        id = id,
        title = title,
        cefrLevel = getString(CEFR_LEVEL_FIELD).orEmpty().ifBlank { DEFAULT_CEFR_LEVEL },
        questionType = getString(QUESTION_TYPE_FIELD).toQuestionType(),
        questionCount = (getLong(QUIZ_QUESTION_COUNT_FIELD) ?: 0L).toInt(),
        createdAtEpochMillis = (
            getTimestamp(QUIZ_CREATED_AT_FIELD)?.toDate()?.time
                ?: getLong(QUIZ_CREATED_AT_EPOCH_FIELD)
                ?: 0L
            )
    )
}

private fun DocumentSnapshot.toAnnouncement(): RoomAnnouncement? {
    val title = getString(ANNOUNCEMENT_TITLE_FIELD) ?: return null
    val message = getString(ANNOUNCEMENT_MESSAGE_FIELD) ?: return null

    return RoomAnnouncement(
        id = id,
        title = title,
        message = message,
        authorName = getString(ANNOUNCEMENT_AUTHOR_NAME_FIELD).orEmpty().ifBlank {
            DEFAULT_ANNOUNCEMENT_AUTHOR
        },
        createdAtEpochMillis = (
            getTimestamp(ANNOUNCEMENT_CREATED_AT_FIELD)?.toDate()?.time
                ?: getLong(ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD)
                ?: 0L
            )
    )
}

private fun String?.toQuestionType(): QuestionType {
    return when (this) {
        "fill_in_blank" -> QuestionType.FILL_IN_BLANK
        else -> QuestionType.MULTIPLE_CHOICE
    }
}

private fun DocumentSnapshot.toRoom(): Room? {
    val name = getString(NAME_FIELD) ?: return null

    return Room(
        id = id,
        name = name,
        topic = getString(TOPIC_FIELD).orEmpty().ifBlank { DEFAULT_TOPIC },
        participantCount = (getLong(PARTICIPANT_COUNT_FIELD) ?: 0L).toInt(),
        unansweredQuizCount = (
            getLong(UNANSWERED_QUIZ_COUNT_FIELD)
                ?: getLong(LEGACY_LIVE_QUIZ_COUNT_FIELD)
                ?: 0L
            ).toInt(),
        ownerId = getString(OWNER_ID_FIELD).orEmpty(),
        memberIds = getStringList(MEMBER_IDS_FIELD),
        collaboratorIds = getStringList(COLLABORATOR_IDS_FIELD),
        createdAtEpochMillis = getLong(CREATED_AT_FIELD) ?: 0L
    )
}

private fun DocumentSnapshot.getStringList(field: String): List<String> {
    return get(field)
        .let { it as? List<*> }
        .orEmpty()
        .filterIsInstance<String>()
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

private const val ROOMS_COLLECTION = "rooms"
private const val QUIZZES_COLLECTION = "quizzes"
private const val ANNOUNCEMENTS_COLLECTION = "announcements"
private const val ID_FIELD = "id"
private const val NAME_FIELD = "name"
private const val TOPIC_FIELD = "topic"
private const val PARTICIPANT_COUNT_FIELD = "participantCount"
private const val UNANSWERED_QUIZ_COUNT_FIELD = "unansweredQuizCount"
private const val LEGACY_LIVE_QUIZ_COUNT_FIELD = "liveQuizCount"
private const val OWNER_ID_FIELD = "ownerId"
private const val OWNER_NAME_FIELD = "ownerName"
private const val MEMBER_IDS_FIELD = "memberIds"
private const val COLLABORATOR_IDS_FIELD = "collaboratorIds"
private const val CREATED_AT_FIELD = "createdAtEpochMillis"
private const val QUIZ_CREATED_AT_FIELD = "createdAt"
private const val QUIZ_CREATED_AT_EPOCH_FIELD = "createdAtEpochMillis"
private const val DEFAULT_TOPIC = "AI"
private const val DEFAULT_CEFR_LEVEL = "B1"
private const val QUIZ_TITLE_FIELD = "title"
private const val CEFR_LEVEL_FIELD = "cefrLevel"
private const val QUESTION_TYPE_FIELD = "questionType"
private const val QUIZ_QUESTION_COUNT_FIELD = "questionCount"
private const val ANNOUNCEMENT_TITLE_FIELD = "title"
private const val ANNOUNCEMENT_MESSAGE_FIELD = "message"
private const val ANNOUNCEMENT_AUTHOR_ID_FIELD = "authorId"
private const val ANNOUNCEMENT_AUTHOR_NAME_FIELD = "authorName"
private const val ANNOUNCEMENT_CREATED_AT_FIELD = "createdAt"
private const val ANNOUNCEMENT_CREATED_AT_EPOCH_FIELD = "createdAtEpochMillis"
private const val DEFAULT_ANNOUNCEMENT_AUTHOR = "Teacher"
