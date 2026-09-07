package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

    companion object {
        const val TOOL_BG = "canvas_bg"

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY   = 0xFF616161.toInt()
    }

    private val toolItems = LinkedHashMap<String, ViewGroup>()
    var activeTag: String = ""

    /**
     * Dipanggil saat status detail (buka/tutup) berubah, agar activity bisa
     * mengekspansi panel dan menyembunyikan/memunculkan kembali nav.
     */
    var onDetailExpandedChanged: ((Boolean) -> Unit)? = null

    private fun notifyDetailExpanded() {
        onDetailExpandedChanged?.invoke(activeTag.isNotEmpty())
    }

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
        buildToolStrip()
        setupCanvasBgSwatches()
        setupBackgroundModeChips()
        setupGradientPicker()
        setupCustomColorButtons()
        setupImageBackgroundButtons()
        restoreCanvasBgMode()
    }

    // ────────────────────────────────────────────────────────────────────────
    // TOOL STRIP (ikon menu master, pola sama dengan Text/Objects)
    // ────────────────────────────────────────────────────────────────────────

    private fun buildToolStrip() {
        data class Spec(val tag: String, val label: String, val iconRes: Int)
        val specs = listOf(
            Spec(TOOL_BG, "Background", R.drawable.ic_background_24px)
        )
        val density = activity.resources.displayMetrics.density
        val container = binding.canvasToolStripInclude.canvasToolStripContainer
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
                colorFilter = PorterDuffColorFilter(COLOR_GRAY, PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            })
            item.addView(TextView(activity).apply {
                text = spec.label; textSize = 9.5f; maxLines = 1
                gravity = android.view.Gravity.CENTER; setTextColor(COLOR_GRAY)
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
            val c = if (sel) COLOR_ACTIVE else COLOR_GRAY
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
                PorterDuffColorFilter(COLOR_GRAY, PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? TextView)?.setTextColor(COLOR_GRAY)
        }
        applyContentVisibility()
    }

    private fun applyContentVisibility() {
        binding.canvasContentPanel.visibility =
            if (activeTag.isEmpty()) View.GONE else View.VISIBLE
        restoreCanvasBgMode()
        notifyDetailExpanded()
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
        binding.canvasToolStripInclude.canvasToolStripScroll.visibility = View.VISIBLE
        if (activeTag.isEmpty()) binding.canvasContentPanel.visibility = View.GONE
        else applyContentVisibility()
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
