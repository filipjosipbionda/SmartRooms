package com.benza.smartrooms.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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
 * Route-level login entry that binds [LoginViewModel] to the stateless login screen.
 */
@Composable
internal fun LoginRouteScreen(
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.value.isAuthenticated) {
        if (uiState.value.isAuthenticated) {
            onLoginSuccess()
            viewModel.onAuthenticationHandled()
        }
    }

    LoginScreen(
        uiState = uiState.value,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggled,
        onLoginClick = viewModel::submit,
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
    )
}

/**
 * Stateless login UI.
 */
@Composable
internal fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
    ) {
        if (uiState.generalMessageRes != null) {
            AuthFeedbackBanner(
                message = stringResource(uiState.generalMessageRes),
                type = AuthFeedbackType.Error,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onForgotPasswordClick,
                enabled = !uiState.isLoading,
            ) {
                Text(stringResource(R.string.action_forgot_password))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        AuthPrimaryButton(
            text =
                stringResource(
                    if (uiState.isLoading) R.string.action_signing_in else R.string.action_sign_in,
                ),
            onClick = onLoginClick,
            enabled = !uiState.isLoading,
            loading = uiState.isLoading,
        )
        Spacer(modifier = Modifier.height(12.dp))
        InlineActionText(
            leadingText = stringResource(R.string.login_new_user),
            actionText = stringResource(R.string.action_create_account),
            onActionClick = onRegisterClick,
            enabled = !uiState.isLoading,
        )
    }
}

/**
 * Preview for the login screen.
 */
@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    SmartRoomsTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onLoginClick = {},
            onRegisterClick = {},
            onForgotPasswordClick = {},
        )
    }
}
