package com.benza.smartrooms.feature.auth.authgate

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
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
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun SplashRouteScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRoleSelection: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthGateViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        launch {
            viewModel.events.collect { event ->
                when (event) {
                    AuthGateEvent.NavigateToLogin -> onNavigateToLogin()
                    AuthGateEvent.NavigateToRoleSelection -> onNavigateToRoleSelection()
                    AuthGateEvent.NavigateToHome -> onNavigateToHome()
                }
            }
        }
        viewModel.resolve()
    }

    AuthGateScreen(
        uiState = uiState.value,
        onRetryClick = viewModel::retry
    )
}

@Composable
internal fun AuthGateScreen(
    uiState: AuthGateUiState,
    onRetryClick: () -> Unit
) {
    AuthScaffold(
        title = stringResource(R.string.auth_gate_title),
        subtitle = stringResource(R.string.auth_gate_subtitle)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.errorMessageRes != null) {
            AuthFeedbackBanner(
                message = stringResource(uiState.errorMessageRes),
                type = AuthFeedbackType.Error
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthPrimaryButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetryClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthGateScreenLoadingPreview() {
    SmartRoomsTheme {
        AuthGateScreen(
            uiState = AuthGateUiState(isLoading = true),
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
                isLoading = false,
                errorMessageRes = R.string.error_user_profile_auth_required
            ),
            onRetryClick = {}
        )
    }
}
