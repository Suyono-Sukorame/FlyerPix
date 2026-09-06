package com.flyerpix.editor.editableimageview

import android.view.MotionEvent
import com.flyerpix.editor.editableimageview.figures.Line
import com.flyerpix.editor.editableimageview.paint.Path

class StrategyPaint : StrategyTool() {

    override fun onTouchEvent(event: MotionEvent) {
        val pointerIndex = event.actionIndex
        val id = event.getPointerId(pointerIndex)
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_DOWN -> {
                imageView.addPath(id)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val mId = event.getPointerId(i)
                    imageView.updateLines(mId, event.getX(i), event.getY(i))
                }
                invalidate()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                val path = imageView.pathMap.getValue(id)
                val p = Path(path.lines, path.color, path.strokeWidth)
                imageView.paths.add(p)
                path.lines = ArrayList<Line>()
                path.strokeWidth = 8f
            }
        }
    }
}