package com.benza.smartrooms.feature.roominvite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.CreateRoomInvitationRequest
import com.benza.smartrooms.data.room.model.RoomInvitationAccess
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import com.benza.smartrooms.util.orPrettyEmailLocalPart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class RoomInviteSearchUserUiState(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val role: UserRole?,
    val access: RoomInvitationAccess,
)

internal data class RoomInviteUiState(
    val roomId: String,
    val roomName: String,
    val memberIds: List<String> = emptyList(),
    val collaboratorIds: List<String> = emptyList(),
    val query: String = "",
    val results: List<RoomInviteSearchUserUiState> = emptyList(),
    val emptyMessageRes: Int? = null,
    val isLoadingRoom: Boolean = true,
    val isSearching: Boolean = false,
    val isSendingInvite: Boolean = false,
    val errorMessageRes: Int? = null,
    val infoMessageRes: Int? = null,
)

internal class RoomInviteViewModel(
    roomId: String,
    roomName: String,
    private val roomRepository: RoomRepository,
    authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()
    private var searchJob: Job? = null

    private val _uiState =
        MutableStateFlow(
            RoomInviteUiState(
                roomId = roomId,
                roomName = roomName,
            ),
        )
    val uiState: StateFlow<RoomInviteUiState> = _uiState.asStateFlow()

    init {
        observeRoom()
    }

    internal fun onQueryChanged(value: String) {
        val query = value.trimStart()
        searchJob?.cancel()

        _uiState.update {
            it.copy(
                query = query,
                results = if (query.length < MIN_QUERY_LENGTH) emptyList() else it.results,
                emptyMessageRes = null,
                isSearching = query.length >= MIN_QUERY_LENGTH,
                errorMessageRes = null,
            )
        }

        if (query.length < MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(isSearching = false) }
            return
        }

        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                when (val result = userProfileRepository.searchProfiles(query)) {
                    is UserProfileOperationResult.Success -> applySearchResults(result.data)
                    is UserProfileOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                errorMessageRes = result.messageRes,
                            )
                        }
                    }
                }
            }
    }

    internal fun sendInvite(user: RoomInviteSearchUserUiState) {
        val current =
            currentUser ?: run {
                _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
                return
            }
        val state = _uiState.value
        if (state.isSendingInvite) return

        _uiState.update {
            it.copy(
                isSendingInvite = true,
                errorMessageRes = null,
                infoMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (
                val result =
                    roomRepository.createRoomInvitation(
                        CreateRoomInvitationRequest(
                            roomId = state.roomId,
                            roomName = state.roomName,
                            inviterId = current.uid,
                            inviterName = current.displayNameOrEmailName(),
                            inviteeId = user.uid,
                            inviteeEmail = user.email,
                            inviteeDisplayName = user.displayName,
                            access = user.access,
                        ),
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isSendingInvite = false,
                            results = currentState.results.filterNot { it.uid == user.uid },
                            emptyMessageRes =
                                if (currentState.results.size <= 1) {
                                    R.string.room_invite_search_empty
                                } else {
                                    null
                                },
                            infoMessageRes = R.string.room_invite_sent,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSendingInvite = false,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    internal fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessageRes = null) }
    }

    private fun observeRoom() {
        viewModelScope.launch {
            roomRepository.observeRoom(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                memberIds = result.data.memberIds,
                                collaboratorIds = result.data.collaboratorIds,
                                isLoadingRoom = false,
                                errorMessageRes = null,
                            )
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingRoom = false,
                                errorMessageRes = result.messageRes,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applySearchResults(profiles: List<UserProfile>) {
        _uiState.update { state ->
            val mappedResults = profiles.map(UserProfile::toInviteUserUiState)
            val containsCurrentUser = mappedResults.any { it.uid == currentUser?.uid }
            val containsExistingRoomUser =
                mappedResults.any { it.uid in state.memberIds || it.uid in state.collaboratorIds }
            val filteredResults =
                mappedResults
                    .filter { it.uid != currentUser?.uid }
                    .filterNot { it.uid in state.memberIds || it.uid in state.collaboratorIds }

            state.copy(
                results = filteredResults,
                emptyMessageRes =
                    when {
                        filteredResults.isNotEmpty() -> null
                        containsCurrentUser -> R.string.room_invite_search_self_hidden
                        containsExistingRoomUser -> R.string.room_invite_search_already_in_room
                        else -> R.string.room_invite_search_empty
                    },
                isSearching = false,
            )
        }
    }
}

private fun UserProfile.toInviteUserUiState(): RoomInviteSearchUserUiState {
    val access =
        when (role) {
            UserRole.TEACHER -> RoomInvitationAccess.COLLABORATOR
            UserRole.STUDENT, null -> RoomInvitationAccess.MEMBER
        }

    return RoomInviteSearchUserUiState(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        role = role,
        access = access,
    )
}

private fun AuthUser.displayNameOrEmailName(): String =
    displayName?.takeIf(String::isNotBlank) ?: email.orPrettyEmailLocalPart(email)

private const val MIN_QUERY_LENGTH = 2
private const val SEARCH_DEBOUNCE_MS = 300L
