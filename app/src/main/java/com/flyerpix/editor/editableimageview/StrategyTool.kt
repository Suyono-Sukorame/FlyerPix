package com.flyerpix.editor.editableimageview

import android.view.MotionEvent

abstract class StrategyTool {
    lateinit var imageView: EditableImageView

    fun invalidate() {
        imageView.invalidate()
    }

    abstract fun onTouchEvent(event: MotionEvent)
}