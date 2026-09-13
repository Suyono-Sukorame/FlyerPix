package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.ActivityResultLauncher
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.databinding.DialogFontPickerBinding
import com.flyerpix.editor.font.FontItem
import com.flyerpix.editor.font.FontManager
import com.flyerpix.editor.font.FontPickerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private var folderFontLauncher: ActivityResultLauncher<Uri?>? = null
    private var fontDialog: Dialog? = null
    private var dialogPreview: android.widget.TextView? = null
    private var dialogRefresh: (() -> Unit)? = null

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

    /**
     * Set folder font launcher (OpenDocumentTree) dari Activity.
     */
    fun setFolderFontLauncher(launcher: ActivityResultLauncher<Uri?>) {
        folderFontLauncher = launcher
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
            val layer = pixelCanvasView.selectedLayer as? TextLayer
            if (layer != null) {
                applyFont(layer, fontItem)
                pixelCanvasView.invalidate()
            }
        }
        binding.textPropertyPanelInclude.fontPanel.rvFontPicker.adapter = fontPickerAdapter
    }

    fun openFontPicker(textLayer: TextLayer) {
        val dialogBinding = DialogFontPickerBinding.inflate(LayoutInflater.from(activity))
        val originalTypeface = textLayer.typeface
        val originalFontName = textLayer.fontName
        var confirmed = false
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
            .setView(dialogBinding.root)
            .create()
        fontDialog = dialog

        val dialogAdapter = FontPickerAdapter(FontManager.getFonts()) { fontItem ->
            applyFont(textLayer, fontItem)
            FontManager.recordRecent(activity, fontItem.name)
            dialogBinding.tvFontPreview.typeface = fontItem.typeface
            dialogBinding.tvSelectedFontName.text = fontItem.name
            pixelCanvasView.invalidate()
        }
        dialogPreview = dialogBinding.tvFontPreview
        dialogBinding.rvFontPicker.layoutManager = LinearLayoutManager(activity)
        dialogBinding.rvFontPicker.adapter = dialogAdapter
        dialogBinding.tvFontPreview.text = textLayer.text.ifBlank { "New Text" }
        dialogBinding.tvFontPreview.typeface = originalTypeface
        val initialFont = FontManager.findFont(textLayer.fontName)
            ?: FontManager.getFonts().find { fontItem -> fontItem.typeface == originalTypeface }
        dialogBinding.tvSelectedFontName.text = initialFont?.name ?: "Current font"
        initialFont?.name?.let { dialogAdapter.setSelectedFont(it) }

        var activeFontFilter: (FontItem) -> Boolean = { it.category != "My Fonts" }
        var activeFontLabel = "Basic"

        fun showFonts(filter: (com.flyerpix.editor.font.FontItem) -> Boolean, label: String) {
            activeFontFilter = filter
            activeFontLabel = label
            val query = dialogBinding.searchFont.query?.toString()?.trim().orEmpty()
            val fonts = FontManager.getFonts().filter(filter).filter { font ->
                query.isBlank() || font.name.contains(query, ignoreCase = true)
            }
            dialogBinding.tvFontCategory.text = label.uppercase()
            dialogAdapter.updateFonts(fonts)
            textLayer.fontName?.let { dialogAdapter.setSelectedFont(it) }
        }
        dialogRefresh = { showFonts(activeFontFilter, activeFontLabel) }

        dialogBinding.searchFont.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                showFonts(activeFontFilter, activeFontLabel)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                showFonts(activeFontFilter, activeFontLabel)
                return true
            }
        })

        fun selectTab(tab: android.view.View, filter: (com.flyerpix.editor.font.FontItem) -> Boolean, label: String) {
            dialogBinding.tabFonts.isSelected = false
            dialogBinding.tabMyFonts.isSelected = false
            dialogBinding.tabRecent.isSelected = false
            dialogBinding.tabFonts.setTextColor(0xFF616161.toInt())
            dialogBinding.tabMyFonts.setTextColor(0xFF616161.toInt())
            dialogBinding.tabRecent.setTextColor(0xFF616161.toInt())
            tab.isSelected = true
            (tab as android.widget.TextView).setTextColor(0xFF1769FF.toInt())
            showFonts(filter, label)
        }

        dialogBinding.tabFonts.setOnClickListener {
            selectTab(it, { font -> font.category != "My Fonts" }, "Basic")
        }
        dialogBinding.tabMyFonts.setOnClickListener {
            selectTab(it, { font -> font.category == "My Fonts" }, "My Fonts")
        }
        dialogBinding.tabRecent.setOnClickListener {
            selectTab(it, { font -> FontManager.getRecentFonts().any { recent -> recent.name == font.name } }, "Recent")
        }
        dialogBinding.btnAddCustomFont.setOnClickListener { openCustomFontPicker() }
        dialogBinding.btnAddCustomFontFolder.setOnClickListener { openFolderFontPicker() }
        dialogBinding.btnFontCancel.setOnClickListener {
            textLayer.typeface = originalTypeface
            textLayer.fontName = originalFontName
            pixelCanvasView.invalidate()
            dialog.dismiss()
        }
        dialogBinding.btnFontOk.setOnClickListener {
            confirmed = true
            dialog.dismiss()
        }
        selectTab(dialogBinding.tabFonts, { font -> font.category != "My Fonts" }, "Basic")
        dialog.setOnDismissListener {
            if (!confirmed) {
                textLayer.typeface = originalTypeface
                textLayer.fontName = originalFontName
                pixelCanvasView.invalidate()
            }
            fontDialog = null
            dialogPreview = null
            dialogRefresh = null
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.94f).toInt(),
            (activity.resources.displayMetrics.heightPixels * 0.90f).toInt()
        )
    }

    /**
     * Setup tombol untuk menambahkan custom font.
     */
    private fun setupCustomFontButton() {
        binding.textPropertyPanelInclude.fontPanel.btnAddCustomFont.setOnClickListener {
            openCustomFontPicker()
        }
        binding.textPropertyPanelInclude.fontPanel.btnAddCustomFontFolder.setOnClickListener {
            openFolderFontPicker()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // CUSTOM FONT HANDLING
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Membuka file picker untuk memilih satu atau lebih font custom (.ttf atau .otf).
     */
    private fun openCustomFontPicker() {
        customFontLauncher?.launch("font/*")
    }

    /**
     * Membuka folder picker (OpenDocumentTree) untuk mengimpor seluruh font dalam folder.
     */
    private fun openFolderFontPicker() {
        folderFontLauncher?.launch(null)
    }

    /**
     * Memproses satu atau lebih font custom yang dipilih dari file picker.
     * Method ini dipanggil dari Activity setelah customFontLauncher mengembalikan hasil.
     */
    fun handleCustomFontResults(uris: List<Uri>) {
        val imported = FontManager.loadFontsFromUris(activity, uris)
        onFontsImported(imported)
        if (imported.isNotEmpty()) {
            val last = imported.last()
            fontPickerAdapter.setSelectedFont(last.name)
            applyFontToSelectedLayer(last)
            showSnackbar(
                if (imported.size > 1) {
                    "${imported.size} fonts added to 'My Fonts'!"
                } else {
                    "Font '${last.name}' added to 'My Fonts'!"
                }
            )
        } else {
            showSnackbar("Failed to load font. Make sure the file is .ttf or .otf format.")
        }
    }

    /**
     * Memproses seluruh font .ttf / .otf dalam folder yang dipilih.
     * Nama font hasil import diberi prefix nama folder (mis. "FolderX - FontY").
     */
    fun handleFolderFontResult(folderUri: Uri) {
        val imported = FontManager.loadFontsFromFolder(activity, folderUri)
        handleFolderFontImportResult(imported)
    }

    suspend fun importFontsFromFolder(folderUri: Uri): Int {
        return withContext(Dispatchers.IO) {
            FontManager.loadFontsFromFolder(activity, folderUri)
        }
    }

    fun handleFolderFontImportResult(imported: Int) {
        if (imported > 0) {
            onFontsImported(emptyList())
            showSnackbar("$imported fonts imported from folder to 'My Fonts'!")
        } else {
            showSnackbar("No .ttf/.otf fonts found in that folder.")
        }
    }

    /**
     * Menyinkronkan UI (adapter + dialog) setelah sejumlah font berhasil diimpor.
     */
    private fun onFontsImported(imported: List<FontItem>) {
        fontPickerAdapter.updateFonts(FontManager.getFonts())
        dialogRefresh?.invoke()
        if (imported.isNotEmpty()) {
            FontManager.recordRecent(activity, imported.last().name)
            dialogPreview?.typeface = imported.last().typeface
        }
    }

    private fun applyFontToSelectedLayer(fontItem: FontItem) {
        val selectedLayer = pixelCanvasView.selectedLayer as? TextLayer
        if (selectedLayer != null) {
            applyFont(selectedLayer, fontItem)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // APPLY FONT TO TEXT LAYER
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Menerapkan typeface ke TextLayer yang sedang dipilih.
     */
    private fun applyFont(textLayer: TextLayer, fontItem: FontItem) {
        if (textLayer.isLocked) return
        textLayer.typeface = fontItem.typeface
        textLayer.fontName = fontItem.name
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
            val matchingFont = FontManager.findFont(textLayer.fontName)
                ?: FontManager.getFonts().find { fontItem ->
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
            val layer = pixelCanvasView.selectedLayer as? TextLayer
            if (layer != null) applyFont(layer, defaultFont)
            fontPickerAdapter.setSelectedFont(defaultFont.name)
            showSnackbar("Font reset to ${defaultFont.name}")
        }
    }
}
