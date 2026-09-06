package com.flyerpix.editor.editableimageview

import android.view.MotionEvent
import com.flyerpix.editor.editableimageview.figures.Line

class StrategyLine : StrategyTool() {

    override fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                val line = Line(x, y, x, y, imageView.currentColor)
                imageView.lines.add(line)
            }
            MotionEvent.ACTION_MOVE -> {
                val i = imageView.lines.size - 1
                val xf = event.x
                val yf = event.y
                if (imageView.lines.size > 0) {
                    val mLine = imageView.lines[i]
                    mLine.xf = xf
                    mLine.yf = yf
                    imageView.lines[i] = mLine
                }
            }
        }
        invalidate()
    }
}