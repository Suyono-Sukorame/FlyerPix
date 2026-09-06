package com.flyerpix.editor.canvas.gesture

import android.view.MotionEvent
import kotlin.math.atan2

/**
 * Helper detector untuk mendeteksi gestur rotasi dua jari pada kanvas.
 * Menghitung selisih sudut rotasi (delta angle) dalam derajat secara real-time dan mulus.
 */
class RotationGestureDetector(private val listener: OnRotationGestureListener) {

    interface OnRotationGestureListener {
        /**
         * Dipanggil ketika terjadi perputaran dua jari.
         *
         * @param detector Instance [RotationGestureDetector].
         * @param deltaAngle Selisih perubahan sudut rotasi dalam satuan derajat (-180° hingga +180°).
         * @return true jika event berhasil ditangani.
         */
        fun onRotation(detector: RotationGestureDetector, deltaAngle: Float): Boolean

        /**
         * Dipanggil saat gestur rotasi dimulai (dua jari terdeteksi menyentuh layar).
         */
        fun onRotationBegin(detector: RotationGestureDetector): Boolean = true

        /**
         * Dipanggil saat salah satu atau kedua jari diangkat dari layar.
         */
        fun onRotationEnd(detector: RotationGestureDetector) {}
    }

    private var ptrId1: Int = INVALID_POINTER_ID
    private var ptrId2: Int = INVALID_POINTER_ID
    private var previousAngle: Float = 0f

    /**
     * Menandakan apakah gestur rotasi saat ini sedang berlangsung aktif.
     */
    var isInProgress: Boolean = false
        private set

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ptrId1 = event.getPointerId(event.actionIndex)
                ptrId2 = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ptrId2 == INVALID_POINTER_ID && event.pointerCount >= 2) {
                    ptrId1 = event.getPointerId(0)
                    ptrId2 = event.getPointerId(1)
                    previousAngle = calculateAngle(event)
                    isInProgress = listener.onRotationBegin(this)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isInProgress && ptrId1 != INVALID_POINTER_ID && ptrId2 != INVALID_POINTER_ID) {
                    val index1 = event.findPointerIndex(ptrId1)
                    val index2 = event.findPointerIndex(ptrId2)

                    if (index1 != -1 && index2 != -1) {
                        val currentAngle = calculateAngle(event)
                        var deltaAngle = currentAngle - previousAngle

                        // Normalisasi delta angle agar tidak melompat di perbatasan -180° / +180°
                        if (deltaAngle > 180f) {
                            deltaAngle -= 360f
                        } else if (deltaAngle < -180f) {
                            deltaAngle += 360f
                        }

                        previousAngle = currentAngle
                        listener.onRotation(this, deltaAngle)
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)
                if (pointerId == ptrId1 || pointerId == ptrId2) {
                    if (isInProgress) {
                        isInProgress = false
                        listener.onRotationEnd(this)
                    }
                    ptrId1 = INVALID_POINTER_ID
                    ptrId2 = INVALID_POINTER_ID
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isInProgress) {
                    isInProgress = false
                    listener.onRotationEnd(this)
                }
                ptrId1 = INVALID_POINTER_ID
                ptrId2 = INVALID_POINTER_ID
            }
        }
        return true
    }

    private fun calculateAngle(event: MotionEvent): Float {
        val index1 = event.findPointerIndex(ptrId1)
        val index2 = event.findPointerIndex(ptrId2)
        if (index1 == -1 || index2 == -1) return 0f

        val x1 = event.getX(index1)
        val y1 = event.getY(index1)
        val x2 = event.getX(index2)
        val y2 = event.getY(index2)

        val deltaX = (x2 - x1).toDouble()
        val deltaY = (y2 - y1).toDouble()
        val radians = atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
    }
}
