package com.benza.smartrooms.data.room.remote

import android.net.Uri
import com.benza.smartrooms.data.room.model.CreateAnnouncementAttachment
import com.benza.smartrooms.data.room.model.RoomAnnouncementAttachment
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Uploads room announcement attachments into Firebase Storage and returns stored metadata.
 */
internal class FirebaseStorageRoomAttachmentDataSource(
    private val firebaseStorage: FirebaseStorage,
) {
    internal suspend fun uploadAnnouncementAttachment(
        roomId: String,
        announcementId: String,
        attachment: CreateAnnouncementAttachment,
    ): RoomAnnouncementAttachment {
        val attachmentId = UUID.randomUUID().toString()
        val sanitizedFileName = attachment.name.sanitizeStorageFileName()
        val storagePath =
            "rooms/$roomId/announcements/$announcementId/${attachmentId}_$sanitizedFileName"
        val fileReference = firebaseStorage.reference.child(storagePath)

        fileReference.putFile(Uri.parse(attachment.uriString)).await()
        val downloadUrl = fileReference.downloadUrl.await().toString()

        return RoomAnnouncementAttachment(
            id = attachmentId,
            name = attachment.name,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes,
            storagePath = storagePath,
            downloadUrl = downloadUrl,
        )
    }

    internal suspend fun deleteAttachments(storagePaths: Collection<String>) {
        storagePaths.forEach { storagePath ->
            runCatching {
                firebaseStorage.reference
                    .child(storagePath)
                    .delete()
                    .await()
            }
        }
    }
}

private fun String.sanitizeStorageFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "attachment" }

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
