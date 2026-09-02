#Requires -Version 5.1
<#
.SYNOPSIS
  S9-QA - Run Android Maestro suite by class.
.PARAMETER Class
  smoke|critical|isolation|cert|pilot|input|stress|all
.PARAMETER Context
  personal|group|business (for -Class input)
.PARAMETER Shard
  Shard number (1-based) for input/stress
#>
param(
  [ValidateSet("smoke", "critical", "isolation", "all", "cert", "pilot", "input", "stress")]
  [string]$Class = "smoke",
  [ValidateSet("", "personal", "group", "business")]
  [string]$Context = "",
  [int]$Shard = 0,
  [string]$Device = "",
  [switch]$AllowEmulator,
  [switch]$SkipInstall,
  [switch]$PrepareFixtures,
  [switch]$VerifyBackend,
  [switch]$GenerateFlows
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $Adb)) { $Adb = "adb" }
$PlatformTools = Split-Path $Adb -Parent
if (Test-Path $PlatformTools) {
  $env:Path = "$PlatformTools;$env:Path"
}

$Jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path "$Jbr\bin\java.exe") {
  $env:JAVA_HOME = $Jbr
  $env:Path = "$Jbr\bin;$env:USERPROFILE\.maestro\bin;$env:Path"
} else {
  $env:Path = "$env:USERPROFILE\.maestro\bin;$env:Path"
}

$Maestro = @(
  "$env:USERPROFILE\.maestro\bin\maestro.bat",
  "$env:USERPROFILE\.maestro\bin\maestro"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Maestro) { throw "Maestro CLI not found. See .maestro/README.md" }

. (Join-Path $PSScriptRoot "Load-MaestroEnv.ps1")
$EnvFile = Join-Path $Root ".maestro\.env.maestro.local"
$RunId = Get-Date -Format "yyyyMMddHHmmss"
$MaestroEnvArgs = Get-MaestroEnvCliArgs -EnvFile $EnvFile -RunId $RunId

$lines = & $Adb devices | Where-Object { $_ -match "`tdevice$" }
$serials = @($lines | ForEach-Object { ($_ -split "\s+")[0] } | Where-Object { $_ -and $_ -ne "List" })
if ($Device) { $target = $Device }
else {
  $physical = @($serials | Where-Object { $_ -notmatch "^emulator-" })
  if ($physical.Count -ge 1) { $target = $physical[0] }
  elseif ($AllowEmulator -and $serials.Count -ge 1) { $target = $serials[0] }
  else { throw "No Android device. Connect USB device or pass -AllowEmulator." }
}

Write-Host "==> S9-QA class=$Class context=$Context shard=$Shard device=$target runId=$($env:MAESTRO_RUN_ID)"

if ($Class -eq "cert") {
  $catalog = Join-Path $Root ".maestro\cert\catalog.json"
  if (-not (Test-Path $catalog)) {
    Write-Host "==> Building certification catalog (Q0)"
    Push-Location (Join-Path $Root "backend\typescript")
    $env:QA_FIXTURES_ENABLED = "true"
    npm run qa:build-catalog
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "qa:build-catalog failed" }
    npm run qa:generate-flows
    Pop-Location
  }
}

if ($GenerateFlows -or $Class -in @("pilot", "input", "stress")) {
  $manifest = Join-Path $Root ".maestro\input\MANIFEST.json"
  if ($GenerateFlows -or -not (Test-Path $manifest)) {
    Write-Host "==> Generating input flows (S9-QA-D)"
    Push-Location (Join-Path $Root "backend\typescript")
    $env:QA_FIXTURES_ENABLED = "true"
    npm run qa:sync-ledger-data
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "qa:sync-ledger-data failed" }
    npm run qa:generate-input-flows
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "qa:generate-input-flows failed" }
    Pop-Location
  }
}

if ($PrepareFixtures) {
  Write-Host "==> Preparing QA fixtures (reset+seed)"
  Push-Location (Join-Path $Root "backend\typescript")
  $env:QA_FIXTURES_ENABLED = "true"
  $env:ALLOW_DEV_AUTH = "1"
  npm run qa:prepare-fixtures
  if ($LASTEXITCODE -ne 0) { Pop-Location; throw "qa:prepare-fixtures failed" }
  Pop-Location
  $MaestroEnvArgs = Get-MaestroEnvCliArgs -EnvFile $EnvFile -RunId $RunId
}

