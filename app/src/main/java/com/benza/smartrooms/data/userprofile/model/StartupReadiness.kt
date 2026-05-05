package com.benza.smartrooms.data.userprofile.model

/**
 * Minimal startup information returned by the backend for an authenticated user.
 */
internal data class StartupReadiness(
    val isReady: Boolean,
)
