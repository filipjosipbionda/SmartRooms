package com.benza.smartrooms.data.room.model

/**
 * Pending room invitation shown to the invited user.
 */
internal data class RoomInvitation(
    val id: String,
    val roomId: String,
    val roomName: String,
    val inviterName: String,
    val access: RoomInvitationAccess,
    val createdAtEpochMillis: Long,
)
