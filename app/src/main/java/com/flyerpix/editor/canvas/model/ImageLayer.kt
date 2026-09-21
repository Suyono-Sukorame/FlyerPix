package com.flyerpix.editor.canvas.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import java.util.UUID

/**
 * Mode fade (soft edge / feathering) untuk [ImageLayer].
 *
 * Semua mode memudarkan piksel gambar ke transparan sehingga gambar menyatu
 * mulus dengan latar belakang (mis. foto masjid yang melebur ke kanvas).
 *
 * - [LINEAR_LEFT]          — memudar dari tepi kiri.
 * - [LINEAR_RIGHT]         — memudar dari tepi kanan.
 * - [LINEAR_TOP]           — memudar dari tepi atas.
 * - [LINEAR_BOTTOM]        — memudar dari tepi bawah.
 * - [ALL_EDGES_FEATHER]    — memudar mulus di keempat sisi sekaligus.
 * - [RADIAL]               — vignette radial/oval (tengah solid, tepi transparan).
 */
enum class ImageFadeType {
    LINEAR_LEFT,
    LINEAR_RIGHT,
    LINEAR_TOP,
    LINEAR_BOTTOM,
    ALL_EDGES_FEATHER,
    RADIAL
}

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
 *  - Soft Edge / Gradient Fade (fadeEnabled, fadeType, fadeIntensity, fadeCurve)
 */
