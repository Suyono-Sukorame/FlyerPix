package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.ui.adapter.GradientPickerAdapter
import com.flyerpix.editor.ui.dialog.ColorPickerDialog

/**
 * Controller untuk mengelola menu Canvas (Background, Size, Grid, dll).
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi panel canvas menu
 * - Mengatur background kanvas (Transparent, Solid Color, Gradient, Image)
 * - Mengelola swatches warna solid
 * - Mengatur ukuran dan rasio aspek kanvas
 */
class CanvasMenuController(
    private val activity: Activity,
    private val binding: ActivityEditorBinding,
    private val pixelCanvasView: PixelCanvasView,
    private val fragmentManager: FragmentManager,
    private val showSnackbar: (String) -> Unit,
    private val updateCanvasAspectRatio: (Int, Int) -> Unit
) {

    private lateinit var bgGradientAdapter: GradientPickerAdapter
    private var bgGalleryLauncher: ActivityResultLauncher<String>? = null
    private var onCameraRequested: (() -> Unit)? = null

    /**
     * Set launcher untuk memilih gambar dari galeri.
     */
    fun setBgGalleryLauncher(launcher: ActivityResultLauncher<String>) {
        bgGalleryLauncher = launcher
    }

    /**
     * Set callback untuk membuka kamera.
     */
    fun setOnCameraRequested(callback: () -> Unit) {
        onCameraRequested = callback
    }

    /**
     * Inisialisasi semua kontrol canvas menu.
     */
    fun initialize() {
        setupCanvasBgSwatches()
        setupBackgroundModeChips()
        setupGradientPicker()
        setupCustomColorButtons()
        setupImageBackgroundButtons()
        restoreCanvasBgMode()
    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP BACKGROUND MODE CHIPS
    // ────────────────────────────────────────────────────────────────────────

    private fun setupBackgroundModeChips() {
        val canvasBgModes = listOf(
            R.id.chipBgTransparent to binding.containerBgTransparent,
            R.id.chipBgSolid to binding.containerBgSolid,
            R.id.chipBgGradient to binding.containerBgGradient,
            R.id.chipBgImage to binding.containerBgImage
        )

        binding.cgCanvasBgMode.setOnCheckedChangeListener { _, checkedId ->
            canvasBgModes.forEach { (id, section) ->
                section.visibility = if (id == checkedId) View.VISIBLE else View.GONE
            }
            when (checkedId) {
                R.id.chipBgTransparent -> pixelCanvasView.setTransparentBackground()
                R.id.chipBgSolid -> {
                    val color = pixelCanvasView.canvasBackgroundColor
                    pixelCanvasView.setColorBackground(color)
                }
                R.id.chipBgGradient -> {
                    pixelCanvasView.canvasBackground.gradient?.let {
                        pixelCanvasView.setGradientBackground(it)
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP COLOR SWATCHES
    // ────────────────────────────────────────────────────────────────────────

    private fun setupCanvasBgSwatches() {
        val colors = listOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF212121.toInt(), 0xFFB0BEC5.toInt(),
            0xFF00E5FF.toInt(), 0xFF2979FF.toInt(), 0xFF651FFF.toInt(), 0xFFAA00FF.toInt(),
            0xFFF50057.toInt(), 0xFFE53935.toInt(), 0xFFFF6D00.toInt(), 0xFFFFD600.toInt(),
            0xFF00E676.toInt(), 0xFF00BFA5.toInt(), 0xFFFFF9C4.toInt(), 0xFFBBDEFB.toInt(),
            0xFFC8E6C9.toInt(), 0xFFFFCCBC.toInt()
        )

        binding.llBgSolidSwatches.removeAllViews()
        val size = (30 * activity.resources.displayMetrics.density).toInt()
        val margin = (3 * activity.resources.displayMetrics.density).toInt()

        for (color in colors) {
            val swatch = View(activity).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    val light = color == Color.WHITE || color == 0xFFFFF9C4.toInt()
                    setStroke(
                        if (light) (1.5f * activity.resources.displayMetrics.density).toInt() else 0,
                        if (light) Color.parseColor("#B0BEC5") else Color.TRANSPARENT
                    )
                }
                setOnClickListener {
                    pixelCanvasView.setColorBackground(color)
                }
            }
            binding.llBgSolidSwatches.addView(swatch)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP GRADIENT PICKER
    // ────────────────────────────────────────────────────────────────────────

    private fun setupGradientPicker() {
        bgGradientAdapter = GradientPickerAdapter(GradientColor.PRESETS) { preset ->
            pixelCanvasView.setGradientBackground(preset)
        }
        binding.rvBgGradients.adapter = bgGradientAdapter
    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP CUSTOM COLOR BUTTONS
    // ────────────────────────────────────────────────────────────────────────

    private fun setupCustomColorButtons() {
        binding.btnCustomSolidColor.setOnClickListener {
            val currentColor = pixelCanvasView.canvasBackgroundColor
            ColorPickerDialog
                .newInstance(initialColor = currentColor)
                .show(fragmentManager, "CanvasSolidColorPicker")
        }

        binding.btnCustomGradient.setOnClickListener {
            val currentGrad = pixelCanvasView.canvasBackground.gradient
                ?: GradientColor.PRESETS[0]
            ColorPickerDialog
                .newInstance(initialGradient = currentGrad)
                .show(fragmentManager, "CanvasGradientPicker")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // SETUP IMAGE BACKGROUND BUTTONS
    // ────────────────────────────────────────────────────────────────────────

    private fun setupImageBackgroundButtons() {
        binding.btnBgGallery.setOnClickListener {
            bgGalleryLauncher?.launch("image/*")
        }

        binding.btnBgCamera.setOnClickListener {
            onCameraRequested?.invoke()
        }

        binding.btnBgClearImage.setOnClickListener {
            pixelCanvasView.clearImageBackground()
            showSnackbar("Gambar latar belakang dihapus")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // RESTORE BACKGROUND MODE
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Memulihkan chip background mode sesuai dengan state kanvas saat ini.
     */
    private fun restoreCanvasBgMode() {
        when (pixelCanvasView.canvasBackground.mode) {
            CanvasBackgroundMode.TRANSPARENT -> {
                binding.chipBgTransparent.isChecked = true
            }
            CanvasBackgroundMode.GRADIENT -> {
                binding.chipBgGradient.isChecked = true
            }
            CanvasBackgroundMode.IMAGE -> {
                binding.chipBgImage.isChecked = true
            }
            else -> binding.chipBgSolid.isChecked = true
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Refresh UI canvas menu jika diperlukan.
     */
    fun refreshUI() {
        restoreCanvasBgMode()
    }

    /**
     * Menampilkan dialog ukuran kanvas.
     */
    fun showImageSizeDialog() {
        com.flyerpix.editor.ui.dialog.ImageSizeDialog.show(
            activity,
            pixelCanvasView.canvasWidth,
            pixelCanvasView.canvasHeight
        ) { width, height ->
            updateCanvasAspectRatio(width, height)
        }
    }
}
