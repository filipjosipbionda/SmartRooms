package com.benza.smartrooms.data.room.model

/**
 * Payload used by a room owner to invite another user into a room.
 */
internal data class CreateRoomInvitationRequest(
    val roomId: String,
    val roomName: String,
    val inviterId: String,
    val inviterName: String,
    val inviteeId: String,
    val inviteeEmail: String,
    val inviteeDisplayName: String,
    val access: RoomInvitationAccess,
)
