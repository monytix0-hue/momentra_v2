#Requires -Version 5.1
# S9-QA — Run full Android personal + group + business certification on emulator/device.
# Continues on shard failure; writes docs/qa/ANDROID_CERT_RUN_REPORT.md
param(
  [switch]$SkipPilot,
  [switch]$SkipFixtures,
  [switch]$AllowEmulator = $true,
  [string]$Device = ""
)

$ErrorActionPreference = "Continue"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Runner = Join-Path $PSScriptRoot "run-qa-android.ps1"
$ManifestPath = Join-Path $Root ".maestro\input\MANIFEST.json"
$ReportPath = Join-Path $Root "docs\qa\ANDROID_CERT_RUN_REPORT.md"
$RunStamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogDir = Join-Path $Root ".maestro\reports\full_cert_android_$RunStamp"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (-not (Test-Path $ManifestPath)) {
  Push-Location (Join-Path $Root "backend\typescript")
  $env:QA_FIXTURES_ENABLED = "true"
  npm run qa:generate-input-flows
  Pop-Location
}

$manifest = Get-Content $ManifestPath -Raw | ConvertFrom-Json
$results = [System.Collections.Generic.List[object]]::new()

function Invoke-Shard {
  param(
    [string]$Class,
    [string]$Context = "",
    [int]$Shard = 0,
    [string]$Label
  )
  $logFile = Join-Path $LogDir ("{0}.log" -f ($Label -replace '[^a-zA-Z0-9_-]', '_'))
  Write-Host "`n========== $Label ==========" -ForegroundColor Cyan
  $splat = @{
    Class = $Class
    AllowEmulator = $true
  }
  if ($Device) { $splat.Device = $Device }
  if ($Context) { $splat.Context = $Context }
  if ($Shard -gt 0) { $splat.Shard = $Shard }
  & $Runner @splat *>&1 | Tee-Object -FilePath $logFile
  $exit = $LASTEXITCODE
  $status = if ($exit -eq 0) { "PASS" } else { "FAIL" }
  $results.Add([pscustomobject]@{ When = (Get-Date).ToString("o"); Label = $Label; Class = $Class; Context = $Context; Shard = $Shard; Exit = $exit; Status = $status; Log = $logFile })
  Write-Host "==> $Label => $status (exit=$exit)" -ForegroundColor $(if ($status -eq "PASS") { "Green" } else { "Yellow" })
  return $exit
}

if (-not $SkipFixtures) {
  Write-Host "==> Preparing fixtures once" -ForegroundColor Cyan
  & $Runner -Class smoke -PrepareFixtures -AllowEmulator -SkipInstall 2>&1 | Tee-Object (Join-Path $LogDir "fixtures.log")
}

if (-not $SkipPilot) {
  Invoke-Shard -Class pilot -Label "pilot_all"
}

foreach ($ctx in @("personal", "group", "business")) {
  $shards = $manifest.android.shardFiles.$ctx
  $n = 1
  foreach ($rel in $shards) {
    if ($rel -match 'shard_(\d+)') { $n = [int]$Matches[1] }
    Invoke-Shard -Class input -Context $ctx -Shard $n -Label ("input_{0}_s{1:D2}" -f $ctx, $n)
  }
}

$pass = @($results | Where-Object { $_.Status -eq "PASS" }).Count
$fail = @($results | Where-Object { $_.Status -eq "FAIL" }).Count
$total = $results.Count

$md = @"
# Android full certification run ($RunStamp)

Device: emulator/device via -AllowEmulator  
Log directory: ``$LogDir``

## Summary

| Metric | Count |
|--------|------:|
| Total runs | $total |
| PASS | $pass |
| FAIL | $fail |

## Results

| When | Label | Context | Shard | Exit | Status |
|------|-------|---------|------:|-----:|--------|
"@

foreach ($r in $results) {
  $md += "| $($r.When) | $($r.Label) | $($r.Context) | $($r.Shard) | $($r.Exit) | $($r.Status) |`n"
}

$md += @"

## Failed runs

"@

$failed = @($results | Where-Object { $_.Status -eq "FAIL" })
if ($failed.Count -eq 0) {
  $md += "_None - all shards PASS._`n"
} else {
  foreach ($f in $failed) {
    $md += "- **$($f.Label)** - log: ``$($f.Log)```n"
  }
}

Set-Content -Path $ReportPath -Value $md -Encoding UTF8
Write-Host "`n==> Report: $ReportPath" -ForegroundColor Green
Write-Host "==> PASS=$pass FAIL=$fail TOTAL=$total"
if ($fail -gt 0) { exit 1 } else { exit 0 }
