package com.example.veegtrackerpro.ui.admin

import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.veegtrackerpro.BuildConfig
import com.example.veegtrackerpro.R
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.RouteRun
import com.example.veegtrackerpro.ui.components.VeegMap
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    viewModel: AdminViewModel = viewModel(),
    onOpenMenu: () -> Unit = {}
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Route>()
    val routes by viewModel.allRoutes.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val selectedRun by viewModel.selectedRun.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val trackingPoints by viewModel.trackingPoints.collectAsState()
    val routeRuns by viewModel.routeRuns.collectAsState()
    val pois by viewModel.pois.collectAsState()
    val allPoisByRouteId by viewModel.allPoisByRouteId.collectAsState()
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

    val isDesktopMode = navigator.scaffoldValue.secondary == PaneAdaptedValue.Expanded &&
        navigator.scaffoldValue.primary == PaneAdaptedValue.Expanded

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            RouteListPane(
                routes = routes,
                poisByRouteId = allPoisByRouteId,
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
                routeRuns = routeRuns,
                selectedRun = selectedRun,
                pois = pois,
                onSelectRun = { viewModel.selectRun(it) },
                onUpdateImage = { r, uri -> viewModel.updateRouteImage(r, uri) },
                onUpdateComments = { r, c -> viewModel.updateRouteComments(r, c) },
                onAddPoi = { r, point ->
                    viewModel.addPoi(r.id, "Onkruid", point.latitude, point.longitude, "Gevonden onkruid")
                },
                onDeletePoi = { viewModel.deletePoi(it) },
                onDelete = {
                    viewModel.deleteRoute(it)
                    coroutineScope.launch {
                        navigator.navigateBack()
                    }
                },
                onGeneratePdf = { viewModel.generatePdf(it) },
                onExportKml = { viewModel.exportSelectedRunKml() },
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
    poisByRouteId: Map<Long, List<Poi>>,
    selectedRouteId: Long,
    onRouteSelected: (Route) -> Unit,
    onImportGpx: () -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

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
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
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
                    val routePois = poisByRouteId[route.id].orEmpty()
                    val routeThumbnailUrl = remember(route.id, route.gpxData, routePois) {
                        buildRoutePreviewUrl(
                            route = route,
                            routePoints = emptyList(),
                            pois = routePois
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(route.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        },
                        supportingContent = {
                            val date = DateFormat
                                .getBestDateTimePattern(locale, "ddMMyyyy")
                                .let { pattern -> java.text.SimpleDateFormat(pattern, locale) }
                                .format(Date(route.createdAt))
                            Text(
                                text = if (routePois.isEmpty()) date else "$date • ${routePois.size} markers",
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (routeThumbnailUrl != null) {
                                    AsyncImage(
                                        model = routeThumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                Color.Transparent
                            }
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
    routePoints: List<GeoPoint>,
    trackingPoints: List<GeoPoint>,
    routeRuns: List<RouteRun>,
    selectedRun: RouteRun?,
    pois: List<Poi>,
    onSelectRun: (RouteRun) -> Unit,
    onUpdateImage: (Route, Uri?) -> Unit,
    onUpdateComments: (Route, String) -> Unit,
    onAddPoi: (Route, GeoPoint) -> Unit,
    onDeletePoi: (Poi) -> Unit,
    onDelete: (Route) -> Unit,
    onGeneratePdf: (Route) -> Unit,
    onExportKml: () -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    if (route == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Dashboard,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
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
    val routePreviewUrl = remember(route.id, routePoints, pois) {
        buildRoutePreviewUrl(route, routePoints, pois)
    }

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
                    Button(onClick = onExportKml, enabled = selectedRun != null) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("KML")
                    }
                    IconButton(onClick = { onDelete(route) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val isCompactLayout = maxWidth < 900.dp

            @Composable
            fun PreviewPanel(modifier: Modifier = Modifier) {
                Card(
                    modifier = modifier,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Locatiepreview",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (routePreviewUrl != null) {
                                AsyncImage(
                                    model = routePreviewUrl,
                                    contentDescription = "Locatiepreview van de route",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Route,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Text(
                            "Automatische preview op basis van de route of gekoppelde punten.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Text(
                            "Handmatige routefoto",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { imageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (route.imageUri != null) {
                                AsyncImage(
                                    model = route.imageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp))
                            }
                        }
                        Text(
                            "Optioneel: koppel hier een eigen foto. De miniatuur en locatiepreview komen uit het eerste GPX-punt.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            @Composable
            fun MainContent(modifier: Modifier = Modifier) {
                Column(
                    modifier = modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isCompactLayout) {
                        PreviewPanel(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }

                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .height(400.dp),
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
                        "Tip: houd de kaart lang ingedrukt om een onkruidlocatie toe te voegen.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Statistieken", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Gereden punten: ${trackingPoints.size}")
                                Text("Ritten: ${routeRuns.size}")
                                Text("Onkruid locaties: ${pois.size}")
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Datum", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val date = DateFormat
                                    .getBestDateTimePattern(locale, "ddMMyyyy HHmm")
                                    .let { pattern -> java.text.SimpleDateFormat(pattern, locale) }
                                    .format(Date(route.createdAt))
                                Text(date)
                                selectedRun?.let { run ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Status: ${run.status}")
                                    Text("Voortgang: ${run.progressPercent}%")
                                    Text("Dekking: ${run.coveragePercent}%")
                                }
                            }
                        }
                    }

                    if (routeRuns.isNotEmpty()) {
                        Text("Ritten", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                        routeRuns.forEach { run ->
                            ListItem(
                                headlineContent = { Text("Run #${run.id}") },
                                supportingContent = {
                                    Text("${run.status} • ${run.progressPercent}% • ${String.format("%.1f", run.distanceKm)} km")
                                },
                                leadingContent = {
                                    RadioButton(
                                        selected = selectedRun?.id == run.id,
                                        onClick = { onSelectRun(run) }
                                    )
                                },
                                modifier = Modifier.clickable { onSelectRun(run) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = commentText,
                        onValueChange = {
                            commentText = it
                            onUpdateComments(route, it)
                        },
                        label = { Text("Logboek / Opmerkingen") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
            }

            if (isCompactLayout) {
                MainContent(modifier = Modifier.fillMaxSize())
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    PreviewPanel(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .padding(16.dp)
                    )
                    MainContent(
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            }
        }
    }
}

private fun buildRoutePreviewUrl(
    route: Route,
    routePoints: List<GeoPoint>,
    pois: List<Poi>
): String? {
    val previewPoints = buildList {
        addAll(routePoints)
        extractFirstPreviewPoint(route.gpxData)?.let(::add)
        pois.forEach { add(GeoPoint(it.latitude, it.longitude)) }
    }
    val centerPoint = previewPoints.firstOrNull() ?: return null

    return if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
        buildGoogleStaticMapUrl(
            center = centerPoint,
            routePoints = routePoints,
            pois = pois
        )
    } else {
        buildOpenStreetMapPreviewUrl(
            center = centerPoint,
            routePoints = routePoints,
            pois = pois
        )
    }
}

private fun extractFirstPreviewPoint(gpxData: String): GeoPoint? {
    if (gpxData.isBlank()) return null

    val pointRegex = Regex("""<(trkpt|wpt)\s+[^>]*lat="([-0-9.]+)"\s+[^>]*lon="([-0-9.]+)"""")
    val match = pointRegex.find(gpxData) ?: return null
    val lat = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return null
    val lon = match.groupValues.getOrNull(3)?.toDoubleOrNull() ?: return null
    return GeoPoint(lat, lon)
}

private fun buildGoogleStaticMapUrl(
    center: GeoPoint,
    routePoints: List<GeoPoint>,
    pois: List<Poi>
): String {
    val params = mutableListOf(
        "center=${formatPoint(center)}",
        "zoom=16",
        "size=640x360",
        "scale=2",
        "maptype=roadmap"
    )

    if (routePoints.size > 1) {
        val path = routePoints
            .take(40)
            .joinToString("|") { formatPoint(it) }
        params += "path=${encode("color:0x2E7D32FF|weight:5|$path")}"
    }

    if (pois.isNotEmpty()) {
        pois.take(20).forEachIndexed { index, poi ->
            val label = ((index % 26) + 'A'.code).toChar()
            val marker = "color:red|label:$label|${formatLatLon(poi.latitude, poi.longitude)}"
            params += "markers=${encode(marker)}"
        }
    } else {
        params += "markers=${encode("color:blue|${formatPoint(center)}")}"
    }

    params += "key=${encode(BuildConfig.MAPS_API_KEY)}"
    return "https://maps.googleapis.com/maps/api/staticmap?${params.joinToString("&")}"
}

private fun buildOpenStreetMapPreviewUrl(
    center: GeoPoint,
    routePoints: List<GeoPoint>,
    pois: List<Poi>
): String {
    val params = mutableListOf(
        "center=${formatPoint(center)}",
        "zoom=16",
        "size=640x360"
    )

    val markerParams = if (pois.isNotEmpty()) {
        pois.take(20).joinToString("&") { poi ->
            "markers=${formatLatLon(poi.latitude, poi.longitude)},red-pushpin"
        }
    } else {
        "markers=${formatPoint(center)},blue-pushpin"
    }
    params += markerParams

    if (routePoints.size > 1) {
        val path = routePoints
            .take(80)
            .joinToString("|") { formatPoint(it) }
        params += "path=${encode("color:0x2E7D32|weight:4|$path")}"
    }

    return "https://staticmap.openstreetmap.de/staticmap.php?${params.joinToString("&")}"
}

private fun formatPoint(point: GeoPoint): String = formatLatLon(point.latitude, point.longitude)

private fun formatLatLon(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.6f,%.6f", latitude, longitude)

private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
