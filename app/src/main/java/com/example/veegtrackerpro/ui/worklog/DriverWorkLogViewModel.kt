package com.example.veegtrackerpro.ui.worklog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veegtrackerpro.VeegApplication
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.media.MarkerPhotoResolver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DriverWorkLogEntry(
    val id: Long,
    val title: String,
    val status: String,
    val actionTaken: String,
    val followUpAction: String,
    val note: String,
    val workLog: String,
    val updatedAt: Long,
    val photoUris: List<String>,
    val previewUri: String?
)

class DriverWorkLogViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as VeegApplication).database.veegDao()

    val entries = dao.getAllPois()
        .map { pois ->
            pois.filter(::hasDriverWork)
                .sortedByDescending { it.updatedAt }
                .map(::toEntry)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entryCount = entries
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun hasDriverWork(poi: Poi): Boolean {
        return !poi.workLog.isNullOrBlank()
            || !poi.actionTaken.isNullOrBlank()
            || !poi.followUpAction.isNullOrBlank()
            || !poi.photoUris.isNullOrBlank()
            || !poi.imageUri.isNullOrBlank()
            || !poi.status.equals("open", ignoreCase = true)
    }

    private fun toEntry(poi: Poi): DriverWorkLogEntry {
        val photos = MarkerPhotoResolver.storedPhotoUris(poi)
        val previewUri = MarkerPhotoResolver.resolvePoiPreviewUri(getApplication(), poi)

        return DriverWorkLogEntry(
            id = poi.id,
            title = poi.type,
            status = poi.status,
            actionTaken = poi.actionTaken.orEmpty(),
            followUpAction = poi.followUpAction.orEmpty(),
            note = poi.description.orEmpty(),
            workLog = poi.workLog.orEmpty(),
            updatedAt = poi.updatedAt,
            photoUris = photos,
            previewUri = previewUri
        )
    }
}
