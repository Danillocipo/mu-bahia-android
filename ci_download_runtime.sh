#!/bin/bash
set -e

echo "=========================================="
echo " CI: Baixando rootfs + Box64 + Wine"
echo "=========================================="

mkdir -p app/src/main/assets app/src/main/jniLibs/arm64-v8a

# --- Download Winlator APK para extrair rootfs ---
echo "[1/4] Baixando Winlator APK..."
WINLATOR_URL="https://github.com/brunodev85/winlator/releases/latest/download/Winlator_11.1.apk"
wget -q --show-progress "$WINLATOR_URL" -O /tmp/winlator.apk || {
  # Fallback: tentar versao anterior
  WINLATOR_URL="https://github.com/brunodev85/winlator/releases/download/v11.1.0/Winlator_11.1.apk"
  wget -q --show-progress "$WINLATOR_URL" -O /tmp/winlator.apk
}

echo "  Extraindo rootfs..."
unzip -o /tmp/winlator.apk "assets/imagefs.*" -d app/src/main/ 2>/dev/null || true
unzip -o /tmp/winlator.apk "lib/arm64-v8a/*" -d app/src/main/jniLibs/ 2>/dev/null || true

# Renomear libs
if [ -d "app/src/main/jniLibs/lib" ]; then
  mv app/src/main/jniLibs/lib/* app/src/main/jniLibs/arm64-v8a/ 2>/dev/null || true
  rm -rf app/src/main/jniLibs/lib
fi

rm -f /tmp/winlator.apk

echo "  Rootfs: $(ls -lh app/src/main/assets/imagefs.* 2>/dev/null | awk '{print $5, $9}')"
echo "  Libs: $(ls app/src/main/jniLibs/arm64-v8a/ 2>/dev/null | wc -l) arquivos"

# --- Wine (Kron4ek) ---
echo "[2/4] Baixando Wine..."
WINE_URL="https://github.com/Kron4ek/Wine-Builds/releases/download/11.0/wine-11.0-staging-tkg-amd64-wow64.tar.xz"
wget -q --show-progress "$WINE_URL" -O /tmp/wine.tar.xz
mkdir -p app/src/main/assets/wine
tar -xf /tmp/wine.tar.xz -C app/src/main/assets/wine/ --strip-components=1
rm -f /tmp/wine.tar.xz
echo "  Wine: $(find app/src/main/assets/wine -type f | wc -l) arquivos"

# --- Box64 Android ---
echo "[3/4] Baixando Box64..."
BOX64_URL="https://github.com/ptitSeb/box64/releases/download/v0.4.2/box64-android"
wget -q --show-progress "$BOX64_URL" -O app/src/main/assets/box64 2>/dev/null || {
  # Fallback: repo do usuario
  BOX64_URL="https://github.com/ptitSeb/box64/releases/download/v0.4.0/box64-android"
  wget -q --show-progress "$BOX64_URL" -O app/src/main/assets/box64
}
chmod +x app/src/main/assets/box64 2>/dev/null || true
echo "  Box64: $(ls -lh app/src/main/assets/box64 2>/dev/null | awk '{print $5}')"

# --- Game Assets ---
echo "[4/4] Baixando Mu Bahia assets..."
GAME_URL="https://github.com/${GITHUB_REPOSITORY}/releases/latest/download/mubahia-assets.zip"
if wget -q --show-progress "$GAME_URL" -O /tmp/mubahia.zip 2>/dev/null; then
    echo "  Extraindo..."
    mkdir -p app/src/main/assets/game
    unzip -q /tmp/mubahia.zip -d app/src/main/assets/game/
    rm -f /tmp/mubahia.zip
else
    echo "  AVISO: Assets nao encontrados. Serao baixados na 1a execucao."
fi

echo "=========================================="
echo " Resumo:"
echo "  Rootfs: $(find app/src/main/assets -maxdepth 1 -name 'imagefs.*' | wc -l) arquivo(s)"
echo "  Wine:   $(find app/src/main/assets/wine -type f 2>/dev/null | wc -l) arquivos"
echo "  Box64:  $(file app/src/main/assets/box64 2>/dev/null | head -1)"
echo "  Libs:   $(ls app/src/main/jniLibs/arm64-v8a/ 2>/dev/null | wc -l) arquivos"
echo "  Game:   $(find app/src/main/assets/game -type f 2>/dev/null | wc -l) arquivos"
du -sh app/src/main/assets/ app/src/main/jniLibs/ 2>/dev/null
echo "=========================================="
