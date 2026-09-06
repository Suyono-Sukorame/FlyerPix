package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.ui.dialog.LayerManagerBottomSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk fitur Drawer / Floating Dialog Layer Manager (Prompt 31).
 *
 * Memverifikasi:
 *  1. Urutan layer pada Layer Manager disajikan dari atas ke bawah (urutan z-index terbalik).
 *  2. Konversi indeks antara daftar tampilan (displayIndex) dan tumpukan kanvas (canvasIndex).
 *  3. Penanganan tumpukan kosong (empty state).
 *  4. Integritas properti layer saat dipetakan ke Layer Manager.
 */
class LayerManagerTest {

    @Test
    fun `getReversedLayers orders layers with highest z-index at the top`() {
        val layerBottom = TextLayer(text = "Layer 1 (Bawah)", x = 10f, y = 10f)
        val layerMiddle = TextLayer(text = "Layer 2 (Tengah)", x = 20f, y = 20f)
        val layerTop = TextLayer(text = "Layer 3 (Atas)", x = 30f, y = 30f)

        val canvasLayers = listOf(layerBottom, layerMiddle, layerTop)

        // Urutan z-index kanvas: 0 = Bawah, 1 = Tengah, 2 = Atas
        val displayedLayers = LayerManagerBottomSheet.getReversedLayers(canvasLayers)

        // Tampilan list di Layer Manager harus terbalik (Atas -> Tengah -> Bawah)
        assertEquals(3, displayedLayers.size)
        assertEquals(layerTop, displayedLayers[0])
        assertEquals(layerMiddle, displayedLayers[1])
        assertEquals(layerBottom, displayedLayers[2])
    }

    @Test
    fun `toCanvasIndex correctly maps display position to canvas layer index`() {
        val totalSize = 4

        // Item paling atas di dialog (displayIndex = 0) -> layer teratas di kanvas (index 3)
        assertEquals(3, LayerManagerBottomSheet.toCanvasIndex(displayIndex = 0, totalSize = totalSize))

        // Item ke-2 di dialog (displayIndex = 1) -> layer kedua dari atas (index 2)
        assertEquals(2, LayerManagerBottomSheet.toCanvasIndex(displayIndex = 1, totalSize = totalSize))

        // Item paling bawah di dialog (displayIndex = 3) -> layer terbawah di kanvas (index 0)
        assertEquals(0, LayerManagerBottomSheet.toCanvasIndex(displayIndex = 3, totalSize = totalSize))
    }

    @Test
    fun `toDisplayIndex correctly maps canvas layer index to display position`() {
        val totalSize = 4

        // Layer teratas kanvas (canvasIndex = 3) -> posisi paling atas di list (displayIndex = 0)
        assertEquals(0, LayerManagerBottomSheet.toDisplayIndex(canvasIndex = 3, totalSize = totalSize))

        // Layer terbawah kanvas (canvasIndex = 0) -> posisi paling bawah di list (displayIndex = 3)
        assertEquals(3, LayerManagerBottomSheet.toDisplayIndex(canvasIndex = 0, totalSize = totalSize))
    }

    @Test
    fun `empty canvas returns empty reversed layer list`() {
        val canvasLayers = emptyList<TextLayer>()
        val displayedLayers = LayerManagerBottomSheet.getReversedLayers(canvasLayers)

        assertTrue(displayedLayers.isEmpty())
        assertEquals(0, displayedLayers.size)
    }

    @Test
    fun `layer properties remain intact during reverse ordering`() {
        val layer1 = TextLayer(text = "Header Text", x = 100f, y = 50f, isVisible = true, isLocked = false)
        val layer2 = TextLayer(text = "Watermark", x = 200f, y = 400f, isVisible = false, isLocked = true)

        val displayed = LayerManagerBottomSheet.getReversedLayers(listOf(layer1, layer2))

        assertEquals("Watermark", (displayed[0] as TextLayer).text)
        assertEquals(false, displayed[0].isVisible)
        assertEquals(true, displayed[0].isLocked)

        assertEquals("Header Text", (displayed[1] as TextLayer).text)
        assertEquals(true, displayed[1].isVisible)
        assertEquals(false, displayed[1].isLocked)
    }
}
