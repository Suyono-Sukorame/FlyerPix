package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import java.io.Serializable
import java.util.UUID

/**
 * Preset umum untuk transformasi perspektif warping 3D.
 */
enum class PerspectivePreset {
    FLAT,
    LEFT_WALL,
    RIGHT_WALL,
    TOP_BILLBOARD,
    FLOOR_TILT
}

/**
 * Base abstract class untuk semua elemen layer pada Pixel-Lab canvas.
 * Menyediakan properti transformasi dasar (posisi x & y, skala, rotasi, opasitas),
 * kontrol status (terkunci/isLocked, terlihat/isVisible), transformasi perspektif warping
 * ([Matrix.setPolyToPoly]), serta fungsi penggambaran dan kalkulasi bounding box non-destruktif.
 */
abstract class CanvasLayer(
    open var id: String = UUID.randomUUID().toString(),
    open var x: Float = 0f,
    open var y: Float = 0f,
    open var scale: Float = 1f,
    open var rotation: Float = 0f,
    open var opacity: Int = 255,
    open var isLocked: Boolean = false,
    open var isVisible: Boolean = true,
    open var perspectiveEnabled: Boolean = false,
    open var perspectiveCorners: FloatArray = floatArrayOf(
        0f, 0f,  // Top-Left (x0, y0)
        1f, 0f,  // Top-Right (x1, y1)
        1f, 1f,  // Bottom-Right (x2, y2)
        0f, 1f   // Bottom-Left (x3, y3)
    ),
    open var blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC_OVER
) : Cloneable, Serializable {

    /**
     * Menggambar layer pada [canvas] dengan menggunakan [paint].
     *
     * @param canvas Target canvas untuk rendering layer.
     * @param paint Objek paint dasar yang dapat dikonfigurasi selama penggambaran.
     */
    abstract fun draw(canvas: Canvas, paint: Paint)

    /**
     * Menghitung dan mengembalikan batas koordinat (bounding box) dari layer
     * dalam ruang koordinat kanvas untuk deteksi sentuhan (hit-testing)
     * dan transformasi.
     *
     * @return [RectF] area batas layer.
     */
    abstract fun getBounds(): RectF

    /**
     * Menghasilkan salinan/duplikat (clone) dari layer saat ini dengan properti yang sama.
     *
     * @return Instance baru dari [CanvasLayer] hasil duplikasi.
     */
    abstract fun copyLayer(): CanvasLayer

    /**
     * Menghasilkan kloning identik yang mempertahankan ID dan posisi asli (x, y) tanpa pergeseran offset,
     * khusus untuk keperluan snapshot riwayat Undo/Redo dan serialization.
     */
    open fun cloneLayer(): CanvasLayer {
        val copy = copyLayer()
        copy.id = this.id
        copy.x = this.x
        copy.y = this.y
        return copy
    }

    /**
     * Implementasi Cloneable bawaan Java yang mendelegasikan ke [copyLayer].
     */
    public override fun clone(): CanvasLayer {
        return copyLayer()
    }

    /**
     * Mengembalikan dimensi lokal unwarped (width, height) sebelum transformasi.
     * Digunakan untuk perhitungan perspektif dan handle sudut.
     */
    open fun getUnwarpedDimensions(): Pair<Float, Float> {
        val bounds = getBounds()
        return Pair(bounds.width(), bounds.height())
    }

    /**
     * Memeriksa apakah titik pada kanvas (px, py) berada di dalam batas layer
     * menggunakan perhitungan geometri inversi transformasi (bekerja baik pada runtime Android
     * maupun pengujian unit headless JVM).
     */
    open fun containsCanvasPoint(px: Float, py: Float): Boolean {
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return false

        // Invers transformasi posisi dan rotasi ke koordinat lokal layer
        val cx = x + w / 2f
        val cy = y + h / 2f
        val dx = px - cx
        val dy = py - cy

        val rad = Math.toRadians(-rotation.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)

        val unrotX = (dx * cos - dy * sin)
        val unrotY = (dx * sin + dy * cos)

        val s = if (scale != 0f) scale else 1f
        val localX = (unrotX / s) + w / 2f
        val localY = (unrotY / s) + h / 2f

        return localX in 0f..w && localY in 0f..h
    }

    /**
     * Menghitung matriks transformasi layer luar (posisi x & y, skala, rotasi).
     */
    open fun getLayerTransformMatrix(w: Float, h: Float): Matrix {
        val matrix = Matrix()
        matrix.postTranslate(x, y)
        matrix.postScale(scale, scale, x + w / 2f, y + h / 2f)
        matrix.postRotate(rotation, x + w / 2f, y + h / 2f)
        return matrix
    }

    /**
     * Menghitung matriks transformasi perspektif dari 4 titik sudut menggunakan [Matrix.setPolyToPoly].
     *
     * @param width Lebar unwarped layer dalam piksel.
     * @param height Tinggi unwarped layer dalam piksel.
     * @return [Matrix] perspektif jika valid, atau null jika tidak aktif/terjadi degenerasi.
     */
    fun getPerspectiveMatrix(width: Float, height: Float): Matrix? {
        if (!perspectiveEnabled || width <= 0f || height <= 0f) return null
        val corners = perspectiveCorners
        if (corners.size < 8) return null

        val src = floatArrayOf(
            0f, 0f,
            width, 0f,
            width, height,
            0f, height
        )
        val dst = floatArrayOf(
            corners[0] * width, corners[1] * height,
            corners[2] * width, corners[3] * height,
            corners[4] * width, corners[5] * height,
            corners[6] * width, corners[7] * height
        )
        val matrix = Matrix()
        val success = matrix.setPolyToPoly(src, 0, dst, 0, 4)
        return if (success) matrix else null
    }

    /**
     * Mengembalikan posisi 4 titik handle sudut perspektif dalam koordinat kanvas (screen coordinates).
     * Urutan: Top-Left (0,1), Top-Right (2,3), Bottom-Right (4,5), Bottom-Left (6,7).
     *
     * @return [FloatArray] berisi 8 elemen koordinat (x, y) pada kanvas.
     */
    open fun getPerspectiveScreenPoints(): FloatArray {
        val (w, h) = getUnwarpedDimensions()
        val corners = perspectiveCorners
        val localPts = floatArrayOf(
            corners[0] * w, corners[1] * h,
            corners[2] * w, corners[3] * h,
            corners[4] * w, corners[5] * h,
            corners[6] * w, corners[7] * h
        )

        val matrix = getLayerTransformMatrix(w, h)
        matrix.mapPoints(localPts)
        return localPts
    }

    /**
     * Mentransformasikan satu titik lokal (lx, ly) ke ruang koordinat kanvas (screen space)
     * dengan memperhitungkan titik pusat (w/2, h/2), skala, rotasi, dan translasi (x, y).
     */
    fun mapLocalPointToCanvas(lx: Float, ly: Float, w: Float, h: Float): Pair<Float, Float> {
        val cx = w / 2f
        val cy = h / 2f
        // 1. Skala terhadap titik pusat
        val sx = (lx - cx) * scale
        val sy = (ly - cy) * scale
        // 2. Rotasi terhadap titik pusat
        val rad = Math.toRadians(rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val rx = sx * cos - sy * sin + cx
        val ry = sx * sin + sy * cos + cy
        // 3. Translasi posisi layer (x, y)
        return Pair(rx + x, ry + y)
    }

    /**
     * Mengembalikan 4 titik sudut Bounding Box seleksi dalam ruang koordinat kanvas (screen coordinates).
     * Memperhitungkan margin [padding] serta seluruh transformasi layer (posisi x & y, skala, rotasi, dll).
     *
     * Urutan titik:
     *  - 0, 1: Top-Left (x0, y0)     -> Anchor Handle Duplicate (Prompt 26)
     *  - 2, 3: Top-Right (x1, y1)    -> Anchor Handle Delete (Prompt 26)
     *  - 4, 5: Bottom-Right (x2, y2) -> Anchor Handle Scale / Resize (Prompt 26)
     *  - 6, 7: Bottom-Left (x3, y3)  -> Anchor Handle Rotate (Prompt 26)
     *
     * @param padding Jarak margin padding tambahan di sekeliling batas layer (dalam piksel).
     * @return [FloatArray] berisi 8 elemen koordinat (x, y) pada kanvas.
     */
    open fun getSelectionBoxPoints(padding: Float = 0f): FloatArray {
        val (w, h) = getUnwarpedDimensions()
        val localPts = floatArrayOf(
            -padding, -padding,        // Top-Left
            w + padding, -padding,     // Top-Right
            w + padding, h + padding,  // Bottom-Right
            -padding, h + padding      // Bottom-Left
        )

        val result = FloatArray(8)
        for (i in 0..3) {
            val (cx, cy) = mapLocalPointToCanvas(localPts[i * 2], localPts[i * 2 + 1], w, h)
            result[i * 2] = cx
            result[i * 2 + 1] = cy
        }
        return result
    }

    /**
     * Memperbarui posisi salah satu dari 4 sudut handle perspektif dari koordinat sentuhan kanvas.
     *
     * @param cornerIndex Indeks sudut (0: Top-Left, 1: Top-Right, 2: Bottom-Right, 3: Bottom-Left).
     * @param canvasX Posisi sentuh X pada kanvas.
     * @param canvasY Posisi sentuh Y pada kanvas.
     */
    open fun setPerspectiveCornerFromCanvas(cornerIndex: Int, canvasX: Float, canvasY: Float) {
        if (cornerIndex !in 0..3) return
        val (w, h) = getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        val matrix = getLayerTransformMatrix(w, h)
        val invMatrix = Matrix()
        if (matrix.invert(invMatrix)) {
            val pts = floatArrayOf(canvasX, canvasY)
            invMatrix.mapPoints(pts)
            perspectiveCorners[cornerIndex * 2] = pts[0] / w
            perspectiveCorners[cornerIndex * 2 + 1] = pts[1] / h
        }
    }

    /**
     * Mengatur preset transformasi perspektif populer.
     */
    fun applyPerspectivePreset(preset: PerspectivePreset) {
        perspectiveCorners = when (preset) {
            PerspectivePreset.FLAT -> floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
            PerspectivePreset.LEFT_WALL -> floatArrayOf(0f, -0.2f, 1f, 0.1f, 1f, 0.9f, 0f, 1.2f)
            PerspectivePreset.RIGHT_WALL -> floatArrayOf(0f, 0.1f, 1f, -0.2f, 1f, 1.2f, 0f, 0.9f)
            PerspectivePreset.TOP_BILLBOARD -> floatArrayOf(-0.15f, -0.1f, 1.15f, -0.1f, 1f, 1f, 0f, 1f)
            PerspectivePreset.FLOOR_TILT -> floatArrayOf(0.15f, 0f, 0.85f, 0f, 1.15f, 1f, -0.15f, 1f)
        }
    }

    /**
     * Mengembalikan 4 sudut perspektif ke bentuk persegi semula (normal / flat).
     */
    fun resetPerspective() {
        applyPerspectivePreset(PerspectivePreset.FLAT)
    }

    /**
     * Mengembalikan nama deskriptif dari [blendMode] saat ini.
     */
    fun getBlendModeName(): String = when (blendMode) {
        PorterDuff.Mode.SRC_OVER -> "Normal"
        PorterDuff.Mode.MULTIPLY -> "Multiply"
        PorterDuff.Mode.SCREEN   -> "Screen"
        PorterDuff.Mode.OVERLAY  -> "Overlay"
        PorterDuff.Mode.DARKEN   -> "Darken"
        PorterDuff.Mode.LIGHTEN  -> "Lighten"
        PorterDuff.Mode.ADD      -> "Add"
        else                     -> blendMode.name
    }
}
