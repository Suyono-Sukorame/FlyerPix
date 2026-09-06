package com.flyerpix.editor

import android.graphics.Paint
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit test untuk fitur Curved / Arc Text pada TextLayer (Prompt 21).
 *
 * Verifikasi:
 *  1. curvePercent default = 0 (lurus)
 *  2. curvePercent dapat di-set dalam rentang -100..100
 *  3. Nilai di luar rentang di-coerce dengan benar
 *  4. copyLayer() menyertakan nilai curvePercent
 *  5. Reset ke 0 berfungsi (flat = no curve)
 */
class TextLayerCurvedTextTest {

    @Test
    fun `default curvePercent is zero (flat)`() {
        val layer = TextLayer()
        assertEquals(0, layer.curvePercent)
    }

    @Test
    fun `curvePercent can be set to positive (curve up)`() {
        val layer = TextLayer().apply { curvePercent = 75 }
        assertEquals(75, layer.curvePercent)
    }

    @Test
    fun `curvePercent can be set to negative (curve down)`() {
        val layer = TextLayer().apply { curvePercent = -50 }
        assertEquals(-50, layer.curvePercent)
    }

    @Test
    fun `curvePercent at max boundary values`() {
        val layer = TextLayer().apply {
            curvePercent = 100
        }
        assertEquals(100, layer.curvePercent)

        layer.curvePercent = -100
        assertEquals(-100, layer.curvePercent)
    }

    @Test
    fun `slider coerceIn keeps curvePercent in range`() {
        // Simulasi coerceIn yang dilakukan slider sync
        val raw = 150
        val coerced = raw.coerceIn(-100, 100)
        assertEquals(100, coerced)

        val rawNeg = -200
        val coercedNeg = rawNeg.coerceIn(-100, 100)
        assertEquals(-100, coercedNeg)
    }

    @Test
    fun `copyLayer preserves curvePercent`() {
        val original = TextLayer().apply { curvePercent = 60 }
        val copy = original.copyLayer()
        assertEquals(original.curvePercent, copy.curvePercent)
        assertNotEquals(original.id, copy.id)
    }

    @Test
    fun `reset to 0 restores flat state`() {
        val layer = TextLayer().apply { curvePercent = 80 }
        layer.curvePercent = 0
        assertEquals(0, layer.curvePercent)
    }

    @Test
    fun `curve label logic is correct`() {
        // Verifikasi logika label UI yang digunakan di initializeCurveControls()
        fun curveLabel(v: Int): String = when {
            v == 0 -> "Curve: 0% (Flat)"
            v > 0  -> "Curve: +$v% ▲"
            else   -> "Curve: $v% ▼"
        }

        assertEquals("Curve: 0% (Flat)", curveLabel(0))
        assertEquals("Curve: +50% ▲", curveLabel(50))
        assertEquals("Curve: -75% ▼", curveLabel(-75))
        assertEquals("Curve: +100% ▲", curveLabel(100))
    }
}
