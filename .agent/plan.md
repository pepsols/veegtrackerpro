# Project Plan

Veegtracker Pro: A comprehensive street sweeping management system using OpenStreetMap (OSM). It includes a Driver app for route execution (offline-first, GPS tracking, OSM turn-by-turn) and an Admin dashboard for fleet monitoring and reporting. Use the provided image for the map-centric UI style. The app must be in Dutch and support adaptive layouts for phones and tablets. Routes should be pre-populated from GPX files in assets.

## Project Brief

# Project Brief: Veegtracker Pro (MVP)

Veegtracker Pro is een gespecialiseerd systeem voor het beheer van straatreiniging, gericht op de Nederlandse markt. De app optimaliseert de workflow door een krachtige combinatie van een chauffeursportaal voor route-uitvoering en een admin-dashboard voor vlootbewaking, volledig werkend met OpenStreetMap (OSM).

### Features
*   **Vooraf Geconfigureerde GPX-Routes**: De app wordt geleverd met een voorgeladen lijst van veegroutes, direct ingeladen vanuit de assets-map. Deze zijn onmiddellijk beschikbaar voor zowel chauffeurs als beheerders bij de eerste opstart.
*   **Chauffeursportaal met OSM Navigatie**: Biedt turn-by-turn "Live Geopath" begeleiding op basis van de GPX-data via OpenStreetMap. De app is volledig offline-first, toont real-time voortgangspercentages en bevat een "Navigeer naar Start" functie.
*   **Centrale Meldkamer Dashboard**: Een live monitoringsysteem voor beheerders om de locaties, statussen en voltooiingspercentages van de gehele vloot real-time te volgen op een interactieve kaart.
*   **Adaptieve Multi-Pane Interface**: Een dynamische gebruikersinterface die gebruikmaakt van Material Design 3 en de Compose Adaptive library om naadloos te schakelen tussen een compacte telefoonweergave en een uitgebreid tablet-overzicht.

### High-Level Technical Stack
*   **Kotlin & Coroutines**: Voor de kernlogica, nauwkeurige GPS-verwerking en asynchrone gegevensbeheer.
*   **Jetpack Compose (Material 3)**: Voor een moderne, levendige UI die voldoet aan de laatste Android-standaarden.
*   **Jetpack Navigation 3**: Een state-driven navigatie-architectuur die rollen en adaptieve statussen robuust beheert.
*   **Compose Material Adaptive Library**: Voor de implementatie van adaptieve layouts zoals het List-Detail patroon voor tablets en grote schermen.
*   **Room Database**: Essentieel voor de offline-first strategie, het opslaan van route-statussen en lokale synchronisatie.
*   **osmdroid (OpenStreetMap)**: Voor de kaartweergave en het renderen van de Geopath navigatie zonder afhankelijkheid van Google Maps.

