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
const isLocalPreviewContext =
  window.location.protocol === "file:" ||
  window.location.hostname === "127.0.0.1" ||
  window.location.hostname === "localhost";

const routeList = document.getElementById("route-list");
const exceptionList = document.getElementById("exception-list");
const routeTable = document.getElementById("route-table");
const detailPane = document.querySelector(".detail-pane");
const detailTitle = document.getElementById("detail-title");
const detailStatus = document.getElementById("detail-status");
const detailFocus = document.getElementById("detail-focus");
const detailPreview = document.getElementById("detail-preview");
const detailStats = document.getElementById("detail-stats");
const detailHealth = document.getElementById("detail-health");
const detailActions = document.getElementById("detail-actions");
const detailActionState = document.getElementById("detail-action-state");
const detailConnection = document.getElementById("detail-connection");
const detailRuns = document.getElementById("detail-runs");
const detailPois = document.getElementById("detail-pois");
const detailMarkers = document.getElementById("detail-markers");
const routeAttentionList = document.getElementById("route-attention-list");

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
let markerAssets = [];
let hasSubscribedToData = false;
let isUsingMockData = false;
const DEFAULT_MAP_CENTER = [52.0455, 5.3010];
const DEFAULT_MAP_ZOOM = 13;
const MAPS_API_KEY = "AIzaSyDlsqOX2E8u43qErdVAq2miPGlLbYCcniw";
const dataState = {
  routes: [],
  routeRuns: [],
  trackingPoints: [],
  pois: []
};

const MOCK_MARKER_GPX = `
  <gpx>
    <wpt lat="52.08980" lon="5.12430"><name>Markerpunt A</name><desc>Controle van middeneiland</desc></wpt>
    <wpt lat="52.09055" lon="5.12610"><name>Markerpunt B</name><desc>Paaltje en markering controleren</desc></wpt>
    <wpt lat="52.09110" lon="5.12800"><name>Markerpunt C</name><desc>Extra aandacht voor inrit</desc></wpt>
  </gpx>
`;

const MOCK_DATA = {
  routes: [
    {
      id: "route-1",
      name: "Rijsenburgselaan Noord",
      createdAt: Date.parse("2026-06-10T07:05:00Z"),
      distanceKm: 4.8,
      gpxData: '<gpx><trk><trkseg><trkpt lat="52.08940" lon="5.12380"></trkpt><trkpt lat="52.08990" lon="5.12450"></trkpt><trkpt lat="52.09045" lon="5.12510"></trkpt><trkpt lat="52.09100" lon="5.12590"></trkpt><trkpt lat="52.09145" lon="5.12680"></trkpt></trkseg></trk></gpx>'
    },
    {
      id: "route-2",
      name: "Dorpskern Oost",
      createdAt: Date.parse("2026-06-10T06:20:00Z"),
      distanceKm: 3.2,
      gpxData: '<gpx><trk><trkseg><trkpt lat="52.08790" lon="5.12180"></trkpt><trkpt lat="52.08830" lon="5.12220"></trkpt><trkpt lat="52.08885" lon="5.12295"></trkpt><trkpt lat="52.08920" lon="5.12340"></trkpt></trkseg></trk></gpx>'
    },
    {
      id: "route-3",
      name: "Landgoed Zuid",
      createdAt: Date.parse("2026-06-10T05:40:00Z"),
      distanceKm: 5.6,
      gpxData: '<gpx><trk><trkseg><trkpt lat="52.09210" lon="5.12920"></trkpt><trkpt lat="52.09250" lon="5.13010"></trkpt><trkpt lat="52.09310" lon="5.13100"></trkpt><trkpt lat="52.09360" lon="5.13180"></trkpt></trkseg></trk></gpx>'
    },
    {
      id: "markers-2026-demo",
      name: "Markers 2026",
      createdAt: Date.parse("2026-06-10T05:15:00Z"),
      distanceKm: 0,
      gpxData: MOCK_MARKER_GPX
    }
  ],
  routeRuns: [
    { id: "run-101", routeId: "route-1", status: "in_progress", progressPercent: 72, coveragePercent: 68, distanceKm: 3.5, offRouteMeters: 8, startedAt: Date.parse("2026-06-10T07:35:00Z") },
    { id: "run-102", routeId: "route-2", status: "completed", progressPercent: 100, coveragePercent: 96, distanceKm: 3.3, offRouteMeters: 4, startedAt: Date.parse("2026-06-10T06:45:00Z") },
    { id: "run-103", routeId: "route-3", status: "incomplete", progressPercent: 54, coveragePercent: 47, distanceKm: 2.6, offRouteMeters: 29, startedAt: Date.parse("2026-06-10T07:00:00Z") }
  ],
  trackingPoints: [
    { id: "tp-1", routeId: "route-2", runId: "run-102", latitude: 52.08790, longitude: 5.12180, timestamp: Date.parse("2026-06-10T06:46:00Z"), accuracyMeters: 4 },
    { id: "tp-2", routeId: "route-2", runId: "run-102", latitude: 52.08835, longitude: 5.12225, timestamp: Date.parse("2026-06-10T06:49:00Z"), accuracyMeters: 5 },
    { id: "tp-3", routeId: "route-2", runId: "run-102", latitude: 52.08890, longitude: 5.12300, timestamp: Date.parse("2026-06-10T06:53:00Z"), accuracyMeters: 4 },
    { id: "tp-4", routeId: "route-1", runId: "run-101", latitude: 52.08940, longitude: 5.12380, timestamp: Date.parse("2026-06-10T07:38:00Z"), accuracyMeters: 6 },
    { id: "tp-5", routeId: "route-1", runId: "run-101", latitude: 52.08990, longitude: 5.12450, timestamp: Date.parse("2026-06-10T07:43:00Z"), accuracyMeters: 5 }
  ],
  pois: [
    { id: "poi-1", routeId: "route-1", type: "Onkruid", description: "Middeneiland deels overgroeid", latitude: 52.08992, longitude: 5.12455 },
    { id: "poi-2", routeId: "route-1", type: "Paaltje", description: "Schade aan markering", latitude: 52.09040, longitude: 5.12505 },
    { id: "poi-3", routeId: "route-2", type: "Obstakel", description: "Auto dicht op inrit", latitude: 52.08882, longitude: 5.12298 },
    { id: "poi-4", routeId: "route-3", type: "Controle", description: "Route onvolledig gereden", latitude: 52.09308, longitude: 5.13102 }
  ]
};

