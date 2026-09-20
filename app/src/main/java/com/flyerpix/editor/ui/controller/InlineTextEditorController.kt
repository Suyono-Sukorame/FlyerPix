package com.flyerpix.editor.ui.controller

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.text.InputType
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.fragment.app.FragmentActivity
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.RichTextSpan
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.LayoutInlineTextToolbarBinding
import com.flyerpix.editor.ui.dialog.ColorPickerDialog
import kotlin.math.max

/**
 * Editor teks inline (on-canvas, Option 1).
 *
 * Menempatkan overlay EditText tepat di atas [TextLayer] yang sedang diedit dan
 * toolbar pemformatan yang terkunci di atas soft keyboard. Selama editing:
 *  - teks layer digambar sebagai "ghost" ([PixelCanvasView.drawEditingGhost]),
 *  - ukuran/posisi overlay mengikuti transform viewport (zoom + pan + rotasi layer),
 *  - commit ditulis melalui [PixelCanvasView.runRecordedAction] agar bisa Undo/Redo.
 */
class InlineTextEditorController(
    private val activity: FragmentActivity,
    private val pixelCanvasView: PixelCanvasView,
    private val overlayFrame: FrameLayout,
    private val editBox: FrameLayout,
    private val editText: EditText,
    private val toolbarBinding: LayoutInlineTextToolbarBinding,
    private val toolbarContainer: View,
    private val rootLayout: ViewGroup,
    private val onEditingStateChanged: (Boolean) -> Unit
) {
    private var editingLayer: TextLayer? = null
    private var savedPanX = 0f
    private var savedPanY = 0f
    private var suppressOutsideTapUntil = 0L
    private var savedSelection: IntRange? = null

    private val colorChips: List<View>
        get() = listOf(
            toolbarBinding.chipColor1, toolbarBinding.chipColor2, toolbarBinding.chipColor3,
            toolbarBinding.chipColor4, toolbarBinding.chipColor5, toolbarBinding.chipColor6,
            toolbarBinding.chipColor7, toolbarBinding.chipColor8, toolbarBinding.chipColor9
        )

    private val paletteColors = intArrayOf(
        Color.WHITE, Color.parseColor("#FF3B30"), Color.parseColor("#FF9500"),
        Color.parseColor("#FFCC00"), Color.parseColor("#34C759"), Color.parseColor("#00BCD4"),
        Color.parseColor("#1769FF"), Color.parseColor("#AF52DE"), Color.parseColor("#1A1A1A")
    )

    private val globalLayoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
        if (editingLayer != null) {
            updateOverlayPosition()
            ensureVisible()
        }
    }

    fun isEditing(): Boolean = editingLayer != null

    fun startEditing(layer: TextLayer) {
        if (editingLayer != null) finishEditing(commit = true)
        editingLayer = layer
        pixelCanvasView.selectedLayer = layer

        pixelCanvasView.editingTextLayerId = layer.id
        pixelCanvasView.invalidate()

        val (px, py) = pixelCanvasView.getCanvasPanState()
        savedPanX = px
        savedPanY = py

        setupEditText(layer)
        updateOverlayPosition()

        overlayFrame.visibility = View.VISIBLE
        toolbarContainer.visibility = View.VISIBLE
        suppressOutsideTapUntil = System.currentTimeMillis() + 300

        onEditingStateChanged(true)

        overlayFrame.post {
            if (editingLayer != null) {
                updateOverlayPosition()
                ensureVisible()
            }
        }
        editText.requestFocus()
        showKeyboard()
        editText.post {
            if (layeredSafe()) showKeyboard()
        }

        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    /** Jalankan ulang posisi/ukuran overlay mengikuti transform viewport terbaru. */
    private fun updateOverlayPosition() {
        val layer = editingLayer ?: return
        val transform = pixelCanvasView.getLayerScreenTransform(layer)
        val effScale = transform.scale
        val (logW, logH) = layer.measureRichContentDimensions(
            currentText(), currentSpansLogical()
        )

        val lp = editBox.layoutParams as FrameLayout.LayoutParams
        lp.width = max(1, logW.toInt())
        lp.height = max(1, logH.toInt())
        val screenW = logW * effScale
        val screenH = logH * effScale
        lp.leftMargin = (transform.centerX - screenW / 2f).toInt()
        lp.topMargin = (transform.centerY - screenH / 2f).toInt()
        editBox.layoutParams = lp
        editBox.rotation = transform.rotation
        editBox.scaleX = effScale
        editBox.scaleY = effScale
    }

    /** Geser viewport ke atas bila kotak teks tertutup toolbar/keyboard. */
    private fun ensureVisible() {
        val layer = editingLayer ?: return
        if (toolbarContainer.visibility != View.VISIBLE) return
        val density = activity.resources.displayMetrics.density
        val margin = 48f * density
        val toolbarTop = toolbarContainer.top.toFloat()
        val availableBottom = toolbarTop - margin
        val boxBottom = pixelCanvasView.getLayerScreenBounds(layer).bottom
        val overflow = boxBottom - availableBottom
        if (overflow > 0f) {
            pixelCanvasView.panUpForInlineEditor(overflow)
        }
    }

    private fun setupEditText(layer: TextLayer) {
        editText.setTextSize(layer.textSize)
        editText.setTextColor(layer.textColor)
        editText.letterSpacing = layer.letterSpacing
        editText.setLineSpacing(layer.lineSpacing, 1.0f)
        editText.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        var style = Typeface.NORMAL
        if (layer.isBold && layer.isItalic) style = Typeface.BOLD_ITALIC
        else if (layer.isBold) style = Typeface.BOLD
        else if (layer.isItalic) style = Typeface.ITALIC
        editText.setTypeface(Typeface.create(layer.typeface ?: Typeface.DEFAULT, style))

        editText.setPadding(
            max(0, layer.paddingLeft.toInt()),
            max(0, layer.paddingTop.toInt()),
            max(0, layer.paddingRight.toInt()),
            max(0, layer.paddingBottom.toInt())
        )

        val horizontal = when (layer.alignment) {
            Layout.Alignment.ALIGN_CENTER -> Gravity.CENTER_HORIZONTAL
            Layout.Alignment.ALIGN_OPPOSITE -> Gravity.END
            else -> Gravity.START
        }
        editText.gravity = Gravity.TOP or horizontal

        val editable = SpannableStringBuilder(layer.text)
        applyInitialSpans(editable, layer.richTextSpans)
        editText.setText(editable, android.widget.TextView.BufferType.SPANNABLE)

        editText.highlightColor = Color.parseColor("#331769FF")
    }

    private fun showKeyboard() {
        val imm = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    /** Commit (true) atau Discard (false) hasil editing. */
    fun finishEditing(commit: Boolean) {
        val layer = editingLayer ?: return
        editingLayer = null

        hideKeyboard()
        editText.clearFocus()

        pixelCanvasView.editingTextLayerId = null
        pixelCanvasView.restoreInlineEditorPan(savedPanX, savedPanY)
        pixelCanvasView.invalidate()

        overlayFrame.visibility = View.GONE
        toolbarContainer.visibility = View.GONE
        toolbarBinding.inlineColorPalette.visibility = View.GONE
        toolbarBinding.btnInlineColor.isSelected = false

        rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        onEditingStateChanged(false)

        if (commit) {
            val newText = currentText()
            val spans = currentSpansLogical()
            pixelCanvasView.runRecordedAction("Edit Text") {
                layer.text = newText
                layer.richTextSpans = spans.toMutableList()
            }
            pixelCanvasView.invalidate()
        }
    }

    /** Kembalikan true bila Back dikonsumsi (editor aktif & dikomit). */
    fun handleBackPress(): Boolean {
        if (!isEditing()) return false
        finishEditing(commit = true)
        return true
    }

    // ── Helper teks / span ────────────────────────────────────────────────

    private fun currentText(): String = editText.text?.toString().orEmpty()

    private fun currentSpansLogical(): List<RichTextSpan> =
        extractSpans(editText.editableText as SpannableStringBuilder)

    private fun layeredSafe(): Boolean = editingLayer != null

    // ── Span mapping (logical px, sejalan dengan TextLayer) ────────────────

    private fun applyInitialSpans(editable: SpannableStringBuilder, spans: List<RichTextSpan>) {
        spans.forEach { span ->
            val start = span.start.coerceIn(0, editable.length)
            val end = span.end.coerceIn(start, editable.length)
            if (end <= start) return@forEach
            if (span.isBold && span.isItalic) editable.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            else if (span.isBold) editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            else if (span.isItalic) editable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.color?.let { editable.setSpan(ForegroundColorSpan(it), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            span.textSize?.let { editable.setSpan(AbsoluteSizeSpan(it.toInt().coerceAtLeast(1), false), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            if (span.isUnderline) editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (span.isStrikethrough) editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun extractSpans(editable: SpannableStringBuilder): List<RichTextSpan> {
        if (editable.isEmpty()) return emptyList()
        val boundaries = sortedSetOf(0, editable.length)
        editable.getSpans(0, editable.length, Any::class.java).forEach { span ->
            boundaries += editable.getSpanStart(span).coerceIn(0, editable.length)
            boundaries += editable.getSpanEnd(span).coerceIn(0, editable.length)
        }

        val result = mutableListOf<RichTextSpan>()
        val points = boundaries.toList()
        for (index in 0 until points.lastIndex) {
            val start = points[index]
            val end = points[index + 1]
            if (end <= start) continue
            val probe = start.coerceAtMost(end - 1)
            val style = editable.getSpans(probe, probe + 1, StyleSpan::class.java).lastOrNull()
            val color = editable.getSpans(probe, probe + 1, ForegroundColorSpan::class.java).lastOrNull()?.foregroundColor
            val size = editable.getSpans(probe, probe + 1, AbsoluteSizeSpan::class.java).lastOrNull()?.size?.toFloat()
            val span = RichTextSpan(
                start = start,
                end = end,
                color = color,
                textSize = size,
                isBold = style?.style == Typeface.BOLD || style?.style == Typeface.BOLD_ITALIC,
                isItalic = style?.style == Typeface.ITALIC || style?.style == Typeface.BOLD_ITALIC,
                isUnderline = editable.getSpans(probe, probe + 1, UnderlineSpan::class.java).isNotEmpty(),
                isStrikethrough = editable.getSpans(probe, probe + 1, StrikethroughSpan::class.java).isNotEmpty()
            )
            if (span.color != null || span.textSize != null || span.isBold || span.isItalic || span.isUnderline || span.isStrikethrough) {
                val previous = result.lastOrNull()
                if (previous != null && previous.end == span.start && previous.copy(start = 0, end = 0) == span.copy(start = 0, end = 0)) {
                    result[result.lastIndex] = previous.copy(end = span.end)
                } else {
                    result += span
                }
            }
        }
        return result
    }

    // ── Seleksi & toolbar ──────────────────────────────────────────────────

    private fun rememberSelection() {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start >= 0 && end > start) savedSelection = start until end
    }

    private fun selectionRange(): IntRange? {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start >= 0 && end > start) return start until end
        val fallback = savedSelection ?: return null
        val length = editText.text?.length ?: 0
        return if (fallback.first >= 0 && fallback.last < length) fallback else null
    }

    private fun preserveSelectionOnClick(view: View) {
        view.setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) rememberSelection()
            false
        }
        view.isFocusable = false
        view.isFocusableInTouchMode = false
    }

    private fun setFormatButtonState(button: com.google.android.material.button.MaterialButton, active: Boolean) {
        button.isSelected = active
        val activeBackground = Color.parseColor("#EAF3FF")
        val activeBorder = Color.parseColor("#1769FF")
        val defaultText = Color.parseColor("#1F2A44")
        val disabledText = Color.parseColor("#9AA3AF")
        button.setBackgroundTintList(if (active) android.content.res.ColorStateList.valueOf(activeBackground) else null)
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(if (active) activeBorder else Color.TRANSPARENT))
        button.strokeWidth = if (active) 1 else 0
        button.setTextColor(if (button.isEnabled) if (active) activeBorder else defaultText else disabledText)
    }

    private fun updateToolbarState() {
        val range = selectionRange()
        val hasSelection = range != null
        val editable = editText.editableText
        val len = editable.length
        val start = range?.first ?: 0
        val end = range?.last?.plus(1) ?: 0
        if (len <= 0) {
            setFormatButtonState(toolbarBinding.btnInlineBold, false)
            setFormatButtonState(toolbarBinding.btnInlineItalic, false)
            setFormatButtonState(toolbarBinding.btnInlineUnderline, false)
            setFormatButtonState(toolbarBinding.btnInlineStrike, false)
            return
        }
        val probe = start.coerceAtMost(end - 1)
        val styles = if (hasSelection) editable.getSpans(probe, probe + 1, StyleSpan::class.java) else emptyArray()
        setFormatButtonState(toolbarBinding.btnInlineBold, styles.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC })
        setFormatButtonState(toolbarBinding.btnInlineItalic, styles.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC })
        setFormatButtonState(toolbarBinding.btnInlineUnderline, hasSelection && editable.getSpans(probe, probe + 1, UnderlineSpan::class.java).isNotEmpty())
        setFormatButtonState(toolbarBinding.btnInlineStrike, hasSelection && editable.getSpans(probe, probe + 1, StrikethroughSpan::class.java).isNotEmpty())

        val count = len
        toolbarBinding.tvInlineCharCount.text = if (count == 1) "1 char" else "$count chars"
    }

    private fun toggleStyle(style: Int) {
        val range = selectionRange() ?: return
        val editable = editText.editableText
        val start = range.first
        val end = range.last + 1
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
        val isActive = spans.any { it.style == style }
        if (isActive) {
            for (span in spans) if (span.style == style) editable.removeSpan(span)
        } else {
            editable.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        editText.setSelection(start, end)
        rememberSelection()
        updateToolbarState()
        onContentChanged()
    }

    private fun toggleUnderline() {
        val range = selectionRange() ?: return
        val editable = editText.editableText
        val start = range.first
        val end = range.last + 1
        val spans = editable.getSpans(start, end, UnderlineSpan::class.java)
        if (spans.isNotEmpty()) for (span in spans) editable.removeSpan(span)
        else editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editText.setSelection(start, end)
        rememberSelection()
        updateToolbarState()
        onContentChanged()
    }

    private fun toggleStrikethrough() {
        val range = selectionRange() ?: return
        val editable = editText.editableText
        val start = range.first
        val end = range.last + 1
        val spans = editable.getSpans(start, end, StrikethroughSpan::class.java)
        if (spans.isNotEmpty()) for (span in spans) editable.removeSpan(span)
        else editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editText.setSelection(start, end)
        rememberSelection()
        updateToolbarState()
        onContentChanged()
    }

    private fun applyTransformation(transform: (String) -> String) {
        val editable = editText.editableText as SpannableStringBuilder
        val preservedRange = selectionRange()
        val hasSelection = preservedRange != null
        val allLen = editable.length
        val spans = editable.getSpans(0, allLen, Any::class.java).map { span ->
            Triple(span, editable.getSpanStart(span), editable.getSpanEnd(span))
        }
        val targetStart = if (hasSelection) preservedRange!!.first else 0
        val targetEnd = if (hasSelection) preservedRange!!.last + 1 else allLen
        val transformed = transform(editable.subSequence(targetStart, targetEnd).toString())
        editable.replace(targetStart, targetEnd, transformed)
        editText.setSelection(targetStart, targetStart + transformed.length)
        for ((span, s, e) in spans) editable.setSpan(span, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        updateToolbarState()
        onContentChanged()
    }

    private fun applyColor(range: IntRange, color: Int) {
        editText.editableText.setSpan(ForegroundColorSpan(color), range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editText.setSelection(range.first, range.last + 1)
        rememberSelection()
        updateToolbarState()
        onContentChanged()
    }

    /** Setelah konten berubah → ukur ulang kotak & pastikan tetap terlihat. */
    private fun onContentChanged() {
        overlayFrame.post {
            if (editingLayer != null) {
                updateOverlayPosition()
                ensureVisible()
            }
        }
    }

    // ── Wiring toolbar ─────────────────────────────────────────────────────

    init {
        pixelCanvasView.onViewportTransformChangedListener = { onViewportChanged() }

        toolbarBinding.btnInlineDone.setOnClickListener { finishEditing(commit = true) }
        toolbarBinding.btnInlineCancel.setOnClickListener { finishEditing(commit = false) }

        listOf(
            toolbarBinding.btnInlineBold,
            toolbarBinding.btnInlineItalic,
            toolbarBinding.btnInlineUnderline,
            toolbarBinding.btnInlineStrike,
            toolbarBinding.btnInlineCase
        ).forEach(::preserveSelectionOnClick)

        toolbarBinding.btnInlineBold.setOnClickListener { toggleStyle(Typeface.BOLD) }
        toolbarBinding.btnInlineItalic.setOnClickListener { toggleStyle(Typeface.ITALIC) }
        toolbarBinding.btnInlineUnderline.setOnClickListener { toggleUnderline() }
        toolbarBinding.btnInlineStrike.setOnClickListener { toggleStrikethrough() }

        toolbarBinding.btnInlineCase.setOnClickListener { cycleCase() }

        toolbarBinding.btnInlineColor.setOnClickListener {
            toolbarBinding.btnInlineColor.isSelected = !toolbarBinding.btnInlineColor.isSelected
            toolbarBinding.inlineColorPalette.visibility =
                if (toolbarBinding.btnInlineColor.isSelected) View.VISIBLE else View.GONE
        }

        colorChips.forEachIndexed { index, chip ->
            chip.setBackgroundColor(paletteColors[index])
            chip.setOnClickListener {
                val range = selectionRange() ?: 0 until (editText.text?.length ?: 0)
                applyColor(range, paletteColors[index])
                toolbarBinding.inlineColorPalette.visibility = View.GONE
                toolbarBinding.btnInlineColor.isSelected = false
            }
        }

        // Tap di luar kotak teks (overlay komit) — commit.
        overlayFrame.setOnClickListener {
            if (suppressOutsideTapUntil <= System.currentTimeMillis()) {
                finishEditing(commit = true)
            }
        }
        pixelCanvasView.onInlineEditTapOutside = {
            if (suppressOutsideTapUntil <= System.currentTimeMillis()) {
                finishEditing(commit = true)
            }
        }

        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                updateToolbarState()
                onContentChanged()
            }
        })

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && editingLayer != null) {
                // EditText jelas kehilangan fokus (mis. IME ditutup paksa) → jangan commit otomatis,
                // kembali aktifkan fokus agar sesi editing tetap berjalan.
            }
        }

        editText.setOnEditorActionListener { _, _, _ ->
            // IME Action (mis. Next) default → tidak menutup editor.
            false
        }
    }

    private fun onViewportChanged() {
        if (editingLayer != null) updateOverlayPosition()
    }

    private fun cycleCase() {
        val range = selectionRange()
        val hasSelection = range != null
        val text = if (hasSelection) {
            editText.text?.subSequence(range!!.first, range.last + 1).toString()
        } else {
            editText.text?.toString().orEmpty()
        }
        if (text.isEmpty()) return
        val transform: (String) -> String = when {
            text == text.uppercase() -> { value -> value.lowercase() }
            text == text.lowercase() -> { value ->
                value.lowercase().replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase() else character.toString()
                }
            }
            else -> { value -> value.uppercase() }
        }
        applyTransformation(transform)
    }
}