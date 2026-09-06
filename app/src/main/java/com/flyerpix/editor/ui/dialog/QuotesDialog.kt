package com.flyerpix.editor.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.flyerpix.editor.R

/**
 * Dialog pemilihan kutipan (Quotes) otentik PixelLab.
 */
class QuotesDialog : DialogFragment() {

    var onQuoteSelected: ((String) -> Unit)? = null

    private val quotesList = listOf(
        "Dream big and dare to fail.",
        "Your limitation—it's only your imagination.",
        "Push yourself, because no one else is going to do it for you.",
        "Great things never come from comfort zones.",
        "Success doesn't just find you. You have to go out and get it.",
        "The harder you work for something, the greater you'll feel when you achieve it.",
        "Don't stop when you're tired. Stop when you're done.",
        "Wake up with determination. Go to bed with satisfaction.",
        "Do something today that your future self will thank you for.",
        "Little things make big days.",
        "It's going to be hard, but hard does not mean impossible.",
        "Stay positive, work hard, make it happen.",
        "Action is the foundational key to all success.",
        "Believe you can and you're halfway there."
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listView = ListView(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                quotesList
            )
            setOnItemClickListener { _, _, position, _ ->
                val selectedQuote = quotesList[position]
                onQuoteSelected?.invoke(selectedQuote)
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih Kutipan (Quotes)")
            .setView(listView)
            .setNegativeButton("Tutup", null)
            .create()
    }

    companion object {
        const val TAG = "QuotesDialog"

        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            onQuoteSelected: (String) -> Unit
        ): QuotesDialog {
            val dialog = QuotesDialog()
            dialog.onQuoteSelected = onQuoteSelected
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
