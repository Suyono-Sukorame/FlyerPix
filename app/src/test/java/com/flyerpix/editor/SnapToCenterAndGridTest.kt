package com.flyerpix.editor

import com.flyerpix.editor.canvas.calc.SnapCalculator
import com.flyerpix.editor.canvas.calc.SnapCalculator.SnapResult
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk fitur Snap-to-Center & Grid Guidelines (Prompt 30).
 *
 * Memverifikasi:
 *  1. Deteksi kedekatan titik pusat layer terhadap garis tengah horizontal & vertikal kanvas (toleransi 5dp).
 *  2. Penguncian posisi (snapping) koordinat layer tepat ke tengah kanvas.
 *  3. Pengembalian posisi normal ketika berada di luar ambang batas toleransi.
 *  4. Indikator garis panduan aktif (isSnappedX, isSnappedY) bekerja secara independen maupun simultan.
 */
class SnapToCenterAndGridTest {

    private fun snap(
        layerX: Float,
        layerY: Float,
        layerW: Float,
        layerH: Float,
        canvasW: Float,
        canvasH: Float,
        tolerance: Float,
        centerX: Float? = null,
        centerY: Float? = null
    ): SnapResult = if (centerX != null && centerY != null) {
        SnapCalculator.calculate(
            layerX = layerX,
            layerY = layerY,
            layerWidth = layerW,
            layerHeight = layerH,
            canvasWidth = canvasW,
            canvasHeight = canvasH,
            tolerance = tolerance,
            canvasCenterX = centerX,
            canvasCenterY = centerY
        )
    } else {
        SnapCalculator.calculate(
            layerX = layerX,
            layerY = layerY,
            layerWidth = layerW,
            layerHeight = layerH,
            canvasWidth = canvasW,
            canvasHeight = canvasH,
            tolerance = tolerance
        )
    }

    @Test
    fun `layer snaps to horizontal center when within tolerance`() {
        val canvasW = 1000f
        val canvasH = 1000f
        val layerW = 200f
        val layerH = 100f
        val tolerance = 15f // misal 5dp pada layar xhdpi (3x) = 15px

        // Canvas Center: (500, 500)
        // Posisi target tepat di tengah: X = 500 - 100 = 400
        // Layer ditempatkan di X = 405 (layerCenter = 505, selisih 5px <= tolerance 15px)
        // Posisi Y = 200 (layerCenter = 250, selisih 250px > tolerance 15px)
        val result = snap(
            layerX = 405f,
            layerY = 200f,
            layerW = layerW,
            layerH = layerH,
            canvasW = canvasW,
            canvasH = canvasH,
            tolerance = tolerance
        )

        // X harus terkunci tepat ke tengah (400f)
        assertTrue(result.isSnappedX)
        assertEquals(400f, result.snappedX, 0.001f)

        // Y tidak boleh ter-snap
        assertFalse(result.isSnappedY)
        assertEquals(200f, result.snappedY, 0.001f)
    }

    @Test
    fun `layer snaps to vertical center when within tolerance`() {
        val canvasW = 800f
        val canvasH = 1200f
        val layerW = 150f
        val layerH = 80f
        val tolerance = 10f

        // Canvas Center: (400, 600)
        // Posisi target tepat di tengah: Y = 600 - 40 = 560
        // Layer ditempatkan di X = 100 (jauh dari tengah), Y = 566 (selisih 6px <= 10px)
        val result = snap(
            layerX = 100f,
            layerY = 566f,
            layerW = layerW,
            layerH = layerH,
            canvasW = canvasW,
            canvasH = canvasH,
            tolerance = tolerance
        )

        assertFalse(result.isSnappedX)
        assertEquals(100f, result.snappedX, 0.001f)

        assertTrue(result.isSnappedY)
        assertEquals(560f, result.snappedY, 0.001f)
    }

