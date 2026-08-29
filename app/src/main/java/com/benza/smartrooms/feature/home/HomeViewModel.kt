package com.benza.smartrooms.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomInvitation
import com.benza.smartrooms.data.room.model.RoomInvitationAccess
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import com.benza.smartrooms.util.orPrettyEmailLocalPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI model used by the home screen room list.
 */
internal data class HomeRoomUiState(
    val id: String,
    val name: String,
    val topic: String,
    val cefrLevel: String = "",
    val participantCount: Int,
    val unansweredQuizCount: Int,
    val ownerName: String,
    val role: HomeRoomRole = HomeRoomRole.MEMBER,
    val createdAtEpochMillis: Long = 0L,
)

internal enum class HomeRoomRole {
    OWNER,
    COLLABORATOR,
    MEMBER,
}

internal data class HomeInvitationUiState(
    val id: String,
    val roomName: String,
    val inviterName: String,
    val access: RoomInvitationAccess,
    val createdAtEpochMillis: Long,
)

/**
 * UI state for the authenticated home feature.
 */
internal data class HomeUiState(
    val profileInitials: String = "?",
    val profileDisplayName: String = "",
    val profilePhotoUrl: String? = null,
    val joinedRoomsCount: Int = 0,
    val unansweredQuizCount: Int = 0,
    val rooms: List<HomeRoomUiState> = emptyList(),
    val pagedRooms: List<HomeRoomUiState> = emptyList(),
    val isLoadingRooms: Boolean = true,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val pendingInvitations: List<HomeInvitationUiState> = emptyList(),
    val processingInvitationIds: Set<String> = emptySet(),
    val errorMessageRes: Int? = null,
    val infoMessageRes: Int? = null,
)

/**
 * Provides Firestore-backed room state and exposes logout / room creation behavior.
 */
