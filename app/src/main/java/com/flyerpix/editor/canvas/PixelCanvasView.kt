package com.flyerpix.editor.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import com.flyerpix.editor.filter.FilterEngine
import com.flyerpix.editor.nativepix.FpNative
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import kotlin.math.min
import android.os.Build
import android.os.Process
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import android.view.View
import java.util.concurrent.CountDownLatch
import com.flyerpix.editor.canvas.calc.RotationCalculator
import com.flyerpix.editor.canvas.calc.SnapCalculator
import com.flyerpix.editor.canvas.calc.ViewportCalculator
import com.flyerpix.editor.canvas.gesture.RotationGestureDetector
import com.flyerpix.editor.canvas.history.CanvasHistoryManager
import com.flyerpix.editor.canvas.history.CanvasStateSnapshot
import com.flyerpix.editor.canvas.history.SnapshotCommand
import com.flyerpix.editor.canvas.model.AnchorPoint
import com.flyerpix.editor.canvas.model.AnchorType
import com.flyerpix.editor.canvas.model.ArrowLayer
import com.flyerpix.editor.canvas.model.Box3DLayer
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.HitAnchor
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.ExportFormat
import com.flyerpix.editor.canvas.model.ExportQuality
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.ImageLayer
import com.flyerpix.editor.canvas.model.PenLayer
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.canvas.model.StickerLayer
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.project.ProjectModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max


// Helper extensions untuk perhitungan RectF yang aman pada runtime Android maupun JVM unit test
private inline val RectF.spanX: Float get() = right - left
private inline val RectF.spanY: Float get() = bottom - top
private inline val RectF.midX: Float get() = (left + right) / 2f
private inline val RectF.midY: Float get() = (top + bottom) / 2f

/**
 * Custom View untuk kanvas non-destruktif PixelLab.
 * Mengelola tumpukan layer ([layers]) serta layer yang sedang aktif ([selectedLayer]).
 * Merender setiap layer berurutan berdasarkan urutan z-index.
 */
