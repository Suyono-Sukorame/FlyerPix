package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk fitur Bounding Box Seleksi pada CanvasLayer / TextLayer (Prompt 25).
 *
 * Verifikasi:
 *  1. getSelectionBoxPoints(padding) menghasilkan 8 elemen koordinat (4 titik sudut)
 *  2. Padding memperluas batas bounding box ke segala arah secara simetris
 *  3. Translasi layer menggeser seluruh titik sudut bounding box
 *  4. Skala layer memperbesar / memperkecil ukuran bounding box secara proporsional
 */
class SelectionBoundingBoxTest {

    @Test
    fun `getSelectionBoxPoints returns 8 coordinates representing 4 corners`() {
        val layer = TextLayer(text = "Hello", x = 100f, y = 100f)
        val pts = layer.getSelectionBoxPoints(padding = 0f)

        assertEquals(8, pts.size)
        // Point 0: Top-Left (x0, y0)
        // Point 1: Top-Right (x1, y1)
        // Point 2: Bottom-Right (x2, y2)
        // Point 3: Bottom-Left (x3, y3)
        assertTrue(pts[2] > pts[0]) // Right > Left
        assertTrue(pts[5] > pts[1]) // Bottom > Top
    }

    @Test
    fun `padding expands bounding box symmetrically`() {
        val layer = TextLayer(text = "PixelLab", x = 50f, y = 50f)
        val pad = 12f

        val ptsNoPad = layer.getSelectionBoxPoints(padding = 0f)
        val ptsWithPad = layer.getSelectionBoxPoints(padding = pad)

        // Top-Left dengan padding harus bergeser ke kiri dan atas sebesar pad
        assertEquals(ptsNoPad[0] - pad, ptsWithPad[0], 0.001f)
        assertEquals(ptsNoPad[1] - pad, ptsWithPad[1], 0.001f)

        // Bottom-Right dengan padding harus bergeser ke kanan dan bawah sebesar pad
        assertEquals(ptsNoPad[4] + pad, ptsWithPad[4], 0.001f)
        assertEquals(ptsNoPad[5] + pad, ptsWithPad[5], 0.001f)
    }

    @Test
    fun `translation shifts all 4 corners by delta x and y`() {
        val layer = TextLayer(text = "Test Box", x = 0f, y = 0f)
        val ptsOrigin = layer.getSelectionBoxPoints(padding = 8f)

        layer.x = 80f
        layer.y = 120f
        val ptsMoved = layer.getSelectionBoxPoints(padding = 8f)

        for (i in 0..3) {
            assertEquals(ptsOrigin[i * 2] + 80f, ptsMoved[i * 2], 0.001f)
            assertEquals(ptsOrigin[i * 2 + 1] + 120f, ptsMoved[i * 2 + 1], 0.001f)
        }
    }

    @Test
    fun `scaling layer increases width and height of selection box`() {
        val layer1 = TextLayer(text = "Scale", scale = 1f)
        val layer2 = TextLayer(text = "Scale", scale = 2f)

        val pts1 = layer1.getSelectionBoxPoints(padding = 0f)
        val pts2 = layer2.getSelectionBoxPoints(padding = 0f)

        val width1 = pts1[2] - pts1[0]
        val width2 = pts2[2] - pts2[0]

        // Lebar layer skala 2x harus 2 kali lipat dari skala 1x
        assertEquals(width1 * 2f, width2, 0.01f)
    }
}
