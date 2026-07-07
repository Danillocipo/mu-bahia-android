param([switch]$Build)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Mu Bahia Android v2 - Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Download runtime + copy game assets
Write-Host "[1/3] Baixando runtime e copiando assets..." -ForegroundColor Yellow
& .\download_runtime.ps1

# 2. Build
if ($Build) {
    Write-Host "[2/3] Compilando APK..." -ForegroundColor Yellow
    if (-not (Test-Path "local.properties")) {
        Write-Host "  AVISO: local.properties nao encontrado." -ForegroundColor Red
        Write-Host "  Copie local.properties.example -> local.properties" -ForegroundColor Yellow
        Write-Host "  E configure o caminho do Android SDK" -ForegroundColor Yellow
        exit 1
    }
    .\gradlew assembleRelease
    if ($?) {
        Write-Host "  APK gerado em: app/build/outputs/apk/release/" -ForegroundColor Green
    }
} else {
    Write-Host "[2/3] Build pulado (use -Build para compilar)" -ForegroundColor Yellow
}

Write-Host "[3/3] Concluido!" -ForegroundColor Yellow
Write-Host ""
Write-Host "Para compilar:" -ForegroundColor White
Write-Host "  1. Configure ANDROID_HOME" -ForegroundColor White
Write-Host "  2. .\setup.ps1 -Build" -ForegroundColor White
Write-Host ""
Write-Host "Ou via GitHub Actions:" -ForegroundColor White
Write-Host "  Siga SETUP_GITHUB.md" -ForegroundColor White
