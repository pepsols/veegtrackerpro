param(
    [Parameter(Mandatory = $true)]
    [string]$Task,

    [string]$Model,

    [ValidateSet("api", "cli")]
    [string]$Transport = "api",

    [ValidateSet("text", "json", "stream-json")]
    [string]$OutputFormat = "text",

    [string[]]$IncludeDirectories,

    [switch]$SkipTrust
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Test-GeminiAuthConfigured {
    if ($env:GEMINI_API_KEY -or $env:GOOGLE_GENAI_USE_VERTEXAI -or $env:GOOGLE_GENAI_USE_GCA) {
        return $true
    }

    $settingsPath = Join-Path $HOME ".gemini\settings.json"
    return Test-Path -LiteralPath $settingsPath
}

if (-not (Test-GeminiAuthConfigured)) {
    Write-Error "Gemini auth is not configured. Set GEMINI_API_KEY or configure $HOME\.gemini\settings.json."
}

if (-not $Model) {
    $Model = "gemini-2.5-flash"
}

if ($Transport -eq "api") {
    if (-not $env:GEMINI_API_KEY) {
        Write-Error "API transport requires GEMINI_API_KEY in the environment."
    }

    if ($OutputFormat -eq "stream-json") {
        Write-Error "API transport does not support stream-json output."
    }

    $uri = "https://generativelanguage.googleapis.com/v1beta/models/$Model`:generateContent?key=$($env:GEMINI_API_KEY)"
    $payload = @{
        contents = @(
            @{
                parts = @(
                    @{
                        text = $Task
                    }
                )
            }
        )
    } | ConvertTo-Json -Depth 6

    $response = Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $payload

    if ($OutputFormat -eq "json") {
        $response | ConvertTo-Json -Depth 20
        exit 0
    }

    $parts = @($response.candidates[0].content.parts | ForEach-Object { $_.text }) | Where-Object { $_ }
    $text = ($parts -join [Environment]::NewLine).Trim()
    if (-not $text) {
        Write-Error "Gemini API returned no text output."
    }

    Write-Output $text
    exit 0
}

$arguments = @("-p", $Task, "--output-format", $OutputFormat)

$arguments += @("--model", $Model)

if ($SkipTrust) {
    $arguments += "--skip-trust"
}

foreach ($directory in ($IncludeDirectories | Where-Object { $_ })) {
    $arguments += @("--include-directories", $directory)
}

& gemini @arguments
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) {
    exit $exitCode
}
