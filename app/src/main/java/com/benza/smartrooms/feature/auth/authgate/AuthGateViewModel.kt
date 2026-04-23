package com.benza.smartrooms.feature.auth.authgate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.data.auth.repository.AuthRepository
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

internal data class AuthGateUiState(
    val isLoading: Boolean = true,
    val errorMessageRes: Int? = null
)

internal sealed interface AuthGateEvent {
    data object NavigateToLogin : AuthGateEvent
    data object NavigateToRoleSelection : AuthGateEvent
    data object NavigateToHome : AuthGateEvent
}

/**
 * Resolves startup and post-auth navigation based on auth session and Firestore profile state.
 */
internal class AuthGateViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthGateUiState())
    internal val uiState: StateFlow<AuthGateUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthGateEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val events: SharedFlow<AuthGateEvent> = _events.asSharedFlow()

    internal fun retry() {
        resolve()
    }

    internal fun resolve() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageRes = null
            )
        }

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update { it.copy(isLoading = false) }
            _events.tryEmit(AuthGateEvent.NavigateToLogin)
            return
        }

        viewModelScope.launch {
            when (val result = userProfileRepository.ensureProfile(currentUser)) {
                is UserProfileOperationResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }

                    val event = if (result.data.profileComplete && result.data.role != null) {
                        AuthGateEvent.NavigateToHome
                    } else {
                        AuthGateEvent.NavigateToRoleSelection
                    }
                    _events.emit(event)
                }

                is UserProfileOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = result.messageRes
                        )
                    }
                }
            }
        }
    }
}
