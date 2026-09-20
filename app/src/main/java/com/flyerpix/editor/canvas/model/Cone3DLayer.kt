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
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Gaya tampilan utama kerucut 3D (Koni 3D). */
enum class ConeStyle {
    MINIMAL,        // kerucut putih minimalis melayang
    TRAFFIC_CONE,   // kerucut lalu lintas oranye + stripe reflektif
    PARTY_HAT,      // topi pesta biru dengan pita warna-warni
    GOLD_PEAK,      // puncak emas krom berkilau
    SITH_PYRAMID,   // piramida putih futuristik
    CYBER_NEON      // piramida neon sci-fi berpendar
}

/** Preset material permukaan kerucut 3D. */
enum class ConeMaterial {
    MATTE,          // doff minimalis
    GLOSSY,         // plastik mengkilap
    METALLIC_GOLD,  // logam emas krom berkilau
    CHROME_SILVER,  // krom perak menyilaukan
    NEON            // badan warna neon berpendar
}

/** Mode garis/band reflektif pada kerucut 3D. */
enum class ConeStripeMode {
    NONE,       // tanpa stripe
    ONE,        // satu band
    TWO         // dua band (pola peringatan)
}

/**
 * Layer objek 3D Cone (Kerucut 3D) — bangun kerucut dengan alas elips.
 *
 * Geometri disusun dari alas elips berjari-jari [radiusX] × [radiusY]
 * (radiusY menciptakan ilusi kemiringan/perspektif) dan tinggi [coneHeight]
 * dari bidang alas ke puncak. Opsi [flipApex] membalik orientasi puncak
 * (puncak mengarah ke bawah).
 *
 * Urutan render:
 *  1. Bayangan lantai kerucut (floor shadow) di bawah alas.
 *  2. Halo neon di belakang (style CYBER_NEON / neonEnabled).
 *  3. Mantle (selimut) kerucut dengan gradien pencahayaan diagonal.
 *  4. Band/Stripe reflektif horizontal pada permukaan depan.
 *  5. Specular highlight + rim alas.
 *  6. Wireframe (bila aktif).
 */
