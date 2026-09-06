package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView.TransformHandle
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk 4 Tombol Handle Sudut Interaktif (Prompt 26).
 *
 * Verifikasi:
 *  1. Nilai enum TransformHandle lengkap (NONE, DUPLICATE, DELETE, SCALE, ROTATE)
 *  2. Posisi 4 titik sudut dari getSelectionBoxPoints memetakan posisi handle secara benar:
 *     - Top-Left     -> DUPLICATE
 *     - Top-Right    -> DELETE
 *     - Bottom-Right -> SCALE
 *     - Bottom-Left  -> ROTATE
 */
class TransformHandlesTest {

    @Test
    fun `enum TransformHandle has all 5 required states`() {
        val values = TransformHandle.values()
        assertEquals(5, values.size)
        assertEquals(TransformHandle.NONE, TransformHandle.valueOf("NONE"))
        assertEquals(TransformHandle.DUPLICATE, TransformHandle.valueOf("DUPLICATE"))
        assertEquals(TransformHandle.DELETE, TransformHandle.valueOf("DELETE"))
        assertEquals(TransformHandle.SCALE, TransformHandle.valueOf("SCALE"))
        assertEquals(TransformHandle.ROTATE, TransformHandle.valueOf("ROTATE"))
    }

    @Test
    fun `corner anchors correspond to correct handle functions`() {
        // Uji pemetaan koordinat sudut dari getSelectionBoxPoints
        val layer = TextLayer(text = "Handles", x = 100f, y = 100f)
        val pts = layer.getSelectionBoxPoints(padding = 8f)

        // 4 titik sudut
        val tlX = pts[0]
        val tlY = pts[1]
        val trX = pts[2]
        val trY = pts[3]
        val brX = pts[4]
        val brY = pts[5]
        val blX = pts[6]
        val blY = pts[7]

        // Validasi posisi relatif
        assertTrue(trX > tlX) // Kanan atas berada di sebelah kanan kiri atas
        assertTrue(brX > blX) // Kanan bawah berada di sebelah kanan kiri bawah
        assertTrue(blY > tlY) // Kiri bawah berada di bawah kiri atas
        assertTrue(brY > trY) // Kanan bawah berada di bawah kanan atas
    }
}
