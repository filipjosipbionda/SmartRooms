package com.benza.smartrooms.data.room.model

/**
 * Student profile and completed quiz attempts used to build a room-wide quiz leaderboard.
 */
internal data class RoomQuizLeaderboardStudent(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val results: List<RoomQuizResult>,
)
