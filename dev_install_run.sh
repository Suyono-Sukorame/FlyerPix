#!/bin/bash

set -e

PACKAGE="com.flyerpix.editor"
ACTIVITY="$PACKAGE/.ui.EditorActivity"

# Resolusi lokasi Android SDK: env -> local.properties -> lokasi umum
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [ -z "$SDK" ] && [ -f "local.properties" ]; then
	SDK="$(sed -n 's/^sdk\.dir=//p' local.properties | tail -1)"
fi

if [ -z "$SDK" ] || [ ! -x "$SDK/platform-tools/adb" ]; then
	for cand in "$HOME/Android/Sdk" "$HOME/Android/sdk" "/usr/lib/android-sdk" "$HOME/Library/Android/sdk"; do
		if [ -x "$cand/platform-tools/adb" ]; then
			SDK="$cand"
			break
		fi
	done
fi

ADB="$SDK/platform-tools/adb"

if [ ! -x "$ADB" ]; then
	echo "❌ adb tidak ditemukan di: $ADB"
	echo "Set ANDROID_SDK_ROOT ke lokasi Android SDK yang benar."
	exit 1
fi

export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"

echo "🔨 Build dan install FlyerPix tanpa menghapus data..."
./gradlew :app:installDebug

echo "⏹️  Menghentikan aplikasi lama..."
"$ADB" shell am force-stop "$PACKAGE"

echo "▶️  Menjalankan FlyerPix..."
"$ADB" shell am start -n "$ACTIVITY"

echo "✅ FlyerPix berhasil di-install dan dijalankan tanpa uninstall."

echo "🧹 Menghentikan daemon Gradle/Kotlin (hemat RAM)..."
./gradlew --stop >/dev/null 2>&1 || true
