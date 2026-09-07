package com.flyerpix.editor.canvas.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import java.util.UUID

/**
 * Representasi layer gambar / bitmap pada kanvas PixelLab.
 * Digunakan untuk layer gambar eksternal serta hasil penggabungan multi-layer (*Merge Layers*) (Prompt 36).
 *
 * Mendukung seluruh transformasi standar [CanvasLayer]:
 *  - Posisi (x, y)
 *  - Skala (scale)
 *  - Rotasi (rotation)
 *  - Opasitas (opacity 0..255)
 *  - Kunci (isLocked) & Visibilitas (isVisible)
 *  - Blending Mode (blendMode)
 *  - Transformasi Perspektif 3D warping (perspectiveEnabled & perspectiveCorners)
 */
open class ImageLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    override var perspectiveEnabled: Boolean = false,
    override var perspectiveCorners: FloatArray = floatArrayOf(
        0f, 0f,  // Top-Left
        1f, 0f,  // Top-Right
        1f, 1f,  // Bottom-Right
        0f, 1f   // Bottom-Left
    ),
    override var blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC_OVER,
    var bitmap: Bitmap,
    var layerName: String = "Image Layer"
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

    override fun draw(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        val saveCount = canvas.save()

        // 1. Transformasi layer luar (Posisi, Skala, Rotasi berpusat pada titik tengah layer)
        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(x, y)
        canvas.scale(scale, scale, cx, cy)
        canvas.rotate(rotation, cx, cy)

        // 2. Transformasi perspektif (jika diaktifkan)
        val pMat = getPerspectiveMatrix(w, h)
        if (pMat != null) {
            canvas.concat(pMat)
        }

        // 3. Konfigurasi opasitas dan penggambaran bitmap
        paint.alpha = opacity.coerceIn(0, 255)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        canvas.restoreToCount(saveCount)
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

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        val width = try {
            bitmap.width.toFloat()
        } catch (_: Throwable) {
            0f
        }
        val height = try {
            bitmap.height.toFloat()
        } catch (_: Throwable) {
            0f
        }
        return Pair(width, height)
    }

    override fun contentBlurSignature(): Int {
        var h = 1
        h = h * 31 + id.hashCode()
        h = h * 31 + x.hashCode()
        h = h * 31 + y.hashCode()
        h = h * 31 + scale.hashCode()
        h = h * 31 + rotation.hashCode()
        h = h * 31 + opacity
        h = h * 31 + (if (isVisible) 1 else 0)
        h = h * 31 + (if (isLocked) 1 else 0)
        h = h * 31 + blendMode.ordinal
        h = h * 31 + (if (perspectiveEnabled) 1 else 0)
        for (c in perspectiveCorners) h = h * 31 + c.hashCode()
        h = h * 31 + System.identityHashCode(bitmap)
        h = h * 31 + bitmap.generationId
        h = h * 31 + bitmap.width
        h = h * 31 + bitmap.height
        return h
    }

    override fun copyLayer(): ImageLayer {
        return ImageLayer(
            id = UUID.randomUUID().toString(),
            x = x,
            y = y,
            scale = scale,
            rotation = rotation,
            opacity = opacity,
            isLocked = isLocked,
            isVisible = isVisible,
            perspectiveEnabled = perspectiveEnabled,
            perspectiveCorners = perspectiveCorners.clone(),
            blendMode = blendMode,
            bitmap = bitmap,
            layerName = layerName
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageLayer
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
