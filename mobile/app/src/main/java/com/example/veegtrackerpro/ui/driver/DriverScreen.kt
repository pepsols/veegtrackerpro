package com.example.veegtrackerpro.ui.driver

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.veegtrackerpro.BuildConfig
import com.example.veegtrackerpro.R
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.RouteRun
import com.example.veegtrackerpro.ui.components.VeegMap
import org.osmdroid.util.GeoPoint
import java.util.Locale

@Composable
fun DriverScreen(
    viewModel: DriverViewModel = viewModel(),
    onOpenMenu: () -> Unit = {},
    routePickerTrigger: Int = 0
) {
    val context = LocalContext.current
    val routePoints by viewModel.routePoints.collectAsState()
    val completedRoutePoints by viewModel.completedRoutePoints.collectAsState()
    val trackingPoints by viewModel.trackingPoints.collectAsState()
    val currentInstruction by viewModel.currentInstruction.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val coverage by viewModel.coverage.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val runStatus by viewModel.runStatus.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val allRoutes by viewModel.allRoutes.collectAsState()
    var showRoutePicker by remember { mutableStateOf(false) }
    var pendingRouteStart by remember { mutableStateOf<Route?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val priority = remember(selectedRoute, runStatus, progress, coverage, viewModel.isTracking) {
        driverPriorityState(
            hasRoute = selectedRoute != null,
            runStatus = runStatus,
            isTracking = viewModel.isTracking,
            progress = progress,
            coverage = coverage
        )
    }
    val nextStep = remember(selectedRoute, runStatus, viewModel.isTracking, currentInstruction) {
        when {
            selectedRoute == null -> "Selecteer eerst een route."
            viewModel.isTracking && currentInstruction.isNotBlank() -> currentInstruction
            viewModel.isTracking -> "Blijf de route volgen en werk richting volledige dekking."
            runStatus == RouteRun.STATUS_INCOMPLETE -> "Hervat of controleer deze route voordat je afsluit."
            runStatus == RouteRun.STATUS_COMPLETED -> "Kies de volgende route voor vandaag."
            else -> "Start de route zodra je klaarstaat."
        }
    }
    val blocker = remember(selectedRoute, allRoutes, runStatus, coverage) {
        when {
            allRoutes.isEmpty() -> "Nog geen routes geladen of geimporteerd."
            selectedRoute == null -> "Er is nog geen route geselecteerd."
            runStatus == RouteRun.STATUS_INCOMPLETE -> "Laatste rit is incompleet en vraagt controle."
            coverage < 60 && runStatus == RouteRun.STATUS_IN_PROGRESS -> "Dekking is nog laag; volg de route nauwkeuriger."
            else -> "Geen blokkades."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(allRoutes) {
        viewModel.ensureRouteSelected(allRoutes)
    }

    LaunchedEffect(routePickerTrigger) {
        if (routePickerTrigger == 0) return@LaunchedEffect
        if (allRoutes.isEmpty()) {
            snackbarHostState.showSnackbar("Nog geen routes geladen.")
        } else {
            showRoutePicker = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DriverViewModel.UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 112.dp)
        )

        VeegMap(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            completedRoutePoints = completedRoutePoints,
            trackingPoints = trackingPoints,
            isFollowing = viewModel.isFollowing
        )

        if (currentInstruction.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 92.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp, start = 16.dp)
                .width(250.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Vandaag",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = selectedRoute?.name ?: "Geen route",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                PriorityChip(priority = priority)
                Text(
                    text = "Volgende stap",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = nextStep,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Blokkade: $blocker",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (blocker == "Geen blokkades.") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TodayStatChip(label = "Voortgang", value = "$progress%")
                    TodayStatChip(label = "Dekking", value = "$coverage%")
                    TodayStatChip(label = "Afstand", value = String.format("%.1f km", distanceKm))
                    TodayStatChip(label = "Status", value = runStatus.replace('_', ' '))
                }
            }
        }

        StatusTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            onOpenMenu = onOpenMenu
        )

        DriverControls(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 164.dp, start = 16.dp),
            onImportGpx = { gpxLauncher.launch("*/*") }
        )

        Button(
            onClick = { viewModel.toggleTracking() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .height(64.dp)
                .width(220.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            enabled = selectedRoute != null
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

        if (selectedRoute != null && !viewModel.isTracking && progress > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 108.dp)
                    .width(280.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (runStatus == RouteRun.STATUS_COMPLETED) "Route afgerond" else "Laatste route samenvatting",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Voortgang $progress% • Dekking $coverage% • ${String.format("%.1f", distanceKm)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (runStatus == RouteRun.STATUS_COMPLETED) {
                            "Kies direct je volgende route om door te werken."
                        } else {
                            "Start opnieuw om deze route af te maken of controleer later de incomplete rit."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (showRoutePicker) {
            AlertDialog(
                onDismissRequest = { showRoutePicker = false },
                title = { Text(stringResource(R.string.tab_routes)) },
                text = {
                    if (allRoutes.isEmpty()) {
                        Text("Nog geen routes geladen.")
                    } else {
                        LazyColumn {
                            items(allRoutes) { route ->
                                ListItem(
                                    headlineContent = { Text(route.name) },
                                    supportingContent = {
                                        if (selectedRoute?.id == route.id) {
                                            Text("Geselecteerd")
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        pendingRouteStart = route
                                        showRoutePicker = false
                                    }
                                )
                            }
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

        pendingRouteStart?.let { route ->
            val routePreviewUrl = remember(route.id, route.gpxData) {
                buildDriverRoutePreviewUrl(route)
            }
            Dialog(onDismissRequest = { pendingRouteStart = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Route starten?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        routePreviewUrl?.let { previewUrl ->
                            AsyncImage(
                                model = previewUrl,
                                contentDescription = "Preview van ${route.name}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Button(
                            onClick = { openNavigationToRouteStart(context, route) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Navigeer naar startpunt")
                        }
                        Text("Weet je zeker dat je de route \"${route.name}\" wilt starten?")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { pendingRouteStart = null }) {
                                Text("Annuleren")
                            }
                            TextButton(
                                onClick = {
                                    viewModel.selectRoute(route)
                                    pendingRouteStart = null
                                }
                            ) {
                                Text("Ja, starten")
                            }
                        }
                    }
                }
            }
        }

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

        LaunchedEffect(allRoutes, selectedRoute) {
            if (allRoutes.isEmpty()) {
                snackbarHostState.showSnackbar("Routes worden nog geladen of zijn nog niet geimporteerd.")
            } else if (selectedRoute != null) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }
}

private data class DriverPriorityState(
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

private fun driverPriorityState(
    hasRoute: Boolean,
    runStatus: String,
    isTracking: Boolean,
    progress: Int,
    coverage: Int
): DriverPriorityState {
    return when {
        !hasRoute -> DriverPriorityState("Nu: route kiezen", Color(0xFFFFE5DB), Color(0xFFB44721))
        isTracking -> DriverPriorityState("Nu bezig", Color(0xFFDFF4EF), Color(0xFF0A6B59))
        runStatus == RouteRun.STATUS_INCOMPLETE -> DriverPriorityState("Controle nodig", Color(0xFFFFE5DB), Color(0xFFB44721))
        runStatus == RouteRun.STATUS_COMPLETED -> DriverPriorityState("Klaar voor volgende", Color(0xFFE8EDF1), Color(0xFF52616D))
        progress > 0 || coverage > 0 -> DriverPriorityState("Hervatten", Color(0xFFFFF0D5), Color(0xFFB06200))
        else -> DriverPriorityState("Gepland", Color(0xFFE8EDF1), Color(0xFF52616D))
    }
}

@Composable
private fun PriorityChip(priority: DriverPriorityState) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(priority.label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = priority.containerColor,
            disabledLabelColor = priority.contentColor,
            disabledLeadingIconContentColor = priority.contentColor
        )
    )
}

@Composable
private fun TodayStatChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun buildDriverRoutePreviewUrl(route: Route): String? {
    val firstPoint = extractDriverPreviewPoint(route.gpxData) ?: return null

    if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
        return String.format(
            Locale.US,
            "https://maps.googleapis.com/maps/api/streetview?size=640x360&location=%.6f,%.6f&fov=90&heading=0&pitch=0&source=outdoor&key=%s",
            firstPoint.latitude,
            firstPoint.longitude,
            BuildConfig.MAPS_API_KEY
        )
    }

    return String.format(
        Locale.US,
        "https://staticmap.openstreetmap.de/staticmap.php?center=%.6f,%.6f&zoom=17&size=640x360&markers=%.6f,%.6f,red-pushpin",
        firstPoint.latitude,
        firstPoint.longitude,
        firstPoint.latitude,
        firstPoint.longitude
    )
}

private fun extractDriverPreviewPoint(gpxData: String): GeoPoint? {
    if (gpxData.isBlank()) return null

    val pointRegex = Regex("""<(trkpt|wpt)\s+[^>]*lat="([-0-9.]+)"\s+[^>]*lon="([-0-9.]+)"""")
    val match = pointRegex.find(gpxData) ?: return null
    val lat = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return null
    val lon = match.groupValues.getOrNull(3)?.toDoubleOrNull() ?: return null
    return GeoPoint(lat, lon)
}

private fun openNavigationToRouteStart(context: android.content.Context, route: Route): Boolean {
    val startPoint = extractDriverPreviewPoint(route.gpxData) ?: return false

    val googleMapsIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("google.navigation:q=${startPoint.latitude},${startPoint.longitude}")
    ).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(googleMapsIntent)
        true
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${startPoint.latitude},${startPoint.longitude}?q=${startPoint.latitude},${startPoint.longitude}(Startpunt route)")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(fallbackIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
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
            .height(68.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.label_veegwagen),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                StatusItem(Icons.Default.GpsFixed, "GPS OK", Color(0xFF00C853))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "85%",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun StatusItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DriverControls(
    modifier: Modifier = Modifier,
    onImportGpx: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FloatingActionButton(
            onClick = onImportGpx,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = "Import GPX")
        }
    }
}
