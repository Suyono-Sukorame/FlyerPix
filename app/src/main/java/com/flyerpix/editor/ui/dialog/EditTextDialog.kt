package com.flyerpix.editor.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.DialogEditTextBinding

/**
 * Dialog pop-up Material Design untuk mengedit teks secara interaktif.
 * Menyediakan input multi-line, tombol cepat ubah kapitalisasi (ALL CAPS / lowercase),
 * serta tombol konfirmasi OK dan Batal.
 */
class EditTextDialog(
    private val context: Context,
    private val initialText: String = "",
    private val onTextConfirmed: (newText: String) -> Unit
) {

    fun show(): Dialog {
        val binding = DialogEditTextBinding.inflate(LayoutInflater.from(context))

        // Set teks awal dan posisikan kursor di akhir teks
        binding.etTextInput.setText(initialText)
        binding.etTextInput.setSelection(initialText.length)

        // Tombol Cepat: Toggle ALL CAPS / lowercase
        binding.btnToggleCase.setOnClickListener {
            val current = binding.etTextInput.text?.toString().orEmpty()
            val hasLetters = current.any { it.isLetter() }
            val isAllUpper = hasLetters && current.none { it.isLowerCase() }

            val transformed = if (isAllUpper) {
                current.lowercase()
            } else {
                current.uppercase()
            }
            binding.etTextInput.setText(transformed)
            binding.etTextInput.setSelection(transformed.length)
        }

        // Tombol Cepat: Bersihkan teks
        binding.btnClearText.setOnClickListener {
            binding.etTextInput.setText("")
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
        dialog.show()
        binding.etTextInput.requestFocus()

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
