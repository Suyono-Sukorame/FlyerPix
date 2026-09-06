package com.flyerpix.editor.font

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flyerpix.editor.databinding.ItemFontPickerBinding

/**
 * Adapter horizontal RecyclerView untuk memilih font teks dengan preview visual tiap font.
 */
class FontPickerAdapter(
    private var fonts: List<FontItem>,
    private val onFontSelected: (FontItem) -> Unit
) : RecyclerView.Adapter<FontPickerAdapter.FontViewHolder>() {

    private var selectedPosition = 0

    inner class FontViewHolder(val binding: ItemFontPickerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val item = fonts[position]
        val isSelected = position == selectedPosition

        with(holder.binding) {
            tvFontName.text = item.name
            tvFontCategory.text = item.category

            // Terapkan Typeface langsung pada teks preview visual
            tvFontPreview.typeface = item.typeface

            // Indikator visual seleksi font
            if (isSelected) {
                cardFont.strokeColor = Color.parseColor("#1E88E5")
                cardFont.strokeWidth = 4
                cardFont.cardElevation = 6f
                tvFontName.setTextColor(Color.parseColor("#1E88E5"))
            } else {
                cardFont.strokeColor = Color.parseColor("#DDDDDD")
                cardFont.strokeWidth = 2
                cardFont.cardElevation = 2f
                tvFontName.setTextColor(Color.parseColor("#555555"))
            }

            root.setOnClickListener {
                val prevSelected = selectedPosition
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    selectedPosition = currentPos
                    notifyItemChanged(prevSelected)
                    notifyItemChanged(selectedPosition)
                    onFontSelected(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = fonts.size

    fun updateFonts(newFonts: List<FontItem>) {
        fonts = newFonts
        notifyDataSetChanged()
    }

    fun setSelectedFont(name: String) {
        val index = fonts.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index != -1 && index != selectedPosition) {
            val prev = selectedPosition
            selectedPosition = index
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
        }
    }
}
