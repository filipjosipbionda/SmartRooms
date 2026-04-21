package com.benza.smartrooms.data.auth.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Thin Firebase Authentication wrapper that exposes suspend-friendly auth primitives.
 */
internal class FirebaseAuthDataSource(
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Signs in an existing user with email and password.
     */
    internal suspend fun signIn(email: String, password: String): FirebaseUser? {
        return firebaseAuth.signInWithEmailAndPassword(email, password).await().user
    }

    /**
     * Creates a new Firebase account with email and password.
     */
    internal suspend fun register(email: String, password: String): FirebaseUser? {
        return firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
    }

    /**
     * Updates the current user's display name if a user is signed in.
     */
    internal suspend fun updateDisplayName(fullName: String) {
        val user = firebaseAuth.currentUser ?: return
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()
        user.updateProfile(profileUpdates).await()
    }

    /**
     * Sends a password reset email for the provided address.
     */
    internal suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    /**
     * Returns the currently signed-in Firebase user, if present.
     */
    internal fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Clears the active Firebase Authentication session.
     */
    internal fun signOut() {
        firebaseAuth.signOut()
    }
}

/**
 * Converts a Firebase Task into a cancellable suspending call.
 */
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
