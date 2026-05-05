package com.benza.smartrooms.feature.auth.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthOperationResult
import com.benza.smartrooms.data.auth.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the login feature.
 */
internal data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val emailErrorRes: Int? = null,
    val passwordErrorRes: Int? = null,
    val generalMessageRes: Int? = null,
)

/**
 * Handles validation and Firebase-backed login requests.
 */
internal class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Updates the email input and clears stale validation feedback.
     */
    internal fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailErrorRes = null,
                generalMessageRes = null,
            )
        }
    }

    /**
     * Updates the password input and clears stale validation feedback.
     */
    internal fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordErrorRes = null,
                generalMessageRes = null,
            )
        }
    }

    /**
     * Toggles password visibility in the login form.
     */
    internal fun onPasswordVisibilityToggled() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * Validates the form and triggers the login request.
     */
    internal fun submit() {
        val state = _uiState.value
        val email = state.email.trim()
        val emailError =
            when {
                email.isBlank() -> R.string.error_email_required
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.error_email_invalid
                else -> null
            }
        val passwordError =
            when {
                state.password.isBlank() -> R.string.error_password_required
                state.password.length < 8 -> R.string.error_password_length
                else -> null
            }
        val isValid = emailError == null && passwordError == null

        _uiState.update {
            it.copy(
                email = email,
                emailErrorRes = emailError,
                passwordErrorRes = passwordError,
                generalMessageRes = if (isValid) null else R.string.error_fix_highlighted_fields,
            )
        }

        if (!isValid) return

        _uiState.update {
            it.copy(
                isLoading = true,
                generalMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (val result = authRepository.login(email, state.password)) {
                is AuthOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            generalMessageRes = null,
                        )
                    }
                }

                is AuthOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    /**
     * Resets the one-shot authenticated flag after navigation is handled.
     */
    internal fun onAuthenticationHandled() {
        _uiState.update {
            it.copy(isAuthenticated = false)
        }
    }
}
