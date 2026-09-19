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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Gaya tampilan utama cincin 3D Torus. */
enum class TorusStyle {
    MODERN_ABSTRACT, // cincin plastik minimalis melayang
    LUXURY_JEWELRY,  // cincin emas/perak perhiasan + permata
    SWEET_DONUT,     // donat roti manis + krim leleh + meses
    CYBER_NEON       // portal halo sci-fi berpendar
}

/** Preset material permukaan cincin 3D. */
enum class TorusMaterial {
    GLOSSY,         // plastik mengkilap
    MATTE,          // doff minimalis
    METALLIC_GOLD,  // logam emas krom berkilau
    CHROME_SILVER,  // krom perak menyilaukan
    NEON            // badan warna neon berpendar
}

/**
 * Layer objek 3D Torus (Donat / Cincin 3D) — pipa tertutup berlubang.
 *
 * Secara geometris cincin diproyeksikan sebagai dua elips konsentris:
 *  - Elips LUAR berjari-jari (R + r) → [ox] × [oy] (batas tertinggi/bawah cincin).
 *  - Elips DALAM (lubang) berjari-jari (R − r) → [ix] × [iy].
 *  - Elips tengah pipa [mx] × [my] menjadi referensi icing/permata/specular.
 *
 * Kemiringan 3D ([tiltAngle]) menekan sumbu vertikal dengan cos(θ) sehingga
 * cincin bundar dari atas (0°) perlahan menjadi elips miring (75°).
 *
 * Urutan render:
 *  1. Bayangan lantai cincin berlubang (Donut Shadow) di bawah.
 *  2. Halo neon di belakang (style CYBER_NEON / neonEnabled).
 *  3. Busur BELAKANG (Back Arc = separuh atas) dengan gradien lebih gelap.
 *  4. Interior lubang: dinding belakang gelap + rim okklusi.
 *  5. Busur DEPAN (Front Arc = separuh bawah) dengan gradien utama terang.
 *  6. Krim leleh bergelombang + meses (style SWEET_DONUT).
 *  7. Batu permata prisma di puncak (style LUXURY_JEWELRY).
 *  8. Kilau melengkung 360° (tubular arc highlight) + rim outline.
 */
