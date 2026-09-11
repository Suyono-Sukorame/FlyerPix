package com.flyerpix.editor.ui.dialog

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.DialogImagePreEditBinding
import com.flyerpix.editor.imageedit.CropShape

class ImagePreEditDialog : DialogFragment() {

    private var image: Bitmap? = null
    var onResult: ((Bitmap?) -> Unit)? = null

    private var _binding: DialogImagePreEditBinding? = null
    private val binding get() = _binding!!

    private val ratioPresets: List<Pair<String, Pair<Int, Int>>> = listOf(
        "1:1" to (1 to 1),
        "16:9" to (16 to 9),
        "4:3" to (4 to 3),
        "3:2" to (3 to 2),
        "9:16" to (9 to 16),
        "3:4" to (3 to 4),
        "2:3" to (2 to 3),
        "4:6" to (4 to 6),
        "6:8" to (6 to 8),
    )

    private class RatioPill(val label: TextView, val box: View)

private val ratioPills = mutableListOf<RatioPill>()
    private var freeBtn: ImageButton? = null
    private var selectedPreset: Pair<Int, Int>? = null

    private val pillSelectedColor = 0xFF1769FF.toInt()
    private val pillTextColor = 0xFF5F6B7A.toInt()
    private val pillTextSelected = Color.WHITE
    private val pillBorderColor = 0xFFB9C2CE.toInt()
    private val pillActiveFill = 0xFFE3EEFC.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogImagePreEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        image?.let { binding.preEditView.setImage(it) }

        val editor = binding.preEditView
        buildRatioPills()
        binding.btnRotateLeft.setOnClickListener { editor.rotateLeft() }
        binding.btnRotateRight.setOnClickListener { editor.rotateRight() }
        binding.btnZoomOut.setOnClickListener { editor.zoomOut() }
        binding.btnZoomIn.setOnClickListener { editor.zoomIn() }
        binding.btnFlipRight.setOnClickListener { editor.flipHorizontal() }
        binding.btnFlipLeft.setOnClickListener { editor.flipVertical() }

        binding.btnLock.setOnClickListener {
            val locked = editor.toggleLock()
            binding.btnLock.setImageResource(
                if (locked) R.drawable.ic_lock_24px else R.drawable.ic_lock_open_24px
            )
        }

        binding.btnShape.setOnClickListener {
            val shape = editor.toggleShape()
            binding.btnShape.setImageResource(
                if (shape == CropShape.CIRCLE) R.drawable.ic_sharp_crop_free_24px
                else R.drawable.ic_sharp_circle_outline_24px
            )
        }

        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnOk.setOnClickListener {
            onResult?.invoke(editor.buildResultBitmap())
            dismiss()
        }
    }

    private fun buildRatioPills() {
        val container = binding.ratioContainer
        container.removeAllViews()
        ratioPills.clear()
        for ((label, ratio) in ratioPresets) {
            val item = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                isClickable = true
                isFocusable = true
                setPadding(dp(9), dp(5), dp(9), dp(5))
                background = pillItemBackground(false)
                setOnClickListener {
                    selectPreset(ratio)
                    binding.preEditView.applyAspectRatio(ratio.first, ratio.second)
                }
            }

            val box = View(requireContext()).apply {
                background = ratioBoxBackground(false, ratio.first, ratio.second)
            }
            val boxLp = LinearLayout.LayoutParams(
                ratioBoxWidthDp(ratio.first, ratio.second),
                dp(18)
            )
            item.addView(box, boxLp)

            val label = TextView(requireContext()).apply {
                text = label
                textSize = 11f
                setTextColor(pillTextColor)
                gravity = Gravity.CENTER
                setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                setPadding(0, dp(3), 0, 0)
            }
            val labelLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            item.addView(label, labelLp)

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(dp(2), 0, dp(2), 0)
            container.addView(item, lp)
            ratioPills.add(RatioPill(label, box))
        }

        val freeBtn = ImageButton(requireContext())
        freeBtn.setImageResource(R.drawable.ic_sharp_crop_free_24px)
        freeBtn.contentDescription = "Rasio Bebas"
        freeBtn.background = rippleBackground()
        freeBtn.scaleType = android.widget.ImageView.ScaleType.CENTER
        freeBtn.setPadding(0, 0, 0, 0)
        freeBtn.setOnClickListener { selectFree() }
        this.freeBtn = freeBtn
        val freeLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        freeLp.gravity = Gravity.CENTER_VERTICAL
        freeLp.width = dp(44)
        freeLp.height = dp(40)
        freeLp.setMargins(dp(4), 0, 0, 0)
        container.addView(freeBtn, freeLp)
    }

    private fun selectPreset(ratio: Pair<Int, Int>) {
        selectedPreset = ratio
        updatePillStyles()
    }

    private fun selectFree() {
        selectedPreset = null
        binding.preEditView.applyFreeRatio()
        updatePillStyles()
    }

    private fun updatePillStyles() {
        for ((index, pill) in ratioPills.withIndex()) {
            val active = ratioPresets[index].second == selectedPreset
            pill.label.setTextColor(if (active) pillSelectedColor else pillTextColor)
            pill.box.background = ratioBoxBackground(
                active, ratioPresets[index].second.first, ratioPresets[index].second.second
            )
            (pill.box.parent as? android.view.View)?.background = pillItemBackground(active)
        }
        freeBtn?.setColorFilter(if (selectedPreset == null) pillSelectedColor else pillTextColor)
    }

    /** Lebar kotak preview proporsional terhadap rasio (dibatasi agar tidak ekstrem). */
    private fun ratioBoxWidthDp(ratioW: Int, ratioH: Int): Int {
        val dpW = (18f * ratioW.toFloat() / ratioH.toFloat()).coerceIn(9f, 34f)
        return (dpW * resources.displayMetrics.density).toInt()
    }

    private fun ratioBoxBackground(active: Boolean, ratioW: Int, ratioH: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(2).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(dp(2), if (active) pillSelectedColor else pillBorderColor)
        }

    private fun pillItemBackground(active: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(9).toFloat()
            setColor(if (active) pillActiveFill else Color.TRANSPARENT)
        }

    private fun rippleBackground(): android.graphics.drawable.Drawable {
    val out = TypedValue()
    requireContext().theme.resolveAttribute(
        android.R.attr.selectableItemBackgroundBorderless, out, true
    )
    return requireContext().getDrawable(out.resourceId)
        ?: android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
}

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ImagePreEditDialog"

        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            bitmap: Bitmap,
            onResult: (Bitmap?) -> Unit
        ): ImagePreEditDialog {
            val dialog = ImagePreEditDialog()
            dialog.image = bitmap
            dialog.onResult = onResult
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}