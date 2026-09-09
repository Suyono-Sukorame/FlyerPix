package com.flyerpix.editor.ui.controller

import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.RadioButton
import android.widget.SeekBar
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.ui.EditorActivity

/**
 * Controller untuk Shape Settings Panel
 * Menangani tampilan dan interaksi panel pengaturan Shape
 */
class ShapePanelController(
    private val activity: EditorActivity,
    private val binding: ActivityEditorBinding,
    private val canvas: PixelCanvasView,
    private val showSnackbar: (String) -> Unit
) {

    private var currentShape: ShapeLayer? = null
    private var snapshotShape: ShapeLayer? = null // Backup untuk Cancel

    init {
        setupShapePanel()
    }

    private fun setupShapePanel() {
        val panelRoot = binding.shapeSettingsPanel.root

        // Apply Button (header DetailPanel)
        (panelRoot as? com.flyerpix.editor.ui.view.DetailPanel)?.applyButton?.setOnClickListener {
            applyShapeChanges()
        }

        // Cancel Button (header DetailPanel)
        (panelRoot as? com.flyerpix.editor.ui.view.DetailPanel)?.cancelButton?.setOnClickListener {
            cancelShapeChanges()
        }

        // Corner Radius Slider
        setupSlider(
            panelRoot.findViewById(R.id.shapeCornerRadiusControl),
            R.id.sliderLabelCorner, R.id.sliderValueCorner, R.id.sliderCorner,
            "Corner Radius",
            0f, 100f, 20f,
            onValueChange = { value ->
                currentShape?.let {
                    it.cornerRadiusX = value
                    it.cornerRadiusY = value
                    canvas.invalidate()
                }
            }
        )

        // Opacity Slider
        setupSlider(
            panelRoot.findViewById(R.id.shapeOpacityControl),
            R.id.sliderLabelOpacity, R.id.sliderValueOpacity, R.id.sliderOpacity,
            "Opacity",
            0f, 100f, 100f,
            onValueChange = { value ->
                currentShape?.let {
                    it.opacity = (value / 100f * 255f).toInt()
                    canvas.invalidate()
                }
            }
        )

        // Fill Color Button
        panelRoot.findViewById<View>(R.id.btnShapeFillColor).setOnClickListener {
            currentShape?.let { shape ->
                val colors = listOf(android.graphics.Color.WHITE, android.graphics.Color.RED, 
                    android.graphics.Color.GREEN, android.graphics.Color.BLUE, android.graphics.Color.YELLOW)
                val currentIndex = colors.indexOf(shape.fillColor)
                shape.fillColor = colors[(currentIndex + 1) % colors.size]
                updateColorPreview(panelRoot.findViewById(R.id.shapeColorPreview), shape.fillColor)
                canvas.invalidate()
            }
        }

        // Fill Color Preview Click
        panelRoot.findViewById<View>(R.id.shapeColorPreview).setOnClickListener {
            panelRoot.findViewById<View>(R.id.btnShapeFillColor).performClick()
        }

        // Stroke Width Slider
        setupSlider(
            panelRoot.findViewById(R.id.shapeStrokeWidthControl),
            R.id.sliderLabelStroke, R.id.sliderValueStroke, R.id.sliderStroke,
            "Stroke Width",
            0f, 50f, 0f,
            onValueChange = { value ->
                currentShape?.let {
                    it.strokeWidth = value
                    canvas.invalidate()
                }
            }
        )

        // Stroke Opacity Slider
        setupSlider(
            panelRoot.findViewById(R.id.shapeStrokeOpacityControl),
            R.id.sliderLabelStrokeOpacity, R.id.sliderValueStrokeOpacity, R.id.sliderStrokeOpacity,
            "Stroke Opacity",
            0f, 100f, 100f,
            onValueChange = { value ->
                currentShape?.let {
                    it.strokeOpacity = (value / 100f * 255f).toInt()
                    canvas.invalidate()
                }
            }
        )

        // Stroke Color Button
        panelRoot.findViewById<View>(R.id.btnShapeStrokeColor).setOnClickListener {
            currentShape?.let { shape ->
                val colors = listOf(android.graphics.Color.BLACK, android.graphics.Color.RED,
                    android.graphics.Color.GREEN, android.graphics.Color.BLUE, android.graphics.Color.WHITE)
                val currentIndex = colors.indexOf(shape.strokeColor)
                shape.strokeColor = colors[(currentIndex + 1) % colors.size]
                updateColorPreview(panelRoot.findViewById(R.id.shapeStrokeColorPreview), shape.strokeColor)
                canvas.invalidate()
            }
        }

        // Stroke Color Preview Click
        panelRoot.findViewById<View>(R.id.shapeStrokeColorPreview).setOnClickListener {
            panelRoot.findViewById<View>(R.id.btnShapeStrokeColor).performClick()
        }

        // Join Style Radio Buttons
        panelRoot.findViewById<android.widget.RadioGroup>(R.id.rgShapeJoinStyle).setOnCheckedChangeListener { _, checkedId ->
            currentShape?.let { shape ->
                shape.strokeJoin = when (checkedId) {
                    R.id.rbJoinMiter -> Paint.Join.MITER
                    R.id.rbJoinBevel -> Paint.Join.BEVEL
                    R.id.rbJoinRound -> Paint.Join.ROUND
                    else -> Paint.Join.MITER
                }
                canvas.invalidate()
            }
        }

        // Blur Radius Slider
        setupSlider(
            panelRoot.findViewById(R.id.shapeBlurRadiusControl),
            R.id.sliderLabelBlur, R.id.sliderValueBlur, R.id.sliderBlur,
            "Blur Radius",
            0f, 50f, 0f,
            onValueChange = { value ->
                currentShape?.let {
                    it.shadowRadius = value
                    it.shadowEnabled = value > 0f
                    canvas.invalidate()
                }
            }
        )
    }

    /**
     * Tampilkan Shape Settings Panel untuk shape yang dipilih
     */
    fun showShapeSettings(shape: ShapeLayer) {
        currentShape = shape
        snapshotShape = shape.copy() // Backup untuk Cancel

        // Tinggi panel dihitung dari ruang kosong di bawah canvas agar objek di
        // kanvas tetap terlihat (aturan PanelHeightManager: 60% canvas / cap 50%
        // layar), anchor bawah 56dp di atas bottom nav.
        val density = PanelHeightManager.densityOf(activity.resources)
        val panel = binding.shapeSettingsPanel.root
        PanelHeightManager.applyCanvasAwareHeight(
            panel = panel,
            root = binding.parentLayout,
            canvasCard = binding.canvasCard,
            bottomMarginPx = (56 * density).toInt(),
            screenHeightPx = PanelHeightManager.screenHeightPx(activity.resources),
            density = density,
        )

        // Update UI dengan nilai shape saat ini
        updateUIFromShape(shape)

        // Show panel, menggantikan halaman Objek agar tidak berhimpit
        binding.shapeSettingsPanel.root.visibility = View.VISIBLE
        binding.objectMenuPanel.visibility = View.GONE
        binding.effectSettingsInclude.root.visibility = View.GONE

        // Hide other panels
        hideOtherPanels()
    }

    /**
     * Sembunyikan Shape Settings Panel
     */
    fun hideShapeSettings() {
        binding.shapeSettingsPanel.root.visibility = View.GONE
        // Kembalikan halaman Objek hanya jika masih berada di page Objek
        if (binding.bottomNavigation.selectedItemId == R.id.nav_object) {
            binding.objectMenuPanel.visibility = View.VISIBLE
        }
        currentShape = null
        snapshotShape = null
    }

    private fun updateUIFromShape(shape: ShapeLayer) {
        val panelRoot = binding.shapeSettingsPanel.root
        
        // Corner Radius
        setSliderValue(panelRoot.findViewById(R.id.shapeCornerRadiusControl), R.id.sliderValueCorner, R.id.sliderCorner, shape.cornerRadiusX)

        // Opacity
        setSliderValue(panelRoot.findViewById(R.id.shapeOpacityControl), R.id.sliderValueOpacity, R.id.sliderOpacity, shape.opacity / 255f * 100f)

        // Fill Color
        updateColorPreview(panelRoot.findViewById(R.id.shapeColorPreview), shape.fillColor)

        // Stroke Width
        setSliderValue(panelRoot.findViewById(R.id.shapeStrokeWidthControl), R.id.sliderValueStroke, R.id.sliderStroke, shape.strokeWidth)

        // Stroke Opacity
        setSliderValue(panelRoot.findViewById(R.id.shapeStrokeOpacityControl), R.id.sliderValueStrokeOpacity, R.id.sliderStrokeOpacity, shape.strokeOpacity / 255f * 100f)

        // Stroke Color
        updateColorPreview(panelRoot.findViewById(R.id.shapeStrokeColorPreview), shape.strokeColor)

        // Join Style
        val joinRadioId = when (shape.strokeJoin) {
            Paint.Join.MITER -> R.id.rbJoinMiter
            Paint.Join.BEVEL -> R.id.rbJoinBevel
            Paint.Join.ROUND -> R.id.rbJoinRound
            else -> R.id.rbJoinMiter
        }
        panelRoot.findViewById<android.widget.RadioGroup>(R.id.rgShapeJoinStyle).check(joinRadioId)

        // Blur Radius
        setSliderValue(panelRoot.findViewById(R.id.shapeBlurRadiusControl), R.id.sliderValueBlur, R.id.sliderBlur, shape.shadowRadius)
    }

    private fun applyShapeChanges() {
        // Changes already applied in real-time, just hide panel
        hideShapeSettings()
        showSnackbar("Shape updated")
    }

    private fun cancelShapeChanges() {
        // Restore dari snapshot
        snapshotShape?.let { snapshot ->
            currentShape?.let { shape ->
                shape.cornerRadiusX = snapshot.cornerRadiusX
                shape.cornerRadiusY = snapshot.cornerRadiusY
                shape.opacity = snapshot.opacity
                shape.fillColor = snapshot.fillColor
                shape.strokeWidth = snapshot.strokeWidth
                shape.strokeOpacity = snapshot.strokeOpacity
                shape.strokeColor = snapshot.strokeColor
                shape.strokeJoin = snapshot.strokeJoin
                shape.shadowRadius = snapshot.shadowRadius
                shape.shadowEnabled = snapshot.shadowEnabled
                canvas.invalidate()
            }
        }
        hideShapeSettings()
        showSnackbar("Changes cancelled")
    }

    private fun hideOtherPanels() {
        binding.effectSettingsInclude.root.visibility = View.GONE
        // Add other panels to hide if needed
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupSlider(
        controlView: View,
        labelId: Int,
        valueId: Int,
        seekBarId: Int,
        label: String,
        min: Float,
        max: Float,
        initial: Float,
        onValueChange: (Float) -> Unit
    ) {
        val labelTextView = controlView.findViewById<android.widget.TextView>(labelId)
        val valueTextView = controlView.findViewById<android.widget.TextView>(valueId)
        val seekBar = controlView.findViewById<SeekBar>(seekBarId)

        labelTextView.text = label
        seekBar.max = ((max - min) * 10).toInt() // 0.1 precision
        seekBar.progress = ((initial - min) * 10).toInt()
        valueTextView.text = String.format("%.1f", initial)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = min + (progress / 10f)
                valueTextView.text = String.format("%.1f", value)
                if (fromUser) {
                    onValueChange(value)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setSliderValue(controlView: View, valueId: Int, seekBarId: Int, value: Float) {
        val seekBar = controlView.findViewById<SeekBar>(seekBarId)
        val valueTextView = controlView.findViewById<android.widget.TextView>(valueId)
        
        // Assume slider was set up with specific min/max, we need to calculate progress
        // For simplicity, we'll just set the text and progress based on common ranges
        valueTextView.text = String.format("%.1f", value)
        seekBar.progress = (value * 10).toInt()
    }

    private fun updateColorPreview(cardView: View, color: Int) {
        // Find inner view in MaterialCardView
        val innerView = if (cardView is com.google.android.material.card.MaterialCardView) {
            cardView.getChildAt(0)
        } else {
            cardView
        }
        innerView?.setBackgroundColor(color)
    }
}
