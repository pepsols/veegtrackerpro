---
name: github-cli
description: Gebruik deze skill voor GitHub CLI werk met `gh`, zoals PR's bekijken, issues triëren, reviews lezen, checks inspecteren en releases of branches controleren.
---

# GitHub CLI

Reageer in het Nederlands.

Gebruik deze skill wanneer de taak draait om GitHub repository-context en `gh` daarvoor de snelste route is.

## Vereisten

- `gh` moet lokaal geïnstalleerd en beschikbaar zijn op het pad.
- Als `gh` niet beschikbaar is, gebruik dan de GitHub-plugin of werk met lokale git- en repo-context.

## Standaardaanpak

1. Controleer eerst of `gh` beschikbaar is.
2. Kies daarna het kleinste commando dat de benodigde context geeft.
3. Vat de uitkomst samen in plaats van ruwe output te dumpen.
4. Koppel GitHub-context terug naar concrete code-impact in deze repo.

## Typische commando's

```powershell
gh repo view
gh pr status
gh pr view <nummer> --comments
gh issue view <nummer>
gh run list
gh run view <run-id> --log
```

## Grenzen

- Gebruik geen destructieve git- of GitHub-acties zonder expliciete vraag.
- Maak geen release, merge of labelwijziging alleen op basis van aannames.
- Dump geen lange logs in het eindantwoord; vat samen op oorzaak en impact.

