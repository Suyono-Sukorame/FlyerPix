package com.flyerpix.editor.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val onTextConfirmed: (newText: String) -> Unit
) {

    fun show(): Dialog {
        val binding = DialogEditTextBinding.inflate(LayoutInflater.from(context))

        // ===== Logika Formatting (Bold/Italic/Underline/Strikethrough) =====

        fun toggleStyle(style: Int) {
            val editable = binding.etTextInput.editableText
            val start = binding.etTextInput.selectionStart
            val end = binding.etTextInput.selectionEnd
            if (start < 0 || end <= start) return

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
            val start = binding.etTextInput.selectionStart
            val end = binding.etTextInput.selectionEnd
            if (start < 0 || end <= start) return

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
            val start = binding.etTextInput.selectionStart
            val end = binding.etTextInput.selectionEnd
            if (start < 0 || end <= start) return

            val spans = editable.getSpans(start, end, StrikethroughSpan::class.java)
            if (spans.isNotEmpty()) {
                for (span in spans) {
                    editable.removeSpan(span)
                }
            } else {
                editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
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

        binding.btnUppercase.setOnClickListener { applyTransformation { it.uppercase() } }
        binding.btnCapitalize.setOnClickListener {
            applyTransformation { value ->
                value.lowercase().replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase() else character.toString()
                }
            }
        }
        binding.btnLowercase.setOnClickListener { applyTransformation { it.lowercase() } }

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
                val result = binding.etTextInput.text?.toString().orEmpty()
                onTextConfirmed(result)
                d.dismiss()
            }
            .create()

        // Munculkan keyboard otomatis saat dialog terbuka
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        // Set teks, focus, dan selectAll SETELAH dialog di-show (layout selesai)
        dialog.setOnShowListener {
            binding.etTextInput.setText(initialText as CharSequence, android.widget.TextView.BufferType.SPANNABLE)
            binding.etTextInput.requestFocus()
            if (initialText.isNotEmpty()) {
                // Post ke queue agar layout pass selesai dulu
                binding.etTextInput.post {
                    binding.etTextInput.selectAll()
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
            onTextConfirmed: (newText: String) -> Unit
        ): Dialog {
            return EditTextDialog(context, initialText, onTextConfirmed).show()
        }
    }
}
