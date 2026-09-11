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
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.ArrowLayer
import com.flyerpix.editor.canvas.model.ArrowStyle
import com.flyerpix.editor.canvas.model.PenLayer
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.canvas.model.StrokeStyle
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
    private val onPanelChanged: () -> Unit = {},
    private val onShapeCreated: ((ShapeLayer) -> Unit)? = null
) {
    companion object {
        const val OBJ_TEXT     = "obj_text"
        const val OBJ_STICKER  = "obj_sticker"
        const val OBJ_IMPORT   = "obj_import"
        const val OBJ_DRAW     = "obj_draw"
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

    /**
     * Callback saat salah satu Compose sheet Add dibuka/ditutup,
     * untuk menganimasikan navigasi bawah (translationY 56dp) dan fit canvas viewport.
     */
    var onAddSettingsOpenChanged: ((Boolean) -> Unit)? = null

    private fun computeSheetHeight(ratio: Float): Int {
        val displayMetrics = activity.resources.displayMetrics
        val density = displayMetrics.density
        val targetPx = (displayMetrics.heightPixels * ratio).toInt()
        val floorPx = (300 * density).toInt()
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
        if (tag == OBJ_TEXT) {
            canvas.addTextLayer("New Text")
            showSnackbar("Text layer added")
            return
        }
        if (tag == activeTag) deselect(restoreStrip = true) else select(tag)
    }

    fun select(tag: String) {
        activeTag = tag
        updateToolStripSelection(tag)
        when (tag) {
            OBJ_STICKER -> showComposeStickerSheet()
            OBJ_IMPORT  -> showComposeImportSheet()
            OBJ_DRAW    -> showComposeDrawSheet()
            OBJ_SHAPES  -> showComposeShapeSheet()
            OBJ_BEZIER  -> showComposeBezierSheet()
            OBJ_ARROW   -> showComposeArrowSheet()
        }
        onAddSettingsOpenChanged?.invoke(true)
        onPanelChanged()
    }

    fun deselect(restoreStrip: Boolean = false) {
        activeTag = ""
        updateToolStripSelection("")

        // Cleanup draft layers if cancelled
        draftShape = null
        draftArrow = null
        draftPen = null
        canvas.freeDrawEnabled = false

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
        if (activeTag.isEmpty()) {
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
            val color = bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE)
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
    // 1. Sticker Sheet (~68% Screen Height)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeStickerSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeSheetHeight(0.68f)
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            StickerDetailPage(
                onStickerSelected = { stickerItem ->
                    canvas.addEmojiLayer(stickerItem.emoji)
                    showSnackbar("Added ${stickerItem.emoji} to canvas")
                },
                onClose = { deselect(restoreStrip = true) },
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

        val sheetMaxH = computeSheetHeight(0.38f)
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

        val sheetMaxH = computeSheetHeight(0.58f)
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
    // 3. Free Draw Studio Sheet (~48% Screen Height)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeDrawSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeSheetHeight(0.48f)
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        canvas.freeDrawEnabled = true

        host.setContent {
            var currentBrushSize by remember { mutableStateOf(canvas.freeDrawStrokeWidth) }
            var currentBrushColor by remember { mutableStateOf(canvas.freeDrawColor) }

            DrawDetailPage(
                brushSize = currentBrushSize,
                brushColor = currentBrushColor,
                onBrushSizeChange = { size ->
                    currentBrushSize = size
                    canvas.freeDrawStrokeWidth = size
                },
                onBrushColorChange = { color ->
                    currentBrushColor = color
                    canvas.freeDrawColor = color
                },
                onOpenColorPicker = {
                    ColorPickerDialog.newInstance(
                        initialColor = canvas.freeDrawColor,
                        resultKey = DRAW_COLOR_RESULT_KEY
                    ).show(fragmentManager, "DrawColorPicker")
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
    // 4. Import Sheet (~38% Screen Height)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeImportSheet() {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeSheetHeight(0.38f)
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
                onClose = { deselect(restoreStrip = true) },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Arrow Studio Sheet (~42% Screen Height)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeArrowSheet(existingArrow: ArrowLayer? = null) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeSheetHeight(0.48f)
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        val arrow = existingArrow ?: draftArrow ?: canvas.addArrowLayer().also {
            canvas.selectedLayer = it
        }
        draftArrow = arrow

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
    // 6. Bézier Curve Sheet (~42% Screen Height)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showComposeBezierSheet(existingPen: PenLayer? = null) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeSheetHeight(0.42f)
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        val pen = existingPen ?: draftPen ?: canvas.addPenLayer().also {
            canvas.selectedLayer = it
        }
        draftPen = pen

        host.setContent {
            var currentWidth by remember { mutableStateOf(pen.strokeWidth) }
            var currentColor by remember { mutableStateOf(pen.strokeColor) }

            BezierDetailPage(
                strokeWidth = currentWidth,
                strokeColor = currentColor,
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
                onApply = {
                    canvas.runRecordedAction("Add Bézier") {}
                    showSnackbar("Bézier curve added")
                    draftPen = null
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    canvas.removeLayer(pen)
                    canvas.invalidate()
                    draftPen = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }
}
