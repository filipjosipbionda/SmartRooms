package com.benza.smartrooms.data.auth.repository

import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthOperationResult
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.remote.FirebaseAuthDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Firebase-backed implementation of [AuthRepository].
 */
internal class FirebaseAuthRepository(
    private val authDataSource: FirebaseAuthDataSource,
) : AuthRepository {
    /**
     * Signs in an existing Firebase user and maps the result to the app domain model.
     */
    override suspend fun login(
        email: String,
        password: String,
    ): AuthOperationResult<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                authDataSource.signIn(email, password)?.toAuthUser()
                    ?: throw IllegalStateException("Firebase returned no user after sign in")
            }.fold(
                onSuccess = { AuthOperationResult.Success(it) },
                onFailure = { AuthOperationResult.Error(it.toAuthErrorRes()) },
            )
        }

    /**
     * Creates a Firebase account and best-effort updates the display name.
     */
    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): AuthOperationResult<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val user =
                    authDataSource.register(email, password)
                        ?: throw IllegalStateException("Firebase returned no user after registration")
                runCatching {
                    authDataSource.updateDisplayName(fullName)
                }
                authDataSource.currentUser()?.toAuthUser() ?: user.toAuthUser()
            }.fold(
                onSuccess = { AuthOperationResult.Success(it) },
                onFailure = { AuthOperationResult.Error(it.toAuthErrorRes()) },
            )
        }

    /**
     * Sends a password reset email. Unknown users are treated as success to avoid account leakage.
     */
    override suspend fun sendPasswordResetEmail(email: String): AuthOperationResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                authDataSource.sendPasswordResetEmail(email)
            }.fold(
                onSuccess = { AuthOperationResult.Success(Unit) },
                onFailure = { throwable ->
                    if (throwable is FirebaseAuthInvalidUserException) {
                        AuthOperationResult.Success(Unit)
                    } else {
                        AuthOperationResult.Error(throwable.toAuthErrorRes())
                    }
                },
            )
        }

    /**
     * Returns the currently authenticated Firebase user mapped to [AuthUser].
     */
    override fun getCurrentUser(): AuthUser? = authDataSource.currentUser()?.toAuthUser()

    /**
     * Signs out the active Firebase session.
     */
    override fun signOut() {
        authDataSource.signOut()
    }
}

/**
 * Maps Firebase auth exceptions to user-facing string resources used by the UI layer.
 */
private fun Throwable.toAuthErrorRes(): Int =
    when (this) {
        is FirebaseAuthUserCollisionException -> R.string.error_auth_email_in_use
        is FirebaseAuthWeakPasswordException -> R.string.error_auth_weak_password
        is FirebaseAuthInvalidUserException -> R.string.error_auth_invalid_user
        is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_invalid_credentials
        is FirebaseTooManyRequestsException -> R.string.error_auth_too_many_requests
        is FirebaseNetworkException -> R.string.error_auth_network
        else -> R.string.error_auth_generic
    }

/**
 * Converts a Firebase SDK user into the minimal auth model used by the app.
 */
private fun com.google.firebase.auth.FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
    )
