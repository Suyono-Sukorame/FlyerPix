package com.flyerpix.editor.canvas.model

import com.flyerpix.editor.nativepix.FpNative

/**
 * Utilitas untuk generasi dan manipulasi mask layer.
 * Digunakan oleh Mask Editor (Prompt 06/07).
 */
object MaskUtils {

    /** Direction codes untuk gradient linear. */
    const val DIR_TOP_BOTTOM = 0
    const val DIR_BOTTOM_TOP = 1
    const val DIR_LEFT_RIGHT = 2
    const val DIR_RIGHT_LEFT = 3

    /** Smoothstep: interpolasi cubic Hermite yang menghasilkan transisi lebih halus dari linear. */
    fun smoothstep(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    /**
     * Generate gradient mask pada [bitmap] yang sudah ada.
     * Bitmap harus berukuran sesuai mask layer (maskBitmap).
     *
     * @param type      GradientType.LINEAR atau GradientType.RADIAL
     * @param direction Arah gradient (hanya untuk LINEAR): DIR_TOP_BOTTOM / DIR_BOTTOM_TOP / DIR_LEFT_RIGHT / DIR_RIGHT_LEFT
     * @param radius    Radius relatif (0..1) untuk radial; diabaikan untuk linear
     */
    fun generateGradientMask(
        bitmap: android.graphics.Bitmap,
        type: GradientType,
        direction: Int = DIR_TOP_BOTTOM,
        radius: Float = 0.5f
    ) {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val t = when (type) {
                    GradientType.LINEAR -> when (direction) {
                        DIR_TOP_BOTTOM -> smoothstep(y.toFloat() / h)
                        DIR_BOTTOM_TOP -> smoothstep(1f - y.toFloat() / h)
                        DIR_LEFT_RIGHT -> smoothstep(x.toFloat() / w)
                        DIR_RIGHT_LEFT -> smoothstep(1f - x.toFloat() / w)
                        else -> smoothstep(y.toFloat() / h)
                    }
                    GradientType.RADIAL -> {
                        val cx = w / 2f
                        val cy = h / 2f
                        val maxR = (kotlin.math.min(w, h) / 2f) * radius.coerceIn(0.01f, 1f)
                        val dx = x - cx
                        val dy = y - cy
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        smoothstep(1f - dist / maxR)
                    }
                    else -> 0f
                }
                val alpha = (t * 255).toInt().coerceIn(0, 255)
                val idx = y * w + x
                pixels[idx] = (alpha shl 24) or (pixels[idx] and 0x00FFFFFF)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * Feather (gaussian blur) pada mask bitmap.
     * Blur diterapkan ke seluruh saluran — untuk mask, hanya alpha yang bermakna;
     * saluran RGB tetap 0 dan tidak mempengaruhi hasil.
     *
     * @param bitmap  Mask bitmap yang akan di-blur
     * @param radius  Radius blur dalam piksel mask (0 = tidak ada blur)
     */
    fun featherMask(bitmap: android.graphics.Bitmap, radius: Int) {
        if (radius <= 0) return
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        FpNative.blurPixels(pixels, w, h, radius.coerceAtLeast(1))
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /** Generate smoothstep test values (monotonik). */
    fun smoothstepValues(count: Int): List<Float> =
        (0 until count).map { i -> smoothstep(i.toFloat() / (count - 1).coerceAtLeast(1)) }
}
