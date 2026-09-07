#!/bin/bash

# install_apk.sh - Install FlyerPix APK ke Android device

echo "=== FlyerPix APK Installer ==="
echo ""

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "❌ ERROR: adb not found. Please install Android SDK Platform Tools."
    echo ""
    echo "Installation instructions:"
    echo "  macOS: brew install android-sdk-platform-tools"
    echo "  Ubuntu: sudo apt-get install android-tools-adb"
    echo "  Windows: Download from https://developer.android.com/studio/releases/platform-tools"
    exit 1
fi

# Check if device is connected
echo "Checking connected devices..."
DEVICES=$(adb devices | grep -v "List of attached" | grep -v "^$" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ ERROR: No Android device connected"
    echo ""
    echo "Steps to connect:"
    echo "  1. Enable USB Debugging on your device (Settings > Developer Options)"
    echo "  2. Connect device via USB cable"
    echo "  3. Accept the ADB authorization prompt on device"
    echo "  4. Run this script again"
    exit 1
fi

echo "✓ Found $DEVICES connected device(s)"
adb devices

echo ""
echo "Building APK..."
./gradlew assembleDebug --no-daemon

if [ $? -ne 0 ]; then
    echo "❌ BUILD FAILED"
    exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi

echo "✓ APK built successfully"
echo ""
echo "Installing APK..."
echo "  File: $APK_PATH"
echo "  Size: $(du -h $APK_PATH | cut -f1)"

adb install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ INSTALLATION SUCCESSFUL!"
    echo ""
    echo "Next steps:"
    echo "  1. Open FlyerPix app on device"
    echo "  2. Test the native rendering engine"
    echo "  3. Check logcat for any errors:"
    echo "     adb logcat | grep flyerpix"
else
    echo ""
    echo "❌ INSTALLATION FAILED"
    echo ""
    echo "Troubleshooting:"
    echo "  1. Check if app already installed: adb shell pm list packages | grep flyerpix"
    echo "  2. Uninstall first: adb uninstall com.flyerpix.editor"
    echo "  3. Check device storage: adb shell df"
    echo "  4. View error details: adb logcat | tail -50"
    exit 1
fi

echo ""
echo "Launching app..."
adb shell am start -n com.flyerpix.editor/.ui.EditorActivity

echo ""
echo "Done! Monitoring logcat for native engine..."
echo "Press Ctrl+C to stop"
echo ""
adb logcat | grep -E "(FlyerPix|Engine|native|jni)" | head -100

