package com.benza.smartrooms.feature.auth.forgotpassword

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
 * UI state for the forgot-password feature.
 */
internal data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val emailErrorRes: Int? = null,
    val generalMessageRes: Int? = null,
    val infoMessageRes: Int? = null,
)

/**
 * Handles validation and password reset email requests.
 */
internal class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /**
     * Updates the email input and clears stale feedback.
     */
    internal fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailErrorRes = null,
                generalMessageRes = null,
                infoMessageRes = null,
            )
        }
    }

    /**
     * Validates the email and triggers the password reset flow.
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

        _uiState.update {
            it.copy(
                email = email,
                emailErrorRes = emailError,
                generalMessageRes = null,
                infoMessageRes = null,
            )
        }

        if (emailError != null) return

        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is AuthOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            emailErrorRes = null,
                            generalMessageRes = null,
                            infoMessageRes = R.string.forgot_password_info,
                        )
                    }
                }

                is AuthOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalMessageRes = result.messageRes,
                            infoMessageRes = null,
                        )
                    }
                }
            }
        }
    }
}
