package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PointF
import android.graphics.RectF
import java.util.UUID
import kotlin.math.max
import com.flyerpix.editor.canvas.renderer.EffectRenderUtils

/**
 * Mode proyeksi perspektif untuk [Box3DLayer].
 *  - [ISOMETRIC] — Proyeksi isometrik 30° teknik standar.
 *  - [ONE_POINT] — Perspektif satu titik lenyap (hilang di tengah).
 *  - [TWO_POINT] — Perspektif dua titik lenyap horizontal yang realistis.
 *  - [FREE_VP]   — Titik lenyap bebas via 3 sumbu pengendali ([Box3DLayer.vpX], vpY, vpZ).
 */
enum class Box3DPerspectiveMode {
    ISOMETRIC,
    ONE_POINT,
    TWO_POINT,
    FREE_VP
}

/**
 * Urutan 6 sisi kubus — dipakai untuk [Box3DLayer.customFaceColors].
 */
enum class Box3DFace(val index: Int) {
    FRONT(0),
    BACK(1),
    TOP(2),
    BOTTOM(3),
    LEFT(4),
    RIGHT(5)
}

/**
 * Layer objek geometri 3D Box (balok) sejati dengan 8 titik sudut & 6 sisi.
 *
 * Menghitung proyeksi 3D secara manual: rotasi sumbu X/Y lalu proyeksi
 * isometrik / perspektif (focal divide), back-face culling via normal 3D,
 * Painter's Algorithm untuk urutan penggambaran sisi, dan auto-shading
 * berbasis orientasi normal sisi:
 *  - Top +25% terang, Front 0%, Kiri/Kanan −25%, Bottom −45%.
 *
 * Sisi digambar dalam ruang lokal positif (0..w, 0..h) hasil proyeksi sehingga
 * konsisten dengan [CanvasLayer] (selection box, resize, transformasi luar).
 */
