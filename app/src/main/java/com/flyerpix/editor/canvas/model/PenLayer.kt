package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Tipe anchor point pada kurva Bézier — menentukan perilaku kontrol handle.
 *
 * - [SMOOTH] — Handle in & out terkunci simetris (gaya Photoshop 'smooth').
 *              Memindahkan satu handle otomatis memutar handle lawannya 180°.
 * - [CORNER] — Handle in & out independen. Sudut tajam/tumpul di anchor.
 */
enum class AnchorType {
    SMOOTH,
    CORNER
}

/**
 * Representasi satu titik anchor (jangkar) pada path Bézier.
 *
 * Setiap anchor memiliki:
 * - **position** `(x, y)` — titik anchor utama pada kurva.
 * - **handleIn** `(handleInX, handleInY)` — kontrol handle masuk (menentukan kurva dari anchor sebelumnya).
 * - **handleOut** `(handleOutX, handleOutY)` — kontrol handle keluar (menentukan kurva ke anchor berikutnya).
 * - **type** [AnchorType] — menentukan apakah handle in & out terkunci simetris ([SMOOTH])
 *   atau independen ([CORNER]).
 *
 * @property id         ID unik anchor untuk referensi cepat (drag, selection).
 * @property x          Posisi X anchor pada koordinat lokal layer.
 * @property y          Posisi Y anchor pada koordinat lokal layer.
 * @property handleInX  Posisi X kontrol handle masuk (default = x).
 * @property handleInY  Posisi Y kontrol handle masuk (default = y).
 * @property handleOutX Posisi X kontrol handle keluar (default = x).
 * @property handleOutY Posisi Y kontrol handle keluar (default = y).
 * @property type       Tipe anchor ([SMOOTH] atau [CORNER]).
 */
