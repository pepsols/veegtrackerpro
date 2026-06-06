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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.veegtrackerpro.ui.admin.AdminDashboard
import com.example.veegtrackerpro.ui.admin.AdminViewModel
import com.example.veegtrackerpro.ui.components.VeegMap
import com.example.veegtrackerpro.ui.driver.DriverScreen
import com.example.veegtrackerpro.ui.driver.DriverViewModel
import com.example.veegtrackerpro.ui.navigation.*
import com.example.veegtrackerpro.ui.theme.VeegtrackerProTheme
import com.example.veegtrackerpro.ui.auth.AuthViewModel
import com.example.veegtrackerpro.ui.auth.LoginScreen
import com.example.veegtrackerpro.ui.profile.ProfileScreen
import com.example.veegtrackerpro.util.AnalyticsHelper
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
    val currentUser = authViewModel.currentUser
    val context = LocalContext.current
    val analytics = remember { AnalyticsHelper(context) }
    
    val backStack = remember { 
        mutableStateListOf<NavKey>(
            if (currentUser == null) LoginKey else RoleSelectionKey
        ) 
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = backStack.last() != RoleSelectionKey && backStack.last() != LoginKey,
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
                    label = { Text("Routes Beheren") },
                    selected = backStack.last() == AdminKey,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (backStack.last() != AdminKey) backStack.add(AdminKey)
                    },
                    icon = { Icon(Icons.Default.Route, contentDescription = null) }
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
                            onSignInClick = {
                                authViewModel.signIn(context) { success ->
                                    analytics.logSignIn("google", success)
                                    if (success) {
                                        backStack.clear()
                                        backStack.add(RoleSelectionKey)
                                    }
                                }
                            }
                        )
                    }
                    is RoleSelectionKey -> NavEntry(key) {
                        RoleSelectionScreen(
                            onRoleSelected = { role ->
                                analytics.logRoleSelected(role)
                                if (role == "driver") backStack.add(DriverKey)
                                else backStack.add(AdminKey)
                            }
                        )
                    }
                    is DriverKey -> NavEntry(key) { 
                        val driverViewModel: DriverViewModel = viewModel()
                        DriverScreen(
                            viewModel = driverViewModel,
                            onOpenMenu = { scope.launch { drawerState.open() } }
                        ) 
                    }
                    is AdminKey -> NavEntry(key) { 
                        val adminViewModel: AdminViewModel = viewModel()
                        AdminDashboard(
                            viewModel = adminViewModel,
                            onOpenMenu = { scope.launch { drawerState.open() } }
                        ) 
                    }
                    is ProfileKey -> NavEntry(key) {
                        ProfileScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenMenu = { scope.launch { drawerState.open() } }
                        )
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

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { onRoleSelected("driver") },
                modifier = Modifier.fillMaxWidth(0.7f).height(64.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(R.string.role_chauffeur), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onRoleSelected("admin") },
                modifier = Modifier.fillMaxWidth(0.7f).height(64.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(R.string.role_admin), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
