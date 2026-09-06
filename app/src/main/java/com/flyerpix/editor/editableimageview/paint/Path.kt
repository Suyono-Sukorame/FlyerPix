package com.flyerpix.editor.editableimageview.paint

import com.flyerpix.editor.editableimageview.figures.Line

class Path(var lines: ArrayList<Line>, var color: Int = -1, var strokeWidth: Float = 0f) {

    constructor() : this(ArrayList())
    constructor(color: Int, strokeWidth: Float) : this(ArrayList(), color, strokeWidth)
}