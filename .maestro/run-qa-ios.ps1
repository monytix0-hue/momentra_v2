# S9-QA iOS runner gate for Windows hosts (full script is run-qa-ios.sh on macOS).
param(
  [ValidateSet("smoke", "critical", "isolation", "all", "cert", "pilot", "input", "stress")]
  [string]$Class = "smoke",
  [string]$Context = "",
  [int]$Shard = 0
)

Write-Host "IOS_MAESTRO EXECUTION=BLOCKED_ENVIRONMENT (requires macOS + Simulator)"
Write-Host "Suite is implemented under .maestro/ios/ , .maestro/cert/ios/ , .maestro/input/ios/"
Write-Host "Requested: class=$Class context=$Context shard=$Shard"
Write-Host "Record status BLOCKED_ENVIRONMENT and continue Android certification on this host."

if ($Class -in @("pilot", "input", "stress")) {
  Push-Location (Join-Path $PSScriptRoot "..\backend\typescript")
  try {
    $env:QA_FIXTURES_ENABLED = "true"
    npm run qa:generate-input-flows | Out-Host
  } finally {
    Pop-Location
  }
}

exit 2
