package com.flyerpix.editor

import android.graphics.PorterDuff
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit test untuk fitur Blending Modes pada CanvasLayer / TextLayer (Prompt 24).
 *
 * Verifikasi:
 *  1. Nilai default blendMode = PorterDuff.Mode.SRC_OVER (Normal)
 *  2. Pengaturan mode populer (Multiply, Screen, Overlay, Darken, Lighten, Add)
 *  3. getBlendModeName() menghasilkan nama deskriptif yang sesuai
 *  4. copyLayer() menduplikasi blendMode secara akurat
 *  5. Reset blendMode mengembalikan ke SRC_OVER
 */
class CanvasLayerBlendModeTest {

    @Test
    fun `default blendMode is SRC_OVER (Normal)`() {
        val layer = TextLayer()
        assertEquals(PorterDuff.Mode.SRC_OVER, layer.blendMode)
        assertEquals("Normal", layer.getBlendModeName())
    }

    @Test
    fun `blendMode can be set to popular modes`() {
        val layer = TextLayer()

        layer.blendMode = PorterDuff.Mode.MULTIPLY
        assertEquals(PorterDuff.Mode.MULTIPLY, layer.blendMode)
        assertEquals("Multiply", layer.getBlendModeName())

        layer.blendMode = PorterDuff.Mode.SCREEN
        assertEquals(PorterDuff.Mode.SCREEN, layer.blendMode)
        assertEquals("Screen", layer.getBlendModeName())

        layer.blendMode = PorterDuff.Mode.OVERLAY
        assertEquals(PorterDuff.Mode.OVERLAY, layer.blendMode)
        assertEquals("Overlay", layer.getBlendModeName())

        layer.blendMode = PorterDuff.Mode.DARKEN
        assertEquals(PorterDuff.Mode.DARKEN, layer.blendMode)
        assertEquals("Darken", layer.getBlendModeName())

        layer.blendMode = PorterDuff.Mode.LIGHTEN
        assertEquals(PorterDuff.Mode.LIGHTEN, layer.blendMode)
        assertEquals("Lighten", layer.getBlendModeName())

        layer.blendMode = PorterDuff.Mode.ADD
        assertEquals(PorterDuff.Mode.ADD, layer.blendMode)
        assertEquals("Add", layer.getBlendModeName())
    }

    @Test
    fun `copyLayer preserves blendMode`() {
        val original = TextLayer().apply {
            blendMode = PorterDuff.Mode.MULTIPLY
        }

        val copy = original.copyLayer()
        assertEquals(original.blendMode, copy.blendMode)
        assertEquals(PorterDuff.Mode.MULTIPLY, copy.blendMode)
        assertNotEquals(original.id, copy.id)
    }

    @Test
    fun `reset blendMode restores to SRC_OVER`() {
        val layer = TextLayer().apply {
            blendMode = PorterDuff.Mode.SCREEN
        }
        assertEquals(PorterDuff.Mode.SCREEN, layer.blendMode)

        layer.blendMode = PorterDuff.Mode.SRC_OVER
        assertEquals(PorterDuff.Mode.SRC_OVER, layer.blendMode)
        assertEquals("Normal", layer.getBlendModeName())
    }
}
