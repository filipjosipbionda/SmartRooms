package com.benza.smartrooms.data.room.repository

import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomOperationResult
import kotlinx.coroutines.flow.Flow

/**
 * Room abstraction consumed by ViewModels so Firestore details stay in the data layer.
 */
internal interface RoomRepository {
    /**
     * Observes the signed-in user's rooms.
     */
    fun observeRooms(ownerId: String): Flow<RoomOperationResult<List<Room>>>

    /**
     * Creates a new room document for the signed-in user.
     */
    suspend fun createRoom(request: CreateRoomRequest): RoomOperationResult<Unit>
}
