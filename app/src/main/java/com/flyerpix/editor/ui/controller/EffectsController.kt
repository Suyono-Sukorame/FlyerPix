package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.ui.compose.CanvasAdjustDetailPage
import com.flyerpix.editor.ui.compose.CanvasBlurDetailPage
import com.flyerpix.editor.ui.compose.CanvasFiltersDetailPage

/**
 * Controller untuk mengelola efek kanvas (Vignette, Noise, Filter, Adjustments, Blur)
 * dengan dukungan Jetpack Compose bottom sheets dan snapshot rollback.
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi panel effects menu
 * - Mengelola adjustment sliders (Brightness, Contrast, Saturation) via Compose
 * - Mengelola effect filters (Vignette, Noise, Monochrome) via Compose
 * - Mengelola blur slider via Compose
 * - Snapshot state untuk rollback instan saat dibatalkan (Cancel / Back press)
 * - Pencatatan ke undo/redo history saat diterapkan (Apply)
 */
class EffectsController(
    private val activity: Activity,
    private val binding: ActivityEditorBinding,
    private val pixelCanvasView: PixelCanvasView,
    private val showSnackbar: (String) -> Unit
) {

    companion object {
        const val TOOL_ADJUST   = "effects_adjust"
        const val TOOL_EFFECTS  = "effects_effects"
        const val TOOL_BLUR     = "effects_blur"

        // Diseragamkan dengan Text dan Object: biru brand #1769FF
        const val COLOR_ACTIVE   = 0xFF1769FF.toInt()
        const val COLOR_INACTIVE = 0xFF616161.toInt()
    }

    private val composeHost: ComposeView? get() = binding.composeThreeDDetail
    private val composeContainer: FrameLayout? get() = binding.composeThreeDSheetContainer

    private val toolItems = LinkedHashMap<String, ViewGroup>()
    private var activeTag = ""

    var onDetailExpandedChanged: ((Boolean) -> Unit)? = null

    /**
     * Snapshot state efek kanvas sebelum tool diedit.
     */
    data class CanvasEffectsSnapshot(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float,
        val blur: Float,
        val activeEffects: Set<PixelCanvasView.CanvasEffect>
    )

    private var initialSnapshot: CanvasEffectsSnapshot? = null

    private fun notifyDetailExpanded() {
        onDetailExpandedChanged?.invoke(activeTag.isNotEmpty())
    }

    private fun computeComposeSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        val floorPx = (320 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.42f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    /**
     * Inisialisasi semua kontrol effects menu.
     */
    fun initialize() {
        buildToolStrip()
        setupAdjustmentSliders()
        setupEffectChips()
        setupBlurSlider()
    }

    // ────────────────────────────────────────────────────────────────────────
    // TOOL STRIP (ikon menu master, pola sama dengan Text/Objects/Canvas)
    // ────────────────────────────────────────────────────────────────────────

    private fun buildToolStrip() {
        data class Spec(val tag: String, val label: String, val iconRes: Int)
        val specs = listOf(
            Spec(TOOL_ADJUST,  "Adjust",  R.drawable.ic_sharp_palette_24px),
            Spec(TOOL_EFFECTS, "Filters", R.drawable.ic_nav_wand_24px),
            Spec(TOOL_BLUR,    "Blur",    R.drawable.ic_opacity_24px)
        )
        val density = activity.resources.displayMetrics.density
        val container = binding.effectToolStripInclude.effectToolStripContainer
        container.removeAllViews()

        for (spec in specs) {
            val item = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                isClickable = true; isFocusable = true
                setBackgroundResource(R.drawable.bg_panel_tool_item)
                setPadding((8*density).toInt(), (8*density).toInt(), (8*density).toInt(), (6*density).toInt())
                setOnClickListener { onToolClicked(spec.tag) }
            }
            val iconSize = (28 * density).toInt()
            item.addView(ImageView(activity).apply {
                setImageResource(spec.iconRes)
                colorFilter = PorterDuffColorFilter(COLOR_INACTIVE, PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            })
            item.addView(TextView(activity).apply {
                text = spec.label; textSize = 11f; maxLines = 1
                gravity = android.view.Gravity.CENTER; setTextColor(COLOR_INACTIVE)
            })
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.width = (62 * density).toInt()
            container.addView(item, lp)
            toolItems[spec.tag] = item
        }
    }

    private fun onToolClicked(tag: String) {
        if (tag == activeTag) cancelSheet() else select(tag)
    }

    fun select(tag: String) {
        if (activeTag != tag) {
            if (initialSnapshot == null) {
                snapshotCurrentState()
            }
        }
        activeTag = tag
        for ((t, item) in toolItems) {
            val sel = t == tag
            item.isSelected = sel
            val c = if (sel) COLOR_ACTIVE else COLOR_INACTIVE
            (item.getChildAt(0) as? ImageView)?.colorFilter =
                PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? TextView)?.setTextColor(c)
        }
        applyContentVisibility()
    }

    fun deselect() {
        if (initialSnapshot != null) {
            restoreSnapshot()
            initialSnapshot = null
        }
        activeTag = ""
        for (item in toolItems.values) {
            item.isSelected = false
            (item.getChildAt(0) as? ImageView)?.colorFilter =
                PorterDuffColorFilter(COLOR_INACTIVE, PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? TextView)?.setTextColor(COLOR_INACTIVE)
        }
        composeContainer?.visibility = View.GONE
        applyContentVisibility()
    }

    private fun snapshotCurrentState() {
        initialSnapshot = CanvasEffectsSnapshot(
            brightness = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS),
            contrast = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST),
            saturation = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION),
            blur = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BLUR),
            activeEffects = pixelCanvasView.activeEffectList.toSet()
        )
    }

    private fun restoreSnapshot() {
        val snap = initialSnapshot ?: return
        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS, snap.brightness)
        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST, snap.contrast)
        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION, snap.saturation)
        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, snap.blur)

        for (effect in PixelCanvasView.CanvasEffect.values()) {
            pixelCanvasView.setEffectEnabled(effect, snap.activeEffects.contains(effect))
        }
        pixelCanvasView.invalidate()
        refreshLegacyUI()
    }

    private fun applyContentVisibility() {
        if (composeHost != null && composeContainer != null) {
            binding.effectContentPanel.visibility = View.GONE
            if (activeTag.isEmpty()) {
                composeContainer?.visibility = View.GONE
            } else {
                when (activeTag) {
                    TOOL_ADJUST -> showComposeAdjustSheet()
                    TOOL_EFFECTS -> showComposeFiltersSheet()
                    TOOL_BLUR -> showComposeBlurSheet()
                    else -> composeContainer?.visibility = View.GONE
                }
            }
        } else {
            // Fallback ke XML panel legacy jika Compose view tidak tersedia
            binding.effectContentPanel.visibility =
                if (activeTag.isEmpty()) View.GONE else View.VISIBLE
            binding.effectContentAdjust.visibility =
                if (activeTag == TOOL_ADJUST) View.VISIBLE else View.GONE
            binding.effectContentEffects.visibility =
                if (activeTag == TOOL_EFFECTS) View.VISIBLE else View.GONE
            binding.effectContentBlur.visibility =
                if (activeTag == TOOL_BLUR) View.VISIBLE else View.GONE
        }
        notifyDetailExpanded()
    }

    // ────────────────────────────────────────────────────────────────────────
    // COMPOSE BOTTOM SHEETS
    // ────────────────────────────────────────────────────────────────────────

    private fun showComposeAdjustSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.effectContentPanel.visibility = View.GONE
        container.visibility = View.VISIBLE

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { pixelCanvasView.invalidate() }

        val brightness = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS)
        val contrast = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST)
        val saturation = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION)

        host.setContent {
            CanvasAdjustDetailPage(
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                onBrightnessChange = { b ->
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS, b)
                    refreshLegacyUI()
                },
                onContrastChange = { c ->
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST, c)
                    refreshLegacyUI()
                },
                onSaturationChange = { s ->
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION, s)
                    refreshLegacyUI()
                },
                onReset = {
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS, 0f)
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST, 0f)
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION, 0f)
                    refreshLegacyUI()
                },
                onApply = {
                    pixelCanvasView.runRecordedAction("Adjust Canvas Colors") {}
                    initialSnapshot = null
                    deselect()
                },
                onCancel = {
                    cancelSheet()
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showComposeFiltersSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.effectContentPanel.visibility = View.GONE
        container.visibility = View.VISIBLE

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { pixelCanvasView.invalidate() }

        val vignette = pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE)
        val noise = pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.NOISE)
        val filter = pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.FILTER)

        host.setContent {
            CanvasFiltersDetailPage(
                vignetteEnabled = vignette,
                noiseEnabled = noise,
                filterMonochromeEnabled = filter,
                onVignetteChange = { en ->
                    pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE, en)
                    refreshLegacyUI()
                },
                onNoiseChange = { en ->
                    pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.NOISE, en)
                    refreshLegacyUI()
                },
                onFilterMonochromeChange = { en ->
                    pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.FILTER, en)
                    refreshLegacyUI()
                },
                onReset = {
                    pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE, false)
                    pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.NOISE, false)
                    pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.FILTER, false)
                    refreshLegacyUI()
                },
                onApply = {
                    pixelCanvasView.runRecordedAction("Apply Canvas Filters") {}
                    initialSnapshot = null
                    deselect()
                },
                onCancel = {
                    cancelSheet()
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showComposeBlurSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.effectContentPanel.visibility = View.GONE
        container.visibility = View.VISIBLE

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { pixelCanvasView.invalidate() }

        val blur = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BLUR)

        host.setContent {
            CanvasBlurDetailPage(
                blurRadius = blur,
                onBlurRadiusChange = { r ->
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, r)
                    refreshLegacyUI()
                },
                onReset = {
                    pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, 0f)
                    refreshLegacyUI()
                },
                onApply = {
                    pixelCanvasView.runRecordedAction("Set Canvas Blur") {}
                    initialSnapshot = null
                    deselect()
                },
                onCancel = {
                    cancelSheet()
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    /**
     * Mengecek apakah bottom sheet effects saat ini sedang terbuka.
     */
    fun isSheetOpen(): Boolean = activeTag.isNotEmpty() && composeContainer?.visibility == View.VISIBLE

    /**
     * Membatalkan perubahan pada sheet saat ini dan mengembalikannya ke snapshot awal.
     */
    fun cancelSheet(): Boolean {
        if (activeTag.isNotEmpty()) {
            restoreSnapshot()
            initialSnapshot = null
            deselect()
            return true
        }
        return false
    }

    // ────────────────────────────────────────────────────────────────────────
    // LEGACY SETUP ADJUSTMENT SLIDERS (Fallback)
    // ────────────────────────────────────────────────────────────────────────

    private fun setupAdjustmentSliders() {
        binding.sliderBrightness.addOnChangeListener { _, value, _ ->
            binding.tvAdjustBrightness.text = "Brightness: ${value.toInt()}"
            pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS, value)
        }

        binding.sliderContrast.addOnChangeListener { _, value, _ ->
            binding.tvAdjustContrast.text = "Contrast: ${value.toInt()}"
            pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST, value)
        }

        binding.sliderSaturation.addOnChangeListener { _, value, _ ->
            binding.tvAdjustSaturation.text = "Saturation: ${value.toInt()}"
            pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION, value)
        }
    }

    private fun resetAllAdjustments() {
        binding.sliderBrightness.value = 0f
        binding.sliderContrast.value = 0f
        binding.sliderSaturation.value = 0f

        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS, 0f)
        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST, 0f)
        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION, 0f)

        showSnackbar("Semua adjustment direset")
    }

    // ────────────────────────────────────────────────────────────────────────
    // LEGACY SETUP EFFECT CHIPS (Fallback)
    // ────────────────────────────────────────────────────────────────────────

    private fun setupEffectChips() {
        binding.chipEffectVignette.setOnCheckedChangeListener { _, checked ->
            pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE, checked)
            if (checked) showSnackbar("Efek Vignette diaktifkan")
        }

        binding.chipEffectNoise.setOnCheckedChangeListener { _, checked ->
            pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.NOISE, checked)
            if (checked) showSnackbar("Efek Noise diaktifkan")
        }

        binding.chipEffectFilter.setOnCheckedChangeListener { _, checked ->
            pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.FILTER, checked)
            if (checked) showSnackbar("Efek Filter (Monochrome) diaktifkan")
        }
    }

    private fun resetAllEffects() {
        binding.chipEffectVignette.isChecked = false
        binding.chipEffectNoise.isChecked = false
        binding.chipEffectFilter.isChecked = false

        pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE, false)
        pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.NOISE, false)
        pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.FILTER, false)

        showSnackbar("Semua efek dimatikan")
    }

    // ────────────────────────────────────────────────────────────────────────
    // LEGACY SETUP BLUR SLIDER (Fallback)
    // ────────────────────────────────────────────────────────────────────────

    private fun setupBlurSlider() {
        binding.sliderBlurRadius.addOnChangeListener { _, value, _ ->
            binding.tvBlurRadius.text = "Blur Radius: ${value.toInt()}"
            pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, value)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Refresh UI effects menu.
     * Sinkronkan state UI dengan state canvas saat ini.
     */
    fun refreshUI() {
        binding.effectToolStripInclude.effectToolStripScroll.visibility = View.VISIBLE
        if (activeTag.isEmpty()) {
            binding.effectContentPanel.visibility = View.GONE
            composeContainer?.visibility = View.GONE
        } else {
            select(activeTag)
        }
        refreshLegacyUI()
    }

    private fun refreshLegacyUI() {
        val brightness = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS)
        val contrast = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST)
        val saturation = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION)
        val blur = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BLUR)

        if (binding.sliderBrightness.value != brightness) binding.sliderBrightness.value = brightness
        if (binding.sliderContrast.value != contrast) binding.sliderContrast.value = contrast
        if (binding.sliderSaturation.value != saturation) binding.sliderSaturation.value = saturation
        if (binding.sliderBlurRadius.value != blur) binding.sliderBlurRadius.value = blur

        binding.tvAdjustBrightness.text = "Brightness: ${brightness.toInt()}"
        binding.tvAdjustContrast.text = "Contrast: ${contrast.toInt()}"
        binding.tvAdjustSaturation.text = "Saturation: ${saturation.toInt()}"
        binding.tvBlurRadius.text = "Blur Radius: ${blur.toInt()}"

        val vig = pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE)
        val noise = pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.NOISE)
        val filter = pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.FILTER)

        if (binding.chipEffectVignette.isChecked != vig) binding.chipEffectVignette.isChecked = vig
        if (binding.chipEffectNoise.isChecked != noise) binding.chipEffectNoise.isChecked = noise
        if (binding.chipEffectFilter.isChecked != filter) binding.chipEffectFilter.isChecked = filter
    }

    /**
     * Reset semua efek dan adjustment.
     */
    fun resetAll() {
        resetAllAdjustments()
        resetAllEffects()
        binding.sliderBlurRadius.value = 0f
        pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, 0f)
    }

    /**
     * Mengecek apakah ada efek atau adjustment yang aktif.
     */
    fun hasActiveEffects(): Boolean {
        val brightness = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS)
        val contrast = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST)
        val saturation = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION)
        val blur = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BLUR)

        val hasAdjustments = brightness != 0f || contrast != 0f || saturation != 0f || blur != 0f
        val hasEffects = pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE) ||
                pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.NOISE) ||
                pixelCanvasView.isEffectEnabled(PixelCanvasView.CanvasEffect.FILTER)

        return hasAdjustments || hasEffects
    }
}
