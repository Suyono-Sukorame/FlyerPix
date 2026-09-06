package com.flyerpix.editor.font

import android.graphics.Typeface

/**
 * Model item font untuk FontManager dan FontPickerAdapter.
 *
 * @param name Nama tampilan font (misal: "Roboto Bold", "Impact").
 * @param category Kategori gaya font ("Modern", "Serif", "Sans-Serif", "Hand-written", "Bold Display").
 * @param assetPath Path file font di dalam folder assets (jika berasal dari assets).
 * @param typeface Objek [Typeface] Android untuk merender teks.
 */
data class FontItem(
    val name: String,
    val category: String,
    val assetPath: String? = null,
    val filePath: String? = null,
    val typeface: Typeface
)
