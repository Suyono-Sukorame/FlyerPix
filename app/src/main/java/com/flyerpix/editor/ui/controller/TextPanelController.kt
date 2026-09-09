package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.lifecycleScope
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.ExtrudeViewType
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.ui.adapter.GradientPickerAdapter
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.flyerpix.editor.ui.compose.ColorControls


/**
 * Controller untuk mengelola semua panel properti Text Layer (Shadow, Inner Shadow,
 * Emboss, Gradient, Texture, 3D Extrusion, 3D Rotate, Curve, Perspective, Spacing, dll).
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi semua kontrol panel text
 * - Sinkronisasi state UI dengan TextLayer yang sedang dipilih
 * - Menerapkan perubahan real-time dari slider/switch ke TextLayer
 */
class TextPanelController(
    private val activity: Activity,
    private val binding: ActivityEditorBinding,
    private val pixelCanvasView: PixelCanvasView,
    private val showSnackbar: (String) -> Unit,
    private val onShowMenu: (Int) -> Unit,
    private val onEditTextRequested: (TextLayer) -> Unit,
    private val onFontRequested: (TextLayer) -> Unit,
    private val onCanvasChanged: () -> Unit,
    private val onEffectSettingsOpenChanged: (Boolean) -> Unit
) {

    private lateinit var gradientPickerAdapter: GradientPickerAdapter
    private var texturePickerLauncher: ActivityResultLauncher<String>? = null

    // ── Text Page State─────────────────────────────────────────────────────
    private val textToolItems = LinkedHashMap<String, ViewGroup>()
    private val textCategoryItems = LinkedHashMap<String, TextView>()
    private val textPanelViews = LinkedHashMap<String, View>()
    private val savedTextStyles = LinkedHashMap<String, SavedTextStyle>()
    private var activeTextToolTag: String = ""
    private var activeTextCategory: String = CATEGORY_BASIC
    private var maxPropertyPanelScrollH = 0
    var isPageOpen = false
    var pagePinnedByNav = false
    private val textToolLabels = HashMap<String, String>()
    
    // Flag untuk mencegah auto-switch ke menu Text saat initialization
    private var isInitializing = true

    private data class TextToolSpec(val tag: String, val label: String, val iconRes: Int)

    // ── Effect Settings Page State ──────────────────────────────────────────────
    private var settingsSnapshot: com.flyerpix.editor.canvas.model.TextLayer? = null
    private var effectSettingsOpen = false
    private var textToolTagBeforeEffect = ""
    private var toolIsTextPage = false
    private val complexEffectTags = setOf(TOOL_SHADOW, TOOL_INNER, TOOL_EMBOSS, TOOL_GRADIENT, TOOL_TEXTURE, TOOL_3D_TEXT, TOOL_3D_SHADOW, TOOL_3D_ROTATE, TOOL_PERSPECTIVE, TOOL_REFLECTION, TOOL_BLEND, TOOL_NEON, TOOL_STROKE, TOOL_LINE, TOOL_LETTER, TOOL_ALIGN, TOOL_BG, TOOL_CURVE, TOOL_STYLE, TOOL_MASK, TOOL_OPACITY, TOOL_ROTATE, TOOL_COLOR, TOOL_PADDING, TOOL_SIZE, TOOL_POSITION, TOOL_REL_POS, TOOL_STYLES)
    private var syncTextureUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncInnerShadowUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncEmbossUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncPerspectiveUIHook: ((com.flyerpix.editor.canvas.model.CanvasLayer) -> Unit)? = null
    private var syncReflectionUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncBlendUIHook: ((com.flyerpix.editor.canvas.model.CanvasLayer) -> Unit)? = null
    private var rebuildExtrudePaletteHook: ((com.flyerpix.editor.canvas.model.TextLayer?) -> Unit)? = null
    private var syncShadow3DUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncNeonUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncShadowUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncStrokeUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncSpacingUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncAlignUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncBackgroundUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncCurveUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncStyleUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncMaskUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncRotateUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncOpacityUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncColorUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var colorComposeHost: androidx.compose.ui.platform.ComposeView? = null
    private var strokeComposeHost: androidx.compose.ui.platform.ComposeView? = null
    private var paddingComposeHost: androidx.compose.ui.platform.ComposeView? = null
    private var syncPaddingUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncGradientUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncSizeUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncPositionUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncRelativePositionUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null
    private var syncStylesUIHook: ((com.flyerpix.editor.canvas.model.TextLayer) -> Unit)? = null

    companion object {
        private const val CATEGORY_BASIC = "basic"
        private const val CATEGORY_LAYOUT = "layout"
        private const val CATEGORY_TEXT = "text"
        private const val CATEGORY_APPEARANCE = "appearance"
        private const val CATEGORY_EFFECTS = "effects"
        private const val CATEGORY_ADVANCED = "advanced"
        private const val CATEGORY_LAYER = "layer"

        const val TOOL_STYLES       = "styles"
        const val TOOL_EDIT         = "edit"
        const val TOOL_DELETE       = "delete"
        const val TOOL_COPY         = "copy"
        const val TOOL_FRONT        = "front"
        const val TOOL_BACK         = "back"
        const val TOOL_POSITION     = "position"
        const val TOOL_REL_POS      = "relposition"
        const val TOOL_SIZE         = "size"
        const val TOOL_PADDING      = "padding"
        const val TOOL_COLOR        = "color"
        const val TOOL_GRADIENT     = "gradient"
        const val TOOL_TEXTURE      = "texture"
        const val TOOL_OPACITY      = "opacity"
        const val TOOL_ROTATE       = "rotate"
        const val TOOL_MASK         = "mask"
        const val TOOL_FONT         = "font"
        const val TOOL_STYLE        = "style"
        const val TOOL_CURVE        = "curve"
        const val TOOL_BG           = "background"
        const val TOOL_ALIGN        = "align"
        const val TOOL_LETTER       = "letter"
        const val TOOL_LINE         = "line"
        const val TOOL_STROKE       = "stroke"
        const val TOOL_SHADOW       = "shadow"
        const val TOOL_INNER        = "inner"
        const val TOOL_EMBOSS       = "emboss"
        const val TOOL_PERSPECTIVE  = "perspective"
        const val TOOL_3D_ROTATE    = "3drotate"
        const val TOOL_3D_TEXT      = "3dtext"
        const val TOOL_3D_SHADOW    = "3dshadow"
        const val TOOL_REFLECTION   = "reflection"
        const val TOOL_BLEND        = "blend"
        const val TOOL_NEON         = "neon"

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY      = 0xFF616161.toInt()
    }

    /**
     * Inisialisasi semua panel kontrol text editor.
     * Harus dipanggil setelah binding dan pixelCanvasView siap.
     */
    fun initialize() {
        buildTextCategoryStrip()
        buildTextToolStrip()
        registerTextPanels()
        configurePanelHeights()

        // Saat tinggi bar berubah (panel dibuka/ditutup/di-clamp), geser margin kanvas ke atas.
        binding.textEditorBar.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom != oldBottom) {
                clampPropertyPanelHeight()
                onCanvasChanged()
            }
        }

        // Controller terpusat. Dipasang PALING AWAL sehingga menjadi innermost
        // pada rantai onLayerSelectedListener → dijalankan PALING AKHIR, dan
        // visibilitas halaman yang ditetapkannya selalu menang.
        pixelCanvasView.onLayerSelectedListener = listener@{ layer ->
            val textLayer = layer as? TextLayer

            // Skip auto-switch saat initialization untuk mempertahankan menu default (Presets)
            if (!isInitializing && textLayer != null && !textLayer.isLocked) {
                onShowMenu(R.id.nav_text)
            } else if (!isInitializing) {
                if (isPageOpen) {
                    onShowMenu(R.id.nav_presets)
                } else {
                    // Halaman lain (Objek/Kanvas/Efek) dibiarkan terbuka;
                    // cukup nonaktifkan panel properti teks.
                    for (v in textPanelViews.values) v.visibility = View.GONE
                }
            }
        }

        initializePositionControls()
initializeRelativePositionControls()
initializeSizeControls()
initializeOpacityControls()
initializeRotateControls()
initializeStyleControls()
        initializeAlignControls()
initializeColorControls()
        initializeStrokeControls()
initializePaddingControls()
        initializeBackgroundControls()
        initializeReflectionControls()
