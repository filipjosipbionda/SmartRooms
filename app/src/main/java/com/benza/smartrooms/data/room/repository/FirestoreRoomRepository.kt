package com.benza.smartrooms.data.room.repository

import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.remote.FirebaseFunctionsRoomDataSource
import com.benza.smartrooms.data.room.remote.FirestoreRoomDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Firestore-backed implementation of [RoomRepository].
 */
internal class FirestoreRoomRepository(
    private val roomDataSource: FirestoreRoomDataSource,
    private val functionsRoomDataSource: FirebaseFunctionsRoomDataSource
) : RoomRepository {
    /**
     * Streams rooms from Firestore and maps failures to UI-facing messages.
     */
    override fun observeRooms(ownerId: String): Flow<RoomOperationResult<List<Room>>> {
        return observeOwnedRooms(ownerId)
    }

    /**
     * Streams rooms owned by the supplied user.
     */
    override fun observeOwnedRooms(ownerId: String): Flow<RoomOperationResult<List<Room>>> {
        return roomDataSource.observeRooms(ownerId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
    }

    /**
     * Streams rooms where the supplied user is listed as a member.
     */
    override fun observeMemberRooms(userId: String): Flow<RoomOperationResult<List<Room>>> {
        return roomDataSource.observeMemberRooms(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
    }

    /**
     * Streams rooms where the supplied professor is listed as a collaborator.
     */
    override fun observeCollaboratingRooms(userId: String): Flow<RoomOperationResult<List<Room>>> {
        return roomDataSource.observeCollaboratingRooms(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
    }

    /**
     * Streams generated quizzes for the selected room.
     */
    override fun observeQuizzes(roomId: String): Flow<RoomOperationResult<List<RoomQuizSummary>>> {
        return roomDataSource.observeQuizzes(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomQuizSummary>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
    }

    /**
     * Streams room announcements from Firestore.
     */
    override fun observeAnnouncements(roomId: String): Flow<RoomOperationResult<List<RoomAnnouncement>>> {
        return roomDataSource.observeAnnouncements(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomAnnouncement>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
    }

    /**
     * Creates a room document in Firestore.
     */
    override suspend fun createRoom(request: CreateRoomRequest): RoomOperationResult<Unit> {
        return runCatching {
            roomDataSource.createRoom(request)
        }.fold(
            onSuccess = { RoomOperationResult.Success(Unit) },
            onFailure = { RoomOperationResult.Error(it.toRoomErrorRes()) }
        )
    }

    /**
     * Creates a new announcement inside the selected room.
     */
    override suspend fun createAnnouncement(request: CreateAnnouncementRequest): RoomOperationResult<Unit> {
        return runCatching {
            roomDataSource.createAnnouncement(request)
        }.fold(
            onSuccess = { RoomOperationResult.Success(Unit) },
            onFailure = { RoomOperationResult.Error(it.toRoomErrorRes()) }
        )
    }

    /**
     * Triggers backend quiz generation through a callable Cloud Function.
     */
    override suspend fun generateQuiz(request: GenerateQuizRequest): RoomOperationResult<Unit> {
        return runCatching {
            functionsRoomDataSource.generateQuiz(request)
        }.fold(
            onSuccess = { RoomOperationResult.Success(Unit) },
            onFailure = { RoomOperationResult.Error(it.toRoomErrorRes()) }
        )
    }
}

private fun Throwable.toRoomErrorRes(): Int {
    return when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException -> when (code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_quiz_generation_permission
            FirebaseFunctionsException.Code.NOT_FOUND -> R.string.error_quiz_generation_room_not_found
            else -> R.string.error_quiz_generation_generic
        }

        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_room_permission
            FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_room_unavailable
            else -> R.string.error_room_generic
        }

        else -> R.string.error_room_generic
    }
}
