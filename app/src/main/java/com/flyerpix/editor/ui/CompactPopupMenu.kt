package com.flyerpix.editor.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.flyerpix.editor.R

/**
 * Popup menu ringkas berbasis [PopupWindow] untuk menu top-bar (`+` dan ⋮).
 * Item dibuat dengan row height padat (~38dp) — lebih ringkas daripada
 * PopupMenu AndroidX yang memaksa row 48dp di layout internal.
 */
class CompactPopupMenu(
    context: Context,
    private val anchor: View,
    private val items: List<String>,
    private val onClick: (index: Int, label: String) -> Unit
) {

    private val density = context.resources.displayMetrics.density

    private val popup: PopupWindow = run {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = 4 * density
            }
            elevation = 8 * density
        }

        val inflater = LayoutInflater.from(context)
        var maxWidth = 0
        // popup di-assign setelah daftar item selesai; referensi ke popup di
        // dalam listener memakai popupRef sehingga kompiler tahu aman.
        var popupRef: PopupWindow? = null
        items.forEachIndexed { index, label ->
            val row = inflater.inflate(R.layout.view_compact_menu_item, container, false) as TextView
            row.text = label
            row.setOnClickListener {
                popupRef?.dismiss()
                onClick(index, label)
            }
            // ukur lebar terpanjang untuk memberik lebar popup yang pas
            row.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            maxWidth = maxOf(maxWidth, row.measuredWidth)
            container.addView(row)
        }

        PopupWindow(
            container,
            maxWidth + 8 * density.toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 8 * density
            popupRef = this
        }
    }

    fun show() {
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorCenterX = location[0] + anchor.width / 2
        var x = anchorCenterX - popup.width / 2
        // jaga popup tetap di dalam layar
        val screenWidth = anchor.resources.displayMetrics.widthPixels
        val margin = (4 * density).toInt()
        x = x.coerceIn(margin, screenWidth - popup.width - margin)
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, location[1] + anchor.height + 4)
    }
}