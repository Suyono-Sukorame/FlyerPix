package com.flyerpix.editor.canvas.model

import com.google.gson.GsonBuilder
import com.flyerpix.editor.project.model.ProjectDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LayerAdjustmentsTest (Prompt 03) - JVM test untuk model adjustment per-layer:
 * netral, isActive, deep-copy pada cloneLayer, perbedaan signature konten,
 * serta round-trip DTO via Gson (tanpa Android runtime).
 */
class LayerAdjustmentsTest {

    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    @Test
    fun `neutral adjustments are inactive`() {
        val a = LayerAdjustments()
        assertFalse(a.isActive)
    }

    @Test
    fun `any nonzero parameter activates adjustment`() {
        assertTrue(LayerAdjustments(exposure = 10f).isActive)
        assertTrue(LayerAdjustments(highlights = -20f).isActive)
        assertTrue(LayerAdjustments(shadows = 5f).isActive)
        assertTrue(LayerAdjustments(temperature = 15f).isActive)
        assertTrue(LayerAdjustments(tint = -8f).isActive)
        assertTrue(LayerAdjustments(gamma = 12f).isActive)
        assertTrue(LayerAdjustments(vibrance = 30f).isActive)
        assertTrue(LayerAdjustments(hue = 45f).isActive)
        assertTrue(LayerAdjustments(contrast = 20f).isActive)
        assertTrue(LayerAdjustments(saturation = -60f).isActive)
        assertTrue(LayerAdjustments(exposure = 0f, highlights = 0f, shadows = 0f,
            temperature = 0f, tint = 0f, gamma = 0f, vibrance = 0f, hue = 0f,
            contrast = 0f, saturation = 0f).isActive.not())
    }

    @Test
    fun `cloneLayer deep copies adjustments instead of sharing reference`() {
        val original = TextLayer(text = "X").apply {
            adjustmentsEnabled = true
            adjustments.exposure = 30f
            adjustments.hue = 90f
        }
        val copy = original.cloneLayer()

        assertEquals(true, copy.adjustmentsEnabled)
        assertEquals(30f, copy.adjustments.exposure, 0f)
        assertEquals(90f, copy.adjustments.hue, 0f)

        copy.adjustments.exposure = -40f
        assertEquals(30f, original.adjustments.exposure, 0f)
        copy.adjustments.hue = 0f
        assertEquals(90f, original.adjustments.hue, 0f)
    }

    @Test
    fun `identical layers with different adjustments produce different content signatures`() {
        val a = TextLayer(text = "Same", id = "sig-a")
        val b = TextLayer(text = "Same", id = "sig-a")
        a.adjustmentsEnabled = true
        a.adjustments.temperature = 25f
        b.adjustmentsEnabled = true

        assertNotEquals(a.contentBlurSignature(), b.contentBlurSignature())
    }

    @Test
    fun `resetting adjustment restores signature equal to layer without adjustment`() {
        val sharedId = "same-id"
        val plain = TextLayer(text = "Same", x = 5f, y = 6f, id = sharedId)
        val adjusted = TextLayer(text = "Same", x = 5f, y = 6f, id = sharedId)
        adjusted.adjustmentsEnabled = true
        adjusted.adjustments.gamma = 50f
        adjusted.adjustments.vibrance = -30f

        assertNotEquals(plain.contentBlurSignature(), adjusted.contentBlurSignature())

        adjusted.adjustmentsEnabled = false
        adjusted.adjustments.gamma = 0f
        adjusted.adjustments.vibrance = 0f
        assertEquals(plain.contentBlurSignature(), adjusted.contentBlurSignature())
    }

    @Test
    fun `preset params fill multiple fields at once`() {
        val faded = com.flyerpix.editor.ui.compose.layerAdjustPresetParams("Faded")
        assertEquals(-45f, faded.vibrance, 0f)
        assertEquals(-25f, faded.contrast, 0f)
        assertEquals(0f, faded.saturation, 0f)
        assertEquals(0f, faded.temperature, 0f)

        val punchy = com.flyerpix.editor.ui.compose.layerAdjustPresetParams("Punchy")
        assertEquals(45f, punchy.vibrance, 0f)
        assertEquals(25f, punchy.contrast, 0f)

        val warm = com.flyerpix.editor.ui.compose.layerAdjustPresetParams("Warm")
        assertEquals(35f, warm.temperature, 0f)
        assertEquals(0f, warm.contrast, 0f)

        val bw = com.flyerpix.editor.ui.compose.layerAdjustPresetParams("B&W")
        assertEquals(-95f, bw.saturation, 0f)

        val normal = com.flyerpix.editor.ui.compose.layerAdjustPresetParams("Normal")
        assertFalse(normal.saturation != 0f || normal.vibrance != 0f || normal.contrast != 0f || normal.temperature != 0f)
    }

    @Test
    fun `layer adjustment fields survive gson round trip`() {
        val dto = ProjectDto(
            projectName  = "P3",
            canvasWidth  = 1000,
            canvasHeight = 1000,
            layers = listOf(
                com.flyerpix.editor.project.model.LayerDto(
                    type = "TEXT",
                    id = "t1",
                    text = "Hi",
                    adjustmentsEnabled = true,
                    exposure = 12.5f,
                    shadows = -7f,
                    gamma = 20f,
                    hue = 45f,
                    contrast = 15f,
                    saturation = -30f,
                    preset = "Punchy"
                )
            )
        )

        val json = gson.toJson(dto)
        val back = gson.fromJson(json, ProjectDto::class.java)

        assertNotNull(back.layers.firstOrNull())
        val layer = back.layers[0]
        assertTrue(layer.adjustmentsEnabled)
        assertEquals(12.5f, layer.exposure!!, 0f)
        assertEquals(-7f, layer.shadows!!, 0f)
        assertEquals(20f, layer.gamma!!, 0f)
        assertEquals(45f, layer.hue!!, 0f)
        assertEquals(15f, layer.contrast!!, 0f)
        assertEquals(-30f, layer.saturation!!, 0f)
        assertEquals("Punchy", layer.preset)
        assertEquals(null, layer.tint)
        assertEquals(null, layer.temperature)
    }
}