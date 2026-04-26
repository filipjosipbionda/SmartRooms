package com.benza.smartrooms.data.room.repository

import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.CreateRoomInvitationRequest
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomInvitation
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.remote.FirebaseFunctionsRoomDataSource
import com.benza.smartrooms.data.room.remote.FirestoreRoomDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.firestore.FirebaseFirestoreException
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
            .flowOn(Dispatchers.IO)
    }

    /**
     * Streams rooms where the supplied user is listed as a member.
     */
    override fun observeMemberRooms(userId: String): Flow<RoomOperationResult<List<Room>>> {
        return roomDataSource.observeMemberRooms(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Streams rooms where the supplied professor is listed as a collaborator.
     */
    override fun observeCollaboratingRooms(userId: String): Flow<RoomOperationResult<List<Room>>> {
        return roomDataSource.observeCollaboratingRooms(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<Room>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Streams the selected room document.
     */
    override fun observeRoom(roomId: String): Flow<RoomOperationResult<Room>> {
        return roomDataSource.observeRoom(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<Room> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Streams pending invitations for the supplied user.
     */
    override fun observePendingRoomInvitations(userId: String): Flow<RoomOperationResult<List<RoomInvitation>>> {
        return roomDataSource.observePendingRoomInvitations(userId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomInvitation>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Streams generated quizzes for the selected room.
     */
    override fun observeQuizzes(roomId: String): Flow<RoomOperationResult<List<RoomQuizSummary>>> {
        return roomDataSource.observeQuizzes(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomQuizSummary>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Streams room announcements from Firestore.
     */
    override fun observeAnnouncements(roomId: String): Flow<RoomOperationResult<List<RoomAnnouncement>>> {
        return roomDataSource.observeAnnouncements(roomId)
            .map { RoomOperationResult.Success(it) as RoomOperationResult<List<RoomAnnouncement>> }
            .catch { emit(RoomOperationResult.Error(it.toRoomErrorRes())) }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Creates a room document in Firestore.
     */
    override suspend fun createRoom(request: CreateRoomRequest): RoomOperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.createRoom(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes()) }
            )
        }
    }

    /**
     * Creates a new announcement inside the selected room.
     */
    override suspend fun createAnnouncement(request: CreateAnnouncementRequest): RoomOperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                roomDataSource.createAnnouncement(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomErrorRes()) }
            )
        }
    }

    /**
     * Creates a pending room invitation.
     */
    override suspend fun createRoomInvitation(request: CreateRoomInvitationRequest): RoomOperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.createRoomInvitation(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomInvitationCreateErrorRes()) }
            )
        }
    }

    /**
     * Accepts a pending invitation through the backend.
     */
    override suspend fun acceptRoomInvitation(invitationId: String): RoomOperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.acceptRoomInvitation(invitationId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomInvitationResolveErrorRes()) }
            )
        }
    }

    /**
     * Rejects a pending invitation through the backend.
     */
    override suspend fun rejectRoomInvitation(invitationId: String): RoomOperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.rejectRoomInvitation(invitationId)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toRoomInvitationResolveErrorRes()) }
            )
        }
    }

    /**
     * Triggers backend quiz generation through a callable Cloud Function.
     */
    override suspend fun generateQuiz(request: GenerateQuizRequest): RoomOperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                functionsRoomDataSource.generateQuiz(request)
            }.fold(
                onSuccess = { RoomOperationResult.Success(Unit) },
                onFailure = { RoomOperationResult.Error(it.toQuizGenerationErrorRes()) }
            )
        }
    }
}

private fun Throwable.toRoomErrorRes(): Int {
    return when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException -> when (code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_room_permission
            else -> R.string.error_room_generic
        }

        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_room_permission
            FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_room_unavailable
            else -> R.string.error_room_generic
        }

        else -> R.string.error_room_generic
    }
}

private fun Throwable.toQuizGenerationErrorRes(): Int {
    return when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException -> when (code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_quiz_generation_permission
            FirebaseFunctionsException.Code.NOT_FOUND -> R.string.error_quiz_generation_room_not_found
            else -> R.string.error_quiz_generation_generic
        }

        else -> toRoomErrorRes()
    }
}

private fun Throwable.toRoomInvitationCreateErrorRes(): Int {
    return when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException -> when (code) {
            FirebaseFunctionsException.Code.NOT_FOUND -> R.string.error_room_invite_target_not_found
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_room_invite_owner_required
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> R.string.error_room_invite_unavailable
            else -> R.string.error_room_invite_generic
        }

        else -> toRoomErrorRes()
    }
}

private fun Throwable.toRoomInvitationResolveErrorRes(): Int {
    return when (this) {
        is FirebaseNetworkException -> R.string.error_room_network
        is FirebaseFunctionsException -> when (code) {
            FirebaseFunctionsException.Code.NOT_FOUND -> R.string.error_room_invitation_not_found
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_room_invitation_permission
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> R.string.error_room_invitation_unavailable
            else -> R.string.error_room_invitation_generic
        }

        else -> toRoomErrorRes()
    }
}
