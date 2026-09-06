package com.flyerpix.editor.imageedit

import kotlin.math.max
import kotlin.math.min

enum class CropShape { RECTANGLE, CIRCLE }

enum class DragCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

data class RectNorm(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(right > left) { "right > left" }
        require(bottom > top) { "bottom > top" }
    }

    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun normalized(): RectNorm {
        val l = min(left, right).coerceIn(0f, 1f)
        val t = min(top, bottom).coerceIn(0f, 1f)
        val r = max(left, right).coerceIn(0f, 1f)
        val b = max(top, bottom).coerceIn(0f, 1f)
        val width = min(max(r - l, 0.001f), 1f)
        val height = min(max(b - t, 0.001f), 1f)
        return if (width >= 1f || height >= 1f) {
            RectNorm(0f, 0f, width.coerceAtMost(1f), height.coerceAtMost(1f))
        } else {
            RectNorm(l, t, l + width, t + height)
        }
    }
}

data class CircleNorm(
    val centerX: Float,
    val centerY: Float,
    val radius: Float
) {
    fun normalized(): CircleNorm {
        val r = radius.coerceIn(0.02f, 0.5f)
        val cx = centerX.coerceIn(r, 1f - r)
        val cy = centerY.coerceIn(r, 1f - r)
        return CircleNorm(cx, cy, r)
    }
}

class CropSelection {
    var shape: CropShape = CropShape.RECTANGLE
        private set
    var isLocked: Boolean = false

    var rect: RectNorm = RectNorm(0.05f, 0.05f, 0.95f, 0.95f)
    var circle: CircleNorm = CircleNorm(0.5f, 0.5f, 0.4f)

    fun reset() {
        shape = CropShape.RECTANGLE
        isLocked = false
        rect = RectNorm(0.05f, 0.05f, 0.95f, 0.95f)
        circle = CircleNorm(0.5f, 0.5f, 0.4f)
    }

    fun toggleShape(): CropShape {
        shape = if (shape == CropShape.RECTANGLE) CropShape.CIRCLE else CropShape.RECTANGLE
        return shape
    }
}

object CropMath {

    fun applyShapeLock(r: RectNorm, dragged: DragCorner): RectNorm {
        val ox = if (dragged == DragCorner.TOP_LEFT || dragged == DragCorner.BOTTOM_LEFT) r.right else r.left
        val oy = if (dragged == DragCorner.TOP_LEFT || dragged == DragCorner.TOP_RIGHT) r.bottom else r.top
        val xPlus = dragged == DragCorner.TOP_LEFT || dragged == DragCorner.BOTTOM_LEFT
        val yPlus = dragged == DragCorner.TOP_LEFT || dragged == DragCorner.TOP_RIGHT
        val maxSideX = if (xPlus) 1f - ox else ox
        val maxSideY = if (yPlus) 1f - oy else oy
        val side = min(min(r.width, r.height), min(maxSideX, maxSideY)).coerceIn(0.02f, 1f)
        val l = if (xPlus) ox - side else ox
        val t = if (yPlus) oy - side else oy
        return RectNorm(l, t, l + side, t + side).normalized()
    }

    fun rotateCW(r: RectNorm): RectNorm =
        RectNorm(1f - r.bottom, r.left, 1f - r.top, r.right).normalized()

    fun rotateCW(c: CircleNorm): CircleNorm =
        CircleNorm(1f - c.centerY, c.centerX, c.radius).normalized()

    fun rotateCCW(r: RectNorm): RectNorm =
        RectNorm(r.top, 1f - r.right, r.bottom, 1f - r.left).normalized()

    fun rotateCCW(c: CircleNorm): CircleNorm =
        CircleNorm(c.centerY, 1f - c.centerX, c.radius).normalized()

    fun flipH(r: RectNorm): RectNorm =
        RectNorm(1f - r.right, r.top, 1f - r.left, r.bottom).normalized()

    fun flipH(c: CircleNorm): CircleNorm =
        CircleNorm(1f - c.centerX, c.centerY, c.radius).normalized()

    fun flipV(r: RectNorm): RectNorm =
        RectNorm(r.left, 1f - r.bottom, r.right, 1f - r.top).normalized()

    fun flipV(c: CircleNorm): CircleNorm =
        CircleNorm(c.centerX, 1f - c.centerY, c.radius).normalized()
}