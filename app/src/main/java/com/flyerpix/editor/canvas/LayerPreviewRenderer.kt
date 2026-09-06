package com.flyerpix.editor.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.flyerpix.editor.canvas.model.CanvasLayer
import kotlin.math.ceil
import kotlin.math.min

/**
 * Merender pratinjau (thumbnail) sebuah [CanvasLayer] ke bitmap kecil,
 * menggunakan pola offscreen yang sama seperti merge layer.
 */
object LayerPreviewRenderer {

    const val THUMB_SIZE = 96

    fun render(layer: CanvasLayer): Bitmap? {
        val bounds = try {
            layer.getBounds()
        } catch (_: Throwable) {
            null
        } ?: return null
        val bw = ceil(bounds.width()).toInt()
        val bh = ceil(bounds.height()).toInt()
        if (bw <= 0 || bh <= 0) return null

        val scale = min(
            THUMB_SIZE.toFloat() / bw,
            THUMB_SIZE.toFloat() / bh
        )

        val bitmap = try {
            Bitmap.createBitmap(THUMB_SIZE, THUMB_SIZE, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            return null
        }

        try {
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left * scale, -bounds.top * scale)
            canvas.scale(scale, scale)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            layer.draw(canvas, paint)
        } catch (_: Throwable) {
            // Thumbnail kosong bila layer tidak bisa dirender
        }
        return bitmap
    }
}