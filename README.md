# FlyerPix

<img src="./assets/icon-ok.svg" width="120" height="120">

A PixelLab-style flyer & poster design editor for Android, built with Kotlin.

## Features

- **Presets** – built-in template gallery with a "My Projects" shortcut to open saved projects
- **Text** – add, edit and style text layers (fonts, spacing, stroke, gradient, shadow, 3D extrude/rotate, curved text, perspective, reflection, masks)
- **Objects** – shapes, freehand drawing, Bézier curves, arrows, stickers and image import (gallery / camera)
- **Canvas** – background modes (transparent, solid, gradient, image) and canvas size presets
- **Effects** – brightness, contrast, saturation, vignette, noise, monochrome and blur adjustments
- **Layers** – stack, reorder, lock, hide, duplicate, delete and merge layers
- **Projects** – save and open projects, export the final image

## Tech Stack

- Kotlin + Android View system (XML layouts + MotionLayout)
- Custom canvas rendering (`PixelCanvasView`) with command-based undo/redo history
- Material Components, ViewBinding, Glide, Gson

## Requirements

- Android Studio with Android SDK + Emulator
- JDK 17 or 21 (Gradle wrapper 8.5 does not support Java 24)
- An Android Virtual Device (AVD)

## Build & Run

```bash
# 1. Start the emulator (background)
~/Library/Android/sdk/emulator/emulator -avd Pixel_7 -no-snapshot &

# 2. Wait for the device to boot
~/Library/Android/sdk/platform-tools/adb wait-for-device

# 3. Build the debug APK
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug

# 4. Install
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. Launch
~/Library/Android/sdk/platform-tools/adb shell am start -n com.flyerpix.editor/.ui.EditorActivity
```

The APK output is located at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Info

- Application ID / namespace: `com.flyerpix.editor`
- Launcher activity: `.ui.EditorActivity`