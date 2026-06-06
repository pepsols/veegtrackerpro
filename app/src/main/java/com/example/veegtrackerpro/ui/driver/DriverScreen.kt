package com.example.veegtrackerpro.ui.driver

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.veegtrackerpro.R
import com.example.veegtrackerpro.ui.components.VeegMap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import org.osmdroid.util.GeoPoint
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun DriverScreen(
    viewModel: DriverViewModel = viewModel(),
    onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val routePoints by viewModel.routePoints.collectAsState()
    val trackingPoints by viewModel.trackingPoints.collectAsState()
    val currentInstruction by viewModel.currentInstruction.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val allRoutes by viewModel.allRoutes.collectAsState()
    var showRoutePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val gpxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                viewModel.importGpx(stream)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map Background
        VeegMap(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            trackingPoints = trackingPoints,
            isFollowing = viewModel.isFollowing
        )

        // Navigation Instruction Overlay
        if (currentInstruction.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .fillMaxWidth(0.9f),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentInstruction.contains("links")) Icons.Default.ArrowBack 
                                     else if (currentInstruction.contains("rechts")) Icons.Default.ArrowForward 
                                     else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = currentInstruction,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Progress Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp, start = 16.dp)
                .width(150.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.label_route_progress, progress),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }
        }

        // Top Bar
        StatusTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            onOpenMenu = onOpenMenu
        )

        // Overlay Controls
        DriverControls(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 80.dp, start = 16.dp),
            onImportGpx = { gpxLauncher.launch("*/*") },
            onPickRoute = { showRoutePicker = true }
        )

        // Start/Stop Button at bottom center
        Button(
            onClick = { viewModel.toggleTracking() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .height(64.dp)
                .width(200.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = if (viewModel.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (viewModel.isTracking) stringResource(R.string.label_stop_route) else stringResource(R.string.label_draw),
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (showRoutePicker) {
            AlertDialog(
                onDismissRequest = { showRoutePicker = false },
                title = { Text(stringResource(R.string.tab_routes)) },
                text = {
                    LazyColumn {
                        items(allRoutes) { route ->
                            ListItem(
                                headlineContent = { Text(route.name) },
                                modifier = Modifier.clickable {
                                    viewModel.selectRoute(route)
                                    showRoutePicker = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRoutePicker = false }) {
                        Text("Sluiten")
                    }
                }
            )
        }
        
        // Bottom Right Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.isFollowing = !viewModel.isFollowing },
                containerColor = if (viewModel.isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    imageVector = if (viewModel.isFollowing) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                    contentDescription = "Follow Me"
                )
            }
        }
    }
}

@Composable
fun StatusTopBar(
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.label_veegwagen),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusItem(Icons.Default.GpsFixed, stringResource(R.string.label_status_gps), Color.Green)
                Spacer(modifier = Modifier.width(16.dp))
                StatusItem(Icons.Default.BatteryFull, "85%", Color.Green)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Person, contentDescription = "User")
            }
        }
    }
}

@Composable
fun StatusItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DriverControls(
    modifier: Modifier = Modifier,
    onImportGpx: () -> Unit,
    onPickRoute: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ControlToggle(
            label = stringResource(R.string.label_follow),
            icon = Icons.Default.Map,
            isActive = false,
            onClick = onPickRoute
        )
        FloatingActionButton(
            onClick = onImportGpx,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = "Import GPX")
        }
    }
}

@Composable
fun ControlToggle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor else MaterialTheme.colorScheme.surface,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
