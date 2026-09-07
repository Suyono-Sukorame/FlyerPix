package com.flyerpix.editor.template

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.flyerpix.editor.R

/**
 * Adapter untuk menampilkan carousel horizontal Template Preset otentik PixelLab.
 */
class TemplatePresetAdapter(
    private val presets: List<TemplatePreset>,
    private val onPresetSelected: (TemplatePreset) -> Unit
) : RecyclerView.Adapter<TemplatePresetAdapter.PresetViewHolder>() {

    private var selectedIndex: Int = 1 // Default dipilih

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template_preset, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val preset = presets[position]
        holder.bind(preset, position == selectedIndex)
    }

    override fun getItemCount(): Int = presets.size

    inner class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardPresetPreview)
        private val previewContainer: FrameLayout = itemView.findViewById(R.id.presetPreviewContainer)
        private val tvPreviewText: TextView = itemView.findViewById(R.id.tvPresetPreviewText)
        private val imgIcon: ImageView = itemView.findViewById(R.id.imgPresetIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvPresetTitle)

        fun bind(preset: TemplatePreset, isSelected: Boolean) {
            tvTitle.text = preset.title

            if (isSelected) {
                card.strokeColor = 0xFF18C8F5.toInt() // Biru cyan aktif
                card.strokeWidth = 4
                tvTitle.setTextColor(0xFF18C8F5.toInt())
            } else {
                card.strokeColor = 0xFF555555.toInt()
                card.strokeWidth = 2
                tvTitle.setTextColor(0xFFCCCCCC.toInt())
            }

            if (preset.isMyProjects) {
                tvPreviewText.visibility = View.GONE
                imgIcon.visibility = View.VISIBLE
                imgIcon.setImageResource(R.drawable.ic_outline_photo_24px)
                previewContainer.setBackgroundColor(0xFF1A3A6B.toInt())
            } else {
                imgIcon.visibility = View.GONE
                tvPreviewText.visibility = View.VISIBLE
                tvPreviewText.text = if (preset.id == "keep_calm") "KEEP\nCALM" else if (preset.id == "three_d") "3D" else "New\nText"

                val gd = GradientDrawable().apply {
                    if (preset.isRadial) {
                        gradientType = GradientDrawable.RADIAL_GRADIENT
                        gradientRadius = 90f
                    } else {
                        gradientType = GradientDrawable.LINEAR_GRADIENT
                        orientation = GradientDrawable.Orientation.TOP_BOTTOM
                    }
                    colors = preset.previewBgColors
                    cornerRadius = 8f
                }
                previewContainer.background = gd
            }

            itemView.setOnClickListener {
                val prev = selectedIndex
                selectedIndex = adapterPosition
                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(selectedIndex)
                onPresetSelected(preset)
            }
        }
    }
}
