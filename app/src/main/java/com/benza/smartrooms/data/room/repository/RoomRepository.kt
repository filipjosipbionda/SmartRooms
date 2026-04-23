package com.benza.smartrooms.data.room.repository

import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuizSummary
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
     * Observes rooms owned by the supplied user.
     */
    fun observeOwnedRooms(ownerId: String): Flow<RoomOperationResult<List<Room>>>

    /**
     * Observes rooms where the supplied user is a member.
     */
    fun observeMemberRooms(userId: String): Flow<RoomOperationResult<List<Room>>>

    /**
     * Observes rooms where the supplied professor is a collaborator.
     */
    fun observeCollaboratingRooms(userId: String): Flow<RoomOperationResult<List<Room>>>

    /**
     * Observes quizzes generated for a room.
     */
    fun observeQuizzes(roomId: String): Flow<RoomOperationResult<List<RoomQuizSummary>>>

    /**
     * Observes announcements created for a room.
     */
    fun observeAnnouncements(roomId: String): Flow<RoomOperationResult<List<RoomAnnouncement>>>

    /**
     * Creates a new room document for the signed-in user.
     */
    suspend fun createRoom(request: CreateRoomRequest): RoomOperationResult<Unit>

    /**
     * Creates a new announcement document inside the supplied room.
     */
    suspend fun createAnnouncement(request: CreateAnnouncementRequest): RoomOperationResult<Unit>

    /**
     * Triggers backend quiz generation for the supplied room.
     */
    suspend fun generateQuiz(request: GenerateQuizRequest): RoomOperationResult<Unit>
}
