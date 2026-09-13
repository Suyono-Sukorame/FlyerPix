package com.flyerpix.editor.template

import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.R

data class TemplateTheme(
    val headlineRatio: Float = 0.08f,
    val subtitleRatio: Float = 0.045f,
    val headlineTypeface: Typeface? = null,
    val headlineColor: Int = Color.WHITE,
    val accentColor: Int = Color.WHITE,
    val alignment: android.text.Layout.Alignment = android.text.Layout.Alignment.ALIGN_CENTER,
    val shadowEnabled: Boolean = false,
    val shadowRadius: Float = 8f,
    val shadowColor: Int = Color.BLACK
)

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
    val previewText: String = "New\nText",
    val previewTextSize: Float = 10f,
    val previewTypeface: Typeface? = null,
    val theme: TemplateTheme = TemplateTheme(),
    val applyToCanvas: (PixelCanvasView) -> Unit
) {
    companion object {
        /**
         * Warna gradasi radial khas default PixelLab.
         */
        val DEFAULT_RADIAL_COLORS = intArrayOf(0xFF739281.toInt(), 0xFF2E473B.toInt())

        private fun responsiveSize(canvas: PixelCanvasView, ratio: Float): Float {
            val width = canvas.canvasWidth.takeIf { it > 0 } ?: 1080
            return (width * ratio).coerceIn(32f, 160f)
        }

        private fun applyTextTheme(layer: TextLayer, canvas: PixelCanvasView, theme: TemplateTheme, ratio: Float = theme.headlineRatio) {
            layer.textColor = theme.headlineColor
            layer.textSize = responsiveSize(canvas, ratio)
            layer.typeface = theme.headlineTypeface
            layer.alignment = theme.alignment
            layer.shadowEnabled = theme.shadowEnabled
            layer.shadowRadius = theme.shadowRadius
            layer.shadowColor = theme.shadowColor
        }

        private fun drawableToBitmap(canvas: PixelCanvasView, drawableId: Int): Bitmap {
            val drawable: Drawable = canvas.context.getDrawable(drawableId)
                ?: error("Template drawable not found: $drawableId")
            val size = 1080
            return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
                val bitmapCanvas = Canvas(bitmap)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(bitmapCanvas)
            }
        }

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
                text = "Create\nSomething",
                textColor = Color.WHITE,
                textSize = responsiveSize(canvasView, 0.09f),
                typeface = Typeface.create("sans-serif", Typeface.BOLD),
                alignment = android.text.Layout.Alignment.ALIGN_CENTER
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
                previewText = "Create\nSomething",
                theme = TemplateTheme(
                    headlineRatio = 0.09f,
                    headlineTypeface = Typeface.create("sans-serif", Typeface.BOLD),
                    headlineColor = Color.WHITE
                ),
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setGradientBackground(
                        GradientColor(
                            colors = DEFAULT_RADIAL_COLORS.clone(),
                            type = GradientType.RADIAL,
                            name = "PixelLab Default"
                        )
                    )
                    val textLayer = TextLayer(text = "Create\nSomething")
                    applyTextTheme(textLayer, canvas, TemplatePreset.getBuiltinPresets().first { it.id == "default" }.theme)
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "thin",
                title = "Thin",
                previewBgColors = intArrayOf(0xFF3A3D40.toInt(), 0xFF181719.toInt()),
                isRadial = true,
                previewText = "Less\nIs More",
                theme = TemplateTheme(
                    headlineRatio = 0.085f,
                    headlineTypeface = Typeface.create("sans-serif-thin", Typeface.NORMAL),
                    headlineColor = 0xFFF4F4F4.toInt()
                ),
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
                        text = "Less\nIs More",
                        typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
                    )
                    applyTextTheme(textLayer, canvas, TemplateTheme(headlineRatio = 0.085f, headlineTypeface = Typeface.create("sans-serif-thin", Typeface.NORMAL), headlineColor = 0xFFF4F4F4.toInt()))
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
                previewText = "Quiet\nLuxury",
                theme = TemplateTheme(
                    headlineRatio = 0.08f,
                    headlineTypeface = Typeface.create("sans-serif-light", Typeface.NORMAL),
                    headlineColor = 0xFFE8E8E8.toInt()
                ),
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
                        text = "Quiet\nLuxury",
                        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                    )
                    applyTextTheme(textLayer, canvas, TemplateTheme(headlineRatio = 0.08f, headlineTypeface = Typeface.create("sans-serif-light", Typeface.NORMAL), headlineColor = 0xFFE8E8E8.toInt()))
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
                previewText = "KEEP\nCALM",
                previewTextSize = 9f,
                theme = TemplateTheme(
                    headlineRatio = 0.075f,
                    headlineTypeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
                    headlineColor = Color.WHITE,
                    shadowEnabled = true,
                    shadowRadius = 5f
                ),
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setColorBackground(0xFFCC181E.toInt())
                    val textLayer = TextLayer(
                        text = "KEEP CALM\nAND\nCARRY ON",
                        textColor = Color.WHITE,
                        textSize = 72f,
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
                previewText = "TOP\nTEXT",
                previewTextSize = 9f,
                theme = TemplateTheme(
                    headlineRatio = 0.065f,
                    headlineTypeface = Typeface.create("sans-serif-black", Typeface.BOLD),
                    headlineColor = Color.WHITE,
                    shadowEnabled = true,
                    shadowRadius = 8f
                ),
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
                        textSize = 64f,
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
                        textSize = 64f,
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
                previewText = "DEPTH",
                previewTextSize = 11f,
                previewTypeface = Typeface.create("sans-serif", Typeface.BOLD),
                theme = TemplateTheme(
                    headlineRatio = 0.09f,
                    headlineTypeface = Typeface.create("sans-serif", Typeface.BOLD),
                    headlineColor = Color.WHITE
                ),
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
                        textSize = 82f,
                        typeface = Typeface.create("sans-serif", Typeface.BOLD),
                        extrudeEnabled = true,
                        extrudeDepth = 16
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "emboss",
                title = "Emboss",
                previewBgColors = intArrayOf(0xFF5B4636.toInt(), 0xFF241A15.toInt()),
                isRadial = true,
                previewText = "MAKE\nAN IMPACT",
                previewTextSize = 8f,
                previewTextColor = 0xFFFFE0A3.toInt(),
                theme = TemplateTheme(
                    headlineRatio = 0.085f,
                    headlineTypeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
                    headlineColor = 0xFFFFE0A3.toInt()
                ),
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setGradientBackground(
                        GradientColor(
                            name = "Warm Emboss",
                            colors = intArrayOf(0xFF5B4636.toInt(), 0xFF241A15.toInt()),
                            type = GradientType.RADIAL
                        )
                    )
                    val textLayer = TextLayer(
                        text = "MAKE\nAN IMPACT",
                        textColor = 0xFFFFE0A3.toInt(),
                        textSize = responsiveSize(canvas, 0.085f),
                        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
                        embossEnabled = true,
                        embossLightAngle = 135f,
                        embossAmbient = 0.3f,
                        embossSpecular = 15f,
                        embossIntensity = 1.4f,
                        embossBevel = 5f
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "neon",
                title = "Neon",
                previewBgColors = intArrayOf(0xFF151A3D.toInt(), 0xFF050611.toInt()),
                isRadial = true,
                previewText = "LIGHT\nIT UP",
                previewTextSize = 8f,
                previewTextColor = 0xFFFF4FD8.toInt(),
                theme = TemplateTheme(
                    headlineRatio = 0.085f,
                    headlineTypeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
                    headlineColor = 0xFFFF4FD8.toInt()
                ),
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setGradientBackground(
                        GradientColor(
                            name = "Electric Neon",
                            colors = intArrayOf(0xFF151A3D.toInt(), 0xFF050611.toInt()),
                            type = GradientType.RADIAL
                        )
                    )
                    val textLayer = TextLayer(
                        text = "LIGHT\nIT UP",
                        textColor = 0xFFFFE6FA.toInt(),
                        textSize = responsiveSize(canvas, 0.085f),
                        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
                        neonEnabled = true,
                        neonColor = 0xFFFF4FD8.toInt(),
                        neonRadius = 18f,
                        neonIntensity = 1.6f,
                        neonCoreEnabled = true
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            ),
            TemplatePreset(
                id = "mountain_flyer",
                title = "Mountain",
                previewBgColors = intArrayOf(0xFFD9825B.toInt(), 0xFF203444.toInt()),
                isRadial = false,
                previewText = "FIND\nYOUR PEAK",
                previewTextSize = 8f,
                previewTextColor = Color.WHITE,
                theme = TemplateTheme(
                    headlineRatio = 0.085f,
                    headlineTypeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
                    headlineColor = Color.WHITE,
                    shadowEnabled = true,
                    shadowRadius = 8f
                ),
                applyToCanvas = { canvas ->
                    canvas.clearLayers()
                    canvas.setCanvasSize(1080, 1080)
                    canvas.setImageBackground(drawableToBitmap(canvas, R.drawable.bg_template_mountain_pixabay))
                    val textLayer = TextLayer(
                        text = "FIND\nYOUR PEAK",
                        textColor = Color.WHITE,
                        textSize = responsiveSize(canvas, 0.085f),
                        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
                        alignment = android.text.Layout.Alignment.ALIGN_CENTER,
                        shadowEnabled = true,
                        shadowRadius = 8f,
                        shadowColor = Color.BLACK,
                        shadowOpacity = 0.75f
                    )
                    canvas.addLayer(textLayer)
                    canvas.selectedLayer = textLayer
                    canvas.invalidate()
                }
            )
        )
    }
}
