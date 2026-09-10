package com.flyerpix.editor.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.databinding.DialogColorPickerBinding

/**
 * Dialog Color Picker komprehensif dengan 2 tab:
 *  1. **Solid** — preset grid warna + slider HSV + hex input.
 *  2. **Gradient** — pemilih gradasi (arah linear, 2 titik warna).
 *
 * Hasil dikirim melalui [Fragment Result API] dengan key [RESULT_KEY].
 * Bundle berisi:
 *  - [EXTRA_IS_GRADIENT] `Boolean` — true jika hasilnya gradient.
 *  - [EXTRA_COLOR] `Int` — warna solid (hanya jika isGradient = false).
 *  - [EXTRA_GRADIENT] `GradientColor` — model gradasi (hanya jika isGradient = true).
 */
class ColorPickerDialog : DialogFragment() {

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    private var initialColor: Int = 0xFFFFFFFF.toInt()
    private var initialGradient: GradientColor? = null
    private var resultKey: String = RESULT_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AppAlertDialog)
        initialColor = arguments?.getInt(ARG_COLOR, 0xFFFFFFFF.toInt()) ?: 0xFFFFFFFF.toInt()
        initialGradient = @Suppress("DEPRECATION") (arguments?.getSerializable(ARG_GRADIENT) as? GradientColor)
        resultKey = arguments?.getString(ARG_RESULT_KEY) ?: RESULT_KEY
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val solidFragment = SolidColorFragment.newInstance(initialColor)
        val gradientFragment = GradientColorFragment.newInstance(initialGradient)

        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int) =
                if (position == 0) solidFragment else gradientFragment
        }

        binding.viewPagerColorPicker.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayoutColorPicker, binding.viewPagerColorPicker) { tab, pos ->
            tab.text = if (pos == 0) "Solid" else "Gradient"
        }.attach()

        binding.btnColorPickerOk.setOnClickListener {
            val currentFragment = childFragmentManager.findFragmentByTag(
                "f${binding.viewPagerColorPicker.currentItem}"
            )
            val result = Bundle()
            if (binding.viewPagerColorPicker.currentItem == 0 && currentFragment is SolidColorFragment) {
                val color = currentFragment.getSelectedColor()
                result.putBoolean(EXTRA_IS_GRADIENT, false)
                result.putInt(EXTRA_COLOR, color)
                ColorRecents(requireContext()).pushSolid(color)
            } else if (currentFragment is GradientColorFragment) {
                result.putBoolean(EXTRA_IS_GRADIENT, true)
                @Suppress("DEPRECATION")
                val gradient = currentFragment.getGradient()
                result.putSerializable(EXTRA_GRADIENT, gradient)
                ColorRecents(requireContext()).pushGradient(gradient)
            }
            setFragmentResult(resultKey, result)
            dismiss()
        }

        binding.btnColorPickerCancel.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ColorPickerDialog"
        const val RESULT_KEY = "color_picker_result"
        const val DEPTH_RESULT_KEY = "depth_color_picker_result"
        const val SHADOW3D_RESULT_KEY = "shadow3d_color_picker_result"
        const val INNER_SHADOW_RESULT_KEY = "inner_shadow_color_picker_result"
        const val NEON_RESULT_KEY = "neon_color_picker_result"
        const val SHADOW_RESULT_KEY = "shadow_color_picker_result"
        const val STROKE_RESULT_KEY = "stroke_color_picker_result"
        const val RICH_TEXT_RESULT_KEY = "rich_text_color_picker_result"
        const val BG_RESULT_KEY = "bg_color_picker_result"
        const val TEXT_RESULT_KEY = "text_color_picker_result"
        const val EXTRA_IS_GRADIENT = "is_gradient"
        const val EXTRA_COLOR = "selected_color"
        const val EXTRA_GRADIENT = "selected_gradient"
        private const val ARG_COLOR = "initial_color"
        private const val ARG_GRADIENT = "initial_gradient"
        private const val ARG_RESULT_KEY = "result_key"

        fun newInstance(
            initialColor: Int = 0xFFFFFFFF.toInt(),
            initialGradient: GradientColor? = null,
            resultKey: String = RESULT_KEY
        ): ColorPickerDialog {
            return ColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLOR, initialColor)
                    @Suppress("DEPRECATION")
                    putSerializable(ARG_GRADIENT, initialGradient)
                    putString(ARG_RESULT_KEY, resultKey)
                }
            }
        }
    }
}
