package com.benza.smartrooms.data.room.model

/**
 * Payload used to create a Firestore-backed room document.
 */
internal data class CreateRoomRequest(
    val ownerId: String,
    val ownerName: String,
    val name: String,
    val topic: String
)
