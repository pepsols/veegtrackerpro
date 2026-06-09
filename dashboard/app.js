const firebaseConfig = {
  apiKey: "AIzaSyCUhThqkzV53HeSuJnuHGMhW5qpuWaQcmI",
  authDomain: "veegtrackerpro-c03ae.firebaseapp.com",
  databaseURL: "https://veegtrackerpro-c03ae-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "veegtrackerpro-c03ae",
  storageBucket: "veegtrackerpro-c03ae.firebasestorage.app",
  messagingSenderId: "534557233635",
  appId: "1:534557233635:web:60f19991ee49be1d6c50d6",
  measurementId: "G-D0ZT5GDNXZ"
};

const loginForm = document.getElementById("login-form");
const authStatus = document.getElementById("auth-status");
const userSession = document.getElementById("user-session");
const emailInput = document.getElementById("email-input");
const passwordInput = document.getElementById("password-input");
const currentPage = window.location.pathname.split("/").pop() || "index.html";
const isLoginPage = currentPage === "login.html";
const isProtectedPage = currentPage === "index.html" || currentPage === "routes.html";

const routeList = document.getElementById("route-list");
const routeTable = document.getElementById("route-table");
const detailTitle = document.getElementById("detail-title");
const detailStatus = document.getElementById("detail-status");
const detailPreview = document.getElementById("detail-preview");
const detailStats = document.getElementById("detail-stats");
const detailConnection = document.getElementById("detail-connection");
const detailRuns = document.getElementById("detail-runs");
const detailPois = document.getElementById("detail-pois");
const detailMarkers = document.getElementById("detail-markers");

const metricRoutes = document.getElementById("metric-routes");
const metricRoutesMeta = document.getElementById("metric-routes-meta");
const metricVehicles = document.getElementById("metric-vehicles");
const metricVehiclesMeta = document.getElementById("metric-vehicles-meta");
const metricCompleted = document.getElementById("metric-completed");
const metricCompletedMeta = document.getElementById("metric-completed-meta");

const opRoutes = document.getElementById("op-routes");
const opRoutesMeta = document.getElementById("op-routes-meta");
const opRuns = document.getElementById("op-runs");
const opRunsMeta = document.getElementById("op-runs-meta");
const opTracking = document.getElementById("op-tracking");
const opTrackingMeta = document.getElementById("op-tracking-meta");

const filterButtons = Array.from(document.querySelectorAll("[data-filter]"));

let selectedRouteId = null;
let activeFilter = "all";
let routeMap = null;
let routeLayerGroup = null;
const DEFAULT_MAP_CENTER = [52.0455, 5.3010];
const DEFAULT_MAP_ZOOM = 13;
const MAPS_API_KEY = "AIzaSyDlsqOX2E8u43qErdVAq2miPGlLbYCcniw";
const dataState = {
  routes: [],
  routeRuns: [],
  trackingPoints: [],
  pois: []
};

if (window.firebase && !firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}

function setAuthStatus(message) {
  if (authStatus) authStatus.textContent = message;
}

function renderUserSession(user) {
  if (!userSession) return;
  if (!user) {
    userSession.innerHTML = '<p class="session-empty">Nog niet ingelogd.</p>';
    return;
  }

  userSession.innerHTML = `
    <p class="status-label">Ingelogd als</p>
    <p class="session-user">${user.displayName || "Dashboard gebruiker"}</p>
    <p class="session-email">${user.email || ""}</p>
    <div class="session-actions">
      <a class="secondary-button" href="./routes.html">Routes</a>
      <button class="secondary-button" id="logout-button" type="button">Uitloggen</button>
    </div>
  `;

  const logoutButton = document.getElementById("logout-button");
  if (logoutButton) {
    logoutButton.addEventListener("click", async () => {
      await firebase.auth().signOut();
      window.location.href = "./login.html";
    });
  }
}

function getStatusInfo(status, routeId) {
  if (status === "in_progress") return { label: "In uitvoering", className: "live" };
  if (status === "completed") return { label: "Voltooid", className: "idle" };
  if (status === "not_started") return { label: "Gepland", className: "idle" };
  if (status === "incomplete" || hasAttention(routeId)) return { label: "Controle", className: "attention" };
  return { label: "Onbekend", className: "idle" };
}

function hasAttention(routeId) {
  const latestRun = getLatestRunForRoute(routeId);
  return latestRun ? latestRun.offRouteMeters > 20 || latestRun.status === "incomplete" : false;
}

