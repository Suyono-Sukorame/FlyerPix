package com.flyerpix.editor.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flyerpix.editor.canvas.model.StickerItem
import com.flyerpix.editor.databinding.ItemStickerPickerBinding

/**
 * Adapter grid untuk picker stiker/emoji.
 *
 * Menampilkan daftar [StickerItem] dalam format grid. Saat sebuah item diklik,
 * callback [onStickerSelected] dipanggil dengan item yang dipilih.
 */
class StickerPickerAdapter(
    private var items: List<StickerItem>,
    private val onStickerSelected: (StickerItem) -> Unit
) : RecyclerView.Adapter<StickerPickerAdapter.StickerViewHolder>() {

    inner class StickerViewHolder(val binding: ItemStickerPickerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val binding = ItemStickerPickerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val item = items[position]

        with(holder.binding) {
            tvEmoji.text = item.emoji

            root.setOnClickListener {
                onStickerSelected(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<StickerItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
