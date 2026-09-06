package com.flyerpix.editor.editableimageview

import android.view.MotionEvent

class MyContext(var editableImageView: EditableImageView) {

    var strategyTool: StrategyTool? = null
        set(value) {
            value?.imageView = editableImageView
            field = value
        }

    fun onTouchEvent(event: MotionEvent) {
        strategyTool?.onTouchEvent(event)
    }
}