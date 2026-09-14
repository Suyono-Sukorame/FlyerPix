package com.flyerpix.editor.canvas.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MaskUtilsTest (Prompt 07) - JVM test untuk gradient mask & feather:
 * smoothstep monotonic, nilai ujung, dan simulasi blur edge. Buffer & native
 * blur tidak dijalankan di JVM (tanpa Android runtime).
 */
class MaskUtilsTest {

    @Test
    fun `smoothstep is monotonic and ranges 0 to 1`() {
        val values = MaskUtils.smoothstepValues(101)
        assertEquals(0f, values.first(), 1e-4f)
        assertEquals(1f, values.last(), 1e-4f)
        for (i in 1 until values.size) {
            assertTrue("non-decreasing at $i", values[i] >= values[i - 1])
            assertTrue(values[i] >= 0f && values[i] <= 1f)
        }
    }

    @Test
    fun `smoothstep endpoints and midpoint`() {
        assertEquals(0f, MaskUtils.smoothstep(0f), 1e-4f)
        assertEquals(1f, MaskUtils.smoothstep(1f), 1e-4f)
        assertEquals(0.5f, MaskUtils.smoothstep(0.5f), 1e-4f)
        assertEquals(0f, MaskUtils.smoothstep(-1f), 1e-4f)
        assertEquals(1f, MaskUtils.smoothstep(2f), 1e-4f)
    }

    @Test
    fun `linear gradient alpha increases along direction`() {
        // Simulasi inti generateGradientMask: alpha = smoothstep(y / height)
        val h = 64
        val alphas = (0 until h).map { y -> (MaskUtils.smoothstep(y.toFloat() / h) * 255).toInt() }
        assertTrue(alphas[0] == 0)
        assertTrue("top reaches near max", alphas[h - 1] >= 250)
        var prev = alphas[0]
        for (a in alphas.drop(1)) {
            assertTrue("monotonic alpha", a >= prev)
            prev = a
        }
    }

    @Test
    fun `radial gradient peaks at center`() {
        val w = 64
        val h = 64
        val cx = w / 2f
        val cy = h / 2f
        val maxR = 0.5f * kotlin.math.min(w, h)
        fun radialAlpha(x: Int, y: Int): Int {
            val dx = x - cx
            val dy = y - cy
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            return (MaskUtils.smoothstep(1f - dist / maxR) * 255).toInt()
        }
        val center = radialAlpha(cx.toInt(), cy.toInt())
        val corner = radialAlpha(0, 0)
        assertTrue(center > corner)
        assertTrue(center >= 250)
        assertTrue(corner < 20)
    }

    @Test
    fun `feather turns sharp edge into gradual transition`() {
        // Simulasi box-average blur satu pass pada alpha edge: [0..0 | 255..255]
        var edge = (0 until 32).map { if (it < 16) 0 else 255 }.toIntArray()
        val blurred = IntArray(edge.size)
        for (i in edge.indices) {
            val lo = (i - 2).coerceAtLeast(0)
            val hi = (i + 2).coerceAtMost(edge.size - 1)
            var s = 0
            for (j in lo..hi) s += edge[j]
            blurred[i] = s / (hi - lo + 1)
        }
        // internal ids now ramp
        val interior = blurred[15]
        assertTrue("sharp edge becomes gradual", interior in 1..254)
        val monotonic = (1 until blurred.size).all { blurred[it] >= blurred[it - 1] }
        assertTrue("feather preserves monotonic ramp", monotonic)
    }
}