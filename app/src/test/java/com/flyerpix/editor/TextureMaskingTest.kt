package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextureMaskingTest {

    @Test
    fun testTextLayerTextureDefaultValues() {
        val layer = TextLayer(text = "Texture Test")
        assertFalse(layer.textureEnabled)
        assertNull(layer.textureBitmap)
        assertEquals(1.0f, layer.textureScale, 0.001f)
        assertEquals(0f, layer.textureRotation, 0.001f)
    }

    @Test
    fun testTextLayerTextureConfig() {
        val layer = TextLayer(text = "Texture Config")
        layer.textureEnabled = true
        layer.textureScale = 1.8f
        layer.textureRotation = 45f

        assertTrue(layer.textureEnabled)
        assertEquals(1.8f, layer.textureScale, 0.001f)
        assertEquals(45f, layer.textureRotation, 0.001f)
    }

    @Test
    fun testTextLayerCopyIncludesTextureProperties() {
        val layer = TextLayer(text = "Copy Texture")
        layer.textureEnabled = true
        layer.textureScale = 2.5f
        layer.textureRotation = 180f

        val copied = layer.copyLayer()
        assertTrue(copied.textureEnabled)
        assertEquals(2.5f, copied.textureScale, 0.001f)
        assertEquals(180f, copied.textureRotation, 0.001f)
    }

    @Test
    fun testCreateTextureShaderNullWhenBitmapNull() {
        val layer = TextLayer(text = "Shader Null Test")
        layer.textureEnabled = true
        layer.textureBitmap = null

        assertNull(layer.createTextureShader())
    }
}
