# Launch Maestro Studio with QA env from .env.maestro.local (avoids email "undefined").
# Usage: .\.maestro\run-maestro-studio-android.ps1 [optional path to flow yaml]
param(
  [string]$Flow = ""
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
. (Join-Path $PSScriptRoot "Load-MaestroEnv.ps1")
$EnvFile = Join-Path $Root ".maestro\.env.maestro.local"
$MaestroEnvArgs = Get-MaestroEnvCliArgs -EnvFile $EnvFile -RunId (Get-Date -Format "yyyyMMddHHmmss")

$Maestro = @(
  "$env:USERPROFILE\.maestro\bin\maestro.bat",
  "$env:USERPROFILE\.maestro\bin\maestro"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Maestro) { throw "Maestro CLI not found" }

Write-Host "==> Maestro Studio with $($MaestroEnvArgs.Count / 2) env vars from .env.maestro.local"
if ($Flow) {
  & $Maestro studio @MaestroEnvArgs $Flow
} else {
  & $Maestro studio @MaestroEnvArgs
}
