package com.flyerpix.editor.editableimageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.constraintlayout.utils.widget.ImageFilterView
import com.flyerpix.editor.editableimageview.figures.Circle
import com.flyerpix.editor.editableimageview.figures.CropSquare
import com.flyerpix.editor.editableimageview.figures.Figure
import com.flyerpix.editor.editableimageview.figures.Line
import com.flyerpix.editor.editableimageview.figures.Polygon
import com.flyerpix.editor.editableimageview.figures.Square
import com.flyerpix.editor.editableimageview.paint.Path

class EditableImageView(context: Context, attrs: AttributeSet) : ImageFilterView(context, attrs) {
    private var mScaleFactor = 1f
    lateinit var gestureDetector: GestureDetector
    lateinit var scaleDetector: ScaleGestureDetector
    val paint = Paint()
    var polygons: MutableList<Polygon> = ArrayList()
    var paths: MutableList<Path> = ArrayList()
    var lines: MutableList<Line> = ArrayList()
    var pathMap: MutableMap<Int, Path> = HashMap()

    var currentColor = Color.WHITE

    var figureMode = -1
    var editMode = -1

    lateinit var myContext: MyContext
    private var currentStroke: Float = 0f
    private val STROKE_WIDTH = 8f

    init {
        currentStroke = STROKE_WIDTH
        myContext = MyContext(this)
        val gestureListener = GestureListener()
        gestureDetector = GestureDetector(getContext(), gestureListener)
        paint.strokeWidth = STROKE_WIDTH
        scaleDetector = ScaleGestureDetector(getContext(), ScaleListener())
    }

