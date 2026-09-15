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

    var rect: RectNorm = RectNorm(0f, 0f, 1f, 1f)
    var circle: CircleNorm = CircleNorm(0.5f, 0.5f, 0.4f)

    fun reset() {
        shape = CropShape.RECTANGLE
        isLocked = false
        rect = RectNorm(0f, 0f, 1f, 1f)
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

    /**
     * Preset rasio aspek yang memenuhi kanvas [0,1]² (memuat area seluas mungkin
     * sambil menjaga rasio width:height). Persegi 1:1 menghasilkan kotak penuh.
     * 
     * NOTE: This function assumes the image canvas [0,1]² uses normalized image coordinates,
     * where (0,0) is top-left and (1,1) is bottom-right of the IMAGE (not the view).
     * For a rectangular image (e.g., 600x800), the [0,1]² space represents the actual
     * image dimensions, so aspect ratio calculations are always relative to the image
     * itself, not the view dimensions.
     */
    fun fitRatioToCanvas(ratioW: Int, ratioH: Int): RectNorm {
        val rw = ratioW.coerceAtLeast(1).toFloat()
        val rh = ratioH.coerceAtLeast(1).toFloat()
        var w = 1f
        var h = w * rh / rw
        if (h > 1f) {
            h = 1f
            w = h * rw / rh
        }
        val l = (1f - w) / 2f
        val t = (1f - h) / 2f
        return RectNorm(l, t, l + w, t + h)
    }

    /**
     * Preset rasio aspek yang memenuhi kanvas [0,1]² dengan mempertimbangkan
     * aspect ratio gambar. Ini memperbaiki masalah di mana 1:1 selection
     * pada gambar rectangular (600x800) tampil sebagai 3:4 bukan 1:1.
     * 
     * The key insight: normalized coords [0,1]² map to actual image pixels [0,imgWidth] x [0,imgHeight].
     * To get a desired pixel aspect ratio (e.g., 1:1 square), we need to adjust the 
     * normalized ratio to account for the image's own aspect ratio.
     * 
     * For a 600x800 image wanting a 1:1 square:
     * - We need: norm_width * 600 = norm_height * 800
     * - So: norm_width / norm_height = 800/600 = 4/3
     * 
     * @param ratioW desired pixel width ratio (e.g., 1 for square)
     * @param ratioH desired pixel height ratio (e.g., 1 for square)
     * @param imgWidth actual bitmap width in pixels
     * @param imgHeight actual bitmap height in pixels
     * @return normalized rect that maintains the desired pixel ratio within the image bounds
     */
    fun fitRatioToCanvasAdjusted(ratioW: Int, ratioH: Int, imgWidth: Int, imgHeight: Int): RectNorm {
        val rw = ratioW.coerceAtLeast(1).toFloat()
        val rh = ratioH.coerceAtLeast(1).toFloat()
        
        // Adjust desired ratio to account for image aspect ratio
        // desiredPixelRatio = rw / rh
        // imageAspect = imgWidth / imgHeight
        // normalizedRatio = desiredPixelRatio * (imgHeight / imgWidth)
        val adjustedRatioW = rw * imgHeight
        val adjustedRatioH = rh * imgWidth
        
        // Now use standard fitting logic with adjusted ratio
        var w = 1f
        var h = w * adjustedRatioH / adjustedRatioW
        if (h > 1f) {
            h = 1f
            w = h * adjustedRatioW / adjustedRatioH
        }

        val l = (1f - w) / 2f
        val t = (1f - h) / 2f
        return RectNorm(l, t, l + w, t + h)
    }

    /**
     * Kunci rasio saat menyeret sudut: menjaga rasio [ratio] (null → persegi via
     * [applyShapeLock]) dengan anchor pada sudut yang tidak diseret.
     */
    fun applyRatioLock(r: RectNorm, dragged: DragCorner, ratio: Pair<Int, Int>?): RectNorm {
        if (ratio == null) return applyShapeLock(r, dragged)
        val ox = if (dragged == DragCorner.TOP_LEFT || dragged == DragCorner.BOTTOM_LEFT) r.right else r.left
        val oy = if (dragged == DragCorner.TOP_LEFT || dragged == DragCorner.TOP_RIGHT) r.bottom else r.top
        val xPlus = dragged == DragCorner.TOP_LEFT || dragged == DragCorner.BOTTOM_LEFT
        val yPlus = dragged == DragCorner.TOP_LEFT || dragged == DragCorner.TOP_RIGHT
        val rw = ratio.first.coerceAtLeast(1).toFloat()
        val rh = ratio.second.coerceAtLeast(1).toFloat()
        val fitX = if (xPlus) 1f - ox else ox
        val fitY = if (yPlus) 1f - oy else oy
        val minSize = 0.02f
        var w = fitX
        var h = w * rh / rw
        if (h > fitY) {
            h = fitY
            w = h * rw / rh
        }
        if (w > fitX) {
            w = fitX
            h = w * rh / rw
        }
        if (w < minSize) {
            w = minSize
            h = w * rh / rw
            if (h > fitY) {
                h = fitY
                w = h * rw / rh
            }
        }
        val l = if (xPlus) ox - w else ox
        val t = if (yPlus) oy - h else oy
        return RectNorm(l, t, l + w, t + h).normalized()
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