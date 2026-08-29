package com.benza.smartrooms.data.room.model

/**
 * Profile metadata for a user that belongs to a room.
 */
internal data class RoomMember(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val roomRole: RoomMemberRole,
    val accountRole: RoomMemberAccountRole?,
    val isProfileLoaded: Boolean,
)

internal enum class RoomMemberRole {
    OWNER,
    COLLABORATOR,
    MEMBER,
}

internal enum class RoomMemberAccountRole {
    TEACHER,
    STUDENT,
}
