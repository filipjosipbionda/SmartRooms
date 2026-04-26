package com.benza.smartrooms.data.userprofile.repository

import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.userprofile.model.StartupReadiness
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over Firestore-backed profile onboarding state.
 */
internal interface UserProfileRepository {
    /**
     * Resolves whether the currently authenticated user can enter the home screen.
     */
    suspend fun resolveStartupReadiness(): UserProfileOperationResult<StartupReadiness>

    /**
     * Creates a default profile if missing and returns the latest profile snapshot.
     */
    suspend fun ensureProfile(user: AuthUser): UserProfileOperationResult<UserProfile>

    /**
     * Observes the latest Firestore profile document for the supplied user.
     */
    fun observeProfile(user: AuthUser): Flow<UserProfileOperationResult<UserProfile>>

    /**
     * Searches user profiles by display name or email.
     */
    suspend fun searchProfiles(query: String): UserProfileOperationResult<List<UserProfile>>

    /**
     * Persists the student role for the supplied user and completes onboarding.
     */
    suspend fun selectStudentRole(uid: String): UserProfileOperationResult<Unit>

    /**
     * Creates or refreshes a pending teacher request for the supplied user.
     */
    suspend fun submitTeacherRequest(user: AuthUser): UserProfileOperationResult<Unit>
}
