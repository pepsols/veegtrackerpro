param(
    [string]$GradleTask = "build",
    [string[]]$GradleArgs = @(),
    [string]$BaseUrl = "http://192.168.178.199:11434/v1",
    [string]$Model = "meta-llama-3.1-8b-instruct",
    [double]$Temperature = 0.1,
    [int]$MaxLogChars = 24000
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"
$llamaDelegate = Join-Path $PSScriptRoot "llama-delegate.ps1"

if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Gradle wrapper niet gevonden: $gradleWrapper"
}

if (-not (Test-Path -LiteralPath $llamaDelegate)) {
    throw "Llama helper niet gevonden: $llamaDelegate"
}

$startedAt = Get-Date
$gradleCommand = @($GradleTask) + $GradleArgs

Push-Location $repoRoot
try {
    $outputLines = & $gradleWrapper @gradleCommand 2>&1 | ForEach-Object { $_.ToString() }
    $gradleExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

$finishedAt = Get-Date
$log = ($outputLines -join [Environment]::NewLine).Trim()
if ($log.Length -gt $MaxLogChars) {
    $log = $log.Substring($log.Length - $MaxLogChars)
    $log = "[Log ingekort tot laatste $MaxLogChars tekens]" + [Environment]::NewLine + $log
}

$system = @"
Je bent een Android/Gradle build-agent voor deze repository.
Antwoord in het Nederlands.
Geef compacte analyse met:
- resultaat
- oorzaak bij failure of waarschuwingen
- impact
- eerstvolgende stap
Dump geen lange logs en verzin geen bestanden die niet in de output staan.
"@

$prompt = @"
Android/Gradle builddelegatie

Commando:
.\gradlew.bat $($gradleCommand -join " ")

Start:
$($startedAt.ToString("s"))

Einde:
$($finishedAt.ToString("s"))

Exitcode:
$gradleExitCode

Buildoutput:
~~~text
$log
~~~
"@

"Gradle exitcode: $gradleExitCode"
"Llama analyse:"
$prompt | & $llamaDelegate -BaseUrl $BaseUrl -Model $Model -System $system -Temperature $Temperature

exit $gradleExitCode
