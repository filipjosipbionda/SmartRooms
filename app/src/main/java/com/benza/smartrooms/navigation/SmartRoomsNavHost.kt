package com.benza.smartrooms.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.feature.auth.forgotpassword.ForgotPasswordRouteScreen
import com.benza.smartrooms.feature.auth.login.LoginRouteScreen
import com.benza.smartrooms.feature.auth.register.RegisterRouteScreen
import com.benza.smartrooms.feature.home.HomeRouteScreen
import org.koin.compose.koinInject

/**
 * Declares the app navigation graph and wires route-level navigation callbacks.
 */
@Composable
internal fun SmartRoomsNavHost(navController: NavHostController) {
    val authRepository = koinInject<AuthRepository>()
    val startDestination = remember {
        if (authRepository.getCurrentUser() != null) {
            HomeRoute
        } else {
            LoginRoute
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<LoginRoute> {
            LoginRouteScreen(
                onRegisterClick = { navController.navigate(RegisterRoute) },
                onForgotPasswordClick = { navController.navigate(ForgotPasswordRoute) },
                onLoginSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<RegisterRoute> {
            RegisterRouteScreen(
                onBackToLoginClick = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<ForgotPasswordRoute> {
            ForgotPasswordRouteScreen(
                onBackToLoginClick = { navController.popBackStack() }
            )
        }

        composable<HomeRoute> {
            HomeRouteScreen(
                onLoggedOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(HomeRoute) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
