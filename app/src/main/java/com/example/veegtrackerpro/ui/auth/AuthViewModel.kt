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

    var authError by mutableStateOf<String?>(null)
        private set

    init {
        currentUser = auth.currentUser
    }

    fun signIn(context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            authError = null
            val helper = GoogleSignInHelper(context)
            val result = helper.signIn()
            currentUser = result.user
            authError = result.errorMessage
            isLoading = false
            onResult(result.user != null)
        }
    }

    fun onSignInSuccess(user: FirebaseUser?) {
        currentUser = user
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            runCatching { GoogleSignInHelper(context).signOut() }
            currentUser = null
            authError = null
        }
    }
}
