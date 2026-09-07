#!/bin/bash

set -e

if [ -x "/usr/lib/android-sdk/emulator/emulator" ]; then
    SDK="/usr/lib/android-sdk"
elif [ -n "$ANDROID_SDK_ROOT" ] && [ -x "$ANDROID_SDK_ROOT/emulator/emulator" ]; then
    SDK="$ANDROID_SDK_ROOT"
elif [ -n "$ANDROID_HOME" ] && [ -x "$ANDROID_HOME/emulator/emulator" ]; then
    SDK="$ANDROID_HOME"
else
    echo "❌ Emulator SDK tidak ditemukan. Set ANDROID_SDK_ROOT atau install emulator."
    exit 1
fi
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
AVD="${1:-Pixel_7}"

export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"

echo "🚀 Menjalankan emulator ($AVD)..."
if ! "$EMULATOR" -list-avds 2>/dev/null | grep -q "^$AVD$"; then
    echo "⚠️  AVD '$AVD' tidak ditemukan. Daftar AVD:"
    "$EMULATOR" -list-avds
    exit 1
fi

"$EMULATOR" -avd "$AVD" -no-snapshot -no-boot-anim -no-audio -gpu swiftshader_indirect > /tmp/flyerpix-emulator.log 2>&1 &

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