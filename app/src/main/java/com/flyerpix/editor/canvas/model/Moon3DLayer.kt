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
import kotlin.math.sqrt

enum class CrescentStyle {
    THIN_CRESCENT,
    WIDE_CRESCENT,
    FINIAL_SPIRE,
    FLOATING_ORB,
    PUFFY_3D
}

enum class CrescentMaterial {
    LUXURY_GOLD,
    ROSE_GOLD,
    CHROME_SILVER,
    EMERALD,
    NEON
}

enum class StarType {
    NONE,
    STAR_8,
    STAR_5,
    STAR_6,
    SPARKLE_4
}

data class Moon3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    var outerRadius: Float = 150f,
    var innerOffset: Float = 0.55f,
    var extrusionDepth: Float = 28f,
    var tiltAngle: Float = 15f,
    var spinAngle: Float = 0f,
    var style: CrescentStyle = CrescentStyle.WIDE_CRESCENT,
    var materialType: CrescentMaterial = CrescentMaterial.LUXURY_GOLD,
    var baseColor: Int = 0xFFD4AF37.toInt(),
    var lightAngle: Float = -45f,
    var specularIntensity: Float = 0.9f,
    var auraEnabled: Boolean = true,
    var auraColor: Int = 0xFFFFD700.toInt(),
    var auraRadius: Float = 1.4f,
    var starType: StarType = StarType.STAR_8,
    var starScale: Float = 0.28f,
    var starColor: Int = 0xFFFFD700.toInt(),
    var hangingCordEnabled: Boolean = true,
    var floorShadowEnabled: Boolean = true,
    var floatingElevation: Float = 20f,
    var floorShadowOpacity: Float = 0.45f,
    var wireframeEnabled: Boolean = false,
    var wireStrokeWidth: Float = 2f,
    var wireColor: Int = 0xFFD4AF37.toInt(),
    var wireStrokeOpacity: Float = 0.6f,
    var crescentPuffiness: Float = 1.0f,
    var mosqueEnabled: Boolean = true,
    var mosqueDomeColor: Int = 0xFFF5B813.toInt(),
    var mosqueDoorColor: Int = 0xFF1B7A4B.toInt(),
    var mosqueMinaretEnabled: Boolean = true,
    var sparklesEnabled: Boolean = true,
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

    data class MoonLayout(
        val w: Float,
        val h: Float,
        val cx: Float,
        val cy: Float,
        val outerR: Float,
        val innerR: Float,
        val innerShiftX: Float,
        val outerOval: RectF,
        val innerOval: RectF,
        val crescent: Path,
        val tipUpper: Pair<Float, Float>,
        val tipLower: Pair<Float, Float>,
        val starCx: Float,
        val starCy: Float,
        val mosqueCx: Float = 0f,
        val mosqueCy: Float = 0f,
        val mosqueScale: Float = 0f
    )

    fun computeLayout(): MoonLayout {
        val or = outerRadius.coerceIn(30f, 600f)
        val puff = crescentPuffiness.coerceIn(0.5f, 2.0f)
        val innerFrac = if (style == CrescentStyle.PUFFY_3D) {
            innerOffset.coerceIn(0.3f, 0.9f) * (1f - puff * 0.12f)
        } else {
            innerOffset.coerceIn(0.3f, 0.9f)
        }
        val ir = or * (1f - innerFrac)
        val shiftX = or * innerFrac
        val depth = abs(extrusionDepth)
        val w = or * 2f + depth
        val h = or * 2f + depth
        val cx = w / 2f
        val cy = h / 2f

        val outerOval = RectF(cx - or, cy - or, cx + or, cy + or)
        val innerOval = RectF(cx + shiftX - ir, cy - ir, cx + shiftX + ir, cy + ir)

        val outerPath = Path().apply { addOval(outerOval, Path.Direction.CW) }
        val innerPath = Path().apply { addOval(innerOval, Path.Direction.CW) }
        val crescent = Path().apply {
            op(outerPath, innerPath, Path.Op.DIFFERENCE)
        }

        val d = or * or - ir * ir + shiftX * shiftX
        val tipX = (shiftX * or * or + or * d) / (or * or + ir * ir - shiftX * shiftX).coerceAtLeast(0.001f)
        val tipYsq = or * or - tipX * tipX
        val tipY = if (tipYsq > 0f) sqrt(tipYsq) else 0f
        val tipUpper = Pair(cx + tipX * 0.5f, cy - tipY * 0.5f)
        val tipLower = Pair(cx + tipX * 0.5f, cy + tipY * 0.5f)

        val starAngle = Math.toRadians(135.0)
        val starR = or * starScale
        val starCx = cx - or * 0.15f + (starR * cos(starAngle)).toFloat()
        val starCy = cy - or * 0.15f + (starR * sin(starAngle)).toFloat()

        val mosqueCx = cx + shiftX * 0.45f
        val mosqueCy = cy
        val mosqueScale = or * 0.35f * puff

        return MoonLayout(
            w = w, h = h, cx = cx, cy = cy,
            outerR = or, innerR = ir, innerShiftX = shiftX,
            outerOval = outerOval, innerOval = innerOval,
            crescent = crescent,
            tipUpper = tipUpper, tipLower = tipLower,
            starCx = starCx, starCy = starCy,
            mosqueCx = mosqueCx, mosqueCy = mosqueCy, mosqueScale = mosqueScale
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

    override fun copyLayer(): Moon3DLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        perspectiveCorners = this.perspectiveCorners.clone()
    ).also {
        it.stretchX = this.stretchX
        it.stretchY = this.stretchY
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

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

        if (style == CrescentStyle.PUFFY_3D) {
            drawPuffy3DMoon(canvas, l, effOpacity, si)
        } else {
            if (shadowEnabled && shadowOpacity > 0f && shadowRadius > 0f) {
                drawEngineShadow(canvas, l, effOpacity)
            }
            if (floorShadowEnabled && floorShadowOpacity > 0f) {
                drawFloorShadow(canvas, l, effOpacity)
            }
            if (auraEnabled) {
                drawAura(canvas, l, effOpacity)
            }
            if (neonEnabled) {
                drawNeonHalo(canvas, l, effOpacity)
            }
            drawExtrusion(canvas, l, effOpacity)
            drawMantle(canvas, l, effOpacity, si)
            drawBevel(canvas, l, effOpacity, si)
            drawSpecular(canvas, l, effOpacity, si)
            if (wireframeEnabled) {
                drawWireframe(canvas, l, effOpacity)
            }
            if (starType != StarType.NONE) {
                drawHangingCord(canvas, l, effOpacity)
                drawStar(canvas, l, effOpacity)
            }
        }

        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null
        canvas.restoreToCount(saveCount)
    }

    // ── Step 2: Floor Shadow ──────────────────────────────────────────────────

    private fun drawFloorShadow(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val elevFactor = (floatingElevation / (l.outerR * 0.9f)).coerceIn(0f, 1f)
        val alpha = (255 * floorShadowOpacity.coerceIn(0f, 1f) * (1f - 0.5f * elevFactor) * effOpacity)
            .toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        val cy = l.cy + l.outerR + floatingElevation.coerceAtMost(l.outerR * 0.6f) * 0.5f
        paintShadow(canvas, l.cx, cy, l.outerR, alpha)
    }

    // ── Step 3: Golden Aura ───────────────────────────────────────────────────

    private fun drawAura(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val radius = l.outerR * auraRadius.coerceIn(1.0f, 2.5f)
        val alpha = (50 * effOpacity).toInt().coerceIn(0, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            l.cx, l.cy, max(radius, 0.1f),
            intArrayOf(
                withAlpha(auraColor, alpha),
                withAlpha(auraColor, (alpha * 0.4f).toInt()),
                withAlpha(auraColor, 0)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(l.cx, l.cy, radius, p)
        p.shader = null
    }

    // ── Step 4: Neon Halo ─────────────────────────────────────────────────────

    private fun drawNeonHalo(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val rex = l.outerR + neonRadius.coerceIn(2f, 60f) * neonIntensity.coerceIn(0.2f, 3f)
        val alpha = (110 * neonIntensity.coerceIn(0.1f, 2.5f) * effOpacity).toInt().coerceIn(0, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            l.cx, l.cy, max(rex, 0.1f),
            intArrayOf(
                withAlpha(neonColor, alpha),
                withAlpha(neonColor, (alpha * 0.35f).toInt()),
                withAlpha(neonColor, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(l.cx, l.cy, rex, p)
        p.shader = null
    }

    // ── Step 5: Side-wall extrusion (3D depth) ────────────────────────────────

    private fun drawExtrusion(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val depth = extrusionDepth.coerceIn(0f, 120f)
        if (depth <= 0f) return
        val rad = Math.toRadians(tiltAngle.toDouble())
        val extrusionDx = (depth * cos(rad) * 0.6f).toFloat()
        val extrusionDy = (depth * sin(rad) * 0.6f).toFloat()
        val darkened = shadeBlack(effectiveBase(), 0.35f)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = withAlpha(darkened, (220 * effOpacity).toInt())

        val steps = max(1, (depth / 2f).toInt())
        for (i in steps downTo 1) {
            val frac = i.toFloat() / steps
            val ox = extrusionDx * frac
            val oy = extrusionDy * frac
            canvas.save()
            canvas.translate(ox, oy)
            canvas.drawPath(l.crescent, p)
            canvas.restore()
        }
    }

    // ── Step 6: Crescent mantle (main face) ───────────────────────────────────

    private fun drawMantle(canvas: Canvas, l: MoonLayout, effOpacity: Float, si: Float) {
        val base = effectiveBase()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL

        val rad = Math.toRadians(lightAngle.toDouble())
        val lx = (cos(rad) * l.outerR).toFloat()
        val ly = (sin(rad) * l.outerR).toFloat()

        p.shader = LinearGradient(
            l.cx - lx, l.cy - ly,
            l.cx + lx, l.cy + ly,
            intArrayOf(
                withAlpha(shadeWhite(base, 0.55f), (255 * effOpacity).toInt()),
                withAlpha(base, (252 * effOpacity).toInt()),
                withAlpha(shadeBlack(base, 0.30f), (240 * effOpacity).toInt()),
                withAlpha(shadeBlack(base, 0.15f), (230 * effOpacity).toInt())
            ),
            floatArrayOf(0f, 0.35f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(l.crescent, p)
        p.shader = null
    }

    // ── Step 7: Bevel ridge / spine ───────────────────────────────────────────

    private fun drawBevel(canvas: Canvas, l: MoonLayout, effOpacity: Float, si: Float) {
        if (si <= 0.03f) return
        val spine = Path()
        val angle = Math.toRadians((lightAngle + 90).toDouble())
        val dx = (cos(angle) * l.outerR * 0.95f).toFloat()
        val dy = (sin(angle) * l.outerR * 0.95f).toFloat()
        spine.moveTo(l.cx - dx, l.cy - dy)
        spine.lineTo(l.cx + dx, l.cy + dy)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1.2f, l.outerR * 0.015f)
        p.color = withAlpha(shadeWhite(effectiveBase(), 0.6f), (120 * effOpacity * si).toInt())
        p.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(spine, p)
    }

    // ── Step 8: Specular flare at tips ────────────────────────────────────────

    private fun drawSpecular(canvas: Canvas, l: MoonLayout, effOpacity: Float, si: Float) {
        if (si <= 0.03f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL

        val flareR = max(l.outerR * 0.22f, 0.1f)
        for (tip in listOf(l.tipUpper, l.tipLower)) {
            p.shader = RadialGradient(
                tip.first, tip.second, flareR,
                intArrayOf(
                    withAlpha(Color.WHITE, (130 * effOpacity * si).toInt()),
                    withAlpha(Color.WHITE, (30 * effOpacity * si).toInt()),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(tip.first, tip.second, flareR, p)
        }
        p.shader = null
    }

    // ── Step 9: Outer rim stroke ──────────────────────────────────────────────

    private fun drawWireframe(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val alpha = (255 * wireStrokeOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = wireStrokeWidth.coerceIn(0.5f, 12f)
        p.color = withAlpha(wireColor, alpha)
        canvas.drawPath(l.crescent, p)
        canvas.drawOval(l.outerOval, p)
        canvas.drawOval(l.innerOval, p)
    }

    // ── Step 10: Hanging Star ─────────────────────────────────────────────────

    private fun drawHangingCord(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        if (!hangingCordEnabled) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1.0f, l.outerR * 0.008f)
        p.color = withAlpha(starColor, (160 * effOpacity).toInt())
        p.strokeCap = Paint.Cap.ROUND

        val cord = Path()
        cord.moveTo(l.starCx, l.starCy)
        val midX = (l.starCx + l.tipUpper.first) / 2f
        val midY = (l.starCy + l.tipUpper.second) / 2f - l.outerR * 0.08f
        cord.quadTo(midX, midY, l.tipUpper.first, l.tipUpper.second)
        canvas.drawPath(cord, p)
    }

    private fun drawStar(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val points = when (starType) {
            StarType.STAR_8 -> 8
            StarType.STAR_5 -> 5
            StarType.STAR_6 -> 6
            else -> return
        }
        val radius = l.outerR * starScale.coerceIn(0.05f, 0.6f)
        if (radius <= 0f) return

        val starPath = Path()
        val innerR = radius * 0.42f
        val step = Math.PI * 2.0 / (points * 2)

        for (i in 0 until points * 2) {
            val angle = i * step - Math.PI / 2.0
            val r = if (i % 2 == 0) radius else innerR
            val px = (l.starCx + r * cos(angle)).toFloat()
            val py = (l.starCy + r * sin(angle)).toFloat()
            if (i == 0) starPath.moveTo(px, py)
            else starPath.lineTo(px, py)
        }
        starPath.close()

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = withAlpha(starColor, (255 * effOpacity).toInt())
        canvas.drawPath(starPath, p)
    }

    // ── Engine Shadow Helper ───────────────────────────────────────────────────

    // ════════════════════════════════════════════════════════════════════════════
    // PUFFY 3D PIPELINE
    // ════════════════════════════════════════════════════════════════════════════

    private fun drawPuffy3DMoon(canvas: Canvas, l: MoonLayout, effOpacity: Float, si: Float) {
        if (shadowEnabled && shadowOpacity > 0f && shadowRadius > 0f) {
            drawEngineShadow(canvas, l, effOpacity)
        }
        if (floorShadowEnabled && floorShadowOpacity > 0f) {
            drawFloorShadow(canvas, l, effOpacity)
        }
        if (auraEnabled) {
            drawAura(canvas, l, effOpacity)
        }
        drawPuffyExtrusion(canvas, l, effOpacity)
        drawPuffyMantle(canvas, l, effOpacity, si)
        drawCurvedSpecularSpine(canvas, l, effOpacity, si)
        drawRimLighting(canvas, l, effOpacity)
        drawSpecular(canvas, l, effOpacity, si)
        if (wireframeEnabled) {
            drawWireframe(canvas, l, effOpacity)
        }
        if (mosqueEnabled) {
            drawMosque(canvas, l, effOpacity)
        }
        if (sparklesEnabled && starType == StarType.SPARKLE_4) {
            drawSparkles(canvas, l, effOpacity)
        } else if (starType != StarType.NONE && starType != StarType.SPARKLE_4) {
            drawHangingCord(canvas, l, effOpacity)
            drawStar(canvas, l, effOpacity)
        }
    }

    private fun drawPuffyExtrusion(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val depth = extrusionDepth.coerceIn(0f, 120f)
        if (depth <= 0f) return
        val puff = crescentPuffiness.coerceIn(0.5f, 2.0f)
        val rad = Math.toRadians(tiltAngle.toDouble())
        val extrusionDx = (depth * cos(rad) * 0.55f).toFloat()
        val extrusionDy = (depth * sin(rad) * 0.55f).toFloat()
        val base = effectiveBase()

        val colors = intArrayOf(
            shadeBlack(base, 0.50f),
            shadeBlack(base, 0.35f),
            shadeBlack(base, 0.22f),
            shadeBlack(base, 0.12f),
            base
        )

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL

        val steps = max(1, (depth / 1.5f).toInt())
        for (i in steps downTo 1) {
            val frac = i.toFloat() / steps
            val ox = extrusionDx * frac
            val oy = extrusionDy * frac
            val colorIdx = ((1f - frac) * (colors.size - 1).toFloat()).toInt().coerceIn(0, colors.size - 1)
            val alpha = (230 * effOpacity * (0.6f + 0.4f * frac)).toInt().coerceIn(0, 255)
            p.color = withAlpha(colors[colorIdx], alpha)
            canvas.save()
            canvas.translate(ox, oy)
            if (puff > 1.0f) {
                val pScale = 1f + (puff - 1f) * 0.04f * frac
                canvas.scale(pScale, pScale, l.cx, l.cy)
            }
            canvas.drawPath(l.crescent, p)
            canvas.restore()
        }
    }

    private fun drawPuffyMantle(canvas: Canvas, l: MoonLayout, effOpacity: Float, si: Float) {
        val base = effectiveBase()
        val puff = crescentPuffiness.coerceIn(0.5f, 2.0f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL

        val rad = Math.toRadians(lightAngle.toDouble())
        val lx = (cos(rad) * l.outerR).toFloat()
        val ly = (sin(rad) * l.outerR).toFloat()

        val brightGold = 0xFFFFE259.toInt()
        val warmGold = 0xFFFFA726.toInt()
        val amberGold = 0xFFE59E0B.toInt()
        val deepBrown = 0xFF7A4100.toInt()

        val a = (255 * effOpacity).toInt()
        val aDark = (240 * effOpacity).toInt()

        p.shader = LinearGradient(
            l.cx - lx, l.cy - ly,
            l.cx + lx, l.cy + ly,
            intArrayOf(
                withAlpha(shadeWhite(base, 0.70f * puff), a),
                withAlpha(brightGold, a),
                withAlpha(warmGold, a),
                withAlpha(amberGold, aDark),
                withAlpha(deepBrown, (aDark * 0.85f).toInt())
            ),
            floatArrayOf(0f, 0.20f, 0.45f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(l.crescent, p)
        p.shader = null

        if (puff > 1.0f) {
            val innerGlow = Paint(Paint.ANTI_ALIAS_FLAG)
            innerGlow.style = Paint.Style.FILL
            val glowR = l.outerR * 0.6f
            val glowCx = l.cx + l.innerShiftX * 0.3f
            innerGlow.shader = RadialGradient(
                glowCx, l.cy, glowR,
                intArrayOf(
                    withAlpha(shadeWhite(warmGold, 0.45f), (60 * effOpacity * (puff - 1f)).toInt().coerceIn(0, 80)),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(l.crescent, innerGlow)
            innerGlow.shader = null
        }
    }

    private fun drawCurvedSpecularSpine(canvas: Canvas, l: MoonLayout, effOpacity: Float, si: Float) {
        if (si <= 0.03f) return
        val spine = Path()
        spine.moveTo(l.tipUpper.first, l.tipUpper.second)
        val ctrlX = l.cx - l.outerR * 0.08f
        val ctrlY = l.cy
        spine.quadTo(ctrlX, ctrlY, l.tipLower.first, l.tipLower.second)

        val base = effectiveBase()
        val highlightColor = shadeWhite(base, 0.75f)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1.8f, l.outerR * 0.022f)
        p.color = withAlpha(highlightColor, (140 * effOpacity * si).toInt().coerceIn(0, 255))
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(spine, p)
    }

    private fun drawRimLighting(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = max(1.5f, l.outerR * 0.018f)
        rimPaint.color = withAlpha(0xFFFFE259.toInt(), (55 * effOpacity).toInt().coerceIn(0, 255))
        rimPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(l.crescent, rimPaint)

        val innerRim = Paint(Paint.ANTI_ALIAS_FLAG)
        innerRim.style = Paint.Style.STROKE
        innerRim.strokeWidth = max(0.8f, l.outerR * 0.008f)
        innerRim.color = withAlpha(Color.WHITE, (30 * effOpacity).toInt().coerceIn(0, 255))
        canvas.drawPath(l.crescent, innerRim)
    }

    private fun drawMosque(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val mcx = l.mosqueCx
        val mcy = l.mosqueCy
        val ms = l.mosqueScale
        if (ms <= 0f) return

        val domePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        domePaint.style = Paint.Style.FILL

        val facadeW = ms * 0.85f
        val facadeH = ms * 0.55f
        val facadeTop = mcy - facadeH * 0.3f
        val facadeLeft = mcx - facadeW / 2f
        val facadeRight = mcx + facadeW / 2f
        val facadeBottom = mcy + ms * 0.42f

        val wallColor = 0xFFF5E6C8.toInt()
        val wallDark = shadeBlack(wallColor, 0.15f)
        val a = (220 * effOpacity).toInt()

        val facadeRect = RectF(facadeLeft, facadeTop, facadeRight, facadeBottom)
        val facadePath = Path().apply { addRoundRect(facadeRect, ms * 0.04f, ms * 0.04f, Path.Direction.CW) }
        domePaint.shader = LinearGradient(
            facadeLeft, facadeTop, facadeRight, facadeBottom,
            intArrayOf(withAlpha(wallColor, a), withAlpha(wallDark, a)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(facadePath, domePaint)
        domePaint.shader = null

        val doorW = ms * 0.22f
        val doorH = ms * 0.32f
        val doorCx = mcx
        val doorBottom = facadeBottom - ms * 0.02f
        val doorTop = doorBottom - doorH
        val doorLeft = doorCx - doorW / 2f
        val doorRight = doorCx + doorW / 2f

        val doorPath = Path().apply {
            addArc(RectF(doorLeft, doorTop, doorRight, doorBottom), 180f, 180f)
            addRect(doorLeft, doorTop + doorH * 0.35f, doorRight, doorBottom, Path.Direction.CW)
        }
        domePaint.shader = LinearGradient(
            doorLeft, doorTop, doorRight, doorBottom,
            intArrayOf(withAlpha(mosqueDoorColor, a), withAlpha(shadeBlack(mosqueDoorColor, 0.3f), a)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(doorPath, domePaint)
        domePaint.shader = null

        val archHighlight = Paint(Paint.ANTI_ALIAS_FLAG)
        archHighlight.style = Paint.Style.STROKE
        archHighlight.strokeWidth = max(0.8f, ms * 0.012f)
        archHighlight.color = withAlpha(shadeWhite(mosqueDoorColor, 0.4f), (80 * effOpacity).toInt())
        canvas.drawPath(doorPath, archHighlight)

        val domeR = ms * 0.32f
        val domeCy = facadeTop - domeR * 0.35f
        drawMosqueDome(canvas, mcx, domeCy, domeR, mosqueDomeColor, effOpacity)

        val smallDomeR = ms * 0.15f
        val leftSmallX = facadeLeft + ms * 0.15f
        val rightSmallX = facadeRight - ms * 0.15f
        val smallDomeCy = facadeTop - smallDomeR * 0.15f
        drawMosqueDome(canvas, leftSmallX, smallDomeCy, smallDomeR, mosqueDomeColor, effOpacity)
        drawMosqueDome(canvas, rightSmallX, smallDomeCy, smallDomeR, mosqueDomeColor, effOpacity)

        if (mosqueMinaretEnabled) {
            val minaretH = ms * 0.75f
            val minaretW = ms * 0.1f
            drawMinaret(canvas, facadeLeft - ms * 0.12f, facadeBottom, minaretH, minaretW, effOpacity)
            drawMinaret(canvas, facadeRight + ms * 0.12f, facadeBottom, minaretH, minaretW, effOpacity)
        }

        val finialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        finialPaint.style = Paint.Style.FILL
        finialPaint.color = withAlpha(mosqueDomeColor, a)
        canvas.drawCircle(mcx, domeCy - domeR - ms * 0.06f, ms * 0.035f, finialPaint)
    }

    private fun drawMosqueDome(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, effOpacity: Float) {
        val a = (230 * effOpacity).toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL

        val domeRect = RectF(cx - r, cy - r * 0.7f, cx + r, cy + r * 0.3f)
        p.shader = RadialGradient(
            cx, cy - r * 0.2f, r,
            intArrayOf(
                withAlpha(shadeWhite(color, 0.45f), a),
                withAlpha(color, a),
                withAlpha(shadeBlack(color, 0.3f), a)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(domeRect, p)
        p.shader = null

        val baseLine = Paint(Paint.ANTI_ALIAS_FLAG)
        baseLine.style = Paint.Style.STROKE
        baseLine.strokeWidth = max(0.6f, r * 0.04f)
        baseLine.color = withAlpha(shadeBlack(color, 0.2f), (100 * effOpacity).toInt())
        canvas.drawLine(cx - r, cy + r * 0.05f, cx + r, cy + r * 0.05f, baseLine)
    }

    private fun drawMinaret(canvas: Canvas, cx: Float, baseY: Float, h: Float, w: Float, effOpacity: Float) {
        val a = (220 * effOpacity).toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL

        val bodyTop = baseY - h
        val bodyRect = RectF(cx - w / 2f, bodyTop + h * 0.15f, cx + w / 2f, baseY)
        val bodyColor = 0xFFF5E6C8.toInt()
        p.shader = LinearGradient(
            cx - w, bodyTop, cx + w, baseY,
            intArrayOf(withAlpha(shadeWhite(bodyColor, 0.3f), a), withAlpha(bodyColor, a), withAlpha(shadeBlack(bodyColor, 0.2f), a)),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(bodyRect, p)
        p.shader = null

        val segmentH = h * 0.18f
        val segY1 = bodyTop + h * 0.35f
        val segRect1 = RectF(cx - w * 0.6f, segY1, cx + w * 0.6f, segY1 + segmentH)
        p.color = withAlpha(shadeBlack(bodyColor, 0.08f), a)
        canvas.drawRoundRect(segRect1, w * 0.1f, w * 0.1f, p)

        val balconyW = w * 1.4f
        val balconyH = h * 0.06f
        val balconyY = bodyTop + h * 0.18f
        val balconyRect = RectF(cx - balconyW / 2f, balconyY, cx + balconyW / 2f, balconyY + balconyH)
        p.color = withAlpha(0xFFB8860B.toInt(), a)
        canvas.drawRoundRect(balconyRect, 2f, 2f, p)

        val minDomeR = w * 0.55f
        val minDomeCy = bodyTop + h * 0.12f
        drawMosqueDome(canvas, cx, minDomeCy, minDomeR, mosqueDomeColor, effOpacity)

        val finialY = minDomeCy - minDomeR * 0.7f - h * 0.04f
        p.color = withAlpha(mosqueDomeColor, a)
        canvas.drawCircle(cx, finialY, w * 0.08f, p)

        val crescentFinial = Path()
        val cfR = w * 0.12f
        val cfCx = cx
        val cfCy = finialY - w * 0.15f
        val outerArc = Path().apply { addArc(RectF(cfCx - cfR, cfCy - cfR, cfCx + cfR, cfCy + cfR), -45f, 270f) }
        val innerArc = Path().apply { addArc(RectF(cfCx - cfR * 0.55f, cfCy - cfR * 0.55f, cfCx + cfR * 0.55f, cfCy + cfR * 0.55f), -45f, 270f) }
        crescentFinial.op(outerArc, innerArc, Path.Op.DIFFERENCE)
        p.style = Paint.Style.FILL
        p.color = withAlpha(mosqueDomeColor, a)
        canvas.drawPath(crescentFinial, p)
    }

    private fun drawSparkles(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val or = l.outerR
        val positions = listOf(
            Pair(-0.55f, -0.70f),
            Pair(0.65f, -0.55f),
            Pair(-0.72f, 0.15f),
            Pair(0.58f, 0.35f),
            Pair(-0.30f, -0.88f),
            Pair(0.42f, -0.82f),
            Pair(-0.82f, -0.30f),
            Pair(0.80f, -0.10f)
        )
        val sparkleR = or * 0.055f
        for ((idx, pos) in positions.withIndex()) {
            val sx = l.cx + or * pos.first * 1.15f
            val sy = l.cy + or * pos.second * 1.15f
            val r = sparkleR * (0.7f + 0.3f * ((idx * 3 + 7) % 5) / 4f)
            drawSparkle(canvas, sx, sy, r, effOpacity)
        }
    }

    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, r: Float, effOpacity: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL

        val a = (200 * effOpacity).toInt()

        val sparklePath = Path()
        val innerR = r * 0.28f
        val points = 4
        val step = Math.PI * 2.0 / (points * 2)
        for (i in 0 until points * 2) {
            val angle = i * step - Math.PI / 2.0
            val curR = if (i % 2 == 0) r else innerR
            val px = (cx + curR * cos(angle)).toFloat()
            val py = (cy + curR * sin(angle)).toFloat()
            if (i == 0) sparklePath.moveTo(px, py) else sparklePath.lineTo(px, py)
        }
        sparklePath.close()

        p.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(
                withAlpha(shadeWhite(0xFFFFD700.toInt(), 0.5f), a),
                withAlpha(0xFFFFD700.toInt(), a),
                withAlpha(0xFFE59E0B.toInt(), (a * 0.7f).toInt())
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(sparklePath, p)
        p.shader = null
    }

    private fun drawEngineShadow(canvas: Canvas, l: MoonLayout, effOpacity: Float) {
        val alpha = (255 * shadowOpacity.coerceIn(0f, 1f) * effOpacity).toInt().coerceIn(0, 255)
        if (alpha <= 4) return
        paintShadow(
            canvas,
            l.cx + shadowDx.coerceIn(-30f, 30f),
            l.cy + l.outerR * 0.9f + shadowDy.coerceIn(-30f, 30f),
            l.outerR * 0.9f,
            alpha
        )
    }

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
        canvas.scale(1f, 0.55f)
        canvas.drawCircle(0f, 0f, rx, p)
        canvas.restore()
        p.shader = null
    }

    // ── Color Utilities ───────────────────────────────────────────────────────

    private fun effectiveBase(): Int {
        return when (materialType) {
            CrescentMaterial.LUXURY_GOLD -> baseColor
            CrescentMaterial.ROSE_GOLD -> 0xFFB76E79.toInt()
            CrescentMaterial.CHROME_SILVER -> 0xFFEDEFF5.toInt()
            CrescentMaterial.EMERALD -> 0xFF2E8B57.toInt()
            CrescentMaterial.NEON -> baseColor
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
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun shadeBlack(color: Int, t: Float): Int {
        val r = (((color shr 16) and 0xFF) * (1f - t)).toInt()
        val g = (((color shr 8) and 0xFF) * (1f - t)).toInt()
        val b = ((color and 0xFF) * (1f - t)).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    companion object {
        private fun sqrt(value: Float): Float = kotlin.math.sqrt(value.toDouble()).toFloat()
    }
}
