package com.flyerpix.editor.imageedit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ImagePreEditView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var image: Bitmap? = null
    val selection = CropSelection()

    private val zoomLevels = floatArrayOf(1f, 1.6f, 2.4f, 3.6f)
    private var zoomIndex = 0
    private var panX = 0f
    private var panY = 0f

    private var mode = DragMode.NONE
    private var draggedCorner = DragCorner.TOP_LEFT
    private var lastX = 0f
    private var lastY = 0f

    private val density = resources.displayMetrics.density
    private val minNormSize = 0.03f

    private val bgPaint = Paint().apply { color = Color.rgb(24, 24, 24) }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val scrimPaint = Paint().apply { color = 0x73000000 }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.WHITE
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = 0x55FFFFFF
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.WHITE
    }
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(66, 165, 245)
    }

    fun setImage(bitmap: Bitmap) {
        image = bitmap
        selection.reset()
        zoomIndex = 0
        panX = 0f
        panY = 0f
        invalidate()
    }

    fun rotateLeft() = rotate(clockwise = false)

    fun rotateRight() = rotate(clockwise = true)

    fun flipHorizontal() = flip(vertical = false)

    fun flipVertical() = flip(vertical = true)

    fun zoomIn() {
        if (zoomIndex < zoomLevels.lastIndex) {
            zoomIndex++
            panX = 0f
            panY = 0f
            invalidate()
        }
    }

    fun zoomOut() {
        if (zoomIndex > 0) {
            zoomIndex--
            panX = 0f
            panY = 0f
            invalidate()
        }
    }

    fun toggleLock(): Boolean {
        selection.isLocked = !selection.isLocked
        if (selection.isLocked && selection.shape == CropShape.RECTANGLE) {
            selection.rect = CropMath.applyShapeLock(selection.rect, DragCorner.TOP_LEFT)
        }
        invalidate()
        return selection.isLocked
    }

    fun toggleShape(): CropShape {
        val shape = selection.toggleShape()
        invalidate()
        return shape
    }

    fun buildResultBitmap(): Bitmap? {
        val src = image ?: return null
        return if (selection.shape == CropShape.CIRCLE) cropCircle(src) else cropRect(src)
    }

    // ── Transformasi bitmap ───────────────────────────────────────────────────

    private fun rotate(clockwise: Boolean) {
        val src = image ?: return
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(h, w, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (clockwise) {
            canvas.translate(h.toFloat(), 0f)
            canvas.rotate(90f)
        } else {
            canvas.translate(0f, w.toFloat())
            canvas.rotate(-90f)
        }
        canvas.drawBitmap(src, 0f, 0f, null)
        image = out
        selection.rect = if (clockwise) {
            CropMath.rotateCW(selection.rect)
        } else {
            CropMath.rotateCCW(selection.rect)
        }
        selection.circle = if (clockwise) {
            CropMath.rotateCW(selection.circle)
        } else {
            CropMath.rotateCCW(selection.circle)
        }
        zoomIndex = 0
        panX = 0f
        panY = 0f
        invalidate()
    }

    private fun flip(vertical: Boolean) {
        val src = image ?: return
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (vertical) {
            canvas.scale(1f, -1f, w / 2f, h / 2f)
        } else {
            canvas.scale(-1f, 1f, w / 2f, h / 2f)
        }
        canvas.drawBitmap(src, 0f, 0f, null)
        image = out
        if (vertical) {
            selection.rect = CropMath.flipV(selection.rect)
            selection.circle = CropMath.flipV(selection.circle)
        } else {
            selection.rect = CropMath.flipH(selection.rect)
            selection.circle = CropMath.flipH(selection.circle)
        }
        invalidate()
    }

    private fun cropRect(src: Bitmap): Bitmap {
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val s = selection.rect
        val left = (s.left * w).roundToInt().coerceIn(0, src.width - 1)
        val top = (s.top * h).roundToInt().coerceIn(0, src.height - 1)
        val right = (s.right * w).roundToInt().coerceIn(left + 1, src.width)
        val bottom = (s.bottom * h).roundToInt().coerceIn(top + 1, src.height)
        return Bitmap.createBitmap(src, left, top, right - left, bottom - top, null, false)
    }

    private fun cropCircle(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val minDim = min(w, h)
        val c = selection.circle
        val rad = (c.radius * minDim).toFloat()
        val cx = c.centerX * w
        val cy = c.centerY * h
        val size = (rad * 2f).roundToInt().coerceIn(1, minDim)
        val left = (cx - rad).roundToInt().coerceIn(0, w - size)
        val top = (cy - rad).roundToInt().coerceIn(0, h - size)
        val crop = Bitmap.createBitmap(src, left, top, size, size)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(crop, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return out
    }

    // ── Geometri tampilan ─────────────────────────────────────────────────────

    private fun currentDisplayRect(): RectF {
        val bmp = image ?: return RectF()
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw <= 0f || vh <= 0f) return RectF()
        val fit = min(vw / bmp.width, vh / bmp.height)
        val zoom = zoomLevels[zoomIndex]
        val dw = bmp.width * fit * zoom
        val dh = bmp.height * fit * zoom
        val left = (vw - dw) / 2f + panX
        val top = (vh - dh) / 2f + panY
        return RectF(left, top, left + dw, top + dh)
    }

    private fun toNorm(fx: Float, fy: Float, rc: RectF): Pair<Float, Float> {
        val u = (fx - rc.left) / rc.width()
        val v = (fy - rc.top) / rc.height()
        return u to v
    }

    private enum class DragMode { NONE, CORNER, MOVE, CIRCLE_MOVE, CIRCLE_RESIZE, PAN }

    private fun hitTest(fx: Float, fy: Float): DragMode {
        val rc = currentDisplayRect()
        if (rc.width() <= 0f || rc.height() <= 0f) return DragMode.NONE
        val slop = 16f * density
        if (selection.shape == CropShape.CIRCLE) {
            val cx = rc.left + selection.circle.centerX * rc.width()
            val cy = rc.top + selection.circle.centerY * rc.height()
            val rad = selection.circle.radius * min(rc.width(), rc.height())
            val handleX = cx + rad
            if (hypot((handleX - fx).toDouble(), (cy - fy).toDouble()) <= slop + rad * 0.2f) {
                return DragMode.CIRCLE_RESIZE
            }
            if (hypot((cx - fx).toDouble(), (cy - fy).toDouble()) <= slop + rad) {
                return DragMode.CIRCLE_MOVE
            }
            if (zoomIndex > 0) return DragMode.PAN
            return DragMode.NONE
        }

        val s = selection.rect
        val l = rc.left + s.left * rc.width()
        val t = rc.top + s.top * rc.height()
        val r = rc.left + s.right * rc.width()
        val b = rc.top + s.bottom * rc.height()

        if (near(fx, fy, l, t, slop)) {
            draggedCorner = DragCorner.TOP_LEFT
            return DragMode.CORNER
        }
        if (near(fx, fy, r, t, slop)) {
            draggedCorner = DragCorner.TOP_RIGHT
            return DragMode.CORNER
        }
        if (near(fx, fy, r, b, slop)) {
            draggedCorner = DragCorner.BOTTOM_RIGHT
            return DragMode.CORNER
        }
        if (near(fx, fy, l, b, slop)) {
            draggedCorner = DragCorner.BOTTOM_LEFT
            return DragMode.CORNER
        }
        val (u, v) = toNorm(fx, fy, rc)
        if (u in s.left..s.right && v in s.top..s.bottom) return DragMode.MOVE
        if (zoomIndex > 0) return DragMode.PAN
        return DragMode.NONE
    }

    private fun near(fx: Float, fy: Float, px: Float, py: Float, slop: Float): Boolean {
        return kotlin.math.abs(fx - px) <= slop && kotlin.math.abs(fy - py) <= slop
    }

    private fun moveSelection(du: Float, dv: Float) {
        val s = selection.rect
        val nl = (s.left + du).coerceIn(0f, 1f - s.width)
        val nt = (s.top + dv).coerceIn(0f, 1f - s.height)
        selection.rect = RectNorm(nl, nt, nl + s.width, nt + s.height).normalized()
    }

    private fun moveCircle(du: Float, dv: Float) {
        val c = selection.circle
        selection.circle = CircleNorm(c.centerX + du, c.centerY + dv, c.radius).normalized()
    }

    private fun resizeFromCorner(fx: Float, fy: Float) {
        val rc = currentDisplayRect()
        val (u, v) = toNorm(fx, fy, rc)
        val s = selection.rect
        val nl: Float
        val nt: Float
        val nr: Float
        val nb: Float
        when (draggedCorner) {
            DragCorner.TOP_LEFT -> {
                nl = min(u, s.right - minNormSize)
                nt = min(v, s.bottom - minNormSize)
                nr = s.right
                nb = s.bottom
            }
            DragCorner.TOP_RIGHT -> {
                nl = s.left
                nt = min(v, s.bottom - minNormSize)
                nr = max(u, s.left + minNormSize)
                nb = s.bottom
            }
            DragCorner.BOTTOM_RIGHT -> {
                nl = s.left
                nt = s.top
                nr = max(u, s.left + minNormSize)
                nb = max(v, s.top + minNormSize)
            }
            DragCorner.BOTTOM_LEFT -> {
                nl = min(u, s.right - minNormSize)
                nt = s.top
                nr = s.right
                nb = max(v, s.top + minNormSize)
            }
        }
        var next = RectNorm(nl, nt, nr, nb).normalized()
        if (selection.isLocked) {
            next = CropMath.applyShapeLock(next, draggedCorner)
        }
        selection.rect = next
    }

    private fun resizeCircle(fx: Float, fy: Float) {
        val rc = currentDisplayRect()
        val c = selection.circle
        val cx = rc.left + c.centerX * rc.width()
        val cy = rc.top + c.centerY * rc.height()
        val minDisp = min(rc.width(), rc.height())
        val rad = (hypot((fx - cx).toDouble(), (fy - cy).toDouble()) / minDisp).toFloat()
        selection.circle = CircleNorm(c.centerX, c.centerY, rad).normalized()
    }

    // ── Gambar ────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = image ?: return
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val rc = currentDisplayRect()
        canvas.drawBitmap(bmp, null, rc, bitmapPaint)
        if (selection.shape == CropShape.CIRCLE) {
            drawCircleSelection(canvas, rc)
        } else {
            drawRectSelection(canvas, rc)
        }
    }

    private fun drawRectSelection(canvas: Canvas, rc: RectF) {
        val s = selection.rect
        val l = rc.left + s.left * rc.width()
        val t = rc.top + s.top * rc.height()
        val r = rc.left + s.right * rc.width()
        val b = rc.top + s.bottom * rc.height()

        canvas.drawRect(0f, 0f, rc.width(), t, scrimPaint)
        canvas.drawRect(0f, b, rc.width(), rc.height(), scrimPaint)
        canvas.drawRect(0f, t, l, b, scrimPaint)
        canvas.drawRect(r, t, rc.width(), b, scrimPaint)

        for (i in 1..2) {
            val gx = rc.left + (s.left + (s.right - s.left) * i / 3f) * rc.width()
            val gy = rc.top + (s.top + (s.bottom - s.top) * i / 3f) * rc.height()
            canvas.drawLine(gx, t, gx, b, gridPaint)
            canvas.drawLine(l, gy, r, gy, gridPaint)
        }

        canvas.drawRect(l, t, r, b, borderPaint)

        val hs = 6f * density
        drawHandle(canvas, l, t, hs)
        drawHandle(canvas, r, t, hs)
        drawHandle(canvas, l, b, hs)
        drawHandle(canvas, r, b, hs)
    }

    private fun drawCircleSelection(canvas: Canvas, rc: RectF) {
        val c = selection.circle
        val cx = rc.left + c.centerX * rc.width()
        val cy = rc.top + c.centerY * rc.height()
        val rad = c.radius * min(rc.width(), rc.height())

        val save = canvas.saveLayer(
            0f, 0f, width.toFloat(), height.toFloat(),
            null, Canvas.ALL_SAVE_FLAG
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        val clear = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        canvas.drawCircle(cx, cy, rad, clear)
        canvas.restoreToCount(save)

        canvas.drawCircle(cx, cy, rad, handlePaint)
        drawHandle(canvas, cx + rad, cy, 6f * density)
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawCircle(x, y, size, handleFillPaint)
        canvas.drawCircle(x, y, size, handlePaint)
    }

    // ── Sentuhan ──────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                mode = hitTest(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val rc = currentDisplayRect()
                val dx = event.x - lastX
                val dy = event.y - lastY
                if (rc.width() > 0f && rc.height() > 0f) {
                    when (mode) {
                        DragMode.MOVE -> moveSelection(dx / rc.width(), dy / rc.height())
                        DragMode.CIRCLE_MOVE -> moveCircle(dx / rc.width(), dy / rc.height())
                        DragMode.CORNER -> resizeFromCorner(event.x, event.y)
                        DragMode.CIRCLE_RESIZE -> resizeCircle(event.x, event.y)
                        DragMode.PAN -> {
                            panX += dx
                            panY += dy
                        }
                        DragMode.NONE -> Unit
                    }
                }
                lastX = event.x
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = DragMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}