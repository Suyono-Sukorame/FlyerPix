package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.ui.dialog.LayerManagerBottomSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk Fitur Reorder 'Bring to Front' & 'Send to Back' (Prompt 35).
 *
 * Menguji:
 * 1. 'To Front' memindahkan layer aktif ke posisi paling atas tumpukan (z-index tertinggi, index size - 1).
 * 2. 'To Back' memindahkan layer aktif tepat di atas background kanvas (posisi paling bawah, index 0).
 * 3. Layer yang sudah di posisi paling atas tetap di paling atas saat 'To Front'.
 * 4. Layer yang sudah di posisi paling bawah tetap di paling bawah saat 'To Back'.
 * 5. Penanganan kanvas dengan 1 layer (edge case).
 * 6. Penanganan layer yang tidak ditemukan pada kanvas (error handling).
 * 7. Sinkronisasi visual dengan Layer Manager:
 *    - Layer paling atas kanvas menjadi item pertama (index 0) di daftar pengelola lapisan.
 *    - Layer paling bawah kanvas menjadi item terakhir di daftar pengelola lapisan.
 */
class BringToFrontAndSendToBackTest {

    @Test
    fun `bringLayerToFront moves bottom layer to top of stack`() {
        val layerBottom = TextLayer(text = "Layer 0 (Bottom)")
        val layerMiddle = TextLayer(text = "Layer 1 (Middle)")
        val layerTop = TextLayer(text = "Layer 2 (Top)")

        val canvasLayers = mutableListOf<CanvasLayer>(layerBottom, layerMiddle, layerTop)

        // Pindahkan layerBottom ke posisi paling atas (To Front)
        val result = LayerManagerBottomSheet.bringCanvasLayerToFront(canvasLayers, layerBottom)

        assertTrue(result)
        assertEquals(3, canvasLayers.size)
        assertEquals(layerMiddle, canvasLayers[0])
        assertEquals(layerTop, canvasLayers[1])
        assertEquals(layerBottom, canvasLayers[2]) // Sekarang berada di paling atas (index 2)

        // Verifikasi pada daftar Layer Manager terbalik:
        val displayLayers = LayerManagerBottomSheet.getReversedLayers(canvasLayers)
        assertEquals(layerBottom, displayLayers[0]) // Di daftar pengelola, layerBottom tampil di baris teratas
        assertEquals(layerTop, displayLayers[1])
        assertEquals(layerMiddle, displayLayers[2])
    }

    @Test
    fun `bringLayerToFront moves middle layer to top of stack`() {
        val layerA = TextLayer(text = "Layer A")
        val layerB = TextLayer(text = "Layer B")
        val layerC = TextLayer(text = "Layer C")

        val canvasLayers = mutableListOf<CanvasLayer>(layerA, layerB, layerC)

        // Pindahkan layerB ke posisi paling atas (To Front)
        val result = LayerManagerBottomSheet.bringCanvasLayerToFront(canvasLayers, layerB)

        assertTrue(result)
        assertEquals(layerA, canvasLayers[0])
        assertEquals(layerC, canvasLayers[1])
        assertEquals(layerB, canvasLayers[2]) // layerB sekarang paling atas
    }

    @Test
    fun `sendLayerToBack moves top layer to bottom of stack`() {
        val layerBottom = TextLayer(text = "Layer 0 (Bottom)")
        val layerMiddle = TextLayer(text = "Layer 1 (Middle)")
        val layerTop = TextLayer(text = "Layer 2 (Top)")

        val canvasLayers = mutableListOf<CanvasLayer>(layerBottom, layerMiddle, layerTop)

        // Pindahkan layerTop ke posisi tepat di atas background kanvas (To Back)
        val result = LayerManagerBottomSheet.sendCanvasLayerToBack(canvasLayers, layerTop)

        assertTrue(result)
        assertEquals(3, canvasLayers.size)
        assertEquals(layerTop, canvasLayers[0])    // Sekarang berada di posisi paling bawah (index 0)
        assertEquals(layerBottom, canvasLayers[1])
        assertEquals(layerMiddle, canvasLayers[2]) // layerMiddle sekarang menjadi yang teratas

        // Verifikasi pada daftar Layer Manager terbalik:
        val displayLayers = LayerManagerBottomSheet.getReversedLayers(canvasLayers)
        assertEquals(layerMiddle, displayLayers[0]) // layerMiddle tampil di paling atas UI
        assertEquals(layerBottom, displayLayers[1])
        assertEquals(layerTop, displayLayers[2])    // layerTop tampil di paling bawah UI
    }

