---
name: local-llm
description: Use when the user wants to run, compare, choose, or integrate local LLMs, especially local coding agents on Ollama and Open WebUI. Trigger on local coding models, on-device AI, Ollama, llama.cpp, LM Studio, Open WebUI, GGUF models, or self-hosted inference stacks. This skill helps narrow the stack, model, and deployment path based on hardware, latency, privacy, and integration constraints.
---

# Local LLM

Use this skill for practical local-LLM work: choosing a runtime, selecting a model, wiring a local API, or evaluating an on-device setup.

The default path for end-user local coding assistants is `Ollama + Open WebUI` unless the user's hardware, ops needs, or integration constraints point elsewhere.

Prefer concrete recommendations over broad surveys. Start from the user's hardware, operating system, use case, privacy constraints, and target interface.

## Quick Intake

Ask only for the constraints that materially change the answer:

- hardware: CPU only, NVIDIA, AMD, Apple Silicon, RAM, VRAM, NPU
- platform: Windows, macOS, Linux, Android, edge device, server
- workload: chat, coding, embeddings, RAG, vision, audio, agents
- interface: CLI, desktop UI, web UI, API server, SDK integration
- priorities: privacy, offline use, latency, quality, cost, simplicity

If the user did not give hardware details, state the missing assumption explicitly and make a conservative recommendation.

## Workflow

1. Classify the request.
   - stack selection: pick runtime and UI
   - model selection: pick a model family and size
   - integration: expose a local OpenAI-compatible or native API
   - troubleshooting: isolate hardware, driver, quantization, or context issues
2. Read the focused reference file in `references/` that matches the task.
   - for local coding assistants with a browser UI, read `references/ollama-open-webui-coding.md` first
3. If you need a broader catalog of current tools or model families, consult `awesome-local-llm/README.md` in the repo instead of dumping long lists from memory.
4. Return a narrow recommendation with tradeoffs, not a giant matrix.

## Recommendation Rules

- Default to the simplest viable stack.
- For local coding agents on a desktop:
  - start with `Ollama + Open WebUI`
  - only move away from that stack if the user needs lower-level control, server throughput, or a different runtime API shape
- For Windows or general desktop users who want fast setup without agent workflows:
  - start with Ollama or LM Studio
- For lightweight local APIs and broad hardware support:
  - consider `llama.cpp`
- For high-throughput server inference:
  - consider `vLLM`, `SGLang`, or TensorRT-LLM when the hardware and ops budget justify them
- For coding use cases:
  - prefer coding-tuned open models over general chat models when code quality matters
  - prefer stacks that can expose a stable local API endpoint and preserve larger context windows
- For privacy-sensitive offline use:
  - avoid cloud fallbacks unless the user explicitly allows them

## Model Selection Heuristics

- If hardware is unclear, recommend a small-to-mid model first and say why.
- Match model size to VRAM or RAM realities rather than ideal benchmark rankings.
- Prefer GGUF variants for `llama.cpp`-style local desktop inference.
- For coding:
  - start with a coding-specific family such as Qwen coder variants, Devstral, or other coding-tuned open models that fit the machine
  - if the user explicitly wants local coding agents, optimize first for edit quality, context length, and predictable local serving behavior
- For multimodal or audio:
  - verify the runtime actually supports the modality before recommending the model

Do not imply that a frontier-sized open model is practical on consumer hardware unless the memory budget is actually plausible.

## Integration Guidance

When the user wants to build against a local model:

- identify whether they need:
  - OpenAI-compatible HTTP API
  - native runtime API
  - CLI-only workflow
- recommend the shortest path to a working endpoint
- mention model download size, expected memory footprint, and whether quantized weights are needed
- separate local inference from app-layer features like RAG, tools, memory, and UI

When the user asks for local coding agents specifically:

- distinguish between:
  - code completion in an IDE
  - chat-based coding help in a browser UI
  - tool-using coding agents that edit files or call shell commands
- do not assume `Open WebUI` alone provides a full coding-agent execution environment
- recommend `Ollama` as the model server and `Open WebUI` as the human-facing chat layer unless the user needs a different agent host

## Output Format

Structure answers in this order when making recommendations:

1. recommended stack
2. recommended model or model class
3. why this fits the constraints
4. concrete next commands or setup steps
5. upgrade path if the first option is too slow or too weak

## References

- For stack and model selection patterns, read [references/selection-guide.md](references/selection-guide.md).
- For the default coding-assistant path, read [references/ollama-open-webui-coding.md](references/ollama-open-webui-coding.md).
- For a broader curated ecosystem list, inspect `awesome-local-llm/README.md`.
