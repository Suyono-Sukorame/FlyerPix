package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.PerspectivePreset
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk fitur Perspektif Warping (Matrix.setPolyToPoly) pada CanvasLayer / TextLayer (Prompt 22).
 *
 * Verifikasi:
 *  1. Nilai default: perspectiveEnabled = false, 4 sudut normal (0,0), (1,0), (1,1), (0,1)
 *  2. Mengaktifkan / menonaktifkan properti perspectiveEnabled
 *  3. getPerspectiveMatrix() mengembalikan null jika disabled atau width/height <= 0
 *  4. Preset perspektif (FLAT, LEFT_WALL, RIGHT_WALL, TOP_BILLBOARD, FLOOR_TILT) mengubah koordinat
 *  5. resetPerspective() mengembalikan sudut ke normal
 *  6. copyLayer() menyalin perspectiveEnabled dan menghasilkan klon independen dari perspectiveCorners
 *  7. Modifikasi sudut pada hasil copyLayer() tidak mempengaruhi layer asal (deep clone)
 */
class LayerPerspectiveWarpTest {

    private val defaultCorners = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        0f, 1f
    )

    @Test
    fun `default perspective values are disabled with standard rectangle corners`() {
        val layer = TextLayer()
        assertFalse(layer.perspectiveEnabled)
        assertEquals(8, layer.perspectiveCorners.size)
        assertArrayEquals(defaultCorners, layer.perspectiveCorners, 0.0001f)
    }

    @Test
    fun `perspectiveEnabled can be toggled`() {
        val layer = TextLayer().apply {
            perspectiveEnabled = true
        }
        assertTrue(layer.perspectiveEnabled)

        layer.perspectiveEnabled = false
        assertFalse(layer.perspectiveEnabled)
    }

    @Test
    fun `getPerspectiveMatrix returns null when perspective is disabled or dimensions invalid`() {
        val layer = TextLayer().apply {
            perspectiveEnabled = false
        }
        assertNull(layer.getPerspectiveMatrix(200f, 100f))
        assertNull(layer.getPerspectiveMatrix(0f, 0f))
        assertNull(layer.getPerspectiveMatrix(-50f, 100f))
    }

    @Test
    fun `applyPreset updates 4 corners to distinct perspective geometries`() {
        val layer = TextLayer().apply { perspectiveEnabled = true }

        layer.applyPerspectivePreset(PerspectivePreset.LEFT_WALL)
        assertFalse(defaultCorners.contentEquals(layer.perspectiveCorners))
        assertEquals(-0.2f, layer.perspectiveCorners[1], 0.0001f) // Top-Left Y naik ke atas
        assertEquals(1.2f, layer.perspectiveCorners[7], 0.0001f)  // Bottom-Left Y turun ke bawah

        layer.applyPerspectivePreset(PerspectivePreset.RIGHT_WALL)
        assertEquals(-0.2f, layer.perspectiveCorners[3], 0.0001f) // Top-Right Y naik ke atas
        assertEquals(1.2f, layer.perspectiveCorners[5], 0.0001f)  // Bottom-Right Y turun ke bawah

        layer.applyPerspectivePreset(PerspectivePreset.TOP_BILLBOARD)
        assertEquals(-0.15f, layer.perspectiveCorners[0], 0.0001f)
        assertEquals(1.15f, layer.perspectiveCorners[2], 0.0001f)

        layer.applyPerspectivePreset(PerspectivePreset.FLOOR_TILT)
        assertEquals(0.15f, layer.perspectiveCorners[0], 0.0001f)
        assertEquals(0.85f, layer.perspectiveCorners[2], 0.0001f)

        layer.applyPerspectivePreset(PerspectivePreset.FLAT)
        assertArrayEquals(defaultCorners, layer.perspectiveCorners, 0.0001f)
    }

    @Test
    fun `resetPerspective restores corners to flat rectangle`() {
        val layer = TextLayer().apply {
            perspectiveEnabled = true
            applyPerspectivePreset(PerspectivePreset.LEFT_WALL)
        }
        assertFalse(defaultCorners.contentEquals(layer.perspectiveCorners))

        layer.resetPerspective()
        assertArrayEquals(defaultCorners, layer.perspectiveCorners, 0.0001f)
    }

    @Test
    fun `copyLayer clones perspectiveEnabled and perspectiveCorners independently`() {
        val original = TextLayer().apply {
            perspectiveEnabled = true
            applyPerspectivePreset(PerspectivePreset.RIGHT_WALL)
        }

        val copy = original.copyLayer()
        assertTrue(copy.perspectiveEnabled)
        assertArrayEquals(original.perspectiveCorners, copy.perspectiveCorners, 0.0001f)
        assertNotEquals(original.id, copy.id)

        // Mutasi copy tidak boleh merusak original
        copy.perspectiveCorners[0] = 0.5f
        assertNotEquals(original.perspectiveCorners[0], copy.perspectiveCorners[0])
    }
}
