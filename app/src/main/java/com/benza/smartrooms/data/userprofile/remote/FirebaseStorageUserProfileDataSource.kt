package com.benza.smartrooms.data.userprofile.remote

import androidx.core.net.toUri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Uploads profile images to Firebase Storage.
 *
 * Firestore stores only the resulting download URL and Storage path; the image bytes belong in
 * Firebase Storage.
 */
internal class FirebaseStorageUserProfileDataSource(
    private val firebaseStorage: FirebaseStorage,
) {
    internal suspend fun uploadProfilePhoto(
        userId: String,
        localUri: String,
        mimeType: String?,
    ): UploadedUserProfilePhoto {
        require(mimeType == null || mimeType.startsWith(IMAGE_MIME_PREFIX)) {
            "Selected file is not an image."
        }

        val storagePath = "users/$userId/profile/${UUID.randomUUID()}"
        val fileReference = firebaseStorage.reference.child(storagePath)
        val metadata =
            StorageMetadata
                .Builder()
                .setContentType(mimeType ?: DEFAULT_IMAGE_MIME_TYPE)
                .setCacheControl(PROFILE_PHOTO_CACHE_CONTROL)
                .build()

        fileReference.putFile(localUri.toUri(), metadata).await()
        val downloadUrl = fileReference.downloadUrl.await().toString()

        return UploadedUserProfilePhoto(
            downloadUrl = downloadUrl,
            storagePath = storagePath,
        )
    }

    internal suspend fun deleteProfilePhoto(storagePath: String) {
        if (storagePath.isBlank()) return
        firebaseStorage.reference
            .child(storagePath)
            .delete()
            .await()
    }
}

internal data class UploadedUserProfilePhoto(
    val downloadUrl: String,
    val storagePath: String,
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

private val FIREBASE_TASK_EXECUTOR = Dispatchers.IO.asExecutor()
private const val IMAGE_MIME_PREFIX = "image/"
private const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"
private const val PROFILE_PHOTO_CACHE_CONTROL = "private,max-age=31536000,immutable"
