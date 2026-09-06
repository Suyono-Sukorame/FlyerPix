package com.flyerpix.editor.font

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/**
 * Singleton objek FontManager untuk memuat dan mengelola font bawaan dari folder
 * assets/fonts/ serta font kustom (.ttf / .otf) dari penyimpanan perangkat.
 * Mendukung kategori: Modern, Serif, Sans-Serif, Hand-written, Bold Display, dan My Fonts.
 */
object FontManager {

    private val fontList = mutableListOf<FontItem>()
    private var isInitialized = false

    private val BUILT_IN_FONT_DEFINITIONS = listOf(
        // Sans-Serif
        Triple("Roboto Regular", "Sans-Serif", "fonts/roboto_regular.ttf"),
        Triple("Roboto Bold", "Sans-Serif", "fonts/roboto_bold.ttf"),

        // Modern
        Triple("Arial Rounded", "Modern", "fonts/arial_rounded.ttf"),
        Triple("Courier New", "Modern", "fonts/courier_new.ttf"),

        // Serif
        Triple("Georgia", "Serif", "fonts/georgia.ttf"),
        Triple("Georgia Bold", "Serif", "fonts/georgia_bold.ttf"),
        Triple("Times New Roman", "Serif", "fonts/times_new_roman.ttf"),

        // Bold Display
        Triple("Impact", "Bold Display", "fonts/impact.ttf"),
        Triple("Arial Black", "Bold Display", "fonts/arial_black.ttf"),

        // Hand-written
        Triple("Bradley Hand", "Hand-written", "fonts/bradley_hand.ttf"),
        Triple("Brush Script", "Hand-written", "fonts/brush_script.ttf"),
        Triple("Chalkduster", "Hand-written", "fonts/chalkduster.ttf")
    )

    /**
     * Memuat semua font bawaan dari assets dan me-restore font kustom yang tersimpan di internal storage.
     */
    fun init(context: Context) {
        if (isInitialized) return

        fontList.clear()

        // 1. Tambahkan default system font
        fontList.add(FontItem("Default", "Sans-Serif", null, null, Typeface.DEFAULT))

        // 2. Muat font bawaan dari assets/fonts/
        for ((name, category, assetPath) in BUILT_IN_FONT_DEFINITIONS) {
            try {
                val typeface = Typeface.createFromAsset(context.assets, assetPath)
                fontList.add(FontItem(name, category, assetPath, null, typeface))
            } catch (e: Exception) {
                val fallbackTypeface = when (category) {
                    "Serif" -> Typeface.SERIF
                    "Modern" -> Typeface.MONOSPACE
                    "Bold Display" -> Typeface.DEFAULT_BOLD
                    else -> Typeface.SANS_SERIF
                }
                fontList.add(FontItem(name, category, null, null, fallbackTypeface))
            }
        }

        // 3. Muat kembali font kustom (My Fonts) yang pernah disimpan di internal storage
        loadSavedCustomFonts(context)

        isInitialized = true
    }

    /**
     * Membaca dan membuat Typeface dari URI file .ttf / .otf dari penyimpanan pengguna,
     * menyalinnya secara aman ke internal storage aplikasi, dan menyimpannya ke kategori "My Fonts".
     *
     * @param context Context aplikasi
     * @param uri URI file font yang dipilih dari SAF (Storage Access Framework)
     * @return [FontItem] jika berhasil dimuat, atau null jika gagal
     */
    fun loadFontFromUri(context: Context, uri: Uri): FontItem? {
        try {
            val fileName = queryFileName(context, uri) ?: "custom_font_${System.currentTimeMillis()}.ttf"
            val customFontsDir = File(context.filesDir, "custom_fonts").apply {
                if (!exists()) mkdirs()
            }

            val destFile = File(customFontsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val typeface = Typeface.createFromFile(destFile)
            val displayName = fileName.substringBeforeLast(".")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

            val fontItem = FontItem(
                name = displayName,
                category = "My Fonts",
                assetPath = null,
                filePath = destFile.absolutePath,
                typeface = typeface
            )

            // Tambahkan ke daftar font (posisikan setelah default agar mudah ditemukan)
            val existingIndex = fontList.indexOfFirst { it.name.equals(displayName, ignoreCase = true) }
            if (existingIndex != -1) {
                fontList[existingIndex] = fontItem
            } else {
                fontList.add(1, fontItem)
            }

            return fontItem
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun loadSavedCustomFonts(context: Context) {
        val customFontsDir = File(context.filesDir, "custom_fonts")
        if (customFontsDir.exists() && customFontsDir.isDirectory) {
            val files = customFontsDir.listFiles { file ->
                val name = file.name.lowercase()
                name.endsWith(".ttf") || name.endsWith(".otf")
            } ?: return

            for (file in files) {
                try {
                    val typeface = Typeface.createFromFile(file)
                    val displayName = file.name.substringBeforeLast(".")
                        .replace("_", " ")
                        .replace("-", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

                    fontList.add(
                        FontItem(
                            name = displayName,
                            category = "My Fonts",
                            assetPath = null,
                            filePath = file.absolutePath,
                            typeface = typeface
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    /**
     * Mengembalikan seluruh daftar font (termasuk font bawaan dan My Fonts).
     */
    fun getFonts(): List<FontItem> = fontList

    /**
     * Mengembalikan hanya font kustom pengguna (kategori "My Fonts").
     */
    fun getMyFonts(): List<FontItem> = fontList.filter { it.category == "My Fonts" }

    /**
     * Menambahkan font kustom langsung ke daftar font.
     */
    fun addCustomFont(fontItem: FontItem) {
        fontList.add(fontItem)
    }
}
