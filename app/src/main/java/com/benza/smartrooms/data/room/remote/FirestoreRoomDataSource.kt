package com.benza.smartrooms.data.room.remote

import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.Room
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
                LIVE_QUIZ_COUNT_FIELD to 0,
                OWNER_ID_FIELD to request.ownerId,
                OWNER_NAME_FIELD to request.ownerName,
                CREATED_AT_FIELD to now
            )
        ).await()
    }
}

private fun DocumentSnapshot.toRoom(): Room? {
    val name = getString(NAME_FIELD) ?: return null

    return Room(
        id = id,
        name = name,
        topic = getString(TOPIC_FIELD).orEmpty().ifBlank { DEFAULT_TOPIC },
        participantCount = (getLong(PARTICIPANT_COUNT_FIELD) ?: 0L).toInt(),
        liveQuizCount = (getLong(LIVE_QUIZ_COUNT_FIELD) ?: 0L).toInt(),
        ownerId = getString(OWNER_ID_FIELD).orEmpty(),
        createdAtEpochMillis = getLong(CREATED_AT_FIELD) ?: 0L
    )
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
private const val ID_FIELD = "id"
private const val NAME_FIELD = "name"
private const val TOPIC_FIELD = "topic"
private const val PARTICIPANT_COUNT_FIELD = "participantCount"
private const val LIVE_QUIZ_COUNT_FIELD = "liveQuizCount"
private const val OWNER_ID_FIELD = "ownerId"
private const val OWNER_NAME_FIELD = "ownerName"
private const val CREATED_AT_FIELD = "createdAtEpochMillis"
private const val DEFAULT_TOPIC = "AI"
