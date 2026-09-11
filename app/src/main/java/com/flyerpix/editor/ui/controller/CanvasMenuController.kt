package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.ui.adapter.GradientPickerAdapter
import com.flyerpix.editor.ui.compose.CanvasBgDetailPage
import com.flyerpix.editor.ui.compose.CanvasGridDetailPage
import com.flyerpix.editor.ui.compose.CanvasSizeDetailPage
import com.flyerpix.editor.ui.compose.CanvasSnapDetailPage
import com.flyerpix.editor.ui.dialog.ColorPickerDialog

/**
 * Controller untuk mengelola menu Canvas (Background, Size, Grid, dll).
 *
 * Bertanggung jawab untuk:
 * - Inisialisasi panel canvas menu
 * - Mengatur background kanvas (Transparent, Solid Color, Gradient, Image) via Compose bottom sheet
 * - Mengatur ukuran dan rasio aspek kanvas via Compose bottom sheet
 * - Mengatur Grid dan Magnetic Snap via Compose bottom sheet
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

    private val composeHost: ComposeView? get() = binding.composeThreeDDetail
    private val composeContainer: FrameLayout? get() = binding.composeThreeDSheetContainer
    private var initialBgSnapshot: CanvasBackground? = null
    private var initialGridEnabled: Boolean = false
    private var initialGridSpacing: Float = 32f
    private var initialSnapEnabled: Boolean = true

    companion object {
        const val TOOL_BG   = "canvas_bg"
        const val TOOL_SIZE = "canvas_size"
        const val TOOL_GRID = "canvas_grid"
        const val TOOL_SNAP = "canvas_snap"

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY   = 0xFF616161.toInt()

        const val CANVAS_BG_RESULT_KEY = "canvas_bg_color_picker_result"
    }

    private val toolItems = LinkedHashMap<String, ViewGroup>()
    var activeTag: String = ""

    /**
     * Dipanggil saat status detail (buka/tutup) berubah, agar activity bisa
     * mengekspansi panel dan menyembunyikan/memunculkan kembali nav.
     */
    var onDetailExpandedChanged: ((Boolean) -> Unit)? = null

    /**
     * Callback saat salah satu Compose sheet canvas dibuka/ditutup,
     * untuk menganimasikan navigasi bawah (translationY 56dp) dan fit canvas viewport.
     */
    var onCanvasSettingsOpenChanged: ((Boolean) -> Unit)? = null

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
        setupColorPickerResultListener()
        setupCanvasBgSwatches()
        setupBackgroundModeChips()
        setupGradientPicker()
        setupCustomColorButtons()
        setupImageBackgroundButtons()
        restoreCanvasBgMode()
    }

    private fun setupColorPickerResultListener() {
        val lifecycleOwner = activity as? LifecycleOwner ?: return
        fragmentManager.setFragmentResultListener(CANVAS_BG_RESULT_KEY, lifecycleOwner) { _, bundle ->
            val isGradient = bundle.getBoolean(ColorPickerDialog.EXTRA_IS_GRADIENT, false)
            if (!isGradient) {
                val color = bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE)
                pixelCanvasView.setColorBackground(color)
            } else {
                @Suppress("DEPRECATION")
                val grad = bundle.getSerializable(ColorPickerDialog.EXTRA_GRADIENT) as? GradientColor
                if (grad != null) {
                    pixelCanvasView.setGradientBackground(grad)
                }
            }
            if (activeTag == TOOL_BG && composeHost != null) {
                showComposeBgSheet()
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // TOOL STRIP (ikon menu master, pola sama dengan Text/Objects)
    // ────────────────────────────────────────────────────────────────────────

    private fun buildToolStrip() {
        data class Spec(val tag: String, val label: String, val iconRes: Int)
        val specs = listOf(
            Spec(TOOL_BG,   "Background", R.drawable.ic_background_24px),
            Spec(TOOL_SIZE, "Canvas Size", R.drawable.ic_aspect_ratio_24px),
            Spec(TOOL_GRID, "Grid", R.drawable.ic_grid_on_24px),
            Spec(TOOL_SNAP, "Snap", R.drawable.ic_snap_24px)
        )
        val density = activity.resources.displayMetrics.density
        val container = binding.canvasToolStripInclude.canvasToolStripContainer
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
                colorFilter = PorterDuffColorFilter(COLOR_GRAY, PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            })
            item.addView(TextView(activity).apply {
                text = spec.label; textSize = 11f; maxLines = 1
                gravity = android.view.Gravity.CENTER; setTextColor(COLOR_GRAY)
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
        if (tag == activeTag) deselect(restoreStrip = true) else select(tag)
    }

    /** Highlight sementara untuk tool aksi instan, lalu kembali ke warna default. */
    private fun flashSelection(tag: String) {
        if (activeTag == tag) return
        val item = toolItems[tag] ?: return
        item.isSelected = true
        val c = COLOR_ACTIVE
        (item.getChildAt(0) as? ImageView)?.colorFilter = PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN)
        (item.getChildAt(1) as? TextView)?.setTextColor(c)
        item.postDelayed({
            if (activeTag != tag) {
                item.isSelected = false
                val g = COLOR_GRAY
                (item.getChildAt(0) as? ImageView)?.colorFilter = PorterDuffColorFilter(g, PorterDuff.Mode.SRC_IN)
                (item.getChildAt(1) as? TextView)?.setTextColor(g)
            }
        }, 400)
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

    fun deselect(restoreStrip: Boolean = false) {
        activeTag = ""
        initialBgSnapshot = null
        for (item in toolItems.values) {
            item.isSelected = false
            (item.getChildAt(0) as? ImageView)?.colorFilter =
                PorterDuffColorFilter(COLOR_GRAY, PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? TextView)?.setTextColor(COLOR_GRAY)
        }
        composeContainer?.visibility = View.GONE
        binding.canvasContentPanel.visibility = View.GONE
        if (restoreStrip) {
            binding.canvasMenuPanel.visibility = View.VISIBLE
        }
        onCanvasSettingsOpenChanged?.invoke(false)
        notifyDetailExpanded()
    }

    private fun applyContentVisibility() {
        if (activeTag.isEmpty()) {
            composeContainer?.visibility = View.GONE
            binding.canvasContentPanel.visibility = View.GONE
            binding.canvasMenuPanel.visibility = View.VISIBLE
            onCanvasSettingsOpenChanged?.invoke(false)
            notifyDetailExpanded()
            return
        }

        if (composeHost != null && composeContainer != null) {
            binding.canvasContentPanel.visibility = View.GONE
            binding.canvasMenuPanel.visibility = View.GONE
            composeContainer?.visibility = View.VISIBLE
            composeContainer?.bringToFront()
            onCanvasSettingsOpenChanged?.invoke(true)
            when (activeTag) {
                TOOL_BG -> {
                    initialBgSnapshot = pixelCanvasView.canvasBackground.copy(
                        imageBitmap = pixelCanvasView.canvasBackground.imageBitmap
                    )
                    showComposeBgSheet()
                }
                TOOL_SIZE -> {
                    showComposeSizeSheet()
                }
                TOOL_GRID -> {
                    initialGridEnabled = pixelCanvasView.isGridEnabled
                    initialGridSpacing = pixelCanvasView.gridSpacingDp
                    showComposeGridSheet()
                }
                TOOL_SNAP -> {
                    initialSnapEnabled = pixelCanvasView.isSnapToCenterEnabled
                    showComposeSnapSheet()
                }
                else -> {
                    composeContainer?.visibility = View.GONE
                    binding.canvasMenuPanel.visibility = View.VISIBLE
                    onCanvasSettingsOpenChanged?.invoke(false)
                }
            }
        } else {
            // Fallback ke XML panel legacy jika Compose view tidak tersedia
            binding.canvasContentPanel.visibility = View.VISIBLE
            restoreCanvasBgMode()
        }
        notifyDetailExpanded()
    }

    // ────────────────────────────────────────────────────────────────────────
    // COMPOSE BOTTOM SHEETS
    // ────────────────────────────────────────────────────────────────────────

    private fun showComposeBgSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.canvasContentPanel.visibility = View.GONE
        binding.canvasMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { pixelCanvasView.invalidate() }

        val currentBg = pixelCanvasView.canvasBackground
        host.setContent {
            CanvasBgDetailPage(
                currentMode = currentBg.mode,
                solidColor = pixelCanvasView.canvasBackgroundColor,
                gradient = currentBg.gradient,
                hasImage = currentBg.imageBitmap != null,
                onModeChange = { mode ->
                    when (mode) {
                        CanvasBackgroundMode.TRANSPARENT -> pixelCanvasView.setTransparentBackground()
                        CanvasBackgroundMode.SOLID_COLOR -> {
                            val color = pixelCanvasView.canvasBackgroundColor
                            pixelCanvasView.setColorBackground(color)
                        }
                        CanvasBackgroundMode.GRADIENT -> {
                            val grad = currentBg.gradient ?: GradientColor.PRESETS.first()
                            pixelCanvasView.setGradientBackground(grad)
                        }
                        CanvasBackgroundMode.IMAGE -> {
                            currentBg.imageBitmap?.let { pixelCanvasView.setImageBackground(it) }
                        }
                    }
                },
                onSolidColorChange = { color ->
                    pixelCanvasView.setColorBackground(color)
                },
                onOpenColorPicker = {
                    val currentColor = pixelCanvasView.canvasBackgroundColor
                    ColorPickerDialog.newInstance(
                        initialColor = currentColor,
                        resultKey = CANVAS_BG_RESULT_KEY
                    ).show(fragmentManager, "CanvasBgSolidColorPicker")
                },
                onGradientChange = { grad ->
                    pixelCanvasView.setGradientBackground(grad)
                },
                onOpenGradientPicker = {
                    val currentGrad = currentBg.gradient ?: GradientColor.PRESETS.first()
                    ColorPickerDialog.newInstance(
                        initialGradient = currentGrad,
                        resultKey = CANVAS_BG_RESULT_KEY
                    ).show(fragmentManager, "CanvasBgGradientPicker")
                },
                onGalleryPickRequested = {
                    bgGalleryLauncher?.launch("image/*")
                },
                onCameraRequested = {
                    onCameraRequested?.invoke()
                },
                onRemoveImage = {
                    pixelCanvasView.clearImageBackground()
                    showSnackbar("Background image removed")
                    showComposeBgSheet()
                },
                onReset = {
                    pixelCanvasView.setColorBackground(Color.WHITE)
                    showComposeBgSheet()
                },
                onApply = {
                    pixelCanvasView.runRecordedAction("Change Canvas Background") {}
                    initialBgSnapshot = null
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    initialBgSnapshot?.let { snap ->
                        when (snap.mode) {
                            CanvasBackgroundMode.TRANSPARENT -> pixelCanvasView.setTransparentBackground()
                            CanvasBackgroundMode.SOLID_COLOR -> pixelCanvasView.setColorBackground(snap.solidColor)
                            CanvasBackgroundMode.GRADIENT -> snap.gradient?.let { pixelCanvasView.setGradientBackground(it) }
                            CanvasBackgroundMode.IMAGE -> snap.imageBitmap?.let { pixelCanvasView.setImageBackground(it) } ?: pixelCanvasView.clearImageBackground()
                        }
                    }
                    initialBgSnapshot = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showComposeSizeSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.canvasContentPanel.visibility = View.GONE
        binding.canvasMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { pixelCanvasView.invalidate() }

        host.setContent {
            CanvasSizeDetailPage(
                initialWidth = pixelCanvasView.canvasWidth,
                initialHeight = pixelCanvasView.canvasHeight,
                onApply = { width, height ->
                    updateCanvasAspectRatio(width, height)
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showComposeGridSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.canvasContentPanel.visibility = View.GONE
        binding.canvasMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { pixelCanvasView.invalidate() }

        host.setContent {
            CanvasGridDetailPage(
                isGridEnabled = pixelCanvasView.isGridEnabled,
                gridSpacingDp = pixelCanvasView.gridSpacingDp,
                onGridEnabledChange = { enabled ->
                    pixelCanvasView.isGridEnabled = enabled
                    pixelCanvasView.invalidate()
                },
                onGridSpacingChange = { spacing ->
                    pixelCanvasView.gridSpacingDp = spacing
                    pixelCanvasView.invalidate()
                },
                onReset = {
                    pixelCanvasView.isGridEnabled = false
                    pixelCanvasView.gridSpacingDp = 32f
                    pixelCanvasView.invalidate()
                    showComposeGridSheet()
                },
                onApply = {
                    pixelCanvasView.runRecordedAction("Configure Grid") {}
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    pixelCanvasView.isGridEnabled = initialGridEnabled
                    pixelCanvasView.gridSpacingDp = initialGridSpacing
                    pixelCanvasView.invalidate()
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showComposeSnapSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.canvasContentPanel.visibility = View.GONE
        binding.canvasMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { pixelCanvasView.invalidate() }

        host.setContent {
            CanvasSnapDetailPage(
                isSnapEnabled = pixelCanvasView.isSnapToCenterEnabled,
                onSnapEnabledChange = { enabled ->
                    pixelCanvasView.isSnapToCenterEnabled = enabled
                    pixelCanvasView.invalidate()
                },
                onReset = {
                    pixelCanvasView.isSnapToCenterEnabled = true
                    pixelCanvasView.invalidate()
                    showComposeSnapSheet()
                },
                onApply = {
                    pixelCanvasView.runRecordedAction("Configure Magnetic Snap") {}
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    pixelCanvasView.isSnapToCenterEnabled = initialSnapEnabled
                    pixelCanvasView.invalidate()
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // LEGACY SETUP BACKGROUND MODE CHIPS (Fallback)
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
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
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
                .newInstance(initialColor = currentColor, resultKey = CANVAS_BG_RESULT_KEY)
                .show(fragmentManager, "CanvasSolidColorPicker")
        }

        binding.btnCustomGradient.setOnClickListener {
            val currentGrad = pixelCanvasView.canvasBackground.gradient
                ?: GradientColor.PRESETS[0]
            ColorPickerDialog
                .newInstance(initialGradient = currentGrad, resultKey = CANVAS_BG_RESULT_KEY)
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
            showSnackbar("Background image removed")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // RESTORE BACKGROUND MODE
    // ────────────────────────────────────────────────────────────────────────

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
        binding.canvasMenuPanel.visibility = View.VISIBLE
        binding.canvasToolStripInclude.canvasToolStripScroll.visibility = View.VISIBLE
        if (activeTag.isEmpty()) {
            binding.canvasContentPanel.visibility = View.GONE
            composeContainer?.visibility = View.GONE
        } else {
            select(activeTag)
        }
        restoreCanvasBgMode()
    }

    /**
     * Menampilkan dialog ukuran kanvas (kompatibilitas untuk pemanggil luar).
     */
    fun showImageSizeDialog() {
        if (composeHost != null && composeContainer != null) {
            select(TOOL_SIZE)
        } else {
            com.flyerpix.editor.ui.dialog.ImageSizeDialog.show(
                activity,
                pixelCanvasView.canvasWidth,
                pixelCanvasView.canvasHeight
            ) { width, height ->
                updateCanvasAspectRatio(width, height)
            }
        }
    }
}
