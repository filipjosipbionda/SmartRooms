package com.benza.smartrooms.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.Room
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI model used by the home screen room list.
 */
internal data class HomeRoomUiState(
    val id: String,
    val name: String,
    val topic: String,
    val participantCount: Int,
    val unansweredQuizCount: Int
)

/**
 * UI state for the authenticated home feature.
 */
internal data class HomeUiState(
    val profileInitials: String = "?",
    val joinedRoomsCount: Int = 0,
    val unansweredQuizCount: Int = 0,
    val rooms: List<HomeRoomUiState> = emptyList(),
    val pagedRooms: List<HomeRoomUiState> = emptyList(),
    val isLoadingRooms: Boolean = true,
    val isCreatingRoom: Boolean = false,
    val isCreateRoomDialogOpen: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val roomNameInput: String = "",
    val roomTopicInput: String = "",
    val roomNameErrorRes: Int? = null,
    val roomTopicErrorRes: Int? = null,
    val errorMessageRes: Int? = null,
    val infoMessageRes: Int? = null
)

/**
 * Provides Firestore-backed room state and exposes logout / room creation behavior.
 */
internal class HomeViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            profileInitials = currentUser.toInitials(),
            isLoadingRooms = currentUser != null,
            errorMessageRes = if (currentUser == null) R.string.error_room_auth_required else null
        )
    )
    internal val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRooms()
    }

    /**
     * Opens the create-room dialog.
     */
    internal fun showCreateRoomDialog() {
        _uiState.update {
            it.copy(
                isCreateRoomDialogOpen = true,
                roomNameErrorRes = null,
                roomTopicErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    /**
     * Closes the create-room dialog and clears its transient validation state.
     */
    internal fun dismissCreateRoomDialog() {
        if (_uiState.value.isCreatingRoom) return

        _uiState.update {
            it.copy(
                isCreateRoomDialogOpen = false,
                roomNameInput = "",
                roomTopicInput = "",
                roomNameErrorRes = null,
                roomTopicErrorRes = null
            )
        }
    }

    /**
     * Updates the pending room name input.
     */
    internal fun onRoomNameChanged(value: String) {
        _uiState.update {
            it.copy(
                roomNameInput = value,
                roomNameErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    /**
     * Updates the pending room topic input.
     */
    internal fun onRoomTopicChanged(value: String) {
        _uiState.update {
            it.copy(
                roomTopicInput = value,
                roomTopicErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    /**
     * Creates a new Firestore room from dialog input.
     */
    internal fun createRoom() {
        val user = currentUser ?: run {
            _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
            return
        }
        if (_uiState.value.isCreatingRoom) return

        val roomName = _uiState.value.roomNameInput.trim()
        val roomTopic = _uiState.value.roomTopicInput.trim()
        val roomNameError = if (roomName.isBlank()) R.string.error_room_name_required else null
        val roomTopicError = if (roomTopic.isBlank()) R.string.error_room_topic_required else null

        _uiState.update {
            it.copy(
                roomNameInput = roomName,
                roomTopicInput = roomTopic,
                roomNameErrorRes = roomNameError,
                roomTopicErrorRes = roomTopicError,
                errorMessageRes = if (roomNameError == null && roomTopicError == null) {
                    null
                } else {
                    R.string.error_room_fix_fields
                }
            )
        }

        if (roomNameError != null || roomTopicError != null) return

        _uiState.update {
            it.copy(
                isCreatingRoom = true,
                errorMessageRes = null,
                infoMessageRes = null
            )
        }

        viewModelScope.launch {
            when (
                val result = roomRepository.createRoom(
                    CreateRoomRequest(
                        ownerId = user.uid,
                        ownerName = user.displayNameOrEmailName(),
                        name = roomName,
                        topic = roomTopic
                    )
                )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRoom = false,
                            isCreateRoomDialogOpen = false,
                            roomNameInput = "",
                            roomTopicInput = "",
                            roomNameErrorRes = null,
                            roomTopicErrorRes = null,
                            infoMessageRes = R.string.home_room_created
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRoom = false,
                            errorMessageRes = result.messageRes
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

    private fun observeRooms() {
        val user = currentUser ?: return

        viewModelScope.launch {
            roomRepository.observeRooms(user.uid).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        val rooms = result.data.map(Room::toHomeRoomUiState)
                        _uiState.update {
                            it.copy(
                                rooms = rooms,
                                isLoadingRooms = false,
                                joinedRoomsCount = rooms.size,
                                unansweredQuizCount = rooms.sumOf(HomeRoomUiState::unansweredQuizCount),
                                errorMessageRes = null
                            ).withPagination(currentPage = it.currentPage)
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingRooms = false,
                                errorMessageRes = result.messageRes
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Room.toHomeRoomUiState(): HomeRoomUiState {
    return HomeRoomUiState(
        id = id,
        name = name,
        topic = topic,
        participantCount = participantCount,
        unansweredQuizCount = unansweredQuizCount
    )
}

private fun HomeUiState.withPagination(currentPage: Int): HomeUiState {
    val totalPages = if (rooms.isEmpty()) 0 else ((rooms.size - 1) / JOINED_ROOMS_PAGE_SIZE) + 1
    val safePage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val startIndex = safePage * JOINED_ROOMS_PAGE_SIZE
    val endIndex = (startIndex + JOINED_ROOMS_PAGE_SIZE).coerceAtMost(rooms.size)

    return copy(
        pagedRooms = rooms.subList(startIndex, endIndex),
        currentPage = if (totalPages == 0) 0 else safePage,
        totalPages = totalPages
    )
}

private fun AuthUser?.displayNameOrEmailName(): String {
    return this?.displayName
        ?.takeIf(String::isNotBlank)
        ?: this?.email?.substringBefore("@").orEmpty()
}

private fun AuthUser?.toInitials(): String {
    return displayNameOrEmailName()
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "?" }
}

private const val JOINED_ROOMS_PAGE_SIZE = 4
