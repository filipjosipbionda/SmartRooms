package com.benza.smartrooms.data.room.repository

import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.CreateRoomInvitationRequest
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.RoomInvitation
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuiz
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.remote.FirebaseFunctionsRoomDataSource
import com.benza.smartrooms.data.room.remote.FirestoreRoomDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Firestore-backed implementation of [RoomRepository].
 */
internal class FirestoreRoomRepository(
    private val roomDataSource: FirestoreRoomDataSource,
    private val functionsRoomDataSource: FirebaseFunctionsRoomDataSource,
) : RoomRepository {
    /**
     * Streams rooms from Firestore and maps failures to UI-facing messages.
     */
    override fun observeRooms(ownerId: String): Flow<RoomOperationResult<List<Room>>> = observeOwnedRooms(ownerId)

    /**
     * Streams rooms owned by the supplied user.
     */
    override fun observeOwnedRooms(ownerId: String): Flow<RoomOperationResult<List<Room>>> =
        roomDataSource
            .observeRooms(ownerId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Streams rooms where the supplied user is listed as a member.
     */
    override fun observeMemberRooms(userId: String): Flow<RoomOperationResult<List<Room>>> =
        roomDataSource
            .observeMemberRooms(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Streams rooms where the supplied professor is listed as a collaborator.
     */
    override fun observeCollaboratingRooms(userId: String): Flow<RoomOperationResult<List<Room>>> =
        roomDataSource
            .observeCollaboratingRooms(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Streams the selected room document.
     */
    override fun observeRoom(roomId: String): Flow<RoomOperationResult<Room>> =
        roomDataSource
            .observeRoom(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<Room> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Streams pending invitations for the supplied user.
     */
    override fun observePendingRoomInvitations(userId: String): Flow<RoomOperationResult<List<RoomInvitation>>> =
        roomDataSource
            .observePendingRoomInvitations(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomInvitation>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Streams generated quizzes for the selected room.
     */
    override fun observeQuizzes(roomId: String): Flow<RoomOperationResult<List<RoomQuizSummary>>> =
        roomDataSource
            .observeQuizzes(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomQuizSummary>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Streams one generated quiz from Firestore.
     */
    override fun observeQuiz(
        roomId: String,
        quizId: String,
    ): Flow<RoomOperationResult<RoomQuiz>> =
        roomDataSource
            .observeQuiz(roomId, quizId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<RoomQuiz> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Streams room announcements from Firestore.
     */
    override fun observeAnnouncements(roomId: String): Flow<RoomOperationResult<List<RoomAnnouncement>>> =
        roomDataSource
            .observeAnnouncements(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomAnnouncement>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage())) }
            .flowOn(Dispatchers.IO)

    /**
     * Creates a room document in Firestore.
     */
    override suspend fun createRoom(request: CreateRoomRequest): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.createRoom(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage()) },
            )
        }

    /**
     * Creates a new announcement inside the selected room.
     */
    override suspend fun createAnnouncement(request: CreateAnnouncementRequest): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.createAnnouncement(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage()) },
            )
        }

    /**
     * Creates a pending room invitation.
     */
    override suspend fun createRoomInvitation(request: CreateRoomInvitationRequest): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.createRoomInvitation(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomInvitationCreateErrorRes(), it.toDebugMessage()) },
            )
        }

    /**
     * Accepts a pending invitation through the backend.
     */
    override suspend fun acceptRoomInvitation(invitationId: String): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.acceptRoomInvitation(invitationId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomInvitationResolveErrorRes(), it.toDebugMessage()) },
            )
        }

    /**
     * Rejects a pending invitation through the backend.
     */
    override suspend fun rejectRoomInvitation(invitationId: String): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.rejectRoomInvitation(invitationId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomInvitationResolveErrorRes(), it.toDebugMessage()) },
            )
        }

    /**
     * Triggers backend quiz generation through a callable Cloud Function.
     */
    override suspend fun generateQuiz(request: GenerateQuizRequest): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.generateQuiz(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toQuizGenerationErrorRes(), it.toDebugMessage()) },
            )
        }

    override suspend fun retryQuizGeneration(
        roomId: String,
        quizId: String,
    ): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.retryQuizGeneration(roomId, quizId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toQuizGenerationErrorRes(), it.toDebugMessage()) },
            )
        }

    override suspend fun publishQuiz(
        roomId: String,
        quizId: String,
    ): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.publishQuiz(roomId, quizId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage()) },
            )
        }

    override suspend fun deleteQuiz(
        roomId: String,
        quizId: String,
    ): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.deleteQuiz(roomId, quizId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage()) },
            )
        }

    override suspend fun deleteQuizzes(
        roomId: String,
        quizIds: Collection<String>,
    ): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.deleteQuizzes(roomId, quizIds)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage()) },
            )
        }

    override suspend fun updateQuizQuestion(
        roomId: String,
        quizId: String,
        question: RoomQuizQuestion,
    ): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.updateQuizQuestion(roomId, quizId, question)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage()) },
            )
        }

    override suspend fun deleteQuizQuestion(
        roomId: String,
        quizId: String,
        questionId: String,
    ): RoomOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.deleteQuizQuestion(roomId, quizId, questionId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes(), it.toDebugMessage()) },
            )
        }
}

private fun Throwable.toDebugMessage(): String? =
    when (this) {
        is FirebaseFunctionsException -> {
            val detailsMessage = details?.toString()?.trim().orEmpty()
            if (detailsMessage.isNotBlank()) {
                detailsMessage
            } else {
                message?.trim()
            }
        }

        is FirebaseFirestoreException,
        is FirebaseNetworkException,
        -> message?.trim()

        else -> message?.trim()
    }?.takeIf(String::isNotBlank)

private fun Throwable.toRoomErrorRes(): Int =
    when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException ->
            when (code) {
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_room_permission
                else -> R.string.error_room_generic
            }

        is FirebaseFirestoreException ->
            when (code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_room_permission
                FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_room_unavailable
                else -> R.string.error_room_generic
            }

        else -> R.string.error_room_generic
    }

private fun Throwable.toQuizGenerationErrorRes(): Int =
    when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException ->
            when (code) {
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_quiz_generation_permission
                FirebaseFunctionsException.Code.NOT_FOUND -> R.string.error_quiz_generation_room_not_found
                else -> R.string.error_quiz_generation_generic
            }

        else -> toRoomErrorRes()
    }

private fun Throwable.toRoomInvitationCreateErrorRes(): Int =
    when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException ->
            when (code) {
                FirebaseFunctionsException.Code.NOT_FOUND -> R.string.error_room_invite_target_not_found
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_room_invite_owner_required
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> R.string.error_room_invite_unavailable
                else -> R.string.error_room_invite_generic
            }

        else -> toRoomErrorRes()
    }

private fun Throwable.toRoomInvitationResolveErrorRes(): Int =
    when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException ->
            when (code) {
                FirebaseFunctionsException.Code.NOT_FOUND -> R.string.error_room_invitation_not_found
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_room_invitation_permission
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> R.string.error_room_invitation_unavailable
                else -> R.string.error_room_invitation_generic
            }

        else -> toRoomErrorRes()
    }
