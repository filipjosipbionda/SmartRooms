package com.benza.smartrooms.data.auth.model

/**
 * Minimal authenticated user model exposed to the app layer.
 */
internal data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)
