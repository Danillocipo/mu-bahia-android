param(
    [string]$OutputZip = "mubahia-assets.zip"
)

$ErrorActionPreference = "Stop"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Criando ZIP dos assets para Release" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$ClientDir = Join-Path (Split-Path $PSScriptRoot -Parent) "muBahia"

if (-not (Test-Path $ClientDir)) {
    Write-Host "ERRO: Cliente nao encontrado em: $ClientDir" -ForegroundColor Red
    exit 1
}

Write-Host "Comprimindo assets de: $ClientDir" -ForegroundColor Yellow
Write-Host "Arquivo de saida: $OutputZip" -ForegroundColor Yellow

# Create zip excluding screenshots and logs
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($OutputZip, [System.IO.Compression.ZipArchiveMode]::Create)

$totalFiles = 0
Get-ChildItem $ClientDir -Recurse -File | Where-Object {
    $_.DirectoryName -notmatch "(ScreenShots|MHP_LOG)"
} | ForEach-Object {
    $relativePath = $_.FullName.Substring($ClientDir.Length + 1)
    $null = [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $relativePath, [System.IO.Compression.CompressionLevel]::Optimal)
    $totalFiles++
}

$zip.Dispose()

$sizeInMB = [math]::Round((Get-Item $OutputZip).Length / 1MB, 1)
Write-Host ""
Write-Host "ZIP criado: $OutputZip" -ForegroundColor Green
Write-Host "Arquivos: $totalFiles" -ForegroundColor Green
Write-Host "Tamanho: $sizeInMB MB" -ForegroundColor Green
Write-Host ""
Write-Host "Proximos passos:" -ForegroundColor Yellow
Write-Host "1. Crie uma Release no GitHub" -ForegroundColor White
Write-Host "2. Upload deste ZIP como mubahia-assets.zip" -ForegroundColor White
Write-Host "3. O CI vai baixar automaticamente" -ForegroundColor White
