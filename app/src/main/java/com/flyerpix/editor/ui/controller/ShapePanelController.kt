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
    private val canvas: PixelCanvasView
) {

    private var currentShape: ShapeLayer? = null
    private var snapshotShape: ShapeLayer? = null // Backup untuk Cancel

    init {
        setupShapePanel()
    }

    private fun setupShapePanel() {
        val panelRoot = binding.shapeSettingsPanel
        
        // Apply Button
        panelRoot.findViewById<View>(R.id.btnApplyShape).setOnClickListener {
            applyShapeChanges()
        }

        // Cancel Button
        panelRoot.findViewById<View>(R.id.btnCancelShape).setOnClickListener {
            cancelShapeChanges()
        }

        // Corner Radius Slider
        setupSlider(
            panelRoot.findViewById(R.id.shapeCornerRadiusControl),
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

        // Update UI dengan nilai shape saat ini
        updateUIFromShape(shape)

        // Show panel
        binding.shapeSettingsPanel.visibility = View.VISIBLE

        // Hide other panels
        hideOtherPanels()
    }

    /**
     * Sembunyikan Shape Settings Panel
     */
    fun hideShapeSettings() {
        binding.shapeSettingsPanel.visibility = View.GONE
        currentShape = null
        snapshotShape = null
    }

    private fun updateUIFromShape(shape: ShapeLayer) {
        val panelRoot = binding.shapeSettingsPanel
        
        // Corner Radius
        setSliderValue(panelRoot.findViewById(R.id.shapeCornerRadiusControl), shape.cornerRadiusX)

        // Opacity
        setSliderValue(panelRoot.findViewById(R.id.shapeOpacityControl), shape.opacity / 255f * 100f)

        // Fill Color
        updateColorPreview(panelRoot.findViewById(R.id.shapeColorPreview), shape.fillColor)

        // Stroke Width
        setSliderValue(panelRoot.findViewById(R.id.shapeStrokeWidthControl), shape.strokeWidth)

        // Stroke Opacity
        setSliderValue(panelRoot.findViewById(R.id.shapeStrokeOpacityControl), shape.strokeOpacity / 255f * 100f)

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
        setSliderValue(panelRoot.findViewById(R.id.shapeBlurRadiusControl), shape.shadowRadius)
    }

    private fun applyShapeChanges() {
        // Changes already applied in real-time, just hide panel
        hideShapeSettings()
        activity.showSnackbar("Shape updated")
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
        activity.showSnackbar("Changes cancelled")
    }

    private fun hideOtherPanels() {
        binding.effectSettingsPanel.visibility = View.GONE
        // Add other panels to hide if needed
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupSlider(
        controlView: View,
        label: String,
        min: Float,
        max: Float,
        initial: Float,
        onValueChange: (Float) -> Unit
    ) {
        val labelTextView = controlView.findViewById<android.widget.TextView>(R.id.sliderLabel)
        val valueTextView = controlView.findViewById<android.widget.TextView>(R.id.sliderValue)
        val seekBar = controlView.findViewById<SeekBar>(R.id.slider)

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

    private fun setSliderValue(controlView: View, value: Float) {
        val seekBar = controlView.findViewById<SeekBar>(R.id.slider)
        val valueTextView = controlView.findViewById<android.widget.TextView>(R.id.sliderValue)
        
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
