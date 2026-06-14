param(
    [string]$HtmlPath = "app/src/main/assets/web/index.html",
    [string]$BaseUrl = "http://192.168.178.199:11434/v1",
    [string]$Model = "meta-llama-3.1-8b-instruct",
    [double]$Temperature = 0.15,
    [int]$MaxHtmlChars = 32000,

    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Task
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$resolvedHtmlPath = if ([System.IO.Path]::IsPathRooted($HtmlPath)) {
    (Resolve-Path -LiteralPath $HtmlPath).Path
} else {
    (Resolve-Path -LiteralPath (Join-Path $repoRoot $HtmlPath)).Path
}

$llamaDelegate = Join-Path $PSScriptRoot "llama-delegate.ps1"
if (-not (Test-Path -LiteralPath $llamaDelegate)) {
    throw "Llama helper niet gevonden: $llamaDelegate"
}

$taskText = ($Task -join " ").Trim()
if ([string]::IsNullOrWhiteSpace($taskText)) {
    $taskText = "Review deze HTML op concrete UI-, toegankelijkheids-, performance- en onderhoudsrisico's. Geef prioriteiten en eerstvolgende stappen."
}

$html = Get-Content -LiteralPath $resolvedHtmlPath -Raw
if ($html.Length -gt $MaxHtmlChars) {
    $headSize = [Math]::Floor($MaxHtmlChars * 0.65)
    $tailSize = $MaxHtmlChars - $headSize
    $html = $html.Substring(0, $headSize) +
        [Environment]::NewLine +
        "[HTML ingekort: midden weggelaten]" +
        [Environment]::NewLine +
        $html.Substring($html.Length - $tailSize)
}

$system = @"
Je bent een HTML/dashboard review-agent voor deze repository.
Antwoord in het Nederlands.
Focus op concrete HTML, CSS en browsergedrag-risico's.
Geef waar mogelijk selectors, elementnamen of herkenbare tekstfragmenten.
Als de taak om implementatie vraagt, geef dan een compacte patchstrategie; wijzig zelf geen bestanden.
Dump geen lange HTML-blokken.
"@

$prompt = @"
HTML-delegatie

Bestand:
$resolvedHtmlPath

Taak:
$taskText

HTML-context:
~~~html
$html
~~~
"@

$prompt | & $llamaDelegate -BaseUrl $BaseUrl -Model $Model -System $system -Temperature $Temperature
