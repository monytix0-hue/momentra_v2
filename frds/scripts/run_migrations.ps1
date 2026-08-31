# Run V001-V030 against Supabase PostgreSQL (direct connection required for DDL).
param(
    [Parameter(Mandatory = $true)]
    [string]$DatabaseUri,
    [switch]$InstallOnly,  # V001-V029
    [switch]$ValidationOnly  # V030 only
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Migrations = Join-Path $Root "migrations"
$Manifest = Join-Path $Root "manifest"
$OrderFile = Join-Path $Manifest "MIGRATION_ORDER.txt"

if (-not (Test-Path $OrderFile)) {
    throw "Run assemble_migrations.py first. Missing $OrderFile"
}

# Verify checksums
Push-Location $Migrations
Get-Content (Join-Path $Manifest "SHA256SUMS.txt") | ForEach-Object {
    $parts = $_ -split '\s+', 2
    $expected = $parts[0]
    $file = $parts[1]
    $actual = (Get-FileHash -Algorithm SHA256 $file).Hash.ToLower()
    if ($actual -ne $expected) {
        throw "Checksum mismatch for $file"
    }
}
Pop-Location
Write-Host "Checksums OK"

$order = Get-Content $OrderFile | Where-Object { $_.Trim() -ne "" }
$install = $order | Where-Object { $_ -match '^V0(0[1-9]|[12][0-9])__' }
$validation = $order | Where-Object { $_ -match '^V030__' }

function Invoke-Migration([string]$File) {
    Write-Host "==> $File"
    $path = Join-Path $Migrations $File
    psql $DatabaseUri -v ON_ERROR_STOP=1 -f $path
    if ($LASTEXITCODE -ne 0) { throw "Migration failed: $File" }
}

if (-not $ValidationOnly) {
    foreach ($f in $install) { Invoke-Migration $f }
    Write-Host "V001-V029 installation complete"
}

if (-not $InstallOnly) {
    foreach ($f in $validation) {
        try {
            Invoke-Migration $f
            Write-Host "V030 validation passed"
        } catch {
            Write-Warning "V030 validation reported blockers (expected for PRE-RC DRAFT seeds): $_"
        }
    }
}
