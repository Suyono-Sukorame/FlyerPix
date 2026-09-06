package com.flyerpix.editor.canvas.calc

/**
 * Kalkulasi murni (stateless) untuk mekanik magnetic snap-to-center
 * beserta garis panduan (Prompt 30, 43).
 */
object SnapCalculator {

    /**
     * Hasil perhitungan penempelan magnetik (Snap Guidelines).
     */
    data class SnapResult(
        val snappedX: Float,
        val snappedY: Float,
        val isSnappedX: Boolean,
        val isSnappedY: Boolean
    )

    /**
     * Menghitung posisi kunci magnetik (Snap Guidelines) ke titik tengah kanvas
     * secara otomatis ketika layer berada dalam rentang toleransi (Prompt 30, 43).
     *
     * @param layerX Posisi X layer saat ini.
     * @param layerY Posisi Y layer saat ini.
     * @param layerWidth Lebar unwarped layer.
     * @param layerHeight Tinggi unwarped layer.
     * @param canvasWidth Lebar kanvas atau lebar viewport.
     * @param canvasHeight Tinggi kanvas atau tinggi viewport.
     * @param tolerance Jarak toleransi snap (misalnya 5dp dalam piksel).
     * @param canvasCenterX Titik tengah horizontal target snap (default: canvasWidth / 2f).
     * @param canvasCenterY Titik tengah vertikal target snap (default: canvasHeight / 2f).
     * @return [SnapResult] yang berisi koordinat ter-snap dan flag apakah snap aktif.
     */
    fun calculate(
        layerX: Float,
        layerY: Float,
        layerWidth: Float,
        layerHeight: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        tolerance: Float,
        canvasCenterX: Float = canvasWidth / 2f,
        canvasCenterY: Float = canvasHeight / 2f
    ): SnapResult {
        val layerCenterX = layerX + layerWidth / 2f
        val layerCenterY = layerY + layerHeight / 2f

        val snapX = kotlin.math.abs(layerCenterX - canvasCenterX) <= tolerance
        val snapY = kotlin.math.abs(layerCenterY - canvasCenterY) <= tolerance

        val newX = if (snapX) canvasCenterX - layerWidth / 2f else layerX
        val newY = if (snapY) canvasCenterY - layerHeight / 2f else layerY

        return SnapResult(newX, newY, snapX, snapY)
    }
}
