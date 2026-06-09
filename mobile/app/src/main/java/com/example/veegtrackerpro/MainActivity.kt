package com.example.veegtrackerpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.veegtrackerpro.ui.driver.DriverScreen
import com.example.veegtrackerpro.ui.driver.DriverViewModel
import com.example.veegtrackerpro.ui.navigation.*
import com.example.veegtrackerpro.ui.theme.VeegtrackerProTheme
import com.example.veegtrackerpro.ui.auth.AuthViewModel
import com.example.veegtrackerpro.ui.auth.LoginScreen
import com.example.veegtrackerpro.ui.profile.ProfileScreen
import com.example.veegtrackerpro.util.AnalyticsHelper
import com.example.veegtrackerpro.ui.waypoints.ObjectWaypointsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeegtrackerProTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent() {
    val authViewModel: AuthViewModel = viewModel()
    val driverViewModel: DriverViewModel = viewModel()
    val currentUser = authViewModel.currentUser
    val context = LocalContext.current
    val analytics = remember { AnalyticsHelper(context) }
    var routePickerTrigger by remember { mutableIntStateOf(0) }
    
    val backStack = remember { 
        mutableStateListOf<NavKey>(
            if (currentUser == null) LoginKey else DriverKey
        ) 
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser != null && backStack.lastOrNull() == LoginKey) {
            backStack.clear()
            backStack.add(DriverKey)
        } else if (currentUser == null && backStack.none { it == LoginKey }) {
            backStack.clear()
            backStack.add(LoginKey)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = backStack.last() != LoginKey,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("Mijn Profiel") },
                    selected = backStack.last() == ProfileKey,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (backStack.last() != ProfileKey) backStack.add(ProfileKey)
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Kaart") },
                    selected = backStack.last() == DriverKey,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (backStack.last() != DriverKey) backStack.add(DriverKey)
                    },
                    icon = { Icon(Icons.Default.Place, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Selecteer route") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (backStack.last() != DriverKey) backStack.add(DriverKey)
                        routePickerTrigger++
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Objecten / waypoints") },
                    selected = backStack.last() == ObjectWaypointsKey,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (backStack.last() != ObjectWaypointsKey) backStack.add(ObjectWaypointsKey)
                    },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Uitloggen") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authViewModel.signOut()
                        backStack.clear()
                        backStack.add(LoginKey)
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                )
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize(),
            entryProvider = { key ->
                when (key) {
                    is LoginKey -> NavEntry(key) {
                        LoginScreen(
                            isLoading = authViewModel.isLoading,
                            errorMessage = authViewModel.errorMessage,
                            onSignInClick = {
                                authViewModel.signIn(context) { success ->
                                    analytics.logSignIn("google", success)
                                }
                            }
                        )
                    }
                    is DriverKey -> NavEntry(key) { 
                        DriverScreen(
                            viewModel = driverViewModel,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            routePickerTrigger = routePickerTrigger
                        ) 
                    }
                    is ProfileKey -> NavEntry(key) {
                        ProfileScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenMenu = { scope.launch { drawerState.open() } }
                        )
                    }
                    is ObjectWaypointsKey -> NavEntry(key) {
                        ObjectWaypointsScreen(
                            onOpenMenu = { scope.launch { drawerState.open() } }
                        )
                    }
                    is AdminKey, is RoleSelectionKey -> NavEntry(key) {
                        LaunchedEffect(Unit) {
                            backStack.clear()
                            backStack.add(if (currentUser == null) LoginKey else DriverKey)
                        }
                    }
                    is SettingsKey -> NavEntry(key) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Instellingen")
                        }
                    }
                }
            }
        )
    }
}
