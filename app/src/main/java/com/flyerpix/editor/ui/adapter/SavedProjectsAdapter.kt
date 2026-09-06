package com.flyerpix.editor.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonParser
import com.flyerpix.editor.databinding.ItemSavedProjectBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data holder untuk menampilkan ringkasan informasi proyek .plp pada list.
 */
data class SavedProjectItem(
    val file: File,
    val displayName: String,
    val details: String,
    val lastModified: Long
)

/**
 * Adapter RecyclerView untuk menampilkan daftar proyek .plp yang tersimpan.
 */
class SavedProjectsAdapter(
    private var items: List<SavedProjectItem>,
    private val onOpenProject: (File) -> Unit,
    private val onDeleteProject: (File) -> Unit
) : RecyclerView.Adapter<SavedProjectsAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(val binding: ItemSavedProjectBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemSavedProjectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val item = items[position]

        with(holder.binding) {
            tvProjectName.text = item.displayName
            tvProjectDetails.text = item.details

            root.setOnClickListener {
                onOpenProject(item.file)
            }

            btnDeleteProject.setOnClickListener {
                onDeleteProject(item.file)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SavedProjectItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    companion object {
        private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())

        /**
         * Membaca file .plp dan membuat [SavedProjectItem] yang memuat ringkasan metadata.
         */
        fun createFromFiles(files: List<File>): List<SavedProjectItem> {
            return files.map { file ->
                var name = file.nameWithoutExtension
                val details = try {
                    val json = file.readText(Charsets.UTF_8)
                    val root = JsonParser.parseString(json).asJsonObject
                    if (root.has("project_name")) {
                        val parsedName = root.get("project_name").asString
                        if (!parsedName.isNullOrBlank()) {
                            name = parsedName
                        }
                    }
                    val canvasObj = root.getAsJsonObject("canvas")
                    val w = canvasObj?.get("width")?.asInt ?: 1080
                    val h = canvasObj?.get("height")?.asInt ?: 1080
                    val layerCount = root.getAsJsonArray("layers")?.size() ?: 0
                    val sizeKb = (file.length() + 1023) / 1024
                    val dateStr = dateFormat.format(Date(file.lastModified()))

                    "$w×$h • $layerCount Layer • ${sizeKb} KB • $dateStr"
                } catch (e: Exception) {
                    val sizeKb = (file.length() + 1023) / 1024
                    val dateStr = dateFormat.format(Date(file.lastModified()))
                    "${sizeKb} KB • $dateStr"
                }

                SavedProjectItem(
                    file = file,
                    displayName = name,
                    details = details,
                    lastModified = file.lastModified()
                )
            }
        }
    }
}
