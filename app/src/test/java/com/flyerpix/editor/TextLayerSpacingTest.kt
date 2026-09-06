package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit test untuk fitur Text Spacing (Kerning & Leading) pada TextLayer (Prompt 23).
 *
 * Verifikasi:
 *  1. Nilai default: letterSpacing = 0f, lineSpacing = 0f
 *  2. Pengaturan nilai letterSpacing (positif = renggang, negatif = rapat)
 *  3. Pengaturan nilai lineSpacing (positif = spasi baris lebar, negatif = baris rapat)
 *  4. copyLayer() mempertahankan letterSpacing dan lineSpacing
 *  5. Reset spacing mengembalikan keduanya ke 0f
 *  6. Batas nilai slider coerceIn beroperasi dengan benar
 */
class TextLayerSpacingTest {

    @Test
    fun `default letterSpacing and lineSpacing are zero`() {
        val layer = TextLayer()
        assertEquals(0f, layer.letterSpacing, 0.0001f)
        assertEquals(0f, layer.lineSpacing, 0.0001f)
    }

    @Test
    fun `letterSpacing can be set to positive and negative values`() {
        val layer = TextLayer().apply {
            letterSpacing = 0.25f
        }
        assertEquals(0.25f, layer.letterSpacing, 0.0001f)

        layer.letterSpacing = -0.10f
        assertEquals(-0.10f, layer.letterSpacing, 0.0001f)
    }

    @Test
    fun `lineSpacing can be set to custom pixel increments`() {
        val layer = TextLayer().apply {
            lineSpacing = 24f
        }
        assertEquals(24f, layer.lineSpacing, 0.0001f)

        layer.lineSpacing = -8f
        assertEquals(-8f, layer.lineSpacing, 0.0001f)
    }

    @Test
    fun `copyLayer preserves letterSpacing and lineSpacing`() {
        val original = TextLayer().apply {
            letterSpacing = 0.35f
            lineSpacing = 16f
        }

        val copy = original.copyLayer()
        assertEquals(original.letterSpacing, copy.letterSpacing, 0.0001f)
        assertEquals(original.lineSpacing, copy.lineSpacing, 0.0001f)
        assertNotEquals(original.id, copy.id)
    }

    @Test
    fun `reset spacing restores values to zero`() {
        val layer = TextLayer().apply {
            letterSpacing = 0.5f
            lineSpacing = 40f
        }
        assertEquals(0.5f, layer.letterSpacing, 0.0001f)
        assertEquals(40f, layer.lineSpacing, 0.0001f)

        // Reset
        layer.letterSpacing = 0f
        layer.lineSpacing = 0f

        assertEquals(0f, layer.letterSpacing, 0.0001f)
        assertEquals(0f, layer.lineSpacing, 0.0001f)
    }

    @Test
    fun `slider ranges coerce properly`() {
        val rawLetter = 1.5f
        val coercedLetter = rawLetter.coerceIn(-0.2f, 1.0f)
        assertEquals(1.0f, coercedLetter, 0.0001f)

        val rawLetterNeg = -0.5f
        val coercedLetterNeg = rawLetterNeg.coerceIn(-0.2f, 1.0f)
        assertEquals(-0.2f, coercedLetterNeg, 0.0001f)

        val rawLine = 120f
        val coercedLine = rawLine.coerceIn(-20f, 80f)
        assertEquals(80f, coercedLine, 0.0001f)

        val rawLineNeg = -50f
        val coercedLineNeg = rawLineNeg.coerceIn(-20f, 80f)
        assertEquals(-20f, coercedLineNeg, 0.0001f)
    }
}
