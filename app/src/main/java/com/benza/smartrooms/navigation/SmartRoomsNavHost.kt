package com.benza.smartrooms.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.benza.smartrooms.feature.auth.authgate.SplashRouteScreen
import com.benza.smartrooms.feature.auth.forgotpassword.ForgotPasswordRouteScreen
import com.benza.smartrooms.feature.auth.login.LoginRouteScreen
import com.benza.smartrooms.feature.auth.register.RegisterRouteScreen
import com.benza.smartrooms.feature.auth.roleselection.RoleSelectionRouteScreen
import com.benza.smartrooms.feature.home.HomeRouteScreen
import com.benza.smartrooms.feature.profile.ProfileRouteScreen
import com.benza.smartrooms.feature.roomdetail.RoomDetailRouteScreen
import com.benza.smartrooms.feature.roomquizbuilder.RoomQuizBuilderRouteScreen

/**
 * Declares the app navigation graph and wires route-level navigation callbacks.
 */
@Composable
internal fun SmartRoomsNavHost(
    navController: NavHostController,
    startDestination: SmartRoomsDestination = SplashRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<SplashRoute> {
            SplashRouteScreen(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo(SplashRoute) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToRoleSelection = {
                    navController.navigate(RoleSelectionRoute) {
                        popUpTo(SplashRoute) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(SplashRoute) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<LoginRoute> {
            LoginRouteScreen(
                onRegisterClick = { navController.navigate(RegisterRoute) },
                onForgotPasswordClick = { navController.navigate(ForgotPasswordRoute) },
                onLoginSuccess = {
                    navController.navigate(SplashRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<RegisterRoute> {
            RegisterRouteScreen(
                onBackToLoginClick = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(SplashRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<ForgotPasswordRoute> {
            ForgotPasswordRouteScreen(
                onBackToLoginClick = { navController.popBackStack() }
            )
        }

        composable<RoleSelectionRoute> {
            RoleSelectionRouteScreen(
                onRoleSaved = {
                    navController.navigate(HomeRoute) {
                        popUpTo(RoleSelectionRoute) {
                            inclusive = true
                        }
                    }
                },
                onLogout = {
                    navController.navigate(LoginRoute) {
                        popUpTo(RoleSelectionRoute) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<HomeRoute> {
            HomeRouteScreen(
                onProfileClick = { navController.navigate(ProfileRoute) },
                onRoomClick = { room ->
                    navController.navigate(
                        RoomDetailRoute(
                            roomId = room.id,
                            roomName = room.name,
                            roomTopic = room.topic
                        )
                    )
                }
            )
        }

        composable<ProfileRoute> {
            ProfileRouteScreen(
                onBackClick = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(HomeRoute) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<RoomDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RoomDetailRoute>()
            RoomDetailRouteScreen(
                roomId = route.roomId,
                roomName = route.roomName,
                roomTopic = route.roomTopic,
                onBackClick = { navController.popBackStack() },
                onOpenQuizzesClick = {
                    navController.navigate(
                        RoomQuizBuilderRoute(
                            roomId = route.roomId,
                            roomName = route.roomName,
                            roomTopic = route.roomTopic
                        )
                    )
                }
            )
        }

        composable<RoomQuizBuilderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RoomQuizBuilderRoute>()
            RoomQuizBuilderRouteScreen(
                roomId = route.roomId,
                roomName = route.roomName,
                roomTopic = route.roomTopic,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
