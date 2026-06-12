# Codex + Gemini

Deze repo heeft nu simpele wrappers om Codex direct, en Codex + Gemini samen te gebruiken vanaf dezelfde workspace.

Bestanden:

- `tools/codex.ps1`
- `tools/codex-gemini.ps1`

`tools/codex.ps1` doet alleen dit:

- start `codex` altijd met deze repo als workspace
- geeft alle extra argumenten 1-op-1 door aan de Codex CLI

Voorbeelden:

```powershell
.\tools\codex.ps1
```

```powershell
.\tools\codex.ps1 exec "Bekijk deze repo en vat de architectuur samen"
```

Wat het script doet:

- `review`: Codex beantwoordt de taak eerst, Gemini reviewt daarna het Codex-resultaat.
- `compare`: Codex en Gemini geven elk hun eigen antwoord op dezelfde prompt.

Voorwaarden:

- `codex` moet in je `PATH` staan en ingelogd zijn.
- `gemini` moet in je `PATH` staan en ingelogd zijn.

Voorbeelden:

```powershell
.\tools\codex-gemini.ps1 -Mode review -Prompt "Fix the login redirect bug in dashboard/"
```

```powershell
.\tools\codex-gemini.ps1 -Mode compare -Prompt "Design a safer Firestore write flow for route updates"
```

Output:

- Resultaten worden opgeslagen in `.ai/pairing/`
- Per run krijg je een `codex-*.txt` en `gemini-*.txt`

Optionele modellen:

```powershell
.\tools\codex-gemini.ps1 `
  -Mode review `
  -CodexModel "gpt-5" `
  -GeminiModel "gemini-2.5-pro" `
  -Prompt "Review the dashboard auth flow and propose minimal changes"
```
