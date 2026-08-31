# Dot-source from run-android.ps1 / run-qa-android.ps1
# Maestro CLI does not inherit shell env for ${VAR} in flows — pass -e KEY=VALUE.

function Import-MaestroEnvFile {
  param(
    [Parameter(Mandatory = $true)][string]$EnvFile,
    [switch]$SkipRunId
  )
  if (-not (Test-Path $EnvFile)) { return }
  Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    if ($line -match '^([^=]+)=(.*)$') {
      $key = $Matches[1].Trim()
      $val = $Matches[2].Trim().Trim("'").Trim('"')
      if ($SkipRunId -and $key -eq 'MAESTRO_RUN_ID') { return }
      Set-Item -Path "Env:$key" -Value $val -ErrorAction SilentlyContinue
    }
  }
}

function Get-MaestroEnvCliArgs {
  param(
    [Parameter(Mandatory = $true)][string]$EnvFile,
    [string]$RunId = ""
  )
  $args = @()
  if (-not (Test-Path $EnvFile)) {
    if ($RunId) {
      Set-Item -Path Env:MAESTRO_RUN_ID -Value $RunId
      $args += '-e'
      $args += "MAESTRO_RUN_ID=$RunId"
    }
    return $args
  }
  Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    if ($line -match '^([^=]+)=(.*)$') {
      $key = $Matches[1].Trim()
      $val = $Matches[2].Trim().Trim("'").Trim('"')
      if ($key -eq 'MAESTRO_RUN_ID') { return }
      Set-Item -Path "Env:$key" -Value $val -ErrorAction SilentlyContinue
      $args += '-e'
      $args += "${key}=${val}"
    }
  }
  if ($RunId) {
    Set-Item -Path Env:MAESTRO_RUN_ID -Value $RunId
    $args += '-e'
    $args += "MAESTRO_RUN_ID=$RunId"
  }
  return $args
}
