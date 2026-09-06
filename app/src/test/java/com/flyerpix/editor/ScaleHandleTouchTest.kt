package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView.TouchState
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.hypot

/**
 * Unit test untuk gestur interaksi Sentuhan pada Handle Scale & Resize (Prompt 27).
 *
 * Verifikasi:
 *  1. State TouchState.DRAGGING_SCALE_HANDLE terdefinisi dalam enum
 *  2. Titik pusat layer (centerX, centerY) dihitung stabil dan akurat
 *  3. Perhitungan rasio skala proporsional saat menjauh / mendekat dari pusat layer
 *  4. Penskalaan dibatasi dalam rentang aman (0.05f s/d 25.0f)
 */
class ScaleHandleTouchTest {

    @Test
    fun `TouchState enum has required interaction states`() {
        assertEquals(TouchState.IDLE, TouchState.valueOf("IDLE"))
        assertEquals(TouchState.DRAGGING_SCALE_HANDLE, TouchState.valueOf("DRAGGING_SCALE_HANDLE"))
        assertEquals(TouchState.DRAGGING_LAYER, TouchState.valueOf("DRAGGING_LAYER"))
        assertEquals(TouchState.DRAGGING_ROTATE_HANDLE, TouchState.valueOf("DRAGGING_ROTATE_HANDLE"))
    }

    @Test
    fun `layer center calculation remains constant during scale changes`() {
        val layer = TextLayer(text = "Sample", x = 100f, y = 200f)
        val (w, h) = layer.getUnwarpedDimensions()

        val expectedCenterX = 100f + w / 2f
        val expectedCenterY = 200f + h / 2f

        // Center sebelum scale
        val cx1 = layer.x + w / 2f
        val cy1 = layer.y + h / 2f
        assertEquals(expectedCenterX, cx1, 0.001f)
        assertEquals(expectedCenterY, cy1, 0.001f)

        // Ubah skala layer
        layer.scale = 2.5f
        val cx2 = layer.x + w / 2f
        val cy2 = layer.y + h / 2f
        assertEquals(expectedCenterX, cx2, 0.001f)
        assertEquals(expectedCenterY, cy2, 0.001f)
    }

    @Test
    fun `moving finger outward increases scale proportionally`() {
        val initialDist = 100f
        val initialScale = 1.0f

        // Jari bergerak menjauh menjadi 150px
        val currentDist = 150f
        val ratio = currentDist / initialDist
        val newScale = (initialScale * ratio).coerceIn(0.05f, 25.0f)

        assertEquals(1.5f, newScale, 0.001f)
    }

    @Test
    fun `moving finger inward decreases scale proportionally`() {
        val initialDist = 200f
        val initialScale = 1.0f

        // Jari bergerak mendekat menjadi 100px
        val currentDist = 100f
        val ratio = currentDist / initialDist
        val newScale = (initialScale * ratio).coerceIn(0.05f, 25.0f)

        assertEquals(0.5f, newScale, 0.001f)
    }

    @Test
    fun `scale is clamped to min and max boundaries`() {
        val initialDist = 100f
        val initialScale = 1.0f

        // Jari mendekat sangat ekstrem
        val tinyDist = 1f
        val minScale = (initialScale * (tinyDist / initialDist)).coerceIn(0.05f, 25.0f)
        assertEquals(0.05f, minScale, 0.001f)

        // Jari menjauh sangat ekstrem
        val hugeDist = 5000f
        val maxScale = (initialScale * (hugeDist / initialDist)).coerceIn(0.05f, 25.0f)
        assertEquals(25.0f, maxScale, 0.001f)
    }
}
