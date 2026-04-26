package com.benza.smartrooms.feature.auth.authgate

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.benza.smartrooms.R
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.components.AuthPrimaryButton
import com.benza.smartrooms.ui.components.AuthScaffold
import com.benza.smartrooms.ui.theme.SmartRoomsTheme

@Composable
internal fun AuthGateScreen(
    uiState: AuthGateUiState,
    onRetryClick: () -> Unit
) {
    AuthScaffold(
        title = stringResource(R.string.auth_gate_title),
        subtitle = stringResource(R.string.auth_gate_subtitle)
    ) {
        when (val state = uiState.state) {
            AuthGateState.Loading -> {
                CircularProgressIndicator()
            }

            is AuthGateState.Error -> {
                AuthFeedbackBanner(
                    message = stringResource(state.messageRes),
                    type = AuthFeedbackType.Error
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthPrimaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetryClick
                )
            }

            is AuthGateState.Resolved -> Unit
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthGateScreenLoadingPreview() {
    SmartRoomsTheme {
        AuthGateScreen(
            uiState = AuthGateUiState(state = AuthGateState.Loading),
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthGateScreenErrorPreview() {
    SmartRoomsTheme {
        AuthGateScreen(
            uiState = AuthGateUiState(
                state = AuthGateState.Error(R.string.error_user_profile_auth_required)
            ),
            onRetryClick = {}
        )
    }
}
