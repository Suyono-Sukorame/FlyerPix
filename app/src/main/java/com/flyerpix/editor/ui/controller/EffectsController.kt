package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.view.View
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.databinding.ActivityEditorBinding

/**
 * Controller untuk mengelola efek kanvas (Vignette, Noise, Filter, Adjustments, Blur).
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi panel effects menu
 * - Mengatur category chips (Adjust, Effects, Blur)
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

    /**
     * Inisialisasi semua kontrol effects menu.
     */
    fun initialize() {
        setupCategoryChips()
        setupAdjustmentSliders()
        setupEffectChips()
        setupBlurSlider()
    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP CATEGORY CHIPS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Setup chips untuk memilih kategori efek (Adjust, Effects, Blur).
     */
    private fun setupCategoryChips() {
        val contentMap = mapOf(
            R.id.chipFxAdjust  to binding.effectContentAdjust,
            R.id.chipFxEffects to binding.effectContentEffects,
            R.id.chipFxBlur    to binding.effectContentBlur
        )

        // Tampilkan content Adjust sebagai default
        showContent(contentMap, R.id.chipFxAdjust)

        binding.cgEffectCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            showContent(contentMap, checkedIds[0])
        }
    }

    /**
     * Menampilkan content sesuai dengan chip yang dipilih.
     */
    private fun showContent(contentMap: Map<Int, View>, chipId: Int) {
        contentMap.forEach { (id, view) ->
            view.visibility = if (id == chipId) View.VISIBLE else View.GONE
        }
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
