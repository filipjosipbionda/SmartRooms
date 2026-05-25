package com.benza.smartrooms.feature.createroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.CreateRoomRequest
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.util.orPrettyEmailLocalPart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CreateRoomUiState(
    val roomNameInput: String = "",
    val roomTopicInput: String = "",
    val selectedCefrLevel: String = "",
    val roomNameErrorRes: Int? = null,
    val roomTopicErrorRes: Int? = null,
    val cefrLevelErrorRes: Int? = null,
    val isCreatingRoom: Boolean = false,
    val isRoomCreated: Boolean = false,
    val errorMessageRes: Int? = null,
)

internal class CreateRoomViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateRoomUiState())
    val uiState: StateFlow<CreateRoomUiState> = _uiState.asStateFlow()

    internal fun onRoomNameChanged(value: String) {
        _uiState.update {
            it.copy(
                roomNameInput = value,
                roomNameErrorRes = null,
                errorMessageRes = null,
            )
        }
    }

    internal fun onRoomTopicChanged(value: String) {
        _uiState.update {
            it.copy(
                roomTopicInput = value,
                roomTopicErrorRes = null,
                errorMessageRes = null,
            )
        }
    }

    internal fun onCefrLevelSelected(value: String) {
        _uiState.update {
            it.copy(
                selectedCefrLevel = value,
                cefrLevelErrorRes = null,
                errorMessageRes = null,
            )
        }
    }

    internal fun consumeRoomCreated() {
        _uiState.update { it.copy(isRoomCreated = false) }
    }

    internal fun createRoom() {
        val user =
            authRepository.getCurrentUser() ?: run {
                _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
                return
            }
        if (_uiState.value.isCreatingRoom) return

        val roomName = _uiState.value.roomNameInput.trim()
        val roomTopic = _uiState.value.roomTopicInput.trim()
        val cefrLevel = _uiState.value.selectedCefrLevel.trim()
        val roomNameError = if (roomName.isBlank()) R.string.error_room_name_required else null
        val roomTopicError = if (roomTopic.isBlank()) R.string.error_room_topic_required else null
        val cefrLevelError = if (cefrLevel.isBlank()) R.string.error_room_cefr_required else null

        _uiState.update {
            it.copy(
                roomNameInput = roomName,
                roomTopicInput = roomTopic,
                selectedCefrLevel = cefrLevel,
                roomNameErrorRes = roomNameError,
                roomTopicErrorRes = roomTopicError,
                cefrLevelErrorRes = cefrLevelError,
                errorMessageRes =
                    if (
                        roomNameError == null &&
                        roomTopicError == null &&
                        cefrLevelError == null
                    ) {
                        null
                    } else {
                        R.string.error_room_fix_fields
                    },
            )
        }

        if (roomNameError != null || roomTopicError != null || cefrLevelError != null) return

        _uiState.update {
            it.copy(
                isCreatingRoom = true,
                errorMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (
                val result =
                    roomRepository.createRoom(
                        CreateRoomRequest(
                            ownerId = user.uid,
                            ownerName = user.displayNameOrEmailName(),
                            name = roomName,
                            topic = roomTopic,
                            cefrLevel = cefrLevel,
                        ),
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRoom = false,
                            isRoomCreated = true,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreatingRoom = false,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }
}

private fun AuthUser.displayNameOrEmailName(): String =
    displayName
        ?.takeIf(String::isNotBlank)
        ?: email.orPrettyEmailLocalPart(email)
