#!/bin/bash

set -e

PACKAGE="com.flyerpix.editor"
ACTIVITY="$PACKAGE/.ui.EditorActivity"
SDK="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"

if [ ! -x "$ADB" ]; then
	echo "❌ adb tidak ditemukan di: $ADB"
	echo "Set ANDROID_SDK_ROOT ke lokasi Android SDK yang benar."
	exit 1
fi

echo "🔨 Build dan install FlyerPix tanpa menghapus data..."
./gradlew :app:installDebug

echo "⏹️  Menghentikan aplikasi lama..."
"$ADB" shell am force-stop "$PACKAGE"

echo "▶️  Menjalankan FlyerPix..."
"$ADB" shell am start -n "$ACTIVITY"

echo "✅ FlyerPix berhasil di-install dan dijalankan tanpa uninstall."