if (window.firebase && !firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}

fetch("./assets/markers/generated/manifest.json")
  .then((response) => (response.ok ? response.json() : []))
  .then((manifest) => {
    markerAssets = Array.isArray(manifest) ? manifest : [];
    renderRouteList();
    renderRouteTable();
    renderDetailPanel();
  })
  .catch(() => {
    markerAssets = [];
  });

function setAuthStatus(message) {
  if (authStatus) authStatus.textContent = message;
}

function renderPreviewSession() {
  if (!userSession) return;
  userSession.innerHTML = `
    <p class="status-label">Previewmodus</p>
    <p class="session-user">Lokaal dashboard</p>
    <p class="session-email">Mockdata actief voor ontwerp en ontwikkeling</p>
    <div class="session-actions">
      <a class="secondary-button" href="./routes.html">Routes</a>
      <a class="secondary-button" href="./index.html">Overzicht</a>
    </div>
  `;
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

function hashString(value) {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) >>> 0;
  }
  return hash;
}

function getMarkerAssetForKey(key) {
  if (!markerAssets.length) return "";
  const entry = markerAssets[hashString(String(key)) % markerAssets.length];
  return entry?.webPath || "";
}

function buildPhotoMarkerIcon(imageUrl, altText) {
  if (!imageUrl || !window.L) return null;
  return L.divIcon({
    className: "photo-marker-wrapper",
    html: `
      <div class="photo-marker" title="${altText}">
        <img src="${imageUrl}" alt="${altText}">
      </div>
    `,
    iconSize: [54, 68],
    iconAnchor: [27, 62],
    popupAnchor: [0, -58]
  });
}

