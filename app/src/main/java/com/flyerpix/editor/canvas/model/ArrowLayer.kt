package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import java.util.UUID
import kotlin.math.max

/**
 * Gaya visual panah pada [ArrowLayer].
 *
 * - [STRAIGHT] — Garis lurus antara titik awal dan akhir.
 * - [CURVED]   — Garis lengkung (Bezier kubik) dengan kontrol belok ([curveBend]).
 */
enum class ArrowStyle {
    STRAIGHT,
    CURVED
}

/**
 * Representasi layer panah penunjuk pada kanvas PixelLab.
 *
 * Mendukung:
 *  - Kepala panah ([headEnabled]) dan/atau ekor panah ([tailEnabled])
 *  - Gaya lurus ([ArrowStyle.STRAIGHT]) atau melengkung ([ArrowStyle.CURVED])
 *  - Ketebalan batang panah ([stemWidth])
 *  - Warna panah ([arrowColor]) dengan opasitas & blend mode inheriting dari [CanvasLayer]
 *  - Ukuran kepala/ekor dinamis ([headSize], [tailSize])
 *  - Optional fill pada segitiga kepala/ekor ([headFilled], [tailFilled])
 *
 * **Model koordinat lokal:**
 * Titik awal panah berada di `(0, h/2)` dan titik akhir di `(w, h/2)` dalam ruang lokal,
 * dengan `w = panjang batang` dan `h = stemWidth * 3` (ruang ekstra untuk kepala/ekor).
 */
