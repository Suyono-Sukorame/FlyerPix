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
            if (start >= 0 && end > start) {
                savedSelectionStart = start
                savedSelectionEnd = end
            }
        }

        fun selectionRange(): IntRange? {
            rememberSelection()
            return if (savedSelectionStart >= 0 && savedSelectionEnd > savedSelectionStart) {
                savedSelectionStart until savedSelectionEnd
            } else {
                null
            }
        }

        fun preserveSelectionOnClick(view: android.view.View) {
            view.setOnTouchListener { _, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) rememberSelection()
                false
            }
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }

        // ===== Logika Formatting (Bold/Italic/Underline/Strikethrough) =====

        fun toggleStyle(style: Int) {
            val editable = binding.etTextInput.editableText
            val range = selectionRange() ?: return
            val start = range.first
            val end = range.last + 1

            val sb = SpannableStringBuilder(editable)
            val spans = sb.getSpans(start, end, StyleSpan::class.java)
            val isActive = spans.any { it.style == style }

            if (isActive) {
                // Hapus style yang ada
                for (span in spans) {
                    if (span.style == style) {
                        editable.removeSpan(span)
                    }
                }
            } else {
                // Tambahkan style baru
                editable.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
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
                val style = editable.getSpans(probe, probe + 1, StyleSpan::class.java)
                    .lastOrNull()
                val color = editable.getSpans(probe, probe + 1, ForegroundColorSpan::class.java)
                    .lastOrNull()?.foregroundColor
                val size = editable.getSpans(probe, probe + 1, AbsoluteSizeSpan::class.java)
                    .lastOrNull()?.size?.toFloat()
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
        }

        fun applySize(size: Int) {
            val range = selectionRange() ?: return
            binding.etTextInput.editableText.setSpan(
                AbsoluteSizeSpan(size, false), range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // ===== Logika Kapitalisasi (Uppercase/Capitalize/Lowercase) =====

        fun applyTransformation(transform: (String) -> String) {
            val editable = binding.etTextInput.editableText as SpannableStringBuilder
            val start = binding.etTextInput.selectionStart
            val end = binding.etTextInput.selectionEnd
            val hasSelection = start in 0..end && end > start

            // Simpan spans beserta posisinya supaya format teks tetap terjaga
            val allLen = editable.length
            val spans = editable.getSpans(0, allLen, Any::class.java).map { span ->
                Triple(span, editable.getSpanStart(span), editable.getSpanEnd(span))
            }

            // Apply transform langsung pada Editable (spans di luar range otomatis dipertahankan)
            val targetStart = if (hasSelection) start else 0
            val targetEnd = if (hasSelection) end else allLen
            val selected = editable.subSequence(targetStart, targetEnd).toString()
            val transformed = transform(selected)

            editable.replace(targetStart, targetEnd, transformed)
            binding.etTextInput.setSelection(targetStart, targetStart + transformed.length)

            // Restore spans untuk seluruh teks (termasuk yang tergerak oleh replace)
            for ((span, s, e) in spans) {
                editable.setSpan(span, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // ===== Wire up toolbar =====

        binding.btnBold.setOnClickListener { toggleStyle(Typeface.BOLD) }
        binding.btnItalic.setOnClickListener { toggleStyle(Typeface.ITALIC) }
        binding.btnUnderline.setOnClickListener { toggleUnderline() }
        binding.btnStrikethrough.setOnClickListener { toggleStrikethrough() }
        listOf(
            binding.btnBold,
            binding.btnItalic,
            binding.btnUnderline,
            binding.btnStrikethrough,
            binding.btnTextColor,
            binding.btnTextSize,
            binding.btnUppercase,
            binding.btnCapitalize,
            binding.btnLowercase
        ).forEach(::preserveSelectionOnClick)

        binding.btnUppercase.setOnClickListener { applyTransformation { it.uppercase() } }
        binding.btnCapitalize.setOnClickListener {
            applyTransformation { value ->
                value.lowercase().replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase() else character.toString()
                }
            }
        }
        binding.btnLowercase.setOnClickListener { applyTransformation { it.lowercase() } }
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
                hint = "Ukuran px"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setSingleLine(true)
            }
            MaterialAlertDialogBuilder(context)
                .setTitle("Ukuran teks terseleksi")
                .setView(input)
                .setPositiveButton("Terapkan") { _, _ ->
                    input.text.toString().toFloatOrNull()?.toInt()?.coerceIn(8, 512)?.let(::applySize)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Tombol Cepat: Bersihkan teks
        binding.btnClearText.setOnClickListener {
            binding.etTextInput.setText("")
        }

        // Nonaktifkan toolbar sistem keyboard (text action bar)
        binding.etTextInput.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode, menu: android.view.Menu): Boolean = false
            override fun onPrepareActionMode(mode: android.view.ActionMode, menu: android.view.Menu): Boolean = false
            override fun onActionItemClicked(mode: android.view.ActionMode, item: android.view.MenuItem): Boolean = false
            override fun onDestroyActionMode(mode: android.view.ActionMode) {}
        }

        val dialog = MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
            .setView(binding.root)
            .setNegativeButton(R.string.btn_cancel) { d, _ ->
                d.dismiss()
            }
            .setPositiveButton(R.string.btn_ok) { d, _ ->
                val editable = binding.etTextInput.editableText as SpannableStringBuilder
                onTextConfirmed(editable.toString(), extractSpans(editable))
                d.dismiss()
            }
            .create()

        // Munculkan keyboard otomatis saat dialog terbuka
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        // Set teks, focus, dan selectAll SETELAH dialog di-show (layout selesai)
        dialog.setOnShowListener {
            val editable = SpannableStringBuilder(initialText)
            applyInitialSpans(editable)
            binding.etTextInput.setText(editable, android.widget.TextView.BufferType.SPANNABLE)
            binding.etTextInput.requestFocus()
            if (initialText.isNotEmpty()) {
                // Post ke queue agar layout pass selesai dulu
                binding.etTextInput.post {
                    binding.etTextInput.selectAll()
                    rememberSelection()
                }
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