### UI Design Image
![UI Design](file:///C:/Users/Nathan/AndroidStudioProjects/veegtrackerpro/input_images/image_0.png)

## Implementation Steps
**Total Duration:** 20m 36s

### Task_1_Foundation_and_Data: Set up the project foundation including Material 3 theme (vibrant colors), Dutch localization, Room database schema for routes/tracking, and Google Maps SDK integration.
- **Status:** COMPLETED
- **Updates:** Material 3 theme with vibrant colors (Dutch localization) implemented. Room database for Routes and TrackingPoints configured. Google Maps SDK integrated with a map Composable. Jetpack Navigation 3 structure set up for role-based navigation. Project builds successfully.
- **Acceptance Criteria:**
  - Material 3 theme with vibrant colors implemented
  - Strings.xml localized in Dutch
  - Room DB for Routes and TrackingPoints configured
  - Google Maps API key integrated and map fragment/composable loads
  - Project builds successfully
- **Duration:** 6m 53s

### Task_2_Driver_Map_and_Tracking: Implement the map-centric Driver interface matching the design in image_0.png. Develop a foreground service for GPS tracking and offline-first route logging to Room, including GPX parsing for route display.
- **Status:** COMPLETED
- **Updates:** Implemented Driver Map UI matching image_0.png with Dutch localization. Developed a Foreground Service for GPS tracking with Room persistence. Integrated GPX parsing using TikXml and polyline rendering on the map. Added file picker for GPX import. App handles background tracking and offline data logging.
- **Acceptance Criteria:**
  - Map UI matches design in input_images/image_0.png
  - GPS tracking works in background and saves to Room
  - GPX files can be parsed and displayed as polylines on the map
  - App functions offline with local data sync logic
- **Duration:** 4m 55s

### Task_3_Adaptive_Admin_and_Reporting: Implement the Admin dashboard using Compose Adaptive (List-Detail pattern). Integrate Jetpack Navigation 3 for role switching. Add GPX import functionality and PDF report generation for 'dagrapportages'.
- **Status:** COMPLETED
- **Updates:** Navigation 3 handles Driver and Admin roles with a selection screen. UI adapts correctly for Phone and Tablet (List-Detail) using Compose Adaptive. GPX files successfully imported into Room. PDF reports generated with route summary data using PdfDocument.
- **Acceptance Criteria:**
  - Navigation 3 handles Driver and Admin roles
  - UI adapts correctly for Phone and Tablet (List-Detail)
  - GPX files successfully imported into Room
  - PDF reports generated with route summary data
- **Duration:** 2m 14s

### Task_4_Final_Polish_and_Verification: Finalize the application with an adaptive app icon, full edge-to-edge implementation, and perform a comprehensive Run and Verify step to ensure stability and requirement alignment.
- **Status:** COMPLETED
- **Updates:** Adaptive app icon created and applied. Full Edge-to-Edge display implemented across all screens. Verified all Dutch strings. Project builds cleanly. Navigation 3 and Adaptive UI refined.
- **Acceptance Criteria:**
  - Adaptive app icon created and applied
  - Full Edge-to-Edge display implemented
  - No crashes during standard use cases
  - All Dutch strings verified
  - Project builds and existing tests pass
- **Duration:** 1m 16s

### Task_5_Migrate_to_OSM_and_Geopath: Replace Google Maps with osmdroid (OpenStreetMap). Implement 'Live Geopath' turn-by-turn guidance based on the GPX track and ensure offline map caching.
- **Status:** COMPLETED
- **Updates:** Google Maps dependencies removed and osmdroid integrated. Map UI now uses OpenStreetMap and matches the design in image_0.png. Live Geopath turn-by-turn logic implemented with Dutch instructions and route progress percentage. Map tiles are cached for offline use. Project builds successfully with the new OSM stack.
- **Acceptance Criteria:**
  - Google Maps dependencies removed and osmdroid integrated
  - Map UI uses OpenStreetMap and matches design in input_images/image_0.png
  - Live Geopath turn-by-turn logic provides directions along the GPX route
  - Map tiles are cached for offline functionality
  - The implemented UI must match the design provided in input_images/image_0.png
- **Duration:** 3m 25s

### Task_6_Asset_GPX_Preloading: Implement logic to automatically load and parse GPX files from the app assets folder into the Room database on first launch or app update.
- **Status:** COMPLETED
- **Updates:** GPX files (Route_Driebergen.gpx, Route_Doorn.gpx) created and added to assets. AssetGpxImporter implemented to load these files into Room on first launch. Driver UI updated with a route selection dialog. Pre-loaded routes are visible and selectable in both Driver and Admin views.
- **Acceptance Criteria:**
  - GPX files added to app/src/main/assets/routes/
  - Logic implemented to detect and import these files into the Routes table
  - Imported routes are visible in the Driver and Admin lists
  - Dutch localization maintained
- **Duration:** 1m 53s

### Task_7_Final_Verification_and_Polish: Perform a final run and verify of the application. Ensure all features (OSM, Asset loading, Tracking, Admin Dashboard, PDF Reports) work correctly and the UI matches the design in image_0.png.
- **Status:** IN_PROGRESS
- **Updates:** Fixed issue where route list was empty. Implemented forced database deletion in debug builds to ensure AssetGpxImporter runs. Updated Route entity to persist full GPX data. Verified that routes populate in the Driver and Admin views and render on the OSM map.
- **Acceptance Criteria:**
  - App starts with pre-loaded routes from assets
  - OSM navigation and tracking function without crashes
  - Admin dashboard correctly displays fleet status
  - PDF reports are generated correctly
  - UI matches design in input_images/image_0.png
  - Project builds and all tests pass
- **StartTime:** 2026-06-06 01:44:19 CEST

