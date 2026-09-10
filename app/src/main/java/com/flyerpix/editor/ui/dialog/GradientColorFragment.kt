package com.flyerpix.editor.ui.dialog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import kotlin.math.roundToInt

/**
 * Tab Gradient Color — gradasi modern:
 *  - Preview bar gradasi.
 *  - 2 titik warna (start/end) — ketuk membuka editor inline (bukan dialog bertumpuk).
 *  - Tipe gradasi (Linear/Radial/Sweep) + slider sudut + preset populer.
 */
class GradientColorFragment : Fragment() {

    private var colorStart: Int = 0xFFFF512F.toInt()
    private var colorEnd: Int = 0xFFDD2476.toInt()
    private var gradientType: GradientType = GradientType.LINEAR
    private var angle: Float = 0f
    private var editingStart = true

    private lateinit var preview: View
    private lateinit var colorStartView: View
    private lateinit var colorEndView: View
    private lateinit var rgType: RadioGroup
    private lateinit var layoutAngle: View
    private lateinit var tvAngleLabel: TextView
    private lateinit var sliderAngle: Slider
    private lateinit var gradientContent: View
    private lateinit var stopEditorSection: View
    private lateinit var inlineStopEditor: SolidColorEditorView
    private lateinit var tvStopEditorTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_gradient_color, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initGrad = @Suppress("DEPRECATION") (arguments?.getSerializable(ARG_GRADIENT) as? GradientColor)
        if (initGrad != null && initGrad.colors.size >= 2) {
            colorStart = initGrad.colors[0]
            colorEnd = initGrad.colors[initGrad.colors.size - 1]
            gradientType = initGrad.type
            angle = initGrad.angle
        }

        preview = view.findViewById(R.id.viewGradientPreview)
        colorStartView = view.findViewById(R.id.viewColorStart)
        colorEndView = view.findViewById(R.id.viewColorEnd)
        rgType = view.findViewById(R.id.rgGradientType)
        layoutAngle = view.findViewById(R.id.layoutAngle)
        tvAngleLabel = view.findViewById(R.id.tvAngleLabel)
        sliderAngle = view.findViewById(R.id.sliderAngle)
        gradientContent = view.findViewById(R.id.gradientContent)
        stopEditorSection = view.findViewById(R.id.stopEditorSection)
        inlineStopEditor = view.findViewById(R.id.inlineStopEditor)
        tvStopEditorTitle = view.findViewById(R.id.tvStopEditorTitle)
        val rvPresets = view.findViewById<RecyclerView>(R.id.rvGradientPresets)

        fun updatePreview() {
            val gd = GradientDrawable().apply {
                colors = intArrayOf(colorStart, colorEnd)
                gradientType = when (this@GradientColorFragment.gradientType) {
                    GradientType.LINEAR -> GradientDrawable.LINEAR_GRADIENT
                    GradientType.RADIAL -> GradientDrawable.RADIAL_GRADIENT
                    GradientType.SWEEP -> GradientDrawable.SWEEP_GRADIENT
                }
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = 12f
            }
            preview.background = gd
        }

        fun swatchDrawable(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(2, 0x22888888)
        }

        fun updateColorSwatches() {
            colorStartView.background = swatchDrawable(colorStart)
            colorEndView.background = swatchDrawable(colorEnd)
        }

        fun closeStopEditor() {
            stopEditorSection.visibility = View.GONE
            gradientContent.visibility = View.VISIBLE
        }

        fun openStopEditor(isStart: Boolean) {
            editingStart = isStart
            tvStopEditorTitle.text = if (isStart) "Start Color" else "End Color"
            inlineStopEditor.setInitialColor(if (isStart) colorStart else colorEnd)
            gradientContent.visibility = View.GONE
            stopEditorSection.visibility = View.VISIBLE
        }

