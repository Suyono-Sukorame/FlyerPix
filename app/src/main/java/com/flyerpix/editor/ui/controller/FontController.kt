package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.font.FontManager
import com.flyerpix.editor.font.FontPickerAdapter

/**
 * Controller untuk mengelola font picker dan custom fonts.
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi FontManager
 * - Setup font picker RecyclerView dengan adapter
 * - Mengelola custom font launcher
 * - Menerapkan font ke TextLayer yang dipilih
 */
class FontController(
    private val activity: Activity,
    private val binding: ActivityEditorBinding,
    private val pixelCanvasView: PixelCanvasView,
    private val showSnackbar: (String) -> Unit
) {

    private lateinit var fontPickerAdapter: FontPickerAdapter
    private var customFontLauncher: ActivityResultLauncher<String>? = null

    /**
     * Inisialisasi FontManager dan font picker.
     */
    fun initialize() {
        initializeFontManager()
        setupFontPicker()
        setupCustomFontButton()
    }

    /**
     * Set custom font launcher dari Activity.
     * Diperlukan karena ActivityResultLauncher harus di-register di Activity.
     */
    fun setCustomFontLauncher(launcher: ActivityResultLauncher<String>) {
        customFontLauncher = launcher
    }

    // ────────────────────────────────────────────────────────────────────────
    // FONT MANAGER INITIALIZATION
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Inisialisasi FontManager dengan context aplikasi.
     */
    private fun initializeFontManager() {
        FontManager.init(activity)
    }

    // ────────────────────────────────────────────────────────────────────────
    // FONT PICKER SETUP
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Setup font picker RecyclerView dengan adapter dan listener.
     */
    private fun setupFontPicker() {
        fontPickerAdapter = FontPickerAdapter(FontManager.getFonts()) { fontItem ->
            applyFontToSelectedTextLayer(fontItem.typeface)
        }
        binding.textPropertyPanelInclude.fontPanel.rvFontPicker.adapter = fontPickerAdapter
    }

    /**
     * Setup tombol untuk menambahkan custom font.
     */
    private fun setupCustomFontButton() {
        binding.textPropertyPanelInclude.fontPanel.btnAddCustomFont.setOnClickListener {
            openCustomFontPicker()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // CUSTOM FONT HANDLING
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Membuka file picker untuk memilih font custom (.ttf atau .otf).
     */
    private fun openCustomFontPicker() {
        customFontLauncher?.launch("font/*")
    }

    /**
     * Memproses font custom yang dipilih dari file picker.
     * Method ini dipanggil dari Activity setelah customFontLauncher mengembalikan hasil.
     */
    fun handleCustomFontResult(uri: Uri) {
        val fontItem = FontManager.loadFontFromUri(activity, uri)
        
        if (fontItem != null) {
            // Update adapter dengan daftar font terbaru
            fontPickerAdapter.updateFonts(FontManager.getFonts())
            fontPickerAdapter.setSelectedFont(fontItem.name)

            // Terapkan langsung ke layer teks aktif jika ada
            applyFontToSelectedTextLayer(fontItem.typeface)

            showSnackbar("Font '${fontItem.name}' berhasil ditambahkan ke 'My Fonts'!")
        } else {
            showSnackbar("Gagal memuat font. Pastikan file berformat .ttf atau .otf.")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // APPLY FONT TO TEXT LAYER
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menerapkan typeface ke TextLayer yang sedang dipilih.
     */
    private fun applyFontToSelectedTextLayer(typeface: android.graphics.Typeface) {
        val textLayer = pixelCanvasView.selectedLayer as? TextLayer
        if (textLayer != null && !textLayer.isLocked) {
            textLayer.typeface = typeface
            pixelCanvasView.invalidate()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Refresh font picker untuk sinkronisasi dengan font yang dipilih saat ini.
     */
    fun refreshUI() {
        val textLayer = pixelCanvasView.selectedLayer as? TextLayer
        if (textLayer != null) {
            // Cari font yang cocok dengan typeface layer
            val matchingFont = FontManager.getFonts().find { fontItem ->
                fontItem.typeface == textLayer.typeface
            }
            
            if (matchingFont != null) {
                fontPickerAdapter.setSelectedFont(matchingFont.name)
            }
        }
    }

    /**
     * Mendapatkan adapter font picker (untuk keperluan testing atau eksternal).
     */
    fun getFontPickerAdapter(): FontPickerAdapter = fontPickerAdapter

    /**
     * Mendapatkan jumlah font yang tersedia.
     */
    fun getAvailableFontsCount(): Int = FontManager.getFonts().size

    /**
     * Mengecek apakah font dengan nama tertentu sudah ada.
     */
    fun isFontExists(fontName: String): Boolean {
        return FontManager.getFonts().any { it.name == fontName }
    }

    /**
     * Reset font picker ke font default (biasanya Roboto Regular).
     */
    fun resetToDefaultFont() {
        val defaultFont = FontManager.getFonts().firstOrNull()
        if (defaultFont != null) {
            applyFontToSelectedTextLayer(defaultFont.typeface)
            fontPickerAdapter.setSelectedFont(defaultFont.name)
            showSnackbar("Font direset ke ${defaultFont.name}")
        }
    }
}