    /** Clears the current canvas */
    fun clear() {
        paths = ArrayList()
        lines = ArrayList()
        polygons = ArrayList()
        pathMap = HashMap()
        currentStroke = STROKE_WIDTH
        paint.strokeWidth = STROKE_WIDTH
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        when (editMode) {
            EditorTool.PAINT -> {
                myContext.strategyTool = StrategyPaint()
                myContext.onTouchEvent(event)
            }
            EditorTool.FIGURE -> {
                if (figureMode == Figure.LINE) {
                    myContext.strategyTool = StrategyLine()
                    myContext.onTouchEvent(event)
                } else {
                    myContext.strategyTool = StrategyRotation()
                    myContext.onTouchEvent(event)
                }
            }
            EditorTool.STICKER -> {
                myContext.strategyTool = StrategyRotation()
                myContext.onTouchEvent(event)
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        for (p in polygons) {
            when (p) {
                is Circle -> {
                    paint.color = p.color.toInt()
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                is CropSquare -> {
                    if (p.bitmap != null) {
                        val scaleBitmap = Bitmap.createScaledBitmap(
                            p.bitmap!!,
                            (p.size * 2).toInt(),
                            (p.size * 2).toInt(),
                            true
                        )
                        canvas.drawBitmap(scaleBitmap, p.x - p.size, p.y - p.size, paint)
                    } else {
                        paint.style = Paint.Style.STROKE
                        paint.color = p.color.toInt()
                        canvas.drawRect(
                            p.x - p.size, p.y - p.size,
                            p.x + p.size, p.y + p.size,
                            paint
                        )
                    }
                }
                is Square -> {
                    canvas.save()
                    paint.style = Paint.Style.FILL
                    paint.color = p.color.toInt()
                    canvas.rotate(p.rotation, p.x, p.y)
                    canvas.drawRect(
                        p.x - p.size, p.y - p.size,
                        p.x + p.size, p.y + p.size,
                        paint
                    )
                    canvas.restore()
                }
            }
        }
        for (p in paths) {
            paint.color = p.color
            paint.strokeWidth = p.strokeWidth
            for (l in p.lines) {
                canvas.drawLine(l.x0, l.y0, l.xf, l.yf, paint)
            }
            paint.strokeWidth = STROKE_WIDTH
        }
        for (id in pathMap.keys) {
            val path = pathMap.getValue(id)
            paint.color = path.color
            paint.strokeWidth = path.strokeWidth
            for (l in path.lines) {
                canvas.drawLine(l.x0, l.y0, l.xf, l.yf, paint)
            }
            paint.strokeWidth = STROKE_WIDTH
        }
        paint.strokeWidth = STROKE_WIDTH
        for (l in lines) {
            paint.color = l.color
            canvas.drawLine(l.x0, l.y0, l.xf, l.yf, paint)
        }
        canvas.restore()
    }

    fun getTouchedPolygon(xTouch: Float, yTouch: Float): Polygon? {
        var touched: Polygon? = null
        for (p in polygons) {
            val size = p.size
            val x = p.x
            val y = p.y
            if (((x - size) < xTouch && (x + size) > xTouch) &&
                ((y + size) > yTouch && (y - size) < yTouch)
            ) {
                touched = p
            }
        }
        return touched
    }

    fun addPath(id: Int) {
        pathMap[id] = Path(currentColor, currentStroke)
    }

    fun updateLines(id: Int, x: Float, y: Float) {
        val path = pathMap.getValue(id)
        val lines = path.lines
        if (lines.size > 1) {
            lines[lines.size - 1].xf = x
            lines[lines.size - 1].yf = y
        }
        lines.add(Line(x, y, x, y, Color.BLACK))
    }

    /**
     * Listener of gesture actions.
     * Double tap: figure mode determines which figure will be drawn.
     */
    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        var p: Polygon? = null
        var lastTouchX = 0f
        var lastTouchY = 0f

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            p = getTouchedPolygon(e.x, e.y)
            val crop = p
            if (crop != null && crop is CropSquare) {
                val d: Drawable = myContext.editableImageView.drawable
                var bitmap = (d as BitmapDrawable).bitmap
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    myContext.editableImageView.width,
                    myContext.editableImageView.height,
                    true
                )
                var cropx0 = (crop.x - crop.size).toInt()
                var cropy0 = (crop.y - crop.size).toInt()
                if (cropx0 < 0) cropx0 = 0
                if (cropy0 < 0) cropy0 = 0
                val cropBitmap: Bitmap = if (crop.x + crop.size > bitmap.width) {
                    Bitmap.createBitmap(
                        bitmap, cropx0, cropy0,
                        (bitmap.width - (crop.x - crop.size)).toInt(),
                        (crop.size * 2).toInt()
                    )
                } else if (crop.y + crop.size > bitmap.height) {
                    Bitmap.createBitmap(
                        bitmap, cropx0, cropy0,
                        (crop.size * 2).toInt(),
                        (bitmap.height - (crop.y - crop.size)).toInt()
                    )
                } else if (crop.y + crop.size > bitmap.height && crop.x + crop.size > bitmap.width) {
                    Bitmap.createBitmap(
                        bitmap, cropx0, cropy0,
                        (bitmap.width - (crop.x - crop.size)).toInt(),
                        (bitmap.height - (crop.y - crop.size)).toInt()
                    )
                } else {
                    Bitmap.createBitmap(
                        bitmap, cropx0, cropy0,
                        (crop.size * 2).toInt(),
                        (crop.size * 2).toInt()
                    )
                }
                crop.bitmap = cropBitmap
            }
        }

        override fun onDown(e: MotionEvent): Boolean {
            p = getTouchedPolygon(e.x, e.y)
            lastTouchX = e.x
            lastTouchY = e.y
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (!scaleDetector.isInProgress &&
                ((editMode == EditorTool.FIGURE && figureMode != Figure.LINE) || editMode == EditorTool.STICKER) &&
                editMode != EditorTool.PAINT
            ) {
                val touched = p
                if (touched != null) {
                    val deltaX = e2.x - lastTouchX
                    val deltaY = e2.y - lastTouchY
                    touched.x += deltaX
                    touched.y += deltaY
                    lastTouchX = e2.x
                    lastTouchY = e2.y
                }
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (editMode == EditorTool.FIGURE) {
                when (figureMode) {
                    Figure.SQUARE -> {
                        val s: Polygon = Square(e.x, e.y, 100.0, currentColor.toFloat())
                        polygons.add(s)
                    }
                    Figure.CIRCLE -> {
                        val c: Polygon = Circle(e.x, e.y, 100f, currentColor.toFloat())
                        polygons.add(c)
                    }
                }
            } else if (editMode == EditorTool.STICKER) {
                val s = CropSquare(e.x, e.y, 100.0)
                polygons.add(s)
            }
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        var p: Polygon? = null

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (editMode != EditorTool.PAINT) {
                p = getTouchedPolygon(detector.focusX, detector.focusY)
                mScaleFactor *= detector.scaleFactor
                mScaleFactor = maxOf(85f, minOf(250f, mScaleFactor))
                val touched = p
                if (touched != null) {
                    touched.size = mScaleFactor
                }
                invalidate()
            }
            return true
        }
    }
}