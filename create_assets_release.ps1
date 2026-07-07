param(
    [string]$OutputZip = "mubahia-assets.zip"
)

$ErrorActionPreference = "Continue"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Criando ZIP dos assets para Release" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$ClientDir = Join-Path (Split-Path $PSScriptRoot -Parent) "muBahia"

if (-not (Test-Path $ClientDir)) {
    Write-Host "ERRO: Cliente nao encontrado em: $ClientDir" -ForegroundColor Red
    exit 1
}

# Load compression assemblies
Add-Type -AssemblyName 'System.IO.Compression'
Add-Type -AssemblyName 'System.IO.Compression.FileSystem'

$totalFiles = (Get-ChildItem $ClientDir -Recurse -File).Count
$sizeInMB = [math]::Round((Get-ChildItem $ClientDir -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 0)

Write-Host "Total: $totalFiles arquivos (~${sizeInMB}MB)" -ForegroundColor Cyan
Write-Host "Compactando diretamente..." -ForegroundColor Yellow
Write-Host "Aguardar ate finalizar..." -ForegroundColor Yellow
Write-Host ""

if (Test-Path $OutputZip) { Remove-Item $OutputZip -Force }

[System.IO.Compression.ZipFile]::CreateFromDirectory($ClientDir, (Join-Path (Get-Location) $OutputZip), [System.IO.Compression.CompressionLevel]::Optimal, $false)

$sizeOut = [math]::Round((Get-Item $OutputZip).Length / 1MB, 1)
Write-Host ""
Write-Host "ZIP criado: $OutputZip ($sizeOut MB)" -ForegroundColor Green
Write-Host ""
Write-Host "Proximos passos:" -ForegroundColor Yellow
Write-Host "1. Va em: https://github.com/Danillocipo/mu-bahia-android/releases/new" -ForegroundColor White
Write-Host "2. Tag: v1.0-assets / Title: Assets do Mu Bahia" -ForegroundColor White
Write-Host "3. Anexe o arquivo mubahia-assets.zip" -ForegroundColor White
Write-Host "4. Publique a Release" -ForegroundColor White
