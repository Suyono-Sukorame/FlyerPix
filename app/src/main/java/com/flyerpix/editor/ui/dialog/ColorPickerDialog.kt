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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AppAlertDialog)
        initialColor = arguments?.getInt(ARG_COLOR, 0xFFFFFFFF.toInt()) ?: 0xFFFFFFFF.toInt()
        initialGradient = @Suppress("DEPRECATION") (arguments?.getSerializable(ARG_GRADIENT) as? GradientColor)
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
                result.putBoolean(EXTRA_IS_GRADIENT, false)
                result.putInt(EXTRA_COLOR, currentFragment.getSelectedColor())
            } else if (currentFragment is GradientColorFragment) {
                result.putBoolean(EXTRA_IS_GRADIENT, true)
                @Suppress("DEPRECATION")
                result.putSerializable(EXTRA_GRADIENT, currentFragment.getGradient())
            }
            setFragmentResult(RESULT_KEY, result)
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
        const val EXTRA_IS_GRADIENT = "is_gradient"
        const val EXTRA_COLOR = "selected_color"
        const val EXTRA_GRADIENT = "selected_gradient"
        private const val ARG_COLOR = "initial_color"
        private const val ARG_GRADIENT = "initial_gradient"

        fun newInstance(
            initialColor: Int = 0xFFFFFFFF.toInt(),
            initialGradient: GradientColor? = null
        ): ColorPickerDialog {
            return ColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLOR, initialColor)
                    @Suppress("DEPRECATION")
                    putSerializable(ARG_GRADIENT, initialGradient)
                }
            }
        }
    }
}
