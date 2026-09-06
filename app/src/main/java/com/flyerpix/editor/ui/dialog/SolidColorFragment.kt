package com.flyerpix.editor.ui.dialog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.flyerpix.editor.R
import kotlin.math.roundToInt

/**
 * Tab Solid Color — picker warna solid dengan:
 *  - Preview warna + hex input.
 *  - 3 slider HSV (Hue, Saturation, Value).
 *  - Grid preset warna 7×N.
 */
class SolidColorFragment : Fragment() {

    private var hsv = floatArrayOf(0f, 0f, 1f)
    private var currentColor: Int = Color.WHITE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_solid_color, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initColor = arguments?.getInt(ARG_COLOR, Color.WHITE) ?: Color.WHITE
        setColor(initColor)

        val seekHue = view.findViewById<SeekBar>(R.id.seekHue)
        val seekSat = view.findViewById<SeekBar>(R.id.seekSaturation)
        val seekVal = view.findViewById<SeekBar>(R.id.seekValue)
        val preview = view.findViewById<View>(R.id.viewColorPreview)
        val etHex = view.findViewById<android.widget.EditText>(R.id.etHexInput)
        val btnApply = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHexApply)
        val grid = view.findViewById<GridLayout>(R.id.gridPresets)

        // HSV sliders
        val hsvListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                hsv[0] = seekHue.progress.toFloat()
                hsv[1] = seekSat.progress / 100f
                hsv[2] = seekVal.progress / 100f
                currentColor = Color.HSVToColor(hsv)
                updatePreview(preview, etHex)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
        seekHue.setOnSeekBarChangeListener(hsvListener)
        seekSat.setOnSeekBarChangeListener(hsvListener)
        seekVal.setOnSeekBarChangeListener(hsvListener)

        // Hex input
        btnApply.setOnClickListener {
            val hex = etHex.text.toString().trim()
            val parsed = parseHex(hex)
            if (parsed != null) setColor(parsed)
            syncSliders(seekHue, seekSat, seekVal)
            updatePreview(preview, etHex)
        }

        etHex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hex = s?.toString()?.trim() ?: return
                val parsed = parseHex(hex)
                if (parsed != null) {
                    currentColor = parsed
                    android.graphics.Color.colorToHSV(parsed, hsv)
                    syncSliders(seekHue, seekSat, seekVal)
                    updatePreview(preview, etHex)
                }
            }
        })

        // Preset grid
        buildPresetGrid(grid) { color ->
            setColor(color)
            syncSliders(seekHue, seekSat, seekVal)
            updatePreview(preview, etHex)
        }

        // Initial sync
        syncSliders(seekHue, seekSat, seekVal)
        updatePreview(preview, etHex)
    }

    private fun setColor(color: Int) {
        currentColor = color
        Color.colorToHSV(color, hsv)
    }

    private fun syncSliders(seekHue: SeekBar, seekSat: SeekBar, seekVal: SeekBar) {
        seekHue.progress = hsv[0].roundToInt()
        seekSat.progress = (hsv[1] * 100).roundToInt()
        seekVal.progress = (hsv[2] * 100).roundToInt()
    }

    private fun updatePreview(preview: View, etHex: android.widget.EditText) {
        preview.setBackgroundColor(currentColor)
        val hex = String.format("#%06X", 0xFFFFFF and currentColor)
        if (etHex.text?.toString()?.uppercase() != hex) {
            etHex.setText(hex)
            etHex.setSelection(hex.length)
        }
    }

    private fun parseHex(hex: String): Int? {
        val cleaned = hex.trim().removePrefix("#")
        return try {
            when (cleaned.length) {
                6 -> Color.parseColor("#$cleaned")
                3 -> {
                    val r = cleaned[0].toString().repeat(2)
                    val g = cleaned[1].toString().repeat(2)
                    val b = cleaned[2].toString().repeat(2)
                    Color.parseColor("#$r$g$b")
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildPresetGrid(grid: GridLayout, onColorSelected: (Int) -> Unit) {
        val presets = intArrayOf(
            Color.WHITE, Color.parseColor("#F5F5F5"), Color.parseColor("#E0E0E0"),
            Color.parseColor("#9E9E9E"), Color.parseColor("#616161"), Color.parseColor("#424242"),
            Color.BLACK,
            Color.parseColor("#B71C1C"), Color.parseColor("#D50000"), Color.parseColor("#FF5252"),
            Color.parseColor("#FF8A80"), Color.parseColor("#F8BBD0"), Color.parseColor("#E91E63"),
            Color.parseColor("#AD1457"),
            Color.parseColor("#E65100"), Color.parseColor("#FF6D00"), Color.parseColor("#FF9E80"),
            Color.parseColor("#FFE0B2"), Color.parseColor("#FFF3E0"), Color.parseColor("#FFAB40"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#F9A825"), Color.parseColor("#FDD835"), Color.parseColor("#FFEE58"),
            Color.parseColor("#FFF9C4"), Color.parseColor("#FFFDE7"), Color.parseColor("#FFD54F"),
            Color.parseColor("#FF8F00"),
            Color.parseColor("#1B5E20"), Color.parseColor("#2E7D32"), Color.parseColor("#66BB6A"),
            Color.parseColor("#A5D6A7"), Color.parseColor("#C8E6C9"), Color.parseColor("#00C853"),
            Color.parseColor("#1B5E20"),
            Color.parseColor("#004D40"), Color.parseColor("#00796B"), Color.parseColor("#26A69A"),
            Color.parseColor("#80CBC4"), Color.parseColor("#B2DFDB"), Color.parseColor("#1DE9B6"),
            Color.parseColor("#00897B"),
            Color.parseColor("#0D47A1"), Color.parseColor("#1976D2"), Color.parseColor("#42A5F5"),
            Color.parseColor("#90CAF9"), Color.parseColor("#BBDEFB"), Color.parseColor("#448AFF"),
            Color.parseColor("#2962FF"),
            Color.parseColor("#4A148C"), Color.parseColor("#7B1FA2"), Color.parseColor("#AB47BC"),
            Color.parseColor("#CE93D8"), Color.parseColor("#E1BEE7"), Color.parseColor("#AA00FF"),
            Color.parseColor("#6200EA"),
            Color.parseColor("#3E2723"), Color.parseColor("#5D4037"), Color.parseColor("#8D6E63"),
            Color.parseColor("#BCAAA4"), Color.parseColor("#D7CCC8"), Color.parseColor("#795548"),
            Color.parseColor("#4E342E"),
            Color.parseColor("#263238"), Color.parseColor("#37474F"), Color.parseColor("#546E7A"),
            Color.parseColor("#78909C"), Color.parseColor("#B0BEC5"), Color.parseColor("#607D8B"),
            Color.parseColor("#455A64")
        )

        grid.removeAllViews()
        val size = resources.getDimensionPixelSize(R.dimen.preset_color_size)
        val margin = resources.getDimensionPixelSize(R.dimen.preset_color_margin)

        for (color in presets) {
            val swatch = View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    cornerRadius = 8f
                    setStroke(2, if (color == Color.WHITE) Color.parseColor("#DDDDDD") else Color.TRANSPARENT)
                }
                background = bg
                setOnClickListener { onColorSelected(color) }
            }
            grid.addView(swatch)
        }
    }

    fun getSelectedColor(): Int = currentColor

    companion object {
        private const val ARG_COLOR = "initial_color"

        fun newInstance(initialColor: Int): SolidColorFragment {
            return SolidColorFragment().apply {
                arguments = Bundle().apply { putInt(ARG_COLOR, initialColor) }
            }
        }
    }
}
