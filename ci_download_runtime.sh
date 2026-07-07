#!/bin/bash
set -e

echo "=========================================="
echo " CI: Download Wine + Box64 + Game Assets"
echo "=========================================="

ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR/mubahia" "$ASSETS_DIR/wine/bin" "$ASSETS_DIR/box64"

# --- Wine ARM64 ---
echo "[1/4] Baixando Wine..."
WINE_URL="https://github.com/brunodev85/winlator/releases/download/v10.1/wine-arm64.tar.xz"
wget -q --show-progress "$WINE_URL" -O /tmp/wine.tar.xz
echo "  Extraindo..."
tar -xf /tmp/wine.tar.xz -C "$ASSETS_DIR/wine/" 2>/dev/null || echo "  (aviso: extracao manual pode ser necessaria)"
rm -f /tmp/wine.tar.xz

# --- Box64 ---
echo "[2/4] Baixando Box64..."
BOX64_URL="https://github.com/ptitSeb/box64/releases/latest/download/box64-android"
wget -q --show-progress "$BOX64_URL" -O "$ASSETS_DIR/box64/box64"
chmod +x "$ASSETS_DIR/box64/box64"

# --- Game Assets ---
echo "[3/4] Baixando Mu Bahia assets..."
# Tenta baixar de uma release do proprio repositorio
GAME_URL="https://github.com/${GITHUB_REPOSITORY}/releases/latest/download/mubahia-assets.zip"
if wget -q --show-progress "$GAME_URL" -O /tmp/mubahia.zip 2>/dev/null; then
    echo "  Extraindo..."
    unzip -q /tmp/mubahia.zip -d "$ASSETS_DIR/mubahia/"
    rm -f /tmp/mubahia.zip
else
    echo "  AVISO: Assets nao encontrados em release."
    echo "  Crie uma Release com o arquivo mubahia-assets.zip"
    echo "  Ou compile localmente com download_runtime.ps1"
fi

# --- Summary ---
echo "[4/4] Verificando..."
echo "  Wine:  $(find $ASSETS_DIR/wine -type f | wc -l) arquivos"
echo "  Box64: $(find $ASSETS_DIR/box64 -type f | wc -l) arquivos"
echo "  Game:  $(find $ASSETS_DIR/mubahia -type f 2>/dev/null | wc -l) arquivos"

echo "=========================================="
echo " Download concluido!"
echo "=========================================="
