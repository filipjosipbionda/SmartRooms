package com.benza.smartrooms.data.userprofile.repository

import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.userprofile.model.StartupReadiness
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.remote.FirebaseFunctionsUserProfileDataSource
import com.benza.smartrooms.data.userprofile.remote.FirebaseStorageUserProfileDataSource
import com.benza.smartrooms.data.userprofile.remote.FirestoreUserProfileDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Firestore-backed implementation of [UserProfileRepository].
 */
internal class FirestoreUserProfileRepository(
    private val userProfileDataSource: FirestoreUserProfileDataSource,
    private val functionsUserProfileDataSource: FirebaseFunctionsUserProfileDataSource,
    private val storageUserProfileDataSource: FirebaseStorageUserProfileDataSource,
) : UserProfileRepository {
    override suspend fun resolveStartupReadiness(): UserProfileOperationResult<StartupReadiness> =
        withContext(Dispatchers.IO) {
            runCatching {
                functionsUserProfileDataSource.resolveStartupReadiness()
            }.fold(
                onSuccess = { UserProfileOperationResult.Success(it) },
                onFailure = { UserProfileOperationResult.Error(it.toUserProfileErrorRes()) },
            )
        }

    override suspend fun ensureProfile(user: AuthUser): UserProfileOperationResult<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                userProfileDataSource.ensureProfile(user)
            }.fold(
                onSuccess = { UserProfileOperationResult.Success(it) },
                onFailure = { UserProfileOperationResult.Error(it.toUserProfileErrorRes()) },
            )
        }

    override fun observeProfile(user: AuthUser): Flow<UserProfileOperationResult<UserProfile>> =
        userProfileDataSource
            .observeProfile(user)
            .map { UserProfileOperationResult.Success(it) as UserProfileOperationResult<UserProfile> }
            .catch { emit(UserProfileOperationResult.Error(it.toUserProfileErrorRes())) }
            .flowOn(Dispatchers.IO)

    override fun observeProfiles(userIds: List<String>): Flow<UserProfileOperationResult<List<UserProfile>>> =
        userProfileDataSource
            .observeProfiles(userIds)
            .map { UserProfileOperationResult.Success(it) as UserProfileOperationResult<List<UserProfile>> }
            .catch { emit(UserProfileOperationResult.Error(it.toUserProfileErrorRes())) }
            .flowOn(Dispatchers.IO)

    override suspend fun searchProfiles(query: String): UserProfileOperationResult<List<UserProfile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                userProfileDataSource.searchProfiles(query)
            }.fold(
                onSuccess = { UserProfileOperationResult.Success(it) },
                onFailure = { UserProfileOperationResult.Error(it.toUserProfileErrorRes()) },
            )
        }

    override suspend fun updateProfilePhoto(
        userId: String,
        localUri: String,
        mimeType: String?,
    ): UserProfileOperationResult<String> =
        withContext(Dispatchers.IO) {
            var uploadedStoragePath: String? = null
            runCatching {
                val uploadedPhoto =
                    storageUserProfileDataSource.uploadProfilePhoto(
                        userId = userId,
                        localUri = localUri,
                        mimeType = mimeType,
                    )
                uploadedStoragePath = uploadedPhoto.storagePath
                val previousStoragePath =
                    userProfileDataSource.updateProfilePhoto(
                        userId = userId,
                        photoUrl = uploadedPhoto.downloadUrl,
                        storagePath = uploadedPhoto.storagePath,
                    )

                if (previousStoragePath != null && previousStoragePath != uploadedPhoto.storagePath) {
                    runCatching {
                        storageUserProfileDataSource.deleteProfilePhoto(previousStoragePath)
                    }
                }
                uploadedPhoto.downloadUrl
            }.fold(
                onSuccess = { UserProfileOperationResult.Success(it) },
                onFailure = { error ->
                    uploadedStoragePath?.let { newStoragePath ->
                        runCatching {
                            storageUserProfileDataSource.deleteProfilePhoto(newStoragePath)
                        }
                    }
                    UserProfileOperationResult.Error(error.toUserProfileErrorRes())
                },
            )
        }

    override suspend fun selectStudentRole(user: AuthUser): UserProfileOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                userProfileDataSource.selectStudentRole(user)
            }.fold(
                onSuccess = { UserProfileOperationResult.Success(Unit) },
                onFailure = { UserProfileOperationResult.Error(it.toUserProfileErrorRes()) },
            )
        }

    override suspend fun submitTeacherRequest(user: AuthUser): UserProfileOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                userProfileDataSource.submitTeacherRequest(user)
            }.fold(
                onSuccess = { UserProfileOperationResult.Success(Unit) },
                onFailure = { UserProfileOperationResult.Error(it.toUserProfileErrorRes()) },
            )
        }
}

private fun Throwable.toUserProfileErrorRes(): Int =
    when (this) {
        is FirebaseNetworkException -> R.string.error_user_profile_network
        is FirebaseFunctionsException ->
            when (code) {
                FirebaseFunctionsException.Code.PERMISSION_DENIED,
                FirebaseFunctionsException.Code.UNAUTHENTICATED,
                -> R.string.error_user_profile_auth_required
                FirebaseFunctionsException.Code.UNAVAILABLE -> R.string.error_user_profile_unavailable
                else -> R.string.error_user_profile_generic
            }

        is FirebaseFirestoreException ->
            when (code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_user_profile_permission
                FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_user_profile_unavailable
                else -> R.string.error_user_profile_generic
            }

        is StorageException ->
            when (errorCode) {
                StorageException.ERROR_NOT_AUTHENTICATED -> R.string.error_user_profile_auth_required
                StorageException.ERROR_NOT_AUTHORIZED -> R.string.error_profile_photo_permission
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> R.string.error_user_profile_unavailable
                StorageException.ERROR_QUOTA_EXCEEDED -> R.string.error_user_profile_unavailable
                else -> R.string.error_profile_photo_upload
            }

        is IllegalArgumentException -> R.string.error_profile_photo_invalid
        else -> R.string.error_user_profile_generic
    }
