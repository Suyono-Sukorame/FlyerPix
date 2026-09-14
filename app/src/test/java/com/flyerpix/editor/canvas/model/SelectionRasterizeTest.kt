package com.flyerpix.editor.canvas.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SelectionRasterizeTest (Prompt 10) - JVM equivalent dari logika rasterize
 * seleksi → layer mask. Memverifikasi koordinat mask lokal (identik
 * canvasToMaskLocal: translate → rotate → scale, semuanya sekitar pusat lokal)
 * & klasifikasi piksel di dalam/di luar persegi.
 */
class SelectionRasterizeTest {

    /** Inverse transform → koordinat layer lokal (identik canvasToMaskLocal). */
    private fun mapToLocal(px: Float, py: Float, lx: Float, ly: Float, scale: Float, rotation: Float, w: Float, h: Float): Pair<Float, Float> {
        val cx = w / 2f
        val cy = h / 2f
        val tx = px - lx
        val ty = py - ly
        val rad = Math.toRadians(-rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val x2 = cx + (tx - cx) * cos - (ty - cy) * sin
        val y2 = cy + (tx - cx) * sin + (ty - cy) * cos
        val rx = cx + (x2 - cx) / scale
        val ry = cy + (y2 - cy) / scale
        return rx to ry
    }

    @Test
    fun `rect selection at zero layer transform maps to same mask rect`() {
        val (lx, ly) = mapToLocal(100f, 50f, 0f, 0f, 1f, 0f, 200f, 200f)
        assertEquals(100f, lx, 0.01f)
        assertEquals(50f, ly, 0.01f)
        val inside = lx in 0f..200f && ly in 0f..200f
        assertEquals(true, inside)
    }

    @Test
    fun `scaled layer magnifies canvas distance into mask region`() {
        val (lx, ly) = mapToLocal(100f, 100f, 0f, 0f, 0.5f, 0f, 400f, 400f)
        assertEquals(0f, lx, 0.01f)
        assertEquals(0f, ly, 0.01f)
        val (lx2, ly2) = mapToLocal(300f, 300f, 0f, 0f, 0.5f, 0f, 400f, 400f)
        assertEquals(400f, lx2, 0.01f)
        assertEquals(400f, ly2, 0.01f)
    }

    @Test
    fun `rotated layer maps corners consistently with quarter turn`() {
        val (lx, ly) = mapToLocal(200f, -100f, 0f, 0f, 1f, 90f, 200f, 200f)
        assertEquals(-100f, lx, 0.01f)
        assertEquals(0f, ly, 0.01f)
    }

    @Test
    fun `point outside selection rect stays masked out`() {
        val (lx, ly) = mapToLocal(500f, 500f, 0f, 0f, 1f, 0f, 200f, 200f)
        val inside = lx in 0f..200f && ly in 0f..200f
        assertEquals(false, inside)
    }
}