param(
    [string]$Workspace = (Get-Location).Path,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$ErrorActionPreference = "Stop"

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command not found: $Name"
    }
}

Assert-Command "codex"

$resolvedWorkspace = (Resolve-Path -LiteralPath $Workspace).Path
$codexArgs = @("-C", $resolvedWorkspace) + $Arguments

& codex @codexArgs
exit $LASTEXITCODE
