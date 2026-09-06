package com.flyerpix.editor.ui.dialog

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.CanvasSizePreset
import com.flyerpix.editor.databinding.DialogImageSizeBinding

/**
 * Dialog pemilihan dan pengaturan ukuran resolusi kanvas (Image Size Dialog) (Prompt 43).
 * Menyediakan preset rasio standar industri (1:1, 16:9, YouTube Banner, Facebook Cover)
 * serta input ukuran kustom dengan rasio aspek real-time.
 */
class ImageSizeDialog private constructor(
    private val context: Context,
    private val initialWidth: Int,
    private val initialHeight: Int,
    private val onSizeApplied: (width: Int, height: Int) -> Unit
) {

    private var dialog: AlertDialog? = null
    private var isUpdatingFields = false
    private var lockedRatio: Float = 1f

    fun show() {
        val binding = DialogImageSizeBinding.inflate(LayoutInflater.from(context))

        // 1. Inisialisasi Spinner Preset
        val presets = CanvasSizePreset.PRESETS
        val presetNames = presets.map { "${it.name} (${it.width}×${it.height})" }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, presetNames)
        binding.spinnerPresets.adapter = adapter

        // 2. Set nilai awal input
        val startW = if (initialWidth > 0) initialWidth else 1080
        val startH = if (initialHeight > 0) initialHeight else 1080
        lockedRatio = startW.toFloat() / startH.toFloat()

        binding.etWidth.setText(startW.toString())
        binding.etHeight.setText(startH.toString())
        updateRatioBadge(binding, startW, startH)

        // Pilih preset awal yang cocok jika ada
        val matchedIndex = presets.indexOfFirst { it.width == startW && it.height == startH }
        if (matchedIndex != -1) {
            binding.spinnerPresets.setSelection(matchedIndex)
        } else {
            binding.spinnerPresets.setSelection(0) // Custom
        }

        // 3. Listener Pilihan Spinner
        binding.spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingFields) return
                val preset = presets[position]
                if (preset.name != "Custom") {
                    isUpdatingFields = true
                    binding.etWidth.setText(preset.width.toString())
                    binding.etHeight.setText(preset.height.toString())
                    lockedRatio = preset.width.toFloat() / preset.height.toFloat()
                    updateRatioBadge(binding, preset.width, preset.height)
                    isUpdatingFields = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 4. Listener Tombol Cepat (Chips)
        fun applyPresetByChip(w: Int, h: Int) {
            isUpdatingFields = true
            binding.etWidth.setText(w.toString())
            binding.etHeight.setText(h.toString())
            lockedRatio = w.toFloat() / h.toFloat()
            updateRatioBadge(binding, w, h)
            val idx = presets.indexOfFirst { it.width == w && it.height == h }
            if (idx != -1) binding.spinnerPresets.setSelection(idx)
            isUpdatingFields = false
        }

        binding.btnChip1x1.setOnClickListener { applyPresetByChip(1080, 1080) }
        binding.btnChip16x9.setOnClickListener { applyPresetByChip(1280, 720) }
        binding.btnChipBanner.setOnClickListener { applyPresetByChip(2560, 1440) }
        binding.btnChipFbCover.setOnClickListener { applyPresetByChip(820, 312) }

        // 5. Watcher untuk sinkronisasi teks input Width & Height
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFields) return

                val w = binding.etWidth.text.toString().toIntOrNull() ?: 0
                val h = binding.etHeight.text.toString().toIntOrNull() ?: 0

                // Jika kunci rasio aktif dan satu sisi diubah pengguna
                if (binding.cbLockAspectRatio.isChecked && lockedRatio > 0f) {
                    if (binding.etWidth.hasFocus() && w > 0) {
                        val newH = (w / lockedRatio).toInt().coerceAtLeast(1)
                        isUpdatingFields = true
                        binding.etHeight.setText(newH.toString())
                        isUpdatingFields = false
                    } else if (binding.etHeight.hasFocus() && h > 0) {
                        val newW = (h * lockedRatio).toInt().coerceAtLeast(1)
                        isUpdatingFields = true
                        binding.etWidth.setText(newW.toString())
                        isUpdatingFields = false
                    }
                }

                val curW = binding.etWidth.text.toString().toIntOrNull() ?: 0
                val curH = binding.etHeight.text.toString().toIntOrNull() ?: 0
                updateRatioBadge(binding, curW, curH)

                // Cek apakah ukuran cocok dengan salah satu preset
                val match = presets.indexOfFirst { it.width == curW && it.height == curH }
                if (match != -1) {
                    if (binding.spinnerPresets.selectedItemPosition != match) {
                        binding.spinnerPresets.setSelection(match)
                    }
                } else {
                    if (binding.spinnerPresets.selectedItemPosition != 0) {
                        binding.spinnerPresets.setSelection(0) // Custom
                    }
                }
            }
        }

        binding.etWidth.addTextChangedListener(textWatcher)
        binding.etHeight.addTextChangedListener(textWatcher)

        binding.cbLockAspectRatio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val w = binding.etWidth.text.toString().toIntOrNull() ?: 1080
                val h = binding.etHeight.text.toString().toIntOrNull() ?: 1080
                if (h > 0) lockedRatio = w.toFloat() / h.toFloat()
            }
        }

        // 6. Tombol Aksi
        binding.btnCancel.setOnClickListener {
            dialog?.dismiss()
        }

        binding.btnApply.setOnClickListener {
            val widthVal = binding.etWidth.text.toString().toIntOrNull() ?: 0
            val heightVal = binding.etHeight.text.toString().toIntOrNull() ?: 0

            if (widthVal < 50 || heightVal < 50) {
                Toast.makeText(context, "Ukuran minimal kanvas adalah 50 × 50 piksel", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (widthVal > 8192 || heightVal > 8192) {
                Toast.makeText(context, "Ukuran maksimal kanvas adalah 8192 × 8192 piksel", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onSizeApplied(widthVal, heightVal)
            dialog?.dismiss()
        }

        // 7. Bangun dan tampilkan dialog
        dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.show()
    }

    private fun updateRatioBadge(binding: DialogImageSizeBinding, w: Int, h: Int) {
        if (w > 0 && h > 0) {
            val ratioText = CanvasSizePreset.formatAspectRatio(w, h)
            binding.tvAspectRatioBadge.text = "Rasio: $ratioText"
            binding.tvAspectRatioBadge.visibility = View.VISIBLE
        } else {
            binding.tvAspectRatioBadge.visibility = View.GONE
        }
    }

    companion object {
        /**
         * Menampilkan [ImageSizeDialog] secara praktis.
         */
        fun show(
            context: Context,
            currentWidth: Int,
            currentHeight: Int,
            onSizeApplied: (width: Int, height: Int) -> Unit
        ): ImageSizeDialog {
            val dialog = ImageSizeDialog(context, currentWidth, currentHeight, onSizeApplied)
            dialog.show()
            return dialog
        }
    }
}