open class ImageLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    /** Stretch horizontal non-uniform (1f = normal). Dipakai handle tengah-kiri/kanan. */
    override var stretchX: Float = 1f,
    /** Stretch vertikal non-uniform (1f = normal). Dipakai handle tengah-atas/bawah. */
    override var stretchY: Float = 1f,
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
    var strokeOpacity: Int = 255,
    // ── Soft Edge / Gradient Fade (Feathering) ──────────────────────────────
    /** Aktifkan pemudaran tepi gambar (soft edge). */
    var fadeEnabled: Boolean = false,
    /** Mode/arah pemudaran. Lihat [ImageFadeType]. */
    var fadeType: ImageFadeType = ImageFadeType.LINEAR_LEFT,
    /** Kedalaman pemudaran 0f..1f (fraksi dimensi yang dipakai untuk gradasi). */
    var fadeIntensity: Float = 0.5f,
    /** Eksponen kelengkungan falloff (>1 = makin lembut/gradual). */
    var fadeCurve: Float = 1f
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

    // ── Cache bitmap hasil feathering agar tidak dibangun ulang tiap frame ──
    private var fadedBitmapCache: Bitmap? = null
    private var fadedBitmapCacheKey: Int = 0

    /**
     * Mengembalikan bitmap yang dipakai untuk render: bitmap asli bila fade
     * nonaktif, atau salinan ter-mask (soft edge) bila [fadeEnabled] aktif.
     * Hasil di-cache dan hanya dibangun ulang saat bitmap/parameter berubah.
     */
    private fun resolveRenderBitmap(): Bitmap {
        if (!fadeEnabled || fadeIntensity <= 0.001f) return bitmap
        if (bitmap.isRecycled) return bitmap

        val key = fadeCacheKey()
        val cached = fadedBitmapCache
        if (cached != null && !cached.isRecycled && fadedBitmapCacheKey == key) return cached

        val faded = buildFadedBitmap(bitmap) ?: return bitmap
        fadedBitmapCache?.takeIf { it !== faded && !it.isRecycled }?.recycle()
        fadedBitmapCache = faded
        fadedBitmapCacheKey = key
        return faded
    }

    private fun fadeCacheKey(): Int {
        var h = System.identityHashCode(bitmap)
        h = h * 31 + bitmap.generationId
        h = h * 31 + fadeType.ordinal
        h = h * 31 + fadeIntensity.hashCode()
        h = h * 31 + fadeCurve.hashCode()
        return h
    }

    /**
     * Membangun salinan bitmap dengan tepi ter-feather menggunakan
     * [PorterDuff.Mode.DST_IN] dan gradasi alpha.
     */
    private fun buildFadedBitmap(src: Bitmap): Bitmap? {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return null
        return try {
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            c.drawBitmap(src, 0f, 0f, null)

            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            val intensity = fadeIntensity.coerceIn(0f, 1f)
            val curve = fadeCurve.coerceIn(0.2f, 4f)

            fun drawLinear(x0: Float, y0: Float, x1: Float, y1: Float) {
                maskPaint.shader = buildLinearFadeShader(x0, y0, x1, y1, curve)
                c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)
            }

            when (fadeType) {
                ImageFadeType.LINEAR_LEFT ->
                    drawLinear(0f, 0f, w * intensity, 0f)
                ImageFadeType.LINEAR_RIGHT ->
                    drawLinear(w.toFloat(), 0f, w * (1f - intensity), 0f)
                ImageFadeType.LINEAR_TOP ->
                    drawLinear(0f, 0f, 0f, h * intensity)
                ImageFadeType.LINEAR_BOTTOM ->
                    drawLinear(0f, h.toFloat(), 0f, h * (1f - intensity))
                ImageFadeType.ALL_EDGES_FEATHER -> {
                    drawLinear(0f, 0f, w * intensity, 0f)
                    drawLinear(w.toFloat(), 0f, w * (1f - intensity), 0f)
                    drawLinear(0f, 0f, 0f, h * intensity)
                    drawLinear(0f, h.toFloat(), 0f, h * (1f - intensity))
                }
                ImageFadeType.RADIAL -> {
                    maskPaint.shader = buildRadialFadeShader(w, h, intensity, curve)
                    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)
                }
            }
            maskPaint.shader = null
            c.setBitmap(null)
            out
        } catch (_: Throwable) {
            null
        }
    }

    /** Gradasi alpha 0 (tepi) → 255 (dalam) dengan kelengkungan [curve]. */
    private fun buildLinearFadeShader(
        x0: Float, y0: Float, x1: Float, y1: Float, curve: Float
    ): LinearGradient {
        val (colors, positions) = fadeStops(curve, fromOpaque = false)
        return LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
    }

    /** Gradasi radial: alpha 255 di pusat → 0 di tepi, dibatasi [intensity]. */
    private fun buildRadialFadeShader(w: Int, h: Int, intensity: Float, curve: Float): RadialGradient {
        val radius = kotlin.math.hypot(w / 2.0, h / 2.0).toFloat().coerceAtLeast(1f)
        val (colors, positions) = fadeStops(curve, fromOpaque = true)
        // Solid sampai (1 - intensity) radius, lalu memudar sampai tepi.
        val mapped = FloatArray(positions.size) { i ->
            (1f - intensity) + positions[i] * intensity
        }
        return RadialGradient(
            w / 2f, h / 2f, radius,
            colors, mapped, Shader.TileMode.CLAMP
        )
    }

    /**
     * Menghasilkan stop gradasi alpha halus (9 sampel) mengikuti eksponen [curve].
     * fromOpaque=true → alpha 255→0 (radial); false → alpha 0→255 (linear).
     */
    private fun fadeStops(curve: Float, fromOpaque: Boolean): Pair<IntArray, FloatArray> {
        val steps = 8
        val colors = IntArray(steps + 1)
        val positions = FloatArray(steps + 1)
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val shaped = Math.pow(t.toDouble(), curve.toDouble()).toFloat()
            val alpha = ((if (fromOpaque) 1f - shaped else shaped) * 255f).toInt().coerceIn(0, 255)
            colors[i] = alpha shl 24
            positions[i] = t
        }
        return colors to positions
    }

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

        // Bitmap render efektif: asli atau hasil soft-edge/feather (cached).
        val renderBitmap = resolveRenderBitmap()

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
                    c.drawBitmap(renderBitmap, 0f, 0f, p)
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
                    c.drawBitmap(renderBitmap, 0f, 0f, p)
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
                    c.drawBitmap(renderBitmap, 0f, 0f, p)
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
                    c.drawBitmap(renderBitmap, 0f, 0f, p)
                },
                drawContentEmboss = { c, p ->
                    c.drawBitmap(renderBitmap, 0f, 0f, p)
                }
            )
        } else {
            canvas.drawBitmap(renderBitmap, 0f, 0f, paint)
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
                    c.drawBitmap(renderBitmap, 0f, 0f, p)
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
        h = h * 31 + (if (fadeEnabled) 1 else 0)
        h = h * 31 + fadeType.ordinal
        h = h * 31 + fadeIntensity.hashCode()
        h = h * 31 + fadeCurve.hashCode()
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
            layerName = layerName,
            fadeEnabled = fadeEnabled,
            fadeType = fadeType,
            fadeIntensity = fadeIntensity,
            fadeCurve = fadeCurve
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
