package com.flyerpix.editor.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Overlay kaca pembesar (magnifier) lingkaran untuk fitur Eyedropper.
 *
 * Menampilkan area zoom di sekitar titik sentuh pengguna, garis silang (crosshair),
 * border putih, dan preview warna pixel yang diambil.
 *
 * Digunakan oleh [EditorActivity] — diposisikan di atas kanvas saat eyedropper aktif.
 */
class EyedropperOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Bitmap canvas yang di-capture untuk dibaca pixelnya. */
    var canvasBitmap: Bitmap? = null

    /** Posisi X titik sentuh dalam koordinat kanvas (bukan koordinat overlay). */
    var touchCanvasX: Float = 0f

    /** Posisi Y titik sentuh dalam koordinat kanvas. */
    var touchCanvasY: Float = 0f

    /** Warna pixel yang sedang diambil (diupdate setiap kali touch bergerak). */
    var sampledColor: Int = Color.TRANSPARENT

    /** Apakah overlay aktif dan harus digambar. */
    var isActive: Boolean = false

    private val magnifierRadius = 72f * resources.displayMetrics.density
    private val magnifierCenterOffset = -100f * resources.displayMetrics.density  // offset ke atas
    private val zoomFactor = 2.5f

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        color = Color.WHITE
        setShadowLayer(4f, 0f, 2f, 0x66000000)
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = Color.WHITE
    }

    private val crosshairShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = 0x66000000
    }

    private val colorPreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val colorBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        color = Color.WHITE
    }

    private val clipPath = Path()

    override fun onDraw(canvas: Canvas) {
        if (!isActive) return

        val bmp = canvasBitmap ?: return
        if (bmp.isRecycled) return

        val magCx = width / 2f
        val magCy = height / 2f + magnifierCenterOffset

        // Clip bulat
        clipPath.reset()
        clipPath.addCircle(magCx, magCy, magnifierRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)

        // Gambar bitmap yang di-zoom
        val srcSize = magnifierRadius * 2f / zoomFactor
        val srcRect = Rect(
            (touchCanvasX - srcSize / 2f).toInt(),
            (touchCanvasY - srcSize / 2f).toInt(),
            (touchCanvasX + srcSize / 2f).toInt(),
            (touchCanvasY + srcSize / 2f).toInt()
        )
        val dstRect = RectF(
            magCx - magnifierRadius,
            magCy - magnifierRadius,
            magCx + magnifierRadius,
            magCy + magnifierRadius
        )
        canvas.drawBitmap(bmp, srcRect, dstRect, null)

        canvas.restore()

        // Border lingkaran
        canvas.drawCircle(magCx, magCy, magnifierRadius, borderPaint)

        // Crosshair
        val crossSize = 16f * resources.displayMetrics.density
        canvas.drawLine(magCx - crossSize, magCy, magCx + crossSize, magCy, crosshairShadowPaint)
        canvas.drawLine(magCx, magCy - crossSize, magCx, magCy + crossSize, crosshairShadowPaint)
        canvas.drawLine(magCx - crossSize, magCy, magCx + crossSize, magCy, crosshairPaint)
        canvas.drawLine(magCx, magCy - crossSize, magCx, magCy + crossSize, crosshairPaint)

        // Preview warna di bawah magnifier
        val previewRadius = 18f * resources.displayMetrics.density
        val previewCy = magCy + magnifierRadius + previewRadius + 12f * resources.displayMetrics.density
        colorPreviewPaint.color = sampledColor
        canvas.drawCircle(magCx, previewCy, previewRadius, colorPreviewPaint)
        canvas.drawCircle(magCx, previewCy, previewRadius, colorBorderPaint)

        // Hex label di bawah preview
        val hex = String.format("#%06X", 0xFFFFFF and sampledColor)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f * resources.displayMetrics.density
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, Color.BLACK)
        }
        canvas.drawText(hex, magCx, previewCy + previewRadius + 16f * resources.displayMetrics.density, textPaint)
    }

    /**
     * Memperbarui posisi sentuh dan warna yang diambil, lalu request redraw.
     */
    fun update(touchX: Float, touchY: Float, color: Int) {
        touchCanvasX = touchX
        touchCanvasY = touchY
        sampledColor = color
        invalidate()
    }
}
