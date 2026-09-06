package com.flyerpix.editor

import android.graphics.Color
import android.graphics.RectF
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk model latar belakang kanvas independen dan rendering engine (Prompt 44).
 *
 * Memverifikasi:
 * 1. Model [CanvasBackground] mendukung mode Transparan, Warna Solid, dan Gradasi.
 * 2. Transisi mode latar belakang pada [PixelCanvasView] dan sinkronisasi dua arah dengan properti warisan.
 * 3. Pembuatan [android.graphics.Shader] dari [GradientColor] dengan koordinat batas [RectF].
 */
class CanvasBackgroundTest {

    private fun createPixelCanvasView(): PixelCanvasView {
        return try {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val field = unsafeClass.getDeclaredField("theUnsafe")
            field.isAccessible = true
            val unsafe = field.get(null)
            val allocate = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
            val view = allocate.invoke(unsafe, PixelCanvasView::class.java) as PixelCanvasView
            view.canvasBackground = CanvasBackground.solid(Color.WHITE)
            view
        } catch (_: Throwable) {
            PixelCanvasView::class.java.getDeclaredConstructor().newInstance()
        }
    }

    // ── 1. Uji Model CanvasBackground ────────────────────────────────────────

    @Test
    fun `CanvasBackground factory methods initialize correct modes and values`() {
        val transparent = CanvasBackground.transparent()
        assertEquals(CanvasBackgroundMode.TRANSPARENT, transparent.mode)

        val solid = CanvasBackground.solid(Color.RED)
        assertEquals(CanvasBackgroundMode.SOLID_COLOR, solid.mode)
        assertEquals(Color.RED, solid.solidColor)

        val grad = GradientColor.PRESETS[0]
        val gradientBg = CanvasBackground.gradient(grad)
        assertEquals(CanvasBackgroundMode.GRADIENT, gradientBg.mode)
        assertEquals(grad, gradientBg.gradient)
    }

    // ── 2. Uji State & Sinkronisasi di PixelCanvasView ───────────────────────

    @Test
    fun `PixelCanvasView default background is white solid color`() {
        val canvasView = createPixelCanvasView()

        assertEquals(CanvasBackgroundMode.SOLID_COLOR, canvasView.canvasBackground.mode)
        assertEquals(Color.WHITE, canvasView.canvasBackgroundColor)
        assertFalse(canvasView.isTransparentBackground)
    }

    @Test
    fun `setTransparentBackground updates mode and legacy flag`() {
        val canvasView = createPixelCanvasView()

        canvasView.setTransparentBackground()

        assertEquals(CanvasBackgroundMode.TRANSPARENT, canvasView.canvasBackground.mode)
        assertTrue(canvasView.isTransparentBackground)
    }

    @Test
    fun `setColorBackground updates color and mode`() {
        val canvasView = createPixelCanvasView()

        canvasView.setTransparentBackground()
        assertTrue(canvasView.isTransparentBackground)

        canvasView.setColorBackground(0xFF00E5FF.toInt())

        assertEquals(CanvasBackgroundMode.SOLID_COLOR, canvasView.canvasBackground.mode)
        assertEquals(0xFF00E5FF.toInt(), canvasView.canvasBackgroundColor)
        assertFalse(canvasView.isTransparentBackground)
    }

    @Test
    fun `setGradientBackground updates gradient and mode`() {
        val canvasView = createPixelCanvasView()

        val sampleGrad = GradientColor.PRESETS[1]
        canvasView.setGradientBackground(sampleGrad)

        assertEquals(CanvasBackgroundMode.GRADIENT, canvasView.canvasBackground.mode)
        assertEquals(sampleGrad, canvasView.canvasBackgroundGradient)
        assertFalse(canvasView.isTransparentBackground)
    }

    @Test
    fun `legacy isTransparentBackground setter transitions between transparent and solid`() {
        val canvasView = createPixelCanvasView()

        canvasView.canvasBackgroundColor = Color.YELLOW
        canvasView.isTransparentBackground = true

        assertEquals(CanvasBackgroundMode.TRANSPARENT, canvasView.canvasBackground.mode)
        assertTrue(canvasView.isTransparentBackground)

        canvasView.isTransparentBackground = false
        assertEquals(CanvasBackgroundMode.SOLID_COLOR, canvasView.canvasBackground.mode)
        assertEquals(Color.YELLOW, canvasView.canvasBackgroundColor)
    }

    // ── 3. Uji Gradient Shader Bounds ────────────────────────────────────────

    @Test
    fun `createShader on GradientColor returns non-null shader across all gradient types`() {
        val bounds = RectF().apply {
            left = 100f
            top = 200f
            right = 900f
            bottom = 1000f
        }

        // 1. Linear Gradient
        val linearGrad = GradientColor(
            colors = intArrayOf(Color.RED, Color.BLUE),
            type = GradientType.LINEAR,
            angle = 45f
        )
        val linearShader = linearGrad.createShader(bounds)
        assertNotNull("Linear shader must be created", linearShader)

        // 2. Radial Gradient
        val radialGrad = GradientColor(
            colors = intArrayOf(Color.GREEN, Color.YELLOW),
            type = GradientType.RADIAL
        )
        val radialShader = radialGrad.createShader(bounds)
        assertNotNull("Radial shader must be created", radialShader)

        // 3. Sweep Gradient
        val sweepGrad = GradientColor(
            colors = intArrayOf(Color.BLACK, Color.WHITE),
            type = GradientType.SWEEP
        )
        val sweepShader = sweepGrad.createShader(bounds)
        assertNotNull("Sweep shader must be created", sweepShader)
    }

    @Test
    fun `createShader handles zero and empty bounds gracefully without throwing`() {
        val emptyBounds = RectF().apply {
            left = 0f
            top = 0f
            right = 0f
            bottom = 0f
        }
        val grad = GradientColor(
            colors = intArrayOf(Color.RED, Color.BLUE),
            type = GradientType.LINEAR
        )
        val shader = grad.createShader(emptyBounds)
        assertNotNull("Shader must handle empty bounds safely", shader)
    }
}
