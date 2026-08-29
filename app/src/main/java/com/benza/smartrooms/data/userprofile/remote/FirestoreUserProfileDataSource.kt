package com.benza.smartrooms.data.userprofile.remote

import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.userprofile.model.TeacherApprovalStatus
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.util.orPrettyEmailLocalPart
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.Normalizer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firestore access for app-level user profiles.
 */
internal class FirestoreUserProfileDataSource(
    private val firestore: FirebaseFirestore,
) {
    /**
     * Ensures that a Firestore profile exists for the supplied authenticated user.
     */
    internal suspend fun ensureProfile(user: AuthUser): UserProfile {
        val document = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = document.get().await()
        val resolvedProfile = snapshot.toResolvedUserProfile(user)

        if (!snapshot.exists()) {
            document.set(resolvedProfile.toMap()).await()
            return resolvedProfile
        }

        val missingProfileFields =
            buildMap<String, Any?> {
                if (snapshot.getString(UID_FIELD).isNullOrBlank() ||
                    snapshot.getString(UID_FIELD) != resolvedProfile.uid
                ) {
                    put(UID_FIELD, resolvedProfile.uid)
                }
                if (snapshot.getString(EMAIL_FIELD).orEmpty() != resolvedProfile.email &&
                    resolvedProfile.email.isNotBlank()
                ) {
                    put(EMAIL_FIELD, resolvedProfile.email)
                }
                if (snapshot.getString(DISPLAY_NAME_FIELD).orEmpty() != resolvedProfile.displayName &&
                    resolvedProfile.displayName.isNotBlank()
                ) {
                    put(DISPLAY_NAME_FIELD, resolvedProfile.displayName)
                }
                if (snapshot.getString(PHOTO_URL_FIELD) != resolvedProfile.photoUrl &&
                    !resolvedProfile.photoUrl.isNullOrBlank()
                ) {
                    put(PHOTO_URL_FIELD, resolvedProfile.photoUrl)
                }
                if (!snapshot.contains(PROFILE_COMPLETE_FIELD)) {
                    put(PROFILE_COMPLETE_FIELD, resolvedProfile.profileComplete)
                }
                if (!snapshot.contains(TEACHER_APPROVAL_STATUS_FIELD)) {
                    put(TEACHER_APPROVAL_STATUS_FIELD, resolvedProfile.teacherApprovalStatus.toBackendValue())
                }
            }
        if (missingProfileFields.isNotEmpty()) {
            document
                .set(
                    missingProfileFields + (UPDATED_AT_FIELD to System.currentTimeMillis()),
                    SetOptions.merge(),
                ).await()
        }

        return resolvedProfile
    }

    /**
     * Observes the user profile document and emits updates as soon as approval state changes.
     */
    internal fun observeProfile(user: AuthUser): Flow<UserProfile> =
        callbackFlow {
            val document = firestore.collection(USERS_COLLECTION).document(user.uid)
            val registration =
                document.addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val profile =
                        if (snapshot?.exists() == true) {
                            snapshot.toUserProfile(user)
                        } else {
                            user.toDefaultProfile()
                        }

                    trySend(profile)
                }

            awaitClose { registration.remove() }
        }

    /**
     * Observes current profile data for a group of users. This keeps shared avatars current instead
     * of relying on profile URLs copied into comments or other documents.
     */
    internal fun observeProfiles(userIds: List<String>): Flow<List<UserProfile>> =
        callbackFlow {
            val orderedUserIds = userIds.distinct().filter(String::isNotBlank)
            if (orderedUserIds.isEmpty()) {
                trySend(emptyList())
                awaitClose()
                return@callbackFlow
            }

            val profilesById = mutableMapOf<String, UserProfile>()
            val registrations =
                orderedUserIds.chunked(MAX_PROFILE_QUERY_SIZE).map { userIdChunk ->
                    firestore
                        .collection(USERS_COLLECTION)
                        .whereIn(FieldPath.documentId(), userIdChunk)
                        .addSnapshotListener(FIRESTORE_CALLBACK_EXECUTOR) { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }

                            userIdChunk.forEach(profilesById::remove)
                            snapshot
                                ?.documents
                                .orEmpty()
                                .forEach { document ->
                                    profilesById[document.id] = document.toUserProfile(document.toFallbackAuthUser())
                                }
                            trySend(orderedUserIds.mapNotNull(profilesById::get))
                        }
                }

            awaitClose { registrations.forEach { it.remove() } }
        }

    /**
     * Searches existing profiles by display name or email.
     */
    internal suspend fun searchProfiles(query: String): List<UserProfile> {
        val normalizedQuery = query.toSearchKey()
        if (normalizedQuery.length < MIN_SEARCH_QUERY_LENGTH) return emptyList()

        return coroutineScope {
            val usersDeferred =
                async {
                    firestore
                        .collection(USERS_COLLECTION)
                        .get()
                        .await()
                        .documents
                }
            val teacherRequestsDeferred =
                async {
                    firestore
                        .collection(TEACHER_REQUESTS_COLLECTION)
                        .get()
                        .await()
                        .documents
                }

            val teacherRequestFallbacks =
                teacherRequestsDeferred
                    .await()
                    .associateBy(
                        keySelector = DocumentSnapshot::getIdOrUid,
                        valueTransform = { snapshot ->
                            SearchFallbackData(
                                email = snapshot.getString(EMAIL_FIELD),
                                displayName = snapshot.getString(DISPLAY_NAME_FIELD),
                            )
                        },
                    )

            usersDeferred
                .await()
                .mapNotNull { snapshot ->
                    val fallback = teacherRequestFallbacks[snapshot.getIdOrUid()]
                    val fallbackUser =
                        AuthUser(
                            uid = snapshot.id,
                            email = snapshot.getString(EMAIL_FIELD) ?: fallback?.email,
                            displayName = snapshot.getString(DISPLAY_NAME_FIELD) ?: fallback?.displayName,
                            photoUrl = snapshot.getString(PHOTO_URL_FIELD),
                        )
                    snapshot.toUserProfile(fallbackUser)
                }.distinctBy(UserProfile::uid)
                .filter { profile -> profile.matchesSearchQuery(normalizedQuery) }
                .sortedWith(
                    compareBy<UserProfile> { !profileStartsWithQuery(it, normalizedQuery) }
                        .thenBy { !profileContainsQueryToken(it, normalizedQuery) }
                        .thenBy { it.displayName.toSearchKey() },
                ).take(MAX_SEARCH_RESULTS)
        }
    }

    /**
     * Selects the student role and marks onboarding as complete.
     */
    internal suspend fun selectStudentRole(user: AuthUser) {
        val identity = user.toDefaultProfile()
        firestore
            .collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    UID_FIELD to identity.uid,
                    EMAIL_FIELD to identity.email,
                    DISPLAY_NAME_FIELD to identity.displayName,
                    PHOTO_URL_FIELD to identity.photoUrl,
                    ROLE_FIELD to STUDENT_ROLE,
                    PROFILE_COMPLETE_FIELD to true,
                    TEACHER_APPROVAL_STATUS_FIELD to TeacherApprovalStatus.NONE.toBackendValue(),
                    UPDATED_AT_FIELD to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
    }

    /**
     * Stores a pending teacher access request and marks the profile as waiting for review.
     */
    internal suspend fun submitTeacherRequest(user: AuthUser) {
        val now = System.currentTimeMillis()

        firestore
            .collection(TEACHER_REQUESTS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    UID_FIELD to user.uid,
                    EMAIL_FIELD to user.email.orEmpty(),
                    DISPLAY_NAME_FIELD to user.displayName.orEmailLocalPart(user.email),
                    PHOTO_URL_FIELD to user.photoUrl,
                    REQUEST_STATUS_FIELD to TeacherApprovalStatus.PENDING.toBackendValue(),
                    REQUESTED_ROLE_FIELD to TEACHER_ROLE,
                    CREATED_AT_FIELD to now,
                    UPDATED_AT_FIELD to now,
                ),
                SetOptions.merge(),
            ).await()

        firestore
            .collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    UID_FIELD to user.uid,
                    EMAIL_FIELD to user.email.orEmpty(),
                    DISPLAY_NAME_FIELD to user.displayName.orEmailLocalPart(user.email),
                    PHOTO_URL_FIELD to user.photoUrl,
                    ROLE_FIELD to null,
                    PROFILE_COMPLETE_FIELD to false,
                    TEACHER_APPROVAL_STATUS_FIELD to TeacherApprovalStatus.PENDING.toBackendValue(),
                    UPDATED_AT_FIELD to now,
                ),
                SetOptions.merge(),
            ).await()
    }

    /**
     * Stores the public download URL and the owned Storage object path on the user's document.
     * Returns the previous owned object path so it can be removed after a successful update.
     */
    internal suspend fun updateProfilePhoto(
        userId: String,
        photoUrl: String,
        storagePath: String,
    ): String? {
        val document = firestore.collection(USERS_COLLECTION).document(userId)
        val previousStoragePath =
            document
                .get()
                .await()
                .getString(PROFILE_PHOTO_STORAGE_PATH_FIELD)
                ?.takeIf(String::isNotBlank)

        document
            .set(
                mapOf(
                    UID_FIELD to userId,
                    PHOTO_URL_FIELD to photoUrl,
                    PROFILE_PHOTO_STORAGE_PATH_FIELD to storagePath,
                    UPDATED_AT_FIELD to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()

        return previousStoragePath
    }
}

private fun DocumentSnapshot.toUserProfile(fallbackUser: AuthUser): UserProfile {
    val email = getString(EMAIL_FIELD).orEmpty().ifBlank { fallbackUser.email.orEmpty() }
    val displayName =
        getString(DISPLAY_NAME_FIELD).orEmpty().ifBlank {
            fallbackUser.displayName.orEmailLocalPart(fallbackUser.email)
        }
    val photoUrl = getString(PHOTO_URL_FIELD).orEmpty().ifBlank { fallbackUser.photoUrl.orEmpty() }
    val role = getString(ROLE_FIELD).toUserRole()

    return UserProfile(
        uid = id,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl.ifBlank { null },
        role = role,
        profileComplete = getBoolean(PROFILE_COMPLETE_FIELD) ?: (role != null),
        teacherApprovalStatus = getString(TEACHER_APPROVAL_STATUS_FIELD).toTeacherApprovalStatus(),
    )
}

private fun DocumentSnapshot.toResolvedUserProfile(user: AuthUser): UserProfile {
    val existingProfile = toUserProfile(user)
    val authDisplayName = user.displayName?.takeIf(String::isNotBlank)
    val authEmail = user.email.orEmpty()

    return existingProfile.copy(
        email = authEmail.ifBlank { existingProfile.email },
        photoUrl = existingProfile.photoUrl ?: user.photoUrl,
        displayName =
            authDisplayName
                ?: existingProfile.displayName.takeIf(String::isNotBlank)
                ?: user.displayName.orEmailLocalPart(user.email),
    )
}

private fun AuthUser.toDefaultProfile(): UserProfile =
    UserProfile(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmailLocalPart(email),
        photoUrl = photoUrl,
        role = null,
        profileComplete = false,
        teacherApprovalStatus = TeacherApprovalStatus.NONE,
    )

private fun UserProfile.toMap(): Map<String, Any?> {
    val now = System.currentTimeMillis()
    return mapOf(
        UID_FIELD to uid,
        EMAIL_FIELD to email,
        DISPLAY_NAME_FIELD to displayName,
        PHOTO_URL_FIELD to photoUrl,
        ROLE_FIELD to role?.toBackendValue(),
        PROFILE_COMPLETE_FIELD to profileComplete,
        TEACHER_APPROVAL_STATUS_FIELD to teacherApprovalStatus.toBackendValue(),
        CREATED_AT_FIELD to now,
        UPDATED_AT_FIELD to now,
    )
}

private fun String?.toUserRole(): UserRole? =
    when (this) {
        "teacher" -> UserRole.TEACHER
        "student" -> UserRole.STUDENT
        else -> null
    }

private fun String?.toTeacherApprovalStatus(): TeacherApprovalStatus =
    when (this) {
        "pending" -> TeacherApprovalStatus.PENDING
        "approved" -> TeacherApprovalStatus.APPROVED
        "rejected" -> TeacherApprovalStatus.REJECTED
        else -> TeacherApprovalStatus.NONE
    }

private fun UserRole.toBackendValue(): String =
    when (this) {
        UserRole.TEACHER -> TEACHER_ROLE
        UserRole.STUDENT -> STUDENT_ROLE
    }

private fun TeacherApprovalStatus.toBackendValue(): String =
    when (this) {
        TeacherApprovalStatus.NONE -> "none"
        TeacherApprovalStatus.PENDING -> "pending"
        TeacherApprovalStatus.APPROVED -> "approved"
        TeacherApprovalStatus.REJECTED -> "rejected"
    }

private fun String?.orEmailLocalPart(email: String?): String = orPrettyEmailLocalPart(email)

private fun UserProfile.matchesSearchQuery(normalizedQuery: String): Boolean {
    val fields =
        listOf(
            displayName.toSearchKey(),
            email.toSearchKey(),
            email.substringBefore("@", "").toSearchKey(),
        )

    return fields.any { field -> field.contains(normalizedQuery) } ||
        fields
            .flatMap(String::searchTokens)
            .any { token -> token.startsWith(normalizedQuery) || normalizedQuery in token }
}

private fun profileStartsWithQuery(
    profile: UserProfile,
    normalizedQuery: String,
): Boolean =
    listOf(
        profile.displayName.toSearchKey(),
        profile.email.toSearchKey(),
        profile.email.substringBefore("@", "").toSearchKey(),
    ).any { field -> field.startsWith(normalizedQuery) }

private fun profileContainsQueryToken(
    profile: UserProfile,
    normalizedQuery: String,
): Boolean =
    listOf(
        profile.displayName.toSearchKey(),
        profile.email.toSearchKey(),
        profile.email.substringBefore("@", "").toSearchKey(),
    ).flatMap(String::searchTokens)
        .any { token -> token.startsWith(normalizedQuery) }

private fun String.toSearchKey(): String =
    Normalizer
        .normalize(trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

private fun String.searchTokens(): List<String> = split(SEARCH_TOKEN_DELIMITERS).filter(String::isNotBlank)

private fun DocumentSnapshot.getIdOrUid(): String = getString(UID_FIELD).orEmpty().ifBlank { id }

private fun DocumentSnapshot.toFallbackAuthUser(): AuthUser =
    AuthUser(
        uid = id,
        email = getString(EMAIL_FIELD),
        displayName = getString(DISPLAY_NAME_FIELD),
        photoUrl = getString(PHOTO_URL_FIELD),
    )

private data class SearchFallbackData(
    val email: String?,
    val displayName: String?,
)

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener(FIREBASE_TASK_EXECUTOR) { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
            }
        }
    }

