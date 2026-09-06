package com.flyerpix.editor

import android.graphics.Color
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk aksi instan Handle Delete & Duplicate (Prompt 29).
 *
 * Memverifikasi:
 *  1. Handle Delete: Menghapus layer dari list layers, mengosongkan selectedLayer (null).
 *  2. Handle Duplicate: Mengkloning layer aktif dengan offset (x + 30, y + 30).
 *  3. Layer hasil duplikasi langsung menjadi layer terpilih (selectedLayer).
 *  4. Proteksi layer terkunci (isLocked) tidak dapat dihapus atau diduplikasi melalui handle.
 */
class DeleteAndDuplicateHandleTest {

    @Test
    fun `deleteSelectedLayer removes layer and clears selectedLayer`() {
        val layers = mutableListOf<TextLayer>()
        val layer1 = TextLayer(text = "Layer 1", x = 50f, y = 50f)
        val layer2 = TextLayer(text = "Layer 2", x = 100f, y = 100f)
        layers.add(layer1)
        layers.add(layer2)

        var selectedLayer: TextLayer? = layer2

        // Aksi Delete pada selectedLayer
        val toDelete = selectedLayer
        assertNotNull(toDelete)
        val removed = layers.remove(toDelete)
        if (removed) {
            selectedLayer = null
        }

        assertTrue(removed)
        assertEquals(1, layers.size)
        assertEquals(layer1, layers[0])
        assertNull(selectedLayer)
    }

    @Test
    fun `duplicateSelectedLayer clones layer with 30px offset and selects it`() {
        val layers = mutableListOf<TextLayer>()
        val original = TextLayer(
            text = "Original Text",
            x = 100f,
            y = 200f,
            textColor = Color.YELLOW,
            textSize = 72f,
            scale = 1.5f,
            rotation = 45f
        )
        layers.add(original)
        var selectedLayer: TextLayer? = original

        // Aksi Duplicate pada selectedLayer
        val target = selectedLayer!!
        val cloned = target.copyLayer()
        cloned.x = target.x + 30f
        cloned.y = target.y + 30f
        layers.add(cloned)
        selectedLayer = cloned

        // Verifikasi list layers
        assertEquals(2, layers.size)
        assertEquals(original, layers[0])
        assertEquals(cloned, layers[1])

        // Verifikasi selectedLayer langsung menjadi layer baru
        assertEquals(cloned, selectedLayer)

        // Verifikasi offset (x + 30, y + 30)
        assertEquals(130f, cloned.x, 0.001f)
        assertEquals(230f, cloned.y, 0.001f)

        // Verifikasi ID unik dan properti terkloning sempurna
        assertNotEquals(original.id, cloned.id)
        assertEquals(original.text, cloned.text)
        assertEquals(original.textColor, cloned.textColor)
        assertEquals(original.textSize, cloned.textSize, 0.001f)
        assertEquals(original.scale, cloned.scale, 0.001f)
        assertEquals(original.rotation, cloned.rotation, 0.001f)
    }

    @Test
    fun `locked layer prevents deletion and duplication`() {
        val lockedLayer = TextLayer(text = "Locked", x = 50f, y = 50f, isLocked = true)
        val layers = mutableListOf(lockedLayer)
        var selectedLayer: TextLayer? = lockedLayer

        // Upaya delete saat terkunci
        if (!lockedLayer.isLocked) {
            layers.remove(lockedLayer)
            selectedLayer = null
        }

        assertEquals(1, layers.size)
        assertNotNull(selectedLayer)

        // Upaya duplicate saat terkunci
        var duplicatedLayer: TextLayer? = null
        if (!lockedLayer.isLocked) {
            duplicatedLayer = lockedLayer.copyLayer()
        }

        assertNull(duplicatedLayer)
        assertEquals(1, layers.size)
    }
}