data class Cone3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Dimensi & Sudut 3D ───────────────────────────────────────────────────
    var radiusX: Float = 150f,       // setengah lebar alas elips
    var radiusY: Float = 54f,        // setengah tinggi alas elips (perspektif)
    var coneHeight: Float = 210f,    // tinggi puncak dari bidang alas
    var flipApex: Boolean = false,   // true = puncak mengarah ke bawah
    // ── Gaya & Mode Tampilan ─────────────────────────────────────────────────
    var style: ConeStyle = ConeStyle.TRAFFIC_CONE,
    var materialType: ConeMaterial = ConeMaterial.MATTE,
    var baseColor: Int = 0xFFFF7F1A.toInt(),
    // ── Pencahayaan & Naungan ────────────────────────────────────────────────
    var autoShade: Boolean = true,
    var specularIntensity: Float = 0.8f,
    // ── Stripe / Band Reflektif ──────────────────────────────────────────────
    var stripeMode: ConeStripeMode = ConeStripeMode.TWO,
    var stripeColor: Int = 0xFFFFFFFF.toInt(),
    var stripeWidth: Float = 44f,
    // ── Wireframe ────────────────────────────────────────────────────────────
    var wireframeEnabled: Boolean = false,
    var wireStrokeWidth: Float = 2.5f,
    var wireColor: Int = 0xFF1A1A2E.toInt(),
    var wireStrokeOpacity: Float = 0.55f,
    // ── Bayangan Lantai ──────────────────────────────────────────────────────
    var floorShadowEnabled: Boolean = true,
    var floatingElevation: Float = 16f,
    var floorShadowOpacity: Float = 0.5f,
    // ── Efek Warisan ──────────────────────────────────────────────────────────
    override var neonEnabled: Boolean = false,
    override var neonColor: Int = 0xFF00E5FF.toInt(),
    override var neonRadius: Float = 16f,
    override var neonIntensity: Float = 1f,
    override var shadowEnabled: Boolean = false,
    override var shadowColor: Int = 0xFF000000.toInt(),
    override var shadowRadius: Float = 8f,
    override var shadowOpacity: Float = 0.6f,
    override var shadowDx: Float = 0f,
    override var shadowDy: Float = 5f,
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

    data class ConeLayout(
        val rx: Float,
        val ry: Float,
        val hgt: Float,
        val w: Float,
        val h: Float,
        val cx: Float,
        val baseCy: Float,
        val apexY: Float
    )

    fun computeLayout(): ConeLayout {
        val rx = radiusX.coerceIn(16f, 600f)
        val ry = radiusY.coerceIn(4f, 260f)
        val hgt = coneHeight.coerceIn(12f, 900f)
        val w = rx * 2f
        val h = hgt + ry * 2f
        val flip = flipApex
        val baseCy = if (flip) ry else ry + hgt
        val apexY = if (flip) ry + hgt else ry
        return ConeLayout(
            rx = rx, ry = ry, hgt = hgt,
            w = w, h = h,
            cx = rx,
            baseCy = baseCy, apexY = apexY
        )
    }

    /** Rect elips alas dalam koordinat lokal. */
    private fun baseOvalRect(l: ConeLayout): RectF =
        RectF(l.cx - l.rx, l.baseCy - l.ry, l.cx + l.rx, l.baseCy + l.ry)

    /** Rect elips pada fraksi tinggi [f] (0 = puncak, 1 = bidang alas). */
    private fun stripeOvalRect(l: ConeLayout, f: Float): RectF {
        val y = l.apexY + l.hgt * f
        val r = l.rx * f
        val ry = l.ry * f
        return RectF(l.cx - r, y - ry, l.cx + r, y + ry)
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

    override fun copyLayer(): Cone3DLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        perspectiveCorners = this.perspectiveCorners.clone()
    ).also {
        it.stretchX = this.stretchX
        it.stretchY = this.stretchY
    }

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

    /** Path selimut kerucut: puncak → tepi kanan alas → busur bawah → kembali. */
    private fun mantlePath(l: ConeLayout): Path {
        val base = baseOvalRect(l)
        return Path().apply {
            moveTo(l.cx, l.apexY)
            lineTo(l.cx + l.rx, l.baseCy)
            arcTo(base, 0f, 180f)
            close()
        }
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
        val si = specularIntensity.coerceIn(0f, 2f)
        val isNeon = neonEnabled || style == ConeStyle.CYBER_NEON ||
            materialType == ConeMaterial.NEON

        // 1. Bayangan lantai.
        if (floorShadowEnabled && floorShadowOpacity > 0f) {
            drawFloorShadow(canvas, l, effOpacity)
        }

        // 2. Drop shadow engine (blur di bawah objek).
        if (shadowEnabled && shadowOpacity > 0f && shadowRadius > 0f) {
            drawEngineShadow(canvas, l, effOpacity)
        }

        // 3. Halo neon di belakang.
        if (isNeon) {
            drawNeonHalo(canvas, l, effOpacity)
        }

        // 4. Selimut kerucut.
        drawMantle(canvas, l, effOpacity, si)

        // 5. Band/Stripe.
        drawStripes(canvas, l, effOpacity)

        // 6. Specular + rim alas.
        drawSpecular(canvas, l, effOpacity, si)
        drawBaseRim(canvas, l, effOpacity)

        // 7. Matte rim.
        if (isNeon) {
            drawNeonRim(canvas, l, effOpacity)
        }

        // 8. Wireframe.
        if (wireframeEnabled) {
            drawWireframe(canvas, l, effOpacity)
        }

        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null
        canvas.restoreToCount(saveCount)
    }

    /** 3. Selimut kerucut dengan gradien pencahayaan diagonal. */
    private fun drawMantle(canvas: Canvas, l: ConeLayout, effOpacity: Float, si: Float) {
        val base = effectiveBase()
        val baseRect = baseOvalRect(l)
        val mantle = mantlePath(l)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        if (autoShade) {
            val bright = if (materialType == ConeMaterial.METALLIC_GOLD) 0.46f
            else if (materialType == ConeMaterial.CHROME_SILVER) 0.55f
            else if (materialType == ConeMaterial.NEON) 0.38f else 0.30f
            val dark = if (materialType == ConeMaterial.METALLIC_GOLD) 0.42f
            else if (materialType == ConeMaterial.CHROME_SILVER) 0.28f else 0.22f
            p.shader = LinearGradient(
                l.cx - l.rx, l.apexY,
                l.cx + l.rx, l.baseCy + l.ry,
                intArrayOf(
                    withAlpha(shadeWhite(base, bright * 0.9f), (255 * effOpacity).toInt()),
                    withAlpha(base, (252 * effOpacity).toInt()),
                    withAlpha(shadeBlack(base, dark), (228 * effOpacity).toInt())
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            p.color = withAlpha(base, (255 * effOpacity).toInt())
        }
        canvas.save()
        canvas.clipPath(mantle, android.graphics.Region.Op.INTERSECT)
        canvas.drawRect(0f, l.apexY, l.w, l.baseCy + l.ry, p)
        canvas.restore()
        p.shader = null

        if (!autoShade) return

        // Naungan vertikal: permukaan depan sedikit gelap menjelang alas.
        val shade = Paint(Paint.ANTI_ALIAS_FLAG)
        shade.style = Paint.Style.FILL
        shade.shader = LinearGradient(
            0f, l.apexY, 0f, l.baseCy + l.ry,
            intArrayOf(
                Color.TRANSPARENT,
                withAlpha(Color.BLACK, (60 * effOpacity * si).toInt())
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.clipPath(mantle, android.graphics.Region.Op.INTERSECT)
        canvas.drawRect(0f, l.apexY, l.w, l.baseCy + l.ry, shade)
        canvas.restore()
        shade.shader = null
    }

    /** 4. Band/Stripe reflektif horizontal pada permukaan depan. */
    private fun drawStripes(canvas: Canvas, l: ConeLayout, effOpacity: Float) {
        if (stripeMode == ConeStripeMode.NONE) return
        val centers = when (stripeMode) {
            ConeStripeMode.NONE -> FloatArray(0)
            ConeStripeMode.ONE -> floatArrayOf(0.30f)
            ConeStripeMode.TWO -> floatArrayOf(0.20f, 0.44f)
        }
        val frac = (stripeWidth / l.hgt).coerceIn(0.03f, 0.12f)

        for (c in centers) {
            val f0 = (c - frac * 0.5f).coerceAtLeast(0.0f)
            val f1 = (c + frac * 0.5f).coerceAtMost(0.92f)
            if (f1 - f0 < 0.005f) continue
            val outer = stripeOvalRect(l, f1)
            val inner = stripeOvalRect(l, f0)

            val band = Path().apply {
                addArc(outer, 0f, 180f)
                addArc(inner, 180f, -180f)
                close()
            }

            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(
                0f, outer.centerY(), 0f, outer.bottom,
                intArrayOf(
                    withAlpha(shadeWhite(stripeColor, 0.30f), (235 * effOpacity).toInt()),
                    withAlpha(shadeBlack(stripeColor, 0.20f), (255 * effOpacity).toInt()),
                    withAlpha(shadeBlack(stripeColor, 0.45f), (235 * effOpacity).toInt())
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(band, p)
            p.shader = null
        }
    }

    /** 5. Specular highlight di sisi terang (kiri-atas). */
    private fun drawSpecular(canvas: Canvas, l: ConeLayout, effOpacity: Float, si: Float) {
        if (!autoShade || si <= 0.03f) return
        val mantle = mantlePath(l)

        // Band kilau tipis mengikuti sisi kiri selimut.
        val span = l.rx * 0.34f
        val grad = LinearGradient(
            l.cx - l.rx, 0f, l.cx - l.rx + span * 2f, 0f,
            intArrayOf(
                withAlpha(Color.WHITE, (0).coerceAtLeast(0)),
                withAlpha(Color.WHITE, (150 * effOpacity * si).toInt()),
                withAlpha(Color.WHITE, (20 * effOpacity * si).toInt()),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        canvas.save()
        canvas.clipPath(mantle, android.graphics.Region.Op.INTERSECT)
        canvas.drawRect(l.cx - l.rx, l.apexY, l.cx - l.rx + span * 2f, l.baseCy + l.ry, p)
        canvas.restore()
        p.shader = null

        // Titik puncak bercahaya transparan lambat.
        val tip = Paint(Paint.ANTI_ALIAS_FLAG)
        tip.style = Paint.Style.FILL
        tip.shader = RadialGradient(
            l.cx - l.rx * 0.35f, l.apexY + l.hgt * 0.4f,
            max(l.rx * 0.7f, 0.1f),
            intArrayOf(
                withAlpha(Color.WHITE, (70 * effOpacity * si).toInt()),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.clipPath(mantle, android.graphics.Region.Op.INTERSECT)
        canvas.drawRect(0f, l.apexY, l.w, l.baseCy + l.ry, tip)
        canvas.restore()
        tip.shader = null
    }

    /** Rim alas: busur depan yang menegaskan dasar kerucut menempel ke tanah. */
    private fun drawBaseRim(canvas: Canvas, l: ConeLayout, effOpacity: Float) {
        val base = effectiveBase()
        val baseRect = baseOvalRect(l)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(0.9f, l.ry * 0.08f)
        p.color = withAlpha(shadeBlack(base, 0.5f), (150 * effOpacity).toInt())
        canvas.drawArc(baseRect, 0f, 180f, false, p)
    }

    /** Neon rim di sepanjang siluet kerucut. */
    private fun drawNeonRim(canvas: Canvas, l: ConeLayout, effOpacity: Float) {
        val glow = if (style == ConeStyle.CYBER_NEON) {
            (if (neonEnabled) neonColor else (if (materialType == ConeMaterial.NEON) baseColor else neonColor))
        } else {
            neonColor
        }
        val a = (150 * neonIntensity.coerceIn(0.1f, 3f) * effOpacity).toInt().coerceIn(0, 255)
        val baseRect = baseOvalRect(l)
        val mantle = mantlePath(l)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        for ((w, aa) in listOf(max(7f, l.rx * 0.16f) to (a / 2), max(3f, l.rx * 0.07f) to a)) {
            p.strokeWidth = w
            p.color = withAlpha(glow, aa)
            canvas.drawPath(mantle, p)
            canvas.drawArc(baseRect, 0f, 180f, false, p)
        }
        // Puncak bercahaya.
        p.strokeWidth = max(1.6f, l.rx * 0.04f)
        p.color = withAlpha(shadeWhite(glow, 0.4f), (220 * effOpacity).toInt())
        canvas.drawCircle(l.cx, l.apexY, max(2.5f, l.rx * 0.05f), p)
    }

    /** 6+8. Wireframe kerucut: siluet, alas, dan garis level tengah. */
    private fun drawWireframe(canvas: Canvas, l: ConeLayout, effOpacity: Float) {
        val alpha = (255 * wireStrokeOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        val baseRect = baseOvalRect(l)
        val mantle = mantlePath(l)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = wireStrokeWidth.coerceIn(0.5f, 12f)
        p.color = withAlpha(wireColor, alpha)
        canvas.drawPath(mantle, p)
        canvas.drawOval(baseRect, p)
        val mid = stripeOvalRect(l, 0.5f)
        canvas.drawOval(mid, p)
        // garis simetri puncak–alas
        canvas.drawLine(l.cx, l.apexY, l.cx, l.baseCy, p)
    }

    // ── Bayangan ──────────────────────────────────────────────────────────────

    private fun drawFloorShadow(canvas: Canvas, l: ConeLayout, effOpacity: Float) {
        val elevFactor = (floatingElevation / (l.rx * 0.9f)).coerceIn(0f, 1f)
        val alpha = (255 * floorShadowOpacity.coerceIn(0f, 1f) * (1f - 0.5f * elevFactor) * effOpacity)
            .toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        val cy = l.baseCy + l.ry + floatingElevation.coerceAtMost(l.rx * 0.6f) * 0.5f
        paintShadow(canvas, l.cx, cy, l.rx, alpha)
    }

    private fun drawEngineShadow(canvas: Canvas, l: ConeLayout, effOpacity: Float) {
        val alpha = (255 * shadowOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        paintShadow(
            canvas,
            l.cx + shadowDx.coerceIn(-30f, 30f),
            l.baseCy + l.ry * 0.9f + shadowDy.coerceIn(-30f, 30f),
            l.rx * 0.9f,
            alpha
        )
    }

    /** Bayangan elips lembut di lantai di bawah kerucut. */
    private fun paintShadow(canvas: Canvas, cx: Float, cy: Float, rx: Float, alpha: Int) {
        if (rx <= 0f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, rx * 1.15f,
            intArrayOf(
                Color.argb(alpha, 0, 0, 0),
                Color.argb((alpha * 0.55f).toInt(), 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(1f, 0.6f)
        canvas.drawCircle(0f, 0f, rx, p)
        canvas.restore()
        p.shader = null
    }

    /** Halo neon di belakang cone. */
    private fun drawNeonHalo(canvas: Canvas, l: ConeLayout, effOpacity: Float) {
        val glow = if (style == ConeStyle.CYBER_NEON) {
            (if (neonEnabled) neonColor else (if (materialType == ConeMaterial.NEON) baseColor else neonColor))
        } else {
            neonColor
        }
        val rex = l.rx + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f)
        val rey = l.ry + l.hgt + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f) * 0.5f
        val alpha = (110 * neonIntensity.coerceIn(0.1f, 2.5f) * effOpacity).toInt().coerceIn(0, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            l.cx, l.apexY + l.hgt / 2f, max(rex, 0.1f),
            intArrayOf(
                withAlpha(glow, alpha),
                withAlpha(glow, (alpha * 0.4f).toInt()),
                withAlpha(glow, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(l.cx, l.apexY + l.hgt / 2f)
        canvas.scale(1f, max(rey / max(rex, 0.1f), 0.2f))
        canvas.drawCircle(0f, 0f, rex, p)
        canvas.restore()
        p.shader = null
    }

    // ── Util warna ────────────────────────────────────────────────────────────

    /** Warna efektif badan sesuai material. */
    private fun effectiveBase(): Int {
        return when (materialType) {
            ConeMaterial.MATTE -> baseColor
            ConeMaterial.GLOSSY -> baseColor
            ConeMaterial.METALLIC_GOLD -> 0xFFFFD200.toInt()
            ConeMaterial.CHROME_SILVER -> 0xFFEDEFF5.toInt()
            ConeMaterial.NEON -> baseColor
        }
    }

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