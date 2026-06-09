package com.example.veegtrackerpro.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    var currentUser: FirebaseUser? by mutableStateOf(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        currentUser = auth.currentUser
    }

    fun signIn(context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val helper = GoogleSignInHelper(context)
            val user = runCatching { helper.signIn() }
                .onFailure { error ->
                    errorMessage = error.message?.takeIf { it.isNotBlank() }
                        ?: "Inloggen is mislukt. Controleer Google-instelling en internet."
                }
                .getOrNull()
            currentUser = user ?: auth.currentUser
            if (currentUser == null && errorMessage == null) {
                errorMessage = "Inloggen heeft geen gebruiker opgeleverd."
            }
            isLoading = false
            onResult(currentUser != null)
        }
    }

    fun onSignInSuccess(user: FirebaseUser?) {
        currentUser = user
    }

    fun signOut() {
        auth.signOut()
        currentUser = null
        errorMessage = null
    }

    fun clearError() {
        errorMessage = null
    }
}