function getLatestRunForRoute(routeId) {
  return dataState.routeRuns
    .filter((run) => String(run.routeId) === String(routeId))
    .sort((a, b) => (b.startedAt || 0) - (a.startedAt || 0))[0] || null;
}

function isCheckedOffRun(run) {
  return Boolean(run && run.status === "completed");
}

function isMarkerCollectionRoute(route) {
  const routeName = String(route?.name || "").toLowerCase();
  return routeName.includes("markers 2026") || routeName.includes("markers2026");
}

function getOperationalRoutes() {
  return dataState.routes.filter((route) => !isMarkerCollectionRoute(route));
}

function parseGpxTrackPoints(gpxData) {
  if (!gpxData) return [];
  const pointRegex = /<trkpt[^>]*lat="([-0-9.]+)"[^>]*lon="([-0-9.]+)"/g;
  const points = [];
  let match;
  while ((match = pointRegex.exec(gpxData)) !== null) {
    points.push([Number(match[1]), Number(match[2])]);
  }
  return points;
}

function extractPreviewPoint(gpxData) {
  if (!gpxData) return null;
  const pointRegex = /<(trkpt|wpt)[^>]*lat="([-0-9.]+)"[^>]*lon="([-0-9.]+)"/i;
  const match = gpxData.match(pointRegex);
  if (!match) return null;

  return {
    latitude: Number(match[2]),
    longitude: Number(match[3])
  };
}

function parseGpxWaypoints(gpxData) {
  if (!gpxData) return [];

  const waypointRegex = /<wpt[^>]*lat="([-0-9.]+)"[^>]*lon="([-0-9.]+)"[^>]*>([\s\S]*?)<\/wpt>/g;
  const tagValue = (content, tag) => {
    const match = content.match(new RegExp(`<${tag}>([\\s\\S]*?)<\\/${tag}>`, "i"));
    return match ? match[1].trim() : "";
  };

  const points = [];
  let match;
  while ((match = waypointRegex.exec(gpxData)) !== null) {
    points.push({
      latitude: Number(match[1]),
      longitude: Number(match[2]),
      name: tagValue(match[3], "name") || "Markerpunt",
      description: tagValue(match[3], "desc") || tagValue(match[3], "cmt") || "",
      timestamp: tagValue(match[3], "time") || ""
    });
  }
  return points;
}

function getMarkerCollectionPoints() {
  return dataState.routes
    .filter((route) => isMarkerCollectionRoute(route))
    .flatMap((route) =>
      parseGpxWaypoints(route.gpxData).map((point) => ({
        ...point,
        sourceRouteName: route.name || "Markers"
      }))
    );
}

function getUsableRouteImage(route) {
  const imageUri = String(route?.imageUri || "").trim();
  if (imageUri.startsWith("http://") || imageUri.startsWith("https://") || imageUri.startsWith("data:image/")) {
    return imageUri;
  }
  return "";
}

function buildRoutePreviewUrl(route) {
  const routeImage = getUsableRouteImage(route);
  if (routeImage) return routeImage;

  const firstPoint = extractPreviewPoint(route?.gpxData || "");
  if (!firstPoint) return "";

  if (MAPS_API_KEY) {
    return `https://maps.googleapis.com/maps/api/streetview?size=640x360&location=${firstPoint.latitude.toFixed(6)},${firstPoint.longitude.toFixed(6)}&fov=90&heading=0&pitch=0&source=outdoor&key=${MAPS_API_KEY}`;
  }

  return "";
}

