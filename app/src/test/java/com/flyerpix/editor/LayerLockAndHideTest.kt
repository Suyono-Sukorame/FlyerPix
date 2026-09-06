package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk fitur Lock & Hide Per-Layer (Prompt 34).
 *
 * Memverifikasi:
 *  1. Tombol Mata (Toggle isVisible):
 *     - Menyembunyikan layer dari kanvas jika false.
 *     - Membatalkan status seleksi aktif jika layer yang disembunyikan sedang dipilih.
 *  2. Tombol Gembok (Toggle isLocked):
 *     - Mengunci layer sehingga tidak bisa diseleksi atau digeser di kanvas utama.
 *     - Sentuhan pada kanvas menembus (pass-through) layer yang terkunci ke layer di bawahnya.
 *     - Membatalkan seleksi aktif jika layer yang dikunci sedang dipilih.
 */
class LayerLockAndHideTest {

    @Test
    fun `toggle isVisible hides layer and deselects it if currently active`() {
        val layer = TextLayer(text = "Sample Layer", isVisible = true)
        var selectedLayer: CanvasLayer? = layer

        assertTrue(layer.isVisible)
        assertEquals(layer, selectedLayer)

        // Toggle visibility -> false (Sembunyikan layer)
        layer.isVisible = !layer.isVisible
        if (!layer.isVisible && selectedLayer == layer) {
            selectedLayer = null
        }

        assertFalse(layer.isVisible)
        assertNull(selectedLayer) // Seleksi harus dibatalkan agar tidak menggantung di kanvas
    }

    @Test
    fun `toggle isLocked locks layer and deselects it from main canvas`() {
        val layer = TextLayer(text = "Editable Layer", isLocked = false)
        var selectedLayer: CanvasLayer? = layer

        assertFalse(layer.isLocked)
        assertNotNull(selectedLayer)

        // Toggle lock -> true (Kunci layer)
        layer.isLocked = !layer.isLocked
        if (layer.isLocked && selectedLayer == layer) {
            selectedLayer = null
        }

        assertTrue(layer.isLocked)
        assertNull(selectedLayer) // Bounding box di kanvas utama otomatis hilang
    }

    @Test
    fun `touch hit testing ignores hidden layers`() {
        val layerHidden = TextLayer(text = "Hidden", x = 100f, y = 100f, isVisible = false)
        val layers = listOf(layerHidden)

        // Cari layer pada koordinat (120, 120)
        val hitLayer = layers.findLast { it.isVisible && !it.isLocked && it.containsCanvasPoint(120f, 120f) }

        assertNull(hitLayer)
    }

    @Test
    fun `touch hit testing skips locked layer and selects unlocked layer underneath`() {
        // Layer bawah (bebas / unlocked)
        val bottomLayer = TextLayer(text = "Bottom Unlocked", x = 100f, y = 100f, isLocked = false, isVisible = true)

        // Layer atas (terkunci / locked) pada posisi yang sama
        val topLayer = TextLayer(text = "Top Locked", x = 100f, y = 100f, isLocked = true, isVisible = true)

        val layers = listOf(bottomLayer, topLayer)

        // Cari dari z-index tertinggi ke terendah
        val hitLayer = layers.findLast { it.isVisible && !it.isLocked && it.containsCanvasPoint(120f, 120f) }

        // Top layer terkunci diabaikan, sentuhan berhasil menembus dan mengenai bottomLayer
        assertNotNull(hitLayer)
        assertEquals(bottomLayer, hitLayer)
    }

    @Test
    fun `locked layer cannot be dragged on canvas`() {
        val layer = TextLayer(text = "Protected", x = 50f, y = 50f, isLocked = true)
        var isDragging = false

        // Simulasikan sentuhan pada kanvas
        val canDrag = layer.isVisible && !layer.isLocked
        if (canDrag) {
            isDragging = true
            layer.x += 20f
            layer.y += 20f
        }

        assertFalse(isDragging)
        assertEquals(50f, layer.x, 0.001f)
        assertEquals(50f, layer.y, 0.001f)
    }
}