data class Box3DLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Dimensi Balok ───────────────────────────────────────────────────────
    var boxWidth: Float = 220f,
    var boxHeight: Float = 180f,
    var boxDepth: Float = 220f,
    // ── Proyeksi & Vanishing Points ─────────────────────────────────────────
    var perspectiveMode: Box3DPerspectiveMode = Box3DPerspectiveMode.ISOMETRIC,
    var angleX: Float = 0f,
    var angleY: Float = 0f,
    var vpX: PointF = PointF(0f, 0f),
    var vpY: PointF = PointF(0f, 0f),
    var vpZ: PointF = PointF(0f, 0f),
    // ── Warna & Shading ─────────────────────────────────────────────────────
    var baseColor: Int = 0xFF1769FF.toInt(),
    var autoShadingEnabled: Boolean = true,
    var customFaceColors: IntArray? = null,
    var faceOpacity: Int = 255,
    // ── Wireframe ───────────────────────────────────────────────────────────
    var strokeWidth: Float = 0f,
    var strokeColor: Int = Color.BLACK,
    var strokeOpacity: Int = 255,
    var showHiddenEdges: Boolean = false,
    // ── Transformasi Warping (diwarisi, tidak dipakai reshape) ──────────────
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
    // Geometri
    // ─────────────────────────────────────────────────────────────────────────

    private data class FaceInfo(
        val index: Int,
        val vertexIndices: IntArray,
        val normal: FloatArray,
        val depth: Float
    )

    /** 6 sisi: urutan index vertex mengikuti arah luar (CCW dilihat dari luar). */
    private val FACE_ORDER = listOf(
        intArrayOf(0, 1, 2, 3),  // FRONT  (+Z)
        intArrayOf(5, 4, 7, 6),  // BACK   (-Z)
        intArrayOf(3, 2, 6, 7),  // TOP    (+Y)
        intArrayOf(4, 5, 1, 0),  // BOTTOM (-Y)
        intArrayOf(4, 0, 3, 7),  // LEFT   (-X)
        intArrayOf(1, 5, 6, 2)   // RIGHT  (+X)
    )

    /** 12 rusuk (pasangan indeks vertex). */
    private fun edgeList(): List<IntArray> = listOf(
        intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
        intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
        intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)
    )

    private class Projection(
        val localXY: FloatArray,
        val work3D: FloatArray,
        val sizeW: Float,
        val sizeH: Float,
        val faces: List<FaceInfo>
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Proyeksi
    // ─────────────────────────────────────────────────────────────────────────

    private fun project(): Projection {
        val hw = boxWidth / 2f
        val hh = boxHeight / 2f
        val hd = boxDepth / 2f

        // Urutan vertex: 0..3 sisi depan (+Z), 4..7 sisi belakang (-Z).
        val model = floatArrayOf(
            -hw, -hh, +hd,   // 0 front bottom-left
            +hw, -hh, +hd,   // 1 front bottom-right
            +hw, +hh, +hd,   // 2 front top-right
            -hw, +hh, +hd,   // 3 front top-left
            -hw, -hh, -hd,   // 4 back bottom-left
            +hw, -hh, -hd,   // 5 back bottom-right
            +hw, +hh, -hd,   // 6 back top-right
            -hw, +hh, -hd    // 7 back top-left
        )

        val mode = perspectiveMode
        val work = FloatArray(24)

        if (mode == Box3DPerspectiveMode.FREE_VP) {
            // Tanpa rotasi: orientasi bebas via 3 sumbu pengendali (vpX/vpY/vpZ).
            System.arraycopy(model, 0, work, 0, 24)
        } else {
            val (yawDeg, pitchDeg) = when (mode) {
                Box3DPerspectiveMode.ISOMETRIC -> (45f + angleY) to (-35.264f + angleX)
                Box3DPerspectiveMode.ONE_POINT -> angleY to angleX
                Box3DPerspectiveMode.TWO_POINT -> (32f + angleY) to angleX
                else -> angleY to angleX
            }
            val radY = Math.toRadians(yawDeg.toDouble())
            val radX = Math.toRadians(pitchDeg.toDouble())
            val cy = Math.cos(radY).toFloat()
            val sy = Math.sin(radY).toFloat()
            val cxp = Math.cos(radX).toFloat()
            val sxp = Math.sin(radX).toFloat()
            for (i in 0 until 8) {
                val mx = model[i * 3]
                val my = model[i * 3 + 1]
                val mz = model[i * 3 + 2]
                val x1 = mx * cy + mz * sy
                val z1 = -mx * sy + mz * cy
                val y2 = my * cxp - z1 * sxp
                val z2 = my * sxp + z1 * cxp
                work[i * 3] = x1
                work[i * 3 + 1] = y2
                work[i * 3 + 2] = z2
            }
        }

        // Simpan depth tiap vertex (koordinat Z pekerja) — untuk painter & shadow.
        val depth = FloatArray(8) { work[it * 3 + 2] }

        // Proyeksi ke layar (ruang terpusat di titik asal).
        val centered = FloatArray(16)
        if (mode == Box3DPerspectiveMode.FREE_VP) {
            val kXx = 1f + vpX.x
            val kYy = 1f + vpY.y
            for (i in 0 until 8) {
                val mx = work[i * 3]
                val my = work[i * 3 + 1]
                val mz = work[i * 3 + 2]
                centered[i * 2] = mx * kXx + my * vpY.x + mz * vpZ.x
                centered[i * 2 + 1] = -my * kYy - mx * vpX.y - mz * vpZ.y
            }
        } else if (mode == Box3DPerspectiveMode.ISOMETRIC) {
            for (i in 0 until 8) {
                centered[i * 2] = work[i * 3]
                centered[i * 2 + 1] = -work[i * 3 + 1]
            }
        } else {
            // ONE_POINT / TWO_POINT: perspective divide dengan focal length.
            val maxDim = max(max(hw, hh), hd)
            val focal = 4f * maxDim
            for (i in 0 until 8) {
                val rz = work[i * 3 + 2]
                val factor = focal / (focal - rz).coerceAtMost(focal - 1f)
                centered[i * 2] = work[i * 3] * factor
                centered[i * 2 + 1] = -work[i * 3 + 1] * factor
            }
        }

        // Geser ke ruang lokal positif.
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until 8) {
            minX = Math.min(minX, centered[i * 2])
            minY = Math.min(minY, centered[i * 2 + 1])
            maxX = Math.max(maxX, centered[i * 2])
            maxY = Math.max(maxY, centered[i * 2 + 1])
        }
        val sizeW = (maxX - minX).coerceAtLeast(1f)
        val sizeH = (maxY - minY).coerceAtLeast(1f)
        val localXY = FloatArray(16)
        for (i in 0 until 8) {
            localXY[i * 2] = centered[i * 2] - minX
            localXY[i * 2 + 1] = centered[i * 2 + 1] - minY
        }

        // Info 6 sisi: normal luar + depth rata-rata (painter's algorithm).
        val verts3D = work
        val faces = FACE_ORDER.mapIndexed { idx, vids ->
            val ax = verts3D[vids[0] * 3]; val ay = verts3D[vids[0] * 3 + 1]; val az = verts3D[vids[0] * 3 + 2]
            val bx = verts3D[vids[1] * 3]; val by = verts3D[vids[1] * 3 + 1]; val bz = verts3D[vids[1] * 3 + 2]
            val cxv = verts3D[vids[2] * 3]; val cyv = verts3D[vids[2] * 3 + 1]; val czv = verts3D[vids[2] * 3 + 2]
            val ux = bx - ax; val uy = by - ay; val uz = bz - az
            val wx = cxv - ax; val wy = cyv - ay; val wz = czv - az
            // cross(u, w)
            val nx = uy * wz - uz * wy
            val ny = uz * wx - ux * wz
            val nz = ux * wy - uy * wx
            var dAcc = 0f
            for (j in 0 until vids.size) dAcc += verts3D[vids[j] * 3 + 2]
            FaceInfo(
                index = idx,
                vertexIndices = vids,
                normal = floatArrayOf(nx, ny, nz),
                depth = dAcc / vids.size
            )
        }

        return Projection(localXY, verts3D, sizeW, sizeH, faces)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bounds & Copy
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        val p = project()
        return Pair(p.sizeW, p.sizeH)
    }

    override fun getBounds(): RectF {
        val (w, h) = getUnwarpedDimensions()
        val pts = getSelectionBoxPoints(0f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0..3) {
            minX = Math.min(minX, pts[i * 2])
            minY = Math.min(minY, pts[i * 2 + 1])
            maxX = Math.max(maxX, pts[i * 2])
            maxY = Math.max(maxY, pts[i * 2 + 1])
        }
        return RectF(minX, minY, maxX, maxY)
    }

    override fun copyLayer(): Box3DLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x = this.x + 30f,
        y = this.y + 30f,
        customFaceColors = this.customFaceColors?.clone(),
        vpX = PointF(this.vpX.x, this.vpX.y),
        vpY = PointF(this.vpY.x, this.vpY.y),
        vpZ = PointF(this.vpZ.x, this.vpZ.y),
        perspectiveCorners = this.perspectiveCorners.clone()
    ).also {
        it.stretchX = this.stretchX
        it.stretchY = this.stretchY
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper untuk handle interaktif (PixelCanvasView)
    // ─────────────────────────────────────────────────────────────────────────

    /** Posisi pusat sisi belakang (depth handle) dalam koordinat lokal. */
    fun getBackFaceCenterLocal(): Pair<Float, Float> {
        val p = project()
        val idx = intArrayOf(4, 5, 6, 7)
        var sx = 0f
        var sy = 0f
        for (i in idx) {
            sx += p.localXY[i * 2]
            sy += p.localXY[i * 2 + 1]
        }
        return Pair(sx / 4f, sy / 4f)
    }

    /** Posisi pusat sisi depan dalam koordinat lokal. */
    fun getFrontFaceCenterLocal(): Pair<Float, Float> {
        val p = project()
        val idx = intArrayOf(0, 1, 2, 3)
        var sx = 0f
        var sy = 0f
        for (i in idx) {
            sx += p.localXY[i * 2]
            sy += p.localXY[i * 2 + 1]
        }
        return Pair(sx / 4f, sy / 4f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        val p = project()
        val w = p.sizeW
        val h = p.sizeH
        if (w <= 0f || h <= 0f) return

        // Sanitasi paint bersama di AWAL.
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.pathEffect = null
        paint.shader = null
        paint.maskFilter = null
        paint.setShadowLayer(0f, 0f, 0f, 0)

        val saveCount = canvas.save()

        // Transformasi layer luar (posisi, skala, rotasi).
        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(x, y)
        canvas.scale(scale * stretchX, scale * stretchY, cx, cy)
        canvas.rotate(rotation, cx, cy)

        // Urutan sisi terlihat: culling normal 3D (nz > 0) + painter sort.
        val faces = p.faces
            .map { f -> f to dot3Z(f.normal) }
            .sortedBy { it.first.depth }
        val visible = faces.filter { it.second > 0f }
        val visiblePerVertex = BooleanArray(8)
        for ((f, _) in visible) {
            for (vi in f.vertexIndices) visiblePerVertex[vi] = true
        }

        // Bayangan silhouette (di bawah balok).
        if (shadowEnabled && shadowRadius > 0f && visible.isNotEmpty()) {
            drawBoxShadow(canvas, p.localXY, w, h)
        }

        // Faces (painter: jauh → dekat).
        if (visible.isNotEmpty()) {
            val effAlpha = ((opacity.coerceIn(0, 255) / 255f) * (faceOpacity.coerceIn(0, 255) / 255f) * 255f)
                .toInt().coerceIn(0, 255)
            val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            facePaint.style = Paint.Style.FILL
            facePaint.strokeWidth = 0f
            for ((f, _) in visible) {
                facePaint.color = resolveFaceColor(f.index, f.normal)
                facePaint.alpha = effAlpha.coerceIn(0, 255)
                facePaint.pathEffect = null
                facePaint.shader = null
                canvas.drawPath(buildFacePath(p.localXY, f.vertexIndices), facePaint)
            }
        }

        // Effects & wireframe (rusuk).
        val hasWire = strokeWidth > 0f
        if (hasWire) {
            when {
                neonEnabled ->
                    EffectRenderUtils.drawNeonEffect(canvas, this, w, h) { c, pnt ->
                        drawWireframe(c, pnt, p.localXY, visiblePerVertex, color = neonColor, alphaOverride = strokeOpacity)
                    }
                embossEnabled ->
                    EffectRenderUtils.drawEmbossEffect(
                        canvas, this, w, h, if (customFaceColors == null) baseColor else strokeColor,
                        drawContentBase = { c, pnt ->
                            drawWireframe(c, pnt, p.localXY, visiblePerVertex, color = strokeColor, alphaOverride = strokeOpacity, honorPaint = false)
                        },
                        drawContentEmboss = { c, pnt ->
                            drawWireframe(c, pnt, p.localXY, visiblePerVertex, honorPaint = true)
                        }
                    )
                else ->
                    drawWireframe(canvas, paint, p.localXY, visiblePerVertex, color = strokeColor, alphaOverride = strokeOpacity)
            }
        }

        // Reset paint bersama.
        paint.pathEffect = null
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.maskFilter = null

        canvas.restoreToCount(saveCount)
    }

    private fun dot3Z(n: FloatArray): Float = n[2]

    private fun resolveFaceColor(faceIndex: Int, normal: FloatArray): Int {
        customFaceColors?.let { custom ->
            if (custom.size > faceIndex && custom[faceIndex] != 0) return custom[faceIndex]
        }
        if (!autoShadingEnabled) return baseColor
        val len = Math.hypot(normal[0].toDouble(), Math.hypot(normal[1].toDouble(), normal[2].toDouble())).toFloat()
        if (len <= 0f) return baseColor
        val ny = normal[1] / len
        val nz = normal[2] / len
        val factor = when {
            ny > 0.6f -> 0.25f
            ny < -0.6f -> -0.45f
            nz >= 0f -> 0f
            else -> -0.25f
        }
        return shade(baseColor, factor)
    }

    /** Gradasi warna: positif = mix ke putih, negatif = mix ke hitam. */
    private fun shade(color: Int, factor: Float): Int {
        if (factor == 0f) return color
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val t = Math.abs(factor).toFloat().coerceIn(0f, 1f)
        return if (factor > 0f) {
            Color.rgb(
                (r + (255 - r) * t).toInt(),
                (g + (255 - g) * t).toInt(),
                (b + (255 - b) * t).toInt()
            )
        } else {
            Color.rgb(
                (r * (1f - t)).toInt(),
                (g * (1f - t)).toInt(),
                (b * (1f - t)).toInt()
            )
        }
    }

    private fun buildFacePath(localXY: FloatArray, vids: IntArray): Path {
        val path = Path()
        path.moveTo(localXY[vids[0] * 2], localXY[vids[0] * 2 + 1])
        for (j in 1 until vids.size) {
            path.lineTo(localXY[vids[j] * 2], localXY[vids[j] * 2 + 1])
        }
        path.close()
        return path
    }

    private fun drawWireframe(
        canvas: Canvas,
        paint: Paint,
        localXY: FloatArray,
        visiblePerVertex: BooleanArray,
        color: Int = strokeColor,
        alphaOverride: Int = strokeOpacity,
        honorPaint: Boolean = false
    ) {
        val sp = Paint(paint)
        sp.style = Paint.Style.STROKE
        sp.strokeWidth = strokeWidth.coerceAtLeast(1f)
        sp.strokeJoin = Paint.Join.MITER
        sp.strokeCap = Paint.Cap.ROUND
        sp.shader = null
        sp.maskFilter = null
        sp.setShadowLayer(0f, 0f, 0f, 0)
        if (honorPaint) {
            // Warna/alpha mengikuti Paint yang disuntikkan renderer efek.
        } else {
            sp.color = color
            sp.alpha = alphaOverride.coerceIn(0, 255)
        }

        val edges = edgeList()
        for (e in edges) {
            val a = e[0]
            val b = e[1]
            val hidden = !(visiblePerVertex[a] && visiblePerVertex[b])
            if (hidden && !showHiddenEdges) continue
            sp.pathEffect = null
            val alphaBase = if (honorPaint) sp.alpha else alphaOverride.coerceIn(0, 255)
            if (hidden) {
                sp.pathEffect = DashPathEffect(floatArrayOf(strokeWidth * 2f, strokeWidth * 2f), 0f)
                if (!honorPaint) sp.alpha = (alphaBase * 0.35f).toInt().coerceIn(0, 255)
            } else if (!honorPaint) {
                sp.alpha = alphaBase
            }
            canvas.drawLine(
                localXY[a * 2], localXY[a * 2 + 1],
                localXY[b * 2], localXY[b * 2 + 1],
                sp
            )
        }
        sp.pathEffect = null
    }

    /**
     * Bayangan kotak via silhouette (convex hull 8 vertex proyeksi), di-render
     * offscreen + BlurMaskFilter lalu di-blit; balok digambar di atasnya.
     */
    private fun drawBoxShadow(canvas: Canvas, localXY: FloatArray, w: Float, h: Float) {
        val hull = convexHull(localXY)
        if (hull.size < 3) return

        val r = shadowRadius.coerceIn(0.5f, 40f)
        val dx = shadowDx
        val dy = shadowDy
        val pad = (r + Math.abs(dx) + Math.abs(dy) + 8).toInt().coerceAtLeast(16)
        val bw = (w + pad * 2).toInt().coerceAtLeast(1)
        val bh = (h + pad * 2).toInt().coerceAtLeast(1)
        val bmp = EffectRenderUtils.createOffscreenBitmap(bw, bh)
        val sc = Canvas(bmp)

        val hullPath = Path().apply {
            moveTo(localXY[hull[0] * 2], localXY[hull[0] * 2 + 1])
            for (i in 1 until hull.size) {
                lineTo(localXY[hull[i] * 2], localXY[hull[i] * 2 + 1])
            }
            close()
        }

        val shadowPaint = EffectRenderUtils.createQualityPaint().apply {
            color = shadowColor
            alpha = (((opacity.coerceIn(0, 255) * shadowOpacity.coerceIn(0f, 1f)) / 255f) * 255).toInt().coerceIn(0, 255)
            EffectRenderUtils.applyBlurMaskFilter(this, r, android.graphics.BlurMaskFilter.Blur.NORMAL)
            style = Paint.Style.FILL
        }
        sc.translate(pad.toFloat(), pad.toFloat())
        sc.drawPath(hullPath, shadowPaint)
        EffectRenderUtils.clearMaskFilter(shadowPaint)

        val blitPaint = EffectRenderUtils.createQualityPaint()
        canvas.drawBitmap(bmp, (-pad + dx).toFloat(), (-pad + dy).toFloat(), blitPaint)
        EffectRenderUtils.recycleOffscreenBitmap(bmp)
    }

    /** Convex hull monotonic chain pada 8 titik — mengembalikan indeks vertex. */
    private fun convexHull(xy: FloatArray): List<Int> {
        val pts = (0 until 8).map { it to Pair(xy[it * 2], xy[it * 2 + 1]) }

        fun cross2(ox: Float, oy: Float, ax: Float, ay: Float, bx: Float, by: Float): Float =
            (ax - ox) * (by - oy) - (ay - oy) * (bx - ox)

        val sorted = pts.sortedWith(compareBy<Pair<Int, Pair<Float, Float>>> { it.second.first }
            .thenBy { it.second.second })
        val lower = ArrayDeque<Pair<Int, Pair<Float, Float>>>()
        for (pt in sorted) {
            while (lower.size >= 2 &&
                cross2(lower[lower.size - 2].second.first, lower[lower.size - 2].second.second,
                    lower.last().second.first, lower.last().second.second,
                    pt.second.first, pt.second.second) <= 0f
            ) lower.removeLast()
            lower.addLast(pt)
        }
        val upper = ArrayDeque<Pair<Int, Pair<Float, Float>>>()
        for (pt in sorted.asReversed()) {
            while (upper.size >= 2 &&
                cross2(upper[upper.size - 2].second.first, upper[upper.size - 2].second.second,
                    upper.last().second.first, upper.last().second.second,
                    pt.second.first, pt.second.second) <= 0f
            ) upper.removeLast()
            upper.addLast(pt)
        }
        // Gabung tanpa duplikasi ujung.
        val result = (lower.dropLast(1) + upper.dropLast(1)).distinctBy { it.first }
        return result.map { it.first }
    }
}