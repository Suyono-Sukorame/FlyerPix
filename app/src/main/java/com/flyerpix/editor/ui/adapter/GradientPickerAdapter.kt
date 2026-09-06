package com.flyerpix.editor.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.databinding.ItemGradientPickerBinding

/**
 * Adapter horizontal RecyclerView untuk menampilkan swatch preview daftar preset gradasi warna teks.
 */
class GradientPickerAdapter(
    private var presets: List<GradientColor> = GradientColor.PRESETS,
    private val onGradientSelected: (GradientColor) -> Unit
) : RecyclerView.Adapter<GradientPickerAdapter.GradientViewHolder>() {

    private var selectedPosition = -1

    inner class GradientViewHolder(val binding: ItemGradientPickerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradientViewHolder {
        val binding = ItemGradientPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GradientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GradientViewHolder, position: Int) {
        val item = presets[position]
        val isSelected = position == selectedPosition

        with(holder.binding) {
            tvGradientName.text = item.name

            // Render visual swatch menggunakan GradientDrawable
            val gd = GradientDrawable().apply {
                cornerRadius = 10f
                colors = item.colors
                when (item.type) {
                    GradientType.LINEAR -> {
                        gradientType = GradientDrawable.LINEAR_GRADIENT
                        orientation = GradientDrawable.Orientation.LEFT_RIGHT
                    }
                    GradientType.RADIAL -> {
                        gradientType = GradientDrawable.RADIAL_GRADIENT
                        gradientRadius = 50f
                    }
                    GradientType.SWEEP -> {
                        gradientType = GradientDrawable.SWEEP_GRADIENT
                    }
                }
            }
            viewGradientSwatch.background = gd

            // Indikator visual seleksi item
            if (isSelected) {
                cardGradient.strokeColor = Color.parseColor("#1E88E5")
                cardGradient.strokeWidth = 4
                cardGradient.cardElevation = 6f
                tvGradientName.setTextColor(Color.parseColor("#1E88E5"))
            } else {
                cardGradient.strokeColor = Color.parseColor("#DDDDDD")
                cardGradient.strokeWidth = 2
                cardGradient.cardElevation = 2f
                tvGradientName.setTextColor(Color.parseColor("#555555"))
            }

            root.setOnClickListener {
                val prevSelected = selectedPosition
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    selectedPosition = currentPos
                    if (prevSelected != -1) notifyItemChanged(prevSelected)
                    notifyItemChanged(selectedPosition)
                    onGradientSelected(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = presets.size

    fun setSelectedPreset(preset: GradientColor?) {
        if (preset == null) {
            val prev = selectedPosition
            selectedPosition = -1
            if (prev != -1) notifyItemChanged(prev)
            return
        }
        val idx = presets.indexOfFirst { it.name.equals(preset.name, ignoreCase = true) }
        if (idx != selectedPosition) {
            val prev = selectedPosition
            selectedPosition = idx
            if (prev != -1) notifyItemChanged(prev)
            if (selectedPosition != -1) notifyItemChanged(selectedPosition)
        }
    }
}
