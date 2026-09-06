package com.flyerpix.editor.canvas.calc

import android.graphics.RectF

/**
 * Kalkulasi murni (stateless) untuk menghitung area viewport kanvas
 * di dalam area tampilan View, mempertahankan rasio aspek resolusi kanvas
 * secara terpusat (Prompt 43).
 */
object ViewportCalculator {

    /**
     * Menghitung area viewport kanvas ([RectF]) di dalam area tampilan View
     * ([viewWidth] x [viewHeight]) yang mempertahankan rasio aspek resolusi
     * kanvas ([canvasWidth] x [canvasHeight]) secara terpusat.
     *
     * @param viewWidth Lebar tampilan View dalam piksel.
     * @param viewHeight Tinggi tampilan View dalam piksel.
     * @param canvasWidth Lebar resolusi target kanvas dalam piksel.
     * @param canvasHeight Tinggi resolusi target kanvas dalam piksel.
     * @return [RectF] area viewport yang terpusat secara proporsional.
     */
    fun calculate(
        viewWidth: Int,
        viewHeight: Int,
        canvasWidth: Int,
        canvasHeight: Int
    ): RectF {
        if (viewWidth <= 0 || viewHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            return RectF().apply {
                left = 0f
                top = 0f
                right = 0f
                bottom = 0f
            }
        }
        val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
        val canvasAspect = canvasWidth.toFloat() / canvasHeight.toFloat()

        val targetWidth: Float
        val targetHeight: Float

        if (canvasAspect > viewAspect) {
            // Kanvas lebih lebar dari view -> penuhi lebar, letterbox vertikal (atas/bawah)
            targetWidth = viewWidth.toFloat()
            targetHeight = targetWidth / canvasAspect
        } else {
            // Kanvas lebih tinggi atau sama -> penuhi tinggi, letterbox horizontal (kiri/kanan)
            targetHeight = viewHeight.toFloat()
            targetWidth = targetHeight * canvasAspect
        }

        val left = (viewWidth - targetWidth) / 2f
        val top = (viewHeight - targetHeight) / 2f
        val right = left + targetWidth
        val bottom = top + targetHeight

        return RectF().apply {
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        }
    }
}
