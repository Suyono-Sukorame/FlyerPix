package com.flyerpix.editor.canvas.model

import com.flyerpix.editor.filter.FilterEngine

/**
 * Adjustment warna non-destruktif per-layer (Prompt 03).
 *
 * Seluruh nilai memakai konvensi UI percent dalam rentang -100..100 (netral di 0),
 * identik dengan skala extended adjustment global Prompt 02. Saat di-render,
 * nilai dipetakan ke pipeline native [-1..1] (GAMMA dimetakan ke faktor 0.1..4
 * dengan netral di 1.0) lewat [applyTo].
 */
data class LayerAdjustments(
    var exposure: Float = 0f,
    var highlights: Float = 0f,
    var shadows: Float = 0f,
    var temperature: Float = 0f,
    var tint: Float = 0f,
    var gamma: Float = 0f,
    var vibrance: Float = 0f,
    var hue: Float = 0f
) {

    /** True bila setidaknya satu parameter tidak netral → perlu di-render terpisah. */
    val isActive: Boolean
        get() = exposure != 0f || highlights != 0f || shadows != 0f ||
            temperature != 0f || tint != 0f || gamma != 0f ||
            vibrance != 0f || hue != 0f

    /**
     * Mengeksekusi pipeline color adjustment pada piksel [pixels] (in-place).
     * Dipanggil hanya saat [isActive]; parameter yang tidak dipakai layer
     * (brightness/contrast/saturation) dikirim netral.
     */
    fun applyTo(pixels: IntArray, width: Int, height: Int) {
        if (!isActive || pixels.isEmpty()) return
        runCatching {
            FilterEngine.applyColorAdjustPixels(
                pixels, width, height,
                0f, 0f, 0f, hue,
                exposure / 100f, highlights / 100f, shadows / 100f,
                temperature / 100f, tint / 100f,
                (1f + gamma / 100f * 3f).coerceIn(0.1f, 4f),
                vibrance / 100f
            )
        }
    }
}