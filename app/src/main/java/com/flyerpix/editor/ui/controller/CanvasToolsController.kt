package com.flyerpix.editor.ui.controller

import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.databinding.ActivityEditorBinding

/**
 * Controller untuk tools kanvas yang berjalan langsung dari Activity
 * (Eyedropper, Crop, dan Palette/Color Picker).
 *
 * Bertanggung jawab untuk:
 * - Mengelola mode eyedropper (pipet warna) dan menerapkan warna ke layer/background.
 * - Mengelola mode crop kanvas (confirm/cancel, overlay).
 * - Mengelola palette warna & color picker untuk layer dan background kanvas.
 */
class CanvasToolsController(
    private val activity: AppCompatActivity,
    private val binding: ActivityEditorBinding,
    private val pixelCanvasView: PixelCanvasView,
    private val showSnackbar: (String) -> Unit
) {
    private val fragmentManager: FragmentManager
        get() = activity.supportFragmentManager

    /**
     * Inisialisasi seluruh tools kanvas (eyedropper, crop, palette).
     */
    fun initialize() {
        setupContextFabVisibility()
        initializeEyedropper()
        initCrop()
        initPalette()
        updateContextFabVisibility()
    }

    private fun setupContextFabVisibility() {
        val previousSelectedListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            previousSelectedListener?.invoke(layer)
            updateContextFabVisibility()
        }

        val previousLayersChangedListener = pixelCanvasView.onLayersChangedListener
        pixelCanvasView.onLayersChangedListener = {
            previousLayersChangedListener?.invoke()
            updateContextFabVisibility()
        }
    }

    /**
     * Update visibility FAB berdasarkan menu aktif dan kondisi canvas.
     * 
     * Aturan:
     * - paletteFab & eyedropperFab: hanya muncul di menu Text & Object (saat ada layer terpilih)
     * - cropFab: hanya muncul di menu Canvas
     */
    fun updateContextFabVisibility() {
        val currentMenu = binding.bottomNavigation.selectedItemId
        val selectedLayer = pixelCanvasView.selectedLayer
        
        // Palette & Eyedropper: hanya untuk Text & Object menu dengan layer terpilih
        val showColorTools = selectedLayer != null && 
                            (currentMenu == com.flyerpix.editor.R.id.nav_text || 
                             currentMenu == com.flyerpix.editor.R.id.nav_object) &&
                            !pixelCanvasView.isEyedropperMode &&
                            !binding.cropCanvasOverlay.isActive
        
        binding.paletteFab.visibility = if (showColorTools) View.VISIBLE else View.GONE
        binding.eyedropperFab.visibility = if (showColorTools) View.VISIBLE else View.GONE
        
        // Crop: hanya untuk Canvas menu
        val showCrop = currentMenu == com.flyerpix.editor.R.id.nav_canvas &&
                       !pixelCanvasView.isEyedropperMode &&
                       !binding.cropCanvasOverlay.isActive
        
        binding.cropFab.visibility = if (showCrop) View.VISIBLE else View.GONE
    }

    // ────────────────────────────────────────────────────────────────────────
    // EYEDROPPER (Prompt 42)
    // ────────────────────────────────────────────────────────────────────────

    private fun initializeEyedropper() {
        binding.eyedropperFab.setOnClickListener {
            val canvas = pixelCanvasView
            if (canvas.isEyedropperMode) return@setOnClickListener

            // Masuk mode eyedropper
            canvas.isEyedropperMode = true
            binding.eyedropperOverlay.isActive = true
            binding.eyedropperOverlay.visibility = View.VISIBLE
            updateContextFabVisibility()

            // Tangkap bitmap canvas dan refresh saat layer berubah
            canvas.refreshCapturedBitmap()

            // Set callback pembacaan warna
            canvas.onEyedropperColorListener = { color ->
                binding.eyedropperOverlay.update(
                    canvas.touchEventX, canvas.touchEventY, color
                )
                if (!canvas.isEyedropperMode) {
                    // Touch UP — terapkan warna ke layer yang dipilih
                    applyEyedropperColor(color)
                    binding.eyedropperOverlay.isActive = false
                    binding.eyedropperOverlay.visibility = View.GONE
                    canvas.onEyedropperColorListener = null
                    updateContextFabVisibility()
                }
            }
        }
    }

    private fun applyEyedropperColor(color: Int) {
        val layer = pixelCanvasView.selectedLayer
        when (layer) {
            is com.flyerpix.editor.canvas.model.TextLayer -> {
                if (!layer.isLocked) {
                    layer.textColor = color
                    pixelCanvasView.invalidate()
                }
            }
            is com.flyerpix.editor.canvas.model.ShapeLayer -> {
                if (!layer.isLocked) {
                    layer.fillColor = color
                    pixelCanvasView.invalidate()
                }
            }
            is com.flyerpix.editor.canvas.model.PenLayer -> {
                if (!layer.isLocked) {
                    layer.strokeColor = color
                    pixelCanvasView.invalidate()
                }
            }
            is com.flyerpix.editor.canvas.model.ArrowLayer -> {
                if (!layer.isLocked) {
                    layer.headColor = color
                    pixelCanvasView.invalidate()
                }
            }
            else -> {
                pixelCanvasView.canvasBackgroundColor = color
            }
        }
    }

    fun isEyedropperActive(): Boolean = pixelCanvasView.isEyedropperMode

    /**
     * Menonaktifkan mode eyedropper dan menyembunyikan overlay.
     */
    fun disableEyedropper() {
        pixelCanvasView.isEyedropperMode = false
        binding.eyedropperOverlay.isActive = false
        binding.eyedropperOverlay.visibility = View.GONE
        pixelCanvasView.onEyedropperColorListener = null
        updateContextFabVisibility()
    }

    // ────────────────────────────────────────────────────────────────────────
    // CROP CANVAS (Prompt 46)
    // ────────────────────────────────────────────────────────────────────────

    private fun initCrop() {
        binding.cropFab.setOnClickListener {
            enterCropMode()
        }

        binding.btnCropConfirm.setOnClickListener {
            val crop = binding.cropCanvasOverlay.cropRect
            binding.cropCanvasOverlay.onCropConfirmed?.invoke(crop.left, crop.top, crop.right, crop.bottom)
        }

        binding.btnCropCancel.setOnClickListener {
            exitCropMode()
        }
    }

    fun isCropActive(): Boolean = binding.cropCanvasOverlay.isActive

    private fun enterCropMode() {
        val canvas = pixelCanvasView
        val vp = canvas.viewportRect
        if (vp.width() <= 0f || vp.height() <= 0f) return

        binding.cropCanvasOverlay.canvasBounds = android.graphics.RectF(vp)
        binding.cropCanvasOverlay.resetCropRect()
        binding.cropCanvasOverlay.isActive = true
        binding.cropCanvasOverlay.visibility = View.VISIBLE

        updateContextFabVisibility()

        // Tampilkan toolbar confirm/cancel
        binding.toolbarConfirmCancel.visibility = View.VISIBLE
    }

    /**
     * Keluar dari mode crop dan mengembalikan FAB serta toolbar ke keadaan semula.
     */
    fun exitCropMode() {
        binding.cropCanvasOverlay.isActive = false
        binding.cropCanvasOverlay.visibility = View.GONE

        updateContextFabVisibility()

        binding.toolbarConfirmCancel.visibility = View.GONE
    }

    // ────────────────────────────────────────────────────────────────────────
    // PALETTE & COLOR PICKER
    // ────────────────────────────────────────────────────────────────────────

    private fun initPalette() {
        binding.paletteFab.setOnClickListener {
            if (pixelCanvasView.selectedLayer != null) {
                launchColorPicker()
            } else {
                com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.show(fragmentManager, pixelCanvasView)
            }
        }
        fragmentManager.setFragmentResultListener(
            com.flyerpix.editor.ui.dialog.ColorPickerDialog.RESULT_KEY, this.activity
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
                    val currentLayer = pixelCanvasView.selectedLayer
                    if (currentLayer is com.flyerpix.editor.canvas.model.TextLayer && !currentLayer.isLocked) {
                        pixelCanvasView.runRecordedAction("Ubah Gradien Teks") {
                            currentLayer.gradient = gradient
                            currentLayer.gradientEnabled = true
                        }
                        pixelCanvasView.invalidate()
                    } else if (currentLayer == null) {
                        pixelCanvasView.setGradientBackground(gradient)
                    }
                }
            } else {
                val color = bundle.getInt(
                    com.flyerpix.editor.ui.dialog.ColorPickerDialog.EXTRA_COLOR,
                    Color.WHITE
                )
                val layer = pixelCanvasView.selectedLayer
                if (layer != null) {
                    pixelCanvasView.runRecordedAction("Ubah Warna Layer") {
                        when (layer) {
                            is com.flyerpix.editor.canvas.model.TextLayer -> {
                                if (!layer.isLocked) layer.textColor = color
                            }
                            is com.flyerpix.editor.canvas.model.ShapeLayer -> {
                                if (!layer.isLocked) layer.fillColor = color
                            }
                            is com.flyerpix.editor.canvas.model.PenLayer -> {
                                if (!layer.isLocked) layer.strokeColor = color
                            }
                            is com.flyerpix.editor.canvas.model.ArrowLayer -> {
                                if (!layer.isLocked) layer.headColor = color
                            }
                            else -> {}
                        }
                    }
                    pixelCanvasView.invalidate()
                } else {
                    pixelCanvasView.setColorBackground(color)
                }
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
            .show(fragmentManager, com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG)
    }
}
