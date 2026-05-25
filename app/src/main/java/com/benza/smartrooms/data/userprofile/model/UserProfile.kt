package com.benza.smartrooms.data.userprofile.model

/**
 * Persistent Firestore-backed profile metadata for an authenticated user.
 */
internal data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val role: UserRole?,
    val profileComplete: Boolean,
    val teacherApprovalStatus: TeacherApprovalStatus = TeacherApprovalStatus.NONE,
)
