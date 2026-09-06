package com.flyerpix.editor.ui.dialog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import kotlin.math.roundToInt

/**
 * Tab Gradient Color — picker gradasi dengan:
 *  - Preview bar gradasi.
 *  - 2 titik warna (start/end) — ketuk untuk ubah.
 *  - Pilihan tipe gradasi (Linear/Radial/Sweep).
 *  - Slider sudut (Linear).
 *  - Preset gradasi populer.
 */
class GradientColorFragment : Fragment() {

    private var colorStart: Int = 0xFFFF512F.toInt()
    private var colorEnd: Int = 0xFFDD2476.toInt()
    private var gradientType: GradientType = GradientType.LINEAR
    private var angle: Float = 0f
    private var pickingStart = true

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

        val preview = view.findViewById<View>(R.id.viewGradientPreview)
        val colorStartView = view.findViewById<View>(R.id.viewColorStart)
        val colorEndView = view.findViewById<View>(R.id.viewColorEnd)
        val rgType = view.findViewById<RadioGroup>(R.id.rgGradientType)
        val layoutAngle = view.findViewById<View>(R.id.layoutAngle)
        val tvAngleLabel = view.findViewById<TextView>(R.id.tvAngleLabel)
        val sliderAngle = view.findViewById<Slider>(R.id.sliderAngle)
        val rvPresets = view.findViewById<RecyclerView>(R.id.rvGradientPresets)

        fun updatePreview() {
            val colors = intArrayOf(colorStart, colorEnd)
            val gd = GradientDrawable().apply {
                this.colors = colors
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

        fun updateColorSwatches() {
            colorStartView.setBackgroundColor(colorStart)
            colorEndView.setBackgroundColor(colorEnd)
        }

        // Color picker dialogs for start/end
        fun showColorPicker(initialColor: Int, onPicked: (Int) -> Unit) {
            val hsv = floatArrayOf(0f, 0f, 1f)
            Color.colorToHSV(initialColor, hsv)

            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_solid_color, null)

            val seekHue = dialogView.findViewById<SeekBar>(R.id.seekHue)
            val seekSat = dialogView.findViewById<SeekBar>(R.id.seekSaturation)
            val seekVal = dialogView.findViewById<SeekBar>(R.id.seekValue)
            val previewView = dialogView.findViewById<View>(R.id.viewColorPreview)
            val etHex = dialogView.findViewById<android.widget.EditText>(R.id.etHexInput)
            val btnApply = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHexApply)
            val grid = dialogView.findViewById<android.widget.GridLayout>(R.id.gridPresets)

            var currentColor = initialColor

            fun updateDialogPreview() {
                previewView.setBackgroundColor(currentColor)
                val hex = String.format("#%06X", 0xFFFFFF and currentColor)
                etHex.setText(hex)
                etHex.setSelection(hex.length)
            }

            fun syncSliders() {
                seekHue.progress = hsv[0].roundToInt()
                seekSat.progress = (hsv[1] * 100).roundToInt()
                seekVal.progress = (hsv[2] * 100).roundToInt()
            }

            val hsvListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    hsv[0] = seekHue.progress.toFloat()
                    hsv[1] = seekSat.progress / 100f
                    hsv[2] = seekVal.progress / 100f
                    currentColor = Color.HSVToColor(hsv)
                    updateDialogPreview()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            }
            seekHue.setOnSeekBarChangeListener(hsvListener)
            seekSat.setOnSeekBarChangeListener(hsvListener)
            seekVal.setOnSeekBarChangeListener(hsvListener)

            btnApply.setOnClickListener {
                val hex = etHex.text.toString().trim().removePrefix("#")
                try {
                    currentColor = Color.parseColor("#$hex")
                    Color.colorToHSV(currentColor, hsv)
                    syncSliders()
                    updateDialogPreview()
                } catch (_: Exception) {}
            }

            syncSliders()
            updateDialogPreview()

            MaterialAlertDialogBuilder(requireContext(), R.style.AppAlertDialog)
                .setTitle("Pick Color")
                .setView(dialogView)
                .setPositiveButton("OK") { _, _ -> onPicked(currentColor) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        colorStartView.setOnClickListener {
            showColorPicker(colorStart) { color ->
                colorStart = color
                updateColorSwatches()
                updatePreview()
            }
        }

        colorEndView.setOnClickListener {
            showColorPicker(colorEnd) { color ->
                colorEnd = color
                updateColorSwatches()
                updatePreview()
            }
        }

        // Gradient type
        rgType.setOnCheckedChangeListener { _, checkedId ->
            gradientType = when (checkedId) {
                R.id.rbRadial -> GradientType.RADIAL
                R.id.rbSweep -> GradientType.SWEEP
                else -> GradientType.LINEAR
            }
            layoutAngle.visibility = if (gradientType == GradientType.LINEAR) View.VISIBLE else View.GONE
            updatePreview()
        }

        // Angle slider
        sliderAngle.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                angle = value
                tvAngleLabel.text = "Angle: ${value.roundToInt()}°"
                updatePreview()
            }
        }

        // Gradient presets
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

        // Init
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

    // ── Preset adapter (inline, simple) ─────────────────────────────────────

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
