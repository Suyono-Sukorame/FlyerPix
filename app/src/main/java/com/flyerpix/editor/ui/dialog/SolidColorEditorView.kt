package com.flyerpix.editor.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.flyerpix.editor.R
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

/**
 * Editor warna solid modern (reusable):
 *  - Preview bulat + hex editor live (termasuk alpha #AARRGGBB).
 *  - 4 slider Material: Hue, Saturation, Brightness, Alpha — dengan label nilai.
 *  - Daftar warna terakhir (Recent, disimpan di SharedPreferences).
 *  - Strip preset 70 warna dengan ring seleksi.
 *
 * Dipakai oleh tab Solid (SolidColorFragment) dan sebagai editor inline untuk
 * dua stop gradasi (GradientColorFragment) menggantikan dialog bertumpuk.
 */
class SolidColorEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var currentColor: Int = Color.WHITE
    private var onColorChanged: ((Int) -> Unit)? = null

    private val hsv = floatArrayOf(0f, 0f, 1f)

    private lateinit var preview: View
    private lateinit var etHex: EditText
    private lateinit var svPanel: SvHuePanel
    private lateinit var sliderAlpha: Slider
    private lateinit var tvAlphaValue: TextView
    private lateinit var llPresets: LinearLayout
    private lateinit var llRecents: LinearLayout
    private lateinit var tvRecentLabel: TextView
    private lateinit var hsvRecents: View

    private val presetViews = ArrayList<Pair<Int, View>>()
    private val recentViews = ArrayList<Pair<Int, View>>()

    init {
        orientation = VERTICAL
        val content = inflate(context, R.layout.view_solid_color_editor, this)
        bindViews(content)
        wireUp()
    }

    private fun bindViews(root: View) {
        preview = root.findViewById(R.id.viewColorPreview)
        etHex = root.findViewById(R.id.etHexInput)
        svPanel = root.findViewById(R.id.svPanel)
        sliderAlpha = root.findViewById(R.id.sliderAlpha)
        tvAlphaValue = root.findViewById(R.id.tvAlphaValue)
        llPresets = root.findViewById(R.id.llPresets)
        llRecents = root.findViewById(R.id.llRecents)
        tvRecentLabel = root.findViewById(R.id.tvRecentLabel)
        hsvRecents = root.findViewById(R.id.hsvRecents)
    }

    private fun wireUp() {
        preview.background = circularSwatch(currentColor, borderWidth = 2)

        // Hex input — live, tanpa tombol Apply; Enter = commit.
        etHex.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        etHex.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                commitCurrentColorToRecents()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                        as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(etHex.windowToken, 0)
                true
            } else {
                false
            }
        }
        etHex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val parsed = parseHex(s?.toString().orEmpty())
                if (parsed != null) {
                    preview.setBackgroundColor(parsed)
                    applyColor(parsed, fromHex = true)
                } else {
                    etHex.setTextColor(0xFFE53935.toInt())
                }
            }
        })

        // Long-press preview = salin hex ke clipboard.
        preview.setOnLongClickListener {
            val hex = hexOf(currentColor)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Hex Color", hex))
            android.widget.Toast.makeText(context, "Copied $hex", android.widget.Toast.LENGTH_SHORT)
                .show()
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true
        }

        sliderAlpha.addOnChangeListener { _, alpha, fromUser ->
            if (fromUser) {
                tvAlphaValue.text = "${(alpha / 255f * 100f).roundToInt()}%"
                syncFromPanel()
            }
        }
        sliderAlpha.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                commitCurrentColorToRecents()
            }
        })

        svPanel.onChange = { h, s, v ->
            hsv[0] = h
            hsv[1] = s
            hsv[2] = v
            syncFromPanel()
        }
        svPanel.onCommit = { commitCurrentColorToRecents() }

        buildPresets()
        loadRecents()
    }

    // ── Public API ──────────────────────────────────────────────────────────

    fun setInitialColor(color: Int) {
        applyColor(color, fromHex = false)
    }

    fun getSelectedColor(): Int = currentColor

    fun setOnColorChanged(listener: (Int) -> Unit) {
        onColorChanged = listener
    }

    /** Tambahkan warna terpilih ke daftar Recent (SharedPreferences). */
    fun commitCurrentColorToRecents() {
        val recents = loadRecentsFromStorage()
        if (recents.firstOrNull() == currentColor) return
        recents.remove(currentColor)
        recents.add(0, currentColor)
        val trimmed = recents.take(MAX_RECENTS).toMutableList()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENTS, trimmed.joinToString(","))
            .apply()
        rebuildRecents(trimmed)
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private fun applyColor(color: Int, fromHex: Boolean) {
        currentColor = color
        Color.colorToHSV(color, hsv)

        svPanel.setHsv(hsv[0], hsv[1], hsv[2])
        sliderAlpha.value = Color.alpha(color).toFloat()
        tvAlphaValue.text = "${(Color.alpha(color) / 255f * 100f).roundToInt()}%"

        val hex = hexOf(color)
        if (!fromHex) {
            etHex.setText(hex)
            etHex.setSelection(hex.length)
        }
        etHex.setTextColor(0xFF1A1A2E.toInt())
        preview.background = circularSwatch(color, borderWidth = 2)
        refreshSelectionHighlight()
        onColorChanged?.invoke(color)
    }

    private fun syncFromPanel() {
        val rgb = Color.HSVToColor(hsv)
        currentColor = Color.argb(
            sliderAlpha.value.roundToInt(),
            Color.red(rgb),
            Color.green(rgb),
            Color.blue(rgb)
        )
        val hex = hexOf(currentColor)
        if (etHex.text.toString().uppercase() != hex) {
            etHex.setText(hex)
            etHex.setSelection(hex.length)
        }
        etHex.setTextColor(0xFF1A1A2E.toInt())
        preview.background = circularSwatch(currentColor, borderWidth = 2)
        refreshSelectionHighlight()
        onColorChanged?.invoke(currentColor)
    }

    private fun hexOf(color: Int): String =
        if (Color.alpha(color) == 255)
            String.format("#%06X", 0xFFFFFF and color)
        else
            String.format("#%08X", color)

    private fun parseHex(hex: String): Int? {
        val cleaned = hex.trim().removePrefix("#")
        return try {
            when (cleaned.length) {
                3, 6, 8 -> Color.parseColor("#$cleaned")
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun circularSwatch(color: Int, borderWidth: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (borderWidth > 0) {
                setStroke(
                    dip(borderWidth),
                    if (Color.alpha(color) < 255) 0x33666666 else 0x22666666
                )
            }
        }

    // ── Presets ─────────────────────────────────────────────────────────────

    private fun buildPresets() {
        presetViews.clear()
        llPresets.removeAllViews()
        val size = dip(SWATCH_DP)
        val margin = dip(SWATCH_MARGIN_DP)
        for (color in PRESET_COLORS) {
            val swatch = makeSwatch(size)
            val gd = circularSwatch(color, borderWidth = 1)
            gd.setStroke(1, if (color == Color.WHITE) 0xFFDDDDDD.toInt() else 0x22FFFFFF)
            swatch.background = gd
            swatch.setOnClickListener {
                swatch.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                applyColor(color, fromHex = false)
                commitCurrentColorToRecents()
            }
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(margin, margin, margin, margin)
            llPresets.addView(swatch, lp)
            presetViews.add(color to swatch)
        }
        refreshSelectionHighlight()
    }

    // ── Recents ─────────────────────────────────────────────────────────────

    private fun loadRecentsFromStorage(): MutableList<Int> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RECENTS, "")
            .orEmpty()
        val list = ArrayList<Int>()
        for (part in raw.split(",")) {
            val v = part.toIntOrNull()
            if (v != null) list.add(v)
        }
        return list
    }

    private fun loadRecents() {
        rebuildRecents(loadRecentsFromStorage())
    }

    private fun rebuildRecents(recents: List<Int>) {
        val hasRecent = recents.isNotEmpty()
        tvRecentLabel.visibility = if (hasRecent) View.VISIBLE else View.GONE
        hsvRecents.visibility = if (hasRecent) View.VISIBLE else View.GONE
        recentViews.clear()
        llRecents.removeAllViews()
        if (!hasRecent) return

        val size = dip(SWATCH_DP)
        val margin = dip(SWATCH_MARGIN_DP)
        for (color in recents) {
            val swatch = makeSwatch(size)
            swatch.background = circularSwatch(color, borderWidth = 1)
            swatch.setOnClickListener {
                swatch.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                applyColor(color, fromHex = false)
            }
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(margin, margin, margin, margin)
            llRecents.addView(swatch, lp)
            recentViews.add(color to swatch)
        }
        refreshSelectionHighlight()
    }

    private fun makeSwatch(size: Int): View = View(context).apply {
        layoutParams = ViewGroup.LayoutParams(size, size)
    }

    private fun refreshSelectionHighlight() {
        fun highlight(list: List<Pair<Int, View>>) {
            for ((color, swatch) in list) {
                val selected = color == currentColor
                val gd = circularSwatch(color, borderWidth = if (selected) 3 else 1)
                if (!selected && color == Color.WHITE) {
                    gd.setStroke(1, 0xFFDDDDDD.toInt())
                } else if (!selected) {
                    gd.setStroke(1, 0x22FFFFFF)
                }
                swatch.background = gd
            }
        }
        highlight(presetViews)
        highlight(recentViews)
    }

    private fun dip(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFS = "color_picker"
        private const val KEY_RECENTS = "solid_color_recents"
        private const val MAX_RECENTS = 12
        private const val SWATCH_DP = 34
        private const val SWATCH_MARGIN_DP = 4
    }
}

private val PRESET_COLORS = intArrayOf(
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