if (-not $SkipInstall) {
  $apk = Join-Path $Root "apk\app\build\outputs\apk\debug\app-debug.apk"
  if (-not (Test-Path $apk)) {
    Push-Location (Join-Path $Root "apk")
    & .\gradlew.bat :app:assembleDebug -q
    if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed" }
    Pop-Location
  }
  & $Adb -s $target install -r $apk
}

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$reportDir = Join-Path $Root ".maestro\reports\qa_android_${Class}_$stamp"
if ($Context) { $reportDir = Join-Path $Root ".maestro\reports\qa_android_${Class}_${Context}_s${Shard}_$stamp" }
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$flowTarget = $null
$include = $null

switch ($Class) {
  "smoke" { $flowTarget = Join-Path $Root ".maestro\android"; $include = "smoke" }
  "critical" { $flowTarget = Join-Path $Root ".maestro\android"; $include = "critical" }
  "isolation" { $flowTarget = Join-Path $Root ".maestro\android"; $include = "isolation" }
  "cert" { $flowTarget = Join-Path $Root ".maestro\cert\android"; $include = "cert" }
  "pilot" {
    $flowTarget = Join-Path $Root ".maestro\input\android\pilot"
    $include = "pilot"
  }
  "input" {
    if (-not $Context) { throw "-Context personal|group|business required for -Class input" }
    $dir = Join-Path $Root ".maestro\input\android\$Context"
    if ($Shard -gt 0) {
      $n = "{0:D2}" -f $Shard
      $flowTarget = Join-Path $dir "shard_$n.yaml"
    } else {
      $flowTarget = $dir
    }
    $include = "input"
  }
  "stress" {
    $dir = Join-Path $Root ".maestro\input\android\stress"
    if ($Shard -gt 0) {
      $n = "{0:D2}" -f $Shard
      $flowTarget = Join-Path $dir "interleaved_$n.yaml"
    } else {
      $flowTarget = $dir
    }
    $include = "stress"
  }
  "all" { $flowTarget = Join-Path $Root ".maestro\android"; $include = "*" }
}

if (-not (Test-Path $flowTarget)) {
  throw "Flow path missing: $flowTarget - run with -GenerateFlows first"
}

Write-Host "==> Testing $flowTarget"
if ($Class -eq "all") {
  & $Maestro --device $target test $flowTarget @MaestroEnvArgs --format junit --output (Join-Path $reportDir "junit.xml") --debug-output $reportDir
} elseif ($Class -in @("pilot", "input", "stress") -and (Test-Path $flowTarget -PathType Leaf)) {
  & $Maestro --device $target test $flowTarget @MaestroEnvArgs --format junit --output (Join-Path $reportDir "junit.xml") --debug-output $reportDir
} elseif ($Class -in @("pilot", "input", "stress")) {
  & $Maestro --device $target test $flowTarget @MaestroEnvArgs --include-tags $include --format junit --output (Join-Path $reportDir "junit.xml") --debug-output $reportDir
} else {
  & $Maestro --device $target test $flowTarget @MaestroEnvArgs --include-tags $include --format junit --output (Join-Path $reportDir "junit.xml") --debug-output $reportDir
}

$maestroExit = $LASTEXITCODE

if ($VerifyBackend -or $Class -in @("cert", "pilot", "input", "stress")) {
  Write-Host "==> Backend verify / reports"
  Push-Location (Join-Path $Root "backend\typescript")
  $env:QA_FIXTURES_ENABLED = "true"
  $env:ALLOW_DEV_AUTH = "1"
  if ($Class -eq "cert") { npm run qa:generate-reports }
  # S9-QA-F..I performance checkpoints (safe no-op if DB unreachable)
  $wave = switch ($Class) {
    "pilot" { "E" }
    "input" {
      switch ($Context) {
        "personal" { "F" }
        "group" { "G" }
        "business" { "H" }
        default { "F" }
      }
    }
    "stress" { "I" }
    default { "X" }
  }
  $milestone = if ($Shard -gt 0) { $Shard * 50 } else { 0 }
  npm run qa:record-backend-checkpoint -- --platform android --wave $wave --milestone $milestone --run-id $RunId 2>$null
  npm run qa:generate-certification-report -- --platform android --class $Class --run-id $RunId --exit $maestroExit
  Pop-Location
}

Write-Host "==> Reports: $reportDir exit=$maestroExit"
Write-Host "==> Hard gate: pilot must PASS before input/stress scale runs (S9-QA-E)."
exit $maestroExit
