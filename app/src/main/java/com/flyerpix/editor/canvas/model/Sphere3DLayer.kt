package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import java.util.UUID
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Preset material permukaan bola — mengubah parameter optik (gradisen diffuse,
 * kilau specular, refleksi) agar tampil seperti bahan sungguhan.
 *  - [MATTE]        — doff/beludru, bayangan lembut tanpa kilau tajam.
 *  - [GLOSSY]       — plastik mengkilap, kilau specular kontras.
 *  - [METALLIC]     — logam/krom, kontras tinggi + titik kilau pekat kecil.
 *  - [GLASS_CRYSTAL] — kaca bening, interior semi-transparan + refleksi ganda.
 *  - [NEON_GLOW]    — orb cahaya berpendar dari dalam ke luar.
 */
enum class SphereMaterial {
    MATTE,
    GLOSSY,
    METALLIC,
    GLASS_CRYSTAL,
    NEON_GLOW
}

/**
 * Layer objek 3D Sphere (bola) dengan pencahayaan vektor matematis berbasis
 * radial gradient multi-stop (Photorealistic & Stylized 2D Vector Phong):
 *
 *  1. Floor shadow   — elips bayangan lantai (RadialGradient) di bawah bola.
 *  2. Diffuse body   — RadialGradient 4-stop (highlight → midtone → core shadow
 *                      → rim/bounce light) yang fokus-nya mengikuti arah cahaya.
 *  3. Specular spot  — elips kilau putih lembut di titik pantulan.
 *  4. Outline        — lingkaran tepi opsional (gaya ilustrasi kartun 3D).
 *
 * Konvensi sudut [lightAngle]: arah titik kilau pada bidang layar, diukur
 * searah jarum jam dari sumbu atas (0° = atas, 90° = kanan, 180° = bawah,
 * -90° = kiri). Default -45° = kilau di kiri-atas (cahaya datang dari sana).
 * Bayangan lantai bergeser ke sisi berlawanan arah kilau (fisika realistis).
 */
