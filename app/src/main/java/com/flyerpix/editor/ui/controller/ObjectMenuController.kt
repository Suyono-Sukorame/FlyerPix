package com.flyerpix.editor.ui.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.databinding.ActivityEditorBinding

class ObjectMenuController(
    private val context: Context,
    private val binding: ActivityEditorBinding,
    private val canvas: PixelCanvasView,
    private val showSnackbar: (String) -> Unit,
    private val onGalleryRequested: () -> Unit,
    private val onCameraRequested: () -> Unit,
    private val onPanelChanged: () -> Unit = {},
    private val onShapeCreated: ((com.flyerpix.editor.canvas.model.ShapeLayer) -> Unit)? = null
) {
    companion object {
        // Object-only creation tools: fitur pembuatan/penambahan layer objek.
        // Semuanya bersifat berbeda dari Text editing tools dan tidak boleh
        // di-duplicate ke menu Text.
        const val OBJ_STICKER = "obj_sticker"
        const val OBJ_IMPORT  = "obj_import"
        const val OBJ_DRAW    = "obj_draw"
        const val OBJ_SHAPES  = "obj_shapes"
        const val OBJ_BEZIER  = "obj_bezier"
        const val OBJ_ARROW   = "obj_arrow"

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY   = 0xFF616161.toInt()
    }

    private val toolItems = LinkedHashMap<String, android.view.ViewGroup>()
    private val contentViews = LinkedHashMap<String, View>()
    var activeTag: String = ""

    /**
     * Dipanggil saat status detail (buka/tutup) berubah, agar activity bisa
     * mengekspansi panel dan menyembunyikan/memunculkan kembali nav.
     */
    var onDetailExpandedChanged: ((Boolean) -> Unit)? = null

    private fun notifyDetailExpanded() {
        onDetailExpandedChanged?.invoke(activeTag.isNotEmpty())
    }

    fun initialize() {
        contentViews[OBJ_STICKER] = binding.fragmentTabSticker
        contentViews[OBJ_IMPORT]  = binding.objectContentImage
        contentViews[OBJ_SHAPES]  = binding.objectContentShape
        contentViews[OBJ_DRAW]    = binding.objectContentDraw
        contentViews[OBJ_BEZIER]  = binding.objectContentBezier
        contentViews[OBJ_ARROW]   = binding.objectContentArrow

        buildToolStrip()

        binding.btnObjGallery.setOnClickListener { onGalleryRequested() }
        binding.btnObjCamera.setOnClickListener { onCameraRequested() }
        buildShapeRow()

        binding.btnObjStartDraw.setOnClickListener { toggleFreeDrawMode() }
        binding.btnObjAddBezier.setOnClickListener {
            canvas.addPenLayer()
            showSnackbar("Bézier curve added")
        }
        binding.btnObjAddArrow.setOnClickListener {
            canvas.addArrowLayer()
            showSnackbar("Arrow added")
        }
    }

    private fun buildToolStrip() {
        data class Spec(val tag: String, val label: String, val iconRes: Int)
        val specs = listOf(
            Spec(OBJ_STICKER, "Sticker", R.drawable.ic_sharp_face_24px),
            Spec(OBJ_IMPORT,  "Import",  R.drawable.ic_outline_photo_24px),
            Spec(OBJ_DRAW,    "Draw",    R.drawable.ic_sharp_brush_24px),
            Spec(OBJ_SHAPES,  "Shapes",  R.drawable.ic_nav_shapes_24px),
            Spec(OBJ_BEZIER,  "Bezier",  R.drawable.ic_curve_24px),
            Spec(OBJ_ARROW,   "Arrow",   R.drawable.ic_arrow_24px)
        )
        val density = context.resources.displayMetrics.density
        val container = binding.objectToolStripInclude.objectToolStripContainer
        container.removeAllViews()

        for (spec in specs) {
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                isClickable = true; isFocusable = true
                setBackgroundResource(R.drawable.bg_panel_tool_item)
                setPadding((6*density).toInt(), (6*density).toInt(), (6*density).toInt(), (4*density).toInt())
                setOnClickListener { onToolClicked(spec.tag) }
            }
            val iconSize = (22 * density).toInt()
            item.addView(android.widget.ImageView(context).apply {
                setImageResource(spec.iconRes)
                colorFilter = android.graphics.PorterDuffColorFilter(COLOR_GRAY, android.graphics.PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            })
            item.addView(android.widget.TextView(context).apply {
                text = spec.label; textSize = 9.5f; maxLines = 1
                gravity = android.view.Gravity.CENTER; setTextColor(COLOR_GRAY)
            })
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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
            (item.getChildAt(0) as? android.widget.ImageView)?.colorFilter =
                android.graphics.PorterDuffColorFilter(c, android.graphics.PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? android.widget.TextView)?.setTextColor(c)
        }
        applyContentVisibility()
        onPanelChanged()
    }

    fun deselect() {
        activeTag = ""
        for (item in toolItems.values) {
            item.isSelected = false
            (item.getChildAt(0) as? android.widget.ImageView)?.colorFilter =
                android.graphics.PorterDuffColorFilter(COLOR_GRAY, android.graphics.PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? android.widget.TextView)?.setTextColor(COLOR_GRAY)
        }
        binding.objectContentPanel.visibility = View.GONE
        applyContentVisibility()
        canvas.freeDrawEnabled = false
        onPanelChanged()
    }

    private fun applyContentVisibility() {
        binding.objectContentPanel.visibility = if (activeTag.isEmpty()) View.GONE else View.VISIBLE
        for ((t, v) in contentViews) v.visibility = if (t == activeTag) View.VISIBLE else View.GONE
        notifyDetailExpanded()
    }

    fun refreshUI() {
        binding.objectToolStripInclude.objectToolStripScroll.visibility = View.VISIBLE
        if (activeTag.isEmpty()) binding.objectContentPanel.visibility = View.GONE
        else applyContentVisibility()
        syncDrawButton()
    }

    private fun toggleFreeDrawMode() {
        val enabled = !canvas.freeDrawEnabled
        canvas.freeDrawEnabled = enabled
        canvas.onFreeDrawStart = { syncDrawButton() }
        syncDrawButton()
        showSnackbar(if (enabled) "Free Draw mode on. Drag on the canvas." else "Free Draw mode off")
    }

    fun syncDrawButton() {
        val enabled = canvas.freeDrawEnabled
        binding.btnObjStartDraw.text = if (enabled) "🛑 Finish Drawing" else "✏️ Start Free Draw"
        binding.btnObjStartDraw.setStrokeColor(
            android.content.res.ColorStateList.valueOf(if (enabled) 0xFF2E7D32.toInt() else 0xFF444444.toInt())
        )
    }

    private fun buildShapeRow() {
        val shapes = listOf(
            ShapeType.RECTANGLE, ShapeType.CIRCLE, ShapeType.TRIANGLE,
            ShapeType.STAR, ShapeType.ROUNDED_RECTANGLE
        )
        val density = context.resources.displayMetrics.density
        val size = (48 * density).toInt()
        val margin = (6 * density).toInt()
        binding.llShapeRow.removeAllViews()
        for (type in shapes) {
            val tile = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(margin, margin, margin, margin) }
                setBackgroundResource(R.drawable.bg_shape_preview)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val shape = canvas.addShapeLayer(type)
                    canvas.selectedLayer = shape // Auto select shape
                    showSnackbar("Shape ${type.name} created")
                    onShapeCreated?.invoke(shape) // Notify controller to show settings
                }
            }
            tile.addView(ImageView(context).apply {
                setImageDrawable(buildShapeIcon(type, size))
                layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            })
            binding.llShapeRow.addView(tile)
        }
    }

    /**
     * Gambar preview shape (fill + stroke) ke dalam Bitmap menggunakan builder
     * path yang sama dengan rendering kanvas, sehingga preview selalu WYSIWYG.
     */
    private fun buildShapeIcon(type: ShapeType, sizePx: Int): BitmapDrawable {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val inset = sizePx * 0.15f
        val side = sizePx - 2 * inset
        val model = ShapeLayer(shapeType = type, width = side, height = side)
        val path = model.buildPath()
        c.translate(inset, inset)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1769FF.toInt()
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D47A1.toInt()
            style = Paint.Style.STROKE
            strokeWidth = (side * 0.05f).coerceAtLeast(1.5f)
        }
        c.drawPath(path, fill)
        c.drawPath(path, stroke)
        return BitmapDrawable(context.resources, bmp)
    }
}
