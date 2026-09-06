package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.PixelCanvasView.CanvasEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk state efek kanvas non-destruktif (Prompt 51).
 *
 * Memverifikasi:
 * 1. Semua efek nonaktif secara default.
 * 2. [PixelCanvasView.setEffectEnabled] mengaktifkan/menonaktifkan efek dengan benar.
 * 3. [PixelCanvasView.toggleEffect] membalik status dan mengembalikan status baru.
 * 4. [PixelCanvasView.activeEffectList] mencerminkan urutan efek yang aktif secara deterministik.
 */
class CanvasEffectTest {

    private fun createPixelCanvasView(): PixelCanvasView {
        return try {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val field = unsafeClass.getDeclaredField("theUnsafe")
            field.isAccessible = true
            val unsafe = field.get(null)
            val allocate = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
            allocate.invoke(unsafe, PixelCanvasView::class.java) as PixelCanvasView
        } catch (_: Throwable) {
            PixelCanvasView::class.java.getDeclaredConstructor().newInstance()
        }
    }

    @Test
    fun `efek kanvas nonaktif secara default`() {
        val view = createPixelCanvasView()

        assertFalse(view.isEffectEnabled(CanvasEffect.VIGNETTE))
        assertFalse(view.isEffectEnabled(CanvasEffect.NOISE))
        assertFalse(view.isEffectEnabled(CanvasEffect.FILTER))
        assertTrue(view.activeEffectList.isEmpty())
    }

    @Test
    fun `setEffectEnabled mengaktifkan dan menonaktifkan efek`() {
        val view = createPixelCanvasView()

        view.setEffectEnabled(CanvasEffect.VIGNETTE, true)
        assertTrue(view.isEffectEnabled(CanvasEffect.VIGNETTE))
        assertEquals(listOf(CanvasEffect.VIGNETTE), view.activeEffectList)

        view.setEffectEnabled(CanvasEffect.NOISE, true)
        assertTrue(view.isEffectEnabled(CanvasEffect.NOISE))
        assertEquals(
            listOf(CanvasEffect.VIGNETTE, CanvasEffect.NOISE),
            view.activeEffectList
        )

        view.setEffectEnabled(CanvasEffect.VIGNETTE, false)
        assertFalse(view.isEffectEnabled(CanvasEffect.VIGNETTE))
        assertEquals(listOf(CanvasEffect.NOISE), view.activeEffectList)
    }

    @Test
    fun `setEffectEnabled idempotent tidak menggandakan efek`() {
        val view = createPixelCanvasView()

        view.setEffectEnabled(CanvasEffect.FILTER, true)
        view.setEffectEnabled(CanvasEffect.FILTER, true)
        view.setEffectEnabled(CanvasEffect.FILTER, false)
        view.setEffectEnabled(CanvasEffect.FILTER, false)

        assertFalse(view.isEffectEnabled(CanvasEffect.FILTER))
        assertTrue(view.activeEffectList.isEmpty())
    }

    @Test
    fun `toggleEffect membalik status dan mengembalikan status baru`() {
        val view = createPixelCanvasView()

        val first = view.toggleEffect(CanvasEffect.NOISE)
        assertTrue(first)
        assertTrue(view.isEffectEnabled(CanvasEffect.NOISE))

        val second = view.toggleEffect(CanvasEffect.NOISE)
        assertFalse(second)
        assertFalse(view.isEffectEnabled(CanvasEffect.NOISE))
    }

    @Test
    fun `activeEffectList mempertahankan urutan aktivasi`() {
        val view = createPixelCanvasView()

        view.toggleEffect(CanvasEffect.NOISE)
        view.toggleEffect(CanvasEffect.FILTER)
        view.toggleEffect(CanvasEffect.VIGNETTE)

        assertEquals(
            listOf(CanvasEffect.NOISE, CanvasEffect.FILTER, CanvasEffect.VIGNETTE),
            view.activeEffectList
        )

        view.toggleEffect(CanvasEffect.NOISE)
        assertEquals(
            listOf(CanvasEffect.FILTER, CanvasEffect.VIGNETTE),
            view.activeEffectList
        )
    }
}