package com.flyerpix.editor.canvas.calc

/**
 * Kalkulasi murni (stateless) untuk sudut sentuh & rotasi layer (Prompt 28).
 */
object RotationCalculator {

    /**
     * Menghitung sudut dalam derajat (-180° s/d +180°) dari titik pusat
     * (cx, cy) ke titik sentuh (touchX, touchY) menggunakan Math.atan2.
     */
    fun touchAngle(cx: Float, cy: Float, touchX: Float, touchY: Float): Float {
        val rad = Math.atan2((touchY - cy).toDouble(), (touchX - cx).toDouble())
        return Math.toDegrees(rad).toFloat()
    }

    /**
     * Menghitung sudut rotasi layer baru secara mulus berdasarkan pergerakan sudut
     * sentuh jari, dengan normalisasi 0°..360° dan bebas dari lonjakan di perbatasan
     * ±180° (Prompt 28).
     */
    fun updatedRotation(
        initialOrPrevLayerRotation: Float,
        prevAngle: Float,
        currentAngle: Float
    ): Float {
        var deltaAngle = currentAngle - prevAngle
        if (deltaAngle > 180f) {
            deltaAngle -= 360f
        } else if (deltaAngle < -180f) {
            deltaAngle += 360f
        }
        var newRotation = (initialOrPrevLayerRotation + deltaAngle) % 360f
        if (newRotation < 0f) {
            newRotation += 360f
        }
        return newRotation
    }
}
