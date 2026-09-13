package com.flyerpix.editor.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.Color
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.widget.EditText
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.canvas.model.RichTextSpan
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.DialogEditTextBinding

/**
 * Dialog pop-up Material Design untuk mengedit teks secara interaktif.
 * Menyediakan toolbar kustom (Bold/Italic/Underline/Strikethrough + kapitalisasi),
 * input multi-line, serta tombol konfirmasi OK dan Batal.
 *
 * Toolbar sistem dari keyboard (text action bar) dinonaktifkan agar tampilan
 * konsisten di semua device.
 */
class EditTextDialog(
    private val context: Context,
    private val initialText: String = "",
    private val initialSpans: List<RichTextSpan> = emptyList(),
    private val onTextConfirmed: (newText: String, spans: List<RichTextSpan>) -> Unit
) {

    fun show(): Dialog {
        val binding = DialogEditTextBinding.inflate(LayoutInflater.from(context))
        var savedSelectionStart = -1
        var savedSelectionEnd = -1

        fun rememberSelection() {
            val start = binding.etTextInput.selectionStart
            val end = binding.etTextInput.selectionEnd
            if (start >= 0 && end >= start) {
                savedSelectionStart = start
                savedSelectionEnd = end
            }
        }

        fun selectionRange(): IntRange? {
            val start = binding.etTextInput.selectionStart
            val end = binding.etTextInput.selectionEnd
            return if (start >= 0 && end > start) start until end else null
        }

        fun preserveSelectionOnClick(view: android.view.View) {
            view.setOnTouchListener { _, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) rememberSelection()
                false
            }
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }

        fun setFormatButtonState(button: com.google.android.material.button.MaterialButton, active: Boolean) {
            button.isSelected = active
            val activeBackground = Color.parseColor("#EAF3FF")
            val activeBorder = Color.parseColor("#4A90E2")
            val defaultText = Color.parseColor("#1F2A44")
            val disabledText = Color.parseColor("#9AA3AF")

            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (active) activeBackground else Color.TRANSPARENT))
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(if (active) activeBorder else Color.TRANSPARENT))
            button.strokeWidth = if (active) 1 else 0
            button.setTextColor(if (button.isEnabled) if (active) activeBorder else defaultText else disabledText)
        }

        fun updateToolbarState() {
            val range = selectionRange()
            val hasSelection = range != null
            val editable = binding.etTextInput.editableText
            val start = range?.first ?: 0
            val end = range?.last?.plus(1) ?: 0

            val styles = if (hasSelection) editable.getSpans(start, end, StyleSpan::class.java) else emptyArray()
            val boldActive = styles.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
            val italicActive = styles.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
            val underlineActive = if (hasSelection) editable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty() else false
            val strikeActive = if (hasSelection) editable.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty() else false

            setFormatButtonState(binding.btnBold, boldActive)
            setFormatButtonState(binding.btnItalic, italicActive)
            setFormatButtonState(binding.btnUnderline, underlineActive)
            setFormatButtonState(binding.btnStrikethrough, strikeActive)

            listOf(binding.btnBold, binding.btnItalic, binding.btnUnderline, binding.btnStrikethrough, binding.btnTextColor, binding.btnTextSize)
                .forEach { button ->
                    button.isEnabled = hasSelection
                    button.alpha = if (hasSelection) 1f else 0.5f
                    if (button === binding.btnBold || button === binding.btnItalic || button === binding.btnUnderline || button === binding.btnStrikethrough) {
                        setFormatButtonState(button, if (hasSelection) button.isSelected else false)
                    }
                }

            binding.btnMoreText.isEnabled = true
        }

        fun toggleStyle(style: Int) {
            val editable = binding.etTextInput.editableText
            val range = selectionRange() ?: return
            val start = range.first
            val end = range.last + 1

            val spans = editable.getSpans(start, end, StyleSpan::class.java)
            val isActive = spans.any { it.style == style }

            if (isActive) {
                for (span in spans) {
                    if (span.style == style) {
                        editable.removeSpan(span)
                    }
                }
            } else {
                editable.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            binding.etTextInput.setSelection(start, end)
            updateToolbarState()
        }

        fun toggleUnderline() {
            val editable = binding.etTextInput.editableText
            val range = selectionRange() ?: return
            val start = range.first
            val end = range.last + 1

            val spans = editable.getSpans(start, end, UnderlineSpan::class.java)
            if (spans.isNotEmpty()) {
                for (span in spans) {
                    editable.removeSpan(span)
                }
            } else {
                editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            binding.etTextInput.setSelection(start, end)
            updateToolbarState()
        }

        fun toggleStrikethrough() {
            val editable = binding.etTextInput.editableText
            val range = selectionRange() ?: return
            val start = range.first
            val end = range.last + 1

            val spans = editable.getSpans(start, end, StrikethroughSpan::class.java)
            if (spans.isNotEmpty()) {
                for (span in spans) {
                    editable.removeSpan(span)
                }
            } else {
                editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            binding.etTextInput.setSelection(start, end)
            updateToolbarState()
        }

        fun applyInitialSpans(editable: SpannableStringBuilder) {
            initialSpans.forEach { span ->
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

        fun extractSpans(editable: SpannableStringBuilder): List<RichTextSpan> {
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

        fun applyColor(range: IntRange, color: Int) {
            binding.etTextInput.editableText.setSpan(
                ForegroundColorSpan(color), range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            updateToolbarState()
        }

        fun applySize(size: Int) {
            val range = selectionRange() ?: return
            binding.etTextInput.editableText.setSpan(
                AbsoluteSizeSpan(size, false), range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            updateToolbarState()
        }

        fun applyTransformation(transform: (String) -> String) {
            val editable = binding.etTextInput.editableText as SpannableStringBuilder
            val start = binding.etTextInput.selectionStart
            val end = binding.etTextInput.selectionEnd
            val hasSelection = start in 0..end && end > start

            val allLen = editable.length
            val spans = editable.getSpans(0, allLen, Any::class.java).map { span ->
                Triple(span, editable.getSpanStart(span), editable.getSpanEnd(span))
            }

            val targetStart = if (hasSelection) start else 0
            val targetEnd = if (hasSelection) end else allLen
            val selected = editable.subSequence(targetStart, targetEnd).toString()
            val transformed = transform(selected)

            editable.replace(targetStart, targetEnd, transformed)
            binding.etTextInput.setSelection(targetStart, targetStart + transformed.length)

            for ((span, s, e) in spans) {
                editable.setSpan(span, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            updateToolbarState()
        }

        binding.btnBold.setOnClickListener { toggleStyle(Typeface.BOLD) }
        binding.btnItalic.setOnClickListener { toggleStyle(Typeface.ITALIC) }
        binding.btnUnderline.setOnClickListener { toggleUnderline() }
        binding.btnStrikethrough.setOnClickListener { toggleStrikethrough() }
        binding.btnTextIndicator.setOnClickListener {
            val currentText = binding.etTextInput.text?.toString().orEmpty()
            val shouldUppercase = currentText != currentText.uppercase()
            applyTransformation { value ->
                if (shouldUppercase) value.uppercase() else value.lowercase()
            }
        }

        listOf(
            binding.btnBold,
            binding.btnItalic,
            binding.btnUnderline,
            binding.btnStrikethrough,
            binding.btnTextColor,
            binding.btnTextSize
        ).forEach(::preserveSelectionOnClick)

        binding.btnTextColor.setOnClickListener {
            val activity = context as? FragmentActivity ?: return@setOnClickListener
            val range = selectionRange() ?: return@setOnClickListener
            val fragmentManager = activity.supportFragmentManager
            fragmentManager.setFragmentResultListener(
                ColorPickerDialog.RICH_TEXT_RESULT_KEY,
                activity
            ) { _, bundle ->
                if (!bundle.getBoolean(ColorPickerDialog.EXTRA_IS_GRADIENT, false)) {
                    applyColor(range, bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE))
                }
            }
            ColorPickerDialog.newInstance(
                initialColor = Color.WHITE,
                resultKey = ColorPickerDialog.RICH_TEXT_RESULT_KEY
            ).show(fragmentManager, ColorPickerDialog.TAG)
        }

        binding.btnTextSize.setOnClickListener {
            val input = EditText(context).apply {
                hint = "Size in px"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setSingleLine(true)
            }
            MaterialAlertDialogBuilder(context)
                .setTitle("Selected text size")
                .setView(input)
                .setPositiveButton("Apply") { _, _ ->
                    input.text.toString().toFloatOrNull()?.toInt()?.coerceIn(8, 512)?.let(::applySize)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnMoreText.setOnClickListener {
            val popup = android.widget.PopupMenu(context, binding.btnMoreText)
            popup.menu.add(0, 1, 0, "UPPERCASE")
            popup.menu.add(0, 2, 1, "Capitalize")
            popup.menu.add(0, 3, 2, "lowercase")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> applyTransformation { it.uppercase() }
                    2 -> applyTransformation { value ->
                        value.lowercase().replaceFirstChar { character ->
                            if (character.isLowerCase()) character.titlecase() else character.toString()
                        }
                    }
                    3 -> applyTransformation { it.lowercase() }
                }
                true
            }
            popup.show()
        }

        binding.btnClearText.setOnClickListener {
            binding.etTextInput.setText("")
            updateToolbarState()
        }

        binding.etTextInput.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode, menu: android.view.Menu): Boolean = false
            override fun onPrepareActionMode(mode: android.view.ActionMode, menu: android.view.Menu): Boolean = false
            override fun onActionItemClicked(mode: android.view.ActionMode, item: android.view.MenuItem): Boolean = false
            override fun onDestroyActionMode(mode: android.view.ActionMode) {}
        }

        binding.etTextInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                updateToolbarState()
            }
        })

        val dialog = MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
            .setView(binding.root)
            .create()

        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnOk.setOnClickListener {
            val editable = binding.etTextInput.editableText as SpannableStringBuilder
            onTextConfirmed(editable.toString(), extractSpans(editable))
            dialog.dismiss()
        }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        dialog.setOnShowListener {
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.80f).toInt()
            val height = (displayMetrics.heightPixels * 0.72f).toInt()
            dialog.window?.setLayout(width, height)

            binding.etTextInput.setTextColor(Color.parseColor("#07164F"))
            binding.etTextInput.highlightColor = Color.parseColor("#1D3B8F")

            val editable = SpannableStringBuilder(initialText)
            applyInitialSpans(editable)
            binding.etTextInput.setText(editable, android.widget.TextView.BufferType.SPANNABLE)
            binding.etTextInput.requestFocus()
            binding.etTextInput.post {
                if (initialText.isNotEmpty()) {
                    binding.etTextInput.selectAll()
                }
                updateToolbarState()
            }
        }

        dialog.show()
        return dialog
    }

    companion object {
        fun show(
            context: Context,
            initialText: String = "",
            initialSpans: List<RichTextSpan> = emptyList(),
            onTextConfirmed: (newText: String, spans: List<RichTextSpan>) -> Unit
        ): Dialog {
            return EditTextDialog(context, initialText, initialSpans, onTextConfirmed).show()
        }
    }
}
