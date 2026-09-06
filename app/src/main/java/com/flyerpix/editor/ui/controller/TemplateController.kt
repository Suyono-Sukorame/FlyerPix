package com.flyerpix.editor.ui.controller

import android.app.Activity
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.template.TemplatePreset
import com.flyerpix.editor.template.TemplatePresetAdapter

/**
 * Controller untuk mengelola template presets dan project actions.
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi template preset carousel
 * - Menerapkan template ke canvas
 * - Membuka project manager dari preset My Projects
 */
class TemplateController(
    private val activity: Activity,
    private val binding: ActivityEditorBinding,
    private val pixelCanvasView: PixelCanvasView,
    private val showSnackbar: (String) -> Unit,
    private val onProjectOpen: () -> Unit,
    private val onProjectSave: () -> Unit
) {

    private lateinit var templateAdapter: TemplatePresetAdapter

    /**
     * Inisialisasi template presets dan project actions.
     */
    fun initialize() {
        setupTemplateCarousel()
        applyDefaultTemplateIfNeeded()
    }

    // ────────────────────────────────────────────────────────────────────────
    // TEMPLATE CAROUSEL SETUP
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Setup carousel template presets dengan adapter.
     */
    private fun setupTemplateCarousel() {
        val presets = TemplatePreset.getBuiltinPresets()
        
        templateAdapter = TemplatePresetAdapter(presets) { preset ->
            handlePresetClick(preset)
        }
        
        binding.rvTemplatePresets.adapter = templateAdapter
    }

    /**
     * Handle klik pada template preset.
     */
    private fun handlePresetClick(preset: TemplatePreset) {
        if (preset.isMyProjects) {
            // Preset khusus "My Projects" membuka project manager
            onProjectOpen()
        } else {
            // Apply preset normal ke canvas
            preset.applyToCanvas(pixelCanvasView)
            showSnackbar("Preset '${preset.title}' diterapkan")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // DEFAULT TEMPLATE APPLICATION
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Terapkan template default jika canvas kosong saat pertama dibuka.
     */
    private fun applyDefaultTemplateIfNeeded() {
        pixelCanvasView.post {
            if (pixelCanvasView.layers.isEmpty() && 
                (activity.intent.getStringExtra("image").isNullOrEmpty())) {
                TemplatePreset.applyDefaultPixelLabState(pixelCanvasView)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Refresh UI template controller.
     */
    fun refreshUI() {
        // Carousel selalu tampil sebagai konten utama Presets.
    }

    /**
     * Mendapatkan adapter template (untuk keperluan testing atau eksternal).
     */
    fun getTemplateAdapter(): TemplatePresetAdapter = templateAdapter

    /**
     * Mendapatkan jumlah template yang tersedia.
     */
    fun getTemplateCount(): Int = TemplatePreset.getBuiltinPresets().size

    /**
     * Menerapkan template tertentu berdasarkan index.
     */
    fun applyTemplateByIndex(index: Int) {
        val presets = TemplatePreset.getBuiltinPresets()
        if (index in presets.indices) {
            val preset = presets[index]
            handlePresetClick(preset)
        }
    }

    /**
     * Menerapkan template tertentu berdasarkan title.
     */
    fun applyTemplateByTitle(title: String) {
        val preset = TemplatePreset.getBuiltinPresets().find { it.title == title }
        if (preset != null) {
            handlePresetClick(preset)
        } else {
            showSnackbar("Template '$title' tidak ditemukan")
        }
    }

    /**
     * Reset ke template default (PixelLab default state).
     */
    fun resetToDefaultTemplate() {
        pixelCanvasView.clearLayers()
        TemplatePreset.applyDefaultPixelLabState(pixelCanvasView)
        showSnackbar("Template default diterapkan")
    }

    /**
     * Mengecek apakah canvas kosong (tidak ada layer).
     */
    fun isCanvasEmpty(): Boolean {
        return pixelCanvasView.layers.isEmpty()
    }

    /**
     * Clear canvas dan reset ke view template.
     */
    fun clearAndShowTemplates() {
        pixelCanvasView.clearLayers()
        showSnackbar("Canvas dibersihkan")
    }
}
