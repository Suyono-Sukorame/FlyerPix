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
import com.flyerpix.editor.canvas.renderer.EffectRenderUtils
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Preset gaya podium panggung produk — mengubah palet warna badan, permukaan
 * panggung, serta aksen ring/neon agar tampil seperti material sungguhan.
 *  - [MINIMAL_STUDIO] — Panggung putih studio matte (Apple/Uniqlo vibes).
 *  - [LUXURY_GOLD]    — Pilar emas mengkilap dengan kilau logam elegan.
 *  - [DARK_ELEGANCE]  — Hitam/marmer gelap mewah untuk kosmetik & parfum.
 *  - [PASTEL_POP]     — Warna pastel ceria untuk makanan/fashion.
 *  - [CYBER_NEON]     — Panggung neon cyberpunk dengan ring & halo berpendar.
 */
enum class PodiumStyle {
    MINIMAL_STUDIO,
    LUXURY_GOLD,
    DARK_ELEGANCE,
    PASTEL_POP,
    CYBER_NEON
}

/**
 * Layer objek 3D Cylinder / Podium (panggung pemajang produk) dengan geometri
 * vektor analitik murni di Android Canvas:
 *
 *  1. Floor shadow      — elips bayangan lantai (RadialGradient) di dasar panggung.
 *  2. Dinding silinder  — Path gabungan elips atas & bawah, diisi LinearGradient
 *                         horizontal multi-stop (kiri teduh → kilau vertikal →
 *                         kanan bayangan) simulasikan kelengkungan tabung.
 *  3. Top Cap panggung  — elips permukaan atas yang lebih terang (+20%) dengan
 *                         bevel kedalaman & opsi ring emas di bibirnya.
 *  4. Multi-tier podium — opsi [tierCount] 2 = silinder bawah lebih lebar sebagai
 *                         undakan, podium utama lebih kecil sebagai puncak.
 *
 * Konvensi [lightAngle] sama dengan Sphere3D: arah datang cahaya pada bidang
 * layar searah jarum jam dari atas (0° atas, 90° kanan, -90° kiri). Default
 * -45° = cahaya kiri-atas sehingga kilau vertikal pilar sedikit ke kiri.
 */
