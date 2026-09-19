package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Simbol ukiran tengah koin.
 */
enum class CoinSymbol {
    PERCENT,      // %
    RUPIAH,       // Rp
    DOLLAR,       // $
    STAR,         // ★
    CROWN,        // 👑 (digambar vektor)
    DIAMOND       // 💎 (digambar vektor)
}

/** Mode isi ukiran tengah koin. */
enum class CoinContentMode {
    PRESET_SYMBOL,
    CUSTOM_TEXT
}

/** Preset material logam koin. */
enum class CoinMaterial {
    GOLD,
    SILVER,
    BRONZE,
    ROSE_GOLD,
    NEON_CYBER
}

/**
 * Layer objek 3D Coin / Koin Promo (geometri vektor elips analitik):
 *
 *  Muka koin = elips proyeksi lingkaran yang diputar pada sumbu Y ([tiltAngle])
 *  dan sumbu Z ([spinAngle]). Ketebalan logam terlihat sebagai band di sisi
 *  kanan (lebar = thickness·sin θ) dengan gerigi tepi ([reededEdgeEnabled]).
 *
 *  Urutan gambar:
 *   1. Bayangan lantai kontak (+ drop shadow engine / neon halo di belakang).
 *   2. Band ketebalan logam + gerigi (Reeded Edge).
 *   3. Muka koin dengan gradien logam multi-stop (top-left light).
 *   4. Cincin bibir ganda (Double Bevel Rim).
 *   5. Ukiran timbul tengah (simbol/teks) — transformasi mengikuti tilt & spin.
 *   6. Bintang kilau (Star Sparkle) di tepi sudut koin.
 */
