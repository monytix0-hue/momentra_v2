# Generate Kotlin Retrofit client from openapi/momentra-v1.yaml
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Spec = Join-Path $Root "openapi/momentra-v1.bundled.yaml"
$Out = Join-Path $Root "../../apk/openapi-generated"

if (-not (Test-Path $Spec)) { throw "Missing bundled spec. Run npm run openapi:bundle first." }

New-Item -ItemType Directory -Force -Path $Out | Out-Null
Remove-Item -Recurse -Force (Join-Path $Out "src/test") -ErrorAction SilentlyContinue

$Jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $Jbr) {
  $env:JAVA_HOME = $Jbr
  $env:PATH = "$Jbr\bin;$env:PATH"
} elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  throw "Java not found. Install JDK or Android Studio JBR."
}

$SpecArg = $Spec.Replace('\', '/')
Write-Host "Generating Kotlin client -> $Out"
npx --yes @openapitools/openapi-generator-cli@2.13.4 generate `
  -i $SpecArg `
  -g kotlin `
  -o $Out `
  --global-property apiTests=false,modelTests=false `
  -p "library=jvm-retrofit2,serializationLibrary=gson,packageName=com.example.momentra.data.api.generated,useCoroutines=true"

if ($LASTEXITCODE -ne 0) { throw "Kotlin OpenAPI generation failed" }

# Keep only transport sources; drop standalone Gradle project artifacts from generator output.
@(
  (Join-Path $Out "src/test"),
  (Join-Path $Out "docs"),
  (Join-Path $Out "gradle"),
  (Join-Path $Out "build.gradle"),
  (Join-Path $Out "settings.gradle"),
  (Join-Path $Out "gradlew"),
  (Join-Path $Out "gradlew.bat"),
  (Join-Path $Out "README.md")
) | ForEach-Object { if (Test-Path $_) { Remove-Item -Recurse -Force $_ } }
Write-Host "Done. Wire generated interfaces alongside hand-maintained ApiService.kt if needed."
