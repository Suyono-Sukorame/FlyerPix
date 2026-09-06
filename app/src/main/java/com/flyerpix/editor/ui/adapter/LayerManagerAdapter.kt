package com.flyerpix.editor.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.ImageLayer
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ItemLayerBinding

/**
 * Adapter RecyclerView untuk menampilkan daftar layer di [LayerManagerBottomSheet].
 * Daftar disajikan dari atas ke bawah sesuai urutan z-index terbalik
 * (item teratas pada list adalah layer dengan z-index tertinggi pada kanvas).
 *
 * Mendukung Mode Normal (Reorder, Kunci, Sembunyikan, Hapus) dan
 * Mode Merge (Checkbox seleksi multi-layer untuk penggabungan - Prompt 36).
 */
class LayerManagerAdapter(
    private val onLayerSelected: (CanvasLayer) -> Unit,
    private val onToggleVisibility: (CanvasLayer) -> Unit,
    private val onToggleLock: (CanvasLayer) -> Unit,
    private val onDeleteLayer: (CanvasLayer) -> Unit,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
) : RecyclerView.Adapter<LayerManagerAdapter.LayerViewHolder>() {

    private val displayedLayers = mutableListOf<CanvasLayer>()
    private var currentSelectedLayer: CanvasLayer? = null

    /**
     * Mode Merge (Prompt 36). Saat aktif, setiap baris menampilkan CheckBox untuk seleksi penggabungan.
     */
    var isMergeMode: Boolean = false
        private set

    /**
     * Kumpulan layer yang sedang dicentang untuk digabungkan.
     */
    val selectedMergeLayers = mutableSetOf<CanvasLayer>()

    /**
     * Listener saat jumlah layer yang dicentang pada Mode Merge berubah.
     */
    var onMergeSelectionChanged: ((count: Int) -> Unit)? = null

    /**
     * Mengaktifkan atau menonaktifkan Mode Merge.
     */
    fun setMergeMode(enabled: Boolean) {
        if (isMergeMode != enabled) {
            isMergeMode = enabled
            selectedMergeLayers.clear()
            onMergeSelectionChanged?.invoke(0)
            notifyDataSetChanged()
        }
    }

    /**
     * Mendapatkan daftar layer yang dicentang untuk digabungkan.
     */
    fun getSelectedMergeLayers(): List<CanvasLayer> = selectedMergeLayers.toList()

    /**
     * Memperbarui daftar layer dari kanvas (otomatis dibalik urutannya agar z-index tertinggi di posisi paling atas).
     */
    fun submitLayers(canvasLayers: List<CanvasLayer>, selected: CanvasLayer?) {
        displayedLayers.clear()
        // Urutan z-index terbalik: layer teratas kanvas (indeks terakhir) tampil di atas
        displayedLayers.addAll(canvasLayers.asReversed())
        currentSelectedLayer = selected
        // Hapus layer yang mungkin sudah tidak ada dari seleksi merge
        selectedMergeLayers.retainAll(canvasLayers.toSet())
        notifyDataSetChanged()
    }

    /**
     * Mendapatkan daftar layer yang saat ini ditampilkan dalam adapter.
     */
    fun getItems(): List<CanvasLayer> = displayedLayers

    /**
     * Memindahkan posisi item dalam daftar tampilan adapter (misalnya saat drag-and-drop).
     */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition in 0 until displayedLayers.size && toPosition in 0 until displayedLayers.size) {
            java.util.Collections.swap(displayedLayers, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val binding = ItemLayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        holder.bind(displayedLayers[position])
    }

    override fun getItemCount(): Int = displayedLayers.size

    inner class LayerViewHolder(private val binding: ItemLayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(layer: CanvasLayer) {
            val context = binding.root.context
            val isSelected = (layer == currentSelectedLayer)
            val isMergeChecked = selectedMergeLayers.contains(layer)

            // 1. Highlight border & background card
            if (isMergeMode) {
                if (isMergeChecked) {
                    binding.cardLayerItem.strokeColor = 0xFF00E5FF.toInt() // Cyan PixelLab
                    binding.cardLayerItem.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                    binding.cardLayerItem.setCardBackgroundColor(0xFF1E3942.toInt())
                } else {
                    binding.cardLayerItem.strokeColor = 0xFF3A3A3A.toInt()
                    binding.cardLayerItem.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
                    binding.cardLayerItem.setCardBackgroundColor(0xFF2D2D2D.toInt())
                }
            } else {
                if (isSelected) {
                    binding.cardLayerItem.strokeColor = 0xFF00E5FF.toInt() // Cyan PixelLab
                    binding.cardLayerItem.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                    binding.cardLayerItem.setCardBackgroundColor(0xFF37474F.toInt())
                } else {
                    binding.cardLayerItem.strokeColor = 0xFF3A3A3A.toInt()
                    binding.cardLayerItem.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
                    binding.cardLayerItem.setCardBackgroundColor(0xFF2D2D2D.toInt())
                }
            }

            // 2. Tampilan kontrol awal (Drag Handle vs CheckBox Merge) & tombol aksi kanan
            if (isMergeMode) {
                binding.ivDragHandle.visibility = android.view.View.GONE
                binding.cbMergeSelect.visibility = android.view.View.VISIBLE
                binding.cbMergeSelect.isChecked = isMergeChecked

                binding.btnToggleVisibility.visibility = android.view.View.GONE
                binding.btnToggleLock.visibility = android.view.View.GONE
                binding.btnDeleteLayer.visibility = android.view.View.GONE
            } else {
                binding.ivDragHandle.visibility = android.view.View.VISIBLE
                binding.cbMergeSelect.visibility = android.view.View.GONE

                binding.btnToggleVisibility.visibility = android.view.View.VISIBLE
                binding.btnToggleLock.visibility = android.view.View.VISIBLE
                binding.btnDeleteLayer.visibility = android.view.View.VISIBLE
            }

            // 3. Icon jenis layer, label nama layer, dan detail teknis
            when (layer) {
                is TextLayer -> {
                    binding.ivLayerTypeIcon.setImageResource(R.drawable.ic_text_fields_24px)
                    binding.tvLayerName.text = if (layer.text.isNotBlank()) layer.text else "Teks Kosong"
                    binding.tvLayerDetails.text = "Teks • Ukuran ${layer.textSize.toInt()}sp • ${layer.getBlendModeName()}"
                }
                is ImageLayer -> {
                    binding.ivLayerTypeIcon.setImageResource(R.drawable.ic_sharp_photo_24px)
                    binding.tvLayerName.text = layer.layerName
                    val (w, h) = layer.getUnwarpedDimensions()
                    binding.tvLayerDetails.text = "Gambar • ${w.toInt()}×${h.toInt()}px • ${layer.getBlendModeName()}"
                }
                else -> {
                    binding.ivLayerTypeIcon.setImageResource(R.drawable.ic_sharp_crop_square_24px)
                    binding.tvLayerName.text = "Layer #${layer.id.take(4)}"
                    binding.tvLayerDetails.text = "Lapisan • ${layer.getBlendModeName()}"
                }
            }

            // 4. Status Visibilitas (Mata)
            if (layer.isVisible) {
                binding.btnToggleVisibility.setImageResource(R.drawable.ic_visibility_24px)
                binding.btnToggleVisibility.setColorFilter(0xFFFFFFFF.toInt())
                binding.tvLayerName.alpha = 1.0f
                binding.tvLayerDetails.alpha = 1.0f
                binding.cardLayerTypePreview.alpha = 1.0f
            } else {
                binding.btnToggleVisibility.setImageResource(R.drawable.ic_visibility_off_24px)
                binding.btnToggleVisibility.setColorFilter(0xFF757575.toInt())
                binding.tvLayerName.alpha = 0.45f
                binding.tvLayerDetails.alpha = 0.45f
                binding.cardLayerTypePreview.alpha = 0.45f
            }

            // 5. Status Kunci (Gembok) & Proteksi Hapus
            if (layer.isLocked) {
                binding.btnToggleLock.setImageResource(R.drawable.ic_lock_24px)
                binding.btnToggleLock.setColorFilter(0xFFFFD54F.toInt()) // Kuning/Amber gembok
                binding.btnDeleteLayer.isEnabled = false
                binding.btnDeleteLayer.alpha = 0.35f
            } else {
                binding.btnToggleLock.setImageResource(R.drawable.ic_lock_open_24px)
                binding.btnToggleLock.setColorFilter(0xFFFFFFFF.toInt())
                binding.btnDeleteLayer.isEnabled = true
                binding.btnDeleteLayer.alpha = 1.0f
            }

            // 6. Listener interaksi
            binding.root.setOnClickListener {
                if (isMergeMode) {
                    toggleMergeSelection(layer)
                } else {
                    onLayerSelected(layer)
                }
            }

            binding.cbMergeSelect.setOnClickListener {
                toggleMergeSelection(layer)
            }

            binding.btnToggleVisibility.setOnClickListener {
                onToggleVisibility(layer)
            }

            binding.btnToggleLock.setOnClickListener {
                onToggleLock(layer)
            }

            binding.btnDeleteLayer.setOnClickListener {
                if (!layer.isLocked) {
                    onDeleteLayer(layer)
                }
            }

            // 7. Handle drag touch listener untuk reorder
            binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag?.invoke(this)
                }
                false
            }
        }

        private fun toggleMergeSelection(layer: CanvasLayer) {
            if (selectedMergeLayers.contains(layer)) {
                selectedMergeLayers.remove(layer)
            } else {
                selectedMergeLayers.add(layer)
            }
            val isChecked = selectedMergeLayers.contains(layer)
            binding.cbMergeSelect.isChecked = isChecked
            val context = binding.root.context
            if (isChecked) {
                binding.cardLayerItem.strokeColor = 0xFF00E5FF.toInt()
                binding.cardLayerItem.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                binding.cardLayerItem.setCardBackgroundColor(0xFF1E3942.toInt())
            } else {
                binding.cardLayerItem.strokeColor = 0xFF3A3A3A.toInt()
                binding.cardLayerItem.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
                binding.cardLayerItem.setCardBackgroundColor(0xFF2D2D2D.toInt())
            }
            onMergeSelectionChanged?.invoke(selectedMergeLayers.size)
        }
    }
}
