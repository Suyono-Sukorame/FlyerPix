#!/bin/bash

set -e

SDK="$HOME/Library/Android/sdk"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

echo "🚀 Menjalankan emulator..."

"$EMULATOR" -avd Pixel_7 -no-snapshot > /tmp/flyerpix-emulator.log 2>&1 &

echo "⏳ Menunggu emulator terdeteksi..."
"$ADB" wait-for-device

echo "⏳ Menunggu Android selesai boot..."

until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
done

echo "✅ Emulator siap."

echo "🔨 Build FlyerPix..."
export JAVA_HOME
./gradlew assembleDebug

echo "🧹 Uninstall aplikasi lama (jika ada)..."
"$ADB" uninstall com.flyerpix.editor 2>/dev/null || echo "   (Tidak ada aplikasi lama)"
"$ADB" uninstall com.flyerpix.flyerpix 2>/dev/null || true
"$ADB" uninstall com.pixellab.photoeditor 2>/dev/null || true

echo "📦 Install APK baru..."
"$ADB" install app/build/outputs/apk/debug/app-debug.apk

echo "▶️ Menjalankan FlyerPix..."
"$ADB" shell am start -n com.flyerpix.editor/.ui.EditorActivity

echo ""
echo "================================"
echo "✅ FlyerPix berhasil dijalankan!"
echo "================================"
