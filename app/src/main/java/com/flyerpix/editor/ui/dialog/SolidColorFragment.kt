package com.flyerpix.editor.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment

/**
 * Tab Solid Color — membungkus [SolidColorEditorView] yang modern:
 * slider HSV + alpha, hex editor, preset, dan warna terakhir (recent).
 */
class SolidColorFragment : Fragment() {

    private var editorView: SolidColorEditorView? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val scroll = ScrollView(requireContext()).apply {
            isFillViewport = true
            val editor = SolidColorEditorView(context)
            editorView = editor
            addView(editor, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val initColor = arguments?.getInt(ARG_COLOR, Color.WHITE) ?: Color.WHITE
        editorView?.setInitialColor(initColor)
    }

    /** Warna solid terpilih — dipanggil saat dialog OK. */
    fun getSelectedColor(): Int {
        val color = editorView?.getSelectedColor() ?: Color.WHITE
        editorView?.commitCurrentColorToRecents()
        return color
    }

    companion object {
        private const val ARG_COLOR = "initial_color"

        fun newInstance(initialColor: Int): SolidColorFragment {
            return SolidColorFragment().apply {
                arguments = Bundle().apply { putInt(ARG_COLOR, initialColor) }
            }
        }
    }
}