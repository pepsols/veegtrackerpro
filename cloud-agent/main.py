import json
import os
from functools import lru_cache
from urllib import error, parse, request as urlrequest

from flask import Flask, jsonify, request
import firebase_admin
from firebase_admin import credentials, firestore

app = Flask(__name__)

# Initialize Firebase Admin SDK
# On Cloud Run, it uses the default service account automatically
if not firebase_admin._apps:
    firebase_admin.initialize_app()

db = firestore.client()

SUPPORTED_PROVIDERS = {"openai", "local_openai", "codex"}
DEFAULT_PROVIDER_CHAIN = ("local_openai", "codex")


class LlmConfigError(RuntimeError):
    pass


class LlmRequestError(RuntimeError):
    def __init__(self, message, status_code=400, code="llm_request_error", retryable=False, provider=None):
        super().__init__(message)
        self.status_code = status_code
        self.code = code
        self.retryable = retryable
        self.provider = provider


def _json_response_from_request():
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        raise LlmRequestError("Body must be valid JSON.", status_code=400, code="invalid_json")
    return data


def _resolve_provider(payload):
    provider = str(payload.get("provider") or os.environ.get("LLM_PROVIDER", DEFAULT_PROVIDER_CHAIN[0])).strip().lower()
    if provider not in SUPPORTED_PROVIDERS:
        supported = ", ".join(sorted(SUPPORTED_PROVIDERS))
        raise LlmRequestError(
            f"Unsupported provider '{provider}'. Supported providers: {supported}.",
            status_code=400,
            code="unsupported_provider",
            provider=provider,
        )
    return provider


def _resolve_model(provider, payload):
    explicit_model = str(payload.get("model") or "").strip()
    if explicit_model:
        return explicit_model

    if provider == "openai":
        return os.environ.get("OPENAI_MODEL", "gpt-5.4-mini")

    if provider == "local_openai":
        configured_model = os.environ.get("LOCAL_OPENAI_MODEL", "").strip()
        return configured_model or _infer_local_openai_model()

    if provider == "codex":
        return os.environ.get("CODEX_MODEL", "gpt-5.3-codex")

    raise LlmRequestError(f"No default model configured for provider '{provider}'.")


def _read_temperature(payload):
    if "temperature" not in payload or payload["temperature"] is None:
        return None

    try:
        return float(payload["temperature"])
    except (TypeError, ValueError) as exc:
        raise LlmRequestError("temperature must be a number.", status_code=400, code="invalid_temperature") from exc


def _http_json_request(url, method="POST", payload=None, headers=None):
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    req = urlrequest.Request(url, data=body, method=method)
    req.add_header("Content-Type", "application/json")

    for key, value in (headers or {}).items():
        req.add_header(key, value)

    try:
        with urlrequest.urlopen(req, timeout=60) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except error.HTTPError as exc:
        raw_error = exc.read().decode("utf-8", errors="replace")
        message = raw_error
        try:
            parsed_error = json.loads(raw_error)
            message = parsed_error.get("error", {}).get("message") or parsed_error.get("message") or raw_error
        except json.JSONDecodeError:
            pass
        status_code = 503 if exc.code in {408, 409, 425, 429, 500, 502, 503} else 504 if exc.code == 504 else 400
        retryable = status_code in {503, 504}
        error_code = "provider_unavailable" if status_code == 503 else "provider_timeout" if status_code == 504 else "provider_http_error"
        raise LlmRequestError(message, status_code=status_code, code=error_code, retryable=retryable) from exc
    except error.URLError as exc:
        raise LlmRequestError(
            f"LLM connection failed: {exc.reason}",
            status_code=504,
            code="provider_timeout",
            retryable=True,
        ) from exc


@lru_cache(maxsize=8)
def _list_local_openai_models(base_url):
    response = _http_json_request(f"{base_url}/models", method="GET")
    return response.get("data") or []


def _infer_local_openai_model():
    base_url = os.environ.get("LOCAL_OPENAI_BASE_URL", "http://172.28.48.1:1234/v1").strip().rstrip("/")
    if not base_url:
        raise LlmConfigError("LOCAL_OPENAI_BASE_URL is not set.")

    models = _list_local_openai_models(base_url)
    if not models:
        raise LlmConfigError("No models were returned by LOCAL_OPENAI_BASE_URL /models.")

    for item in models:
        model_id = str(item.get("id") or "").strip()
        if model_id and "embedding" not in model_id.lower():
            return model_id

    fallback_model = str(models[0].get("id") or "").strip()
    if fallback_model:
        return fallback_model

    raise LlmConfigError("Could not infer a usable LOCAL_OPENAI_MODEL from /models.")


