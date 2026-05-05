package com.benza.smartrooms.feature.auth.roleselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.userprofile.model.TeacherApprovalStatus
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class RoleSelectionUiState(
    val displayName: String = "",
    val email: String = "",
    val teacherApprovalStatus: TeacherApprovalStatus = TeacherApprovalStatus.NONE,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = true,
    val errorMessageRes: Int? = null,
)

internal sealed interface RoleSelectionEvent {
    data object NavigateToHome : RoleSelectionEvent

    data object NavigateToLogin : RoleSelectionEvent
}

/**
 * Handles the one-time user role selection flow.
 */
internal class RoleSelectionViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()
    private var hasPendingNavigation = false

    private val _uiState =
        MutableStateFlow(
            RoleSelectionUiState(
                displayName = currentUser.displayNameOrFallback(),
                email = currentUser?.email.orEmpty(),
            ),
        )
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    private val _events =
        MutableSharedFlow<RoleSelectionEvent>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<RoleSelectionEvent> = _events.asSharedFlow()

    init {
        refreshProfile()
        observeProfile()
    }

    internal fun selectStudentRole() {
        val user =
            currentUser ?: run {
                _uiState.update { it.copy(errorMessageRes = R.string.error_user_profile_auth_required) }
                return
            }
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (val result = userProfileRepository.selectStudentRole(user.uid)) {
                is UserProfileOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                        )
                    }
                    emitNavigationEvent(RoleSelectionEvent.NavigateToHome)
                }

                is UserProfileOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    internal fun submitTeacherRequest() {
        val user =
            currentUser ?: run {
                _uiState.update { it.copy(errorMessageRes = R.string.error_user_profile_auth_required) }
                return
            }
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (val result = userProfileRepository.submitTeacherRequest(user)) {
                is UserProfileOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            teacherApprovalStatus = TeacherApprovalStatus.PENDING,
                        )
                    }
                }

                is UserProfileOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    internal fun refreshProfile() {
        val user =
            currentUser ?: run {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessageRes = R.string.error_user_profile_auth_required,
                    )
                }
                return
            }

        _uiState.update {
            it.copy(
                isRefreshing = true,
                errorMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (val result = userProfileRepository.ensureProfile(user)) {
                is UserProfileOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            displayName = result.data.displayName,
                            email = result.data.email,
                            teacherApprovalStatus = result.data.teacherApprovalStatus,
                            isRefreshing = false,
                        )
                    }
                    if (result.data.profileComplete && result.data.role != null) {
                        emitNavigationEvent(RoleSelectionEvent.NavigateToHome)
                    }
                }

                is UserProfileOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
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
                                teacherApprovalStatus = result.data.teacherApprovalStatus,
                                isRefreshing = false,
                                isLoading = false,
                                errorMessageRes = null,
                            )
                        }
                        if (result.data.profileComplete && result.data.role != null) {
                            emitNavigationEvent(RoleSelectionEvent.NavigateToHome)
                        }
                    }

                    is UserProfileOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                isLoading = false,
                                errorMessageRes = result.messageRes,
                            )
                        }
                    }
                }
            }
        }
    }

    internal fun logout() {
        authRepository.signOut()
        emitNavigationEvent(RoleSelectionEvent.NavigateToLogin)
    }

    private fun emitNavigationEvent(event: RoleSelectionEvent) {
        if (hasPendingNavigation) return

        hasPendingNavigation = true
        _events.tryEmit(event)
    }
}

private fun AuthUser?.displayNameOrFallback(): String =
    this
        ?.displayName
        ?.takeIf(String::isNotBlank)
        ?: this?.email?.substringBefore("@").orEmpty()
