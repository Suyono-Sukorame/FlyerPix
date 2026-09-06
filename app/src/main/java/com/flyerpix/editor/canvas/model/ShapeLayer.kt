package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import java.util.UUID

/**
 * Tipe bentuk geometris yang didukung oleh [ShapeLayer].
 */
enum class ShapeType {
    RECTANGLE,
    ROUNDED_RECTANGLE,
    CIRCLE,
    TRIANGLE,
    STAR
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
    var fillColor: Int = Color.WHITE,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 0f,
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

    private fun buildPath(): Path {
        return when (shapeType) {
            ShapeType.RECTANGLE        -> buildRectanglePath()
            ShapeType.ROUNDED_RECTANGLE -> buildRoundedRectanglePath()
            ShapeType.CIRCLE           -> buildCirclePath()
            ShapeType.TRIANGLE         -> buildTrianglePath()
            ShapeType.STAR             -> buildStarPath()
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

    // ─────────────────────────────────────────────────────────────────────────
    // Render pipeline
    // ─────────────────────────────────────────────────────────────────────────

    override fun draw(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        val saveCount = canvas.save()

        // 1. Transformasi layer luar (Posisi, Skala, Rotasi berpusat pada titik tengah)
        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(x, y)
        canvas.scale(scale, scale, cx, cy)
        canvas.rotate(rotation, cx, cy)

        // 2. Transformasi perspektif
        val pMat = getPerspectiveMatrix(w, h)
        if (pMat != null) {
            canvas.concat(pMat)
        }

        // 3. Gambar bentuk
        val path = buildPath()

        // Fill
        paint.style = Paint.Style.FILL
        paint.color = fillColor
        paint.alpha = opacity.coerceIn(0, 255)
        paint.strokeWidth = 0f
        canvas.drawPath(path, paint)

        // Stroke
        if (strokeWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.color = strokeColor
            paint.alpha = opacity.coerceIn(0, 255)
            paint.strokeWidth = strokeWidth
            canvas.drawPath(path, paint)
        }

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
    )
}
