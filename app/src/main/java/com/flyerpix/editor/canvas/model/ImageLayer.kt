package com.flyerpix.editor.canvas.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    /** Stretch horizontal non-uniform (1f = normal). Dipakai handle tengah-kanan. */
    var stretchX: Float = 1f,
    /** Stretch vertikal non-uniform (1f = normal). Dipakai handle tengah-bawah. */
    var stretchY: Float = 1f,
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
    var layerName: String = "Image Layer",
    // Fill & Stroke properties (for color/stroke effects on image)
    var fillColor: Int = Color.WHITE,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 0f,
    var strokeOpacity: Int = 255
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
        /**
         * Factory method untuk membuat ImageLayer yang centered di canvas
         * (horizontal & vertical center).
         * 
         * @param bitmap Bitmap yang akan ditampilkan
         * @param canvasWidth Lebar kanvas (untuk kalkulasi center)
         * @param canvasHeight Tinggi kanvas (untuk kalkulasi center)
         * @return ImageLayer dengan positioning centered
         */
        fun createCentered(
            bitmap: Bitmap,
            canvasWidth: Float,
            canvasHeight: Float,
            layerName: String = "Image Layer"
        ): ImageLayer {
            val imgWidth = bitmap.width.toFloat()
            val imgHeight = bitmap.height.toFloat()
            
            // Calculate center position
            val centerX = (canvasWidth - imgWidth) / 2f
            val centerY = (canvasHeight - imgHeight) / 2f
            
            return ImageLayer(
                x = centerX,
                y = centerY,
                bitmap = bitmap,
                layerName = layerName
            )
        }
    }

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        val saveCount = canvas.save()

        // 1. Transformasi layer luar (Posisi, Skala, Rotasi berpusat pada titik tengah layer)
        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(x, y)
        canvas.scale(scale * stretchX, scale * stretchY, cx, cy)
        canvas.rotate(rotation, cx, cy)

        // 2. Transformasi perspektif (jika diaktifkan)
        val pMat = getPerspectiveMatrix(w, h)
        if (pMat != null) {
            canvas.concat(pMat)
        }

        // 3. Konfigurasi opasitas dan penggambaran bitmap dengan shadow/neon/emboss/inner shadow
        paint.alpha = opacity.coerceIn(0, 255)

        // 3D Extrusion (FIRST — provides depth base)
        if (extrudeEnabled && extrudeDepth > 0) {
            com.flyerpix.editor.canvas.renderer.EffectRenderUtils.draw3DExtrusionEffect(
                canvas,
                this,
                w,
                h,
                0xFF1769FF.toInt(),
                drawContent = { c, p ->
                    c.drawBitmap(bitmap, 0f, 0f, p)
                }
            )
        } else if (shadowEnabled && shadowRadius > 0f) {
            com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawDropShadowEffect(
                canvas,
                this,
                w,
                h,
                shadowColor,
                drawContent = { c, p ->
                    p.alpha = opacity.coerceIn(0, 255)
                    c.drawBitmap(bitmap, 0f, 0f, p)
                }
            )
        } else if (neonEnabled) {
            com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawNeonEffect(
                canvas,
                this,
                w,
                h,
                drawContent = { c, p ->
                    p.alpha = opacity.coerceIn(0, 255)
                    c.drawBitmap(bitmap, 0f, 0f, p)
                }
            )
        } else if (embossEnabled) {
            com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawEmbossEffect(
                canvas,
                this,
                w,
                h,
                0xFF1769FF.toInt(), // Default color jika tidak ada texture/gradient
                drawContentBase = { c, p ->
                    p.alpha = opacity.coerceIn(0, 255)
                    c.drawBitmap(bitmap, 0f, 0f, p)
                },
                drawContentEmboss = { c, p ->
                    c.drawBitmap(bitmap, 0f, 0f, p)
                }
            )
        } else {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        // Handle inner shadow (applies after main content)
        if (innerShadowEnabled && innerShadowRadius > 0f) {
            com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawInnerShadowEffect(
                canvas,
                this,
                w,
                h,
                0xFF1769FF.toInt(),
                drawContent = { c, p ->
                    c.drawBitmap(bitmap, 0f, 0f, p)
                }
            )
        }

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

    private fun effectiveScaleX(): Float = if (scale * stretchX != 0f) scale * stretchX else 1f

    private fun effectiveScaleY(): Float = if (scale * stretchY != 0f) scale * stretchY else 1f

    override fun getSelectionBoxPoints(padding: Float): FloatArray {
        val (w, h) = getUnwarpedDimensions()
        val localPts = floatArrayOf(
            -padding, -padding,        // Top-Left
            w + padding, -padding,     // Top-Right
            w + padding, h + padding,  // Bottom-Right
            -padding, h + padding      // Bottom-Left
        )

        val sx = effectiveScaleX()
        val sy = effectiveScaleY()
        val rad = Math.toRadians(rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val cx = w / 2f
        val cy = h / 2f

        val result = FloatArray(8)
        for (i in 0..3) {
            val ox = (localPts[i * 2] - cx) * sx
            val oy = (localPts[i * 2 + 1] - cy) * sy
            result[i * 2] = ox * cos - oy * sin + cx + x
            result[i * 2 + 1] = ox * sin + oy * cos + cy + y
        }
        return result
    }

    override fun containsCanvasPoint(px: Float, py: Float): Boolean {
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return false

        val cx = x + w / 2f
        val cy = y + h / 2f
        val dx = px - cx
        val dy = py - cy

        val rad = Math.toRadians(-rotation.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val unrotX = dx * cos - dy * sin
        val unrotY = dx * sin + dy * cos

        val localX = unrotX / effectiveScaleX() + w / 2f
        val localY = unrotY / effectiveScaleY() + h / 2f

        return localX in 0f..w && localY in 0f..h
    }

    override fun getLayerTransformMatrix(w: Float, h: Float): Matrix {
        val matrix = Matrix()
        matrix.postTranslate(x, y)
        matrix.postScale(effectiveScaleX(), effectiveScaleY(), x + w / 2f, y + h / 2f)
        matrix.postRotate(rotation, x + w / 2f, y + h / 2f)
        return matrix
    }

    override fun contentBlurSignature(): Int {
        var h = 1
        h = h * 31 + id.hashCode()
        h = h * 31 + x.hashCode()
        h = h * 31 + y.hashCode()
        h = h * 31 + scale.hashCode()
        h = h * 31 + stretchX.hashCode()
        h = h * 31 + stretchY.hashCode()
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
        h = h * 31 + (if (adjustmentsEnabled) 1 else 0)
        h = h * 31 + adjustments.hashCode()
        return maskBlurSignature(h)
    }

    override fun copyLayer(): ImageLayer {
        return ImageLayer(
            id = UUID.randomUUID().toString(),
            x = x,
            y = y,
            scale = scale,
            stretchX = stretchX,
            stretchY = stretchY,
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
