# Dashboard HTML Plan

## Doel

De huidige HTML-laag in `app/src/main/assets/web/index.html` verschuift van een databron-gedreven scherm naar een operationeel dashboard dat eerst antwoord geeft op:

1. Wat vraagt nu actie?
2. Wat loopt buiten plan?
3. Waar zitten de belangrijkste uitzonderingen?

## Dual AI status

- `Codex`: planuitwerking gelukt via `tools/codex-gemini.ps1`.
- `Gemini`: niet afgerond door `429 RESOURCE_EXHAUSTED`; de lokale Gemini CLI meldt dat de prepayment credits op zijn.

## Nieuwe informatie-architectuur

- `Overzicht`: KPI's, prioriteiten, live status, snelle acties.
- `Operatie`: actieve routes, voertuigen, kaart, routevoortgang.
- `Planning`: backlog, AI-aanbevelingen, capaciteitsgaten, herverdeling.
- `Uitzonderingen`: incomplete runs, routes zonder start, off-route, urgente meldingen.
- `Inzichten`: prestatie- en kwaliteitsblokken als secundaire laag.

## Nieuwe HTML-indeling

- `Topbar`
  - producttitel
  - live/sync-status
  - rolselector
  - datum- of periodefilter
  - primaire actie
- `Prioriteitenstrook`
  - `nu`
  - `controle`
  - `gepland`
  - `uitzonderingen`
- `Hoofdgrid`
  - links: kaart als primair werkvlak
  - rechts: vast contextpaneel voor geselecteerde route, taak of melding
- `Onderste secties`
  - operations vandaag
  - planning backlog
  - uitzonderingenlijst
  - prestatie-inzichten

## Gefaseerd uitvoerplan

### Fase 1: Quick wins

- Vervang tabs `routes`, `meldingen`, `taken`, `ai` door `overzicht`, `operatie`, `planning`, `uitzonderingen`.
- Maak van de huidige stats-bar een prioriteitenstrook met urgentie.
- Voeg een exception-widget toe met de top 3 afwijkingen.
- Verplaats de detail-overlay naar een vast rechter contextpaneel.
- Zet AI om van losse tab naar een aanbevelingen-card in planning.

### Fase 2: Middelgroot

- Bouw losse panelen voor operatie, planning en uitzonderingen.
- Voeg prioriteitslabels toe: `nu`, `controle`, `gepland`.
- Introduceer filters voor gebied, status, ernst en bron.
- Koppel meldingen aan operatie of uitzonderingen in plaats van aan een losse lijst.
- Voeg KPI's toe voor op tijd gestart, afgerond, off-route en open opvolging.

### Fase 3: Grotere vernieuwing

- Rolgerichte dashboardmodes voor meldkamer, planner en teamleider.
- Werkdekkingslaag of heatmap op de kaart.
- AI-planningsassistent met accept/reject en impact-uitleg.
- Prestatie- en kwaliteitsinzichten met trends per route, gebied en team.
- Audit trail en bewijs per taak of melding.

## Concrete componenten

### Operations

- live operatiekaart
- actieve routes-lijst met status, voortgang, ETA en laatste update
- snelle acties: taak maken, route openen, escaleren
- volgende actie-widget voor de geselecteerde route

### Planning

- planningboard met `nu`, `controle`, `gepland`
- backloglijst met prioriteit, gebied en bron
- AI-aanbevelingen met impacttekst
- herverdeelpaneel voor capaciteit en bezetting

### Uitzonderingen

- exception-widget bovenaan
- afwijkingenlijst met severity, leeftijd en eigenaar
- detailkaart met oorzaak, locatie en aanbevolen actie
- filterchips voor incomplete runs, geen start, off-route en meldingen zonder opvolging

## Responsive aandachtspunten

- Verwijder vaste breedtes zoals `380px` sidebar en `320px` details.
- Houd op tablet een 2-pane layout aan.
- Maak op mobiel de kaart full-width en verplaats details naar een bottom sheet.
- Laat KPI's horizontaal scrollen op kleine schermen.
- Maak filters en snelle acties sticky.
- Vermijd modale overlays waar inline of sheet-interactie beter werkt.

## Behouden, herschikken, vervangen

### Behouden

- Leaflet-kaart
- Firestore live listeners
- snelle taakactie als concept
- detailrender-logica als basis

### Herschikken

- stats-bar naar prioriteitenstrook
- details-overlay naar vast contextpaneel
- AI-tab naar planning
- meldingen integreren in operatie en uitzonderingen

### Vervangen

- databron-gebaseerde tabstrip
- generieke lijstweergave als hoofdpatroon
- `prompt`, `alert`, `confirm` interacties
- POI-only kaartlaag zonder operationele context

## Aanbevolen eerstvolgende implementatie

Maak eerst een nieuwe wireframe-versie van `index.html` met:

1. topbar
2. prioriteitenstrook
3. kaart links
4. vast contextpaneel rechts
5. drie onderste secties voor operatie, planning en uitzonderingen
