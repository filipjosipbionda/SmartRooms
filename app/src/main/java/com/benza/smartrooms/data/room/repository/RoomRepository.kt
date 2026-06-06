package com.benza.smartrooms.data.room.repository

import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.CreateRoomInvitationRequest
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.RoomAnnouncementAttachment
import com.benza.smartrooms.data.room.model.RoomInvitation
import com.benza.smartrooms.data.room.model.RoomMember
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuiz
import com.benza.smartrooms.data.room.model.RoomQuizLeaderboardStudent
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.model.RoomQuizResult
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.model.UpdateAnnouncementRequest
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
     * Observes a single room document.
     */
    fun observeRoom(roomId: String): Flow<RoomOperationResult<Room>>

    /**
     * Observes pending invitations for the supplied user.
     */
    fun observePendingRoomInvitations(userId: String): Flow<RoomOperationResult<List<RoomInvitation>>>

    /**
     * Observes quizzes generated for a room.
     */
    fun observeQuizzes(roomId: String): Flow<RoomOperationResult<List<RoomQuizSummary>>>

    /**
     * Observes one generated quiz including all questions.
     */
    fun observeQuiz(
        roomId: String,
        quizId: String,
    ): Flow<RoomOperationResult<RoomQuiz>>

    fun observeQuizResults(
        userId: String,
        roomId: String,
    ): Flow<RoomOperationResult<List<RoomQuizResult>>>

    fun observeQuizLeaderboardStudents(
        roomId: String,
        memberIds: List<String>,
    ): Flow<RoomOperationResult<List<RoomQuizLeaderboardStudent>>>

    fun observeRoomMembers(
        ownerId: String,
        memberIds: List<String>,
        collaboratorIds: List<String>,
    ): Flow<RoomOperationResult<List<RoomMember>>>

    suspend fun saveQuizResult(result: RoomQuizResult): RoomOperationResult<Unit>

    /**
     * Observes announcements created for a room.
     */
    fun observeAnnouncements(roomId: String): Flow<RoomOperationResult<List<RoomAnnouncement>>>

    /**
     * Loads one announcement from the selected room.
     */
    suspend fun getAnnouncement(
        roomId: String,
        announcementId: String,
    ): RoomOperationResult<RoomAnnouncement>

    /**
     * Creates a new room document for the signed-in user.
     */
    suspend fun createRoom(request: CreateRoomRequest): RoomOperationResult<Unit>

    /**
     * Creates a new announcement document inside the supplied room.
     */
    suspend fun createAnnouncement(request: CreateAnnouncementRequest): RoomOperationResult<Unit>

    /**
     * Updates one announcement document inside the supplied room.
     */
    suspend fun updateAnnouncement(request: UpdateAnnouncementRequest): RoomOperationResult<Unit>

    /**
     * Deletes one announcement document from the supplied room.
     */
    suspend fun deleteAnnouncement(
        roomId: String,
        announcementId: String,
        attachments: Collection<RoomAnnouncementAttachment>,
    ): RoomOperationResult<Unit>

    /**
     * Creates a pending room invitation for the selected user.
     */
    suspend fun createRoomInvitation(request: CreateRoomInvitationRequest): RoomOperationResult<Unit>

    /**
     * Accepts a pending invitation and joins the target room.
     */
    suspend fun acceptRoomInvitation(invitationId: String): RoomOperationResult<Unit>

    /**
     * Rejects a pending invitation.
     */
    suspend fun rejectRoomInvitation(invitationId: String): RoomOperationResult<Unit>

    /**
     * Removes one non-owner user from the selected room.
     */
    suspend fun removeRoomMember(
        roomId: String,
        targetUserId: String,
    ): RoomOperationResult<Unit>

    /**
     * Triggers backend quiz generation for the supplied room.
     */
    suspend fun generateQuiz(request: GenerateQuizRequest): RoomOperationResult<Unit>

    /**
     * Retries backend generation for an existing failed quiz.
     */
    suspend fun retryQuizGeneration(
        roomId: String,
        quizId: String,
    ): RoomOperationResult<Unit>

    /**
     * Publishes a reviewed quiz so it becomes solvable.
     */
    suspend fun publishQuiz(
        roomId: String,
        quizId: String,
    ): RoomOperationResult<Unit>

    /**
     * Deletes one quiz from the selected room.
     */
    suspend fun deleteQuiz(
        roomId: String,
        quizId: String,
    ): RoomOperationResult<Unit>

    /**
     * Deletes multiple quizzes from the selected room in one operation.
     */
    suspend fun deleteQuizzes(
        roomId: String,
        quizIds: Collection<String>,
    ): RoomOperationResult<Unit>

    /**
     * Saves a manually edited quiz question.
     */
    suspend fun updateQuizQuestion(
        roomId: String,
        quizId: String,
        question: RoomQuizQuestion,
    ): RoomOperationResult<Unit>

    /**
     * Deletes one question from the selected quiz.
     */
    suspend fun deleteQuizQuestion(
        roomId: String,
        quizId: String,
        questionId: String,
    ): RoomOperationResult<Unit>
}
