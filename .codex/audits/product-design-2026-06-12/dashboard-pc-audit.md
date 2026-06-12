# Dashboard HTML PC Audit

Datum: 2026-06-12
Doel: controle van de desktopweergave van `app/src/main/assets/web/index.html` op layout, informatiehiërarchie en toegankelijkheid.

Bronnen:

- Live preview via `http://127.0.0.1:8766/index.html`
- Desktopviewport `1440x1200`
- CSS/layoutregels in `app/src/main/assets/web/index.html`

## Wat werkt goed

- De hoofdstructuur is sterk en duidelijk: vaste sidenavigatie, hero, prioriteitenstrook, kaart links, context rechts en drie operationele panelen daaronder.
- De desktopgrid is technisch gezond: geen horizontale overflow op 1440px, met stabiele kolommen voor sidebar, topbar, workspace en section-grid.
- De contextkolom naast de kaart maakt het dashboard veel rustiger dan overlay-details bovenop de kaart.
- Visuele taal is consistent: panelen, schaduwen, radius en accentkleur ondersteunen een duidelijke operationele interface.

## Belangrijkste problemen

### P1

- De hero is te hoog voor desktopgebruik. Er gaat veel verticale ruimte op aan introcopy en filter/actions, waardoor de kaart te laat in beeld komt.
- De prioriteitenstrook gebruikt vier even zware KPI-kaarten; daardoor is `Nu actie` niet echt dominanter dan de rest.
- De kaartheader heeft veel chips voordat de kaart begint. Dat kost extra hoogte in het belangrijkste werkvlak.
- De contextkolom is inhoudelijk goed, maar visueel nog te kaartgericht op één selectie. Voor pc mag hier meer compacte samenvatting en minder grote previewkaart.

### P2

- De sidebar links voelt breed voor de hoeveelheid navigatie-inhoud; 280px is verdedigbaar, maar visueel iets te royaal ten opzichte van de kaartbreedte.
- De topbar-side stapelt filters en acties onder elkaar; op desktop voelt dat eerder als formulierenkolom dan als snelle command-zone.
- De onderste drie secties zijn netjes, maar kaarten in `Operatie vandaag` en `Planning en taken` ogen nog behoorlijk lang, waardoor scanbaarheid op drukke dagen kan afnemen.

### Accessibility

- Navigatieknoppen en actieknoppen leunen zichtbaar op Font Awesome-iconen; die moeten semantisch goed verborgen of correct gelabeld blijven.
- De prioriteitenkaarten lijken vooral kleur- en positiebased. Urgentie moet niet alleen uit toonverschil blijken.
- De kaart zelf bevat veel kleine interactieve markers; zonder alternatieve lijst- of filterinteractie is dat voor precisie en keyboardgebruik zwak.

## Aanbevolen eerstvolgende desktopverbeteringen

1. Maak de hero compacter: minder introhoogte, filters en acties dichter op elkaar.
2. Geef `Nu actie` een duidelijker visueel gewicht dan de andere KPI-kaarten.
3. Trek de kaart hoger de viewport in door de header erboven in te korten.
4. Verdicht het contextpaneel: compactere summary, acties direct zichtbaar, minder decoratieve ruimte.
5. Maak de operationele lijsten compacter zodat onder de kaart meer items tegelijk zichtbaar zijn.