initializeMaskControls()
        initializeStylesControls()

        initializeShadowControls()
        initializeInnerShadowControls()
        initializeEmbossControls()
        initializeGradientControls()
        initializeTextureControls()
        initializeExtrudeControls()
        initialize3DShadowControls()
        initializeRotate3DControls()
        initializeCurveControls()
        initializePerspectiveControls()
        initializeSpacingControls()
        initializeBlendModeControls()
        initializeNeonControls()
        initializeEffectSettingsHeader()
        
        // Selesai inisialisasi panel. Catatan: flag isInitializing sengaja TIDAK
        // dimatikan di sini — harus diakhiri via finishInitialization() SETELAH
        // template default tertunda diterapkan (lihat EditorActivity), agar
        // auto-switch ke menu Text tidak menimpa menu Presets saat pertama buka.
    }

    /**
     * Mengakhiri fase inisialisasi dan mengizinkan auto-switch ke menu Text.
     * Dipanggil setelah template default selesai diterapkan saat pertama buka.
     */
    fun finishInitialization() {
        isInitializing = false
    }

    /**
     * Set texture picker launcher dari Activity.
     * Diperlukan karena ActivityResultLauncher harus di-register di Activity.
     */
    fun setTexturePickerLauncher(launcher: ActivityResultLauncher<String>) {
        texturePickerLauncher = launcher
    }

    // ────────────────────────────────────────────────────────────────────────
    // DROP SHADOW CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Drop Shadow (gaya 3D Text).
     * Termasuk pemilih warna, tombol reset, dan label dinamis.
     */
    private fun initializeShadowControls() {
        val b = binding.effectSettingsInclude.shadowControlsInclude
        val panel = b.root
        val switch = b.switchShadowEnabled
        val group = b.shadowSliderGroup
        val sRadius = b.sliderShadowRadius
        val sOpacity = b.sliderShadowOpacity
        val sDx = b.sliderShadowDx
        val sDy = b.sliderShadowDy
        val tvRadius = b.tvShadowRadiusLabel
        val tvOpacity = b.tvShadowOpacityLabel
        val tvDx = b.tvShadowDxLabel
        val tvDy = b.tvShadowDyLabel
        val chipPreview = b.chipShadowColorPreview
        val tvColorValue = b.tvShadowColorValue
        val btnPick = b.btnPickShadowColor
        val btnReset = b.btnResetShadow

        fun setupColorPreview(layer: TextLayer?) {
            val color = layer?.shadowColor ?: Color.BLACK
            chipPreview.setCardBackgroundColor(color)
            tvColorValue.text = String.format(Locale.US, "#%08X", color)
        }

        fun openColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.shadowColor,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.SHADOW_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.shadowEnabled
            group.visibility = if (layer.shadowEnabled) View.VISIBLE else View.GONE
            sRadius.value = layer.shadowRadius.coerceIn(0f, 40f)
            sOpacity.value = layer.shadowOpacity.coerceIn(0f, 1f)
            sDx.value = layer.shadowDx.coerceIn(-30f, 30f)
            sDy.value = layer.shadowDy.coerceIn(-30f, 30f)
            tvRadius.text = String.format(Locale.US, "Blur: %.1f px", layer.shadowRadius)
            tvOpacity.text = String.format(Locale.US, "Opacity: %d%%", (layer.shadowOpacity * 100).toInt())
            tvDx.text = String.format(Locale.US, "Offset X: %.1f px", layer.shadowDx)
            tvDy.text = String.format(Locale.US, "Offset Y: %.1f px", layer.shadowDy)
            setupColorPreview(layer)
        }

        syncShadowUIHook = { layer -> syncUI(layer) }

        // Terima hasil pemilihan Shadow Color dari ColorPickerDialog
        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.SHADOW_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (!isGradient) {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        Color.BLACK
                    )
                    applyToTextLayer { layer ->
                        layer.shadowColor = color
                    }
                    setupColorPreview(pixelCanvasView.selectedLayer as? TextLayer)
                    pixelCanvasView.invalidate()
                }
            }

        chipPreview.setOnClickListener { openColorPicker() }
        btnPick.setOnClickListener { openColorPicker() }

        // Toggle enable/disable → tampilkan/sembunyikan slider group
        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.shadowEnabled = isChecked }
        }

        // Real-time slider listener + label dinamis
        sRadius.addOnChangeListener { _, value, _ ->
            tvRadius.text = String.format(Locale.US, "Blur: %.1f px", value)
            applyToTextLayer { it.shadowRadius = value }
        }
        sOpacity.addOnChangeListener { _, value, _ ->
            tvOpacity.text = String.format(Locale.US, "Opacity: %d%%", (value * 100).toInt())
            applyToTextLayer { it.shadowOpacity = value }
        }
        sDx.addOnChangeListener { _, value, _ ->
            tvDx.text = String.format(Locale.US, "Offset X: %.1f px", value)
            applyToTextLayer { it.shadowDx = value }
        }
        sDy.addOnChangeListener { _, value, _ ->
            tvDy.text = String.format(Locale.US, "Offset Y: %.1f px", value)
            applyToTextLayer { it.shadowDy = value }
        }

        // Reset ke nilai default
        btnReset.setOnClickListener {
            switch.isChecked = false
            group.visibility = View.GONE
            sRadius.value = 8f
            sOpacity.value = 0.6f
            sDx.value = 4f
            sDy.value = 4f
            val black = Color.BLACK
            applyToTextLayer { layer ->
                layer.shadowEnabled = false
                layer.shadowRadius = 8f
                layer.shadowOpacity = 0.6f
                layer.shadowDx = 4f
                layer.shadowDy = 4f
                layer.shadowColor = black
            }
            setupColorPreview(pixelCanvasView.selectedLayer as? TextLayer)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // INNER SHADOW CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Inner Shadow.
     * Sinkronisasi slider dilakukan setiap kali onLayerSelectedListener dipicu.
     * Semua slider beroperasi secara real-time.
     */
    private fun initializeInnerShadowControls() {
        val b = binding.effectSettingsInclude.innerShadowControlsInclude
        val panel = b.root
        val switch = b.switchInnerShadowEnabled
        val group = b.innerShadowSliderGroup
        val sRadius = b.sliderInnerShadowRadius
        val sOpacity = b.sliderInnerShadowOpacity
        val sDx = b.sliderInnerShadowDx
        val sDy = b.sliderInnerShadowDy
        val tvRadius = b.tvInnerShadowRadiusLabel
        val tvOpacity = b.tvInnerShadowOpacityLabel
        val tvDx = b.tvInnerShadowDxLabel
        val tvDy = b.tvInnerShadowDyLabel
        val chipPreview = b.chipInnerShadowColorPreview
        val tvValue = b.tvInnerShadowColorValue
        val btnPick = b.btnPickInnerShadowColor

        fun setupColorPreview(layer: TextLayer?) {
            val color = layer?.innerShadowColor ?: Color.BLACK
            chipPreview.setCardBackgroundColor(color)
            tvValue.text = String.format(Locale.US, "#%08X", color)
        }

        fun openColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.innerShadowColor,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.INNER_SHADOW_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        // Terima hasil pemilihan Inner Shadow Color dari ColorPickerDialog
        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.INNER_SHADOW_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (!isGradient) {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        Color.BLACK
                    )
                    applyToTextLayer { layer ->
                        layer.innerShadowColor = color
                    }
                    setupColorPreview(pixelCanvasView.selectedLayer as? TextLayer)
                    pixelCanvasView.invalidate()
                }
            }

        chipPreview.setOnClickListener { openColorPicker() }
        btnPick.setOnClickListener { openColorPicker() }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.innerShadowEnabled
            group.visibility = if (layer.innerShadowEnabled) View.VISIBLE else View.GONE
            sRadius.value = layer.innerShadowRadius.coerceIn(0f, 40f)
            tvRadius.text = "Blur: ${layer.innerShadowRadius}"
            sOpacity.value = layer.innerShadowOpacity.coerceIn(0f, 1f)
            tvOpacity.text = "Opacity: ${(layer.innerShadowOpacity * 100).toInt()}%"
            sDx.value = layer.innerShadowDx.coerceIn(-30f, 30f)
            tvDx.text = "Offset X: ${(layer.innerShadowDx * 10).toInt() / 10f}"
            sDy.value = layer.innerShadowDy.coerceIn(-30f, 30f)
            tvDy.text = "Offset Y: ${(layer.innerShadowDy * 10).toInt() / 10f}"
            setupColorPreview(layer)
        }

        // Perbarui state panel setiap kali layer teks baru dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.innerShadowEnabled = isChecked }
        }
        sRadius.addOnChangeListener { _, v, _ ->
            tvRadius.text = "Blur: ${v}"
            applyToTextLayer { it.innerShadowRadius = v }
        }
        sOpacity.addOnChangeListener { _, v, _ ->
            tvOpacity.text = "Opacity: ${(v * 100).toInt()}%"
            applyToTextLayer { it.innerShadowOpacity = v }
        }
        sDx.addOnChangeListener { _, v, _ ->
            tvDx.text = "Offset X: ${(v * 10).toInt() / 10f}"
            applyToTextLayer { it.innerShadowDx = v }
        }
        sDy.addOnChangeListener { _, v, _ ->
            tvDy.text = "Offset Y: ${(v * 10).toInt() / 10f}"
            applyToTextLayer { it.innerShadowDy = v }
        }

        syncInnerShadowUIHook = { layer -> syncUI(layer) }
        setupColorPreview(null)
    }

    // ────────────────────────────────────────────────────────────────────────
    // EMBOSS / BEVEL CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Emboss / Bevel.
     * Sinkronisasi slider dilakukan setiap kali onLayerSelectedListener dipicu.
     * Slider beroperasi secara real-time.
     */
    private fun initializeEmbossControls() {
        val b = binding.effectSettingsInclude.embossControlsInclude
        val panel = b.root
        val switch = b.switchEmbossEnabled
        val group = b.embossSliderGroup
        val sAngle = b.sliderEmbossAngle
        val sAmbient = b.sliderEmbossAmbient
        val sSpecular = b.sliderEmbossSpecular
        val sBevel = b.sliderEmbossBevel
        val sIntensity = b.sliderEmbossIntensity
        val tvAngle = b.tvEmbossAngleLabel
        val tvAmbient = b.tvEmbossAmbientLabel
        val tvSpecular = b.tvEmbossSpecularLabel
        val tvBevel = b.tvEmbossBevelLabel
        val tvIntensity = b.tvEmbossIntensityLabel

        fun setupLabels(layer: TextLayer) {
            tvAngle.text = "Light Angle (${layer.embossLightAngle.toInt()}°)"
            tvAmbient.text = "Ambient: ${(layer.embossAmbient * 100).toInt() / 100f}"
            tvSpecular.text = "Specular: ${(layer.embossSpecular * 10).toInt() / 10f}"
            tvBevel.text = "Bevel: ${(layer.embossBevel * 10).toInt() / 10f}"
            tvIntensity.text = "Intensity: ${(layer.embossIntensity * 100).toInt() / 100f}"
        }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.embossEnabled
            group.visibility = if (layer.embossEnabled) View.VISIBLE else View.GONE
            sAngle.value = layer.embossLightAngle.coerceIn(0f, 360f)
            sAmbient.value = layer.embossAmbient.coerceIn(0f, 1f)
            sSpecular.value = layer.embossSpecular.coerceIn(0.1f, 20f)
            sBevel.value = layer.embossBevel.coerceIn(0.5f, 12f)
            sIntensity.value = layer.embossIntensity.coerceIn(0f, 2.5f)
            setupLabels(layer)
        }
        syncEmbossUIHook = { layer -> syncUI(layer) }

        // Perbarui state panel setiap kali layer teks baru dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) syncUI(layer) else panel.visibility = View.GONE
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.embossEnabled = isChecked }
        }
        sAngle.addOnChangeListener { _, v, _ ->
            tvAngle.text = "Light Angle (${v.toInt()}°)"
            applyToTextLayer { it.embossLightAngle = v }
        }
        sAmbient.addOnChangeListener { _, v, _ ->
            tvAmbient.text = "Ambient: ${(v * 100).toInt() / 100f}"
            applyToTextLayer { it.embossAmbient = v }
        }
        sSpecular.addOnChangeListener { _, v, _ ->
            tvSpecular.text = "Specular: ${(v * 10).toInt() / 10f}"
            applyToTextLayer { it.embossSpecular = v }
        }
        sBevel.addOnChangeListener { _, v, _ ->
            tvBevel.text = "Bevel: ${(v * 10).toInt() / 10f}"
            applyToTextLayer { it.embossBevel = v }
        }
        sIntensity.addOnChangeListener { _, v, _ ->
            tvIntensity.text = "Intensity: ${(v * 100).toInt() / 100f}"
            applyToTextLayer { it.embossIntensity = v }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GRADIENT FILL CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Gradient Fill untuk TextLayer.
     * Mendukung mode Linear, Radial, dan Sweep, pemilihan preset visual real-time,
     * serta rotasi sudut (angle) untuk gradasi linier.
     */
    private fun initializeGradientControls() {
        val b = binding.effectSettingsInclude.gradientControlsInclude
        val panel = b.root
        val switch = b.switchGradientEnabled
        val group = b.gradientControlsGroup
        val rgType = b.rgGradientType
        val rbLinear = b.rbLinear
        val rbRadial = b.rbRadial
        val rbSweep = b.rbSweep
        val rvPresets = b.rvGradientPresets
        val angleContainer = b.gradientAngleContainer
        val tvAngleLabel = b.tvAngleLabel
        val sAngle = b.sliderGradientAngle
        val btnReset = b.btnResetGradient
        var syncing = false

        gradientPickerAdapter = GradientPickerAdapter { selectedPreset ->
            applyToTextLayer { layer ->
                val currentAngle = (layer.gradient?.angle ?: sAngle.value).coerceIn(0f, 360f)
                layer.gradient = selectedPreset.copy(angle = currentAngle)
                layer.gradientEnabled = true
                switch.isChecked = true
                group.visibility = View.VISIBLE

                // Sinkronkan radio button tipe gradasi dengan preset
                when (selectedPreset.type) {
                    GradientType.LINEAR -> rbLinear.isChecked = true
                    GradientType.RADIAL -> rbRadial.isChecked = true
                    GradientType.SWEEP  -> rbSweep.isChecked = true
                }
                angleContainer.visibility = if (selectedPreset.type == GradientType.LINEAR)
                    View.VISIBLE else View.GONE
            }
        }
        rvPresets.adapter = gradientPickerAdapter

        fun syncUI(layer: TextLayer?) {
            if (layer == null) {
                panel.visibility = View.GONE
                return
            }
            panel.visibility = View.VISIBLE
            syncing = true
            switch.isChecked = layer.gradientEnabled
            group.visibility = if (layer.gradientEnabled) View.VISIBLE else View.GONE

            val grad = layer.gradient
            if (grad != null) {
                when (grad.type) {
                    GradientType.LINEAR -> rbLinear.isChecked = true
                    GradientType.RADIAL -> rbRadial.isChecked = true
                    GradientType.SWEEP  -> rbSweep.isChecked = true
                }
                angleContainer.visibility = if (grad.type == GradientType.LINEAR)
                    View.VISIBLE else View.GONE
                sAngle.value = grad.angle.coerceIn(0f, 360f)
                tvAngleLabel.text = "Angle: ${grad.angle.toInt()}°"
                gradientPickerAdapter.setSelectedPreset(grad)
            } else {
                rbLinear.isChecked = true
                angleContainer.visibility = View.VISIBLE
                sAngle.value = 0f
tvAngleLabel.text = "Angle: 0°"
                gradientPickerAdapter.setSelectedPreset(null)
            }
            syncing = false
        }
        syncGradientUIHook = { layer -> syncUI(layer) }

        // Toggle Switch Enable / Disable
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (syncing) return@setOnCheckedChangeListener
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { layer ->
                layer.gradientEnabled = isChecked
                if (isChecked && layer.gradient == null) {
                    val defaultPreset = GradientColor.PRESETS[0].copy(angle = sAngle.value)
                    layer.gradient = defaultPreset
                    gradientPickerAdapter.setSelectedPreset(defaultPreset)
                }
            }
        }

        // RadioGroup Tipe Gradasi
        rgType.setOnCheckedChangeListener { _, checkedId ->
            if (syncing) return@setOnCheckedChangeListener
            val newType = when (checkedId) {
                R.id.rbRadial -> GradientType.RADIAL
                R.id.rbSweep  -> GradientType.SWEEP
                else          -> GradientType.LINEAR
            }
            angleContainer.visibility = if (newType == GradientType.LINEAR)
                View.VISIBLE else View.GONE

            applyToTextLayer { layer ->
                val g = layer.gradient
                if (g != null) {
                    g.type = newType
                } else {
                    layer.gradient = GradientColor.PRESETS[0].copy(type = newType, angle = sAngle.value)
                }
            }
        }

        // Slider Sudut Putar Gradasi Linier
        sAngle.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvAngleLabel.text = "Angle: ${value.toInt()}°"
            applyToTextLayer { layer ->
                layer.gradient?.angle = value
            }
        }

        // Reset gradasi ke kondisi awal (off, preset default)
        btnReset.setOnClickListener {
            syncing = true
            switch.isChecked = false
            group.visibility = View.GONE
            rbLinear.isChecked = true
            angleContainer.visibility = View.VISIBLE
            sAngle.value = 0f
            tvAngleLabel.text = "Angle: 0°"
            gradientPickerAdapter.setSelectedPreset(null)
            syncing = false
            applyToTextLayer { layer ->
                layer.gradientEnabled = false
                layer.gradient = GradientColor.PRESETS[0].copy(angle = 0f)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // TEXTURE MASKING CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Texture Masking pada TextLayer.
     * Mengizinkan pemilihan foto dari galeri HP, pengaturan rasio skala tekstur (10% - 300%),
     * dan rotasi sudut tekstur (0° - 360°).
     */
    private fun initializeTextureControls() {
        val b = binding.effectSettingsInclude.textureControlsInclude
        val panel = b.root
        val switch = b.switchTextureEnabled
        val group = b.textureControlsGroup
        val imgThumb = b.imgTextureThumbnail
        val btnSelect = b.btnSelectTexture
        val btnDelete = b.btnDeleteTexture
        val sScale = b.sliderTextureScale
        val tvScale = b.tvTextureScaleLabel
        val sRotation = b.sliderTextureRotation
        val tvRotation = b.tvTextureRotationLabel
        val btnReset = b.btnResetTexture
        var syncing = false

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            syncing = true
            switch.isChecked = layer.textureEnabled
            group.visibility = if (layer.textureEnabled) View.VISIBLE else View.GONE

            if (layer.textureBitmap != null && !layer.textureBitmap!!.isRecycled) {
                imgThumb.setImageBitmap(layer.textureBitmap)
                btnSelect.text = "Change Photo"
                btnDelete.visibility = View.VISIBLE
            } else {
                imgThumb.setImageResource(R.drawable.ic_sharp_photo_24px)
                btnSelect.text = "Choose from Gallery"
                btnDelete.visibility = View.GONE
            }

            sScale.value = layer.textureScale.coerceIn(0.1f, 3.0f)
            tvScale.text = "Scale: ${(layer.textureScale * 100).toInt()}%"
            sRotation.value = layer.textureRotation.coerceIn(0f, 360f)
            tvRotation.text = "Rotation: ${layer.textureRotation.toInt()}°"
            syncing = false
        }

        // Toggle Switch Enable / Disable
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (syncing) return@setOnCheckedChangeListener
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { layer ->
                layer.textureEnabled = isChecked
                if (isChecked && layer.textureBitmap == null) {
                    texturePickerLauncher?.launch("image/*")
                }
            }
            val mask = binding.effectSettingsInclude.maskControlsInclude
            mask.switchMaskEnabled.isChecked = isChecked
            mask.maskControlsGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Tombol Pilih Foto dari Galeri
        btnSelect.setOnClickListener {
            texturePickerLauncher?.launch("image/*")
        }

        // Tombol Hapus Tekstur
        btnDelete.setOnClickListener {
            applyToTextLayer { layer ->
                layer.textureBitmap = null
                layer.textureEnabled = false
            }
            val curLayer = pixelCanvasView.selectedLayer as? TextLayer
            if (curLayer != null) syncUI(curLayer)
            val mask = binding.effectSettingsInclude.maskControlsInclude
            mask.switchMaskEnabled.isChecked = false
            mask.maskControlsGroup.visibility = View.GONE
        }

        // Slider Skala Tekstur
        sScale.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvScale.text = "Scale: ${(value * 100).toInt()}%"
            applyToTextLayer { layer ->
                layer.textureScale = value
            }
        }

        // Slider Rotasi Tekstur
        sRotation.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvRotation.text = "Rotation: ${value.toInt()}°"
            applyToTextLayer { layer ->
                layer.textureRotation = value
            }
        }

        // Reset Tekstur
        btnReset.setOnClickListener {
            syncing = true
            switch.isChecked = false
            syncing = false
            group.visibility = View.GONE
            imgThumb.setImageResource(R.drawable.ic_sharp_photo_24px)
            btnSelect.text = "Choose from Gallery"
            btnDelete.visibility = View.GONE
            applyToTextLayer { layer ->
                layer.textureBitmap = null
                layer.textureEnabled = false
                layer.textureScale = 1.0f
                layer.textureRotation = 0f
            }
            val curLayer = pixelCanvasView.selectedLayer as? TextLayer
            if (curLayer != null) syncUI(curLayer)
            val mask = binding.effectSettingsInclude.maskControlsInclude
            mask.switchMaskEnabled.isChecked = false
            mask.maskControlsGroup.visibility = View.GONE
            showSnackbar("Texture reset")
        }

        syncTextureUIHook = { layer -> syncUI(layer) }
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3D TEXT EXTRUSION CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol 3D Text Extrusion pada TextLayer.
     * Mengatur ketebalan depth (1..50), tipe proyeksi (Oblique / Isometric),
     * arah sudut oblique (0°..360°), dan pilihan warna depth sisi 3D.
     */
    private fun initializeExtrudeControls() {
        val b = binding.effectSettingsInclude.extrudeControlsInclude
        val panel = b.root
        val switch = b.switchExtrudeEnabled
        val group = b.extrudeControlsGroup
        val rgType = b.rgExtrudeViewType
        val rbOblique = b.rbOblique
        val rbIso = b.rbIsometric
        val sDepth = b.sliderExtrudeDepth
        val tvDepth = b.tvExtrudeDepthLabel
        val angleContainer = b.extrudeAngleContainer
        val sAngle = b.sliderExtrudeAngle
        val tvAngle = b.tvExtrudeAngleLabel
        val chipPreview = b.chipDepthColorPreview
        val tvValue = b.tvDepthColorValue
        val btnPick = b.btnPickDepthColor

        fun setupDepthColorPreview(layer: TextLayer?) {
            val grad = layer?.extrudeGradient
            if (grad != null) {
                val first = grad.colors.firstOrNull() ?: 0xFF333333.toInt()
                chipPreview.setCardBackgroundColor(first)
                tvValue.text = "Grad: ${grad.name.ifBlank { grad.type.name }}"
            } else {
                val color = layer?.extrudeColor ?: 0xFF333333.toInt()
                chipPreview.setCardBackgroundColor(color)
                tvValue.text = String.format(Locale.US, "#%08X", color)
            }
        }

        fun openDepthColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.extrudeColor,
                    initialGradient = layer.extrudeGradient,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.DEPTH_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        // Terima hasil pemilihan Depth Color / Gradient dari ColorPickerDialog
        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.DEPTH_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (isGradient) {
                    @Suppress("DEPRECATION")
                    val gradient = bundle.getSerializable(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_GRADIENT
                    ) as? com.flyerpix.editor.canvas.model.GradientColor
                    if (gradient != null) {
                        applyToTextLayer { layer ->
                            layer.extrudeGradient = gradient
                        }
                    }
                } else {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        0xFF333333.toInt()
                    )
                    applyToTextLayer { layer ->
                        layer.extrudeColor = color
                        layer.extrudeGradient = null
                    }
                }
                setupDepthColorPreview(pixelCanvasView.selectedLayer as? TextLayer)
                pixelCanvasView.invalidate()
            }

        chipPreview.setOnClickListener { openDepthColorPicker() }
        btnPick.setOnClickListener { openDepthColorPicker() }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.extrudeEnabled
            group.visibility = if (layer.extrudeEnabled) View.VISIBLE else View.GONE
            rgType.visibility = if (layer.extrudeEnabled) View.VISIBLE else View.GONE

            if (layer.extrudeViewType == ExtrudeViewType.ISOMETRIC) {
                rbIso.isChecked = true
                angleContainer.visibility = View.GONE
            } else {
                rbOblique.isChecked = true
                angleContainer.visibility = View.VISIBLE
            }

            sDepth.value = layer.extrudeDepth.coerceIn(1, 50).toFloat()
            tvDepth.text = "Depth: ${layer.extrudeDepth}"

            sAngle.value = layer.extrudeAngle.coerceIn(0f, 360f)
            tvAngle.text = "Angle (${layer.extrudeAngle.toInt()}°)"

            setupDepthColorPreview(layer)
        }

        // Sinkronisasi saat layer teks aktif dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        // Toggle Switch
        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            rgType.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { layer ->
                layer.extrudeEnabled = isChecked
            }
        }

        // RadioGroup Proyeksi
        rgType.setOnCheckedChangeListener { _, checkedId ->
            val isIso = checkedId == R.id.rbIsometric
            angleContainer.visibility = if (isIso) View.GONE else View.VISIBLE
            applyToTextLayer { layer ->
                layer.extrudeViewType = if (isIso)
                    ExtrudeViewType.ISOMETRIC
                else
                    ExtrudeViewType.OBLIQUE
            }
        }

        // Slider Depth
        sDepth.addOnChangeListener { _, value, _ ->
            tvDepth.text = "Depth: ${value.toInt()}"
            applyToTextLayer { layer ->
                layer.extrudeDepth = value.toInt()
            }
        }

        // Slider Angle
        sAngle.addOnChangeListener { _, value, _ ->
            tvAngle.text = "Angle (${value.toInt()}°)"
            applyToTextLayer { layer ->
                layer.extrudeAngle = value
            }
        }

        rebuildExtrudePaletteHook = { layer -> if (layer != null) syncUI(layer) }

        setupDepthColorPreview(null)
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3D SHADOW CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol 3D Shadow pada TextLayer.
     * Mengikuti gaya UI/UX panel 3D Text Extrusion: mengatur ketebalan depth
     * (1..50), tipe proyeksi (Oblique / Isometric), arah sudut oblique (0°..360°),
     * kelembutan blur (0..40), kegelapan opacity, dan warna bayangan 3D.
     */
    private fun initialize3DShadowControls() {
        val b = binding.effectSettingsInclude.shadow3DControlsInclude
        val panel = b.root
        val switch = b.switchShadow3DEnabled
        val group = b.shadow3DControlsGroup
        val rgType = b.rgShadow3DViewType
        val rbOblique = b.rbShadow3DOblique
        val rbIso = b.rbShadow3DIsometric
        val sDepth = b.sliderShadow3DDepth
        val tvDepth = b.tvShadow3DDepthLabel
        val angleContainer = b.shadow3DAngleContainer
        val sAngle = b.sliderShadow3DAngle
        val tvAngle = b.tvShadow3DAngleLabel
        val sBlur = b.sliderShadow3DBlur
        val tvBlur = b.tvShadow3DBlurLabel
        val sOpacity = b.sliderShadow3DOpacity
        val tvOpacity = b.tvShadow3DOpacityLabel
        val chipPreview = b.chipShadow3DColorPreview
        val tvValue = b.tvShadow3DColorValue
        val btnPick = b.btnPickShadow3DColor

        fun setupColorPreview(layer: TextLayer?) {
            val color = layer?.shadow3DColor ?: 0xB3000000.toInt()
            chipPreview.setCardBackgroundColor(color)
            tvValue.text = String.format(Locale.US, "#%08X", color)
        }

        fun openColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.shadow3DColor,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.SHADOW3D_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        // Terima hasil pemilihan Shadow Color dari ColorPickerDialog
        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.SHADOW3D_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (!isGradient) {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        0xB3000000.toInt()
                    )
                    applyToTextLayer { layer ->
                        layer.shadow3DColor = color
                    }
                    setupColorPreview(pixelCanvasView.selectedLayer as? TextLayer)
                    pixelCanvasView.invalidate()
                }
            }

        chipPreview.setOnClickListener { openColorPicker() }
        btnPick.setOnClickListener { openColorPicker() }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.shadow3DEnabled
            group.visibility = if (layer.shadow3DEnabled) View.VISIBLE else View.GONE
            rgType.visibility = if (layer.shadow3DEnabled) View.VISIBLE else View.GONE

            if (layer.shadow3DViewType == ExtrudeViewType.ISOMETRIC) {
                rbIso.isChecked = true
                angleContainer.visibility = View.GONE
            } else {
                rbOblique.isChecked = true
                angleContainer.visibility = View.VISIBLE
            }

            sDepth.value = layer.shadow3DDepth.coerceIn(1, 50).toFloat()
            tvDepth.text = "Depth: ${layer.shadow3DDepth}"

            sAngle.value = layer.shadow3DAngle.coerceIn(0f, 360f)
            tvAngle.text = "Angle (${layer.shadow3DAngle.toInt()}°)"

            sBlur.value = layer.shadow3DBlur.coerceIn(0f, 40f)
            tvBlur.text = "Blur: ${Math.round(layer.shadow3DBlur * 10f) / 10f}"

            sOpacity.value = layer.shadow3DOpacity.coerceIn(0f, 1f)
            tvOpacity.text = "Opacity: ${(layer.shadow3DOpacity * 100).toInt()}%"

            setupColorPreview(layer)
        }

        // Sinkronisasi saat layer teks aktif dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        // Toggle Switch
        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            rgType.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { layer ->
                layer.shadow3DEnabled = isChecked
            }
        }

        // RadioGroup Proyeksi
        rgType.setOnCheckedChangeListener { _, checkedId ->
            val isIso = checkedId == R.id.rbShadow3DIsometric
            angleContainer.visibility = if (isIso) View.GONE else View.VISIBLE
            applyToTextLayer { layer ->
                layer.shadow3DViewType = if (isIso)
                    ExtrudeViewType.ISOMETRIC
                else
                    ExtrudeViewType.OBLIQUE
            }
        }

        // Slider Depth
        sDepth.addOnChangeListener { _, value, _ ->
            tvDepth.text = "Depth: ${value.toInt()}"
            applyToTextLayer { layer ->
                layer.shadow3DDepth = value.toInt()
            }
        }

        // Slider Angle
        sAngle.addOnChangeListener { _, value, _ ->
            tvAngle.text = "Angle (${value.toInt()}°)"
            applyToTextLayer { layer ->
                layer.shadow3DAngle = value
            }
        }

        // Slider Blur
        sBlur.addOnChangeListener { _, value, _ ->
            tvBlur.text = "Blur: ${Math.round(value * 10f) / 10f}"
            applyToTextLayer { layer ->
                layer.shadow3DBlur = value
            }
        }

        // Slider Opacity
        sOpacity.addOnChangeListener { _, value, _ ->
            tvOpacity.text = "Opacity: ${(value * 100).toInt()}%"
            applyToTextLayer { layer ->
                layer.shadow3DOpacity = value
            }
        }

        syncShadow3DUIHook = { layer -> syncUI(layer) }

        setupColorPreview(null)
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3D ROTATE CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol 3D Rotate (Rotasi Sumbu X dan Y).
     * Menggunakan android.graphics.Camera untuk transformasi perspektif 3D dinamis.
     */
    private fun initializeRotate3DControls() {
        val b = binding.effectSettingsInclude.rotate3DControlsInclude
        val panel = b.root
        val tvX = b.tvRotateXLabel
        val tvY = b.tvRotateYLabel
        val tvZ = b.tvRotateZLabel
        val sX = b.sliderRotateX
        val sY = b.sliderRotateY
        val sZ = b.sliderRotateZ
        val btnReset = b.btnReset3DRotate

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            sX.value = layer.rotate3DX.coerceIn(-180f, 180f)
            tvX.text = "X Axis (${layer.rotate3DX.toInt()}°)"
            sY.value = layer.rotate3DY.coerceIn(-180f, 180f)
            tvY.text = "Y Axis (${layer.rotate3DY.toInt()}°)"
            sZ.value = layer.rotate3DZ.coerceIn(-180f, 180f)
            tvZ.text = "Z Axis (${layer.rotate3DZ.toInt()}°)"
        }

        // Sinkronisasi saat layer teks aktif dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        // Slider Rotasi X
        sX.addOnChangeListener { _, value, _ ->
            tvX.text = "X Axis (${value.toInt()}°)"
            applyToTextLayer { layer -> layer.rotate3DX = value }
        }

        // Slider Rotasi Y
        sY.addOnChangeListener { _, value, _ ->
            tvY.text = "Y Axis (${value.toInt()}°)"
            applyToTextLayer { layer -> layer.rotate3DY = value }
        }

        // Slider Rotasi Z
        sZ.addOnChangeListener { _, value, _ ->
            tvZ.text = "Z Axis (${value.toInt()}°)"
            applyToTextLayer { layer -> layer.rotate3DZ = value }
        }

        // Tombol Reset: kembalikan semua sumbu ke 0°
        btnReset.setOnClickListener {
            applyToTextLayer { layer ->
                layer.rotate3DX = 0f
                layer.rotate3DY = 0f
                layer.rotate3DZ = 0f
            }
            val sel = pixelCanvasView.selectedLayer
            if (sel is TextLayer) syncUI(sel)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // CURVE CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Curved / Arc Text.
     *
     * Slider -100 s/d +100:
     *  - 0   = teks lurus (flat)
     *  - +N  = melengkung ke atas
     *  - -N  = melengkung ke bawah
     *
     * Tombol preset (-100, -50, 0, +50, +100) mempercepat pemilihan kurva.
     */
    private fun initializeCurveControls() {
        val b = binding.effectSettingsInclude.curveControlsInclude
        val panel = b.root
        val tvLabel = b.tvCurveLabel
        val slider = b.sliderCurve
        val btnReset = b.btnResetCurve
        val presets = listOf(
            b.btnCurveDown100 to -100,
            b.btnCurveDown50 to -50,
            b.btnCurveFlat to 0,
            b.btnCurveUp50 to 50,
            b.btnCurveUp100 to 100
        )

        fun curveLabel(v: Int): String = when {
            v == 0   -> "Curve: 0% (Flat)"
            v > 0    -> "Curve: +$v% ▲"
            else     -> "Curve: $v% ▼"
        }

        fun setActive(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else android.graphics.Color.TRANSPARENT)
            btn.setTextColor(if (active) android.graphics.Color.WHITE else COLOR_GRAY)
        }

        fun applyPreset(value: Int) {
            slider.value = value.coerceIn(-100, 100).toFloat()
            tvLabel.text = curveLabel(value)
            for ((btn, pv) in presets) setActive(btn, pv == value)
            applyToTextLayer { layer -> layer.curvePercent = value }
        }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            val v = layer.curvePercent.coerceIn(-100, 100)
            slider.value = v.toFloat()
            tvLabel.text = curveLabel(v)
            for ((btn, pv) in presets) setActive(btn, pv == v)
        }

        // Slider perubahan nilai
        slider.addOnChangeListener { _, value, _ ->
            val v = value.toInt()
            tvLabel.text = curveLabel(v)
            for ((btn, pv) in presets) setActive(btn, pv == v)
            applyToTextLayer { layer -> layer.curvePercent = v }
        }

        // Tombol preset
        for ((btn, value) in presets) btn.setOnClickListener { applyPreset(value) }

        // Reset ke flat
        btnReset.setOnClickListener { applyPreset(0) }

        syncCurveUIHook = { layer -> syncUI(layer) }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PERSPECTIVE CONTROLS (Placeholder)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Transformasi Perspektif Warping.
     * TODO: Implement perspective controls jika ada di UI
     */
    private fun initializePerspectiveControls() {
        val b = binding.effectSettingsInclude.perspectiveControlsInclude
        val panel = b.root
        val switch = b.switchPerspectiveEnabled
        val group = b.perspectiveControlsGroup
        val btnReset = b.btnResetPerspective
        val btnFlat = b.btnPresetFlat
        val btnSlightLeft = b.btnPresetSlightLeft
        val btnLeft = b.btnPresetLeftWall
        val btnRight = b.btnPresetRightWall
        val btnGentleTilt = b.btnPresetGentleTilt
        val btnBillboard = b.btnPresetBillboard
        val btnFloor = b.btnPresetFloor

        val presets = listOf(
            btnFlat        to com.flyerpix.editor.canvas.model.PerspectivePreset.FLAT,
            btnSlightLeft  to com.flyerpix.editor.canvas.model.PerspectivePreset.SLIGHT_LEFT,
            btnLeft        to com.flyerpix.editor.canvas.model.PerspectivePreset.LEFT_WALL,
            btnRight       to com.flyerpix.editor.canvas.model.PerspectivePreset.RIGHT_WALL,
            btnGentleTilt  to com.flyerpix.editor.canvas.model.PerspectivePreset.GENTLE_TILT,
            btnBillboard   to com.flyerpix.editor.canvas.model.PerspectivePreset.TOP_BILLBOARD,
            btnFloor       to com.flyerpix.editor.canvas.model.PerspectivePreset.FLOOR_TILT
        )

        // Sorot tombol preset yang nilainya saat ini sama dengan korner layer.
        fun highlightActivePreset(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
            val corners = layer.perspectiveCorners
            for ((btn, preset) in presets) {
                val target = layer.perspectiveCornersFor(preset)
                val active = corners.size == target.size &&
                    corners.indices.all { kotlin.math.abs(corners[it] - target[it]) < 0.01f }
                btn.isSelected = active
                btn.setBackgroundColor(if (active) COLOR_ACTIVE else Color.TRANSPARENT)
                btn.setTextColor(if (active) Color.WHITE else COLOR_GRAY)
            }
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.perspectiveEnabled
            group.visibility = if (layer.perspectiveEnabled) View.VISIBLE else View.GONE
            highlightActivePreset(layer)
        }
        syncPerspectiveUIHook = { layer -> syncUI(layer) }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer != null) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            pixelCanvasView.selectedLayer?.let { layer ->
                if (!layer.isLocked) {
                    layer.perspectiveEnabled = isChecked
                    pixelCanvasView.invalidate()
                }
            }
        }

        fun applyPreset(preset: com.flyerpix.editor.canvas.model.PerspectivePreset) {
            pixelCanvasView.selectedLayer?.let { layer ->
                if (!layer.isLocked) {
                    layer.applyPerspectivePreset(preset)
                    highlightActivePreset(layer)
                    pixelCanvasView.invalidate()
                }
            }
        }

        for ((btn, preset) in presets) {
            btn.setOnClickListener { applyPreset(preset) }
        }

        btnReset.setOnClickListener {
            pixelCanvasView.selectedLayer?.let { layer ->
                if (!layer.isLocked) {
                    layer.resetPerspective()
                    highlightActivePreset(layer)
                    pixelCanvasView.invalidate()
                }
            }
        }
    }

    /**
     * Menginisialisasi panel kontrol Text Spacing (Prompt 23).
     * Memungkinkan penyesuaian tipografi lanjut:
     *  1. Spasi Antar Huruf (Letter Spacing / Kerning: -0.2 s/d 1.0 EM)
     *  2. Spasi Antar Baris (Line Spacing / Leading: -20 s/d 80 px)
     * Keduanya terhubung secara reaktif ke kanvas secara real-time.
     */

    // ────────────────────────────────────────────────────────────────────────
    // SPACING CONTROLS (Placeholder)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Letter Spacing & Line Height.
     * TODO: Implement spacing controls jika ada di UI
     */
    private fun initializeSpacingControls() {
        val b = binding.effectSettingsInclude.spacingControlsInclude
        val panel = b.root
        val tvLetterLabel = b.tvLetterSpacingLabel
        val tvLineLabel = b.tvLineSpacingLabel
        val sLetter = b.sliderLetterSpacing
        val sLine = b.sliderLineSpacing
        val btnReset = b.btnResetSpacing
        val btnLetterTight = b.btnLetterTight
        val btnLetterNormal = b.btnLetterNormal
        val btnLetterMedium = b.btnLetterMedium
        val btnLetterWide = b.btnLetterWide
        val btnLineTight = b.btnLineTight
        val btnLineNormal = b.btnLineNormal
        val btnLineMedium = b.btnLineMedium
        val btnLineWide = b.btnLineWide

        fun formatLetter(v: Float) = String.format(Locale.US, "Letter Spacing: %.2f", v)
        fun formatLine(v: Float) = String.format(Locale.US, "Line Spacing: %.0f px", v)

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            val letterVal = layer.letterSpacing.coerceIn(-0.2f, 1.0f)
            val lineVal = layer.lineSpacing.coerceIn(-20f, 80f)
            sLetter.value = (Math.round(letterVal * 50f) / 50f).coerceIn(-0.2f, 1.0f)
            sLine.value = (Math.round(lineVal / 2f) * 2f).coerceIn(-20f, 80f)
            tvLetterLabel.text = formatLetter(letterVal)
            tvLineLabel.text = formatLine(lineVal)
        }

        syncSpacingUIHook = { layer -> syncUI(layer) }

        sLetter.addOnChangeListener { _, value, _ ->
            tvLetterLabel.text = formatLetter(value)
            applyToTextLayer { layer -> layer.letterSpacing = value }
        }

        sLine.addOnChangeListener { _, value, _ ->
            tvLineLabel.text = formatLine(value)
            applyToTextLayer { layer -> layer.lineSpacing = value }
        }

        // Preset Letter Spacing
        btnLetterTight.setOnClickListener {
            sLetter.value = -0.10f
            tvLetterLabel.text = formatLetter(-0.10f)
            applyToTextLayer { it.letterSpacing = -0.10f }
        }
        btnLetterNormal.setOnClickListener {
            sLetter.value = 0.0f
            tvLetterLabel.text = formatLetter(0.0f)
            applyToTextLayer { it.letterSpacing = 0.0f }
        }
        btnLetterMedium.setOnClickListener {
            sLetter.value = 0.15f
            tvLetterLabel.text = formatLetter(0.15f)
            applyToTextLayer { it.letterSpacing = 0.15f }
        }
        btnLetterWide.setOnClickListener {
            sLetter.value = 0.40f
            tvLetterLabel.text = formatLetter(0.40f)
            applyToTextLayer { it.letterSpacing = 0.40f }
        }

        // Preset Line Spacing
        btnLineTight.setOnClickListener {
            sLine.value = -10f
            tvLineLabel.text = formatLine(-10f)
            applyToTextLayer { it.lineSpacing = -10f }
        }
        btnLineNormal.setOnClickListener {
            sLine.value = 0f
            tvLineLabel.text = formatLine(0f)
            applyToTextLayer { it.lineSpacing = 0f }
        }
        btnLineMedium.setOnClickListener {
            sLine.value = 20f
            tvLineLabel.text = formatLine(20f)
            applyToTextLayer { it.lineSpacing = 20f }
        }
        btnLineWide.setOnClickListener {
            sLine.value = 40f
            tvLineLabel.text = formatLine(40f)
            applyToTextLayer { it.lineSpacing = 40f }
        }

        // Reset spacing
        btnReset.setOnClickListener {
            sLetter.value = 0.0f
            sLine.value = 0f
            tvLetterLabel.text = formatLetter(0.0f)
            tvLineLabel.text = formatLine(0f)
            applyToTextLayer {
                it.letterSpacing = 0f
                it.lineSpacing = 0f
            }
        }
    }

    /**
     * Menginisialisasi panel kontrol Blending Mode (Prompt 24).
     * Mendukung mode percampuran populer: Normal, Multiply, Screen, Overlay, Darken, Lighten, Add.
     * Menggunakan PorterDuffXfermode pada rendering canvas untuk komposisi warna non-destruktif.
     */

    private fun initializeBlendModeControls() {
        val b = binding.effectSettingsInclude.blendControlsInclude
        val panel = b.root
        val tvDesc = b.tvBlendDescription
        val btnReset = b.btnResetBlendMode
        val btnNormal = b.btnBlendNormal
        val btnMultiply = b.btnBlendMultiply
        val btnScreen = b.btnBlendScreen
        val btnOverlay = b.btnBlendOverlay
        val btnDarken = b.btnBlendDarken
        val btnLighten = b.btnBlendLighten
        val btnAdd = b.btnBlendAdd
        val btnHardLight = b.btnBlendHardLight
        val btnSoftLight = b.btnBlendSoftLight
        val btnColorBurn = b.btnBlendColorBurn
        val btnColorDodge = b.btnBlendColorDodge
        val btnDifference = b.btnBlendDifference
        val btnExclusion = b.btnBlendExclusion

        // Mode lanjutan (BlendMode) baru tersedia sejak API 29.
        val extraButtons = if (android.os.Build.VERSION.SDK_INT >= 29) {
            b.blendAdvancedRow.visibility = View.VISIBLE
            listOf(
                btnHardLight  to com.flyerpix.editor.canvas.model.ExtendedBlendMode.HARD_LIGHT,
                btnSoftLight  to com.flyerpix.editor.canvas.model.ExtendedBlendMode.SOFT_LIGHT,
                btnColorBurn  to com.flyerpix.editor.canvas.model.ExtendedBlendMode.COLOR_BURN,
                btnColorDodge to com.flyerpix.editor.canvas.model.ExtendedBlendMode.COLOR_DODGE,
                btnDifference to com.flyerpix.editor.canvas.model.ExtendedBlendMode.DIFFERENCE,
                btnExclusion  to com.flyerpix.editor.canvas.model.ExtendedBlendMode.EXCLUSION
            )
        } else {
            b.blendAdvancedRow.visibility = View.GONE
            emptyList()
        }

        val buttons = listOf(
            btnNormal to android.graphics.PorterDuff.Mode.SRC_OVER,
            btnMultiply to android.graphics.PorterDuff.Mode.MULTIPLY,
            btnScreen to android.graphics.PorterDuff.Mode.SCREEN,
            btnOverlay to android.graphics.PorterDuff.Mode.OVERLAY,
            btnDarken to android.graphics.PorterDuff.Mode.DARKEN,
            btnLighten to android.graphics.PorterDuff.Mode.LIGHTEN,
            btnAdd to android.graphics.PorterDuff.Mode.ADD
        )

        fun descriptionFor(mode: android.graphics.PorterDuff.Mode): String = when (mode) {
            android.graphics.PorterDuff.Mode.SRC_OVER -> "Normal: Shows the layer's standard color over the background."
            android.graphics.PorterDuff.Mode.MULTIPLY -> "Multiply: Multiplies colors (darker text that blends in)."
            android.graphics.PorterDuff.Mode.SCREEN   -> "Screen: Inverts and multiplies (makes text glow brightly)."
            android.graphics.PorterDuff.Mode.OVERLAY  -> "Overlay: Combines Multiply and Screen based on the background."
            android.graphics.PorterDuff.Mode.DARKEN   -> "Darken: Keeps the darker pixels of text and background."
            android.graphics.PorterDuff.Mode.LIGHTEN  -> "Lighten: Keeps the brighter pixels of text and background."
            android.graphics.PorterDuff.Mode.ADD      -> "Add: Adds text and background colors (strong light effect)."
            else                                      -> "Mode: ${mode.name}"
        }

        fun descForExtra(extra: com.flyerpix.editor.canvas.model.ExtendedBlendMode): String = when (extra) {
            com.flyerpix.editor.canvas.model.ExtendedBlendMode.HARD_LIGHT -> "Hard Light: Combines Multiply & Screen, harsher contrast."
            com.flyerpix.editor.canvas.model.ExtendedBlendMode.SOFT_LIGHT  -> "Soft Light: Soft contrast like diffused light."
            com.flyerpix.editor.canvas.model.ExtendedBlendMode.COLOR_BURN   -> "Burn: Darkens the background with high contrast."
            com.flyerpix.editor.canvas.model.ExtendedBlendMode.COLOR_DODGE  -> "Dodge: Brightens the background with a strong light effect."
            com.flyerpix.editor.canvas.model.ExtendedBlendMode.DIFFERENCE   -> "Difference: Absolute color difference (inverts contrast)."
            com.flyerpix.editor.canvas.model.ExtendedBlendMode.EXCLUSION    -> "Exclusion: Similar to Difference but with softer contrast."
        }

        fun updateButtonStates(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
            val extra = layer.blendExtra
            val mode = layer.blendMode
            tvDesc.text = if (extra != null) descForExtra(extra) else descriptionFor(mode)
            val activeColor = 0xFF1769FF.toInt()
            for ((btn, m) in buttons) {
                val active = extra == null && mode == m
                btn.setBackgroundColor(if (active) activeColor else Color.TRANSPARENT)
                btn.setTextColor(if (active) Color.WHITE else Color.LTGRAY)
                btn.strokeWidth = if (active) 3 else 1
            }
            for ((btn, e) in extraButtons) {
                val active = extra == e
                btn.setBackgroundColor(if (active) activeColor else Color.TRANSPARENT)
                btn.setTextColor(if (active) Color.WHITE else Color.LTGRAY)
                btn.strokeWidth = if (active) 3 else 1
            }
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
            panel.visibility = View.VISIBLE
            updateButtonStates(layer)
        }
        syncBlendUIHook = { layer -> syncUI(layer) }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer != null) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        fun setBlendMode(mode: android.graphics.PorterDuff.Mode) {
            pixelCanvasView.selectedLayer?.let { layer ->
                if (!layer.isLocked) {
                    layer.blendMode = mode
                    layer.blendExtra = null
                    updateButtonStates(layer)
                    pixelCanvasView.invalidate()
                }
            }
        }

        fun setExtraMode(extra: com.flyerpix.editor.canvas.model.ExtendedBlendMode) {
            pixelCanvasView.selectedLayer?.let { layer ->
                if (!layer.isLocked) {
                    layer.blendMode = android.graphics.PorterDuff.Mode.SRC_OVER
                    layer.blendExtra = extra
                    updateButtonStates(layer)
                    pixelCanvasView.invalidate()
                }
            }
        }

        for ((btn, mode) in buttons) {
            btn.setOnClickListener { setBlendMode(mode) }
        }
        for ((btn, extra) in extraButtons) {
            btn.setOnClickListener { setExtraMode(extra) }
        }

        btnReset.setOnClickListener { setBlendMode(android.graphics.PorterDuff.Mode.SRC_OVER) }
    }

    /**
     * Wire tombol header ✓ (terapkan) dan ✕ (batal) pada halaman Effect Settings.
     */
    private fun initializeEffectSettingsHeader() {
        binding.effectSettingsInclude.btnEffectApply.setOnClickListener {
            // Jika dibuka dari Object menu, arahkan ke ObjectPanelController
            if (objectEffectSettingsActive()) {
                onObjectEffectApply()
            } else {
                applyEffectSettings()
            }
        }
        binding.effectSettingsInclude.btnEffectCancel.setOnClickListener {
            if (objectEffectSettingsActive()) {
                onObjectEffectCancel()
            } else {
                cancelEffectSettings()
            }
        }
    }

    /** Callback di-set oleh EditorActivity setelah ObjectPanelController dibuat. */
    var onObjectEffectApply: () -> Unit = {}
    var onObjectEffectCancel: () -> Unit = {}
    var isObjectEffectSettingsOpen: () -> Boolean = { false }

    private fun objectEffectSettingsActive(): Boolean = isObjectEffectSettingsOpen()

    /**
     * Inisialisasi panel kontrol Neon / Glow.
     * Slider & switch operate real-time; warna dari palet chip di sisi kiri.
     */
    private fun initializeNeonControls() {
        val b = binding.effectSettingsInclude.neonControlsInclude
        val switch = b.switchNeonEnabled
        val group = b.neonControlsGroup
        val sRadius = b.sliderNeonRadius
        val sIntensity = b.sliderNeonIntensity
        val switchCore = b.switchNeonCore
        val tvRadius = b.tvNeonRadiusLabel
        val tvIntensity = b.tvNeonIntensityLabel
        val tvColor = b.tvNeonColorValue
        val chipPreview = b.chipNeonColorPreview
        val btnPick = b.btnPickNeonColor
        val btnReset = b.btnResetNeon

        fun setupColorPreview(layer: TextLayer?) {
            val color = layer?.neonColor ?: 0xFF00E5FF.toInt()
            chipPreview.setCardBackgroundColor(color)
            tvColor.text = String.format(Locale.US, "#%08X", color)
        }

        fun openColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.neonColor,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.NEON_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.NEON_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (!isGradient) {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        0xFF00E5FF.toInt()
                    )
                    applyToTextLayer { layer ->
                        layer.neonColor = color
                        layer.neonEnabled = true
                    }
                    setupColorPreview(pixelCanvasView.selectedLayer as? TextLayer)
                    pixelCanvasView.invalidate()
                }
            }

        chipPreview.setOnClickListener { openColorPicker() }
        btnPick.setOnClickListener { openColorPicker() }

        fun syncUI(layer: TextLayer?) {
            if (layer == null) {
                b.root.visibility = View.GONE
                return
            }
            b.root.visibility = View.VISIBLE
            eventsGated = true
            switch.isChecked = layer.neonEnabled
            group.visibility = if (layer.neonEnabled) View.VISIBLE else View.GONE
            sRadius.value = layer.neonRadius.coerceIn(1f, 40f)
            sIntensity.value = layer.neonIntensity.coerceIn(0.1f, 2f)
            switchCore.isChecked = layer.neonCoreEnabled
            tvRadius.text = "Glow Radius: ${(layer.neonRadius * 10).toInt() / 10f}"
            tvIntensity.text = "Intensity: ${(layer.neonIntensity * 100).toInt() / 100f}"
            setupColorPreview(layer)
            eventsGated = false
        }
        syncNeonUIHook = { layer -> syncUI(layer) }

        switch.setOnCheckedChangeListener { _, isChecked ->
            if (eventsGated) return@setOnCheckedChangeListener
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer {
                it.neonEnabled = isChecked
                if (isChecked) onCanvasChanged()
            }
        }

        sRadius.addOnChangeListener { _, value, _ ->
            if (eventsGated) return@addOnChangeListener
            tvRadius.text = "Glow Radius: ${(value * 10).toInt() / 10f}"
            applyToTextLayer { it.neonRadius = value }
        }
        sIntensity.addOnChangeListener { _, value, _ ->
            if (eventsGated) return@addOnChangeListener
            tvIntensity.text = "Intensity: ${(value * 100).toInt() / 100f}"
            applyToTextLayer { it.neonIntensity = value }
        }
        switchCore.setOnCheckedChangeListener { _, isChecked ->
            if (eventsGated) return@setOnCheckedChangeListener
            applyToTextLayer {
                it.neonCoreEnabled = isChecked
                onCanvasChanged()
            }
        }

        // Palet warna chip (dibangun sekali saat init).
        val palette = intArrayOf(
            0xFF00E5FF.toInt(), 0xFF00FF88.toInt(), 0xFFFF00FF.toInt(), 0xFFFFFF00.toInt(),
            0xFFFF4500.toInt(), 0xFF00BFFF.toInt(), 0xFF3AFF3A.toInt(), 0xFFFF1493.toInt(),
            0xFFFFFAFA.toInt(), 0xFF000000.toInt()
        )
        val density = activity.resources.displayMetrics.density
        val row = b.llNeonColorPalette
        row.removeAllViews()
        for (c in palette) {
            val chip = android.view.View(activity).apply {
                val s = (32 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(s, s).apply { marginEnd = (8 * density).toInt() }
                setBackgroundResource(R.drawable.bg_color_swatch)
                backgroundTintList = android.content.res.ColorStateList.valueOf(c)
                isClickable = true
                isFocusable = true
            }
            chip.setTag(c)
            chip.setOnClickListener {
                applyToTextLayer {
                    it.neonColor = c
                    it.neonEnabled = true
                    eventsGated = true
                    switch.isChecked = true
                    group.visibility = View.VISIBLE
                    eventsGated = false
                    setupColorPreview(pixelCanvasView.selectedLayer as? TextLayer)
                    onCanvasChanged()
                }
            }
            row.addView(chip)
        }

        btnReset.setOnClickListener {
            pixelCanvasView.selectedLayer?.let { layer ->
                if (layer is TextLayer && !layer.isLocked) {
                    layer.neonColor = 0xFF00E5FF.toInt()
                    layer.neonRadius = 12f
                    layer.neonIntensity = 1f
                    layer.neonCoreEnabled = true
                    layer.neonEnabled = true
                    eventsGated = true
                    switch.isChecked = true
                    group.visibility = View.VISIBLE
                    sRadius.value = layer.neonRadius
                    sIntensity.value = layer.neonIntensity
                    switchCore.isChecked = true
                    eventsGated = false
                    syncUI(layer)
                    pixelCanvasView.invalidate()
                }
            }
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            syncUI(layer as? TextLayer)
        }
    }

    /**
     * Sinkronkan UI kontrol efek aktif dengan nilai layer saat halaman dibuka.
     */
    private fun syncEffectUI(tag: String, layer: TextLayer?) {        if (layer == null) return
        val fs = binding.effectSettingsInclude
        when (tag) {
            TOOL_SHADOW -> syncShadowUIHook?.invoke(layer)
            TOOL_STROKE -> syncStrokeUIHook?.invoke(layer)
            TOOL_LINE, TOOL_LETTER -> syncSpacingUIHook?.invoke(layer)
            TOOL_ALIGN -> syncAlignUIHook?.invoke(layer)
            TOOL_BG -> syncBackgroundUIHook?.invoke(layer)
            TOOL_CURVE -> syncCurveUIHook?.invoke(layer)
            TOOL_STYLE -> syncStyleUIHook?.invoke(layer)
            TOOL_MASK -> syncMaskUIHook?.invoke(layer)
            TOOL_OPACITY -> syncOpacityUIHook?.invoke(layer)
            TOOL_ROTATE -> syncRotateUIHook?.invoke(layer)
            TOOL_COLOR -> syncColorUIHook?.invoke(layer)
            TOOL_PADDING -> syncPaddingUIHook?.invoke(layer)
            TOOL_SIZE -> syncSizeUIHook?.invoke(layer)
            TOOL_POSITION -> syncPositionUIHook?.invoke(layer)
            TOOL_REL_POS -> syncRelativePositionUIHook?.invoke(layer)
            TOOL_STYLES -> syncStylesUIHook?.invoke(layer)
            TOOL_INNER -> syncInnerShadowUIHook?.invoke(layer)
            TOOL_EMBOSS -> syncEmbossUIHook?.invoke(layer)
            TOOL_GRADIENT -> syncGradientUIHook?.invoke(layer)
            TOOL_TEXTURE -> syncTextureUIHook?.invoke(layer)
            TOOL_3D_TEXT -> rebuildExtrudePaletteHook?.invoke(layer)
            TOOL_3D_SHADOW -> syncShadow3DUIHook?.invoke(layer)
            TOOL_REFLECTION -> syncReflectionUIHook?.invoke(layer)
            TOOL_3D_ROTATE -> {
                val c = fs.rotate3DControlsInclude
                c.sliderRotateX.value = layer.rotate3DX.coerceIn(-180f, 180f)
                c.sliderRotateY.value = layer.rotate3DY.coerceIn(-180f, 180f)
                c.sliderRotateZ.value = layer.rotate3DZ.coerceIn(-180f, 180f)
            }
            TOOL_PERSPECTIVE -> syncPerspectiveUIHook?.invoke(layer)
            TOOL_BLEND -> syncBlendUIHook?.invoke(layer)
            TOOL_NEON -> syncNeonUIHook?.invoke(layer)
        }
        val title = textToolLabels[tag] ?: "Effect Settings"
        binding.effectSettingsInclude.effectSettingsTitle.text = title
    }

    /**
     * Membuka EditTextDialog interaktif untuk mengubah isi teks layer.
     */

    // ────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Helper: terapkan perubahan ke TextLayer yang sedang terpilih lalu invalidate canvas.
     */
    private inline fun applyToTextLayer(block: (TextLayer) -> Unit) {
        val layer = pixelCanvasView.selectedLayer as? TextLayer
        if (layer != null && !layer.isLocked) {
            block(layer)
            pixelCanvasView.invalidate()
        }
    }

    /**
     * Decode Bitmap secara aman dari Uri galeri HP dengan async background thread,
     * resolusi terkontrol via downsampling, dan format ARGB_8888.
     * 
     * @param uri URI gambar dari galeri
     * @param maxSize Ukuran maksimum dimensi (default 2048px untuk performa optimal)
     * @return Bitmap yang sudah di-downsample atau null jika gagal
     */
    suspend fun decodeBitmapFromUriAsync(uri: Uri, maxSize: Int = 2048): Bitmap? {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Modern API: ImageDecoder dengan proper downsampling
                    val source = ImageDecoder.createSource(activity.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val maxDim = kotlin.math.max(info.size.width, info.size.height)
                        if (maxDim > maxSize) {
                            decoder.setTargetSampleSize(maxDim / maxSize)
                        }
                    }
                    // ImageDecoder already returns ARGB_8888, no need to copy
                } else {
                    // Legacy API: Manual downsampling dengan inSampleSize
                    activity.contentResolver.openInputStream(uri)?.use { stream ->
                        // Step 1: Decode bounds only untuk dapat dimensi
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(stream, null, options)
                        
                        // Step 2: Calculate inSampleSize
                        val maxDim = kotlin.math.max(options.outWidth, options.outHeight)
                        val sampleSize = if (maxDim > maxSize) {
                            var size = 1
                            var dimension = maxDim
                            while (dimension / 2 >= maxSize) {
                                size *= 2
                                dimension /= 2
                            }
                            size
                        } else 1
                        
                        // Step 3: Decode actual bitmap dengan downsampling
                        activity.contentResolver.openInputStream(uri)?.use { stream2 ->
                            val finalOptions = BitmapFactory.Options().apply {
                                inSampleSize = sampleSize
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                                inJustDecodeBounds = false
                            }
                            BitmapFactory.decodeStream(stream2, null, finalOptions)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Legacy synchronous method - deprecated, use decodeBitmapFromUriAsync instead
     * Kept for backward compatibility
     */
    @Deprecated("Use decodeBitmapFromUriAsync for better performance")
    fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(activity.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDim = kotlin.math.max(info.size.width, info.size.height)
                    if (maxDim > 2048) {
                        decoder.setTargetSampleSize(maxDim / 2048)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                activity.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            bmp?.copy(Bitmap.Config.ARGB_8888, true) ?: bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Menerapkan texture dari bitmap hasil picker.
     */
    fun applyTextureBitmap(bitmap: Bitmap) {
        applyToTextLayer { layer ->
            layer.textureBitmap = bitmap
            layer.textureEnabled = true
        }
        val curLayer = pixelCanvasView.selectedLayer as? TextLayer
        if (curLayer != null) {
            val b = binding.effectSettingsInclude.textureControlsInclude
            b.imgTextureThumbnail.setImageBitmap(bitmap)
            b.btnSelectTexture.text = "Change Photo"
            b.btnDeleteTexture.visibility = View.VISIBLE
            b.switchTextureEnabled.isChecked = true
            b.textureControlsGroup.visibility = View.VISIBLE
            syncMaskUIHook?.invoke(curLayer)
        }
        showSnackbar("Photo texture applied to text!")
    }


    private fun openTextPage() {
        onShowMenu(R.id.nav_text)
    }

    private fun closeTextPage(resetNav: Boolean = true) {
        isPageOpen = false
        pagePinnedByNav = false
        activeTextToolTag = ""
        for (v in textPanelViews.values) v.visibility = View.GONE
        refreshTextPageUI()
        if (resetNav) {
            binding.bottomNavigation.selectedItemId = R.id.nav_presets
        } else {
            onShowMenu(R.id.nav_presets)
        }
    }

    private fun refreshTextPageUI() {
        val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
        val hasEditor = layer != null && !layer.isLocked

        binding.textEditorBar.visibility = if (isPageOpen && !effectSettingsOpen) View.VISIBLE else View.GONE
        binding.textCategoryStripInclude.root.visibility =
            if (isPageOpen && !effectSettingsOpen) View.VISIBLE else View.GONE
        updateTextCategorySelection()
        updateVisibleTextTools()
        updateEffectSettingsVisibility()
        binding.textPropertyPanelInclude.root.visibility =
            if (isPageOpen && hasEditor && activeTextToolTag.isNotEmpty() && activeTextToolTag !in complexEffectTags) View.VISIBLE else View.GONE
        binding.textToolStripInclude.textToolStripScroll.visibility =
            if (isPageOpen && activeTextToolTag !in complexEffectTags) View.VISIBLE else View.GONE

        if (isPageOpen && hasEditor) {
            if (activeTextToolTag.isEmpty()) {
                for (v in textPanelViews.values) v.visibility = View.GONE
            } else {
                applyTextPanelVisibility()
            }
            updateTextPanelTitle()
        } else {
            for (v in textPanelViews.values) v.visibility = View.GONE
        }
        clampPropertyPanelHeight()
        onCanvasChanged()
    }

    /**
     * Tampilkan/sembunyikan halaman Effect Settings sesuai tool yang aktif.
     * Halaman ini mengambil alih area strip + panel properti sehingga efek
     * kompleks mendapat ruang yang lebih lega tanpa tumpukan menu.
     */
    private fun updateEffectSettingsVisibility() {
        val show = effectSettingsOpen && isPageOpen && activeTextToolTag in complexEffectTags &&
            (pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer)?.let { !it.isLocked } ?: false
        val wasOpen = binding.effectSettingsInclude.root.visibility == View.VISIBLE
        val changed = wasOpen != show
        binding.effectSettingsInclude.root.visibility = if (show) View.VISIBLE else View.GONE
        effectSettingsOpen = show
        if (changed) onEffectSettingsOpenChanged(show)
    }

    /**
     * Entri titik-antarmuka: panggil dari Activity & controller lain untuk
     * membuka halaman Effect Settings dari tool strip.
     */
    fun openEffectSettings(tag: String) {
        val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
        if (layer == null || layer.isLocked) {
            showSnackbar("Select a text layer first")
            return
        }
        if (tag !in complexEffectTags) {
            selectTextTool(tag)
            return
        }
        if (effectSettingsOpen && activeTextToolTag == tag) return
        textToolTagBeforeEffect = activeTextToolTag.takeUnless { it in complexEffectTags } ?: ""
        snapshotCurrentState()
        activeTextToolTag = tag
        effectSettingsOpen = true
        eventsGated = true
        binding.textEditorBar.visibility = View.GONE
        for (v in textPanelViews.values) v.visibility = View.GONE
        updateEffectSettingsVisibility()
        syncEffectUI(tag, layer)
        eventsGated = false
    }

    /**
     * Terapkan (✓): tutup halaman settings namun pertahankan perubahan di kanvas.
     */
    fun applyEffectSettings() { closeEffectSettings() }

    /**
     * Periksa apakah halaman Effect Settings sedang terbuka (untuk back-press).
     */
    fun isEffectSettingsOpen(): Boolean = effectSettingsOpen

    /**
     * Batal (✕): kembalikan seluruh parameter efek ke kondisi sebelum halaman dibuka.
     */
    fun cancelEffectSettings() {
        val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
        if (layer != null) restoreSnapshot(layer)
        closeEffectSettings()
    }

    /**
     * Tutup halaman settings (baik via ✓ maupun ✕) dan kembali ke strip tool.
     */
    private fun closeEffectSettings() {
        settingsSnapshot = null
        effectSettingsOpen = false
        activeTextToolTag = textToolTagBeforeEffect
        textToolTagBeforeEffect = ""
        pixelCanvasView.invalidate()
        refreshTextPageUI()
    }

    /**
     * Simpan snapshot parameter efek-affectable sebelum pengguna mengedit.
    * Salin layer agar perubahan real-time tidak ikut mengubah snapshot.
     */
    private fun snapshotCurrentState() {
        val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
        settingsSnapshot = layer?.copyLayer()
    }

    private fun restoreSnapshot(snapshot: com.flyerpix.editor.canvas.model.TextLayer) {
        val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
        layer.shadowEnabled        = snapshot.shadowEnabled
        layer.shadowColor          = snapshot.shadowColor
        layer.shadowRadius         = snapshot.shadowRadius
        layer.shadowDx             = snapshot.shadowDx
        layer.shadowDy             = snapshot.shadowDy
        layer.shadowOpacity        = snapshot.shadowOpacity
        layer.innerShadowEnabled   = snapshot.innerShadowEnabled
        layer.innerShadowColor     = snapshot.innerShadowColor
        layer.innerShadowRadius    = snapshot.innerShadowRadius
        layer.innerShadowDx        = snapshot.innerShadowDx
        layer.innerShadowDy        = snapshot.innerShadowDy
        layer.innerShadowOpacity   = snapshot.innerShadowOpacity
        layer.embossEnabled        = snapshot.embossEnabled
        layer.embossLightAngle     = snapshot.embossLightAngle
        layer.embossAmbient        = snapshot.embossAmbient
        layer.embossSpecular       = snapshot.embossSpecular
        layer.embossIntensity      = snapshot.embossIntensity
        layer.embossBevel          = snapshot.embossBevel
        layer.gradientEnabled      = snapshot.gradientEnabled
        layer.gradient             = snapshot.gradient?.copy()
        layer.textureEnabled       = snapshot.textureEnabled
        layer.textureBitmap        = snapshot.textureBitmap
        layer.textureScale         = snapshot.textureScale
        layer.textureRotation      = snapshot.textureRotation
        layer.extrudeEnabled       = snapshot.extrudeEnabled
        layer.extrudeDepth         = snapshot.extrudeDepth
        layer.extrudeColor         = snapshot.extrudeColor
        layer.extrudeGradient      = snapshot.extrudeGradient?.copy()
        layer.extrudeViewType      = snapshot.extrudeViewType
        layer.extrudeAngle         = snapshot.extrudeAngle
        layer.shadow3DEnabled      = snapshot.shadow3DEnabled
        layer.shadow3DDepth        = snapshot.shadow3DDepth
        layer.shadow3DColor        = snapshot.shadow3DColor
        layer.shadow3DViewType     = snapshot.shadow3DViewType
        layer.shadow3DAngle        = snapshot.shadow3DAngle
        layer.shadow3DBlur         = snapshot.shadow3DBlur
        layer.shadow3DOpacity      = snapshot.shadow3DOpacity
        layer.reflectionEnabled        = snapshot.reflectionEnabled
        layer.reflectionOpacity        = snapshot.reflectionOpacity
        layer.reflectionDistance       = snapshot.reflectionDistance
        layer.reflectionFade           = snapshot.reflectionFade
        layer.rotate3DX            = snapshot.rotate3DX
        layer.rotate3DY            = snapshot.rotate3DY
        layer.rotate3DZ            = snapshot.rotate3DZ
        layer.perspectiveEnabled   = snapshot.perspectiveEnabled
        layer.perspectiveCorners   = snapshot.perspectiveCorners.clone()
        layer.blendMode            = snapshot.blendMode
        layer.blendExtra           = snapshot.blendExtra
        layer.neonEnabled          = snapshot.neonEnabled
        layer.neonColor            = snapshot.neonColor
        layer.neonRadius           = snapshot.neonRadius
        layer.neonIntensity        = snapshot.neonIntensity
        layer.neonCoreEnabled      = snapshot.neonCoreEnabled
        layer.strokeColor          = snapshot.strokeColor
        layer.strokeWidth          = snapshot.strokeWidth
        layer.letterSpacing        = snapshot.letterSpacing
        layer.lineSpacing          = snapshot.lineSpacing
        layer.alignment            = snapshot.alignment
        layer.justifyEnabled       = snapshot.justifyEnabled
        layer.wrapTextEnabled      = snapshot.wrapTextEnabled
        layer.wrapWidth            = snapshot.wrapWidth
        layer.bgEnabled            = snapshot.bgEnabled
        layer.bgColor              = snapshot.bgColor
        layer.bgOpacity            = snapshot.bgOpacity
        layer.bgPadding            = snapshot.bgPadding
        layer.bgCornerRadius       = snapshot.bgCornerRadius
        layer.curvePercent         = snapshot.curvePercent
        layer.isBold               = snapshot.isBold
        layer.isItalic             = snapshot.isItalic
        layer.isUnderline          = snapshot.isUnderline
        layer.isStrikethrough      = snapshot.isStrikethrough
        layer.typeface             = snapshot.typeface
        layer.fontName             = snapshot.fontName
        layer.opacity              = snapshot.opacity
        layer.rotation             = snapshot.rotation
        layer.textSize             = snapshot.textSize
        layer.scale                = snapshot.scale
        layer.x                    = snapshot.x
        layer.y                    = snapshot.y
        layer.paddingTop           = snapshot.paddingTop
        layer.paddingBottom        = snapshot.paddingBottom
        layer.paddingLeft          = snapshot.paddingLeft
        layer.paddingRight         = snapshot.paddingRight
    }

    /**
     * Jeda sementara listener slider/switch agar nilai awal yang di-set ke UI
     * saat membuka halaman settings tidak tercatat sebagai perubahan on-submit.
     */
    private var eventsGated = false

    private fun updateTextPanelTitle() {
        binding.textPropertyPanelInclude.textPropertyPanelTitle.text =
            textToolLabels[activeTextToolTag] ?: "Tool"
    }

private fun registerTextPanels() {
        val tp = binding.textPropertyPanelInclude
        textPanelViews.clear()
        textPanelViews[TOOL_FONT]        = tp.fontPanel.root

        // Efek kompleks → halaman Effect Settings terpisah
        val fs = binding.effectSettingsInclude
        textPanelViews[TOOL_GRADIENT]    = fs.gradientControlsInclude.root
        textPanelViews[TOOL_TEXTURE]     = fs.textureControlsInclude.root
        textPanelViews[TOOL_SHADOW]      = fs.shadowControlsInclude.root
        textPanelViews[TOOL_INNER]       = fs.innerShadowControlsInclude.root
        textPanelViews[TOOL_EMBOSS]      = fs.embossControlsInclude.root
        textPanelViews[TOOL_PERSPECTIVE] = fs.perspectiveControlsInclude.root
        textPanelViews[TOOL_3D_ROTATE]   = fs.rotate3DControlsInclude.root
        textPanelViews[TOOL_3D_TEXT]     = fs.extrudeControlsInclude.root
        textPanelViews[TOOL_3D_SHADOW]   = fs.shadow3DControlsInclude.root
        textPanelViews[TOOL_REFLECTION]  = fs.reflectionControlsInclude.root
        textPanelViews[TOOL_BLEND]       = fs.blendControlsInclude.root
        textPanelViews[TOOL_NEON]        = fs.neonControlsInclude.root
        textPanelViews[TOOL_LETTER]      = fs.spacingControlsInclude.root
        textPanelViews[TOOL_LINE]        = fs.spacingControlsInclude.root
        textPanelViews[TOOL_STROKE]      = fs.strokeControlsInclude.root
        textPanelViews[TOOL_ALIGN]       = fs.alignControlsInclude.root
        textPanelViews[TOOL_BG]          = fs.backgroundControlsInclude.root
        textPanelViews[TOOL_CURVE]       = fs.curveControlsInclude.root
        textPanelViews[TOOL_STYLE]       = fs.styleControlsInclude.root
        textPanelViews[TOOL_MASK]        = fs.maskControlsInclude.root
        textPanelViews[TOOL_OPACITY]     = fs.opacityControlsInclude.root
        textPanelViews[TOOL_ROTATE]      = fs.rotateControlsInclude.root
        textPanelViews[TOOL_COLOR]       = fs.colorControlsInclude.root
        textPanelViews[TOOL_PADDING]     = fs.paddingControlsInclude.root
        textPanelViews[TOOL_POSITION]    = fs.positionControlsInclude.root
        textPanelViews[TOOL_REL_POS]     = fs.relativePositionControlsInclude.root
        textPanelViews[TOOL_SIZE]        = fs.sizeControlsInclude.root
        textPanelViews[TOOL_STYLES]      = fs.stylesControlsInclude.root
    }

    private fun buildTextToolStrip() {
        val specs = listOf(
            TextToolSpec(TOOL_STYLES,      "Saved Styles", R.drawable.ic_text_style_24px),
            TextToolSpec(TOOL_EDIT,        "Edit",        R.drawable.ic_edit_24px),
            TextToolSpec(TOOL_DELETE,      "Delete",      R.drawable.ic_delete_24px),
            TextToolSpec(TOOL_COPY,        "Copy",        R.drawable.ic_copy_24px),
            TextToolSpec(TOOL_FRONT,       "To Front",    R.drawable.ic_bring_to_front_24px),
            TextToolSpec(TOOL_BACK,        "To Back",     R.drawable.ic_send_to_back_24px),
            TextToolSpec(TOOL_POSITION,    "Position",    R.drawable.ic_position_24px),
            TextToolSpec(TOOL_REL_POS,     "Relative",    R.drawable.ic_relative_position_24px),
            TextToolSpec(TOOL_SIZE,        "Size",        R.drawable.ic_size_24px),
            TextToolSpec(TOOL_PADDING,     "Padding",     R.drawable.ic_padding_24px),
            TextToolSpec(TOOL_COLOR,       "Color",       R.drawable.ic_sharp_palette_24px),
            TextToolSpec(TOOL_GRADIENT,    "Gradient",    R.drawable.ic_gradient_24px),
            TextToolSpec(TOOL_TEXTURE,     "Texture",     R.drawable.ic_texture_24px),
            TextToolSpec(TOOL_OPACITY,     "Opacity",     R.drawable.ic_opacity_24px),
            TextToolSpec(TOOL_ROTATE,      "Rotate",      R.drawable.ic_rotate_right_24px),
            TextToolSpec(TOOL_MASK,        "Mask",        R.drawable.ic_mask_24px),
            TextToolSpec(TOOL_FONT,        "Font",        R.drawable.ic_text_fields_24px),
            TextToolSpec(TOOL_STYLE,       "Style",       R.drawable.ic_text_style_24px),
            TextToolSpec(TOOL_CURVE,       "Curve",       R.drawable.ic_curve_24px),
            TextToolSpec(TOOL_BG,          "Background",  R.drawable.ic_background_24px),
            TextToolSpec(TOOL_ALIGN,       "Align",       R.drawable.ic_align_24px),
            TextToolSpec(TOOL_LETTER,      "Letter",      R.drawable.ic_letter_spacing_24px),
            TextToolSpec(TOOL_LINE,        "Line",        R.drawable.ic_line_spacing_24px),
            TextToolSpec(TOOL_STROKE,      "Stroke",      R.drawable.ic_stroke_24px),
            TextToolSpec(TOOL_SHADOW,      "Shadow",      R.drawable.ic_shadow_24px),
            TextToolSpec(TOOL_INNER,       "Inner",       R.drawable.ic_inner_shadow_24px),
            TextToolSpec(TOOL_EMBOSS,      "Emboss",      R.drawable.ic_emboss_24px),
            TextToolSpec(TOOL_PERSPECTIVE, "Perspective", R.drawable.ic_perspective_24px),
            TextToolSpec(TOOL_3D_ROTATE,   "3D Rotate",   R.drawable.ic_3d_rotate_24px),
            TextToolSpec(TOOL_3D_TEXT,     "3D Text",     R.drawable.ic_3d_text_24px),
            TextToolSpec(TOOL_3D_SHADOW,   "3D Shadow",   R.drawable.ic_3d_shadow_24px),
            TextToolSpec(TOOL_REFLECTION,  "Reflection",  R.drawable.ic_reflection_24px),
            TextToolSpec(TOOL_BLEND,       "Blend",       R.drawable.ic_layers_24px),
            TextToolSpec(TOOL_NEON,        "Neon",        R.drawable.ic_neon_24px)
        )

        val density = activity.resources.displayMetrics.density
        val container = binding.textToolStripInclude.textToolStripContainer
        container.removeAllViews()

        for (spec in specs) {
            textToolLabels[spec.tag] = spec.label
            val item = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                isClickable = true
                isFocusable = true
                setBackgroundResource(R.drawable.bg_panel_tool_item)
                setPadding(
                    (6 * density).toInt(), (6 * density).toInt(),
                    (6 * density).toInt(), (4 * density).toInt()
                )
                setOnClickListener { onTextToolClicked(spec.tag) }
            }
            val iconSize = (22 * density).toInt()
            val icon = android.widget.ImageView(activity).apply {
                setImageResource(spec.iconRes)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                colorFilter = android.graphics.PorterDuffColorFilter(
                    COLOR_GRAY,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            val label = android.widget.TextView(activity).apply {
                text = spec.label
                textSize = 9.5f
                maxLines = 1
                gravity = android.view.Gravity.CENTER
                setTextColor(COLOR_GRAY)
            }
            item.addView(icon)
            item.addView(label)
            val layerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layerParams.width = (52 * density).toInt()
            container.addView(item, layerParams)
            textToolItems[spec.tag] = item
        }
        updateVisibleTextTools()
    }

    private fun buildTextCategoryStrip() {
        val categories = listOf(
            CATEGORY_BASIC to "Basic",
            CATEGORY_LAYOUT to "Layout",
            CATEGORY_TEXT to "Text",
            CATEGORY_APPEARANCE to "Appearance",
            CATEGORY_EFFECTS to "Effects",
            CATEGORY_ADVANCED to "Advanced",
            CATEGORY_LAYER to "Layer"
        )
        val density = activity.resources.displayMetrics.density
        val container = binding.textCategoryStripInclude.textCategoryContainer
        container.removeAllViews()
        textCategoryItems.clear()

        for ((category, labelText) in categories) {
            val label = TextView(activity).apply {
                text = labelText
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                isClickable = true
                isFocusable = true
                setPadding(
                    (14 * density).toInt(), (7 * density).toInt(),
                    (14 * density).toInt(), (7 * density).toInt()
                )
                setBackgroundResource(R.drawable.bg_text_category_item)
                setOnClickListener { selectTextCategory(category) }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
            container.addView(label, params)
            textCategoryItems[category] = label
        }
        updateTextCategorySelection()
    }

    private fun selectTextCategory(category: String) {
        if (activeTextCategory == category) return
        activeTextCategory = category
        if (activeTextToolTag.isNotEmpty() && !isToolInCategory(activeTextToolTag, category)) {
            deselectTextTool()
        }
        updateTextCategorySelection()
        updateVisibleTextTools()
    }

    private fun updateTextCategorySelection() {
        for ((category, item) in textCategoryItems) {
            item.isSelected = category == activeTextCategory
            item.setTextColor(if (item.isSelected) Color.WHITE else COLOR_GRAY)
        }
    }

    private fun updateVisibleTextTools() {
        for ((tag, item) in textToolItems) {
            item.visibility = if (isToolInCategory(tag, activeTextCategory)) View.VISIBLE else View.GONE
        }
    }

    private fun isToolInCategory(tag: String, category: String): Boolean {
        return when (category) {
            CATEGORY_BASIC -> tag in setOf(TOOL_EDIT, TOOL_FONT, TOOL_STYLE)
            CATEGORY_LAYOUT -> tag in setOf(TOOL_POSITION, TOOL_REL_POS, TOOL_SIZE, TOOL_PADDING, TOOL_ROTATE, TOOL_ALIGN)
            CATEGORY_TEXT -> tag in setOf(TOOL_LETTER, TOOL_LINE, TOOL_CURVE, TOOL_BG, TOOL_MASK)
            CATEGORY_APPEARANCE -> tag in setOf(TOOL_COLOR, TOOL_GRADIENT, TOOL_TEXTURE, TOOL_OPACITY, TOOL_STROKE)
            CATEGORY_EFFECTS -> tag in setOf(TOOL_SHADOW, TOOL_INNER, TOOL_EMBOSS, TOOL_REFLECTION, TOOL_NEON)
            CATEGORY_ADVANCED -> tag in setOf(TOOL_PERSPECTIVE, TOOL_3D_ROTATE, TOOL_3D_TEXT, TOOL_3D_SHADOW, TOOL_BLEND)
            CATEGORY_LAYER -> tag in setOf(TOOL_STYLES, TOOL_COPY, TOOL_FRONT, TOOL_BACK, TOOL_DELETE)
            else -> false
        }
    }

    private fun onTextToolClicked(tag: String) {
        val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
        if (layer == null || layer.isLocked) {
            showSnackbar("Select a text layer first")
            return
        }
        if (tag == activeTextToolTag && tag != TOOL_EDIT) {
            deselectTextTool()
            return
        }
        when (tag) {
            TOOL_EDIT -> {
                selectTextTool(tag)
                onEditTextRequested(layer)
                return
            }
            TOOL_DELETE -> {
                val ok = pixelCanvasView.deleteSelectedLayer()
                showSnackbar(if (ok) "Text layer deleted" else "No layer selected")
                return
            }
            TOOL_COPY -> {
                val copy = pixelCanvasView.duplicateSelectedLayer()
                showSnackbar(if (copy != null) "Text layer duplicated" else "Failed to duplicate layer")
                return
            }
            TOOL_FRONT -> {
                pixelCanvasView.bringSelectedLayerToFront()
                showSnackbar("Layer moved to front")
                return
            }
            TOOL_BACK -> {
                pixelCanvasView.sendSelectedLayerToBack()
                showSnackbar("Layer moved to back")
                return
            }
            TOOL_FONT -> {
                onFontRequested(layer)
                return
            }
        }
        selectTextTool(tag)
    }

    private fun selectTextTool(tag: String) {
        if (isInitializing) return
        if (activeTextToolTag == tag) return

        // Routing untuk efek kompleks → buka halaman Effect Settings
        if (tag in complexEffectTags) {
            openEffectSettings(tag)
            return
        }

        activeTextToolTag = tag
        if (textToolItems.isEmpty()) return

        // Sembunyikan halaman Effect Settings jika beralih ke tool sederhana
        updateEffectSettingsVisibility()

        for ((t, item) in textToolItems) {
            val selected = t == tag
            item.isSelected = selected
            val icon = item.getChildAt(0) as? android.widget.ImageView
            val label = item.getChildAt(1) as? android.widget.TextView
            val c = if (selected) COLOR_ACTIVE else COLOR_GRAY
            icon?.colorFilter =
                android.graphics.PorterDuffColorFilter(c, android.graphics.PorterDuff.Mode.SRC_IN)
            label?.setTextColor(c)
        }
        applyTextPanelVisibility()
        binding.textPropertyPanelInclude.root.visibility = View.VISIBLE
        updateTextPanelTitle()
        binding.textToolStripInclude.textToolStripScroll.visibility = View.VISIBLE
    }

    private fun deselectTextTool() {
        activeTextToolTag = ""
        for ((_, item) in textToolItems) {
            item.isSelected = false
            val icon = item.getChildAt(0) as? android.widget.ImageView
            val label = item.getChildAt(1) as? android.widget.TextView
            icon?.colorFilter =
                android.graphics.PorterDuffColorFilter(COLOR_GRAY, android.graphics.PorterDuff.Mode.SRC_IN)
            label?.setTextColor(COLOR_GRAY)
        }
        binding.textPropertyPanelInclude.root.visibility = View.GONE
        for (v in textPanelViews.values) v.visibility = View.GONE
        updateTextPanelTitle()
        clampPropertyPanelHeight()
        onCanvasChanged()
    }

    private fun applyTextPanelVisibility() {
        for ((t, v) in textPanelViews) {
            v.visibility = if (t == activeTextToolTag) View.VISIBLE else View.GONE
        }
    }

    private fun clampPropertyPanelHeight() {
        if (maxPropertyPanelScrollH == 0) {
            maxPropertyPanelScrollH = (activity.resources.displayMetrics.heightPixels * 0.32f).toInt()
        }
        val scroll = binding.textPropertyPanelInclude.textPropertyPanelScroll
        scroll.post {
            val contentH = scroll.getChildAt(0)?.height ?: 0
            val minH = (48 * activity.resources.displayMetrics.density).toInt()
            val target = minOf(contentH, maxPropertyPanelScrollH).coerceAtLeast(minH)
            if (scroll.layoutParams.height != target) {
                scroll.layoutParams = scroll.layoutParams.apply { height = target }
                scroll.requestLayout()
                onCanvasChanged()
            }
        }
    }

    private fun configurePanelHeights() {
        binding.effectSettingsInclude.root.post {
            val density = activity.resources.displayMetrics.density
            val screenHeight = activity.resources.displayMetrics.heightPixels
            val preferred = (screenHeight * 0.38f).toInt()
            val minHeight = (180 * density).toInt()
            val maxHeight = (360 * density).toInt()
            binding.effectSettingsInclude.root.layoutParams =
                binding.effectSettingsInclude.root.layoutParams.apply {
                    height = preferred.coerceIn(minHeight, maxHeight)
                }
        }
    }

    private fun launchColorPicker() {
        val currentLayer = pixelCanvasView.selectedLayer
        val initialColor: Int
        val initialGradient: com.flyerpix.editor.canvas.model.GradientColor?

        when (currentLayer) {
            is com.flyerpix.editor.canvas.model.TextLayer -> {
                initialColor = currentLayer.textColor
                initialGradient = if (currentLayer.gradientEnabled) currentLayer.gradient else null
            }
            is com.flyerpix.editor.canvas.model.ShapeLayer -> {
                initialColor = currentLayer.fillColor
                initialGradient = null
            }
            is com.flyerpix.editor.canvas.model.PenLayer -> {
                initialColor = currentLayer.strokeColor
                initialGradient = null
            }
            is com.flyerpix.editor.canvas.model.ArrowLayer -> {
                initialColor = currentLayer.headColor
                initialGradient = null
            }
            else -> {
                initialColor = pixelCanvasView.canvasBackgroundColor
                initialGradient = pixelCanvasView.canvasBackgroundGradient
            }
        }

        com.flyerpix.editor.ui.dialog.ColorPickerDialog
            .newInstance(initialColor, initialGradient)
             .show((activity as androidx.fragment.app.FragmentActivity).supportFragmentManager, com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG)
    }

    // ─── Effect Page: Posisi ───────────────────────────────────────────────

    private fun initializePositionControls() {
        val b = binding.effectSettingsInclude.positionControlsInclude
        val panel = b.root
        val advancedButton = panel.findViewById<MaterialButton>(R.id.btnPositionAdvanced)
        val fineControls = panel.findViewById<View>(R.id.positionFineControls)
        val centerControls = panel.findViewById<View>(R.id.positionCenterControls)
        var syncing = false
        var advancedOpen = false
        fun setAdvancedOpen(open: Boolean) {
            advancedOpen = open
            fineControls.visibility = if (open) View.VISIBLE else View.GONE
            centerControls.visibility = if (open) View.VISIBLE else View.GONE
            advancedButton.text = if (open) "Hide advanced" else "Advanced controls"
        }
        setAdvancedOpen(false)
        advancedButton.setOnClickListener { setAdvancedOpen(!advancedOpen) }
        val range = max(pixelCanvasView.width, pixelCanvasView.height).toFloat().coerceAtLeast(1000f)
        b.sliderPosX.valueFrom = -range
        b.sliderPosX.valueTo = range
        b.sliderPosY.valueFrom = -range
        b.sliderPosY.valueTo = range

        fun refreshAll(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            b.editPosX.setText(if (layer.x == 0f) "0" else String.format(Locale.US, "%.0f", layer.x))
            b.editPosY.setText(if (layer.y == 0f) "0" else String.format(Locale.US, "%.0f", layer.y))
            syncing = true
            b.sliderPosX.value = layer.x.coerceIn(-range, range)
            b.sliderPosY.value = layer.y.coerceIn(-range, range)
            syncing = false
            b.tvPosXLabel.text = String.format(Locale.US, "X: %.0f px", layer.x)
            b.tvPosYLabel.text = String.format(Locale.US, "Y: %.0f px", layer.y)
        }

        fun commitX() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            val v = b.editPosX.text.toString().toFloatOrNull() ?: return
            pixelCanvasView.runRecordedAction("Set Position X") { layer.x = v }
            pixelCanvasView.invalidate()
            refreshAll(layer)
        }

        fun commitY() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            val v = b.editPosY.text.toString().toFloatOrNull() ?: return
            pixelCanvasView.runRecordedAction("Set Position Y") { layer.y = v }
            pixelCanvasView.invalidate()
            refreshAll(layer)
        }

        b.editPosX.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commitX() }
        b.editPosY.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commitY() }
        b.editPosX.setOnEditorActionListener { _, _, _ -> commitX(); true }
        b.editPosY.setOnEditorActionListener { _, _, _ -> commitY(); true }

        fun nudgeX(delta: Float) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Move X") { layer.x += delta }
            pixelCanvasView.invalidate()
            refreshAll(layer)
        }

        fun nudgeY(delta: Float) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Move Y") { layer.y += delta }
            pixelCanvasView.invalidate()
            refreshAll(layer)
        }

        b.btnPosXMinus.setOnClickListener { nudgeX(-10f) }
        b.btnPosXPlus.setOnClickListener { nudgeX(10f) }
        b.btnPosYMinus.setOnClickListener { nudgeY(-10f) }
        b.btnPosYPlus.setOnClickListener { nudgeY(10f) }

        b.sliderPosX.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvPosXLabel.text = String.format(Locale.US, "X: %.0f px", value)
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@addOnChangeListener
            pixelCanvasView.runRecordedAction("Set Position X") { layer.x = value }
            pixelCanvasView.invalidate()
        }

        b.sliderPosY.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvPosYLabel.text = String.format(Locale.US, "Y: %.0f px", value)
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@addOnChangeListener
            pixelCanvasView.runRecordedAction("Set Position Y") { layer.y = value }
            pixelCanvasView.invalidate()
        }

        fun center(horizontal: Boolean, vertical: Boolean) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            val (lw, lh) = layer.getUnwarpedDimensions()
            pixelCanvasView.runRecordedAction("Center on Canvas") {
                if (horizontal) layer.x = (pixelCanvasView.width - lw * layer.scale) / 2f
                if (vertical) layer.y = (pixelCanvasView.height - lh * layer.scale) / 2f
            }
            refreshAll(layer)
            pixelCanvasView.invalidate()
        }

        b.btnPosCenterH.setOnClickListener { center(true, false) }
        b.btnPosCenterV.setOnClickListener { center(false, true) }
        b.btnPosCenter.setOnClickListener { center(true, true) }

        b.btnResetPosition.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            pixelCanvasView.runRecordedAction("Reset Position") {
                layer.x = 0f
                layer.y = 0f
            }
            refreshAll(layer)
            pixelCanvasView.invalidate()
        }

        syncPositionUIHook = { layer ->
            panel.visibility = View.VISIBLE
            refreshAll(layer)
        }
    }

    // ─── Effect Page: Posisi Relatif (Compact Directional Movement) ────────

    private fun initializeRelativePositionControls() {
        val b = binding.effectSettingsInclude.relativePositionControlsInclude
        val panel = b.root

        // Get pixel distance from input
        fun getPixelDistance(): Float {
            val text = b.etRelMovePixels.text.toString().trim()
            return text.toFloatOrNull() ?: 10f
        }

        // Movement functions
        fun moveLayer(deltaX: Float, deltaY: Float) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Move Layer") {
                layer.x += deltaX
                layer.y += deltaY
            }
            pixelCanvasView.invalidate()
        }

        // Left arrow: move left
        b.btnRelLeft.setOnClickListener {
            val px = -getPixelDistance()
            moveLayer(px, 0f)
        }

        // Right arrow: move right
        b.btnRelRight.setOnClickListener {
            val px = getPixelDistance()
            moveLayer(px, 0f)
        }

        // Up arrow: move up
        b.btnRelUp.setOnClickListener {
            val py = -getPixelDistance()
            moveLayer(0f, py)
        }

        // Down arrow: move down
        b.btnRelDown.setOnClickListener {
            val py = getPixelDistance()
            moveLayer(0f, py)
        }

        // Center: reset to center
        b.btnRelCenter.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            val (lw, lh) = layer.getUnwarpedDimensions()
            val w = pixelCanvasView.width
            val h = pixelCanvasView.height
            val sw = lw * layer.scale
            val sh = lh * layer.scale

            pixelCanvasView.runRecordedAction("Center Layer") {
                layer.x = (w - sw) / 2f
                layer.y = (h - sh) / 2f
            }
            pixelCanvasView.invalidate()
        }

        // Slider: adjust pixel distance in real-time
        b.sliderRelMove.addOnChangeListener { _, value, _ ->
            b.etRelMovePixels.setText(value.toInt().toString())
        }

        // Confirm button: sync value from slider
        b.btnRelConfirm.setOnClickListener {
            val sliderValue = b.sliderRelMove.value.toInt()
            b.etRelMovePixels.setText(sliderValue.toString())
        }

        // Close button: hide panel
        b.btnRelClose.setOnClickListener {
            panel.visibility = View.GONE
        }

        // Plus button: increase value
        b.btnRelPlus.setOnClickListener {
            val current = b.sliderRelMove.value
            b.sliderRelMove.value = (current + 10f).coerceAtMost(200f)
        }

        // Minus button: decrease value
        b.btnRelMinus.setOnClickListener {
            val current = b.sliderRelMove.value
            b.sliderRelMove.value = (current - 10f).coerceAtLeast(0f)
        }

        syncRelativePositionUIHook = { layer ->
            panel.visibility = View.VISIBLE
        }
    }

    // ─── Effect Page: Ukuran ───────────────────────────────────────────────

    private fun initializeSizeControls() {
        val b = binding.effectSettingsInclude.sizeControlsInclude
        val panel = b.root
        val advancedButton = panel.findViewById<MaterialButton>(R.id.btnSizeAdvanced)
        val advancedControls = panel.findViewById<View>(R.id.sizeScaleControls)
        val advancedActions = panel.findViewById<View>(R.id.sizeActionControls)
        var syncing = false
        var advancedOpen = false
        fun setAdvancedOpen(open: Boolean) {
            advancedOpen = open
            advancedControls.visibility = if (open) View.VISIBLE else View.GONE
            advancedActions.visibility = if (open) View.VISIBLE else View.GONE
            advancedButton.text = if (open) "Hide scale" else "Advanced scale controls"
        }
        setAdvancedOpen(false)
        advancedButton.setOnClickListener { setAdvancedOpen(!advancedOpen) }

        val presets = linkedMapOf(
            b.btnSizePreset24 to 24f,
            b.btnSizePreset36 to 36f,
            b.btnSizePreset48 to 48f,
            b.btnSizePreset64 to 64f,
            b.btnSizePreset96 to 96f,
            b.btnSizePreset160 to 160f
        )

        fun setActive(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else Color.TRANSPARENT)
            btn.setTextColor(if (active) Color.WHITE else COLOR_GRAY)
        }

        fun refreshPresets(textSize: Float) {
            for ((btn, v) in presets) setActive(btn, textSize == v)
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            syncing = true
            b.sliderFontSize.value = layer.textSize.coerceIn(8f, 600f)
            b.sliderScaleXY.value = layer.scale.coerceIn(0.1f, 8f)
            syncing = false
            b.tvSizeLabel.text = String.format(Locale.US, "Font size: %.0f px", layer.textSize)
            b.tvScaleLabel.text = "${(layer.scale * 100).toInt()}%"
            refreshPresets(layer.textSize)
        }
        syncSizeUIHook = { layer -> syncUI(layer) }

        b.sliderFontSize.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvSizeLabel.text = String.format(Locale.US, "Font size: %.0f px", value)
            refreshPresets(value)
            applyToTextLayer { it.textSize = value }
        }

        b.sliderScaleXY.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvScaleLabel.text = "${(value * 100).toInt()}%"
            applyToTextLayer { it.scale = value }
        }

        fun applyFontSize(value: Float) {
            syncing = true
            b.sliderFontSize.value = value.coerceIn(8f, 600f)
            syncing = false
            b.tvSizeLabel.text = String.format(Locale.US, "Size: %.0f px", value)
            refreshPresets(value)
            applyToTextLayer { it.textSize = value }
        }

        for ((btn, v) in presets) btn.setOnClickListener { applyFontSize(v) }

        b.btnSizeReset.setOnClickListener {
            applyToTextLayer {
                it.textSize = 64f
                it.scale = 1f
            }
            (pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer)?.let { syncUI(it) }
        }

        b.btnScaleReset.setOnClickListener {
            applyToTextLayer { it.scale = 1f }
            (pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer)?.let { syncUI(it) }
        }

        b.btnScaleFit.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            val (lw, lh) = layer.getUnwarpedDimensions()
            val s = kotlin.math.min(
                pixelCanvasView.width / lw,
                pixelCanvasView.height / lh * 0.9f
            )
            pixelCanvasView.runRecordedAction("Fit to Canvas") { layer.scale = s.coerceAtLeast(0.01f) }
            syncUI(layer)
            pixelCanvasView.invalidate()
        }
    }

    // ─── Panel: Opasitas ───────────────────────────────────────────────────

    private fun initializeOpacityControls() {
        val b = binding.effectSettingsInclude.opacityControlsInclude
        val panel = b.root
        val slider = b.sliderOpacity
        val tvLabel = b.tvOpacityLabel
        val btnReset = b.btnResetOpacity
        var syncing = false

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            val pct = Math.round(layer.opacity.coerceIn(0, 255) * 100f / 255f).toFloat()
            syncing = true
            slider.value = pct
            syncing = false
            tvLabel.text = "${pct.toInt()}%"
        }

        slider.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvLabel.text = "${value.toInt()}%"
            applyToTextLayer { it.opacity = (value * 255 / 100).toInt() }
        }

        btnReset.setOnClickListener {
            syncing = true
            slider.value = 100f
            syncing = false
            tvLabel.text = "100%"
            applyToTextLayer { it.opacity = 255 }
        }

        syncOpacityUIHook = { layer -> syncUI(layer) }
    }

    // ─── Panel: Rotasi ─────────────────────────────────────────────────────

    private fun initializeRotateControls() {
        val b = binding.effectSettingsInclude.rotateControlsInclude
        val panel = b.root
        val slider = b.sliderRotate
        val tvLabel = b.tvRotateLabel
        val btnReset = b.btnResetRotate
        var syncing = false
        val presets = listOf(
            b.btnRotateMinus90 to -90f,
            b.btnRotateReset to 0f,
            b.btnRotatePlus90 to 90f,
            b.btnRotate180 to 180f
        )

        fun normalize(v: Float): Float = ((v % 360f) + 360f) % 360f

        fun setActive(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else android.graphics.Color.TRANSPARENT)
            btn.setTextColor(if (active) android.graphics.Color.WHITE else COLOR_GRAY)
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            val r = normalize(layer.rotation)
            syncing = true
            slider.value = r
            syncing = false
            tvLabel.text = "${r.toInt()}°"
            for ((btn, pv) in presets) setActive(btn, pv == r)
        }

        slider.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            val r = normalize(value)
            tvLabel.text = "${r.toInt()}°"
            for ((btn, pv) in presets) setActive(btn, pv == r)
            applyToTextLayer { it.rotation = r }
        }

        fun applyPreset(value: Float) {
            val r = normalize(value)
            syncing = true
            slider.value = r
            syncing = false
            tvLabel.text = "${r.toInt()}°"
            for ((btn, pv) in presets) setActive(btn, pv == r)
            applyToTextLayer { it.rotation = r }
        }

        fun rotateBy(delta: Float) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Rotate Text") {
                layer.rotation = normalize(layer.rotation + delta)
            }
            syncUI(layer)
            pixelCanvasView.invalidate()
        }

        for ((btn, value) in presets) btn.setOnClickListener { applyPreset(value) }
        btnReset.setOnClickListener { applyPreset(0f) }

        syncRotateUIHook = { layer -> syncUI(layer) }
    }

    // ─── Panel: Gaya Teks (B / I / U / S + Font Weight) ────────────────────

    private fun initializeStyleControls() {
        val b = binding.effectSettingsInclude.styleControlsInclude
        val panel = b.root
        val boldBtn = b.btnStyleBold
        val italicBtn = b.btnStyleItalic
        val underlineBtn = b.btnStyleUnderline
        val strikeBtn = b.btnStyleStrike
        val btnReset = b.btnResetStyle

        fun setActive(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else android.graphics.Color.TRANSPARENT)
            val tint = if (active) android.graphics.Color.WHITE else COLOR_GRAY
            btn.setTextColor(tint)
            btn.iconTint = android.content.res.ColorStateList.valueOf(tint)
        }

        fun toggle(flag: String) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Change Text Style") {
                when (flag) {
                    "B" -> layer.isBold = !layer.isBold
                    "I" -> layer.isItalic = !layer.isItalic
                    "U" -> layer.isUnderline = !layer.isUnderline
                    "S" -> layer.isStrikethrough = !layer.isStrikethrough
                }
            }
            setActive(boldBtn, layer.isBold)
            setActive(italicBtn, layer.isItalic)
            setActive(underlineBtn, layer.isUnderline)
            setActive(strikeBtn, layer.isStrikethrough)
            pixelCanvasView.invalidate()
        }

        boldBtn.setOnClickListener { toggle("B") }
        italicBtn.setOnClickListener { toggle("I") }
        underlineBtn.setOnClickListener { toggle("U") }
        strikeBtn.setOnClickListener { toggle("S") }

        val weightButtons = listOf(
            b.btnWeightThin to 100,
            b.btnWeightLight to 300,
            b.btnWeightRegular to 400,
            b.btnWeightMedium to 500,
            b.btnWeightSemiBold to 600,
            b.btnWeightBold to 700,
            b.btnWeightExtraBold to 800,
            b.btnWeightBlack to 900
        )

        fun applyWeight(weight: Int) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Change Font Weight") {
                val base = layer.typeface ?: android.graphics.Typeface.DEFAULT
                layer.typeface = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.Typeface.create(base, weight, layer.isItalic)
                } else {
                    android.graphics.Typeface.create(base, if (weight >= 700) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                }
            }
            for ((btn, w) in weightButtons) setActive(btn, w == weight)
            pixelCanvasView.invalidate()
        }

        for ((btn, w) in weightButtons) btn.setOnClickListener { applyWeight(w) }

        fun reset() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) {
                pixelCanvasView.runRecordedAction("Reset Text Style") {
                    layer.isBold = false
                    layer.isItalic = false
                    layer.isUnderline = false
                    layer.isStrikethrough = false
                    val base = layer.typeface ?: android.graphics.Typeface.DEFAULT
                    layer.typeface = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        android.graphics.Typeface.create(base, 400, false)
                    } else {
                        android.graphics.Typeface.create(base, android.graphics.Typeface.NORMAL)
                    }
                }
            }
            setActive(boldBtn, false)
            setActive(italicBtn, false)
            setActive(underlineBtn, false)
            setActive(strikeBtn, false)
            for ((btn, w) in weightButtons) setActive(btn, w == 400)
            pixelCanvasView.invalidate()
        }

        btnReset.setOnClickListener { reset() }

        syncStyleUIHook = { layer ->
            panel.visibility = View.VISIBLE
            setActive(boldBtn, layer.isBold)
            setActive(italicBtn, layer.isItalic)
            setActive(underlineBtn, layer.isUnderline)
            setActive(strikeBtn, layer.isStrikethrough)
            val weight = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layer.typeface?.weight ?: 400
            } else {
                400
            }
            for ((btn, w) in weightButtons) setActive(btn, w == weight)
        }
    }

    // ─── Panel: Perataan Teks ──────────────────────────────────────────────

    private fun initializeAlignControls() {
        val b = binding.effectSettingsInclude.alignControlsInclude
        val panel = b.root

        fun setActive(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else android.graphics.Color.TRANSPARENT)
            btn.iconTint = android.content.res.ColorStateList.valueOf(
                if (active) android.graphics.Color.WHITE else COLOR_GRAY
            )
            btn.strokeWidth = if (active) 0 else 1
        }

        fun setActiveText(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else android.graphics.Color.TRANSPARENT)
            btn.setTextColor(if (active) android.graphics.Color.WHITE else COLOR_GRAY)
        }

        fun apply(align: android.text.Layout.Alignment, justify: Boolean) {
            applyToTextLayer { layer ->
                layer.alignment = align
                layer.justifyEnabled = justify
            }
        }

        b.btnAlignLeft.setOnClickListener {
            apply(android.text.Layout.Alignment.ALIGN_NORMAL, false)
            setActive(b.btnAlignLeft, true); setActive(b.btnAlignCenter, false)
            setActive(b.btnAlignRight, false); setActive(b.btnAlignJustify, false)
        }
        b.btnAlignCenter.setOnClickListener {
            apply(android.text.Layout.Alignment.ALIGN_CENTER, false)
            setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, true)
            setActive(b.btnAlignRight, false); setActive(b.btnAlignJustify, false)
        }
        b.btnAlignRight.setOnClickListener {
            apply(android.text.Layout.Alignment.ALIGN_OPPOSITE, false)
            setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, false)
            setActive(b.btnAlignRight, true); setActive(b.btnAlignJustify, false)
        }
        b.btnAlignJustify.setOnClickListener {
            apply(android.text.Layout.Alignment.ALIGN_NORMAL, true)
            setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, false)
            setActive(b.btnAlignRight, false); setActive(b.btnAlignJustify, true)
        }

        // ── Wrap Text: pembungkusan baris pada lebar tetap ───────────────────
        var syncingWrap = false

        fun syncWrap(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            syncingWrap = true
            val enabled = layer.wrapTextEnabled
            setActiveText(b.btnWrapText, enabled)
            b.wrapControlsGroup.visibility = if (enabled) View.VISIBLE else View.GONE
            if (layer.wrapWidth > 0f) {
                val v = layer.wrapWidth.coerceIn(60f, 1600f)
                b.sliderWrapWidth.value = v
                b.tvWrapWidth.text = "Width: ${v.toInt()} px"
            }
            syncingWrap = false
        }

        b.btnWrapText.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
                ?: return@setOnClickListener
            applyToTextLayer { target ->
                target.wrapTextEnabled = !target.wrapTextEnabled
                if (target.wrapTextEnabled && target.wrapWidth <= 0f) {
                    target.wrapWidth = target.measureNaturalWidth().coerceAtLeast(60f)
                }
            }
            syncWrap(layer)
        }

        b.sliderWrapWidth.addOnChangeListener { _, value, _ ->
            if (syncingWrap) return@addOnChangeListener
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
                ?: return@addOnChangeListener
            if (!layer.wrapTextEnabled) return@addOnChangeListener
            b.tvWrapWidth.text = "Width: ${value.toInt()} px"
            applyToTextLayer { it.wrapWidth = value }
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            val justify = layer.justifyEnabled
            val align = layer.alignment
            setActive(b.btnAlignJustify, justify)
            when {
                !justify && align == android.text.Layout.Alignment.ALIGN_NORMAL -> { setActive(b.btnAlignLeft, true); setActive(b.btnAlignCenter, false); setActive(b.btnAlignRight, false) }
                align == android.text.Layout.Alignment.ALIGN_CENTER -> { setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, true); setActive(b.btnAlignRight, false) }
                align == android.text.Layout.Alignment.ALIGN_OPPOSITE -> { setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, false); setActive(b.btnAlignRight, true) }
                else -> { setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, false); setActive(b.btnAlignRight, false) }
            }
            syncWrap(layer)
        }

        syncAlignUIHook = { layer -> syncUI(layer) }
    }

    // ─── Panel: Warna Teks ─────────────────────────────────────────────────

    private fun initializeColorControls() {
        val b = binding.effectSettingsInclude.colorControlsInclude
        val panel = b.root
        val density = activity.resources.displayMetrics.density
        val chipPreview = b.chipColorPreview
        val tvColorValue = b.tvColorValue

        val palette = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF9E9E9E.toInt(), 0xFFD32F2F.toInt(),
            0xFFF57C00.toInt(), 0xFFFBC02D.toInt(), 0xFF388E3C.toInt(), 0xFF0288D1.toInt(),
            0xFF1976D2.toInt(), 0xFF7B1FA2.toInt(), 0xFFC2185B.toInt(), 0xFF607D8B.toInt()
        )

        fun setupColorPreview(layer: com.flyerpix.editor.canvas.model.TextLayer?) {
            val color = layer?.textColor ?: Color.WHITE
            chipPreview.setCardBackgroundColor(color)
            tvColorValue.text = String.format(Locale.US, "#%08X", color)
        }

        fun setSolidColor(color: Int) {
            applyToTextLayer { layer ->
                layer.textColor = color
                layer.gradientEnabled = false
                layer.textureEnabled = false
            }
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) {
                setupColorPreview(layer)
                syncGradientUIHook?.invoke(layer)
            }
            pixelCanvasView.invalidate()
        }

        fun buildSwatches() {
            b.layoutColorSwatches.removeAllViews()
            for (c in palette) {
                val sw = View(activity).apply {
                    val s = (30 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(s, s).apply {
                        marginEnd = (8 * density).toInt()
                    }
                    setBackgroundResource(R.drawable.bg_color_swatch)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(c)
                    isClickable = true
                    setOnClickListener { setSolidColor(c) }
                }
                b.layoutColorSwatches.addView(sw)
            }
        }

        fun openColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.textColor,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.TEXT_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        // Terima hasil pemilihan Warna Teks dari ColorPickerDialog
        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.TEXT_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (!isGradient) {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        Color.WHITE
                    )
                    setSolidColor(color)
                }
            }

        chipPreview.setOnClickListener { openColorPicker() }
        b.btnPickColor.setOnClickListener { openColorPicker() }
        b.btnColorPicker.setOnClickListener { openColorPicker() }
        b.btnColorGradient.setOnClickListener { selectTextTool(TOOL_GRADIENT) }

        buildSwatches()

        // Keep Compose host reference to update content when selected layer changes
        val composeHost = panel.findViewById<ComposeView>(R.id.composeColorControls)
        colorComposeHost = composeHost

        syncColorUIHook = { layer ->
            panel.visibility = View.VISIBLE
            setupColorPreview(layer)
            // update Compose content to reflect current layer color
            composeHost?.setContent {
                ColorControls(
                    initialColor = layer?.textColor ?: Color.WHITE,
                    onColorSelected = { color -> setSolidColor(color) },
                    onOpenColorPicker = { openColorPicker() },
                    onOpenGradient = { selectTextTool(TOOL_GRADIENT) }
                )
            }
            composeHost?.visibility = View.VISIBLE
            panel.visibility = View.GONE
        }
        // Ensure ViewCompositionStrategy is set
        try {
            composeHost?.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        } catch (_: Exception) {}
    }

    // ─── Panel: Stroke / Outline ───────────────────────────────────────────

    private var lastStrokeWidth = 4f

    private fun initializeStrokeControls() {
        val b = binding.effectSettingsInclude.strokeControlsInclude
        val panel = b.root
        val switch = b.switchStrokeEnabled
        val group = b.strokeSliderGroup
        val sWidth = b.sliderStrokeWidth
        val sOpacity = b.sliderStrokeOpacity
        val tvWidth = b.tvStrokeWidthLabel
        val tvOpacity = b.tvStrokeOpacityLabel
        val chipPreview = b.chipStrokeColorPreview
        val tvColorValue = b.tvStrokeColorValue
        val btnPick = b.btnPickStrokeColor
        val btnReset = b.btnResetStroke
        var syncing = false

        fun setupColorPreview(layer: com.flyerpix.editor.canvas.model.TextLayer?) {
            val color = layer?.strokeColor ?: Color.BLACK
            chipPreview.setCardBackgroundColor(color)
            tvColorValue.text = String.format(Locale.US, "#%08X", color)
        }

        fun openColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.strokeColor,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.STROKE_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        fun applyStrokeChange(actionName: String, block: (com.flyerpix.editor.canvas.model.TextLayer) -> Unit) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer == null || layer.isLocked) {
                showSnackbar("Select a text layer first")
                return
            }
            val before = pixelCanvasView.captureCurrentState(actionName)
            block(layer)
            pixelCanvasView.invalidate()
            pixelCanvasView.recordAction(actionName, before)
            onCanvasChanged()
        }

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            syncing = true
            val enabled = layer.strokeWidth > 0f
            switch.isChecked = enabled
            group.visibility = if (enabled) View.VISIBLE else View.GONE
            sWidth.value = layer.strokeWidth.coerceIn(0f, 60f)
            val alpha = (layer.strokeColor ushr 24) and 0xFF
            sOpacity.value = alpha * 100f / 255f
            syncing = false
            setupColorPreview(layer)
            tvWidth.text = String.format(Locale.US, "Width: %.1f px", layer.strokeWidth)
            tvOpacity.text = "Opacity: ${(alpha * 100 / 255)}%"
            if (layer.strokeWidth > 0f) lastStrokeWidth = layer.strokeWidth
            // update Compose host if present
            try {
                strokeComposeHost?.setContent {
                    com.flyerpix.editor.ui.compose.StrokeControls(
                        initialEnabled = enabled,
                        initialWidth = layer.strokeWidth,
                        initialOpacityPct = alpha * 100f / 255f,
                        initialColor = layer.strokeColor,
                        onEnabledChanged = { v ->
                            if (v) applyStrokeChange("Enable Stroke") { tl -> tl.strokeWidth = lastStrokeWidth }
                            else applyStrokeChange("Disable Stroke") { tl -> lastStrokeWidth = tl.strokeWidth; tl.strokeWidth = 0f }
                        },
                        onWidthChanged = { v -> applyStrokeChange("Change Stroke Width") { it.strokeWidth = v } },
                        onOpacityChanged = { v -> applyStrokeChange("Change Stroke Opacity") { layer -> val a = (v * 255 / 100).toInt(); layer.strokeColor = (layer.strokeColor and 0x00FFFFFF) or (a shl 24) } },
                        onPickColor = { openColorPicker() },
                        onReset = { applyStrokeChange("Reset Stroke") { l -> l.strokeWidth = 0f; l.strokeColor = Color.BLACK } }
                    )
                }
                strokeComposeHost?.visibility = View.VISIBLE
                panel.visibility = View.GONE
            } catch (_: Exception) {}
        }

        syncStrokeUIHook = { layer -> sync(layer) }

        // Compose host setup
        try {
            val ch = panel.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeStrokeControls)
            strokeComposeHost = ch
            ch?.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            // initialize content with current selection
            val cur = (pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer)
            if (cur != null) {
                ch?.setContent {
                    com.flyerpix.editor.ui.compose.StrokeControls(
                        initialEnabled = cur.strokeWidth > 0f,
                        initialWidth = cur.strokeWidth,
                        initialOpacityPct = ((cur.strokeColor ushr 24) and 0xFF) * 100f / 255f,
                        initialColor = cur.strokeColor,
                        onEnabledChanged = { v -> if (v) applyStrokeChange("Enable Stroke") { it.strokeWidth = lastStrokeWidth } else applyStrokeChange("Disable Stroke") { lastStrokeWidth = it.strokeWidth; it.strokeWidth = 0f } },
                        onWidthChanged = { v -> applyStrokeChange("Change Stroke Width") { it.strokeWidth = v } },
                        onOpacityChanged = { v -> applyStrokeChange("Change Stroke Opacity") { layer -> val a = (v * 255 / 100).toInt(); layer.strokeColor = (layer.strokeColor and 0x00FFFFFF) or (a shl 24) } },
                        onPickColor = { openColorPicker() },
                        onReset = { applyStrokeChange("Reset Stroke") { l -> l.strokeWidth = 0f; l.strokeColor = Color.BLACK } }
                    )
                }
                ch?.visibility = View.VISIBLE
                panel.visibility = View.GONE
            }
        } catch (_: Exception) {}

        // Terima hasil pemilihan Stroke Color dari ColorPickerDialog
        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.STROKE_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (!isGradient) {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        Color.BLACK
                    )
                    applyStrokeChange("Change Stroke Color") { layer ->
                        val currentAlpha = (layer.strokeColor ushr 24) and 0xFF
                        layer.strokeColor = (color and 0x00FFFFFF) or (currentAlpha shl 24)
                    }
                    val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
                    if (layer != null) sync(layer)
                    pixelCanvasView.invalidate()
                }
            }

        chipPreview.setOnClickListener { openColorPicker() }
        btnPick.setOnClickListener { openColorPicker() }

        switch.setOnCheckedChangeListener { _, isChecked ->
            if (syncing) return@setOnCheckedChangeListener
            applyStrokeChange(if (isChecked) "Enable Stroke" else "Disable Stroke") { layer ->
                if (isChecked) {
                    if (layer.strokeWidth <= 0f) layer.strokeWidth = lastStrokeWidth
                } else {
                    lastStrokeWidth = layer.strokeWidth
                    layer.strokeWidth = 0f
                }
            }
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) sync(layer)
        }

        sWidth.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvWidth.text = String.format(Locale.US, "Width: %.1f px", value)
            lastStrokeWidth = value
            applyStrokeChange("Change Stroke Width") { it.strokeWidth = value }
        }

        sOpacity.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvOpacity.text = "Opacity: ${value.toInt()}%"
            applyStrokeChange("Change Stroke Opacity") { layer ->
                val a = (value * 255 / 100).toInt()
                layer.strokeColor = (layer.strokeColor and 0x00FFFFFF) or (a shl 24)
            }
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) setupColorPreview(layer)
        }

        btnReset.setOnClickListener {
            lastStrokeWidth = 4f
            syncing = true
            switch.isChecked = false
            sWidth.value = 0f
            sOpacity.value = 100f
            syncing = false
            group.visibility = View.GONE
            applyStrokeChange("Reset Stroke") { layer ->
                layer.strokeWidth = 0f
                layer.strokeColor = Color.BLACK
            }
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) sync(layer)
        }
    }

    // ─── Panel: Padding ────────────────────────────────────────────────────

    private fun initializePaddingControls() {
        val b = binding.effectSettingsInclude.paddingControlsInclude
        val panel = b.root
        var syncing = false

        fun applyAll(value: Float) {
            applyToTextLayer { it.paddingTop = value; it.paddingBottom = value; it.paddingLeft = value; it.paddingRight = value }
        }

        fun syncTexts(top: Float, bottom: Float, left: Float, right: Float) {
            b.tvPaddingTop.text = String.format(Locale.US, "Top: %.0f px", top)
            b.tvPaddingBottom.text = String.format(Locale.US, "Bottom: %.0f px", bottom)
            b.tvPaddingLeft.text = String.format(Locale.US, "Left: %.0f px", left)
            b.tvPaddingRight.text = String.format(Locale.US, "Right: %.0f px", right)
        }

        fun setLinked() {
            val v = b.sliderPaddingTop.value
            syncing = true
            b.sliderPaddingTop.value = v
            b.sliderPaddingBottom.value = v
            b.sliderPaddingLeft.value = v
            b.sliderPaddingRight.value = v
            syncing = false
            applyAll(v)
            syncTexts(v, v, v, v)
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            syncing = true
            b.sliderPaddingTop.value = layer.paddingTop.coerceIn(0f, 200f)
            b.sliderPaddingBottom.value = layer.paddingBottom.coerceIn(0f, 200f)
            b.sliderPaddingLeft.value = layer.paddingLeft.coerceIn(0f, 200f)
            b.sliderPaddingRight.value = layer.paddingRight.coerceIn(0f, 200f)
            syncing = false
            syncTexts(layer.paddingTop, layer.paddingBottom, layer.paddingLeft, layer.paddingRight)
        }

        b.sliderPaddingTop.addOnChangeListener { _, value, _ ->
            if (!syncing) {
                if (b.chkPaddingLinked.isChecked) {
                    setLinked()
                } else {
                    syncTexts(value, b.sliderPaddingBottom.value, b.sliderPaddingLeft.value, b.sliderPaddingRight.value)
                    applyToTextLayer { it.paddingTop = value }
                }
            }
        }
        b.sliderPaddingBottom.addOnChangeListener { _, value, _ ->
            if (!syncing) {
                if (b.chkPaddingLinked.isChecked) {
                    setLinked()
                } else {
                    syncTexts(b.sliderPaddingTop.value, value, b.sliderPaddingLeft.value, b.sliderPaddingRight.value)
                    applyToTextLayer { it.paddingBottom = value }
                }
            }
        }
        b.sliderPaddingLeft.addOnChangeListener { _, value, _ ->
            if (!syncing) {
                if (b.chkPaddingLinked.isChecked) {
                    setLinked()
                } else {
                    syncTexts(b.sliderPaddingTop.value, b.sliderPaddingBottom.value, value, b.sliderPaddingRight.value)
                    applyToTextLayer { it.paddingLeft = value }
                }
            }
        }
        b.sliderPaddingRight.addOnChangeListener { _, value, _ ->
            if (!syncing) {
                if (b.chkPaddingLinked.isChecked) {
                    setLinked()
                } else {
                    syncTexts(b.sliderPaddingTop.value, b.sliderPaddingBottom.value, b.sliderPaddingLeft.value, value)
                    applyToTextLayer { it.paddingRight = value }
                }
            }
        }

        b.btnResetPadding.setOnClickListener {
            syncing = true
            b.sliderPaddingTop.value = 0f
            b.sliderPaddingBottom.value = 0f
            b.sliderPaddingLeft.value = 0f
            b.sliderPaddingRight.value = 0f
            syncing = false
            applyAll(0f)
            syncTexts(0f, 0f, 0f, 0f)
        }

        syncPaddingUIHook = { layer -> syncUI(layer) }

        // Compose host setup for Padding controls
        try {
            val ch = panel.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composePaddingControls)
            paddingComposeHost = ch
            ch?.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val cur = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (cur != null) {
                ch?.setContent {
                    com.flyerpix.editor.ui.compose.PaddingControls(
                        linked = cur.paddingTop == cur.paddingBottom && cur.paddingLeft == cur.paddingRight && cur.paddingTop == cur.paddingLeft,
                        top = cur.paddingTop,
                        bottom = cur.paddingBottom,
                        left = cur.paddingLeft,
                        right = cur.paddingRight,
                        onLinkedChanged = { linked -> ch.post { b.chkPaddingLinked.isChecked = linked } },
                        onTopChanged = { v -> applyToTextLayer { it.paddingTop = v } },
                        onBottomChanged = { v -> applyToTextLayer { it.paddingBottom = v } },
                        onLeftChanged = { v -> applyToTextLayer { it.paddingLeft = v } },
                        onRightChanged = { v -> applyToTextLayer { it.paddingRight = v } },
                        onApplyAll = { v -> applyAll(v) },
                        onReset = { ch.post { b.btnResetPadding.performClick() } }
                    )
                }
                ch?.visibility = View.VISIBLE
                panel.visibility = View.GONE
            }
        } catch (_: Exception) {}
    }

    // ─── Panel: Background Teks ────────────────────────────────────────────

    private fun initializeBackgroundControls() {
        val b = binding.effectSettingsInclude.backgroundControlsInclude
        val panel = b.root
        val switch = b.switchBgEnabled
        val group = b.bgControlsGroup
        val sOpacity = b.sliderBgOpacity
        val sPadding = b.sliderBgPadding
        val sCorner = b.sliderBgCornerRadius
        val tvOpacity = b.tvBgOpacityLabel
        val tvPadding = b.tvBgPadding
        val tvCorner = b.tvBgCornerRadius
        val chipPreview = b.chipBgColorPreview
        val tvColorValue = b.tvBgColorValue
        val btnPick = b.btnPickBgColor
        val btnReset = b.btnResetBg
        var syncing = false

        fun setupColorPreview(layer: com.flyerpix.editor.canvas.model.TextLayer?) {
            val color = layer?.bgColor ?: Color.BLACK
            chipPreview.setCardBackgroundColor(color)
            tvColorValue.text = String.format(Locale.US, "#%08X", color)
        }

        fun openColorPicker() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            com.flyerpix.editor.ui.dialog.ColorPickerDialog
                .newInstance(
                    initialColor = layer.bgColor,
                    resultKey = com.flyerpix.editor.ui.dialog.ColorPickerDialog.BG_RESULT_KEY
                )
                .show(
                    (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG
                )
        }

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            syncing = true
            switch.isChecked = layer.bgEnabled
            group.visibility = if (layer.bgEnabled) View.VISIBLE else View.GONE
            sOpacity.value = (layer.bgOpacity * 100f).coerceIn(0f, 100f)
            sPadding.value = layer.bgPadding.coerceIn(0f, 100f)
            sCorner.value = layer.bgCornerRadius.coerceIn(0f, 120f)
            syncing = false
            setupColorPreview(layer)
            tvOpacity.text = "Opacity: ${(layer.bgOpacity * 100).toInt()}%"
            tvPadding.text = String.format(Locale.US, "Padding: %.0f px", layer.bgPadding)
            tvCorner.text = String.format(Locale.US, "Corner Radius: %.0f px", layer.bgCornerRadius)
        }

        syncBackgroundUIHook = { layer -> sync(layer) }

        // Terima hasil pemilihan Background Color dari ColorPickerDialog
        (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
            .setFragmentResultListener(
                com.flyerpix.editor.ui.dialog.ColorPickerDialog.BG_RESULT_KEY,
                activity
            ) { _, bundle ->
                val isGradient = bundle.getBoolean(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_IS_GRADIENT, false
                )
                if (!isGradient) {
                    val color = bundle.getInt(
                        com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                        Color.BLACK
                    )
                    applyToTextLayer { layer ->
                        val currentAlpha = (layer.bgColor ushr 24) and 0xFF
                        layer.bgColor = (color and 0x00FFFFFF) or (currentAlpha shl 24)
                    }
                    val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
                    if (layer != null) sync(layer)
                    pixelCanvasView.invalidate()
                }
            }

        chipPreview.setOnClickListener { openColorPicker() }
        btnPick.setOnClickListener { openColorPicker() }

        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.bgEnabled = isChecked }
        }

        sOpacity.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvOpacity.text = "Opacity: ${value.toInt()}%"
            applyToTextLayer { it.bgOpacity = value / 100f }
        }

        sPadding.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvPadding.text = String.format(Locale.US, "Padding: %.0f px", value)
            applyToTextLayer { it.bgPadding = value }
        }

        sCorner.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvCorner.text = String.format(Locale.US, "Corner Radius: %.0f px", value)
            applyToTextLayer { it.bgCornerRadius = value }
        }

        btnReset.setOnClickListener {
            switch.isChecked = false
            group.visibility = View.GONE
            sOpacity.value = 100f
            sPadding.value = 0f
            sCorner.value = 0f
            tvOpacity.text = "Opacity: 100%"
            tvPadding.text = "Padding: 0 px"
            tvCorner.text = "Corner Radius: 0 px"
            applyToTextLayer { layer ->
                layer.bgEnabled = false
                layer.bgOpacity = 1f
                layer.bgPadding = 0f
                layer.bgCornerRadius = 0f
                layer.bgColor = Color.BLACK
            }
            setupColorPreview(pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer)
        }
    }

    // ─── Efek: Reflection (Pantulan Teks) ──────────────────────────────────

    private fun initializeReflectionControls() {
        val b = binding.effectSettingsInclude.reflectionControlsInclude
        val panel = b.root
        val switch = b.switchReflectionEnabled
        val group = b.reflectionSliderGroup
        val sOpacity = b.sliderReflectionOpacity
        val sDistance = b.sliderReflectionDistance
        val sFade = b.sliderReflectionFade
        val tvOpacity = b.tvReflectionOpacity
        val tvDistance = b.tvReflectionDistance
        val tvFade = b.tvReflectionFade
        val btnReset = b.btnResetReflection

        fun updateLabels(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            tvOpacity.text = "Opacity: ${(layer.reflectionOpacity * 100).toInt()}%"
            tvDistance.text = String.format(Locale.US, "Distance: %.0f px", layer.reflectionDistance)
            tvFade.text = "Fade: ${(layer.reflectionFade * 100).toInt()}%"
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.reflectionEnabled
            group.visibility = if (layer.reflectionEnabled) View.VISIBLE else View.GONE
            sOpacity.value = (layer.reflectionOpacity * 100f).coerceIn(0f, 100f)
            sDistance.value = layer.reflectionDistance.coerceIn(0f, 200f)
            sFade.value = (layer.reflectionFade * 100f).coerceIn(0f, 100f)
            updateLabels(layer)
        }
        syncReflectionUIHook = { layer -> syncUI(layer) }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.reflectionEnabled = isChecked }
        }

        sOpacity.addOnChangeListener { _, value, _ ->
            tvOpacity.text = "Opacity: ${value.toInt()}%"
            applyToTextLayer { it.reflectionOpacity = value / 100f }
        }

        sDistance.addOnChangeListener { _, value, _ ->
            tvDistance.text = String.format(Locale.US, "Distance: %.0f px", value)
            applyToTextLayer { it.reflectionDistance = value }
        }

        sFade.addOnChangeListener { _, value, _ ->
            tvFade.text = "Fade: ${value.toInt()}%"
            applyToTextLayer { it.reflectionFade = value / 100f }
        }

        btnReset.setOnClickListener {
            pixelCanvasView.selectedLayer?.let { layer ->
                if (layer is com.flyerpix.editor.canvas.model.TextLayer && !layer.isLocked) {
                    layer.reflectionOpacity = 0.4f
                    layer.reflectionDistance = 10f
                    layer.reflectionFade = 0.5f
                    layer.reflectionEnabled = true
                    syncUI(layer)
                    pixelCanvasView.invalidate()
                }
            }
        }
    }

    // ─── Panel: Masking (Foto di Dalam Teks) ───────────────────────────────

    private fun initializeMaskControls() {
        val b = binding.effectSettingsInclude.maskControlsInclude
        val panel = b.root
        val switch = b.switchMaskEnabled
        val group = b.maskControlsGroup
        val imgThumb = b.imgMaskThumbnail
        val btnSelect = b.btnSelectMask
        val btnDelete = b.btnDeleteMask
        val sScale = b.sliderMaskScale
        val tvScale = b.tvMaskScaleLabel
        val sRotation = b.sliderMaskRotation
        val tvRotation = b.tvMaskRotationLabel
        val btnReset = b.btnResetMask
        var syncing = false

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            syncing = true
            switch.isChecked = layer.textureEnabled
            group.visibility = if (layer.textureEnabled) View.VISIBLE else View.GONE

            if (layer.textureBitmap != null && !layer.textureBitmap!!.isRecycled) {
                imgThumb.setImageBitmap(layer.textureBitmap)
                btnSelect.text = "Change Photo"
                btnDelete.visibility = View.VISIBLE
            } else {
                imgThumb.setImageResource(R.drawable.ic_sharp_photo_24px)
                btnSelect.text = "Choose from Gallery"
                btnDelete.visibility = View.GONE
            }

            sScale.value = layer.textureScale.coerceIn(0.1f, 3.0f)
            tvScale.text = "Scale: ${(layer.textureScale * 100).toInt()}%"
            sRotation.value = layer.textureRotation.coerceIn(0f, 360f)
            tvRotation.text = "Rotation: ${layer.textureRotation.toInt()}°"
            syncing = false
        }

        // Sinkronkan kontrol Tekstur agar tetap selaras (berbagi field yang sama).
        fun syncTexturePage() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            binding.effectSettingsInclude.textureControlsInclude.run {
                if (layer.textureBitmap != null && !layer.textureBitmap!!.isRecycled) {
                    imgTextureThumbnail.setImageBitmap(layer.textureBitmap)
                    btnSelectTexture.text = "Change Photo"
                } else {
                    imgTextureThumbnail.setImageResource(R.drawable.ic_sharp_photo_24px)
                    btnSelectTexture.text = "Choose from Gallery"
                }
                btnDeleteTexture.visibility = if (layer.textureEnabled && layer.textureBitmap != null) View.VISIBLE else View.GONE
                switchTextureEnabled.isChecked = layer.textureEnabled
                textureControlsGroup.visibility = if (layer.textureEnabled) View.VISIBLE else View.GONE
            }
        }

        syncMaskUIHook = { layer -> syncUI(layer) }

        // Toggle Switch Aktif / Nonaktif
        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { layer ->
                layer.textureEnabled = isChecked
                if (isChecked && layer.textureBitmap == null) {
                    texturePickerLauncher?.launch("image/*")
                }
            }
            syncTexturePage()
        }

        // Tombol Pilih Foto dari Galeri
        btnSelect.setOnClickListener {
            texturePickerLauncher?.launch("image/*")
        }

        // Tombol Hapus Mask
        btnDelete.setOnClickListener {
            applyToTextLayer { layer ->
                layer.textureBitmap = null
                layer.textureEnabled = false
            }
            pixelCanvasView.invalidate()
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) syncUI(layer)
            syncTexturePage()
            showSnackbar("Text mask removed")
        }

        // Slider Skala Mask
        sScale.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvScale.text = "Scale: ${(value * 100).toInt()}%"
            applyToTextLayer { it.textureScale = value }
        }

        // Slider Rotasi Mask
        sRotation.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            tvRotation.text = "Rotation: ${value.toInt()}°"
            applyToTextLayer { it.textureRotation = value }
        }

        // Reset Mask
        btnReset.setOnClickListener {
            applyToTextLayer { layer ->
                layer.textureBitmap = null
                layer.textureEnabled = false
                layer.textureScale = 1.0f
                layer.textureRotation = 0f
            }
            pixelCanvasView.invalidate()
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) syncUI(layer)
            syncTexturePage()
            showSnackbar("Mask reset")
        }
    }

    // ─── Panel: Styles Teks (Simpan / Terapkan) ────────────────────────────

    private fun initializeStylesControls() {
        val b = binding.effectSettingsInclude.stylesControlsInclude
        val panel = b.root

        savedTextStyles.clear()
        savedTextStyles.putAll(TextStyleStorage.load(activity))

        fun rebuildChips() {
            b.layoutStyleChips.removeAllViews()
            val density = activity.resources.displayMetrics.density
            for ((name, style) in savedTextStyles) {
                val chip = MaterialButton(activity)
                chip.text = name
                chip.textSize = 12f
                chip.minimumWidth = 0
                chip.isAllCaps = false
                chip.setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
                chip.setTextColor(style.textColor)
                chip.setBackgroundColor(Color.TRANSPARENT)
                chip.strokeWidth = 1
                chip.strokeColor = android.content.res.ColorStateList.valueOf(COLOR_GRAY)
                val preview = GradientDrawable()
                preview.shape = GradientDrawable.OVAL
                preview.setColor(style.textColor)
                chip.icon = preview
                chip.iconSize = (12 * density).toInt()
                chip.iconPadding = (6 * density).toInt()
                chip.setOnClickListener {
                    val target = pixelCanvasView.selectedLayer as? TextLayer
                    if (target == null || target.isLocked) {
                        showSnackbar("Select a text layer first")
                        return@setOnClickListener
                    }
                    pixelCanvasView.runRecordedAction("Apply Style") {
                        style.applyTo(target)
                    }
                    pixelCanvasView.invalidate()
                    showSnackbar("Style '$name' applied")
                }
                chip.setOnLongClickListener {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Delete Style")
                        .setMessage("Delete style '$name'?")
                        .setPositiveButton("Delete") { _, _ ->
                            savedTextStyles.remove(name)
                            TextStyleStorage.save(activity, savedTextStyles)
                            rebuildChips()
                            showSnackbar("Style '$name' deleted")
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (36 * density).toInt()
                )
                params.marginEnd = (6 * density).toInt()
                b.layoutStyleChips.addView(chip, params)
            }
        }

        b.btnStyleSave.setOnClickListener {
            val name = b.editStyleName.text?.toString()?.trim()
            val layer = pixelCanvasView.selectedLayer as? TextLayer
            if (name.isNullOrEmpty()) {
                showSnackbar("Enter a style name first")
                return@setOnClickListener
            }
            if (layer == null || layer.isLocked) {
                showSnackbar("Select a text layer first")
                return@setOnClickListener
            }
            savedTextStyles[name] = SavedTextStyle.fromLayer(layer)
            TextStyleStorage.save(activity, savedTextStyles)
            b.editStyleName.setText("")
            rebuildChips()
            hideKeyboard(b.editStyleName)
            showSnackbar("Style '$name' saved")
        }

        b.btnStylesClearAll.setOnClickListener {
            if (savedTextStyles.isEmpty()) {
                showSnackbar("No saved styles yet")
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(activity)
                .setTitle("Delete All Styles")
                .setMessage("All saved styles will be permanently deleted.")
                .setPositiveButton("Delete") { _, _ ->
                    savedTextStyles.clear()
                    TextStyleStorage.save(activity, savedTextStyles)
                    rebuildChips()
                    showSnackbar("All styles deleted")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        syncStylesUIHook = { layer ->
            panel.visibility = View.VISIBLE
        }

        rebuildChips()
    }

    private fun hideKeyboard(view: View) {
        val imm = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    // ── Bridges untuk Activity ──────────────────────────────────────────────

    fun refreshUI() {
        refreshTextPageUI()
    }

    fun hideStripAndPanels() {
        val wasOpen = effectSettingsOpen
        binding.textPropertyPanelInclude.root.visibility = View.GONE
        binding.textCategoryStripInclude.root.visibility = View.GONE
        binding.textToolStripInclude.textToolStripScroll.visibility = View.GONE
        binding.effectSettingsInclude.root.visibility = View.GONE
        activeTextToolTag = ""
        effectSettingsOpen = false
        textToolTagBeforeEffect = ""
        settingsSnapshot = null
        for (v in textPanelViews.values) v.visibility = View.GONE
        if (wasOpen) onEffectSettingsOpenChanged(false)
    }
}