data class Coin3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Dimensi & Rotasi 3D ───────────────────────────────────────────────────
    var diameter: Float = 160f,
    var thickness: Float = 22f,
    var tiltAngle: Float = 35f,     // 0° = hadap depan lurus, 75° = sangat miring
    var spinAngle: Float = 0f,      // 0°..360° putaran ukiran
    // ── Ukiran Tengah ─────────────────────────────────────────────────────────
    var contentMode: CoinContentMode = CoinContentMode.PRESET_SYMBOL,
    var symbolType: CoinSymbol = CoinSymbol.PERCENT,
    var customText: String = "50%",
    var embossDepth: Float = 4f,
    var emblemColor: Int = Color.WHITE,
    // ── Gerigi Tepi ───────────────────────────────────────────────────────────
    var reededEdgeEnabled: Boolean = true,
    var reedCount: Int = 36,
    // ── Material & Warna Logam ────────────────────────────────────────────────
    var materialType: CoinMaterial = CoinMaterial.GOLD,
    var baseColor: Int = 0xFFFFD700.toInt(),
    var useCustomColor: Boolean = false,
    // ── Kilau Bintang ─────────────────────────────────────────────────────────
    var sparkleEnabled: Boolean = true,
    var sparkleAngle: Float = -45f,
    var sparkleSize: Float = 28f,
    // ── Bayangan Lantai ───────────────────────────────────────────────────────
    var floorShadowEnabled: Boolean = true,
    var floorShadowOpacity: Float = 0.5f,
    // ── Efek Warisan ──────────────────────────────────────────────────────────
    override var neonEnabled: Boolean = false,
    override var neonColor: Int = 0xFF00E5FF.toInt(),
    override var neonRadius: Float = 16f,
    override var neonIntensity: Float = 1f,
    override var shadowEnabled: Boolean = false,
    override var shadowColor: Int = 0xFF000000.toInt(),
    override var shadowRadius: Float = 12f,
    override var shadowOpacity: Float = 0.6f,
    override var shadowDx: Float = 0f,
    override var shadowDy: Float = 6f,
    override var embossEnabled: Boolean = false,
    override var embossLightAngle: Float = -45f,
    override var embossIntensity: Float = 1f,
    override var embossAmbient: Float = 0.3f,
    override var embossSpecular: Float = 0.7f,
    override var embossBevel: Float = 2f,
    // ── Transformasi Warping ──────────────────────────────────────────────────
    override var perspectiveEnabled: Boolean = false,
    override var perspectiveCorners: FloatArray = floatArrayOf(
        0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f
    ),
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

    // ─────────────────────────────────────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────────────────────────────────────

    data class CoinLayout(
        val w: Float,
        val h: Float,
        val rx: Float,
        val ry: Float,
        val dx: Float,        // lebar band ketebalan sisi kanan
        val fx: Float,        // pusat muka koin (x, dalam konten)
        val fy: Float,        // pusat muka koin (y)
        val bx: Float,        // pusat muka belakang (x = fx + dx)
        val fs: Float,        // ruang bayangan lantai
        val cosT: Float,
        val sinT: Float
    )

    fun computeLayout(): CoinLayout {
        val th = Math.toRadians(tiltAngle.coerceIn(0f, 75f).toDouble())
        val cosT = cos(th).toFloat().coerceIn(0f, 1f)
        val sinT = sin(th).toFloat().coerceIn(0f, 1f)
        val rx = (diameter / 2f).coerceAtLeast(10f)
        val ry = max(rx * cosT, 2f)
        val dx = max(thickness.coerceAtLeast(0f) * sinT * 0.92f, 0f)
        val fs = rx * 0.42f
        val fx = rx
        val fy = ry
        val bx = fx + dx
        return CoinLayout(
            w = rx * 2f + dx,
            h = ry * 2f + fs,
            rx = rx,
            ry = ry,
            dx = dx,
            fx = fx,
            fy = fy,
            bx = bx,
            fs = fs,
            cosT = cosT,
            sinT = sinT
        )
    }

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.w, l.h)
    }

    override fun getBounds(): RectF {
        val (w, h) = getUnwarpedDimensions()
        val pts = getSelectionBoxPoints(0f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0..3) {
            minX = min(minX, pts[i * 2])
            minY = min(minY, pts[i * 2 + 1])
            maxX = max(maxX, pts[i * 2])
            maxY = max(maxY, pts[i * 2 + 1])
        }
        return RectF(minX, minY, maxX, maxY)
    }

    override fun copyLayer(): Coin3DLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        perspectiveCorners = this.perspectiveCorners.clone()
    ).also {
        it.stretchX = this.stretchX
        it.stretchY = this.stretchY
    }

    /** Invers transformasi layer untuk deteksi sentuhan (pola sama dgn Sphere). */
    fun mapCanvasPointToLocal(px: Float, py: Float, w: Float, h: Float): Pair<Float, Float> {
        val cx = w / 2f
        val cy = h / 2f
        val sxEff = if (scale * stretchX != 0f) scale * stretchX else 1f
        val syEff = if (scale * stretchY != 0f) scale * stretchY else 1f
        val dx = px - x - cx
        val dy = py - y - cy
        val rad = Math.toRadians(-rotation.toDouble())
        val cosr = Math.cos(rad).toFloat()
        val sinr = Math.sin(rad).toFloat()
        val rx = dx * cosr - dy * sinr
        val ry = dx * sinr + dy * cosr
        return Pair(rx / sxEff + cx, ry / syEff + cy)
    }

    /** Titik handle sentuh di bibir atas-kanan koin (tarik vertikal = tilt, horizontal = spin). */
    fun getTiltHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.fx + l.rx * 0.80f, l.fy - l.ry * 0.66f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Material & Warna
    // ─────────────────────────────────────────────────────────────────────────

    private fun effectiveBase(): Int =
        if (useCustomColor) baseColor else materialBase()

    private fun materialBase(): Int {
        return when (materialType) {
            CoinMaterial.GOLD -> 0xFFFFD700.toInt()
            CoinMaterial.SILVER -> 0xFFE7EBF3.toInt()
            CoinMaterial.BRONZE -> 0xFFD49354.toInt()
            CoinMaterial.ROSE_GOLD -> 0xFFF0B3A4.toInt()
            CoinMaterial.NEON_CYBER -> 0xFF00E5FF.toInt()
        }
    }

    /** Palet gradien logam muka koin (highlight, midtone, gelap, rim gelap). */
    private fun facePalette(baseE: Int): IntArray {
        if (!useCustomColor) {
            return when (materialType) {
                CoinMaterial.GOLD -> intArrayOf(0xFFFFF9D6.toInt(), 0xFFFFD700.toInt(), 0xFFC99900.toInt(), 0xFF7A6000.toInt())
                CoinMaterial.SILVER -> intArrayOf(0xFFFFFFFF.toInt(), 0xFFE7EBF3.toInt(), 0xFF9FA9BB.toInt(), 0xFF5F6775.toInt())
                CoinMaterial.BRONZE -> intArrayOf(0xFFFFE9C7.toInt(), 0xFFD49354.toInt(), 0xFF8A5525.toInt(), 0xFF4E2E12.toInt())
                CoinMaterial.ROSE_GOLD -> intArrayOf(0xFFFFE2E0.toInt(), 0xFFF0B3A4.toInt(), 0xFFB76E79.toInt(), 0xFF6E3540.toInt())
                CoinMaterial.NEON_CYBER -> intArrayOf(0xFFEAFBFF.toInt(), 0xFF00E5FF.toInt(), 0xFF0066CC.toInt(), 0xFF00305A.toInt())
            }
        }
        return intArrayOf(
            shadeWhite(baseE, 0.55f),
            baseE,
            shadeBlack(baseE, 0.35f),
            shadeBlack(baseE, 0.62f)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val l = computeLayout()
        if (l.w <= 0f || l.h <= 0f) return

        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.pathEffect = null
        paint.shader = null
        paint.maskFilter = null
        paint.setShadowLayer(0f, 0f, 0f, 0)

        val saveCount = canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale * stretchX, scale * stretchY, l.w / 2f, l.h / 2f)
        canvas.rotate(rotation, l.w / 2f, l.h / 2f)

        val effOpacity = opacity.coerceIn(0, 255) / 255f
        val baseE = effectiveBase()

        // 1a. Halo neon (paling belakang).
        if (neonEnabled) {
            drawNeonHalo(canvas, l, baseE, effOpacity)
        }

        // 1b. Drop shadow engine (bayangan lembut melayang di belakang koin).
        if (shadowEnabled && shadowOpacity > 0f && shadowRadius > 0f) {
            drawEngineShadow(canvas, l, effOpacity)
        }

        // 1c. Bayangan lantai kontak.
        if (floorShadowEnabled && floorShadowOpacity > 0f) {
            drawFloorShadow(canvas, l, effOpacity)
        }

        // 2. Band ketebalan logam + gerigi tepi.
        if (l.dx > 0.5f) {
            drawEdgeBand(canvas, l, baseE, effOpacity)
        }

        // 3. Muka koin.
        drawFace(canvas, l, baseE, effOpacity)

        // 4. Cincin bibir ganda.
        drawRim(canvas, l, baseE, effOpacity)

        // 5. Ukiran timbul tengah.
        drawEmblem(canvas, l, effOpacity)

        // 6. Bintang kilau.
        if (sparkleEnabled) {
            drawSparkle(canvas, l, effOpacity)
        }

        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null

        canvas.restoreToCount(saveCount)
    }

    // --- 1a. Neon halo -------------------------------------------------------

    private fun drawNeonHalo(canvas: Canvas, l: CoinLayout, baseE: Int, effOpacity: Float) {
        val glowColor = if (materialType == CoinMaterial.NEON_CYBER) baseE else neonColor
        val glowR = l.rx * 1.15f + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f)
        val alpha = (255 * neonIntensity.coerceIn(0.1f, 2.5f) / 2.5f * effOpacity).toInt().coerceIn(0, 255)
        val colors = intArrayOf(
            withAlpha(glowColor, alpha),
            withAlpha(glowColor, (alpha * 0.5f).toInt()),
            withAlpha(glowColor, 0)
        )
        val positions = floatArrayOf(0f, 0.5f, 1f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(l.fx, l.fy, glowR, colors, positions, Shader.TileMode.CLAMP)
        canvas.save()
        canvas.translate(l.fx, l.fy + l.ry)
        canvas.scale(1f, l.ry / l.rx)
        canvas.drawCircle(0f, 0f, glowR, p)
        canvas.restore()
        p.shader = null
    }

    // --- 1b. Drop shadow engine -----------------------------------------------

    private fun drawEngineShadow(canvas: Canvas, l: CoinLayout, effOpacity: Float) {
        val rx = l.rx * (0.62f + 0.10f * (shadowRadius.coerceIn(0f, 40f) / 40f))
        val ry = rx * 0.30f
        val dx = shadowDx.coerceIn(-30f, 30f)
        val dy = shadowDy.coerceIn(-30f, 30f)
        val alpha = (255 * shadowOpacity.coerceIn(0f, 1f) * effOpacity)
            .toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        val colors = intArrayOf(
            Color.argb(alpha, (shadowColor shr 16) and 0xFF, (shadowColor shr 8) and 0xFF, shadowColor and 0xFF),
            Color.argb((alpha * 0.5f).toInt(), 0, 0, 0),
            Color.argb(0, (shadowColor shr 16) and 0xFF, (shadowColor shr 8) and 0xFF, shadowColor and 0xFF)
        )
        val positions = floatArrayOf(0f, 0.55f, 1f)
        val gradR = rx * 1.15f
        p.shader = RadialGradient(0f, 0f, gradR, colors, positions, Shader.TileMode.CLAMP)
        canvas.save()
        canvas.translate(l.fx + dx, l.ry + l.ry * 0.45f + dy)
        canvas.scale(1f, ry / rx)
        canvas.drawCircle(0f, 0f, gradR, p)
        canvas.restore()
        p.shader = null
    }

    // --- 1c. Bayangan lantai kontak --------------------------------------------

    private fun drawFloorShadow(canvas: Canvas, l: CoinLayout, effOpacity: Float) {
        val rx = l.rx * 0.52f
        val ry = rx * 0.30f
        val alpha = (255 * floorShadowOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        val colors = intArrayOf(
            Color.argb(alpha, 0, 0, 0),
            Color.argb((alpha * 0.45f).toInt(), 0, 0, 0),
            Color.argb(0, 0, 0, 0)
        )
        val positions = floatArrayOf(0f, 0.5f, 1f)
        p.shader = RadialGradient(0f, 0f, l.rx * 1.1f, colors, positions, Shader.TileMode.CLAMP)
        canvas.save()
        canvas.translate(l.fx, l.ry * 2f + l.fs * 0.5f)
        canvas.scale(1f, (rx * 0.28f) / rx)
        canvas.drawCircle(0f, 0f, rx, p)
        canvas.restore()
        p.shader = null
    }

    // --- 2. Band ketebalan + gerigi ---------------------------------------------

    private fun drawEdgeBand(canvas: Canvas, l: CoinLayout, baseE: Int, effOpacity: Float) {
        val faceL = l.fx - l.rx
        val edgeL = l.fx + l.rx
        val backR = RectF(l.bx - l.rx, l.fy - l.ry, l.bx + l.rx, l.fy + l.ry)
        val frontR = RectF(faceL, l.fy - l.ry, l.fx + l.rx, l.fy + l.ry)

        // Fill band.
        val band = Path()
        band.moveTo(l.fx, l.fy - l.ry)
        band.lineTo(l.bx, l.fy - l.ry)
        band.arcTo(backR, -90f, 180f)      // atas->bawah melalui ujung kanan
        band.lineTo(l.fx, l.fy + l.ry)
        band.arcTo(frontR, 90f, -180f)     // bawah->atas melalui ujung kanan
        band.close()

        val g = LinearGradient(
            edgeL, l.fy, l.bx + l.rx, l.fy,
            intArrayOf(
                shadeBlack(baseE, 0.5f),
                shadeBlack(baseE, 0.12f),
                shadeWhite(baseE, 0.55f),
                shadeBlack(baseE, 0.30f),
                shadeBlack(baseE, 0.6f)
            ),
            floatArrayOf(0f, 0.28f, 0.5f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = g
        canvas.drawPath(band, p)
        p.shader = null

        // Gerigi tepi (Reeded Edge).
        if (reededEdgeEnabled && l.dx > 1.5f) {
            val lineCount = (reedCount.coerceAtLeast(4) * (l.dx / 36f)).toInt().coerceIn(2, reedCount.coerceAtLeast(4))
            val lp = Paint(Paint.ANTI_ALIAS_FLAG)
            lp.style = Paint.Style.STROKE
            lp.strokeWidth = max(1f, l.rx * 0.012f)
            val dark = withAlpha(shadeBlack(baseE, 0.55f), (170 * effOpacity).toInt().coerceIn(0, 255))
            val gap = l.dx / lineCount
            for (i in 1 until lineCount) {
                val x = edgeL + gap * i + gap * 0.5f
                val rel = (x - l.bx) / l.rx
                if (abs(rel) > 0.995f) continue
                val yTop = l.fy - l.ry * sqrt(1f - rel * rel)
                val yBot = l.fy + l.ry * sqrt(1f - rel * rel)
                lp.color = dark
                canvas.drawLine(x, yTop, x, yBot, lp)
                // Pembatas gerigi terang tipis.
                val light = withAlpha(shadeWhite(baseE, 0.6f), (90 * effOpacity).toInt().coerceIn(0, 255))
                lp.color = light
                lp.strokeWidth = max(0.8f, l.rx * 0.006f)
                canvas.drawLine(x + gap * 0.25f, yTop, x + gap * 0.25f, yBot, lp)
                lp.strokeWidth = max(1f, l.rx * 0.012f)
            }
        }

        // Outline tipis band agar tegas.
        val outline = Paint(Paint.ANTI_ALIAS_FLAG)
        outline.style = Paint.Style.STROKE
        outline.strokeWidth = max(1f, l.rx * 0.02f)
        outline.color = withAlpha(shadeBlack(baseE, 0.5f), (130 * effOpacity).toInt().coerceIn(0, 255))
        canvas.drawPath(band, outline)
    }

    // --- 3. Muka koin -----------------------------------------------------------

    private fun drawFace(canvas: Canvas, l: CoinLayout, baseE: Int, effOpacity: Float) {
        val faceR = RectF(l.fx - l.rx, l.fy - l.ry, l.fx + l.rx, l.fy + l.ry)
        val palette = facePalette(baseE)
        val g = RadialGradient(
            l.fx - l.rx * 0.35f,
            l.fy - l.ry * 0.42f,
            l.rx * 1.7f,
            palette,
            floatArrayOf(0f, 0.38f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = g
        canvas.drawOval(faceR, p)
        p.shader = null

        // Kilau lembut tambahan (mint/sheen) di kuadran sumber cahaya.
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
        sheen.style = Paint.Style.FILL
        sheen.shader = RadialGradient(
            l.fx - l.rx * 0.25f,
            l.fy - l.ry * 0.30f,
            l.rx * 0.7f,
            intArrayOf(
                Color.argb(((36 * effOpacity).toInt()), 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(faceR, sheen)
        sheen.shader = null
    }

    // --- 4. Cincin bibir ganda ---------------------------------------------------

    private fun drawRim(canvas: Canvas, l: CoinLayout, baseE: Int, effOpacity: Float) {
        val faceR = RectF(l.fx - l.rx, l.fy - l.ry, l.fx + l.rx, l.fy + l.ry)
        val a = (effOpacity * 255).toInt().coerceIn(0, 255)

        // Bevel shading atas terang / bawah gelap (kesan bibir timbul).
        val topLight = Paint(Paint.ANTI_ALIAS_FLAG)
        topLight.style = Paint.Style.STROKE
        topLight.strokeWidth = l.rx * 0.085f
        topLight.color = withAlpha(Color.WHITE, (60 * a / 255).coerceIn(0, 255))
        canvas.drawArc(faceR, 185f, 170f, false, topLight)

        val bottomDark = Paint(Paint.ANTI_ALIAS_FLAG)
        bottomDark.style = Paint.Style.STROKE
        bottomDark.strokeWidth = l.rx * 0.085f
        bottomDark.color = withAlpha(shadeBlack(baseE, 0.55f), (55 * a / 255).coerceIn(0, 255))
        canvas.drawArc(faceR, 5f, 170f, false, bottomDark)

        // Cincin luar tipis terang.
        val outer = Paint(Paint.ANTI_ALIAS_FLAG)
        outer.style = Paint.Style.STROKE
        outer.strokeWidth = max(1.2f, l.rx * 0.035f)
        outer.color = withAlpha(shadeWhite(baseE, 0.9f), (215 * a / 255).coerceIn(0, 255))
        canvas.drawOval(faceR, outer)

        // Alur dalam (groove) cincin kedua.
        val innerR = RectF(
            l.fx - l.rx * 0.88f, l.fy - l.ry * 0.88f,
            l.fx + l.rx * 0.88f, l.fy + l.ry * 0.88f
        )
        val groove = Paint(Paint.ANTI_ALIAS_FLAG)
        groove.style = Paint.Style.STROKE
        groove.strokeWidth = max(1f, l.rx * 0.02f)
        groove.color = withAlpha(shadeBlack(baseE, 0.45f), (170 * a / 255).coerceIn(0, 255))
        canvas.drawOval(innerR, groove)

        // Cincin kedua terang di dalam groove.
        val midR = RectF(
            l.fx - l.rx * 0.93f, l.fy - l.ry * 0.93f,
            l.fx + l.rx * 0.93f, l.fy + l.ry * 0.93f
        )
        val mid = Paint(Paint.ANTI_ALIAS_FLAG)
        mid.style = Paint.Style.STROKE
        mid.strokeWidth = max(0.8f, l.rx * 0.018f)
        mid.color = withAlpha(Color.WHITE, (80 * a / 255).coerceIn(0, 255))
        canvas.drawOval(midR, mid)
    }

    // --- 5. Ukiran timbul ---------------------------------------------------------

    private fun drawEmblem(canvas: Canvas, l: CoinLayout, effOpacity: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        val title = if (contentMode == CoinContentMode.CUSTOM_TEXT) customText else when (symbolType) {
            CoinSymbol.PERCENT -> "%"
            CoinSymbol.RUPIAH -> "Rp"
            CoinSymbol.DOLLAR -> "$"
            CoinSymbol.STAR, CoinSymbol.CROWN, CoinSymbol.DIAMOND -> null
        }

        val a = (effOpacity * 255).toInt().coerceIn(0, 255)
        val darkEmblem = withAlpha(shadeBlack(emblemColor, 0.62f), (200 * a / 255).coerceIn(0, 255))
        val lightEmblem = withAlpha(shadeWhite(emblemColor, 0.85f), (120 * a / 255).coerceIn(0, 255))
        val mainColor = withAlpha(emblemColor, a)

        canvas.save()
        canvas.translate(l.fx, l.fy)
        canvas.rotate(spinAngle)
        canvas.scale(1f, l.cosT.coerceAtLeast(0.02f))

        val off = embossDepth.coerceIn(0f, 12f)

        // Simbol vektor -> gambar dua lapis (bayangan timbul + lapis terang + inti).
        if (title == null) {
            val s = l.rx * 0.92f
            drawSymbolPath(canvas, p, symbolType, s, darkEmblem, 0f + off * 1.3f, off * 1.3f)
            drawSymbolPath(canvas, p, symbolType, s, lightEmblem, -off * 0.8f, -off * 0.8f)
            drawSymbolPath(canvas, p, symbolType, s, mainColor, 0f, 0f)
        } else {
            drawEmblemText(canvas, p, title, l.rx, darkEmblem, off * 1.3f, off * 1.3f)
            drawEmblemText(canvas, p, title, l.rx, lightEmblem, -off * 0.8f, -off * 0.8f)
            drawEmblemText(canvas, p, title, l.rx, mainColor, 0f, 0f)
        }

        canvas.restore()
    }

    private inline fun drawSymbolPath(
        canvas: Canvas,
        p: Paint,
        s: CoinSymbol,
        scale: Float,
        color: Int,
        ox: Float,
        oy: Float
    ) {
        val path = when (s) {
            CoinSymbol.STAR -> starPath(scale)
            CoinSymbol.CROWN -> crownPath(scale)
            CoinSymbol.DIAMOND -> diamondPath(scale)
            else -> return
        }
        p.color = color
        canvas.save()
        canvas.translate(ox, oy)
        canvas.drawPath(path, p)
        canvas.restore()
    }

    private inline fun drawEmblemText(
        canvas: Canvas,
        p: Paint,
        text: String,
        rx: Float,
        color: Int,
        ox: Float,
        oy: Float
    ) {
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var size = rx * 0.62f
        p.textSize = size
        val sw = p.measureText(text)
        val maxW = rx * 1.15f
        if (sw > maxW) {
            size *= maxW / sw
            p.textSize = size
        }
        val bounds = android.graphics.Rect()
        p.getTextBounds(text, 0, text.length, bounds)
        val baseline = -(bounds.top + bounds.bottom) / 2f
        p.color = color
        canvas.save()
        canvas.translate(ox, oy)
        canvas.drawText(text, 0f, baseline, p)
        canvas.restore()
    }

    private fun starPath(scale: Float): Path {
        val R = scale * 0.55f
        val inner = R * 0.40f
        val path = Path()
        for (i in 0 until 10) {
            val ang = -90.0 + i * 36.0
            val rad = if (i % 2 == 0) R else inner
            val x = cos(Math.toRadians(ang)).toFloat() * rad
            val y = sin(Math.toRadians(ang)).toFloat() * rad
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun crownPath(scale: Float): Path {
        val w2 = scale * 0.5f
        val h = scale * 0.72f
        val path = Path()
        path.moveTo(-w2, h * 0.18f)
        path.lineTo(-w2 * 0.76f, -h * 0.46f)   // paku kiri
        path.lineTo(-w2 * 0.14f, h * 0.02f)
        path.lineTo(0f, -h * 0.60f)            // paku tengah
        path.lineTo(w2 * 0.14f, h * 0.02f)
        path.lineTo(w2 * 0.76f, -h * 0.46f)    // paku kanan
        path.lineTo(w2, h * 0.18f)
        path.lineTo(w2, h * 0.62f)
        path.lineTo(-w2, h * 0.62f)
        path.close()
        return path
    }

    private fun diamondPath(scale: Float): Path {
        val w2 = scale * 0.5f
        val h2 = scale * 0.60f
        val path = Path()
        path.moveTo(0f, -h2)
        path.lineTo(w2 * 0.82f, -h2 * 0.34f)
        path.lineTo(w2 * 0.36f, h2)
        path.lineTo(-w2 * 0.36f, h2)
        path.lineTo(-w2 * 0.82f, -h2 * 0.34f)
        path.close()
        return path
    }

    // --- 6. Bintang kilau ---------------------------------------------------------

    private fun drawSparkle(canvas: Canvas, l: CoinLayout, effOpacity: Float) {
        val th = Math.toRadians(sparkleAngle.toDouble())
        val sx = l.fx + sin(th).toFloat() * l.rx * 0.80f
        val sy = l.fy - cos(th).toFloat() * l.ry * 0.82f
        val big = sparkleSize.coerceIn(6f, 60f)
        val small = big * 0.30f
        val a = (255 * effOpacity).toInt().coerceIn(0, 255)

        val glint = Path()
        for (i in 0 until 8) {
            val ang = Math.toRadians(i * 45.0)
            val rad = if (i % 2 == 0) big else small
            val x = sx + cos(ang).toFloat() * rad
            val y = sy + sin(ang).toFloat() * rad
            if (i == 0) glint.moveTo(x, y) else glint.lineTo(x, y)
        }
        glint.close()

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = withAlpha(Color.WHITE, (225 * a / 255).coerceIn(0, 255))
        canvas.drawPath(glint, p)

        // Inti terang.
        val core = Paint(Paint.ANTI_ALIAS_FLAG)
        core.style = Paint.Style.FILL
        core.color = withAlpha(Color.WHITE, (235 * a / 255).coerceIn(0, 255))
        canvas.drawCircle(sx, sy, small * 1.15f, core)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shader warna util
    // ─────────────────────────────────────────────────────────────────────────

    private fun withAlpha(color: Int, a: Int): Int {
        val aa = a.coerceIn(0, 255)
        return (color and 0x00FFFFFF.toInt()) or (aa shl 24)
    }

    private fun shadeWhite(color: Int, t: Float): Int {
        val r = (((color shr 16) and 0xFF) + (255 - ((color shr 16) and 0xFF)) * t).toInt()
        val g = (((color shr 8) and 0xFF) + (255 - ((color shr 8) and 0xFF)) * t).toInt()
        val b = ((color and 0xFF) + (255 - (color and 0xFF)) * t).toInt()
        return Color.rgb(r, g, b)
    }

    private fun shadeBlack(color: Int, t: Float): Int {
        val r = (((color shr 16) and 0xFF) * (1f - t)).toInt()
        val g = (((color shr 8) and 0xFF) * (1f - t)).toInt()
        val b = ((color and 0xFF) * (1f - t)).toInt()
        return Color.rgb(r, g, b)
    }
}