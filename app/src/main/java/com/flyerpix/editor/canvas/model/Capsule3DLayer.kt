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

/**
 * Mode tampilan kapsul.
 */
enum class CapsuleMode {
    CTA_BUTTON,     // Tombol CTA / badge diskon satu warna + teks & ikon
    TWO_TONE_PILL   // Kapsul medis dua warna dengan cincin sambungan
}

/** Preset material permukaan kapsul. */
enum class CapsuleMaterial {
    GLOSSY,        // plastik mengkilap, kilau specular tajam
    MATTE,         // doff halus ala pil obat
    METALLIC,      // logam perak/emas krom berkilau kontras
    CYBER_NEON     // futuristik, garis & isi berpendar neon
}

/** Ikon opsional pada permukaan tombol CTA. */
enum class ButtonIcon {
    NONE,
    CART,
    ARROW_RIGHT,
    FLASH,
    CHECK,
    STAR
}

/** Posisi ikon relatif terhadap teks tombol. */
enum class IconPosition {
    LEFT_OF_TEXT,
    RIGHT_OF_TEXT
}

/**
 * Layer objek 3D Capsule / Kapsul (stadium = silinder horizontal + dua kubah
 * setengah bola). Di dalam frame kapsul, badan membentang dari -effL/2..effL/2
 * (sumbu X) dengan tinggi 2·[capsuleRadius]. Seluruh kapsul digambar murni
 * matematis:
 *
 *  1. Floor shadow    — elips lantai memanjang yang memudar (Elevation).
 *  2. Body fill       — satu warna gradien (CTA) atau dua warna ter-split (Pill).
 *  3. Seam            — cincin sambungan tengah (mode Two-Tone).
 *  4. Shading vertikal— core shadow bawah + sinar atas (glossy volume).
 *  5. Hemisphere caps — radial highlight atas & bayangan kubah kedua ujung.
 *  6. Specular spine  — garis kilau putih memanjang di punggung kapsul.
 *  7. Neon           — rim berpendar (material CYBER_NEON / neonEnabled).
 *  8. CTA glyph       — ikon + teks promosi sejajar tengah (mode CTA).
 *
 * [rotationAngle] memiringkan kapsul di bidang gambar (0° = mendatar,
 * 90° = tegak) dan ikut dihitung pada bounds (AABB hasil rotasi kapsul).
 */
