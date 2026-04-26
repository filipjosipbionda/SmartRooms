package com.benza.smartrooms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import com.benza.smartrooms.data.userprofile.model.StartupDestination
import com.benza.smartrooms.feature.auth.authgate.AuthGateState
import com.benza.smartrooms.feature.auth.authgate.AuthGateScreen
import com.benza.smartrooms.feature.auth.authgate.AuthGateViewModel
import com.benza.smartrooms.navigation.HomeRoute
import com.benza.smartrooms.navigation.LoginRoute
import com.benza.smartrooms.navigation.RoleSelectionRoute
import com.benza.smartrooms.navigation.SmartRoomsNavHost
import org.koin.androidx.compose.koinViewModel

/**
 * Root composable that resolves the startup destination before creating the nav host.
 */
@Composable
internal fun SmartRoomsApp() {
    val authGateViewModel: AuthGateViewModel = koinViewModel()
    val authGateUiState by authGateViewModel.uiState.collectAsStateWithLifecycle()

    when (val authGateState = authGateUiState.state) {
        AuthGateState.Loading,
        is AuthGateState.Error -> {
            AuthGateScreen(
                uiState = authGateUiState,
                onRetryClick = authGateViewModel::retry
            )
        }

        is AuthGateState.Resolved -> {
            val startDestination = when (authGateState.destination) {
                StartupDestination.LOGIN -> LoginRoute
                StartupDestination.ROLE_SELECTION -> RoleSelectionRoute
                StartupDestination.HOME -> HomeRoute
            }

            val navController = rememberFreshNavController(startDestination)
            SmartRoomsNavHost(
                navController = navController,
                startDestination = startDestination,
                onSessionResolvedRequired = authGateViewModel::resolve
            )
        }
    }
}

@Composable
private fun rememberFreshNavController(key: Any?): NavHostController {
    val context = LocalContext.current

    return remember(key) {
        NavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
        }
    }
}
