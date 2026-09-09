package com.flyerpix.editor.canvas.model

import android.graphics.Bitmap

/**
 * Format berkas gambar yang didukung saat mengekspor kanvas.
 */
enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val compressFormat: Bitmap.CompressFormat,
    val displayName: String
) {
    PNG("png", "image/png", Bitmap.CompressFormat.PNG, "PNG (Supports Transparency)"),
    JPEG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG, "JPEG (Light Compression)")
}

/**
 * Preset tingkat kualitas dan resolusi ekspor off-screen kanvas (Prompt 49).
 *
 * Mendukung rendering mulai dari resolusi asli kanvas (1.0x) hingga
 * resolusi Ultra HD / 4K (4.0x, misal 3840x2160 jika rasio kanvas 16:9).
 */
enum class ExportQuality(
    val displayName: String,
    val scaleMultiplier: Float,
    val description: String
) {
    DEFAULT("Default", 1.0f, "Original Canvas Resolution"),
    LOW("Low (0.5x)", 0.5f, "Small file size"),
    MEDIUM("Medium (0.75x)", 0.75f, "Standard quality"),
    HIGH("High (1.5x)", 1.5f, "Sharp high quality"),
    VERY_HIGH("Very High (2.0x)", 2.0f, "Very sharp quality (2K)"),
    ULTRA_HD("Ultra HD / 4K (4.0x)", 4.0f, "Maximum Ultra HD 4K");

    /**
     * Menghitung lebar dan tinggi resolusi target berdasarkan resolusi logika kanvas saat ini.
     * Batas minimum 50px dan maksimum 8192px untuk menjaga batas alokasi GPU/Bitmap.
     */
    fun calculateDimensions(canvasWidth: Int, canvasHeight: Int): Pair<Int, Int> {
        val w = (canvasWidth * scaleMultiplier).toInt().coerceIn(50, 8192)
        val h = (canvasHeight * scaleMultiplier).toInt().coerceIn(50, 8192)
        return Pair(w, h)
    }

    companion object {
        fun fromOrdinal(ordinal: Int): ExportQuality {
            val values = values()
            return if (ordinal in values.indices) values[ordinal] else DEFAULT
        }
    }
}
