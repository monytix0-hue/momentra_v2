# Generate Swift OpenAPI client from momentra-v1.yaml
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Spec = Join-Path $Root "openapi/momentra-v1.bundled.yaml"
$Out = Join-Path $Root "../../momentra/momentra/API/Generated"

if (-not (Test-Path $Spec)) { throw "Missing bundled spec. Run npm run openapi:bundle first." }

$Jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $Jbr) {
  $env:JAVA_HOME = $Jbr
  $env:PATH = "$Jbr\bin;$env:PATH"
} elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  throw "Java not found. Install JDK or Android Studio JBR."
}

New-Item -ItemType Directory -Force -Path $Out | Out-Null

$SpecArg = $Spec.Replace('\', '/')
Write-Host "Generating Swift client -> $Out"
npx --yes @openapitools/openapi-generator-cli@2.13.4 generate `
  -i $SpecArg `
  -g swift5 `
  -o $Out `
  -p "projectName=MomentraAPI,responseAs=AsyncAwait,library=urlsession"

if ($LASTEXITCODE -ne 0) { throw "Swift OpenAPI generation failed" }
Write-Host "Done. Use adapter layer above Generated/ for app domain models."
