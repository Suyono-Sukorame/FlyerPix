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
}