function buildDetailMarkerThumb(imageUrl, altText) {
  if (!imageUrl) {
    return '<div class="detail-marker-thumb detail-marker-thumb-placeholder"></div>';
  }
  return `
    <div class="detail-marker-thumb">
      <img src="${imageUrl}" alt="${altText}">
    </div>
  `;
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

function routePriorityMeta(route, latestRun) {
  if (!latestRun) return "Nog niet gestart";
  if (latestRun.status === "in_progress") return "Nu in uitvoering";
  if (latestRun.status === "completed") return "Route afgerond";
  if (latestRun.status === "incomplete") return "Controle nodig";
  return "Klaar voor planning";
}

function routeMetrics(route, latestRun) {
  const poisForRoute = dataState.pois.filter((poi) => String(poi.routeId) === String(route.id));
  return {
    poiCount: poisForRoute.length,
    offRouteMeters: latestRun?.offRouteMeters || 0,
    progressPercent: latestRun?.progressPercent || 0,
    coveragePercent: latestRun?.coveragePercent || 0,
    distanceKm: latestRun?.distanceKm ?? route.distanceKm ?? 0
  };
}

function routeNextAction(route, latestRun) {
  if (!latestRun) return "Plan een eerste rit en wijs een chauffeur of voertuig toe.";
  if (latestRun.status === "in_progress") return "Volg voortgang live en controleer afwijkingen tijdens de rit.";
  if (latestRun.status === "incomplete") return "Controleer waarom de rit is afgebroken en plan een vervolgactie.";
  if ((latestRun.offRouteMeters || 0) > 20) return "Onderzoek de routeafwijking en bevestig of dit toegestaan was.";
  if (latestRun.status === "completed") return "Controleer rapportage en bereid de volgende uitvoering voor.";
  return "Werk de planning en route-informatie bij voor de volgende rit.";
}

function routeRiskLabel(route, latestRun) {
  if (!latestRun) return "Geen ritdata";
  if (latestRun.status === "incomplete") return "Rit onvolledig";
  if ((latestRun.offRouteMeters || 0) > 20) return "Routeafwijking";
  if (latestRun.status === "in_progress") return "Actief volgen";
  return "Op schema";
}

function routeHealthItems(route, latestRun, metrics) {
  return [
    {
      label: "Statusbeeld",
      value: routeRiskLabel(route, latestRun),
      tone: hasAttention(route.id) ? "warn" : "ok"
    },
    {
      label: "Dekking",
      value: `${metrics.coveragePercent}%`,
      tone: metrics.coveragePercent < 60 && latestRun ? "warn" : "ok"
    },
    {
      label: "Objecten",
      value: `${metrics.poiCount} gekoppeld`,
      tone: metrics.poiCount > 0 ? "info" : "ok"
    },
    {
      label: "Afwijking",
      value: `${metrics.offRouteMeters.toFixed(0)} meter`,
      tone: metrics.offRouteMeters > 20 ? "warn" : "ok"
    }
  ];
}

function routeActionItems(route, latestRun) {
  return [
    {
      id: "assign",
      title: "Route toewijzen",
      meta: latestRun ? "Werk chauffeur of voertuig bij" : "Nog geen rit actief",
      tone: "neutral",
      cta: "Toewijzen"
    },
    {
      id: latestRun?.status === "completed" ? "report" : "control",
      title: latestRun?.status === "completed" ? "Rapport klaarzetten" : "Controle plannen",
      meta: latestRun?.status === "completed" ? "Gebruik na afgeronde rit" : "Voor incomplete of afwijkende ritten",
      tone: hasAttention(route.id) ? "warn" : "neutral",
      cta: latestRun?.status === "completed" ? "Rapport openen" : "Controle starten"
    },
    {
      id: "export",
      title: "Export / KML",
      meta: "Deel route en resultaten extern",
      tone: "neutral",
      cta: "Exporteren"
    }
  ];
}

function detailActionMessage(actionId, route, latestRun) {
  if (actionId === "assign") {
    return `Actie klaar: ${route.name} staat gemarkeerd voor toewijzing${latestRun ? ` bij ${latestRun.id}` : ""}.`;
  }
  if (actionId === "control") {
    return `Controleflow gestart voor ${route.name}. Afwijking en routeverloop staan bovenaan.`;
  }
  if (actionId === "report") {
    return `Rapportweergave voorbereid voor ${route.name}. Afronding en opvolging zijn leidend.`;
  }
  if (actionId === "export") {
    return `Exportvoorbereiding gestart voor ${route.name}. GPX/KML kan hierna gekoppeld worden.`;
  }
  return `Actie verwerkt voor ${route.name}.`;
}

function routeDominantMessage(route, latestRun, metrics) {
  if (!latestRun) return "Nog niet gestart: plan eerste rit vandaag.";
  if (latestRun.status === "incomplete") {
    return `Controle nodig: ${metrics.offRouteMeters.toFixed(0)} m off-route, rit onvolledig.`;
  }
  if (metrics.offRouteMeters > 20) {
    return `Afwijking registreren: ${metrics.offRouteMeters.toFixed(0)} m buiten route.`;
  }
  if (latestRun.status === "in_progress") {
    return `Actief volgen: ${metrics.progressPercent}% voortgang, ${metrics.coveragePercent}% dekking.`;
  }
  if (latestRun.status === "completed") {
    return "Afgerond: rapport en opvolging controleren.";
  }
  return "Klaar voor planning en toewijzing.";
}

function routeFocusLabel(route, latestRun, metrics) {
  if (!latestRun) return { label: "Inplannen", tone: "idle" };
  if (latestRun.status === "incomplete") return { label: "Direct controleren", tone: "warn" };
  if (metrics.offRouteMeters > 20) return { label: "Afwijking onderzoeken", tone: "warn" };
  if (latestRun.status === "in_progress") return { label: "Live volgen", tone: "live" };
  if (latestRun.status === "completed") return { label: "Rapport nalopen", tone: "ok" };
  return { label: "Opvolgen", tone: "idle" };
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

function renderAll() {
  renderOverviewMetrics();
  renderExceptions();
  renderRouteAttentionList();
  renderRouteList();
  renderRouteTable();
  renderDetailPanel();
}

function scrollToDetailOnMobile() {
  if (!detailPane || window.innerWidth > 640) return;
  window.setTimeout(() => {
    detailPane.scrollIntoView({ behavior: "smooth", block: "start" });
  }, 80);
}

function applyMockData() {
  isUsingMockData = true;
  dataState.routes = MOCK_DATA.routes.map((item) => ({ ...item }));
  dataState.routeRuns = MOCK_DATA.routeRuns.map((item) => ({ ...item }));
  dataState.trackingPoints = MOCK_DATA.trackingPoints.map((item) => ({ ...item }));
  dataState.pois = MOCK_DATA.pois.map((item) => ({ ...item }));
  const operationalRoutes = getOperationalRoutes();
  if (!selectedRouteId && operationalRoutes.length) {
    selectedRouteId = operationalRoutes[0].id;
  }
  renderPreviewSession();
  renderAll();
}

function renderOverviewMetrics() {
  const operationalRoutes = getOperationalRoutes();
  const routesCount = operationalRoutes.length;
  const attentionRoutes = operationalRoutes.filter((route) => hasAttention(route.id)).length;
  const inProgressRuns = dataState.routeRuns.filter((run) => run.status === "in_progress");
  const completedRuns = dataState.routeRuns.filter((run) => run.status === "completed");
  const archivedTrackingPoints = dataState.trackingPoints.filter((point) => {
    const run = dataState.routeRuns.find((item) => String(item.id) === String(point.runId));
    return isCheckedOffRun(run);
  });

  if (metricRoutes) metricRoutes.textContent = String(routesCount);
  if (metricRoutesMeta) metricRoutesMeta.textContent = routesCount ? `${attentionRoutes} met aandachtspunt` : "Nog geen routes geladen";
  if (metricVehicles) metricVehicles.textContent = String(inProgressRuns.length);
  if (metricVehiclesMeta) metricVehiclesMeta.textContent = inProgressRuns.length ? "Ritten nu in uitvoering" : "Geen routes actief";
  if (metricCompleted) metricCompleted.textContent = String(completedRuns.length);
  if (metricCompletedMeta) metricCompletedMeta.textContent = completedRuns.length ? "Afgeronde ritten in Firestore" : "Nog geen afgeronde ritten";

  if (opRoutes) opRoutes.textContent = String(routesCount);
  if (opRoutesMeta) opRoutesMeta.textContent = routesCount ? `${operationalRoutes.filter((route) => hasAttention(route.id)).length} routes vragen aandacht` : "Nog geen routes geladen";
  if (opRuns) opRuns.textContent = String(completedRuns.length);
  if (opRunsMeta) opRunsMeta.textContent = completedRuns.length ? "Afgevinkte ritten" : "Nog geen afgeronde ritten";
  if (opTracking) opTracking.textContent = String(archivedTrackingPoints.length);
  if (opTrackingMeta) opTrackingMeta.textContent = archivedTrackingPoints.length ? "Bewaarde GPS-punten na afronden" : "Nog geen bewaarde GPS-punten";
}

function getExceptionItems() {
  const items = [];
  const now = Date.now();
  const routes = getOperationalRoutes();

  routes.forEach((route) => {
    const latestRun = getLatestRunForRoute(route.id);
    if (!latestRun) {
      items.push({
        severity: "info",
        title: `${route.name} is nog niet gestart`,
        meta: "Plan of start deze route vanuit de mobiele app."
      });
      return;
    }

    if (latestRun.status === "incomplete") {
      items.push({
        severity: "warn",
        title: `${route.name} is incompleet afgesloten`,
        meta: `${latestRun.progressPercent || 0}% voortgang • ${latestRun.coveragePercent || 0}% dekking`
      });
    }

    if ((latestRun.offRouteMeters || 0) > 20) {
      items.push({
        severity: "warn",
        title: `${route.name} wijkt af van de route`,
        meta: `${Number(latestRun.offRouteMeters).toFixed(0)} meter off-route geregistreerd`
      });
    }

    if (latestRun.status === "in_progress" && latestRun.startedAt && now - latestRun.startedAt > 1000 * 60 * 90) {
      items.push({
        severity: "live",
        title: `${route.name} loopt al lang door`,
        meta: `Gestart op ${formatDate(latestRun.startedAt)}`
      });
    }
  });

  return items
    .sort((a, b) => {
      const score = { warn: 0, live: 1, info: 2 };
      return score[a.severity] - score[b.severity];
    })
    .slice(0, 6);
}

function renderExceptions() {
  if (!exceptionList) return;
  const items = getExceptionItems();
  exceptionList.innerHTML = items.length
    ? items
        .map(
          (item) => `
            <article class="exception-item">
              <strong class="pill ${item.severity === "warn" ? "attention" : item.severity === "live" ? "live" : "idle"}">${item.severity === "warn" ? "Controle" : item.severity === "live" ? "Actief" : "Info"}</strong>
              <div>
                <p class="exception-title">${item.title}</p>
                <p class="exception-meta">${item.meta}</p>
              </div>
            </article>
          `
        )
        .join("")
    : '<article class="exception-item"><strong class="pill success">Rustig</strong><div><p class="exception-title">Geen urgente afwijkingen</p><p class="exception-meta">Routes en ritten tonen nu geen directe aandachtspunten.</p></div></article>';
}

function renderRouteAttentionList() {
  if (!routeAttentionList) return;
  const items = [...getOperationalRoutes()]
    .map((route) => {
      const latestRun = getLatestRunForRoute(route.id);
      const metrics = routeMetrics(route, latestRun);
      const statusInfo = getStatusInfo(latestRun?.status, route.id);
      return {
        route,
        latestRun,
        metrics,
        statusInfo,
        urgentScore: latestRun?.status === "incomplete" ? 3 : metrics.offRouteMeters > 20 ? 2 : latestRun?.status === "in_progress" ? 1 : 0,
        dominantMessage: routeDominantMessage(route, latestRun, metrics)
      };
    })
    .sort((a, b) => b.urgentScore - a.urgentScore || (b.latestRun?.startedAt || 0) - (a.latestRun?.startedAt || 0))
    .slice(0, 3);
  routeAttentionList.innerHTML = items.length
    ? items
        .map(
          (item) => `
            <article class="exception-item">
              <strong class="pill ${item.statusInfo.className}">${item.urgentScore >= 2 ? "Nu" : item.latestRun?.status === "in_progress" ? "Live" : "Info"}</strong>
              <div>
                <p class="exception-title">${item.route.name}</p>
                <p class="exception-meta">${item.dominantMessage}</p>
                <div class="detail-badges">
                  <span class="micro-badge ${item.metrics.offRouteMeters > 20 ? "warn" : ""}">${item.metrics.offRouteMeters.toFixed(0)} m afwijking</span>
                  <span class="micro-badge">${item.metrics.progressPercent}% voortgang</span>
                </div>
              </div>
            </article>
          `
        )
        .join("")
    : '<article class="exception-item"><strong class="pill success">Rustig</strong><div><p class="exception-title">Geen directe blokkades</p><p class="exception-meta">Alle routes ogen stabiel in de huidige preview.</p></div></article>';
}

function renderRouteList() {
  if (!routeList) return;

  const latestRoutes = [...getOperationalRoutes()]
    .sort((a, b) => {
      const aAttention = hasAttention(a.id) ? 1 : 0;
      const bAttention = hasAttention(b.id) ? 1 : 0;
      if (aAttention !== bAttention) return bAttention - aAttention;
      return (b.createdAt || 0) - (a.createdAt || 0);
    })
    .slice(0, 5);

  routeList.innerHTML = latestRoutes.length
    ? latestRoutes
        .map((route) => {
          const latestRun = getLatestRunForRoute(route.id);
          const statusInfo = getStatusInfo(latestRun?.status, route.id);
          const metrics = routeMetrics(route, latestRun);
          const priorityMeta = hasAttention(route.id) ? "Heeft aandacht nodig" : routePriorityMeta(route, latestRun);
          return `
            <article class="route-item">
              <div class="route-summary">
                ${buildRoutePreviewUrl(route)
                  ? `<img class="route-thumb" src="${buildRoutePreviewUrl(route)}" alt="Preview van ${route.name}">`
                  : '<div class="route-thumb-placeholder"></div>'}
                <div>
                  <p class="route-name">${route.name}</p>
                  <p class="route-meta">${priorityMeta} • ${formatDate(route.createdAt)}</p>
                  <div class="route-inline-badges">
                    <span class="micro-badge">${metrics.distanceKm.toFixed(1)} km</span>
                    <span class="micro-badge">${metrics.poiCount} objecten</span>
                    <span class="micro-badge ${metrics.offRouteMeters > 20 ? "warn" : ""}">${metrics.offRouteMeters.toFixed(0)} m afwijking</span>
                  </div>
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
          const metrics = routeMetrics(route, latestRun);
          const startedLabel = latestRun?.startedAt ? formatDate(latestRun.startedAt) : "Nog niet gestart";
          const routeStateLabel = routePriorityMeta(route, latestRun);
          const dominantMessage = routeDominantMessage(route, latestRun, metrics);
          const focusLabel = routeFocusLabel(route, latestRun, metrics);

          return `
            <article class="route-table-item is-clickable ${selectedClass}" data-route-id="${route.id}">
              <div class="route-summary">
                ${previewUrl
                  ? `<img class="route-thumb" src="${previewUrl}" alt="Preview van ${route.name}">`
                  : '<div class="route-thumb-placeholder"></div>'}
                <div>
                  <div class="route-card-topline">
                    <strong class="pill ${statusInfo.className}">${statusInfo.label}</strong>
                    <span class="route-focus-chip route-focus-chip-${focusLabel.tone}">${focusLabel.label}</span>
                  </div>
                  <strong>${route.name}</strong>
                  <span class="route-meta">${routeStateLabel} • ${formatDate(route.createdAt)}</span>
                  <span class="route-meta route-secondary-meta">${archivedPointCount} bewaarde punten • ${metrics.distanceKm.toFixed(1)} km</span>
                  <p class="route-primary-alert">${dominantMessage}</p>
                  <div class="route-inline-badges">
                    <span class="micro-badge ${metrics.poiCount > 0 ? "strong" : ""}">${metrics.poiCount} objecten</span>
                    <span class="micro-badge ${metrics.offRouteMeters > 20 ? "warn" : ""}">${metrics.offRouteMeters.toFixed(0)} m afwijking</span>
                  </div>
                </div>
              </div>
              <div class="route-table-stack route-stack-run">
                <strong>${vehicleLabel}</strong>
                <span class="route-meta">${startedLabel}</span>
                <span class="route-meta">${isCheckedOffRun(latestRun) ? "Afgevinkt en opgeslagen" : "Nog niet afgevinkt"}</span>
              </div>
              <div class="route-table-stack route-stack-progress">
                <strong>${progressLabel}</strong>
                <div class="route-progress-track">
                  <div class="route-progress-fill" style="width: ${metrics.progressPercent}%;"></div>
                </div>
                <span class="route-meta">${latestRun ? `${metrics.coveragePercent}% dekking` : "Nog geen ritdata"}</span>
              </div>
              <div class="route-table-stack route-table-status route-stack-status">
                <strong class="pill ${statusInfo.className}">${statusInfo.label}</strong>
                <span class="route-meta">${hasAttention(route.id) ? "Direct oppakken" : "Op schema"}</span>
              </div>
              <div class="route-card-footer">
                <span class="route-meta">Start: ${startedLabel}</span>
                <span class="route-card-link">${String(selectedRouteId) === String(route.id) ? "Detail actief" : "Open detail"}</span>
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
      scrollToDetailOnMobile();
    });
  });
}

