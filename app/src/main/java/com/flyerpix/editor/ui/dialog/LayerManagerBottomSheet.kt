package com.flyerpix.editor.ui.dialog

import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.databinding.LayoutLayerManagerBottomSheetBinding
import com.flyerpix.editor.ui.adapter.LayerManagerAdapter
import java.util.Collections

/**
 * BottomSheet pop-up untuk mengelola lapisan (Layer Manager UI) (Prompt 31, 32, 33).
 * Menampilkan seluruh layer aktif di kanvas dari atas ke bawah (sesuai urutan z-index terbalik),
 * dan mendukung drag-and-drop reorder z-index menggunakan ItemTouchHelper (Prompt 33).
 */
class LayerManagerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutLayerManagerBottomSheetBinding? = null
    private val binding get() = _binding!!

    var pixelCanvasView: PixelCanvasView? = null

    private var previousLayersChangedListener: (() -> Unit)? = null
    private var myLayersChangedListener: (() -> Unit)? = null

    private lateinit var adapter: LayerManagerAdapter
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutLayerManagerBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupItemTouchHelper()
        setupListeners()
        refreshLayers()

        // Berlangganan perubahan daftar layer di kanvas agar dialog selalu sinkron
        // (misalnya layer ditambahkan/dihapus/di-undo saat dialog terbuka).
        previousLayersChangedListener = pixelCanvasView?.onLayersChangedListener
        myLayersChangedListener = {
            previousLayersChangedListener?.invoke()
            refreshLayers()
        }
        pixelCanvasView?.onLayersChangedListener = myLayersChangedListener
    }

    private fun setupAdapter() {
        adapter = LayerManagerAdapter(
            onLayerSelected = { layer ->
                pixelCanvasView?.let { canvas ->
                    canvas.selectedLayer = layer
                    refreshLayers()
                }
            },
            onToggleVisibility = { layer ->
                layer.isVisible = !layer.isVisible
                if (!layer.isVisible && pixelCanvasView?.selectedLayer == layer) {
                    pixelCanvasView?.selectedLayer = null
                }
                pixelCanvasView?.invalidate()
                refreshLayers()
            },
            onToggleLock = { layer ->
                layer.isLocked = !layer.isLocked
                if (layer.isLocked && pixelCanvasView?.selectedLayer == layer) {
                    pixelCanvasView?.selectedLayer = null
                }
                pixelCanvasView?.invalidate()
                refreshLayers()
            },
            onDeleteLayer = { layer ->
                pixelCanvasView?.let { canvas ->
                    canvas.removeLayer(layer)
                    refreshLayers()
                }
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
            }
        )
        binding.rvLayers.adapter = adapter
    }

    /**
     * Memasang [ItemTouchHelper] pada RecyclerView untuk reordering urutan layer (Prompt 33).
     */
    private fun setupItemTouchHelper() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun isLongPressDragEnabled(): Boolean = true

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false
                }
                return reorderLayer(fromPos, toPos)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Tidak ada aksi swipe
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.animate()
                        ?.scaleX(1.02f)
                        ?.scaleY(1.02f)
                        ?.setDuration(120)
                        ?.start()
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
        }

        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(binding.rvLayers)
        itemTouchHelper = helper
    }

    private fun setupListeners() {
        binding.btnCloseBottomSheet.setOnClickListener {
            dismiss()
        }

        binding.btnToggleMergeMode.setOnClickListener {
            setMergeMode(!adapter.isMergeMode)
        }

        binding.btnCancelMerge.setOnClickListener {
            setMergeMode(false)
        }

        binding.btnExecuteMerge.setOnClickListener {
            executeMerge()
        }

        adapter.onMergeSelectionChanged = { count ->
            binding.btnExecuteMerge.isEnabled = (count >= 2)
            binding.tvMergeCountStatus.text = if (count == 0) {
                "Pilih minimal 2 layer"
            } else {
                "$count layer terpilih"
            }
        }
    }

    /**
     * Mengatur status aktif/nonaktif Mode Merge pada Layer Manager (Prompt 36).
     */
    fun setMergeMode(enabled: Boolean) {
        adapter.setMergeMode(enabled)
        binding.layoutMergeActionBar.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnToggleMergeMode.setColorFilter(
            if (enabled) 0xFF18C8F5.toInt() else 0xFFAAAAAA.toInt()
        )
        if (enabled) {
            binding.tvMergeCountStatus.text = "Pilih minimal 2 layer"
            binding.btnExecuteMerge.isEnabled = false
        }
    }

    /**
     * Menjalankan penggabungan layer-layer yang dicentang menjadi satu objek ImageLayer (Prompt 36).
     */
    fun executeMerge(): Boolean {
        val selected = adapter.getSelectedMergeLayers()
        if (selected.size < 2) return false
        val canvas = pixelCanvasView ?: return false

        val merged = canvas.mergeLayers(selected)
        if (merged != null) {
            setMergeMode(false)
            refreshLayers()
            context?.let { ctx ->
                Toast.makeText(ctx, "${selected.size} layer berhasil digabungkan!", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return false
    }

    /**
     * Menukar posisi layer saat didrag di RecyclerView dan menyinkronkannya
     * langsung ke tumpukan z-index kanvas [PixelCanvasView.layers] (Prompt 33).
     */
    fun reorderLayer(fromDisplayPos: Int, toDisplayPos: Int): Boolean {
        val canvas = pixelCanvasView ?: return false
        val swapped = swapCanvasLayersFromDisplay(canvas.layers, fromDisplayPos, toDisplayPos)
        if (swapped) {
            adapter.moveItem(fromDisplayPos, toDisplayPos)
            canvas.invalidate()
        }
        return swapped
    }

    /**
     * Memperbarui data layer pada adapter dan tampilan badge / empty state.
     */
    fun refreshLayers() {
        val canvas = pixelCanvasView ?: return
        val layers = canvas.layers
        val count = layers.size

        // Update badge jumlah layer
        binding.tvLayerCountBadge.text = "$count Layer"

        // Tampilkan empty state jika kanvas kosong
        if (count == 0) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvLayers.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvLayers.visibility = View.VISIBLE
            adapter.submitLayers(layers, canvas.selectedLayer)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Lepas langganan agar daftar listener panggilan tidak membocorkan referensi dialog
        val current = pixelCanvasView?.onLayersChangedListener
        if (current == myLayersChangedListener) {
            pixelCanvasView?.onLayersChangedListener = previousLayersChangedListener
        }
        myLayersChangedListener = null
        _binding = null
    }

    companion object {
        const val TAG = "LayerManagerBottomSheet"

        /**
         * Membuka [LayerManagerBottomSheet] secara aman dengan FragmentManager.
         */
        fun show(fragmentManager: FragmentManager, canvasView: PixelCanvasView): LayerManagerBottomSheet {
            val existing = fragmentManager.findFragmentByTag(TAG) as? LayerManagerBottomSheet
            if (existing != null && existing.isAdded) {
                return existing
            }
            val sheet = LayerManagerBottomSheet().apply {
                this.pixelCanvasView = canvasView
            }
            sheet.show(fragmentManager, TAG)
            return sheet
        }

        /**
         * Menghasilkan daftar layer dengan urutan z-index terbalik (Prompt 31):
         * Item pertama di daftar hasil adalah layer dengan z-index tertinggi (paling atas di kanvas).
         */
        fun getReversedLayers(layers: List<CanvasLayer>): List<CanvasLayer> {
            return layers.reversed()
        }

        /**
         * Mengonversi indeks tampilan pada daftar (0 = teratas) menjadi indeks list kanvas [layers].
         */
        fun toCanvasIndex(displayIndex: Int, totalSize: Int): Int {
            if (totalSize <= 0) return 0
            return ((totalSize - 1) - displayIndex).coerceIn(0, totalSize - 1)
        }

        /**
         * Mengonversi indeks list kanvas [layers] menjadi indeks tampilan pada daftar (0 = teratas).
         */
        fun toDisplayIndex(canvasIndex: Int, totalSize: Int): Int {
            if (totalSize <= 0) return 0
            return ((totalSize - 1) - canvasIndex).coerceIn(0, totalSize - 1)
        }

        /**
         * Melakukan swap/reorder pada tumpukan layer kanvas berdasarkan perpindahan posisi di daftar UI (Prompt 33).
         */
        fun swapCanvasLayersFromDisplay(
            canvasLayers: MutableList<CanvasLayer>,
            fromDisplayPos: Int,
            toDisplayPos: Int
        ): Boolean {
            val totalSize = canvasLayers.size
            if (fromDisplayPos !in 0 until totalSize || toDisplayPos !in 0 until totalSize) return false
            if (fromDisplayPos == toDisplayPos) return false

            val canvasFrom = toCanvasIndex(fromDisplayPos, totalSize)
            val canvasTo = toCanvasIndex(toDisplayPos, totalSize)
            Collections.swap(canvasLayers, canvasFrom, canvasTo)
            return true
        }

        /**
         * Memindahkan [layer] ke posisi z-index paling atas (To Front) pada list kanvas [canvasLayers] (Prompt 35).
         * Pada daftar tampilan UI [getReversedLayers], layer ini akan berada di posisi paling atas (index 0).
         */
        fun bringCanvasLayerToFront(canvasLayers: MutableList<CanvasLayer>, layer: CanvasLayer): Boolean {
            val index = canvasLayers.indexOf(layer)
            if (index == -1) return false
            if (index == canvasLayers.size - 1) return true
            canvasLayers.removeAt(index)
            canvasLayers.add(layer)
            return true
        }

        /**
         * Memindahkan [layer] ke posisi z-index paling bawah (To Back) pada list kanvas [canvasLayers] (Prompt 35).
         * Pada daftar tampilan UI [getReversedLayers], layer ini akan berada di posisi paling bawah (index size - 1).
         */
        fun sendCanvasLayerToBack(canvasLayers: MutableList<CanvasLayer>, layer: CanvasLayer): Boolean {
            val index = canvasLayers.indexOf(layer)
            if (index == -1) return false
            if (index == 0) return true
            canvasLayers.removeAt(index)
            canvasLayers.add(0, layer)
            return true
        }

        /**
         * Menghitung bounding box bersama yang mencakup semua layer dalam daftar [layersToMerge] (Prompt 36).
         */
        fun calculateMergedBoundingBox(layersToMerge: List<CanvasLayer>): RectF {
            if (layersToMerge.isEmpty()) return RectF(0f, 0f, 0f, 0f)
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE

            for (layer in layersToMerge) {
                val b = layer.getBounds()
                if (b.left < minX) minX = b.left
                if (b.top < minY) minY = b.top
                if (b.right > maxX) maxX = b.right
                if (b.bottom > maxY) maxY = b.bottom
            }
            return RectF().apply {
                left = minX
                top = minY
                right = maxX
                bottom = maxY
            }
        }

        /**
         * Menghitung indeks z-index terendah di [canvasLayers] dari kumpulan [layersToMerge] (Prompt 36).
         */
        fun calculateMergedZIndex(canvasLayers: List<CanvasLayer>, layersToMerge: List<CanvasLayer>): Int {
            return layersToMerge.map { canvasLayers.indexOf(it) }.filter { it != -1 }.minOrNull() ?: 0
        }

        /**
         * Melakukan logika penghapusan layer lama dan penyisipan layer hasil merge pada posisi z-index terendah (Prompt 36).
         */
        fun performMergeLogic(
            canvasLayers: MutableList<CanvasLayer>,
            layersToMerge: List<CanvasLayer>,
            mergedLayer: CanvasLayer
        ): Boolean {
            if (layersToMerge.size < 2) return false
            val valid = layersToMerge.filter { canvasLayers.contains(it) }
            if (valid.size < 2) return false

            val minIndex = calculateMergedZIndex(canvasLayers, valid)
            canvasLayers.removeAll(valid)
            val insertIndex = minIndex.coerceIn(0, canvasLayers.size)
            canvasLayers.add(insertIndex, mergedLayer)
            return true
        }
    }
}
