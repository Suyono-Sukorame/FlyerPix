package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.result.ActivityResultLauncher
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
import java.util.Locale
import kotlin.math.max

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
    private val onCanvasChanged: () -> Unit
) {

    private lateinit var gradientPickerAdapter: GradientPickerAdapter
    private var texturePickerLauncher: ActivityResultLauncher<String>? = null

    // ── Text Page State─────────────────────────────────────────────────────
    private val textToolItems = LinkedHashMap<String, ViewGroup>()
    private val textPanelViews = LinkedHashMap<String, View>()
    private val savedTextStyles = LinkedHashMap<String, TextLayer>()
    private var activeTextToolTag: String = ""
    private var maxPropertyPanelScrollH = 0
    var isPageOpen = false
    var pagePinnedByNav = false
    private val textToolLabels = HashMap<String, String>()
    
    // Flag untuk mencegah auto-switch ke menu Text saat initialization
    private var isInitializing = true

    private data class TextToolSpec(val tag: String, val label: String, val iconRes: Int)

    companion object {
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

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY      = 0xFF616161.toInt()
    }

    /**
     * Inisialisasi semua panel kontrol text editor.
     * Harus dipanggil setelah binding dan pixelCanvasView siap.
     */
    fun initialize() {
        buildTextToolStrip()
        registerTextPanels()

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
        pixelCanvasView.onLayerSelectedListener = { layer ->
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

        initializePositionPanel()
        initializeRelativePositionPanel()
        initializeSizePanel()
        initializeOpacityPanel()
        initializeRotatePanel()
        initializeStylePanel()
        initializeAlignPanel()
        initializeColorPanel()
        initializeStrokePanel()
        initializePaddingPanel()
        initializeBackgroundPanel()
        initializeReflectionPanel()
        initializeMaskPanel()
        initializeTextStylesPanel()

        initializeShadowControls()
        initializeInnerShadowControls()
        initializeEmbossControls()
        initializeGradientControls()
        initializeTextureControls()
        initializeExtrudeControls()
        initializeRotate3DControls()
        initializeCurveControls()
        initializePerspectiveControls()
        initializeSpacingControls()
        initializeBlendModeControls()
        
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
     * Menginisialisasi panel kontrol Drop Shadow.
     * Panel hanya muncul jika layer teks sedang dipilih.
     * Semua slider beroperasi secara real-time tanpa perlu tombol Terapkan.
     */
    private fun initializeShadowControls() {
        val b = binding.textPropertyPanelInclude.shadowControlsInclude
        val panel = b.root
        val switch = b.switchShadowEnabled
        val group = b.shadowSliderGroup
        val sRadius = b.sliderShadowRadius
        val sOpacity = b.sliderShadowOpacity
        val sDx = b.sliderShadowDx
        val sDy = b.sliderShadowDy

        // Tampilkan panel shadow hanya saat layer teks aktif dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                panel.visibility = View.VISIBLE
                // Sinkronisasi state awal slider dengan nilai layer
                switch.isChecked = layer.shadowEnabled
                group.visibility = if (layer.shadowEnabled) View.VISIBLE else View.GONE
                sRadius.value = layer.shadowRadius.coerceIn(0f, 40f)
                sOpacity.value = layer.shadowOpacity.coerceIn(0f, 1f)
                sDx.value = layer.shadowDx.coerceIn(-30f, 30f)
                sDy.value = layer.shadowDy.coerceIn(-30f, 30f)
            } else {
                panel.visibility = View.GONE
            }
        }

        // Toggle enable/disable → tampilkan/sembunyikan slider group
        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.shadowEnabled = isChecked }
        }

        // Real-time slider listener
        sRadius.addOnChangeListener { _, value, _ ->
            applyToTextLayer { it.shadowRadius = value }
        }
        sOpacity.addOnChangeListener { _, value, _ ->
            applyToTextLayer { it.shadowOpacity = value }
        }
        sDx.addOnChangeListener { _, value, _ ->
            applyToTextLayer { it.shadowDx = value }
        }
        sDy.addOnChangeListener { _, value, _ ->
            applyToTextLayer { it.shadowDy = value }
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
        val b = binding.textPropertyPanelInclude.innerShadowControlsInclude
        val panel = b.root
        val switch = b.switchInnerShadowEnabled
        val group = b.innerShadowSliderGroup
        val sRadius = b.sliderInnerShadowRadius
        val sOpacity = b.sliderInnerShadowOpacity
        val sDx = b.sliderInnerShadowDx
        val sDy = b.sliderInnerShadowDy

        // Perbarui state panel setiap kali layer teks baru dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                panel.visibility = View.VISIBLE
                switch.isChecked = layer.innerShadowEnabled
                group.visibility = if (layer.innerShadowEnabled) View.VISIBLE else View.GONE
                sRadius.value = layer.innerShadowRadius.coerceIn(0f, 40f)
                sOpacity.value = layer.innerShadowOpacity.coerceIn(0f, 1f)
                sDx.value = layer.innerShadowDx.coerceIn(-30f, 30f)
                sDy.value = layer.innerShadowDy.coerceIn(-30f, 30f)
            } else {
                panel.visibility = View.GONE
            }
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.innerShadowEnabled = isChecked }
        }
        sRadius.addOnChangeListener { _, v, _ -> applyToTextLayer { it.innerShadowRadius = v } }
        sOpacity.addOnChangeListener { _, v, _ -> applyToTextLayer { it.innerShadowOpacity = v } }
        sDx.addOnChangeListener { _, v, _ -> applyToTextLayer { it.innerShadowDx = v } }
        sDy.addOnChangeListener { _, v, _ -> applyToTextLayer { it.innerShadowDy = v } }
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
        val b = binding.textPropertyPanelInclude.embossControlsInclude
        val panel = b.root
        val switch = b.switchEmbossEnabled
        val group = b.embossSliderGroup
        val sAngle = b.sliderEmbossAngle
        val sAmbient = b.sliderEmbossAmbient
        val sSpecular = b.sliderEmbossSpecular
        val sBlur = b.sliderEmbossBlurRadius

        // Perbarui state panel setiap kali layer teks baru dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                panel.visibility = View.VISIBLE
                switch.isChecked = layer.embossEnabled
                group.visibility = if (layer.embossEnabled) View.VISIBLE else View.GONE
                sAngle.value = layer.embossLightAngle.coerceIn(0f, 360f)
                sAmbient.value = layer.embossAmbient.coerceIn(0f, 1f)
                sSpecular.value = layer.embossSpecular.coerceIn(0.1f, 20f)
                sBlur.value = layer.embossBlurRadius.coerceIn(0.5f, 10f)
            } else {
                panel.visibility = View.GONE
            }
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.embossEnabled = isChecked }
        }
        sAngle.addOnChangeListener { _, v, _ -> applyToTextLayer { it.embossLightAngle = v } }
        sAmbient.addOnChangeListener { _, v, _ -> applyToTextLayer { it.embossAmbient = v } }
        sSpecular.addOnChangeListener { _, v, _ -> applyToTextLayer { it.embossSpecular = v } }
        sBlur.addOnChangeListener { _, v, _ -> applyToTextLayer { it.embossBlurRadius = v } }
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
        val b = binding.textPropertyPanelInclude.gradientControlsInclude
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

        // Sinkronisasi state saat layer teks dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                panel.visibility = View.VISIBLE
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
                    tvAngleLabel.text = "Gradient Angle (${grad.angle.toInt()}°)"
                    gradientPickerAdapter.setSelectedPreset(grad)
                } else {
                    rbLinear.isChecked = true
                    angleContainer.visibility = View.VISIBLE
                    sAngle.value = 0f
                    tvAngleLabel.text = "Gradient Angle (0°)"
                    gradientPickerAdapter.setSelectedPreset(null)
                }
            } else {
                panel.visibility = View.GONE
            }
        }

        // Toggle Switch Enable / Disable
        switch.setOnCheckedChangeListener { _, isChecked ->
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
            tvAngleLabel.text = "Gradient Angle (${value.toInt()}°)"
            applyToTextLayer { layer ->
                layer.gradient?.angle = value
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
        val b = binding.textPropertyPanelInclude.textureControlsInclude
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

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.textureEnabled
            group.visibility = if (layer.textureEnabled) View.VISIBLE else View.GONE

            if (layer.textureBitmap != null && !layer.textureBitmap!!.isRecycled) {
                imgThumb.setImageBitmap(layer.textureBitmap)
                btnSelect.text = "Ganti Foto"
                btnDelete.visibility = View.VISIBLE
            } else {
                imgThumb.setImageResource(R.drawable.ic_sharp_photo_24px)
                btnSelect.text = "Pilih dari Galeri"
                btnDelete.visibility = View.GONE
            }

            sScale.value = layer.textureScale.coerceIn(0.1f, 3.0f)
            tvScale.text = "Scale Texture (${(layer.textureScale * 100).toInt()}%)"
            sRotation.value = layer.textureRotation.coerceIn(0f, 360f)
            tvRotation.text = "Rotate Texture (${layer.textureRotation.toInt()}°)"
        }

        // Sinkronisasi saat layer teks dipilih
        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is TextLayer) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

        // Toggle Switch Enable / Disable
        switch.setOnCheckedChangeListener { _, isChecked ->
            group.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { layer ->
                layer.textureEnabled = isChecked
                if (isChecked && layer.textureBitmap == null) {
                    texturePickerLauncher?.launch("image/*")
                }
            }
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
        }

        // Slider Skala Tekstur
        sScale.addOnChangeListener { _, value, _ ->
            tvScale.text = "Scale Texture (${(value * 100).toInt()}%)"
            applyToTextLayer { layer ->
                layer.textureScale = value
            }
        }

        // Slider Rotasi Tekstur
        sRotation.addOnChangeListener { _, value, _ ->
            tvRotation.text = "Rotate Texture (${value.toInt()}°)"
            applyToTextLayer { layer ->
                layer.textureRotation = value
            }
        }
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
        val b = binding.textPropertyPanelInclude.extrudeControlsInclude
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
        val llPalette = b.llDepthColorPalette

        val depthColors = intArrayOf(
            0xFF222222.toInt(), 0xFF444444.toInt(), 0xFF000000.toInt(),
            0xFF1A237E.toInt(), 0xFF880E4F.toInt(), 0xFF3E2723.toInt(),
            0xFF1B5E20.toInt(), 0xFFE65100.toInt(), 0xFF4A148C.toInt(),
            0xFFB0BEC5.toInt()
        )

        fun setupColorPalette(currentLayer: TextLayer?) {
            llPalette.removeAllViews()
            val curColor = currentLayer?.extrudeColor ?: depthColors[0]
            val dp36 = (36 * activity.resources.displayMetrics.density).toInt()
            val margin = (4 * activity.resources.displayMetrics.density).toInt()

            for (color in depthColors) {
                val card = com.google.android.material.card.MaterialCardView(activity).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(dp36, dp36).apply {
                        setMargins(margin, margin, margin, margin)
                    }
                    radius = dp36 / 2f
                    setCardBackgroundColor(color)
                    strokeWidth = if (color == curColor) 6 else 2
                    strokeColor = if (color == curColor) Color.WHITE else 0x44FFFFFF.toInt()
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        applyToTextLayer { layer ->
                            layer.extrudeColor = color
                        }
                        setupColorPalette(pixelCanvasView.selectedLayer as? TextLayer)
                    }
                }
                llPalette.addView(card)
            }
        }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.extrudeEnabled
            group.visibility = if (layer.extrudeEnabled) View.VISIBLE else View.GONE

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
            tvAngle.text = "Depth Angle (${layer.extrudeAngle.toInt()}°)"

            setupColorPalette(layer)
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
            tvAngle.text = "Depth Angle (${value.toInt()}°)"
            applyToTextLayer { layer ->
                layer.extrudeAngle = value
            }
        }

        setupColorPalette(null)
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3D ROTATE CONTROLS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol 3D Rotate (Rotasi Sumbu X dan Y).
     * Menggunakan android.graphics.Camera untuk transformasi perspektif 3D dinamis.
     */
    private fun initializeRotate3DControls() {
        val b = binding.textPropertyPanelInclude.rotate3DControlsInclude
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
        val b = binding.textPropertyPanelInclude.curveControlsInclude
        val panel = b.root
        val tvLabel = b.tvCurveLabel
        val slider = b.sliderCurve
        val btnReset = b.btnResetCurve
        val btnDown100 = b.btnCurveDown100
        val btnDown50 = b.btnCurveDown50
        val btnFlat = b.btnCurveFlat
        val btnUp50 = b.btnCurveUp50
        val btnUp100 = b.btnCurveUp100

        fun curveLabel(v: Int): String = when {
            v == 0   -> "Curve: 0% (Flat)"
            v > 0    -> "Curve: +$v% ▲"
            else     -> "Curve: $v% ▼"
        }

        fun applyPreset(value: Int) {
            slider.value = value.coerceIn(-100, 100).toFloat()
            tvLabel.text = curveLabel(value)
            applyToTextLayer { layer -> layer.curvePercent = value }
        }

        fun syncUI(layer: TextLayer) {
            panel.visibility = View.VISIBLE
            val v = layer.curvePercent.coerceIn(-100, 100)
            slider.value = v.toFloat()
            tvLabel.text = curveLabel(v)
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

        // Slider perubahan nilai
        slider.addOnChangeListener { _, value, _ ->
            val v = value.toInt()
            tvLabel.text = curveLabel(v)
            applyToTextLayer { layer -> layer.curvePercent = v }
        }

        // Tombol preset
        btnDown100.setOnClickListener { applyPreset(-100) }
        btnDown50.setOnClickListener  { applyPreset(-50) }
        btnFlat.setOnClickListener    { applyPreset(0) }
        btnUp50.setOnClickListener    { applyPreset(50) }
        btnUp100.setOnClickListener   { applyPreset(100) }

        // Reset ke flat
        btnReset.setOnClickListener { applyPreset(0) }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PERSPECTIVE CONTROLS (Placeholder)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menginisialisasi panel kontrol Transformasi Perspektif Warping.
     * TODO: Implement perspective controls jika ada di UI
     */
    private fun initializePerspectiveControls() {
        val b = binding.textPropertyPanelInclude.perspectiveControlsInclude
        val panel = b.root
        val switch = b.switchPerspectiveEnabled
        val group = b.perspectiveControlsGroup
        val btnReset = b.btnResetPerspective
        val btnFlat = b.btnPresetFlat
        val btnLeft = b.btnPresetLeftWall
        val btnRight = b.btnPresetRightWall
        val btnBillboard = b.btnPresetBillboard
        val btnFloor = b.btnPresetFloor

        fun syncUI(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
            panel.visibility = View.VISIBLE
            switch.isChecked = layer.perspectiveEnabled
            group.visibility = if (layer.perspectiveEnabled) View.VISIBLE else View.GONE
        }

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
                    pixelCanvasView.invalidate()
                }
            }
        }

        btnFlat.setOnClickListener { applyPreset(com.flyerpix.editor.canvas.model.PerspectivePreset.FLAT) }
        btnLeft.setOnClickListener { applyPreset(com.flyerpix.editor.canvas.model.PerspectivePreset.LEFT_WALL) }
        btnRight.setOnClickListener { applyPreset(com.flyerpix.editor.canvas.model.PerspectivePreset.RIGHT_WALL) }
        btnBillboard.setOnClickListener { applyPreset(com.flyerpix.editor.canvas.model.PerspectivePreset.TOP_BILLBOARD) }
        btnFloor.setOnClickListener { applyPreset(com.flyerpix.editor.canvas.model.PerspectivePreset.FLOOR_TILT) }

        btnReset.setOnClickListener {
            pixelCanvasView.selectedLayer?.let { layer ->
                if (!layer.isLocked) {
                    layer.resetPerspective()
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
        val b = binding.textPropertyPanelInclude.spacingControlsInclude
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

        fun formatLetter(v: Float) = String.format(Locale.US, "Spasi Huruf: %.2f", v)
        fun formatLine(v: Float) = String.format(Locale.US, "Spasi Baris: %.0f px", v)

        fun syncUI(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            panel.visibility = View.VISIBLE
            val letterVal = layer.letterSpacing.coerceIn(-0.2f, 1.0f)
            val lineVal = layer.lineSpacing.coerceIn(-20f, 80f)
            sLetter.value = (Math.round(letterVal * 50f) / 50f).coerceIn(-0.2f, 1.0f)
            sLine.value = (Math.round(lineVal / 2f) * 2f).coerceIn(-20f, 80f)
            tvLetterLabel.text = formatLetter(letterVal)
            tvLineLabel.text = formatLine(lineVal)
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) {
                syncUI(layer)
            } else {
                panel.visibility = View.GONE
            }
        }

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
        val b = binding.textPropertyPanelInclude.blendControlsInclude
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
            android.graphics.PorterDuff.Mode.SRC_OVER -> "Normal: Menampilkan warna layer standar menutupi background."
            android.graphics.PorterDuff.Mode.MULTIPLY -> "Multiply: Mengalikan warna (membuat teks lebih gelap dan menyatu)."
            android.graphics.PorterDuff.Mode.SCREEN   -> "Screen: Membalikkan dan mengalikan (efek teks bersinar terang)."
            android.graphics.PorterDuff.Mode.OVERLAY  -> "Overlay: Kombinasi Multiply dan Screen berdasarkan background."
            android.graphics.PorterDuff.Mode.DARKEN   -> "Darken: Memilih piksel yang lebih gelap antara teks dan background."
            android.graphics.PorterDuff.Mode.LIGHTEN  -> "Lighten: Memilih piksel yang lebih terang antara teks dan background."
            android.graphics.PorterDuff.Mode.ADD      -> "Add: Menjumlahkan warna teks dan background (efek cahaya kuat)."
            else                                      -> "Mode: ${mode.name}"
        }

        fun updateButtonStates(currentMode: android.graphics.PorterDuff.Mode) {
            tvDesc.text = descriptionFor(currentMode)
            val activeColor = 0xFF1769FF.toInt()
            for ((btn, mode) in buttons) {
                if (mode == currentMode) {
                    btn.setBackgroundColor(activeColor)
                    btn.setTextColor(Color.WHITE)
                    btn.strokeWidth = 3
                } else {
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(Color.LTGRAY)
                    btn.strokeWidth = 1
                }
            }
        }

        fun syncUI(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
            panel.visibility = View.VISIBLE
            updateButtonStates(layer.blendMode)
        }

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
                    updateButtonStates(mode)
                    pixelCanvasView.invalidate()
                }
            }
        }

        for ((btn, mode) in buttons) {
            btn.setOnClickListener { setBlendMode(mode) }
        }

        btnReset.setOnClickListener { setBlendMode(android.graphics.PorterDuff.Mode.SRC_OVER) }
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
     * Decode Bitmap secara aman dari Uri galeri HP, dengan resolusi terkontrol
     * dan format ARGB_8888 agar kompatibel dengan BitmapShader & Canvas software offscreen.
     */
    fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(activity.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDim = max(info.size.width, info.size.height)
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
            val b = binding.textPropertyPanelInclude.textureControlsInclude
            b.imgTextureThumbnail.setImageBitmap(bitmap)
            b.btnSelectTexture.text = "Ganti Foto"
            b.btnDeleteTexture.visibility = View.VISIBLE
            b.switchTextureEnabled.isChecked = true
            b.textureControlsGroup.visibility = View.VISIBLE
        }
        showSnackbar("Tekstur foto berhasil diterapkan pada teks!")
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

        binding.textEditorBar.visibility = if (isPageOpen) View.VISIBLE else View.GONE
        binding.textPropertyPanelInclude.root.visibility =
            if (isPageOpen && hasEditor && activeTextToolTag.isNotEmpty()) View.VISIBLE else View.GONE
        binding.textToolStripInclude.textToolStripScroll.visibility = if (isPageOpen) View.VISIBLE else View.GONE

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

    private fun updateTextPanelTitle() {
        binding.textPropertyPanelInclude.textPropertyPanelTitle.text =
            textToolLabels[activeTextToolTag] ?: "Tool"
    }

    private fun registerTextPanels() {
        val tp = binding.textPropertyPanelInclude
        textPanelViews.clear()
        textPanelViews[TOOL_STYLES]      = tp.stylesPanel.root
        textPanelViews[TOOL_FONT]        = tp.fontPanel.root
        textPanelViews[TOOL_POSITION]    = tp.positionPanel.root
        textPanelViews[TOOL_REL_POS]     = tp.relativePositionPanel.root
        textPanelViews[TOOL_SIZE]        = tp.sizePanel.root
        textPanelViews[TOOL_PADDING]     = tp.paddingPanel.root
        textPanelViews[TOOL_COLOR]       = tp.colorPanel.root
        textPanelViews[TOOL_GRADIENT]    = tp.gradientControlsInclude.root
        textPanelViews[TOOL_TEXTURE]     = tp.textureControlsInclude.root
        textPanelViews[TOOL_OPACITY]     = tp.opacityPanel.root
        textPanelViews[TOOL_ROTATE]      = tp.rotatePanel.root
        textPanelViews[TOOL_MASK]        = tp.maskPanel.root
        textPanelViews[TOOL_STYLE]       = tp.stylePanel.root
        textPanelViews[TOOL_CURVE]       = tp.curveControlsInclude.root
        textPanelViews[TOOL_BG]          = tp.backgroundPanel.root
        textPanelViews[TOOL_ALIGN]       = tp.alignPanel.root
        textPanelViews[TOOL_LETTER]      = tp.spacingControlsInclude.root
        textPanelViews[TOOL_LINE]        = tp.spacingControlsInclude.root
        textPanelViews[TOOL_STROKE]      = tp.strokePanel.root
        textPanelViews[TOOL_SHADOW]      = tp.shadowControlsInclude.root
        textPanelViews[TOOL_INNER]       = tp.innerShadowControlsInclude.root
        textPanelViews[TOOL_EMBOSS]      = tp.embossControlsInclude.root
        textPanelViews[TOOL_PERSPECTIVE] = tp.perspectiveControlsInclude.root
        textPanelViews[TOOL_3D_ROTATE]   = tp.rotate3DControlsInclude.root
        textPanelViews[TOOL_3D_TEXT]     = tp.extrudeControlsInclude.root
        textPanelViews[TOOL_3D_SHADOW]   = tp.shadowControlsInclude.root
        textPanelViews[TOOL_REFLECTION]  = tp.reflectionPanel.root
    }

    private fun buildTextToolStrip() {
        val specs = listOf(
            TextToolSpec(TOOL_STYLES,      "Styles",      R.drawable.ic_text_style_24px),
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
            TextToolSpec(TOOL_REFLECTION,  "Reflection",  R.drawable.ic_reflection_24px)
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
                setBackgroundResource(R.drawable.bg_text_tool_item)
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
    }

    private fun onTextToolClicked(tag: String) {
        val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
        if (layer == null || layer.isLocked) {
            showSnackbar("Pilih layer teks terlebih dahulu")
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
                showSnackbar(if (ok) "Lapisan teks dihapus" else "Tidak ada lapisan yang dipilih")
                return
            }
            TOOL_COPY -> {
                val copy = pixelCanvasView.duplicateSelectedLayer()
                showSnackbar(if (copy != null) "Lapisan teks digandakan" else "Gagal menggandakan lapisan")
                return
            }
            TOOL_FRONT -> {
                pixelCanvasView.bringSelectedLayerToFront()
                showSnackbar("Layer dipindahkan ke posisi paling atas")
                return
            }
            TOOL_BACK -> {
                pixelCanvasView.sendSelectedLayerToBack()
                showSnackbar("Layer dipindahkan ke posisi paling belakang")
                return
            }
            TOOL_FONT -> {
                selectTextTool(tag)
                return
            }
        }
        selectTextTool(tag)
    }

    private fun selectTextTool(tag: String) {
        activeTextToolTag = tag
        if (textToolItems.isEmpty()) return
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
    }

    private fun deselectTextTool() {
        activeTextToolTag = ""
        for ((t, item) in textToolItems) {
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
            maxPropertyPanelScrollH = (activity.resources.displayMetrics.heightPixels * 0.08f).toInt()
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

    // ─── Panel: Posisi ─────────────────────────────────────────────────────

    private fun initializePositionPanel() {
        val b = binding.textPropertyPanelInclude.positionPanel

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            b.editPosX.setText(if (layer.x == 0f) "0" else String.format(Locale.US, "%.0f", layer.x))
            b.editPosY.setText(if (layer.y == 0f) "0" else String.format(Locale.US, "%.0f", layer.y))
        }

        fun commitX() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            val v = b.editPosX.text.toString().toFloatOrNull() ?: return
            pixelCanvasView.runRecordedAction("Atur Posisi X") { layer.x = v }
            pixelCanvasView.invalidate()
        }

        fun commitY() {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            val v = b.editPosY.text.toString().toFloatOrNull() ?: return
            pixelCanvasView.runRecordedAction("Atur Posisi Y") { layer.y = v }
            pixelCanvasView.invalidate()
        }

        b.editPosX.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commitX() }
        b.editPosY.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commitY() }
        b.editPosX.setOnEditorActionListener { _, _, _ -> commitX(); true }
        b.editPosY.setOnEditorActionListener { _, _, _ -> commitY(); true }

        fun nudgeX(delta: Float, edit: com.google.android.material.textfield.TextInputEditText) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Pindah X") { layer.x += delta }
            pixelCanvasView.invalidate()
            edit.setText(String.format(Locale.US, "%.0f", layer.x))
        }

        fun nudgeY(delta: Float, edit: com.google.android.material.textfield.TextInputEditText) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Pindah Y") { layer.y += delta }
            pixelCanvasView.invalidate()
            edit.setText(String.format(Locale.US, "%.0f", layer.y))
        }

        b.btnPosXMinus.setOnClickListener { nudgeX(-10f, b.editPosX) }
        b.btnPosXPlus.setOnClickListener { nudgeX(10f, b.editPosX) }
        b.btnPosYMinus.setOnClickListener { nudgeY(-10f, b.editPosY) }
        b.btnPosYPlus.setOnClickListener { nudgeY(10f, b.editPosY) }

        b.btnPosCenterH.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            val (lw, _) = layer.getUnwarpedDimensions()
            pixelCanvasView.runRecordedAction("Tengah Horizontal") {
                layer.x = (pixelCanvasView.width - lw * layer.scale) / 2f
            }
            sync(layer)
            pixelCanvasView.invalidate()
        }

        b.btnPosCenterV.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            val (_, lh) = layer.getUnwarpedDimensions()
            pixelCanvasView.runRecordedAction("Tengah Vertikal") {
                layer.y = (pixelCanvasView.height - lh * layer.scale) / 2f
            }
            sync(layer)
            pixelCanvasView.invalidate()
        }

        b.btnPosReset.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            pixelCanvasView.runRecordedAction("Reset Posisi") {
                layer.x = 0f
                layer.y = 0f
            }
            sync(layer)
            pixelCanvasView.invalidate()
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Posisi Relatif ─────────────────────────────────────────────

    private var relAnchorX = 0f
    private var relAnchorY = 0f

    private fun initializeRelativePositionPanel() {
        val b = binding.textPropertyPanelInclude.relativePositionPanel
        var syncing = false

        fun place(horizontal: String, vertical: String) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            val (lw, lh) = layer.getUnwarpedDimensions()
            val w = pixelCanvasView.width
            val h = pixelCanvasView.height
            val sw = lw * layer.scale
            val sh = lh * layer.scale
            pixelCanvasView.runRecordedAction("Posisi Relatif") {
                when (horizontal) {
                    "left" -> layer.x = 0f
                    "right" -> layer.x = w - sw
                    else -> layer.x = (w - sw) / 2f
                }
                when (vertical) {
                    "top" -> layer.y = 0f
                    "bottom" -> layer.y = h - sh
                    else -> layer.y = (h - sh) / 2f
                }
            }
            relAnchorX = layer.x
            relAnchorY = layer.y
            syncing = true
            b.sliderRelOffsetX.value = 0f
            b.sliderRelOffsetY.value = 0f
            syncing = false
            pixelCanvasView.invalidate()
        }

        b.btnRelTopLeft.setOnClickListener { place("left", "top") }
        b.btnRelTopCenter.setOnClickListener { place("center", "top") }
        b.btnRelTopRight.setOnClickListener { place("right", "top") }
        b.btnRelCenterLeft.setOnClickListener { place("left", "center") }
        b.btnRelCenter.setOnClickListener { place("center", "center") }
        b.btnRelCenterRight.setOnClickListener { place("right", "center") }
        b.btnRelBottomLeft.setOnClickListener { place("left", "bottom") }
        b.btnRelBottomCenter.setOnClickListener { place("center", "bottom") }
        b.btnRelBottomRight.setOnClickListener { place("right", "bottom") }

        b.sliderRelOffsetX.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@addOnChangeListener
            layer.x = relAnchorX + value
            pixelCanvasView.invalidate()
        }

        b.sliderRelOffsetY.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@addOnChangeListener
            layer.y = relAnchorY + value
            pixelCanvasView.invalidate()
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) {
                relAnchorX = layer.x
                relAnchorY = layer.y
                syncing = true
                b.sliderRelOffsetX.value = 0f
                b.sliderRelOffsetY.value = 0f
                syncing = false
            }
        }
    }

    // ─── Panel: Ukuran ─────────────────────────────────────────────────────

    private fun initializeSizePanel() {
        val b = binding.textPropertyPanelInclude.sizePanel
        var syncing = false

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            syncing = true
            b.sliderFontSize.value = layer.textSize.coerceIn(8f, 600f)
            b.sliderScaleXY.value = layer.scale.coerceIn(0.1f, 8f)
            syncing = false
            b.tvSizeLabel.text =
                String.format(Locale.US, "Ukuran Font: %.0f px", layer.textSize)
            b.tvScaleLabel.text = "${(layer.scale * 100).toInt()}%"
        }

        b.sliderFontSize.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvSizeLabel.text = String.format(Locale.US, "Ukuran Font: %.0f px", value)
            applyToTextLayer { it.textSize = value }
        }

        b.sliderScaleXY.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvScaleLabel.text = "${(value * 100).toInt()}%"
            applyToTextLayer { it.scale = value }
        }

        b.btnSizeReset.setOnClickListener {
            applyToTextLayer { it.textSize = 64f }
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) sync(layer)
            pixelCanvasView.invalidate()
        }

        b.btnScaleReset.setOnClickListener {
            applyToTextLayer { it.scale = 1f }
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) sync(layer)
            pixelCanvasView.invalidate()
        }

        b.btnScaleFit.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            val (lw, lh) = layer.getUnwarpedDimensions()
            val s = kotlin.math.min(
                pixelCanvasView.width / lw,
                pixelCanvasView.height / lh * 0.9f
            )
            pixelCanvasView.runRecordedAction("Sesuaikan Kanvas") { layer.scale = s.coerceAtLeast(0.01f) }
            sync(layer)
            pixelCanvasView.invalidate()
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Opasitas ───────────────────────────────────────────────────

    private fun initializeOpacityPanel() {
        val b = binding.textPropertyPanelInclude.opacityPanel
        var syncing = false

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            syncing = true
            b.sliderOpacity.value = (layer.opacity.coerceIn(0, 255) * 100f / 255f)
            syncing = false
            b.tvOpacityLabel.text = "${(layer.opacity.coerceIn(0, 255) * 100 / 255)}%"
        }

        b.sliderOpacity.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvOpacityLabel.text = "${value.toInt()}%"
            applyToTextLayer { it.opacity = (value * 255 / 100).toInt() }
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Rotasi ─────────────────────────────────────────────────────

    private fun initializeRotatePanel() {
        val b = binding.textPropertyPanelInclude.rotatePanel
        var syncing = false

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            val r = ((layer.rotation % 360f) + 360f) % 360f
            syncing = true
            b.sliderRotate.value = r
            syncing = false
            b.tvRotateLabel.text = "${r.toInt()}°"
        }

        b.sliderRotate.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvRotateLabel.text = "${value.toInt()}°"
            applyToTextLayer { it.rotation = value }
        }

        fun rotateBy(delta: Float) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Putar Teks") {
                layer.rotation = (((layer.rotation + delta) % 360f) + 360f) % 360f
            }
            sync(layer)
            pixelCanvasView.invalidate()
        }

        b.btnRotateMinus90.setOnClickListener { rotateBy(-90f) }
        b.btnRotateReset.setOnClickListener {
            applyToTextLayer { it.rotation = 0f }
            val layer = pixelCanvasView.selectedLayer
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
            pixelCanvasView.invalidate()
        }
        b.btnRotatePlus90.setOnClickListener { rotateBy(90f) }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Gaya Teks (B / I / U / S + Font Weight) ────────────────────

    private fun initializeStylePanel() {
        val b = binding.textPropertyPanelInclude.stylePanel
        val boldBtn = b.btnStyleBold
        val italicBtn = b.btnStyleItalic
        val underlineBtn = b.btnStyleUnderline
        val strikeBtn = b.btnStyleStrike

        fun setActive(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else android.graphics.Color.TRANSPARENT)
            btn.setTextColor(if (active) android.graphics.Color.WHITE else COLOR_GRAY)
        }

        fun toggle(flag: String) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Ubah Gaya Teks") {
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
            pixelCanvasView.runRecordedAction("Ubah Ketebalan Font") {
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

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) {
                setActive(boldBtn, layer.isBold)
                setActive(italicBtn, layer.isItalic)
                setActive(underlineBtn, layer.isUnderline)
                setActive(strikeBtn, layer.isStrikethrough)
            }
        }
    }

    // ─── Panel: Perataan Teks ──────────────────────────────────────────────

    private fun initializeAlignPanel() {
        val b = binding.textPropertyPanelInclude.alignPanel

        fun setActive(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
            btn.isSelected = active
            btn.setBackgroundColor(if (active) COLOR_ACTIVE else android.graphics.Color.TRANSPARENT)
            btn.iconTint = android.content.res.ColorStateList.valueOf(
                if (active) android.graphics.Color.WHITE else COLOR_GRAY
            )
            btn.strokeWidth = if (active) 0 else 1
        }

        fun apply(align: android.text.Layout.Alignment, justify: Boolean) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return
            pixelCanvasView.runRecordedAction("Ubah Perataan Teks") {
                layer.alignment = align
                layer.justifyEnabled = justify
            }
            pixelCanvasView.invalidate()
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

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) {
                val justify = layer.justifyEnabled
                val align = layer.alignment
                setActive(b.btnAlignJustify, justify)
                when {
                    !justify && align == android.text.Layout.Alignment.ALIGN_NORMAL -> { setActive(b.btnAlignLeft, true); setActive(b.btnAlignCenter, false); setActive(b.btnAlignRight, false) }
                    align == android.text.Layout.Alignment.ALIGN_CENTER -> { setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, true); setActive(b.btnAlignRight, false) }
                    align == android.text.Layout.Alignment.ALIGN_OPPOSITE -> { setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, false); setActive(b.btnAlignRight, true) }
                    else -> { setActive(b.btnAlignLeft, false); setActive(b.btnAlignCenter, false); setActive(b.btnAlignRight, false) }
                }
            }
        }
    }

    // ─── Panel: Warna Teks ─────────────────────────────────────────────────

    private fun initializeColorPanel() {
        val b = binding.textPropertyPanelInclude.colorPanel
        val density = activity.resources.displayMetrics.density

        val palette = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF9E9E9E.toInt(), 0xFFD32F2F.toInt(),
            0xFFF57C00.toInt(), 0xFFFBC02D.toInt(), 0xFF388E3C.toInt(), 0xFF0288D1.toInt(),
            0xFF1976D2.toInt(), 0xFF7B1FA2.toInt(), 0xFFC2185B.toInt(), 0xFF607D8B.toInt()
        )

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
                    setOnClickListener {
                        applyToTextLayer { layer ->
                            layer.textColor = c
                            layer.gradientEnabled = false
                            layer.textureEnabled = false
                        }
                        b.tvColorHex.text = String.format(Locale.US, "#%08X", c)
                        b.btnColorPicker.iconTint = android.content.res.ColorStateList.valueOf(c)
                        pixelCanvasView.invalidate()
                    }
                }
                b.layoutColorSwatches.addView(sw)
            }
        }
        buildSwatches()

        b.btnColorPicker.setOnClickListener { launchColorPicker() }
        b.btnColorGradient.setOnClickListener { selectTextTool(TOOL_GRADIENT) }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) {
                b.tvColorHex.text = String.format(Locale.US, "#%08X", layer.textColor)
                b.btnColorPicker.iconTint = android.content.res.ColorStateList.valueOf(layer.textColor)
            }
        }
    }

    // ─── Panel: Stroke / Outline ───────────────────────────────────────────

    private var lastStrokeWidth = 4f

    private fun initializeStrokePanel() {
        val b = binding.textPropertyPanelInclude.strokePanel
        val density = activity.resources.displayMetrics.density
        var syncing = false

        fun updateSwatch(color: Int) {
            b.viewStrokeColorSwatch.backgroundTintList =
                android.content.res.ColorStateList.valueOf(color)
        }

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            syncing = true
            val enabled = layer.strokeWidth > 0f
            b.switchStrokeEnabled.isChecked = enabled
            b.strokeSliderGroup.visibility = if (enabled) View.VISIBLE else View.GONE
            b.sliderStrokeWidth.value = layer.strokeWidth.coerceIn(0f, 60f)
            val alpha = (layer.strokeColor ushr 24) and 0xFF
            b.sliderStrokeOpacity.value = (alpha * 100f / 255f)
            syncing = false
            updateSwatch(layer.strokeColor)
            b.tvStrokeWidthLabel.text =
                String.format(Locale.US, "Ketebalan: %.1f px", layer.strokeWidth)
            b.tvStrokeOpacityLabel.text = "Opasitas: ${(alpha * 100 / 255)}%"
        }

        b.switchStrokeEnabled.setOnCheckedChangeListener { _, isChecked ->
            applyToTextLayer { layer ->
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

        b.sliderStrokeWidth.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvStrokeWidthLabel.text = String.format(Locale.US, "Ketebalan: %.1f px", value)
            lastStrokeWidth = value
            applyToTextLayer { it.strokeWidth = value }
        }

        b.sliderStrokeOpacity.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvStrokeOpacityLabel.text = "Opasitas: ${value.toInt()}%"
            applyToTextLayer { layer ->
                val a = (value * 255 / 100).toInt()
                layer.strokeColor = (layer.strokeColor and 0x00FFFFFF) or (a shl 24)
            }
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer != null) updateSwatch(layer.strokeColor)
        }

        b.viewStrokeColorSwatch.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            showColorSwatchDialog("Warna Outline", layer.strokeColor) { color ->
                applyToTextLayer { it.strokeColor = color }
                sync(layer)
                pixelCanvasView.invalidate()
            }
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Padding ────────────────────────────────────────────────────

    private fun initializePaddingPanel() {
        val b = binding.textPropertyPanelInclude.paddingPanel
        var syncing = false

        fun applyAll(value: Float) {
            applyToTextLayer { it.paddingTop = value; it.paddingBottom = value; it.paddingLeft = value; it.paddingRight = value }
        }

        fun syncTexts(top: Float, bottom: Float, left: Float, right: Float) {
            b.tvPaddingTop.text = String.format(Locale.US, "Atas: %.0f px", top)
            b.tvPaddingBottom.text = String.format(Locale.US, "Bawah: %.0f px", bottom)
            b.tvPaddingLeft.text = String.format(Locale.US, "Kiri: %.0f px", left)
            b.tvPaddingRight.text = String.format(Locale.US, "Kanan: %.0f px", right)
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

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
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

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Background Teks ────────────────────────────────────────────

    private fun initializeBackgroundPanel() {
        val b = binding.textPropertyPanelInclude.backgroundPanel
        var syncing = false

        fun updateSwatch(color: Int) {
            b.viewBgColorSwatch.backgroundTintList =
                android.content.res.ColorStateList.valueOf(color)
        }

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            syncing = true
            b.switchBgEnabled.isChecked = layer.bgEnabled
            b.bgControlGroup.visibility = if (layer.bgEnabled) View.VISIBLE else View.GONE
            b.sliderBgOpacity.value = (layer.bgOpacity * 100f).coerceIn(0f, 100f)
            b.sliderBgPadding.value = layer.bgPadding.coerceIn(0f, 100f)
            b.sliderBgCornerRadius.value = layer.bgCornerRadius.coerceIn(0f, 120f)
            syncing = false
            updateSwatch(layer.bgColor)
            b.tvBgOpacityLabel.text = "Opasitas: ${(layer.bgOpacity * 100).toInt()}%"
            b.tvBgPadding.text = String.format(Locale.US, "Padding Latar: %.0f px", layer.bgPadding)
            b.tvBgCornerRadius.text = String.format(Locale.US, "Sudut Membulat: %.0f px", layer.bgCornerRadius)
        }

        b.switchBgEnabled.setOnCheckedChangeListener { _, isChecked ->
            b.bgControlGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.bgEnabled = isChecked }
        }

        b.sliderBgOpacity.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvBgOpacityLabel.text = "Opasitas: ${value.toInt()}%"
            applyToTextLayer { it.bgOpacity = value / 100f }
        }

        b.sliderBgPadding.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvBgPadding.text = String.format(Locale.US, "Padding Latar: %.0f px", value)
            applyToTextLayer { it.bgPadding = value }
        }

        b.sliderBgCornerRadius.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvBgCornerRadius.text = String.format(Locale.US, "Sudut Membulat: %.0f px", value)
            applyToTextLayer { it.bgCornerRadius = value }
        }

        b.viewBgColorSwatch.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            showColorSwatchDialog("Warna Latar Teks", layer.bgColor) { color ->
                applyToTextLayer { it.bgColor = color }
                sync(layer)
                pixelCanvasView.invalidate()
            }
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Refleksi ───────────────────────────────────────────────────

    private fun initializeReflectionPanel() {
        val b = binding.textPropertyPanelInclude.reflectionPanel
        var syncing = false

        fun sync(layer: com.flyerpix.editor.canvas.model.TextLayer) {
            syncing = true
            b.switchReflectionEnabled.isChecked = layer.reflectionEnabled
            b.reflectionSliderGroup.visibility = if (layer.reflectionEnabled) View.VISIBLE else View.GONE
            b.sliderReflectionOpacity.value = (layer.reflectionOpacity * 100f).coerceIn(0f, 100f)
            b.sliderReflectionDistance.value = layer.reflectionDistance.coerceIn(0f, 200f)
            b.sliderReflectionFade.value = (layer.reflectionFade * 100f).coerceIn(0f, 100f)
            syncing = false
            b.tvReflectionOpacity.text = "Opasitas: ${(layer.reflectionOpacity * 100).toInt()}%"
            b.tvReflectionDistance.text = String.format(Locale.US, "Jarak: %.0f px", layer.reflectionDistance)
            b.tvReflectionFade.text = "Memudar (Fade): ${(layer.reflectionFade * 100).toInt()}%"
        }

        b.switchReflectionEnabled.setOnCheckedChangeListener { _, isChecked ->
            b.reflectionSliderGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyToTextLayer { it.reflectionEnabled = isChecked }
        }

        b.sliderReflectionOpacity.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvReflectionOpacity.text = "Opasitas: ${value.toInt()}%"
            applyToTextLayer { it.reflectionOpacity = value / 100f }
        }

        b.sliderReflectionDistance.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvReflectionDistance.text = String.format(Locale.US, "Jarak: %.0f px", value)
            applyToTextLayer { it.reflectionDistance = value }
        }

        b.sliderReflectionFade.addOnChangeListener { _, value, _ ->
            if (syncing) return@addOnChangeListener
            b.tvReflectionFade.text = "Memudar (Fade): ${value.toInt()}%"
            applyToTextLayer { it.reflectionFade = value / 100f }
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
            if (layer is com.flyerpix.editor.canvas.model.TextLayer) sync(layer)
        }
    }

    // ─── Panel: Masking (Foto di Dalam Teks) ───────────────────────────────

    private fun initializeMaskPanel() {
        val b = binding.textPropertyPanelInclude.maskPanel

        b.btnMaskSelectImage.setOnClickListener {
            texturePickerLauncher?.launch("image/*")
        }

        b.btnMaskClear.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer ?: return@setOnClickListener
            pixelCanvasView.runRecordedAction("Hapus Mask") {
                layer.textureBitmap = null
                layer.textureEnabled = false
            }
            val t = binding.textPropertyPanelInclude.textureControlsInclude
            t.imgTextureThumbnail.setImageDrawable(null)
            t.btnSelectTexture.text = "Pilih Foto"
            t.btnDeleteTexture.visibility = View.GONE
            t.switchTextureEnabled.isChecked = false
            t.textureControlsGroup.visibility = View.GONE
            pixelCanvasView.invalidate()
            showSnackbar("Mask teks dihapus")
        }
    }

    // ─── Panel: Styles Teks (Simpan / Terapkan) ────────────────────────────

    private fun initializeTextStylesPanel() {
        val b = binding.textPropertyPanelInclude.stylesPanel

        fun rebuildChips() {
            b.layoutStyleChips.removeAllViews()
            val density = activity.resources.displayMetrics.density
            for ((name, style) in savedTextStyles) {
                val chip = com.google.android.material.button.MaterialButton(activity)
                chip.text = name
                chip.textSize = 12f
                chip.minimumWidth = 0
                chip.isAllCaps = false
                chip.setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
                chip.setOnClickListener {
                    val target = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
                    if (target == null || target.isLocked) {
                        showSnackbar("Pilih layer teks terlebih dahulu")
                        return@setOnClickListener
                    }
                    pixelCanvasView.runRecordedAction("Terapkan Style") {
                        copyStyleOntoTarget(style, target)
                    }
                    pixelCanvasView.invalidate()
                    showSnackbar("Style '$name' diterapkan")
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
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (name.isNullOrEmpty()) {
                showSnackbar("Tulis nama style terlebih dahulu")
                return@setOnClickListener
            }
            if (layer == null || layer.isLocked) {
                showSnackbar("Pilih layer teks terlebih dahulu")
                return@setOnClickListener
            }
            savedTextStyles[name] = layer.copyLayer()
            b.editStyleName.setText("")
            rebuildChips()
            hideKeyboard(b.editStyleName)
            showSnackbar("Style '$name' berhasil disimpan")
        }

        val prevListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevListener?.invoke(layer)
        }
        rebuildChips()
    }

    private fun copyStyleOntoTarget(
        style: com.flyerpix.editor.canvas.model.TextLayer,
        target: com.flyerpix.editor.canvas.model.TextLayer
    ) {
        target.text = style.text
        target.textSize = style.textSize
        target.textColor = style.textColor
        target.typeface = style.typeface
        target.fontName = style.fontName
        target.letterSpacing = style.letterSpacing
        target.lineSpacing = style.lineSpacing
        target.alignment = style.alignment
        target.justifyEnabled = style.justifyEnabled
        target.isBold = style.isBold
        target.isItalic = style.isItalic
        target.isUnderline = style.isUnderline
        target.isStrikethrough = style.isStrikethrough
        target.strokeColor = style.strokeColor
        target.strokeWidth = style.strokeWidth
        target.shadowEnabled = style.shadowEnabled
        target.shadowColor = style.shadowColor
        target.shadowRadius = style.shadowRadius
        target.shadowDx = style.shadowDx
        target.shadowDy = style.shadowDy
        target.shadowOpacity = style.shadowOpacity
        target.innerShadowEnabled = style.innerShadowEnabled
        target.innerShadowColor = style.innerShadowColor
        target.innerShadowRadius = style.innerShadowRadius
        target.innerShadowDx = style.innerShadowDx
        target.innerShadowDy = style.innerShadowDy
        target.innerShadowOpacity = style.innerShadowOpacity
        target.embossEnabled = style.embossEnabled
        target.embossLightAngle = style.embossLightAngle
        target.embossAmbient = style.embossAmbient
        target.embossSpecular = style.embossSpecular
        target.embossBlurRadius = style.embossBlurRadius
        target.gradientEnabled = style.gradientEnabled
        target.gradient = style.gradient?.copy()
        target.textureEnabled = style.textureEnabled
        target.textureBitmap = style.textureBitmap
        target.textureScale = style.textureScale
        target.textureRotation = style.textureRotation
        target.extrudeEnabled = style.extrudeEnabled
        target.extrudeDepth = style.extrudeDepth
        target.extrudeColor = style.extrudeColor
        target.extrudeViewType = style.extrudeViewType
        target.extrudeAngle = style.extrudeAngle
        target.rotate3DX = style.rotate3DX
        target.rotate3DY = style.rotate3DY
        target.rotate3DZ = style.rotate3DZ
        target.curvePercent = style.curvePercent
        target.paddingTop = style.paddingTop
        target.paddingBottom = style.paddingBottom
        target.paddingLeft = style.paddingLeft
        target.paddingRight = style.paddingRight
        target.bgEnabled = style.bgEnabled
        target.bgColor = style.bgColor
        target.bgOpacity = style.bgOpacity
        target.bgPadding = style.bgPadding
        target.bgCornerRadius = style.bgCornerRadius
        target.reflectionEnabled = style.reflectionEnabled
        target.reflectionOpacity = style.reflectionOpacity
        target.reflectionDistance = style.reflectionDistance
        target.reflectionFade = style.reflectionFade
    }

    private fun showColorSwatchDialog(title: String, initial: Int, onPick: (Int) -> Unit) {
        val density = activity.resources.displayMetrics.density
        val palette = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF757575.toInt(), 0xFFD32F2F.toInt(),
            0xFFF57C00.toInt(), 0xFFFBC02D.toInt(), 0xFF388E3C.toInt(), 0xFF0288D1.toInt(),
            0xFF1976D2.toInt(), 0xFF7B1FA2.toInt(), 0xFFC2185B.toInt(), 0xFF795548.toInt(),
            0xFF9E9E9E.toInt(), 0xFF607D8B.toInt()
        )
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (4 * density).toInt())
        }
        for (c in palette) {
            val sw = View(activity).apply {
                val s = (34 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(s, s).apply { marginEnd = (8 * density).toInt() }
                setBackgroundResource(R.drawable.bg_color_swatch)
                backgroundTintList = android.content.res.ColorStateList.valueOf(c)
                isClickable = true
                isFocusable = true
            }
            sw.setTag(c)
            row.addView(sw)
        }

        val dialog = MaterialAlertDialogBuilder(activity, R.style.AppAlertDialog)
            .setTitle(title)
            .setView(row)
            .setNegativeButton("Tutup", null)
            .create()

        for (i in 0 until row.childCount) {
            val sw = row.getChildAt(i)
            sw.setOnClickListener {
                onPick(sw.tag as Int)
                dialog.dismiss()
            }
        }
        dialog.show()
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
        binding.textPropertyPanelInclude.root.visibility = View.GONE
        binding.textToolStripInclude.textToolStripScroll.visibility = View.GONE
        activeTextToolTag = ""
        for (v in textPanelViews.values) v.visibility = View.GONE
    }
}
