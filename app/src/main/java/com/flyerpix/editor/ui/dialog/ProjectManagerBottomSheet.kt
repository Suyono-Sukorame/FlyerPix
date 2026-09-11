package com.flyerpix.editor.ui.dialog

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.LayoutProjectManagerBottomSheetBinding
import com.flyerpix.editor.project.ProjectModel
import com.flyerpix.editor.project.ProjectSerializer
import com.flyerpix.editor.ui.adapter.SavedProjectsAdapter
import java.io.File
import kotlin.math.min

/**
 * BottomSheet pop-up untuk melihat, memuat ulang, dan mengelola proyek .plp yang tersimpan (Prompt 48).
 */
class ProjectManagerBottomSheet : BottomSheetDialogFragment() {

    private enum class SortMode {
        NEWEST,
        OLDEST,
        NAME
    }

    private var _binding: LayoutProjectManagerBottomSheetBinding? = null
    private val binding get() = _binding!!

    var onProjectLoaded: ((ProjectModel) -> Unit)? = null
    var onImportExternalRequested: (() -> Unit)? = null

    private lateinit var adapter: SavedProjectsAdapter
    private var sortMode = SortMode.NEWEST

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

    override fun onStart() {
        super.onStart()

        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        val density = resources.displayMetrics.density
        val maxWidth = (640f * density).toInt()
        val compactWidth = min(
            (resources.displayMetrics.widthPixels * 0.80f).toInt(),
            maxWidth
        )

        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            width = compactWidth
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        bottomSheet.background = ColorDrawable(Color.TRANSPARENT)
        BottomSheetBehavior.from(bottomSheet).isFitToContents = true
        bottomSheet.requestLayout()
        bottomSheetDialog.window?.setLayout(compactWidth, WindowManager.LayoutParams.WRAP_CONTENT)
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
        binding.btnSortProjects.setOnClickListener { showSortMenu() }
        binding.btnDoneProjects.setOnClickListener { dismiss() }
    }

    private fun showSortMenu() {
        PopupMenu(requireContext(), binding.btnSortProjects).apply {
            menu.add("Newest first")
            menu.add("Oldest first")
            menu.add("Name A-Z")
            setOnMenuItemClickListener { item ->
                sortMode = when (item.title.toString()) {
                    "Oldest first" -> SortMode.OLDEST
                    "Name A-Z" -> SortMode.NAME
                    else -> SortMode.NEWEST
                }
                loadProjects()
                true
            }
        }.show()
    }

    fun loadProjects() {
        val context = context ?: return
        val files = ProjectSerializer.listProjects(context)
        val items = SavedProjectsAdapter.createFromFiles(files).let { projects ->
            when (sortMode) {
                SortMode.NEWEST -> projects.sortedByDescending { it.lastModified }
                SortMode.OLDEST -> projects.sortedBy { it.lastModified }
                SortMode.NAME -> projects.sortedBy { it.displayName.lowercase() }
            }
        }

        adapter.updateItems(items)

        binding.tvProjectCountBadge.text = "${items.size} Project(s)"
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
                .setTitle("Failed to Open Project")
                .setMessage("Project file could not be loaded: ${e.localizedMessage}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun confirmDeleteProject(file: File) {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
            .setTitle("Delete Project?")
            .setMessage("Are you sure you want to delete project '${file.nameWithoutExtension}'?")
            .setPositiveButton("Delete") { _, _ ->
                val deleted = file.delete()
                if (deleted) {
                    Toast.makeText(ctx, "Project deleted", Toast.LENGTH_SHORT).show()
                    loadProjects()
                } else {
                    Toast.makeText(ctx, "Failed to delete project file", Toast.LENGTH_SHORT).show()
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
