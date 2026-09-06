package com.flyerpix.editor.editableimageview.figures

class Circle(
    override var x: Float,
    override var y: Float,
    var radius: Float,
    override var color: Float
) : Polygon {

    override var size: Float
        get() = radius
        set(value) {
            radius = value
        }
}