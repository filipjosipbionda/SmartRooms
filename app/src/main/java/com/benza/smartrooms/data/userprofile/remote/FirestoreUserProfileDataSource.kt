package com.benza.smartrooms.data.userprofile.remote

import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.userprofile.model.TeacherApprovalStatus
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Firestore access for app-level user profiles.
 */
internal class FirestoreUserProfileDataSource(
    private val firestore: FirebaseFirestore
) {
    /**
     * Ensures that a Firestore profile exists for the supplied authenticated user.
     */
    internal suspend fun ensureProfile(user: AuthUser): UserProfile {
        val document = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = document.get().await()

        if (!snapshot.exists()) {
            val createdProfile = user.toDefaultProfile()
            document.set(createdProfile.toMap()).await()
            return createdProfile
        }

        val profile = snapshot.toUserProfile(user)
        document.set(
            mapOf(
                UID_FIELD to profile.uid,
                EMAIL_FIELD to profile.email,
                DISPLAY_NAME_FIELD to profile.displayName,
                PROFILE_COMPLETE_FIELD to profile.profileComplete,
                UPDATED_AT_FIELD to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()

        return profile
    }

    /**
     * Observes the user profile document and emits updates as soon as approval state changes.
     */
    internal fun observeProfile(user: AuthUser): Flow<UserProfile> = callbackFlow {
        val document = firestore.collection(USERS_COLLECTION).document(user.uid)
        val registration = document.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val profile = if (snapshot?.exists() == true) {
                snapshot.toUserProfile(user)
            } else {
                user.toDefaultProfile()
            }

            trySend(profile)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Selects the student role and marks onboarding as complete.
     */
    internal suspend fun selectStudentRole(uid: String) {
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(
                mapOf(
                    ROLE_FIELD to STUDENT_ROLE,
                    PROFILE_COMPLETE_FIELD to true,
                    TEACHER_APPROVAL_STATUS_FIELD to TeacherApprovalStatus.NONE.toBackendValue(),
                    UPDATED_AT_FIELD to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    /**
     * Stores a pending teacher access request and marks the profile as waiting for review.
     */
    internal suspend fun submitTeacherRequest(user: AuthUser) {
        val now = System.currentTimeMillis()

        firestore.collection(TEACHER_REQUESTS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    UID_FIELD to user.uid,
                    EMAIL_FIELD to user.email.orEmpty(),
                    DISPLAY_NAME_FIELD to user.displayName.orEmailLocalPart(user.email),
                    REQUEST_STATUS_FIELD to TeacherApprovalStatus.PENDING.toBackendValue(),
                    REQUESTED_ROLE_FIELD to TEACHER_ROLE,
                    CREATED_AT_FIELD to now,
                    UPDATED_AT_FIELD to now
                ),
                SetOptions.merge()
            )
            .await()

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    ROLE_FIELD to null,
                    PROFILE_COMPLETE_FIELD to false,
                    TEACHER_APPROVAL_STATUS_FIELD to TeacherApprovalStatus.PENDING.toBackendValue(),
                    UPDATED_AT_FIELD to now
                ),
                SetOptions.merge()
            )
            .await()
    }
}

private fun DocumentSnapshot.toUserProfile(fallbackUser: AuthUser): UserProfile {
    val email = getString(EMAIL_FIELD).orEmpty().ifBlank { fallbackUser.email.orEmpty() }
    val displayName = getString(DISPLAY_NAME_FIELD).orEmpty().ifBlank {
        fallbackUser.displayName.orEmailLocalPart(fallbackUser.email)
    }
    val role = getString(ROLE_FIELD).toUserRole()

    return UserProfile(
        uid = id,
        email = email,
        displayName = displayName,
        role = role,
        profileComplete = getBoolean(PROFILE_COMPLETE_FIELD) ?: (role != null),
        teacherApprovalStatus = getString(TEACHER_APPROVAL_STATUS_FIELD).toTeacherApprovalStatus()
    )
}

private fun AuthUser.toDefaultProfile(): UserProfile {
    return UserProfile(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmailLocalPart(email),
        role = null,
        profileComplete = false,
        teacherApprovalStatus = TeacherApprovalStatus.NONE
    )
}

private fun UserProfile.toMap(): Map<String, Any?> {
    val now = System.currentTimeMillis()
    return mapOf(
        UID_FIELD to uid,
        EMAIL_FIELD to email,
        DISPLAY_NAME_FIELD to displayName,
        ROLE_FIELD to role?.toBackendValue(),
        PROFILE_COMPLETE_FIELD to profileComplete,
        TEACHER_APPROVAL_STATUS_FIELD to teacherApprovalStatus.toBackendValue(),
        CREATED_AT_FIELD to now,
        UPDATED_AT_FIELD to now
    )
}

private fun String?.toUserRole(): UserRole? {
    return when (this) {
        "teacher" -> UserRole.TEACHER
        "student" -> UserRole.STUDENT
        else -> null
    }
}

private fun String?.toTeacherApprovalStatus(): TeacherApprovalStatus {
    return when (this) {
        "pending" -> TeacherApprovalStatus.PENDING
        "approved" -> TeacherApprovalStatus.APPROVED
        "rejected" -> TeacherApprovalStatus.REJECTED
        else -> TeacherApprovalStatus.NONE
    }
}

private fun UserRole.toBackendValue(): String {
    return when (this) {
        UserRole.TEACHER -> TEACHER_ROLE
        UserRole.STUDENT -> STUDENT_ROLE
    }
}

private fun TeacherApprovalStatus.toBackendValue(): String {
    return when (this) {
        TeacherApprovalStatus.NONE -> "none"
        TeacherApprovalStatus.PENDING -> "pending"
        TeacherApprovalStatus.APPROVED -> "approved"
        TeacherApprovalStatus.REJECTED -> "rejected"
    }
}

private fun String?.orEmailLocalPart(email: String?): String {
    return this?.takeIf(String::isNotBlank)
        ?: email?.substringBefore("@").orEmpty()
}

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
            }
        }
    }
}

private const val USERS_COLLECTION = "users"
private const val TEACHER_REQUESTS_COLLECTION = "teacherRequests"
private const val UID_FIELD = "uid"
private const val EMAIL_FIELD = "email"
private const val DISPLAY_NAME_FIELD = "displayName"
private const val ROLE_FIELD = "role"
private const val PROFILE_COMPLETE_FIELD = "profileComplete"
private const val TEACHER_APPROVAL_STATUS_FIELD = "teacherApprovalStatus"
private const val REQUEST_STATUS_FIELD = "status"
private const val REQUESTED_ROLE_FIELD = "requestedRole"
private const val CREATED_AT_FIELD = "createdAtEpochMillis"
private const val UPDATED_AT_FIELD = "updatedAtEpochMillis"
private const val TEACHER_ROLE = "teacher"
private const val STUDENT_ROLE = "student"