function renderDetailPanel() {
  if (!detailTitle || !detailStatus || !detailFocus || !detailPreview || !detailStats || !detailHealth || !detailActions || !detailConnection || !detailRuns || !detailPois || !detailMarkers) return;

  const operationalRoutes = getOperationalRoutes();
  const route = operationalRoutes.find((item) => String(item.id) === String(selectedRouteId)) || operationalRoutes[0];
  if (!route) {
    detailTitle.textContent = "Nog geen route geselecteerd";
    detailStatus.textContent = "Geen status";
    detailStatus.className = "pill idle";
    detailFocus.innerHTML = "";
    detailPreview.innerHTML = '<div class="detail-preview-empty">Nog geen preview beschikbaar.</div>';
    detailStats.innerHTML = "";
    detailHealth.innerHTML = "";
    detailActions.innerHTML = "";
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
  const metrics = routeMetrics(route, latestRun);
  const nextAction = routeNextAction(route, latestRun);
  const riskLabel = routeRiskLabel(route, latestRun);
  const healthItems = routeHealthItems(route, latestRun, metrics);
  const actionItems = routeActionItems(route, latestRun);
  const focusLabel = routeFocusLabel(route, latestRun, metrics);

  detailTitle.textContent = route.name;
  detailStatus.textContent = statusInfo.label;
  detailStatus.className = `pill ${statusInfo.className}`;
  detailFocus.innerHTML = `
    <article class="detail-focus-card detail-focus-card-${focusLabel.tone}">
      <div>
        <p class="detail-focus-label">Nu oppakken</p>
        <strong>${focusLabel.label}</strong>
        <span>${routeDominantMessage(route, latestRun, metrics)}</span>
      </div>
      <div class="detail-badges">
        <span class="micro-badge ${hasAttention(route.id) ? "warn" : "strong"}">${riskLabel}</span>
        <span class="micro-badge">${metrics.progressPercent}% voortgang</span>
        <span class="micro-badge">${metrics.poiCount} objecten</span>
      </div>
    </article>
  `;
  detailPreview.innerHTML = previewUrl
    ? `<img src="${previewUrl}" alt="Preview van ${route.name}">`
    : '<div class="detail-preview-empty">Geen previewafbeelding voor deze route.</div>';
  detailStats.innerHTML = `
    <div class="detail-stat-card">
      <span>Voortgang</span>
      <strong>${latestRun ? `${latestRun.progressPercent}%` : "0%"}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Dekking</span>
      <strong>${latestRun ? `${latestRun.coveragePercent}%` : "0%"}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Objecten</span>
      <strong>${pois.length}</strong>
    </div>
    <div class="detail-stat-card">
      <span>Spoor</span>
      <strong>${latestTrackingPoint ? "Opgeslagen" : latestRun ? "Nog open" : "Geen run"}</strong>
      <em>${latestTrackingPoint ? formatDate(latestTrackingPoint.timestamp) : `${trackingPoints.length} punten bewaard`}</em>
    </div>
  `;

  detailHealth.innerHTML = `
    <div class="detail-list-item detail-health-hero ${hasAttention(route.id) ? "detail-health-hero-warn" : "detail-health-hero-ok"}">
      <strong>${hasAttention(route.id) ? "Route vraagt opvolging" : "Route ligt op koers"}</strong>
      <span>${nextAction}</span>
    </div>
    ${healthItems
      .map(
        (item) => `
          <div class="detail-list-item detail-health-item">
            <span class="detail-health-label">${item.label}</span>
            <strong class="detail-health-value ${item.tone === "warn" ? "detail-health-value-warn" : ""}">${item.value}</strong>
          </div>
        `
      )
      .join("")}
  `;

  detailActions.innerHTML = actionItems
    .map(
      (item) => `
        <article class="detail-action-card ${item.tone === "warn" ? "detail-action-card-warn" : ""}">
          <div class="detail-action-copy">
            <strong>${item.title}</strong>
            <span>${item.meta}</span>
          </div>
          <button class="detail-action-button ${item.tone === "warn" ? "detail-action-button-warn" : ""}" type="button" data-route-action="${item.id}">${item.cta}</button>
        </article>
      `
    )
    .join("");
  if (detailActionState) {
    detailActionState.innerHTML = `<span>Kies een actie voor ${route.name}.</span>`;
  }
  detailActions.querySelectorAll("[data-route-action]").forEach((button) => {
    button.addEventListener("click", () => {
      const actionId = button.getAttribute("data-route-action") || "";
      if (detailActionState) {
        detailActionState.innerHTML = `<span>${detailActionMessage(actionId, route, latestRun)}</span>`;
      }
    });
  });

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
    <div class="detail-list-item detail-callout ${hasAttention(route.id) ? "detail-callout-warn" : "detail-callout-ok"}">
      <strong>${hasAttention(route.id) ? "Aandacht nodig" : "Routebeeld stabiel"}</strong>
      <span>${nextAction}</span>
      <div class="detail-badges">
        <span class="micro-badge ${hasAttention(route.id) ? "warn" : "strong"}">${riskLabel}</span>
        <span class="micro-badge">${metrics.poiCount} objecten</span>
        <span class="micro-badge">${metrics.distanceKm.toFixed(1)} km</span>
      </div>
    </div>
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
        .map((poi) => {
          const imageUrl = getMarkerAssetForKey(`${poi.id}-${poi.type}-${poi.latitude}-${poi.longitude}`);
          return `
          <div class="detail-list-item detail-list-item-with-thumb">
            ${buildDetailMarkerThumb(imageUrl, poi.type || "Object")}
            <div>
              <strong>${poi.type}</strong>
              <span>${poi.description || "Geen beschrijving"} • ${poi.latitude.toFixed(5)}, ${poi.longitude.toFixed(5)}</span>
            </div>
          </div>
        `;
        })
        .join("")
    : '<div class="detail-list-item"><strong>Geen objecten</strong><span>Er zijn nog geen POI’s of meldingen gekoppeld.</span></div>';

  detailMarkers.innerHTML = markerCollectionPoints.length
    ? markerCollectionPoints
        .slice(0, 8)
        .map((point) => {
          const imageUrl = getMarkerAssetForKey(`${point.sourceRouteName}-${point.name}-${point.latitude}-${point.longitude}`);
          return `
          <div class="detail-list-item detail-list-item-with-thumb">
            ${buildDetailMarkerThumb(imageUrl, point.name || "Markerpunt")}
            <div>
              <strong>${point.name}</strong>
              <span>${point.description || "Los kaartpunt uit markers2026"} • ${point.latitude.toFixed(5)}, ${point.longitude.toFixed(5)}</span>
            </div>
          </div>
        `;
        })
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
    const imageUrl = getMarkerAssetForKey(`${poi.id}-${poi.type}-${poi.latitude}-${poi.longitude}`);
    const icon = buildPhotoMarkerIcon(imageUrl, poi.type || "Object");
    const marker = icon
      ? L.marker([poi.latitude, poi.longitude], { icon })
      : L.circleMarker([poi.latitude, poi.longitude], {
          radius: 6,
          color: "#9b5b00",
          fillColor: "#f1a43a",
          fillOpacity: 0.95,
          weight: 2
        });
    marker.bindPopup(`<strong>${poi.type}</strong><br>${poi.description || "Geen beschrijving"}`);
    marker.addTo(routeLayerGroup);
    bounds.push([poi.latitude, poi.longitude]);
  });

  markerCollectionPoints.forEach((point) => {
    const imageUrl = getMarkerAssetForKey(`${point.sourceRouteName}-${point.name}-${point.latitude}-${point.longitude}`);
    const icon = buildPhotoMarkerIcon(imageUrl, point.name || "Markerpunt");
    const marker = icon
      ? L.marker([point.latitude, point.longitude], { icon })
      : L.circleMarker([point.latitude, point.longitude], {
          radius: 7,
          color: "#7c3aed",
          fillColor: "#9f67ff",
          fillOpacity: 0.9,
          weight: 2
        });
    marker.bindPopup(
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
  if (hasSubscribedToData) return;
  hasSubscribedToData = true;
  const db = firebase.firestore();

  db.collection("routes").onSnapshot((snapshot) => {
    isUsingMockData = false;
    dataState.routes = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    const operationalRoutes = getOperationalRoutes();
    if (!selectedRouteId && operationalRoutes.length) {
      selectedRouteId = operationalRoutes[0].id;
    }
    renderAll();
  });

  db.collection("routeRuns").onSnapshot((snapshot) => {
    dataState.routeRuns = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    renderAll();
  });

  db.collection("trackingPoints").onSnapshot((snapshot) => {
    dataState.trackingPoints = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    renderAll();
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
    if (user) {
      renderUserSession(user);
    } else if (!isUsingMockData) {
      renderUserSession(user);
    }

    if (isProtectedPage && !user && isLocalPreviewContext) {
      applyMockData();
      return;
    }

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