data class Cylinder3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Geometri Silinder ────────────────────────────────────────────────────
    var radiusX: Float = 140f,        // setengah lebar podium (sumbu X elips)
    var radiusY: Float = 45f,         // kemiringan pandang (perspective tilt <-> sumbu Y elips)
    var cylinderHeight: Float = 90f,  // tinggi dinding silinder
    var tierCount: Int = 1,           // 1 = single stage, 2 = double tiered podium
    var tierRatio: Float = 0.75f,     // rasio ukuran tingkat atas (lebih kecil dari bawah)
    // ── Gaya & Warna ─────────────────────────────────────────────────────────
    var style: PodiumStyle = PodiumStyle.MINIMAL_STUDIO,
    var baseColor: Int = 0xFFF3F4F6.toInt(),   // warna badan silinder
    var topColor: Int? = null,                 // null = auto +20% terang dari baseColor
    var topRingEnabled: Boolean = true,        // lingkaran bibir panggung (bevel/ring)
    var topRingColor: Int = 0xFFD4AF37.toInt(),
    var topRingWidth: Float = 3f,
    // ── Pencahayaan ──────────────────────────────────────────────────────────
    var lightAngle: Float = -45f,
    // ── Bayangan Lantai ──────────────────────────────────────────────────────
    var floorShadowEnabled: Boolean = true,
    var floorShadowOpacity: Float = 0.5f,
    // ── Neon (glow ring panggung) ────────────────────────────────────────────
    override var neonEnabled: Boolean = false,
    override var neonColor: Int = 0xFF00E5FF.toInt(),
    override var neonRadius: Float = 18f,
    override var neonIntensity: Float = 1f,
    // ── Transformasi Warping (tidak dipakai untuk podium) ───────────────────
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

    /** Hasil perhitungan geometri podium pada ukuran konten saat ini. */
    data class CylinderLayout(
        val w: Float,
        val h: Float,
        val cx: Float,
        val capCenterY: Float,
        val mainBottomY: Float,
        val mainRx: Float,
        val lowerRx: Float,
        val lowerTopY: Float,
        val lowerBottomY: Float,
        val lowerHeight: Float
    )

    /** Tinggi silinder utama (nilai efektif positif). */
    private fun mainHeight(): Float = cylinderHeight.coerceAtLeast(1f)

    /** Tinggi undakan bawah (hanya relevan saat [tierCount] == 2). */
    private fun lowerHeight(): Float =
        if (tierCount >= 2) (mainHeight() * 0.45f).coerceAtLeast(12f) else 0f

    /** Menghitung seluruh tata letak geometri podium dalam ruang lokal. */
    fun computeLayout(): CylinderLayout {
        val mainRx = radiusX.coerceAtLeast(1f)
        val ry = radiusY.coerceAtLeast(1f)
        val mh = mainHeight()
        val lh = lowerHeight()
        val lrx = lowerRx().coerceAtLeast(mainRx)
        val capCenterY = ry
        val mainBottomY = capCenterY + mh
        val lowerTopY = mainBottomY
        val lowerBottomY = lowerTopY + lh
        val h = lowerBottomY + ry
        val w = lrx * 2f
        return CylinderLayout(
            w = w,
            h = h,
            cx = w / 2f,
            capCenterY = capCenterY,
            mainBottomY = mainBottomY,
            mainRx = mainRx,
            lowerRx = lrx,
            lowerTopY = lowerTopY,
            lowerBottomY = lowerBottomY,
            lowerHeight = lh
        )
    }

    /** Jejari silinder bawah (lebih lebar dari podium utama). */
    private fun lowerRx(): Float =
        if (tierCount >= 2) radiusX / tierRatio.coerceIn(0.4f, 0.95f) else radiusX

    // ─────────────────────────────────────────────────────────────────────────
    // Geometri & Bounds
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.w, l.h)
    }

    override fun getBounds(): RectF {
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return RectF(x, y, x + w, y + h)
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

    override fun copyLayer(): Cylinder3DLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        topColor = this.topColor,
        perspectiveCorners = this.perspectiveCorners.clone()
    ).also {
        it.stretchX = this.stretchX
        it.stretchY = this.stretchY
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
    // Helper untuk handle interaktif (PixelCanvasView)
    // ─────────────────────────────────────────────────────────────────────────

    /** Posisi handle ukuran (sisi dinding kanan): mendatar = diameter, tegak = tinggi. */
    fun getSizeHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.w, l.capCenterY + mainHeight() * 0.5f)
    }

    /** Posisi handle kemiringan (bibir panggung atas kanan): tegak = tilt. */
    fun getTiltHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.w, l.capCenterY - radiusY.coerceAtLeast(1f))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val l = computeLayout()
        val w = l.w
        val h = l.h
        if (w <= 0f || h <= 0f) return

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
        val cx = l.cx

        // Neon halo di belakang panggung (paling belakang).
        if (neonEnabled) {
            drawNeonHalo(canvas, cx, l.capCenterY, l.mainRx, effOpacity)
        }

        // Drop shadow engine (bayangan lembut di belakang podium).
        if (shadowEnabled && shadowOpacity > 0f && shadowRadius > 0f) {
            drawEngineShadow(canvas, cx, if (tierCount >= 2) l.lowerBottomY else l.mainBottomY, l, effOpacity)
        }

        // 1. Bayangan lantai.
        if (floorShadowEnabled && floorShadowOpacity > 0f) {
            drawFloorShadow(canvas, cx, if (tierCount >= 2) l.lowerBottomY else l.mainBottomY, l)
        }

        // 2. Undakan bawah (double tier) — digambar duluan, ditimpa podium utama.
        if (tierCount >= 2) {
            drawWall(canvas, l, cx, l.lowerRx, l.lowerTopY, l.lowerHeight, effOpacity, isTop = false)
        }

        // 3. Podium utama (dinding + panggung atas + ring + efek finishing).
        drawWall(canvas, l, cx, l.mainRx, l.capCenterY, mainHeight(), effOpacity, isTop = true)

        // Reset paint bersama.
        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null

        canvas.restoreToCount(saveCount)
    }

    /** Halo neon lembut di belakang panggung (hardware-safe, RadialGradient). */
    private fun drawNeonHalo(canvas: Canvas, cx: Float, cy: Float, rx: Float, effOpacity: Float) {
        val ry = radiusY.coerceAtLeast(1f)
        val glowR = rx + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f)
        val alpha = (255 * neonIntensity.coerceIn(0.1f, 2.5f) / 2.5f * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        val k = (ry / rx).coerceIn(0.35f, 1.4f)
        val colors = intArrayOf(
            withAlpha(neonColor, alpha),
            withAlpha(neonColor, (alpha * 0.5f).toInt()),
            withAlpha(neonColor, 0)
        )
        val positions = floatArrayOf(0f, 0.5f, 1f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, glowR, colors, positions, Shader.TileMode.CLAMP)
        canvas.drawOval(
            RectF(cx - glowR, cy - glowR * k, cx + glowR, cy + glowR * k),
            p
        )
        p.shader = null
    }

    /**
     * Drop shadow lembut di belakang podium (dari [CanvasLayer.shadowEnabled] dan
     * kawan-kawannya): elips gelap off-center yang ikut [shadowDx]/[shadowDy].
     */
    private fun drawEngineShadow(
        canvas: Canvas,
        cx: Float,
        bottomCenterY: Float,
        l: CylinderLayout,
        effOpacity: Float
    ) {
        val baseRx = if (tierCount >= 2) l.lowerRx else l.mainRx
        val rx = baseRx * (0.62f + 0.10f * (shadowRadius.coerceIn(0f, 40f) / 40f))
        val ry = rx * 0.30f
        val dx = shadowDx.coerceIn(-30f, 30f)
        val dy = shadowDy.coerceIn(-30f, 30f)
        val alpha = (255 * shadowOpacity.coerceIn(0f, 1f) * (0.55f + 0.45f * (shadowRadius.coerceIn(1f, 40f) / 40f)) * effOpacity)
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
        canvas.translate(cx + dx, bottomCenterY + radiusY.coerceAtLeast(1f) * 0.35f + dy)
        canvas.scale(1f, ry / rx)
        canvas.drawCircle(0f, 0f, gradR, p)
        canvas.restore()
        p.shader = null
    }

    /**
     * Bayangan lantai kontak: elips radial-gradient tepat di dasar podium
     * (menapak kokoh). Sedikit bergeser berlawanan arah cahaya.
     */
    private fun drawFloorShadow(
        canvas: Canvas,
        cx: Float,
        bottomCenterY: Float,
        l: CylinderLayout
    ) {
        val baseRx = if (tierCount >= 2) l.lowerRx else l.mainRx
        val rx = baseRx * 1.05f
        val ry = rx * 0.30f
        val effOpacity = opacity.coerceIn(0, 255) / 255f
        val alpha = (255 * floorShadowOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        val th = Math.toRadians(lightAngle.toDouble())
        val shiftX = -sin(th).toFloat() * rx * 0.18f
        val shadowCy = bottomCenterY + radiusY.coerceAtLeast(1f) * 0.55f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        val colors = intArrayOf(
            Color.argb(alpha, 0, 0, 0),
            Color.argb((alpha * 0.55f).toInt(), 0, 0, 0),
            Color.argb(0, 0, 0, 0)
        )
        val positions = floatArrayOf(0f, 0.55f, 1f)
        val gradR = rx * 1.15f
        p.shader = RadialGradient(0f, 0f, gradR, colors, positions, Shader.TileMode.CLAMP)
        canvas.save()
        canvas.translate(cx + shiftX, shadowCy)
        canvas.scale(1f, ry / rx)
        canvas.drawCircle(0f, 0f, gradR, p)
        canvas.restore()
        p.shader = null
    }

    /**
     * Menggambar satu silinder: dinding (wall) + panggung atas (cap). Ring,
     * emboss, dan neon hanya dipasang pada podium tertinggi ([isTop]).
     */
    private fun drawWall(
        canvas: Canvas,
        l: CylinderLayout,
        cx: Float,
        rx: Float,
        topCenterY: Float,
        height: Float,
        effOpacity: Float,
        isTop: Boolean
    ) {
        val ry = radiusY.coerceAtLeast(1f)
        val bottomCenterY = topCenterY + height
        val topRect = RectF(cx - rx, topCenterY - ry, cx + rx, topCenterY + ry)
        val bottomRect = RectF(cx - rx, bottomCenterY - ry, cx + rx, bottomCenterY + ry)

        // ── Dinding silinder: patched path (sisi kiri/kanan + busur depan bawah +
        // busur belakang atas) diisi LinearGradient horizontal multi-stop.
        val wall = Path()
        wall.moveTo(cx + rx, topCenterY)
        wall.lineTo(cx + rx, bottomCenterY)
        wall.arcTo(bottomRect, 0f, 180f)
        wall.lineTo(cx - rx, topCenterY)
        wall.arcTo(topRect, 180f, 180f)
        wall.close()

        val (colors, positions) = buildWallGradient()
        val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        wallPaint.style = Paint.Style.FILL
        wallPaint.alpha = ff2(effOpacity)
        wallPaint.shader = LinearGradient(
            cx - rx, 0f, cx + rx, 0f, colors, positions, Shader.TileMode.CLAMP
        )
        canvas.drawPath(wall, wallPaint)
        wallPaint.shader = null

        // Penegas tepi kiri/kanan dinding agar siluet tabung tegas.
        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        edgePaint.style = Paint.Style.STROKE
        edgePaint.strokeWidth = max(1f, rx * 0.015f)
        edgePaint.color = Color.argb((70 * ff2(effOpacity)).toInt(), 0, 0, 0)
        canvas.drawLine(cx + rx, topCenterY, cx + rx, bottomCenterY, edgePaint)
        canvas.drawLine(cx - rx, topCenterY, cx - rx, bottomCenterY, edgePaint)

        // Bibir bawah dinding (busur depan) — garis tipis gelap bantu "menapak".
        val lipPaint = Paint(edgePaint)
        lipPaint.color = Color.argb((120 * ff2(effOpacity)).toInt(), 0, 0, 0)
        lipPaint.strokeWidth = max(1f, rx * 0.02f)
        canvas.drawArc(bottomRect, 0f, 180f, false, lipPaint)

        // Permukaan panggung atas.
        drawTopCap(canvas, cx, rx, ry, topCenterY, effOpacity)

        if (isTop) {
            drawTopTrim(canvas, topRect, rx, ry, effOpacity)
            drawTopEffects(canvas, l, topRect, effOpacity)
        }
    }

    /** Permukaan panggung atas: gradien vertikal + bevel kedalaman di bibir. */
    private fun drawTopCap(
        canvas: Canvas,
        cx: Float,
        rx: Float,
        ry: Float,
        topCenterY: Float,
        effOpacity: Float
    ) {
        val topRect = RectF(cx - rx, topCenterY - ry, cx + rx, topCenterY + ry)
        val derived = topColor ?: shadeWhite(baseColor, 0.20f)
        val back = shadeBlack(derived, 0.10f)
        val front = shadeWhite(derived, 0.06f)
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        capPaint.style = Paint.Style.FILL
        capPaint.alpha = ff2(effOpacity)
        capPaint.shader = LinearGradient(
            0f, topRect.top, 0f, topRect.bottom, back, front, Shader.TileMode.CLAMP
        )
        canvas.drawOval(topRect, capPaint)
        capPaint.shader = null

        // Bevel kedalaman: elips dalam sedikit lebih gelap di bibir panggung.
        val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bevelPaint.style = Paint.Style.STROKE
        bevelPaint.strokeWidth = max(1f, rx * 0.025f)
        bevelPaint.color = Color.argb((60 * ff2(effOpacity)).toInt(), 0, 0, 0)
        val inner = RectF(
            cx - rx * 0.82f, topRect.top + ry * 0.18f,
            cx + rx * 0.82f, topRect.bottom - ry * 0.18f
        )
        canvas.drawOval(inner, bevelPaint)
    }

    /** Ring bibir panggung (standar / neon color bila glowing). */
    private fun drawTopTrim(
        canvas: Canvas,
        topRect: RectF,
        rx: Float,
        ry: Float,
        effOpacity: Float
    ) {
        if (!topRingEnabled || topRingWidth <= 0f) return
        val ringColor = if (neonEnabled) neonColor else topRingColor
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = topRingWidth.coerceAtLeast(0.5f)
        ringPaint.color = ringColor
        ringPaint.alpha = ff2(effOpacity)
        canvas.drawOval(topRect, ringPaint)

        // Garis luar tipis (mengikuti cahaya) memberi kesan logam.
        val outerPaint = Paint(ringPaint)
        outerPaint.strokeWidth = (topRingWidth.coerceAtLeast(0.5f) * 0.35f)
        outerPaint.color = shadeBlack(ringColor, 0.35f)
        outerPaint.alpha = (ff2(effOpacity) * 0.8f).toInt().coerceIn(0, 255)
        canvas.drawOval(
            RectF(
                topRect.left - topRingWidth * 0.8f, topRect.top - topRingWidth * 0.8f,
                topRect.right + topRingWidth * 0.8f, topRect.bottom + topRingWidth * 0.8f
            ),
            outerPaint
        )
    }

    /** Efek finishing ring: Emboss bevel relief (jika aktif). */
    private fun drawTopEffects(
        canvas: Canvas,
        l: CylinderLayout,
        topRect: RectF,
        effOpacity: Float
    ) {
        if (!embossEnabled || !topRingEnabled || topRingWidth <= 0f) return
        EffectRenderUtils.drawEmbossEffect(
            canvas, this, l.w, l.h,
            if (neonEnabled) neonColor else topRingColor,
            drawContentBase = { c, pnt ->
                drawEmbossRing(c, pnt, topRect, effOpacity)
            },
            drawContentEmboss = { c, pnt ->
                drawEmbossRing(c, pnt, topRect, effOpacity)
            }
        )
    }

    private fun drawEmbossRing(canvas: Canvas, pnt: Paint, topRect: RectF, effOpacity: Float) {
        pnt.style = Paint.Style.STROKE
        pnt.strokeWidth = topRingWidth.coerceAtLeast(0.5f) * 3f
        pnt.alpha = ff2(effOpacity)
        pnt.shader = null
        pnt.maskFilter = null
        canvas.drawOval(topRect, pnt)
    }

    /** Gradien dinding silinder multi-stop; kilau vertikal mengikuti [lightAngle]. */
    private fun buildWallGradient(): Pair<IntArray, FloatArray> {
        val th = Math.toRadians(lightAngle.toDouble())
        val hl = (0.5f + sin(th).toFloat() * 0.32f).coerceIn(0.18f, 0.82f)
        val positions = floatArrayOf(
            0f,
            (hl - 0.18f).coerceAtLeast(0.02f),
            hl,
            (hl + 0.32f).coerceAtMost(0.96f),
            1f
        )
        val colors = intArrayOf(
            shadeBlack(baseColor, 0.20f),
            shadeBlack(baseColor, 0.05f),
            shadeWhite(baseColor, 0.42f),
            shadeBlack(baseColor, 0.42f),
            shadeBlack(baseColor, 0.25f)
        )
        return colors to positions
    }

    // ── Utilitas warna ───────────────────────────────────────────────────────

    private fun ff2(a: Float): Int = (a.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)

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