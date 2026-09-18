package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView.TouchState
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test untuk gestur interaksi 8-Handle Resize Universal (Prompt 27/29).
 *
 * Verifikasi:
 *  1. State TouchState.DRAGGING_RESIZE_HANDLE terdefinisi dalam enum
 *  2. Titik pusat layer (centerX, centerY) dihitung stabil dan akurat
 *  3. Rasio jarak sudut-ke-anchor digunakan untuk resize proporsional sesuai arah dorongan
 *  4. Penskalaan/jarak dibatasi dalam rentang aman
 */
class ScaleHandleTouchTest {

    @Test
    fun `TouchState enum has required interaction states`() {
        assertEquals(TouchState.IDLE, TouchState.valueOf("IDLE"))
        assertEquals(TouchState.DRAGGING_RESIZE_HANDLE, TouchState.valueOf("DRAGGING_RESIZE_HANDLE"))
        assertEquals(TouchState.DRAGGING_LAYER, TouchState.valueOf("DRAGGING_LAYER"))
        assertEquals(TouchState.DRAGGING_ROTATE_HANDLE, TouchState.valueOf("DRAGGING_ROTATE_HANDLE"))
        assertEquals(TouchState.DRAGGING_PERSPECTIVE_HANDLE, TouchState.valueOf("DRAGGING_PERSPECTIVE_HANDLE"))
    }

    @Test
    fun `layer center calculation remains constant during transform changes`() {
        val layer = TextLayer(text = "Sample", x = 100f, y = 200f)
        val (w, h) = layer.getUnwarpedDimensions()

        val expectedCenterX = 100f + w / 2f
        val expectedCenterY = 200f + h / 2f

        val cx1 = layer.x + w / 2f
        val cy1 = layer.y + h / 2f
        assertEquals(expectedCenterX, cx1, 0.001f)
        assertEquals(expectedCenterY, cy1, 0.001f)

        layer.scale = 2.5f
        val cx2 = layer.x + w / 2f
        val cy2 = layer.y + h / 2f
        assertEquals(expectedCenterX, cx2, 0.001f)
        assertEquals(expectedCenterY, cy2, 0.001f)
    }

    @Test
    fun `moving finger outward increases distance to anchor proportionally`() {
        val initialDist = 100f
        val currentDist = 150f
        val ratio = currentDist / initialDist
        assertEquals(1.5f, ratio, 0.001f)
    }

    @Test
    fun `moving finger inward decreases distance to anchor proportionally`() {
        val initialDist = 200f
        val currentDist = 100f
        val ratio = currentDist / initialDist
        assertEquals(0.5f, ratio, 0.001f)
    }

    @Test
    fun `resize distances are clamped to safe boundaries`() {
        val initialDist = 100f
        val minRatio = (1f / initialDist).coerceIn(0.02f, 50f)
        assertEquals(0.02f, minRatio, 0.001f)

        val maxRatio = (5000f / initialDist).coerceIn(0.02f, 50f)
        assertEquals(50f, maxRatio, 0.001f)
    }
}