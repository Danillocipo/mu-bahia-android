# Mu Bahia Android - Build via GitHub Actions

## Passo 1: Criar repositorio

Crie um repositorio **PRIVADO** no GitHub:

```powershell
cd mu-bahia-android
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/SEU_USUARIO/mu-bahia-android.git
git push -u origin main
```

## Passo 2: Fazer upload dos assets do jogo

Os ~800MB de assets do Mu Bahia NAO sao enviados pelo git (estao no .gitignore).

Crie um ZIP com os assets:

```powershell
.\create_assets_release.ps1
```

Isso gera `mubahia-assets.zip`. Agora:

1. Va no GitHub > seu repositorio > **Releases**
2. Crie uma nova Release (ex: `v1.0-assets`)
3. Upload do `mubahia-assets.zip`
4. Publique a Release

## Passo 3: Build automatico

Acesse:
```
https://github.com/SEU_USUARIO/mu-bahia-android/actions
```

Clique em **"Build APK"** > **"Run workflow"**

O CI vai:
- Baixar Wine + Box64
- Baixar os assets do jogo da Release
- Compilar o APK
- Disponibilizar em **Artifacts**

## Passo 4: Instalar

1. Baixe `mu-bahia-android-v2.zip` dos Artifacts
2. Extraia e instale o `app-release.apk` no celular
3. Abra o app (permissoes de armazenamento)
4. Aguarde o download inicial (Wine + Box64)
5. Clique em **JOGAR**

## Compilacao local (alternativa)

Se quiser compilar direto no PC sem GitHub Actions:

```powershell
.\download_runtime.ps1
.\gradlew assembleRelease
```

O APK estara em `app/build/outputs/apk/release/`.
