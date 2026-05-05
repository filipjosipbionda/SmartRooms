package com.benza.smartrooms.navigation

import kotlinx.serialization.Serializable

/**
 * Shared marker interface for all type-safe app destinations.
 */
internal sealed interface SmartRoomsDestination

/**
 * Type-safe destination for the login flow.
 */
@Serializable
internal data object LoginRoute : SmartRoomsDestination

/**
 * Type-safe destination for account registration.
 */
@Serializable
internal data object RegisterRoute : SmartRoomsDestination

/**
 * Type-safe destination for password reset.
 */
@Serializable
internal data object ForgotPasswordRoute : SmartRoomsDestination

/**
 * Type-safe destination for choosing the user role after authentication.
 */
@Serializable
internal data object RoleSelectionRoute : SmartRoomsDestination

/**
 * Type-safe destination for the authenticated home screen.
 */
@Serializable
internal data object HomeRoute : SmartRoomsDestination

/**
 * Type-safe destination for dedicated room creation.
 */
@Serializable
internal data object CreateRoomRoute : SmartRoomsDestination

/**
 * Type-safe destination for the authenticated profile screen.
 */
@Serializable
internal data object ProfileRoute : SmartRoomsDestination

/**
 * Type-safe destination for a single room feed.
 */
@Serializable
internal data class RoomDetailRoute(
    val roomId: String,
    val roomName: String,
    val roomTopic: String,
) : SmartRoomsDestination

/**
 * Type-safe destination for dedicated quiz generation inside a room.
 */
@Serializable
internal data class RoomQuizBuilderRoute(
    val roomId: String,
    val roomName: String,
    val roomTopic: String,
) : SmartRoomsDestination

/**
 * Type-safe destination for solving one generated quiz.
 */
@Serializable
internal data class RoomQuizPlayerRoute(
    val roomId: String,
    val roomName: String,
    val quizId: String,
) : SmartRoomsDestination

/**
 * Type-safe destination for reviewing generated quizzes before publishing.
 */
@Serializable
internal data class RoomQuizReviewRoute(
    val roomId: String,
    val roomName: String,
    val quizId: String,
) : SmartRoomsDestination
