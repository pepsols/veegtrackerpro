package com.example.veegtrackerpro.ui.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.veegtrackerpro.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class SignInResult(
    val user: FirebaseUser? = null,
    val errorMessage: String? = null
)

class GoogleSignInHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val auth = FirebaseAuth.getInstance()

    suspend fun signIn(): SignInResult {
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (serverClientId.isBlank() || serverClientId == "your-web-client-id.apps.googleusercontent.com") {
            return SignInResult(
                errorMessage = "Google login is nog niet geconfigureerd. Vul GOOGLE_WEB_CLIENT_ID in local.properties in."
            )
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val user = auth.signInWithCredential(firebaseCredential).await().user
                SignInResult(user = user)
            } else {
                SignInResult(errorMessage = "Geen geldig Google-account ontvangen.")
            }
        } catch (_: GoogleIdTokenParsingException) {
            SignInResult(errorMessage = "Google login kon niet worden verwerkt.")
        } catch (exception: Exception) {
            SignInResult(errorMessage = exception.message ?: "Google login mislukt.")
        }
    }

    suspend fun signOut() {
        auth.signOut()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
