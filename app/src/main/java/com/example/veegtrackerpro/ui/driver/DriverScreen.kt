package com.example.veegtrackerpro.ui.driver

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.veegtrackerpro.R
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.ui.components.VeegMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    viewModel: DriverViewModel = viewModel(),
    onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val routePoints by viewModel.routePoints.collectAsState()
    val trackingPoints by viewModel.trackingPoints.collectAsState()
    val pois by viewModel.pois.collectAsState()
    val currentInstruction by viewModel.currentInstruction.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val availableRoutes by viewModel.availableRoutes.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val todayState by viewModel.todayState.collectAsState()
    var showRoutePicker by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<Poi?>(null) }

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
        VeegMap(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            trackingPoints = trackingPoints,
            pois = pois,
            isFollowing = viewModel.isFollowing,
            onPoiClick = { poi -> selectedPoi = poi }
        )

        TodaySummaryCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 76.dp, start = 16.dp, end = 16.dp),
            state = todayState,
            currentInstruction = currentInstruction
        )

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
                .padding(top = 352.dp, start = 16.dp),
            onImportGpx = { gpxLauncher.launch("*/*") },
            onPickRoute = { showRoutePicker = true }
        )

        BottomActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            progress = progress,
            isTracking = viewModel.isTracking,
            isFollowing = viewModel.isFollowing,
            onToggleTracking = { viewModel.toggleTracking() },
            onToggleFollowing = { viewModel.isFollowing = !viewModel.isFollowing }
        )

        if (showRoutePicker) {
            AlertDialog(
                onDismissRequest = { showRoutePicker = false },
                title = { Text(stringResource(R.string.tab_routes)) },
                text = {
                    LazyColumn {
                        items(availableRoutes) { route ->
                            ListItem(
                                headlineContent = { Text(route.name) },
                                supportingContent = {
                                    if (route.distanceKm > 0.0) {
                                        Text(
                                            text = stringResource(R.string.label_route_distance, route.distanceKm),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.selectRoute(route)
                                    showRoutePicker = false
                                },
                                trailingContent = {
                                    if (selectedRoute?.id == route.id) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
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

        selectedPoi?.let { poi ->
            ModalBottomSheet(
                onDismissRequest = { selectedPoi = null }
            ) {
                PoiActionSheet(
                    poi = poi,
                    onDismiss = { selectedPoi = null },
                    onSave = { status, actionTaken, followUpAction, note, photoUris ->
                        viewModel.updatePoi(
                            poi = poi,
                            status = status,
                            actionTaken = actionTaken,
                            followUpAction = followUpAction,
                            note = note,
                            photoUris = photoUris
                        )
                        selectedPoi = null
                    }
                )
            }
        }
    }
}

@Composable
fun TodaySummaryCard(
    state: DriverTodayState,
    currentInstruction: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_today_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.routeName ?: stringResource(R.string.label_no_route_selected),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            if (state.isTracking) {
                                stringResource(R.string.label_status_now)
                            } else {
                                stringResource(R.string.label_status_check)
                            }
                        )
                    }
                )
            }

            Text(
                text = state.nextAction,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (currentInstruction.isNotBlank() && currentInstruction != state.nextAction) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                currentInstruction.contains("links", ignoreCase = true) -> Icons.AutoMirrored.Filled.ArrowBack
                                currentInstruction.contains("rechts", ignoreCase = true) -> Icons.AutoMirrored.Filled.ArrowForward
                                else -> Icons.Default.ArrowUpward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentInstruction,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (state.blocker != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = state.blocker, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_route_progress, state.progress),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.label_today_points,
                            state.trackedPoints,
                            state.totalRoutePoints
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

@Composable
fun BottomActionBar(
    progress: Int,
    isTracking: Boolean,
    isFollowing: Boolean,
    onToggleTracking: () -> Unit,
    onToggleFollowing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(0.95f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.label_route_progress, progress),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        Button(
            onClick = onToggleTracking,
            modifier = Modifier
                .weight(1.55f)
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(36.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isTracking) stringResource(R.string.label_stop_route) else stringResource(R.string.label_draw),
                style = MaterialTheme.typography.titleLarge
            )
        }

        FloatingActionButton(
            onClick = onToggleFollowing,
            modifier = Modifier.size(72.dp),
            containerColor = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isFollowing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                contentDescription = "Follow Me"
            )
        }
    }
}

@Composable
fun PoiActionSheet(
    poi: Poi,
    onDismiss: () -> Unit,
    onSave: (status: String, actionTaken: String, followUpAction: String, note: String, photoUris: List<Uri>) -> Unit
) {
    val statusOptions = listOf("afgerond", "overgeslagen", "vervolg nodig")
    val actionOptions = listOf("Onkruid verwijderd", "Obstakel gecontroleerd", "Schade vastgelegd", "Punt niet bereikbaar")
    val followUpOptions = listOf("Geen vervolg nodig", "Nieuwe controle nodig", "Monteur of team nodig")
    val initialPhotoUris = remember(poi.id, poi.photoUris, poi.imageUri) { parseStoredPhotoUris(poi).map(Uri::parse) }
    var currentStep by rememberSaveable(poi.id) { mutableIntStateOf(0) }
    var selectedStatus by rememberSaveable(poi.id) {
        mutableStateOf(if (poi.status == "open") "" else poi.status)
    }
    var selectedAction by rememberSaveable(poi.id) { mutableStateOf(poi.actionTaken ?: "") }
    var selectedFollowUp by rememberSaveable(poi.id) { mutableStateOf(poi.followUpAction ?: "") }
    var note by rememberSaveable(poi.id) { mutableStateOf(poi.description.orEmpty()) }
    var photoUris by remember(poi.id) { mutableStateOf(initialPhotoUris) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris = (photoUris + uris).distinctBy(Uri::toString)
        }
    }
    val steps = listOf("Status", "Actie", "Vervolg", "Notitie", "Foto's", "Opslaan")
    val canContinue = when (currentStep) {
        0 -> selectedStatus.isNotBlank()
        1 -> selectedAction.isNotBlank()
        2 -> selectedFollowUp.isNotBlank()
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = poi.type, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Punt ${currentStep + 1} van ${steps.size}: ${steps[currentStep]}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        LinearProgressIndicator(
            progress = { (currentStep + 1) / steps.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )

        if (!poi.workLog.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = poi.workLog,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        when (currentStep) {
            0 -> StepChoiceList(
                title = "Wat is de status van dit punt?",
                subtitle = "Kies één status voordat je verdergaat.",
                options = statusOptions,
                selected = selectedStatus,
                onSelect = { selectedStatus = it }
            )
            1 -> StepChoiceList(
                title = "Wat heb je hier gedaan?",
                subtitle = "Kies de actie die het best past bij dit punt.",
                options = actionOptions,
                selected = selectedAction,
                onSelect = { selectedAction = it }
            )
            2 -> StepChoiceList(
                title = "Is vervolg nodig?",
                subtitle = "Leg vast of dit punt nog opvolging nodig heeft.",
                options = followUpOptions,
                selected = selectedFollowUp,
                onSelect = { selectedFollowUp = it }
            )
            3 -> OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notitie chauffeur") },
                placeholder = { Text("Bijvoorbeeld wat je hebt gezien of aangepast") },
                minLines = 4
            )
            4 -> PhotoStep(
                photoUris = photoUris,
                onAddPhotos = { imagePicker.launch("image/*") },
                onRemovePhoto = { index ->
                    photoUris = photoUris.filterIndexed { currentIndex, _ -> currentIndex != index }
                }
            )
            else -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryRow("Status", selectedStatus)
                        SummaryRow("Actie", selectedAction)
                        SummaryRow("Vervolg", selectedFollowUp)
                        SummaryRow("Notitie", note.ifBlank { "Geen notitie" })
                        SummaryRow("Foto's", photoUris.size.toString())
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (currentStep == 0) onDismiss() else currentStep -= 1
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (currentStep == 0) "Sluiten" else "Vorige")
            }

            Button(
                onClick = {
                    if (currentStep == steps.lastIndex) {
                        onSave(
                            selectedStatus,
                            selectedAction,
                            selectedFollowUp,
                            note,
                            photoUris
                        )
                    } else {
                        currentStep += 1
                    }
                },
                enabled = canContinue,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (currentStep == steps.lastIndex) "Punt opslaan" else "Volgende")
            }
        }
    }
}

@Composable
private fun PhotoStep(
    photoUris: List<Uri>,
    onAddPhotos: () -> Unit,
    onRemovePhoto: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onAddPhotos,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (photoUris.isEmpty()) "Foto's toevoegen" else "Meer foto's toevoegen")
        }

        if (photoUris.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Nog geen foto's gekozen. Voeg één of meer foto's toe voor bewijs of context.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Text(
                text = "Miniaturen (${photoUris.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                photoUris.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(120.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        ) {
                            Text(
                                text = "#${index + 1}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(
                            onClick = { onRemovePhoto(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Verwijder foto")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepChoiceList(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.forEach { option ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                tonalElevation = if (selected == option) 2.dp else 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected == option,
                        onClick = { onSelect(option) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = option, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun parseStoredPhotoUris(poi: Poi): List<String> {
    return poi.photoUris
        ?.lineSequence()
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?.toList()
        ?.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(poi.imageUri)
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
