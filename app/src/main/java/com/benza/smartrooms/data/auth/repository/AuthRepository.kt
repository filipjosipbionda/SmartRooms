package com.benza.smartrooms.data.auth.repository

import com.benza.smartrooms.data.auth.model.AuthOperationResult
import com.benza.smartrooms.data.auth.model.AuthUser

/**
 * Auth abstraction consumed by ViewModels so Firebase details stay in the data layer.
 */
internal interface AuthRepository {
    /**
     * Attempts to authenticate a user with email and password.
     */
    suspend fun login(
        email: String,
        password: String,
    ): AuthOperationResult<AuthUser>

    /**
     * Creates a new account and populates the user's display name.
     */
    suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): AuthOperationResult<AuthUser>

    /**
     * Triggers a password reset email flow for the supplied address.
     */
    suspend fun sendPasswordResetEmail(email: String): AuthOperationResult<Unit>

    /**
     * Returns the current authenticated user mapped to the app domain model.
     */
    fun getCurrentUser(): AuthUser?

    /**
     * Signs out the current user.
     */
    fun signOut()
}
