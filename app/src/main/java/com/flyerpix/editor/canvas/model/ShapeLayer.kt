package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import java.util.UUID
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.renderer.EffectRenderUtils

/**
 * Tipe bentuk geometris yang didukung oleh [ShapeLayer].
 */
enum class ShapeType {
    RECTANGLE,
    ROUNDED_RECTANGLE,
    CIRCLE,
    ARC,
    TRIANGLE,
    STAR,
    HEART,
    HEXAGON,
    DIAMOND,
    // ── Dekoratif & Islami ──────────────────────────────────────────────────
    /** Lengkungan runcing gaya gerbang masjid (kubah / moorish-gothic arch). */
    ISLAMIC_ARCH,
    /** Rub el Hizb — bintang 8 sudut dari dua persegi bertumpuk (1 diputar 45°). */
    EIGHT_POINT_STAR,
    /** Pita/banner dengan ujung chevron bergerigi di kedua sisi. */
    BANNER_RIBBON,
    /** Badge kartu pembicara simetris dengan ujung runcing (double-arch). */
    BADGE_OGEE
}

enum class StrokeStyle {
    SOLID,
    DASHED,
    DOTTED
}


/**
 * Posisi garis tepi (stroke) relatif terhadap tepi huruf/bentuk.
 *
 * - [OUTSIDE] — Seluruh stroke berada di luar bentuk (crisp, tidak "makan"
 *   rongga huruf seperti 'e'/'a'/'o' pada width besar). Untuk teks di-render
 *   dengan stroke `width * 2` SEBELUM fill (separuh dalam tertutup fill),
 *   sehingga hanya separuh luar yang terlihat → lega seperti Canva/Illustrator.
 * - [CENTER]  — Stroke terpusat pada tepi (perilaku Android standar/legacy;
 *   separuh luar + separuh dalam, dalam tertutup fill).
 * - [INSIDE]  — Stroke digambar di dalam bentuk (mengikuti kontur rongga;
 *   width * 2 DIGAMBAR SENGAJA menutupi sebagian fill, stroke terlihat penuh
 *   pada kontur huruf/bentuk seperti Photoshop "inside stroke").
 */
enum class StrokeAlignment {
    OUTSIDE,
    CENTER,
    INSIDE
}

/**
 * Representasi layer bentuk geometris pada kanvas PixelLab.
 *
 * Mendukung berbagai tipe bentuk:
 *  - [ShapeType.RECTANGLE]          — Persegi / Persegi Panjang
 *  - [ShapeType.ROUNDED_RECTANGLE]  — Persegi Sudut Tumpul (corner radius dinamis)
 *  - [ShapeType.CIRCLE]             — Lingkaran / Elips
 *  - [ShapeType.TRIANGLE]           — Segitiga sama sisi
 *  - [ShapeType.STAR]               — Bintang dengan jumlah titik & inner radius dinamis
 *  - [ShapeType.HEART]              — Hati (kurva Bezier simetris)
 *  - [ShapeType.HEXAGON]            — Segi enam beraturan
 *  - [ShapeType.DIAMOND]            — Belah ketupat 4 titik
 *
 * Properti visual:
 *  - [fillColor]   — Warna isi bentuk
 *  - [strokeColor] — Warna garis tepi
 *  - [strokeWidth] — Ketebalan garis tepi (0 = tanpa tepi)
 */
