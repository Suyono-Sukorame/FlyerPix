package com.flyerpix.editor.template

import android.graphics.Color
import android.graphics.Typeface
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.TextLayer

/**
 * Model data untuk Template Preset otentik PixelLab.
 */
data class TemplatePreset(
    val id: String,
    val title: String,
    val isMyProjects: Boolean = false,
    val previewBgColors: IntArray = intArrayOf(0xFF739281.toInt(), 0xFF2E473B.toInt()),
    val isRadial: Boolean = true,
    val previewTextColor: Int = Color.WHITE,
    val applyToCanvas: (PixelCanvasView) -> Unit
) {
    companion object {
        /**
         * Warna gradasi radial khas default PixelLab.
         */
        val DEFAULT_RADIAL_COLORS = intArrayOf(0xFF739281.toInt(), 0xFF2E473B.toInt())

        /**
         * Menerapkan tampilan awal otentik PixelLab ke kanvas.
         */
        fun applyDefaultPixelLabState(canvasView: PixelCanvasView) {
            canvasView.clearLayers()
            canvasView.setGradientBackground(
                GradientColor(
                    name = "PixelLab Default",
                    colors = DEFAULT_RADIAL_COLORS.clone(),
                    type = GradientType.RADIAL
                )
            )
            val textLayer = TextLayer(
                text = "New Text",
                textColor = Color.WHITE,
                textSize = 36f
            )
            canvasView.addLayer(textLayer)
            canvasView.selectedLayer = textLayer
            canvasView.invalidate()
        }

        /**
         * Daftar preset otentik bawaan PixelLab.
         */
        fun getBuiltinPresets(): List<TemplatePreset> = listOf(
            TemplatePreset(
                id = "my_projects",
                title = "My Projects",
                isMyProjects = true,
                applyToCanvas = { /* Handled via project picker */ }
            ),
            TemplatePreset(
                id = "default",
                title = "Default",
                previewBgColors = DEFAULT_RADIAL_COLORS,
                isRadial = true,
                applyToCanvas = { canvas ->
                    applyDefaultPixelLabState(canvas)
                }
            ),
            TemplatePreset(
                id = "thin",
                title = "Thin",
                previewBgColors = intArrayOf(0xFF3A3D40.toInt(), 0xFF181719.toInt()),
                isRadial = true,
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setGradientBackground(
                        GradientColor(
                            name = "Thin Dark",
                            colors = intArrayOf(0xFF3A3D40.toInt(), 0xFF181719.toInt()),
                            type = GradientType.RADIAL
                        )
                    )
                    val textLayer = TextLayer(
                        text = "New Text",
                        textColor = Color.WHITE,
                        textSize = 36f,
                        typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "thin_dark",
                title = "Thin Dark",
                previewBgColors = intArrayOf(0xFF232526.toInt(), 0xFF0C0C0C.toInt()),
                isRadial = true,
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setGradientBackground(
                        GradientColor(
                            name = "Dark Charcoal",
                            colors = intArrayOf(0xFF232526.toInt(), 0xFF0C0C0C.toInt()),
                            type = GradientType.RADIAL
                        )
                    )
                    val textLayer = TextLayer(
                        text = "New Text",
                        textColor = 0xFFDDDDDD.toInt(),
                        textSize = 34f,
                        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "keep_calm",
                title = "Keep Calm",
                previewBgColors = intArrayOf(0xFFCC181E.toInt(), 0xFFCC181E.toInt()),
                isRadial = false,
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setColorBackground(0xFFCC181E.toInt())
                    val textLayer = TextLayer(
                        text = "KEEP CALM\nAND\nCARRY ON",
                        textColor = Color.WHITE,
                        textSize = 28f,
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "meme",
                title = "Meme",
                previewBgColors = intArrayOf(0xFF434343.toInt(), 0xFF000000.toInt()),
                isRadial = false,
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setGradientBackground(
                        GradientColor(
                            name = "Meme Dark",
                            colors = intArrayOf(0xFF434343.toInt(), 0xFF000000.toInt()),
                            type = GradientType.LINEAR
                        )
                    )
                    val topText = TextLayer(
                        text = "TOP TEXT",
                        textColor = Color.WHITE,
                        textSize = 32f,
                        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                    ).apply {
                        y = 120f
                        shadowEnabled = true
                        shadowRadius = 8f
                        shadowColor = Color.BLACK
                    }
                    val bottomText = TextLayer(
                        text = "BOTTOM TEXT",
                        textColor = Color.WHITE,
                        textSize = 32f,
                        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                    ).apply {
                        y = 360f
                        shadowEnabled = true
                        shadowRadius = 8f
                        shadowColor = Color.BLACK
                    }
                    canvas.addLayer(topText)
                    canvas.addLayer(bottomText)
                    canvas.selectedLayer = topText
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "three_d",
                title = "3D",
                previewBgColors = intArrayOf(0xFF1A2980.toInt(), 0xFF26D0CE.toInt()),
                isRadial = false,
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setGradientBackground(
                        GradientColor(
                            name = "Blue Lagoon",
                            colors = intArrayOf(0xFF1A2980.toInt(), 0xFF26D0CE.toInt()),
                            type = GradientType.LINEAR
                        )
                    )
                    val textLayer = TextLayer(
                        text = "3D Text",
                        textColor = Color.WHITE,
                        textSize = 42f,
                        typeface = Typeface.create("sans-serif", Typeface.BOLD),
                        extrudeEnabled = true,
                        extrudeDepth = 16
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            )
        )
    }
}