function formatDate(timestamp) {
  if (!timestamp) return "Onbekend";
  return new Date(timestamp).toLocaleString("nl-NL", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function getTrackingSourceLabel(latestRun, latestTrackingPoint) {
  if (isCheckedOffRun(latestRun) && latestTrackingPoint) return "Afronde route met opgeslagen spoor";
  if (isCheckedOffRun(latestRun)) return "Route afgerond, zonder opgeslagen GPS";
  if (latestRun) return "Tracking wordt pas bewaard na afronden";
  return "Nog geen afgeronde rit";
}

function ensureMap() {
  const mapElement = document.getElementById("detail-map");
  if (!mapElement || !window.L) return null;
  if (!routeMap) {
    routeMap = L.map(mapElement, { zoomControl: true }).setView(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap-bijdragers"
    }).addTo(routeMap);
    routeLayerGroup = L.layerGroup().addTo(routeMap);
  }
  return routeMap;
}

function renderOverviewMetrics() {
  const operationalRoutes = getOperationalRoutes();
  const routesCount = operationalRoutes.length;
  const inProgressRuns = dataState.routeRuns.filter((run) => run.status === "in_progress");
  const completedRuns = dataState.routeRuns.filter((run) => run.status === "completed");
  const archivedTrackingPoints = dataState.trackingPoints.filter((point) => {
    const run = dataState.routeRuns.find((item) => String(item.id) === String(point.runId));
    return isCheckedOffRun(run);
  });

  if (metricRoutes) metricRoutes.textContent = String(routesCount);
  if (metricRoutesMeta) metricRoutesMeta.textContent = routesCount ? `${operationalRoutes.filter((route) => hasAttention(route.id)).length} met aandachtspunt` : "Nog geen routes geladen";
  if (metricVehicles) metricVehicles.textContent = String(completedRuns.length);
  if (metricVehiclesMeta) metricVehiclesMeta.textContent = completedRuns.length ? "Afgevinkte routes met historie" : "Nog geen afgevinkte routes";
  if (metricCompleted) metricCompleted.textContent = String(completedRuns.length);
  if (metricCompletedMeta) metricCompletedMeta.textContent = completedRuns.length ? "Afgeronde ritten in Firestore" : "Nog geen afgeronde ritten";

  if (opRoutes) opRoutes.textContent = String(routesCount);
  if (opRoutesMeta) opRoutesMeta.textContent = routesCount ? `${operationalRoutes.filter((route) => hasAttention(route.id)).length} routes vragen aandacht` : "Nog geen routes geladen";
  if (opRuns) opRuns.textContent = String(completedRuns.length);
  if (opRunsMeta) opRunsMeta.textContent = completedRuns.length ? "Afgevinkte ritten" : "Nog geen afgeronde ritten";
  if (opTracking) opTracking.textContent = String(archivedTrackingPoints.length);
  if (opTrackingMeta) opTrackingMeta.textContent = archivedTrackingPoints.length ? "Bewaarde GPS-punten na afronden" : "Nog geen bewaarde GPS-punten";
}

function renderRouteList() {
  if (!routeList) return;

  const latestRoutes = [...getOperationalRoutes()]
    .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
    .slice(0, 5);

  routeList.innerHTML = latestRoutes.length
    ? latestRoutes
        .map((route) => {
          const latestRun = getLatestRunForRoute(route.id);
          const statusInfo = getStatusInfo(latestRun?.status, route.id);
          return `
            <article class="route-item">
              <div class="route-summary">
                ${buildRoutePreviewUrl(route)
                  ? `<img class="route-thumb" src="${buildRoutePreviewUrl(route)}" alt="Preview van ${route.name}">`
                  : '<div class="route-thumb-placeholder"></div>'}
                <div>
                  <p class="route-name">${route.name}</p>
                  <p class="route-meta">${formatDate(route.createdAt)}</p>
                </div>
              </div>
              <strong class="pill ${statusInfo.className}">${statusInfo.label}</strong>
            </article>
          `;
        })
        .join("")
    : '<article class="route-item"><div><p class="route-name">Nog geen routes</p><p class="route-meta">Wacht op synchronisatie vanuit de mobiele app.</p></div></article>';
}

function filteredRoutes() {
  const sorted = [...getOperationalRoutes()].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
  if (activeFilter === "all") return sorted;
  if (activeFilter === "attention") return sorted.filter((route) => hasAttention(route.id));
  return sorted.filter((route) => {
    const latestRun = getLatestRunForRoute(route.id);
    return (latestRun?.status || "not_started") === activeFilter;
  });
}

function renderRouteTable() {
  if (!routeTable) return;

  const routes = filteredRoutes();
  const header = `
    <article class="route-table-item route-table-head">
      <strong>Route</strong>
      <strong>Voertuig</strong>
      <strong>Voortgang</strong>
      <strong>Status</strong>
    </article>
  `;

  const rows = routes.length
    ? routes
        .map((route) => {
          const latestRun = getLatestRunForRoute(route.id);
          const statusInfo = getStatusInfo(latestRun?.status, route.id);
          const trackingForRoute = dataState.trackingPoints.filter((point) => String(point.routeId) === String(route.id));
          const archivedPointCount = isCheckedOffRun(latestRun) ? trackingForRoute.length : 0;
          const vehicleLabel = latestRun ? `Run ${latestRun.id}` : "Nog niet gestart";
          const progressLabel = latestRun ? `${latestRun.progressPercent}%` : "0%";
          const selectedClass = String(selectedRouteId) === String(route.id) ? "is-selected" : "";
          const previewUrl = buildRoutePreviewUrl(route);

          return `
            <article class="route-table-item is-clickable ${selectedClass}" data-route-id="${route.id}">
              <div class="route-summary">
                ${previewUrl
                  ? `<img class="route-thumb" src="${previewUrl}" alt="Preview van ${route.name}">`
                  : '<div class="route-thumb-placeholder"></div>'}
                <div>
                  <strong>${route.name}</strong>
                  <span class="route-meta">${formatDate(route.createdAt)}</span>
                  <span class="route-meta">${archivedPointCount} bewaarde punten • ${route.distanceKm?.toFixed?.(1) || "0.0"} km</span>
                </div>
              </div>
              <div>
                <strong>${vehicleLabel}</strong>
                <span class="route-meta">${isCheckedOffRun(latestRun) ? "Afgevinkt en opgeslagen" : "Nog niet afgevinkt"}</span>
              </div>
              <div>
                <strong>${progressLabel}</strong>
                <span class="route-meta">${latestRun ? `${latestRun.coveragePercent}% dekking` : "Nog geen ritdata"}</span>
              </div>
              <div>
                <strong class="pill ${statusInfo.className}">${statusInfo.label}</strong>
              </div>
            </article>
          `;
        })
        .join("")
    : '<article class="route-table-item"><div><strong>Geen routes voor dit filter</strong><span class="route-meta">Pas het filter aan of wacht op synchronisatie.</span></div></article>';

  routeTable.innerHTML = header + rows;

  routeTable.querySelectorAll("[data-route-id]").forEach((element) => {
    element.addEventListener("click", () => {
      selectedRouteId = element.getAttribute("data-route-id");
      renderRouteTable();
      renderDetailPanel();
    });
  });
}

function renderDetailPanel() {
  if (!detailTitle || !detailStatus || !detailPreview || !detailStats || !detailConnection || !detailRuns || !detailPois || !detailMarkers) return;

  const operationalRoutes = getOperationalRoutes();
  const route = operationalRoutes.find((item) => String(item.id) === String(selectedRouteId)) || operationalRoutes[0];
  if (!route) {
    detailTitle.textContent = "Nog geen route geselecteerd";
    detailStatus.textContent = "Geen status";
    detailStatus.className = "pill idle";
    detailPreview.innerHTML = '<div class="detail-preview-empty">Nog geen preview beschikbaar.</div>';
    detailStats.innerHTML = "";
    detailConnection.innerHTML = "";
    detailRuns.innerHTML = '<div class="detail-list-item"><strong>Geen ritten</strong><span>Er is nog geen data beschikbaar.</span></div>';
    detailPois.innerHTML = '<div class="detail-list-item"><strong>Geen objecten</strong><span>Er zijn nog geen aandachtspunten beschikbaar.</span></div>';
    detailMarkers.innerHTML = '<div class="detail-list-item"><strong>Geen markerpunten</strong><span>Er zijn nog geen losse markerpunten beschikbaar.</span></div>';
    return;
  }

  selectedRouteId = route.id;
  const latestRun = getLatestRunForRoute(route.id);
  const statusInfo = getStatusInfo(latestRun?.status, route.id);
  const routeRuns = dataState.routeRuns
    .filter((run) => String(run.routeId) === String(route.id))
    .sort((a, b) => (b.startedAt || 0) - (a.startedAt || 0));
  const trackingPoints = latestRun && isCheckedOffRun(latestRun)
    ? dataState.trackingPoints.filter((point) => String(point.runId) === String(latestRun.id))
    : [];
  const latestTrackingPoint = trackingPoints
    .slice()
    .sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))[0] || null;
  const pois = dataState.pois.filter((poi) => String(poi.routeId) === String(route.id));
  const gpxPoints = parseGpxTrackPoints(route.gpxData);
  const markerCollectionPoints = getMarkerCollectionPoints();
  const previewUrl = buildRoutePreviewUrl(route);

  detailTitle.textContent = route.name;
  detailStatus.textContent = statusInfo.label;
  detailStatus.className = `pill ${statusInfo.className}`;
  detailPreview.innerHTML = previewUrl
    ? `<img src="${previewUrl}" alt="Preview van ${route.name}">`
    : '<div class="detail-preview-empty">Geen previewafbeelding voor deze route.</div>';
  detailStats.innerHTML = `
    <div class="detail-stat-card">
      <span>Laatste voortgang</span>
      <strong>${latestRun ? `${latestRun.progressPercent}%` : "0%"}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Dekking</span>
      <strong>${latestRun ? `${latestRun.coveragePercent}%` : "0%"}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Bewaarde punten</span>
      <strong>${trackingPoints.length}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Objecten</span>
      <strong>${pois.length}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Losse markerpunten</span>
      <strong>${markerCollectionPoints.length}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Laatste positie</span>
      <strong>${latestTrackingPoint ? `${latestTrackingPoint.latitude.toFixed(5)}, ${latestTrackingPoint.longitude.toFixed(5)}` : "Beschikbaar na afronden"}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Laatste update</span>
      <strong>${latestTrackingPoint ? formatDate(latestTrackingPoint.timestamp) : "Nog geen opgeslagen spoor"}</strong>
    </div>
  `;

  const connectionBadges = [
    `<span class="micro-badge strong">Route ${route.id}</span>`,
    latestRun
      ? `<span class="micro-badge strong">Run ${latestRun.id}</span>`
      : `<span class="micro-badge">Geen run</span>`,
    latestTrackingPoint
      ? `<span class="micro-badge strong">Spoor opgeslagen</span>`
      : `<span class="micro-badge warn">Geen GPS</span>`,
    hasAttention(route.id)
      ? `<span class="micro-badge warn">Aandacht vereist</span>`
      : `<span class="micro-badge">Normaal</span>`
  ].join("");

  detailConnection.innerHTML = `
    <div class="detail-list-item">
      <strong>${latestRun ? `Verbonden met rit ${latestRun.id}` : "Nog geen ritkoppeling"}</strong>
      <span>${getTrackingSourceLabel(latestRun, latestTrackingPoint)}</span>
      <span>${latestTrackingPoint ? `Laatste GPS: ${formatDate(latestTrackingPoint.timestamp)}` : "Spoor wordt pas zichtbaar nadat de route is afgevinkt."}</span>
      <div class="detail-badges">${connectionBadges}</div>
    </div>
  `;

  detailRuns.innerHTML = routeRuns.length
    ? routeRuns
        .slice(0, 4)
        .map((run) => {
          const info = getStatusInfo(run.status, route.id);
          return `
            <div class="detail-list-item">
              <strong>Run ${run.id} • ${info.label}</strong>
              <span>${run.progressPercent}% voortgang • ${run.coveragePercent}% dekking • ${run.distanceKm.toFixed(1)} km</span>
              <div class="detail-badges">
                <span class="micro-badge ${info.className === "attention" ? "warn" : info.className === "live" ? "strong" : ""}">${info.label}</span>
                <span class="micro-badge">${run.offRouteMeters?.toFixed?.(0) || "0"} m afwijking</span>
                <span class="micro-badge">${formatDate(run.startedAt)}</span>
              </div>
            </div>
          `;
        })
        .join("")
    : '<div class="detail-list-item"><strong>Geen ritten</strong><span>Deze route is nog niet gestart vanuit de mobiele app.</span></div>';

  detailPois.innerHTML = pois.length
    ? pois
        .slice(0, 5)
        .map((poi) => `
          <div class="detail-list-item">
            <strong>${poi.type}</strong>
            <span>${poi.description || "Geen beschrijving"} • ${poi.latitude.toFixed(5)}, ${poi.longitude.toFixed(5)}</span>
          </div>
        `)
        .join("")
    : '<div class="detail-list-item"><strong>Geen objecten</strong><span>Er zijn nog geen POI’s of meldingen gekoppeld.</span></div>';

  detailMarkers.innerHTML = markerCollectionPoints.length
    ? markerCollectionPoints
        .slice(0, 8)
        .map((point) => `
          <div class="detail-list-item">
            <strong>${point.name}</strong>
            <span>${point.description || "Los kaartpunt uit markers2026"} • ${point.latitude.toFixed(5)}, ${point.longitude.toFixed(5)}</span>
          </div>
        `)
        .join("")
    : '<div class="detail-list-item"><strong>Geen markerpunten</strong><span>Er zijn nog geen losse punten uit markers2026 geladen.</span></div>';

  const map = ensureMap();
  if (!map || !routeLayerGroup) return;
  routeLayerGroup.clearLayers();

  const bounds = [];
  if (gpxPoints.length > 1) {
    const routePolyline = L.polyline(gpxPoints, { color: "#0d7a67", weight: 5, opacity: 0.9 });
    routePolyline.addTo(routeLayerGroup);
    bounds.push(...gpxPoints);
  }

  const trackedPolylinePoints = trackingPoints.map((point) => [point.latitude, point.longitude]);
  if (trackedPolylinePoints.length > 1) {
    const trackingPolyline = L.polyline(trackedPolylinePoints, { color: "#d14e32", weight: 4, opacity: 0.85 });
    trackingPolyline.addTo(routeLayerGroup);
    bounds.push(...trackedPolylinePoints);
  }

  if (latestTrackingPoint) {
    const vehicleIcon = L.divIcon({
      className: "vehicle-marker",
      html: '<div class="vehicle-marker-dot"></div>',
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    });

    const vehicleMarker = L.marker([latestTrackingPoint.latitude, latestTrackingPoint.longitude], {
      icon: vehicleIcon
    }).bindPopup(
      `<strong>Laatst opgeslagen positie</strong><br>${formatDate(latestTrackingPoint.timestamp)}<br>Nauwkeurigheid: ${latestTrackingPoint.accuracyMeters ?? "onbekend"} m`
    );
    vehicleMarker.addTo(routeLayerGroup);
    bounds.push([latestTrackingPoint.latitude, latestTrackingPoint.longitude]);
  }

  pois.forEach((poi) => {
    const marker = L.circleMarker([poi.latitude, poi.longitude], {
      radius: 6,
      color: "#9b5b00",
      fillColor: "#f1a43a",
      fillOpacity: 0.95,
      weight: 2
    }).bindPopup(`<strong>${poi.type}</strong><br>${poi.description || "Geen beschrijving"}`);
    marker.addTo(routeLayerGroup);
    bounds.push([poi.latitude, poi.longitude]);
  });

  markerCollectionPoints.forEach((point) => {
    const marker = L.circleMarker([point.latitude, point.longitude], {
      radius: 7,
      color: "#7c3aed",
      fillColor: "#9f67ff",
      fillOpacity: 0.9,
      weight: 2
    }).bindPopup(
      `<strong>${point.name}</strong><br>${point.description || "Los markerpunt"}<br>${point.sourceRouteName}`
    );
    marker.addTo(routeLayerGroup);
    bounds.push([point.latitude, point.longitude]);
  });

  if (bounds.length) {
    map.fitBounds(bounds, { padding: [24, 24] });
  } else {
    map.setView(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM);
  }
}

