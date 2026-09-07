package com.flyerpix.editor.canvas.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import java.util.UUID

/**
 * Kategori stiker/emoji pada picker.
 */
enum class StickerCategory(val label: String) {
    SMILEYS("😊"),
    ANIMALS("🐶"),
    FOOD("🍔"),
    ACTIVITIES("⚽"),
    TRAVEL("🚗"),
    OBJECTS("💡"),
    SYMBOLS("❤️")
}

/**
 * Representasi satu item stiker/emoji pada picker.
 */
data class StickerItem(
    val emoji: String,
    val label: String,
    val category: StickerCategory
)

/**
 * Layer stiker pada kanvas PixelLab.
 *
 * Menyimpan representasi bitmap dari stiker atau emoji dan menggambar
 * layaknya [ImageLayer] — dengan seluruh transformasi standar [CanvasLayer]
 * (posisi, skala, rotasi, opasitas, perspektif warping).
 *
 * **Cara pembuatan:**
 *  Gunakan companion [fromEmoji] atau [fromBitmap] untuk membuat instance.
 *
 * @property stickerBitmap Bitmap hasil rendering stiker/emoji.
 * @property stickerName   Nama tampilan stiker (untuk layer manager).
 */
data class StickerLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    var stickerBitmap: Bitmap,
    var stickerName: String = "Sticker",
    // ── Perspective Warping ────────────────────────────────────────────────
    override var perspectiveEnabled: Boolean = false,
    override var perspectiveCorners: FloatArray = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        0f, 1f
    ),
    // ── Blending Mode ──────────────────────────────────────────────────────
    override var blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC_OVER
) : CanvasLayer(
    id = id,
    x = x,
    y = y,
    scale = scale,
    rotation = rotation,
    opacity = opacity,
    isLocked = isLocked,
    isVisible = isVisible,
    perspectiveEnabled = perspectiveEnabled,
    perspectiveCorners = perspectiveCorners,
    blendMode = blendMode
) {

    companion object {
        private const val DEFAULT_BITMAP_SIZE = 128

        /**
         * Membuat [StickerLayer] dari teks emoji.
         *
         * Bitmap di-render menggunakan software [Canvas] dengan ukuran [size] piksel,
         * sehingga emoji dirasterisasi secara independen dari hardware acceleration.
         */
        fun fromEmoji(emoji: String, size: Int = DEFAULT_BITMAP_SIZE): StickerLayer {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size * 0.72f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
            }

            val xPos = size / 2f
            val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(emoji, xPos, yPos, paint)

            return StickerLayer(
                stickerBitmap = bmp,
                stickerName = emoji
            )
        }

        /**
         * Membuat [StickerLayer] dari [Bitmap] yang sudah ada.
         */
        fun fromBitmap(bitmap: Bitmap, name: String = "Sticker"): StickerLayer {
            return StickerLayer(
                stickerBitmap = bitmap,
                stickerName = name
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render Pipeline
    // ─────────────────────────────────────────────────────────────────────────

    override fun draw(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        val saveCount = canvas.save()

        // 1. Transformasi layer luar (Posisi, Skala, Rotasi berpusat pada titik tengah)
        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(x, y)
        canvas.scale(scale, scale, cx, cy)
        canvas.rotate(rotation, cx, cy)

        // 2. Transformasi perspektif
        val pMat = getPerspectiveMatrix(w, h)
        if (pMat != null) {
            canvas.concat(pMat)
        }

        // 3. Gambar bitmap stiker
        paint.alpha = opacity.coerceIn(0, 255)
        canvas.drawBitmap(stickerBitmap, 0f, 0f, paint)

        canvas.restoreToCount(saveCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bounds & Copy
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        return try {
            Pair(stickerBitmap.width.toFloat(), stickerBitmap.height.toFloat())
        } catch (_: Throwable) {
            Pair(0f, 0f)
        }
    }

    override fun contentBlurSignature(): Int {
        var h = hashCode()
        h = h * 31 + System.identityHashCode(stickerBitmap)
        h = h * 31 + stickerBitmap.generationId
        return h
    }

    override fun getBounds(): RectF {
        val pts = getSelectionBoxPoints(0f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0..3) {
            val px = pts[i * 2]
            val py = pts[i * 2 + 1]
            if (px < minX) minX = px
            if (py < minY) minY = py
            if (px > maxX) maxX = px
            if (py > maxY) maxY = py
        }
        return RectF(minX, minY, maxX, maxY)
    }

    override fun copyLayer(): StickerLayer {
        val clonedBitmap = stickerBitmap.copy(stickerBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        return StickerLayer(
            id = UUID.randomUUID().toString(),
            x = this.x + 30f,
            y = this.y + 30f,
            scale = this.scale,
            rotation = this.rotation,
            opacity = this.opacity,
            isLocked = this.isLocked,
            isVisible = this.isVisible,
            stickerBitmap = clonedBitmap,
            stickerName = this.stickerName,
            perspectiveEnabled = this.perspectiveEnabled,
            perspectiveCorners = this.perspectiveCorners.clone(),
            blendMode = this.blendMode
        )
    }
}