data class Capsule3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Dimensi & Orientasi ─────────────────────────────────────────────────
    var capsuleLength: Float = 240f,
    var capsuleRadius: Float = 45f,
    var rotationAngle: Float = 0f,     // 0°..360° miring kapsul di bidang
    // ── Mode & Pewarnaan ─────────────────────────────────────────────────────
    var mode: CapsuleMode = CapsuleMode.CTA_BUTTON,
    var material: CapsuleMaterial = CapsuleMaterial.GLOSSY,
    var primaryColor: Int = 0xFFE53935.toInt(),
    var secondaryColor: Int = 0xFFFFB300.toInt(),
    var splitRatio: Float = 0.5f,      // posisi pemisah 0.2..0.8
    var seamColor: Int = 0x33000000,
    // ── Teks & Ikon CTA ──────────────────────────────────────────────────────
    var buttonText: String = "BELI SEKARANG",
    var textColor: Int = Color.WHITE,
    var textSize: Float = 16f,
    var isTextBold: Boolean = true,
    var iconType: ButtonIcon = ButtonIcon.CART,
    var iconPosition: IconPosition = IconPosition.LEFT_OF_TEXT,
    // ── Pencahayaan & Bayangan ───────────────────────────────────────────────
    var lightAngle: Float = -45f,
    var specularIntensity: Float = 0.85f,
    var floatingElevation: Float = 15f,
    var floorShadowEnabled: Boolean = true,
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
    // Layout (frame akses kapsul + AABB hasil rotasi)
    // ─────────────────────────────────────────────────────────────────────────

    data class CapsuleLayout(
        val effL: Float,     // panjang efektif (>= 2r)
        val cr: Float,       // radius kapsul
        val cw: Float,       // AABB width
        val ch: Float,       // AABB height
        val cx: Float,
        val cy: Float,
        val cosR: Float,
        val sinR: Float,
        val rotDeg: Float
    )

    fun computeLayout(): CapsuleLayout {
        val cr = capsuleRadius.coerceAtLeast(8f)
        val effL = max(capsuleLength, cr * 2f)
        val t = Math.toRadians(((rotationAngle % 360f + 360f) % 360f).toDouble())
        val cosR = cos(t).toFloat()
        val sinR = sin(t).toFloat()
        val cw = effL * abs(cosR) + cr * 2f * abs(sinR)
        val ch = effL * abs(sinR) + cr * 2f * abs(cosR)
        return CapsuleLayout(
            effL = effL,
            cr = cr,
            cw = cw,
            ch = ch,
            cx = cw / 2f,
            cy = ch / 2f,
            cosR = cosR,
            sinR = sinR,
            rotDeg = ((rotationAngle % 360f) + 360f) % 360f
        )
    }

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        val l = computeLayout()
        return Pair(l.cw, l.ch)
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

    override fun copyLayer(): Capsule3DLayer = this.copy(
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

    /** Putar titik lokal mengikuti orientasi kapsul [rotationAngle]. */
    private fun rot(px: Float, py: Float, l: CapsuleLayout): Pair<Float, Float> {
        val dx = px - l.cx
        val dy = py - l.cy
        return Pair(l.cx + dx * l.cosR - dy * l.sinR, l.cy + dx * l.sinR + dy * l.cosR)
    }

    /** Invers rotasi kapsul: kembalikan titik lokal ke frame akses (X = sumbu panjang). */
    private fun unrot(px: Float, py: Float, l: CapsuleLayout): Pair<Float, Float> {
        val dx = px - l.cx
        val dy = py - l.cy
        return Pair(l.cx + dx * l.cosR + dy * l.sinR, l.cy - dx * l.sinR + dy * l.cosR)
    }

    /** Posisi handle ujung kanan kapsul (tarik untuk memanjang/memendek). */
    fun getEndHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return rot(l.cx + l.effL / 2f, l.cy, l)
    }

    /** Posisi handle putar (diamond di atas kapsul; seret untuk memiringkan). */
    fun getRotateHandleLocal(): Pair<Float, Float> {
        val l = computeLayout()
        return rot(l.cx, l.cy - l.cr - 40f, l)
    }

    /** Titik [px],[py] frame konten → frame sumbu kapsul (X = arah memanjang). */
    fun toCapsuleAxis(px: Float, py: Float, l: CapsuleLayout): Pair<Float, Float> {
        return unrot(px, py, l)
    }

    /** Normalisasi sudut ke rentang 0..360. */
    fun normAngle(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val l = computeLayout()
        if (l.cw <= 0f || l.ch <= 0f) return

        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.pathEffect = null
        paint.shader = null
        paint.maskFilter = null
        paint.setShadowLayer(0f, 0f, 0f, 0)

        val saveCount = canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale * stretchX, scale * stretchY, l.cw / 2f, l.ch / 2f)
        canvas.rotate(rotation, l.cw / 2f, l.ch / 2f)

        val effOpacity = opacity.coerceIn(0, 255) / 255f
        val si = specularIntensity.coerceIn(0f, 2f)

        // 1. Drop shadow engine (di belakang).
        if (shadowEnabled && shadowOpacity > 0f && shadowRadius > 0f) {
            drawEngineShadow(canvas, l, effOpacity)
        }

        // 2. Floor shadow lantai (frame konten, TIDAK ikut rotasi kapsul).
        if (floorShadowEnabled && floorShadowOpacity > 0f) {
            drawFloorShadow(canvas, l, effOpacity)
        }

        // 3. Halo neon di belakang kapsul.
        if (neonEnabled || material == CapsuleMaterial.CYBER_NEON) {
            drawNeonHalo(canvas, l, effOpacity)
        }

        // ── Badan kapsul (frame akses: translate + rotate(rotationAngle)) ──
        canvas.save()
        canvas.rotate(l.rotDeg, l.cx, l.cy)

        val body = buildCapsulePath(l, 0f)
        drawBodyFill(canvas, body, l)
        if (mode == CapsuleMode.TWO_TONE_PILL && splitRatio in 0.01f..0.99f) {
            drawTwoToneSplit(canvas, body, l)
        }
        drawVolumeShading(canvas, body, l)
        drawHemisphereShading(canvas, l)
        drawSpecularSpine(canvas, l, si)
        drawCoreShadowLite(canvas, l)
        drawNeonRim(canvas, body, l, effOpacity)
        drawOutline(canvas, body, l, effOpacity)

        // 8. Teks + ikon CTA.
        if (mode == CapsuleMode.CTA_BUTTON) {
            drawCTAGlyph(canvas, l)
        }

        canvas.restore()

        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null
        canvas.restoreToCount(saveCount)
    }

    /** Path kapsul (stadium) pada frame akses, translate ke (l.cx, l.cy) + offsetY. */
    private fun buildCapsulePath(l: CapsuleLayout, offsetY: Float): Path {
        val r = l.cr
        val Rc = l.effL / 2f - r
        val cx = l.cx
        val cy = l.cy + offsetY
        val path = Path()
        path.moveTo(cx - Rc, cy - r)
        path.lineTo(cx + Rc, cy - r)
        path.arcTo(RectF(cx + Rc - r, cy - r, cx + Rc + r, cy + r), -90f, 180f)
        path.lineTo(cx - Rc, cy + r)
        path.arcTo(RectF(cx - Rc - r, cy - r, cx - Rc + r, cy + r), 90f, 180f)
        path.close()
        return path
    }

    /** Path setengah kubah (atas/bawah) kiri/kanan untuk shading belahan. */
    private fun halfDisc(
        cx: Float, cy: Float, r: Float,
        right: Boolean, upper: Boolean
    ): Path {
        val path = Path()
        val capX = if (right) cx + r else cx - r  // ujung luar
        val p0x = cx
        val p0y = if (upper) cy - r else cy + r
        path.moveTo(p0x, p0y)
        if (upper) {
            // arc dari p0 (atas sth tengah) ke ujung luar lalu ke bawah-tengah
            path.arcTo(
                RectF(cx - r, cy - r, cx + r, cy + r),
                if (right) 270f else 270f, if (right) 90f else -90f
            )
            path.lineTo(cx, cy)
        } else {
            path.arcTo(
                RectF(cx - r, cy - r, cx + r, cy + r),
                if (right) 90f else 90f, if (right) -90f else 90f
            )
            path.lineTo(cx, cy)
        }
        path.close()
        return path
    }

    /** 1. Fill dasar badan — satu gradien (CTA) atau dua warna (Pill). */
    private fun drawBodyFill(canvas: Canvas, body: Path, l: CapsuleLayout) {
        val r = l.cr
        val top = l.cy - r
        val bot = l.cy + r
        val bright = if (material == CapsuleMaterial.METALLIC) 0.42f else 0.32f
        val dark = if (material == CapsuleMaterial.METALLIC) 0.30f else 0.18f
        val grad = LinearGradient(
            l.cx, top, l.cx, bot,
            intArrayOf(
                shadeWhite(primaryColor, bright),
                primaryColor,
                shadeBlack(primaryColor, dark)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        if (mode == CapsuleMode.TWO_TONE_PILL && splitRatio in 0.01f..0.99f) {
            val splitX = l.cx + (l.effL / 2f) * -1f + splitRatio * l.effL
            canvas.save()
            canvas.clipPath(body)
            canvas.save()
            canvas.clipRect(-4f, -4f, splitX, l.ch + 4f)
            canvas.drawPath(body, p)
            canvas.restore()
            val grad2 = LinearGradient(
                l.cx, top, l.cx, bot,
                intArrayOf(
                    shadeWhite(secondaryColor, bright),
                    secondaryColor,
                    shadeBlack(secondaryColor, dark)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            p.shader = grad2
            canvas.save()
            canvas.clipRect(splitX, -4f, l.cw + 4f, l.ch + 4f)
            canvas.drawPath(body, p)
            canvas.restore()
            canvas.restore()
        } else {
            canvas.drawPath(body, p)
        }
        p.shader = null
    }

    /** 2. Cincin sambungan tengah (mode Two-Tone). */
    private fun drawTwoToneSplit(canvas: Canvas, body: Path, l: CapsuleLayout) {
        val splitX = l.cx + l.effL * (splitRatio - 0.5f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.4f
        p.color = seamColor
        canvas.save()
        canvas.clipPath(body)
        canvas.drawLine(splitX, l.cy - l.cr, splitX, l.cy + l.cr, p)
        canvas.restore()
    }

    /** 3. Shading vertikal — core shadow bawah + band cahaya atas. */
    private fun drawVolumeShading(canvas: Canvas, body: Path, l: CapsuleLayout) {
        val r = l.cr
        val top = l.cy - r
        val bot = l.cy + r
        val soft = material == CapsuleMaterial.MATTE
        val shadowA = if (soft) 70 else if (material == CapsuleMaterial.METALLIC) 120 else 95
        val grad = LinearGradient(
            l.cx, top, l.cx, bot,
            intArrayOf(
                withAlpha(Color.WHITE, if (soft) 45 else 80),
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                withAlpha(Color.BLACK, shadowA),
                withAlpha(Color.BLACK, 40)
            ),
            floatArrayOf(0f, 0.18f, 0.62f, 0.92f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        canvas.drawPath(body, p)
        p.shader = null
    }

    /** 4. Shading kubah — kilau radial atas + bayangan bawah di kedua ujung. */
    private fun drawHemisphereShading(canvas: Canvas, l: CapsuleLayout) {
        val r = l.cr
        val cxR = l.cx + l.effL / 2f - r
        val cxL = l.cx - (l.effL / 2f - r)
        val soft = material == CapsuleMaterial.MATTE
        val shwA = if (soft) 45 else 80
        val shdA = if (soft) 60 else 90

        // Highlight radial atas di setiap kubah.
        run {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.shader = RadialGradient(
                0f, 0f, r * 1.5f,
                intArrayOf(withAlpha(Color.WHITE, shwA), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            for (capX in floatArrayOf(cxL, cxR)) {
                canvas.save()
                canvas.clipPath(halfDisc(capX, l.cy, r, right = capX > l.cx, upper = true))
                canvas.translate(capX, l.cy - r * 0.7f)
                canvas.drawCircle(0f, 0f, r * 1.5f, p)
                canvas.restore()
            }
            p.shader = null
        }
        // Bayangan bawah di setiap kubah.
        run {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.shader = RadialGradient(
                0f, 0f, r * 1.3f,
                intArrayOf(withAlpha(Color.BLACK, shdA), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            for (capX in floatArrayOf(cxL, cxR)) {
                canvas.save()
                canvas.clipPath(halfDisc(capX, l.cy, r, right = capX > l.cx, upper = false))
                canvas.translate(capX, l.cy + r * 0.85f)
                canvas.drawCircle(0f, 0f, r * 1.3f, p)
                canvas.restore()
            }
            p.shader = null
        }
    }

    /** 5. Kilau tubular (specular spine) di punggung kapsul. */
    private fun drawSpecularSpine(canvas: Canvas, l: CapsuleLayout, si: Float) {
        val r = l.cr
        var aTop = (230 * si).toInt().coerceIn(0, 255)
        var aBot = (40 * si).toInt().coerceIn(0, 255)
        if (material == CapsuleMaterial.MATTE) {
            aTop = (aTop * 0.30f).toInt()
            aBot = (aBot * 0.10f).toInt()
        }
        if (aTop <= 4) return

        val shiftX = sin(Math.toRadians(lightAngle.toDouble())).toFloat() * r * 0.18f
        val stripR = r * 0.34f
        val offY = -r * 0.30f
        val bodyLen = l.effL - r * 1.6f
        val path = Path()
        val Rq = bodyLen / 2f - stripR
        val sx = l.cx + shiftX
        val sy = l.cy + offY
        path.moveTo(sx - Rq, sy - stripR)
        path.lineTo(sx + Rq, sy - stripR)
        path.arcTo(RectF(sx + Rq - stripR, sy - stripR, sx + Rq + stripR, sy + stripR), -90f, 180f)
        path.lineTo(sx - Rq, sy + stripR)
        path.arcTo(RectF(sx - Rq - stripR, sy - stripR, sx - Rq + stripR, sy + stripR), 90f, 180f)
        path.close()

        val grad = LinearGradient(
            0f, sy - stripR, 0f, sy + stripR,
            intArrayOf(
                withAlpha(Color.WHITE, aTop),
                withAlpha(Color.WHITE, (aTop * 0.55f).toInt()),
                withAlpha(Color.WHITE, aBot)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        canvas.drawPath(path, p)
        p.shader = null

        // Inti kecil paling terang di puncak.
        val core = Paint(Paint.ANTI_ALIAS_FLAG)
        core.style = Paint.Style.FILL
        core.color = withAlpha(Color.WHITE, (160 * si).toInt().coerceIn(0, 255))
        val coreR = stripR * 0.30f
        val coreY = sy - stripR * 0.55f
        if (material != CapsuleMaterial.MATTE) {
            canvas.drawRoundRect(
                RectF(sx - Rq - coreR * 0.4f, coreY - coreR, sx + Rq + coreR * 0.4f, coreY + coreR),
                coreR, coreR, core
            )
        }
    }

    /** 6. Bayangan bawah tipis + bounce light di tepi dasar. */
    private fun drawCoreShadowLite(canvas: Canvas, l: CapsuleLayout) {
        val r = l.cr
        val offY = r * 0.42f
        val stripR = r * 0.30f
        val bodyLen = l.effL - r * 1.8f
        val path = Path()
        val Rq = bodyLen / 2f - stripR
        val sxc = l.cx
        val sy = l.cy + offY
        path.moveTo(sxc - Rq, sy - stripR)
        path.lineTo(sxc + Rq, sy - stripR)
        path.arcTo(RectF(sxc + Rq - stripR, sy - stripR, sxc + Rq + stripR, sy + stripR), -90f, 180f)
        path.lineTo(sxc - Rq, sy + stripR)
        path.arcTo(RectF(sxc - Rq - stripR, sy - stripR, sxc - Rq + stripR, sy + stripR), 90f, 180f)
        path.close()
        val grad = LinearGradient(
            0f, sy - stripR, 0f, sy + stripR,
            intArrayOf(Color.TRANSPARENT, withAlpha(Color.BLACK, 60)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = grad
        canvas.drawPath(path, p)
        p.shader = null

        // Bounce light tipis di dasar.
        val bounce = Paint(Paint.ANTI_ALIAS_FLAG)
        bounce.style = Paint.Style.STROKE
        bounce.strokeWidth = max(1.2f, r * 0.09f)
        bounce.color = withAlpha(Color.WHITE, 42)
        val bodyQ = buildCapsulePath(l, 0f)
        canvas.save()
        canvas.clipPath(bodyQ)
        canvas.drawLine(sxc - l.effL / 2f + r * 0.6f, l.cy + r * 0.80f, sxc + l.effL / 2f - r * 0.6f, l.cy + r * 0.80f, bounce)
        canvas.restore()
    }

    /** 7. Neon rim (halo menyala di sekeliling kapsul). */
    private fun drawNeonRim(canvas: Canvas, body: Path, l: CapsuleLayout, effOpacity: Float) {
        if (!neonEnabled && material != CapsuleMaterial.CYBER_NEON) return
        val glow = if (material == CapsuleMaterial.CYBER_NEON) {
            if (neonEnabled) neonColor else primaryColor
        } else {
            neonColor
        }
        val base = (110 * neonIntensity.coerceIn(0.1f, 3f) * effOpacity).toInt().coerceIn(0, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        for ((w, a) in listOf(10f to (base / 2), 5f to base, 2f to min(base + 60, 255))) {
            p.strokeWidth = w
            p.color = withAlpha(glow, a)
            canvas.drawPath(body, p)
        }
    }

    /** Outline tipis agar bentuk tegas di atas warna terang. */
    private fun drawOutline(canvas: Canvas, body: Path, l: CapsuleLayout, effOpacity: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1.2f, l.cr * 0.035f)
        p.color = withAlpha(shadeBlack(primaryColor, 0.55f), (150 * effOpacity).toInt().coerceIn(0, 255))
        canvas.drawPath(body, p)
    }

    /** 8. Teks + ikon tombol CTA sejajar tengah, auto-fit ke lebar kapsul. */
    private fun drawCTAGlyph(canvas: Canvas, l: CapsuleLayout) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(
            Typeface.DEFAULT,
            if (isTextBold) Typeface.BOLD else Typeface.NORMAL
        )

        var size = textSize.coerceAtLeast(4f)
        p.textSize = size
        val rawTextW = p.measureText(buttonText)
        val maxTextWidth = l.effL * 0.58f - (if (iconType != ButtonIcon.NONE) size * 1.8f else 0f)
        if (rawTextW > maxTextWidth && rawTextW > 0f) {
            size *= maxTextWidth / rawTextW
            p.textSize = size
        }
        val textW = p.measureText(buttonText)
        val iconW = if (iconType != ButtonIcon.NONE) size * 1.45f else 0f
        val gap = if (iconType != ButtonIcon.NONE) size * 0.4f else 0f
        val totalW = textW + iconW + gap
        val cy = l.cy
        val cx = l.cx

        val iconBeforeText = (iconPosition == IconPosition.LEFT_OF_TEXT)
        val iconCx = if (iconBeforeText) cx - totalW / 2f + iconW / 2f else cx - totalW / 2f + iconW / 2f
        val textCx = if (iconBeforeText) cx - totalW / 2f + iconW + gap + textW / 2f else cx - totalW / 2f + textW / 2f
        val textCx2 = if (iconBeforeText) textCx else cx + totalW / 2f - iconW - gap - textW / 2f
        val iconCx2 = if (iconBeforeText) iconCx else cx + totalW / 2f - iconW / 2f

        p.color = textColor
        val bounds = android.graphics.Rect()
        p.getTextBounds(buttonText, 0, buttonText.length, bounds)
        val baseline = cy - (bounds.top + bounds.bottom) / 2f
        canvas.drawText(buttonText, textCx2, baseline, p)

        if (iconType != ButtonIcon.NONE) {
            drawButtonIcon(canvas, iconType, iconCx2, cy, size, textColor)
        }
    }

    private fun drawButtonIcon(canvas: Canvas, type: ButtonIcon, cx: Float, cy: Float, size: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = color
        val s = size
        when (type) {
            ButtonIcon.NONE -> {}
            ButtonIcon.CART -> {
                canvas.save()
                canvas.translate(cx, cy)
                // Keranjang.
                p.strokeWidth = s * 0.10f
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeJoin = Paint.Join.ROUND
                val path = Path()
                path.moveTo(-s * 0.52f, -s * 0.30f)
                path.lineTo(-s * 0.62f, -s * 0.12f)
                path.lineTo(-s * 0.40f, -s * 0.12f)
                path.lineTo(-s * 0.18f, s * 0.26f)
                path.lineTo(s * 0.20f, s * 0.26f)
                path.lineTo(s * 0.44f, -s * 0.02f)
                path.lineTo(-s * 0.44f, -s * 0.02f)
                canvas.drawPath(path, p)
                p.style = Paint.Style.FILL
                p.strokeWidth = 0f
                canvas.drawCircle(-s * 0.28f, s * 0.42f, s * 0.10f, p)
                canvas.drawCircle(s * 0.14f, s * 0.42f, s * 0.10f, p)
                canvas.restore()
            }
            ButtonIcon.ARROW_RIGHT -> {
                canvas.save()
                canvas.translate(cx, cy)
                p.strokeWidth = s * 0.14f
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(-s * 0.34f, 0f, s * 0.28f, 0f, p)
                canvas.drawLine(s * 0.06f, -s * 0.30f, s * 0.32f, 0f, p)
                canvas.drawLine(s * 0.06f, s * 0.30f, s * 0.32f, 0f, p)
                canvas.restore()
            }
            ButtonIcon.FLASH -> {
                val path = Path()
                path.moveTo(cx - s * 0.04f, cy - s * 0.62f)
                path.lineTo(cx - s * 0.38f, cy + s * 0.16f)
                path.lineTo(cx - s * 0.02f, cy + s * 0.16f)
                path.lineTo(cx - s * 0.14f, cy + s * 0.62f)
                path.lineTo(cx + s * 0.40f, cy - s * 0.18f)
                path.lineTo(cx + s * 0.04f, cy - s * 0.18f)
                path.close()
                canvas.drawPath(path, p)
            }
            ButtonIcon.CHECK -> {
                p.strokeWidth = s * 0.14f
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeJoin = Paint.Join.ROUND
                canvas.drawLine(cx - s * 0.40f, cy, cx - s * 0.06f, cy + s * 0.34f, p)
                canvas.drawLine(cx - s * 0.06f, cy + s * 0.34f, cx + s * 0.42f, cy - s * 0.34f, p)
            }
            ButtonIcon.STAR -> {
                val R = s * 0.42f
                val inner = R * 0.42f
                val path = Path()
                for (i in 0 until 10) {
                    val ang = -90.0 + i * 36.0
                    val rad = if (i % 2 == 0) R else inner
                    val x = cx + cos(Math.toRadians(ang)).toFloat() * rad
                    val y = cy + sin(Math.toRadians(ang)).toFloat() * rad
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, p)
            }
        }
    }

    // ── Bayangan ──────────────────────────────────────────────────────────────

    private fun drawFloorShadow(canvas: Canvas, l: CapsuleLayout, effOpacity: Float) {
        val r = l.cr
        val elevFactor = (floatingElevation / (r * 2f)).coerceIn(0f, 1f)
        val rex = l.effL * 0.5f * abs(l.cosR) + l.cr * 0.62f
        val rey = l.cr * 0.34f
        val alpha = (255 * floorShadowOpacity.coerceIn(0f, 1f) * (1f - 0.55f * elevFactor) * effOpacity)
            .toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        val cx = l.cx
        val cy = l.cy + r * 0.42f + floatingElevation.coerceAtMost(r * 0.8f) * 0.5f
        paintShadowEllipse(canvas, cx, cy, rex, rey, alpha)
    }

    private fun paintShadowEllipse(canvas: Canvas, cx: Float, cy: Float, rex: Float, rey: Float, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, max(rex, 0.1f),
            intArrayOf(
                Color.argb(alpha, 0, 0, 0),
                Color.argb((alpha * 0.5f).toInt(), 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(1f, rey / max(rex, 0.1f))
        canvas.drawCircle(0f, 0f, rex, p)
        canvas.restore()
        p.shader = null
    }

    private fun drawEngineShadow(canvas: Canvas, l: CapsuleLayout, effOpacity: Float) {
        val r = l.cr
        val rex = (l.effL * 0.5f * abs(l.cosR) + r * 0.7f) * 0.6f
        val rey = r * 0.42f
        val dx = shadowDx.coerceIn(-30f, 30f)
        val dy = shadowDy.coerceIn(-30f, 30f)
        val alpha = (255 * shadowOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        paintShadowEllipse(canvas, l.cx + dx, l.cy + r + dy, rex, rey, alpha)
    }

    private fun drawNeonHalo(canvas: Canvas, l: CapsuleLayout, effOpacity: Float) {
        val glow = if (material == CapsuleMaterial.CYBER_NEON) (if (neonEnabled) neonColor else primaryColor) else neonColor
        val r = l.cr
        val rex = l.effL * 0.5f + r + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f)
        val rey = r + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f) * 0.5f
        val alpha = (120 * neonIntensity.coerceIn(0.1f, 2.5f) * effOpacity).toInt().coerceIn(0, 255)
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