@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS", "UNNECESSARY_SAFE_CALL")
class PixelCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    /**
     * Daftar seluruh layer pada kanvas, diurutkan dari z-index terendah (bawah)
     * ke z-index tertinggi (atas).
     */
    val layers = mutableListOf<CanvasLayer>()

    // ── Profiling (debug via `adb shell setprop debug.flyerpix_profile 1`) ──
    private var pfFilterMs = 0L
    private var pfAdjustMs = 0L
    private var pfLayersMs = 0L
    private var pfBlurMs = 0L
    private var pfTotalMs = 0L
    private var pfFrameCount = 0
    private var pfFrameStart = 0L

    private fun profileMark(t0: Long): Long = System.nanoTime() - t0

    /** Bersihkan mode blending pada paint (aman di API < 29). */
    private fun Paint.clearBlend() {
        if (Build.VERSION.SDK_INT >= 29) blendMode = null
        xfermode = null
    }

    /**
     * Terapkan blending aktif dari [layer] ke [Paint] sebelum menggambar.
     * Mode lanjutan ([ExtendedBlendMode], API 29+) diprioritaskan; fallback
     * ke [PorterDuff.Mode] bila [CanvasLayer.blendExtra] kosong.
     */
    private fun Paint.applyLayerBlend(layer: CanvasLayer) {
        val extra = layer.blendExtra
        if (extra != null && Build.VERSION.SDK_INT >= 29) {
            xfermode = null
            blendMode = android.graphics.BlendMode.valueOf(extra.modeName)
        } else if (layer.blendMode != PorterDuff.Mode.SRC_OVER) {
            clearBlend()
            xfermode = PorterDuffXfermode(layer.blendMode)
        } else {
            clearBlend()
        }
    }

    /**
     * Layer yang saat ini sedang aktif dipilih oleh pengguna.
     * Setiap kali berubah, [onLayerSelectedListener] akan dipanggil.
     */
    var selectedLayer: CanvasLayer? = null
        set(value) {
            if (field != value) {
                field = value
                if (!suppressLayerSelectEvents) {
                    onLayerSelectedListener?.invoke(value)
                }
                invalidate()
            }
        }

    /**
     * Callback yang dipanggil setiap kali [selectedLayer] berubah.
     * Gunakan ini dari Activity/Fragment untuk menyinkronkan panel kontrol (shadow, stroke, dll.)
     * dengan properti layer yang baru dipilih.
     */
    var onLayerSelectedListener: ((CanvasLayer?) -> Unit)? = null

    /**
     * Dipanggil ketika user mengetuk (tap, bukan drag) sebuah ShapeLayer di kanvas.
     * Dipakai untuk membuka panel pengaturan shape.
     */
    var onShapeTapRequested: ((ShapeLayer) -> Unit)? = null

    private var textEditMode = false

    private var canvasZoom = 1f
    private var canvasPanX = 0f
    private var canvasPanY = 0f
    private val zoomScrollbarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var activeZoomScrollbar = 0
    private var zoomScrollbarTouchOffset = 0f
    // Zoom tidak boleh di bawah 100% (user memilih model zoom naik saja).
    private val minCanvasZoom = 1f
    private val maxCanvasZoom = 7f
    private val zoomStep = 0.25f

    // Matriks transform viewport (identik dengan Canvas translate/scale di onDraw)
    // beserta inversnya, dipakai menyelaraskan koordinat sentuhan Edit Mode agar
    // hit-test & drag objek tetap presisi walau kanvas sempat di-zoom/di-pan.
    private val canvasTransformMatrix = Matrix()
    private val canvasTransformInverse = Matrix()

    val zoomLevel: Float
        get() = canvasZoom

    /**
     * Callback setiap kali [canvasZoom] berubah (baik lewat pinch maupun tombol +/-),
     * dipakai UI (header) untuk memperbarui label persentase.
     */
    var onZoomChangedListener: ((Float) -> Unit)? = null

    var onEditorZoomModeChangedListener: ((Boolean) -> Unit)? = null

    fun setCanvasZoom(zoom: Float) {
        val clamped = zoom.coerceIn(minCanvasZoom, maxCanvasZoom)
        if (canvasZoom != clamped) {
            canvasZoom = clamped
            clampCanvasPan()
            onZoomChangedListener?.invoke(canvasZoom)
            invalidate()
        }
    }

    private fun setCanvasZoomAt(zoom: Float, focusX: Float, focusY: Float) {
        val oldZoom = canvasZoom
        val clamped = zoom.coerceIn(minCanvasZoom, maxCanvasZoom)
        if (oldZoom == clamped) return

        val cx = width / 2f
        val cy = height / 2f
        val worldX = (focusX - cx - canvasPanX) / oldZoom + cx
        val worldY = (focusY - cy - canvasPanY) / oldZoom + cy
        canvasZoom = clamped
        canvasPanX = focusX - cx - (worldX - cx) * clamped
        canvasPanY = focusY - cy - (worldY - cy) * clamped
        clampCanvasPan()
        onZoomChangedListener?.invoke(canvasZoom)
        invalidate()
    }

    fun zoomIn() = setCanvasZoom(canvasZoom + zoomStep)
    fun zoomOut() = setCanvasZoom(canvasZoom - zoomStep)
    fun resetZoom() {
        canvasZoom = minCanvasZoom
        canvasPanX = 0f
        canvasPanY = 0f
        onZoomChangedListener?.invoke(canvasZoom)
        invalidate()
    }

    private var editorZoomMode = false

    /**
     * true = Zoom Mode: pinch mengubah zoom kanvas, seluruh interaksi objek
     * (pilih/geser/resize/rotate/handle) dinonaktifkan.
     */
    val isEditorZoomMode: Boolean
        get() = editorZoomMode

    fun setEditorZoomMode(active: Boolean) {
        if (editorZoomMode == active) return
        editorZoomMode = active
        if (active) {
            requestFocus()
        }
        // Batalkan gestur objek yang mungkin masih berjalan saat mode berubah.
        isDragging = false
        currentTouchState = TouchState.IDLE
        activePerspectiveCorner = -1
        lastTouchX = 0f
        lastTouchY = 0f
        onEditorZoomModeChangedListener?.invoke(active)
        invalidate()
    }

    fun setTextEditMode(active: Boolean) {
        if (textEditMode != active) {
            textEditMode = active
            invalidate()
        }
    }

    // ── Bezier Edit Mode Methods ────────────────────────────────────────────
    
    /**
     * Masuk ke mode edit Bezier: user dapat menggeser/memanipulasi anchor dan handle.
     */
    fun enterBezierEditMode(penLayer: PenLayer) {
        bezierEditMode = true
        selectedBezierLayer = penLayer
        selectedAnchorIndex = -1
        selectedHandleType = null
        invalidate()
    }

    /**
     * Keluar dari mode edit Bezier: kembalikan ke mode normal.
     */
    fun exitBezierEditMode() {
        bezierEditMode = false
        selectedBezierLayer = null
        selectedAnchorIndex = -1
        selectedHandleType = null
        invalidate()
    }

    /** Apakah sedang dalam Bezier edit mode. */
    fun isBezierEditModeActive(): Boolean = bezierEditMode

    /** Dapatkan PenLayer yang sedang diedit, atau null. */
    fun getSelectedBezierLayer(): PenLayer? = selectedBezierLayer

    /** Dapatkan indeks anchor yang sedang dipilih, atau -1. */
    fun getSelectedAnchorIndex(): Int = selectedAnchorIndex

    /**
     * Saat true, perubahan [selectedLayer] tetap terjadi (field & invalidate jalan)
     * tapi [onLayerSelectedListener] tidak dipanggil. Dipakai untuk perubahan seleksi
     * programatik (mis. apply template preset) agar menu navigasi tidak ikut berpindah.
     * Hanya boleh diubah lewat [runWithLayerSelectSuppressed].
     */
    @Volatile
    private var suppressLayerSelectEvents = false

    /**
     * Menjalankan [block] dengan suppresi sementara event seleksi layer
     * (auto-switch menu navigasi tetap di posisi sekarang; seleksi itu sendiri
     * tetap tersimpan). Aman dipanggil berulang (nilai sebelumnya direstore).
     */
    fun runWithLayerSelectSuppressed(block: () -> Unit) {
        val prev = suppressLayerSelectEvents
        suppressLayerSelectEvents = true
        try {
            block()
        } finally {
            suppressLayerSelectEvents = prev
        }
    }

    /**
     * Callback yang dipanggil setiap kali komposisi [layers] berubah
     * (tambah, hapus, duplikasi, reorder, clear, merge, ulang/import, undo/redo).
     * Gunakan ini dari panel layer agar selalu sinkron tanpa bergantung pada perubahan seleksi.
     */
    var onLayersChangedListener: (() -> Unit)? = null

    fun notifyLayersChanged() {
        onLayersChangedListener?.invoke()
    }

    // ── History & Undo/Redo System (Prompt 50) ─────────────────────────────────
    // Getter menautkan onHistoryChanged satu kali. Tanpa tautan ini,
    // onHistoryStateChangedListener tidak akan pernah dipanggil (dead code bug).
    var historyManager: CanvasHistoryManager = CanvasHistoryManager(maxHistorySize = 30)
        get() {
            if (field == null) {
                field = CanvasHistoryManager(maxHistorySize = 30)
            }
            val current = field
            if (current != null && current.onHistoryChanged == null) {
                current.onHistoryChanged = { canUndo, canRedo ->
                    onHistoryStateChangedListener?.invoke(canUndo, canRedo)
                }
            }
            return field
        }

    var onHistoryStateChangedListener: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    private var touchStartState: CanvasStateSnapshot? = null
    private var hasTouchTransformed: Boolean = false

    /**
     * Menangkap snapshot state kanvas saat ini (Prompt 50).
     */
    fun captureCurrentState(actionName: String = "Canvas Change"): CanvasStateSnapshot {
        val currentLayers = layers ?: emptyList()
        val currentBg = canvasBackground ?: CanvasBackground()
        return CanvasStateSnapshot.capture(
            layers = currentLayers,
            background = currentBg,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            selectedLayer = selectedLayer,
            actionName = actionName
        )
    }

    /**
     * Memulihkan kondisi kanvas dari sebuah [CanvasStateSnapshot] (Prompt 50).
     */
    fun restoreState(snapshot: CanvasStateSnapshot) {
        val layerList = layers ?: return
        layerList.clear()
        layerList.addAll(snapshot.layers.map { it.cloneLayer() })
        canvasBackground = snapshot.background
        setCanvasSize(snapshot.canvasWidth, snapshot.canvasHeight)
        selectedLayer = snapshot.selectedLayerIndex?.let { idx ->
            if (idx in layerList.indices) layerList[idx] else null
        }
        invalidate()
        notifyLayersChanged()
    }

    /**
     * Mencatat transisi state dari [beforeState] ke state saat ini ke dalam [historyManager].
     */
    fun recordAction(actionName: String, beforeState: CanvasStateSnapshot) {
        val mgr = historyManager ?: return
        val afterState = captureCurrentState(actionName)
        val command = SnapshotCommand(
            actionName = actionName,
            beforeState = beforeState,
            afterState = afterState,
            applyState = { snapshot -> restoreState(snapshot) }
        )
        mgr.recordCommand(command)
    }

    /**
     * Menjalankan [action] dan otomatis merekam perubahan ke [historyManager] (Command Pattern).
     */
    inline fun runRecordedAction(actionName: String, action: () -> Unit) {
        val mgr = historyManager
        val before = captureCurrentState(actionName)
        action()
        mgr?.let { recordAction(actionName, before) }
    }

    /**
     * Membatalkan tindakan pengguna terakhir (Undo) (Prompt 50).
     * @return true jika berhasil membatalkan tindakan, false jika tidak ada riwayat untuk di-undo.
     */
    fun undo(): Boolean {
        val cmd = historyManager?.undo()
        return cmd != null
    }

    /**
     * Mengulang tindakan yang dibatalkan sebelumnya (Redo) (Prompt 50).
     * @return true jika berhasil mengulang tindakan, false jika tidak ada riwayat untuk di-redo.
     */
    fun redo(): Boolean {
        val cmd = historyManager?.redo()
        return cmd != null
    }

    fun canUndo(): Boolean = historyManager?.canUndo() == true
    fun canRedo(): Boolean = historyManager?.canRedo() == true
    fun clearHistory() = historyManager?.clear()

    // ── Latar Belakang Kanvas Independen (Prompt 44) ───────────────────────

    /**
     * Konfigurasi objek latar belakang kanvas terpadu (Transparan, Solid Color, Gradient).
     */
    var canvasBackground: CanvasBackground = CanvasBackground.solid(Color.WHITE)
        set(value) {
            field = value
            invalidate()
        }

    var isCanvasBackgroundVisible: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Menentukan apakah latar belakang kanvas dalam mode transparan (Prompt 44).
     * Disinkronkan dua arah dengan [canvasBackground].
     */
    var isTransparentBackground: Boolean
        get() = canvasBackground.mode == CanvasBackgroundMode.TRANSPARENT
        set(value) {
            if (value) {
                setTransparentBackground()
            } else if (isTransparentBackground) {
                setColorBackground(canvasBackgroundColor)
            }
        }

    /**
     * Warna latar belakang kanvas saat mode [CanvasBackgroundMode.SOLID_COLOR]. Default adalah putih (Prompt 44).
     * Disinkronkan dua arah dengan [canvasBackground].
     */
    var canvasBackgroundColor: Int
        get() = canvasBackground.solidColor
        set(value) {
            canvasBackground.solidColor = value
            if (canvasBackground.mode != CanvasBackgroundMode.TRANSPARENT &&
                canvasBackground.mode != CanvasBackgroundMode.GRADIENT
            ) {
                canvasBackground.mode = CanvasBackgroundMode.SOLID_COLOR
            }
            invalidate()
        }

    /**
     * Konfigurasi warna gradasi latar belakang kanvas saat mode gradasi aktif (Prompt 44).
     */
    var canvasBackgroundGradient: GradientColor?
        get() = canvasBackground.gradient
        set(value) {
            if (value != null) {
                setGradientBackground(value)
            } else {
                setColorBackground(canvasBackgroundColor)
            }
        }

    /**
     * Mengubah latar belakang kanvas menjadi transparan (checkerboard pattern) (Prompt 44).
     */
    fun setTransparentBackground() {
        runRecordedAction("Set Transparent Background") {
            canvasBackground = CanvasBackground(
                mode = CanvasBackgroundMode.TRANSPARENT,
                solidColor = canvasBackground.solidColor,
                gradient = canvasBackground.gradient
            )
        }
    }

    /**
     * Mengubah latar belakang kanvas menjadi warna solid tertentu (Prompt 44).
     */
    fun setColorBackground(color: Int) {
        runRecordedAction("Change Background Color") {
            canvasBackground = CanvasBackground(
                mode = CanvasBackgroundMode.SOLID_COLOR,
                solidColor = color,
                gradient = canvasBackground.gradient
            )
        }
    }

    /**
     * Mengubah latar belakang kanvas menjadi gradasi warna tertentu (Prompt 44).
     */
    fun setGradientBackground(gradient: GradientColor) {
        runRecordedAction("Change Gradient Background") {
            canvasBackground = CanvasBackground(
                mode = CanvasBackgroundMode.GRADIENT,
                solidColor = canvasBackground.solidColor,
                gradient = gradient
            )
        }
    }

    /**
     * Mengatur gambar sebagai latar belakang kanvas (Prompt 45).
     * Gambar akan di-crop/fill menyesuaikan rasio aspek kanvas aktif.
     *
     * @param bitmap Bitmap gambar dari Galeri atau Kamera.
     */
    fun setImageBackground(bitmap: Bitmap) {
        runRecordedAction("Change Image Background") {
            canvasBackground = CanvasBackground(
                mode = CanvasBackgroundMode.IMAGE,
                solidColor = canvasBackground.solidColor,
                gradient = canvasBackground.gradient,
                imageBitmap = bitmap
            )
        }
    }

    /**
     * Menghapus gambar latar belakang dan mengembalikan ke warna solid terakhir (Prompt 45).
     */
    fun clearImageBackground() {
        runRecordedAction("Remove Image Background") {
            canvasBackground = CanvasBackground(
                mode = CanvasBackgroundMode.SOLID_COLOR,
                solidColor = canvasBackground.solidColor,
                gradient = canvasBackground.gradient,
                imageBitmap = null
            )
        }
    }

    // ── Efek Kanvas Non-Destruktif (Prompt 51) ──────────────────────────────

    /**
     * Efek visual non-destruktif yang dapat diaktifkan/deaktifkan kapan pun
     * tanpa mengubah konten layer maupun latar belakang kanvas.
     */
    enum class CanvasEffect {
        /** Gelap lembut pada tepi kanvas (vignette). */
        VIGNETTE,

        /** Butiran grain lembut di seluruh kanvas. */
        NOISE,

        /** Filter matriks warna (monokrom) untuk seluruh komposisi. */
        FILTER
    }

    /**
     * Efek yang sedang aktif, diurutkan sesuai urutan penerapan rendering.
     * Menggunakan [java.util.LinkedHashSet] agar urutan deterministik.
     *
     * Diwakili nullable + getter lazy agar aman saat instance dibuat via
     * `sun.misc.Unsafe.allocateInstance` di unit test (konstruktor terlewat,
     * sehingga field `by lazy` bernilai null bila tidak dibuat lazy via getter).
     */
    private var activeEffectsField: MutableSet<CanvasEffect>? = null

    private val activeEffects: MutableSet<CanvasEffect>
        get() {
            val existing = activeEffectsField
            if (existing != null) return existing
            return linkedSetOf<CanvasEffect>().also { activeEffectsField = it }
        }

    /**
     * Mencari tahu apakah efek [effect] sedang aktif pada kanvas.
     */
    fun isEffectEnabled(effect: CanvasEffect): Boolean = activeEffects.contains(effect)

    /**
     * Daftar efek yang sedang aktif sesuai urutan penerapan rendering.
     */
    val activeEffectList: List<CanvasEffect>
        get() = activeEffects.toList()

    /**
     * Mengaktifkan ([enabled] = true) atau menonaktifkan ([enabled] = false)
     * efek kanvas [effect] secara non-destruktif.
     */
    fun setEffectEnabled(effect: CanvasEffect, enabled: Boolean) {
        val changed = if (enabled) activeEffects.add(effect) else activeEffects.remove(effect)
        if (changed) invalidate()
    }

    /**
     * Membalik status aktif [effect] dan mengembalikan status baru (true = aktif).
     */
    fun toggleEffect(effect: CanvasEffect): Boolean {
        val next = !activeEffects.contains(effect)
        setEffectEnabled(effect, next)
        return next
    }

    /**
     * Paint dengan [ColorMatrixColorFilter] untuk efek [CanvasEffect.FILTER]
     * (monokrom). Digunakan sebagai paint layer komposit saat [Canvas.saveLayer].
     */
    private val colorMatrixFilterPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply { setSaturation(0f) }
        )
    }

    /**
     * Paint vignette: gradasi radial transparan di tengah menuju gelap di tepi.
     */
    private val vignettePaint = Paint()

    /**
     * Tile bitmap noise skala abu-abu deterministik (seed tetap) untuk efek
     * [CanvasEffect.NOISE]. Dicache agar tidak dibuat ulang setiap frame.
     */
    private val noiseShader: BitmapShader by lazy {
        val size = 96
        val tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rnd = java.util.Random(NOISE_SEED.toLong())
        for (x in 0 until size) {
            for (y in 0 until size) {
                val v = rnd.nextInt(256)
                tile.setPixel(x, y, Color.rgb(v, v, v))
            }
        }
        BitmapShader(tile, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
    }

    /**
     * Paint grain dengan alpha rendah agar noise tampak halus (bukan blok abu-abu).
     */
    private val noisePaint = Paint().apply {
        alpha = NOISE_OVERLAY_ALPHA
    }

    private fun drawVignetteEffect(canvas: Canvas, target: RectF) {
        if (target.isEmpty()) return
        val radius = max(target.width(), target.height()) * 0.75f
        vignettePaint.shader = RadialGradient(
            target.centerX(),
            target.centerY(),
            radius,
            intArrayOf(0x00000000.toInt(), 0x52000000.toInt(), 0xA6000000.toInt()),
            floatArrayOf(0.50f, 0.78f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(target, vignettePaint)
    }

    /**
     * Helper untuk mengekstrak clipping path dari layer source berdasarkan clipping mode.
     * Mengembalikan Path dalam koordinat layer yang akan di-clip, atau null jika tidak valid.
     */
    private fun getClipPathForLayer(layer: CanvasLayer): Path? {
        val clipLayer = layers.find { it.id == layer.clipLayerId } ?: return null
        
        return when (layer.clippingMode) {
            com.flyerpix.editor.canvas.model.ClippingMode.CLIP_TO_SHAPE_PATH -> {
                if (clipLayer is ShapeLayer) {
                    val (w, h) = clipLayer.getUnwarpedDimensions()
                    val clipPath = clipLayer.buildPath()
                    
                    // Transform clip path ke koordinat canvas
                    val transformMatrix = Matrix()
                    transformMatrix.setTranslate(clipLayer.x, clipLayer.y)
                    transformMatrix.preScale(clipLayer.scale, clipLayer.scale, clipLayer.x + w / 2f, clipLayer.y + h / 2f)
                    transformMatrix.preRotate(clipLayer.rotation, clipLayer.x + w / 2f, clipLayer.y + h / 2f)
                    
                    val perspectiveMatrix = clipLayer.getPerspectiveMatrix(w, h)
                    if (perspectiveMatrix != null) {
                        perspectiveMatrix.postConcat(transformMatrix)
                        clipPath.transform(perspectiveMatrix)
                    } else {
                        clipPath.transform(transformMatrix)
                    }
                    clipPath
                } else null
            }
            com.flyerpix.editor.canvas.model.ClippingMode.CLIP_TO_PEN_PATH -> {
                if (clipLayer is PenLayer) {
                    val clipPath = clipLayer.buildPath()
                    val (w, h) = clipLayer.getUnwarpedDimensions()
                    val transformMatrix = Matrix()
                    transformMatrix.setTranslate(clipLayer.x, clipLayer.y)
                    transformMatrix.preScale(clipLayer.scale, clipLayer.scale, clipLayer.x + w / 2f, clipLayer.y + h / 2f)
                    transformMatrix.preRotate(clipLayer.rotation, clipLayer.x + w / 2f, clipLayer.y + h / 2f)
                    
                    val perspectiveMatrix = clipLayer.getPerspectiveMatrix(w, h)
                    if (perspectiveMatrix != null) {
                        perspectiveMatrix.postConcat(transformMatrix)
                        clipPath.transform(perspectiveMatrix)
                    } else {
                        clipPath.transform(transformMatrix)
                    }
                    clipPath
                } else null
            }
            com.flyerpix.editor.canvas.model.ClippingMode.CLIP_TO_TEXT_BOUNDS -> {
                if (clipLayer is TextLayer) {
                    val bounds = clipLayer.getBounds()
                    val clipPath = Path()
                    clipPath.addRect(bounds, Path.Direction.CW)
                    clipPath
                } else null
            }
            com.flyerpix.editor.canvas.model.ClippingMode.NONE -> null
        }
    }

    private fun drawNoiseEffect(canvas: Canvas, target: RectF) {
        if (target.isEmpty()) return
        noisePaint.shader = noiseShader
        canvas.drawRect(target, noisePaint)
    }

    /**
     * Menggambar efek overlay non-destruktif (Noise lalu Vignette) di atas
     * komposisi background + layer dalam area [target].
     *
     * Efek [CanvasEffect.FILTER] tidak digambar di sini karena diterapkan
     * sebagai layer komposit via [colorMatrixFilterPaint] pada tahap konten.
     */
    private fun drawEffectsOverlay(canvas: Canvas, target: RectF, bakedBlur: Boolean) {
        if (isEffectEnabled(CanvasEffect.NOISE) && !bakedBlur) drawNoiseEffect(canvas, target)
        if (isEffectEnabled(CanvasEffect.VIGNETTE) && !bakedBlur) drawVignetteEffect(canvas, target)
    }

    /**
     * Memulai layer komposit ber-filter warna bila efek [CanvasEffect.FILTER] aktif.
     * @return index save to restore via [endFilterEffectLayer], atau -1 jika tidak aktif.
     */
    private fun beginFilterEffectLayer(canvas: Canvas): Int =
        if (isEffectEnabled(CanvasEffect.FILTER)) {
            canvas.saveLayer(null, colorMatrixFilterPaint)
        } else {
            -1
        }

    private fun endFilterEffectLayer(canvas: Canvas, saveIndex: Int) {
        if (saveIndex >= 0) canvas.restoreToCount(saveIndex)
    }

    /**
     * Membangun Paint dengan ColorMatrix gabungan dari nilai adjustments
     * (brightness, contrast, saturation). Mengembalikan null jika semua nilai 0
     * sehingga tidak ada overhead saveLayer yang tidak perlu.
     */
    private fun buildAdjustmentPaint(): Paint? {
        val brightness  = adjustments[CanvasAdjustment.BRIGHTNESS] ?: 0f
        val contrast    = adjustments[CanvasAdjustment.CONTRAST]   ?: 0f
        val saturation  = adjustments[CanvasAdjustment.SATURATION] ?: 0f
        if (brightness == 0f && contrast == 0f && saturation == 0f) return null

        // Contrast: scale sekitar 0.5 (gelap) s/d 1.5 (terang), pivot 128
        val c = 1f + contrast / 100f
        val t = 128f * (1f - c)
        // Brightness: offset langsung ke R/G/B channel
        val b = brightness * 2.55f

        val contrastMatrix = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t + b,
            0f, c, 0f, 0f, t + b,
            0f, 0f, c, 0f, t + b,
            0f, 0f, 0f, 1f, 0f
        ))

        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(1f + saturation / 100f)
        satMatrix.preConcat(contrastMatrix)

        return Paint().apply { colorFilter = ColorMatrixColorFilter(satMatrix) }
    }

    /**
     * Menerapkan transform standar layer: translate(x,y) + scale(S) about center +
     * rotate(R) about center — konsisten dengan drawContent() tiap subkelas.
     * Dipakai untuk overlay mask & komposit mask agar sejajar dengan konten layer.
     */
    private fun applyLayerStandardTransform(canvas: android.graphics.Canvas, layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
        val (wRaw, hRaw) = layer.getUnwarpedDimensions()
        val w = if (wRaw > 0f) wRaw else 1f
        val h = if (hRaw > 0f) hRaw else 1f
        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(layer.x, layer.y)
        canvas.scale(layer.scale, layer.scale, cx, cy)
        canvas.rotate(layer.rotation, cx, cy)
    }

    /** Sanitasi paint bersama agar tidak terjadi shared-Paint pollution antar layer. */
    private fun sanitizeSharedPaint() {
        renderPaint.style = android.graphics.Paint.Style.FILL
        renderPaint.strokeWidth = 0f
    }

    /** Merender seluruh layer terlihat secara berurutan sesuai z-index. */
    private fun drawVisibleLayers(canvas: Canvas) {
        for (i in 0 until layers.size) {
            val layer = layers[i]
            if (layer.isVisible) {
                // Apply clipping path jika layer memiliki clipping active (Phase 8)
                val clipPath = if (layer.clippingMode != com.flyerpix.editor.canvas.model.ClippingMode.NONE && layer.clipLayerId != null) {
                    getClipPathForLayer(layer)
                } else {
                    null
                }

                // Apply layer mask if enabled (Phase 9-10)
                val hasMask = layer.hasMask()

                if (layer.blendMode != PorterDuff.Mode.SRC_OVER || layer.blendExtra != null || hasMask) {
                    renderPaint.applyLayerBlend(layer)
                    val saveCount = canvas.saveLayer(null, renderPaint)
                    if (clipPath != null) canvas.clipPath(clipPath)
                    sanitizeSharedPaint()
                    layer.draw(canvas, renderPaint)
                    sanitizeSharedPaint()
                    // Apply mask DST_IN
                    if (hasMask && layer.maskBitmap != null) {
                        val maskPaint = android.graphics.Paint().apply {
                            alpha = 255
                            xfermode = android.graphics.PorterDuffXfermode(
                                if (layer.maskInverted) android.graphics.PorterDuff.Mode.DST_OUT
                                else android.graphics.PorterDuff.Mode.DST_IN
                            )
                        }
                        canvas.save()
                        applyLayerStandardTransform(canvas, layer)
                        canvas.drawBitmap(layer.maskBitmap!!, 0f, 0f, maskPaint)
                        canvas.restore()
                    }
                    canvas.restoreToCount(saveCount)
                    renderPaint.clearBlend()
                } else {
                    renderPaint.clearBlend()
                    val saveCount = canvas.save()
                    if (clipPath != null) canvas.clipPath(clipPath)
                    sanitizeSharedPaint()
                    layer.draw(canvas, renderPaint)
                    sanitizeSharedPaint()
                    canvas.restoreToCount(saveCount)
                }
            }
        }
    }

    /** Merender konten komposisi: background, grid, layer, free draw. */
    private fun drawCompositionContent(canvas: Canvas, vp: RectF) {
        drawBackgroundOnCanvas(canvas, vp)
        if (isGridEnabled) drawGridGuidelines(canvas, vp)
        val tLayers = System.nanoTime()
        drawVisibleLayers(canvas)
        if (profileEnabled) pfLayersMs += profileMark(tLayers)
        if (freeDrawActive && freeDrawPoints.size >= 2) {
            val path = android.graphics.Path()
            path.moveTo(freeDrawPoints[0].first, freeDrawPoints[0].second)
            for (p in freeDrawPoints) path.lineTo(p.first, p.second)
            canvas.drawPath(path, freeDrawPaint)
        }

        // Overlay seleksi aktif — putus-putus transparan (Prompt 10)
        selectionPath?.let { selPath ->
            val fill = android.graphics.Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
                color = 0x400088FF.toInt()
                pathEffect = null
            }
            canvas.drawPath(selPath, fill)
            val stroke = android.graphics.Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                color = 0xAA0088FF.toInt()
                strokeWidth = 2f * resources.displayMetrics.density
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f * resources.displayMetrics.density, 6f * resources.displayMetrics.density), 0f)
            }
            canvas.drawPath(selPath, stroke)
        }
    }

    // ── Crop Canvas (Prompt 46) ─────────────────────────────────────────────

    /**
     * Menerapkan operasi crop pada kanvas.
     * Mengubah resolusi logika canvas dan memindahkan semua layer agar posisi visualnya tetap sama.
     *
     * @param newLeft Posisi left crop dalam koordinat viewport (bukan logical canvas).
     * @param newTop Posisi top crop dalam koordinat viewport.
     * @param newRight Posisi right crop dalam koordinat viewport.
     * @param newBottom Posisi bottom crop dalam koordinat viewport.
     */
    fun cropCanvas(newLeft: Float, newTop: Float, newRight: Float, newBottom: Float) {
        val vp = viewportRect
        if (vp.width() <= 0f || vp.height() <= 0f) return

        // Konversi viewport koordinat ke logical canvas koordinat
        val scaleX = canvasWidth.toFloat() / vp.width()
        val scaleY = canvasHeight.toFloat() / vp.height()

        val cropLogicalLeft = ((newLeft - vp.left) * scaleX).toInt().coerceIn(0, canvasWidth)
        val cropLogicalTop = ((newTop - vp.top) * scaleY).toInt().coerceIn(0, canvasHeight)
        val cropLogicalRight = ((newRight - vp.left) * scaleX).toInt().coerceIn(cropLogicalLeft + 1, canvasWidth)
        val cropLogicalBottom = ((newBottom - vp.top) * scaleY).toInt().coerceIn(cropLogicalTop + 1, canvasHeight)

        val newCanvasW = cropLogicalRight - cropLogicalLeft
        val newCanvasH = cropLogicalBottom - cropLogicalTop
        if (newCanvasW <= 0 || newCanvasH <= 0) return

        runRecordedAction("Crop Canvas") {
            // Offset perpindahan viewport (dalam viewport pixels)
            val vpOffsetX = cropLogicalLeft / scaleX
            val vpOffsetY = cropLogicalTop / scaleY

            // Pindahkan semua layer agar posisi visualnya tetap sama
            for (layer in layers) {
                layer.x -= vpOffsetX
                layer.y -= vpOffsetY
            }

            // Update ukuran canvas
            canvasWidth = newCanvasW
            canvasHeight = newCanvasH
            updateViewport()
            invalidate()
        }
    }

    // ── Ukuran Resolusi Kanvas & Viewport (Prompt 43) ───────────────────────

    /**
     * Lebar resolusi logika kanvas dalam piksel (default 1080).
     */
    var canvasWidth: Int = 1080
        private set

    /**
     * Tinggi resolusi logika kanvas dalam piksel (default 1080).
     */
    var canvasHeight: Int = 1080
        private set

    /**
     * Area viewport kanvas di layar yang mempertahankan rasio aspek resolusi kanvas secara proporsional.
     */
    val viewportRect = RectF()

    /**
     * Rasio aspek resolusi kanvas saat ini (lebar / tinggi).
     */
    val canvasAspectRatio: Float
        get() = if (canvasHeight > 0) canvasWidth.toFloat() / canvasHeight.toFloat() else 1f

    /**
     * Mengatur resolusi logika kanvas dan menghitung ulang area viewport proporsional di layar.
     *
     * @param width Lebar kanvas baru dalam piksel (50..8192).
     * @param height Tinggi kanvas baru dalam piksel (50..8192).
     */
    fun setCanvasSize(width: Int, height: Int) {
        val newW = width.coerceIn(50, 8192)
        val newH = height.coerceIn(50, 8192)
        if (canvasWidth != newW || canvasHeight != newH) {
            canvasWidth = newW
            canvasHeight = newH
            updateViewport()
            invalidate()
        }
    }

    /**
     * Memperbarui koordinat [viewportRect] berdasarkan dimensi View dan resolusi kanvas target.
     */
    fun updateViewport() {
        if (width > 0 && height > 0) {
            val vp = ViewportCalculator.calculate(width, height, canvasWidth, canvasHeight)
            viewportRect.left = vp.left
            viewportRect.top = vp.top
            viewportRect.right = vp.right
            viewportRect.bottom = vp.bottom
        } else {
            viewportRect.left = 0f
            viewportRect.top = 0f
            viewportRect.right = 0f
            viewportRect.bottom = 0f
        }
    }

    /**
     * Paint yang digunakan kembali untuk operasi rendering background warna solid.
     */
    private val canvasBgPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    /**
     * Paint yang digunakan kembali untuk operasi rendering layer.
     */
    protected val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * Paint dengan BitmapShader untuk pola catur transparan (checkerboard).
     */
    private val checkerboardPaint = Paint()

    /**
     * State interaksi sentuhan kanvas PixelLab saat ini.
     */
    enum class TouchState {
        IDLE,
        DRAGGING_LAYER,
        DRAGGING_RESIZE_HANDLE,
        DRAGGING_ROTATE_HANDLE,
        DRAGGING_PERSPECTIVE_HANDLE,
        DRAGGING_BOX3D_DEPTH_HANDLE,
        DRAGGING_BOX3D_VP_HANDLE
    }

    var currentTouchState: TouchState = TouchState.IDLE
        private set

    // ── State Drag Resize 8-Handle Universal ────────────────────────────────
    // Anchor (sudut/sisi lawan yang tetap diam) plus snapshot awal layer agar
    // setiap pergerakan MOVE menghitung ulang nilai dengan basis yang stabil.
    private var activeResizeHandle: TransformHandle = TransformHandle.NONE
    private var resizeAnchorPoint: Pair<Float, Float> = Pair(0f, 0f)
    private var resizeStartDist: Float = 0f
    private var resizeInitialScale: Float = 1f
    private var resizeInitialStretchX: Float = 1f
    private var resizeInitialStretchY: Float = 1f
    private var resizeInitialW: Float = 0f
    private var resizeInitialH: Float = 0f
    private var resizeAnchorFracX: Float = 0f
    private var resizeAnchorFracY: Float = 0f
    private var resizeStartWrapWidth: Float = 0f
    private var resizeStartTextSize: Float = 0f

    // ── State Drag Depth Handle 3D Box ───────────────────────────────────────
    private var box3DDepthStartDepth: Float = 0f
    private var box3DDepthStartDist: Float = 0f
    private var box3DDepthBaseX: Float = 0f
    private var box3DDepthBaseY: Float = 0f

    // ── Mode Eyedropper (Prompt 42) ──────────────────────────────────────────

    /** Apakah kanvas sedang dalam mode eyedropper (pipet warna). */
    var isEyedropperMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    capturedBitmap = captureCanvasToBitmap()
                } else {
                    capturedBitmap?.recycle()
                    capturedBitmap = null
                }
            }
        }

    // ── Mode Gambar Bebas (Freehand) ────────────────────────────────────────

    // ── Mode Lukis Mask (Prompt 06) ─────────────────────────────────────────
    /** Konfigurasi brush mask saat mode lukis mask aktif. */
    class MaskPaintBrushState(
        var brushSize: Float = 40f,
        var brushOpacity: Int = 255,
        var brushColor: Int = 0xFFFFFFFF.toInt(),
        var isEraser: Boolean = false
    ) {
        // ── Pressure Sensitivity (OPTION D Phase 1) ──────────────────────
        var pressureSensitivityEnabled: Boolean = true
        var currentPressure: Float = 1f  // 0..1, 1 = max pressure

        // ── Clone Mode (OPTION D Phase 4) ─────────────────────────────
        var cloneMode: Boolean = false
        var cloneSourceX: Float? = null
        var cloneSourceY: Float? = null

        // ── Auto-Color Erase (OPTION D Phase 3) ───────────────────────
        var autoEraseMode: Boolean = false
        var autoEraseThreshold: Int = 50  // 0-255 color distance threshold
        var autoEraseSourceColor: Int? = null  // Cached color for auto-erase

        // ── Erase BG: Finger Offset Cursor & Restore Mode ─────────────
        /** Jarak offset kursor penghapus dari titik sentuh jari (dp). */
        var fingerOffsetDp: Float = 55f
        /** Jika true: hapus (mask=0/transparan); false: pulihkan (mask=opaque/putih). */
        var eraseMode: Boolean = true  // true = hapus, false = pulihkan (restore)
        /** Apakah fitur Erase BG sedang aktif (menggantikan mode Mask Paint standar). */
        var eraseBgActive: Boolean = false
    }

    /** Layer target yang sedang dilukis mask-nya; null = mode tidak aktif. */
    private var maskPaintLayer: com.flyerpix.editor.canvas.model.CanvasLayer? = null

    /** State brush mask aktif. */
    private val maskPaintBrush = MaskPaintBrushState()

    /** Expose mask paint brush state for UI callbacks (Phase 4 & 5). */
    fun getMaskPaintBrush(): MaskPaintBrushState = maskPaintBrush

    /** Snapshot riwayat sebelum sesi lukis dimulai (commit undo saat Selesai). */
    private var maskPaintBeforeSnapshot: CanvasStateSnapshot? = null

    /** Titik brush terakhir (untuk interpolasi garis antar MOVE). */
    private var lastMaskPaintPoint: android.graphics.PointF? = null

    /** Koordinat sentuhan layar mentah (sebelum offset) untuk overlay cursor Erase BG. */
    private var eraseTouchScreenX: Float = -1f
    private var eraseTouchScreenY: Float = -1f
    /** Apakah jari sedang menyentuh layar dalam mode Erase BG. */
    private var eraseIsTouching: Boolean = false

    // ── Undo/Redo per goresan selama sesi Erase BG (tombol ↩️/↪️ di panel) ──
    // Snapshot mask TIDAK memakai riwayat kanvas global (yang hanya mencatat SATU
    // entri saat Selesai). Dua stack Bitmap ringkas ini memungkinkan undo/redo
    // per goresan secara live selama sesi tanp merusak history produksi.
    private val eraseStrokeUndo = ArrayDeque<android.graphics.Bitmap>()
    private val eraseStrokeRedo = ArrayDeque<android.graphics.Bitmap>()
    private val eraseStrokeLimit = 8

    /** Simpan snapshot mask sebelum goresan baru dimulai (panggil saat ACTION_DOWN). */
    private fun snapshotEraseStroke(mask: android.graphics.Bitmap) {
        if (mask.isRecycled) return
        val copy = mask.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return
        if (eraseStrokeUndo.size >= eraseStrokeLimit) {
            eraseStrokeUndo.removeFirst().recycle()
        }
        eraseStrokeUndo.addLast(copy)
        // Goresan baru membatalkan riwayat redo.
        while (eraseStrokeRedo.isNotEmpty()) eraseStrokeRedo.removeFirst().recycle()
    }

    private fun restoreMaskFrom(restore: android.graphics.Bitmap): Boolean {
        val mask = maskPaintLayer?.maskBitmap ?: return false
        if (mask.isRecycled || restore.isRecycled) return false
        mask.eraseColor(0)
        android.graphics.Canvas(mask).drawBitmap(restore, 0f, 0f, null)
        invalidate()
        return true
    }

    /** Undo goresan terakhir dalam sesi Erase BG. */
    fun undoEraseStroke(): Boolean {
        val mask = maskPaintLayer?.maskBitmap
        if (mask == null || mask.isRecycled) return false
        val prev = eraseStrokeUndo.removeLastOrNull() ?: return false
        val currentCopy = mask.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        if (currentCopy != null) eraseStrokeRedo.addLast(currentCopy)
        val ok = restoreMaskFrom(prev)
        prev.recycle()
        return ok
    }

    /** Redo goresan yang dibatalkan dalam sesi Erase BG. */
    fun redoEraseStroke(): Boolean {
        val mask = maskPaintLayer?.maskBitmap
        if (mask == null || mask.isRecycled) return false
        val next = eraseStrokeRedo.removeLastOrNull() ?: return false
        val currentCopy = mask.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        if (currentCopy != null) {
            if (eraseStrokeUndo.size >= eraseStrokeLimit) eraseStrokeUndo.removeFirst().recycle()
            eraseStrokeUndo.addLast(currentCopy)
        }
        val ok = restoreMaskFrom(next)
        next.recycle()
        return ok
    }

    private fun clearEraseStrokeHistory() {
        while (eraseStrokeUndo.isNotEmpty()) eraseStrokeUndo.removeFirst().recycle()
        while (eraseStrokeRedo.isNotEmpty()) eraseStrokeRedo.removeFirst().recycle()
    }

    /** Apakah mode lukis mask sedang aktif. */
    val isMaskPaintActive: Boolean get() = maskPaintLayer != null

    /** Apakah sesi Erase BG sedang aktif (panel kontrol sedang terbuka). */
    val isEraseBgActive: Boolean get() = maskPaintBrush.eraseBgActive

    /**
     * Memulai sesi lukis mask [layer] dengan konfigurasi brush. Mask dibuat otomatis
     * bila belum ada. Undo dicatat hanya saat [endMaskPaint] dipanggil (tombol Selesai).
     */
    fun startMaskPaint(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
        if (layer.maskBitmap == null) {
            val (w, h) = layer.getUnwarpedDimensions()
            if (w > 0 && h > 0) {
                layer.createMask(w.toInt(), h.toInt())
                // ── Erase BG: Inisialisasi mask PUTIH = foto tetap 100% terlihat ──
                // Hitam = hapus (transparan), Putih = tampil. Default putih agar
                // pengguna melihat foto utuh saat mode Erase BG dibuka pertama kali.
                if (maskPaintBrush.eraseBgActive) {
                    layer.maskBitmap?.eraseColor(0xFFFFFFFF.toInt())
                }
            }
        } else if (maskPaintBrush.eraseBgActive && layer.maskBitmap != null) {
            // Jika mask sudah ada namun Erase BG baru diaktifkan: reset ke putih
            layer.maskBitmap!!.eraseColor(0xFFFFFFFF.toInt())
        }
        maskPaintLayer = layer
        lastMaskPaintPoint = null
        eraseIsTouching = false
        eraseTouchScreenX = -1f
        eraseTouchScreenY = -1f
        maskPaintBeforeSnapshot = captureCurrentState("Mask Paint")
        invalidate()
    }

    /**
     * Mulai sesi Erase BG dengan konfigurasi offset jari dan mode hapus/pulihkan.
     * Berbeda dari [startMaskPaint] standar: mask diinisialisasi putih (foto terlihat).
     */
    fun startEraseBg(layer: com.flyerpix.editor.canvas.model.CanvasLayer, fingerOffsetDp: Float = 55f) {
        maskPaintBrush.eraseBgActive = true
        maskPaintBrush.eraseMode = true
        maskPaintBrush.fingerOffsetDp = fingerOffsetDp
        maskPaintBrush.isEraser = false
        clearEraseStrokeHistory()
        startMaskPaint(layer)
    }

    /** Update konfigurasi Erase BG dari panel kontrol. */
    fun updateEraseBgConfig(
        brushSize: Float? = null,
        fingerOffsetDp: Float? = null,
        eraseMode: Boolean? = null,
        autoColorThreshold: Int? = null,
        autoEraseMode: Boolean? = null
    ) {
        brushSize?.let { maskPaintBrush.brushSize = it.coerceIn(5f, 200f) }
        fingerOffsetDp?.let { maskPaintBrush.fingerOffsetDp = it.coerceIn(0f, 150f) }
        eraseMode?.let { maskPaintBrush.eraseMode = it }
        autoColorThreshold?.let { maskPaintBrush.autoEraseThreshold = it.coerceIn(5, 180) }
        autoEraseMode?.let { maskPaintBrush.autoEraseMode = it }
        if (autoEraseMode == true) {
            // Warna sumber diambil ulang pada tap berikutnya setelah ganti mode.
            maskPaintBrush.autoEraseSourceColor = null
            maskPaintBrush.eraseMode = true
        }
        invalidate()
    }

    /** Memperbarui konfigurasi brush mask secara live dari panel. */
    fun updateMaskBrush(
        brushSize: Float,
        brushOpacity: Int,
        brushColor: Int,
        isEraser: Boolean
    ) {
        maskPaintBrush.brushSize = brushSize.coerceIn(2f, 400f)
        maskPaintBrush.brushOpacity = brushOpacity.coerceIn(0, 255)
        maskPaintBrush.brushColor = brushColor
        maskPaintBrush.isEraser = isEraser
    }

    /** Mengakhiri sesi lukis mask dan mencatat satu entri undo (aksi seluruh sesi). */
    fun endMaskPaint(newState: com.flyerpix.editor.canvas.model.CanvasLayer? = null) {
        val layer = maskPaintLayer ?: newState ?: return
        if (layer.maskBitmap != null && maskPaintBeforeSnapshot != null) {
            try {
                recordAction("Mask Paint", maskPaintBeforeSnapshot!!)
            } catch (_: Throwable) {
            }
        }
        maskPaintLayer = null
        maskPaintBeforeSnapshot = null
        lastMaskPaintPoint = null
        maskPaintLayer = null
        invalidate()
    }

    /**
     * Menyelesaikan sesi Erase BG: simpan mask sebagai satu entri undo (Selesai)
     * lalu matikan seluruh state Erase BG.
     */
    fun endEraseBg(layer: com.flyerpix.editor.canvas.model.CanvasLayer? = null) {
        endMaskPaint(layer)
        maskPaintBrush.eraseBgActive = false
        maskPaintBrush.eraseMode = true
        maskPaintBrush.autoEraseMode = false
        maskPaintBrush.autoEraseSourceColor = null
        eraseIsTouching = false
        eraseTouchScreenX = -1f
        eraseTouchScreenY = -1f
        clearEraseStrokeHistory()
    }

    /**
     * Membatalkan sesi Erase BG (Batal): kembalikan ke snapshot awal
     * (mask sebelum sesi dimulai) dan matikan seluruh state Erase BG.
     */
    fun cancelEraseBg() {
        val snapshot = maskPaintBeforeSnapshot
        maskPaintBeforeSnapshot = null
        maskPaintLayer = null
        lastMaskPaintPoint = null
        maskPaintBrush.eraseBgActive = false
        maskPaintBrush.eraseMode = true
        maskPaintBrush.autoEraseMode = false
        maskPaintBrush.autoEraseSourceColor = null
        eraseIsTouching = false
        eraseTouchScreenX = -1f
        eraseTouchScreenY = -1f
        clearEraseStrokeHistory()
        if (snapshot != null) {
            try {
                restoreState(snapshot)
            } catch (_: Throwable) {
            }
        }
        invalidate()
    }

    /** Melukis goresan mask di [screenX]/[screenY] (koordinat layar mentah). */
    private fun maskPaintAt(screenX: Float, screenY: Float, pressure: Float = 1f) {
        val layer = maskPaintLayer ?: return
        val mask = layer.maskBitmap ?: return

        // ── Erase BG: Hitung koordinat target dengan finger offset ─────
        val density = resources.displayMetrics.density
        val targetScreenY = if (maskPaintBrush.eraseBgActive) {
            screenY - (maskPaintBrush.fingerOffsetDp * density)
        } else {
            screenY
        }

        // Transformasi ke koordinat kanvas (sudah diperhitungkan zoom/pan)
        val canvasPts = floatArrayOf(screenX, targetScreenY)
        canvasTransformInverse.mapPoints(canvasPts)
        val canvasX = canvasPts[0]
        val canvasY = canvasPts[1]

        val (lx, ly) = canvasToMaskLocal(layer, canvasX, canvasY)

        // ── Phase 1: Pressure Sensitivity ────────────────────────────
        val effectivePressure = if (maskPaintBrush.pressureSensitivityEnabled) {
            pressure.coerceIn(0f, 1f)
        } else {
            1f
        }
        maskPaintBrush.currentPressure = effectivePressure

        // ── Phase 2: Zoom-Aware Brush Sizing ──────────────────────────
        val zoomFactor = kotlin.math.abs(1f / zoomLevel).coerceIn(0.5f, 2f)
        val effectiveBrushSize = (maskPaintBrush.brushSize * zoomFactor * effectivePressure)
            .coerceIn(2f, 400f)

        // ── Compute effective opacity with pressure ───────────────────
        val effectiveOpacity = (maskPaintBrush.brushOpacity.toFloat() * effectivePressure)
            .toInt().coerceIn(0, 255)

        // ── Phase 3: Auto-Color Erase Mode ───────────────────────────
        if (maskPaintBrush.autoEraseMode) {
            autoEraseAt(layer, mask, lx.toInt(), ly.toInt(), effectiveBrushSize.toInt())
            lastMaskPaintPoint = android.graphics.PointF(lx, ly)
            invalidate()
            return
        }

        // ── Phase 4: Clone Stamp Mode ───────────────────────────────
        if (maskPaintBrush.cloneMode) {
            val layerBitmap = if (layer is com.flyerpix.editor.canvas.model.ImageLayer) {
                layer.bitmap
            } else {
                return  // Clone stamp only works on image layers
            }
            cloneStampAt(layer, layerBitmap, lx.toInt(), ly.toInt(), effectiveBrushSize.toInt())
            lastMaskPaintPoint = android.graphics.PointF(lx, ly)
            invalidate()
            return
        }

        // ── Erase BG: Manual Hapus atau Pulihkan (Restore) ────────────
        // eraseMode=true  → set mask px ke TRANSPARENT (0x00000000 = foto hilang)
        // eraseMode=false → set mask px ke WHITE/OPAQUE (0xFFFFFFFF = foto tampil)
        val targetMaskColor = if (maskPaintBrush.eraseBgActive) {
            if (maskPaintBrush.eraseMode) 0x00000000.toInt() else 0xFFFFFFFF.toInt()
        } else {
            // Mode mask standar: isEraser=true → putih (tampil), false → warna brush
            if (maskPaintBrush.isEraser) 0xFFFFFFFF.toInt() else maskPaintBrush.brushColor
        }

        // ── Manual Paint Mode (Standard & Erase BG) ──────────────────
        // Manual Erase BG (eraseBgActive && eraseMode) menulis mask TRANSPARAN.
        // Warna 0x00000000 ber-alpha 0 tidak akan menggambar apa pun dengan mode SRC,
        // jadi gunakan PorterDuff.CLEAR (hasil = transparan) agar goresan terlihat.
        val eraseToTransparent = maskPaintBrush.eraseBgActive && maskPaintBrush.eraseMode
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = android.graphics.PorterDuffXfermode(
                if (eraseToTransparent) android.graphics.PorterDuff.Mode.CLEAR
                else android.graphics.PorterDuff.Mode.SRC
            )
            color = targetMaskColor
            alpha = if (maskPaintBrush.eraseBgActive) 255 else effectiveOpacity
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = effectiveBrushSize
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        android.graphics.Canvas(mask).apply {
            val last = lastMaskPaintPoint
            if (last != null) {
                drawLine(last.x, last.y, lx, ly, paint)
            } else {
                drawCircle(lx, ly, effectiveBrushSize / 2f, paint)
            }
            drawCircle(lx, ly, effectiveBrushSize / 2f, paint)
        }
        lastMaskPaintPoint = android.graphics.PointF(lx, ly)
        invalidate()
    }

    /**
     * Phase 3: Auto-Color Erase / Magic Erase.
     *
     * Algorithm:
     * 1. Tap pertama (autoEraseSourceColor == null): ambil warna referensi dari piksel
     *    foto di titik sentuh, lalu FLOOD FILL area tersambung yang sewarna (≤ threshold)
     *    sehingga background langsung bersih dalam sekali tap.
     * 2. Saat digeser (drag): terapkan stamp lingkaran berukuran brush yang membandingkan
     *    warna foto sekitar dengan warna referensi; piksel mirip → mask transparan.
     */
    private fun autoEraseAt(
        layer: com.flyerpix.editor.canvas.model.CanvasLayer,
        mask: android.graphics.Bitmap,
        px: Int,
        py: Int,
        brushRadius: Int
    ) {
        val bitmapForSample = if (layer is com.flyerpix.editor.canvas.model.ImageLayer) layer.bitmap else null

        // ── Tap pertama: sample warna + flood fill wilayah tersambung ──
        if (maskPaintBrush.autoEraseSourceColor == null) {
            val sourceColor = if (bitmapForSample != null) {
                val sampleX = px.coerceIn(0, bitmapForSample.width - 1)
                val sampleY = py.coerceIn(0, bitmapForSample.height - 1)
                bitmapForSample.getPixel(sampleX, sampleY)
            } else {
                0xFFFFFFFF.toInt()
            }
            maskPaintBrush.autoEraseSourceColor = sourceColor
            floodFillEraseBitmap(mask, bitmapForSample, px, py, sourceColor)
            invalidate()
            return
        }

        // ── Drag: stamp brush berukuran brushRadius (dukungan area yang lebih luas) ──
        val sourceColor = maskPaintBrush.autoEraseSourceColor ?: 0xFFFFFFFF.toInt()
        val threshold = maskPaintBrush.autoEraseThreshold
        val r = brushRadius.coerceAtLeast(1)
        val startX = (px - r).coerceAtLeast(0)
        val startY = (py - r).coerceAtLeast(0)
        val endX = (px + r).coerceAtMost(mask.width - 1)
        val endY = (py + r).coerceAtMost(mask.height - 1)
        val w = endX - startX + 1
        val h = endY - startY + 1
        if (w <= 0 || h <= 0) return

        val maskPixels = IntArray(w * h)
        mask.getPixels(maskPixels, 0, w, startX, startY, w, h)

        val bitmapPixels = if (bitmapForSample != null && startX < bitmapForSample.width && startY < bitmapForSample.height) {
            val bw = endX.coerceAtMost(bitmapForSample.width - 1) - startX + 1
            val bh = endY.coerceAtMost(bitmapForSample.height - 1) - startY + 1
            IntArray(bw * bh).also { bitmapForSample.getPixels(it, 0, bw, startX, startY, bw, bh) }
        } else null

        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val distFromCenter = kotlin.math.sqrt(((dx - r) * (dx - r) + (dy - r) * (dy - r)).toFloat())
                if (distFromCenter > r) continue

                val photoPixel = if (bitmapPixels != null && dx < (endX.coerceAtMost((bitmapForSample?.width ?: 0) - 1) - startX + 1)
                    && dy < (endY.coerceAtMost((bitmapForSample?.height ?: 0) - 1) - startY + 1)) {
                    val bw = endX.coerceAtMost((bitmapForSample?.width ?: 1) - 1) - startX + 1
                    bitmapPixels[dy * bw + dx]
                } else sourceColor

                if (colorDistance(photoPixel, sourceColor) <= threshold) {
                    maskPixels[dy * w + dx] = 0x00000000.toInt()  // hapus
                }
            }
        }

        mask.setPixels(maskPixels, 0, w, startX, startY, w, h)
    }

    /**
     * Flood fill: buat mask TRANSPARAN pada seluruh area tersambung dalam foto yang
     * warnanya mirip (jarak warna ≤ threshold) dengan warna di titik seed [seedX]/[seedY].
     * Terbatas pada foto ImageLayer agar sampling warna akurat; area lain tidak diproses.
     */
    private fun floodFillEraseBitmap(
        mask: android.graphics.Bitmap,
        bitmap: android.graphics.Bitmap?,
        seedX: Int,
        seedY: Int,
        seedColor: Int
    ) {
        val bmp = bitmap ?: return
        val bW = bmp.width
        val bH = bmp.height
        val mW = mask.width
        val mH = mask.height
        if (bW <= 0 || bH <= 0 || mW <= 0 || mH <= 0) return
        if (seedX < 0 || seedY < 0 || seedX >= bW || seedY >= bH) return

        // Keamanan memori: flood fill penuh hanya untuk bitmap berukuran wajar.
        // Bitmap sangat besar cukup memakai stamp brush per goresan.
        val total = bW.toLong() * bH.toLong()
        if (total > FLOOD_FILL_MAX_PIXELS) return

        val threshold = maskPaintBrush.autoEraseThreshold
        val w = minOf(bW, mW)
        val h = minOf(bH, mH)
        val cnt = w * h

        val photoPixels = IntArray(cnt)
        val maskPixels = IntArray(cnt)
        bmp.getPixels(photoPixels, 0, w, 0, 0, w, h)
        mask.getPixels(maskPixels, 0, w, 0, 0, w, h)

        val visited = BooleanArray(cnt)
        val queue = IntArray(cnt)
        var head = 0
        var tail = 0
        val seedIdx = seedY * w + seedX
        visited[seedIdx] = true
        queue[tail++] = seedIdx
        var erased = 0

        while (head < tail) {
            val idx = queue[head++]
            maskPixels[idx] = 0x00000000.toInt()  // transparan = terhapus
            erased++
            val x = idx % w
            val y = idx / w

            if (x > 0) {
                val ni = idx - 1
                if (!visited[ni] && colorDistance(photoPixels[ni], seedColor) <= threshold) {
                    visited[ni] = true; queue[tail++] = ni
                }
            }
            if (x + 1 < w) {
                val ni = idx + 1
                if (!visited[ni] && colorDistance(photoPixels[ni], seedColor) <= threshold) {
                    visited[ni] = true; queue[tail++] = ni
                }
            }
            if (y > 0) {
                val ni = idx - w
                if (!visited[ni] && colorDistance(photoPixels[ni], seedColor) <= threshold) {
                    visited[ni] = true; queue[tail++] = ni
                }
            }
            if (y + 1 < h) {
                val ni = idx + w
                if (!visited[ni] && colorDistance(photoPixels[ni], seedColor) <= threshold) {
                    visited[ni] = true; queue[tail++] = ni
                }
            }
        }

        if (erased > 0) {
            mask.setPixels(maskPixels, 0, w, 0, 0, w, h)
        }
    }


    /**
     * Calculate color distance between two ARGB colors using CIE Delta E (simplified).
     * Returns value 0..255 representing perceptual distance.
     */
    private fun colorDistance(color1: Int, color2: Int): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        // Simplified Euclidean RGB distance
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2

        val dist = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
        return dist.toInt().coerceIn(0, 255)
    }

    /**
     * Phase 4: Clone Stamp — sample pixels from source region and paint to target.
     * 
     * Usage:
     * 1. Hold Shift + tap to set clone source point (cloneSourceX/Y)
     * 2. Drag normally to paint (samples from source region relative to initial offset)
     * 3. Uses PorterDuff.Mode.DARKEN for realistic blending
     * 
     * Algorithm:
     * - For each pixel in brush radius at (px, py):
     *   - Calculate offset from clone source: (offsetX, offsetY)
     *   - Sample pixel from layer bitmap at (cloneSourceX + offsetX, cloneSourceY + offsetY)
     *   - Blend pixel onto target bitmap using DARKEN mode
     *   - Update mask to keep pixel visible (opaque)
     */
    private fun cloneStampAt(
        layer: com.flyerpix.editor.canvas.model.CanvasLayer,
        bitmap: android.graphics.Bitmap,
        px: Int,
        py: Int,
        brushRadius: Int
    ) {
        // If no clone source set, cannot proceed
        if (maskPaintBrush.cloneSourceX == null || maskPaintBrush.cloneSourceY == null) {
            return
        }

        val sourceX = maskPaintBrush.cloneSourceX!!.toInt()
        val sourceY = maskPaintBrush.cloneSourceY!!.toInt()
        
        // For ImageLayer: clone from bitmap
        val sourceLayer = if (layer is com.flyerpix.editor.canvas.model.ImageLayer) {
            layer
        } else {
            return  // Clone only works on image layers for now
        }

        val sourceBitmap = sourceLayer.bitmap
        val effectiveOpacity = maskPaintBrush.brushOpacity.toFloat() / 255f

        // Paint circular brush area with cloned pixels
        for (dy in -brushRadius..brushRadius) {
            for (dx in -brushRadius..brushRadius) {
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                if (dist > brushRadius) continue

                val targetX = (px + dx).coerceIn(0, bitmap.width - 1)
                val targetY = (py + dy).coerceIn(0, bitmap.height - 1)
                
                // Calculate source position with offset
                val sampleX = (sourceX + dx).coerceIn(0, sourceBitmap.width - 1)
                val sampleY = (sourceY + dy).coerceIn(0, sourceBitmap.height - 1)

                // Sample pixel from source
                val sourcePixel = sourceBitmap.getPixel(sampleX, sampleY)
                
                // Get current target pixel
                val targetPixel = bitmap.getPixel(targetX, targetY)

                // Blend pixels using DARKEN mode (keep darker of two colors)
                // This gives realistic cloning by preserving shadows
                val sr = (sourcePixel shr 16) and 0xFF
                val sg = (sourcePixel shr 8) and 0xFF
                val sb = sourcePixel and 0xFF
                val sa = (sourcePixel shr 24) and 0xFF

                val tr = (targetPixel shr 16) and 0xFF
                val tg = (targetPixel shr 8) and 0xFF
                val tb = targetPixel and 0xFF
                val ta = (targetPixel shr 24) and 0xFF

                // DARKEN: use minimum of each channel
                val blendedR = kotlin.math.min(sr, tr)
                val blendedG = kotlin.math.min(sg, tg)
                val blendedB = kotlin.math.min(sb, tb)
                
                // Apply opacity to alpha channel
                val blendedA = ((sa + ta) / 2f * effectiveOpacity).toInt().coerceIn(0, 255)

                val blendedPixel = (blendedA shl 24) or (blendedR shl 16) or (blendedG shl 8) or blendedB
                bitmap.setPixel(targetX, targetY, blendedPixel)
            }
        }

        // Update mask to mark these pixels as visible (opaque)
        val mask = layer.maskBitmap ?: return
        for (dy in -brushRadius..brushRadius) {
            for (dx in -brushRadius..brushRadius) {
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                if (dist > brushRadius) continue

                val mx = (px + dx).coerceIn(0, mask.width - 1)
                val my = (py + dy).coerceIn(0, mask.height - 1)

                // Keep pixel visible (white in mask = opaque)
                val maskPixel = 0xFFFFFFFF.toInt()
                mask.setPixel(mx, my, maskPixel)
            }
        }
    }

    /** Menanggalkan transformasi zoom/pan & memetakan titik kanvas ke ruang lokal mask. */
    private fun canvasToMaskLocal(layer: com.flyerpix.editor.canvas.model.CanvasLayer, px: Float, py: Float): Pair<Float, Float> {
        val (wRaw, hRaw) = layer.getUnwarpedDimensions()
        val w = if (wRaw > 0f) wRaw else 1f
        val h = if (hRaw > 0f) hRaw else 1f
        val cx = w / 2f
        val cy = h / 2f
        val m = android.graphics.Matrix()
        m.postTranslate(-layer.x, -layer.y)
        m.postRotate(-layer.rotation, cx, cy)
        m.postScale(1f / layer.scale, 1f / layer.scale, cx, cy)
        val pts = floatArrayOf(px, py)
        m.mapPoints(pts)
        return pts[0] to pts[1]
    }

    /** Apakah mode gambar bebas aktif: semua sentuhan di kanvas menjadi goresan pena. */
    var freeDrawEnabled: Boolean = false

    /** Apakah mode input titik Bézier aktif: setiap tap menambah poin baru ke layer draft. */
    var bezierInputEnabled: Boolean = false

    /** Layer Bézier yang sedang menerima input titik. */
    var bezierInputLayer: PenLayer? = null

    /** Callback saat jumlah titik input Bézier berubah untuk sinkronisasi UI panel. */
    var onBezierInputPointChanged: ((Int) -> Unit)? = null

    /** Callback saat goresan pertama dimulai (buat menyinkronkan UI panel). */
    var onFreeDrawStart: (() -> Unit)? = null

    /** Sedang menggambar bebas (antara ACTION_DOWN dan ACTION_UP). */
    private var freeDrawActive = false

    /** Titik-titik goresan aktif dalam koordinat kanvas. */
    private val freeDrawPoints = ArrayList<Pair<Float, Float>>()

    /** Paint goresan bebas (live preview), konsisten dengan warna default PenLayer. */
    private val freeDrawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
        color = 0xFF1769FF.toInt()
    }

    // ── Selection Tools (Prompt 10) ──────────────────────────────────────────
    enum class SelectionTool { RECT, ELLIPSE, LASSO }

    /** Tool seleksi yang sedang aktif; null = tidak ada mode seleksi. */
    var selectionTool: SelectionTool? = null

    /** Jalur seleksi terakhir (kanvas space) untuk overlay & rasterize. */
    var selectionPath: android.graphics.Path? = null

    /** Callback saat seleksi berubah (finish DANDING start) untuk sinkronisasi UI. */
    var onSelectionChanged: (() -> Unit)? = null

    private var selectionDragActive = false
    private var selectionStartX = 0f
    private var selectionStartY = 0f
    private var selectionCurrentX = 0f
    private var selectionCurrentY = 0f
    private val selectionLassoPoints = ArrayList<Pair<Float, Float>>()

    /** Begin mode seleksi baru. */
    fun beginSelection(tool: SelectionTool) {
        selectionTool = tool
        selectionPath = null
        selectionDragActive = false
        selectionLassoPoints.clear()
        invalidate()
    }

    /** Batalkan seleksi & matikan mode seleksi. */
    fun clearSelection() {
        selectionTool = null
        selectionPath = null
        selectionDragActive = false
        selectionLassoPoints.clear()
        invalidate()
    }

    /** Bangun Path saat ini (rect/ellipse/lasso) dalam koordinat kanvas. */
    private fun rebuildSelectionPath() {
        selectionPath = buildSelectionPath()
    }

    private fun buildSelectionPath(): android.graphics.Path? {
        val tool = selectionTool ?: return null
        return when (tool) {
            SelectionTool.RECT, SelectionTool.ELLIPSE -> {
                val left = minOf(selectionStartX, selectionCurrentX)
                val top = minOf(selectionStartY, selectionCurrentY)
                val right = maxOf(selectionStartX, selectionCurrentX)
                val bottom = maxOf(selectionStartY, selectionCurrentY)
                if (right - left < 1f && bottom - top < 1f) return null
                val rect = android.graphics.RectF(left, top, right, bottom)
                android.graphics.Path().apply {
                    if (tool == SelectionTool.ELLIPSE) addOval(rect, android.graphics.Path.Direction.CW)
                    else addRect(rect, android.graphics.Path.Direction.CW)
                }
            }
            SelectionTool.LASSO -> {
                if (selectionLassoPoints.size < 3) return null
                android.graphics.Path().apply {
                    moveTo(selectionLassoPoints[0].first, selectionLassoPoints[0].second)
                    selectionLassoPoints.drop(1).forEach { lineTo(it.first, it.second) }
                    close()
                }
            }
        }
    }

    /**
     * Rasterize seleksi menjadi layer mask (Prompt 10): putih = terlihat, hitam = tersembunyi,
     * lalu gabung/overwrite ke maskBitmap layer terpilih + opsional feather + inversi.
     */
    fun rasterizeSelectionToMask(feather: Int = 0, inverted: Boolean = false): Boolean {
        android.util.Log.d("FlyerPixMask", "rasterize:: selPath=${selectionPath != null} layer=${selectedLayer?.javaClass?.simpleName} locked=${selectedLayer?.isLocked} selTool=${selectionTool}")
        val selection = selectionPath ?: return false
        val layer = selectedLayer ?: return false
        if (layer.isLocked) return false
        val (wRaw, hRaw) = layer.getUnwarpedDimensions()
        val w = if (wRaw > 0f) wRaw.toInt() else 0
        val h = if (hRaw > 0f) hRaw.toInt() else 0
        if (w <= 0 || h <= 0) return false

        if (layer.maskBitmap == null) layer.createMask(w, h)
        val mask = layer.maskBitmap ?: return false

        // Inverse transform kanvas → koordinat layer lokal (konsisten dgn canvasToMaskLocal)
        val m = android.graphics.Matrix()
        m.postTranslate(-layer.x, -layer.y)
        m.postRotate(-layer.rotation, w / 2f, h / 2f)
        m.postScale(1f / layer.scale, 1f / layer.scale, w / 2f, h / 2f)
        val localPath = android.graphics.Path(selection)
        localPath.transform(m)

        val clearPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.FILL
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }

        if (inverted) {
            // Inverted: selection area VISIBLE, rest TRANSPARENT
            mask.eraseColor(android.graphics.Color.TRANSPARENT)
            val bc = android.graphics.Canvas(mask)
            bc.drawPath(localPath, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
                color = android.graphics.Color.WHITE
            })
        } else {
            // Non-inverted (default): selection area TRANSPARENT, rest VISIBLE
            mask.eraseColor(android.graphics.Color.WHITE)
            val bc = android.graphics.Canvas(mask)
            bc.drawPath(localPath, clearPaint)
        }

        if (feather > 0) com.flyerpix.editor.canvas.model.MaskUtils.featherMask(mask, feather)
        layer.maskEnabled = true
        layer.maskInverted = inverted
        layer.maskGeneration++
        invalidate()
        notifyLayersChanged()
        return true
    }

    var freeDrawColor: Int
        get() = freeDrawPaint.color
        set(value) { freeDrawPaint.color = value }

    var freeDrawStrokeWidth: Float
        get() = freeDrawPaint.strokeWidth
        set(value) { freeDrawPaint.strokeWidth = value }

    // ── Bezier Edit Mode (Phase 1: Anchor Editor) ──────────────────────────
    /** Apakah Bezier edit mode sedang aktif (user sedang mengedit anchor path). */
    private var bezierEditMode = false

    /** Layer PenLayer yang sedang diedit dalam Bezier edit mode. */
    private var selectedBezierLayer: PenLayer? = null

    /** Indeks anchor yang saat ini dipilih (-1 jika tidak ada). */
    private var selectedAnchorIndex: Int = -1

    /** Jenis hit yang sedang di-drag: HandleIn, HandleOut, atau null (drag anchor). */
    private var selectedHandleType: HitAnchor? = null

    /** Paint untuk visualisasi anchor point. */
    private val anchorPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF18C8F5.toInt()  // Cyan
        style = Paint.Style.FILL
    }

    /** Paint untuk anchor point yang dipilih. */
    private val selectedAnchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFEB3B.toInt()  // Yellow
        style = Paint.Style.FILL
    }

    /** Paint untuk garis handle. */
    private val handleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8818C8F5.toInt()  // Cyan with alpha
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }

    /** Paint untuk handle control point. */
    private val handlePointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF18C8F5.toInt()  // Cyan
        style = Paint.Style.FILL
    }

    /** Callback ketika anchor dipilih. */
    var onBezierAnchorSelected: ((Int) -> Unit)? = null

    /** Callback ketika anchor tidak ada yang dipilih. */
    var onBezierAnchorDeselected: (() -> Unit)? = null

    /** Callback ketika anchor bergerak/berubah. */
    var onBezierAnchorChanged: ((Int) -> Unit)? = null

    /** Bitmap hasil capture kanvas untuk pembacaan pixel. */
    private var capturedBitmap: Bitmap? = null

    /**
     * Pipeline blur native worker:
     * - UI thread merender snapshot konten ke [nativeBlurBitmap] (render ringan ¼ res).
     * - Worker thread menjalankan getPixels → blur → setPixels → publish
     *   [nativeBlurResult], lalu [postInvalidateOnAnimation].
     * - Saat konten/radius/ukuran tidak berubah ([nativeBlurResultFp] sama)
     *   skema "cache hit": blur block hanya `drawBitmap` hasil cached.
     */
    private var nativeBlurBitmap: Bitmap? = null
    private var nativeBlurPixels: IntArray? = null
    private var nativeBlurResult: Bitmap? = null
    private var nativeBlurResultFp: Int = Int.MIN_VALUE
    private val blurOutBuffer: Array<Bitmap?> = arrayOfNulls(2)
    private var blurOutIndex = 0
    private val nativeBlurOverlayPaint by lazy {
        Paint().apply { alpha = 220; isFilterBitmap = true }
    }
    private val blurWorkerThread = HandlerThread("fp-blur", Process.THREAD_PRIORITY_DEFAULT)
        .apply { start() }
    private val blurWorker = Handler(blurWorkerThread.looper)

    /** Eksekutor untuk export/render off-UI-thread (tidak memblokir blur worker). */
    private val exportExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var blurRebuildPending = false

    /**
     * Snapshot blur yang menunggu recycle oleh worker. UI thread TIDAK boleh
     * me-recycle bitmap snapshot sementara build sedang berjalan (worker masih
     * memegang referensinya → `getPixels()` pada bitmap recycled = crash).
     * Saat resize terjadi dengan build in-flight, UI mendaftarkan bitmap lama di
     * sini dan [runBlurBuild] yang me-recycle setelah selesai memakainya.
     */
    private val blurStaleBitmaps = java.util.concurrent.ConcurrentLinkedDeque<Bitmap>()
    @Volatile
    private var blurRebuildFp: Int = 0
    @Volatile
    private var blurRebuildRadius: Int = 0

    /** Jumlah rebuild blur yang selesai (debug/stress). */
    @Volatile
    var blurRebuildCount: Int = 0
        private set

    /** Posisi sentuh X terakhir dalam koordinat kanvas (dibaca oleh overlay). */
    var touchEventX: Float = 0f
        private set

    /** Posisi sentuh Y terakhir dalam koordinat kanvas (dibaca oleh overlay). */
    var touchEventY: Float = 0f
        private set

    fun addBezierInputPoint(canvasX: Float, canvasY: Float): PenLayer? {
        val layer = bezierInputLayer ?: selectedLayer as? PenLayer ?: return null
        val localX = canvasX - layer.x
        val localY = canvasY - layer.y
        layer.addAnchor(localX, localY, AnchorType.CORNER)
        onBezierInputPointChanged?.invoke(layer.anchors.size)
        invalidate()
        return layer
    }

    /**
     * Menangani touch events untuk mode edit Bezier anchor.
     * User dapat menggeser anchor dan handle untuk memanipulasi path.
     */
    private fun handleBezierEditModeTouch(event: MotionEvent): Boolean {
        val pen = selectedBezierLayer ?: return false

        // Transform koordinat layar → koordinat lokal layer (accounting for zoom/pan)
        var touchX = event.x
        var touchY = event.y
        
        // If we're zoomed/panned, coordinates have already been transformed by canvasTransformInverse
        // Now convert from canvas space to layer space
        val localX = touchX - pen.x
        val localY = touchY - pen.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Hit-test: apakah user menyentuh anchor atau handle?
                val hit = pen.hitTest(localX, localY)
                when (hit) {
                    is HitAnchor.Anchor -> {
                        selectedAnchorIndex = hit.index
                        selectedHandleType = null
                        onBezierAnchorSelected?.invoke(hit.index)
                    }
                    is HitAnchor.HandleIn -> {
                        selectedAnchorIndex = hit.index
                        selectedHandleType = hit
                    }
                    is HitAnchor.HandleOut -> {
                        selectedAnchorIndex = hit.index
                        selectedHandleType = hit
                    }
                    HitAnchor.None -> {
                        // User tap area kosong → deselect
                        selectedAnchorIndex = -1
                        selectedHandleType = null
                        onBezierAnchorDeselected?.invoke()
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedAnchorIndex >= 0) {
                    when (selectedHandleType) {
                        is HitAnchor.HandleIn -> {
                            pen.moveHandleIn(selectedAnchorIndex, localX, localY)
                        }
                        is HitAnchor.HandleOut -> {
                            pen.moveHandleOut(selectedAnchorIndex, localX, localY)
                        }
                        else -> {
                            // Moving anchor itself
                            pen.moveAnchor(selectedAnchorIndex, localX, localY)
                        }
                    }
                    onBezierAnchorChanged?.invoke(selectedAnchorIndex)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                selectedHandleType = null
                invalidate()
            }
        }
        return true
    }

    /** Callback yang dipanggil saat user memilih warna dari kanvas. */
    var onEyedropperColorListener: ((Int) -> Unit)? = null

    /**
     * Menangkap seluruh konten kanvas (background + semua layer) menjadi Bitmap.
     * Bitmap ini digunakan oleh mode eyedropper untuk membaca warna pixel.
     */
    private fun captureCanvasToBitmap(): Bitmap {
        val w = if (width > 0) width else 1
        val h = if (height > 0) height else 1
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val offscreen = Canvas(bmp)

        val vp = if (viewportRect.spanX > 0 && viewportRect.spanY > 0) viewportRect else RectF().apply {
            left = 0f
            top = 0f
            right = w.toFloat()
            bottom = h.toFloat()
        }

        // Gambar background independen (Prompt 44)
        drawBackgroundOnCanvas(offscreen, vp)

        // Gambar grid jika aktif
        if (isGridEnabled) {
            drawGridGuidelines(offscreen, vp)
        }

        // Gambar semua layer
        for (layer in layers) {
            if (layer.isVisible) {
                if (layer.blendMode != PorterDuff.Mode.SRC_OVER || layer.blendExtra != null) {
                    renderPaint.applyLayerBlend(layer)
                    val saveCount = offscreen.saveLayer(null, renderPaint)
                    sanitizeSharedPaint()
                    layer.draw(offscreen, renderPaint)
                    sanitizeSharedPaint()
                    offscreen.restoreToCount(saveCount)
                    renderPaint.clearBlend()
                } else {
                    renderPaint.clearBlend()
                    val saveCount = offscreen.save()
                    sanitizeSharedPaint()
                    layer.draw(offscreen, renderPaint)
                    sanitizeSharedPaint()
                    offscreen.restoreToCount(saveCount)
                }
            }
        }
        return bmp
    }

    /**
     * Membaca warna pixel dari bitmap kanvas yang di-capture pada koordinat tertentu.
     * @return warna pixel, atau [Color.TRANSPARENT] jika bitmap tidak tersedia.
     */
    fun getPixelColorAt(canvasX: Float, canvasY: Float): Int {
        val bmp = capturedBitmap ?: return Color.TRANSPARENT
        val x = canvasX.toInt().coerceIn(0, bmp.width - 1)
        val y = canvasY.toInt().coerceIn(0, bmp.height - 1)
        return bmp.getPixel(x, y)
    }

    /**
     * Menangkap ulang bitmap kanvas (misal setelah layer berubah).
     * Harus dipanggil dari luar jika konten kanvas berubah saat eyedropper aktif.
     */
    fun refreshCapturedBitmap() {
        if (isEyedropperMode) {
            capturedBitmap?.recycle()
            capturedBitmap = captureCanvasToBitmap()
        }
    }

    /**
     * Indeks sudut handle perspektif yang sedang disentuh/didrag (-1 jika tidak ada).
     * 0: Top-Left, 1: Top-Right, 2: Bottom-Right, 3: Bottom-Left.
     */
    private var activePerspectiveCorner: Int = -1

    // ── Paint untuk Bounding Box Seleksi (Prompt 25) ────────────────────────
    private val selectionBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF18C8F5.toInt() // Cyan khas PixelLab
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }

    private val textSelectionFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x6618C8F5
        style = Paint.Style.FILL
    }

    private val textEditSelectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x801769FF.toInt()
        style = Paint.Style.FILL
    }

    private val selectionOuterBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val textEditBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1769FF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textEditLabelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1769FF.toInt()
        style = Paint.Style.FILL
    }

    private val textEditLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 11f * resources.displayMetrics.density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    // ── Paint untuk 4 Handle Sudut Bounding Box (Prompt 26) ─────────────────
    private val handleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00B0FF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // ── Paint untuk Handle & Garis Pandu Perspektif ─────────────────────────
    private val perspectiveGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF18C8F5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val perspectiveHandleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val perspectiveHandleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00B0FF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val perspectiveHandleCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF18C8F5.toInt()
        style = Paint.Style.FILL
    }

    private val perspectiveHandleActiveCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4081.toInt()
        style = Paint.Style.FILL
    }

    // ── Paint & Pengaturan Garis Pandu Magnetik Snap & Grid (Prompt 30) ─────
    var isSnapToCenterEnabled: Boolean = true

    var isSnapGuideXVisible: Boolean = false
        internal set

    var isSnapGuideYVisible: Boolean = false
        internal set

    private var snapGuideXPosition: Float? = null
    private var snapGuideYPosition: Float? = null

    var isGridEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var gridSpacingDp: Float = 32f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val snapGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF18C8F5.toInt() // Biru cyan magnetik khas PixelLab
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private fun clearSnapGuides() {
        isSnapGuideXVisible = false
        isSnapGuideYVisible = false
        snapGuideXPosition = null
        snapGuideYPosition = null
    }

    /**
     * Mengumpulkan koordinat target magnet dari layer lain (peer snapping): tepi
     * kiri/tengah/kanan untuk sumbu X dan tepi atas/tengah/bawah untuk sumbu Y.
     * Layer yang sedang digeser ([exclude]) dilewati agar tidak menempel ke dirinya
     * sendiri.
     */
    private fun collectSnapPeerTargets(exclude: CanvasLayer): Pair<FloatArray, FloatArray> {
        val xTargets = ArrayList<Float>()
        val yTargets = ArrayList<Float>()
        var count = 0
        for (other in layers) {
            if (other === exclude || !other.isVisible) continue
            val b = other.getBounds()
            if (b.width() <= 0f || b.height() <= 0f) continue
            xTargets.add(b.left)
            xTargets.add(b.centerX())
            xTargets.add(b.right)
            yTargets.add(b.top)
            yTargets.add(b.centerY())
            yTargets.add(b.bottom)
            count++
            if (count >= MAX_SNAP_PEER_LAYERS) break
        }
        return xTargets.toFloatArray() to yTargets.toFloatArray()
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x2600E5FF.toInt() // Biru cyan transparan lembut untuk kisi grid
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    init {
        initCheckerboardPattern()
    }

    /**
     * Menginisialisasi pola kotak-kotak catur abu-abu putih (checkerboard pattern)
     * berukuran proporsional layar menggunakan BitmapShader yang hemat memori dan terakselerasi GPU.
     */
    private fun initCheckerboardPattern() {
        val squareSize = (12 * resources.displayMetrics.density).toInt().coerceAtLeast(16)
        val tileSize = squareSize * 2
        val tileBitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val tileCanvas = Canvas(tileBitmap)

        val lightPaint = Paint().apply { color = Color.WHITE }
        val darkPaint = Paint().apply { color = Color.rgb(224, 224, 224) } // Abu-abu terang (#E0E0E0)

        // Kotak kiri atas (putih)
        tileCanvas.drawRect(0f, 0f, squareSize.toFloat(), squareSize.toFloat(), lightPaint)
        // Kotak kanan atas (abu-abu)
        tileCanvas.drawRect(squareSize.toFloat(), 0f, tileSize.toFloat(), squareSize.toFloat(), darkPaint)
        // Kotak kiri bawah (abu-abu)
        tileCanvas.drawRect(0f, squareSize.toFloat(), squareSize.toFloat(), tileSize.toFloat(), darkPaint)
        // Kotak kanan bawah (putih)
        tileCanvas.drawRect(squareSize.toFloat(), squareSize.toFloat(), tileSize.toFloat(), tileSize.toFloat(), lightPaint)

        checkerboardPaint.shader = BitmapShader(tileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewport()
        clampCanvasPan()
    }

    /**
     * Merender latar belakang kanvas independen (transparan, warna solid, atau gradasi) ke dalam area [vp] (Prompt 44).
     * 
     * Ketika background di-hide ([isCanvasBackgroundVisible] = false), secara otomatis menampilkan
     * checkerboard pattern untuk menunjukkan area transparan (konsisten dengan UX standar editor grafis).
     */
    fun drawBackgroundOnCanvas(canvas: Canvas, vp: RectF) {
        if (!isCanvasBackgroundVisible) {
            // Tampilkan checkerboard pattern saat background di-hide
            canvas.drawRect(vp, checkerboardPaint)
            return
        }
        when (canvasBackground.mode) {
            CanvasBackgroundMode.TRANSPARENT -> {
                canvas.drawRect(vp, checkerboardPaint)
            }
            CanvasBackgroundMode.SOLID_COLOR -> {
                canvasBgPaint.shader = null
                canvasBgPaint.color = canvasBackground.solidColor
                canvas.drawRect(vp, canvasBgPaint)
            }
            CanvasBackgroundMode.GRADIENT -> {
                val grad = canvasBackground.gradient
                if (grad != null) {
                    canvasBgPaint.shader = grad.createShader(vp)
                } else {
                    canvasBgPaint.shader = null
                    canvasBgPaint.color = canvasBackground.solidColor
                }
                canvas.drawRect(vp, canvasBgPaint)
            }
            CanvasBackgroundMode.IMAGE -> {
                val bmp = canvasBackground.imageBitmap
                if (bmp != null && !bmp.isRecycled) {
                    canvasBgPaint.shader = null
                    val srcAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    val dstAspect = vp.width() / vp.height()
                    val src: android.graphics.Rect
                    val dst: android.graphics.RectF
                    if (srcAspect > dstAspect) {
                        // Gambar lebih lebar → crop sisi kiri-kanan (fill height)
                        val visibleW = (bmp.height * dstAspect).toInt()
                        val offsetX = (bmp.width - visibleW) / 2
                        src = android.graphics.Rect(offsetX, 0, offsetX + visibleW, bmp.height)
                        dst = vp
                    } else {
                        // Gambar lebih tinggi → crop sisi atas-bawah (fill width)
                        val visibleH = (bmp.width / dstAspect).toInt()
                        val offsetY = (bmp.height - visibleH) / 2
                        src = android.graphics.Rect(0, offsetY, bmp.width, offsetY + visibleH)
                        dst = vp
                    }
                    canvas.drawBitmap(bmp, src, dst, canvasBgPaint)
                } else {
                    canvasBgPaint.shader = null
                    canvasBgPaint.color = canvasBackground.solidColor
                    canvas.drawRect(vp, canvasBgPaint)
                }
            }
        }
    }

    /**
     * Overlay blur berbasis native blur (NDK) dengan pipeline async + cache:
     * 1. Fingerprint konten+radius+ukuran → cache hit: hanya `drawBitmap` hasil
     *    publish lalu selesai (kondisi statis = biaya nyaris nol).
     * 2. Cache miss: UI thread merender snapshot konten ¼ resolusi ke
     *    [nativeBlurBitmap], worker thread menjalankan get/blur/set, hasil
     *    di-publish sebagai [nativeBlurResult] + [postInvalidateOnAnimation].
     * 3. Selama rebuild berjalan, frame saat ini menggambar hasil publish lama
     *    (lag satu frame pada blur tidak terlihat).
     */
    private fun drawNativeBlurOverlay(canvas: Canvas, vp: RectF, blurRadius: Float) {
        val w = vp.width().toInt()
        val h = vp.height().toInt()
        if (w <= 0 || h <= 0) return

        val bw = (w / 4).coerceAtLeast(1)
        val bh = (h / 4).coerceAtLeast(1)

        var bmp = nativeBlurBitmap
        if (bmp == null || bmp.width != bw || bmp.height != bh) {
            if (blurRebuildPending) {
                // Build sedang berjalan dan masih memegang bitmap lama → jangan
                // recycle sekarang, biarkan worker yang me-recycle (lihat
                // runBlurBuild). Memaksa recycle di sini = crash getPixels.
                if (nativeBlurBitmap != null) blurStaleBitmaps.offer(nativeBlurBitmap)
            } else {
                nativeBlurBitmap?.recycle()
            }
            bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            nativeBlurBitmap = bmp
            nativeBlurPixels = IntArray(bw * bh)
        }
        val radius = ((blurRadius * 2f) / 4f).toInt().coerceAtLeast(1)
        val result = nativeBlurResult
        val fp = computeBlurSignature(bw, bh, radius)

        // Cache hit: konten tidak berubah → hanya gambar hasil yang sudah publish.
        if (result != null && result.width == bw && result.height == bh && fp == nativeBlurResultFp) {
            canvas.drawBitmap(result, null, vp, nativeBlurOverlayPaint)
            return
        }

        // Cache miss → minta rebuild async (coalesce bila ada build berjalan).
        blurRebuildFp = fp
        blurRebuildRadius = radius
        if (!blurRebuildPending) {
            blurRebuildPending = true
            blurWorker.post { runBlurBuild() }
        }

        // Sambil menunggu build baru, gambar hasil publish lama (bila ada).
        if (result != null && result.width == bw && result.height == bh) {
            canvas.drawBitmap(result, null, vp, nativeBlurOverlayPaint)
        }
    }

    /**
     * Menghitung fingerprint ringkas konten canvas yang memengaruhi hasil blur:
     * dimensi snapshot, radius, latar belakang, dan setiap layer terlihat.
     */
    private fun computeBlurSignature(bw: Int, bh: Int, radius: Int): Int {
        var h = bw
        h = h * 31 + bh
        h = h * 31 + radius
        val bg = canvasBackground
        h = h * 31 + bg.mode.ordinal
        h = h * 31 + bg.solidColor
        bg.gradient?.let { h = h * 31 + it.hashCode() }
        bg.imageBitmap?.let { bmp ->
            h = h * 31 + System.identityHashCode(bmp)
            h = h * 31 + bmp.generationId
            h = h * 31 + bmp.width
            h = h * 31 + bmp.height
        }
        // Overlay blur membawa warna adjustment, jadi nilainya ikut jadi trigger.
        h = h * 31 + (adjustments[CanvasAdjustment.BRIGHTNESS] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.CONTRAST] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.SATURATION] ?: 0f).toRawBits()
        // Parameter extended (Prompt 02) ikut dibakar ke overlay blur.
        h = h * 31 + (adjustments[CanvasAdjustment.HUE] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.EXPOSURE] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.HIGHLIGHTS] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.SHADOWS] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.TEMPERATURE] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.TINT] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.GAMMA] ?: 0f).toRawBits()
        h = h * 31 + (adjustments[CanvasAdjustment.VIBRANCE] ?: 0f).toRawBits()
        // Noise & vignette ikut dibakar ke overlay saat blur aktif.
        h = h * 31 + (if (isEffectEnabled(CanvasEffect.NOISE)) 1 else 0)
        h = h * 31 + (if (isEffectEnabled(CanvasEffect.VIGNETTE)) 1 else 0)
        for (layer in layers) {
            if (layer.isVisible) h = h * 31 + layer.contentBlurSignature()
        }
        return h
    }

    /** Merender snapshot konten ke [nativeBlurBitmap] (di panggil dari UI thread). */
    private fun renderBlurSnapshot(w: Int, h: Int, vp: RectF) {
        val bitmap = nativeBlurBitmap ?: return
        val bw = bitmap.width
        val bh = bitmap.height
        val scaleX = bw.toFloat() / w
        val scaleY = bh.toFloat() / h
        val off = Canvas(bitmap)
        off.scale(scaleX, scaleY)
        off.translate(-vp.left, -vp.top)
        drawBackgroundOnCanvas(off, RectF(0f, 0f, w.toFloat(), h.toFloat()))
        for (layer in layers) {
            if (layer.isVisible) {
                sanitizeSharedPaint()
                layer.draw(off, renderPaint)
                sanitizeSharedPaint()
            }
        }
    }

    /** Body worker: render snapshot di UI thread → blur native → publish. */
    private fun runBlurBuild() {
        try {
            while (true) {
                val bmp = nativeBlurBitmap
                val pixels = nativeBlurPixels
                if (bmp == null || pixels == null) break
                val bw = bmp.width
                val bh = bmp.height
                val fp = blurRebuildFp
                val radius = blurRebuildRadius

                // Render snapshot di UI thread; tunggu selesai agar pixel stabil.
                val latch = CountDownLatch(1)
                val posted = Handler(Looper.getMainLooper()).post {
                    renderBlurSnapshot(width, height, viewportRectOrFull())
                    latch.countDown()
                }
                if (!posted) { blurRebuildPending = false; return }
                latch.await()

                // UI thread bisa mengganti+men-recycle snapshot saat kita menunggu
                // render (resize kanvas saat panel terbuka). Lewati iterasi: loop
                // mengambil pasangan bitmap yang baru dan merender ulang.
                if (bmp.isRecycled) continue

                val t0 = System.nanoTime()
                bmp.getPixels(pixels, 0, bw, 0, 0, bw, bh)
                runCatching { FpNative.blurPixels(pixels, bw, bh, radius) }
                val t1 = System.nanoTime()
                // Adjustment extended (Prompt 02): satu pipeline yang sama dengan
                // jalur preview, dibakar langsung ke overlay blur.
                val adjParams = currentAdjustmentParams()
                if (adjParams.isActive) {
                    runCatching {
                        FilterEngine.applyColorAdjustPixels(
                            pixels, bw, bh,
                            adjParams.brightness, adjParams.contrast, adjParams.saturation, adjParams.hue,
                            adjParams.exposure, adjParams.highlights, adjParams.shadows,
                            adjParams.temperature, adjParams.tint, adjParams.gamma, adjParams.vibrance
                        )
                    }
                }
                val noiseAlpha = if (isEffectEnabled(CanvasEffect.NOISE)) NOISE_OVERLAY_ALPHA else 0
                val vignette = isEffectEnabled(CanvasEffect.VIGNETTE)
                if (noiseAlpha != 0 || vignette) {
                    runCatching {
                        FpNative.applyNoiseVignette(pixels, bw, bh, noiseAlpha, NOISE_SEED, vignette)
                    }
                }
                val t2 = System.nanoTime()
                val out = blurOutBufferFor(bw, bh)
                out.setPixels(pixels, 0, bw, 0, 0, bw, bh)
                val t3 = System.nanoTime()
                blurRebuildCount++
                if (profileEnabled) {
                    android.util.Log.d(
                        PROFILE_TAG,
                        "blurBuild get+blur=${(t1 - t0) / 1_000_000.0}ms adjust=${(t2 - t1) / 1_000_000.0}ms" +
                            " set=${(t3 - t2) / 1_000_000.0}ms total=${(t3 - t0) / 1_000_000.0}ms" +
                            " size=${bw}x${bh} r=$radius rebuilds=$blurRebuildCount"
                    )
                }

                nativeBlurResult = out
                nativeBlurResultFp = fp
                postInvalidateOnAnimation()

                // Coalescing: bila selama build ada permintaan baru, rebuild lagi.
                if (blurRebuildFp == fp) {
                    blurRebuildPending = false
                    break
                }
            }
        } finally {
            // Snapshot lama yang di-defer saat resize (karena build in-flight)
            // baru aman di-recycle setelah build ini benar-benar selesai.
            while (true) {
                val stale = blurStaleBitmaps.poll() ?: break
                stale.recycle()
            }
        }
    }

    /** viewportRect, atau area penuh view bila belum dihitung. */
    private fun viewportRectOrFull(): RectF {
        if (viewportRect.spanX > 0 && viewportRect.spanY > 0) return viewportRect
        return RectF(0f, 0f, width.toFloat(), height.toFloat())
    }

    private fun canvasPanBounds(vp: RectF = viewportRectOrFull()): FloatArray {
        val cx = width / 2f
        val cy = height / 2f
        val transformedLeft = cx + (vp.left - cx) * canvasZoom
        val transformedRight = cx + (vp.right - cx) * canvasZoom
        val transformedTop = cy + (vp.top - cy) * canvasZoom
        val transformedBottom = cy + (vp.bottom - cy) * canvasZoom
        val horizontalRange = transformedRight - transformedLeft - width
        val verticalRange = transformedBottom - transformedTop - height

        val minPanX = if (horizontalRange > 0f) width - transformedRight else 0f
        val maxPanX = if (horizontalRange > 0f) -transformedLeft else 0f
        val minPanY = if (verticalRange > 0f) height - transformedBottom else 0f
        val maxPanY = if (verticalRange > 0f) -transformedTop else 0f
        return floatArrayOf(minPanX, maxPanX, minPanY, maxPanY)
    }

    private fun clampCanvasPan() {
        val bounds = canvasPanBounds()
        canvasPanX = canvasPanX.coerceIn(bounds[0], bounds[1])
        canvasPanY = canvasPanY.coerceIn(bounds[2], bounds[3])
    }

    /** Returns trackStart, trackEnd, thumbStart and thumbEnd for one scrollbar. */
    private fun zoomScrollbarMetrics(horizontal: Boolean): FloatArray? {
        if (!editorZoomMode || canvasZoom <= minCanvasZoom || width <= 0 || height <= 0) return null
        val vp = viewportRectOrFull()
        val bounds = canvasPanBounds(vp)
        val minPan = if (horizontal) bounds[0] else bounds[2]
        val maxPan = if (horizontal) bounds[1] else bounds[3]
        if (maxPan <= minPan) return null

        val density = resources.displayMetrics.density
        val inset = 8f * density
        val thickness = 4f * density
        val minimumThumb = 24f * density
        val trackStart = inset
        val trackEnd = (if (horizontal) width else height).toFloat() - inset - thickness
        val trackLength = trackEnd - trackStart
        val contentLength = if (horizontal) vp.width() * canvasZoom else vp.height() * canvasZoom
        val viewportLength = if (horizontal) width.toFloat() else height.toFloat()
        val thumbLength = (trackLength * (viewportLength / contentLength))
            .coerceIn(minimumThumb, trackLength)
        val progress = ((if (horizontal) canvasPanX else canvasPanY) - minPan) / (maxPan - minPan)
        val thumbStart = trackStart + (trackLength - thumbLength) * progress.coerceIn(0f, 1f)
        return floatArrayOf(trackStart, trackEnd, thumbStart, thumbStart + thumbLength)
    }

    private fun drawZoomScrollbars(canvas: Canvas) {
        val horizontal = zoomScrollbarMetrics(horizontal = true)
        val vertical = zoomScrollbarMetrics(horizontal = false)
        if (horizontal == null && vertical == null) return

        val density = resources.displayMetrics.density
        val inset = 8f * density
        val thickness = 4f * density
        zoomScrollbarPaint.color = Color.argb(72, 255, 255, 255)
        horizontal?.let {
            canvas.drawRoundRect(
                it[0], height - inset - thickness, it[1], height - inset,
                thickness, thickness, zoomScrollbarPaint
            )
        }
        vertical?.let {
            canvas.drawRoundRect(
                width - inset - thickness, it[0], width - inset, it[1],
                thickness, thickness, zoomScrollbarPaint
            )
        }

        zoomScrollbarPaint.color = Color.argb(220, 255, 255, 255)
        horizontal?.let {
            canvas.drawRoundRect(
                it[2], height - inset - thickness, it[3], height - inset,
                thickness, thickness, zoomScrollbarPaint
            )
        }
        vertical?.let {
            canvas.drawRoundRect(
                width - inset - thickness, it[2], width - inset, it[3],
                thickness, thickness, zoomScrollbarPaint
            )
        }
    }

    private fun moveFromZoomScrollbar(horizontal: Boolean, coordinate: Float, touchOffset: Float) {
        val metrics = zoomScrollbarMetrics(horizontal) ?: return
        val bounds = canvasPanBounds()
        val trackLength = metrics[1] - metrics[0]
        val thumbLength = metrics[3] - metrics[2]
        val progress = ((coordinate - touchOffset - metrics[0]) /
            (trackLength - thumbLength)).coerceIn(0f, 1f)
        if (horizontal) {
            canvasPanX = bounds[0] + (bounds[1] - bounds[0]) * progress
        } else {
            canvasPanY = bounds[2] + (bounds[3] - bounds[2]) * progress
        }
        clampCanvasPan()
        invalidate()
    }

    /**
     * Ambil buffer output blur dari double-buffer (buat saat pertama/ukuran
     * berubah), bergantian tiap rebuild untuk menghindari alokasi bitmap
     * per rebuild (penyebab spike GC).
     */
    private fun blurOutBufferFor(bw: Int, bh: Int): Bitmap {
        val stale = blurOutBuffer.any { it == null || it.width != bw || it.height != bh }
        if (stale) {
            blurOutBuffer.forEach { it?.recycle() }
            blurOutBuffer[0] = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            blurOutBuffer[1] = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            blurOutIndex = 0
        }
        val out = blurOutBuffer[blurOutIndex]!!
        blurOutIndex = blurOutIndex xor 1
        return out
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val vp = if (viewportRect.spanX > 0 && viewportRect.spanY > 0) viewportRect else RectF().apply {
            left = 0f
            top = 0f
            right = width.toFloat()
            bottom = height.toFloat()
        }

        val profiling = profileEnabled
        if (profiling) pfFrameStart = System.nanoTime()

        val cx = width / 2f
        val cy = height / 2f
        val drawSave = canvas.save()
        canvas.translate(cx + canvasPanX, cy + canvasPanY)
        canvas.scale(canvasZoom, canvasZoom)
        canvas.translate(-cx, -cy)

        try {
            // Adjustment extended (Prompt 02): render komposisi lewat pipeline
            // native extended (off-screen snapshot). Blur path ini jalur preview
            // saat blur = 0; bila ada parameter aktif selain blur, snapshot
            // dipakai dua-duanya sehingga hasil konsisten.
            val tAdjust = System.nanoTime()
            val colorParams = currentAdjustmentParams()
            if (colorParams.isActive) {
                drawAdjustedContent(canvas, vp, colorParams)
            } else {
                // Efek Filter (monokrom) dibungkus sebagai layer komposit di atas
                // background, grid, dan seluruh layer (Prompt 51).
                val tFilter = System.nanoTime()
                val filterEffectLayer = beginFilterEffectLayer(canvas)
                if (profiling) pfFilterMs += profileMark(tFilter)

                // Adjustment layer: brightness/contrast/saturation via ColorMatrix saveLayer
                val adjPaint = buildAdjustmentPaint()
                val adjSaveIndex = if (adjPaint != null) canvas.saveLayer(null, adjPaint) else -1

                // 1. Render background kanvas independen (Prompt 44)
                // 1b. Kisi grid penjajaran (Prompt 30)
                // 2. Seluruh layer sesuai z-index
                // 2b. Live preview free draw
                drawCompositionContent(canvas, vp)

                // Tutup layer komposit filter bila aktif (Prompt 51).
                endFilterEffectLayer(canvas, filterEffectLayer)

                // Tutup adjustment layer (brightness/contrast/saturation)
                if (adjSaveIndex >= 0) canvas.restoreToCount(adjSaveIndex)
            }
            if (profiling) pfAdjustMs += profileMark(tAdjust)

            // Blur overlay: snapshot konten, blur via native (NDK), gambar darinya
            val blurRadius = adjustments[CanvasAdjustment.BLUR] ?: 0f
            if (blurRadius > 0f) {
                val tBlur = System.nanoTime()
                drawNativeBlurOverlay(canvas, vp, blurRadius)
                if (profiling) pfBlurMs += profileMark(tBlur)
            }

            // 2b. Render efek overlay non-destruktif (Noise, Vignette) di atas konten (Prompt 51).
            // Saat blur aktif, noise+vignette sudah dibakar ke overlay blur (¼-res native),
            // jadi di sini dilewati agar tidak digambar dua kali.
            drawEffectsOverlay(canvas, vp, blurRadius > 0f)

            // 3. Render Bounding Box seleksi garis putus-putus jika ada layer aktif (Prompt 25, 34)
            selectedLayer?.let { layer ->
                if (!editorZoomMode && layer.isVisible && !layer.isLocked && !layer.perspectiveEnabled) {
                    drawSelectionBoundingBox(canvas, layer)
                }
            }

            // 4. Render handle interaktif perspektif 4 titik sudut jika layer aktif mengaktifkan perspektif
            selectedLayer?.let { layer ->
                if (!editorZoomMode && layer.isVisible && layer.perspectiveEnabled && !layer.isLocked) {
                    drawPerspectiveHandles(canvas, layer)
                }
            }

            // 4c. Overlay handle interaktif 3D Box: depth handle (seret kedalaman)
            selectedLayer?.let { layer ->
                if (!editorZoomMode && layer.isVisible && !layer.isLocked && layer is Box3DLayer) {
                    drawBox3DOverlay(canvas, layer)
                }
            }

            // 4b. Render anchor visualization untuk mode edit Bezier (Phase 1)
            if (bezierEditMode && selectedBezierLayer != null && !editorZoomMode) {
                drawBezierAnchorOverlay(canvas, selectedBezierLayer!!)
            }

            // 5. Render garis panduan magnetik (Snap Guidelines) biru cyan saat layer mendekati tengah kanvas (Prompt 30)
            if (isSnapGuideXVisible || isSnapGuideYVisible) {
                drawSnapGuidelines(canvas, vp)
            }

            if (profiling) {
                pfTotalMs += profileMark(pfFrameStart)
                pfFrameCount++
                if (pfFrameCount >= 30) {
                    Log.d(
                        PROFILE_TAG,
                        "frames=30 total=" + (pfTotalMs / 30 / 1_000_000) +
                            "ms f-filter=" + (pfFilterMs / 30 / 1_000_000) +
                            "ms f-adjust=" + (pfAdjustMs / 30 / 1_000_000) +
                            "ms f-layers=" + (pfLayersMs / 30 / 1_000_000) +
                            "ms f-blur=" + (pfBlurMs / 30 / 1_000_000) +
                            "ms layers=" + layers.size
                    )
                    pfFilterMs = 0; pfAdjustMs = 0; pfLayersMs = 0; pfBlurMs = 0
                    pfTotalMs = 0; pfFrameCount = 0
                }
            }

            // Overlay merah semi-transparan pada area tersembunyi mask saat mode lukis mask aktif (Prompt 06)
            if (isMaskPaintActive && maskPaintLayer?.let { it.hasMask() && it.maskBitmap != null } == true) {
                val mLayer = maskPaintLayer!!
                canvas.save()
                applyLayerStandardTransform(canvas, mLayer)
                val overlayPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x66FF0000.toInt() // merah semi-transparan pada area tersembunyi
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                }
                canvas.drawBitmap(mLayer.maskBitmap!!, 0f, 0f, overlayPaint)
                canvas.restore()
            }
        } finally {
            canvas.restoreToCount(drawSave)
            drawZoomScrollbars(canvas)
            // ── Erase BG: Overlay Cursor (Lingkaran Penghapus Melayang) ─────────────
            // Digambar SETELAH restoreToCount (koordinat LAYAR mentah, bukan kanvas)
            // agar tidak ter-skalakan/tergeser oleh transformasi zoom dan pan.
            // Render hanya saat jari sedang menyentuh layar dalam mode Erase BG aktif.
            if (isMaskPaintActive && maskPaintBrush.eraseBgActive && eraseIsTouching
                && eraseTouchScreenX >= 0f) {
                drawEraseBgCursorOverlay(canvas)
            }
        }
    }

    /**
     * Render overlay kursor penghapus Erase BG di atas layar (dalam koordinat layar langsung).
     * Terdiri dari:
     * 1. Titik merah kecil = posisi jari
     * 2. Garis pemandu vertikal dari jari ke lingkaran target
     * 3. Lingkaran penghapus besar + crosshair di posisi target (atas jari)
     */
    private fun drawEraseBgCursorOverlay(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val fingerX = eraseTouchScreenX
        val fingerY = eraseTouchScreenY
        val targetX = fingerX
        val targetY = fingerY - (maskPaintBrush.fingerOffsetDp * density)

        // Ukuran lingkaran di layar (pixels), proporsional terhadap zoom
        val ringRadius = (maskPaintBrush.brushSize * canvasZoom / 2f).coerceIn(8f, 300f)

        // 1. Garis pemandu dari jari ke target
        val guidePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            strokeWidth = 1.5f * density
            style = android.graphics.Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
        }
        if (maskPaintBrush.fingerOffsetDp > 5f) {
            canvas.drawLine(fingerX, fingerY - 8f * density, targetX, targetY + ringRadius, guidePaint)
        }

        // 2. Titik jari (titik sentuh) — warna sesuai mode
        val fingerDotColor = when {
            maskPaintBrush.autoEraseMode -> 0xFFFF9F0A.toInt()  // oranye = auto warna
            !maskPaintBrush.eraseMode -> 0xFF30D158.toInt()     // hijau = pulihkan
            else -> 0xFFFF453A.toInt()                          // merah = hapus
        }
        val fingerDotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = fingerDotColor
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(fingerX, fingerY, 5f * density, fingerDotPaint)

        // Outline putih pada titik jari
        val fingerOutlinePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        canvas.drawCircle(fingerX, fingerY, 5f * density, fingerOutlinePaint)

        // 3. Lingkaran penghapus di posisi target
        val ringPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        canvas.drawCircle(targetX, targetY, ringRadius, ringPaint)

        // Outline hitam luar untuk visibilitas pada semua background
        val ringOuterPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88000000.toInt()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3.5f * density
        }
        canvas.drawCircle(targetX, targetY, ringRadius, ringOuterPaint)
        // Gambar ulang garis putih di atas outline hitam
        canvas.drawCircle(targetX, targetY, ringRadius, ringPaint)

        // 4. Crosshair (+) kecil di tengah lingkaran
        val crossLen = 6f * density
        val crossPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 1.5f * density
            style = android.graphics.Paint.Style.STROKE
        }
        canvas.drawLine(targetX - crossLen, targetY, targetX + crossLen, targetY, crossPaint)
        canvas.drawLine(targetX, targetY - crossLen, targetX, targetY + crossLen, crossPaint)
    }


    /**
     * Menggambar overlay visualisasi anchor dan handle untuk mode edit Bezier.
     * Menampilkan: anchor points (besar/kecil), handle lines (dashed), handle control points.
     */
    private fun drawBezierAnchorOverlay(canvas: Canvas, penLayer: PenLayer) {
        if (penLayer.anchors.isEmpty()) return

        for ((i, anchor) in penLayer.anchors.withIndex()) {
            val canvasX = penLayer.x + anchor.x
            val canvasY = penLayer.y + anchor.y

            // Draw anchor point (besar jika selected, kecil jika tidak)
            val radius = if (i == selectedAnchorIndex) 10f else 7f
            val paint = if (i == selectedAnchorIndex) selectedAnchorPaint else anchorPointPaint
            canvas.drawCircle(canvasX, canvasY, radius, paint)

            // Draw handles jika selected dan aktif
            if (i == selectedAnchorIndex && anchor.hasActiveHandles()) {
                // Handle In
                val inCanvasX = penLayer.x + anchor.handleInX
                val inCanvasY = penLayer.y + anchor.handleInY
                canvas.drawLine(canvasX, canvasY, inCanvasX, inCanvasY, handleLinePaint)
                canvas.drawCircle(inCanvasX, inCanvasY, 6f, handlePointPaint)

                // Handle Out
                val outCanvasX = penLayer.x + anchor.handleOutX
                val outCanvasY = penLayer.y + anchor.handleOutY
                canvas.drawLine(canvasX, canvasY, outCanvasX, outCanvasY, handleLinePaint)
                canvas.drawCircle(outCanvasX, outCanvasY, 6f, handlePointPaint)
            }
        }
    }

    /**
     * Menggambar garis panduan magnetik biru cyan horizontal dan/atau vertikal
     * ketika posisi layer terkunci mendekati garis tengah kanvas (Prompt 30).
     */
    private fun drawSnapGuidelines(
        canvas: Canvas,
        vp: RectF = if (viewportRect.spanX > 0 && viewportRect.spanY > 0) viewportRect else RectF().apply {
            left = 0f; top = 0f; right = width.toFloat(); bottom = height.toFloat()
        }
    ) {
        if (!isSnapToCenterEnabled || vp.spanX <= 0 || vp.spanY <= 0) return

        // Garis panduan tepi jatuh tepat di border kanvas sehingga tidak terlihat;
        // geser sedikit ke dalam agar umpan balik magnet tepi sama jelas dengan center.
        val edgeInset = 2f * resources.displayMetrics.density

        if (isSnapGuideXVisible) {
            val guideX = snapGuideXPosition ?: vp.midX
            val drawX = when {
                guideX <= vp.left + 0.5f -> vp.left + edgeInset
                guideX >= vp.right - 0.5f -> vp.right - edgeInset
                else -> guideX
            }
            canvas.drawLine(drawX, vp.top, drawX, vp.bottom, snapGuidePaint)
        }

        if (isSnapGuideYVisible) {
            val guideY = snapGuideYPosition ?: vp.midY
            val drawY = when {
                guideY <= vp.top + 0.5f -> vp.top + edgeInset
                guideY >= vp.bottom - 0.5f -> vp.bottom - edgeInset
                else -> guideY
            }
            canvas.drawLine(vp.left, drawY, vp.right, drawY, snapGuidePaint)
        }
    }

    /**
     * Menggambar kisi-kisi grid pembantu penjajaran di kanvas (Prompt 30).
     */
    private fun drawGridGuidelines(
        canvas: Canvas,
        vp: RectF = if (viewportRect.spanX > 0 && viewportRect.spanY > 0) viewportRect else RectF().apply {
            left = 0f; top = 0f; right = width.toFloat(); bottom = height.toFloat()
        }
    ) {
        if (!isGridEnabled || vp.spanX <= 0 || vp.spanY <= 0) return
        val spacing = gridSpacingDp * resources.displayMetrics.density
        if (spacing <= 0f) return

        var x = vp.left + spacing
        while (x < vp.right) {
            canvas.drawLine(x, vp.top, x, vp.bottom, gridPaint)
            x += spacing
        }

        var y = vp.top + spacing
        while (y < vp.bottom) {
            canvas.drawLine(vp.left, y, vp.right, y, gridPaint)
            y += spacing
        }
    }

    /**
     * Menggambar kotak pembatas putus-putus (dashed rectangle) di sekeliling layer aktif
     * beserta margin padding secukupnya (Prompt 25).
     */
    private fun drawSelectionBoundingBox(canvas: Canvas, layer: CanvasLayer) {
        val (w, h) = layer.getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return

        val padding = 0f
        val pts = layer.getSelectionBoxPoints(padding)
        if (pts.size < 8) return

        val boxPath = Path().apply {
            moveTo(pts[0], pts[1])
            lineTo(pts[2], pts[3])
            lineTo(pts[4], pts[5])
            lineTo(pts[6], pts[7])
            close()
        }

        if (layer is TextLayer) {
            canvas.drawPath(
                boxPath,
                if (textEditMode) textEditSelectionPaint else textSelectionFillPaint
            )
            canvas.drawPath(boxPath, selectionOuterBorderPaint)
        }
        canvas.drawPath(boxPath, selectionBoxPaint)

        if (layer is TextLayer && textEditMode) {
            canvas.drawPath(boxPath, textEditBorderPaint)
            drawTextEditLabel(canvas, pts)
        }

        // Gambar 4 tombol handle interaktif di setiap sudut Bounding Box (Prompt 26)
        if (!layer.isLocked) {
            drawTransformHandles(canvas, pts)
        }
    }

    /**
     * Overlay handle interaktif 3D Box: garis bantu arah kedalaman + handle
     * belah ketupat cyan di pusat sisi belakang (seret untuk mengubah kedalaman).
     */
    private fun drawBox3DOverlay(canvas: Canvas, layer: Box3DLayer) {
        val (w, h) = layer.getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return
        val density = resources.displayMetrics.density

        val (fcx, fcy) = layer.getFrontFaceCenterLocal()
        val (hfx, hfy) = layer.mapLocalPointToCanvas(fcx, fcy, w, h)
        val (bcx, bcy) = layer.getBackFaceCenterLocal()
        val (hx, hy) = layer.mapLocalPointToCanvas(bcx, bcy, w, h)

        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAA00A8FF.toInt()
            strokeWidth = 1.5f * density
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f * density, 5f * density), 0f)
        }
        canvas.drawLine(hfx, hfy, hx, hy, guidePaint)

        val size = 9f * density
        val diamondPath = Path().apply {
            moveTo(hx, hy - size)
            lineTo(hx + size, hy)
            lineTo(hx, hy + size)
            lineTo(hx - size, hy)
            close()
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00A8FF.toInt()
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 2f * density
            style = Paint.Style.STROKE
        }
        canvas.drawPath(diamondPath, fillPaint)
        canvas.drawPath(diamondPath, borderPaint)
    }

    private fun drawTextEditLabel(canvas: Canvas, pts: FloatArray) {
        val left = minOf(pts[0], pts[2], pts[4], pts[6])
        val top = minOf(pts[1], pts[3], pts[5], pts[7])
        val label = "EDITING TEXT"
        val horizontalPadding = 7f * resources.displayMetrics.density
        val verticalPadding = 4f * resources.displayMetrics.density
        val width = textEditLabelPaint.measureText(label) + horizontalPadding * 2f
        val height = textEditLabelPaint.textSize + verticalPadding * 2f
        val labelLeft = left
        val labelTop = (top - height - 4f * resources.displayMetrics.density).coerceAtLeast(0f)
        val labelRect = RectF(labelLeft, labelTop, labelLeft + width, labelTop + height)
        canvas.drawRoundRect(labelRect, 5f, 5f, textEditLabelBackgroundPaint)
        canvas.drawText(
            label,
            labelLeft + horizontalPadding,
            labelTop + height - verticalPadding,
            textEditLabelPaint
        )
    }

    /**
     * Tipe handle transformasi interaktif pada 8 titik Bounding Box
     * (4 sudut + 4 sisi). Handle rotasi tidak lagi disediakan di kanvas;
     * rotasi memakai slider derajat di toolbar atau gestur dua jari.
     */
    enum class TransformHandle {
        NONE,
        RESIZE_TL,  // Kiri-atas   (anchor: kanan-bawah)
        RESIZE_TM,  // Tengah-atas (anchor: tengah-bawah)
        RESIZE_TR,  // Kanan-atas  (anchor: kiri-bawah)
        RESIZE_ML,  // Tengah-kiri (anchor: tengah-kanan)
        RESIZE_MR,  // Tengah-kanan (anchor: tengah-kiri)
        RESIZE_BL,  // Kiri-bawah  (anchor: kanan-atas)
        RESIZE_BM,  // Tengah-bawah (anchor: tengah-atas)
        RESIZE_BR   // Kanan-bawah (anchor: kiri-atas)
    }

    /**
     * Indeks 8 handle resize sesuai urutan [com.flyerpix.editor.canvas.model.CanvasLayer.getHandle8Points].
     * Urutan: TL(0), TM(1), TR(2), ML(3), MR(4), BL(5), BM(6), BR(7).
     */
    private fun resizeHandleIndex(handle: TransformHandle): Int = when (handle) {
        TransformHandle.RESIZE_TL -> 0
        TransformHandle.RESIZE_TM -> 1
        TransformHandle.RESIZE_TR -> 2
        TransformHandle.RESIZE_ML -> 3
        TransformHandle.RESIZE_MR -> 4
        TransformHandle.RESIZE_BL -> 5
        TransformHandle.RESIZE_BM -> 6
        TransformHandle.RESIZE_BR -> 7
        else -> -1
    }

    private fun resizeHandleAtIndex(index: Int): TransformHandle = when (index) {
        0 -> TransformHandle.RESIZE_TL
        1 -> TransformHandle.RESIZE_TM
        2 -> TransformHandle.RESIZE_TR
        3 -> TransformHandle.RESIZE_ML
        4 -> TransformHandle.RESIZE_MR
        5 -> TransformHandle.RESIZE_BL
        6 -> TransformHandle.RESIZE_BM
        7 -> TransformHandle.RESIZE_BR
        else -> TransformHandle.NONE
    }

    /** Handle lawan (anchor yang tetap diam) untuk setiap handle resize. */
    private fun oppositeResizeHandleIndex(index: Int): Int = when (index) {
        0 -> 7 // TL <-> BR
        1 -> 6 // TM <-> BM
        2 -> 5 // TR <-> BL
        3 -> 4 // ML <-> MR
        4 -> 3 // MR <-> ML
        5 -> 2 // BL <-> TR
        6 -> 1 // BM <-> TM
        7 -> 0 // BR <-> TL
        else -> -1
    }

    private fun resizeHandleLocalPoint(index: Int, w: Float, h: Float): Pair<Float, Float> = when (index) {
        0 -> Pair(0f, 0f)
        1 -> Pair(w / 2f, 0f)
        2 -> Pair(w, 0f)
        3 -> Pair(0f, h / 2f)
        4 -> Pair(w, h / 2f)
        5 -> Pair(0f, h)
        6 -> Pair(w / 2f, h)
        7 -> Pair(w, h)
        else -> Pair(0f, 0f)
    }

    /**
     * Memeriksa apakah sentuhan mengenai salah satu dari 8 handle resize.
     * Area toleransi sentuh dibuat lebih lebar dari ukuran visual titik
     * (hit radius 12-24dp) agar resize tetap responsif.
     * Mengembalikan [TransformHandle] yang tersentuh, atau [TransformHandle.NONE].
     */
    fun getTransformHandleAt(touchX: Float, touchY: Float): TransformHandle {
        val layer = selectedLayer ?: return TransformHandle.NONE
        if (!layer.isVisible || layer.isLocked || layer.perspectiveEnabled) return TransformHandle.NONE

        val hpts = layer.getHandle8Points(0f)
        if (hpts.size < 16) return TransformHandle.NONE

        val touchRadius = adaptiveTransformHandleRadius(hpts)
            .let { radius ->
                (radius * 1.8f).coerceIn(
                    12f * resources.displayMetrics.density,
                    24f * resources.displayMetrics.density
                )
            }

        for (i in 0..7) {
            if (hypot(touchX - hpts[i * 2], touchY - hpts[i * 2 + 1]) <= touchRadius) {
                return resizeHandleAtIndex(i)
            }
        }

        return TransformHandle.NONE
    }

    /**
     * Memulai drag resize 8-handle: menghitung anchor (sudut/sisi lawan) yang
     * tetap diam selama drag, lalu menyimpan snapshot awal layer.
     * Mengembalikan true jika drag dimulai.
     */
    private fun beginResizeHandleDrag(
        layer: CanvasLayer,
        handle: TransformHandle,
        touchX: Float,
        touchY: Float
    ): Boolean {
        val idx = resizeHandleIndex(handle)
        if (idx < 0) return false
        val anchorIdx = oppositeResizeHandleIndex(idx)
        if (anchorIdx < 0) return false

        val hpts = layer.getHandle8Points(0f)
        if (hpts.size < 16) return false
        val (w, h) = layer.getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return false

        activeResizeHandle = handle
        resizeAnchorPoint = Pair(hpts[anchorIdx * 2], hpts[anchorIdx * 2 + 1])
        resizeStartDist = hypot(
            hpts[idx * 2] - resizeAnchorPoint.first,
            hpts[idx * 2 + 1] - resizeAnchorPoint.second
        ).coerceAtLeast(1f)
        resizeInitialScale = layer.scale
        resizeInitialStretchX = layer.stretchX
        resizeInitialStretchY = layer.stretchY
        resizeInitialW = w
        resizeInitialH = h

        val anchorLocal = resizeHandleLocalPoint(anchorIdx, w, h)
        resizeAnchorFracX = anchorLocal.first / w
        resizeAnchorFracY = anchorLocal.second / h

        // TextLayer: meraih handle tengah-kiri / tengah-kanan mengaktifkan wrapping
        // (wrapWidth) sebagai model resize horizontal.
        if (layer is TextLayer) {
            if ((handle == TransformHandle.RESIZE_ML || handle == TransformHandle.RESIZE_MR) &&
                layer.wrapWidth <= 0f
            ) {
                layer.wrapWidth = layer.measureNaturalWidth().coerceAtLeast(40f)
                layer.wrapTextEnabled = true
            }
            resizeStartWrapWidth = layer.wrapWidth
            resizeStartTextSize = layer.textSize
        }

        currentTouchState = TouchState.DRAGGING_RESIZE_HANDLE
        isDragging = false
        invalidate()
        return true
    }

    /**
     * Memulai drag handle kedalaman 3D Box: mencatat posisi pusat sisi depan
     * (acuan tetap) dan jarak awal jari ke acuan.
     */
    private fun beginBox3DDepthDrag(
        layer: Box3DLayer,
        frontCenterX: Float,
        frontCenterY: Float,
        touchX: Float,
        touchY: Float
    ) {
        box3DDepthStartDepth = layer.boxDepth
        box3DDepthStartDist = hypot(touchX - frontCenterX, touchY - frontCenterY).coerceAtLeast(1f)
        box3DDepthBaseX = frontCenterX
        box3DDepthBaseY = frontCenterY
        currentTouchState = TouchState.DRAGGING_BOX3D_DEPTH_HANDLE
        isDragging = false
        invalidate()
    }

    /** Memperbarui kedalaman 3D Box mengikuti jarak jari ke pusat sisi depan. */
    private fun updateBox3DDepthDrag(layer: Box3DLayer, touchX: Float, touchY: Float) {
        val dist = hypot(touchX - box3DDepthBaseX, touchY - box3DDepthBaseY).coerceAtLeast(1f)
        val factor = dist / box3DDepthStartDist
        layer.boxDepth = (box3DDepthStartDepth * factor).coerceIn(10f, 2000f)
        hasTouchTransformed = true
        invalidate()
    }

    /**
     * Memperbarui nilai resize sesuai jenis layer dan handle yang aktif:
     *  - TextLayer: horizontal -> wrapWidth, vertikal -> textSize, sudut -> textSize (+wrap)
     *  - ShapeLayer: width / height langsung
     *  - Layer lainnya (Image/Sticker/Arrow/Pen): stretchX / stretchY
     * Setelah properti berubah, posisi anchor dipertahankan diam di kanvas.
     */
    private fun updateResizeHandleDrag(
        layer: CanvasLayer,
        handle: TransformHandle,
        touchX: Float,
        touchY: Float
    ) {
        when (layer) {
            is TextLayer -> resizeTextLayer(layer, handle, touchX, touchY)
            is ShapeLayer -> resizeShapeLayer(layer, handle, touchX, touchY)
            else -> resizeStretchLayer(layer, handle, touchX, touchY)
        }
        applyAnchoredStretchFix(layer)
        hasTouchTransformed = true
        invalidate()
    }

    /** Resize layer generik (Image/Sticker/Arrow/Pen) via stretchX/stretchY. */
    private fun resizeStretchLayer(layer: CanvasLayer, handle: TransformHandle, touchX: Float, touchY: Float) {
        val rad = Math.toRadians(layer.rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val dx = touchX - resizeAnchorPoint.first
        val dy = touchY - resizeAnchorPoint.second
        val projX = dx * cos + dy * sin
        val projY = -dx * sin + dy * cos
        val s = if (layer.scale != 0f) layer.scale else 1f

        when (handle) {
            TransformHandle.RESIZE_ML -> {
                val sxEff = ((-projX).coerceAtLeast(1f) / resizeInitialW).coerceIn(0.02f, 50f)
                layer.stretchX = (sxEff / s).coerceIn(0.02f, 50f)
            }
            TransformHandle.RESIZE_MR -> {
                val sxEff = (projX.coerceAtLeast(1f) / resizeInitialW).coerceIn(0.02f, 50f)
                layer.stretchX = (sxEff / s).coerceIn(0.02f, 50f)
            }
            TransformHandle.RESIZE_TM -> {
                val syEff = ((-projY).coerceAtLeast(1f) / resizeInitialH).coerceIn(0.02f, 50f)
                layer.stretchY = (syEff / s).coerceIn(0.02f, 50f)
            }
            TransformHandle.RESIZE_BM -> {
                val syEff = (projY.coerceAtLeast(1f) / resizeInitialH).coerceIn(0.02f, 50f)
                layer.stretchY = (syEff / s).coerceIn(0.02f, 50f)
            }
            TransformHandle.RESIZE_TL, TransformHandle.RESIZE_TR,
            TransformHandle.RESIZE_BL, TransformHandle.RESIZE_BR -> {
                val currentDist = hypot(dx, dy)
                val ratio = (currentDist / resizeStartDist).coerceIn(0.02f, 50f)
                layer.stretchX = (resizeInitialStretchX * ratio).coerceIn(0.02f, 50f)
                layer.stretchY = (resizeInitialStretchY * ratio).coerceIn(0.02f, 50f)
            }
            else -> {}
        }
    }

    /** Resize ShapeLayer langsung mengubah width / height (stroke tetap). */
    private fun resizeShapeLayer(layer: ShapeLayer, handle: TransformHandle, touchX: Float, touchY: Float) {
        val rad = Math.toRadians(layer.rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val dx = touchX - resizeAnchorPoint.first
        val dy = touchY - resizeAnchorPoint.second
        val projX = dx * cos + dy * sin
        val projY = -dx * sin + dy * cos
        val s = if (layer.scale != 0f) layer.scale else 1f
        val sxCurrent = if (s * layer.stretchX != 0f) s * layer.stretchX else 1f
        val syCurrent = if (s * layer.stretchY != 0f) s * layer.stretchY else 1f

        when (handle) {
            TransformHandle.RESIZE_ML, TransformHandle.RESIZE_MR -> {
                val effW = if (handle == TransformHandle.RESIZE_MR) projX else -projX
                val sxTot = (effW.coerceAtLeast(1f) / resizeInitialW).coerceIn(0.02f, 50f)
                layer.width = (resizeInitialW * sxTot / sxCurrent).coerceIn(1f, 10000f)
            }
            TransformHandle.RESIZE_TM, TransformHandle.RESIZE_BM -> {
                val effH = if (handle == TransformHandle.RESIZE_BM) projY else -projY
                val syTot = (effH.coerceAtLeast(1f) / resizeInitialH).coerceIn(0.02f, 50f)
                layer.height = (resizeInitialH * syTot / syCurrent).coerceIn(1f, 10000f)
            }
            TransformHandle.RESIZE_TL, TransformHandle.RESIZE_TR,
            TransformHandle.RESIZE_BL, TransformHandle.RESIZE_BR -> {
                val currentDist = hypot(dx, dy)
                val ratio = (currentDist / resizeStartDist).coerceIn(0.02f, 50f)
                layer.width = (resizeInitialW * ratio).coerceIn(1f, 10000f)
                layer.height = (resizeInitialH * ratio).coerceIn(1f, 10000f)
            }
            else -> {}
        }
    }

    /** Resize TextLayer: horizontal -> wrapWidth, vertikal -> textSize, sudut -> keduanya. */
    private fun resizeTextLayer(layer: TextLayer, handle: TransformHandle, touchX: Float, touchY: Float) {
        val rad = Math.toRadians(layer.rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val dx = touchX - resizeAnchorPoint.first
        val dy = touchY - resizeAnchorPoint.second
        val projX = dx * cos + dy * sin
        val projY = -dx * sin + dy * cos
        val s = if (layer.scale != 0f) layer.scale else 1f

        when (handle) {
            TransformHandle.RESIZE_ML, TransformHandle.RESIZE_MR -> {
                val effPw = if (handle == TransformHandle.RESIZE_MR) projX else -projX
                val pwLocal = effPw / s
                val padL = layer.paddingLeft.coerceAtLeast(0f)
                val padR = layer.paddingRight.coerceAtLeast(0f)
                layer.wrapWidth = (pwLocal - padL - padR).coerceIn(40f, 4000f)
            }
            TransformHandle.RESIZE_TM, TransformHandle.RESIZE_BM -> {
                val effPh = if (handle == TransformHandle.RESIZE_BM) projY else -projY
                val ratio = (effPh / (resizeInitialH * s)).coerceIn(0.01f, 30f)
                layer.textSize = (resizeStartTextSize * ratio).coerceIn(4f, 400f)
            }
            TransformHandle.RESIZE_TL, TransformHandle.RESIZE_TR,
            TransformHandle.RESIZE_BL, TransformHandle.RESIZE_BR -> {
                val currentDist = hypot(dx, dy)
                val ratio = (currentDist / resizeStartDist).coerceIn(0.02f, 50f)
                layer.textSize = (resizeStartTextSize * ratio).coerceIn(4f, 400f)
                if (layer.wrapWidth > 0f) {
                    layer.wrapWidth = (resizeStartWrapWidth * ratio).coerceIn(40f, 4000f)
                }
            }
            else -> {}
        }
    }

    /**
     * Menjaga anchor tetap pada posisi kanvas semula setelah properti resize
     * berubah. Memakai pecahan posisi anchor terhadap dimensi lokal saat ini
     * sehingga berlaku umum untuk semua jenis layer & model resize.
     */
    private fun applyAnchoredStretchFix(layer: CanvasLayer) {
        val (w, h) = layer.getUnwarpedDimensions()
        if (w <= 0f || h <= 0f) return
        val ax = resizeAnchorFracX * w
        val ay = resizeAnchorFracY * h
        val rad = Math.toRadians(layer.rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val sxEff = if (layer.scale * layer.stretchX != 0f) layer.scale * layer.stretchX else 1f
        val syEff = if (layer.scale * layer.stretchY != 0f) layer.scale * layer.stretchY else 1f
        val vx = (ax - w / 2f) * sxEff
        val vy = (ay - h / 2f) * syEff
        val rx = vx * cos - vy * sin + w / 2f
        val ry = vx * sin + vy * cos + h / 2f
        layer.x = resizeAnchorPoint.first - rx
        layer.y = resizeAnchorPoint.second - ry
    }

    /**
     * Menggambar 8 handle resize sebagai titik lingkaran bersih (Clean Dots):
     * sudut sedikit lebih besar (6.5dp), sisi 5.5dp, berbingkai cyan tipis dan
     * tanpa ikon panah. Tidak ada handle rotasi pada bounding box.
     */
    private fun drawTransformHandles(canvas: Canvas, pts: FloatArray) {
        if (pts.size < 8) return
        val hpts = cornersToHandle8(pts)
        if (hpts.size < 16) return
        val fitR = adaptiveTransformHandleRadius(hpts)
        val density = resources.displayMetrics.density
        val cornerR = min(6.5f * density, fitR)
        val midR = min(5.5f * density, fitR)

        for (i in 0..7) {
            val isCorner = i == 0 || i == 2 || i == 5 || i == 7
            drawResizeHandle(
                canvas = canvas,
                cx = hpts[i * 2],
                cy = hpts[i * 2 + 1],
                r = if (isCorner) cornerR else midR,
                handle = resizeHandleAtIndex(i)
            )
        }
    }

    /**
     * Menurunkan 8 posisi handle (16 float) dari 4 sudut kotak seleksi (8 float).
     * Urutan: TL, TM, TR, ML, MR, BL, BM, BR.
     */
    private fun cornersToHandle8(pts: FloatArray): FloatArray {
        if (pts.size < 8) return FloatArray(0)
        val tlX = pts[0]; val tlY = pts[1]
        val trX = pts[2]; val trY = pts[3]
        val brX = pts[4]; val brY = pts[5]
        val blX = pts[6]; val blY = pts[7]
        return floatArrayOf(
            tlX, tlY,
            (tlX + trX) / 2f, (tlY + trY) / 2f,
            trX, trY,
            (tlX + blX) / 2f, (tlY + blY) / 2f,
            (trX + brX) / 2f, (trY + brY) / 2f,
            blX, blY,
            (brX + blX) / 2f, (brY + blY) / 2f,
            brX, brY
        )
    }

    /**
     * Titik resize bersih: lingkaran putih berisi + border cyan tipis, tanpa ikon.
     * Saat sedang ditarik, titik sedikit membesar & highlight pink (feedback aktif).
     */
    private fun drawResizeHandle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        handle: TransformHandle
    ) {
        val isActive = currentTouchState == TouchState.DRAGGING_RESIZE_HANDLE && activeResizeHandle == handle
        val radius = if (isActive) r * 1.15f else r
        canvas.drawCircle(cx, cy, radius, if (isActive) perspectiveHandleActiveCenterPaint else handleBgPaint)
        canvas.drawCircle(cx, cy, radius, handleBorderPaint)
    }

    /** Radius handle yang menyesuaikan jarak terdekat antar 8 posisi handle. */
    private fun adaptiveTransformHandleRadius(hpts: FloatArray): Float {
        val density = resources.displayMetrics.density
        val maxRadius = 13f * density
        val minRadius = 5f * density
        val count = hpts.size / 2

        var nearestDistance = Float.POSITIVE_INFINITY
        for (i in 0 until count) {
            for (j in i + 1 until count) {
                nearestDistance = min(
                    nearestDistance,
                    hypot(hpts[i * 2] - hpts[j * 2], hpts[i * 2 + 1] - hpts[j * 2 + 1])
                )
            }
        }

        val fitRadius = if (nearestDistance.isFinite()) nearestDistance * 0.28f else maxRadius
        return fitRadius.coerceIn(minRadius, maxRadius)
    }

    /**
     * Menggambar 4 handle lingkaran dan garis poligon putus-putus pada sudut bidang perspektif layer.
     */
    private fun drawPerspectiveHandles(canvas: Canvas, layer: CanvasLayer) {
        val pts = layer.getPerspectiveScreenPoints()
        if (pts.size < 8) return

        // 1. Gambar garis pandu poligon perspektif (quad)
        val guidePath = Path().apply {
            moveTo(pts[0], pts[1])
            lineTo(pts[2], pts[3])
            lineTo(pts[4], pts[5])
            lineTo(pts[6], pts[7])
            close()
        }
        canvas.drawPath(guidePath, perspectiveGuidePaint)

        // 2. Gambar 4 lingkaran handle sudut
        val handleRadius = 14f * resources.displayMetrics.density
        val centerRadius = 6f * resources.displayMetrics.density

        for (i in 0..3) {
            val hx = pts[i * 2]
            val hy = pts[i * 2 + 1]
            val isCornerActive = (i == activePerspectiveCorner)

            // Lingkaran luar putih
            canvas.drawCircle(hx, hy, handleRadius, perspectiveHandleFillPaint)
            // Border luar cyan/biru
            canvas.drawCircle(hx, hy, handleRadius, perspectiveHandleBorderPaint)
            // Titik pusat (cyan jika diam, aksen pink jika sedang ditarik aktif)
            val centerPaint = if (isCornerActive) perspectiveHandleActiveCenterPaint else perspectiveHandleCenterPaint
            canvas.drawCircle(hx, hy, centerRadius, centerPaint)
        }
    }

    /**
     * Mencari layer teratas (z-index paling tinggi) yang terlihat dan melingkupi titik ([touchX], [touchY]).
     *
     * @param touchX Koordinat sentuh horizontal pada kanvas.
     * @param touchY Koordinat sentuh vertikal pada kanvas.
     * @return [CanvasLayer] teratas yang tersentuh, atau null jika tidak ada.
     */
    fun findTopLayerAt(touchX: Float, touchY: Float): CanvasLayer? {
        // Cari mundur dari indeks terbesar (layer teratas/z-index tertinggi)
        for (i in layers.indices.reversed()) {
            val layer = layers[i]
            if (layer.isVisible && !layer.isLocked && layer.containsCanvasPoint(touchX, touchY)) {
                return layer
            }
        }
        return null
    }

    private fun updateCanvasTransformMatrices() {
        val cx = width / 2f
        val cy = height / 2f
        val tx = cx + canvasPanX - cx * canvasZoom
        val ty = cy + canvasPanY - cy * canvasZoom
        canvasTransformMatrix.setValues(
            floatArrayOf(
                canvasZoom, 0f, tx,
                0f, canvasZoom, ty,
                0f, 0f, 1f
            )
        )
        if (!canvasTransformMatrix.invert(canvasTransformInverse)) {
            canvasTransformInverse.reset()
        }
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    /**
     * Scale gesture khusus Zoom Mode: pinch mengubah zoom KANVAS (bukan skala layer).
     */
    private val zoomCanvasScaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            if (factor.isNaN() || factor.isInfinite() || factor <= 0f) return false
            setCanvasZoomAt(canvasZoom * factor, detector.focusX, detector.focusY)
            return true
        }
    }

    private val zoomCanvasScaleDetector = ScaleGestureDetector(context, zoomCanvasScaleListener)

    private val rotationGestureListener = object : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(detector: RotationGestureDetector, deltaAngle: Float): Boolean {
            val layer = selectedLayer ?: return false
            if (layer.isLocked) return false

            // Perbarui sudut rotasi layer secara real-time dan akurat (modulo 360 derajat)
            var newRotation = (layer.rotation + deltaAngle) % 360f
            if (newRotation < 0f) {
                newRotation += 360f
            }
            layer.rotation = newRotation
            hasTouchTransformed = true

            invalidate()
            return true
        }

        override fun onRotationBegin(detector: RotationGestureDetector): Boolean {
            val layer = selectedLayer ?: return false
            return !layer.isLocked
        }
    }

    private val rotationGestureDetector = RotationGestureDetector(rotationGestureListener)

    /**
     * Callback ketika layer teks di-double-tap oleh pengguna.
     */
    var onTextLayerDoubleTapListener: ((TextLayer) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val layer = findTopLayerAt(e.x, e.y) ?: selectedLayer
            if (layer == null) return false
            if (layer.isLocked) return false
            if (layer is TextLayer) {
                selectedLayer = layer
                onTextLayerDoubleTapListener?.invoke(layer)
                return true
            }
            // Prompt 29: non-Text → duplikat layer (menggantikan handle duplikat lama).
            duplicateSelectedLayer()
            return true
        }
    })

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (editorZoomMode && event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val verticalDelta = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val horizontalDelta = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val scrollDelta = if (verticalDelta != 0f) verticalDelta else horizontalDelta
            if (scrollDelta != 0f && !scrollDelta.isNaN() && !scrollDelta.isInfinite()) {
                setCanvasZoomAt(
                    canvasZoom + scrollDelta.coerceIn(-1f, 1f) * zoomStep,
                    event.x,
                    event.y
                )
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 0.1. Edit Mode: selaraskan koordinat sentuh dengan transform viewport
        // (zoom + pan) yang dipakai di onDraw, sehingga objek tetap bisa dipilih
        // & diedit walau kanvas sedang diperbesar/digeser. Zoom Mode memakai
        // koordinat layar mentah dan keluar lebih dulu di blok 0.7.
        if (!editorZoomMode && (canvasZoom != 1f || canvasPanX != 0f || canvasPanY != 0f)) {
            updateCanvasTransformMatrices()
            event.transform(canvasTransformInverse)
        }

        // 0. Tangani mode eyedropper — intercept seluruh sentuhan (Prompt 42)
        if (isEyedropperMode) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    touchEventX = event.x
                    touchEventY = event.y
                    val color = getPixelColorAt(event.x, event.y)
                    onEyedropperColorListener?.invoke(color)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isEyedropperMode = false
                }
            }
            return true
        }

        // 0.4. Tangani mode lukis mask — lukis goresan ke maskBitmap layer target (Prompt 06)
        if (maskPaintLayer != null) {
            // ── Erase BG: Simpan koordinat layar mentah SEBELUM transform untuk cursor overlay ──
            // Koordinat ini dipakai di drawEraseBgCursorOverlay (layar, bukan kanvas).
            // Harus diambil dari raw event SEBELUM event.transform() dipanggil di atas.
            // Namun kita tidak bisa undo transform, jadi simpan di sini setelah transform
            // karena Erase BG menggunakan raw screen coords langsung di maskPaintAt.

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    touchEventX = event.x
                    touchEventY = event.y
                    // ── Erase BG: Snapshot mask di awal goresan agar Undo/Redo per-goresan berfungsi ──
                    if (maskPaintBrush.eraseBgActive && event.actionMasked == MotionEvent.ACTION_DOWN && event.pointerCount == 1) {
                        maskPaintLayer?.maskBitmap?.let { snapshotEraseStroke(it) }
                    }
                    // ── Phase 1: Extract pressure from stylus (OPTION D) ──
                    val pressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE).coerceIn(0f, 1f)

                    // ── Erase BG: 2-Jari = Pinch Zoom & Pan (tidak menghapus) ──
                    if (maskPaintBrush.eraseBgActive && event.pointerCount >= 2) {
                        // Event di atas sudah di-transform ke ruang kanvas (0.1), padahal
                        // ScaleGestureDetector & pan harus memakai koordinat layar mentah.
                        // Bangun salinan event dengan koordinat layar mentah kembali.
                        val rawEvent = if (canvasZoom != 1f || canvasPanX != 0f || canvasPanY != 0f) {
                            MotionEvent.obtain(event).also { it.transform(canvasTransformMatrix) }
                        } else {
                            event
                        }
                        try {
                            // Delegasikan ke scale detector untuk zoom/pan
                            zoomCanvasScaleDetector.onTouchEvent(rawEvent)
                            // Pan dengan 2 jari (koordinat layar mentah)
                            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                                val focusX = (rawEvent.getX(0) + rawEvent.getX(1)) / 2f
                                val focusY = (rawEvent.getY(0) + rawEvent.getY(1)) / 2f
                                if (lastTouchX != 0f || lastTouchY != 0f) {
                                    canvasPanX += focusX - lastTouchX
                                    canvasPanY += focusY - lastTouchY
                                    clampCanvasPan()
                                    invalidate()
                                }
                                lastTouchX = focusX
                                lastTouchY = focusY
                            }
                        } finally {
                            if (rawEvent !== event) rawEvent.recycle()
                        }
                        eraseIsTouching = false
                        return true
                    }

                    // Untuk Erase BG: gunakan koordinat layar mentah (maskPaintAt handle transformasinya)
                    // Untuk mode standar: event.x/y sudah ter-transform sebelumnya
                    if (maskPaintBrush.eraseBgActive) {
                        // Reverse transform agar dapat koordinat layar mentah
                        val rawPts = floatArrayOf(event.x, event.y)
                        canvasTransformMatrix.mapPoints(rawPts)  // inverse of canvasTransformInverse
                        eraseTouchScreenX = rawPts[0]
                        eraseTouchScreenY = rawPts[1]
                        eraseIsTouching = true

                        // ── Phase 4: Clone Stamp ──
                        if (maskPaintBrush.cloneMode && (event.metaState and android.view.KeyEvent.META_SHIFT_ON) != 0) {
                            val layer = maskPaintLayer
                            if (layer != null) {
                                val (lx, ly) = canvasToMaskLocal(layer, event.x, event.y)
                                maskPaintBrush.cloneSourceX = lx
                                maskPaintBrush.cloneSourceY = ly
                            }
                        } else {
                            // Erase BG: kirim koordinat layar mentah — maskPaintAt akan hitung offset + transform
                            maskPaintAt(rawPts[0], rawPts[1], pressure)
                        }
                    } else {
                        // Mode mask standar — event sudah ter-transform
                        if (maskPaintBrush.cloneMode && (event.metaState and android.view.KeyEvent.META_SHIFT_ON) != 0) {
                            val layer = maskPaintLayer
                            if (layer != null) {
                                val (lx, ly) = canvasToMaskLocal(layer, event.x, event.y)
                                maskPaintBrush.cloneSourceX = lx
                                maskPaintBrush.cloneSourceY = ly
                                android.util.Log.d("FlyerPixClone", "Clone source set at ($lx, $ly)")
                            }
                        } else {
                            // Normal paint/erase mode
                            maskPaintAt(event.x, event.y, pressure)
                        }
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (maskPaintBrush.eraseBgActive) {
                        val raws = if (canvasZoom != 1f || canvasPanX != 0f || canvasPanY != 0f) {
                            val pts = floatArrayOf(
                                event.getX(0), event.getY(0),
                                event.getX(1), event.getY(1)
                            )
                            canvasTransformMatrix.mapPoints(pts)
                            pts
                        } else {
                            floatArrayOf(
                                event.getX(0), event.getY(0),
                                event.getX(1), event.getY(1)
                            )
                        }
                        lastTouchX = (raws[0] + raws[2]) / 2f
                        lastTouchY = (raws[1] + raws[3]) / 2f
                        lastMaskPaintPoint = null  // Reset goresan saat jari kedua turun
                        eraseIsTouching = false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    lastMaskPaintPoint = null
                    if (maskPaintBrush.eraseBgActive) {
                        eraseIsTouching = false
                        lastTouchX = 0f
                        lastTouchY = 0f
                        // Reset sourceColor saat jari diangkat agar tap berikutnya ambil warna baru
                        if (maskPaintBrush.autoEraseMode) {
                            maskPaintBrush.autoEraseSourceColor = null
                        }
                    }
                    invalidate()
                }
            }
            return true
        }

        // 0.5. Tangani mode input titik Bézier — tap di kanvas menambah titik baru.
        if (bezierInputEnabled) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                addBezierInputPoint(event.x, event.y)
            }
            return true
        }

        // 0.55. Tangani mode edit Bezier — drag anchor/handle untuk edit path (Phase 1)
        if (bezierEditMode && selectedBezierLayer != null) {
            return handleBezierEditModeTouch(event)
        }

        // 0.56. Tangani mode seleksi Rect / Ellipse / Lasso (Prompt 10)
        if (selectionTool != null) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    selectionDragActive = true
                    selectionStartX = event.x
                    selectionStartY = event.y
                    selectionCurrentX = event.x
                    selectionCurrentY = event.y
                    selectionLassoPoints.clear()
                    selectionLassoPoints.add(event.x to event.y)
                    selectionPath = null
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    selectionCurrentX = event.x
                    selectionCurrentY = event.y
                    if (selectionTool == SelectionTool.LASSO) {
                        val last = selectionLassoPoints.last()
                        val dx = event.x - last.first
                        val dy = event.y - last.second
                        if (dx * dx + dy * dy >= 36f) selectionLassoPoints.add(event.x to event.y)
                    }
                    rebuildSelectionPath()
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    selectionDragActive = false
                    rebuildSelectionPath()
                    android.util.Log.d("FlyerPixMask", "touchUP selectionPath=${selectionPath != null}")
                    onSelectionChanged?.invoke()
                    invalidate()
                }
            }
            return true
        }

        // 0.6. Tangani mode gambar bebas — intercept seluruh sentuhan
        if (freeDrawEnabled) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    freeDrawActive = true
                    freeDrawPoints.clear()
                    freeDrawPoints.add(event.x to event.y)
                    onFreeDrawStart?.invoke()
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!freeDrawActive) return false
                    val last = freeDrawPoints.last()
                    val dx = event.x - last.first
                    val dy = event.y - last.second
                    if (dx * dx + dy * dy >= 16f) {
                        freeDrawPoints.add(event.x to event.y)
                    }
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (freeDrawActive) {
                        freeDrawActive = false
                        if (freeDrawPoints.size >= 2) {
                            finishFreeDrawLayer()
                        }
                        freeDrawPoints.clear()
                    }
                    invalidate()
                }
            }
            return true
        }

        // 0.7. Zoom Mode: pinch mengubah zoom kanvas; seluruh interaksi objek
        // (pilih/geser/resize/rotate/handle) dinonaktifkan.
        if (editorZoomMode) {
            zoomCanvasScaleDetector.onTouchEvent(event)

            val density = resources.displayMetrics.density
            val scrollbarHitSize = 16f * density
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                val horizontal = zoomScrollbarMetrics(horizontal = true)
                val vertical = zoomScrollbarMetrics(horizontal = false)
                if (horizontal != null && event.y >= height - 8f * density - scrollbarHitSize &&
                    event.x >= horizontal[0] - scrollbarHitSize && event.x <= horizontal[1] + scrollbarHitSize
                ) {
                    activeZoomScrollbar = 1
                    zoomScrollbarTouchOffset = if (event.x in horizontal[2]..horizontal[3]) {
                        event.x - horizontal[2]
                    } else {
                        (horizontal[3] - horizontal[2]) / 2f
                    }
                    moveFromZoomScrollbar(true, event.x, zoomScrollbarTouchOffset)
                } else if (vertical != null && event.x >= width - 8f * density - scrollbarHitSize &&
                    event.y >= vertical[0] - scrollbarHitSize && event.y <= vertical[1] + scrollbarHitSize
                ) {
                    activeZoomScrollbar = 2
                    zoomScrollbarTouchOffset = if (event.y in vertical[2]..vertical[3]) {
                        event.y - vertical[2]
                    } else {
                        (vertical[3] - vertical[2]) / 2f
                    }
                    moveFromZoomScrollbar(false, event.y, zoomScrollbarTouchOffset)
                } else {
                    activeZoomScrollbar = 0
                }
            }

            // Satu jari hanya menggeser viewport; object tetap tidak interaktif.
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                    activeZoomScrollbar = 0
                    // Jumlah jari berubah (mulai/selesai pinch): segarkan posisi acuan
                    // agar pan tidak meloncat setelah pinch selesai.
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (activeZoomScrollbar != 0 && !zoomCanvasScaleDetector.isInProgress) {
                        moveFromZoomScrollbar(
                            activeZoomScrollbar == 1,
                            if (activeZoomScrollbar == 1) event.x else event.y,
                            zoomScrollbarTouchOffset
                        )
                    } else if (!zoomCanvasScaleDetector.isInProgress) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        canvasPanX += dx
                        canvasPanY += dy
                        clampCanvasPan()
                        invalidate()
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    lastTouchX = 0f
                    lastTouchY = 0f
                    activeZoomScrollbar = 0
                }
            }
            if (zoomCanvasScaleDetector.isInProgress) invalidate()
            return true
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            touchStartState = captureCurrentState("Transform Layer")
            hasTouchTransformed = false
        }

        // 1. Tangani interaksi geser 8-handle resize universal (Prompt 27/29)
        if (currentTouchState == TouchState.DRAGGING_RESIZE_HANDLE) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val layer = selectedLayer
                    val handle = activeResizeHandle
                    if (layer != null && !layer.isLocked && handle != TransformHandle.NONE) {
                        updateResizeHandleDrag(layer, handle, event.x, event.y)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (hasTouchTransformed) {
                        touchStartState?.let { before ->
                            recordAction("Resize Layer", before)
                        }
                    }
                    touchStartState = null
                    hasTouchTransformed = false
                    activeResizeHandle = TransformHandle.NONE
                    currentTouchState = TouchState.IDLE
                    invalidate()
                    return true
                }
            }
        }

        // 1b. Tangani geser handle kedalaman 3D Box (seret titik belakang)
        if (currentTouchState == TouchState.DRAGGING_BOX3D_DEPTH_HANDLE) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val layer = selectedLayer
                    if (layer is Box3DLayer && !layer.isLocked) {
                        updateBox3DDepthDrag(layer, event.x, event.y)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (hasTouchTransformed) {
                        touchStartState?.let { before ->
                            recordAction("Resize Kedalaman 3D", before)
                        }
                    }
                    touchStartState = null
                    hasTouchTransformed = false
                    currentTouchState = TouchState.IDLE
                    invalidate()
                    return true
                }
            }
        }

        // Handle rotasi di kanvas dihapus (rotasi via slider toolbar / gestur dua jari),
        // sehingga tidak ada blok interaksi DRAGGING_ROTATE_HANDLE.

        // Tangani interaksi geser handle sudut perspektif secara prioritas jika sedang aktif
        if (activePerspectiveCorner != -1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    selectedLayer?.let { layer ->
                        if (!layer.isLocked) {
                            layer.setPerspectiveCornerFromCanvas(activePerspectiveCorner, event.x, event.y)
                            hasTouchTransformed = true
                            invalidate()
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (hasTouchTransformed) {
                        touchStartState?.let { before ->
                            recordAction("Move / Transform", before)
                        }
                    }
                    touchStartState = null
                    hasTouchTransformed = false
                    activePerspectiveCorner = -1
                    invalidate()
                    return true
                }
            }
        }

        // Deteksi double-tap untuk membuka dialog edit teks
        gestureDetector.onTouchEvent(event)

        // Rotasi layer tetap tersedia di Edit Mode; pinch zoom hanya tersedia
        // setelah user mengaktifkan Zoom Mode dari header.
        rotationGestureDetector.onTouchEvent(event)

        // Jika gestur skala atau rotasi dua jari sedang berlangsung, hentikan translasi drag
        if (rotationGestureDetector.isInProgress) {
            isDragging = false
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)

                // Cek apakah sentuhan mengenai handle kedalaman 3D Box (if active)
                selectedLayer?.let { layer ->
                    if (!layer.isLocked && layer is Box3DLayer) {
                        val (w, h) = layer.getUnwarpedDimensions()
                        if (w > 0f && h > 0f) {
                            val (fcx, fcy) = layer.getFrontFaceCenterLocal()
                            val (bcx, bcy) = layer.getBackFaceCenterLocal()
                            val (hfx, hfy) = layer.mapLocalPointToCanvas(fcx, fcy, w, h)
                            val (hx, hy) = layer.mapLocalPointToCanvas(bcx, bcy, w, h)
                            val touchRadius = 28f * resources.displayMetrics.density
                            if (hypot(event.x - hx, event.y - hy) <= touchRadius) {
                                beginBox3DDepthDrag(layer, hfx, hfy, event.x, event.y)
                                return true
                            }
                        }
                    }
                }

                // Cek apakah sentuhan mengenai tombol handle sudut Bounding Box (Prompt 26, 27, 28, 29)
                val handle = getTransformHandleAt(event.x, event.y)
                when (handle) {
                    TransformHandle.NONE -> {
                        // Tidak mengenai handle, lanjutkan ke pengecekan perspektif atau seleksi layer
                    }
                    else -> {
                        // 8 handle resize universal (semua jenis layer)
                        selectedLayer?.let { layer ->
                            if (!layer.isLocked) {
                                if (beginResizeHandleDrag(layer, handle, event.x, event.y)) {
                                    return true
                                }
                            }
                        }
                    }
                }

                // Cek apakah sentuhan mengenai salah satu dari 4 handle sudut perspektif
                selectedLayer?.let { layer ->
                    if (layer.perspectiveEnabled && !layer.isLocked) {
                        val pts = layer.getPerspectiveScreenPoints()
                        val touchRadius = 36f * resources.displayMetrics.density
                        for (i in 0..3) {
                            val hx = pts[i * 2]
                            val hy = pts[i * 2 + 1]
                            if (hypot(event.x - hx, event.y - hy) <= touchRadius) {
                                activePerspectiveCorner = i
                                currentTouchState = TouchState.DRAGGING_PERSPECTIVE_HANDLE
                                isDragging = false
                                invalidate()
                                return true
                            }
                        }
                    }
                }

                val touchedLayer = findTopLayerAt(event.x, event.y)
                if (touchedLayer != null) {
                    if (!touchedLayer.isLocked) {
                        selectedLayer = touchedLayer
                        lastTouchX = event.x
                        lastTouchY = event.y
                        isDragging = true
                        invalidate()
                    } else {
                        isDragging = false
                    }
                } else {
                    // Tap di area kosong kanvas membatalkan seleksi layer
                    selectedLayer = null
                    isDragging = false
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Ketika jari kedua menyentuh layar, matikan drag satu jari
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                // Hanya izinkan drag satu jari jika tidak sedang dalam gestur cubit (scale)
                if (event.pointerCount == 1 && !rotationGestureDetector.isInProgress) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1 && isDragging) {
                        val currentX = event.getX(pointerIndex)
                        val currentY = event.getY(pointerIndex)
                        val dx = currentX - lastTouchX
                        val dy = currentY - lastTouchY

                        selectedLayer?.let { layer ->
                            if (!layer.isLocked) {
                                layer.x += dx
                                layer.y += dy
                                hasTouchTransformed = true

                                // Terapkan kunci otomatis ke tengah dan tepi kanvas.
                                val vp = if (viewportRect.spanX > 0 && viewportRect.spanY > 0) viewportRect else RectF().apply {
                                    left = 0f; top = 0f; right = width.toFloat(); bottom = height.toFloat()
                                }
                                if (isSnapToCenterEnabled && vp.spanX > 0 && vp.spanY > 0) {
                                    val bounds = layer.getBounds()
                                    val density = resources.displayMetrics.density
                                    val snapTolerance = 5f * density
                                    val edgeSnapTolerance = 10f * density
                                    val (peerXTargets, peerYTargets) = collectSnapPeerTargets(layer)
                                    val snapResult = SnapCalculator.calculateWithEdges(
                                        layerX = layer.x,
                                        layerY = layer.y,
                                        boundsLeft = bounds.left,
                                        boundsTop = bounds.top,
                                        boundsRight = bounds.right,
                                        boundsBottom = bounds.bottom,
                                        tolerance = snapTolerance,
                                        canvasLeft = vp.left,
                                        canvasTop = vp.top,
                                        canvasRight = vp.right,
                                        canvasBottom = vp.bottom,
                                        edgeTolerance = edgeSnapTolerance,
                                        peerXTargets = peerXTargets,
                                        peerYTargets = peerYTargets
                                    )
                                    layer.x = snapResult.snappedX
                                    layer.y = snapResult.snappedY
                                    isSnapGuideXVisible = snapResult.isSnappedX
                                    isSnapGuideYVisible = snapResult.isSnappedY
                                    snapGuideXPosition = snapResult.guideX
                                    snapGuideYPosition = snapResult.guideY
                                } else {
                                    clearSnapGuides()
                                }

                                invalidate()
                            }
                        }

                        lastTouchX = currentX
                        lastTouchY = currentY
                    }
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) {
                        lastTouchX = event.getX(newPointerIndex)
                        lastTouchY = event.getY(newPointerIndex)
                        activePointerId = event.getPointerId(newPointerIndex)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (hasTouchTransformed) {
                    touchStartState?.let { before ->
                        recordAction("Move / Transform", before)
                    }
                }

                // Tap (tanpa drag) pada ShapeLayer => minta buka panel pengaturan shape
                if (event.actionMasked == MotionEvent.ACTION_UP && !hasTouchTransformed) {
                    val selected = selectedLayer
                    if (selected is ShapeLayer && !selected.isLocked) {
                        onShapeTapRequested?.invoke(selected)
                    }
                }

                touchStartState = null
                hasTouchTransformed = false

                activePointerId = MotionEvent.INVALID_POINTER_ID
                activePerspectiveCorner = -1
                activeResizeHandle = TransformHandle.NONE
                currentTouchState = TouchState.IDLE
                isDragging = false
                val needInvalidate = isSnapGuideXVisible || isSnapGuideYVisible
                clearSnapGuides()
                if (needInvalidate) {
                    invalidate()
                }
            }
        }
        return true
    }

    /**
     * Menambahkan layer baru ke tumpukan teratas (z-index tertinggi).
     */
    fun addLayer(layer: CanvasLayer) {
        runRecordedAction("Add Layer") {
            layers.add(layer)
            selectedLayer = layer
            invalidate()
        }
        notifyLayersChanged()
    }

    /**
     * Menghapus layer tertentu dari daftar.
     */
    fun removeLayer(layer: CanvasLayer): Boolean {
        val before = captureCurrentState("Delete Layer")
        val removed = layers.remove(layer)
        if (removed) {
            if (selectedLayer == layer) {
                selectedLayer = layers.lastOrNull()
            }
            invalidate()
            recordAction("Delete Layer", before)
            notifyLayersChanged()
        }
        return removed
    }

    fun moveLayersBy(layersToMove: Collection<CanvasLayer>, dx: Float, dy: Float): Int {
        val movableLayers = layersToMove.filter { layers.contains(it) && !it.isLocked }
        if (movableLayers.isEmpty()) return 0

        val before = captureCurrentState("Move Layer")
        movableLayers.forEach { layer ->
            layer.x += dx
            layer.y += dy
        }
        invalidate()
        recordAction("Move Layer", before)
        notifyLayersChanged()
        return movableLayers.size
    }

    fun moveSelectedLayerBy(dx: Float, dy: Float): Boolean =
        moveLayersBy(listOfNotNull(selectedLayer), dx, dy) > 0

    /**
     * Mengosongkan seluruh layer dari kanvas.
     */
    fun clearLayers() {
        if (layers.isEmpty()) return
        runRecordedAction("Clear Canvas") {
            layers.clear()
            selectedLayer = null
            invalidate()
        }
        notifyLayersChanged()
    }

    /**
     * Menghapus layer aktif saat ini dari [layers], membatalkan seleksi ([selectedLayer] = null),
     * dan menggambar ulang kanvas (Prompt 29).
     *
     * @return true jika layer berhasil dihapus, false jika tidak ada layer aktif atau layer terkunci.
     */
    fun deleteSelectedLayer(): Boolean {
        val layer = selectedLayer ?: return false
        if (layer.isLocked) return false
        val before = captureCurrentState("Delete Layer")
        val removed = layers.remove(layer)
        if (removed) {
            selectedLayer = null
            invalidate()
            recordAction("Delete Layer", before)
            notifyLayersChanged()
        }
        return removed
    }

    /**
     * Menduplikasi layer aktif saat ini dengan offset (x + 30, y + 30),
     * menambahkannya ke tumpukan [layers], langsung menjadikannya layer aktif terpilih,
     * dan menggambar ulang kanvas (Prompt 29).
     *
     * @return Layer baru hasil kloning, atau null jika tidak ada layer aktif atau layer terkunci.
     */
    fun duplicateSelectedLayer(): CanvasLayer? {
        val layer = selectedLayer ?: return null
        if (layer.isLocked) return null

        val before = captureCurrentState("Duplicate Layer")
        val cloned = layer.copyLayer()
        cloned.x = layer.x + 30f
        cloned.y = layer.y + 30f

        layers.add(cloned)
        selectedLayer = cloned
        invalidate()
        recordAction("Duplicate Layer", before)
        notifyLayersChanged()
        return cloned
    }

    /**
     * Memindahkan [layer] ke posisi paling atas tumpukan z-index (Prompt 35).
     *
     * @return true jika layer berhasil dipindahkan atau sudah di paling atas, false jika layer tidak ditemukan di kanvas.
     */
    fun bringLayerToFront(layer: CanvasLayer): Boolean {
        val index = layers.indexOf(layer)
        if (index == -1) return false
        if (index == layers.size - 1) {
            selectedLayer = layer
            invalidate()
            return true
        }
        runRecordedAction("Bring to Front") {
            layers.removeAt(index)
            layers.add(layer)
            selectedLayer = layer
            invalidate()
        }
        notifyLayersChanged()
        return true
    }

    /**
     * Memindahkan [layer] ke posisi paling bawah tumpukan z-index (tepat di atas background kanvas) (Prompt 35).
     *
     * @return true jika layer berhasil dipindahkan atau sudah di paling bawah, false jika layer tidak ditemukan di kanvas.
     */
    fun sendLayerToBack(layer: CanvasLayer): Boolean {
        val index = layers.indexOf(layer)
        if (index == -1) return false
        if (index == 0) {
            selectedLayer = layer
            invalidate()
            return true
        }
        runRecordedAction("Send to Back") {
            layers.removeAt(index)
            layers.add(0, layer)
            selectedLayer = layer
            invalidate()
        }
        notifyLayersChanged()
        return true
    }

    /**
     * Memindahkan layer aktif saat ini ([selectedLayer]) ke posisi paling atas tumpukan (Prompt 35).
     *
     * @return true jika berhasil, false jika tidak ada layer aktif atau layer sedang terkunci.
     */
    fun bringSelectedLayerToFront(): Boolean {
        val layer = selectedLayer ?: return false
        if (layer.isLocked) return false
        return bringLayerToFront(layer)
    }

    /**
     * Memindahkan layer aktif saat ini ([selectedLayer]) ke posisi paling bawah tumpukan (tepat di atas background kanvas) (Prompt 35).
     *
     * @return true jika berhasil, false jika tidak ada layer aktif atau layer sedang terkunci.
     */
    fun sendSelectedLayerToBack(): Boolean {
        val layer = selectedLayer ?: return false
        if (layer.isLocked) return false
        return sendLayerToBack(layer)
    }

    /**
     * Memeriksa apakah layer aktif dapat dipindahkan ke paling depan.
     */
    fun canBringSelectedLayerToFront(): Boolean {
        val layer = selectedLayer ?: return false
        if (layer.isLocked) return false
        val index = layers.indexOf(layer)
        return index != -1 && index < layers.size - 1
    }

    /**
     * Memeriksa apakah layer aktif dapat dipindahkan ke paling belakang.
     */
    fun canSendSelectedLayerToBack(): Boolean {
        val layer = selectedLayer ?: return false
        if (layer.isLocked) return false
        val index = layers.indexOf(layer)
        return index > 0
    }

    /**
     * Memperoleh jumlah layer yang ada di kanvas.
     */
    fun getLayerCount(): Int = layers.size

    /**
     * Menggabungkan daftar layer [layersToMerge] menjadi satu objek [ImageLayer] tunggal di kanvas (Prompt 36).
     *
     * Alur:
     * 1. Validasi: minimal 2 layer yang terdapat di kanvas.
     * 2. Menghitung bounding box bersama (minX, minY, maxX, maxY) dari seluruh layer yang digabung.
     * 3. Membuat offscreen bitmap berukuran bounding box dan merender layer secara berurutan sesuai z-index.
     * 4. Menghapus layer-layer asal dari [layers].
     * 5. Menyisipkan [ImageLayer] baru di posisi z-index minimum dari layer asal.
     * 6. Menjadikan [ImageLayer] sebagai layer terpilih ([selectedLayer]) dan memanggil [invalidate].
     *
     * @param layersToMerge Koleksi layer yang akan digabungkan (minimal 2 layer).
     * @param bitmapFactory Factory opsional untuk pembuatan bitmap (memudahkan pengujian unit murni).
     * @return Objek [ImageLayer] baru hasil penggabungan, atau null jika validasi gagal.
     */
    fun mergeLayers(
        layersToMerge: Collection<CanvasLayer>,
        bitmapFactory: ((width: Int, height: Int) -> Bitmap)? = null
    ): ImageLayer? {
        if (layersToMerge.size < 2) return null
        val validLayers = layersToMerge.filter { layers.contains(it) }
        if (validLayers.size < 2) return null

        val before = captureCurrentState("Merge Layers")

        // Urutkan berdasarkan urutan kemunculan di kanvas (z-index)
        val sortedLayers = validLayers.sortedBy { layers.indexOf(it) }

        // Hitung bounding box gabungan
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (layer in sortedLayers) {
            val bounds = layer.getBounds()
            if (bounds.left < minX) minX = bounds.left
            if (bounds.top < minY) minY = bounds.top
            if (bounds.right > maxX) maxX = bounds.right
            if (bounds.bottom > maxY) maxY = bounds.bottom
        }

        val bw = ceil(maxX - minX).toInt().coerceAtLeast(1)
        val bh = ceil(maxY - minY).toInt().coerceAtLeast(1)
        if (bw <= 0 || bh <= 0) return null

        val mergedBitmap = try {
            val bitmap = bitmapFactory?.invoke(bw, bh) ?: Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
            val offscreenCanvas = Canvas(bitmap)
            offscreenCanvas.translate(-minX, -minY)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            for (layer in sortedLayers) {
                if (!layer.isVisible) continue
                if (layer.blendMode != PorterDuff.Mode.SRC_OVER || layer.blendExtra != null) {
                    paint.applyLayerBlend(layer)
                    val saveCount = offscreenCanvas.saveLayer(null, paint)
                    layer.draw(offscreenCanvas, paint)
                    offscreenCanvas.restoreToCount(saveCount)
                    paint.clearBlend()
                } else {
                    val saveCount = offscreenCanvas.save()
                    layer.draw(offscreenCanvas, paint)
                    offscreenCanvas.restoreToCount(saveCount)
                }
            }
            bitmap
        } catch (_: Throwable) {
            null
        } ?: return null

        val mergedLayer = ImageLayer(
            x = minX,
            y = minY,
            scale = 1f,
            rotation = 0f,
            opacity = 255,
            bitmap = mergedBitmap,
            layerName = "Merged Layer"
        )

        // Simpan posisi z-index terendah dari layer-layer yang digabungkan
        val minCanvasIndex = sortedLayers.map { layers.indexOf(it) }.minOrNull() ?: 0

        // Hapus layer lama dari kanvas
        layers.removeAll(sortedLayers)

        // Sisipkan layer hasil merge di posisi z-index minimum
        val insertIndex = minCanvasIndex.coerceIn(0, layers.size)
        layers.add(insertIndex, mergedLayer)

        selectedLayer = mergedLayer
        invalidate()
        recordAction("Merge Layers", before)
        notifyLayersChanged()
        return mergedLayer
    }

    /**
     * Menambahkan TextLayer baru ke kanvas dan langsung menjadikannya layer terpilih.
     */
    fun addTextLayer(text: String = "New Text"): TextLayer {
        val posX = if (width > 0) (width / 2f - 140f).coerceAtLeast(30f) else 100f
        val posY = if (height > 0) (height / 2f - 40f).coerceAtLeast(30f) else 200f
        val layer = TextLayer(
            text = text,
            x = posX,
            y = posY,
            textColor = Color.WHITE,
            textSize = 64f
        )
        addLayer(layer)
        return layer
    }

    /**
     * Menambahkan StickerLayer (emoji) baru ke kanvas tepat di tengah dan menjadikannya layer terpilih.
     *
     * @param emoji Teks emoji yang akan dirender sebagai stiker.
     * @param size  Ukuran bitmap stiker dalam piksel (default 128).
     * @return [StickerLayer] yang baru ditambahkan.
     */
    fun addEmojiLayer(emoji: String, size: Int = 128): StickerLayer {
        val layer = StickerLayer.fromEmoji(emoji, size)
        // Posisikan tepat di tengah kanvas
        if (width > 0 && height > 0) {
            layer.x = (width / 2f) - (size / 2f)
            layer.y = (height / 2f) - (size / 2f)
        } else {
            layer.x = 100f
            layer.y = 100f
        }
        addLayer(layer)
        return layer
    }

    // ── Project Snapshot (Prompt 47) ──────────────────────────────────────────

    /**
     * Mengekspor seluruh state kanvas saat ini ke dalam [ProjectModel].
     *
     * [ProjectModel] ini selanjutnya dapat disimpan ke file `.plp` melalui [com.flyerpix.editor.project.ProjectSerializer].
     *
     * @param projectName Nama proyek (digunakan sebagai nama file default).
     * @return [ProjectModel] snapshot state kanvas saat ini.
     */
    fun exportProjectSnapshot(projectName: String = "Untitled"): ProjectModel = ProjectModel(
        projectName  = projectName,
        canvasWidth  = canvasWidth,
        canvasHeight = canvasHeight,
        background   = canvasBackground,
        layers       = layers.toMutableList()
    )

    /**
     * Mengimpor dan memulihkan state kanvas dari [ProjectModel].
     *
     * Operasi ini akan menggantikan seluruh layer dan konfigurasi kanvas yang ada saat ini.
     * Pastikan pengguna sudah mengkonfirmasi (atau proyek sudah disimpan) sebelum memanggil fungsi ini.
     *
     * @param project [ProjectModel] yang akan dimuat ke kanvas.
     */
    fun importProjectSnapshot(project: ProjectModel) {
        layers.clear()
        selectedLayer = null
        setCanvasSize(project.canvasWidth, project.canvasHeight)
        canvasBackground = project.background
        layers.addAll(project.layers)
        invalidate()
        notifyLayersChanged()
    }

    // ── High Resolution / 4K Off-Screen Exporter (Prompt 49) ──────────────────

    /**
     * Merender seluruh kanvas (background dan semua layer aktif) ke dalam [Bitmap] off-screen
     * sesuai dengan [ExportQuality] atau dimensi kustom [customWidth] x [customHeight] (Prompt 49).
     *
     * Fitur utama:
     * 1. Menghasilkan bitmap beresolusi tinggi (misal 1920x1080 Full HD atau 3840x2160 Ultra HD / 4K).
     * 2. TIDAK merender handle seleksi, bounding box, titik handle perspektif, garis grid, ataupun snap lines.
     * 3. Mendukung transparansi murni (true alpha transparency) untuk format PNG jika mode background TRANSPARENT.
     * 4. Memetakan skala koordinat secara proporsional dari on-screen viewport ke canvas off-screen target.
     *
     * @param quality Tingkat kualitas/resolusi ekspor.
     * @param format Format berkas target (PNG atau JPEG).
     * @param customWidth Lebar kustom dalam piksel (opsional).
     * @param customHeight Tinggi kustom dalam piksel (opsional).
     * @return [Bitmap] hasil rendering off-screen beresolusi tinggi.
     */
    fun renderOffscreenBitmap(
        quality: ExportQuality = ExportQuality.DEFAULT,
        format: ExportFormat = ExportFormat.PNG,
        customWidth: Int? = null,
        customHeight: Int? = null
    ): Bitmap {
        val (targetWidth, targetHeight) = if (customWidth != null && customHeight != null && customWidth > 0 && customHeight > 0) {
            Pair(customWidth.coerceIn(50, 8192), customHeight.coerceIn(50, 8192))
        } else {
            quality.calculateDimensions(canvasWidth, canvasHeight)
        }

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val offscreenCanvas = Canvas(bitmap)

        // Ekspor bisa berjalan di thread background (exportHighResolutionAsync);
        // ambil snapshot layer agar aman dari mutasi UI selama render panjang.
        val snapshotLayers = layers.toList()

        // Efek Filter (monokrom) dibungkus sebagai layer komposit (Prompt 51).
        val filterEffectLayer = beginFilterEffectLayer(offscreenCanvas)

        // 1. Render Background Kanvas Off-Screen
        val offscreenRect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        when (canvasBackground.mode) {
            CanvasBackgroundMode.TRANSPARENT -> {
                if (format == ExportFormat.PNG) {
                    // Biarkan transparan murni tanpa gambar checkerboard
                    offscreenCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                } else {
                    // JPEG tidak mendukung channel alpha transparansi, gunakan warna dasar putih
                    offscreenCanvas.drawColor(Color.WHITE)
                }
            }
            CanvasBackgroundMode.SOLID_COLOR -> {
                canvasBgPaint.shader = null
                canvasBgPaint.color = canvasBackground.solidColor
                offscreenCanvas.drawRect(offscreenRect, canvasBgPaint)
            }
            CanvasBackgroundMode.GRADIENT -> {
                val grad = canvasBackground.gradient
                if (grad != null) {
                    canvasBgPaint.shader = grad.createShader(offscreenRect)
                } else {
                    canvasBgPaint.shader = null
                    canvasBgPaint.color = canvasBackground.solidColor
                }
                offscreenCanvas.drawRect(offscreenRect, canvasBgPaint)
            }
            CanvasBackgroundMode.IMAGE -> {
                val bmp = canvasBackground.imageBitmap
                if (bmp != null && !bmp.isRecycled) {
                    canvasBgPaint.shader = null
                    val srcAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    val dstAspect = offscreenRect.width() / offscreenRect.height()
                    val src: Rect
                    if (srcAspect > dstAspect) {
                        val visibleW = (bmp.height * dstAspect).toInt()
                        val offsetX = (bmp.width - visibleW) / 2
                        src = Rect(offsetX, 0, offsetX + visibleW, bmp.height)
                    } else {
                        val visibleH = (bmp.width / dstAspect).toInt()
                        val offsetY = (bmp.height - visibleH) / 2
                        src = Rect(0, offsetY, bmp.width, offsetY + visibleH)
                    }
                    offscreenCanvas.drawBitmap(bmp, src, offscreenRect, canvasBgPaint)
                } else {
                    canvasBgPaint.shader = null
                    canvasBgPaint.color = canvasBackground.solidColor
                    offscreenCanvas.drawRect(offscreenRect, canvasBgPaint)
                }
            }
        }

        // 2. Skalakan dan Petakan Koordinat Layer dari Viewport ke Target Offscreen
        val vp = if (viewportRect.spanX > 0 && viewportRect.spanY > 0) {
            viewportRect
        } else {
            RectF(0f, 0f, if (width > 0) width.toFloat() else canvasWidth.toFloat(), if (height > 0) height.toFloat() else canvasHeight.toFloat())
        }

        val scaleX = targetWidth.toFloat() / vp.spanX
        val scaleY = targetHeight.toFloat() / vp.spanY

        val saveCount = offscreenCanvas.save()
        offscreenCanvas.scale(scaleX, scaleY)
        offscreenCanvas.translate(-vp.left, -vp.top)

        // 3. Render Seluruh Layer Aktif (Tanpa Handle Seleksi Bounding Box, Grid, atau Garis Panduan)
        for (layer in snapshotLayers) {
            if (layer.isVisible) {
                if (layer.blendMode != PorterDuff.Mode.SRC_OVER || layer.blendExtra != null) {
                    renderPaint.applyLayerBlend(layer)
                    val layerSave = offscreenCanvas.saveLayer(null, renderPaint)
                    sanitizeSharedPaint()
                    layer.draw(offscreenCanvas, renderPaint)
                    sanitizeSharedPaint()
                    offscreenCanvas.restoreToCount(layerSave)
                    renderPaint.clearBlend()
                } else {
                    renderPaint.clearBlend()
                    val layerSave = offscreenCanvas.save()
                    sanitizeSharedPaint()
                    layer.draw(offscreenCanvas, renderPaint)
                    sanitizeSharedPaint()
                    offscreenCanvas.restoreToCount(layerSave)
                }
            }
        }

        offscreenCanvas.restoreToCount(saveCount)

        // Tutup layer komposit filter bila aktif (Prompt 51).
        endFilterEffectLayer(offscreenCanvas, filterEffectLayer)

        // 4. Terapkan efek overlay non-destruktif (Noise, Vignette) pada resolusi target (Prompt 51)
        drawEffectsOverlay(offscreenCanvas, offscreenRect, bakedBlur = false)

        return bitmap
    }

    /**
     * Mengekspor kanvas pada resolusi tinggi/Ultra HD ke format berkas PNG atau JPEG
     * dan menyimpannya langsung ke Galeri perangkat via Android MediaStore API (Prompt 49).
     *
     * @param quality Tingkat kualitas/resolusi ekspor (misal: DEFAULT, HIGH, ULTRA_HD).
     * @param format Format berkas target (PNG atau JPEG).
     * @param customWidth Lebar kustom dalam piksel (opsional).
     * @param customHeight Tinggi kustom dalam piksel (opsional).
     * @param fileName Nama berkas gambar tanpa ekstensi (default "PixelLab_<timestamp>").
     * @return [Uri] gambar yang berhasil disimpan di Galeri, atau null jika gagal.
     */
    fun exportHighResolution(
        quality: ExportQuality = ExportQuality.DEFAULT,
        format: ExportFormat = ExportFormat.PNG,
        customWidth: Int? = null,
        customHeight: Int? = null,
        fileName: String? = null
    ): Uri? {
        val bitmap = renderOffscreenBitmap(quality, format, customWidth, customHeight)
        return runExport(bitmap, format, fileName)
    }

    /**
     * Versi asinkron dari [exportHighResolution]: render offscreen + kompres +
     * tulis MediaStore dijalankan di thread background agar UI tidak tersendat,
     * lalu [onResult] dipanggil di main thread dengan URI (null jika gagal).
     */
    fun exportHighResolutionAsync(
        quality: ExportQuality = ExportQuality.DEFAULT,
        format: ExportFormat = ExportFormat.PNG,
        customWidth: Int? = null,
        customHeight: Int? = null,
        fileName: String? = null,
        onResult: (Uri?) -> Unit
    ) {
        val t0 = System.nanoTime()
        shareExportMillis = 0L
        exportExecutor.execute {
            val t1 = System.nanoTime()
            val uri = try {
                val bitmap = renderOffscreenBitmap(quality, format, customWidth, customHeight)
                runExport(bitmap, format, fileName)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            shareExportMillis = (System.nanoTime() - t1) / 1_000_000
            if (profileEnabled) {
                android.util.Log.d(
                    PROFILE_TAG,
                    "export render+write=${(System.nanoTime() - t1) / 1_000_000}ms" +
                        " (mulai render ${(t1 - t0) / 1_000_000}ms setelah diminta)"
                )
            }
            mainHandler.post { onResult(uri) }
        }
    }

    /** Proses kompres + tulis URI dari bitmap hasil render. */
    private fun runExport(bitmap: Bitmap, format: ExportFormat, fileName: String?): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val finalFileName = if (!fileName.isNullOrBlank()) fileName.trim() else "PixelLab_$timestamp"
        val ext = format.extension
        val mime = format.mimeType

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$finalFileName.$ext")
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixelLab")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(format.compressFormat, 100, stream)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    uri
                } else {
                    null
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val pixelLabDir = File(picturesDir, "PixelLab")
                if (!pixelLabDir.exists()) pixelLabDir.mkdirs()
                val destFile = File(pixelLabDir, "$finalFileName.$ext")
                FileOutputStream(destFile).use { stream ->
                    bitmap.compress(format.compressFormat, 100, stream)
                }
                val uri = Uri.fromFile(destFile)
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf(mime), null)
                uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Durasi (ms) export asinkron terakhir (render+kompres+tulis). Untuk debug.
     */
    @Volatile
    var shareExportMillis: Long = 0L
        private set

    // ── Shape Layer ──────────────────────────────────────────────────────────

    fun addShapeLayer(type: ShapeType): ShapeLayer {
        val cx = if (width > 0) width / 2f else 540f
        val cy = if (height > 0) height / 2f else 540f
        val size = (minOf(width, height).takeIf { it > 0 }?.toFloat() ?: 400f) * 0.35f
        val layer = ShapeLayer(
            shapeType = type,
            width = size,
            height = size,
            x = cx - size / 2f,
            y = cy - size / 2f
        )
        addLayer(layer)
        return layer
    }

    /**
     * Menambahkan kurva Bézier preset (gelombang S) di tengah kanvas sebagai [PenLayer].
     */
    fun addPenLayer(): PenLayer {
        val cx = if (width > 0) width / 2f else 540f
        val cy = if (height > 0) height / 2f else 540f
        val size = (minOf(width, height).takeIf { it > 0 }?.toFloat() ?: 400f) * 0.25f
        val w = size
        val h = size * 0.55f
        val layer = PenLayer(isClosed = false).apply {
            anchors.add(AnchorPoint(x = 0f,         y = h * 0.2f, handleOutX = w * 0.25f, handleOutY = h * 0.2f, type = AnchorType.CORNER))
            anchors.add(AnchorPoint(x = w * 0.5f,   y = h * 0.85f, handleInX = w * 0.25f, handleInY = h * 0.85f, handleOutX = w * 0.75f, handleOutY = h * 0.85f, type = AnchorType.CORNER))
            anchors.add(AnchorPoint(x = w,          y = h * 0.2f, handleInX = w * 0.75f, handleInY = h * 0.2f, type = AnchorType.CORNER))
            strokeColor = Color.WHITE
            strokeWidth = 6f
            x = cx - w / 2f
            y = cy - h * 0.525f
        }
        addLayer(layer)
        return layer
    }

    /**
     * Menambahkan panah lurus default di tengah kanvas sebagai [ArrowLayer].
     */
    fun addArrowLayer(): ArrowLayer {
        val cx = if (width > 0) width / 2f else 540f
        val cy = if (height > 0) height / 2f else 540f
        val stem = (minOf(width, height).takeIf { it > 0 }?.toFloat() ?: 400f) * 0.3f
        val head = stem * 0.15f
        val w = stem + head
        val h = 6f * 3f
        val layer = ArrowLayer(
            stemLength = stem,
            stemWidth = 6f,
            angle = 0f,
            headSize = head,
            headEnabled = true,
            headColor = 0xFF1769FF.toInt(),
            x = cx - w / 2f,
            y = cy - h / 2f
        )
        addLayer(layer)
        return layer
    }

    /**
     * Menambahkan objek 3D Box (balok) default di tengah kanvas sebagai
     * [Box3DLayer] dengan proyeksi isometrik.
     */
    fun addBox3DLayer(): Box3DLayer {
        val cx = if (width > 0) width / 2f else 540f
        val cy = if (height > 0) height / 2f else 540f
        val base = (minOf(width, height).takeIf { it > 0 }?.toFloat() ?: 400f) * 0.4f
        val w = base
        val h = base * 0.82f
        val d = base * 0.7f
        val layer = Box3DLayer(
            boxWidth = w,
            boxHeight = h,
            boxDepth = d,
            baseColor = 0xFF1769FF.toInt(),
            x = cx - w / 2f,
            y = cy - h / 2f
        )
        addLayer(layer)
        return layer
    }

    /**
     * Memfinalisasi goresan gambar bebas menjadi [PenLayer] pada koordinat kanvas saat ini.
     */
    private fun finishFreeDrawLayer() {
        if (freeDrawPoints.size < 2) return
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        for (p in freeDrawPoints) {
            minX = min(minX, p.first)
            minY = min(minY, p.second)
        }
        val rel = freeDrawPoints.map { (it.first - minX) to (it.second - minY) }
        val layer = PenLayer.fromPoints(rel).apply {
            x = minX
            y = minY
            strokeColor = freeDrawPaint.color
            strokeWidth = freeDrawPaint.strokeWidth
        }
        addLayer(layer)
        invalidate()
    }

    // ── Canvas Adjustments ───────────────────────────────────────────────────

    enum class CanvasAdjustment {
        BRIGHTNESS, CONTRAST, SATURATION, BLUR,
        EXPOSURE, HIGHLIGHTS, SHADOWS, TEMPERATURE, TINT, GAMMA, VIBRANCE, HUE
    }

    private val adjustments = mutableMapOf(
        CanvasAdjustment.BRIGHTNESS to 0f,
        CanvasAdjustment.CONTRAST   to 0f,
        CanvasAdjustment.SATURATION to 0f,
        CanvasAdjustment.BLUR       to 0f,
        CanvasAdjustment.EXPOSURE   to 0f,
        CanvasAdjustment.HIGHLIGHTS to 0f,
        CanvasAdjustment.SHADOWS    to 0f,
        CanvasAdjustment.TEMPERATURE to 0f,
        CanvasAdjustment.TINT       to 0f,
        CanvasAdjustment.GAMMA      to 0f,
        CanvasAdjustment.VIBRANCE   to 0f,
        CanvasAdjustment.HUE        to 0f
    )

    fun setAdjustment(type: CanvasAdjustment, value: Float) {
        adjustments[type] = value
        invalidate()
    }

    fun getAdjustment(type: CanvasAdjustment): Float = adjustments[type] ?: 0f

    private data class AdjustmentParams(
        val brightness: Float = 0f,
        val contrast: Float = 0f,
        val saturation: Float = 0f,
        val hue: Float = 0f,
        val exposure: Float = 0f,
        val highlights: Float = 0f,
        val shadows: Float = 0f,
        val temperature: Float = 0f,
        val tint: Float = 0f,
        val gamma: Float = 1f,
        val vibrance: Float = 0f
    ) {
        val isActive: Boolean
            get() = brightness != 0f || contrast != 0f || saturation != 0f || hue != 0f ||
                exposure != 0f || highlights != 0f || shadows != 0f ||
                temperature != 0f || tint != 0f || gamma != 1f || vibrance != 0f
    }

    private var adjustSnapshotBitmap: Bitmap? = null
    private var adjustSnapshotPixels: IntArray? = null

    /**
     * Membaca nilai adjustment (skala UI percent -100..100; GAMMA netral di 0)
     * menjadi parameter pipeline native (skala -1..1; GAMMA netral di 1).
     */
    private fun currentAdjustmentParams(): AdjustmentParams = AdjustmentParams(
        brightness = (adjustments[CanvasAdjustment.BRIGHTNESS] ?: 0f) / 100f,
        contrast = (adjustments[CanvasAdjustment.CONTRAST] ?: 0f) / 100f,
        saturation = (adjustments[CanvasAdjustment.SATURATION] ?: 0f) / 100f,
        hue = adjustments[CanvasAdjustment.HUE] ?: 0f,
        exposure = (adjustments[CanvasAdjustment.EXPOSURE] ?: 0f) / 100f,
        highlights = (adjustments[CanvasAdjustment.HIGHLIGHTS] ?: 0f) / 100f,
        shadows = (adjustments[CanvasAdjustment.SHADOWS] ?: 0f) / 100f,
        temperature = (adjustments[CanvasAdjustment.TEMPERATURE] ?: 0f) / 100f,
        tint = (adjustments[CanvasAdjustment.TINT] ?: 0f) / 100f,
        gamma = (adjustments[CanvasAdjustment.GAMMA] ?: 0f).let { g ->
            (1f + g / 100f * 3f).coerceIn(0.1f, 4f)
        },
        vibrance = (adjustments[CanvasAdjustment.VIBRANCE] ?: 0f) / 100f
    )

    private fun obtainAdjustSnapshotBitmap(): Bitmap {
        val w = if (width > 0) width else 1
        val h = if (height > 0) height else 1
        val cur = adjustSnapshotBitmap
        if (cur != null && cur.width == w && cur.height == h && !cur.isRecycled) return cur
        adjustSnapshotBitmap?.recycle()
        adjustSnapshotBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        adjustSnapshotPixels = IntArray(w * h)
        return adjustSnapshotBitmap!!
    }

    /**
     * Render komposisi ke snapshot offscreen ukuran view, proses pipeline
     * extended adjustment via native, lalu gambar balik ke canvas (Prompt 02).
     */
    private fun drawAdjustedContent(canvas: Canvas, vp: RectF, params: AdjustmentParams) {
        val bmp = obtainAdjustSnapshotBitmap()
        val w = bmp.width
        val h = bmp.height
        if (w <= 0 || h <= 0 || vp.width() <= 0f || vp.height() <= 0f) return
        val off = Canvas(bmp)
        off.scale(w / vp.width(), h / vp.height())
        off.translate(-vp.left, -vp.top)
        val filterEffectLayer = beginFilterEffectLayer(off)
        drawCompositionContent(off, vp)
        endFilterEffectLayer(off, filterEffectLayer)
        off.setBitmap(null)

        val pixels = adjustSnapshotPixels ?: return
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        runCatching {
            FilterEngine.applyColorAdjustPixels(
                pixels, w, h,
                params.brightness, params.contrast, params.saturation, params.hue,
                params.exposure, params.highlights, params.shadows,
                params.temperature, params.tint, params.gamma, params.vibrance
            )
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        canvas.drawBitmap(bmp, null, vp, null)
    }

    companion object {
        private const val PROFILE_TAG = "FlyerPixProfile"
        @Volatile var profileEnabled = false

        /** Alpha grain noise (setara [noisePaint] Skia). */
        const val NOISE_OVERLAY_ALPHA = 26

        /** Seed deterministik noise (sama dengan tile Java milik Skia). */
        const val NOISE_SEED = 0xC0FFEE

        /**
         * Batas jumlah piksel untuk flood fill Auto-Color Erase dalam SATU kali tap.
         * Bitmap lebih besar dari ini hanya memakai stamp brush per goresan agar
         * tidak boros memori (getPixels penuh ~4 byte/piksel).
         */
        const val FLOOD_FILL_MAX_PIXELS = 4_000_000L

        /** Batas jumlah layer peer yang dihitung sebagai target magnet saat drag. */
        private const val MAX_SNAP_PEER_LAYERS = 32
    }
}
