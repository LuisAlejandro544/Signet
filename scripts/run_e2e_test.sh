#!/usr/bin/env bash
set -e

# Exportar rutas del SDK de Android para que ADB esté 100% disponible
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

if [ -d "$ANDROID_HOME/platform-tools" ]; then
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi
if [ -d "/usr/local/lib/android/sdk/platform-tools" ]; then
    export PATH="/usr/local/lib/android/sdk/platform-tools:$PATH"
fi

echo "==> Verificando disponibilidad de ADB..."
which adb || echo "Buscando binario adb alternativo..."
adb devices

APK_PATH="$1"
API_LEVEL="${2:-34}"

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    APK_PATH=$(find app/build/outputs/apk/debug/ -type f -name "*.apk" | head -n 1)
fi

echo "==> Instalando APK en el emulador: $APK_PATH"
adb install -r "$APK_PATH"

PACKAGE_NAME="com.signet.app"
adb shell pm grant "$PACKAGE_NAME" android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
adb shell pm grant "$PACKAGE_NAME" android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null || true

# Encontrar binario de Python
PYTHON_CMD="python3"
if ! command -v python3 &> /dev/null; then
    if command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        PYTHON_CMD=$(find /opt/hostedtoolcache/Python/ -name python3 2>/dev/null | head -n 1 || echo "python3")
    fi
fi

echo "==> Ejecutando suite de validación Python con: $PYTHON_CMD"
$PYTHON_CMD ./scripts/e2e_emulator_test.py "$API_LEVEL"
