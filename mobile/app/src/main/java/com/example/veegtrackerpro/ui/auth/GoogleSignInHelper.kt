package com.example.veegtrackerpro.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.veegtrackerpro.BuildConfig
import com.example.veegtrackerpro.R
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class GoogleSignInHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val auth = FirebaseAuth.getInstance()

    suspend fun signIn(): FirebaseUser? {
        val generatedClientId = runCatching {
            context.getString(R.string.default_web_client_id)
        }.getOrDefault("")
        val serverClientId = generatedClientId.ifBlank {
            BuildConfig.GOOGLE_WEB_CLIENT_ID
        }
        if (serverClientId.isBlank()) {
            throw IllegalStateException(
                "Google web client ID ontbreekt. Voeg een Web OAuth client toe in Firebase " +
                    "of zet GOOGLE_WEB_CLIENT_ID in local.properties."
            )
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        return auth.signInWithCredential(firebaseCredential).await().user
    }
}
