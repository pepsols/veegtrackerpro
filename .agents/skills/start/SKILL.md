---
name: start
description: Gebruik deze skill wanneer Codex snel projectcontext moet opbouwen voor Android/Gradle werk in deze repository, inclusief waar app-, dashboard- en toolingcode staat.
---

# Start

Reageer in het Nederlands.

Gebruik deze skill alleen voor snelle oriëntatie aan het begin van een taak of wanneer de projectscope onduidelijk is.

## Doel

Bouw snel context op zonder de hele repo te lezen.

## Werkwijze

1. Bepaal eerst welk deel van de repo relevant is:
   - `app/` voor Android app-code
   - `mobile/` voor mobiele gerelateerde onderdelen buiten de app-module
   - `dashboard/` voor dashboard/frontend
   - `tools/` voor scripts en hulpmiddelen
2. Zoek daarna gericht met `rg` naar features, schermnamen, viewmodels, routes of foutmeldingen.
3. Rapporteer alleen de bestanden en modules die waarschijnlijk relevant zijn.
4. Noem expliciet welke aannames nog onbevestigd zijn.

## Grenzen

- Voer geen brede refactor uit vanuit deze skill.
- Dump geen lange terminaloutput.
- Lees niet ongericht grote aantallen bestanden als een gerichte zoekactie voldoende is.
