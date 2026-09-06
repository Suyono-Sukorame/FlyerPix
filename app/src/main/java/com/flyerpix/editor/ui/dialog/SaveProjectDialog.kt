package com.flyerpix.editor.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.databinding.DialogSaveProjectBinding
import com.flyerpix.editor.project.ProjectSerializer
import java.io.File

/**
 * Dialog Material Design untuk menyimpan proyek saat ini ke format .plp.
 */
class SaveProjectDialog(
    private val context: Context,
    private val pixelCanvasView: PixelCanvasView,
    private val defaultProjectName: String = "",
    private val onProjectSaved: (projectName: String, file: File) -> Unit
) {

    fun show(): Dialog {
        val binding = DialogSaveProjectBinding.inflate(LayoutInflater.from(context))

        // Nilai default nama proyek
        val initialName = if (defaultProjectName.isNotBlank() && defaultProjectName != "Untitled") {
            defaultProjectName
        } else {
            val existingCount = ProjectSerializer.listProjects(context).size
            "Proyek_${existingCount + 1}"
        }

        binding.etProjectName.setText(initialName)
        binding.etProjectName.setSelection(initialName.length)

        val dialog = MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
            .setTitle("Simpan sebagai Proyek")
            .setView(binding.root)
            .setNegativeButton(R.string.btn_cancel) { d, _ ->
                d.dismiss()
            }
            .setPositiveButton("Simpan", null) // listener di-override di bawah agar tidak langsung dismiss jika nama kosong
            .create()

        dialog.setOnShowListener {
            val positiveBtn = dialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                val inputName = binding.etProjectName.text?.toString()?.trim().orEmpty()
                if (inputName.isBlank()) {
                    binding.tilProjectName.error = "Nama proyek tidak boleh kosong"
                    return@setOnClickListener
                }
                binding.tilProjectName.error = null

                // Cek apakah file dengan nama aman ini sudah ada
                val safeFileName = inputName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                val targetFile = File(
                    ProjectSerializer.getProjectsDirectory(context),
                    "$safeFileName${ProjectSerializer.FILE_EXTENSION}"
                )

                if (targetFile.exists()) {
                    // Konfirmasi overwrite
                    MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
                        .setTitle("Timpa Proyek?")
                        .setMessage("Proyek dengan nama '$inputName' sudah ada. Apakah Anda ingin menimpanya?")
                        .setPositiveButton("Timpa") { _, _ ->
                            performSave(inputName, dialog)
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                } else {
                    performSave(inputName, dialog)
                }
            }
        }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        binding.etProjectName.requestFocus()

        return dialog
    }

    private fun performSave(name: String, dialog: Dialog) {
        try {
            val snapshot = pixelCanvasView.exportProjectSnapshot(name)
            val savedFile = ProjectSerializer.saveProject(context, snapshot, name)
            onProjectSaved(name, savedFile)
            dialog.dismiss()
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
                .setTitle("Gagal Menyimpan")
                .setMessage("Terjadi kesalahan saat menyimpan proyek: ${e.localizedMessage}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    companion object {
        fun show(
            context: Context,
            pixelCanvasView: PixelCanvasView,
            defaultProjectName: String = "",
            onProjectSaved: (projectName: String, file: File) -> Unit
        ): Dialog {
            return SaveProjectDialog(context, pixelCanvasView, defaultProjectName, onProjectSaved).show()
        }
    }
}