def _generate_openai(prompt, system_prompt, model, temperature):
    api_key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise LlmConfigError("OPENAI_API_KEY is not set.")

    content = []
    if system_prompt:
        content.append({"role": "system", "content": system_prompt})
    content.append({"role": "user", "content": prompt})

    payload = {
        "model": model,
        "input": content,
    }
    if temperature is not None:
        payload["temperature"] = temperature

    response = _http_json_request(
        "https://api.openai.com/v1/responses",
        payload=payload,
        headers={"Authorization": f"Bearer {api_key}"},
    )

    output_text = str(response.get("output_text") or "").strip()
    if output_text:
        return output_text

    output_items = response.get("output") or []
    collected = []
    for item in output_items:
        for part in item.get("content") or []:
            if part.get("type") == "output_text" and part.get("text"):
                collected.append(part["text"])

    final_text = "\n".join(collected).strip()
    if not final_text:
        raise LlmRequestError("OpenAI response did not contain text output.", status_code=502, code="empty_provider_response", provider="openai")
    return final_text


def _generate_codex(prompt, system_prompt, model, temperature):
    api_key = os.environ.get("CODEX_API_KEY", os.environ.get("OPENAI_API_KEY", "")).strip()
    if not api_key:
        raise LlmConfigError("CODEX_API_KEY or OPENAI_API_KEY is not set.")

    content = []
    if system_prompt:
        content.append({"role": "system", "content": system_prompt})
    content.append({"role": "user", "content": prompt})

    payload = {
        "model": model,
        "input": content,
    }
    if temperature is not None:
        payload["temperature"] = temperature

    response = _http_json_request(
        "https://api.openai.com/v1/responses",
        payload=payload,
        headers={"Authorization": f"Bearer {api_key}"},
    )

    output_text = str(response.get("output_text") or "").strip()
    if output_text:
        return output_text

    output_items = response.get("output") or []
    collected = []
    for item in output_items:
        for part in item.get("content") or []:
            if part.get("type") == "output_text" and part.get("text"):
                collected.append(part["text"])

    final_text = "\n".join(collected).strip()
    if not final_text:
        raise LlmRequestError(
            "Codex response did not contain text output.",
            status_code=502,
            code="empty_provider_response",
            provider="codex",
        )
    return final_text


def _generate_local_openai(prompt, system_prompt, model, temperature):
    base_url = os.environ.get("LOCAL_OPENAI_BASE_URL", "http://172.28.48.1:1234/v1").strip().rstrip("/")
    if not base_url:
        raise LlmConfigError("LOCAL_OPENAI_BASE_URL is not set.")

    api_key = os.environ.get("LOCAL_OPENAI_API_KEY", "").strip()

    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": prompt})

    payload = {
        "model": model,
        "messages": messages,
    }
    if temperature is not None:
        payload["temperature"] = temperature

    headers = {}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"

    response = _http_json_request(
        f"{base_url}/chat/completions",
        payload=payload,
        headers=headers,
    )

    choices = response.get("choices") or []
    texts = []
    for choice in choices:
        message = choice.get("message") or {}
        content = message.get("content")
        if isinstance(content, str) and content.strip():
            texts.append(content)
        elif isinstance(content, list):
            for part in content:
                if isinstance(part, dict) and part.get("type") == "text" and part.get("text"):
                    texts.append(part["text"])

    final_text = "\n".join(texts).strip()
    if not final_text:
        raise LlmRequestError(
            "Local OpenAI-compatible response did not contain text output.",
            status_code=502,
            code="empty_provider_response",
            provider="local_openai",
        )
    return final_text


def generate_text(provider, prompt, system_prompt=None, model=None, temperature=None):
    resolved_model = model or _resolve_model(provider, {})

    if provider == "openai":
        output_text = _generate_openai(prompt, system_prompt, resolved_model, temperature)
    elif provider == "local_openai":
        output_text = _generate_local_openai(prompt, system_prompt, resolved_model, temperature)
    elif provider == "codex":
        output_text = _generate_codex(prompt, system_prompt, resolved_model, temperature)
    else:
        raise LlmRequestError(f"Unsupported provider '{provider}'.")

    return {
        "provider": provider,
        "model": resolved_model,
        "output": output_text,
    }


def generate_text_with_fallback(prompt, system_prompt=None, model=None, temperature=None):
    last_error = None

    for provider in DEFAULT_PROVIDER_CHAIN:
        try:
            provider_model = model if provider == DEFAULT_PROVIDER_CHAIN[0] else None
            return generate_text(
                provider=provider,
                prompt=prompt,
                system_prompt=system_prompt,
                model=provider_model,
                temperature=temperature,
            )
        except (LlmConfigError, LlmRequestError) as exc:
            last_error = exc

    if last_error:
        raise last_error

    raise LlmRequestError("No LLM providers are available.", status_code=503, code="no_provider_available", retryable=True)


def probe_provider(provider, prompt, system_prompt=None, temperature=0):
    try:
        result = generate_text(
            provider=provider,
            prompt=prompt,
            system_prompt=system_prompt,
            temperature=temperature,
        )
        return {
            "provider": provider,
            "ok": True,
            "model": result["model"],
            "output_preview": result["output"][:200],
        }
    except LlmConfigError as exc:
        return {
            "provider": provider,
            "ok": False,
            "error_type": "config",
            "error": str(exc),
        }
    except LlmRequestError as exc:
        return {
            "provider": provider,
            "ok": False,
            "error_type": "request",
            "error": str(exc),
            "code": exc.code,
            "retryable": exc.retryable,
        }

