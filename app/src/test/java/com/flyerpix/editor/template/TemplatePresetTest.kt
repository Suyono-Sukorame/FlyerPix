package com.flyerpix.editor.template

import android.graphics.Color
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk arsitektur Template Preset otentik PixelLab.
 */
class TemplatePresetTest {

    private fun createPixelCanvasView(): PixelCanvasView {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null)
        val allocate = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        val view = allocate.invoke(unsafe, PixelCanvasView::class.java) as PixelCanvasView
        view.canvasBackground = CanvasBackground.solid(Color.WHITE)

        val layersField = PixelCanvasView::class.java.getDeclaredField("layers")
        layersField.isAccessible = true
        layersField.set(view, mutableListOf<CanvasLayer>())

        return view
    }

    private lateinit var canvasView: PixelCanvasView

    @Before
    fun setUp() {
        canvasView = createPixelCanvasView()
    }

    @Test
    fun `test default radial colors match authentic PixelLab`() {
        val colors = TemplatePreset.DEFAULT_RADIAL_COLORS
        assertEquals(2, colors.size)
        assertEquals(0xFF739281.toInt(), colors[0])
        assertEquals(0xFF2E473B.toInt(), colors[1])
    }

    @Test
    fun `test getBuiltinPresets contains all authentic presets`() {
        val presets = TemplatePreset.getBuiltinPresets()
        assertTrue("Preset count should be at least 6", presets.size >= 6)

        val titles = presets.map { it.title }
        assertTrue("Contains My Projects", titles.contains("My Projects"))
        assertTrue("Contains Default", titles.contains("Default"))
        assertTrue("Contains Thin", titles.contains("Thin"))
        assertTrue("Contains Thin Dark", titles.contains("Thin Dark"))
        assertTrue("Contains Keep Calm", titles.contains("Keep Calm"))
        assertTrue("Contains Meme", titles.contains("Meme"))
        assertTrue("Contains 3D", titles.contains("3D"))
    }

    @Test
    fun `test applyDefaultPixelLabState configures radial gradient and New Text layer`() {
        TemplatePreset.applyDefaultPixelLabState(canvasView)

        // 1. Verifikasi background gradasi radial
        val bg = canvasView.canvasBackground
        assertEquals(CanvasBackgroundMode.GRADIENT, bg.mode)
        assertNotNull(bg.gradient)
        assertEquals(GradientType.RADIAL, bg.gradient?.type)
        assertEquals("PixelLab Default", bg.gradient?.name)
        assertArrayEquals(TemplatePreset.DEFAULT_RADIAL_COLORS, bg.gradient?.colors)

        // 2. Verifikasi layer teks New Text
        assertEquals(1, canvasView.layers.size)
        val textLayer = canvasView.layers.first() as TextLayer
        assertEquals("New Text", textLayer.text)
        assertEquals(Color.WHITE, textLayer.textColor)
        assertEquals(canvasView.selectedLayer, textLayer)
    }

    @Test
    fun `test applying keep calm preset sets solid red background and bold text`() {
        val keepCalmPreset = TemplatePreset.getBuiltinPresets().first { it.id == "keep_calm" }
        keepCalmPreset.applyToCanvas(canvasView)

        val bg = canvasView.canvasBackground
        assertEquals(CanvasBackgroundMode.SOLID_COLOR, bg.mode)
        assertEquals(0xFFCC181E.toInt(), bg.solidColor)

        assertEquals(1, canvasView.layers.size)
        val textLayer = canvasView.layers.first() as TextLayer
        assertTrue(textLayer.text.contains("KEEP CALM"))
    }

    @Test
    fun `test applying 3d preset sets 3d text extrusion`() {
        val threeDPreset = TemplatePreset.getBuiltinPresets().first { it.id == "three_d" }
        threeDPreset.applyToCanvas(canvasView)

        val bg = canvasView.canvasBackground
        assertEquals(CanvasBackgroundMode.GRADIENT, bg.mode)

        val textLayer = canvasView.layers.first() as TextLayer
        assertTrue(textLayer.extrudeEnabled)
        assertEquals(16, textLayer.extrudeDepth)
    }

    @Test
    fun `test applying meme preset creates two text layers`() {
        val memePreset = TemplatePreset.getBuiltinPresets().first { it.id == "meme" }
        memePreset.applyToCanvas(canvasView)

        assertEquals(2, canvasView.layers.size)
        val top = canvasView.layers[0] as TextLayer
        val bottom = canvasView.layers[1] as TextLayer
        assertEquals("TOP TEXT", top.text)
        assertEquals("BOTTOM TEXT", bottom.text)
    }
}
