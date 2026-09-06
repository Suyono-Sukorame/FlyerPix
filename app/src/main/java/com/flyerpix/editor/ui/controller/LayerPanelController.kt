package com.flyerpix.editor.ui.controller

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.ui.adapter.AuthenticLayerAdapter

class LayerPanelController(
    private val binding: ActivityEditorBinding,
    private val canvas: PixelCanvasView,
    private val showSnackbar: (String) -> Unit,
    private val showEditTextDialog: (TextLayer) -> Unit
) {
    var isOpen = false
        private set

    private var isBatchMode = false
    private lateinit var adapter: AuthenticLayerAdapter
    private var itemTouchHelper: ItemTouchHelper? = null

    fun initialize() {
        adapter = AuthenticLayerAdapter(
            onLayerSelected = { layer ->
                canvas.selectedLayer = layer
                canvas.invalidate()
                adapter.submitLayers(canvas.layers, canvas.selectedLayer)
            },
            onToggleVisibility = { layer ->
                canvas.runRecordedAction(if (layer.isVisible) "Sembunyikan Layer" else "Tampilkan Layer") {
                    layer.isVisible = !layer.isVisible
                }
                canvas.invalidate()
                adapter.submitLayers(canvas.layers, canvas.selectedLayer)
            },
            onToggleLock = { layer ->
                canvas.runRecordedAction(if (layer.isLocked) "Buka Kunci Layer" else "Kunci Layer") {
                    layer.isLocked = !layer.isLocked
                    if (layer.isLocked && canvas.selectedLayer == layer) canvas.selectedLayer = null
                }
                canvas.invalidate()
                adapter.submitLayers(canvas.layers, canvas.selectedLayer)
            },
            onEditLayer = { layer ->
                if (layer is TextLayer) showEditTextDialog(layer)
                else {
                    canvas.selectedLayer = layer
                    canvas.invalidate()
                    showSnackbar("Lapisan dipilih")
                }
            },
            onDeleteLayer = { layer ->
                canvas.removeLayer(layer)
                adapter.submitLayers(canvas.layers, canvas.selectedLayer)
                showSnackbar("Lapisan dihapus")
            },
            onStartDrag = { vh -> itemTouchHelper?.startDrag(vh) },
            onCheckedChange = { _, _ -> }
        )

        binding.rvAuthenticLayers.apply {
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = this@LayerPanelController.adapter
        }

        setupDragReorder()
        setupFooterButtons()
        setupCanvasListeners()
    }

    private fun setupDragReorder() {
        var dragSnapshot: com.flyerpix.editor.canvas.history.CanvasStateSnapshot? = null
        var hasMoved = false

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    dragSnapshot = canvas.captureCurrentState("Ubah Urutan Lapisan")
                    hasMoved = false
                }
            }

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.moveItem(vh.adapterPosition, target.adapterPosition)
                hasMoved = true
                val newOrder = adapter.getItems().asReversed()
                canvas.layers.clear()
                canvas.layers.addAll(newOrder)
                canvas.invalidate()
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                val snap = dragSnapshot
                if (hasMoved && snap != null) {
                    canvas.recordAction("Ubah Urutan Lapisan", snap)
                    dragSnapshot = null
                    hasMoved = false
                }
            }
        }
        itemTouchHelper = ItemTouchHelper(callback).apply { attachToRecyclerView(binding.rvAuthenticLayers) }
    }

    private fun setupFooterButtons() {
        binding.viewLayerOverlayOutside.setOnClickListener { close() }
        binding.btnLayerToggleBatch.setOnClickListener { setBatchMode(true) }
        binding.btnLayerMoveNormal.setOnClickListener { showSnackbar("Fitur geser lapisan: segera hadir") }

        binding.btnLayerToFront.setOnClickListener {
            if (!canvas.bringSelectedLayerToFront()) showSnackbar("Pilih lapisan yang tidak terkunci terlebih dahulu")
        }
        binding.btnLayerToBack.setOnClickListener {
            if (!canvas.sendSelectedLayerToBack()) showSnackbar("Pilih lapisan yang tidak terkunci terlebih dahulu")
        }

        binding.btnBatchDelete.setOnClickListener {
            val checked = adapter.getCheckedLayers()
            if (checked.isEmpty()) { showSnackbar("Pilih lapisan yang ingin dihapus terlebih dahulu"); return@setOnClickListener }
            val snap = canvas.captureCurrentState("Hapus Lapisan Terpilih")
            checked.forEach { canvas.layers.remove(it) }
            if (canvas.selectedLayer in checked) canvas.selectedLayer = canvas.layers.lastOrNull()
            canvas.recordAction("Hapus Lapisan Terpilih", snap)
            canvas.invalidate()
            setBatchMode(false)
            adapter.submitLayers(canvas.layers, canvas.selectedLayer)
            showSnackbar("${checked.size} lapisan dihapus")
        }
        binding.btnBatchEdit.setOnClickListener {
            val count = adapter.checkedCount()
            if (count == 0) showSnackbar("Pilih lapisan terlebih dahulu")
            else showSnackbar("Fitur atribut massal: segera hadir ($count lapisan dipilih)")
        }
        binding.btnBatchMerge.setOnClickListener {
            if (adapter.checkedCount() < 2) showSnackbar("Pilih minimal 2 lapisan untuk digabungkan")
            else showSnackbar("Fitur Gabung Lapisan: akan diimplementasikan berikutnya")
        }
        binding.btnBatchDone.setOnClickListener { setBatchMode(false) }
        binding.btnBatchMove.setOnClickListener { showSnackbar("Fitur geser bersama: segera hadir") }
    }

    private fun setupCanvasListeners() {
        val prevSelected = canvas.onLayerSelectedListener
        canvas.onLayerSelectedListener = { layer ->
            prevSelected?.invoke(layer)
            updateFooterState()
            if (isOpen) adapter.submitLayers(canvas.layers, canvas.selectedLayer)
        }
        val prevChanged = canvas.onLayersChangedListener
        canvas.onLayersChangedListener = {
            prevChanged?.invoke()
            updateFooterState()
            if (isOpen) adapter.submitLayers(canvas.layers, canvas.selectedLayer)
        }
    }

    private fun updateFooterState() {
        val layer = canvas.selectedLayer
        binding.btnLayerToFront.isEnabled = layer != null && !layer.isLocked && canvas.canBringSelectedLayerToFront()
        binding.btnLayerToBack.isEnabled = layer != null && !layer.isLocked && canvas.canSendSelectedLayerToBack()
    }

    fun setBatchMode(enabled: Boolean) {
        isBatchMode = enabled
        adapter.setBatchMode(enabled)
        binding.layoutLayerFooterNormal.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.layoutLayerFooterBatch.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    fun toggle() { if (isOpen) close() else open() }

    fun refresh() {
        updateFooterState()
        if (isOpen) adapter.submitLayers(canvas.layers, canvas.selectedLayer)
    }

    fun open() {
        isOpen = true
        binding.topBarInclude.btnTopLayers.setBackgroundResource(R.drawable.bg_circle_layer_active)
        updateFooterState()
        adapter.submitLayers(canvas.layers, canvas.selectedLayer)
        val overlay = binding.layoutAuthenticLayerOverlay
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(200).start()
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        binding.topBarInclude.btnTopLayers.setBackgroundResource(android.R.color.transparent)
        if (isBatchMode) setBatchMode(false)
        val overlay = binding.layoutAuthenticLayerOverlay
        overlay.animate().alpha(0f).setDuration(150).withEndAction { overlay.visibility = View.GONE }.start()
    }
}
