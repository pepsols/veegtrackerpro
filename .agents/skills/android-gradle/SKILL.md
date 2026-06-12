---
name: android-gradle
description: Gebruik deze skill voor Android- en Gradle-werk in deze repository, zoals buildfouten, testfouten, manifestissues, dependencyproblemen en gerichte verificatie.
---

# Android Gradle

Reageer in het Nederlands.

Gebruik deze skill wanneer een taak draait om Android Studio, Gradle, buildproblemen, tests of moduleconfiguratie in deze repo.

## Standaardaanpak

1. Bepaal eerst de kleinste relevante scope:
   - module: meestal `app/`
   - buildconfig: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
   - Android resources of manifest in `app/src/`
2. Gebruik gerichte commando's en vermijd brede, dure checks zolang de scope nog onduidelijk is.
3. Kies verificatie passend bij de wijziging:
   - kleine Kotlin/XML wijziging: gerichte Gradle task als die bekend is
   - configuratie/build fix: relevante assemble- of testtask
   - als volledige verificatie niet haalbaar is, meld dat expliciet
4. Vat failures samen als:
   - oorzaak
   - impact
   - eerstvolgende stap

## Repo-specifieke aandachtspunten

- Respecteer bestaande Gradle Kotlin DSL-configuratie.
- Ga ervan uit dat de repo mogelijk meerdere actieve werkstromen heeft; overschrijf geen werk van anderen.
- Houd wijzigingen klein en lokaal.

## Niet doen

- Geen dependency-upgrades buiten de gevraagde scope.
- Geen grote herstructurering van modules zonder expliciete vraag.
- Geen ruwe logdump in het eindantwoord.
