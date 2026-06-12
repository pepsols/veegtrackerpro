# Project Agents

Deze map bevat project-specifieke Codex agents.

Aanbevolen gebruik:

- `explorer`: impactanalyse, bestandsscope, architectuurvragen
- `implementer`: afgebakende feature of bugfix
- `reviewer`: review op regressies, bugs en testgaten
- `tester`: Gradle- en Android-verificatie
- `github-triage`: PR-, issue-, review- en CI-context via GitHub CLI of plugin

Voorbeeldprompt:

```text
Spawn 3 agents: explorer, implementer en reviewer.
1. Explorer: bepaal welke bestanden geraakt worden.
2. Implementer: voer de fix uit in de afgesproken scope.
3. Reviewer: controleer op regressies en ontbrekende tests.
Wacht op alle resultaten en geef daarna 1 samenvatting.
```