data class AnchorPoint(
    var id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var handleInX: Float = x,
    var handleInY: Float = y,
    var handleOutX: Float = x,
    var handleOutY: Float = y,
    var type: AnchorType = AnchorType.SMOOTH
) {
    /** Jarak handle masuk dari anchor. */
    fun handleInDistance(): Float = distance(handleInX, handleInY, x, y)

    /** Jarak handle keluar dari anchor. */
    fun handleOutDistance(): Float = distance(handleOutX, handleOutY, x, y)

    /** Apakah anchor memiliki handle aktif (jarak > 0 dari posisi anchor). */
    fun hasActiveHandles(): Boolean =
        handleInDistance() > 0.01f || handleOutDistance() > 0.01f

    private fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Hasil hit-test pada layer Bézier pen — mengidentifikasi elemen yang disentuh.
 */
sealed class HitAnchor {
    /** Anchor point utama disentuh. */
    data class Anchor(val anchorId: String, val index: Int) : HitAnchor()

    /** Handle masuk disentuh. */
    data class HandleIn(val anchorId: String, val index: Int) : HitAnchor()

    /** Handle keluar disentuh. */
    data class HandleOut(val anchorId: String, val index: Int) : HitAnchor()

    /** Tidak ada elemen yang disentuh. */
    object None : HitAnchor()
}

/**
 * Representasi layer kurva Bézier pada kanvas PixelLab — mode Pen Tool.
 *
 * Pengguna meletakkan titik-titik anchor secara interaktif dan menarik
 * kontrol handle untuk menggambar bentuk kustom yang mulus.
 *
 * **Cara kerja model:**
 *  - [anchors] adalah daftar ordered anchor points.
 *  - Antar dua anchor yang berdekatan → satu segmen Bézier kubik ([Path.cubicTo]).
 *  - Jika [isClosed], anchor terakhir ↔ anchor pertama menyatu membentuk loop.
 *
 * **Anchor Type:**
 *  - [AnchorType.SMOOTH] — Handle in & out terkunci simetris (gaya Photoshop).
 *  - [AnchorType.CORNER] — Handle in & out independen (sudut tajam).
 *
 * **Anchor Management API:**
 *  - [addAnchor], [removeAnchor], [moveAnchor]
 *  - [moveHandleIn], [moveHandleOut], [mirrorHandles]
 *  - [hitTest] untuk interaksi sentuh (drag handle / anchor)
 *  - [closePath], [openPath]
 *
 * **Render pipeline:**
 *  1. Bangun [Path] kubik dari seluruh segmen anchor.
 *  2. Apply fill ([fillColor]) jika [isClosed].
 *  3. Apply stroke ([strokeColor], [strokeWidth]).
 *  4. Transformasi posisi, skala, rotasi, perspektif warping.
 */
data class PenLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Pen / Bézier Properties ────────────────────────────────────────────
    var anchors: MutableList<AnchorPoint> = mutableListOf(),
    var isClosed: Boolean = false,
    // ── Fill ────────────────────────────────────────────────────────────────
    var fillColor: Int = Color.TRANSPARENT,
    var fillEnabled: Boolean = false,
    // ── Stroke ──────────────────────────────────────────────────────────────
    var strokeColor: Int = Color.WHITE,
    var strokeWidth: Float = 4f,
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

    companion object {
        /** Jarak toleransi hit-test anchor & handle (dalam koordinat lokal). */
        const val HIT_TOLERANCE = 24f

        /**
         * Membuat [PenLayer] baru dari daftar titik simpel tanpa handle.
         * Setiap anchor tipe [AnchorType.CORNER] dengan handle di posisi anchor (lurus).
         */
        fun fromPoints(
            points: List<Pair<Float, Float>>,
            closed: Boolean = false
        ): PenLayer {
            val layer = PenLayer(isClosed = closed)
            for ((px, py) in points) {
                layer.anchors.add(AnchorPoint(x = px, y = py, type = AnchorType.CORNER))
            }
            return layer
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Anchor Management — Add / Remove / Move
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Menambahkan anchor baru di akhir daftar.
     *
     * @return Indeks anchor yang baru ditambahkan.
     */
    fun addAnchor(x: Float, y: Float, type: AnchorType = AnchorType.SMOOTH): Int {
        val anchor = AnchorPoint(x = x, y = y, type = type)
        anchors.add(anchor)
        return anchors.lastIndex
    }

    /**
     * Menyisipkan anchor baru di antara dua anchor yang sudah ada.
     *
     * @param afterIndex Indeks anchor SEBELUM posisi penyisipan.
     *                   Anchor baru akan dimasukkan setelah [afterIndex].
     * @return Indeks anchor baru.
     */
    fun insertAnchor(afterIndex: Int, x: Float, y: Float, type: AnchorType = AnchorType.SMOOTH): Int {
        if (afterIndex < 0 || afterIndex >= anchors.size) {
            return addAnchor(x, y, type)
        }
        val anchor = AnchorPoint(x = x, y = y, type = type)
        anchors.add(afterIndex + 1, anchor)
        return afterIndex + 1
    }

    /**
     * Menghapus anchor berdasarkan indeks.
     *
     * @return `true` jika berhasil dihapus, `false` jika indeks tidak valid
     *         atau hanya tersisa ≤ 2 anchor (minimum untuk path bermakna).
     */
    fun removeAnchor(index: Int): Boolean {
        if (index !in anchors.indices) return false
        anchors.removeAt(index)
        return true
    }

    /**
     * Memindahkan posisi anchor berdasarkan indeks dan memperbarui handle secara otomatis.
     *
     * Jika anchor tipe [AnchorType.SMOOTH], handle akan ikut bergeser bersama anchor
     * dengan offset yang sama (simetri dipertahankan).
     */
    fun moveAnchor(index: Int, newX: Float, newY: Float) {
        if (index !in anchors.indices) return
        val anchor = anchors[index]
        val dx = newX - anchor.x
        val dy = newY - anchor.y
        anchor.x = newX
        anchor.y = newY
        anchor.handleInX += dx
        anchor.handleInY += dy
        anchor.handleOutX += dx
        anchor.handleOutY += dy
    }

    /**
     * Menghapus seluruh anchor dan mengosongkan path.
     */
    fun clearAllAnchors() {
        anchors.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Anchor Management — Handle Manipulation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Memindahkan control handle masuk ([handleIn]) dari anchor tertentu.
     *
     * Jika anchor tipe [AnchorType.SMOOTH], handle keluar ([handleOut]) akan
     * otomatis diputar 180° & disesuaikan panjangnya agar simetri.
     */
    fun moveHandleIn(index: Int, newHandleX: Float, newHandleY: Float) {
        if (index !in anchors.indices) return
        val anchor = anchors[index]
        anchor.handleInX = newHandleX
        anchor.handleInY = newHandleY

        if (anchor.type == AnchorType.SMOOTH) {
            // Simetri: putar handleOut 180° dari handleIn relatif terhadap anchor
            anchor.handleOutX = 2 * anchor.x - newHandleX
            anchor.handleOutY = 2 * anchor.y - newHandleY
        }
    }

    /**
     * Memindahkan control handle keluar ([handleOut]) dari anchor tertentu.
     *
     * Jika anchor tipe [AnchorType.SMOOTH], handle masuk ([handleIn]) akan
     * otomatis diputar 180° & disesuaikan panjangnya agar simetri.
     */
    fun moveHandleOut(index: Int, newHandleX: Float, newHandleY: Float) {
        if (index !in anchors.indices) return
        val anchor = anchors[index]
        anchor.handleOutX = newHandleX
        anchor.handleOutY = newHandleY

        if (anchor.type == AnchorType.SMOOTH) {
            anchor.handleInX = 2 * anchor.x - newHandleX
            anchor.handleInY = 2 * anchor.y - newHandleY
        }
    }

    /**
     * Menetapkan tipe anchor ([SMOOTH] atau [CORNER]).
     *
     * Jika beralih ke [SMOOTH] dan anchor memiliki handle aktif,
     * handle out akan otomatis diputar 180° dari handle in.
     */
    fun setAnchorType(index: Int, type: AnchorType) {
        if (index !in anchors.indices) return
        val anchor = anchors[index]
        anchor.type = type
        if (type == AnchorType.SMOOTH && anchor.hasActiveHandles()) {
            anchor.handleOutX = 2 * anchor.x - anchor.handleInX
            anchor.handleOutY = 2 * anchor.y - anchor.handleInY
        }
    }

    /**
     * Membebaskan handle dari simetri (mengubah tipe ke [AnchorType.CORNER])
     * lalu memindahkan handle keluar secara independen.
     */
    fun setHandleOutFree(index: Int, newHandleX: Float, newHandleY: Float) {
        if (index !in anchors.indices) return
        val anchor = anchors[index]
        anchor.type = AnchorType.CORNER
        anchor.handleOutX = newHandleX
        anchor.handleOutY = newHandleY
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Path Open / Close
    // ─────────────────────────────────────────────────────────────────────────

    /** Menutup path (menghubungkan anchor terakhir ↔ anchor pertama). */
    fun closePath() {
        isClosed = true
    }

    /** Membuka path (memisahkan ujung pertama & terakhir). */
    fun openPath() {
        isClosed = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hit-Testing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Hit-test pada koordinat LOKAL layer → mengembalikan elemen yang disentuh.
     *
     * Pencarian diutamakan: Handle In → Handle Out → Anchor utama.
     * Jika lebih dari satu elemen dalam toleransi, yang terdekat diprioritaskan.
     *
     * @param localX Koordinat X dalam ruang koordinat lokal layer (belum ditransform).
     * @param localY Koordinat Y dalam ruang koordinat lokal layer (belum ditransform).
     * @return [HitAnchor] yang terdekat dari titik sentuh, atau [HitAnchor.None].
     */
    fun hitTest(localX: Float, localY: Float): HitAnchor {
        if (anchors.isEmpty()) return HitAnchor.None

        var bestHit: HitAnchor = HitAnchor.None
        var bestDistSq = HIT_TOLERANCE * HIT_TOLERANCE

        for ((i, anchor) in anchors.withIndex()) {
            // Handle In
            val dhiSq = distSq(localX, localY, anchor.handleInX, anchor.handleInY)
            if (dhiSq < bestDistSq && anchor.hasActiveHandles()) {
                bestDistSq = dhiSq
                bestHit = HitAnchor.HandleIn(anchor.id, i)
            }

            // Handle Out
            val dhoSq = distSq(localX, localY, anchor.handleOutX, anchor.handleOutY)
            if (dhoSq < bestDistSq && anchor.hasActiveHandles()) {
                bestDistSq = dhoSq
                bestHit = HitAnchor.HandleOut(anchor.id, i)
            }

            // Anchor utama (prioritas terendah — dicek terakhir agar handle mendahului)
            val daSq = distSq(localX, localY, anchor.x, anchor.y)
            if (daSq < bestDistSq) {
                bestDistSq = daSq
                bestHit = HitAnchor.Anchor(anchor.id, i)
            }
        }
        return bestHit
    }

    private fun distSq(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return dx * dx + dy * dy
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bézier Path Construction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Membangun [Path] Bézier kubik dari seluruh anchor points.
     *
     * Algoritma:
     *  - Anchor pertama → [Path.moveTo]
     *  - Tiap pasangan anchor[i-1] → anchor[i]:
     *    [Path.cubicTo(handleOut[i-1], handleIn[i], position[i])]
     *  - Jika [isClosed]: segmen terakhir menghubungkan anchor terakhir ↔ anchor pertama.
     *
     * @return [Path] yang siap digambar (fill atau stroke).
     */
    fun buildPath(): Path {
        val path = Path()
        if (anchors.isEmpty()) return path

        val first = anchors[0]
        path.moveTo(first.x, first.y)

        for (i in 1 until anchors.size) {
            val prev = anchors[i - 1]
            val curr = anchors[i]
            path.cubicTo(
                prev.handleOutX, prev.handleOutY,
                curr.handleInX, curr.handleInY,
                curr.x, curr.y
            )
        }

        if (isClosed && anchors.size > 1) {
            val last = anchors.last()
            val firstAnchor = anchors[0]
            path.cubicTo(
                last.handleOutX, last.handleOutY,
                firstAnchor.handleInX, firstAnchor.handleInY,
                firstAnchor.x, firstAnchor.y
            )
            path.close()
        }

        return path
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render Pipeline
    // ─────────────────────────────────────────────────────────────────────────

    override fun draw(canvas: Canvas, paint: Paint) {
        if (!isVisible || anchors.isEmpty()) return
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        val saveCount = canvas.save()

        // 1. Transformasi layer luar (Posisi, Skala, Rotasi berpusat pada titik tengah path)
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

        val path = buildPath()
        paint.alpha = opacity.coerceIn(0, 255)
        paint.isAntiAlias = true

        // ── Pass 1: Fill (hanya untuk path tertutup) ─────────────────────────
        if (isClosed && fillEnabled) {
            paint.style = Paint.Style.FILL
            paint.color = fillColor
            paint.strokeWidth = 0f
            canvas.drawPath(path, paint)
        }

        // ── Pass 2: Stroke ──────────────────────────────────────────────────
        if (strokeWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.color = strokeColor
            paint.strokeWidth = strokeWidth
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(path, paint)
        }

        canvas.restoreToCount(saveCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bounds & Copy
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        if (anchors.isEmpty()) return Pair(0f, 0f)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (anchor in anchors) {
            // Anchor position
            minX = min(minX, anchor.x); maxX = max(maxX, anchor.x)
            minY = min(minY, anchor.y); maxY = max(maxY, anchor.y)
            // Handle In
            minX = min(minX, anchor.handleInX); maxX = max(maxX, anchor.handleInX)
            minY = min(minY, anchor.handleInY); maxY = max(maxY, anchor.handleInY)
            // Handle Out
            minX = min(minX, anchor.handleOutX); maxX = max(maxX, anchor.handleOutX)
            minY = min(minY, anchor.handleOutY); maxY = max(maxY, anchor.handleOutY)
        }

        // Margin untuk stroke
        val strokeMargin = strokeWidth / 2f
        val w = (maxX - minX) + strokeMargin * 2
        val h = (maxY - minY) + strokeMargin * 2
        return Pair(max(w, 1f), max(h, 1f))
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

    override fun copyLayer(): PenLayer {
        val anchorsCopy = anchors.map {
            AnchorPoint(
                id = it.id,
                x = it.x,
                y = it.y,
                handleInX = it.handleInX,
                handleInY = it.handleInY,
                handleOutX = it.handleOutX,
                handleOutY = it.handleOutY,
                type = it.type
            )
        }.toMutableList()

        return PenLayer(
            id = UUID.randomUUID().toString(),
            x = this.x + 30f,
            y = this.y + 30f,
            scale = this.scale,
            rotation = this.rotation,
            opacity = this.opacity,
            isLocked = this.isLocked,
            isVisible = this.isVisible,
            anchors = anchorsCopy,
            isClosed = this.isClosed,
            fillColor = this.fillColor,
            fillEnabled = this.fillEnabled,
            strokeColor = this.strokeColor,
            strokeWidth = this.strokeWidth,
            perspectiveEnabled = this.perspectiveEnabled,
            perspectiveCorners = this.perspectiveCorners.clone(),
            blendMode = this.blendMode
        )
    }
}