data class ShapeLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Shape Properties ───────────────────────────────────────────────────
    var shapeType: ShapeType = ShapeType.RECTANGLE,
    var width: Float = 200f,
    var height: Float = 200f,
    // ── Fill & Stroke ──────────────────────────────────────────────────────
    // fillColor default biru (bukan putih) agar shape baru selalu terlihat
    // di atas kanvas berlatarbelakang putih.
    var fillColor: Int = 0xFF1769FF.toInt(),
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 0f,
    var strokeOpacity: Int = 255, // Stroke opacity terpisah dari layer opacity
    var strokeJoin: Paint.Join = Paint.Join.MITER, // MITER, BEVEL, ROUND
    var strokeStyle: StrokeStyle = StrokeStyle.SOLID,
    var strokeAlignment: StrokeAlignment = StrokeAlignment.OUTSIDE,
    /** Stroke bergradasi untuk garis tepi bentuk (null = pakai [strokeColor]). */
    var strokeGradientEnabled: Boolean = false,
    var strokeGradient: GradientColor? = null,
    var arcStartAngle: Float = 0f,
    var arcSweepAngle: Float = 270f,
    // ── Rounded Rectangle ──────────────────────────────────────────────────
    var cornerRadiusX: Float = 20f,
    var cornerRadiusY: Float = 20f,
    // ── Star ───────────────────────────────────────────────────────────────
    var starPoints: Int = 5,
    var starInnerRadiusRatio: Float = 0.4f,
    // ── Perspective Warping ────────────────────────────────────────────────
    override var perspectiveEnabled: Boolean = false,
    override var perspectiveCorners: FloatArray = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        0f, 1f
    ),
    // ── Blending Mode ──────────────────────────────────────────────────────
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
    // Path builders
    // ─────────────────────────────────────────────────────────────────────────

    fun buildPath(): Path {
        return when (shapeType) {
            ShapeType.RECTANGLE         -> buildRectanglePath()
            ShapeType.ROUNDED_RECTANGLE -> buildRoundedRectanglePath()
            ShapeType.CIRCLE            -> buildCirclePath()
            ShapeType.ARC               -> buildCirclePath()
            ShapeType.TRIANGLE          -> buildTrianglePath()
            ShapeType.STAR              -> buildStarPath()
            ShapeType.HEART             -> buildHeartPath()
            ShapeType.HEXAGON           -> buildHexagonPath()
            ShapeType.DIAMOND           -> buildDiamondPath()
            ShapeType.ISLAMIC_ARCH      -> buildIslamicArchPath()
            ShapeType.EIGHT_POINT_STAR  -> buildEightPointStarPath()
            ShapeType.BANNER_RIBBON     -> buildBannerRibbonPath()
            ShapeType.BADGE_OGEE        -> buildBadgeOgeePath()
        }
    }

    private fun buildRectanglePath(): Path {
        val path = Path()
        path.addRect(0f, 0f, width, height, Path.Direction.CW)
        return path
    }

    private fun buildRoundedRectanglePath(): Path {
        val path = Path()
        val rect = RectF(0f, 0f, width, height)
        path.addRoundRect(rect, cornerRadiusX, cornerRadiusY, Path.Direction.CW)
        return path
    }

    private fun buildCirclePath(): Path {
        val path = Path()
        val rect = RectF(0f, 0f, width, height)
        path.addOval(rect, Path.Direction.CW)
        return path
    }

    private fun buildTrianglePath(): Path {
        val path = Path()
        path.moveTo(width / 2f, 0f)
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        return path
    }

    private fun buildStarPath(): Path {
        val path = Path()
        val cx = width / 2f
        val cy = height / 2f
        val outerRx = width / 2f
        val outerRy = height / 2f
        val innerRx = outerRx * starInnerRadiusRatio.coerceIn(0.1f, 0.9f)
        val innerRy = outerRy * starInnerRadiusRatio.coerceIn(0.1f, 0.9f)
        val points = starPoints.coerceAtLeast(3)
        val totalVertices = points * 2
        val angleStep = (2.0 * Math.PI / totalVertices).toFloat()
        val startAngle = -Math.PI / 2.0  // mulai dari atas

        for (i in 0 until totalVertices) {
            val angle = startAngle + i * angleStep
            val rx = if (i % 2 == 0) outerRx else innerRx
            val ry = if (i % 2 == 0) outerRy else innerRy
            val px = cx + (rx * Math.cos(angle.toDouble())).toFloat()
            val py = cy + (ry * Math.sin(angle.toDouble())).toFloat()
            if (i == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()
        return path
    }

    private fun buildHeartPath(): Path {
        val sx = width / 32f
        val sy = height / 29.6f
        val path = Path()
        path.moveTo(16f * sx, 29.6f * sy)
        path.cubicTo(6f * sx, 22.5f * sy, 0f * sx, 14f * sy, 0f * sx, 7.4f * sy)
        path.cubicTo(0f * sx, 2.5f * sy, 4.4f * sx, 0f * sy, 8f * sx, 0f * sy)
        path.cubicTo(12f * sx, 0f * sy, 16f * sx, 4.2f * sy, 16f * sx, 9.4f * sy)
        path.cubicTo(16f * sx, 4.2f * sy, 20f * sx, 0f * sy, 24f * sx, 0f * sy)
        path.cubicTo(27.6f * sx, 0f * sy, 32f * sx, 2.5f * sy, 32f * sx, 7.4f * sy)
        path.cubicTo(32f * sx, 14f * sy, 26f * sx, 22.5f * sy, 16f * sx, 29.6f * sy)
        path.close()
        return path
    }

    private fun buildHexagonPath(): Path {
        val cx = width / 2f
        val cy = height / 2f
        val rx = width / 2f
        val ry = height / 2f
        val path = Path()
        val startAngle = -Math.PI / 2.0
        val angleStep = Math.PI / 3.0
        for (i in 0 until 6) {
            val angle = startAngle + i * angleStep
            val px = cx + (rx * Math.cos(angle)).toFloat()
            val py = cy + (ry * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }

    private fun buildDiamondPath(): Path {
        val path = Path()
        path.moveTo(width / 2f, 0f)
        path.lineTo(width, height / 2f)
        path.lineTo(width / 2f, height)
        path.lineTo(0f, height / 2f)
        path.close()
        return path
    }

    /**
     * Lengkungan Islami runcing: dasar persegi yang bertransisi menjadi
     * arch bergaya moorish/gothic dengan puncak runcing di tengah atas.
     */
    private fun buildIslamicArchPath(): Path {
        val path = Path()
        val springY = height * 0.45f
        path.moveTo(0f, height)
        path.lineTo(0f, springY)
        // Sisi kiri menuju puncak runcing
        path.quadTo(width * 0.5f, springY * 0.18f, width / 2f, 0f)
        // Puncak menurun ke sisi kanan
        path.quadTo(width * 0.5f, springY * 0.18f, width, springY)
        path.lineTo(width, height)
        path.close()
        return path
    }

    /**
     * Rub el Hizb: gabungan (union) dua persegi berukuran sama, satu diputar 45°,
     * menghasilkan bintang berujung 8 yang simetris.
     */
    private fun buildEightPointStarPath(): Path {
        val cx = width / 2f
        val cy = height / 2f
        val rx = width / 2f
        val ry = height / 2f

        val square = Path().apply {
            moveTo(cx - rx, cy - ry)
            lineTo(cx + rx, cy - ry)
            lineTo(cx + rx, cy + ry)
            lineTo(cx - rx, cy + ry)
            close()
        }
        val diamond = Path().apply {
            moveTo(cx, cy - ry)
            lineTo(cx + rx, cy)
            lineTo(cx, cy + ry)
            lineTo(cx - rx, cy)
            close()
        }
        val path = Path()
        path.op(square, diamond, Path.Op.UNION)
        return path
    }

    /**
     * Pita/banner: persegi panjang dengan potongan chevron (notch) ke dalam
     * pada kedua ujung horizontal — khas banner judul acara.
     */
    private fun buildBannerRibbonPath(): Path {
        val path = Path()
        val notch = (width * 0.10f).coerceAtMost(height * 0.6f)
        path.moveTo(notch, 0f)
        path.lineTo(width - notch, 0f)
        path.lineTo(width, height / 2f)
        path.lineTo(width - notch, height)
        path.lineTo(notch, height)
        path.lineTo(0f, height / 2f)
        path.close()
        return path
    }

    /**
     * Badge ogee simetris: kartu dengan ujung kiri-kanan runcing dan lengkungan
     * ganda halus di sisi atas & bawah — cocok untuk nama pembicara.
     */
    private fun buildBadgeOgeePath(): Path {
        val path = Path()
        val cx = width / 2f
        val cy = height / 2f
        path.moveTo(0f, cy)
        // Lengkung kiri-atas ke tengah atas
        path.cubicTo(width * 0.15f, 0f, width * 0.35f, 0f, cx, 0f)
        // Lengkung kanan-atas ke ujung kanan
        path.cubicTo(width * 0.65f, 0f, width * 0.85f, 0f, width, cy)
        // Lengkung kanan-bawah ke tengah bawah
        path.cubicTo(width * 0.85f, height, width * 0.65f, height, cx, height)
        // Lengkung kiri-bawah kembali ke ujung kiri
        path.cubicTo(width * 0.35f, height, width * 0.15f, height, 0f, cy)
        path.close()
        return path
    }

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        // Sanitasi paint di AWAL: pastikan bebas dari pollution layer sebelumnya
        // (shader/pathEffect/maskFilter/style/alpha sisa) sebelum menggambar.
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.pathEffect = null
        paint.shader = null
        paint.maskFilter = null
        paint.setShadowLayer(0f, 0f, 0f, 0)

        val saveCount = canvas.save()

        // 1. Transformasi layer luar (Posisi, Skala, Rotasi berpusat pada titik tengah)
        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(x, y)
        canvas.scale(scale * stretchX, scale * stretchY, cx, cy)
        canvas.rotate(rotation, cx, cy)

        // 2. Transformasi perspektif
        val pMat = getPerspectiveMatrix(w, h)
        if (pMat != null) {
            canvas.concat(pMat)
        }

        // 3. Bentuk geometri & efek
        val path = buildPath()
        val rect = RectF(0f, 0f, width, height)
        val currentGradient = gradient
        val hasStroke = strokeWidth > 0f

        // pathEffect untuk stroke (SOLID / DASHED / DOTTED)
        val currentPathEffect = when (strokeStyle) {
            StrokeStyle.SOLID -> null
            StrokeStyle.DASHED -> DashPathEffect(floatArrayOf(strokeWidth * 3f, strokeWidth * 2f), 0f)
            StrokeStyle.DOTTED -> DashPathEffect(floatArrayOf(strokeWidth, strokeWidth * 2f), 0f)
        }

        if (hasStroke) {
            // ───────────────────────────────────────────────────────────────
            // MODE STROKE-TARGETED: Fill digambar solid (polos, tanpa efek luar
            // agar kontur tegas), sedangkan Stroke menjadi target utama efek.
            // ───────────────────────────────────────────────────────────────
            paint.shader = null
            paint.color = fillColor
            paint.alpha = opacity.coerceIn(0, 255)
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
            if (shapeType == ShapeType.ARC) {
                canvas.drawArc(rect, arcStartAngle, arcSweepAngle, true, paint)
            } else {
                canvas.drawPath(path, paint)
            }

            // Helper menggambar stroke: geometry only (warna/alpha ditentukan
            // oleh Paint yang disuntikkan effect renderer).
            val drawStroke: (Canvas, Paint) -> Unit = { c, p ->
                p.style = Paint.Style.STROKE
                p.strokeWidth = strokeWidth
                p.strokeJoin = strokeJoin
                p.pathEffect = currentPathEffect
                if (shapeType == ShapeType.ARC) {
                    c.drawArc(rect, arcStartAngle, arcSweepAngle, false, p)
                } else {
                    c.drawPath(path, p)
                }
            }

            var strokeEffectDrawn = false

            if (extrudeEnabled && extrudeDepth > 0) {
                EffectRenderUtils.draw3DExtrusionEffect(canvas, this, width, height, strokeColor, drawStroke)
                strokeEffectDrawn = true
            } else if (shadowEnabled && shadowRadius > 0f) {
                EffectRenderUtils.drawDropShadowEffect(
                    canvas,
                    this,
                    width,
                    height,
                    shadowColor,
                    drawContent = { c, p ->
                        // Siluet bayangan + crisp stroke memakai warna stroke
                        // (konsisten dengan pola fill legacy).
                        p.style = Paint.Style.STROKE
                        p.strokeWidth = strokeWidth
                        p.strokeJoin = strokeJoin
                        p.pathEffect = currentPathEffect
                        p.color = strokeColor
                        p.alpha = strokeOpacity.coerceIn(0, 255)
                        if (shapeType == ShapeType.ARC) {
                            c.drawArc(rect, arcStartAngle, arcSweepAngle, false, p)
                        } else {
                            c.drawPath(path, p)
                        }
                    }
                )
                strokeEffectDrawn = true
            } else if (neonEnabled) {
                // Warna neon di-set oleh renderer (neonColor), drawStroke tidak
                // menimpanya sehingga glow memakai neonColor yang sebenarnya.
                EffectRenderUtils.drawNeonEffect(canvas, this, width, height, drawStroke)
                strokeEffectDrawn = true
            } else if (embossEnabled) {
                EffectRenderUtils.drawEmbossEffect(
                    canvas,
                    this,
                    width,
                    height,
                    strokeColor,
                    drawContentBase = drawStroke,
                    drawContentEmboss = drawStroke
                )
                strokeEffectDrawn = true
            } else if (gradientEnabled && currentGradient != null) {
                // Gradient diterapkan pada sapuan stroke.
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = strokeWidth
                paint.strokeJoin = strokeJoin
                paint.pathEffect = currentPathEffect
                paint.shader = currentGradient.createShader(rect)
                paint.color = strokeColor
                paint.alpha = strokeOpacity.coerceIn(0, 255)
                if (shapeType == ShapeType.ARC) {
                    canvas.drawArc(rect, arcStartAngle, arcSweepAngle, false, paint)
                } else {
                    canvas.drawPath(path, paint)
                }
                paint.shader = null
                strokeEffectDrawn = true
            }

            if (!strokeEffectDrawn) {
                // Stroke polos (tanpa efek).
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.color = strokeColor
                paint.alpha = strokeOpacity.coerceIn(0, 255)
                paint.strokeWidth = strokeWidth
                paint.strokeJoin = strokeJoin
                paint.pathEffect = currentPathEffect
                if (shapeType == ShapeType.ARC) {
                    canvas.drawArc(rect, arcStartAngle, arcSweepAngle, false, paint)
                } else {
                    canvas.drawPath(path, paint)
                }
            }
        } else {
            // ───────────────────────────────────────────────────────────────
            // MODE FILL-TARGETED (perilaku lama): efek diterapkan pada bidang isi.
            // ───────────────────────────────────────────────────────────────
            val hasEffect = shadowEnabled || neonEnabled || embossEnabled || extrudeEnabled || innerShadowEnabled

            // Apply gradient shader if available (EXCEPT when effects are enabled)
            if (currentGradient != null && !hasEffect) {
                paint.shader = currentGradient.createShader(rect)
                paint.color = fillColor
                paint.alpha = opacity.coerceIn(0, 255)
            } else {
                // Solid color rendering (for effects compatibility)
                paint.color = fillColor
                paint.shader = null
                paint.alpha = opacity.coerceIn(0, 255)
            }

            if (extrudeEnabled && extrudeDepth > 0) {
                paint.style = Paint.Style.FILL
                paint.alpha = opacity.coerceIn(0, 255)
                paint.strokeWidth = 0f

                // 3D Extrusion (FIRST — provides depth base)
                com.flyerpix.editor.canvas.renderer.EffectRenderUtils.draw3DExtrusionEffect(
                    canvas,
                    this,
                    width,
                    height,
                    fillColor,
                    drawContent = { c, p ->
                        p.style = Paint.Style.FILL
                        if (shapeType == ShapeType.ARC) {
                            c.drawArc(rect, arcStartAngle, arcSweepAngle, true, p)
                        } else {
                            c.drawPath(path, p)
                        }
                    }
                )
            } else if (shadowEnabled && shadowRadius > 0f) {
                // Handle drop shadow (via generic effect renderer)
                com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawDropShadowEffect(
                    canvas,
                    this,
                    width,
                    height,
                    shadowColor,
                    drawContent = { c, p ->
                        p.style = Paint.Style.FILL
                        p.color = fillColor
                        p.alpha = opacity.coerceIn(0, 255)
                        if (shapeType == ShapeType.ARC) {
                            c.drawArc(rect, arcStartAngle, arcSweepAngle, true, p)
                        } else {
                            c.drawPath(path, p)
                        }
                    }
                )
            } else if (neonEnabled) {
                // Neon effect (no shadow)
                com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawNeonEffect(
                    canvas,
                    this,
                    width,
                    height,
                    drawContent = { c, p ->
                        p.style = Paint.Style.FILL
                        p.color = neonColor
                        p.alpha = opacity.coerceIn(0, 255)
                        if (shapeType == ShapeType.ARC) {
                            c.drawArc(rect, arcStartAngle, arcSweepAngle, true, p)
                        } else {
                            c.drawPath(path, p)
                        }
                    }
                )
            } else if (embossEnabled) {
                // Emboss effect (no shadow)
                com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawEmbossEffect(
                    canvas,
                    this,
                    width,
                    height,
                    fillColor,
                    drawContentBase = { c, p ->
                        p.style = Paint.Style.FILL
                        p.color = fillColor
                        p.alpha = opacity.coerceIn(0, 255)
                        if (shapeType == ShapeType.ARC) {
                            c.drawArc(rect, arcStartAngle, arcSweepAngle, true, p)
                        } else {
                            c.drawPath(path, p)
                        }
                    },
                    drawContentEmboss = { c, p ->
                        if (shapeType == ShapeType.ARC) {
                            c.drawArc(rect, arcStartAngle, arcSweepAngle, true, p)
                        } else {
                            c.drawPath(path, p)
                        }
                    }
                )
            } else {
                // Normal rendering tanpa extrude/shadow/neon/emboss.
                // paint.alpha di-set ulang agar opacity slider merespon pada
                // bentuk polos (sebelumnya leak: memakai nilai sisa Paint bersama).
                paint.style = Paint.Style.FILL
                paint.strokeWidth = 0f
                paint.alpha = opacity.coerceIn(0, 255)
                if (shapeType == ShapeType.ARC) {
                    canvas.drawArc(rect, arcStartAngle, arcSweepAngle, true, paint)
                } else {
                    canvas.drawPath(path, paint)
                }
            }
        }

        // Handle inner shadow (applies after main fill, both modes)
        if (innerShadowEnabled && innerShadowRadius > 0f) {
            com.flyerpix.editor.canvas.renderer.EffectRenderUtils.drawInnerShadowEffect(
                canvas,
                this,
                width,
                height,
                fillColor,
                drawContent = { c, p ->
                    p.style = Paint.Style.FILL
                    if (shapeType == ShapeType.ARC) {
                        c.drawArc(rect, arcStartAngle, arcSweepAngle, true, p)
                    } else {
                        c.drawPath(path, p)
                    }
                }
            )
        }

        // Reset shader & pathEffect milik layer ini (jangan bocor ke layer lain)
        paint.pathEffect = null
        paint.shader = null

        // ── Sanitasi paint bersama ke state default dijaga dari pollution ──
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null

        canvas.restoreToCount(saveCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bounds & Copy
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        return Pair(width, height)
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

    override fun copyLayer(): ShapeLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        perspectiveCorners = this.perspectiveCorners.clone(),
        blendMode = this.blendMode
    ).also {
        it.stretchX = this.stretchX
        it.stretchY = this.stretchY
    }
}
