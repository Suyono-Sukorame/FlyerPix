package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView.TransformHandle
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk 8-Handle Resize Universal + Handle Rotasi (Prompt 26/29).
 *
 * Verifikasi:
 *  1. Nilai enum TransformHandle lengkap (NONE, 8xRESIZE_*, ROTATE)
 *  2. getHandle8Points mengembalikan 16 float dengan urutan TL, TM, TR, ML, MR, BL, BM, BR
 *  3. Posisi 4 titik sudut dari getSelectionBoxPoints memetakan ke posisi handle sudut secara benar
 *  4. Urutan 8 titik handle: sudut-sudut berada di posisi yang benar relatif satu sama lain
 */
class TransformHandlesTest {

    @Test
    fun `enum TransformHandle has all 10 required states`() {
        val values = TransformHandle.values()
        assertEquals(10, values.size)
        assertEquals(TransformHandle.NONE, TransformHandle.valueOf("NONE"))
        assertEquals(TransformHandle.RESIZE_TL, TransformHandle.valueOf("RESIZE_TL"))
        assertEquals(TransformHandle.RESIZE_TM, TransformHandle.valueOf("RESIZE_TM"))
        assertEquals(TransformHandle.RESIZE_TR, TransformHandle.valueOf("RESIZE_TR"))
        assertEquals(TransformHandle.RESIZE_ML, TransformHandle.valueOf("RESIZE_ML"))
        assertEquals(TransformHandle.RESIZE_MR, TransformHandle.valueOf("RESIZE_MR"))
        assertEquals(TransformHandle.RESIZE_BL, TransformHandle.valueOf("RESIZE_BL"))
        assertEquals(TransformHandle.RESIZE_BM, TransformHandle.valueOf("RESIZE_BM"))
        assertEquals(TransformHandle.RESIZE_BR, TransformHandle.valueOf("RESIZE_BR"))
        assertEquals(TransformHandle.ROTATE, TransformHandle.valueOf("ROTATE"))
    }

    @Test
    fun `getHandle8Points returns 16 floats in TL TM TR ML MR BL BM BR order`() {
        val layer = TextLayer(text = "Handles", x = 100f, y = 100f)
        val h = layer.getHandle8Points(padding = 8f)

        assertEquals(16, h.size)

        // Sudut-sudut harus berada di posisi yang benar relatif satu sama lain.
        val tlX = h[0]; val tlY = h[1]
        val trX = h[4]; val trY = h[5]
        val blX = h[10]; val blY = h[11]
        val brX = h[14]; val brY = h[15]

        assertTrue(trX > tlX)
        assertTrue(brX > blX)
        assertTrue(blY > tlY)
        assertTrue(brY > trY)
    }

    @Test
    fun `corner anchors from getSelectionBoxPoints match handle8 corners`() {
        val layer = TextLayer(text = "Handles", x = 100f, y = 100f)
        val pts = layer.getSelectionBoxPoints(padding = 8f)
        val h = layer.getHandle8Points(padding = 8f)

        // Handle sudut harus persis mengikuti 4 sudut kotak seleksi.
        assertEquals(pts[0], h[0], 0.001f)  // TL
        assertEquals(pts[1], h[1], 0.001f)
        assertEquals(pts[2], h[4], 0.001f)  // TR
        assertEquals(pts[3], h[5], 0.001f)
        assertEquals(pts[4], h[14], 0.001f) // BR
        assertEquals(pts[5], h[15], 0.001f)
        assertEquals(pts[6], h[10], 0.001f) // BL
        assertEquals(pts[7], h[11], 0.001f)
    }
}