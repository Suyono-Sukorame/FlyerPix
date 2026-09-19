package com.flyerpix.editor.ui.controller

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
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
import com.flyerpix.editor.canvas.model.Box3DLayer
import com.flyerpix.editor.canvas.model.Box3DPerspectiveMode
import com.flyerpix.editor.canvas.model.Cylinder3DLayer
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.PenLayer
import com.flyerpix.editor.canvas.model.PodiumStyle
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.canvas.model.Sphere3DLayer
import com.flyerpix.editor.canvas.model.SphereMaterial
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
        const val OBJ_3D_BOX   = "obj_3d_box"
        const val OBJ_3D_SPHERE = "3d_sphere"
        const val OBJ_3D_CYLINDER = "3d_cylinder"

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY   = 0xFF616161.toInt()

        private const val SHAPE_FILL_RESULT_KEY = "obj_shape_fill_color_key"
        private const val SHAPE_STROKE_RESULT_KEY = "obj_shape_stroke_color_key"
        private const val DRAW_COLOR_RESULT_KEY = "obj_draw_color_key"
        private const val ARROW_COLOR_RESULT_KEY = "obj_arrow_color_key"
        private const val BEZIER_COLOR_RESULT_KEY = "obj_bezier_color_key"
        private const val BOX_BASE_COLOR_RESULT_KEY = "obj_box_base_color_key"
        private const val BOX_STROKE_COLOR_RESULT_KEY = "obj_box_stroke_color_key"
        private const val SPHERE_BASE_COLOR_RESULT_KEY = "obj_sphere_base_color_key"
        private const val SPHERE_STROKE_COLOR_RESULT_KEY = "obj_sphere_stroke_color_key"
        private const val SPHERE_NEON_COLOR_RESULT_KEY = "obj_sphere_neon_color_key"
        private const val CYLINDER_BASE_COLOR_RESULT_KEY = "obj_cylinder_base_color_key"
        private const val CYLINDER_TOP_COLOR_RESULT_KEY = "obj_cylinder_top_color_key"
        private const val CYLINDER_RING_COLOR_RESULT_KEY = "obj_cylinder_ring_color_key"
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

    // Snapshot tambahan untuk rollback Cancel dari effect sheets
    private var shapeSnapshotShadowEnabled: Boolean = false
    private var shapeSnapshotShadowColor: Int = 0xFF000000.toInt()
    private var shapeSnapshotShadowRadius: Float = 8f
    private var shapeSnapshotShadowOpacity: Float = 0.6f
    private var shapeSnapshotShadowDx: Float = 4f
    private var shapeSnapshotShadowDy: Float = 4f
    private var shapeSnapshotNeonEnabled: Boolean = false
    private var shapeSnapshotNeonColor: Int = 0xFF00BFFF.toInt()
    private var shapeSnapshotNeonRadius: Float = 10f
    private var shapeSnapshotNeonIntensity: Float = 1f
    private var shapeSnapshotEmbossEnabled: Boolean = false
    private var shapeSnapshotGradientEnabled: Boolean = false

    private var draftArrow: ArrowLayer? = null
    private var draftPen: PenLayer? = null
    private val bezierFlow = BezierInputFlow()

    // Draft layer 3D Box (untuk instant-create & rollback)
    private var draftBox: Box3DLayer? = null
    private var isNewBox: Boolean = false
    private var boxSnapshot: Box3DLayer? = null

    // Draft layer 3D Sphere (untuk instant-create & rollback)
    private var draftSphere: Sphere3DLayer? = null
    private var isNewSphere: Boolean = false
    private var sphereSnapshot: Sphere3DLayer? = null

    // Draft layer 3D Cylinder / Podium (untuk instant-create & rollback)
    private var draftCylinder: Cylinder3DLayer? = null
    private var isNewCylinder: Boolean = false
    private var cylinderSnapshot: Cylinder3DLayer? = null

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
        val floorPx = (300 * density).toInt()
        val targetPx = (activity.resources.displayMetrics.heightPixels * 0.40f).toInt()
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
            Spec(OBJ_ARROW,    "Arrow",   R.drawable.ic_arrow_24px),
            Spec(OBJ_3D_BOX,   "3D Box",  R.drawable.ic_3d_box_24px),
            Spec(OBJ_3D_SPHERE, "3D Sphere", R.drawable.ic_sphere_3d_24px),
            Spec(OBJ_3D_CYLINDER, "Podium 3D", R.drawable.ic_podium_3d_24px)
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
            OBJ_3D_BOX  -> showComposeBox3DSheet()
            OBJ_3D_SPHERE -> showComposeSphere3DSheet()
            OBJ_3D_CYLINDER -> showComposeCylinder3DSheet()
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
        draftBox = null
        boxSnapshot = null
        draftSphere = null
        isNewSphere = false
        sphereSnapshot = null
        draftCylinder = null
        isNewCylinder = false
        cylinderSnapshot = null
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

    /**
     * Mengambil warna solid efektif dari hasil [ColorPickerDialog].
     *
     * Bila user memilih tab Gradient, tidak ada [ColorPickerDialog.EXTRA_COLOR],
     * sehingga dipakai stop pertama dari gradasi — supaya layer tidak diam-diam
     * jatuh ke warna default. Layer yang mendukung gradient tetap memakai warna
     * ini sebagai fallback solid saat efek aktif.
     */
    private fun bundleSolidColor(bundle: Bundle, fallback: Int): Int {
        val isGradient = bundle.getBoolean(ColorPickerDialog.EXTRA_IS_GRADIENT, false)
        if (isGradient) {
            @Suppress("DEPRECATION")
            val gradient = bundle.getSerializable(ColorPickerDialog.EXTRA_GRADIENT) as? GradientColor
            return gradient?.colors?.firstOrNull() ?: fallback
        }
        return bundle.getInt(ColorPickerDialog.EXTRA_COLOR, fallback)
    }

    private fun setupColorResultListeners() {
        fragmentManager.setFragmentResultListener(SHAPE_FILL_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            val target = (canvas.selectedLayer as? ShapeLayer) ?: draftShape
            target?.let {
                it.fillColor = color
                canvas.invalidate()
                showComposeShapeSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(SHAPE_STROKE_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.BLACK)
            val target = (canvas.selectedLayer as? ShapeLayer) ?: draftShape
            target?.let {
                it.strokeColor = color
                canvas.invalidate()
                showComposeShapeSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(DRAW_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            liveDrawBrushColor.value = color
            canvas.freeDrawColor = color
            showComposeDrawSheet()
        }
        fragmentManager.setFragmentResultListener(ARROW_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            draftArrow?.let {
                it.headColor = color
                it.tailColor = color
                canvas.invalidate()
                showComposeArrowSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(BEZIER_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            draftPen?.let {
                it.strokeColor = color
                canvas.invalidate()
                showComposeBezierSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(BOX_BASE_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            val target = (canvas.selectedLayer as? Box3DLayer) ?: draftBox
            target?.let {
                it.baseColor = color
                it.customFaceColors = null
                canvas.invalidate()
                showComposeBox3DSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(BOX_STROKE_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.BLACK)
            val target = (canvas.selectedLayer as? Box3DLayer) ?: draftBox
            target?.let {
                it.strokeColor = color
                canvas.invalidate()
                showComposeBox3DSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(SPHERE_BASE_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            val target = (canvas.selectedLayer as? Sphere3DLayer) ?: draftSphere
            target?.let {
                it.baseColor = color
                canvas.invalidate()
                showComposeSphere3DSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(SPHERE_STROKE_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.BLACK)
            val target = (canvas.selectedLayer as? Sphere3DLayer) ?: draftSphere
            target?.let {
                it.strokeColor = color
                canvas.invalidate()
                showComposeSphere3DSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(SPHERE_NEON_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.BLACK)
            val target = (canvas.selectedLayer as? Sphere3DLayer) ?: draftSphere
            target?.let {
                it.neonColor = color
                canvas.invalidate()
                showComposeSphere3DSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(CYLINDER_BASE_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            val target = (canvas.selectedLayer as? Cylinder3DLayer) ?: draftCylinder
            target?.let {
                it.baseColor = color
                canvas.invalidate()
                showComposeCylinder3DSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(CYLINDER_TOP_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            val target = (canvas.selectedLayer as? Cylinder3DLayer) ?: draftCylinder
            target?.let {
                it.topColor = color
                canvas.invalidate()
                showComposeCylinder3DSheet(it)
            }
        }
        fragmentManager.setFragmentResultListener(CYLINDER_RING_COLOR_RESULT_KEY, activity) { _, bundle ->
            val color = bundleSolidColor(bundle, Color.WHITE)
            val target = (canvas.selectedLayer as? Cylinder3DLayer) ?: draftCylinder
            target?.let {
                it.topRingColor = color
                canvas.invalidate()
                showComposeCylinder3DSheet(it)
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
    // 2. Shape Studio (alur Instant, ala PixelLab Original)
    //    Saat menu Shapes ditekan, shape default (RECTANGLE) langsung muncul di
    //    kanvas + panel pengaturan lengkap terbuka instan. Pemilihan tipe bentuk
    //    dilakukan lewat pop-up dialog floating (ShapePickerPopupDialog).
    // ─────────────────────────────────────────────────────────────────────────

    fun showComposeShapeSheet(shapeToEdit: ShapeLayer? = null) {
        val shape = shapeToEdit ?: draftShape ?: createNewDraftShape()
        showComposeShapeDetail(shape)
    }

    /**
     * Membuat shape default baru di kanvas, memilihnya, dan mem-backup state awal
     * untuk rollback saat Cancel (isNewShape = true → Cancel menghapus layer).
     */
    private fun createNewDraftShape(): ShapeLayer {
        val shape = canvas.addShapeLayer(ShapeType.RECTANGLE).also {
            isNewShape = true
            canvas.selectedLayer = it
            onShapeCreated?.invoke(it)
        }
        draftShape = shape
        backupShapeSnapshot(shape)
        return shape
    }

    private fun backupShapeSnapshot(shape: ShapeLayer) {
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
        shapeSnapshotShadowEnabled = shape.shadowEnabled
        shapeSnapshotShadowColor   = shape.shadowColor
        shapeSnapshotShadowRadius  = shape.shadowRadius
        shapeSnapshotShadowOpacity = shape.shadowOpacity
        shapeSnapshotShadowDx      = shape.shadowDx
        shapeSnapshotShadowDy      = shape.shadowDy
        shapeSnapshotNeonEnabled   = shape.neonEnabled
        shapeSnapshotNeonColor     = shape.neonColor
        shapeSnapshotNeonRadius    = shape.neonRadius
        shapeSnapshotNeonIntensity = shape.neonIntensity
        shapeSnapshotEmbossEnabled = shape.embossEnabled
        shapeSnapshotGradientEnabled = shape.gradientEnabled
    }

    /**
     * Detail editing shape (warna, stroke, opacity, dsb). Dipanggil baik untuk
     * mengedit shape lama maupun untuk shape baru yang baru dibuat.
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

        // Draft selalu mengikat layer yang sedang aktif diedit (bukan terpatok
        // layer pertama). Snapshot diperbarui tiap kali detail dibuka agar
        // rollback Cancel akurat. isNewShape hanya benar untuk shape yang baru
        // dibuat studio — editing layer lama selalu reset ke false.
        val switchingShape = draftShape !== shape
        draftShape = shape
        if (switchingShape) {
            isNewShape = false
        }
        backupShapeSnapshot(shape)

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
            var currentStarPoints by remember { mutableStateOf(shape.starPoints) }
            var currentStarInnerRatio by remember { mutableStateOf(shape.starInnerRadiusRatio) }

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
                starPoints = currentStarPoints,
                starInnerRatio = currentStarInnerRatio,
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
                    val original = shape.fillColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = shape.fillColor,
                        resultKey = SHAPE_FILL_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        shape.fillColor = color
                        canvas.invalidate()
                    }
                    dialog.onGradientChanged = { gradient ->
                        shape.fillColor = gradient.colors.firstOrNull() ?: original
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        shape.fillColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "ShapeFillColorPicker")
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
                    val original = shape.strokeColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = shape.strokeColor,
                        resultKey = SHAPE_STROKE_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        shape.strokeColor = color
                        canvas.invalidate()
                    }
                    dialog.onGradientChanged = { gradient ->
                        shape.strokeColor = gradient.colors.firstOrNull() ?: original
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        shape.strokeColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "ShapeStrokeColorPicker")
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
                onStarPointsChange = { pts ->
                    currentStarPoints = pts
                    shape.starPoints = pts
                    canvas.invalidate()
                },
                onStarInnerRatioChange = { ratio ->
                    currentStarInnerRatio = ratio
                    shape.starInnerRadiusRatio = ratio
                    canvas.invalidate()
                },
                onOpenShadowEditor = { showShapeShadowSheet(shape) },
                onOpenNeonEditor = { showShapeNeonSheet(shape) },
                onOpenEmbossEditor = { showShapeEmbossSheet(shape) },
                onOpenGradientEditor = { showShapeGradientSheet(shape) },
                onApply = {
                    canvas.runRecordedAction(if (isNewShape) "Add Shape" else "Modify Shape") {}
                    showSnackbar("Shape saved")
                    draftShape = null
                    isNewShape = false
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
                        shape.shadowEnabled = shapeSnapshotShadowEnabled
                        shape.shadowColor = shapeSnapshotShadowColor
                        shape.shadowRadius = shapeSnapshotShadowRadius
                        shape.shadowOpacity = shapeSnapshotShadowOpacity
                        shape.shadowDx = shapeSnapshotShadowDx
                        shape.shadowDy = shapeSnapshotShadowDy
                        shape.neonEnabled = shapeSnapshotNeonEnabled
                        shape.neonColor = shapeSnapshotNeonColor
                        shape.neonRadius = shapeSnapshotNeonRadius
                        shape.neonIntensity = shapeSnapshotNeonIntensity
                        shape.embossEnabled = shapeSnapshotEmbossEnabled
                        shape.gradientEnabled = shapeSnapshotGradientEnabled
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
    // 2.1 Effect Sheets dari Shape Panel (Shadow / Neon / Emboss / Gradient)
    //     Alur linear: sheet efek menggantikan Shape panel. Apply/Cancel selalu
    //     kembali ke Shape panel. Cancel melakukan rollback snapshot efek.
    // ─────────────────────────────────────────────────────────────────────────

    private fun showShapeShadowSheet(shape: ShapeLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            ShadowDetailPage(
                title = "Shape Shadow",
                enabled = shape.shadowEnabled,
                color = shape.shadowColor,
                radius = shape.shadowRadius.coerceIn(0f, 40f),
                opacityPct = (shape.shadowOpacity * 100f).coerceIn(0f, 100f),
                dx = shape.shadowDx.coerceIn(-30f, 30f),
                dy = shape.shadowDy.coerceIn(-30f, 30f),
                onEnabledChange = { en ->
                    shape.shadowEnabled = en
                    canvas.invalidate()
                },
                onColorChange = { c ->
                    shape.shadowColor = c
                    canvas.invalidate()
                },
                onColorPickRequested = {
                    val original = shape.shadowColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = shape.shadowColor,
                        resultKey = "shape_shadow_color_key"
                    )
                    dialog.onColorChanged = { c ->
                        shape.shadowColor = c
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        shape.shadowColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "ShapeShadowColorPicker")
                },
                onRadiusChange = { r ->
                    shape.shadowRadius = r
                    canvas.invalidate()
                },
                onOpacityChange = { opPct ->
                    shape.shadowOpacity = (opPct / 100f).coerceIn(0f, 1f)
                    canvas.invalidate()
                },
                onDxChange = { x ->
                    shape.shadowDx = x
                    canvas.invalidate()
                },
                onDyChange = { y ->
                    shape.shadowDy = y
                    canvas.invalidate()
                },
                onReset = {
                    shape.shadowEnabled = true
                    shape.shadowColor = 0xFF000000.toInt()
                    shape.shadowRadius = 10f
                    shape.shadowOpacity = 0.6f
                    shape.shadowDx = 0f
                    shape.shadowDy = 0f
                    canvas.invalidate()
                },
                onApply = { showComposeShapeSheet(shape) },
                onCancel = {
                    shape.shadowEnabled = shapeSnapshotShadowEnabled
                    shape.shadowColor = shapeSnapshotShadowColor
                    shape.shadowRadius = shapeSnapshotShadowRadius
                    shape.shadowOpacity = shapeSnapshotShadowOpacity
                    shape.shadowDx = shapeSnapshotShadowDx
                    shape.shadowDy = shapeSnapshotShadowDy
                    canvas.invalidate()
                    showComposeShapeSheet(shape)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showShapeNeonSheet(shape: ShapeLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            NeonDetailPage(
                enabled = shape.neonEnabled,
                color = shape.neonColor,
                radius = shape.neonRadius.coerceIn(1f, 40f),
                intensity = shape.neonIntensity,
                coreEnabled = shape.neonCoreEnabled,
                onEnabledChange = { en ->
                    shape.neonEnabled = en
                    canvas.invalidate()
                },
                onColorChange = { c ->
                    shape.neonColor = c
                    canvas.invalidate()
                },
                onColorPickRequested = {
                    val original = shape.neonColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = shape.neonColor,
                        resultKey = "shape_neon_color_key"
                    )
                    dialog.onColorChanged = { c ->
                        shape.neonColor = c
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        shape.neonColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "ShapeNeonColorPicker")
                },
                onRadiusChange = { r ->
                    shape.neonRadius = r
                    canvas.invalidate()
                },
                onIntensityChange = { i ->
                    shape.neonIntensity = i
                    canvas.invalidate()
                },
                onCoreEnabledChange = { en ->
                    shape.neonCoreEnabled = en
                    canvas.invalidate()
                },
                onReset = {
                    shape.neonEnabled = true
                    shape.neonColor = 0xFF00E5FF.toInt()
                    shape.neonRadius = 12f
                    shape.neonIntensity = 1f
                    shape.neonCoreEnabled = true
                    canvas.invalidate()
                },
                onApply = { showComposeShapeSheet(shape) },
                onCancel = {
                    shape.neonEnabled = shapeSnapshotNeonEnabled
                    shape.neonColor = shapeSnapshotNeonColor
                    shape.neonRadius = shapeSnapshotNeonRadius
                    shape.neonIntensity = shapeSnapshotNeonIntensity
                    canvas.invalidate()
                    showComposeShapeSheet(shape)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showShapeEmbossSheet(shape: ShapeLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            EmbossDetailPage(
                enabled = shape.embossEnabled,
                lightAngle = shape.embossLightAngle,
                intensity = shape.embossIntensity,
                ambient = shape.embossAmbient,
                specular = shape.embossSpecular,
                bevel = shape.embossBevel,
                onEnabledChange = { en ->
                    shape.embossEnabled = en
                    canvas.invalidate()
                },
                onLightAngleChange = { v ->
                    shape.embossLightAngle = v
                    canvas.invalidate()
                },
                onIntensityChange = { v ->
                    shape.embossIntensity = v
                    canvas.invalidate()
                },
                onAmbientChange = { v ->
                    shape.embossAmbient = v
                    canvas.invalidate()
                },
                onSpecularChange = { v ->
                    shape.embossSpecular = v
                    canvas.invalidate()
                },
                onBevelChange = { v ->
                    shape.embossBevel = v
                    canvas.invalidate()
                },
                onReset = {
                    shape.embossEnabled = true
                    shape.embossLightAngle = 90f
                    shape.embossIntensity = 1f
                    shape.embossAmbient = 0.5f
                    shape.embossSpecular = 10f
                    shape.embossBevel = 3f
                    canvas.invalidate()
                },
                onApply = { showComposeShapeSheet(shape) },
                onCancel = {
                    shape.embossEnabled = shapeSnapshotEmbossEnabled
                    canvas.invalidate()
                    showComposeShapeSheet(shape)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showShapeGradientSheet(shape: ShapeLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        val grad = shape.gradient
        val c1 = grad?.colors?.getOrNull(0) ?: Color.WHITE
        val c2 = if (grad != null && grad.colors.size > 1) grad.colors[1] else Color.BLACK
        val fallbackType = GradientType.LINEAR

        host.setContent {
            GradientDetailPage(
                enabled = shape.gradientEnabled,
                color1 = c1,
                color2 = c2,
                type = grad?.type ?: fallbackType,
                angle = grad?.angle ?: 0f,
                activePreset = grad?.name?.takeIf { it.isNotBlank() },
                onEnabledChange = { en ->
                    if (en && shape.gradient == null) shape.gradient = defaultGradient()
                    shape.gradientEnabled = en
                    canvas.invalidate()
                },
                onTypeChange = { t ->
                    val cur = shape.gradient ?: defaultGradient()
                    if (cur.type != t) shape.gradient = cur.copy(type = t, name = "")
                    shape.gradientEnabled = true
                    canvas.invalidate()
                },
                onColor1Change = { c -> setGradientColorAt(shape, 0, c) },
                onColor2Change = { c -> setGradientColorAt(shape, 1, c) },
                onAngleChange = { a ->
                    val cur = shape.gradient ?: defaultGradient()
                    shape.gradient = cur.copy(angle = a, name = "")
                    shape.gradientEnabled = true
                    canvas.invalidate()
                },
                onPreset = { p ->
                    val colors = com.flyerpix.editor.ui.compose.gradientPresetColors(p)
                    shape.gradient = (shape.gradient ?: defaultGradient()).copy(
                        colors = colors.copyOf(),
                        positions = null,
                        type = GradientType.LINEAR,
                        angle = 0f,
                        name = p
                    )
                    shape.gradientEnabled = true
                    canvas.invalidate()
                },
                onReset = {
                    shape.gradient = null
                    shape.gradientEnabled = false
                    canvas.invalidate()
                },
                onApply = { showComposeShapeSheet(shape) },
                onCancel = {
                    shape.gradientEnabled = shapeSnapshotGradientEnabled
                    canvas.invalidate()
                    showComposeShapeSheet(shape)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun defaultGradient(): GradientColor =
        GradientColor(
            colors = intArrayOf(Color.WHITE, Color.BLACK),
            type = GradientType.LINEAR,
            angle = 0f,
            name = ""
        )

    private fun setGradientColorAt(shape: ShapeLayer, index: Int, color: Int) {
        val cur = shape.gradient ?: defaultGradient()
        val base = cur.colors
        val newColors = if (base.size >= 2) base.copyOf() else IntArray(2) { i ->
            when {
                i == 0 && base.isNotEmpty() -> base[0]
                i == 0 -> Color.WHITE
                else -> Color.BLACK
            }
        }
        if (index < newColors.size) newColors[index] = color
        shape.gradient = cur.copy(colors = newColors)
        shape.gradientEnabled = true
        canvas.invalidate()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.5 3D Box Studio (objek geometri 3D Box)
    //     Alur instant: menu 3D Box ditekan → balok isometrik langsung muncul di
    //     kanvas + panel pengaturan lengkap terbuka instan.
    // ─────────────────────────────────────────────────────────────────────────

    fun showComposeBox3DSheet(boxToEdit: Box3DLayer? = null) {
        val box = boxToEdit ?: draftBox ?: createNewDraftBox()
        showComposeBox3DDetail(box)
    }

    private fun createNewDraftBox(): Box3DLayer {
        val box = canvas.addBox3DLayer().also {
            isNewBox = true
        }
        draftBox = box
        boxSnapshot = box.copyLayer()
        canvas.selectedLayer = box
        return box
    }

    private fun showComposeBox3DDetail(box: Box3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = OBJ_3D_BOX
        updateToolStripSelection(OBJ_3D_BOX)

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        val switchingBox = draftBox !== box
        draftBox = box
        if (switchingBox) isNewBox = false
        boxSnapshot = box.copyLayer()

        host.setContent {
            var currentMode by remember { mutableStateOf(box.perspectiveMode) }
            var currentWidth by remember { mutableStateOf(box.boxWidth) }
            var currentHeight by remember { mutableStateOf(box.boxHeight) }
            var currentDepth by remember { mutableStateOf(box.boxDepth) }
            var currentAngleX by remember { mutableStateOf(box.angleX) }
            var currentAngleY by remember { mutableStateOf(box.angleY) }
            var currentBaseColor by remember { mutableStateOf(box.baseColor) }
            var currentAutoShading by remember { mutableStateOf(box.autoShadingEnabled) }
            var currentFaceOpacity by remember { mutableStateOf(box.faceOpacity / 255f * 100f) }
            var currentStrokeWidth by remember { mutableStateOf(box.strokeWidth) }
            var currentStrokeColor by remember { mutableStateOf(box.strokeColor) }
            var currentStrokeOpacity by remember { mutableStateOf(box.strokeOpacity / 255f * 100f) }
            var currentShowHidden by remember { mutableStateOf(box.showHiddenEdges) }

            Box3DDetailPage(
                mode = currentMode,
                widthD = currentWidth,
                heightD = currentHeight,
                depthD = currentDepth,
                angleX = currentAngleX,
                angleY = currentAngleY,
                baseColor = currentBaseColor,
                autoShading = currentAutoShading,
                faceOpacity = currentFaceOpacity,
                strokeWidth = currentStrokeWidth,
                strokeColor = currentStrokeColor,
                strokeOpacity = currentStrokeOpacity,
                showHiddenEdges = currentShowHidden,
                onModeChange = { m ->
                    currentMode = m
                    box.perspectiveMode = m
                    canvas.invalidate()
                },
                onWidthChange = { v ->
                    currentWidth = v
                    box.boxWidth = v
                    canvas.invalidate()
                },
                onHeightChange = { v ->
                    currentHeight = v
                    box.boxHeight = v
                    canvas.invalidate()
                },
                onDepthChange = { v ->
                    currentDepth = v
                    box.boxDepth = v
                    canvas.invalidate()
                },
                onAngleXChange = { v ->
                    currentAngleX = v
                    box.angleX = v
                    canvas.invalidate()
                },
                onAngleYChange = { v ->
                    currentAngleY = v
                    box.angleY = v
                    canvas.invalidate()
                },
                onBaseColorChange = { c ->
                    currentBaseColor = c
                    box.baseColor = c
                    box.customFaceColors = null
                    canvas.invalidate()
                },
                onOpenBaseColorPicker = {
                    val original = box.baseColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = box.baseColor,
                        resultKey = BOX_BASE_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        box.baseColor = color
                        box.customFaceColors = null
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        box.baseColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "BoxBaseColorPicker")
                },
                onAutoShadingChange = { en ->
                    currentAutoShading = en
                    box.autoShadingEnabled = en
                    if (en) box.customFaceColors = null
                    canvas.invalidate()
                },
                onFaceOpacityChange = { pct ->
                    currentFaceOpacity = pct
                    box.faceOpacity = (pct / 100f * 255f).toInt()
                    canvas.invalidate()
                },
                onStrokeWidthChange = { v ->
                    currentStrokeWidth = v
                    box.strokeWidth = v
                    canvas.invalidate()
                },
                onStrokeColorChange = { c ->
                    currentStrokeColor = c
                    box.strokeColor = c
                    canvas.invalidate()
                },
                onOpenStrokeColorPicker = {
                    val original = box.strokeColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = box.strokeColor,
                        resultKey = BOX_STROKE_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        box.strokeColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        box.strokeColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "BoxStrokeColorPicker")
                },
                onStrokeOpacityChange = { pct ->
                    currentStrokeOpacity = pct
                    box.strokeOpacity = (pct / 100f * 255f).toInt()
                    canvas.invalidate()
                },
                onShowHiddenEdgesChange = { en ->
                    currentShowHidden = en
                    box.showHiddenEdges = en
                    canvas.invalidate()
                },
                onOpenShadowEditor = { showBox3DShadowSheet(box) },
                onOpenNeonEditor = { showBox3DNeonSheet(box) },
                onOpenEmbossEditor = { showBox3DEmbossSheet(box) },
                onReset = {
                    box.perspectiveMode = Box3DPerspectiveMode.ISOMETRIC
                    box.boxWidth = 220f
                    box.boxHeight = 180f
                    box.boxDepth = 220f
                    box.angleX = 0f
                    box.angleY = 0f
                    box.baseColor = 0xFF1769FF.toInt()
                    box.autoShadingEnabled = true
                    box.customFaceColors = null
                    box.faceOpacity = 255
                    box.strokeWidth = 0f
                    box.strokeColor = android.graphics.Color.BLACK
                    box.strokeOpacity = 255
                    box.showHiddenEdges = false
                    box.shadowEnabled = false
                    box.neonEnabled = false
                    box.embossEnabled = false
                    canvas.invalidate()
                },
                onApply = {
                    canvas.runRecordedAction(if (isNewBox) "Add 3D Box" else "Modify 3D Box") {}
                    showSnackbar("3D Box saved")
                    draftBox = null
                    isNewBox = false
                    boxSnapshot = null
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    if (isNewBox) {
                        canvas.removeLayer(box)
                    } else {
                        boxSnapshot?.let { snap ->
                            box.perspectiveMode = snap.perspectiveMode
                            box.boxWidth = snap.boxWidth
                            box.boxHeight = snap.boxHeight
                            box.boxDepth = snap.boxDepth
                            box.angleX = snap.angleX
                            box.angleY = snap.angleY
                            box.baseColor = snap.baseColor
                            box.autoShadingEnabled = snap.autoShadingEnabled
                            box.customFaceColors = snap.customFaceColors?.clone()
                            box.faceOpacity = snap.faceOpacity
                            box.strokeWidth = snap.strokeWidth
                            box.strokeColor = snap.strokeColor
                            box.strokeOpacity = snap.strokeOpacity
                            box.showHiddenEdges = snap.showHiddenEdges
                            box.shadowEnabled = snap.shadowEnabled
                            box.neonEnabled = snap.neonEnabled
                            box.embossEnabled = snap.embossEnabled
                        }
                    }
                    canvas.invalidate()
                    draftBox = null
                    isNewBox = false
                    boxSnapshot = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ── Effect sheets dari panel 3D Box ──────────────────────────────────────

    private fun showBox3DShadowSheet(box: Box3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            ShadowDetailPage(
                title = "Box Shadow",
                enabled = box.shadowEnabled,
                color = box.shadowColor,
                radius = box.shadowRadius.coerceIn(0f, 40f),
                opacityPct = (box.shadowOpacity * 100f).coerceIn(0f, 100f),
                dx = box.shadowDx.coerceIn(-30f, 30f),
                dy = box.shadowDy.coerceIn(-30f, 30f),
                onEnabledChange = { en -> box.shadowEnabled = en; canvas.invalidate() },
                onColorChange = { c -> box.shadowColor = c; canvas.invalidate() },
                onColorPickRequested = {
                    val original = box.shadowColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = box.shadowColor,
                        resultKey = "box_shadow_color_key"
                    )
                    dialog.onColorChanged = { c -> box.shadowColor = c; canvas.invalidate() }
                    dialog.onCancel = { box.shadowColor = original; canvas.invalidate() }
                    dialog.show(fragmentManager, "BoxShadowColorPicker")
                },
                onRadiusChange = { r -> box.shadowRadius = r; canvas.invalidate() },
                onOpacityChange = { opPct -> box.shadowOpacity = (opPct / 100f).coerceIn(0f, 1f); canvas.invalidate() },
                onDxChange = { x -> box.shadowDx = x; canvas.invalidate() },
                onDyChange = { y -> box.shadowDy = y; canvas.invalidate() },
                onReset = {
                    box.shadowEnabled = true
                    box.shadowColor = 0xFF000000.toInt()
                    box.shadowRadius = 10f
                    box.shadowOpacity = 0.6f
                    box.shadowDx = 0f
                    box.shadowDy = 0f
                    canvas.invalidate()
                },
                onApply = { showComposeBox3DSheet(box) },
                onCancel = {
                    boxSnapshot?.let { snap ->
                        box.shadowEnabled = snap.shadowEnabled
                        box.shadowColor = snap.shadowColor
                        box.shadowRadius = snap.shadowRadius
                        box.shadowOpacity = snap.shadowOpacity
                        box.shadowDx = snap.shadowDx
                        box.shadowDy = snap.shadowDy
                    }
                    canvas.invalidate()
                    showComposeBox3DSheet(box)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showBox3DNeonSheet(box: Box3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            NeonDetailPage(
                enabled = box.neonEnabled,
                color = box.neonColor,
                radius = box.neonRadius.coerceIn(1f, 40f),
                intensity = box.neonIntensity,
                coreEnabled = box.neonCoreEnabled,
                onEnabledChange = { en -> box.neonEnabled = en; canvas.invalidate() },
                onColorChange = { c -> box.neonColor = c; canvas.invalidate() },
                onColorPickRequested = {
                    val original = box.neonColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = box.neonColor,
                        resultKey = "box_neon_color_key"
                    )
                    dialog.onColorChanged = { c -> box.neonColor = c; canvas.invalidate() }
                    dialog.onCancel = { box.neonColor = original; canvas.invalidate() }
                    dialog.show(fragmentManager, "BoxNeonColorPicker")
                },
                onRadiusChange = { r -> box.neonRadius = r; canvas.invalidate() },
                onIntensityChange = { i -> box.neonIntensity = i; canvas.invalidate() },
                onCoreEnabledChange = { en -> box.neonCoreEnabled = en; canvas.invalidate() },
                onReset = {
                    box.neonEnabled = true
                    box.neonColor = 0xFF00E5FF.toInt()
                    box.neonRadius = 12f
                    box.neonIntensity = 1f
                    box.neonCoreEnabled = true
                    canvas.invalidate()
                },
                onApply = { showComposeBox3DSheet(box) },
                onCancel = {
                    boxSnapshot?.let { snap ->
                        box.neonEnabled = snap.neonEnabled
                        box.neonColor = snap.neonColor
                        box.neonRadius = snap.neonRadius
                        box.neonIntensity = snap.neonIntensity
                        box.neonCoreEnabled = snap.neonCoreEnabled
                    }
                    canvas.invalidate()
                    showComposeBox3DSheet(box)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showBox3DEmbossSheet(box: Box3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            EmbossDetailPage(
                enabled = box.embossEnabled,
                lightAngle = box.embossLightAngle,
                ambient = box.embossAmbient,
                specular = box.embossSpecular,
                intensity = box.embossIntensity,
                bevel = box.embossBevel,
                onEnabledChange = { en -> box.embossEnabled = en; canvas.invalidate() },
                onLightAngleChange = { v -> box.embossLightAngle = v; canvas.invalidate() },
                onAmbientChange = { v -> box.embossAmbient = v; canvas.invalidate() },
                onSpecularChange = { v -> box.embossSpecular = v; canvas.invalidate() },
                onIntensityChange = { v -> box.embossIntensity = v; canvas.invalidate() },
                onBevelChange = { v -> box.embossBevel = v; canvas.invalidate() },
                onReset = {
                    box.embossEnabled = true
                    box.embossLightAngle = 45f
                    box.embossAmbient = 0.2f
                    box.embossSpecular = 8f
                    box.embossIntensity = 1f
                    box.embossBevel = 3f
                    canvas.invalidate()
                },
                onApply = { showComposeBox3DSheet(box) },
                onCancel = {
                    boxSnapshot?.let { snap ->
                        box.embossEnabled = snap.embossEnabled
                        box.embossLightAngle = snap.embossLightAngle
                        box.embossAmbient = snap.embossAmbient
                        box.embossSpecular = snap.embossSpecular
                        box.embossIntensity = snap.embossIntensity
                        box.embossBevel = snap.embossBevel
                    }
                    canvas.invalidate()
                    showComposeBox3DSheet(box)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.6 3D Sphere Studio (objek 3D orb dengan pencahayaan vektor)
    //     Alur instant: menu 3D Sphere ditekan → bola glossy langsung muncul di
    //     tengah kanvas + panel pengaturan (material, cahaya, lantai, dll.)
    // ─────────────────────────────────────────────────────────────────────────

    fun showComposeSphere3DSheet(sphereToEdit: Sphere3DLayer? = null) {
        val sphere = sphereToEdit ?: draftSphere ?: createNewDraftSphere()
        showComposeSphere3DDetail(sphere)
    }

    private fun createNewDraftSphere(): Sphere3DLayer {
        val sphere = canvas.addSphere3DLayer().also {
            isNewSphere = true
        }
        draftSphere = sphere
        sphereSnapshot = sphere.copyLayer()
        canvas.selectedLayer = sphere
        return sphere
    }

    private fun showComposeSphere3DDetail(sphere: Sphere3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = OBJ_3D_SPHERE
        updateToolStripSelection(OBJ_3D_SPHERE)

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        val switchingSphere = draftSphere !== sphere
        draftSphere = sphere
        if (switchingSphere) isNewSphere = false
        sphereSnapshot = sphere.copyLayer()

        host.setContent {
            var currentMaterial by remember { mutableStateOf(sphere.material) }
            var currentRadius by remember { mutableStateOf(sphere.radius) }
            var currentBaseColor by remember { mutableStateOf(sphere.baseColor) }
            var currentLightAngle by remember { mutableStateOf(sphere.lightAngle) }
            var currentLightDistance by remember { mutableStateOf(sphere.lightDistance) }
            var currentLightIntensity by remember { mutableStateOf(sphere.lightIntensity) }
            var currentOpacity by remember { mutableStateOf(sphere.opacity / 255f * 100f) }
            var currentFloorShadow by remember { mutableStateOf(sphere.floorShadowEnabled) }
            var currentFloorElevation by remember { mutableStateOf(sphere.floorElevation) }
            var currentFloorShadowOpacity by remember {
                mutableStateOf(sphere.floorShadowOpacity / 1f * 100f)
            }
            var currentStrokeWidth by remember { mutableStateOf(sphere.strokeWidth) }
            var currentStrokeColor by remember { mutableStateOf(sphere.strokeColor) }
            var currentStrokeOpacity by remember {
                mutableStateOf(sphere.strokeOpacity / 255f * 100f)
            }
            var currentNeonEnabled by remember { mutableStateOf(sphere.neonEnabled) }
            var currentNeonColor by remember { mutableStateOf(sphere.neonColor) }
            var currentNeonRadius by remember { mutableStateOf(sphere.neonRadius) }
            var currentNeonIntensity by remember { mutableStateOf(sphere.neonIntensity) }

            Sphere3DDetailPage(
                material = currentMaterial,
                diameter = currentRadius,
                baseColor = currentBaseColor,
                lightAngle = currentLightAngle,
                lightDistance = currentLightDistance,
                lightIntensity = currentLightIntensity,
                opacityPct = currentOpacity,
                floorShadowEnabled = currentFloorShadow,
                floorElevation = currentFloorElevation,
                floorShadowOpacityPct = currentFloorShadowOpacity,
                strokeWidth = currentStrokeWidth,
                strokeColor = currentStrokeColor,
                strokeOpacityPct = currentStrokeOpacity,
                neonEnabled = currentNeonEnabled,
                neonColor = currentNeonColor,
                neonRadius = currentNeonRadius,
                neonIntensity = currentNeonIntensity,
                onMaterialChange = { m ->
                    currentMaterial = m
                    sphere.material = m
                    canvas.invalidate()
                },
                onRadiusChange = { v ->
                    currentRadius = v
                    sphere.radius = v.coerceIn(2f, 2000f)
                    canvas.invalidate()
                },
                onBaseColorChange = { c ->
                    currentBaseColor = c
                    sphere.baseColor = c
                    canvas.invalidate()
                },
                onOpenBaseColorPicker = {
                    val original = sphere.baseColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = sphere.baseColor,
                        resultKey = SPHERE_BASE_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        sphere.baseColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        sphere.baseColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "SphereBaseColorPicker")
                },
                onLightAngleChange = { a ->
                    currentLightAngle = a
                    sphere.lightAngle = a
                    canvas.invalidate()
                },
                onLightDistanceChange = { d ->
                    currentLightDistance = d
                    sphere.lightDistance = d
                    canvas.invalidate()
                },
                onLightIntensityChange = { i ->
                    currentLightIntensity = i
                    sphere.lightIntensity = i
                    canvas.invalidate()
                },
                onOpacityChange = { pct ->
                    currentOpacity = pct
                    sphere.opacity = (pct / 100f * 255f).toInt().coerceIn(0, 255)
                    canvas.invalidate()
                },
                onFloorShadowEnabledChange = { en ->
                    currentFloorShadow = en
                    sphere.floorShadowEnabled = en
                    canvas.invalidate()
                },
                onFloorElevationChange = { v ->
                    currentFloorElevation = v
                    sphere.floorElevation = v
                    canvas.invalidate()
                },
                onFloorShadowOpacityChange = { pct ->
                    currentFloorShadowOpacity = pct
                    sphere.floorShadowOpacity = (pct / 100f).coerceIn(0f, 1f)
                    canvas.invalidate()
                },
                onStrokeWidthChange = { v ->
                    currentStrokeWidth = v
                    sphere.strokeWidth = v
                    canvas.invalidate()
                },
                onStrokeColorChange = { c ->
                    currentStrokeColor = c
                    sphere.strokeColor = c
                    canvas.invalidate()
                },
                onOpenStrokeColorPicker = {
                    val original = sphere.strokeColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = sphere.strokeColor,
                        resultKey = SPHERE_STROKE_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        sphere.strokeColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        sphere.strokeColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "SphereStrokeColorPicker")
                },
                onStrokeOpacityChange = { pct ->
                    currentStrokeOpacity = pct
                    sphere.strokeOpacity = (pct / 100f * 255f).toInt().coerceIn(0, 255)
                    canvas.invalidate()
                },
                onNeonEnabledChange = { en ->
                    currentNeonEnabled = en
                    sphere.neonEnabled = en
                    canvas.invalidate()
                },
                onNeonColorChange = { c ->
                    currentNeonColor = c
                    sphere.neonColor = c
                    canvas.invalidate()
                },
                onOpenNeonColorPicker = {
                    val original = sphere.neonColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = sphere.neonColor,
                        resultKey = SPHERE_NEON_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        sphere.neonColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        sphere.neonColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "SphereNeonColorPicker")
                },
                onNeonRadiusChange = { v ->
                    currentNeonRadius = v
                    sphere.neonRadius = v
                    canvas.invalidate()
                },
                onNeonIntensityChange = { v ->
                    currentNeonIntensity = v
                    sphere.neonIntensity = v
                    canvas.invalidate()
                },
                onReset = {
                    sphere.material = SphereMaterial.GLOSSY
                    sphere.radius = 120f
                    sphere.baseColor = 0xFF1769FF.toInt()
                    sphere.lightAngle = -45f
                    sphere.lightDistance = 0.45f
                    sphere.lightIntensity = 1f
                    sphere.opacity = 255
                    sphere.floorShadowEnabled = true
                    sphere.floorElevation = 20f
                    sphere.floorShadowOpacity = 0.5f
                    sphere.strokeWidth = 0f
                    sphere.strokeColor = android.graphics.Color.BLACK
                    sphere.strokeOpacity = 255
                    sphere.neonEnabled = false
                    sphere.neonColor = 0xFF00E5FF.toInt()
                    sphere.neonRadius = 16f
                    sphere.neonIntensity = 1f
                    canvas.invalidate()
                },
                onApply = {
                    canvas.runRecordedAction(if (isNewSphere) "Add 3D Sphere" else "Modify 3D Sphere") {}
                    showSnackbar("3D Sphere saved")
                    draftSphere = null
                    isNewSphere = false
                    sphereSnapshot = null
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    if (isNewSphere) {
                        canvas.removeLayer(sphere)
                    } else {
                        sphereSnapshot?.let { snap ->
                            sphere.material = snap.material
                            sphere.radius = snap.radius
                            sphere.baseColor = snap.baseColor
                            sphere.lightAngle = snap.lightAngle
                            sphere.lightDistance = snap.lightDistance
                            sphere.lightIntensity = snap.lightIntensity
                            sphere.opacity = snap.opacity
                            sphere.floorShadowEnabled = snap.floorShadowEnabled
                            sphere.floorElevation = snap.floorElevation
                            sphere.floorShadowOpacity = snap.floorShadowOpacity
                            sphere.strokeWidth = snap.strokeWidth
                            sphere.strokeColor = snap.strokeColor
                            sphere.strokeOpacity = snap.strokeOpacity
                            sphere.neonEnabled = snap.neonEnabled
                            sphere.neonColor = snap.neonColor
                            sphere.neonRadius = snap.neonRadius
                            sphere.neonIntensity = snap.neonIntensity
                        }
                    }
                    canvas.invalidate()
                    draftSphere = null
                    isNewSphere = false
                    sphereSnapshot = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.7 3D Cylinder / Podium Studio (panggung pemajang produk 3D)
    //     Alur instant: menu Podium 3D ditekan → podium silinder langsung muncul
    //     di kanvas + panel (gaya, level, ukuran, warna, ring, lantai, finishing).
    // ─────────────────────────────────────────────────────────────────────────

    fun showComposeCylinder3DSheet(cylinderToEdit: Cylinder3DLayer? = null) {
        val cylinder = cylinderToEdit ?: draftCylinder ?: createNewDraftCylinder()
        showComposeCylinder3DDetail(cylinder)
    }

    private fun createNewDraftCylinder(): Cylinder3DLayer {
        val cylinder = canvas.addCylinder3DLayer().also {
            isNewCylinder = true
        }
        draftCylinder = cylinder
        cylinderSnapshot = cylinder.copyLayer()
        canvas.selectedLayer = cylinder
        return cylinder
    }

    /** Menerapkan palet warna default sesuai [PodiumStyle]. */
    private fun applyStylePresets(cylinder: Cylinder3DLayer, style: PodiumStyle) {
        cylinder.style = style
        when (style) {
            PodiumStyle.MINIMAL_STUDIO -> {
                cylinder.baseColor = 0xFFF3F4F6.toInt()
                cylinder.topColor = null
                cylinder.topRingColor = 0xFFC0C0C0.toInt()
                cylinder.neonEnabled = false
                cylinder.neonColor = 0xFF00E5FF.toInt()
            }
            PodiumStyle.LUXURY_GOLD -> {
                cylinder.baseColor = 0xFFD9A441.toInt()
                cylinder.topColor = null
                cylinder.topRingColor = 0xFFFFD700.toInt()
                cylinder.neonEnabled = false
                cylinder.neonColor = 0xFFFFC93C.toInt()
            }
            PodiumStyle.DARK_ELEGANCE -> {
                cylinder.baseColor = 0xFF26262E.toInt()
                cylinder.topColor = null
                cylinder.topRingColor = 0xFF8A8F98.toInt()
                cylinder.neonEnabled = false
                cylinder.neonColor = 0xFFB0B6C4.toInt()
            }
            PodiumStyle.PASTEL_POP -> {
                cylinder.baseColor = 0xFFF9C5D5.toInt()
                cylinder.topColor = null
                cylinder.topRingColor = 0xFFFF8FB1.toInt()
                cylinder.neonEnabled = false
                cylinder.neonColor = 0xFFFF9BB8.toInt()
            }
            PodiumStyle.CYBER_NEON -> {
                cylinder.baseColor = 0xFF0E1030.toInt()
                cylinder.topColor = null
                cylinder.topRingColor = 0xFF00E5FF.toInt()
                cylinder.neonEnabled = true
                cylinder.neonColor = 0xFF00E5FF.toInt()
                cylinder.neonRadius = 18f
                cylinder.neonIntensity = 1.1f
            }
        }
    }

    private fun showComposeCylinder3DDetail(cylinder: Cylinder3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        activeTag = OBJ_3D_CYLINDER
        updateToolStripSelection(OBJ_3D_CYLINDER)

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        val switchingCylinder = draftCylinder !== cylinder
        draftCylinder = cylinder
        if (switchingCylinder) isNewCylinder = false
        cylinderSnapshot = cylinder.copyLayer()

        host.setContent {
            var currentStyle by remember { mutableStateOf(cylinder.style) }
            var currentTierCount by remember { mutableStateOf(cylinder.tierCount) }
            var currentRadiusX by remember { mutableStateOf(cylinder.radiusX) }
            var currentHeight by remember { mutableStateOf(cylinder.cylinderHeight) }
            var currentRadiusY by remember { mutableStateOf(cylinder.radiusY) }
            var currentBaseColor by remember { mutableStateOf(cylinder.baseColor) }
            var currentTopColorEnabled by remember { mutableStateOf(cylinder.topColor != null) }
            var currentTopColor by remember { mutableStateOf(cylinder.topColor ?: cylinder.baseColor) }
            var currentRingEnabled by remember { mutableStateOf(cylinder.topRingEnabled) }
            var currentRingColor by remember { mutableStateOf(cylinder.topRingColor) }
            var currentRingWidth by remember { mutableStateOf(cylinder.topRingWidth) }
            var currentLightAngle by remember { mutableStateOf(cylinder.lightAngle) }
            var currentFloorShadow by remember { mutableStateOf(cylinder.floorShadowEnabled) }
            var currentFloorShadowOpacity by remember {
                mutableStateOf(cylinder.floorShadowOpacity / 1f * 100f)
            }
            var currentOpacity by remember { mutableStateOf(cylinder.opacity / 255f * 100f) }

            Cylinder3DDetailPage(
                style = currentStyle,
                tierCount = currentTierCount,
                radiusX = currentRadiusX,
                cylinderHeight = currentHeight,
                radiusY = currentRadiusY,
                baseColor = currentBaseColor,
                topColorEnabled = currentTopColorEnabled,
                topColor = currentTopColor,
                topRingEnabled = currentRingEnabled,
                topRingColor = currentRingColor,
                topRingWidth = currentRingWidth,
                lightAngle = currentLightAngle,
                floorShadowEnabled = currentFloorShadow,
                floorShadowOpacityPct = currentFloorShadowOpacity,
                opacityPct = currentOpacity,
                onStyleChange = { s ->
                    currentStyle = s
                    applyStylePresets(cylinder, s)
                    currentBaseColor = cylinder.baseColor
                    currentTopColorEnabled = cylinder.topColor != null
                    currentTopColor = cylinder.topColor ?: cylinder.baseColor
                    currentRingColor = cylinder.topRingColor
                    canvas.invalidate()
                },
                onTierCountChange = { t ->
                    currentTierCount = t
                    cylinder.tierCount = t.coerceIn(1, 2)
                    canvas.invalidate()
                },
                onRadiusXChange = { v ->
                    currentRadiusX = v
                    cylinder.radiusX = v.coerceAtLeast(1f)
                    canvas.invalidate()
                },
                onCylinderHeightChange = { v ->
                    currentHeight = v
                    cylinder.cylinderHeight = v.coerceAtLeast(1f)
                    canvas.invalidate()
                },
                onRadiusYChange = { v ->
                    currentRadiusY = v
                    cylinder.radiusY = v.coerceAtLeast(1f)
                    canvas.invalidate()
                },
                onBaseColorChange = { c ->
                    currentBaseColor = c
                    cylinder.baseColor = c
                    canvas.invalidate()
                },
                onOpenBaseColorPicker = {
                    val original = cylinder.baseColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = cylinder.baseColor,
                        resultKey = CYLINDER_BASE_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        cylinder.baseColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        cylinder.baseColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "CylinderBaseColorPicker")
                },
                onTopColorEnabledChange = { en ->
                    currentTopColorEnabled = en
                    cylinder.topColor = if (en) (cylinder.topColor ?: cylinder.baseColor) else null
                    canvas.invalidate()
                },
                onTopColorChange = { c ->
                    currentTopColor = c
                    cylinder.topColor = c
                    canvas.invalidate()
                },
                onOpenTopColorPicker = {
                    val startColor = cylinder.topColor ?: cylinder.baseColor
                    val origTop = cylinder.topColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = startColor,
                        resultKey = CYLINDER_TOP_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        cylinder.topColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        cylinder.topColor = origTop
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "CylinderTopColorPicker")
                },
                onTopRingEnabledChange = { en ->
                    currentRingEnabled = en
                    cylinder.topRingEnabled = en
                    canvas.invalidate()
                },
                onTopRingColorChange = { c ->
                    currentRingColor = c
                    cylinder.topRingColor = c
                    canvas.invalidate()
                },
                onOpenRingColorPicker = {
                    val original = cylinder.topRingColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = cylinder.topRingColor,
                        resultKey = CYLINDER_RING_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        cylinder.topRingColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        cylinder.topRingColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "CylinderRingColorPicker")
                },
                onTopRingWidthChange = { v ->
                    currentRingWidth = v
                    cylinder.topRingWidth = v
                    canvas.invalidate()
                },
                onLightAngleChange = { a ->
                    currentLightAngle = a
                    cylinder.lightAngle = a
                    canvas.invalidate()
                },
                onFloorShadowEnabledChange = { en ->
                    currentFloorShadow = en
                    cylinder.floorShadowEnabled = en
                    canvas.invalidate()
                },
                onFloorShadowOpacityChange = { pct ->
                    currentFloorShadowOpacity = pct
                    cylinder.floorShadowOpacity = (pct / 100f).coerceIn(0f, 1f)
                    canvas.invalidate()
                },
                onOpacityChange = { pct ->
                    currentOpacity = pct
                    cylinder.opacity = (pct / 100f * 255f).toInt().coerceIn(0, 255)
                    canvas.invalidate()
                },
                onOpenShadowEditor = {
                    canvas.invalidate()
                    showCylinder3DShadowSheet(cylinder)
                },
                onOpenEmbossEditor = {
                    canvas.invalidate()
                    showCylinder3DEmbossSheet(cylinder)
                },
                onOpenNeonEditor = {
                    canvas.invalidate()
                    showCylinder3DNeonSheet(cylinder)
                },
                onReset = {
                    currentStyle = PodiumStyle.MINIMAL_STUDIO
                    currentTierCount = 1
                    currentRadiusX = 140f
                    currentHeight = 90f
                    currentRadiusY = 45f
                    currentBaseColor = 0xFFF3F4F6.toInt()
                    currentTopColorEnabled = false
                    currentTopColor = 0xFFF3F4F6.toInt()
                    currentRingEnabled = true
                    currentRingColor = 0xFFD4AF37.toInt()
                    currentRingWidth = 3f
                    currentLightAngle = -45f
                    currentFloorShadow = true
                    currentFloorShadowOpacity = 50f
                    currentOpacity = 100f
                    cylinder.style = PodiumStyle.MINIMAL_STUDIO
                    cylinder.radiusX = 140f
                    cylinder.radiusY = 45f
                    cylinder.cylinderHeight = 90f
                    cylinder.tierCount = 1
                    cylinder.tierRatio = 0.75f
                    cylinder.baseColor = 0xFFF3F4F6.toInt()
                    cylinder.topColor = null
                    cylinder.topRingEnabled = true
                    cylinder.topRingColor = 0xFFD4AF37.toInt()
                    cylinder.topRingWidth = 3f
                    cylinder.lightAngle = -45f
                    cylinder.floorShadowEnabled = true
                    cylinder.floorShadowOpacity = 0.5f
                    cylinder.opacity = 255
                    cylinder.neonEnabled = false
                    cylinder.neonColor = 0xFF00E5FF.toInt()
                    cylinder.neonRadius = 18f
                    cylinder.neonIntensity = 1f
                    cylinder.shadowEnabled = false
                    cylinder.embossEnabled = false
                    cylinder.shadowColor = 0xFF000000.toInt()
                    cylinder.shadowOpacity = 0.6f
                    cylinder.shadowRadius = 10f
                    cylinder.shadowDx = 0f
                    cylinder.shadowDy = 0f
                    canvas.invalidate()
                },
                onApply = {
                    canvas.runRecordedAction(if (isNewCylinder) "Add 3D Podium" else "Modify 3D Podium") {}
                    showSnackbar("3D Podium saved")
                    draftCylinder = null
                    isNewCylinder = false
                    cylinderSnapshot = null
                    deselect(restoreStrip = true)
                },
                onCancel = {
                    if (isNewCylinder) {
                        canvas.removeLayer(cylinder)
                    } else {
                        cylinderSnapshot?.let { snap ->
                            cylinder.style = snap.style
                            cylinder.radiusX = snap.radiusX
                            cylinder.radiusY = snap.radiusY
                            cylinder.cylinderHeight = snap.cylinderHeight
                            cylinder.tierCount = snap.tierCount
                            cylinder.tierRatio = snap.tierRatio
                            cylinder.baseColor = snap.baseColor
                            cylinder.topColor = snap.topColor
                            cylinder.topRingEnabled = snap.topRingEnabled
                            cylinder.topRingColor = snap.topRingColor
                            cylinder.topRingWidth = snap.topRingWidth
                            cylinder.lightAngle = snap.lightAngle
                            cylinder.floorShadowEnabled = snap.floorShadowEnabled
                            cylinder.floorShadowOpacity = snap.floorShadowOpacity
                            cylinder.opacity = snap.opacity
                            cylinder.neonEnabled = snap.neonEnabled
                            cylinder.neonColor = snap.neonColor
                            cylinder.neonRadius = snap.neonRadius
                            cylinder.neonIntensity = snap.neonIntensity
                            cylinder.shadowEnabled = snap.shadowEnabled
                            cylinder.shadowColor = snap.shadowColor
                            cylinder.shadowRadius = snap.shadowRadius
                            cylinder.shadowOpacity = snap.shadowOpacity
                            cylinder.shadowDx = snap.shadowDx
                            cylinder.shadowDy = snap.shadowDy
                            cylinder.embossEnabled = snap.embossEnabled
                        }
                    }
                    canvas.invalidate()
                    draftCylinder = null
                    isNewCylinder = false
                    cylinderSnapshot = null
                    deselect(restoreStrip = true)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showCylinder3DShadowSheet(cylinder: Cylinder3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            ShadowDetailPage(
                title = "Podium Shadow",
                enabled = cylinder.shadowEnabled,
                color = cylinder.shadowColor,
                radius = cylinder.shadowRadius.coerceIn(0f, 40f),
                opacityPct = (cylinder.shadowOpacity * 100f).coerceIn(0f, 100f),
                dx = cylinder.shadowDx.coerceIn(-30f, 30f),
                dy = cylinder.shadowDy.coerceIn(-30f, 30f),
                onEnabledChange = { en -> cylinder.shadowEnabled = en; canvas.invalidate() },
                onColorChange = { c -> cylinder.shadowColor = c; canvas.invalidate() },
                onColorPickRequested = {
                    val original = cylinder.shadowColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = cylinder.shadowColor,
                        resultKey = "cylinder_shadow_color_key"
                    )
                    dialog.onColorChanged = { c -> cylinder.shadowColor = c; canvas.invalidate() }
                    dialog.onCancel = { cylinder.shadowColor = original; canvas.invalidate() }
                    dialog.show(fragmentManager, "CylinderShadowColorPicker")
                },
                onRadiusChange = { r -> cylinder.shadowRadius = r; canvas.invalidate() },
                onOpacityChange = { opPct -> cylinder.shadowOpacity = (opPct / 100f).coerceIn(0f, 1f); canvas.invalidate() },
                onDxChange = { x -> cylinder.shadowDx = x; canvas.invalidate() },
                onDyChange = { y -> cylinder.shadowDy = y; canvas.invalidate() },
                onReset = {
                    cylinder.shadowEnabled = true
                    cylinder.shadowColor = 0xFF000000.toInt()
                    cylinder.shadowRadius = 12f
                    cylinder.shadowOpacity = 0.6f
                    cylinder.shadowDx = 0f
                    cylinder.shadowDy = 6f
                    canvas.invalidate()
                },
                onApply = { showComposeCylinder3DSheet(cylinder) },
                onCancel = {
                    cylinderSnapshot?.let { snap ->
                        cylinder.shadowEnabled = snap.shadowEnabled
                        cylinder.shadowColor = snap.shadowColor
                        cylinder.shadowRadius = snap.shadowRadius
                        cylinder.shadowOpacity = snap.shadowOpacity
                        cylinder.shadowDx = snap.shadowDx
                        cylinder.shadowDy = snap.shadowDy
                    }
                    canvas.invalidate()
                    showComposeCylinder3DSheet(cylinder)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showCylinder3DNeonSheet(cylinder: Cylinder3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            NeonDetailPage(
                enabled = cylinder.neonEnabled,
                color = cylinder.neonColor,
                radius = cylinder.neonRadius.coerceIn(1f, 40f),
                intensity = cylinder.neonIntensity,
                coreEnabled = cylinder.neonCoreEnabled,
                onEnabledChange = { en -> cylinder.neonEnabled = en; canvas.invalidate() },
                onColorChange = { c -> cylinder.neonColor = c; canvas.invalidate() },
                onColorPickRequested = {
                    val original = cylinder.neonColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = cylinder.neonColor,
                        resultKey = "cylinder_neon_color_key"
                    )
                    dialog.onColorChanged = { c -> cylinder.neonColor = c; canvas.invalidate() }
                    dialog.onCancel = { cylinder.neonColor = original; canvas.invalidate() }
                    dialog.show(fragmentManager, "CylinderNeonColorPicker")
                },
                onRadiusChange = { r -> cylinder.neonRadius = r; canvas.invalidate() },
                onIntensityChange = { i -> cylinder.neonIntensity = i; canvas.invalidate() },
                onCoreEnabledChange = { en -> cylinder.neonCoreEnabled = en; canvas.invalidate() },
                onReset = {
                    cylinder.neonEnabled = true
                    cylinder.neonColor = 0xFF00E5FF.toInt()
                    cylinder.neonRadius = 18f
                    cylinder.neonIntensity = 1f
                    cylinder.neonCoreEnabled = true
                    canvas.invalidate()
                },
                onApply = { showComposeCylinder3DSheet(cylinder) },
                onCancel = {
                    cylinderSnapshot?.let { snap ->
                        cylinder.neonEnabled = snap.neonEnabled
                        cylinder.neonColor = snap.neonColor
                        cylinder.neonRadius = snap.neonRadius
                        cylinder.neonIntensity = snap.neonIntensity
                        cylinder.neonCoreEnabled = snap.neonCoreEnabled
                    }
                    canvas.invalidate()
                    showComposeCylinder3DSheet(cylinder)
                },
                maxHeightPx = sheetMaxH
            )
        }
    }

    private fun showCylinder3DEmbossSheet(cylinder: Cylinder3DLayer) {
        val host = composeHost ?: return
        val container = composeContainer ?: return

        binding.objectContentPanel.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.bringToFront()

        val sheetMaxH = computeShapeSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
        container.post { canvas.invalidate() }

        host.setContent {
            EmbossDetailPage(
                enabled = cylinder.embossEnabled,
                lightAngle = cylinder.embossLightAngle,
                intensity = cylinder.embossIntensity,
                ambient = cylinder.embossAmbient,
                specular = cylinder.embossSpecular,
                bevel = cylinder.embossBevel,
                onEnabledChange = { en -> cylinder.embossEnabled = en; canvas.invalidate() },
                onLightAngleChange = { v -> cylinder.embossLightAngle = v; canvas.invalidate() },
                onIntensityChange = { v -> cylinder.embossIntensity = v; canvas.invalidate() },
                onAmbientChange = { v -> cylinder.embossAmbient = v; canvas.invalidate() },
                onSpecularChange = { v -> cylinder.embossSpecular = v; canvas.invalidate() },
                onBevelChange = { v -> cylinder.embossBevel = v; canvas.invalidate() },
                onReset = {
                    cylinder.embossEnabled = true
                    cylinder.embossLightAngle = 45f
                    cylinder.embossAmbient = 0.2f
                    cylinder.embossSpecular = 8f
                    cylinder.embossIntensity = 1f
                    cylinder.embossBevel = 3f
                    canvas.invalidate()
                },
                onApply = { showComposeCylinder3DSheet(cylinder) },
                onCancel = {
                    cylinderSnapshot?.let { snap ->
                        cylinder.embossEnabled = snap.embossEnabled
                        cylinder.embossLightAngle = snap.embossLightAngle
                        cylinder.embossAmbient = snap.embossAmbient
                        cylinder.embossSpecular = snap.embossSpecular
                        cylinder.embossIntensity = snap.embossIntensity
                        cylinder.embossBevel = snap.embossBevel
                    }
                    canvas.invalidate()
                    showComposeCylinder3DSheet(cylinder)
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
                    val original = liveDrawBrushColor.value
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
                    dialog.onCancel = {
                        liveDrawBrushColor.value = original
                        canvas.freeDrawColor = original
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
                    val originalHead = arrow.headColor
                    val originalTail = arrow.tailColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = arrow.headColor,
                        resultKey = ARROW_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        arrow.headColor = color
                        arrow.tailColor = color
                        canvas.invalidate()
                    }
                    dialog.onGradientChanged = { gradient ->
                        val color = gradient.colors.firstOrNull() ?: originalHead
                        arrow.headColor = color
                        arrow.tailColor = color
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        arrow.headColor = originalHead
                        arrow.tailColor = originalTail
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "ArrowColorPicker")
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
                    val original = pen.strokeColor
                    val dialog = ColorPickerDialog.newInstance(
                        initialColor = pen.strokeColor,
                        resultKey = BEZIER_COLOR_RESULT_KEY
                    )
                    dialog.onColorChanged = { color ->
                        pen.strokeColor = color
                        canvas.invalidate()
                    }
                    dialog.onGradientChanged = { gradient ->
                        pen.strokeColor = gradient.colors.firstOrNull() ?: original
                        canvas.invalidate()
                    }
                    dialog.onCancel = {
                        pen.strokeColor = original
                        canvas.invalidate()
                    }
                    dialog.show(fragmentManager, "BezierColorPicker")
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
        val sheetMaxH = computeDrawSheetHeight()
        PanelHeightManager.setHeight(container, sheetMaxH)
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
                },
                maxHeightPx = sheetMaxH
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
        // Box3D terpilih → buka Studio 3D Box (kecuali sedang membuat/yang sama)
        if (layer is Box3DLayer && activeTag != OBJ_3D_BOX && draftBox !== layer) {
            if (activeTag.isNotEmpty()) {
                binding.bottomNavigation.selectedItemId = R.id.nav_add
            }
            showComposeBox3DSheet(layer)
        }
        // Sphere3D terpilih → buka Studio 3D Sphere
        if (layer is Sphere3DLayer && activeTag != OBJ_3D_SPHERE && draftSphere !== layer) {
            if (activeTag.isNotEmpty()) {
                binding.bottomNavigation.selectedItemId = R.id.nav_add
            }
            showComposeSphere3DSheet(layer)
        }
        // Podium 3D terpilih → buka Studio Podium 3D Cylinder
        if (layer is Cylinder3DLayer && activeTag != OBJ_3D_CYLINDER && draftCylinder !== layer) {
            if (activeTag.isNotEmpty()) {
                binding.bottomNavigation.selectedItemId = R.id.nav_add
            }
            showComposeCylinder3DSheet(layer)
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