data class ArrowLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Arrow Properties ───────────────────────────────────────────────────
    var arrowStyle: ArrowStyle = ArrowStyle.STRAIGHT,
    /** Lebar batang panah (ketebalan garis utama). */
    var stemWidth: Float = 6f,
    /** Jarak antara titik awal (x0,y0) dan titik akhir (x1,y1) yang menentukan panjang batang. */
    var stemLength: Float = 300f,
    /** Sudut orientasi panah dalam derajat (0 = kanan, 90 = bawah). */
    var angle: Float = 0f,
    // ── Arrowhead (Kepala Panah — ujung akhir) ─────────────────────────────
    var headEnabled: Boolean = true,
    var headSize: Float = 30f,
    var headColor: Int = Color.WHITE,
    var headFilled: Boolean = true,
    // ── Arrow Tail (Ekor Panah — ujung awal) ──────────────────────────────
    var tailEnabled: Boolean = false,
    var tailSize: Float = 24f,
    var tailColor: Int = Color.WHITE,
    var tailFilled: Boolean = true,
    // ── Curve Bend ──────────────────────────────────────────────────────────
    /** Jarak belokan untuk mode CURVED (0 = lurus, positif = belok kanan, negatif = belok kiri). */
    var curveBend: Float = 0f,
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
    // Internal geometry helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Menghitung dimensi lokal (width, height) dari panah.
     * `width`  = panjang batang + margin untuk kepala/ekor.
     * `height` = batas vertikal dari batang + kepala/ekor yang melampaui batang.
     */
    private fun computeLocalDimensions(): Pair<Float, Float> {
        val sw = stemWidth.coerceAtLeast(1f)
        val marginStart = if (tailEnabled) tailSize else 0f
        val marginEnd   = if (headEnabled) headSize else 0f
        val w = stemLength + marginStart + marginEnd
        val h = sw * 3f  // ruang cukup untuk batang di tengah + kepala/ekor yang menonjol
        return Pair(max(w, 1f), max(h, 1f))
    }

    /**
     * Mengembalikan titik pusat batang awal (di mana batang dimulai, bukan ekor) dalam koordinat lokal.
     */
    private fun stemStartLocal(): Pair<Float, Float> {
        val (_, h) = computeLocalDimensions()
        val marginStart = if (tailEnabled) tailSize else 0f
        return Pair(marginStart, h / 2f)
    }

    /**
     * Mengembalikan titik pusat batang akhir (di mana batang berakhir, bukan kepala) dalam koordinat lokal.
     */
    private fun stemEndLocal(): Pair<Float, Float> {
        val (w, h) = computeLocalDimensions()
        val marginEnd = if (headEnabled) headSize else 0f
        return Pair(w - marginEnd, h / 2f)
    }

    /**
     * Menghitung offset belokan perpendicular untuk mode CURVED.
     */
    private fun curveControlOffset(): Float {
        return if (arrowStyle == ArrowStyle.CURVED) curveBend else 0f
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Path builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Membangun path batang panah (tanpa kepala/ekor).
     */
    private fun buildStemPath(): Path {
        val path = Path()
        val (sx, sy) = stemStartLocal()
        val (ex, ey) = stemEndLocal()
        val halfSw = stemWidth.coerceAtLeast(1f) / 2f

        if (arrowStyle == ArrowStyle.CURVED) {
            val bend = curveControlOffset()
            val midX = (sx + ex) / 2f
            val midY = (sy + ey) / 2f + bend
            path.moveTo(sx, sy - halfSw)
            path.cubicTo(midX, midY - halfSw, midX, midY - halfSw, ex, ey - halfSw)
            path.lineTo(ex, ey + halfSw)
            path.cubicTo(midX, midY + halfSw, midX, midY + halfSw, sx, sy + halfSw)
            path.close()
        } else {
            path.moveTo(sx, sy - halfSw)
            path.lineTo(ex, ey - halfSw)
            path.lineTo(ex, ey + halfSw)
            path.lineTo(sx, sy + halfSw)
            path.close()
        }
        return path
    }

    /**
     * Membangun path kepala panah (segitiga) di ujung akhir.
     */
    private fun buildHeadPath(): Path {
        val path = Path()
        val (ex, ey) = stemEndLocal()
        val halfSw = stemWidth.coerceAtLeast(1f) / 2f
        val tipX = ex + headSize
        val tipY = ey

        path.moveTo(tipX, tipY)
        path.lineTo(ex, ey - halfSw - headSize * 0.45f)
        path.lineTo(ex, ey + halfSw + headSize * 0.45f)
        path.close()
        return path
    }

    /**
     * Membangun path ekor panah (segitiga) di ujung awal.
     */
    private fun buildTailPath(): Path {
        val path = Path()
        val (sx, sy) = stemStartLocal()
        val halfSw = stemWidth.coerceAtLeast(1f) / 2f
        val tipX = sx - tailSize
        val tipY = sy

        path.moveTo(tipX, tipY)
        path.lineTo(sx, sy - halfSw - tailSize * 0.45f)
        path.lineTo(sx, sy + halfSw + tailSize * 0.45f)
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

        paint.alpha = opacity.coerceIn(0, 255)
        paint.isAntiAlias = true

        // ── Pass 1: Ekor panah (di belakang batang) ────────────────────────
        if (tailEnabled) {
            val tailPath = buildTailPath()
            paint.style = if (tailFilled) Paint.Style.FILL else Paint.Style.STROKE
            paint.color = tailColor
            paint.strokeWidth = stemWidth
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(tailPath, paint)
        }

        // ── Pass 2: Batang panah ───────────────────────────────────────────
        val stemPath = buildStemPath()
        paint.style = Paint.Style.FILL
        paint.color = headColor
        canvas.drawPath(stemPath, paint)

        // ── Pass 3: Kepala panah (di depan batang) ─────────────────────────
        if (headEnabled) {
            val headPath = buildHeadPath()
            paint.style = if (headFilled) Paint.Style.FILL else Paint.Style.STROKE
            paint.color = headColor
            paint.strokeWidth = stemWidth
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(headPath, paint)
        }

        canvas.restoreToCount(saveCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bounds & Copy
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUnwarpedDimensions(): Pair<Float, Float> = computeLocalDimensions()

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

    override fun copyLayer(): ArrowLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        perspectiveCorners = this.perspectiveCorners.clone(),
        blendMode = this.blendMode
    )
}
