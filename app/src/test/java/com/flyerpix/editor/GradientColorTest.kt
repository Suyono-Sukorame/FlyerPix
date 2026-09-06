package com.flyerpix.editor

import android.graphics.Color
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GradientColorTest {

    @Test
    fun testDefaultGradientColor() {
        val colors = intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt())
        val gradient = GradientColor(colors = colors, name = "RedBlue")

        assertEquals(GradientType.LINEAR, gradient.type)
        assertEquals(0f, gradient.angle, 0.001f)
        assertEquals(2, gradient.colors.size)
        assertEquals("RedBlue", gradient.name)
    }

    @Test
    fun testPresetsNotEmpty() {
        val presets = GradientColor.PRESETS
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.size >= 10)

        // Pastikan ada preset dengan tipe LINEAR, RADIAL, dan SWEEP
        assertTrue(presets.any { it.type == GradientType.LINEAR })
        assertTrue(presets.any { it.type == GradientType.RADIAL })
        assertTrue(presets.any { it.type == GradientType.SWEEP })
    }

    @Test
    fun testTextLayerGradientProperties() {
        val layer = TextLayer(text = "Hello Gradient")
        assertFalse(layer.gradientEnabled)
        assertEquals(null, layer.gradient)

        val preset = GradientColor.PRESETS[0]
        layer.gradient = preset
        layer.gradientEnabled = true

        assertTrue(layer.gradientEnabled)
        assertNotNull(layer.gradient)
        assertEquals(preset.name, layer.gradient?.name)
    }

    @Test
    fun testTextLayerCopyIncludesGradient() {
        val layer = TextLayer(text = "Copy Test")
        val preset = GradientColor.PRESETS[1].copy(angle = 90f)
        layer.gradient = preset
        layer.gradientEnabled = true

        val copied = layer.copyLayer()
        assertTrue(copied.gradientEnabled)
        assertNotNull(copied.gradient)
        assertEquals(preset.name, copied.gradient?.name)
        assertEquals(90f, copied.gradient?.angle ?: 0f, 0.001f)
    }
}
