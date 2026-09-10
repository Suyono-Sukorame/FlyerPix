package com.flyerpix.editor.ui.dialog

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Panel warna 2D modern: kotak Saturation–Brightness + strip Hue di samping.
 *
 * Digunakan oleh [SolidColorEditorView] menggantikan slider Hue/Sat/Brightness
 * yang terpisah-pisah. Interaksi:
 *  - [onChange](hue, sat, value) dipanggil terus-menerus saat digeser.
 *  - [onCommit] dipanggil saat gerakan berakhir (untuk simpan ke Recent).
 *
 * Koordinat: value = 1 (cerah) di tepi atas, sat = 1 (murni) di tepi kanan.
 */
class SvHuePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var hue: Float = 0f
        private set
    var sat: Float = 0f
        private set
    var value: Float = 1f
        private set

    /** (hue 0..360, sat 0..1, value 0..1) */
    var onChange: ((Float, Float, Float) -> Unit)? = null

    /** Dipanggil saat sentuhan dilepas — biasanya untuk commit Recent. */
    var onCommit: (() -> Unit)? = null

    private val dp = resources.displayMetrics.density

    private val PAD = (4 * dp).toInt()
    private val STRIP_W = (22 * dp).toInt()
    private val GAP = (14 * dp).toInt()
    private val MAX_SQUARE = (200 * dp).toInt()
    private val THUMB_R = (7 * dp).toInt()
    private val CORNER_R = (12 * dp).toInt()

    private val svRect = RectF()
    private val stripRect = RectF()
    private var square = 0

    private var suppressCallbacks = false

    private val hueStripColors: IntArray by lazy {
        val steps = 61
        val colors = IntArray(steps)
        for (i in 0 until steps) {
            colors[i] = Color.HSVToColor(floatArrayOf(i * 360f / (steps - 1), 1f, 1f))
        }
        colors
    }

    private val paintSvBase = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintSvOverlay = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintStrip = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintThumbFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val paintThumbBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * dp
        color = 0xFF444444.toInt()
    }
    private val paintStripBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1 * dp
        color = 0x33888888
    }

    init {
        isClickable = true
        isFocusable = true
    }

    fun setHsv(h: Float, s: Float, v: Float) {
        suppressCallbacks = true
        hue = ((h % 360f) + 360f) % 360f
        sat = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)
        suppressCallbacks = false
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val available = w - PAD * 2 - STRIP_W - GAP
        square = min(available, MAX_SQUARE).coerceAtLeast((96 * dp).toInt())
        setMeasuredDimension(w, (PAD * 2 + square).toInt())

        val svLeft = (w - square) / 2f
        svRect.set(svLeft, PAD.toFloat(), svLeft + square, (PAD + square).toFloat())
        stripRect.set(svRect.right + GAP, PAD.toFloat(), svRect.right + GAP + STRIP_W, svRect.bottom)
    }

    override fun onDraw(canvas: Canvas) {
        // Kotak SV: saturasi kiri->kanan dari hue murni, gelap ke bawah.
        val hueColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        paintSvBase.shader = LinearGradient(
            svRect.left, 0f, svRect.right, 0f,
            Color.WHITE, hueColor, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(svRect, CORNER_R.toFloat(), CORNER_R.toFloat(), paintSvBase)
        paintSvOverlay.shader = LinearGradient(
            0f, svRect.top, 0f, svRect.bottom,
            Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(svRect, CORNER_R.toFloat(), CORNER_R.toFloat(), paintSvOverlay)

        // Strip Hue vertikal.
        paintStrip.shader = LinearGradient(
            0f, stripRect.top, 0f, stripRect.bottom,
            hueStripColors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(stripRect, 4 * dp, 4 * dp, paintStrip)
        canvas.drawRoundRect(stripRect, 4 * dp, 4 * dp, paintStripBorder)

        // Thumb kotak SV.
        val tx = svRect.left + sat * square
        val ty = svRect.top + (1f - value) * square
        canvas.drawCircle(tx, ty, THUMB_R.toFloat(), paintThumbFill)
        canvas.drawCircle(tx, ty, THUMB_R.toFloat(), paintThumbBorder)

        // Indikator posisi Hue.
        val hy = stripRect.top + (hue / 360f) * square
        val ring = RectF(
            stripRect.left - 3 * dp,
            (hy - THUMB_R * 0.8f),
            stripRect.right + 3 * dp,
            (hy + THUMB_R * 0.8f)
        )
        canvas.drawRoundRect(ring, 3 * dp, 3 * dp, paintThumbFill)
        canvas.drawRoundRect(ring, 3 * dp, 3 * dp, paintThumbBorder)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                applyTouch(x, y)
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onCommit?.invoke()
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun applyTouch(x: Float, y: Float) {
        val pad = PAD.toFloat()
        val inSvBand = x >= svRect.left - GAP && x <= svRect.right + GAP
        val inStripBand = x >= stripRect.left - GAP && x <= stripRect.right + GAP

        if ((inStripBand && !inSvBand) || (inStripBand && x > svRect.right + GAP / 2f)) {
            val t = ((y - pad) / square).coerceIn(0f, 1f)
            hue = (t * 360f + 360f) % 360f
        } else {
            sat = ((x - svRect.left) / square).coerceIn(0f, 1f)
            value = (1f - (y - pad) / square).coerceIn(0f, 1f)
        }
        if (!suppressCallbacks) onChange?.invoke(hue, sat, value)
        invalidate()
    }
}