function subscribeToData() {
  if (!window.firebase || !firebase.firestore) return;
  const db = firebase.firestore();

  db.collection("routes").onSnapshot((snapshot) => {
    dataState.routes = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    const operationalRoutes = getOperationalRoutes();
    if (!selectedRouteId && operationalRoutes.length) {
      selectedRouteId = operationalRoutes[0].id;
    }
    renderOverviewMetrics();
    renderRouteList();
    renderRouteTable();
    renderDetailPanel();
  });

  db.collection("routeRuns").onSnapshot((snapshot) => {
    dataState.routeRuns = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    renderOverviewMetrics();
    renderRouteList();
    renderRouteTable();
    renderDetailPanel();
  });

  db.collection("trackingPoints").onSnapshot((snapshot) => {
    dataState.trackingPoints = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    renderOverviewMetrics();
    renderRouteTable();
    renderDetailPanel();
  });

  db.collection("pois").onSnapshot((snapshot) => {
    dataState.pois = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    renderDetailPanel();
  });
}

filterButtons.forEach((button) => {
  button.addEventListener("click", () => {
    activeFilter = button.dataset.filter || "all";
    filterButtons.forEach((item) => item.classList.toggle("active", item === button));
    renderRouteTable();
  });
});

if (window.firebase) {
  firebase.auth().onAuthStateChanged((user) => {
    renderUserSession(user);

    if (isProtectedPage && !user) {
      window.location.href = "./login.html";
      return;
    }

    if (isLoginPage && user) {
      window.location.href = "./routes.html";
      return;
    }

    if (user) {
      subscribeToData();
    } else if (isLoginPage) {
      setAuthStatus("Log in met je e-mailadres en wachtwoord om het dashboard te openen.");
    }
  });
}

if (loginForm && window.firebase) {
  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = emailInput ? emailInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value : "";
    try {
      setAuthStatus("Bezig met inloggen...");
      await firebase.auth().signInWithEmailAndPassword(email, password);
      setAuthStatus("Inloggen gelukt, je wordt doorgestuurd...");
      window.location.href = "./routes.html";
    } catch (error) {
      setAuthStatus(`Inloggen mislukt: ${error.message}`);
    }
  });
}