@app.route('/')
def health():
    return "Veegtracker AI Agent is Online", 200

@app.route('/create_task', methods=['POST'])
def create_task():
    """AI or Admin can call this to inject a task into the cloud"""
    try:
        data = request.json
        task_name = data.get('name', 'Nieuwe Taak')

        task_ref = db.collection('tasks').document()
        task_ref.set({
            'name': task_name,
            'status': 'open',
            'createdAt': firestore.SERVER_TIMESTAMP,
            'source': 'ai_agent'
        })

        return jsonify({"status": "success", "id": task_ref.id}), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/status', methods=['GET'])
def get_status():
    """Get a summary of active operations for the AI"""
    try:
        routes = db.collection('routes').limit(10).get()
        pois = db.collection('pois').limit(10).get()

        summary = {
            "active_routes": len(routes),
            "recent_pois": len(pois),
            "system": "operational"
        }
        return jsonify(summary), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/llm/providers', methods=['GET'])
@app.route('/v1/llm/providers', methods=['GET'])
def get_llm_providers():
    return jsonify(
        {
            "default_provider": os.environ.get("LLM_PROVIDER", DEFAULT_PROVIDER_CHAIN[0]),
            "fallback_order": list(DEFAULT_PROVIDER_CHAIN),
            "providers": [
                {
                    "id": "local_openai",
                    "base_url": os.environ.get("LOCAL_OPENAI_BASE_URL", "http://172.28.48.1:1234/v1"),
                    "default_model": os.environ.get("LOCAL_OPENAI_MODEL", "").strip() or None,
                    "configured": bool(os.environ.get("LOCAL_OPENAI_BASE_URL", "http://172.28.48.1:1234/v1").strip()),
                },
                {
                    "id": "codex",
                    "default_model": os.environ.get("CODEX_MODEL", "gpt-5.3-codex"),
                    "configured": bool(os.environ.get("CODEX_API_KEY") or os.environ.get("OPENAI_API_KEY")),
                },
                {
                    "id": "openai",
                    "default_model": os.environ.get("OPENAI_MODEL", "gpt-5.4-mini"),
                    "configured": bool(os.environ.get("OPENAI_API_KEY")),
                },
            ],
        }
    ), 200


@app.route('/llm/generate', methods=['POST'])
@app.route('/v1/llm/generate', methods=['POST'])
def llm_generate():
    try:
        data = _json_response_from_request()
        prompt = str(data.get("prompt") or "").strip()
        if not prompt:
            raise LlmRequestError("prompt is required.", status_code=400, code="missing_prompt")

        provider = _resolve_provider(data) if str(data.get("provider") or "").strip() else None
        system_prompt = str(data.get("system") or "").strip() or None
        temperature = _read_temperature(data)

        if provider:
            model = _resolve_model(provider, data)
            result = generate_text(
                provider=provider,
                prompt=prompt,
                system_prompt=system_prompt,
                model=model,
                temperature=temperature,
            )
        else:
            result = generate_text_with_fallback(
                prompt=prompt,
                system_prompt=system_prompt,
                model=str(data.get("model") or "").strip() or None,
                temperature=temperature,
            )
        return jsonify({"status": "success", **result}), 200
    except LlmConfigError as exc:
        return jsonify({"status": "error", "error": {"code": "provider_not_configured", "message": str(exc), "retryable": False}}), 500
    except LlmRequestError as exc:
        return jsonify(
            {
                "status": "error",
                "error": {
                    "code": exc.code,
                    "message": str(exc),
                    "retryable": exc.retryable,
                    "provider": exc.provider,
                },
            }
        ), exc.status_code
    except Exception as exc:
        return jsonify({"status": "error", "error": str(exc)}), 500


@app.route('/llm/probe', methods=['GET', 'POST'])
@app.route('/v1/llm/probe', methods=['GET', 'POST'])
def llm_probe():
    try:
        data = request.get_json(silent=True) or {}
        prompt = str(data.get("prompt") or request.args.get("prompt") or "Beantwoord met exact: OK").strip()
        system_prompt = str(data.get("system") or request.args.get("system") or "Antwoord kort in platte tekst.").strip() or None

        results = [
            probe_provider("local_openai", prompt, system_prompt=system_prompt, temperature=0),
            probe_provider("codex", prompt, system_prompt=system_prompt, temperature=0),
        ]

        overall_ok = any(item["ok"] for item in results)
        status_code = 200 if overall_ok else 503

        return jsonify(
            {
                "status": "success" if overall_ok else "error",
                "probe_order": ["local_openai", "codex"],
                "results": results,
            }
        ), status_code
    except Exception as exc:
        return jsonify({"status": "error", "error": str(exc)}), 500

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=int(os.environ.get("PORT", 8080)))
