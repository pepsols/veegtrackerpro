param(
    [string]$BaseUrl = "http://192.168.178.199:11434/v1",
    [string]$Model = "meta-llama-3.1-8b-instruct",
    [string]$System = "Je bent een compacte technische assistent. Antwoord in het Nederlands, concreet en zonder onnodige uitleg.",
    [double]$Temperature = 0.2,

    [Parameter(ValueFromPipeline = $true)]
    [string[]]$InputText,

    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Prompt
)

$ErrorActionPreference = "Stop"

$task = ($Prompt -join " ").Trim()
if ([string]::IsNullOrWhiteSpace($task)) {
    if ($null -ne $InputText) {
        $task = ($InputText -join [Environment]::NewLine).Trim()
    }
}

if ([string]::IsNullOrWhiteSpace($task)) {
    throw "Geef een taak mee, bijvoorbeeld: .\tools\llama-delegate.ps1 `"Vat app/build.gradle.kts samen`""
}

$uri = "$($BaseUrl.TrimEnd('/'))/chat/completions"
$body = @{
    model = $Model
    temperature = $Temperature
    messages = @(
        @{
            role = "system"
            content = $System
        },
        @{
            role = "user"
            content = $task
        }
    )
} | ConvertTo-Json -Depth 8

$response = Invoke-RestMethod -Uri $uri -Method Post -ContentType "application/json" -Body $body -TimeoutSec 120
$response.choices[0].message.content
