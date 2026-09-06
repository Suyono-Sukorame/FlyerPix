package com.flyerpix.editor.editableimageview.figures

import android.graphics.Bitmap
import android.graphics.Color

class CropSquare(x: Float, y: Float, side: Double) : Square(x, y, side, Color.BLACK.toFloat()) {
    var bitmap: Bitmap? = null
}