private val FIRESTORE_CALLBACK_EXECUTOR = Dispatchers.IO.asExecutor()
private val FIREBASE_TASK_EXECUTOR = Dispatchers.IO.asExecutor()

private const val USERS_COLLECTION = "users"
private const val TEACHER_REQUESTS_COLLECTION = "teacherRequests"
private const val UID_FIELD = "uid"
private const val EMAIL_FIELD = "email"
private const val DISPLAY_NAME_FIELD = "displayName"
private const val PHOTO_URL_FIELD = "photoUrl"
private const val PROFILE_PHOTO_STORAGE_PATH_FIELD = "photoStoragePath"
private const val ROLE_FIELD = "role"
private const val PROFILE_COMPLETE_FIELD = "profileComplete"
private const val TEACHER_APPROVAL_STATUS_FIELD = "teacherApprovalStatus"
private const val REQUEST_STATUS_FIELD = "status"
private const val REQUESTED_ROLE_FIELD = "requestedRole"
private const val CREATED_AT_FIELD = "createdAtEpochMillis"
private const val UPDATED_AT_FIELD = "updatedAtEpochMillis"
private const val TEACHER_ROLE = "teacher"
private const val STUDENT_ROLE = "student"
private const val MIN_SEARCH_QUERY_LENGTH = 2
private const val MAX_SEARCH_RESULTS = 12
private const val MAX_PROFILE_QUERY_SIZE = 10
private val SEARCH_TOKEN_DELIMITERS = Regex("[\\s@._-]+")
