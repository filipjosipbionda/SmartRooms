package com.benza.smartrooms.feature.auth.register

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.components.AuthPrimaryButton
import com.benza.smartrooms.ui.components.AuthScaffold
import com.benza.smartrooms.ui.components.AuthTextField
import com.benza.smartrooms.ui.components.InlineActionText
import com.benza.smartrooms.ui.components.PasswordField
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Route-level register entry that binds [RegisterViewModel] to the stateless register screen.
 */
@Composable
internal fun RegisterRouteScreen(
    onBackToLoginClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.value.isAuthenticated) {
        if (uiState.value.isAuthenticated) {
            onRegisterSuccess()
            viewModel.onAuthenticationHandled()
        }
    }

    RegisterScreen(
        uiState = uiState.value,
        onFullNameChange = viewModel::onFullNameChanged,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggled,
        onConfirmPasswordVisibilityToggle = viewModel::onConfirmPasswordVisibilityToggled,
        onRegisterClick = viewModel::submit,
        onBackToLoginClick = onBackToLoginClick,
    )
}

/**
 * Stateless registration UI.
 */
@Composable
internal fun RegisterScreen(
    uiState: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLoginClick: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
    ) {
        if (uiState.generalMessageRes != null) {
            AuthFeedbackBanner(
                message = stringResource(uiState.generalMessageRes),
                type = AuthFeedbackType.Error,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        AuthTextField(
            value = uiState.fullName,
            onValueChange = onFullNameChange,
            label = stringResource(R.string.label_full_name),
            enabled = !uiState.isLoading,
            supportingText = uiState.fullNameErrorRes?.let { stringResource(it) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.label_email),
            enabled = !uiState.isLoading,
            supportingText = uiState.emailErrorRes?.let { stringResource(it) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        PasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.label_password),
            passwordVisible = uiState.passwordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle,
            enabled = !uiState.isLoading,
            supportingText = uiState.passwordErrorRes?.let { stringResource(it) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        PasswordField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.label_confirm_password),
            passwordVisible = uiState.confirmPasswordVisible,
            onVisibilityToggle = onConfirmPasswordVisibilityToggle,
            enabled = !uiState.isLoading,
            supportingText = uiState.confirmPasswordErrorRes?.let { stringResource(it) },
        )
        Spacer(modifier = Modifier.height(20.dp))
        AuthPrimaryButton(
            text =
                stringResource(
                    if (uiState.isLoading) R.string.action_creating_account else R.string.action_create_account,
                ),
            onClick = onRegisterClick,
            enabled = !uiState.isLoading,
            loading = uiState.isLoading,
        )
        Spacer(modifier = Modifier.height(12.dp))
        InlineActionText(
            leadingText = stringResource(R.string.register_existing_user),
            actionText = stringResource(R.string.action_back_to_sign_in),
            onActionClick = onBackToLoginClick,
            enabled = !uiState.isLoading,
        )
    }
}

/**
 * Preview for the registration screen.
 */
@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    SmartRoomsTheme {
        RegisterScreen(
            uiState = RegisterUiState(),
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onConfirmPasswordVisibilityToggle = {},
            onRegisterClick = {},
            onBackToLoginClick = {},
        )
    }
}
