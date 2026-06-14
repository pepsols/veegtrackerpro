# Ollama + Open WebUI For Local Coding

Read this file first when the user wants a practical local coding-assistant setup.

## What This Stack Is

- `Ollama`: local model runtime and HTTP API
- `Open WebUI`: browser UI layered on top of the local runtime

This is a good default when the user wants local coding help with low setup friction.

## What It Is Good At

- local code chat
- repository Q and A
- basic prompt-driven code generation
- privacy-sensitive developer workflows on one machine

## What It Is Not

- not a full replacement for a coding agent runner that can safely edit files and execute tools by itself
- not the best fit for high-concurrency serving
- not the most controllable stack for low-level quantization tuning

State this explicitly when the user says "coding agent" but really describes a chat UI.

## Default Recommendation Pattern

1. serve the model with `Ollama`
2. attach `Open WebUI` as the interactive UI
3. choose a coding-tuned model that fits the user's RAM or VRAM budget
4. upgrade the model size only after the basic path is fast enough and stable

## Model Guidance

Prioritize coding-tuned open models that are practical on local hardware.

Good default categories:

- smaller coding models for CPU-heavy or limited-memory setups
- medium coding models for stronger desktops
- larger coding models only when the user clearly has the memory budget

When naming candidates, prefer a few concrete options rather than a long leaderboard.

## Questions That Matter

Ask these before recommending a model:

- operating system
- RAM and VRAM
- CPU only or GPU acceleration
- desired context length
- whether the user needs chat only or agent-like tool use

## Recommendation Boundaries

- if the user needs browser chat with local privacy, keep the answer centered on `Ollama + Open WebUI`
- if the user needs file editing, shell tools, or autonomous coding loops, say they likely need a separate agent host on top of the local model server
- if the hardware is weak, favor responsiveness over benchmark prestige

## Example Answer Shape

1. recommended stack: `Ollama + Open WebUI`
2. recommended model: a coding-tuned model sized for the machine
3. why: easy setup, private local serving, stable API, browser UI
4. next steps: install Ollama, pull the model, run Open WebUI, test on a small repo
5. upgrade path: move to a larger coding model or a stronger serving stack if latency and memory allow
