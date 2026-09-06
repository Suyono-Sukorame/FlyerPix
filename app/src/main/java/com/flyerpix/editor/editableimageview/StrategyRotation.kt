package com.flyerpix.editor.editableimageview

import android.util.Log
import android.view.MotionEvent
import com.flyerpix.editor.editableimageview.figures.Square

class StrategyRotation : StrategyTool() {

    override fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
                val p = imageView.getTouchedPolygon(event.x, event.y)
                if (p != null && p is Square) {
                    val s = p
                    val size = s.size
                    if (event.pointerCount == 2) {
                        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
                        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
                        val radians = Math.atan2(deltaY, deltaX)
                        Log.d(
                            "Rotation",
                            "$deltaX ## $deltaY ## $radians ## " + Math.toDegrees(radians)
                        )
                        s.rotation = Math.toDegrees(radians).toFloat()
                        s.size = size
                    }
                }
            }
        }
        invalidate()
    }
}