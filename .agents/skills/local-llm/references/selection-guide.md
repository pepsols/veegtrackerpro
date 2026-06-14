# Local LLM Selection Guide

Use this file when the request needs more than a one-line recommendation.

## Runtime Shortlist

### Ollama

Use when the user wants:

- the fastest path to running a local model
- a simple local HTTP API
- a large catalog of ready-to-pull models

Tradeoffs:

- less control than lower-level runtimes
- not the best fit for every advanced serving setup

### LM Studio

Use when the user wants:

- a desktop-first experience
- model discovery and downloads through a GUI
- a local server without deep CLI setup

Tradeoffs:

- more GUI-oriented than infrastructure-oriented

### llama.cpp

Use when the user wants:

- maximum portability
- GGUF-based inference
- a lightweight local runtime with fine-grained control

Tradeoffs:

- more manual setup than the most polished desktop tools

### Open WebUI

Use when the user wants:

- a browser UI on top of a local runtime
- multi-model chat workflows
- a friendlier interface for non-CLI users

Tradeoffs:

- it is a UI layer, not the inference engine itself

### vLLM / SGLang / TensorRT-LLM

Use when the user wants:

- high-throughput serving
- larger-scale deployments
- optimized GPU inference on stronger hardware

Tradeoffs:

- these are operationally heavier
- overkill for a single laptop workflow

## Practical Model Guidance

### General chat

- choose a small or medium instruct model first when hardware is unknown
- scale up only after the first setup works

### Coding

- prefer coding-tuned open models
- if the user asks for local coding agents, verify both the model and the serving stack support the context window and tool workflow they need

### Multimodal

- confirm image or audio support in both model and runtime
- avoid assuming desktop runtimes all expose the same modality support

## Constraint Mapping

### Consumer Windows laptop

Typical starting point:

- Ollama or LM Studio
- optional Open WebUI
- a smaller quantized instruct or coding model

### Apple Silicon

Typical starting point:

- LM Studio, Ollama, or MLX-adjacent stacks when relevant
- choose models known to run well within unified memory limits

### GPU workstation or server

Typical starting point:

- vLLM, SGLang, TensorRT-LLM, or a more configurable server runtime
- use this only when the user actually needs throughput, concurrency, or larger models

## Using The Repo Catalog

When the user asks for alternatives, categories, or current ecosystems:

1. search `awesome-local-llm/README.md` for the specific category
2. extract only the few items relevant to the user's constraints
3. avoid reproducing huge curated lists
