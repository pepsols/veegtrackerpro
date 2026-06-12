# Veegtracker Pro Product Design Audit

Datum: 2026-06-12
Doel: snelle UX-, design- en accessibility-audit van de Android-app op basis van emulator-captures en parallelle AI-agent code-audits.

## Vastgelegde stappen

1. App gestart op emulator.
   Status: werkend, maar eerste zichtbare state was alleen splash.
   Evidence: `01-start.png`

2. Chauffeurs-home na opstart.
   Status: werkend en auditbaar.
   Evidence: `02-post-launch.png`

3. Navigatielade geopend vanuit chauffeurs-home.
   Status: werkend en auditbaar.
   Evidence: `03-drawer.png`

4. Eerste poging naar admin-flow via lade.
   Status: navigatie gedraagt zich instabiel in capture; geen betrouwbare admin-eindstate vastgelegd.
   Evidence: `04-admin-list.png`, `05-admin-after-nav.png`

## Kernbevindingen

### Chauffeursflow

- De kaart wordt zwaar overlapt door vaste panelen en CTA's, waardoor het primaire werkvlak visueel dichtslibt.
- De app start in debug direct in de chauffeurskaart in plaats van in login of rolkeuze; dat versnelt testen maar verbergt echte instapfrictie.
- Locatiepermissie wordt direct op schermopening gevraagd zonder voorafgaande uitleg.
- Routekeuze voelt impliciet: er lijkt direct een route geladen te worden, terwijl de UI nog steeds een routekiezer toont.
- De POI-flow lijkt gestructureerd, maar waarschijnlijk te zwaar voor snel veldwerk.

### Adminflow

- De primaire importactie is te cryptisch: een plus-icoon dat feitelijk een GPX-import opent.
- Het detailpaneel bundelt veel taken tegelijk, wat de scanlast vergroot.
- Verwijderen lijkt een one-tap destructieve actie zonder bevestiging.
- Lege en successtates lijken ondergecommuniceerd: weinig duidelijke CTA of feedback na import/PDF-acties.

### Accessibility-risico's

- Te veel status en betekenis hangt aan kleur, iconen of visuele selectie alleen.
- Meerdere iconen en klikvlakken missen waarschijnlijk duidelijke toegankelijke labels.
- Long-press op de kaart als kernactie voor POI-toevoegen is slecht discoverable en zwak toegankelijk.

## Grenzen van deze audit

- De emulator leverde geen bruikbare `uiautomator` dump op; die crashte lokaal tijdens de sessie.
- De adminflow kon visueel niet stabiel genoeg worden vastgelegd om het uiteindelijke admin-scherm met vertrouwen te beoordelen.
- De bevindingen combineren echte schermcaptures met parallelle code-audits door AI-agents.
