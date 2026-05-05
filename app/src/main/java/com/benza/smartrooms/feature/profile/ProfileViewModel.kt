package com.benza.smartrooms.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.data.userprofile.model.TeacherApprovalStatus
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the authenticated profile screen.
 */
internal data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val initials: String = "?",
    val role: UserRole? = null,
    val teacherApprovalStatus: TeacherApprovalStatus = TeacherApprovalStatus.NONE,
    val ownedRoomCount: Int = 0,
    val memberRoomCount: Int = 0,
    val collaboratingRoomCount: Int = 0,
    val isLoadingProfile: Boolean = true,
    val isLoadingRooms: Boolean = true,
    val errorMessageRes: Int? = null,
)

/**
 * Exposes current-account information, profile metadata, room stats, and logout behavior.
 */
internal class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()

    private val _uiState =
        MutableStateFlow(
            ProfileUiState(
                displayName = currentUser.displayNameOrFallback(),
                email = currentUser?.email.orEmpty(),
                initials = currentUser.toInitials(),
                isLoadingProfile = currentUser != null,
                isLoadingRooms = currentUser != null,
                errorMessageRes = if (currentUser == null) R.string.error_user_profile_auth_required else null,
            ),
        )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        observeRoomStats()
    }

    /**
     * Signs out the current user from Firebase Authentication.
     */
    internal fun logout() {
        authRepository.signOut()
    }

    private fun observeProfile() {
        val user = currentUser ?: return

        viewModelScope.launch {
            userProfileRepository.observeProfile(user).collect { result ->
                when (result) {
                    is UserProfileOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                displayName = result.data.displayName,
                                email = result.data.email,
                                initials = result.data.displayName.toInitials(),
                                role = result.data.role,
                                teacherApprovalStatus = result.data.teacherApprovalStatus,
                                isLoadingProfile = false,
                                errorMessageRes = null,
                            )
                        }
                    }

                    is UserProfileOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingProfile = false,
                                errorMessageRes = result.messageRes,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeRoomStats() {
        val user = currentUser ?: return

        viewModelScope.launch {
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
                        _uiState.update {
                            it.copy(
                                ownedRoomCount = ownedResult.data.size,
                                memberRoomCount = memberResult.data.size,
                                collaboratingRoomCount = collaboratingResult.data.size,
                                isLoadingRooms = false,
                                errorMessageRes = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun AuthUser?.displayNameOrFallback(): String =
    this
        ?.displayName
        ?.takeIf(String::isNotBlank)
        ?: this?.email?.substringBefore("@").orEmpty()

private fun AuthUser?.toInitials(): String = this?.displayNameOrFallback().orEmpty().toInitials()

private fun String.toInitials(): String {
    val source =
        split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.take(1).uppercase() }

    return source.ifBlank { "?" }
}
