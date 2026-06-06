package com.example.veegtrackerpro.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.veegtrackerpro.R
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.ui.components.VeegMap
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    viewModel: AdminViewModel = viewModel(),
    onOpenMenu: () -> Unit = {}
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Route>()
    val routes by viewModel.allRoutes.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val trackingPoints by viewModel.trackingPoints.collectAsState()
    val pois by viewModel.pois.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val gpxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                viewModel.importGpx(stream)
            }
        }
    }

    val isDesktopMode = navigator.scaffoldValue.secondary == PaneAdaptedValue.Expanded 
            && navigator.scaffoldValue.primary == PaneAdaptedValue.Expanded

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            RouteListPane(
                routes = routes,
                selectedRouteId = selectedRoute?.id ?: -1,
                onRouteSelected = { route ->
                    viewModel.selectRoute(route)
                    coroutineScope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, route)
                    }
                },
                onImportGpx = { gpxLauncher.launch("*/*") },
                onOpenMenu = onOpenMenu
            )
        },
        detailPane = {
            RouteDetailPane(
                route = selectedRoute,
                routePoints = routePoints,
                trackingPoints = trackingPoints,
                pois = pois,
                onUpdateImage = { r, uri -> viewModel.updateRouteImage(r, uri) },
                onUpdateComments = { r, c -> viewModel.updateRouteComments(r, c) },
                onAddPoi = { r, point -> viewModel.addPoi(r.id, "Onkruid", point.latitude, point.longitude, "Gevonden onkruid") },
                onDeletePoi = { viewModel.deletePoi(it) },
                onDelete = { 
                    viewModel.deleteRoute(it)
                    coroutineScope.launch {
                        navigator.navigateBack()
                    }
                },
                onGeneratePdf = { viewModel.generatePdf(it) },
                onBack = {
                    coroutineScope.launch {
                        navigator.navigateBack()
                    }
                },
                showBackButton = !isDesktopMode
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListPane(
    routes: List<Route>,
    selectedRouteId: Long,
    onRouteSelected: (Route) -> Unit,
    onImportGpx: () -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.role_admin), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.tab_routes), style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onImportGpx) {
                        Icon(Icons.Default.Add, contentDescription = "Add Route")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Totaal aantal routes", style = MaterialTheme.typography.labelMedium)
                        Text("${routes.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(routes) { route ->
                    val isSelected = route.id == selectedRouteId
                    ListItem(
                        headlineContent = { Text(route.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        supportingContent = {
                            val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(route.createdAt))
                            Text(date)
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (route.imageUri != null) {
                                    AsyncImage(
                                        model = route.imageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Route, 
                                        contentDescription = null, 
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
                        ),
                        modifier = Modifier.clickable { onRouteSelected(route) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailPane(
    route: Route?,
    routePoints: List<GeoPoint> = emptyList(),
    trackingPoints: List<GeoPoint>,
    pois: List<Poi> = emptyList(),
    onUpdateImage: (Route, Uri?) -> Unit,
    onUpdateComments: (Route, String) -> Unit,
    onAddPoi: (Route, GeoPoint) -> Unit,
    onDeletePoi: (Poi) -> Unit,
    onDelete: (Route) -> Unit,
    onGeneratePdf: (Route) -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean
) {
    if (route == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.label_no_route_selected), style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onUpdateImage(route, uri)
    }

    var commentText by remember(route.id) { mutableStateOf(route.comments ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.name) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Terug")
                        }
                    }
                },
                actions = {
                    Button(onClick = { onGeneratePdf(route) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rapport")
                    }
                    IconButton(onClick = { onDelete(route) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().height(400.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    VeegMap(
                        modifier = Modifier.fillMaxSize(),
                        routePoints = routePoints,
                        trackingPoints = trackingPoints,
                        pois = pois,
                        onMapLongClick = { onAddPoi(route, it) }
                    )
                }

                Text(
                    "Tip: Houd de kaart lang ingedrukt om een onkruidlocatie toe te voegen.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Statistieken", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gereden punten: ${trackingPoints.size}")
                            Text("Onkruid locaties: ${pois.size}")
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Datum", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            val date = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(route.createdAt))
                            Text(date)
                        }
                    }
                }

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { 
                        commentText = it
                        onUpdateComments(route, it)
                    },
                    label = { Text("Logboek / Opmerkingen") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    minLines = 5
                )
                
                if (pois.isNotEmpty()) {
                    Text("Objecten op route", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    pois.forEach { poi ->
                        ListItem(
                            headlineContent = { Text(poi.type) },
                            supportingContent = { Text("${poi.latitude}, ${poi.longitude}") },
                            trailingContent = {
                                IconButton(onClick = { onDeletePoi(poi) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete POI")
                                }
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.width(300.dp).fillMaxHeight().padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.secondaryContainer).clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (route.imageUri != null) {
                            AsyncImage(model = route.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp))
                        }
                    }
                    Text("Route Afbeelding", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    Text("Klik op de afbeelding hierboven om een foto van de route of een specifiek object in te stellen.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
