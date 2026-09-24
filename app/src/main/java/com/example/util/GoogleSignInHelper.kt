package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    /**
     * Executes Google Sign-In using Jetpack CredentialManager and authenticates with Firebase Auth
     */
    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String
    ): Result<String> {
        return runCatching {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val email = authResult.user?.email ?: "Пользователь Google"
                Log.i(TAG, "Successfully signed in with Google: $email")
                email
            } else {
                throw IllegalStateException("Получен неподдерживаемый тип учетных данных")
            }
        }.onFailure { e ->
            Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
        }
    }

    fun isUserSignedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun getCurrentUserEmail(): String? {
        return FirebaseAuth.getInstance().currentUser?.email
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
