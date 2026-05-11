package com.benza.smartrooms.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.benza.smartrooms.feature.auth.forgotpassword.ForgotPasswordRouteScreen
import com.benza.smartrooms.feature.auth.login.LoginRouteScreen
import com.benza.smartrooms.feature.auth.register.RegisterRouteScreen
import com.benza.smartrooms.feature.auth.roleselection.RoleSelectionRouteScreen
import com.benza.smartrooms.feature.createpost.CreatePostRouteScreen
import com.benza.smartrooms.feature.createroom.CreateRoomRouteScreen
import com.benza.smartrooms.feature.home.HomeRouteScreen
import com.benza.smartrooms.feature.profile.ProfileRouteScreen
import com.benza.smartrooms.feature.roomdetail.RoomDetailRouteScreen
import com.benza.smartrooms.feature.roomquizbuilder.RoomQuizBuilderRouteScreen
import com.benza.smartrooms.feature.roomquizplayer.RoomQuizPlayerRouteScreen
import com.benza.smartrooms.feature.roomquizreview.RoomQuizReviewRouteScreen

/**
 * Declares the app navigation graph and wires route-level navigation callbacks.
 */
@Composable
internal fun SmartRoomsNavHost(
    navController: NavHostController,
    startDestination: SmartRoomsDestination,
    onSessionResolvedRequired: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<LoginRoute> {
            LoginRouteScreen(
                onRegisterClick = { navController.navigate(RegisterRoute) },
                onForgotPasswordClick = { navController.navigate(ForgotPasswordRoute) },
                onLoginSuccess = onSessionResolvedRequired,
            )
        }

        composable<RegisterRoute> {
            RegisterRouteScreen(
                onBackToLoginClick = { navController.popBackStack() },
                onRegisterSuccess = onSessionResolvedRequired,
            )
        }

        composable<ForgotPasswordRoute> {
            ForgotPasswordRouteScreen(
                onBackToLoginClick = { navController.popBackStack() },
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
                },
            )
        }

        composable<HomeRoute> {
            HomeRouteScreen(
                onProfileClick = { navController.navigate(ProfileRoute) },
                onCreateRoomClick = { navController.navigate(CreateRoomRoute) },
                onRoomClick = { room ->
                    navController.navigate(
                        RoomDetailRoute(
                            roomId = room.id,
                            roomName = room.name,
                            roomTopic = room.topic,
                            roomCefrLevel = room.cefrLevel,
                        ),
                    )
                },
            )
        }

        composable<CreateRoomRoute> {
            CreateRoomRouteScreen(
                onBackClick = { navController.popBackStack() },
                onRoomCreated = { navController.popBackStack() },
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
                },
            )
        }

        composable<RoomDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RoomDetailRoute>()
            RoomDetailRouteScreen(
                roomId = route.roomId,
                roomName = route.roomName,
                roomTopic = route.roomTopic,
                roomCefrLevel = route.roomCefrLevel,
                onBackClick = { navController.popBackStack() },
                onCreatePostClick = {
                    navController.navigate(
                        CreatePostRoute(
                            roomId = route.roomId,
                            roomName = route.roomName,
                        ),
                    )
                },
                onEditPostClick = { announcementId ->
                    navController.navigate(
                        CreatePostRoute(
                            roomId = route.roomId,
                            roomName = route.roomName,
                            announcementId = announcementId,
                        ),
                    )
                },
                onOpenQuizClick = { quizId ->
                    navController.navigate(
                        RoomQuizPlayerRoute(
                            roomId = route.roomId,
                            roomName = route.roomName,
                            quizId = quizId,
                        ),
                    )
                },
                onOpenQuizzesClick = {
                    navController.navigate(
                        RoomQuizBuilderRoute(
                            roomId = route.roomId,
                            roomName = route.roomName,
                            roomTopic = route.roomTopic,
                        ),
                    )
                },
            )
        }

        composable<CreatePostRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CreatePostRoute>()
            CreatePostRouteScreen(
                roomId = route.roomId,
                roomName = route.roomName,
                announcementId = route.announcementId,
                onBackClick = { navController.popBackStack() },
                onPostCompleted = { navController.popBackStack() },
            )
        }

        composable<RoomQuizBuilderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RoomQuizBuilderRoute>()
            RoomQuizBuilderRouteScreen(
                roomId = route.roomId,
                roomName = route.roomName,
                roomTopic = route.roomTopic,
                onBackClick = { navController.popBackStack() },
                onOpenReviewClick = { quizId ->
                    navController.navigate(
                        RoomQuizReviewRoute(
                            roomId = route.roomId,
                            roomName = route.roomName,
                            quizId = quizId,
                        ),
                    )
                },
                onOpenQuizClick = { quizId ->
                    navController.navigate(
                        RoomQuizPlayerRoute(
                            roomId = route.roomId,
                            roomName = route.roomName,
                            quizId = quizId,
                        ),
                    )
                },
            )
        }

        composable<RoomQuizReviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RoomQuizReviewRoute>()
            RoomQuizReviewRouteScreen(
                roomId = route.roomId,
                roomName = route.roomName,
                quizId = route.quizId,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<RoomQuizPlayerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RoomQuizPlayerRoute>()
            RoomQuizPlayerRouteScreen(
                roomId = route.roomId,
                roomName = route.roomName,
                quizId = route.quizId,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
