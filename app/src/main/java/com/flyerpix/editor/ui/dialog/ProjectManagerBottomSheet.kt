package com.flyerpix.editor.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.LayoutProjectManagerBottomSheetBinding
import com.flyerpix.editor.project.ProjectModel
import com.flyerpix.editor.project.ProjectSerializer
import com.flyerpix.editor.ui.adapter.SavedProjectsAdapter
import java.io.File

/**
 * BottomSheet pop-up untuk melihat, memuat ulang, dan mengelola proyek .plp yang tersimpan (Prompt 48).
 */
class ProjectManagerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutProjectManagerBottomSheetBinding? = null
    private val binding get() = _binding!!

    var onProjectLoaded: ((ProjectModel) -> Unit)? = null
    var onImportExternalRequested: (() -> Unit)? = null

    private lateinit var adapter: SavedProjectsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutProjectManagerBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        loadProjects()
    }

    private fun setupAdapter() {
        adapter = SavedProjectsAdapter(
            items = emptyList(),
            onOpenProject = { file ->
                openProjectFile(file)
            },
            onDeleteProject = { file ->
                confirmDeleteProject(file)
            }
        )
        binding.rvSavedProjects.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnImportExternal.setOnClickListener {
            onImportExternalRequested?.invoke()
            dismiss()
        }
    }

    fun loadProjects() {
        val context = context ?: return
        val files = ProjectSerializer.listProjects(context)
        val items = SavedProjectsAdapter.createFromFiles(files)

        adapter.updateItems(items)

        binding.tvProjectCountBadge.text = "${items.size} Proyek"
        if (items.isEmpty()) {
            binding.rvSavedProjects.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvSavedProjects.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun openProjectFile(file: File) {
        try {
            val project = ProjectSerializer.loadProject(file)
            onProjectLoaded?.invoke(project)
            dismiss()
        } catch (e: Exception) {
            val ctx = context ?: return
            MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                .setTitle("Gagal Membuka Proyek")
                .setMessage("File proyek tidak dapat dimuat: ${e.localizedMessage}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun confirmDeleteProject(file: File) {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
            .setTitle("Hapus Proyek?")
            .setMessage("Apakah Anda yakin ingin menghapus proyek '${file.nameWithoutExtension}'?")
            .setPositiveButton("Hapus") { _, _ ->
                val deleted = file.delete()
                if (deleted) {
                    Toast.makeText(ctx, "Proyek dihapus", Toast.LENGTH_SHORT).show()
                    loadProjects()
                } else {
                    Toast.makeText(ctx, "Gagal menghapus file proyek", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProjectManagerBottomSheet"

        fun show(
            fragmentManager: FragmentManager,
            onProjectLoaded: (ProjectModel) -> Unit,
            onImportExternalRequested: (() -> Unit)? = null
        ): ProjectManagerBottomSheet {
            val sheet = ProjectManagerBottomSheet().apply {
                this.onProjectLoaded = onProjectLoaded
                this.onImportExternalRequested = onImportExternalRequested
            }
            sheet.show(fragmentManager, TAG)
            return sheet
        }
    }
}
