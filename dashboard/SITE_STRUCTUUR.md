# Veegtracker Pro webstructuur

De webinterface staat in `app/src/main/assets/web/index.html` en is een single-page dashboard. Er zijn geen aparte URL-pagina's; navigatie gebeurt binnen dezelfde pagina via tabs, overlays en acties.

```mermaid
flowchart TD
    A[index.html<br/>Cloud Dashboard] --> B[Sidebar]
    A --> C[Kaartweergave]
    A --> D[Details-overlay]
    A --> E[Snelle taak knop]

    B --> B1[Tab: Routes]
    B --> B2[Tab: Meldingen]
    B --> B3[Tab: Taken]
    B --> B4[Tab: AI Agent]

    B1 --> F[Routes-lijst]
    B2 --> G[Meldingen-lijst]
    B3 --> H[Taken-lijst]
    B4 --> I[AI-suggesties]

    F --> D
    G --> D
    H --> D
    I --> D

    D --> J[Route-details]
    D --> K[POI-details + kaart centreert]
    D --> L[Taakdetails + verwijder taak]
    D --> M[AI-detail + accepteer suggestie]

    M --> N[Nieuwe taak in Firestore]
    E --> O[Prompt: taak omschrijving]
    O --> N
    L --> P[Taak verwijderen]

    C --> Q[Leaflet kaart]
    G --> Q
    K --> Q

    R[Firestore listeners] --> F
    R --> G
    R --> H
    G --> I
```

## Navigatiestromen

1. `index.html` opent direct het dashboard met standaard de tab `Routes`.
2. Klik op `Routes`, `Meldingen`, `Taken` of `AI Agent` wisselt alleen de inhoud van de lijst binnen dezelfde pagina.
3. Klik op een lijstitem opent de details-overlay rechtsboven.
4. Vanuit `Meldingen` zoomt de kaart naar de gekozen melding.
5. Vanuit `AI Agent` kan een suggestie worden geaccepteerd, wat een taak aanmaakt.
6. Vanuit `SNELLE TAAK MAKEN` kan direct een nieuwe taak worden toegevoegd.
7. Vanuit `Taken` kan een bestaande taak weer worden verwijderd.
