package com.benza.smartrooms.data.userprofile.remote

import com.benza.smartrooms.data.userprofile.model.StartupReadiness
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Cloud Functions wrapper for startup state resolution.
 */
internal class FirebaseFunctionsUserProfileDataSource(
    private val functions: FirebaseFunctions,
) {
    /**
     * Resolves whether the authenticated user can go directly to the home screen.
     */
    internal suspend fun resolveStartupReadiness(): StartupReadiness {
        val response =
            functions
                .getHttpsCallable(RESOLVE_STARTUP_DESTINATION_FUNCTION)
                .call()
                .await()
        val data =
            response.data as? Map<*, *>
                ?: throw IllegalStateException("Startup resolver returned an invalid payload.")

        return StartupReadiness(
            isReady =
                data[READY_FIELD] as? Boolean
                    ?: throw IllegalStateException("Startup resolver returned an invalid readiness flag."),
        )
    }
}

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

private const val RESOLVE_STARTUP_DESTINATION_FUNCTION = "resolveStartupDestination"
private const val READY_FIELD = "isReady"