        inlineStopEditor.setOnColorChanged { }
        view.findViewById<View>(R.id.btnStopEditorSet).setOnClickListener {
            val picked = inlineStopEditor.getSelectedColor()
            inlineStopEditor.commitCurrentColorToRecents()
            if (editingStart) colorStart = picked else colorEnd = picked
            updateColorSwatches()
            updatePreview()
            closeStopEditor()
        }
        view.findViewById<View>(R.id.btnStopEditorCancel).setOnClickListener { closeStopEditor() }
        view.findViewById<View>(R.id.btnStopEditorBack).setOnClickListener { closeStopEditor() }

        colorStartView.setOnClickListener { openStopEditor(true) }
        colorEndView.setOnClickListener { openStopEditor(false) }

        rgType.setOnCheckedChangeListener { _, checkedId ->
            gradientType = when (checkedId) {
                R.id.rbRadial -> GradientType.RADIAL
                R.id.rbSweep -> GradientType.SWEEP
                else -> GradientType.LINEAR
            }
            layoutAngle.visibility = if (gradientType == GradientType.LINEAR) View.VISIBLE else View.GONE
            updatePreview()
        }

        sliderAngle.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                angle = value
                tvAngleLabel.text = "Angle: ${value.roundToInt()}°"
                updatePreview()
            }
        }

        val presetAdapter = GradientPresetAdapter { preset ->
            colorStart = preset.colors[0]
            colorEnd = preset.colors[preset.colors.size - 1]
            gradientType = preset.type
            angle = preset.angle
            rgType.check(
                when (gradientType) {
                    GradientType.LINEAR -> R.id.rbLinear
                    GradientType.RADIAL -> R.id.rbRadial
                    GradientType.SWEEP -> R.id.rbSweep
                }
            )
            layoutAngle.visibility = if (gradientType == GradientType.LINEAR) View.VISIBLE else View.GONE
            sliderAngle.value = angle
            tvAngleLabel.text = "Angle: ${angle.roundToInt()}°"
            updateColorSwatches()
            updatePreview()
        }
        rvPresets.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvPresets.adapter = presetAdapter

        rgType.check(
            when (gradientType) {
                GradientType.LINEAR -> R.id.rbLinear
                GradientType.RADIAL -> R.id.rbRadial
                GradientType.SWEEP -> R.id.rbSweep
            }
        )
        layoutAngle.visibility = if (gradientType == GradientType.LINEAR) View.VISIBLE else View.GONE
        sliderAngle.value = angle
        tvAngleLabel.text = "Angle: ${angle.roundToInt()}°"
        updateColorSwatches()
        updatePreview()
    }

    fun getGradient(): GradientColor {
        return GradientColor(
            colors = intArrayOf(colorStart, colorEnd),
            type = gradientType,
            angle = angle,
            name = "Custom"
        )
    }

    private class GradientPresetAdapter(
        private val onPresetSelected: (GradientColor) -> Unit
    ) : RecyclerView.Adapter<GradientPresetAdapter.VH>() {

        class VH(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = View(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val pad = (8 * parent.context.resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val preset = GradientColor.PRESETS[position]
            val gd = GradientDrawable().apply {
                colors = preset.colors
                cornerRadius = 16f
                gradientType = when (preset.type) {
                    GradientType.LINEAR -> GradientDrawable.LINEAR_GRADIENT
                    GradientType.RADIAL -> GradientDrawable.RADIAL_GRADIENT
                    GradientType.SWEEP -> GradientDrawable.SWEEP_GRADIENT
                }
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
            }
            holder.view.background = gd
            holder.view.setOnClickListener { onPresetSelected(preset) }
        }

        override fun getItemCount() = GradientColor.PRESETS.size
    }

    companion object {
        private const val ARG_GRADIENT = "initial_gradient"

        fun newInstance(initialGradient: GradientColor?): GradientColorFragment {
            return GradientColorFragment().apply {
                arguments = Bundle().apply {
                    @Suppress("DEPRECATION")
                    putSerializable(ARG_GRADIENT, initialGradient)
                }
            }
        }
    }
}