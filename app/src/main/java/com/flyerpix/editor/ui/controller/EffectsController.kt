package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.databinding.ActivityEditorBinding

/**
 * Controller untuk mengelola efek kanvas (Vignette, Noise, Filter, Adjustments, Blur).
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi panel effects menu
 * - Mengelola adjustment sliders (Brightness, Contrast, Saturation)
 * - Mengelola effect chips (Vignette, Noise, Filter)
 * - Mengelola blur slider
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

        const val COLOR_ACTIVE = 0xFF18C8F5.toInt()
        const val COLOR_INACTIVE = 0xFFCCCCCC.toInt()
    }

    private val toolItems = LinkedHashMap<String, ViewGroup>()
    private var activeTag = ""

    var onDetailExpandedChanged: ((Boolean) -> Unit)? = null

    private fun notifyDetailExpanded() {
        onDetailExpandedChanged?.invoke(activeTag.isNotEmpty())
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
    // TOOL STRIP (ikon menu master, pola sama dengan Text/Objects)
    // ────────────────────────────────────────────────────────────────────────

    private fun buildToolStrip() {
        data class Spec(val tag: String, val label: String, val iconRes: Int)
        val specs = listOf(
            Spec(TOOL_ADJUST,  "Adjust",  R.drawable.ic_sharp_palette_24px),
            Spec(TOOL_EFFECTS, "Effects", R.drawable.ic_nav_wand_24px),
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
                setBackgroundResource(R.drawable.bg_text_tool_item)
                setPadding((6*density).toInt(), (6*density).toInt(), (6*density).toInt(), (4*density).toInt())
                setOnClickListener { onToolClicked(spec.tag) }
            }
            val iconSize = (22 * density).toInt()
            item.addView(ImageView(activity).apply {
                setImageResource(spec.iconRes)
                colorFilter = PorterDuffColorFilter(COLOR_INACTIVE, PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            })
            item.addView(TextView(activity).apply {
                text = spec.label; textSize = 9.5f; maxLines = 1
                gravity = android.view.Gravity.CENTER; setTextColor(COLOR_INACTIVE)
            })
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.width = (52 * density).toInt()
            container.addView(item, lp)
            toolItems[spec.tag] = item
        }
    }

    private fun onToolClicked(tag: String) {
        if (tag == activeTag) deselect() else select(tag)
    }

    fun select(tag: String) {
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
        activeTag = ""
        for (item in toolItems.values) {
            item.isSelected = false
            (item.getChildAt(0) as? ImageView)?.colorFilter =
                PorterDuffColorFilter(COLOR_INACTIVE, PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? TextView)?.setTextColor(COLOR_INACTIVE)
        }
        applyContentVisibility()
    }

    private fun applyContentVisibility() {
        binding.effectContentPanel.visibility =
            if (activeTag.isEmpty()) View.GONE else View.VISIBLE
        binding.effectContentAdjust.visibility =
            if (activeTag == TOOL_ADJUST) View.VISIBLE else View.GONE
        binding.effectContentEffects.visibility =
            if (activeTag == TOOL_EFFECTS) View.VISIBLE else View.GONE
        binding.effectContentBlur.visibility =
            if (activeTag == TOOL_BLUR) View.VISIBLE else View.GONE
        notifyDetailExpanded()
    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP ADJUSTMENT SLIDERS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Setup sliders untuk adjustment (Brightness, Contrast, Saturation).
     */
    private fun setupAdjustmentSliders() {
        // Brightness Slider
        binding.sliderBrightness.addOnChangeListener { _, value, _ ->
            binding.tvAdjustBrightness.text = "Brightness: ${value.toInt()}"
            pixelCanvasView.setAdjustment(
                PixelCanvasView.CanvasAdjustment.BRIGHTNESS,
                value
            )
        }

        // Contrast Slider
        binding.sliderContrast.addOnChangeListener { _, value, _ ->
            binding.tvAdjustContrast.text = "Contrast: ${value.toInt()}"
            pixelCanvasView.setAdjustment(
                PixelCanvasView.CanvasAdjustment.CONTRAST,
                value
            )
        }

        // Saturation Slider
        binding.sliderSaturation.addOnChangeListener { _, value, _ ->
            binding.tvAdjustSaturation.text = "Saturation: ${value.toInt()}"
            pixelCanvasView.setAdjustment(
                PixelCanvasView.CanvasAdjustment.SATURATION,
                value
            )
        }

    }

    /**
     * Reset semua adjustment ke nilai default (0).
     */
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
    // SETUP EFFECT CHIPS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Setup chips untuk efek (Vignette, Noise, Filter).
     */
    private fun setupEffectChips() {
        // Vignette Effect
        binding.chipEffectVignette.setOnCheckedChangeListener { _, checked ->
            pixelCanvasView.setEffectEnabled(
                PixelCanvasView.CanvasEffect.VIGNETTE,
                checked
            )
            if (checked) {
                showSnackbar("Efek Vignette diaktifkan")
            }
        }

        // Noise Effect
        binding.chipEffectNoise.setOnCheckedChangeListener { _, checked ->
            pixelCanvasView.setEffectEnabled(
                PixelCanvasView.CanvasEffect.NOISE,
                checked
            )
            if (checked) {
                showSnackbar("Efek Noise diaktifkan")
            }
        }

        // Filter Effect (Monochrome)
        binding.chipEffectFilter.setOnCheckedChangeListener { _, checked ->
            pixelCanvasView.setEffectEnabled(
                PixelCanvasView.CanvasEffect.FILTER,
                checked
            )
            if (checked) {
                showSnackbar("Efek Filter (Monochrome) diaktifkan")
            }
        }

    }

    /**
     * Reset semua efek (matikan semua).
     */
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
    // SETUP BLUR SLIDER
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Setup slider untuk blur effect.
     */
    private fun setupBlurSlider() {
        binding.sliderBlurRadius.addOnChangeListener { _, value, _ ->
            binding.tvBlurRadius.text = "Blur Radius: ${value.toInt()}"
            pixelCanvasView.setAdjustment(
                PixelCanvasView.CanvasAdjustment.BLUR,
                value
            )
        }

    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP CLOSE BUTTON
    // ────────────────────────────────────────────────────────────────────────


    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Refresh UI effects menu.
     * Sinkronkan state UI dengan state canvas saat ini.
     */
    fun refreshUI() {
        // Tool strip selalu tampil; konten detail hanya saat tool dipilih.
        binding.effectToolStripInclude.effectToolStripScroll.visibility = View.VISIBLE
        if (activeTag.isEmpty()) binding.effectContentPanel.visibility = View.GONE
        else applyContentVisibility()

        // Sinkronkan adjustment sliders
        val brightness = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS) ?: 0f
        val contrast = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST) ?: 0f
        val saturation = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION) ?: 0f
        val blur = pixelCanvasView.getAdjustment(PixelCanvasView.CanvasAdjustment.BLUR) ?: 0f

        binding.sliderBrightness.value = brightness
        binding.sliderContrast.value = contrast
        binding.sliderSaturation.value = saturation
        binding.sliderBlurRadius.value = blur

        binding.tvAdjustBrightness.text = "Brightness: ${brightness.toInt()}"
        binding.tvAdjustContrast.text = "Contrast: ${contrast.toInt()}"
        binding.tvAdjustSaturation.text = "Saturation: ${saturation.toInt()}"
        binding.tvBlurRadius.text = "Blur Radius: ${blur.toInt()}"

        // Sinkronkan effect chips
        binding.chipEffectVignette.isChecked = pixelCanvasView.isEffectEnabled(
            PixelCanvasView.CanvasEffect.VIGNETTE
        )
        binding.chipEffectNoise.isChecked = pixelCanvasView.isEffectEnabled(
            PixelCanvasView.CanvasEffect.NOISE
        )
        binding.chipEffectFilter.isChecked = pixelCanvasView.isEffectEnabled(
            PixelCanvasView.CanvasEffect.FILTER
        )
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
        val hasAdjustments = binding.sliderBrightness.value != 0f ||
                binding.sliderContrast.value != 0f ||
                binding.sliderSaturation.value != 0f ||
                binding.sliderBlurRadius.value != 0f

        val hasEffects = binding.chipEffectVignette.isChecked ||
                binding.chipEffectNoise.isChecked ||
                binding.chipEffectFilter.isChecked

        return hasAdjustments || hasEffects
    }
}
