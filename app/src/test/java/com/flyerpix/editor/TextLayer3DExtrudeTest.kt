package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.ExtrudeViewType
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextLayer3DExtrudeTest {

    @Test
    fun testDefaultExtrudeProperties() {
        val layer = TextLayer(text = "3D Test")
        assertFalse(layer.extrudeEnabled)
        assertEquals(10, layer.extrudeDepth)
        assertEquals(ExtrudeViewType.OBLIQUE, layer.extrudeViewType)
        assertEquals(45f, layer.extrudeAngle, 0.001f)
        assertEquals(0xFF333333.toInt(), layer.extrudeColor)
    }

    @Test
    fun testConfigExtrudeProperties() {
        val layer = TextLayer(text = "3D Config")
        layer.extrudeEnabled = true
        layer.extrudeDepth = 25
        layer.extrudeViewType = ExtrudeViewType.ISOMETRIC
        layer.extrudeColor = 0xFF1A237E.toInt()

        assertTrue(layer.extrudeEnabled)
        assertEquals(25, layer.extrudeDepth)
        assertEquals(ExtrudeViewType.ISOMETRIC, layer.extrudeViewType)
        assertEquals(0xFF1A237E.toInt(), layer.extrudeColor)
    }

    @Test
    fun testExtrudeVectorCalculation() {
        val layer = TextLayer(text = "Vector Test")
        layer.extrudeViewType = ExtrudeViewType.OBLIQUE
        layer.extrudeAngle = 0f

        val (dx0, dy0) = layer.getExtrudeVector()
        assertEquals(1.0f, dx0, 0.01f)
        assertEquals(0.0f, dy0, 0.01f)

        layer.extrudeAngle = 90f
        val (dx90, dy90) = layer.getExtrudeVector()
        assertEquals(0.0f, dx90, 0.01f)
        assertEquals(1.0f, dy90, 0.01f)

        layer.extrudeViewType = ExtrudeViewType.ISOMETRIC
        val (dxIso, dyIso) = layer.getExtrudeVector()
        assertTrue(dxIso > 0f)
        assertTrue(dyIso > 0f)
    }

    @Test
    fun testCopyLayerPreservesExtrude() {
        val layer = TextLayer(text = "Copy 3D")
        layer.extrudeEnabled = true
        layer.extrudeDepth = 30
        layer.extrudeAngle = 135f
        layer.extrudeViewType = ExtrudeViewType.ISOMETRIC
        layer.extrudeColor = 0xFF000000.toInt()

        val copied = layer.copyLayer()
        assertTrue(copied.extrudeEnabled)
        assertEquals(30, copied.extrudeDepth)
        assertEquals(135f, copied.extrudeAngle, 0.001f)
        assertEquals(ExtrudeViewType.ISOMETRIC, copied.extrudeViewType)
        assertEquals(0xFF000000.toInt(), copied.extrudeColor)
    }
}
