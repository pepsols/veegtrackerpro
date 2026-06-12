package com.example.veegtrackerpro.ui.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class DashboardTask(
    val id: String,
    val name: String,
    val status: String,
    val source: String,
    val notes: String,
    val createdAt: Long,
    val closedAt: Long?
)

class DashboardTasksViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private var registration: ListenerRegistration? = null

    var tasks by mutableStateOf<List<DashboardTask>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        registration = firestore.collection("tasks").addSnapshotListener { snapshot, error ->
            if (error != null) {
                errorMessage = "Kon dashboardtaken niet laden"
                isLoading = false
                return@addSnapshotListener
            }

            val updatedTasks = snapshot?.documents.orEmpty().mapNotNull { doc ->
                val createdAt = doc.getLong("createdAt") ?: return@mapNotNull null
                DashboardTask(
                    id = doc.id,
                    name = doc.getString("name").orEmpty().ifBlank { "Taak zonder naam" },
                    status = doc.getString("status").orEmpty().ifBlank { "open" },
                    source = doc.getString("source").orEmpty().ifBlank { "DASHBOARD" },
                    notes = doc.getString("notes").orEmpty(),
                    createdAt = createdAt,
                    closedAt = doc.getLong("closedAt")
                )
            }
                .filterNot(::isClosedTask)
                .sortedWith(
                    compareBy<DashboardTask> { taskPriority(it.status) }
                        .thenByDescending { it.createdAt }
                )

            tasks = updatedTasks
            errorMessage = null
            isLoading = false
        }
    }

    fun markTaskDone(taskId: String) {
        firestore.collection("tasks")
            .document(taskId)
            .update(
                mapOf(
                    "status" to "closed",
                    "closedAt" to System.currentTimeMillis()
                )
            )
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }

    private fun isClosedTask(task: DashboardTask): Boolean {
        return when (task.status.trim().lowercase()) {
            "closed", "afgerond", "done" -> true
            else -> false
        }
    }

    private fun taskPriority(status: String): Int {
        return when (status.trim().lowercase()) {
            "open" -> 0
            "planned" -> 1
            else -> 2
        }
    }
}
