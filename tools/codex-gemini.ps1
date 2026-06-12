param(
    [Parameter(Mandatory = $true)]
    [string]$Prompt,

    [ValidateSet("review", "compare")]
    [string]$Mode = "review",

    [string]$Workspace = (Get-Location).Path,
    [string]$OutputDir = ".ai\pairing",
    [string]$CodexModel = "",
    [string]$GeminiModel = ""
)

$ErrorActionPreference = "Stop"

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command not found: $Name"
    }
}

Assert-Command "codex"
Assert-Command "gemini"

$resolvedWorkspace = (Resolve-Path -LiteralPath $Workspace).Path
$resolvedOutputDir = Join-Path $resolvedWorkspace $OutputDir

New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$codexFile = Join-Path $resolvedOutputDir "codex-$timestamp.txt"
$geminiFile = Join-Path $resolvedOutputDir "gemini-$timestamp.txt"

$codexArgs = @(
    "exec",
    "--skip-git-repo-check",
    "-C", $resolvedWorkspace,
    "--output-last-message", $codexFile
)

if ($CodexModel) {
    $codexArgs += @("-m", $CodexModel)
}

$codexArgs += $Prompt

Write-Host "Running Codex..."
& codex @codexArgs

if ($LASTEXITCODE -ne 0) {
    throw "Codex failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path $codexFile)) {
    throw "Codex did not produce an output file: $codexFile"
}

$codexResponse = Get-Content -Raw -LiteralPath $codexFile

$geminiPrompt = switch ($Mode) {
    "review" {
@"
You are reviewing a Codex-generated coding answer for the workspace at:
$resolvedWorkspace

Original task:
$Prompt

Codex answer:
$codexResponse

Review the Codex answer. Be concrete.
- Call out mistakes, risks, and missing validation.
- If the answer is good, say what should be executed next.
- Keep the response concise and implementation-focused.
"@
    }
    "compare" {
@"
Answer this coding task independently for the workspace at:
$resolvedWorkspace

Task:
$Prompt

Do not review another answer. Provide your own concise implementation-focused answer.
"@
    }
}

$geminiArgs = @(
    "--prompt", $geminiPrompt,
    "--output-format", "text",
    "--skip-trust",
    "--approval-mode", "plan"
)

if ($GeminiModel) {
    $geminiArgs += @("--model", $GeminiModel)
}

Write-Host "Running Gemini..."
$geminiResponse = & gemini @geminiArgs 2>&1

if ($LASTEXITCODE -ne 0) {
    $geminiError = ($geminiResponse | Out-String).Trim()
    if ($geminiError) {
        throw "Gemini failed: $geminiError"
    }

    throw "Gemini failed with exit code $LASTEXITCODE"
}

$geminiResponse | Set-Content -LiteralPath $geminiFile

Write-Host ""
Write-Host "Saved outputs:"
Write-Host "  Codex : $codexFile"
Write-Host "  Gemini: $geminiFile"