data class Torus3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Dimensi & Sudut 3D ───────────────────────────────────────────────────
    var majorRadius: Float = 130f,     // dari pusat lubang ke sumbu pipa (R)
    var tubeThickness: Float = 38f,    // ketebalan daging pipa (r)
    var tiltAngle: Float = 40f,        // 0° datar s/d 75° miring
    var spinAngle: Float = 0f,         // 0°..360° posisi highlight/permata
    // ── Gaya & Mode Tampilan ─────────────────────────────────────────────────
    var style: TorusStyle = TorusStyle.MODERN_ABSTRACT,
    var materialType: TorusMaterial = TorusMaterial.METALLIC_GOLD,
    var baseColor: Int = 0xFFFFD700.toInt(),
    // ── Khusus Donat Bakery ──────────────────────────────────────────────────
    var icingEnabled: Boolean = true,
    var icingColor: Int = 0xFFFF69B4.toInt(),
    var sprinklesEnabled: Boolean = true,
    var sprinkleDensity: Int = 24,
    // ── Khusus Cincin Perhiasan ──────────────────────────────────────────────
    var gemEnabled: Boolean = false,
    var gemColor: Int = 0xFF00E5FF.toInt(),
    var gemSize: Float = 28f,
    // ── Pencahayaan & Bayangan ───────────────────────────────────────────────
    var specularIntensity: Float = 0.85f,
    var floorShadowEnabled: Boolean = true,
    var floatingElevation: Float = 20f,
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

    data class TorusLayout(
        val major: Float,
        val minor: Float,
        val cs: Float,
        val ox: Float,
        val oy: Float,
        val ix: Float,
        val iy: Float,
        val mx: Float,
        val my: Float,
        val w: Float,
        val h: Float,
        val cx: Float,
        val cy: Float,
        val tiltDeg: Float,
        val spinDeg: Float
    )

    fun computeLayout(): TorusLayout {
        val R = majorRadius.coerceAtLeast(24f)
        val r = tubeThickness.coerceIn(6f, R * 0.85f)
        val tilt = tiltAngle.coerceIn(0f, 75f)
        val cs = cos(Math.toRadians(tilt.toDouble())).toFloat().coerceAtLeast(0.18f)
        val ox = R + r
        val oy = ox * cs
        val ix = max(R - r, 2f)
        val iy = ix * cs
        val mx = R
        val my = R * cs
        val w = ox * 2f
        val h = oy * 2f
        return TorusLayout(
            major = R, minor = r, cs = cs,
            ox = ox, oy = oy,
            ix = ix, iy = iy,
            mx = mx, my = my,
            w = w, h = h,
            cx = w / 2f, cy = h / 2f,
            tiltDeg = tilt,
            spinDeg = ((spinAngle % 360f) + 360f) % 360f
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

    override fun copyLayer(): Torus3DLayer = this.copy(
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

    // Handle interaktif (titik kanvas lokal)
    fun getSizeHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.cx + l.ox, l.cy)
    }

    fun getThicknessHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.cx, l.cy + l.oy)
    }

    fun getTiltHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.cx, l.cy - l.oy - 32f)
    }

    fun normAngle(deg: Float): Float = ((deg % 360f) + 360f) % 360f

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
        val isNeon = neonEnabled || style == TorusStyle.CYBER_NEON ||
            materialType == TorusMaterial.NEON

        // 1. Drop shadow engine.
        if (shadowEnabled && shadowOpacity > 0f && shadowRadius > 0f) {
            drawEngineShadow(canvas, l, effOpacity)
        }

        // 2. Bayangan lantai cincin berlubang.
        if (floorShadowEnabled && floorShadowOpacity > 0f) {
            drawFloorShadow(canvas, l, effOpacity)
        }

        // 3. Halo neon di belakang.
        if (isNeon) {
            drawNeonHalo(canvas, l, effOpacity)
        }

        // 4..8. Cincin 3D (bujur miring tertanam di layout).
        drawBackArc(canvas, l, effOpacity)
        drawHoleInterior(canvas, l, effOpacity)
        drawFrontArc(canvas, l, effOpacity, si)

        if (style == TorusStyle.SWEET_DONUT && icingEnabled) {
            drawIcing(canvas, l, effOpacity, si)
        }
        if (style == TorusStyle.LUXURY_JEWELRY && gemEnabled) {
            drawGem(canvas, l, effOpacity)
        }

        drawSpecularArc(canvas, l, effOpacity, si)
        drawRimOutline(canvas, l, effOpacity)
        if (isNeon) {
            drawNeonRim(canvas, l, effOpacity)
        }

        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null
        canvas.restoreToCount(saveCount)
    }

    /** Path cincin berlubang (annulus) dengan FillType EVEN_ODD. */
    private fun annulusPath(l: TorusLayout): Path {
        val outer = RectF(l.cx - l.ox, l.cy - l.oy, l.cx + l.ox, l.cy + l.oy)
        val inner = RectF(l.cx - l.ix, l.cy - l.iy, l.cx + l.ix, l.cy + l.iy)
        return Path().apply {
            addOval(outer, Path.Direction.CW)
            addOval(inner, Path.Direction.CW)
            fillType = Path.FillType.EVEN_ODD
        }
    }

    private fun topHalfClip(canvas: Canvas, l: TorusLayout, block: () -> Unit) {
        canvas.save()
        canvas.clipRect(0f, 0f, l.w, l.cy)
        block()
        canvas.restore()
    }

    private fun bottomHalfClip(canvas: Canvas, l: TorusLayout, block: () -> Unit) {
        canvas.save()
        canvas.clipRect(0f, l.cy, l.w, l.h)
        block()
        canvas.restore()
    }

    /** 4. Busur belakang (Back Arc = separuh atas) — gelap, permukaan menjauh. */
    private fun drawBackArc(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val base = effectiveBase()
        val bright = if (materialType == TorusMaterial.METALLIC_GOLD ||
            materialType == TorusMaterial.CHROME_SILVER
        ) 0.30f else 0.26f
        val dark = if (materialType == TorusMaterial.METALLIC_GOLD ||
            materialType == TorusMaterial.CHROME_SILVER
        ) 0.52f else 0.40f
        val top = l.cy - l.oy
        val grad = LinearGradient(
            0f, top, 0f, l.cy + l.oy,
            intArrayOf(
                withAlpha(shadeBlack(base, 0.62f), (255 * effOpacity).toInt()),
                withAlpha(shadeBlack(base, dark), (235 * effOpacity).toInt()),
                withAlpha(shadeBlack(base, bright), (200 * effOpacity).toInt())
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        topHalfClip(canvas, l) {
            canvas.drawPath(annulusPath(l), p)
        }
        p.shader = null
    }

    /** 5. Interior lubang: dinding belakang gelap + rim overhang. */
    private fun drawHoleInterior(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val inner = RectF(l.cx - l.ix, l.cy - l.iy, l.cx + l.ix, l.cy + l.iy)
        val base = effectiveBase()
        // Dinding belakang lubang: gelap di atas, jernih menuju bawah.
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, l.cy - l.iy, 0f, l.cy + l.iy,
            intArrayOf(
                withAlpha(shadeBlack(base, 0.78f), (210 * effOpacity).toInt()),
                withAlpha(shadeBlack(base, 0.45f), (120 * effOpacity).toInt()),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(inner, p)
        p.shader = null

        // Rim okklusi (overhang) di tepi atas lubang: gelap menggelantung.
        val rim = Paint(Paint.ANTI_ALIAS_FLAG)
        rim.style = Paint.Style.STROKE
        rim.strokeWidth = max(1.2f, l.minor * 0.16f)
        rim.color = withAlpha(shadeBlack(base, 0.85f), (150 * effOpacity).toInt())
        canvas.drawOval(inner, rim)
    }

    /** 6. Busur depan (Front Arc = separuh bawah) — gradien utama terang. */
    private fun drawFrontArc(canvas: Canvas, l: TorusLayout, effOpacity: Float, si: Float) {
        val base = effectiveBase()
        val bright = if (materialType == TorusMaterial.METALLIC_GOLD) 0.50f
        else if (materialType == TorusMaterial.CHROME_SILVER) 0.58f else 0.32f
        val dark = if (materialType == TorusMaterial.METALLIC_GOLD) 0.42f
        else if (materialType == TorusMaterial.CHROME_SILVER) 0.30f else 0.20f
        val grad = LinearGradient(
            0f, l.cy - l.oy, 0f, l.cy + l.oy,
            intArrayOf(
                withAlpha(shadeWhite(base, bright), (255 * effOpacity).toInt()),
                withAlpha(base, (250 * effOpacity).toInt()),
                withAlpha(shadeBlack(base, dark), (225 * effOpacity).toInt())
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        bottomHalfClip(canvas, l) {
            canvas.drawPath(annulusPath(l), p)
        }
        p.shader = null

        // Naungan dasar front arc (bagian bawah lebih gelap karena membelok).
        val p2 = Paint(Paint.ANTI_ALIAS_FLAG)
        p2.style = Paint.Style.FILL
        p2.shader = LinearGradient(
            0f, l.cy, 0f, l.cy + l.oy,
            intArrayOf(Color.TRANSPARENT, withAlpha(Color.BLACK, (70 * effOpacity * si).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        bottomHalfClip(canvas, l) {
            canvas.drawPath(annulusPath(l), p2)
        }
        p2.shader = null
    }

    /** 6b. Krim leleh bergelombang + taburan meses (mode donat). */
    private fun drawIcing(canvas: Canvas, l: TorusLayout, effOpacity: Float, si: Float) {
        val st = 46
        val phase = l.spinDeg
        val wob = max(l.minor * 0.34f, 3f)
        val drip = max(l.minor * 0.55f, 4f)
        // Sisi krim: dari elips tengah-pipa (mx,my) menggelantung ke luar sedikit.
        val wx = l.major - l.minor * 0.06f
        val wy = wx * l.cs

        val path = Path()
        // Tepi atas (outer rim) dari kiri (180°) ke kanan (0°) melewati puncak.
        val steps = 60
        for (i in 0..steps) {
            val deg = 180.0 - 180.0 * i / steps
            val rad = Math.toRadians(deg)
            val px = l.cx + l.ox * cos(rad).toFloat()
            val py = l.cy - l.oy * sin(rad).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        // Tepi bawah bergelombang dari kanan (0°) ke kiri (180°):
        // titik menyusuri elips tengah dengan wobble dan tetesan krim.
        for (i in 0..steps) {
            val deg = 0.0 + 180.0 * i / steps
            val rad = Math.toRadians(deg)
            val px = l.cx + wx * cos(rad).toFloat()
            val phaseR = Math.toRadians((deg + phase).toDouble())
            val w1 = wob * (sin(rad * 2.6 + phaseR * 0.4)).toFloat()
            val w2 = drip * (0.5f + 0.5f * sin(rad * 4.0 + phaseR * 0.7).toFloat()) *
                (0.35f + 0.65f * abs(cos(rad)).toFloat())
            val py = l.cy - wy * sin(rad).toFloat() + w1 + w2
            path.lineTo(px, py)
        }
        path.close()

        val grad = LinearGradient(
            0f, l.cy - l.oy, 0f, l.cy,
            intArrayOf(
                withAlpha(shadeWhite(icingColor, 0.18f), (250 * effOpacity).toInt()),
                withAlpha(icingColor, (255 * effOpacity).toInt()),
                withAlpha(shadeBlack(icingColor, 0.22f), (240 * effOpacity).toInt())
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        canvas.drawPath(path, p)
        p.shader = null

        // Garis kilau tipis pada krim.
        val glossP = Paint(Paint.ANTI_ALIAS_FLAG)
        glossP.style = Paint.Style.STROKE
        glossP.strokeWidth = max(1f, l.minor * 0.06f)
        glossP.color = withAlpha(Color.WHITE, (120 * effOpacity * si).toInt())
        canvas.drawPath(path, glossP)

        if (!sprinklesEnabled) return
        drawSprinkles(canvas, l, effOpacity, wx, wy)
    }

    /** Butiran meses warna-warni tersebar di atas lapisan krim. */
    private val SPRINKLE_PALETTE = intArrayOf(
        0xFFEF5350.toInt(), 0xFFFFCA28.toInt(), 0xFF66BB6A.toInt(),
        0xFF29B6F6.toInt(), 0xFFAB47BC.toInt(), 0xFFFF7043.toInt(),
        0xFFFFFFFF.toInt()
    )

    private fun drawSprinkles(canvas: Canvas, l: TorusLayout, effOpacity: Float, wx: Float, wy: Float) {
        val n = sprinkleDensity.coerceIn(6, 48)
        val seed = (l.spinDeg / 7f).toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = max(1.4f, l.minor * 0.10f)
        for (k in 0 until n) {
            val jitter = ((k * 31 + seed * 7) % 97) / 97f
            val deg = 12.0 + 156.0 * ((k * 53 + seed * 13) % 100) / 100.0
            val rad = Math.toRadians(deg)
            val t = 0.18f + 0.55f * (((k * 17 + seed * 5) % 61) / 61f)
            val rx = wx + (l.ox - wx) * t
            val cx2 = l.cx + rx * cos(rad.toFloat())
            val cy2 = l.cy - (rx * l.cs) * sin(rad.toFloat()) - l.minor * 0.12f * (0.5f + jitter)
            val dang = Math.toRadians((deg + (k % 3) * 40.0).toDouble())
            val dx = cos(dang).toFloat() * l.minor * 0.18f
            val dy = sin(dang).toFloat() * l.minor * 0.18f
            p.color = withAlpha(
                SPRINKLE_PALETTE[k % SPRINKLE_PALETTE.size],
                (200 * effOpacity).toInt()
            )
            canvas.drawLine(cx2 - dx, cy2 - dy, cx2 + dx, cy2 + dy, p)
        }
    }

    /** 7. Batu permata prisma berlian di puncak (mengikuti putaran cincin). */
    private fun drawGem(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val phi = Math.toRadians((-90.0 + l.spinDeg).toDouble())
        val gx = l.cx + l.mx * cos(phi).toFloat()
        val gy = l.cy + l.my * sin(phi).toFloat()
        // Sembunyikan permata bila berada di sisi belakang (atas).
        if (gy < l.cy - l.minor * 0.4f) return
        val s = gemSize.coerceIn(10f, 60f)

        val bright = shadeWhite(gemColor, 0.35f)
        val mid = gemColor
        val dark = shadeBlack(gemColor, 0.35f)
        val glow = shadeWhite(gemColor, 0.6f)

        // Siluet prisma (brilliant cut).
        val topY = gy - s * 0.46f
        val waistY = gy - s * 0.06f
        val bottomY = gy + s * 0.42f
        val halfW = s * 0.42f
        val shape = Path()
        shape.moveTo(gx, topY)
        shape.lineTo(gx + halfW, waistY)
        shape.lineTo(gx + halfW * 0.55f, bottomY)
        shape.lineTo(gx, bottomY + s * 0.22f)
        shape.lineTo(gx - halfW * 0.55f, bottomY)
        shape.lineTo(gx - halfW, waistY)
        shape.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            gx - halfW, topY, gx + halfW, bottomY,
            intArrayOf(
                withAlpha(bright, (255 * effOpacity).toInt()),
                withAlpha(mid, (245 * effOpacity).toInt()),
                withAlpha(dark, (230 * effOpacity).toInt())
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(shape, paint)
        paint.shader = null

        // Facet line utama.
        val line = Paint(Paint.ANTI_ALIAS_FLAG)
        line.style = Paint.Style.STROKE
        line.strokeWidth = max(1f, s * 0.05f)
        line.color = withAlpha(glow, (170 * effOpacity).toInt())
        canvas.drawLine(gx, topY, gx - halfW, waistY, line)
        canvas.drawLine(gx, topY, gx + halfW, waistY, line)
        canvas.drawLine(gx, waistY, gx - halfW * 0.55f, bottomY, line)
        canvas.drawLine(gx, waistY, gx + halfW * 0.55f, bottomY, line)

        // Kilau bintang di puncak.
        val spark = Paint(Paint.ANTI_ALIAS_FLAG)
        spark.style = Paint.Style.STROKE
        spark.strokeWidth = max(1f, s * 0.07f)
        spark.strokeCap = Paint.Cap.ROUND
        spark.color = withAlpha(Color.WHITE, (235 * effOpacity).toInt())
        val sr = s * 0.5f
        canvas.drawLine(gx - sr, gy - s * 0.2f, gx + sr, gy - s * 0.2f, spark)
        canvas.drawLine(gx, gy - s * 0.2f - sr, gx, gy - s * 0.2f + sr, spark)
    }

    /** 8. Kilau tubular melengkung 360° + crest luar + rim dalam. */
    private fun drawSpecularArc(canvas: Canvas, l: TorusLayout, effOpacity: Float, si: Float) {
        if (si <= 0.02f) return

        // Crest luar: band kilau di sepanjang bibir luar (mengikuti pipa).
        val crestRx = l.ox - l.minor * 0.34f
        val crestRy = crestRx * l.cs
        val crestRect = RectF(l.cx - crestRx, l.cy - crestRy, l.cx + crestRx, l.cy + crestRy)
        val band = max(l.minor * 0.42f, 2f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = band
        p.shader = LinearGradient(
            0f, l.cy - l.oy, 0f, l.cy + l.oy,
            intArrayOf(
                withAlpha(Color.WHITE, (46 * effOpacity * si).toInt()),
                withAlpha(Color.WHITE, (25 * effOpacity * si).toInt()),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        bottomHalfClip(canvas, l) {
            canvas.drawOval(crestRect, p)
        }
        p.shader = null

        // Crest belakang: garis terang tipis di sepanjang puncak atas (360° backdrop).
        val p2 = Paint(Paint.ANTI_ALIAS_FLAG)
        p2.style = Paint.Style.STROKE
        p2.strokeWidth = max(0.8f, l.minor * 0.07f)
        p2.shader = LinearGradient(
            0f, l.cy - l.oy, 0f, l.cy + l.oy,
            intArrayOf(
                withAlpha(Color.WHITE, (170 * effOpacity * si).toInt()),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        topHalfClip(canvas, l) {
            canvas.drawOval(crestRect, p2)
        }
        p2.shader = null

        // Crest dalam (depan lubang): garis terang tipis di tepi bawah lubang.
        val innerRect = RectF(l.cx - l.ix, l.cy - l.iy, l.cx + l.ix, l.cy + l.iy)
        val p3 = Paint(Paint.ANTI_ALIAS_FLAG)
        p3.style = Paint.Style.STROKE
        p3.strokeWidth = max(0.8f, l.minor * 0.06f)
        p3.shader = LinearGradient(
            0f, l.cy - l.iy, 0f, l.cy + l.iy,
            intArrayOf(Color.TRANSPARENT, withAlpha(Color.WHITE, (120 * effOpacity * si).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        bottomHalfClip(canvas, l) {
            canvas.drawOval(innerRect, p3)
        }
        p3.shader = null
    }

    /** Outline tipis luar-dalam agar cincin tegas. */
    private fun drawRimOutline(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val alpha = (130 * effOpacity).toInt()
        val outline = Paint(Paint.ANTI_ALIAS_FLAG)
        outline.style = Paint.Style.STROKE
        outline.strokeWidth = max(0.8f, l.minor * 0.05f)
        outline.color = withAlpha(shadeBlack(effectiveBase(), 0.55f), alpha)
        canvas.drawOval(RectF(l.cx - l.ox, l.cy - l.oy, l.cx + l.ox, l.cy + l.oy), outline)
        canvas.drawOval(RectF(l.cx - l.ix, l.cy - l.iy, l.cx + l.ix, l.cy + l.iy), outline)
    }

    /** Neon rim + portal glow (mode cyber). */
    private fun drawNeonRim(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val base = if (style == TorusStyle.CYBER_NEON) {
            if (neonEnabled) neonColor else (if (materialType == TorusMaterial.NEON) baseColor else neonColor)
        } else {
            neonColor
        }
        val a = (150 * neonIntensity.coerceIn(0.1f, 3f) * effOpacity).toInt().coerceIn(0, 255)
        val outer = RectF(l.cx - l.ox, l.cy - l.oy, l.cx + l.ox, l.cy + l.oy)
        val inner = RectF(l.cx - l.ix, l.cy - l.iy, l.cx + l.ix, l.cy + l.iy)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        for ((w, aa) in listOf(max(8f, l.minor * 0.45f) to (a / 2), max(3.5f, l.minor * 0.18f) to a)) {
            p.strokeWidth = w
            p.color = withAlpha(base, aa)
            canvas.drawOval(outer, p)
            canvas.drawOval(inner, p)
        }
        // Busur energi orbital (portal).
        val arcW = l.minor * 0.5f
        val arcRect = RectF(l.cx - l.ox * 0.92f, l.cy - l.oy * 0.92f, l.cx + l.ox * 0.92f, l.cy + l.oy * 0.92f)
        p.strokeWidth = max(1.6f, arcW * 0.25f)
        p.color = withAlpha(shadeWhite(base, 0.4f), (220 * effOpacity).toInt())
        val start = l.spinDeg
        canvas.drawArc(arcRect, start, 120f, false, p)
        canvas.drawArc(arcRect, start + 180f, 120f, false, p)
    }

    // ── Bayangan ──────────────────────────────────────────────────────────────

    private fun drawFloorShadow(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val elevFactor = (floatingElevation / (l.ox * 0.9f)).coerceIn(0f, 1f)
        val alpha = (255 * floorShadowOpacity.coerceIn(0f, 1f) * (1f - 0.5f * elevFactor) * effOpacity)
            .toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        paintShadowAnnulus(
            canvas,
            l.cx,
            l.cy + l.oy * 0.55f + floatingElevation.coerceAtMost(l.ox * 0.6f) * 0.5f,
            l.ox,
            l.ix,
            alpha
        )
    }

    private fun drawEngineShadow(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val alpha = (255 * shadowOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        paintShadowAnnulus(
            canvas,
            l.cx + shadowDx.coerceIn(-30f, 30f),
            l.cy + l.oy * 0.7f + shadowDy.coerceIn(-30f, 30f),
            l.ox * 0.86f,
            l.ix * 0.96f,
            alpha
        )
    }

    /** Bayangan cincin berlubang (elips luar - elips dalam) dengan pudar radial. */
    private fun paintShadowAnnulus(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ox: Float,
        ix: Float,
        alpha: Int
    ) {
        if (ix <= 0f || ox <= 0f) return
        val scaleY = 0.62f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, ox * 1.12f,
            intArrayOf(
                Color.argb(alpha, 0, 0, 0),
                Color.argb((alpha * 0.62f).toInt(), 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(1f, scaleY)
        val path = Path()
        path.addCircle(0f, 0f, ox, Path.Direction.CW)
        path.addCircle(0f, 0f, ix, Path.Direction.CW)
        path.fillType = Path.FillType.EVEN_ODD
        canvas.drawPath(path, p)
        canvas.restore()
        p.shader = null
    }

    private fun drawNeonHalo(canvas: Canvas, l: TorusLayout, effOpacity: Float) {
        val glow = if (style == TorusStyle.CYBER_NEON) {
            (if (neonEnabled) neonColor else (if (materialType == TorusMaterial.NEON) baseColor else neonColor))
        } else {
            neonColor
        }
        val rex = l.ox + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f)
        val rey = l.oy + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f) * 0.5f
        val alpha = (110 * neonIntensity.coerceIn(0.1f, 2.5f) * effOpacity).toInt().coerceIn(0, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            l.cx, l.cy, max(rex, 0.1f),
            intArrayOf(
                withAlpha(glow, alpha),
                withAlpha(glow, (alpha * 0.4f).toInt()),
                withAlpha(glow, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(l.cx, l.cy)
        canvas.scale(1f, max(rey / max(rex, 0.1f), 0.2f))
        canvas.drawCircle(0f, 0f, rex, p)
        canvas.restore()
        p.shader = null
    }

    // ── Util warna ────────────────────────────────────────────────────────────

    /** Warna efektif badan sesuai material (gold/silver/chrome). */
    private fun effectiveBase(): Int {
        return when (materialType) {
            TorusMaterial.GLOSSY -> baseColor
            TorusMaterial.MATTE -> baseColor
            TorusMaterial.METALLIC_GOLD -> 0xFFFFD200.toInt()
            TorusMaterial.CHROME_SILVER -> 0xFFEDEFF5.toInt()
            TorusMaterial.NEON -> baseColor
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