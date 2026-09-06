package com.flyerpix.editor.editableimageview.figures

open class Square(
    override var x: Float,
    override var y: Float,
    var side: Double,
    override var color: Float
) : Polygon {

    var rotation: Float = 0f

    override var size: Float
        get() = side.toFloat()
        set(value) {
            side = value.toDouble()
        }
}