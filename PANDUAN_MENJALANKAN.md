# Panduan Menjalankan Aplikasi FlyerPix di Emulator Android

Panduan ini menjelaskan cara build dan menjalankan **FlyerPix** (aplikasi desain flyer/poster Android) di emulator Android.

## Prasyarat

1. **Android SDK + Emulator** terinstal.
   - Lokasi SDK default (macOS): `~/Library/Android/sdk`
2. **Java 21 (JDK)**. Gradle wrapper **8.5 tidak mendukung Java 24** — gunakan Java 17/21.
   - Bundled JBR Android Studio (Java 21):
     ```
     /Applications/Android Studio.app/Contents/jbr/Contents/Home
     ```
3. **AVD (Android Virtual Device)** sudah dibuat (contoh: `Pixel_7`, `Medium_Phone_API_35`).
4. CLI Android (`adb`, `emulator`) bisa diakses dari `~/Library/Android/sdk`.

> **Info build**: `applicationId` & `namespace` = `com.flyerpix.editor`.
> **Launcher activity** = `.ui.EditorActivity` (terdaftar `MAIN`/`LAUNCHER`), jadi bisa dibuka lewat ikon aplikasi maupun perintah `am start`.

## Langkah Persiapan

### 1. Pastikan SDK sudah ditunjuk

File `local.properties` di root project harus berisi lokasi SDK:

```
sdk.dir=/Users/admin/Library/Android/sdk
```

### 2. Periksa AVD yang tersedia

```bash
~/Library/Android/sdk/emulator/emulator -list-avds
```

Contoh output:

```
Medium_Phone_API_35
Pixel_7
```

### 3. Mulai emulator

```bash
~/Library/Android/sdk/emulator/emulator -avd Pixel_7 -no-snapshot &
```

### 4. Tunggu emulator selesai boot

```bash
~/Library/Android/sdk/platform-tools/adb wait-for-device
# cek status boot (output "1" = sudah selesai boot)
~/Library/Android/sdk/platform-tools/adb shell getprop sys.boot_completed
```

## Build dan Install

### 5. Build APK debug

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

- Jika `JAVA_HOME` sudah mengarah ke Java 17/21, cukup tulis `./gradlew assembleDebug`.
- Hasil APK: `app/build/outputs/apk/debug/app-debug.apk`

### 6. Install ke emulator

```bash
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 7. Jalankan aplikasi

```bash
~/Library/Android/sdk/platform-tools/adb shell am start -n com.flyerpix.editor/.ui.EditorActivity
```

> `EditorActivity` adalah launcher, sehingga aplikasi juga bisa dibuka langsung dari ikon di emulator.

## Verifikasi

Cek bahwa aktivitas sedang fokus di depan:

```bash
~/Library/Android/sdk/platform-tools/adb shell dumpsys window | grep mCurrentFocus
```

Output yang diharapkan:

```
mCurrentFocus=Window{... com.flyerpix.editor/com.flyerpix.editor.ui.EditorActivity}
```

## Tips dan Troubleshooting

| Masalah | Solusi |
| --- | --- |
| `Error: Activity class ... MainActivity does not exist` | Activity utama adalah `EditorActivity`, gunakan perintah pada langkah 7 (pakai aplikasi `com.flyerpix.editor`, bukan `com.pixellab.photoeditor`). |
| Gradle gagal dengan Java 24 | Gunakan Java 17/21, mis. `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`. |
| Emulator lambat | Tambahkan flag `-no-snapshot-load` atau gunakan AVD beresolusi lebih rendah. |
| `adb: device not found` | Pastikan emulator sudah boot; cek dengan `~/Library/Android/sdk/platform-tools/adb devices`. |
| `local.properties` salah / SDK tidak ditemukan | Periksa `sdk.dir` mengarah ke lokasi SDK yang benar. |

## Alur Cepat (Copy-Paste)

```bash
# 1. mulai emulator (background)
~/Library/Android/sdk/emulator/emulator -avd Pixel_7 -no-snapshot &

# 2. tunggu boot
~/Library/Android/sdk/platform-tools/adb wait-for-device

# 3. build APK debug
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug

# 4. install
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. jalankan aplikasi
~/Library/Android/sdk/platform-tools/adb shell am start -n com.flyerpix.editor/.ui.EditorActivity
```
===

admin@admins-Mac-mini FlyerPix % ./run.sh   
🚀 Menjalankan emulator...
⏳ Menunggu emulator terdeteksi...
⏳ Menunggu Android selesai boot...
✅ Emulator siap.
🔨 Build FlyerPix...

BUILD SUCCESSFUL in 1s
35 actionable tasks: 1 executed, 34 up-to-date
🧹 Uninstall aplikasi lama (jika ada)...
Success
Failure [DELETE_FAILED_INTERNAL_ERROR]
Failure [DELETE_FAILED_INTERNAL_ERROR]
📦 Install APK baru...
Performing Streamed Install
Success
▶️ Menjalankan FlyerPix...
Starting: Intent { cmp=com.flyerpix.editor/.ui.EditorActivity }

================================
✅ FlyerPix berhasil dijalankan!
================================
admin@admins-Mac-mini FlyerPix % 