    @Test
    fun `sendLayerToBack moves middle layer to bottom of stack`() {
        val layerA = TextLayer(text = "Layer A")
        val layerB = TextLayer(text = "Layer B")
        val layerC = TextLayer(text = "Layer C")

        val canvasLayers = mutableListOf<CanvasLayer>(layerA, layerB, layerC)

        // Pindahkan layerB ke paling bawah (To Back)
        val result = LayerManagerBottomSheet.sendCanvasLayerToBack(canvasLayers, layerB)

        assertTrue(result)
        assertEquals(layerB, canvasLayers[0]) // layerB sekarang di index 0
        assertEquals(layerA, canvasLayers[1])
        assertEquals(layerC, canvasLayers[2])
    }

    @Test
    fun `bringLayerToFront when layer is already at top returns true without altering stack`() {
        val layer1 = TextLayer(text = "Layer 1")
        val layer2 = TextLayer(text = "Layer 2")
        val layer3 = TextLayer(text = "Layer 3")

        val canvasLayers = mutableListOf<CanvasLayer>(layer1, layer2, layer3)

        // layer3 sudah berada di index 2 (paling atas)
        val result = LayerManagerBottomSheet.bringCanvasLayerToFront(canvasLayers, layer3)

        assertTrue(result)
        assertEquals(layer1, canvasLayers[0])
        assertEquals(layer2, canvasLayers[1])
        assertEquals(layer3, canvasLayers[2])
    }

    @Test
    fun `sendLayerToBack when layer is already at bottom returns true without altering stack`() {
        val layer1 = TextLayer(text = "Layer 1")
        val layer2 = TextLayer(text = "Layer 2")
        val layer3 = TextLayer(text = "Layer 3")

        val canvasLayers = mutableListOf<CanvasLayer>(layer1, layer2, layer3)

        // layer1 sudah berada di index 0 (paling bawah)
        val result = LayerManagerBottomSheet.sendCanvasLayerToBack(canvasLayers, layer1)

        assertTrue(result)
        assertEquals(layer1, canvasLayers[0])
        assertEquals(layer2, canvasLayers[1])
        assertEquals(layer3, canvasLayers[2])
    }

    @Test
    fun `reordering with single layer returns true and remains unchanged`() {
        val singleLayer = TextLayer(text = "Only Layer")
        val canvasLayers = mutableListOf<CanvasLayer>(singleLayer)

        assertTrue(LayerManagerBottomSheet.bringCanvasLayerToFront(canvasLayers, singleLayer))
        assertEquals(1, canvasLayers.size)
        assertEquals(singleLayer, canvasLayers[0])

        assertTrue(LayerManagerBottomSheet.sendCanvasLayerToBack(canvasLayers, singleLayer))
        assertEquals(1, canvasLayers.size)
        assertEquals(singleLayer, canvasLayers[0])
    }

    @Test
    fun `reordering non existent layer returns false`() {
        val layer1 = TextLayer(text = "Layer 1")
        val foreignLayer = TextLayer(text = "Foreign Layer")
        val canvasLayers = mutableListOf<CanvasLayer>(layer1)

        assertFalse(LayerManagerBottomSheet.bringCanvasLayerToFront(canvasLayers, foreignLayer))
        assertFalse(LayerManagerBottomSheet.sendCanvasLayerToBack(canvasLayers, foreignLayer))
        assertEquals(1, canvasLayers.size)
        assertEquals(layer1, canvasLayers[0])
    }

    @Test
    fun `continuous reordering preserves integrity of all layers`() {
        val l1 = TextLayer(text = "L1")
        val l2 = TextLayer(text = "L2")
        val l3 = TextLayer(text = "L3")
        val l4 = TextLayer(text = "L4")

        // Initial stack: [L1, L2, L3, L4]
        val canvasLayers = mutableListOf<CanvasLayer>(l1, l2, l3, l4)

        // Send L4 to back: [L4, L1, L2, L3]
        LayerManagerBottomSheet.sendCanvasLayerToBack(canvasLayers, l4)
        assertEquals(listOf(l4, l1, l2, l3), canvasLayers)

        // Bring L2 to front: [L4, L1, L3, L2]
        LayerManagerBottomSheet.bringCanvasLayerToFront(canvasLayers, l2)
        assertEquals(listOf(l4, l1, l3, l2), canvasLayers)

        // Send L3 to back: [L3, L4, L1, L2]
        LayerManagerBottomSheet.sendCanvasLayerToBack(canvasLayers, l3)
        assertEquals(listOf(l3, l4, l1, l2), canvasLayers)

        // Bring L4 to front: [L3, L1, L2, L4]
        LayerManagerBottomSheet.bringCanvasLayerToFront(canvasLayers, l4)
        assertEquals(listOf(l3, l1, l2, l4), canvasLayers)
    }
}
