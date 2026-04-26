package com.benza.smartrooms.feature.auth.authgate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.userprofile.model.StartupDestination
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal sealed interface AuthGateState {
    data object Loading : AuthGateState
    data class Error(val messageRes: Int) : AuthGateState
    data class Resolved(val destination: StartupDestination) : AuthGateState
}

internal data class AuthGateUiState(
    val state: AuthGateState = AuthGateState.Loading
)

/**
 * Resolves startup and post-auth navigation based on auth session and backend user state.
 */
internal class AuthGateViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthGateUiState())
    internal val uiState: StateFlow<AuthGateUiState> = _uiState.asStateFlow()

    internal fun retry() {
        resolve()
    }

    init {
        resolve()
    }

    internal fun resolve() {
        _uiState.update {
            it.copy(state = AuthGateState.Loading)
        }

        if (authRepository.getCurrentUser() == null) {
            _uiState.update {
                it.copy(state = AuthGateState.Resolved(StartupDestination.LOGIN))
            }
            return
        }

        viewModelScope.launch {
            when (val result = userProfileRepository.resolveStartupReadiness()) {
                is UserProfileOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            state = AuthGateState.Resolved(
                                if (result.data.isReady) {
                                    StartupDestination.HOME
                                } else {
                                    StartupDestination.ROLE_SELECTION
                                }
                            )
                        )
                    }
                }

                is UserProfileOperationResult.Error -> {
                    _uiState.update {
                        it.copy(state = AuthGateState.Error(result.messageRes))
                    }
                }
            }
        }
    }
}
