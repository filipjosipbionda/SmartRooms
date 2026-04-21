package com.benza.smartrooms.data.room.repository

import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.remote.FirestoreRoomDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Firestore-backed implementation of [RoomRepository].
 */
internal class FirestoreRoomRepository(
    private val roomDataSource: FirestoreRoomDataSource
) : RoomRepository {
    /**
     * Streams rooms from Firestore and maps failures to UI-facing messages.
     */
    override fun observeRooms(ownerId: String): Flow<RoomOperationResult<List<Room>>> {
        return roomDataSource.observeRooms(ownerId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
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
}

private fun Throwable.toRoomErrorRes(): Int {
    return when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_room_permission
            FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_room_unavailable
            else -> R.string.error_room_generic
        }

        else -> R.string.error_room_generic
    }
}