    @Test
    fun `layer snaps to both axes when near intersection of canvas center`() {
        val canvasW = 600f
        val canvasH = 800f
        val layerW = 100f
        val layerH = 60f
        val tolerance = 15f

        // Center: (300, 400)
        // Target snapped pos: X = 250, Y = 370
        // Posisi layer sedikit meleset: X = 253, Y = 368
        val result = snap(
            layerX = 253f,
            layerY = 368f,
            layerW = layerW,
            layerH = layerH,
            canvasW = canvasW,
            canvasH = canvasH,
            tolerance = tolerance
        )

        assertTrue(result.isSnappedX)
        assertTrue(result.isSnappedY)
        assertEquals(250f, result.snappedX, 0.001f)
        assertEquals(370f, result.snappedY, 0.001f)
    }

    @Test
    fun `layer does not snap when outside tolerance threshold`() {
        val canvasW = 1000f
        val canvasH = 1000f
        val layerW = 200f
        val layerH = 100f
        val tolerance = 10f

        // Posisi X = 350 (layerCenter = 450, selisih 50px > 10px)
        // Posisi Y = 300 (layerCenter = 350, selisih 150px > 10px)
        val result = snap(
            layerX = 350f,
            layerY = 300f,
            layerW = layerW,
            layerH = layerH,
            canvasW = canvasW,
            canvasH = canvasH,
            tolerance = tolerance
        )

        assertFalse(result.isSnappedX)
        assertFalse(result.isSnappedY)
        assertEquals(350f, result.snappedX, 0.001f)
        assertEquals(300f, result.snappedY, 0.001f)
    }

    @Test
    fun `layer center aligns exactly with canvas center after snap`() {
        val layer = TextLayer(text = "Snapped Text", x = 0f, y = 0f)
        val (w, h) = layer.getUnwarpedDimensions()
        val canvasW = 1000f
        val canvasH = 1000f

        // Posisikan layer meleset 5px dari garis tengah (berada dalam toleransi 20px)
        layer.x = (canvasW / 2f - w / 2f) + 5f
        layer.y = (canvasH / 2f - h / 2f) - 4f

        val result = snap(
            layerX = layer.x,
            layerY = layer.y,
            layerW = w,
            layerH = h,
            canvasW = canvasW,
            canvasH = canvasH,
            tolerance = 20f
        )

        layer.x = result.snappedX
        layer.y = result.snappedY

        val finalCenterX = layer.x + w / 2f
        val finalCenterY = layer.y + h / 2f

        assertEquals(canvasW / 2f, finalCenterX, 0.001f)
        assertEquals(canvasH / 2f, finalCenterY, 0.001f)
    }

    @Test
    fun `layer snaps to canvas edges when bounds are within tolerance`() {
        val result = SnapCalculator.calculateWithEdges(
            layerX = 795f,
            layerY = 696f,
            boundsLeft = 795f,
            boundsTop = 696f,
            boundsRight = 995f,
            boundsBottom = 796f,
            tolerance = 8f,
            canvasLeft = 0f,
            canvasTop = 0f,
            canvasRight = 1000f,
            canvasBottom = 800f
        )

        assertTrue(result.isSnappedX)
        assertTrue(result.isSnappedY)
        assertEquals(800f, result.snappedX, 0.001f)
        assertEquals(700f, result.snappedY, 0.001f)
        assertEquals(1000f, result.guideX!!, 0.001f)
        assertEquals(800f, result.guideY!!, 0.001f)
    }

    @Test
    fun `edge snap chooses closest target when center is also nearby`() {
        val result = SnapCalculator.calculateWithEdges(
            layerX = 448f,
            layerY = 100f,
            boundsLeft = 448f,
            boundsTop = 100f,
            boundsRight = 548f,
            boundsBottom = 200f,
            tolerance = 55f,
            canvasLeft = 0f,
            canvasTop = 0f,
            canvasRight = 1000f,
            canvasBottom = 800f
        )

        assertTrue(result.isSnappedX)
        assertEquals(450f, result.snappedX, 0.001f)
        assertEquals(500f, result.guideX!!, 0.001f)
        assertFalse(result.isSnappedY)
    }
}
