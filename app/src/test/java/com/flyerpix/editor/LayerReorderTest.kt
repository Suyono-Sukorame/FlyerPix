package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.ui.dialog.LayerManagerBottomSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk gestur Drag-and-Drop Reorder Z-Index dengan ItemTouchHelper (Prompt 33).
 *
 * Memverifikasi:
 *  1. Penukaran posisi layer pada RecyclerView (dari atas ke bawah) langsung menyinkronkan
 *     posisi z-index pada daftar tumpukan kanvas (canvasLayers).
 *  2. Penukaran item paling atas ke bawah (menurunkan z-index).
 *  3. Penukaran item paling bawah ke atas (menaikan z-index).
 *  4. Proteksi batas indeks (out-of-bounds).
 *  5. Reordering bertingkat multi-langkah (continuous adjacent drag).
 */
class LayerReorderTest {

    @Test
    fun `dragging top display item down decreases its z-index in canvasLayers`() {
        val layerBottom = TextLayer(text = "Layer 0 (Bottom)")
        val layerMiddle = TextLayer(text = "Layer 1 (Middle)")
        val layerTop = TextLayer(text = "Layer 2 (Top)")

        // Canvas z-index: [0: Bottom, 1: Middle, 2: Top]
        val canvasLayers = mutableListOf<CanvasLayer>(layerBottom, layerMiddle, layerTop)

        // Di UI daftar layer:
        // Display 0: Top
        // Display 1: Middle
        // Display 2: Bottom

        // Pengguna men-drag Display 0 (Top) ke Display 1 (Middle)
        val success = LayerManagerBottomSheet.swapCanvasLayersFromDisplay(
            canvasLayers = canvasLayers,
            fromDisplayPos = 0,
            toDisplayPos = 1
        )

        assertTrue(success)

        // Di canvasLayers, z-index Top dan Middle harus bertukar:
        // index 0: Bottom
        // index 1: Top (sebelumnya z:2 sekarang z:1)
        // index 2: Middle (sebelumnya z:1 sekarang z:2, menjadi paling atas)
        assertEquals(layerBottom, canvasLayers[0])
        assertEquals(layerTop, canvasLayers[1])
        assertEquals(layerMiddle, canvasLayers[2])

        // Verifikasi jika di-render ulang di daftar UI:
        val newDisplay = LayerManagerBottomSheet.getReversedLayers(canvasLayers)
        assertEquals(layerMiddle, newDisplay[0]) // Middle sekarang di atas
        assertEquals(layerTop, newDisplay[1])    // Top sekarang di tengah
        assertEquals(layerBottom, newDisplay[2]) // Bottom tetap di bawah
    }

    @Test
    fun `dragging bottom display item up increases its z-index in canvasLayers`() {
        val layerBottom = TextLayer(text = "Layer 0 (Bottom)")
        val layerMiddle = TextLayer(text = "Layer 1 (Middle)")
        val layerTop = TextLayer(text = "Layer 2 (Top)")

        val canvasLayers = mutableListOf<CanvasLayer>(layerBottom, layerMiddle, layerTop)

        // Pengguna men-drag Display 2 (Bottom) ke Display 1 (Middle)
        val success = LayerManagerBottomSheet.swapCanvasLayersFromDisplay(
            canvasLayers = canvasLayers,
            fromDisplayPos = 2,
            toDisplayPos = 1
        )

        assertTrue(success)

        // Di canvasLayers:
        // index 0: Middle (menjadi lapisan paling bawah)
        // index 1: Bottom (naik ke lapisan tengah)
        // index 2: Top (tetap lapisan paling atas)
        assertEquals(layerMiddle, canvasLayers[0])
        assertEquals(layerBottom, canvasLayers[1])
        assertEquals(layerTop, canvasLayers[2])

        // Verifikasi di daftar UI:
        val newDisplay = LayerManagerBottomSheet.getReversedLayers(canvasLayers)
        assertEquals(layerTop, newDisplay[0])
        assertEquals(layerBottom, newDisplay[1])
        assertEquals(layerMiddle, newDisplay[2])
    }

    @Test
    fun `multi-step continuous drag reorders layers accurately`() {
        val l1 = TextLayer(text = "1")
        val l2 = TextLayer(text = "2")
        val l3 = TextLayer(text = "3")
        val l4 = TextLayer(text = "4")

        // Canvas: [1, 2, 3, 4] (4 adalah paling atas)
        val canvasLayers = mutableListOf<CanvasLayer>(l1, l2, l3, l4)

        // UI urutan display: [0: 4, 1: 3, 2: 2, 3: 1]
        // Pengguna men-drag 4 dari display 0 sampai ke display 3 (menjadi paling bawah):
        // Langkah 1: display 0 -> 1
        LayerManagerBottomSheet.swapCanvasLayersFromDisplay(canvasLayers, 0, 1)
        // Langkah 2: display 1 -> 2
        LayerManagerBottomSheet.swapCanvasLayersFromDisplay(canvasLayers, 1, 2)
        // Langkah 3: display 2 -> 3
        LayerManagerBottomSheet.swapCanvasLayersFromDisplay(canvasLayers, 2, 3)

        // Setelah 4 dipindah ke paling bawah di UI, di canvas z-index 4 harus berada di index 0:
        assertEquals(l4, canvasLayers[0]) // 4 paling bawah
        assertEquals(l1, canvasLayers[1])
        assertEquals(l2, canvasLayers[2])
        assertEquals(l3, canvasLayers[3]) // 3 sekarang paling atas

        // Urutan di UI: [0: 3, 1: 2, 2: 1, 3: 4]
        val display = LayerManagerBottomSheet.getReversedLayers(canvasLayers)
        assertEquals(l3, display[0])
        assertEquals(l2, display[1])
        assertEquals(l1, display[2])
        assertEquals(l4, display[3])
    }

    @Test
    fun `swapCanvasLayersFromDisplay handles invalid positions gracefully`() {
        val canvasLayers = mutableListOf<CanvasLayer>(TextLayer(text = "A"), TextLayer(text = "B"))

        // Posisi negatif
        assertFalse(LayerManagerBottomSheet.swapCanvasLayersFromDisplay(canvasLayers, -1, 0))

        // Posisi di luar batas atas
        assertFalse(LayerManagerBottomSheet.swapCanvasLayersFromDisplay(canvasLayers, 0, 5))

        // Posisi sama
        assertFalse(LayerManagerBottomSheet.swapCanvasLayersFromDisplay(canvasLayers, 1, 1))
    }
}
