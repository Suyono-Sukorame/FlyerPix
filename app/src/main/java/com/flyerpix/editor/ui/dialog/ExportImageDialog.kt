package com.flyerpix.editor.ui.dialog

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.ExportFormat
import com.flyerpix.editor.canvas.model.ExportQuality
import com.flyerpix.editor.databinding.DialogExportImageBinding

/**
 * Dialog Material Design untuk konfigurasi dan ekspor gambar kanvas beresolusi tinggi (Prompt 49).
 * Mendukung pemilihan format (PNG/JPEG) dan tingkat kualitas/dimensi mulai dari Default hingga Ultra HD 4K.
 */
class ExportImageDialog(
    private val context: Context,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
    private val onExportToGallery: (quality: ExportQuality, format: ExportFormat, customW: Int?, customH: Int?) -> Unit,
    private val onShareRequested: (quality: ExportQuality, format: ExportFormat, customW: Int?, customH: Int?) -> Unit
) {

    fun show(): Dialog {
        val binding = DialogExportImageBinding.inflate(LayoutInflater.from(context))

        var selectedFormat = ExportFormat.PNG
        var selectedQuality = ExportQuality.DEFAULT
        var isCustom = false

        // Spinner Kualitas & Dimensi Preset
        val qualityOptions = listOf(
            "Default (${canvasWidth} × ${canvasHeight} px)",
            "High 1.5x (${(canvasWidth * 1.5f).toInt()} × ${(canvasHeight * 1.5f).toInt()} px)",
            "Very High 2.0x (${(canvasWidth * 2.0f).toInt()} × ${(canvasHeight * 2.0f).toInt()} px)",
            "Ultra HD / 4K 4.0x (${(canvasWidth * 4.0f).toInt().coerceAtMost(8192)} × ${(canvasHeight * 4.0f).toInt().coerceAtMost(8192)} px)",
            "Kustom (Ukuran Manual)"
        )

        val spinnerAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            qualityOptions
        )
        binding.spinnerQuality.adapter = spinnerAdapter

        // Format RadioButton
        binding.rgFormat.setOnCheckedChangeListener { _, checkedId ->
            selectedFormat = if (checkedId == R.id.rbJpg) {
                ExportFormat.JPEG
            } else {
                ExportFormat.PNG
            }
        }

        fun updatePreview() {
            if (isCustom) {
                val customW = binding.etCustomWidth.text?.toString()?.toIntOrNull() ?: canvasWidth
                val customH = binding.etCustomHeight.text?.toString()?.toIntOrNull() ?: canvasHeight
                binding.tvResolutionPreview.text = "Resolusi Target: $customW × $customH px (${selectedFormat.name})"
            } else {
                val (w, h) = selectedQuality.calculateDimensions(canvasWidth, canvasHeight)
                val badge = if (selectedQuality == ExportQuality.ULTRA_HD) " • Ultra HD / 4K" else ""
                binding.tvResolutionPreview.text = "Resolusi Target: $w × $h px$badge (${selectedFormat.name})"
            }
        }

        binding.spinnerQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> { selectedQuality = ExportQuality.DEFAULT; isCustom = false }
                    1 -> { selectedQuality = ExportQuality.HIGH; isCustom = false }
                    2 -> { selectedQuality = ExportQuality.VERY_HIGH; isCustom = false }
                    3 -> { selectedQuality = ExportQuality.ULTRA_HD; isCustom = false }
                    4 -> { isCustom = true }
                }
                binding.layoutCustomSize.visibility = if (isCustom) View.VISIBLE else View.GONE
                updatePreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updatePreview() }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etCustomWidth.addTextChangedListener(watcher)
        binding.etCustomHeight.addTextChangedListener(watcher)

        // Set default custom edit text
        binding.etCustomWidth.setText(canvasWidth.toString())
        binding.etCustomHeight.setText(canvasHeight.toString())
        updatePreview()

        val dialog = MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
            .setView(binding.root)
            .setPositiveButton("Simpan ke Galeri") { d, _ ->
                val customW = if (isCustom) binding.etCustomWidth.text?.toString()?.toIntOrNull() else null
                val customH = if (isCustom) binding.etCustomHeight.text?.toString()?.toIntOrNull() else null
                onExportToGallery(selectedQuality, selectedFormat, customW, customH)
                d.dismiss()
            }
            .setNeutralButton("Bagikan") { d, _ ->
                val customW = if (isCustom) binding.etCustomWidth.text?.toString()?.toIntOrNull() else null
                val customH = if (isCustom) binding.etCustomHeight.text?.toString()?.toIntOrNull() else null
                onShareRequested(selectedQuality, selectedFormat, customW, customH)
                d.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel) { d, _ ->
                d.dismiss()
            }
            .create()

        dialog.show()
        return dialog
    }

    companion object {
        fun show(
            context: Context,
            canvasWidth: Int,
            canvasHeight: Int,
            onExportToGallery: (quality: ExportQuality, format: ExportFormat, customW: Int?, customH: Int?) -> Unit,
            onShareRequested: (quality: ExportQuality, format: ExportFormat, customW: Int?, customH: Int?) -> Unit
        ): Dialog {
            return ExportImageDialog(
                context,
                canvasWidth,
                canvasHeight,
                onExportToGallery,
                onShareRequested
            ).show()
        }
    }
}
