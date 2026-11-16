#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
cd "$ANDROID_DIR"

echo "=== Building Imtihan Pro Android Debug APK ==="

command -v java >/dev/null 2>&1 || { echo "Error: JDK 17 is required."; exit 1; }

if [ -n "${ANDROID_HOME:-}" ] && [ ! -f "local.properties" ]; then
  echo "sdk.dir=$ANDROID_HOME" > local.properties
  echo "Generated android/local.properties from ANDROID_HOME=$ANDROID_HOME"
fi

if [ ! -f "./gradlew" ]; then
  if command -v gradle >/dev/null 2>&1; then
    echo "Generating Gradle 8.9 wrapper..."
    gradle wrapper --gradle-version 8.9
    chmod +x gradlew
  else
    echo "Error: ./gradlew not found and 'gradle' is not on PATH. Install Gradle 8.9 or open android/ in Android Studio."
    exit 1
  fi
fi

API_PROP=""
if [ -n "${IMTIHAN_API_BASE_URL:-}" ]; then
  echo "Embedding IMTIHAN_API_BASE_URL=$IMTIHAN_API_BASE_URL"
  API_PROP="-PIMTIHAN_API_BASE_URL=$IMTIHAN_API_BASE_URL"
fi

echo "Running Android JVM unit tests..."
./gradlew testDebugUnitTest $API_PROP

echo "Building app-debug.apk..."
./gradlew assembleDebug $API_PROP

APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "APK built at: $APK_PATH"
echo "Install onto USB-connected phone:"
echo "  adb devices"
echo "  adb install -r \"$APK_PATH\""
