package com.flyerpix.editor.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.LayerPreviewRenderer
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
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
    private val onCheckedChange: ((layer: CanvasLayer, isChecked: Boolean) -> Unit)? = null,
    private val backgroundProvider: (() -> CanvasBackground)? = null,
    private val backgroundVisibilityProvider: (() -> Boolean)? = null,
    private val onToggleBackgroundVisibility: (() -> Unit)? = null,
    private val onEditBackground: (() -> Unit)? = null
) : RecyclerView.Adapter<AuthenticLayerAdapter.LayerViewHolder>() {

    private val displayedLayers = mutableListOf<CanvasLayer>()
    private var currentSelectedLayer: CanvasLayer? = null
    private var displayedBackground: CanvasBackground? = null

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
        displayedBackground = backgroundProvider?.invoke()
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
        if (backgroundProvider != null && position == displayedLayers.size) holder.bindBackground(displayedBackground)
        else holder.bind(displayedLayers[position])
    }

    override fun getItemCount(): Int = displayedLayers.size + if (backgroundProvider != null) 1 else 0

    inner class LayerViewHolder(private val binding: ItemLayerAuthenticBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindBackground(background: CanvasBackground?) {
            val isVisible = backgroundVisible(background)
            binding.ivDragHandle.visibility = View.INVISIBLE
            binding.ivLayerPreview.visibility = View.VISIBLE
            binding.tvLayerTitle.visibility = View.INVISIBLE
            val backgroundPreviewParams = binding.ivLayerPreview.layoutParams
            val thumbnailSize = (56f * binding.root.resources.displayMetrics.density).toInt()
            backgroundPreviewParams.width = thumbnailSize
            backgroundPreviewParams.height = thumbnailSize
            if (backgroundPreviewParams is android.widget.LinearLayout.LayoutParams) {
                backgroundPreviewParams.weight = 0f
            }
            binding.ivLayerPreview.layoutParams = backgroundPreviewParams
            binding.ivLayerPreview.setPadding(0, 0, 0, 0)
            binding.layoutLayerBatchCheckbox.visibility = View.GONE
            binding.layoutLayerActionsGrid.visibility = View.VISIBLE
            binding.viewLayerBottomHighlight.visibility = View.GONE
            binding.cardLayerItem.setCardBackgroundColor(Color.parseColor("#F5F7FA"))
            binding.cardLayerItem.strokeColor = Color.parseColor("#B0BEC5")
            binding.ivLayerPreview.setColorFilter(null)
            if (background?.mode == CanvasBackgroundMode.IMAGE && background.imageBitmap != null) {
                binding.ivLayerPreview.setImageBitmap(background.imageBitmap)
                binding.ivLayerPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                binding.ivLayerPreview.setImageResource(R.drawable.ic_background_24px)
                binding.ivLayerPreview.setColorFilter(Color.parseColor("#546E7A"))
            }
            binding.btnLayerLock.setImageResource(R.drawable.ic_lock_24px)
            binding.btnLayerLock.setColorFilter(Color.parseColor("#546E7A"))
            binding.btnLayerLock.setOnClickListener { }
            binding.btnLayerEdit.setImageResource(R.drawable.ic_background_24px)
            binding.btnLayerEdit.setColorFilter(Color.parseColor("#546E7A"))
            binding.btnLayerEdit.setOnClickListener { onEditBackground?.invoke() }
            binding.btnLayerVisibility.setImageResource(
                if (isVisible) R.drawable.ic_visibility_24px else R.drawable.ic_visibility_off_24px
            )
            binding.btnLayerVisibility.setColorFilter(
                if (isVisible) Color.parseColor("#1769FF") else Color.parseColor("#9E9E9E")
            )
            binding.btnLayerVisibility.setOnClickListener { onToggleBackgroundVisibility?.invoke() }
            binding.btnLayerDelete.visibility = View.INVISIBLE
            binding.root.setOnClickListener { onEditBackground?.invoke() }
            binding.ivDragHandle.setOnTouchListener(null)
        }

        private fun backgroundVisible(background: CanvasBackground?): Boolean {
            return if (background == null) true else backgroundVisibilityProvider?.invoke() ?: true
        }

        private fun backgroundTitle(background: CanvasBackground?): String {
            return when (background?.mode) {
                CanvasBackgroundMode.IMAGE -> "Background · Photo"
                CanvasBackgroundMode.GRADIENT -> "Background · Gradient"
                CanvasBackgroundMode.TRANSPARENT -> "Background · Transparent"
                else -> "Background · Solid"
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(layer: CanvasLayer) {
            binding.ivDragHandle.visibility = View.VISIBLE
            binding.btnLayerDelete.visibility = View.VISIBLE
            binding.ivLayerPreview.visibility = View.VISIBLE
            binding.tvLayerTitle.visibility = View.VISIBLE
            binding.tvLayerTitle.textSize = 12f
            binding.tvLayerTitle.maxLines = 1
            resetPreviewWidth()
            binding.ivLayerPreview.scaleType = ImageView.ScaleType.FIT_CENTER
            val isSelected = (layer == currentSelectedLayer)
            val isChecked = (layer.id in checkedLayerIds)

            // 0. Thumbnail pratinjau objek lapisan
            val preview = when (layer) {
                is ImageLayer -> layer.bitmap.takeUnless { it.isRecycled }
                is TextLayer -> renderTextThumbnail(layer)
                else -> previewCache[layer.id]
                    ?: LayerPreviewRenderer.render(layer)?.also { previewCache[layer.id] = it }
            }
            if (preview != null) {
                binding.ivLayerPreview.clearColorFilter()
                binding.ivLayerPreview.setBackgroundColor(Color.TRANSPARENT)
                binding.ivLayerPreview.setPadding(2, 2, 2, 2)
                binding.ivLayerPreview.setImageBitmap(preview)
            } else {
                binding.ivLayerPreview.setImageResource(R.drawable.ic_edit_box_24px)
                binding.ivLayerPreview.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"))
            }

            // 1. Judul Layer
            binding.tvLayerTitle.text = when (layer) {
                is TextLayer -> if (layer.text.isNotBlank()) layer.text else "Empty Text"
                is ImageLayer -> ""
                is ShapeLayer -> "Shape (${layer.shapeType.name.lowercase().replace('_', ' ')})"
                is StickerLayer -> "Sticker"
                else -> "Layer"
            }

            when (layer) {
                is TextLayer -> {
                    binding.ivLayerPreview.visibility = View.GONE
                    binding.tvLayerTitle.visibility = View.VISIBLE
                    binding.tvLayerTitle.textSize = 10f
                    binding.tvLayerTitle.maxLines = 1
                    binding.tvLayerTitle.setTypeface(Typeface.DEFAULT)
                }
                is ImageLayer -> {
                    binding.ivLayerPreview.visibility = View.VISIBLE
                    binding.ivLayerPreview.setPadding(0, 0, 0, 0)
                    binding.ivLayerPreview.setBackgroundColor(Color.TRANSPARENT)
                    binding.tvLayerTitle.visibility = View.INVISIBLE
                    binding.ivLayerPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                    val params = binding.ivLayerPreview.layoutParams
                    params.width = 0
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    if (params is android.widget.LinearLayout.LayoutParams) {
                        params.weight = 1f
                    }
                    binding.ivLayerPreview.layoutParams = params
                }
                else -> {
                    binding.ivLayerPreview.visibility = View.VISIBLE
                    binding.tvLayerTitle.visibility = View.VISIBLE
                    binding.tvLayerTitle.setTypeface(Typeface.DEFAULT)
                }
            }

            if (layer is ImageLayer) {
                binding.tvLayerTitle.visibility = View.INVISIBLE
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

        private fun resetPreviewWidth() {
            val params = binding.ivLayerPreview.layoutParams
            params.width = 42
            params.height = 42
            if (params is android.widget.LinearLayout.LayoutParams) {
                params.weight = 0f
            }
            binding.ivLayerPreview.layoutParams = params
            binding.ivLayerPreview.setBackgroundColor(Color.TRANSPARENT)
            binding.ivLayerPreview.setPadding(2, 2, 2, 2)
        }

        private fun renderTextThumbnail(layer: TextLayer): Bitmap {
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = layer.typeface ?: Typeface.DEFAULT_BOLD
                textSize = 18f
                color = readableThumbnailColor(layer.textColor)
                setShadowLayer(2f, 0f, 1f, Color.argb(100, 0, 0, 0))
            }
            val lines = layer.text.ifBlank { "Empty Text" }.split("\n").take(3)
            val lineHeight = paint.textSize * 1.15f
            val firstBaseline = 48f - (lines.size - 1) * lineHeight / 2f
            lines.forEachIndexed { index, line ->
                canvas.drawText(line.take(18), 48f, firstBaseline + index * lineHeight, paint)
            }
            return bitmap
        }

        private fun readableThumbnailColor(color: Int): Int {
            val luminance = (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color))
            return if (Color.alpha(color) < 100 || luminance > 185f) Color.rgb(25, 40, 55) else color
        }
    }
}
