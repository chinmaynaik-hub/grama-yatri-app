package com.gramayatri.app.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

data class AuthOperationResult(
    val success: Boolean,
    val message: String? = null,
    val accountExists: Boolean? = null,
    val role: String? = null
)

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun signIn(email: String, password: String): AuthOperationResult {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val signedInUser = authResult.user ?: auth.currentUser
            val role = signedInUser?.uid?.let { resolveUserRole(it) } ?: ROLE_USER
            AuthOperationResult(
                success = true,
                accountExists = true,
                role = role
            )
        } catch (error: Exception) {
            val accountExists = if (error is FirebaseAuthException && error.errorCode == "ERROR_USER_NOT_FOUND") {
                false
            } else {
                null
            }
            AuthOperationResult(
                success = false,
                message = mapAuthError(error, action = "login"),
                accountExists = accountExists
            )
        }
    }

    @Suppress("DEPRECATION")
    suspend fun checkAccountExists(email: String): AuthOperationResult {
        return try {
            val signInMethods = auth.fetchSignInMethodsForEmail(email).await().signInMethods.orEmpty()
            AuthOperationResult(
                success = true,
                accountExists = if (signInMethods.isNotEmpty()) true else null
            )
        } catch (error: Exception) {
            AuthOperationResult(
                success = false,
                message = mapAuthError(error, action = "check account")
            )
        }
    }

    suspend fun createAccount(
        email: String,
        password: String,
        role: String
    ): AuthOperationResult {
        val normalizedRole = normalizeRole(role)
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val createdUser = authResult.user
                ?: return AuthOperationResult(
                    success = false,
                    message = "Account was created but user details were unavailable. Please try again."
                )

            try {
                firestore.collection("users")
                    .document(createdUser.uid)
                    .set(
                        mapOf(
                            "uid" to createdUser.uid,
                            "email" to (createdUser.email ?: email),
                            "role" to normalizedRole,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            } catch (writeError: Exception) {
                val profileWriteMessage = mapProfileWriteError(writeError)
                Log.e(
                    "AuthRepository",
                    "Unable to save Firestore profile for uid=${createdUser.uid}",
                    writeError
                )
                val rollbackMessage = rollbackCreatedUser(createdUser.uid)
                return AuthOperationResult(
                    success = false,
                    message = if (rollbackMessage == null) {
                        profileWriteMessage
                    } else {
                        "$profileWriteMessage Rollback also failed: $rollbackMessage"
                    }
                )
            }

            auth.signOut()
            AuthOperationResult(
                success = true,
                message = "Account created successfully. Please login."
            )
        } catch (error: Exception) {
            AuthOperationResult(
                success = false,
                message = mapAuthError(error, action = "create account")
            )
        }
    }

    suspend fun sendPasswordReset(email: String): AuthOperationResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthOperationResult(
                success = true,
                message = "Password reset link sent to your email."
            )
        } catch (error: Exception) {
            AuthOperationResult(
                success = false,
                message = mapAuthError(error, action = "send password reset email")
            )
        }
    }

    private suspend fun rollbackCreatedUser(expectedUserId: String): String? {
        val currentUser = auth.currentUser
            ?: return "No signed-in user available for rollback."
        if (currentUser.uid != expectedUserId) {
            return "Rollback user mismatch. Expected $expectedUserId but found ${currentUser.uid}."
        }
        return try {
            currentUser.delete().await()
            auth.signOut()
            null
        } catch (error: Exception) {
            error.localizedMessage ?: "Unknown rollback failure."
        }
    }

    private fun mapAuthError(error: Exception, action: String): String {
        val rawMessage = (error.localizedMessage ?: error.message).orEmpty()
        if (rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
            return "Firebase Authentication configuration is missing for this app (CONFIGURATION_NOT_FOUND). " +
                "In Firebase Console, verify the Android app config and Email/Password provider, " +
                "then replace app/google-services.json with the latest file and rebuild."
        }

        if (error is FirebaseNetworkException) {
            return "Network error. Check your internet connection and try again."
        }

        if (error is FirebaseTooManyRequestsException) {
            return "Too many attempts. Please wait a bit and try again."
        }

        if (error is FirebaseAuthException) {
            return when (error.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                "ERROR_MISSING_EMAIL" -> "Email is required."
                "ERROR_MISSING_PASSWORD" -> "Password is required."
                "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered."
                "ERROR_USER_NOT_FOUND" -> "No account found for this email."
                "ERROR_WRONG_PASSWORD",
                "ERROR_INVALID_CREDENTIAL" -> "Invalid email or password."
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Email/password sign-in is disabled in Firebase Authentication. Enable it in Firebase Console."
                "ERROR_INTERNAL_ERROR" -> {
                    if (rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                        "Firebase Authentication configuration is missing for this app (CONFIGURATION_NOT_FOUND). " +
                            "In Firebase Console, verify the Android app config and Email/Password provider, " +
                            "then replace app/google-services.json with the latest file and rebuild."
                    } else {
                        "Firebase internal error while trying to $action. Please try again."
                    }
                }
                else -> error.localizedMessage ?: "Unable to $action right now."
            }
        }

        return error.localizedMessage ?: "Unable to $action right now."
    }

    private fun mapProfileWriteError(error: Exception): String {
        if (error is FirebaseNetworkException) {
            return "Unable to save your profile due to a network error. Check your connection and try again."
        }

        if (error is FirebaseFirestoreException) {
            return when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Unable to complete signup because Firestore denied profile write permission. " +
                        "In Firebase Console > Firestore Database > Rules, allow authenticated users to create users/{uid} for their own UID."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Unable to complete signup because Firestore is not initialized for this project. " +
                        "Create the Firestore database in Firebase Console and try again."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Unable to save your profile because Firestore is temporarily unavailable. Please try again."
                else ->
                    error.localizedMessage ?: "Unable to complete signup because user profile could not be saved."
            }
        }

        return error.localizedMessage ?: "Unable to complete signup because user profile could not be saved."
    }

    private suspend fun resolveUserRole(userId: String): String {
        return try {
            val role = firestore.collection("users")
                .document(userId)
                .get()
                .await()
                .getString("role")
            normalizeRole(role)
        } catch (error: Exception) {
            Log.w("AuthRepository", "Unable to resolve role for uid=$userId, defaulting to user.", error)
            ROLE_USER
        }
    }

    private fun normalizeRole(role: String?): String {
        return when (role?.trim()?.lowercase()) {
            ROLE_USER -> ROLE_USER
            ROLE_DRIVER -> ROLE_DRIVER
            ROLE_ADMIN -> ROLE_ADMIN
            else -> ROLE_USER
        }
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_DRIVER = "driver"
        const val ROLE_ADMIN = "admin"
    }
}
