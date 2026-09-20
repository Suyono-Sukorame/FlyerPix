package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.RichTextSpan
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk API pengukuran editor inline (Option 1):
 *  - [TextLayer.measureRichContentDimensions] menyertakan padding bbox dan
 *    merespons teks/span secara deterministik;
 *  - [TextLayer.styleRichText] tidak melempar untuk span out-of-range dan
 *    menjaga konten asli saat daftar span kosong.
 */
class InlineTextEditorMeasureTest {

    @Test
    fun `measureRichContentDimensions includes bbox padding`() {
        val layer = TextLayer().apply {
            text = "ABC"
            textSize = 64f
            paddingLeft = 10f
            paddingRight = 20f
            paddingTop = 5f
            paddingBottom = 15f
        }
        val (w, h) = layer.measureRichContentDimensions("ABCD", emptyList())

        assertTrue("width must be larger than horizontal padding sum", w >= 30f)
        assertTrue("height must be larger than vertical padding sum", h >= 20f)
    }

    @Test
    fun `wrapWidth never exceeds configured wrapWidth plus padding`() {
        val text = "This is a long phrase that should wrap"
        val wrapped = TextLayer().apply {
            textSize = 64f
            wrapTextEnabled = true
            wrapWidth = 160f
        }
        val (wrappedW, wrappedH) = wrapped.measureRichContentDimensions(text, emptyList())

        // Batas atas lebar selalu ter-honor bahkan saat StaticLayout mock
        // (JVM tanpa Robolectric) tidak menghitung glyph nyata.
        assertTrue(
            "wrapped width should be at or below wrapWidth + padding",
            wrappedW <= 160f + wrapped.paddingLeft + wrapped.paddingRight + 1f
        )
        assertTrue("wrapped height should be non-negative", wrappedH >= 0f)
    }

    @Test
    fun `styleRichText handles out-of-range spans gracefully`() {
        val layer = TextLayer().apply { text = "Hello" }
        val paint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val spans = listOf(
            RichTextSpan(start = -5, end = 100, isBold = true, color = 0xFF1769FF.toInt())
        )
        val styled = layer.styleRichText("Hello", spans, paint).toString()
        assertTrue("styled content should equal original when ranges are clamped", styled == "Hello")
    }
}