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
    val liveQuizCount: Int
)

/**
 * UI state for the authenticated home feature.
 */
internal data class HomeUiState(
    val userName: String = "",
    val featuredRoomName: String = "",
    val joinedRoomsCount: Int = 0,
    val liveQuizCount: Int = 0,
    val rooms: List<HomeRoomUiState> = emptyList(),
    val isLoadingRooms: Boolean = true,
    val isCreatingRoom: Boolean = false,
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
            userName = currentUser.displayNameOrEmailName(),
            isLoadingRooms = currentUser != null,
            errorMessageRes = if (currentUser == null) R.string.error_room_auth_required else null
        )
    )
    internal val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRooms()
    }

    /**
     * Creates a new Firestore room with an auto-generated name and topic.
     */
    internal fun createRoom() {
        val user = currentUser ?: run {
            _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
            return
        }
        if (_uiState.value.isCreatingRoom) return

        val roomIndex = _uiState.value.rooms.size + 1
        val roomDraft = buildRoomDraft(user, roomIndex)

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
                        name = roomDraft.name,
                        topic = roomDraft.topic
                    )
                )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRoom = false,
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
     * Signs out the current user from Firebase Authentication.
     */
    internal fun logout() {
        authRepository.signOut()
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
                                liveQuizCount = rooms.sumOf(HomeRoomUiState::liveQuizCount),
                                featuredRoomName = rooms.firstOrNull()?.name.orEmpty(),
                                errorMessageRes = null
                            )
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

    private fun buildRoomDraft(user: AuthUser, roomIndex: Int): RoomDraft {
        val topic = ROOM_TOPICS[(roomIndex - 1) % ROOM_TOPICS.size]
        val ownerName = user.displayNameOrEmailName()
            .substringBefore(" ")
            .ifBlank { "SmartRooms" }

        return RoomDraft(
            name = "$ownerName Room $roomIndex",
            topic = topic
        )
    }
}

private data class RoomDraft(
    val name: String,
    val topic: String
)

private fun Room.toHomeRoomUiState(): HomeRoomUiState {
    return HomeRoomUiState(
        id = id,
        name = name,
        topic = topic,
        participantCount = participantCount,
        liveQuizCount = liveQuizCount
    )
}

private fun AuthUser?.displayNameOrEmailName(): String {
    return this?.displayName
        ?.takeIf(String::isNotBlank)
        ?: this?.email?.substringBefore("@").orEmpty()
}

private val ROOM_TOPICS = listOf(
    "Kotlin",
    "Firebase",
    "Jetpack Compose",
    "Databases",
    "Algorithms"
)
