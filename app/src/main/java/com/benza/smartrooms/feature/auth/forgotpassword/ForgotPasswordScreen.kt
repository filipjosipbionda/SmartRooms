package com.benza.smartrooms.feature.auth.forgotpassword

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Route-level forgot-password entry that binds [ForgotPasswordViewModel] to the stateless screen.
 */
@Composable
internal fun ForgotPasswordRouteScreen(
    onBackToLoginClick: () -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordScreen(
        uiState = uiState.value,
        onEmailChange = viewModel::onEmailChanged,
        onSubmitClick = viewModel::submit,
        onBackToLoginClick = onBackToLoginClick
    )
}

/**
 * Stateless forgot-password UI.
 */
@Composable
internal fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onBackToLoginClick: () -> Unit
) {
    AuthScaffold(
        title = stringResource(R.string.forgot_password_title),
        subtitle = stringResource(R.string.forgot_password_subtitle)
    ) {
        if (uiState.generalMessageRes != null) {
            AuthFeedbackBanner(
                message = stringResource(uiState.generalMessageRes),
                type = AuthFeedbackType.Error
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (uiState.infoMessageRes != null) {
            AuthFeedbackBanner(
                message = stringResource(uiState.infoMessageRes),
                type = AuthFeedbackType.Success
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        AuthTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.label_email),
            enabled = !uiState.isLoading,
            supportingText = uiState.emailErrorRes?.let { stringResource(it) }
        )
        Spacer(modifier = Modifier.height(20.dp))
        AuthPrimaryButton(
            text = stringResource(
                if (uiState.isLoading) R.string.action_sending_reset_link else R.string.action_send_reset_link
            ),
            onClick = onSubmitClick,
            enabled = !uiState.isLoading,
            loading = uiState.isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))
        InlineActionText(
            leadingText = stringResource(R.string.forgot_password_remembered),
            actionText = stringResource(R.string.action_back_to_sign_in),
            onActionClick = onBackToLoginClick,
            enabled = !uiState.isLoading
        )
    }
}

/**
 * Preview for the forgot-password screen.
 */
@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    SmartRoomsTheme {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(),
            onEmailChange = {},
            onSubmitClick = {},
            onBackToLoginClick = {}
        )
    }
}
