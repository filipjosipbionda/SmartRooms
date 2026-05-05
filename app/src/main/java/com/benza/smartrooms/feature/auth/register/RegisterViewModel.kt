package com.benza.smartrooms.feature.auth.register

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
 * UI state for the registration feature.
 */
internal data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val fullNameErrorRes: Int? = null,
    val emailErrorRes: Int? = null,
    val passwordErrorRes: Int? = null,
    val confirmPasswordErrorRes: Int? = null,
    val generalMessageRes: Int? = null,
)

/**
 * Handles validation and Firebase-backed account creation.
 */
internal class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * Updates the full-name input and clears stale validation feedback.
     */
    internal fun onFullNameChanged(value: String) {
        _uiState.update {
            it.copy(
                fullName = value,
                fullNameErrorRes = null,
                generalMessageRes = null,
            )
        }
    }

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
                confirmPasswordErrorRes = null,
                generalMessageRes = null,
            )
        }
    }

    /**
     * Updates the confirm-password input and clears stale validation feedback.
     */
    internal fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordErrorRes = null,
                generalMessageRes = null,
            )
        }
    }

    /**
     * Toggles password visibility in the registration form.
     */
    internal fun onPasswordVisibilityToggled() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * Toggles confirmation password visibility in the registration form.
     */
    internal fun onConfirmPasswordVisibilityToggled() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    /**
     * Validates the form and triggers the account creation request.
     */
    internal fun submit() {
        val state = _uiState.value
        val fullName = state.fullName.trim()
        val email = state.email.trim()
        val fullNameError =
            when {
                fullName.isBlank() -> R.string.error_full_name_required
                else -> null
            }
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
        val confirmPasswordError =
            if (state.confirmPassword != state.password) {
                R.string.error_passwords_do_not_match
            } else {
                null
            }
        val isValid =
            listOf(
                fullNameError,
                emailError,
                passwordError,
                confirmPasswordError,
            ).all { it == null }

        _uiState.update {
            it.copy(
                fullName = fullName,
                email = email,
                fullNameErrorRes = fullNameError,
                emailErrorRes = emailError,
                passwordErrorRes = passwordError,
                confirmPasswordErrorRes = confirmPasswordError,
                generalMessageRes = if (isValid) null else R.string.error_review_account_details,
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
            when (
                val result =
                    authRepository.register(
                        fullName = fullName,
                        email = email,
                        password = state.password,
                    )
            ) {
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