data class Sphere3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Geometri ─────────────────────────────────────────────────────────────
    var radius: Float = 120f,
    // ── Material & Warna ─────────────────────────────────────────────────────
    var material: SphereMaterial = SphereMaterial.GLOSSY,
    var baseColor: Int = 0xFF1769FF.toInt(),
    var specularColor: Int = Color.WHITE,
    // ── Pencahayaan ──────────────────────────────────────────────────────────
    var lightAngle: Float = -45f,      // derajat posisi kilau (-90=kiri, 0=atas, 90=kanan)
    var lightDistance: Float = 0.45f,  // 0.0..0.8 rasio radius (jauhnya fokus)
    var lightIntensity: Float = 1.0f,  // 0.5..2.0 kekuatan pencahayaan
    // ── Bayangan Lantai ──────────────────────────────────────────────────────
    var floorShadowEnabled: Boolean = true,
    var floorElevation: Float = 20f,   // tinggi bola melayang dari lantai (px)
    var floorShadowOpacity: Float = 0.5f,
    // ── Outline Kartun ───────────────────────────────────────────────────────
    var strokeWidth: Float = 0f,
    var strokeColor: Int = Color.BLACK,
    var strokeOpacity: Int = 255,
    // ── Neon Orb (menyala saat neonEnabled) ──────────────────────────────────
    override var neonEnabled: Boolean = false,
    override var neonColor: Int = 0xFF00E5FF.toInt(),
    override var neonRadius: Float = 16f,
    override var neonIntensity: Float = 1f,
    // ── Transformasi Warping (tidak dipakai untuk bola) ─────────────────────
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
    // Geometri & Bounds
    // ─────────────────────────────────────────────────────────────────────────

    /** Ruang vertikal (px) yang dicadangkan untuk bayangan lantai di bawah bola. */
    fun floorSpace(): Float = (floorElevation * 1.4f).coerceAtMost(radius)

    override fun getUnwarpedDimensions(): Pair<Float, Float> =
        Pair(radius * 2f, radius * 2f + floorSpace())

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

    override fun copyLayer(): Sphere3DLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        perspectiveCorners = this.perspectiveCorners.clone()
    ).also {
        it.stretchX = this.stretchX
        it.stretchY = this.stretchY
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper pencahayaan & handle interaktif
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Posisi titik fokus kilau (light highlight) dalam koordinat lokal.
     * Konvensi: offset = (sin θ, −cos θ)·dist·R sehingga lightAngle −45° (kiri-atas)
     * menempatkan kilau tepat di kiri-atas bola.
     */
    fun getHighlightLocal(): Pair<Float, Float> {
        val r = radius
        val th = Math.toRadians(lightAngle.toDouble())
        val ldist = lightDistance.coerceIn(0f, 0.8f) * r
        return Pair(r + sin(th).toFloat() * ldist, r - cos(th).toFloat() * ldist)
    }

    /** Invers transformasi layer: memetakan titik kanvas (px, py) ke ruang lokal. */
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

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val r = radius
        if (r <= 0f) return
        val fs = floorSpace()
        val w = r * 2f
        val h = w + fs

        // Sanitasi paint bersama di AWAL.
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.pathEffect = null
        paint.shader = null
        paint.maskFilter = null
        paint.setShadowLayer(0f, 0f, 0f, 0)

        val saveCount = canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale * stretchX, scale * stretchY, w / 2f, h / 2f)
        canvas.rotate(rotation, w / 2f, h / 2f)

        val effOpacity = opacity.coerceIn(0, 255) / 255f

        // 1. Neon orb halo (bagian belakang, di bawah bola).
        if (neonEnabled) {
            drawNeonHalo(canvas, r)
        }

        // 2. Bayangan lantai (di bawah bola).
        if (floorShadowEnabled && floorShadowOpacity > 0f) {
            drawFloorShadow(canvas, r, fs, effOpacity)
        }

        // 3. Badan bola (diffuse + specular).
        drawSphereBody(canvas, r, effOpacity)

        // 4. Refleksi ganda kaca.
        if (material == SphereMaterial.GLASS_CRYSTAL) {
            drawGlassReflections(canvas, r, effOpacity)
        }

        // 5. Outline kartun.
        if (strokeWidth > 0f) {
            val sp = Paint(Paint.ANTI_ALIAS_FLAG)
            sp.style = Paint.Style.STROKE
            sp.strokeWidth = strokeWidth.coerceAtLeast(0.5f)
            sp.color = strokeColor
            sp.alpha = (strokeOpacity.coerceIn(0, 255) * effOpacity).toInt().coerceIn(0, 255)
            sp.shader = null
            sp.maskFilter = null
            canvas.drawCircle(r, r, r - strokeWidth / 2f, sp)
        }

        // Reset paint bersama.
        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null

        canvas.restoreToCount(saveCount)
    }

    /** Halo neon lembut di belakang bola (bisa aktif kapan saja, hardware-safe). */
    private fun drawNeonHalo(canvas: Canvas, r: Float) {
        val glowColor = if (material == SphereMaterial.NEON_GLOW) baseColor else neonColor
        val glowR = r + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f)
        val alpha = (255 * neonIntensity.coerceIn(0.1f, 2.5f) / 2.5f).toInt().coerceIn(0, 255)
        val colors = intArrayOf(
            withAlpha(glowColor, alpha),
            withAlpha(glowColor, (alpha * 0.55f).toInt()),
            withAlpha(glowColor, 0)
        )
        val positions = floatArrayOf(0f, 0.45f, 1f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(r, r, glowR, colors, positions, Shader.TileMode.CLAMP)
        canvas.drawCircle(r, r, glowR, p)
        p.shader = null
    }

    /**
     * Bayangan lantai mengambang: elips radial-gradient yang memudar, ukuran &
     * opasitas merespons tinggi melayang; posisi bergeser berlawanan arah kilau.
     */
    private fun drawFloorShadow(canvas: Canvas, r: Float, fs: Float, effOpacity: Float) {
        val elevFactor = (fs / r).coerceIn(0f, 1f)
        val rx = r * (0.55f + 0.45f * elevFactor)
        val ry = rx * 0.30f
        val alpha = (255 * floorShadowOpacity.coerceIn(0f, 1f) * (1f - 0.55f * elevFactor) * effOpacity)
            .toInt().coerceIn(0, 255)
        if (alpha <= 0) return

        val th = Math.toRadians(lightAngle.toDouble())
        // Bayangan bereaksi berlawanan arah kilau: bergeser ke sisi gelap.
        val shiftX = -sin(th).toFloat() * r * (0.35f + 0.35f * elevFactor)
        val shiftY = cos(th).toFloat() * r * 0.28f * (0.5f + 0.5f * elevFactor)
        val shadowCx = r + shiftX
        var shadowCy = r + r * 0.30f + fs * 0.45f + shiftY
        // Jaga agar elips bayangan tidak keluar dari area konten.
        shadowCy = shadowCy.coerceIn(ry * 0.5f, 2f * r + fs - ry * 0.4f)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        val colors = intArrayOf(
            Color.argb(alpha, 0, 0, 0),
            Color.argb((alpha * 0.55f).toInt(), 0, 0, 0),
            Color.argb(0, 0, 0, 0)
        )
        val positions = floatArrayOf(0f, 0.55f, 1f)
        val radiusG = rx * 1.1f
        p.shader = RadialGradient(0f, 0f, radiusG, colors, positions, Shader.TileMode.CLAMP)
        canvas.save()
        canvas.translate(shadowCx, shadowCy)
        canvas.scale(1f, ry / rx)
        canvas.drawCircle(0f, 0f, radiusG, p)
        canvas.restore()
        p.shader = null
    }

    /** Badan bola: diffuse radial 4-stop + kilau specular sesuai material. */
    private fun drawSphereBody(canvas: Canvas, r: Float, effOpacity: Float) {
        val th = Math.toRadians(lightAngle.toDouble())
        val ldist = lightDistance.coerceIn(0f, 0.8f) * r
        val cx = r
        val cy = r
        val lx = cx + sin(th).toFloat() * ldist
        val ly = cy - cos(th).toFloat() * ldist
        val li = lightIntensity.coerceIn(0.5f, 2f)

        val (colors, positions) = gradientForMaterial(li, effOpacity)
        val gradRadius = r + ldist

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(lx, ly, gradRadius, colors, positions, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, p)
        p.shader = null

        // Kilau specular (elips putih lembut) — MATTE & NEON tidak berkilau tajam.
        when (material) {
            SphereMaterial.GLOSSY ->
                drawSpecularSpot(canvas, lx, ly, r * 0.17f, r * 0.14f, 0.90f * (0.6f + 0.4f * li), th)
            SphereMaterial.METALLIC ->
                drawSpecularSpot(canvas, lx, ly, r * 0.11f, r * 0.09f, 0.98f * (0.6f + 0.4f * li), th)
            SphereMaterial.GLASS_CRYSTAL -> {
                drawSpecularSpot(canvas, lx, ly, r * 0.08f, r * 0.06f, 0.95f, th)
            }
            else -> {}
        }
    }

    /** Gradien diffuse 4-stop per material — parameter "vektor phong" 2D. */
    private fun gradientForMaterial(li: Float, effOpacity: Float): Pair<IntArray, FloatArray> {
        val pos = floatArrayOf(0f, 0.42f, 0.82f, 1f)
        val colors = when (material) {
            SphereMaterial.MATTE -> intArrayOf(
                shadeWhite(baseColor, 0.30f * li),
                baseColor,
                shadeBlack(baseColor, 0.40f),
                shadeWhite(baseColor, 0.08f)
            )
            SphereMaterial.GLOSSY -> intArrayOf(
                shadeWhite(baseColor, 0.52f * li),
                baseColor,
                shadeBlack(baseColor, 0.55f),
                shadeWhite(baseColor, 0.16f)
            )
            SphereMaterial.METALLIC -> intArrayOf(
                shadeWhite(baseColor, 0.72f * li),
                shadeWhite(baseColor, 0.12f),
                shadeBlack(baseColor, 0.72f),
                shadeWhite(baseColor, 0.12f)
            )
            SphereMaterial.GLASS_CRYSTAL -> intArrayOf(
                withAlpha(shadeWhite(baseColor, 0.65f), (0.80f * effOpacity).coerceIn(0f, 1f)),
                withAlpha(shadeWhite(baseColor, 0.30f), (0.45f * effOpacity).coerceIn(0f, 1f)),
                withAlpha(shadeBlack(baseColor, 0.12f), (0.30f * effOpacity).coerceIn(0f, 1f)),
                withAlpha(shadeWhite(baseColor, 0.55f), (0.85f * effOpacity).coerceIn(0f, 1f))
            )
            SphereMaterial.NEON_GLOW -> intArrayOf(
                shadeWhite(baseColor, 0.85f * li),
                shadeWhite(baseColor, 0.25f),
                shadeBlack(baseColor, 0.25f),
                shadeBlack(baseColor, 0.35f)
            )
        }
        return colors to pos
    }

    /** Kilau specular elips dengan radial-gradient lembut (tanpa BlurMaskFilter). */
    private fun drawSpecularSpot(
        canvas: Canvas,
        lx: Float,
        ly: Float,
        rx: Float,
        ry: Float,
        alphaRatio: Float,
        thetaLight: Double
    ) {
        if (rx <= 0.5f || ry <= 0.5f || alphaRatio <= 0.02f) return
        val alpha = (255 * alphaRatio.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        val c0 = withAlpha(specularColor, alpha)
        val c1 = withAlpha(specularColor, (alpha * 0.55f).toInt())
        val c2 = withAlpha(specularColor, 0)
        val grad = RadialGradient(0f, 0f, rx, intArrayOf(c0, c1, c2), null, Shader.TileMode.CLAMP)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        canvas.save()
        canvas.translate(lx, ly)
        canvas.rotate(Math.toDegrees(thetaLight).toFloat())
        canvas.scale(1f, ry / rx)
        canvas.drawCircle(0f, 0f, rx, p)
        canvas.restore()
        p.shader = null
    }

    /** Refleksi ganda kaca: dua lengkungan terang tipis di tepi atas & bawah. */
    private fun drawGlassReflections(canvas: Canvas, r: Float, effOpacity: Float) {
        val th = Math.toRadians(lightAngle.toDouble())
        val lx = r + sin(th).toFloat() * r * 0.45f
        val ly = r - cos(th).toFloat() * r * 0.45f
        val alphaBase = (200 * effOpacity).toInt().coerceIn(0, 255)
        val streak = Paint(Paint.ANTI_ALIAS_FLAG)
        streak.style = Paint.Style.STROKE
        streak.strokeWidth = r * 0.05f
        streak.strokeCap = Paint.Cap.ROUND
        streak.color = withAlpha(specularColor, alphaBase)
        streak.shader = null

        // Lengkungan atas (kebalikan dari arah cahaya) & bawah (searah cahaya).
        val backLx = r - sin(th).toFloat() * r * 0.35f
        val backLy = r + cos(th).toFloat() * r * 0.35f
        val rect1 = RectF(backLx - r * 0.62f, backLy - r * 0.62f, backLx + r * 0.62f, backLy + r * 0.62f)
        canvas.drawArc(rect1, -150f, 70f, false, streak)

        val rect2 = RectF(lx - r * 0.55f, ly - r * 0.55f, lx + r * 0.55f, ly + r * 0.55f)
        canvas.drawArc(rect2, 20f, 60f, false, streak)
    }

    // ── Utilitas warna ───────────────────────────────────────────────────────

    private fun withAlpha(color: Int, a: Float): Int {
        val ar = (a.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
        return (ar shl 24) or (color and 0x00FFFFFF)
    }

    private fun withAlpha(color: Int, a: Int): Int {
        val ar = a.coerceIn(0, 255)
        return (ar shl 24) or (color and 0x00FFFFFF)
    }

    private fun shadeWhite(color: Int, t: Float): Int {
        val t0 = t.coerceIn(0f, 1f)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return Color.rgb(
            (r + (255 - r) * t0).toInt(),
            (g + (255 - g) * t0).toInt(),
            (b + (255 - b) * t0).toInt()
        )
    }

    private fun shadeBlack(color: Int, t: Float): Int {
        val t0 = t.coerceIn(0f, 1f)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return Color.rgb(
            (r * (1f - t0)).toInt(),
            (g * (1f - t0)).toInt(),
            (b * (1f - t0)).toInt()
        )
    }
}