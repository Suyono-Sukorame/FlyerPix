#!/bin/bash

set -e

# Resolusi lokasi Android SDK: env -> local.properties -> lokasi umum
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [ -z "$SDK" ] && [ -f "local.properties" ]; then
    SDK="$(sed -n 's/^sdk\.dir=//p' local.properties | tail -1)"
fi

if [ -z "$SDK" ] || [ ! -x "$SDK/emulator/emulator" ]; then
    for cand in "$HOME/Android/Sdk" "$HOME/Android/sdk" "/usr/lib/android-sdk" "$HOME/Library/Android/sdk"; do
        if [ -x "$cand/emulator/emulator" ]; then
            SDK="$cand"
            break
        fi
    done
fi

if [ -z "$SDK" ] || [ ! -x "$SDK/emulator/emulator" ]; then
    echo "❌ Emulator SDK tidak ditemukan. Set ANDROID_SDK_ROOT atau install emulator."
    exit 1
fi
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
AVD="${1:-Pixel_7_aosp}"

export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"

echo "🚀 Menjalankan emulator ($AVD)..."

# Pakai emulator yang sudah jalan (mis. dari sesi sebelumnya) untuk start cepat
if "$ADB" devices 2>/dev/null | grep -q "emulator-"; then
    echo "↩️  Emulator sudah berjalan, memakainya."
else
    if ! "$EMULATOR" -list-avds 2>/dev/null | grep -q "^$AVD$"; then
        echo "⚠️  AVD '$AVD' tidak ditemukan. Daftar AVD:"
        "$EMULATOR" -list-avds
        exit 1
    fi
    "$EMULATOR" -avd "$AVD" -no-boot-anim -no-audio -gpu swiftshader_indirect > /tmp/flyerpix-emulator.log 2>&1 &
fi

echo "⏳ Menunggu emulator terdeteksi..."
"$ADB" wait-for-device

echo "⏳ Menunggu Android selesai boot..."
until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
done

echo "✅ Emulator siap."

echo "🔨 Build FlyerPix..."
./gradlew assembleDebug

echo "🧹 Uninstall aplikasi lama (jika ada)..."
"$ADB" uninstall com.flyerpix.editor 2>/dev/null || echo "   (Tidak ada aplikasi lama)"

echo "📦 Install APK baru..."
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk

echo "▶️ Menjalankan FlyerPix..."
"$ADB" shell am start -n com.flyerpix.editor/.ui.EditorActivity

echo ""
echo "================================"
echo "✅ FlyerPix berhasil dijalankan!"
echo "================================"

echo "🧹 Menghentikan daemon Gradle/Kotlin (hemat RAM)..."
./gradlew --stop >/dev/null 2>&1 || true