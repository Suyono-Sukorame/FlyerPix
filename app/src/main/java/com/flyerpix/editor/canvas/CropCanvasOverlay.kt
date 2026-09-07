package com.flyerpix.editor.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Overlay interaktif untuk fitur Crop Canvas (Prompt 46).
 *
 * Menampilkan area kerja kanvas dengan frame crop yang dapat diubah ukurannya
 * melalui 8 handle (4 sudut + 4 sisi tengah). Area di luar crop di-overlay gelap.
 */
class CropCanvasOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Bounding box area kanvas penuh dalam koordinat view (dari PixelCanvasView.viewportRect). */
    var canvasBounds: RectF = RectF()

    /** Bounding box crop saat ini dalam koordinat view. */
    var cropRect: RectF = RectF()

    /** Apakah overlay aktif. */
    var isActive: Boolean = false

    /** Callback saat crop rect berubah (drag handle). */
    var onCropRectChanged: ((RectF) -> Unit)? = null

    /** Callback saat crop dikonfirmasi (tap tombol OK). */
    var onCropConfirmed: ((left: Float, top: Float, right: Float, bottom: Float) -> Unit)? = null

    /** Callback saat crop dibatalkan. */
    var onCropCancelled: (() -> Unit)? = null

    private enum class Handle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT
    }

    private var activeHandle: Handle = Handle.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val handleRadius = 12f * resources.displayMetrics.density
    private val touchSlop = 24f * resources.displayMetrics.density
    private val minCropSize = 48f * resources.displayMetrics.density

    private val overlayPaint = Paint().apply {
        color = 0x88000000.toInt()
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF18C8F5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }

    /**
     * Mengatur ulang crop rect ke ukuran penuh canvas bounds.
     */
    fun resetCropRect() {
        cropRect.set(canvasBounds)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!isActive) return

        // 1. Overlay gelap di seluruh area view
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // 2. Potong area crop dari overlay (buat transparan)
        canvas.save()
        canvas.clipRect(cropRect)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.restore()

        // 3. Grid rule-of-thirds di dalam crop
        drawGrid(canvas)

        // 4. Border crop
        canvas.drawRect(cropRect, borderPaint)

        // 5. 8 Handle
        drawHandles(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val w = cropRect.width()
        val h = cropRect.height()
        if (w < minCropSize * 2 || h < minCropSize * 2) return

        // Vertical lines (1/3, 2/3)
        for (i in 1..2) {
            val x = cropRect.left + w * i / 3f
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint)
        }
        // Horizontal lines (1/3, 2/3)
        for (i in 1..2) {
            val y = cropRect.top + h * i / 3f
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint)
        }
    }

    private fun drawHandles(canvas: Canvas) {
        val corners = listOf(
            Pair(cropRect.left, cropRect.top),
            Pair(cropRect.right, cropRect.top),
            Pair(cropRect.left, cropRect.bottom),
            Pair(cropRect.right, cropRect.bottom)
        )
        for ((cx, cy) in corners) {
            canvas.drawCircle(cx, cy, handleRadius, handlePaint)
            canvas.drawCircle(cx, cy, handleRadius, handleBorderPaint)
        }

        // Edge midpoints
        val edges = listOf(
            Pair((cropRect.left + cropRect.right) / 2f, cropRect.top),
            Pair((cropRect.left + cropRect.right) / 2f, cropRect.bottom),
            Pair(cropRect.left, (cropRect.top + cropRect.bottom) / 2f),
            Pair(cropRect.right, (cropRect.top + cropRect.bottom) / 2f)
        )
        val edgeRadius = handleRadius * 0.7f
        for ((cx, cy) in edges) {
            canvas.drawCircle(cx, cy, edgeRadius, handlePaint)
            canvas.drawCircle(cx, cy, edgeRadius, handleBorderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isActive) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = hitTestHandle(event.x, event.y)
                if (activeHandle == Handle.NONE) return false
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeHandle == Handle.NONE) return false
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y
                applyHandleDrag(dx, dy)
                onCropRectChanged?.invoke(RectF(cropRect))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = Handle.NONE
                return true
            }
        }
        return false
    }

    private fun hitTestHandle(x: Float, y: Float): Handle {
        // Corners (higher priority)
        if (dist(x, y, cropRect.left, cropRect.top) <= touchSlop) return Handle.TOP_LEFT
        if (dist(x, y, cropRect.right, cropRect.top) <= touchSlop) return Handle.TOP_RIGHT
        if (dist(x, y, cropRect.left, cropRect.bottom) <= touchSlop) return Handle.BOTTOM_LEFT
        if (dist(x, y, cropRect.right, cropRect.bottom) <= touchSlop) return Handle.BOTTOM_RIGHT

        // Edge midpoints
        val midX = (cropRect.left + cropRect.right) / 2f
        val midY = (cropRect.top + cropRect.bottom) / 2f
        if (dist(x, y, midX, cropRect.top) <= touchSlop) return Handle.TOP
        if (dist(x, y, midX, cropRect.bottom) <= touchSlop) return Handle.BOTTOM
        if (dist(x, y, cropRect.left, midY) <= touchSlop) return Handle.LEFT
        if (dist(x, y, cropRect.right, midY) <= touchSlop) return Handle.RIGHT

        return Handle.NONE
    }

    private fun applyHandleDrag(dx: Float, dy: Float) {
        val b = canvasBounds
        when (activeHandle) {
            Handle.TOP_LEFT -> {
                cropRect.left = clamp(cropRect.left + dx, b.left, cropRect.right - minCropSize)
                cropRect.top = clamp(cropRect.top + dy, b.top, cropRect.bottom - minCropSize)
            }
            Handle.TOP_RIGHT -> {
                cropRect.right = clamp(cropRect.right + dx, cropRect.left + minCropSize, b.right)
                cropRect.top = clamp(cropRect.top + dy, b.top, cropRect.bottom - minCropSize)
            }
            Handle.BOTTOM_LEFT -> {
                cropRect.left = clamp(cropRect.left + dx, b.left, cropRect.right - minCropSize)
                cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + minCropSize, b.bottom)
            }
            Handle.BOTTOM_RIGHT -> {
                cropRect.right = clamp(cropRect.right + dx, cropRect.left + minCropSize, b.right)
                cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + minCropSize, b.bottom)
            }
            Handle.TOP -> {
                cropRect.top = clamp(cropRect.top + dy, b.top, cropRect.bottom - minCropSize)
            }
            Handle.BOTTOM -> {
                cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + minCropSize, b.bottom)
            }
            Handle.LEFT -> {
                cropRect.left = clamp(cropRect.left + dx, b.left, cropRect.right - minCropSize)
            }
            Handle.RIGHT -> {
                cropRect.right = clamp(cropRect.right + dx, cropRect.left + minCropSize, b.right)
            }
            Handle.NONE -> {}
        }
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun clamp(value: Float, min: Float, max: Float): Float = max(min, min(value, max))
}
