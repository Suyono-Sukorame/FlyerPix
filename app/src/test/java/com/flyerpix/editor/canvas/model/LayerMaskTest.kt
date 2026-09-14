package com.flyerpix.editor.canvas.model

import com.flyerpix.editor.project.model.LayerDto
import com.flyerpix.editor.project.model.ProjectDto
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LayerMaskTest (Prompt 05) - JVM test untuk layer mask 8-bit non-destruktif:
 * matematika komposit mask (setara PorterDuff DST_IN), pengaruh mask pada
 * contentBlurSignature, deep-copy via cloneLayer, dan round-trip DTO via Gson
 * (tanpa Android runtime / Bitmap).
 */
class LayerMaskTest {

    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    /**
     * Komposit alpha setara `(src) dstIn (mask)` pada ruang ARGB:
     * out.alpha = src.alpha * mask.alpha / 255 (RGB tidak berubah).
     * `inverted` membalik mask: 0 <-> 255.
     */
    private fun maskComposite(src: Int, mask: Int, inverted: Boolean): Int {
        val ma = if (inverted) {
            255 - ((mask ushr 24) and 0xFF)
        } else {
            (mask ushr 24) and 0xFF
        }
        val sa = (src ushr 24) and 0xFF
        val out = (sa * ma) / 255
        return (out shl 24) or (src and 0x00FFFFFF)
    }

    private fun argb(a: Int, rgb: Int): Int = (a shl 24) or (rgb and 0x00FFFFFF)

    @Test
    fun `solid black mask hides center region`() {
        val src = argb(255, 0x336699)
        val centerMask = argb(0, 0x000000)   // alpha 0 -> hidden
        val outsideMask = argb(255, 0xFFFFFF) // alpha 255 -> visible
        assertEquals(0, maskComposite(src, centerMask, false) ushr 24)
        assertEquals(255, maskComposite(src, outsideMask, false) ushr 24)
    }

    @Test
    fun `inverted mask shows opposite region`() {
        val src = argb(255, 0x88CC00)
        val black = argb(0, 0x000000)
        val white = argb(255, 0xFFFFFF)
        assertEquals(255, maskComposite(src, black, true) ushr 24)   // center revealed
        assertEquals(0, maskComposite(src, white, true) ushr 24)     // outside hidden
    }

    @Test
    fun `gray gradient produces partial alpha`() {
        val src = argb(255, 0xABCDEF)
        val gray128 = argb(128, 0x808080)
        val result = maskComposite(src, gray128, false) ushr 24
        assertEquals(128, result)
        val gray64 = argb(64, 0x404040)
        assertEquals(64, maskComposite(src, gray64, false) ushr 24)
    }

    @Test
    fun `mask flags change content blur signature`() {
        val sharedId = "mask-sig"
        val plain = TextLayer(text = "M", x = 5f, y = 6f, id = sharedId)
        val masked = TextLayer(text = "M", x = 5f, y = 6f, id = sharedId)
        masked.maskEnabled = true
        assertNotEquals(plain.contentBlurSignature(), masked.contentBlurSignature())
        masked.maskInverted = true
        assertNotEquals(plain.contentBlurSignature(), masked.contentBlurSignature())
        masked.maskGeneration = 7
        val beforeGen = masked.contentBlurSignature()
        masked.maskGeneration = 8
        assertNotEquals(beforeGen, masked.contentBlurSignature())
    }

    @Test
    fun `cloneLayer preserves mask flags`() {
        val a = TextLayer(text = "Masked", x = 10f, y = 20f, id = "clone-mask")
        a.maskEnabled = true
        a.maskInverted = true
        a.maskGeneration = 3
        val b = a.cloneLayer()
        assertTrue(b.maskEnabled)
        assertTrue(b.maskInverted)
        assertEquals(3, b.maskGeneration)
    }

    @Test
    fun `layer mask fields survive gson round trip`() {
        val dto = ProjectDto(
            projectName  = "P5",
            canvasWidth  = 1000,
            canvasHeight = 1000,
            layers = listOf(
                LayerDto(
                    type          = "TEXT",
                    id            = "mask-1",
                    text          = "Masked",
                    textSize      = 48f,
                    maskEnabled   = true,
                    maskInverted  = true,
                    maskBase64    = null,
                    maskGeneration = 5
                )
            )
        )
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, ProjectDto::class.java)
        val layer = back.layers.first()
        assertTrue(layer.maskEnabled)
        assertTrue(layer.maskInverted)
        assertEquals(5, layer.maskGeneration)
    }
}