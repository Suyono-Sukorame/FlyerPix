package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.PixelCanvasView.TouchState
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk gestur interaksi Sentuhan pada Handle Rotate (Prompt 28).
 *
 * Memverifikasi:
 *  1. State TouchState.DRAGGING_ROTATE_HANDLE tersedia
 *  2. Perhitungan sudut Math.atan2 dari titik pusat layer ke koordinat sentuh jari (calculateTouchAngle)
 *  3. Pembaruan nilai rotasi layer secara mulus bebas dari lonjakan batas ±180° (calculateUpdatedRotation)
 *  4. Normalisasi sudut selalu berada dalam rentang [0f, 360f)
 */
class RotateHandleTouchTest {

    @Test
    fun `TouchState enum contains DRAGGING_ROTATE_HANDLE`() {
        val state = TouchState.valueOf("DRAGGING_ROTATE_HANDLE")
        assertEquals(TouchState.DRAGGING_ROTATE_HANDLE, state)
    }

    @Test
    fun `calculateTouchAngle computes exact angles with Math atan2`() {
        val cx = 200f
        val cy = 300f

        // Arah kanan: 0 derajat
        val angleRight = PixelCanvasView.calculateTouchAngle(cx, cy, cx + 100f, cy)
        assertEquals(0f, angleRight, 0.001f)

        // Arah bawah: 90 derajat
        val angleDown = PixelCanvasView.calculateTouchAngle(cx, cy, cx, cy + 100f)
        assertEquals(90f, angleDown, 0.001f)

        // Arah kiri: 180 derajat
        val angleLeft = PixelCanvasView.calculateTouchAngle(cx, cy, cx - 100f, cy)
        assertEquals(180f, Math.abs(angleLeft), 0.001f)

        // Arah atas: -90 derajat
        val angleUp = PixelCanvasView.calculateTouchAngle(cx, cy, cx, cy - 100f)
        assertEquals(-90f, angleUp, 0.001f)

        // Diagonal kanan-bawah: 45 derajat
        val angleDiag = PixelCanvasView.calculateTouchAngle(cx, cy, cx + 50f, cy + 50f)
        assertEquals(45f, angleDiag, 0.001f)
    }

    @Test
    fun `clockwise movement smoothly increases layer rotation`() {
        val initialLayerRotation = 45f
        val prevAngle = 10f
        val currentAngle = 25f // Rotasi 15 derajat searah jarum jam

        val newRotation = PixelCanvasView.calculateUpdatedRotation(initialLayerRotation, prevAngle, currentAngle)
        assertEquals(60f, newRotation, 0.001f)
    }

    @Test
    fun `counter-clockwise movement smoothly decreases layer rotation`() {
        val initialLayerRotation = 45f
        val prevAngle = 30f
        val currentAngle = 10f // Rotasi 20 derajat berlawanan jarum jam

        val newRotation = PixelCanvasView.calculateUpdatedRotation(initialLayerRotation, prevAngle, currentAngle)
        assertEquals(25f, newRotation, 0.001f)
    }

    @Test
    fun `smooth transition across 180 degree boundary in clockwise direction`() {
        // Jari melintasi garis perbatasan dari +178 derajat ke -178 derajat (+4 derajat searah jarum jam)
        val initialLayerRotation = 100f
        val prevAngle = 178f
        val currentAngle = -178f

        val newRotation = PixelCanvasView.calculateUpdatedRotation(initialLayerRotation, prevAngle, currentAngle)
        assertEquals(104f, newRotation, 0.001f)
    }

    @Test
    fun `smooth transition across 180 degree boundary in counter-clockwise direction`() {
        // Jari melintasi garis perbatasan dari -178 derajat ke +178 derajat (-4 derajat berlawanan jarum jam)
        val initialLayerRotation = 100f
        val prevAngle = -178f
        val currentAngle = 178f

        val newRotation = PixelCanvasView.calculateUpdatedRotation(initialLayerRotation, prevAngle, currentAngle)
        assertEquals(96f, newRotation, 0.001f)
    }

    @Test
    fun `rotation wraps within 0 to 360 range on underflow and overflow`() {
        // Underflow: 2 derajat - 10 derajat -> 354 derajat
        val underflowRotation = PixelCanvasView.calculateUpdatedRotation(2f, 20f, 10f)
        assertEquals(352f, underflowRotation, 0.001f)

        // Overflow: 355 derajat + 10 derajat -> 5 derajat
        val overflowRotation = PixelCanvasView.calculateUpdatedRotation(355f, 10f, 20f)
        assertEquals(5f, overflowRotation, 0.001f)
    }

    @Test
    fun `text layer rotation updates accurately`() {
        val layer = TextLayer(text = "Rotate Test", x = 100f, y = 100f)
        assertEquals(0f, layer.rotation, 0.001f)

        layer.rotation = 90f
        assertEquals(90f, layer.rotation, 0.001f)

        val (w, h) = layer.getUnwarpedDimensions()
        assertTrue(w > 0f)
        assertTrue(h > 0f)
    }
}
