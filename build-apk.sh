#!/usr/bin/env bash
set -euo pipefail

echo "===================================================="
echo "  UTube Unlocked (v1.0) — Android APK Build Script  "
echo "===================================================="

# 1. Ensure debug.keystore is present
if [ -f "debug.keystore.base64" ] && [ ! -f "debug.keystore" ]; then
  echo "[1/3] Decoding debug.keystore from debug.keystore.base64..."
  base64 -d debug.keystore.base64 > debug.keystore
fi

# 2. Ensure .env exists for Secrets Gradle Plugin
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  echo "[2/3] Creating .env from .env.example..."
  cp .env.example .env
fi

# 3. Run Gradle assembleDebug
echo "[3/3] Building installable Android APK (:app:assembleDebug)..."
gradle :app:assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
  echo ""
  echo "✅ APK Built Successfully!"
  echo "📦 Output APK: $APK_PATH"
  ls -lh "$APK_PATH"
else
  echo "❌ APK build completed, check app/build/outputs/apk/ for generated files."
fi
