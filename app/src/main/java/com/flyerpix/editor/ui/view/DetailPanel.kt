package com.flyerpix.editor.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.flyerpix.editor.R

/**
 * Komponen page detail reusable untuk SEMUA menu editor (Effect Settings, Shape
 * Settings, Text Property, dst.).
 *
 * Menyediakan: drag handle + header (judul + tombol ✓ / ✕ opsional) + area konten
 * scrollable. Seluruh child yang dideklarasikan di XML di bawah instance komponen
 * ini otomatis dipindah ke dalam area konten, sehingga pemakaian tampil seperti:
 *
 * <com.flyerpix.editor.ui.view.DetailPanel android:id="@+id/effectSettingsPanel" ...>
 *     <include layout="@layout/layout_position_controls" android:id="@+id/positionControlsInclude" />
 * </com.flyerpix.editor.ui.view.DetailPanel>
 *
 * Semua pengaturan tinggi dinamis (agar tidak menutupi canvas) didelegasikan ke
 * PanelHeightManager agar penyetelan tinggi hanya ada di SATU tempat.
 */
class DetailPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    val handleView: View
    val headerView: View
    val titleView: TextView
    val applyButton: ImageButton
    val cancelButton: ImageButton
    val scrollView: ScrollView
    val contentContainer: LinearLayout

    private val internalChildren = mutableListOf<View>()

    // Menandai konstruktor sudah selesai. Inflasi <merge> di dalam konstruktor
    // memicu onFinishInflate lebih awal (saat field belum terisi), jadi kita
    // harus menunda pemindahan child sampai semuanya siap.
    private var ready = false

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_detail_panel, this, true)
        handleView = findViewById(R.id.detailPanelHandle)
        headerView = findViewById(R.id.detailPanelHeader)
        titleView = findViewById(R.id.detailPanelTitle)
        applyButton = findViewById(R.id.detailPanelApply)
        cancelButton = findViewById(R.id.detailPanelCancel)
        scrollView = findViewById(R.id.detailPanelScroll)
        contentContainer = findViewById(R.id.detailPanelContent)
        internalChildren.addAll(
            listOf(handleView, headerView, scrollView).filterNotNull()
        )
        ready = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (!ready) return
        // Pindahkan semua child hasil XML (selain bagian internal shell) ke area konten.
        val content = contentContainer
        val children = (0 until childCount).map { getChildAt(it) }.toMutableList()
        for (child in children) {
            if (child in internalChildren) continue
            removeView(child)
            content.addView(child)
        }
    }

    /** Ganti judul header panel. */
    fun setTitle(text: String) {
        titleView.text = text
    }

    /** Sembunyikan/tampilkan tombol ✓ dan ✕ (untuk panel tanpa aksi confirm/cancel). */
    fun showConfirmActions(show: Boolean) {
        val v = if (show) View.VISIBLE else View.GONE
        applyButton.visibility = v
        cancelButton.visibility = v
    }
}