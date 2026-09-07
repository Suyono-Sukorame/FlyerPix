package com.flyerpix.editor.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.LayerPreviewRenderer
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.ImageLayer
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.StickerLayer
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ItemLayerAuthenticBinding
import java.util.Collections

/**
 * Adapter RecyclerView untuk Authentic PixelLab Floating Layer Panel (Prompt Revisi 1, 2, 3, 4).
 * Mendukung dua mode tampilan: Normal (Grid 2x2) dan Batch Selection (Checkbox).
 */
class AuthenticLayerAdapter(
    private val onLayerSelected: (CanvasLayer) -> Unit,
    private val onToggleVisibility: (CanvasLayer) -> Unit,
    private val onToggleLock: (CanvasLayer) -> Unit,
    private val onEditLayer: (CanvasLayer) -> Unit,
    private val onDeleteLayer: (CanvasLayer) -> Unit,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null,
    private val onCheckedChange: ((layer: CanvasLayer, isChecked: Boolean) -> Unit)? = null
) : RecyclerView.Adapter<AuthenticLayerAdapter.LayerViewHolder>() {

    private val displayedLayers = mutableListOf<CanvasLayer>()
    private var currentSelectedLayer: CanvasLayer? = null

    private val previewCache = mutableMapOf<String, android.graphics.Bitmap>()

    /** Apakah adapter dalam mode seleksi batch (SS 2). */
    private var isBatchMode: Boolean = false

    /** Set ID layer yang sedang dicentang dalam mode batch. */
    private val checkedLayerIds: MutableSet<String> = mutableSetOf()

    /**
     * Memperbarui daftar layer dari kanvas (otomatis dibalik urutannya agar z-index tertinggi di posisi paling atas).
     */
    fun submitLayers(canvasLayers: List<CanvasLayer>, selected: CanvasLayer?) {
        displayedLayers.clear()
        displayedLayers.addAll(canvasLayers.asReversed())
        currentSelectedLayer = selected
        previewCache.clear()
        // Bersihkan centangan layer yang sudah tidak ada
        checkedLayerIds.retainAll { id -> displayedLayers.any { it.id == id } }
        try {
            notifyDataSetChanged()
        } catch (_: Throwable) {
            // Ignored in unit tests
        }
    }

    fun getItems(): List<CanvasLayer> = displayedLayers

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition in 0 until displayedLayers.size && toPosition in 0 until displayedLayers.size) {
            Collections.swap(displayedLayers, fromPosition, toPosition)
            try {
                notifyItemMoved(fromPosition, toPosition)
            } catch (_: Throwable) {
                // Ignored in unit tests
            }
        }
    }

    /**
     * Mengubah mode adapter antara Normal (Grid 2x2) dan Batch Selection (Checkbox).
     */
    fun setBatchMode(enabled: Boolean) {
        isBatchMode = enabled
        if (!enabled) {
            checkedLayerIds.clear()
        }
        try {
            notifyDataSetChanged()
        } catch (_: Throwable) {
            // Ignored in unit tests
        }
    }

    fun isBatchMode(): Boolean = isBatchMode

    /**
     * Mengembalikan daftar layer yang saat ini dicentang dalam mode batch.
     */
    fun getCheckedLayers(): List<CanvasLayer> {
        return displayedLayers.filter { it.id in checkedLayerIds }
    }

    /**
     * Mengembalikan jumlah layer yang dicentang dalam mode batch.
     */
    fun checkedCount(): Int = checkedLayerIds.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val binding = ItemLayerAuthenticBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        holder.bind(displayedLayers[position])
    }

    override fun getItemCount(): Int = displayedLayers.size

    inner class LayerViewHolder(private val binding: ItemLayerAuthenticBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(layer: CanvasLayer) {
            val isSelected = (layer == currentSelectedLayer)
            val isChecked = (layer.id in checkedLayerIds)

            // 0. Thumbnail pratinjau objek lapisan
            val preview = previewCache[layer.id]
                ?: LayerPreviewRenderer.render(layer)?.also { previewCache[layer.id] = it }
            if (preview != null) {
                binding.ivLayerPreview.clearColorFilter()
                binding.ivLayerPreview.setImageBitmap(preview)
            } else {
                binding.ivLayerPreview.setImageResource(R.drawable.ic_edit_box_24px)
                binding.ivLayerPreview.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"))
            }

            // 1. Judul Layer
            binding.tvLayerTitle.text = when (layer) {
                is TextLayer -> if (layer.text.isNotBlank()) layer.text else "Teks Kosong"
                is ImageLayer -> "Gambar"
                is ShapeLayer -> "Bentuk (${layer.shapeType.name.lowercase().replace('_', ' ')})"
                is StickerLayer -> "Stiker"
                else -> "Lapisan"
            }

            // 2. Tampilkan Grid 2x2 atau Checkbox sesuai mode
            if (isBatchMode) {
                // Mode Batch: Sembunyikan Grid 2x2, tampilkan Checkbox
                binding.layoutLayerActionsGrid.visibility = View.GONE
                binding.layoutLayerBatchCheckbox.visibility = View.VISIBLE
                binding.cbLayerBatch.isChecked = isChecked

                // Garis highlight biru bawah kartu (aktif jika dicentang)
                binding.viewLayerBottomHighlight.visibility =
                    if (isChecked) View.VISIBLE else View.GONE

                // Background kartu sesuai status centang
                if (isChecked) {
                    binding.cardLayerItem.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                    binding.cardLayerItem.strokeColor = Color.parseColor("#1769FF")
                } else {
                    binding.cardLayerItem.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                    binding.cardLayerItem.strokeColor = Color.parseColor("#C8C8C8")
                }

                // Klik seluruh item untuk toggle centang
                binding.root.setOnClickListener {
                    val nowChecked = layer.id !in checkedLayerIds
                    if (nowChecked) checkedLayerIds.add(layer.id) else checkedLayerIds.remove(layer.id)
                    onCheckedChange?.invoke(layer, nowChecked)
                    try {
                        notifyItemChanged(adapterPosition)
                    } catch (_: Throwable) {}
                }

                // Nonaktifkan drag handle dalam mode batch
                binding.ivDragHandle.setOnTouchListener(null)

            } else {
                // Mode Normal: Tampilkan Grid 2x2, sembunyikan Checkbox
                binding.layoutLayerActionsGrid.visibility = View.VISIBLE
                binding.layoutLayerBatchCheckbox.visibility = View.GONE
                binding.viewLayerBottomHighlight.visibility = View.GONE

                // Seleksi Visual: latar dan garis tepi saat aktif terpilih
                if (isSelected) {
                    binding.cardLayerItem.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                    binding.cardLayerItem.strokeColor = Color.parseColor("#1769FF")
                } else {
                    binding.cardLayerItem.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                    binding.cardLayerItem.strokeColor = Color.parseColor("#C8C8C8")
                }

                // Lock Button & Icon
                if (layer.isLocked) {
                    binding.btnLayerLock.setImageResource(R.drawable.ic_lock_24px)
                    binding.btnLayerLock.setColorFilter(Color.parseColor("#D32F2F"))
                } else {
                    binding.btnLayerLock.setImageResource(R.drawable.ic_lock_open_24px)
                    binding.btnLayerLock.setColorFilter(Color.parseColor("#546E7A"))
                }
                binding.btnLayerLock.setOnClickListener { onToggleLock(layer) }

                // Edit Button
                binding.btnLayerEdit.setImageResource(R.drawable.ic_edit_box_24px)
                binding.btnLayerEdit.setColorFilter(Color.parseColor("#546E7A"))
                binding.btnLayerEdit.setOnClickListener { onEditLayer(layer) }

                // Visibility Button & Icon
                if (layer.isVisible) {
                    binding.btnLayerVisibility.setImageResource(R.drawable.ic_visibility_24px)
                    binding.btnLayerVisibility.setColorFilter(Color.parseColor("#1769FF"))
                } else {
                    binding.btnLayerVisibility.setImageResource(R.drawable.ic_visibility_off_24px)
                    binding.btnLayerVisibility.setColorFilter(Color.parseColor("#9E9E9E"))
                }
                binding.btnLayerVisibility.setOnClickListener { onToggleVisibility(layer) }

                // Delete Button
                binding.btnLayerDelete.setImageResource(R.drawable.ic_delete_24px)
                binding.btnLayerDelete.setColorFilter(Color.parseColor("#546E7A"))
                binding.btnLayerDelete.setOnClickListener { onDeleteLayer(layer) }

                // Klik pada item untuk seleksi layer di kanvas
                binding.root.setOnClickListener { onLayerSelected(layer) }

                // Drag Handle Touch Listener
                binding.ivDragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        onStartDrag?.invoke(this)
                    }
                    false
                }
            }
        }
    }
}
