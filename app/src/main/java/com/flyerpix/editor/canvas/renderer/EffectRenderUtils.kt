package com.flyerpix.editor.canvas.renderer

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.EmbossMaskFilter
import android.graphics.Paint

/**
 * Utility class untuk shared effect rendering methods.
 * 
 * Berisi helper functions untuk offscreen bitmap rendering, blur effects, emboss effects,
 * dan rendering utilities lainnya yang digunakan oleh multiple layer types.
 * 
 * Created: Prompt 1 - Architecture Foundation
 * Purpose: Centralize effect rendering logic untuk avoid code duplication
 */
object EffectRenderUtils {

    /**
     * Membuat offscreen bitmap dengan konfigurasi optimal untuk effect rendering.
     * 
     * @param width Lebar bitmap dalam pixels
     * @param height Tinggi bitmap dalam pixels
     * @param config Bitmap configuration (default: ARGB_8888 untuk full quality)
     * @return Bitmap baru yang siap digunakan untuk rendering
     */
    fun createOffscreenBitmap(
        width: Int, 
        height: Int, 
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Bitmap {
        // Pastikan dimensi valid (minimum 1x1)
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        
        return Bitmap.createBitmap(w, h, config)
    }

    /**
     * Recycle offscreen bitmap dengan aman untuk free memory.
     * 
     * @param bitmap Bitmap yang akan di-recycle (nullable)
     */
    fun recycleOffscreenBitmap(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    /**
     * Apply BlurMaskFilter ke Paint untuk blur effects (neon, glow, shadows).
     * 
     * PENTING: BlurMaskFilter hanya bekerja di software-rendered canvas (offscreen bitmap).
     * Tidak bekerja di hardware-accelerated canvas view.
     * 
     * @param paint Paint object yang akan di-apply blur
     * @param radius Blur radius (1-40 pixels)
     * @param blurType Tipe blur (NORMAL, SOLID, OUTER, INNER)
     */
    fun applyBlurMaskFilter(
        paint: Paint, 
        radius: Float, 
        blurType: BlurMaskFilter.Blur = BlurMaskFilter.Blur.NORMAL
    ) {
        val clampedRadius = radius.coerceIn(0.1f, 100f)
        paint.maskFilter = BlurMaskFilter(clampedRadius, blurType)
    }

    /**
     * Apply EmbossMaskFilter ke Paint untuk emboss/bevel effects.
     * 
     * PENTING: EmbossMaskFilter (deprecated API) hanya bekerja di software-rendered canvas.
     * Tidak bekerja di hardware-accelerated canvas view.
     * 
     * @param paint Paint object yang akan di-apply emboss
     * @param lightDirection Array [x, y, z] untuk arah cahaya (normalized)
     * @param ambient Cahaya ambient (0.0 - 1.0)
     * @param specular Kilap specular (0 - 20+)
     * @param blurRadius Ketebalan bevel (0.5 - 12)
     */
    @Suppress("DEPRECATION")
    fun applyEmbossMaskFilter(
        paint: Paint,
        lightDirection: FloatArray,
        ambient: Float,
        specular: Float,
        blurRadius: Float
    ) {
        val clampedAmbient = ambient.coerceIn(0f, 1f)
        val clampedSpecular = specular.coerceAtLeast(0.1f)
        val clampedRadius = blurRadius.coerceIn(0.5f, 12f)
        
        paint.maskFilter = EmbossMaskFilter(
            lightDirection,
            clampedAmbient,
            clampedSpecular,
            clampedRadius
        )
    }

    /**
     * Execute drawing operation di offscreen bitmap dan return hasil bitmap.
     * 
     * Pattern ini digunakan untuk effects yang memerlukan software rendering:
     * - Emboss (EmbossMaskFilter)
     * - Neon/Glow (BlurMaskFilter)
     * - Inner Shadow (PorterDuff masking)
     * 
     * @param width Lebar offscreen canvas
     * @param height Tinggi offscreen canvas
     * @param onDraw Lambda yang menerima Canvas untuk drawing operations
     * @return Bitmap hasil rendering
     * 
     * USAGE:
     * ```kotlin
     * val resultBitmap = drawWithOffscreenCanvas(width, height) { canvas ->
     *     // Your drawing code here
     *     canvas.drawPath(path, paint)
     * }
     * ```
     */
    fun drawWithOffscreenCanvas(
        width: Int,
        height: Int,
        onDraw: (Canvas) -> Unit
    ): Bitmap {
        val bitmap = createOffscreenBitmap(width, height)
        val canvas = Canvas(bitmap)
        onDraw(canvas)
        return bitmap
    }

    /**
     * Helper untuk membuat Paint dengan konfigurasi anti-alias dan filter bitmap.
     * Cocok untuk high-quality rendering.
     * 
     * @param flags Additional paint flags (default: ANTI_ALIAS + FILTER_BITMAP)
     * @return Paint object yang siap digunakan
     */
    fun createQualityPaint(flags: Int = Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG): Paint {
        return Paint(flags)
    }

    /**
     * Clear mask filter dari Paint object.
     * Digunakan setelah effect rendering untuk reset paint ke normal state.
     * 
     * @param paint Paint object yang akan di-clear
     */
    fun clearMaskFilter(paint: Paint) {
        paint.maskFilter = null
    }

    /**
     * Clear shadow layer dari Paint object.
     * Digunakan setelah drop shadow rendering untuk reset paint ke normal state.
     * 
     * @param paint Paint object yang akan di-clear
     */
    fun clearShadowLayer(paint: Paint) {
        paint.clearShadowLayer()
    }

    /**
     * Hitung padding yang dibutuhkan untuk effect rendering berdasarkan effect parameters.
     * Digunakan untuk sizing offscreen bitmap agar tidak crop effects.
     * 
     * @param embossBevel Emboss bevel thickness
     * @param neonRadius Neon glow radius
     * @param shadowRadius Shadow blur radius
     * @param shadowDx Shadow offset X
     * @param shadowDy Shadow offset Y
     * @return Padding dalam pixels (integer)
     */
    fun calculateEffectPadding(
        embossBevel: Float = 0f,
        neonRadius: Float = 0f,
        shadowRadius: Float = 0f,
        shadowDx: Float = 0f,
        shadowDy: Float = 0f
    ): Int {
        val embossPad = (embossBevel * 2 + 4).toInt()
        val neonPad = (neonRadius + 8).toInt()
        val shadowPad = (shadowRadius + kotlin.math.abs(shadowDx).coerceAtLeast(kotlin.math.abs(shadowDy)) + 4).toInt()
        
        return maxOf(embossPad, neonPad, shadowPad, 0)
    }

    /**
     * Generic emboss effect renderer untuk semua layer types.
     * 
     * Algoritma:
     * 1. Draw base content solid
     * 2. Create offscreen bitmap dengan padding
     * 3. Calculate light direction dari embossLightAngle
     * 4. Apply embossIntensity scaling ke ambient/specular
     * 5. Render content dengan EmbossMaskFilter ke offscreen
     * 6. Blit hasil ke main canvas
     * 7. Recycle bitmap
     * 
     * PENTING: EmbossMaskFilter hanya bekerja di software canvas, bukan hardware-accelerated.
     * 
     * @param canvas Target canvas (hardware-accelerated)
     * @param layer CanvasLayer dengan emboss properties
     * @param contentWidth Width dari content dalam pixels
     * @param contentHeight Height dari content dalam pixels
     * @param contentColor Color untuk emboss rendering (fallback jika no texture/gradient)
     * @param drawContentBase Lambda untuk render base content (solid)
     * @param drawContentEmboss Lambda untuk render content dengan emboss effect
     * 
     * USAGE:
     * ```kotlin
     * EffectRenderUtils.drawEmbossEffect(canvas, layer, width, height, fillColor,
     *     drawContentBase = { c, p ->
     *         p.color = fillColor
     *         c.drawPath(path, p)
     *     },
     *     drawContentEmboss = { c, p ->
     *         c.drawPath(path, p)
     *     }
     * )
     * ```
     */
    fun drawEmbossEffect(
        canvas: Canvas,
        layer: com.flyerpix.editor.canvas.model.CanvasLayer,
        contentWidth: Float,
        contentHeight: Float,
        contentColor: Int,
        drawContentBase: (Canvas, Paint) -> Unit,
        drawContentEmboss: (Canvas, Paint) -> Unit
    ) {
        if (!layer.embossEnabled || contentWidth <= 0f || contentHeight <= 0f) {
            return
        }

        val basePaint = createQualityPaint()
        
        // 1. Draw base content solid (crisp base untuk avoid glyph disappear)
        basePaint.color = contentColor
        basePaint.alpha = layer.opacity.coerceIn(0, 255)
        drawContentBase(canvas, basePaint)

        // 2. Calculate offscreen bitmap size dengan padding
        val bevel = layer.embossBevel.coerceIn(0.5f, 12f)
        val pad = (bevel * 2 + 4).toInt()
        val bw = contentWidth.toInt() + pad * 2
        val bh = contentHeight.toInt() + pad * 2

        if (bw <= 0 || bh <= 0) return

        // 3. Create offscreen bitmap untuk EmbossMaskFilter
        val bmp = createOffscreenBitmap(bw, bh)
        val bmpCanvas = Canvas(bmp)  // Software-rendered canvas

        // 4. Calculate light direction dari angle
        val rad = Math.toRadians(layer.embossLightAngle.toDouble()).toFloat()
        val lightDir = floatArrayOf(
            kotlin.math.cos(rad),
            kotlin.math.sin(rad),
            0.5f
        )

        // 5. Apply embossIntensity untuk scale ambient/specular
        val intensity = layer.embossIntensity.coerceIn(0f, 2.5f)
        val ambient = (layer.embossAmbient.coerceIn(0f, 1f) / (1f + intensity * 0.5f)).coerceIn(0f, 1f)
        val specular = layer.embossSpecular.coerceAtLeast(0.1f) * (0.6f + intensity)

        // 6. Create emboss paint dengan EmbossMaskFilter
        val embossPaint = createQualityPaint().apply {
            color = contentColor
            alpha = (layer.opacity.coerceIn(0, 255) * 0.72f).toInt()
            @Suppress("DEPRECATION")
            val embossFilter = EmbossMaskFilter(
                lightDir,
                ambient,
                specular,
                bevel
            )
            maskFilter = embossFilter
        }

        // 7. Render content dengan emboss ke offscreen
        bmpCanvas.translate(pad.toFloat(), pad.toFloat())
        drawContentEmboss(bmpCanvas, embossPaint)

        // 8. Blit offscreen result ke main canvas
        val blitPaint = createQualityPaint()
        canvas.drawBitmap(
            bmp,
            (-pad).toFloat(),
            (-pad).toFloat(),
            blitPaint
        )

        // 9. Cleanup
        clearMaskFilter(embossPaint)
        recycleOffscreenBitmap(bmp)
    }

    /**
     * Generic neon/glow effect renderer untuk semua layer types.
     * 
     * Algoritma:
     * 1. Draw base content dengan neon color
     * 2. Create multiple glow layers (backer blur passes) dengan decreasing alpha
     * 3. Stack glow layers dari outer (blurred) ke inner (crisp)
     * 4. Result: Vibrant neon/glow effect dengan intensity control
     * 
     * PENTING: BlurMaskFilter hanya bekerja di software canvas, bukan hardware-accelerated.
     * 
     * @param canvas Target canvas (hardware-accelerated)
     * @param layer CanvasLayer dengan neon properties (neonEnabled, neonColor, neonIntensity, neonRadius)
     * @param contentWidth Width dari content dalam pixels
     * @param contentHeight Height dari content dalam pixels
     * @param drawContent Lambda untuk render content (akan di-draw multiple times untuk glow effect)
     * 
     * USAGE:
     * ```kotlin
     * EffectRenderUtils.drawNeonEffect(canvas, layer, width, height,
     *     drawContent = { c, p ->
     *         p.color = neonColor
     *         c.drawPath(path, p)
     *     }
     * )
     * ```
     */
    fun drawNeonEffect(
        canvas: Canvas,
        layer: com.flyerpix.editor.canvas.model.CanvasLayer,
        contentWidth: Float,
        contentHeight: Float,
        drawContent: (Canvas, Paint) -> Unit
    ) {
        if (!layer.neonEnabled || contentWidth <= 0f || contentHeight <= 0f) {
            return
        }

        val neonColor = layer.neonColor
        val neonIntensity = layer.neonIntensity.coerceIn(0.5f, 3f)
        val neonRadius = layer.neonRadius.coerceIn(2f, 50f)
        val opacity = layer.opacity.coerceIn(0, 255)

        // 1. Calculate offscreen bitmap size dengan padding
        val pad = (neonRadius + 8).toInt().coerceAtLeast(16)
        val bw = contentWidth.toInt() + pad * 2
        val bh = contentHeight.toInt() + pad * 2

        if (bw <= 0 || bh <= 0) return

        // 2. Create paint untuk neon rendering
        val neonPaint = createQualityPaint().apply {
            color = neonColor
        }

        // 3. Draw glow layers (outer blurred passes first, then crisp center)
        // Multi-pass approach: draw several times dengan increasing blur radius, decreasing alpha
        
        // Pass 1: Outer glow (large blur, low alpha)
        val glowRadius1 = neonRadius * 0.8f
        val glowAlpha1 = ((opacity * 0.25f * neonIntensity) / 3f).toInt().coerceIn(0, 255)
        
        val glowBmp1 = createOffscreenBitmap(bw, bh)
        val glowCanvas1 = Canvas(glowBmp1)
        val glowPaint1 = createQualityPaint().apply {
            color = neonColor
            alpha = glowAlpha1
            applyBlurMaskFilter(this, glowRadius1, BlurMaskFilter.Blur.NORMAL)
        }
        glowCanvas1.translate(pad.toFloat(), pad.toFloat())
        drawContent(glowCanvas1, glowPaint1)
        clearMaskFilter(glowPaint1)

        // Pass 2: Mid glow (medium blur, medium alpha)
        val glowRadius2 = neonRadius * 0.5f
        val glowAlpha2 = ((opacity * 0.4f * neonIntensity) / 2f).toInt().coerceIn(0, 255)
        
        val glowBmp2 = createOffscreenBitmap(bw, bh)
        val glowCanvas2 = Canvas(glowBmp2)
        val glowPaint2 = createQualityPaint().apply {
            color = neonColor
            alpha = glowAlpha2
            applyBlurMaskFilter(this, glowRadius2, BlurMaskFilter.Blur.NORMAL)
        }
        glowCanvas2.translate(pad.toFloat(), pad.toFloat())
        drawContent(glowCanvas2, glowPaint2)
        clearMaskFilter(glowPaint2)

        // Pass 3: Inner glow (small blur, higher alpha)
        val glowRadius3 = neonRadius * 0.25f
        val glowAlpha3 = ((opacity * 0.6f * neonIntensity)).toInt().coerceIn(0, 255)
        
        val glowBmp3 = createOffscreenBitmap(bw, bh)
        val glowCanvas3 = Canvas(glowBmp3)
        val glowPaint3 = createQualityPaint().apply {
            color = neonColor
            alpha = glowAlpha3
            if (glowRadius3 > 0.1f) {
                applyBlurMaskFilter(this, glowRadius3, BlurMaskFilter.Blur.NORMAL)
            }
        }
        glowCanvas3.translate(pad.toFloat(), pad.toFloat())
        drawContent(glowCanvas3, glowPaint3)
        clearMaskFilter(glowPaint3)

        // Pass 4: Crisp center (no blur, full alpha)
        val centerAlpha = opacity.coerceIn(0, 255)
        val centerPaint = createQualityPaint().apply {
            color = neonColor
            alpha = centerAlpha
        }

        // 4. Composite glow layers + center to main canvas
        val blitPaint = createQualityPaint()

        // Blit glow layers (outer to inner)
        canvas.drawBitmap(glowBmp1, (-pad).toFloat(), (-pad).toFloat(), blitPaint)
        canvas.drawBitmap(glowBmp2, (-pad).toFloat(), (-pad).toFloat(), blitPaint)
        canvas.drawBitmap(glowBmp3, (-pad).toFloat(), (-pad).toFloat(), blitPaint)

        // Draw crisp center on top
        drawContent(canvas, centerPaint)

        // 5. Cleanup
        recycleOffscreenBitmap(glowBmp1)
        recycleOffscreenBitmap(glowBmp2)
        recycleOffscreenBitmap(glowBmp3)
    }

    /**
     * Generic drop shadow effect renderer untuk semua layer types.
     * 
     * Algoritma:
     * 1. Draw content ke offscreen bitmap dengan BlurMaskFilter
     * 2. Blit shadow result ke main canvas di offset position (shadowDx, shadowDy)
     * 3. Draw crisp content on top
     * 
     * PENTING: BlurMaskFilter hanya bekerja di software canvas, bukan hardware-accelerated.
     * 
     * @param canvas Target canvas (hardware-accelerated)
     * @param layer CanvasLayer dengan shadow properties
     * @param contentWidth Width dari content dalam pixels
     * @param contentHeight Height dari content dalam pixels
     * @param shadowColor Color untuk shadow rendering
     * @param drawContent Lambda untuk render content (akan di-draw untuk shadow & crisp)
     */
    fun drawDropShadowEffect(
        canvas: Canvas,
        layer: com.flyerpix.editor.canvas.model.CanvasLayer,
        contentWidth: Float,
        contentHeight: Float,
        shadowColor: Int,
        drawContent: (Canvas, Paint) -> Unit
    ) {
        if (!layer.shadowEnabled || contentWidth <= 0f || contentHeight <= 0f) {
            return
        }

        val shadowRadius = layer.shadowRadius.coerceIn(0.5f, 40f)
        val shadowDx = layer.shadowDx
        val shadowDy = layer.shadowDy
        val shadowOpacity = layer.shadowOpacity.coerceIn(0f, 1f)
        val opacity = layer.opacity.coerceIn(0, 255)

        // 1. Calculate offscreen bitmap size dengan padding
        val pad = (shadowRadius + kotlin.math.abs(shadowDx) + kotlin.math.abs(shadowDy) + 4).toInt().coerceAtLeast(8)
        val bw = contentWidth.toInt() + pad * 2
        val bh = contentHeight.toInt() + pad * 2

        if (bw <= 0 || bh <= 0) return

        // 2. Create shadow bitmap
        val shadowBmp = createOffscreenBitmap(bw, bh)
        val shadowCanvas = Canvas(shadowBmp)

        val shadowPaint = createQualityPaint().apply {
            color = shadowColor
            alpha = ((opacity * shadowOpacity) / 255f * 255).toInt().coerceIn(0, 255)
            applyBlurMaskFilter(this, shadowRadius, BlurMaskFilter.Blur.NORMAL)
        }

        // 3. Render shadow to offscreen
        shadowCanvas.translate(pad.toFloat(), pad.toFloat())
        drawContent(shadowCanvas, shadowPaint)
        clearMaskFilter(shadowPaint)

        // 4. Blit shadow to main canvas at offset
        val blitPaint = createQualityPaint()
        canvas.drawBitmap(
            shadowBmp,
            (-pad + shadowDx).toFloat(),
            (-pad + shadowDy).toFloat(),
            blitPaint
        )

        // 5. Draw crisp content on top
        val contentPaint = createQualityPaint().apply {
            alpha = opacity
        }
        drawContent(canvas, contentPaint)

        // 6. Cleanup
        recycleOffscreenBitmap(shadowBmp)
    }

    /**
     * Generic inner shadow effect renderer untuk semua layer types.
     * 
     * Algoritma:
     * 1. Draw content normal ke main canvas
     * 2. Create offscreen shadow mask
     * 3. Render shadow dengan PorterDuff.Mode.DST_IN (shadow appears INSIDE content)
     * 4. Blit shadow mask result over content
     * 
     * PENTING: Inner shadow creates depth effect di dalam content boundary.
     * 
     * @param canvas Target canvas (hardware-accelerated)
     * @param layer CanvasLayer dengan inner shadow properties
     * @param contentWidth Width dari content dalam pixels
     * @param contentHeight Height dari content dalam pixels
     * @param contentColor Color untuk content (untuk inner shadow masking)
     * @param drawContent Lambda untuk render content (akan di-draw 3x: base, shadow, final)
     */
    fun drawInnerShadowEffect(
        canvas: Canvas,
        layer: com.flyerpix.editor.canvas.model.CanvasLayer,
        contentWidth: Float,
        contentHeight: Float,
        contentColor: Int,
        drawContent: (Canvas, Paint) -> Unit
    ) {
        if (!layer.innerShadowEnabled || contentWidth <= 0f || contentHeight <= 0f) {
            return
        }

        val innerShadowRadius = layer.innerShadowRadius.coerceIn(0.5f, 40f)
        val innerShadowDx = layer.innerShadowDx
        val innerShadowDy = layer.innerShadowDy
        val innerShadowOpacity = layer.innerShadowOpacity.coerceIn(0f, 1f)
        val opacity = layer.opacity.coerceIn(0, 255)

        val bw = contentWidth.toInt().coerceAtLeast(1)
        val bh = contentHeight.toInt().coerceAtLeast(1)

        // 1. Draw base content solid (crisp, no shadow)
        val basePaint = createQualityPaint().apply {
            alpha = opacity
            color = contentColor
        }
        drawContent(canvas, basePaint)

        // 2. Create offscreen bitmap untuk inner shadow
        val innerShadowBmp = createOffscreenBitmap(bw, bh)
        val innerShadowCanvas = Canvas(innerShadowBmp)

        // 3. Render content ke shadow bitmap
        val innerPaint = createQualityPaint().apply {
            color = layer.innerShadowColor
            alpha = ((opacity * innerShadowOpacity) / 255f * 255).toInt().coerceIn(0, 255)
            applyBlurMaskFilter(this, innerShadowRadius, BlurMaskFilter.Blur.NORMAL)
        }

        innerShadowCanvas.translate(innerShadowDx, innerShadowDy)
        drawContent(innerShadowCanvas, innerPaint)
        clearMaskFilter(innerPaint)

        // 4. Apply PorterDuff masking: shadow appears only INSIDE content
        val maskPaint = createQualityPaint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
        }
        innerShadowCanvas.drawBitmap(innerShadowBmp, 0f, 0f, maskPaint)

        // 5. Blit inner shadow to main canvas
        val blitPaint = createQualityPaint()
        canvas.drawBitmap(innerShadowBmp, 0f, 0f, blitPaint)

        // 6. Cleanup
        recycleOffscreenBitmap(innerShadowBmp)
    }
}

