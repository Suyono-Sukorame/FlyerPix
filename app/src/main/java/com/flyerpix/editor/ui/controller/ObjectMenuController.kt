package com.flyerpix.editor.ui.controller

import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.AnchorType
import com.flyerpix.editor.canvas.model.ArrowLayer
import com.flyerpix.editor.canvas.model.ArrowStyle
import com.flyerpix.editor.canvas.model.BezierInputFlow
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.PenLayer
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.canvas.model.StrokeStyle
import com.flyerpix.editor.canvas.model.TextOnPathLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.ui.EditorActivity
import com.flyerpix.editor.ui.compose.*
import com.flyerpix.editor.ui.dialog.ColorPickerDialog
import com.flyerpix.editor.ui.controller.PanelHeightManager

class ObjectMenuController(
    private val activity: EditorActivity,
    private val binding: ActivityEditorBinding,
    private val canvas: PixelCanvasView,
    private val showSnackbar: (String) -> Unit,
    private val onGalleryRequested: () -> Unit,
    private val onCameraRequested: () -> Unit,
    private val onBackgroundGalleryRequested: () -> Unit = onGalleryRequested,
    private val onPanelChanged: () -> Unit = {},
    private val onShapeCreated: ((ShapeLayer) -> Unit)? = null
) {
    companion object {
        const val OBJ_TEXT     = "obj_text"
        const val OBJ_STICKER  = "obj_sticker"
        const val OBJ_IMPORT   = "obj_import"
        const val OBJ_DRAW     = "obj_draw"
        const val OBJ_SELECT   = "obj_select"
        const val OBJ_SHAPES   = "obj_shapes"
        const val OBJ_BEZIER   = "obj_bezier"
        const val OBJ_ARROW    = "obj_arrow"

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY   = 0xFF616161.toInt()

        private const val SHAPE_FILL_RESULT_KEY = "obj_shape_fill_color_key"
        private const val SHAPE_STROKE_RESULT_KEY = "obj_shape_stroke_color_key"
        private const val DRAW_COLOR_RESULT_KEY = "obj_draw_color_key"
        private const val ARROW_COLOR_RESULT_KEY = "obj_arrow_color_key"
        private const val BEZIER_COLOR_RESULT_KEY = "obj_bezier_color_key"
    }

    private val fragmentManager: FragmentManager get() = activity.supportFragmentManager
    private val composeHost: ComposeView? get() = binding.composeThreeDDetail
    private val composeContainer: FrameLayout? get() = binding.composeThreeDSheetContainer

    private val toolItems = LinkedHashMap<String, ViewGroup>()
    var activeTag: String = ""

    // Draft layer snapshots for Cancel / Rollback
    private var draftShape: ShapeLayer? = null
    private var isNewShape: Boolean = false
    private var shapeSnapshotType: ShapeType = ShapeType.RECTANGLE
    private var shapeSnapshotCornerX: Float = 20f
    private var shapeSnapshotCornerY: Float = 20f
    private var shapeSnapshotOpacity: Int = 255
    private var shapeSnapshotFillColor: Int = 0xFF1769FF.toInt()
    private var shapeSnapshotStrokeWidth: Float = 0f
    private var shapeSnapshotStrokeOpacity: Int = 255
    private var shapeSnapshotStrokeColor: Int = Color.BLACK
    private var shapeSnapshotStrokeJoin: Paint.Join = Paint.Join.MITER
    private var shapeSnapshotStrokeStyle: StrokeStyle = StrokeStyle.SOLID
    private var shapeSnapshotArcStartAngle: Float = 0f
    private var shapeSnapshotArcSweepAngle: Float = 270f

    private var draftArrow: ArrowLayer? = null
    private var draftPen: PenLayer? = null
    private val bezierFlow = BezierInputFlow()

    /** State Brush Color draw sheet yang bisa di-update live dari color picker dialog. */
    private val liveDrawBrushColor = mutableStateOf(0)

    /**
     * Callback saat salah satu Compose sheet Add dibuka/ditutup,
     * untuk menganimasikan navigasi bawah (translationY 56dp) dan fit canvas viewport.
     */
    var onAddSettingsOpenChanged: ((Boolean) -> Unit)? = null

    /** Tinggi sheet identik dengan menu Gradient (floor 300dp, 40% layar). */
    private fun computeComposeSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        val floorPx = (300 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.40f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    private fun computeShapeSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        // Fallback if home panel not yet measured
        val floorPx = (107 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.12f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    private fun computeStickerSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        // Fallback if home panel not yet measured
        val floorPx = (107 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.12f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    private fun computeDrawSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        // Fallback if home panel not yet measured
        val floorPx = (107 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.12f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    private fun computeBezierSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        // Fallback if home panel not yet measured
        val floorPx = (107 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.12f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    private fun computeArrowSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        // Fallback if home panel not yet measured
        val floorPx = (107 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.12f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    private fun computeImportSheetHeight(): Int {
        val density = activity.resources.displayMetrics.density
        val root = binding.parentLayout
        val homePanel = binding.bottomControlPanelContainer
        if (root.height > 0 && homePanel.height > 0) {
            val homeTop = PanelHeightManager.topInRoot(homePanel, root)
            val alignedH = root.height - homeTop
            if (alignedH > 0) return alignedH
        }
        // Fallback if home panel not yet measured
        val floorPx = (107 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.12f).toInt()
        return targetPx.coerceAtLeast(floorPx)
    }

    fun initialize() {
        buildToolStrip()
        setupColorResultListeners()
    }

    private fun buildToolStrip() {
        data class Spec(val tag: String, val label: String, val iconRes: Int)
        val specs = listOf(
            Spec(OBJ_TEXT,     "Text",    R.drawable.ic_nav_text_24px),
            Spec(OBJ_STICKER,  "Sticker", R.drawable.ic_sharp_face_24px),
            Spec(OBJ_IMPORT,   "Import",  R.drawable.ic_outline_photo_24px),
            Spec(OBJ_DRAW,     "Draw",    R.drawable.ic_sharp_brush_24px),
            Spec(OBJ_SELECT,   "Select",  R.drawable.ic_mask_24px),
            Spec(OBJ_SHAPES,   "Shapes",  R.drawable.ic_nav_shapes_24px),
            Spec(OBJ_BEZIER,   "Bezier",  R.drawable.ic_curve_24px),
            Spec(OBJ_ARROW,    "Arrow",   R.drawable.ic_arrow_24px)
        )
        val density = activity.resources.displayMetrics.density
        val container = binding.objectToolStripInclude.objectToolStripContainer
        container.removeAllViews()

        for (spec in specs) {
            val item = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                isClickable = true; isFocusable = true
                setBackgroundResource(R.drawable.bg_panel_tool_item)
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
                setOnClickListener { onToolClicked(spec.tag) }
            }
            val iconSize = (28 * density).toInt()
            item.addView(ImageView(activity).apply {
                setImageResource(spec.iconRes)
                colorFilter = android.graphics.PorterDuffColorFilter(COLOR_GRAY, android.graphics.PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            })
            item.addView(TextView(activity).apply {
                text = spec.label; textSize = 11f; maxLines = 1
                gravity = android.view.Gravity.CENTER; setTextColor(COLOR_GRAY)
            })
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.width = (62 * density).toInt()
            container.addView(item, lp)
            toolItems[spec.tag] = item
        }
    }

    private fun onToolClicked(tag: String) {
        android.util.Log.i("BEZDEBUG", "onToolClicked tag=$tag activeTag=$activeTag selNav=${binding.bottomNavigation.selectedItemId}")
        if (tag == OBJ_TEXT) {
            canvas.addTextLayer("New Text")
            showSnackbar("Text layer added")
            binding.bottomNavigation.selectedItemId = R.id.nav_edit
            return
        }

        if (binding.bottomNavigation.selectedItemId != R.id.nav_add) {
            binding.bottomNavigation.selectedItemId = R.id.nav_add
        }

        if (tag == activeTag) deselect(restoreStrip = true) else select(tag)
    }

    fun select(tag: String) {
        activeTag = tag
        android.util.Log.i("BEZDEBUG", "select($tag)")
        updateToolStripSelection(tag)
        when (tag) {
            OBJ_STICKER -> showComposeStickerSheet()
            OBJ_IMPORT  -> showComposeImportSheet()
            OBJ_DRAW    -> showComposeDrawSheet()
            OBJ_SELECT  -> showSelectionToolSheet()
            OBJ_SHAPES  -> showComposeShapeSheet()
            OBJ_BEZIER  -> showComposeBezierSheet()
            OBJ_ARROW   -> showComposeArrowSheet()
        }
        onAddSettingsOpenChanged?.invoke(true)
        onPanelChanged()
    }

    fun deselect(restoreStrip: Boolean = false) {
        android.util.Log.i("BEZDEBUG", "deselect restoreStrip=$restoreStrip")
        activeTag = ""
        updateToolStripSelection("")

        // Cleanup draft layers if cancelled
        draftShape = null
        draftArrow = null
        draftPen = null
        canvas.freeDrawEnabled = false
        canvas.bezierInputEnabled = false
        canvas.bezierInputLayer = null
        canvas.onBezierInputPointChanged = null
        canvas.clearSelection()
        canvas.onSelectionChanged = null

        // Hide Compose container
        composeContainer?.visibility = View.GONE
        binding.objectContentPanel.visibility = View.GONE

        if (restoreStrip) {
            binding.objectToolStripInclude.objectToolStripScroll.visibility = View.VISIBLE
            binding.objectMenuPanel.visibility = View.VISIBLE
        }

        onAddSettingsOpenChanged?.invoke(false)
        onPanelChanged()
    }

    private fun updateToolStripSelection(active: String) {
        for ((t, item) in toolItems) {
            val sel = t == active
            item.isSelected = sel
            val c = if (sel) COLOR_ACTIVE else COLOR_GRAY
            (item.getChildAt(0) as? ImageView)?.colorFilter =
                android.graphics.PorterDuffColorFilter(c, android.graphics.PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? TextView)?.setTextColor(c)
        }
    }

    fun refreshUI() {
        binding.objectToolStripInclude.objectToolStripScroll.visibility = View.VISIBLE
        binding.objectMenuPanel.visibility = View.VISIBLE
        val hasVisibleComposeSheet = composeContainer?.visibility == View.VISIBLE
        if (activeTag.isEmpty() && !hasVisibleComposeSheet) {
            composeContainer?.visibility = View.GONE
            binding.objectContentPanel.visibility = View.GONE
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Color Picker Result Listeners
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupColorResultListeners() {
        fragmentManager.setFragmentResultListener(SHAPE_FILL_RESULT_KEY, activity) { _, bundle ->
            val color = bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE)
            draftShape?.let {
                it.fillColor = color
                canvas.invalidate()
                showComposeShapeSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(SHAPE_STROKE_RESULT_KEY, activity) { _, bundle ->
            val color = bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.BLACK)
            draftShape?.let {
                it.strokeColor = color
                canvas.invalidate()
                showComposeShapeSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(DRAW_COLOR_RESULT_KEY, activity) { _, bundle ->
            @Suppress("DEPRECATION")
            val gradient = bundle.getSerializable(ColorPickerDialog.EXTRA_GRADIENT) as? GradientColor
            val color = if (gradient != null) {
                gradient.colors.firstOrNull() ?: Color.WHITE
            } else {
                bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE)
            }
            liveDrawBrushColor.value = color
            canvas.freeDrawColor = color
            showComposeDrawSheet()
        }
        fragmentManager.setFragmentResultListener(ARROW_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE)
            draftArrow?.let {
                it.headColor = color
                it.tailColor = color
                canvas.invalidate()
                showComposeArrowSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(BEZIER_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE)
            draftPen?.let {
                it.strokeColor = color
                canvas.invalidate()
                showComposeBezierSheet(it)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Sticker Sheet (Dynamic Height - Home Menu Aligned)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeStickerSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeStickerSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            StickerDetailPage(
                onStickerSelected = { stickerItem ->
                    canvas.addEmojiLayer(stickerItem.emoji)
                    showSnackbar("Added ${stickerItem.emoji} to canvas")
                },
                onApply = { deselect(restoreStrip = true) },
                onCancel = { deselect(restoreStrip = true) },
                onReset = {
                    // Reset just clears the UI selection state (handled in Composable)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Shape Studio (Opsi "Shape Picker First")
    //    Step 1: ShapePickerPage kompak (~38%) -> pilih tipe lebih dulu
    //    Step 2: expand ke ShapeDetailPage di container yang sama (~58%)
    // ─────────────────────────────────────────────────────────────────────────

    fun showComposeShapeSheet(shapeToEdit: ShapeLayer? = null) {
        if (shapeToEdit != null || draftShape != null) {
            showComposeShapeDetail(shapeToEdit ?: draftShape!!)
        } else {
            showComposeShapePicker()
        }
    }

    /**
     * Step 1 — Gallery pemilihan tipe shape. User memilih bentuk lebih dulu,
     * baru layer dibuat di canvas (tidak ada "shape mendadak muncul").
     */
    private fun showComposeShapePicker() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = OBJ_SHAPES
        updateToolStripSelection(OBJ_SHAPES)

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            ShapePickerPage(
                onShapeTypeSelected = { type ->
                    val shape = canvas.addShapeLayer(type).also {
                        isNewShape = true
                        canvas.selectedLayer = it
                        onShapeCreated?.invoke(it)
                    }
                    draftShape = shape
                    // Backup initial state untuk rollback saat Cancel
                    shapeSnapshotType = shape.shapeType
                    shapeSnapshotCornerX = shape.cornerRadiusX
                    shapeSnapshotCornerY = shape.cornerRadiusY
                    shapeSnapshotOpacity = shape.opacity
                    shapeSnapshotFillColor = shape.fillColor
                    shapeSnapshotStrokeWidth = shape.strokeWidth
                    shapeSnapshotStrokeOpacity = shape.strokeOpacity
                    shapeSnapshotStrokeColor = shape.strokeColor
                    shapeSnapshotStrokeJoin = shape.strokeJoin
                    shapeSnapshotStrokeStyle = shape.strokeStyle
                    shapeSnapshotArcStartAngle = shape.arcStartAngle
                    shapeSnapshotArcSweepAngle = shape.arcSweepAngle
                    showComposeShapeDetail(shape)
                },
                onClose = {
                    draftShape = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    /**
     * Step 2 — Detail editing shape (warna, stroke, opacity, dsb). Dipanggil
     * baik untuk mengedit shape lama maupun setelah memilih tipe di picker.
     */
    private fun showComposeShapeDetail(shape: ShapeLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = OBJ_SHAPES
        updateToolStripSelection(OBJ_SHAPES)

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        if (draftShape == null) {
            draftShape = shape
            isNewShape = false
            // Backup initial state
            shapeSnapshotType = shape.shapeType
            shapeSnapshotCornerX = shape.cornerRadiusX
            shapeSnapshotCornerY = shape.cornerRadiusY
            shapeSnapshotOpacity = shape.opacity
            shapeSnapshotFillColor = shape.fillColor
            shapeSnapshotStrokeWidth = shape.strokeWidth
            shapeSnapshotStrokeOpacity = shape.strokeOpacity
            shapeSnapshotStrokeColor = shape.strokeColor
            shapeSnapshotStrokeJoin = shape.strokeJoin
            shapeSnapshotStrokeStyle = shape.strokeStyle
            shapeSnapshotArcStartAngle = shape.arcStartAngle
            shapeSnapshotArcSweepAngle = shape.arcSweepAngle
        }

        host.setContent {
            var currentType by remember { mutableStateOf(shape.shapeType) }
            var currentCorner by remember { mutableStateOf(shape.cornerRadiusX) }
            var currentOpacity by remember { mutableStateOf(shape.opacity / 255f * 100f) }
            var currentFillColor by remember { mutableStateOf(shape.fillColor) }
            var currentStrokeWidth by remember { mutableStateOf(shape.strokeWidth) }
            var currentStrokeOpacity by remember { mutableStateOf(shape.strokeOpacity / 255f * 100f) }
            var currentStrokeColor by remember { mutableStateOf(shape.strokeColor) }
            var currentStrokeJoin by remember { mutableStateOf(shape.strokeJoin) }
            var currentStrokeStyle by remember { mutableStateOf(shape.strokeStyle) }
            var currentArcStartAngle by remember { mutableStateOf(shape.arcStartAngle) }
            var currentArcSweepAngle by remember { mutableStateOf(shape.arcSweepAngle) }

            ShapeDetailPage(
                shapeType = currentType,
                cornerRadius = currentCorner,
                opacity = currentOpacity,
                fillColor = currentFillColor,
                strokeWidth = currentStrokeWidth,
                strokeOpacity = currentStrokeOpacity,
                strokeColor = currentStrokeColor,
                strokeJoin = currentStrokeJoin,
                strokeStyle = currentStrokeStyle,
                arcStartAngle = currentArcStartAngle,
                arcSweepAngle = currentArcSweepAngle,
                onShapeTypeChange = { type ->
                    currentType = type
                    shape.shapeType = type
                    canvas.invalidate()
                },
                onCornerRadiusChange = { radius ->
                    currentCorner = radius
                    shape.cornerRadiusX = radius
                    shape.cornerRadiusY = radius
                    canvas.invalidate()
                },
                onOpacityChange = { op ->
                    currentOpacity = op
                    shape.opacity = (op / 100f * 255f).toInt()
                    canvas.invalidate()
                },
                onFillColorChange = { color ->
                    currentFillColor = color
                    shape.fillColor = color
                    canvas.invalidate()
                },
                onOpenFillColorPicker = {
                    ColorPickerDialog.newInstance(
                        initialColor = shape.fillColor,
                        resultKey = SHAPE_FILL_RESULT_KEY
                    ).show(fragmentManager, "ShapeFillColorPicker")
                },
                onStrokeWidthChange = { width ->
                    currentStrokeWidth = width
                    shape.strokeWidth = width
                    canvas.invalidate()
                },
                onStrokeOpacityChange = { op ->
                    currentStrokeOpacity = op
                    shape.strokeOpacity = (op / 100f * 255f).toInt()
                    canvas.invalidate()
                },
                onStrokeColorChange = { color ->
                    currentStrokeColor = color
                    shape.strokeColor = color
                    canvas.invalidate()
                },
                onOpenStrokeColorPicker = {
                    ColorPickerDialog.newInstance(
                        initialColor = shape.strokeColor,
                        resultKey = SHAPE_STROKE_RESULT_KEY
                    ).show(fragmentManager, "ShapeStrokeColorPicker")
                },
                onStrokeJoinChange = { join ->
                    currentStrokeJoin = join
                    shape.strokeJoin = join
                    canvas.invalidate()
                },
                onStrokeStyleChange = { style ->
                    currentStrokeStyle = style
                    shape.strokeStyle = style
                    canvas.invalidate()
                },
                onArcStartAngleChange = { angle ->
                    currentArcStartAngle = angle
                    shape.arcStartAngle = angle
                    canvas.invalidate()
                },
                onArcSweepAngleChange = { angle ->
                    currentArcSweepAngle = angle
                    shape.arcSweepAngle = angle
                    canvas.invalidate()
                },
                onApply = {
                    canvas.runRecordedAction(if (isNewShape) "Add Shape" else "Modify Shape") {}
                    showSnackbar("Shape saved")
                    draftShape = null
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    if (isNewShape) {
                        canvas.removeLayer(shape)
                    } else {
                        shape.shapeType = shapeSnapshotType
                        shape.cornerRadiusX = shapeSnapshotCornerX
                        shape.cornerRadiusY = shapeSnapshotCornerY
                        shape.opacity = shapeSnapshotOpacity
                        shape.fillColor = shapeSnapshotFillColor
                        shape.strokeWidth = shapeSnapshotStrokeWidth
                        shape.strokeOpacity = shapeSnapshotStrokeOpacity
                        shape.strokeColor = shapeSnapshotStrokeColor
                        shape.strokeJoin = shapeSnapshotStrokeJoin
                        shape.strokeStyle = shapeSnapshotStrokeStyle
                        shape.arcStartAngle = shapeSnapshotArcStartAngle
                        shape.arcSweepAngle = shapeSnapshotArcSweepAngle
                    }
                    canvas.invalidate()
                    draftShape = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Free Draw Studio Sheet (Dynamic Height - Home Menu Aligned)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeDrawSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeDrawSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        canvas.freeDrawEnabled = true

        host.setContent {
            var currentBrushSize by remember { mutableStateOf(canvas.freeDrawStrokeWidth) }
            liveDrawBrushColor.value = canvas.freeDrawColor

            DrawDetailPage(
                brushSize = currentBrushSize,
                brushColor = liveDrawBrushColor.value,
                onBrushSizeChange = { size ->
                    currentBrushSize = size
                    canvas.freeDrawStrokeWidth = size
                },
                onBrushColorChange = { color ->
                    liveDrawBrushColor.value = color
                    canvas.freeDrawColor = color
                },
                onOpenColorPicker = {
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = liveDrawBrushColor.value,
                        resultKey = DRAW_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        liveDrawBrushColor.value = color
                        canvas.freeDrawColor = color
                        canvas.invalidate()
                    }
                    dialog.onGradientChanged = { gradient ->
                        val solid = gradient.colors.firstOrNull() ?: liveDrawBrushColor.value
                        liveDrawBrushColor.value = solid
                        canvas.freeDrawColor = solid
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "DrawColorPicker")
                },
                onApply = {
                    canvas.freeDrawEnabled = false
                    showSnackbar("Drawing saved")
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    canvas.freeDrawEnabled = false
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Import Sheet (Dynamic Height - Home Menu Aligned)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeImportSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeImportSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            ImportDetailPage(
                onGalleryClick = {
                    deselect(restoreStrip = true)
                    onGalleryRequested()
                },
                onCameraClick = {
                    deselect(restoreStrip = true)
                    onCameraRequested()
                },
                onApply = { deselect(restoreStrip = true) },
                onCancel = { deselect(restoreStrip = true) },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Arrow Studio Sheet (Dynamic Height - Home Menu Aligned)
    // ─────────────────────────────────────────────────────────────────────────

private fun showComposeArrowSheet(existingArrow: ArrowLayer? = null) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        val arrow = existingArrow ?: draftArrow ?: canvas.addArrowLayer().also {
            canvas.selectedLayer = it
        }
        draftArrow = arrow

        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeArrowSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            var currentStyle by remember { mutableStateOf(arrow.arrowStyle) }
            var currentStemWidth by remember { mutableStateOf(arrow.stemWidth) }
            var currentColor by remember { mutableStateOf(arrow.headColor) }

            ArrowDetailPage(
                arrowStyle = currentStyle,
                stemWidth = currentStemWidth,
                arrowColor = currentColor,
                onArrowStyleChange = { style ->
                    currentStyle = style
                    arrow.arrowStyle = style
                    canvas.invalidate()
                },
                onStemWidthChange = { width ->
                    currentStemWidth = width
                    arrow.stemWidth = width
                    canvas.invalidate()
                },
                onArrowColorChange = { color ->
                    currentColor = color
                    arrow.headColor = color
                    arrow.tailColor = color
                    canvas.invalidate()
                },
                onOpenColorPicker = {
                    ColorPickerDialog.newInstance(
                        initialColor = arrow.headColor,
                        resultKey = ARROW_COLOR_RESULT_KEY
                    ).show(fragmentManager, "ArrowColorPicker")
                },
                onApply = {
                    canvas.runRecordedAction("Add Arrow") {}
                    showSnackbar("Arrow added")
                    draftArrow = null
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    canvas.removeLayer(arrow)
                    canvas.invalidate()
                    draftArrow = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Bézier Curve Sheet (Dynamic Height - Home Menu Aligned)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeBezierSheet(existingPen: PenLayer? = null) {
        val host = composeHost ?: return
        val container = composeContainer ?: return
        android.util.Log.i("BEZDEBUG", "showComposeBezierSheet enter host=${host != null} container=${container != null}")

        activeTag = OBJ_BEZIER
        updateToolStripSelection(OBJ_BEZIER)

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        val pen = existingPen ?: draftPen ?: PenLayer(isClosed = false).apply {
            strokeColor = 0xFF1769FF.toInt()
            strokeWidth = 6f
            x = (canvas.width / 2f)
            y = (canvas.height / 2f)
        }.also {
            canvas.addLayer(it)
            canvas.selectedLayer = it
        }

        android.util.Log.i("BEZDEBUG", "bezier container set VISIBLE")
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeBezierSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }
        draftPen = pen
        bezierFlow.clear()
        pen.anchors.forEach { anchor -> bezierFlow.addPoint(anchor.x, anchor.y) }

        host.setContent {
            var currentWidth by remember { mutableStateOf(pen.strokeWidth) }
            var currentColor by remember { mutableStateOf(pen.strokeColor) }
            var currentInputCount by remember { mutableStateOf(pen.anchors.size) }

            canvas.bezierInputEnabled = true
            canvas.bezierInputLayer = pen
            canvas.onBezierInputPointChanged = { count ->
                currentInputCount = count
            }

            BezierDetailPage(
                strokeWidth = currentWidth,
                strokeColor = currentColor,
                inputCount = currentInputCount,
                canApply = currentInputCount >= 2,
                onStrokeWidthChange = { width ->
                    currentWidth = width
                    pen.strokeWidth = width
                    canvas.invalidate()
                },
                onStrokeColorChange = { color ->
                    currentColor = color
                    pen.strokeColor = color
                    canvas.invalidate()
                },
                onOpenColorPicker = {
                    ColorPickerDialog.newInstance(
                        initialColor = pen.strokeColor,
                        resultKey = BEZIER_COLOR_RESULT_KEY
                    ).show(fragmentManager, "BezierColorPicker")
                },
                onInputFirst = {
                    val anchorX = if (pen.anchors.isEmpty()) canvas.width / 2f else pen.anchors.last().x + 80f
                    val anchorY = if (pen.anchors.isEmpty()) canvas.height / 2f else pen.anchors.last().y + 40f
                    pen.addAnchor(anchorX - pen.x, anchorY - pen.y, AnchorType.CORNER)
                    bezierFlow.addPoint(anchorX, anchorY)
                    currentInputCount = pen.anchors.size
                    canvas.invalidate()
                },
                onResetInput = {
                    pen.clearAllAnchors()
                    bezierFlow.clear()
                    currentInputCount = 0
                    canvas.invalidate()
                },
                onApply = {
                    if (pen.anchors.size < 2) {
                        showSnackbar("Masukkan minimal 2 titik Bézier dulu")
                        return@BezierDetailPage
                    }
                    canvas.runRecordedAction("Add Bézier") {}
                    showSnackbar("Bézier curve added")
                    draftPen = null
                    bezierFlow.clear()
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    canvas.removeLayer(pen)
                    canvas.invalidate()
                    draftPen = null
                    bezierFlow.clear()
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    /**
     * Panel Selection Tools (Prompt 10): Rectangle / Ellipse / Lasso + feather
     * + "To Mask" rasterize ke layer mask + "Batal".
     */
    private fun showSelectionToolSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = OBJ_SELECT
        updateToolStripSelection(OBJ_SELECT)
        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()
        PanelHeightManager.setHeight(container, (300 * activity.resources.displayMetrics.density).toInt())
        container.post { canvas.invalidate() }

        host.setContent {
            var hasSelection by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(canvas.selectionPath != null) }
            canvas.onSelectionChanged = {
                hasSelection = canvas.selectionPath != null
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                android.util.Log.d("FlyerPixMask", "LaunchedEffect firing beginSelection(RECT)")
                canvas.beginSelection(com.flyerpix.editor.canvas.PixelCanvasView.SelectionTool.RECT)
                android.util.Log.d("FlyerPixMask", "after beginSelection selTool=${canvas.selectionTool}")
            }
            com.flyerpix.editor.ui.composables.SelectionToolPanel(
                hasSelection = hasSelection,
                onToolChanged = { tool -> canvas.beginSelection(tool) },
                onApplyToMask = { feather, inverted ->
                    android.util.Log.d("FlyerPixMask", "onApplyToMask tapped feather=$feather inverted=$inverted hasSelection=$hasSelection")
                    val ok = canvas.rasterizeSelectionToMask(feather, inverted)
                    android.util.Log.d("FlyerPixMask", "rasterize ok=$ok selectedLayer=${canvas.selectedLayer?.javaClass?.simpleName}")
                    showSnackbar(if (ok) "Selection applied to mask" else "Select a photo layer first")
                    canvas.clearSelection()
                    hasSelection = false
                },
                onCancel = {
                    canvas.clearSelection()
                    deselect(restoreStrip = true)
                }
            )
        }
    }

    /**
     * Menampilkan panel editor anchor Bézier ketika user memilih PenLayer yang ada di canvas.
     * Ini memungkinkan user untuk mengedit anchor, handle, dan tipe node (Phase 1).
     */
    fun showComposeBezierAnchorEditor(penLayer: PenLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = OBJ_BEZIER
        updateToolStripSelection(OBJ_BEZIER)

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeBezierSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        // Enter Bezier edit mode
        canvas.enterBezierEditMode(penLayer)

        host.setContent {
            var selectedAnchorIdx by remember { mutableStateOf(-1) }
            var refreshTrigger by remember { mutableStateOf(0) } // Trigger recomposition

            // Listen to anchor selection from canvas
            canvas.onBezierAnchorSelected = { index ->
                selectedAnchorIdx = index
                refreshTrigger++
            }

            canvas.onBezierAnchorDeselected = {
                selectedAnchorIdx = -1
                refreshTrigger++
            }

            // Recompose when anchor changes
            canvas.onBezierAnchorChanged = { index ->
                refreshTrigger++
                canvas.invalidate()
            }

            BezierAnchorEditorPanel(
                penLayer = penLayer,
                selectedAnchorIndex = selectedAnchorIdx,
                onAnchorChanged = {
                    canvas.invalidate()
                    // Trigger recomposition
                    refreshTrigger++
                },
                onExitEditMode = {
                    closeBezierAnchorEditor()
                },
                maxHeightPx = sheetMaxH
            )

            // Force recomposition on refresh trigger change
            LaunchedEffect(refreshTrigger) {
                // Intentionally empty - just to trigger recomposition
            }
        }
    }

    /**
     * Menutup panel editor anchor Bézier dan keluar dari edit mode.
     */
    private fun closeBezierAnchorEditor() {
        canvas.exitBezierEditMode()
        canvas.onBezierAnchorSelected = null
        canvas.onBezierAnchorDeselected = null
        canvas.onBezierAnchorChanged = null
        deselect(restoreStrip = true)
    }

    /**
     * Menangani ketika layer dipilih untuk mengecek apakah itu PenLayer yang perlu edit mode.
     */
    fun onLayerSelected(layer: com.flyerpix.editor.canvas.model.CanvasLayer?) {
        if (layer is PenLayer && layer.anchors.isNotEmpty() && activeTag != OBJ_BEZIER) {
            // User selected a PenLayer with anchors - enable edit mode
            showComposeBezierAnchorEditor(layer)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Text on Path Editor (Phase 5)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Show Text on Path editor panel (Phase 5 - killer feature).
     */
    fun showTextOnPathEditor(textOnPathLayer: com.flyerpix.editor.canvas.model.TextOnPathLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = "obj_text_on_path"
        updateToolStripSelection("obj_text_on_path")

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = 500
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            var refreshTrigger by remember { mutableStateOf(0) }

            TextOnPathEditorPanel(
                textOnPathLayer = textOnPathLayer,
                onTextChanged = {
                    canvas.invalidate()
                    refreshTrigger++
                },
                onExitEditMode = {
                    closeTextOnPathEditor()
                },
                maxHeightPx = sheetMaxH
            )

            LaunchedEffect(refreshTrigger) {
                // Trigger recomposition
            }
        }
    }

    /**
     * Close Text on Path editor panel.
     */
    private fun closeTextOnPathEditor() {
        deselect(restoreStrip = true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gradient Editor (Phase 6)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Show Gradient editor panel for Pen/Shape layers (Phase 6).
     */
    fun showGradientEditor(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = "obj_gradient"
        updateToolStripSelection("obj_gradient")

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = 500
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            var refreshTrigger by remember { mutableStateOf(0) }

            GradientEditorPanel(
                layer = layer,
                onChanged = {
                    canvas.invalidate()
                    refreshTrigger++
                },
                onExitEditMode = {
                    closeGradientEditor()
                },
                maxHeightPx = sheetMaxH
            )

            LaunchedEffect(refreshTrigger) {
                // Trigger recomposition
            }
        }
    }

    /**
     * Close Gradient editor panel.
     */
    private fun closeGradientEditor() {
        deselect(restoreStrip = true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shadow Editor (Phase 7)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Show Shadow editor panel for all layer types (Phase 7).
     */
    fun showShadowEditor(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = "obj_shadow"
        updateToolStripSelection("obj_shadow")

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = 550
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            var refreshTrigger by remember { mutableStateOf(0) }

            ShadowEditorPanel(
                layer = layer,
                onChanged = {
                    canvas.invalidate()
                    refreshTrigger++
                },
                onExitEditMode = {
                    closeShadowEditor()
                },
                maxHeightPx = sheetMaxH
            )

            LaunchedEffect(refreshTrigger) {
                // Trigger recomposition
            }
        }
    }

    /**
     * Close Shadow editor panel.
     */
    private fun closeShadowEditor() {
        deselect(restoreStrip = true)
    }

    /**
     * Show Replace Background panel (Phase 8 - Prompt 08).
     * Allows user to change canvas background and apply auto color match to layers.
     */
    fun showReplaceBackgroundEditor() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = "replace_bg"
        updateToolStripSelection("replace_bg")

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = 600
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            var autoColorMatchEnabled by remember { mutableStateOf(false) }

            com.flyerpix.editor.ui.composables.ReplaceBackgroundComposable(
                currentBackground = canvas.canvasBackground,
                onBackgroundChange = { newBg ->
                    canvas.canvasBackground = newBg
                    canvas.invalidate()
                },
                onColorMatchToggle = { enabled ->
                    autoColorMatchEnabled = enabled
                },
                onGalleryClick = {
                    onBackgroundGalleryRequested()
                },
                onReset = {
                    closeReplaceBackgroundEditor()
                },
                onApply = { autoMatch, blend ->
                    applyAutoColorMatch(autoMatch)
                    if (blend) applyBlendMaskToSelected()
                    closeReplaceBackgroundEditor()
                },
                onCancel = {
                    closeReplaceBackgroundEditor()
                },
                autoColorMatchEnabled = autoColorMatchEnabled
            )
        }
    }

    /**
     * Auto Color Match: jangan rusak piksel layer — hanya ubah nilai adjustment
     * (Prompt 08). Terapkan preset Punchy + naikkan exposure bila background lebih gelap.
     */
    private fun applyAutoColorMatch(enabled: Boolean) {
        val layer = canvas.selectedLayer ?: return
        if (!enabled) return
        if (layer.isLocked) return
        val preset = com.flyerpix.editor.ui.compose.layerAdjustPresetParams("Punchy")
        val a = layer.adjustments
        a.contrast = preset.contrast
        a.saturation = preset.saturation
        a.exposure = preset.exposure
        a.highlights = preset.highlights
        a.shadows = preset.shadows
        a.temperature = preset.temperature
        a.tint = preset.tint
        a.gamma = preset.gamma
        a.vibrance = preset.vibrance
        a.hue = preset.hue
        a.preset = "Punchy"
        layer.adjustmentsEnabled = true
        val bg = canvas.canvasBackground
        val bgLuma = when (bg.mode) {
            com.flyerpix.editor.canvas.model.CanvasBackgroundMode.SOLID_COLOR ->
                ((0.299f * android.graphics.Color.red(bg.solidColor)) +
                    (0.587f * android.graphics.Color.green(bg.solidColor)) +
                    (0.114f * android.graphics.Color.blue(bg.solidColor))) / 255f
            else -> 0.5f
        }
        if (bgLuma < 0.4f) a.exposure = (a.exposure ?: 0f) + 15f
        canvas.invalidate()
    }

    /**
     * Blend Mask: gradient bawah→atas (Prompt 07) pada layer foto terpilih.
     */
    private fun applyBlendMaskToSelected() {
        val layer = canvas.selectedLayer ?: return
        if (layer.isLocked) return
        if (layer.maskBitmap == null) {
            val (w, h) = layer.getUnwarpedDimensions()
            if (w > 0 && h > 0) layer.createMask(w.toInt(), h.toInt())
        }
        layer.maskBitmap?.let { bmp ->
            com.flyerpix.editor.canvas.model.MaskUtils.generateGradientMask(
                bmp,
                com.flyerpix.editor.canvas.model.GradientType.LINEAR,
                com.flyerpix.editor.canvas.model.MaskUtils.DIR_BOTTOM_TOP
            )
            layer.maskEnabled = true
            canvas.invalidate()
        }
    }

    /**
     * Close Replace Background editor panel.
     */
    private fun closeReplaceBackgroundEditor() {
        deselect(restoreStrip = true)
    }

    /**
     * Show Layer Mask Editor panel (Phase 9-10 - Prompt 05-07).
     * Per-layer 8-bit mask: brush, invert, feather, gradient mask.
     */
    fun showMaskEditor(layer: com.flyerpix.editor.canvas.model.CanvasLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = "mask_editor"
        updateToolStripSelection("mask_editor")

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE

        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeComposeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        if (layer.maskBitmap == null) {
            val (w, h) = layer.getUnwarpedDimensions()
            if (w > 0 && h > 0) {
                layer.createMask(w.toInt(), h.toInt())
                // Isi putih agar layer tetap terlihat penuh — langsung bisa dicat hitam untuk menghapus.
                layer.maskBitmap?.eraseColor(0xFFFFFFFF.toInt())
            }
        }

        host.setContent {
            com.flyerpix.editor.ui.composables.LayerMaskEditorComposable(
                layer = layer,
                onMaskChange = { canvas.invalidate() },
                onClose = { canvas.endMaskPaint(); closeMaskEditor() },
                onGradientMaskApply = { type, params ->
                    if (layer.maskBitmap == null) {
                        val (w, h) = layer.getUnwarpedDimensions()
                        if (w > 0 && h > 0) layer.createMask(w.toInt(), h.toInt())
                    }
                    layer.maskBitmap?.let { bitmap ->
                        val direction = if (params.isNotEmpty()) params[0].toInt() else 0
                        com.flyerpix.editor.canvas.model.MaskUtils.generateGradientMask(bitmap, type, direction)
                        canvas.invalidate()
                    }
                },
                onFeatherApply = { radius ->
                    layer.maskBitmap?.let { bitmap ->
                        com.flyerpix.editor.canvas.model.MaskUtils.featherMask(bitmap, radius.toInt())
                        canvas.invalidate()
                    }
                },
                onBrushConfig = { size, opacity, color, isEraser ->
                    canvas.updateMaskBrush(size, opacity, color, isEraser)
                },
                onEnableMaskPaint = {
                    canvas.startMaskPaint(layer)
                },
                onFinish = {
                    canvas.endMaskPaint()
                    closeMaskEditor()
                },
                maxHeightPx = sheetMaxH,
                // ── Phase 4 & 5: Smart Erase Callbacks (OPTION D) ──────────────────
                onModeChange = { mode ->
                    val brush = canvas.getMaskPaintBrush()
                    when (mode) {
                        "manual" -> {
                            brush.autoEraseMode = false
                            brush.cloneMode = false
                        }
                        "auto-erase" -> {
                            brush.autoEraseMode = true
                            brush.cloneMode = false
                            brush.autoEraseSourceColor = null  // Reset on mode switch
                        }
                        "clone" -> {
                            brush.autoEraseMode = false
                            brush.cloneMode = true
                            brush.cloneSourceX = null
                            brush.cloneSourceY = null
                        }
                    }
                },
                onPressureToggle = { enabled ->
                    canvas.getMaskPaintBrush().pressureSensitivityEnabled = enabled
                },
                onAutoEraseThreshold = { threshold ->
                    canvas.getMaskPaintBrush().autoEraseThreshold = threshold.coerceIn(0, 255)
                },
                onCloneModeToggle = { enabled ->
                    val brush = canvas.getMaskPaintBrush()
                    brush.cloneMode = enabled
                    if (!enabled) {
                        brush.cloneSourceX = null
                        brush.cloneSourceY = null
                    }
                }
            )
        }
    }

    /**
     * Close Mask editor panel.
     */
    private fun closeMaskEditor() {
        deselect(restoreStrip = true)
    }
}
