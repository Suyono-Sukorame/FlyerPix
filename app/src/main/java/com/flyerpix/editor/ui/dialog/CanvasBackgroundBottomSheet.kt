package com.flyerpix.editor.ui.dialog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.databinding.LayoutCanvasBackgroundBottomSheetBinding
import com.flyerpix.editor.ui.adapter.GradientPickerAdapter

/**
 * BottomSheet Dialog untuk pengaturan latar belakang kanvas independen (Prompt 44).
 * Mendukung 3 mode:
 *  1. Background Transparan (Checkerboard pattern)
 *  2. Warna Solid (Warna preset cepat & Color Picker kustom)
 *  3. Warna Gradasi (Preset gradasi, Linear/Radial/Sweep, dan pengatur sudut derajat)
 */
class CanvasBackgroundBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutCanvasBackgroundBottomSheetBinding? = null
    private val binding get() = _binding!!

    var pixelCanvasView: PixelCanvasView? = null

    /** Callback untuk meminta Activity menjalankan gallery/camera picker (Prompt 45). */
    var onBackgroundImageRequested: ((Source) -> Unit)? = null

    enum class Source { GALLERY, CAMERA }

    private lateinit var gradientAdapter: GradientPickerAdapter
    private var currentGradientType: GradientType = GradientType.LINEAR
    private var currentGradientAngle: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutCanvasBackgroundBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupModeChips()
        setupSolidColors()
        setupGradientControls()
        setupImageBackground()
        restoreCurrentState()
    }

    private fun setupHeader() {
        binding.btnCloseBottomSheet.setOnClickListener {
            dismiss()
        }
    }

    private fun setupModeChips() {
        binding.cgBackgroundMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipTransparent -> {
                    showSection(isTransparent = true, isSolid = false, isGradient = false, isImage = false)
                    pixelCanvasView?.setTransparentBackground()
                }
                R.id.chipSolidColor -> {
                    showSection(isTransparent = false, isSolid = true, isGradient = false, isImage = false)
                    val color = pixelCanvasView?.canvasBackgroundColor ?: Color.WHITE
                    pixelCanvasView?.setColorBackground(color)
                }
                R.id.chipGradient -> {
                    showSection(isTransparent = false, isSolid = false, isGradient = true, isImage = false)
                    val currentGrad = pixelCanvasView?.canvasBackgroundGradient
                        ?: GradientColor.PRESETS[0].copy(type = currentGradientType, angle = currentGradientAngle)
                    pixelCanvasView?.setGradientBackground(currentGrad)
                }
                R.id.chipImage -> {
                    showSection(isTransparent = false, isSolid = false, isGradient = false, isImage = true)
                }
            }
        }
    }

    private fun showSection(isTransparent: Boolean, isSolid: Boolean, isGradient: Boolean, isImage: Boolean = false) {
        binding.containerTransparent.visibility = if (isTransparent) View.VISIBLE else View.GONE
        binding.containerSolidColor.visibility = if (isSolid) View.VISIBLE else View.GONE
        binding.containerGradient.visibility = if (isGradient) View.VISIBLE else View.GONE
        binding.containerImage.visibility = if (isImage) View.VISIBLE else View.GONE
    }

    private fun setupSolidColors() {
        val colors = listOf(
            0xFFFFFFFF.toInt(), // White
            0xFF000000.toInt(), // Black
            0xFF212121.toInt(), // Charcoal
            0xFFB0BEC5.toInt(), // Gray
            0xFF00E5FF.toInt(), // Cyan PixelLab
            0xFF00B0FF.toInt(), // Light Blue
            0xFF2979FF.toInt(), // Blue
            0xFF651FFF.toInt(), // Indigo
            0xFFAA00FF.toInt(), // Purple
            0xFFF50057.toInt(), // Pink
            0xFFE53935.toInt(), // Red
            0xFFFF6D00.toInt(), // Orange
            0xFFFFD600.toInt(), // Yellow
            0xFF00E676.toInt(), // Green
            0xFF00BFA5.toInt(), // Teal
            0xFFFFF9C4.toInt(), // Pastel Cream
            0xFFBBDEFB.toInt(), // Pastel Blue
            0xFFC8E6C9.toInt(), // Pastel Mint
            0xFFFFCCBC.toInt()  // Pastel Peach
        )

        binding.layoutSolidColorPresets.removeAllViews()
        val size = (38 * resources.displayMetrics.density).toInt()
        val margin = (5 * resources.displayMetrics.density).toInt()

        for (color in colors) {
            val swatch = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    val isWhiteOrLight = (color == Color.WHITE || color == 0xFFFFF9C4.toInt())
                    setStroke(
                        if (isWhiteOrLight) (1.5f * resources.displayMetrics.density).toInt() else 0,
                        if (isWhiteOrLight) Color.parseColor("#B0BEC5") else Color.TRANSPARENT
                    )
                }
                background = bg

                setOnClickListener {
                    pixelCanvasView?.setColorBackground(color)
                    highlightSelectedSolidSwatch(this)
                }
            }
            binding.layoutSolidColorPresets.addView(swatch)
        }

        binding.btnCustomSolidColor.setOnClickListener {
            val currentColor = pixelCanvasView?.canvasBackgroundColor ?: Color.WHITE
            ColorPickerDialog.newInstance(initialColor = currentColor)
                .show(childFragmentManager, "BackgroundSolidColorPicker")
        }

        childFragmentManager.setFragmentResultListener(ColorPickerDialog.RESULT_KEY, viewLifecycleOwner) { _, bundle ->
            val isGradient = bundle.getBoolean(ColorPickerDialog.EXTRA_IS_GRADIENT, false)
            if (!isGradient) {
                val color = bundle.getInt(ColorPickerDialog.EXTRA_COLOR, Color.WHITE)
                pixelCanvasView?.setColorBackground(color)
            } else {
                @Suppress("DEPRECATION")
                val grad = bundle.getSerializable(ColorPickerDialog.EXTRA_GRADIENT) as? GradientColor
                if (grad != null) {
                    pixelCanvasView?.setGradientBackground(grad)
                }
            }
        }
    }

    private fun highlightSelectedSolidSwatch(selectedView: View) {
        for (i in 0 until binding.layoutSolidColorPresets.childCount) {
            val child = binding.layoutSolidColorPresets.getChildAt(i)
            child.scaleX = if (child === selectedView) 1.2f else 1.0f
            child.scaleY = if (child === selectedView) 1.2f else 1.0f
        }
    }

    private fun setupGradientControls() {
        gradientAdapter = GradientPickerAdapter(GradientColor.PRESETS) { selectedPreset ->
            val grad = selectedPreset.copy(type = currentGradientType, angle = currentGradientAngle)
            pixelCanvasView?.setGradientBackground(grad)
        }
        binding.rvBackgroundGradients.adapter = gradientAdapter

        binding.rgBackgroundGradientType.setOnCheckedChangeListener { _, checkedId ->
            currentGradientType = when (checkedId) {
                R.id.rbBgRadial -> GradientType.RADIAL
                R.id.rbBgSweep  -> GradientType.SWEEP
                else            -> GradientType.LINEAR
            }
            binding.containerBgGradientAngle.visibility =
                if (currentGradientType == GradientType.LINEAR) View.VISIBLE else View.GONE

            pixelCanvasView?.canvasBackgroundGradient?.let { currentGrad ->
                val updated = currentGrad.copy(type = currentGradientType)
                pixelCanvasView?.setGradientBackground(updated)
            }
        }

        binding.sliderBgGradientAngle.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentGradientAngle = value
                binding.tvBgAngleLabel.text = "Sudut (${value.toInt()}°)"
                pixelCanvasView?.canvasBackgroundGradient?.let { currentGrad ->
                    val updated = currentGrad.copy(angle = value)
                    pixelCanvasView?.setGradientBackground(updated)
                }
            }
        }

        binding.btnCustomGradient.setOnClickListener {
            val currentGrad = pixelCanvasView?.canvasBackgroundGradient ?: GradientColor.PRESETS[0]
            ColorPickerDialog.newInstance(initialGradient = currentGrad)
                .show(childFragmentManager, "BackgroundGradientPicker")
        }
    }

    private fun setupImageBackground() {
        binding.btnFromGallery.setOnClickListener {
            onBackgroundImageRequested?.invoke(Source.GALLERY)
            dismiss()
        }
        binding.btnFromCamera.setOnClickListener {
            onBackgroundImageRequested?.invoke(Source.CAMERA)
            dismiss()
        }
        binding.btnClearImageBackground.setOnClickListener {
            pixelCanvasView?.clearImageBackground()
            binding.btnClearImageBackground.visibility = View.GONE
            binding.chipSolidColor.isChecked = true
            showSection(isTransparent = false, isSolid = true, isGradient = false, isImage = false)
        }
    }

    private fun restoreCurrentState() {
        val canvas = pixelCanvasView ?: return
        when (canvas.canvasBackground.mode) {
            CanvasBackgroundMode.TRANSPARENT -> {
                binding.chipTransparent.isChecked = true
                showSection(isTransparent = true, isSolid = false, isGradient = false)
            }
            CanvasBackgroundMode.SOLID_COLOR -> {
                binding.chipSolidColor.isChecked = true
                showSection(isTransparent = false, isSolid = true, isGradient = false)
            }
            CanvasBackgroundMode.GRADIENT -> {
                binding.chipGradient.isChecked = true
                showSection(isTransparent = false, isSolid = false, isGradient = true)
                canvas.canvasBackground.gradient?.let { grad ->
                    currentGradientType = grad.type
                    currentGradientAngle = grad.angle
                    when (grad.type) {
                        GradientType.LINEAR -> binding.rbBgLinear.isChecked = true
                        GradientType.RADIAL -> binding.rbBgRadial.isChecked = true
                        GradientType.SWEEP  -> binding.rbBgSweep.isChecked = true
                    }
                    binding.sliderBgGradientAngle.value = grad.angle
                    binding.tvBgAngleLabel.text = "Sudut (${grad.angle.toInt()}°)"
                }
            }
            CanvasBackgroundMode.IMAGE -> {
                binding.chipImage.isChecked = true
                showSection(isTransparent = false, isSolid = false, isGradient = false, isImage = true)
                binding.btnClearImageBackground.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CanvasBackgroundBottomSheet"

        fun show(fragmentManager: FragmentManager, canvasView: PixelCanvasView): CanvasBackgroundBottomSheet {
            val existing = fragmentManager.findFragmentByTag(TAG) as? CanvasBackgroundBottomSheet
            if (existing != null) {
                existing.pixelCanvasView = canvasView
                return existing
            }
            val sheet = CanvasBackgroundBottomSheet().apply {
                this.pixelCanvasView = canvasView
            }
            sheet.show(fragmentManager, TAG)
            return sheet
        }
    }
}
