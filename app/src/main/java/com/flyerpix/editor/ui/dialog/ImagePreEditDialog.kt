package com.flyerpix.editor.ui.dialog

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.DialogImagePreEditBinding
import com.flyerpix.editor.imageedit.CropShape

class ImagePreEditDialog : DialogFragment() {

    private var image: Bitmap? = null
    var onResult: ((Bitmap?) -> Unit)? = null

    private var _binding: DialogImagePreEditBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
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
        binding.btnRotateLeft.setOnClickListener { editor.rotateLeft() }
        binding.btnRotateRight.setOnClickListener { editor.rotateRight() }
        binding.btnZoomOut.setOnClickListener { editor.zoomOut() }
        binding.btnZoomIn.setOnClickListener { editor.zoomIn() }
        binding.btnFlipHorizontal.setOnClickListener { editor.flipHorizontal() }
        binding.btnFlipVertical.setOnClickListener { editor.flipVertical() }

        binding.btnLock.setOnClickListener {
            val locked = editor.toggleLock()
            binding.btnLock.setImageResource(
                if (locked) R.drawable.ic_lock_24px else R.drawable.ic_lock_open_24px
            )
        }

        binding.btnShape.setOnClickListener {
            val shape = editor.toggleShape()
            binding.btnShape.setImageResource(
                if (shape == CropShape.RECTANGLE) R.drawable.ic_sharp_crop_free_24px else R.drawable.ic_sharp_circle_outline_24px
            )
        }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnOk.setOnClickListener {
            onResult?.invoke(editor.buildResultBitmap())
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
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