package com.flyerpix.editor

import com.flyerpix.editor.imageedit.CircleNorm
import com.flyerpix.editor.imageedit.CropMath
import com.flyerpix.editor.imageedit.DragCorner
import com.flyerpix.editor.imageedit.RectNorm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test untuk matematika Seleksi Crop pada layar Pre-Edit Gambar.
 *
 * Menguji:
 * 1. Rotasi 90° CW/CCW pada seleksi persegi & lingkaran.
 * 2. Flip cermin horizontal & vertikal.
 * 3. Round-trip transformasi kembali ke kondisi semula (identitas).
 * 4. Fitur Kunci Rasio (shape lock) menghasilkan seleksi persegi di dalam [0,1].
 */
class ImagePreEditMathTest {

    private val tolerance = 0.001f

    private fun assertRect(expected: RectNorm, actual: RectNorm) {
        assertEquals(expected.left, actual.left, tolerance)
        assertEquals(expected.top, actual.top, tolerance)
        assertEquals(expected.right, actual.right, tolerance)
        assertEquals(expected.bottom, actual.bottom, tolerance)
    }

    private fun assertCircle(expected: CircleNorm, actual: CircleNorm) {
        assertEquals(expected.centerX, actual.centerX, tolerance)
        assertEquals(expected.centerY, actual.centerY, tolerance)
        assertEquals(expected.radius, actual.radius, tolerance)
    }

    @Test
    fun rotateCW_then_rotateCCW_restoresRect() {
        val original = RectNorm(0.2f, 0.3f, 0.7f, 0.8f)
        val restored = CropMath.rotateCCW(CropMath.rotateCW(original))
        assertRect(original, restored)
    }

    @Test
    fun rotateCW_mapsRectCorrectly() {
        val original = RectNorm(0.3f, 0.2f, 0.8f, 0.7f)
        val rotated = CropMath.rotateCW(original)
        assertRect(RectNorm(0.3f, 0.3f, 0.8f, 0.8f), rotated)
    }

    @Test
    fun rotateFourTimes_returnsToStartRect() {
        val original = RectNorm(0.1f, 0.4f, 0.6f, 0.9f)
        var current = original
        repeat(4) { current = CropMath.rotateCW(current) }
        assertRect(original, current)
    }

    @Test
    fun rotateCW_mapsCircleCenter() {
        val original = CircleNorm(0.4f, 0.3f, 0.25f)
        val rotated = CropMath.rotateCW(original)
        assertCircle(CircleNorm(0.7f, 0.4f, 0.25f), rotated)
    }

    @Test
    fun flipHorizontal_twice_isIdentityRect() {
        val original = RectNorm(0.2f, 0.1f, 0.75f, 0.6f)
        val restored = CropMath.flipH(CropMath.flipH(original))
        assertRect(original, restored)
    }

    @Test
    fun flipVertical_twice_isIdentityCircle() {
        val original = CircleNorm(0.3f, 0.6f, 0.2f)
        val restored = CropMath.flipV(CropMath.flipV(original))
        assertCircle(original, restored)
    }

    @Test
    fun flipHorizontal_mirrorsRect() {
        val original = RectNorm(0.2f, 0.2f, 0.5f, 0.7f)
        val flipped = CropMath.flipH(original)
        assertRect(RectNorm(0.5f, 0.2f, 0.8f, 0.7f), flipped)
    }

    @Test
    fun shapeLock_producesSquareStayingInsideCanvas() {
        val dragged = RectNorm(0.2f, 0.5f, 0.6f, 0.9f)
        val locked = CropMath.applyShapeLock(dragged, DragCorner.TOP_LEFT)
        assertEquals(locked.width, locked.height, tolerance)
        assertTrue(locked.left >= 0f)
        assertTrue(locked.top >= 0f)
        assertTrue(locked.right <= 1f)
        assertTrue(locked.bottom <= 1f)
    }

    @Test
    fun shapeLock_snapsToOppositeCorner() {
        val dragged = RectNorm(0.2f, 0.5f, 0.6f, 0.9f)
        val locked = CropMath.applyShapeLock(dragged, DragCorner.TOP_LEFT)
        assertRect(RectNorm(0.5f, 0.8f, 0.6f, 0.9f), locked)
    }

    @Test
    fun shapeLock_sideCoercesWithinBounds() {
        val dragged = RectNorm(0.05f, 0.05f, 0.95f, 0.95f)
        val locked = CropMath.applyShapeLock(dragged, DragCorner.BOTTOM_LEFT)
        assertTrue(locked.width >= 0.02f)
        assertTrue(locked.width <= 1f)
        assertTrue(locked.left >= 0f)
        assertTrue(locked.top >= 0f)
        assertTrue(locked.right <= 1f)
        assertTrue(locked.bottom <= 1f)
    }
}