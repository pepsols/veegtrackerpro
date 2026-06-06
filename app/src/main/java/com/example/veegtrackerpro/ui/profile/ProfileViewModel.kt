package com.example.veegtrackerpro.ui.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {
    var userName by mutableStateOf("Nathan")
    var profilePicUri by mutableStateOf<Uri?>(null)

    fun updateUserName(newName: String) {
        userName = newName
    }

    fun updateProfilePic(uri: Uri?) {
        profilePicUri = uri
    }
}
