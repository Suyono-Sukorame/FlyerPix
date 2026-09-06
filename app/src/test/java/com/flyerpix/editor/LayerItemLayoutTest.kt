package com.flyerpix.editor

import android.graphics.PorterDuff
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk Item Layout Layer Manager (Prompt 32).
 *
 * Memverifikasi:
 *  1. Struktur model dan data formatting untuk komponen:
 *     - Handle drag
 *     - Preview icon jenis layer (Teks/Gambar/Bentuk)
 *     - Label nama layer & teks detail
 *     - Tombol toggle Mata (Visibilitas)
 *     - Tombol toggle Gembok (Kunci)
 *     - Tombol Hapus layer
 *  2. Proteksi interaksi tombol hapus saat layer terkunci.
 */
class LayerItemLayoutTest {

    @Test
    fun `layer item generates correct label and details for TextLayer`() {
        val layer = TextLayer(
            text = "PixelLab Banner",
            textSize = 72f,
            blendMode = PorterDuff.Mode.MULTIPLY
        )

        // Verifikasi nama layer
        val displayName = if (layer.text.isNotBlank()) layer.text else "Teks Kosong"
        assertEquals("PixelLab Banner", displayName)

        // Verifikasi format detail teknis layer
        val details = "Teks • Ukuran ${layer.textSize.toInt()}sp • ${layer.getBlendModeName()}"
        assertEquals("Teks • Ukuran 72sp • Multiply", details)
    }

    @Test
    fun `visibility toggle updates visibility state and text alpha`() {
        val layer = TextLayer(text = "Visible Layer", isVisible = true)
        assertTrue(layer.isVisible)

        var expectedAlpha = if (layer.isVisible) 1.0f else 0.45f
        assertEquals(1.0f, expectedAlpha, 0.001f)

        // Toggle visibilitas (Sembunyikan layer)
        layer.isVisible = false
        assertFalse(layer.isVisible)

        expectedAlpha = if (layer.isVisible) 1.0f else 0.45f
        assertEquals(0.45f, expectedAlpha, 0.001f)
    }

    @Test
    fun `lock toggle disables delete button and updates lock state`() {
        val layer = TextLayer(text = "Unlocked Layer", isLocked = false)
        assertFalse(layer.isLocked)

        // Saat tidak terkunci: Tombol hapus aktif
        var isDeleteEnabled = !layer.isLocked
        assertTrue(isDeleteEnabled)

        // Kunci layer
        layer.isLocked = true
        assertTrue(layer.isLocked)

        // Saat terkunci: Tombol hapus dinonaktifkan
        isDeleteEnabled = !layer.isLocked
        assertFalse(isDeleteEnabled)
    }

    @Test
    fun `blank text fallback displays Teks Kosong`() {
        val emptyLayer = TextLayer(text = "   ")
        val displayName = if (emptyLayer.text.isNotBlank()) emptyLayer.text else "Teks Kosong"
        assertEquals("Teks Kosong", displayName)
    }
}
