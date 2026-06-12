package com.example.veegtrackerpro.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavKey

@Serializable
data object RoleSelectionKey : NavKey

@Serializable
data object LoginKey : NavKey

@Serializable
data object DriverKey : NavKey

@Serializable
data object DashboardTasksKey : NavKey

@Serializable
data object DriverWorkLogKey : NavKey

@Serializable
data object AdminKey : NavKey

@Serializable
data object ProfileKey : NavKey

@Serializable
data object SettingsKey : NavKey
