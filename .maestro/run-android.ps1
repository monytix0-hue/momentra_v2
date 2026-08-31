#Requires -Version 5.1
<#
.SYNOPSIS
  Run Momentra Maestro flows on a connected Android device (physical preferred).

.EXAMPLE
  .\.maestro\run-android.ps1
  .\.maestro\run-android.ps1 -Flow .maestro\flows-android\01_onboarding_to_login.yaml
  .\.maestro\run-android.ps1 -AllowEmulator -Flow .maestro\flows-android\01_onboarding_to_login.yaml
#>
param(
  [string]$Flow = "",
  [string]$Device = "",
  [switch]$AllowEmulator,
  [switch]$SkipInstall,
  [switch]$SmokeOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $Root ".maestro"))) {
  $Root = $PSScriptRoot
  if ((Split-Path -Leaf $Root) -eq ".maestro") {
    $Root = Split-Path -Parent $Root
  }
}

$Adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $Adb)) {
  $Adb = "adb"
}

$Jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path "$Jbr\bin\java.exe") {
  $env:JAVA_HOME = $Jbr
  $env:Path = "$Jbr\bin;$env:Path"
}

$MaestroCandidates = @(
  "$env:USERPROFILE\.maestro\bin\maestro.bat",
  "$env:USERPROFILE\.maestro\bin\maestro.cmd",
  "$env:USERPROFILE\.maestro\bin\maestro"
)
$Maestro = $MaestroCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Maestro) {
  Write-Host "==> Installing Maestro CLI..."
  $install = Invoke-WebRequest -Uri "https://get.maestro.mobile.dev" -UseBasicParsing
  # Official installer is bash; on Windows use scoop or zip. Try npm-less curl bash via Git.
  if (Get-Command bash -ErrorAction SilentlyContinue) {
    bash -c "curl -Ls 'https://get.maestro.mobile.dev' | bash"
  } else {
    throw "Maestro not found. Install: https://docs.maestro.dev/getting-started/installing-maestro (or Git Bash: curl -Ls https://get.maestro.mobile.dev | bash)"
  }
  $Maestro = $MaestroCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
  if (-not $Maestro) { throw "Maestro install completed but binary not found under ~/.maestro/bin" }
}

. (Join-Path $PSScriptRoot "Load-MaestroEnv.ps1")
$EnvFile = Join-Path $Root ".maestro\.env.maestro.local"
$RunId = Get-Date -Format "yyyyMMddHHmmss"
$MaestroEnvArgs = Get-MaestroEnvCliArgs -EnvFile $EnvFile -RunId $RunId
if (Test-Path $EnvFile) {
  Write-Host "==> Loaded .env.maestro.local (+ Maestro -e flags)"
} else {
  Write-Host "==> WARNING: .maestro/.env.maestro.local missing — auth flows need MAESTRO_EMAIL / MAESTRO_PASSWORD"
}

Write-Host "==> Maestro: $(& $Maestro --version 2>&1 | Select-Object -Last 1)"
Write-Host "==> adb devices:"
& $Adb devices -l

$lines = & $Adb devices | Where-Object { $_ -match "`tdevice$" }
$serials = @()
foreach ($line in $lines) {
  $serial = ($line -split "\s+")[0]
  if ($serial -and $serial -ne "List") { $serials += $serial }
}

if ($Device) {
  $target = $Device
} else {
  $physical = $serials | Where-Object { $_ -notmatch "^emulator-" }
  if ($physical.Count -ge 1) {
    $target = $physical[0]
  } elseif ($AllowEmulator -and $serials.Count -ge 1) {
    $target = $serials[0]
    Write-Host "==> Using emulator $target (-AllowEmulator)"
  } else {
    throw "No physical Android device connected. Plug in USB debugging device, or pass -AllowEmulator / -Device <serial>."
  }
}

Write-Host "==> Target device: $target"

if (-not $SkipInstall) {
  $apk = Join-Path $Root "apk\app\build\outputs\apk\debug\app-debug.apk"
  if (-not (Test-Path $apk)) {
    Write-Host "==> Building debug APK..."
    Push-Location (Join-Path $Root "apk")
    try {
      & .\gradlew.bat :app:assembleDebug -q
      if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed" }
    } finally {
      Pop-Location
    }
  }
  Write-Host "==> Installing $apk"
  & $Adb -s $target install -r $apk
  if ($LASTEXITCODE -ne 0) { throw "adb install failed" }
}

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$reportDir = Join-Path $Root ".maestro\reports\android_$stamp"
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

if ($SmokeOnly) {
  $Flow = Join-Path $Root ".maestro\flows-android\01_onboarding_to_login.yaml"
} elseif (-not $Flow) {
  $Flow = Join-Path $Root ".maestro\flows-android"
}

Write-Host "==> Running Maestro on $target → $Flow"
Write-Host "==> Reports: $reportDir"

& $Maestro --device $target test $Flow @MaestroEnvArgs `
  --format junit `
  --output (Join-Path $reportDir "junit.xml") `
  --debug-output $reportDir

Write-Host "==> Done. Exit=$LASTEXITCODE"
exit $LASTEXITCODE
