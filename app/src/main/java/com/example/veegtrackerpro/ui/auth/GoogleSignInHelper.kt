package com.example.veegtrackerpro.ui.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class GoogleSignInHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val auth = FirebaseAuth.getInstance()

    suspend fun signIn(): FirebaseUser? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("YOUR_WEB_CLIENT_ID") // Needs to be replaced with actual ID from Google Cloud Console
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            
            if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                auth.signInWithCredential(firebaseCredential).await().user
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
