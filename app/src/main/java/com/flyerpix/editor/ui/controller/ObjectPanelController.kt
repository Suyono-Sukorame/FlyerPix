package com.flyerpix.editor.ui.controller

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.PenLayer
import com.flyerpix.editor.canvas.model.PerspectivePreset
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Controller efek properti untuk layer Object (Shape, Image, Sticker, Pen, Arrow).
 *
 * Kontrol efek dimuat ke halaman Effect Settings yang SAMA dengan Text
 * (layout_effect_settings_panel.xml), sehingga efek objek dan teks berbagi satu
 * halaman — tidak ada menu ganda. Tool efek ditambahkan ke strip Object.
 */
class ObjectPanelController(
    private val activity: Activity,
    private val binding: ActivityEditorBinding,
    private val pixelCanvasView: PixelCanvasView,
    private val showSnackbar: (String) -> Unit,
    private val onEffectSettingsOpenChanged: (Boolean) -> Unit
) {

    companion object {
        // Shared visual effect tools used by both text and object layers.
        // Keep this list intentionally small and generic so we do not duplicate
        // text-specific tools like font, letter spacing, curve, style, etc.
        const val OBJ_POSITION    = "obj_position"
        const val OBJ_SCALE       = "obj_scale"
        const val OBJ_OPACITY     = "obj_opacity"
        const val OBJ_ROTATE      = "obj_rotate"
        const val OBJ_COLOR       = "obj_color"
        const val OBJ_STROKE      = "obj_stroke"
        const val OBJ_SHADOW      = "obj_shadow"
        const val OBJ_GRADIENT    = "obj_gradient"
        const val OBJ_BLEND       = "obj_blend"
        const val OBJ_PERSPECTIVE = "obj_perspective"

        const val COLOR_ACTIVE = 0xFF1769FF.toInt()
        const val COLOR_GRAY   = 0xFF616161.toInt()

        val sharedCoreEffectTags = setOf(
            OBJ_POSITION, OBJ_SCALE, OBJ_OPACITY, OBJ_ROTATE, OBJ_COLOR,
            OBJ_STROKE, OBJ_SHADOW, OBJ_GRADIENT, OBJ_BLEND, OBJ_PERSPECTIVE
        )
    }

    private val toolItems = LinkedHashMap<String, android.view.ViewGroup>()
    private val toolLabels = LinkedHashMap<String, String>()
    private val panelViews = LinkedHashMap<String, View>()

    private var settingsSnapshot: CanvasLayer? = null
    private var effectSettingsOpen = false
    private var activeToolTag = ""
    private var toolBeforeEffect = ""

    fun isEffectSettingsOpen(): Boolean = effectSettingsOpen

    fun initialize() {
        buildEffectTools()
        initPositionControls()
        initScaleControls()
        initOpacityControls()
        initRotateControls()
        initColorControls()
        initStrokeControls()
        initShadowControls()
        initGradientControls()
        initBlendControls()
        initPerspectiveControls()
    }

    /** Tambahkan tool efek ke akhir strip Object (setelah tool penambahan). */
    private fun buildEffectTools() {
        data class Spec(val tag: String, val label: String, val iconRes: Int)
        val specs = listOf(
            Spec(OBJ_POSITION,    "Position",    R.drawable.ic_position_24px),
            Spec(OBJ_SCALE,       "Scale",       R.drawable.ic_size_24px),
            Spec(OBJ_OPACITY,     "Opacity",     R.drawable.ic_opacity_24px),
            Spec(OBJ_ROTATE,      "Rotate",      R.drawable.ic_rotate_right_24px),
            Spec(OBJ_COLOR,       "Color",       R.drawable.ic_sharp_palette_24px),
            Spec(OBJ_STROKE,      "Stroke",      R.drawable.ic_stroke_24px),
            Spec(OBJ_SHADOW,      "Shadow",      R.drawable.ic_shadow_24px),
            Spec(OBJ_GRADIENT,    "Gradient",    R.drawable.ic_gradient_24px),
            Spec(OBJ_BLEND,       "Blend",       R.drawable.ic_layers_24px),
            Spec(OBJ_PERSPECTIVE, "Perspective", R.drawable.ic_perspective_24px)
        )
        val density = activity.resources.displayMetrics.density
        val container = binding.objectToolStripInclude.objectToolStripContainer
        container.post {
            for (spec in specs) {
                require(spec.tag in sharedCoreEffectTags) {
                    "Object effect tool '${spec.tag}' is not part of the shared-core-only toolset."
                }
                toolLabels[spec.tag] = spec.label
                val item = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    setBackgroundResource(R.drawable.bg_object_tool_item)
                    setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
                    setOnClickListener { onToolClicked(spec.tag) }
                }
                val iconSize = (22 * density).toInt()
                item.addView(ImageView(activity).apply {
                    setImageResource(spec.iconRes)
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                    colorFilter = android.graphics.PorterDuffColorFilter(COLOR_GRAY, android.graphics.PorterDuff.Mode.SRC_IN)
                })
                item.addView(TextView(activity).apply {
                    text = spec.label; textSize = 9.5f; maxLines = 1
                    gravity = android.view.Gravity.CENTER; setTextColor(COLOR_GRAY)
                })
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.width = (52 * density).toInt()
                container.addView(item, lp)
                toolItems[spec.tag] = item
            }
        }
    }

    private fun onToolClicked(tag: String) {
        val layer = pixelCanvasView.selectedLayer
        if (layer == null || layer.isLocked || layer is TextLayer) {
            showSnackbar("Pilih objek (shape/gambar/sticker/dll) terlebih dahulu")
            return
        }
        openEffectSettings(tag)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Effect Settings open/close (berbagi halaman dengan Text)
    // ─────────────────────────────────────────────────────────────────────────

    private fun openEffectSettings(tag: String) {
        val layer = pixelCanvasView.selectedLayer ?: return
        if (effectSettingsOpen && activeToolTag == tag) return
        toolBeforeEffect = activeToolTag
        snapshotCurrentState()
        activeToolTag = tag
        effectSettingsOpen = true
        binding.objectMenuPanel.visibility = View.GONE
        for (v in panelViews.values) v.visibility = View.GONE
        showEffectSettingsVisibility()
        syncEffectUI(tag, layer)
    }

    private fun showEffectSettingsVisibility() {
        val show = effectSettingsOpen && activeToolTag in sharedCoreEffectTags
        val wasOpen = binding.effectSettingsInclude.root.visibility == View.VISIBLE
        val changed = wasOpen != show
        binding.effectSettingsInclude.root.visibility = if (show) View.VISIBLE else View.GONE
        effectSettingsOpen = show
        if (changed) onEffectSettingsOpenChanged(show)
    }

    fun applyEffectSettings() { closeEffectSettings() }

    fun cancelEffectSettings() {
        val layer = pixelCanvasView.selectedLayer
        if (layer != null && settingsSnapshot != null) restoreSnapshot(layer)
        closeEffectSettings()
    }

    private fun closeEffectSettings() {
        settingsSnapshot = null
        effectSettingsOpen = false
        activeToolTag = toolBeforeEffect
        toolBeforeEffect = ""
        pixelCanvasView.invalidate()
        showEffectSettingsVisibility()
    }

    private fun snapshotCurrentState() {
        settingsSnapshot = pixelCanvasView.selectedLayer?.copyLayer()
    }

    private fun restoreSnapshot(snapshot: CanvasLayer) {
        val layer = pixelCanvasView.selectedLayer ?: return
        layer.x = snapshot.x
        layer.y = snapshot.y
        layer.scale = snapshot.scale
        layer.rotation = snapshot.rotation
        layer.opacity = snapshot.opacity
        layer.perspectiveEnabled = snapshot.perspectiveEnabled
        layer.perspectiveCorners = snapshot.perspectiveCorners.clone()
        layer.blendMode = snapshot.blendMode
        layer.blendExtra = snapshot.blendExtra
        layer.shadowEnabled = snapshot.shadowEnabled
        layer.shadowColor = snapshot.shadowColor
        layer.shadowRadius = snapshot.shadowRadius
        layer.shadowDx = snapshot.shadowDx
        layer.shadowDy = snapshot.shadowDy
        layer.shadowOpacity = snapshot.shadowOpacity
        layer.gradientEnabled = snapshot.gradientEnabled
        layer.gradient = snapshot.gradient?.copy()
        when (layer) {
            is ShapeLayer -> {
                val s = snapshot as? ShapeLayer
                if (s != null) { layer.strokeColor = s.strokeColor; layer.strokeWidth = s.strokeWidth; layer.fillColor = s.fillColor }
            }
            is PenLayer -> {
                val s = snapshot as? PenLayer
                if (s != null) { layer.strokeColor = s.strokeColor; layer.strokeWidth = s.strokeWidth; layer.fillColor = s.fillColor }
            }
        }
    }

    private fun syncEffectUI(tag: String, layer: CanvasLayer) {
        val fs = binding.effectSettingsInclude
        when (tag) {
            OBJ_POSITION    -> syncPositionUI(layer)
            OBJ_SCALE       -> syncScaleUI(layer)
            OBJ_OPACITY     -> syncOpacityUI(layer)
            OBJ_ROTATE      -> syncRotateUI(layer)
            OBJ_COLOR       -> syncColorUI(layer)
            OBJ_STROKE      -> syncStrokeUI(layer)
            OBJ_SHADOW      -> syncShadowUI(layer)
            OBJ_GRADIENT    -> syncGradientUI(layer)
            OBJ_BLEND       -> syncBlendUI(layer)
            OBJ_PERSPECTIVE -> syncPerspectiveUI(layer)
        }
        fs.effectSettingsTitle.text = toolLabels[tag] ?: "Effect Settings"
        for ((t, v) in panelViews) v.visibility = if (t == tag) View.VISIBLE else View.GONE
    }

    private fun applyToLayer(block: (CanvasLayer) -> Unit) {
        val layer = pixelCanvasView.selectedLayer
        if (layer == null || layer is TextLayer || layer.isLocked) return
        pixelCanvasView.runRecordedAction("Ubah Properti Objek") { block(layer) }
        pixelCanvasView.invalidate()
    }

    private fun setToolSelection(tag: String?) {
        for ((t, item) in toolItems) {
            val sel = t == tag
            item.isSelected = sel
            val c = if (sel) COLOR_ACTIVE else COLOR_GRAY
            (item.getChildAt(0) as? ImageView)?.colorFilter =
                android.graphics.PorterDuffColorFilter(c, android.graphics.PorterDuff.Mode.SRC_IN)
            (item.getChildAt(1) as? TextView)?.setTextColor(c)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Position
    // ─────────────────────────────────────────────────────────────────────────

    private fun initPositionControls() {
        val b = binding.effectSettingsInclude.positionControlsInclude
        panelViews[OBJ_POSITION] = b.root
        val range = max(pixelCanvasView.width, pixelCanvasView.height).toFloat().coerceAtLeast(1000f)

        fun centerX(l: CanvasLayer): Float = (pixelCanvasView.width - l.getUnwarpedDimensions().first) / 2f
        fun centerY(l: CanvasLayer): Float = (pixelCanvasView.height - l.getUnwarpedDimensions().second) / 2f

        fun syncPosLabels() {
            val l = pixelCanvasView.selectedLayer ?: return
            b.tvPosXLabel.text = "X: ${l.x.toInt()} px"
            b.tvPosYLabel.text = "Y: ${l.y.toInt()} px"
            b.editPosX.setText(String.format(Locale.US, "%.0f", l.x))
            b.editPosY.setText(String.format(Locale.US, "%.0f", l.y))
            b.sliderPosX.value = l.x.coerceIn(-range, range)
            b.sliderPosY.value = l.y.coerceIn(-range, range)
        }

        b.sliderPosX.valueFrom = -range
        b.sliderPosX.valueTo = range
        b.sliderPosY.valueFrom = -range
        b.sliderPosY.valueTo = range

        b.sliderPosX.addOnChangeListener { _, v, _ -> applyToLayer { it.x = v }; syncPosLabels() }
        b.sliderPosY.addOnChangeListener { _, v, _ -> applyToLayer { it.y = v }; syncPosLabels() }
        b.btnPosXMinus.setOnClickListener { applyToLayer { it.x -= 1f }; syncPosLabels() }
        b.btnPosXPlus.setOnClickListener { applyToLayer { it.x += 1f }; syncPosLabels() }
        b.btnPosYMinus.setOnClickListener { applyToLayer { it.y -= 1f }; syncPosLabels() }
        b.btnPosYPlus.setOnClickListener { applyToLayer { it.y += 1f }; syncPosLabels() }
        b.btnPosCenterH.setOnClickListener { applyToLayer { it.x = centerX(it) }; syncPosLabels() }
        b.btnPosCenterV.setOnClickListener { applyToLayer { it.y = centerY(it) }; syncPosLabels() }
        b.btnPosCenter.setOnClickListener { applyToLayer { it.x = centerX(it); it.y = centerY(it) }; syncPosLabels() }
        b.btnResetPosition.setOnClickListener { applyToLayer { it.x = 0f; it.y = 0f }; syncPosLabels() }
    }

    private fun syncPositionUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.positionControlsInclude
        b.tvPosXLabel.text = "X: ${layer.x.toInt()} px"
        b.tvPosYLabel.text = "Y: ${layer.y.toInt()} px"
        b.editPosX.setText(String.format(Locale.US, "%.0f", layer.x))
        b.editPosY.setText(String.format(Locale.US, "%.0f", layer.y))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scale
    // ─────────────────────────────────────────────────────────────────────────

    private fun initScaleControls() {
        val b = binding.effectSettingsInclude.sizeControlsInclude
        panelViews[OBJ_SCALE] = b.root

        fun syncScaleLabels() {
            val l = pixelCanvasView.selectedLayer ?: return
            b.tvScaleLabel.text = "Scale: ${(l.scale * 100).toInt()}%"
            b.sliderScaleXY.value = l.scale.coerceIn(0.1f, 8f)
        }

        b.sliderScaleXY.valueFrom = 0.1f
        b.sliderScaleXY.valueTo = 8f
        b.sliderScaleXY.addOnChangeListener { _, v, _ -> applyToLayer { it.scale = v }; syncScaleLabels() }
        b.btnScaleReset.setOnClickListener { applyToLayer { it.scale = 1f }; syncScaleLabels() }
        b.btnSizeReset.setOnClickListener { applyToLayer { it.scale = 1f }; syncScaleLabels() }
        b.btnScaleFit.setOnClickListener {
            applyToLayer { l ->
                val (w, h) = l.getUnwarpedDimensions()
                if (w > 0f && h > 0f) {
                    l.scale = min(pixelCanvasView.width / w, pixelCanvasView.height / h * 0.9f).coerceAtLeast(0.05f)
                }
            }
            syncScaleLabels()
        }
    }

    private fun syncScaleUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.sizeControlsInclude
        b.tvScaleLabel.text = "Scale: ${(layer.scale * 100).toInt()}%"
        b.sliderScaleXY.value = layer.scale.coerceIn(0.1f, 8f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opacity
    // ─────────────────────────────────────────────────────────────────────────

    private fun initOpacityControls() {
        val b = binding.effectSettingsInclude.opacityControlsInclude
        panelViews[OBJ_OPACITY] = b.root

        fun syncOpacityLabels() {
            val l = pixelCanvasView.selectedLayer ?: return
            b.tvOpacityLabel.text = "${(l.opacity * 100 / 255)}%"
            b.sliderOpacity.value = (l.opacity * 100f / 255f).coerceIn(0f, 100f)
        }

        b.sliderOpacity.addOnChangeListener { _, v, _ -> applyToLayer { it.opacity = (v * 255 / 100).toInt().coerceIn(0, 255) }; syncOpacityLabels() }
        b.btnResetOpacity.setOnClickListener { applyToLayer { it.opacity = 255 }; syncOpacityLabels() }
    }

    private fun syncOpacityUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.opacityControlsInclude
        b.sliderOpacity.value = (layer.opacity * 100f / 255f).coerceIn(0f, 100f)
        b.tvOpacityLabel.text = "${(layer.opacity * 100 / 255)}%"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rotate
    // ─────────────────────────────────────────────────────────────────────────

    private fun initRotateControls() {
        val b = binding.effectSettingsInclude.rotateControlsInclude
        panelViews[OBJ_ROTATE] = b.root

        fun syncRotateLabels() {
            val l = pixelCanvasView.selectedLayer ?: return
            b.tvRotateLabel.text = "${l.rotation.toInt()}°"
            b.sliderRotate.value = l.rotation.coerceIn(-360f, 360f)
        }

        b.sliderRotate.addOnChangeListener { _, v, _ -> applyToLayer { it.rotation = v }; syncRotateLabels() }
        b.btnRotateMinus90.setOnClickListener { applyToLayer { it.rotation -= 90f }; syncRotateLabels() }
        b.btnRotatePlus90.setOnClickListener { applyToLayer { it.rotation += 90f }; syncRotateLabels() }
        b.btnRotate180.setOnClickListener { applyToLayer { it.rotation += 180f }; syncRotateLabels() }
        b.btnRotateReset.setOnClickListener { applyToLayer { it.rotation = 0f }; syncRotateLabels() }
        b.btnResetRotate.setOnClickListener { applyToLayer { it.rotation = 0f }; syncRotateLabels() }
    }

    private fun syncRotateUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.rotateControlsInclude
        b.tvRotateLabel.text = "${layer.rotation.toInt()}°"
        b.sliderRotate.value = layer.rotation.coerceIn(-360f, 360f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Color (Fill untuk Shape & Pen)
    // ─────────────────────────────────────────────────────────────────────────

    private fun initColorControls() {
        val b = binding.effectSettingsInclude.colorControlsInclude
        panelViews[OBJ_COLOR] = b.root
        b.btnColorGradient.setOnClickListener {
            val layer = pixelCanvasView.selectedLayer ?: return@setOnClickListener
            activeToolTag = OBJ_GRADIENT
            toolBeforeEffect = ""
            for (v in panelViews.values) v.visibility = View.GONE
            syncEffectUI(OBJ_GRADIENT, layer)
        }
        b.btnPickColor.setOnClickListener { launchFillColorPicker() }
        b.chipColorPreview.setOnClickListener { launchFillColorPicker() }
    }

    private fun launchFillColorPicker() {
        val layer = pixelCanvasView.selectedLayer ?: return
        val initial = fillColorOf(layer)
        com.flyerpix.editor.ui.dialog.ColorPickerDialog
            .newInstance(initial, null)
            .show((activity as androidx.fragment.app.FragmentActivity).supportFragmentManager, com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG)
    }

    private fun syncColorUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.colorControlsInclude
        val color = fillColorOf(layer)
        b.chipColorPreview.setCardBackgroundColor(color)
        b.tvColorValue.text = String.format(Locale.US, "#%08X", color)
        b.btnColorGradient.text = if (layer.gradientEnabled) "Gradasi (Aktif)" else "Gradasi"
    }

    private fun fillColorOf(l: CanvasLayer): Int = when (l) {
        is ShapeLayer -> l.fillColor
        is PenLayer -> l.fillColor
        else -> Color.WHITE
    }

    private fun setFillColor(l: CanvasLayer, c: Int) = when (l) {
        is ShapeLayer -> l.fillColor = c
        is PenLayer -> l.fillColor = c
        else -> {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stroke (Shape & Pen)
    // ─────────────────────────────────────────────────────────────────────────

    private fun initStrokeControls() {
        val b = binding.effectSettingsInclude.strokeControlsInclude
        panelViews[OBJ_STROKE] = b.root

        fun syncStrokeLabels() {
            val l = pixelCanvasView.selectedLayer ?: return
            b.tvStrokeWidthLabel.text = "Width: ${strokeWidthOf(l).toInt()} px"
            b.tvStrokeOpacityLabel.text = "Opacity: ${strokeOpacityPercent(l)}%"
            b.sliderStrokeWidth.value = strokeWidthOf(l).coerceIn(0f, 100f)
            b.sliderStrokeOpacity.value = strokeOpacityPercent(l).toFloat().coerceIn(0f, 100f)
            b.chipStrokeColorPreview.setCardBackgroundColor(strokeColorOf(l))
            b.tvStrokeColorValue.text = String.format(Locale.US, "#%08X", strokeColorOf(l))
            b.switchStrokeEnabled.isChecked = strokeWidthOf(l) > 0f
            b.strokeSliderGroup.visibility = if (strokeWidthOf(l) > 0f) View.VISIBLE else View.GONE
        }

        b.sliderStrokeWidth.addOnChangeListener { _, v, _ -> applyToLayer { setStrokeWidth(it, v) }; syncStrokeLabels() }
        b.sliderStrokeOpacity.addOnChangeListener { _, v, _ ->
            applyToLayer { l ->
                setStrokeColor(l, argbFromOpacity(strokeColorOf(l), v))
            }
            syncStrokeLabels()
        }
        b.btnResetStroke.setOnClickListener { applyToLayer { setStrokeWidth(it, 0f) }; syncStrokeLabels() }
        b.btnPickStrokeColor.setOnClickListener { launchStrokeColorPicker() }
        b.chipStrokeColorPreview.setOnClickListener { launchStrokeColorPicker() }
    }

    private fun strokeColorOf(l: CanvasLayer): Int = when (l) {
        is ShapeLayer -> l.strokeColor
        is PenLayer -> l.strokeColor
        else -> Color.BLACK
    }

    private fun strokeWidthOf(l: CanvasLayer): Float = when (l) {
        is ShapeLayer -> l.strokeWidth
        is PenLayer -> l.strokeWidth
        else -> 0f
    }

    private fun setStrokeWidth(l: CanvasLayer, w: Float) = when (l) {
        is ShapeLayer -> l.strokeWidth = w
        is PenLayer -> l.strokeWidth = w
        else -> {}
    }

    private fun setStrokeColor(l: CanvasLayer, c: Int) = when (l) {
        is ShapeLayer -> l.strokeColor = c
        is PenLayer -> l.strokeColor = c
        else -> {}
    }

    private fun strokeOpacityPercent(l: CanvasLayer): Int =
        (Color.alpha(strokeColorOf(l)) * 100 / 255)

    private fun argbFromOpacity(color: Int, opacityPct: Float): Int {
        val alpha = (opacityPct * 255 / 100).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }

    private fun launchStrokeColorPicker() {
        val layer = pixelCanvasView.selectedLayer ?: return
        com.flyerpix.editor.ui.dialog.ColorPickerDialog
            .newInstance(strokeColorOf(layer), null)
            .show((activity as androidx.fragment.app.FragmentActivity).supportFragmentManager, com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG)
    }

    private fun syncStrokeUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.strokeControlsInclude
        b.tvStrokeWidthLabel.text = "Width: ${strokeWidthOf(layer).toInt()} px"
        b.tvStrokeOpacityLabel.text = "Opacity: ${strokeOpacityPercent(layer)}%"
        b.sliderStrokeWidth.value = strokeWidthOf(layer).coerceIn(0f, 100f)
        b.sliderStrokeOpacity.value = strokeOpacityPercent(layer).toFloat().coerceIn(0f, 100f)
        b.chipStrokeColorPreview.setCardBackgroundColor(strokeColorOf(layer))
        b.tvStrokeColorValue.text = String.format(Locale.US, "#%08X", strokeColorOf(layer))
        b.switchStrokeEnabled.isChecked = strokeWidthOf(layer) > 0f
        b.strokeSliderGroup.visibility = if (strokeWidthOf(layer) > 0f) View.VISIBLE else View.GONE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shadow
    // ─────────────────────────────────────────────────────────────────────────

    private fun initShadowControls() {
        val b = binding.effectSettingsInclude.shadowControlsInclude
        panelViews[OBJ_SHADOW] = b.root
        b.switchShadowEnabled.setOnCheckedChangeListener { _, c ->
            applyToLayer { it.shadowEnabled = c }
            b.shadowSliderGroup.visibility = if (c) View.VISIBLE else View.GONE
        }
        b.sliderShadowRadius.addOnChangeListener { _, v, _ -> applyToLayer { it.shadowRadius = v }; b.tvShadowRadiusLabel.text = "Blur: ${v.toInt()} px" }
        b.sliderShadowOpacity.addOnChangeListener { _, v, _ -> applyToLayer { it.shadowOpacity = v }; b.tvShadowOpacityLabel.text = "Opacity: ${(v * 100).toInt()}%" }
        b.sliderShadowDx.addOnChangeListener { _, v, _ -> applyToLayer { it.shadowDx = v }; b.tvShadowDxLabel.text = "Offset X: ${v.toInt()} px" }
        b.sliderShadowDy.addOnChangeListener { _, v, _ -> applyToLayer { it.shadowDy = v }; b.tvShadowDyLabel.text = "Offset Y: ${v.toInt()} px" }
        b.btnResetShadow.setOnClickListener {
            applyToLayer {
                it.shadowEnabled = false
                it.shadowRadius = 8f; it.shadowOpacity = 0.6f; it.shadowDx = 4f; it.shadowDy = 4f
            }
            b.shadowSliderGroup.visibility = View.GONE
            b.switchShadowEnabled.isChecked = false
        }
        b.btnPickShadowColor.setOnClickListener { launchShadowColorPicker() }
        b.chipShadowColorPreview.setOnClickListener { launchShadowColorPicker() }
    }

    private fun launchShadowColorPicker() {
        val layer = pixelCanvasView.selectedLayer ?: return
        com.flyerpix.editor.ui.dialog.ColorPickerDialog
            .newInstance(layer.shadowColor, null)
            .show((activity as androidx.fragment.app.FragmentActivity).supportFragmentManager, com.flyerpix.editor.ui.dialog.ColorPickerDialog.TAG)
    }

    private fun syncShadowUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.shadowControlsInclude
        b.switchShadowEnabled.isChecked = layer.shadowEnabled
        b.shadowSliderGroup.visibility = if (layer.shadowEnabled) View.VISIBLE else View.GONE
        b.sliderShadowRadius.value = layer.shadowRadius.coerceIn(0f, 40f)
        b.sliderShadowOpacity.value = layer.shadowOpacity.coerceIn(0f, 1f)
        b.sliderShadowDx.value = layer.shadowDx.coerceIn(-30f, 30f)
        b.sliderShadowDy.value = layer.shadowDy.coerceIn(-30f, 30f)
        b.tvShadowRadiusLabel.text = "Blur: ${layer.shadowRadius.toInt()} px"
        b.tvShadowOpacityLabel.text = "Opacity: ${(layer.shadowOpacity * 100).toInt()}%"
        b.tvShadowDxLabel.text = "Offset X: ${layer.shadowDx.toInt()} px"
        b.tvShadowDyLabel.text = "Offset Y: ${layer.shadowDy.toInt()} px"
        b.chipShadowColorPreview.setCardBackgroundColor(layer.shadowColor)
        b.tvShadowColorValue.text = String.format(Locale.US, "#%08X", layer.shadowColor)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gradient
    // ─────────────────────────────────────────────────────────────────────────

    private fun initGradientControls() {
        val b = binding.effectSettingsInclude.gradientControlsInclude
        panelViews[OBJ_GRADIENT] = b.root
        b.btnResetGradient.setOnClickListener {
            applyToLayer { it.gradientEnabled = false; it.gradient = null }
            b.gradientControlsGroup.visibility = View.GONE
            pixelCanvasView.invalidate()
        }
    }

    private fun syncGradientUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.gradientControlsInclude
        b.switchGradientEnabled.isChecked = layer.gradientEnabled
        b.gradientControlsGroup.visibility = if (layer.gradientEnabled && layer.gradient != null) View.VISIBLE else View.GONE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Blend
    // ─────────────────────────────────────────────────────────────────────────

    private fun initBlendControls() {
        val b = binding.effectSettingsInclude.blendControlsInclude
        panelViews[OBJ_BLEND] = b.root
        val buttons = listOf(
            b.btnBlendNormal to android.graphics.PorterDuff.Mode.SRC_OVER,
            b.btnBlendMultiply to android.graphics.PorterDuff.Mode.MULTIPLY,
            b.btnBlendScreen to android.graphics.PorterDuff.Mode.SCREEN,
            b.btnBlendOverlay to android.graphics.PorterDuff.Mode.OVERLAY,
            b.btnBlendDarken to android.graphics.PorterDuff.Mode.DARKEN,
            b.btnBlendLighten to android.graphics.PorterDuff.Mode.LIGHTEN,
            b.btnBlendAdd to android.graphics.PorterDuff.Mode.ADD
        )
        for ((btn, mode) in buttons) btn.setOnClickListener {
            applyToLayer { it.blendMode = mode; it.blendExtra = null }
            syncBlendUI(pixelCanvasView.selectedLayer ?: return@setOnClickListener)
        }
        b.btnResetBlendMode.setOnClickListener {
            applyToLayer { it.blendMode = android.graphics.PorterDuff.Mode.SRC_OVER; it.blendExtra = null }
            syncBlendUI(pixelCanvasView.selectedLayer ?: return@setOnClickListener)
        }
    }

    private fun syncBlendUI(layer: CanvasLayer) {
        val b = binding.effectSettingsInclude.blendControlsInclude
        b.btnBlendNormal.isSelected = layer.blendMode == android.graphics.PorterDuff.Mode.SRC_OVER
        b.btnBlendMultiply.isSelected = layer.blendMode == android.graphics.PorterDuff.Mode.MULTIPLY
        b.btnBlendScreen.isSelected = layer.blendMode == android.graphics.PorterDuff.Mode.SCREEN
        b.btnBlendOverlay.isSelected = layer.blendMode == android.graphics.PorterDuff.Mode.OVERLAY
        b.btnBlendDarken.isSelected = layer.blendMode == android.graphics.PorterDuff.Mode.DARKEN
        b.btnBlendLighten.isSelected = layer.blendMode == android.graphics.PorterDuff.Mode.LIGHTEN
        b.btnBlendAdd.isSelected = layer.blendMode == android.graphics.PorterDuff.Mode.ADD
        b.tvBlendDescription.text = layer.getBlendModeName()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Perspective
    // ─────────────────────────────────────────────────────────────────────────

    private fun initPerspectiveControls() {
        val b = binding.effectSettingsInclude.perspectiveControlsInclude
        panelViews[OBJ_PERSPECTIVE] = b.root
        val presets = listOf(
            b.btnPresetFlat to PerspectivePreset.FLAT,
            b.btnPresetSlightLeft to PerspectivePreset.SLIGHT_LEFT,
            b.btnPresetLeftWall to PerspectivePreset.LEFT_WALL,
            b.btnPresetRightWall to PerspectivePreset.RIGHT_WALL,
            b.btnPresetGentleTilt to PerspectivePreset.GENTLE_TILT,
            b.btnPresetBillboard to PerspectivePreset.TOP_BILLBOARD,
            b.btnPresetFloor to PerspectivePreset.FLOOR_TILT
        )
        for ((btn, preset) in presets) btn.setOnClickListener {
            applyToLayer { it.applyPerspectivePreset(preset); it.perspectiveEnabled = true }
            syncPerspectiveUI(pixelCanvasView.selectedLayer ?: return@setOnClickListener)
        }
        b.btnResetPerspective.setOnClickListener {
            applyToLayer { it.perspectiveEnabled = false; it.resetPerspective() }
            syncPerspectiveUI(pixelCanvasView.selectedLayer ?: return@setOnClickListener)
        }
        b.switchPerspectiveEnabled.setOnCheckedChangeListener { _, c ->
            applyToLayer { it.perspectiveEnabled = c }
        }
        b.perspectiveControlsGroup.visibility = View.VISIBLE
    }

    private fun syncPerspectiveUI(layer: CanvasLayer) {
        binding.effectSettingsInclude.perspectiveControlsInclude.switchPerspectiveEnabled.isChecked = layer.perspectiveEnabled
    }

    // ─────────────────────────────────────────────────────────────────────────

    fun hideStripAndPanels() {
        binding.effectSettingsInclude.root.visibility = View.GONE
        activeToolTag = ""
        effectSettingsOpen = false
        toolBeforeEffect = ""
        settingsSnapshot = null
        for (v in panelViews.values) v.visibility = View.GONE
    }
}
