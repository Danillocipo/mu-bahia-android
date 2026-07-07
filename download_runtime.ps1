param(
    [string]$AssetsDir = "app/src/main/assets"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Mu Bahia Android v2 - Setup de Assets" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Create assets dirs
$MubahiaDir = Join-Path $AssetsDir "mubahia"
$WineDir = Join-Path $AssetsDir "wine"
$Box64Dir = Join-Path $AssetsDir "box64"

New-Item -ItemType Directory -Path $MubahiaDir -Force | Out-Null
New-Item -ItemType Directory -Path $WineDir -Force | Out-Null
New-Item -ItemType Directory -Path $Box64Dir -Force | Out-Null

# 1. Copy Mu Bahia client
Write-Host "[1/3] Copiando Mu Bahia client..." -ForegroundColor Yellow
$ClientSource = Join-Path (Split-Path $PSScriptRoot -Parent) "muBahia"

if (Test-Path $ClientSource) {
    Get-ChildItem $ClientSource -Directory | ForEach-Object {
        if ($_.Name -notin @("ScreenShots", "MHP_LOG")) {
            Copy-Item -Recurse $_.FullName -Destination $MubahiaDir -Force
        }
    }
    Get-ChildItem $ClientSource -File | ForEach-Object {
        Copy-Item $_.FullName -Destination $MubahiaDir -Force
    }
    $count = (Get-ChildItem $MubahiaDir -Recurse -File).Count
    Write-Host "  OK - $count arquivos" -ForegroundColor Green
} else {
    Write-Host "  ERRO: Cliente nao encontrado em $ClientSource" -ForegroundColor Red
    exit 1
}

# 2. Download Wine runtime (pre-built ARM64)
Write-Host "[2/3] Baixando Wine ARM64..." -ForegroundColor Yellow
$WineUrl = "https://github.com/brunodev85/winlator/releases/download/v10.1/wine-arm64.tar.xz"
$WineArchive = Join-Path $env:TEMP "wine-arm64.tar.xz"

try {
    if (-not (Test-Path $WineArchive)) {
        Invoke-WebRequest -Uri $WineUrl -OutFile $WineArchive -UseBasicParsing
    }
    Write-Host "  Extraindo... (pode levar alguns minutos)" -ForegroundColor Yellow
    # Extract tar.xz
    tar -xf $WineArchive -C $WineDir 2>$null
    if ($?) {
        Write-Host "  Wine extraido em: $WineDir" -ForegroundColor Green
    } else {
        Write-Host "  Falha ao extrair. Use 7z: 7z x $WineArchive -o$WineDir" -ForegroundColor Red
    }
} catch {
    Write-Host "  Nao foi possivel baixar Wine. Voce precisara baixar manualmente." -ForegroundColor Red
    Write-Host "  URL: $WineUrl" -ForegroundColor Yellow
}

# 3. Download Box64
Write-Host "[3/3] Baixando Box64..." -ForegroundColor Yellow
$Box64Url = "https://github.com/ptitSeb/box64/releases/latest/download/box64-android"
$Box64Bin = Join-Path $Box64Dir "box64"

try {
    Invoke-WebRequest -Uri $Box64Url -OutFile $Box64Bin -UseBasicParsing
    Write-Host "  Box64 baixado" -ForegroundColor Green
} catch {
    Write-Host "  Nao foi possivel baixar Box64." -ForegroundColor Red
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Resumo dos assets:" -ForegroundColor Cyan
Write-Host "  Mubahia: $( (Get-ChildItem $MubahiaDir -Recurse -File).Count ) arquivos" -ForegroundColor White
Write-Host "  Wine:    $( (Get-ChildItem $WineDir -Recurse -File).Count ) arquivos" -ForegroundColor White
Write-Host "  Box64:   $( (Get-ChildItem $Box64Dir -Recurse -File).Count ) arquivos" -ForegroundColor White
Write-Host ""
Write-Host " Para compilar o APK:" -ForegroundColor Yellow
Write-Host "   1. Configure ANDROID_HOME" -ForegroundColor White
Write-Host "   2. Copie local.properties.example -> local.properties" -ForegroundColor White
Write-Host "   3. Execute: .\gradlew assembleRelease" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
