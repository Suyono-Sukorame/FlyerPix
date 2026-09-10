package com.flyerpix.editor.ui.dialog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
        var presetAdapter: GradientPresetAdapter? = null

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
            presetAdapter?.updateSelection(matchingPresetIndex())
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
            presetAdapter?.updateSelection(matchingPresetIndex())
        }

        sliderAngle.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                angle = value
                tvAngleLabel.text = "Angle: ${value.roundToInt()}°"
                updatePreview()
                presetAdapter?.updateSelection(matchingPresetIndex())
            }
        }

        presetAdapter = GradientPresetAdapter { preset ->
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
        presetAdapter?.updateSelection(matchingPresetIndex())
    }

    fun getGradient(): GradientColor {
        return GradientColor(
            colors = intArrayOf(colorStart, colorEnd),
            type = gradientType,
            angle = angle,
            name = "Custom"
        )
    }

    /** Indeks preset yang cocok persis dengan state gradasi saat ini, -1 jika tak ada. */
    private fun matchingPresetIndex(): Int =
        GradientColor.PRESETS.indexOfFirst { preset ->
            preset.colors.isNotEmpty() &&
                preset.colors.first() == colorStart &&
                preset.colors.last() == colorEnd &&
                preset.type == gradientType &&
                preset.angle == angle
        }

    private class GradientPresetAdapter(
        private val onPresetSelected: (GradientColor) -> Unit
    ) : RecyclerView.Adapter<GradientPresetAdapter.VH>() {

        private var selectedIndex: Int = -1

        class VH(val frame: FrameLayout, val thumb: View) : RecyclerView.ViewHolder(frame)

        /** Perbarui preset yang ditandai (berdasarkan kecocokan state gradasi). */
        fun updateSelection(index: Int) {
            if (index != selectedIndex) {
                val prev = selectedIndex
                selectedIndex = index
                if (prev in 0 until itemCount) notifyItemChanged(prev)
                if (selectedIndex in 0 until itemCount) notifyItemChanged(selectedIndex)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val dp = parent.context.resources.displayMetrics.density
            val frame = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams((56 * dp).toInt(), (56 * dp).toInt())
            }
            val thumb = View(parent.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            frame.addView(thumb)
            return VH(frame, thumb)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val preset = GradientColor.PRESETS[position]
            val selected = position == selectedIndex
            val dp = holder.frame.context.resources.displayMetrics.density
            val pad = ((if (selected) 3 else 1) * dp).toInt()
            holder.frame.setPadding(pad, pad, pad, pad)
            holder.frame.background = ringDrawable(selected, dp)
            holder.thumb.background = GradientDrawable().apply {
                colors = preset.colors
                cornerRadius = 16f * dp
                gradientType = when (preset.type) {
                    GradientType.LINEAR -> GradientDrawable.LINEAR_GRADIENT
                    GradientType.RADIAL -> GradientDrawable.RADIAL_GRADIENT
                    GradientType.SWEEP -> GradientDrawable.SWEEP_GRADIENT
                }
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
            }
            holder.frame.setOnClickListener {
                onPresetSelected(preset)
                updateSelection(position)
            }
        }

        override fun getItemCount() = GradientColor.PRESETS.size

        private fun ringDrawable(selected: Boolean, dp: Float): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * dp
                setColor(0x00000000)
                setStroke(
                    ((if (selected) 2f else 1f) * dp).toInt(),
                    if (selected) 0xFF1769FF.toInt() else 0x11000000
                )
            }
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