internal class HomeViewModel(
    authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()

    private val _uiState =
        MutableStateFlow(
            HomeUiState(
                profileInitials = currentUser.toInitials(),
                profileDisplayName = currentUser.displayNameOrFallback(),
                profilePhotoUrl = currentUser?.photoUrl,
                isLoadingRooms = currentUser != null,
                errorMessageRes = if (currentUser == null) R.string.error_room_auth_required else null,
            ),
        )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        observeRooms()
        observePendingInvitations()
    }

    private fun observeProfile() {
        val user = currentUser ?: return

        viewModelScope.launch {
            userProfileRepository.observeProfile(user).collect { result ->
                if (result is UserProfileOperationResult.Success) {
                    _uiState.update {
                        it.copy(
                            profileInitials = result.data.displayName.toInitials(),
                            profileDisplayName = result.data.displayName,
                            profilePhotoUrl = result.data.photoUrl,
                        )
                    }
                }
            }
        }
    }

    /**
     * Moves the joined rooms list to the next page.
     */
    internal fun goToNextRoomsPage() {
        _uiState.update { state ->
            if (state.currentPage >= state.totalPages - 1) {
                state
            } else {
                state.withPagination(currentPage = state.currentPage + 1)
            }
        }
    }

    /**
     * Moves the joined rooms list to the previous page.
     */
    internal fun goToPreviousRoomsPage() {
        _uiState.update { state ->
            if (state.currentPage <= 0) {
                state
            } else {
                state.withPagination(currentPage = state.currentPage - 1)
            }
        }
    }

    internal fun acceptInvitation(invitationId: String) {
        updateInvitationProcessing(invitationId = invitationId, processing = true)

        viewModelScope.launch {
            when (val result = roomRepository.acceptRoomInvitation(invitationId)) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            processingInvitationIds = it.processingInvitationIds - invitationId,
                            infoMessageRes = R.string.room_invitation_accepted,
                            errorMessageRes = null,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            processingInvitationIds = it.processingInvitationIds - invitationId,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    internal fun rejectInvitation(invitationId: String) {
        updateInvitationProcessing(invitationId = invitationId, processing = true)

        viewModelScope.launch {
            when (val result = roomRepository.rejectRoomInvitation(invitationId)) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            processingInvitationIds = it.processingInvitationIds - invitationId,
                            infoMessageRes = R.string.room_invitation_rejected,
                            errorMessageRes = null,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            processingInvitationIds = it.processingInvitationIds - invitationId,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    private fun observeRooms() {
        val user = currentUser ?: return

        viewModelScope.launch(Dispatchers.Default) {
            combine(
                roomRepository.observeOwnedRooms(user.uid),
                roomRepository.observeMemberRooms(user.uid),
                roomRepository.observeCollaboratingRooms(user.uid),
            ) { ownedResult, memberResult, collaboratingResult ->
                Triple(ownedResult, memberResult, collaboratingResult)
            }.collect { (ownedResult, memberResult, collaboratingResult) ->
                when {
                    ownedResult is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingRooms = false,
                                errorMessageRes = ownedResult.messageRes,
                            )
                        }
                    }

                    memberResult is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingRooms = false,
                                errorMessageRes = memberResult.messageRes,
                            )
                        }
                    }

                    collaboratingResult is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingRooms = false,
                                errorMessageRes = collaboratingResult.messageRes,
                            )
                        }
                    }

                    ownedResult is RoomOperationResult.Success &&
                        memberResult is RoomOperationResult.Success &&
                        collaboratingResult is RoomOperationResult.Success -> {
                        val ownedRooms = ownedResult.data.map { it.toHomeRoomUiState(HomeRoomRole.OWNER) }
                        val collaboratingRooms =
                            collaboratingResult.data.map {
                                it.toHomeRoomUiState(HomeRoomRole.COLLABORATOR)
                            }
                        val memberRooms = memberResult.data.map { it.toHomeRoomUiState(HomeRoomRole.MEMBER) }
                        val rooms =
                            (ownedRooms + collaboratingRooms + memberRooms)
                                .distinctBy(HomeRoomUiState::id)
                                .sortedByDescending(HomeRoomUiState::createdAtEpochMillis)

                        _uiState.update {
                            it
                                .copy(
                                    rooms = rooms,
                                    isLoadingRooms = false,
                                    joinedRoomsCount = rooms.size,
                                    unansweredQuizCount = rooms.sumOf(HomeRoomUiState::unansweredQuizCount),
                                    errorMessageRes = null,
                                ).withPagination(currentPage = it.currentPage)
                        }
                    }
                }
            }
        }
    }

    private fun observePendingInvitations() {
        val user = currentUser ?: return

        viewModelScope.launch(Dispatchers.Default) {
            roomRepository.observePendingRoomInvitations(user.uid).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                pendingInvitations = result.data.map(RoomInvitation::toHomeInvitationUiState),
                                errorMessageRes = null,
                            )
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update { it.copy(errorMessageRes = result.messageRes) }
                    }
                }
            }
        }
    }

    private fun updateInvitationProcessing(
        invitationId: String,
        processing: Boolean,
    ) {
        _uiState.update {
            it.copy(
                processingInvitationIds =
                    if (processing) {
                        it.processingInvitationIds + invitationId
                    } else {
                        it.processingInvitationIds - invitationId
                    },
                errorMessageRes = null,
                infoMessageRes = null,
            )
        }
    }
}

private fun Room.toHomeRoomUiState(role: HomeRoomRole): HomeRoomUiState =
    HomeRoomUiState(
        id = id,
        name = name,
        topic = topic,
        cefrLevel = cefrLevel,
        participantCount = participantCount,
        unansweredQuizCount = unansweredQuizCount,
        ownerName = ownerName,
        role = role,
        createdAtEpochMillis = createdAtEpochMillis,
    )

private fun RoomInvitation.toHomeInvitationUiState(): HomeInvitationUiState =
    HomeInvitationUiState(
        id = id,
        roomName = roomName,
        inviterName = inviterName,
        access = access,
        createdAtEpochMillis = createdAtEpochMillis,
    )

private fun HomeUiState.withPagination(currentPage: Int): HomeUiState {
    val totalPages = if (rooms.isEmpty()) 0 else ((rooms.size - 1) / JOINED_ROOMS_PAGE_SIZE) + 1
    val safePage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val startIndex = safePage * JOINED_ROOMS_PAGE_SIZE
    val endIndex = (startIndex + JOINED_ROOMS_PAGE_SIZE).coerceAtMost(rooms.size)

    return copy(
        pagedRooms = rooms.subList(startIndex, endIndex),
        currentPage = if (totalPages == 0) 0 else safePage,
        totalPages = totalPages,
    )
}

private fun AuthUser?.toInitials(): String = displayNameOrFallback().toInitials()

private fun AuthUser?.displayNameOrFallback(): String =
    this
        ?.displayName
        ?.takeIf(String::isNotBlank)
        ?: this?.email.orPrettyEmailLocalPart(this?.email)

private fun String.toInitials(): String =
    split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "?" }

private const val JOINED_ROOMS_PAGE_SIZE = 4
