package com.flyerpix.editor.ui.controller

import android.content.res.Resources
import android.view.View

/**
 * Satu-satunya sumber kebenaran untuk tinggi halaman detail menu bawah.
 *
 * Aturan (sesuai panduan migrasi):
 *  1. Jika canvas sudah terukur, utamakan ruang kosong di bawah tepi bawah canvas —
 *     sehingga panel dijamin TIDAK menutupi canvas.
 *  2. Fallback jika canvas belum/benar-benar menempati layar: 60% tinggi canvas,
 *     atau 35% tinggi layar bila canvas belum terukur.
 *  3. Cap keras: 50% tinggi layar.
 *  4. Penerapan selalu aman & reversible: layoutParams.height -> requestLayout ->
 *     post { requestLayout() } agar container benar-benar menghitung ulang.
 */
object PanelHeightManager {

    private const val MAX_RATIO_OF_CANVAS = 0.6f
    private const val FALLBACK_RATIO_OF_SCREEN = 0.35f
    private const val CAP_RATIO_OF_SCREEN = 0.5f

/** Jarak aman (dp) antara tepi bawah canvas dan tepi atas panel. */
    private const val SAFE_GAP_DP = 8

    /**
     * Hitung tinggi maksimum panel detail bawah.
     *
     * @param availableBelowCanvasPx ruang vertikal antara tepi bawah canvas dan tepi
     *        bawah anchor panel (dalam px). Nilai <= 0 berarti belum terukur / mentok,
     *        sehingga dipakai fallback aturan 60% canvas / 35% layar.
     * @param canvasHeightPx tinggi canvas (pixelCanvasView.height), 0 jika belum terukur.
     * @param screenHeightPx tinggi layar dalam px.
     * @param density scale factor layar.
     */
    fun safeDetailHeight(
        availableBelowCanvasPx: Int,
        canvasHeightPx: Int,
        screenHeightPx: Int,
        density: Float,
    ): Int {
        val fallback = if (canvasHeightPx > 0) {
            (canvasHeightPx * MAX_RATIO_OF_CANVAS).toInt()
        } else {
            (screenHeightPx * FALLBACK_RATIO_OF_SCREEN).toInt()
        }
        val cap = (screenHeightPx * CAP_RATIO_OF_SCREEN).toInt()
        val gap = (SAFE_GAP_DP * density).toInt()
        val fromSpace = availableBelowCanvasPx - gap
        return if (fromSpace > 0) {
            minOf(fromSpace, fallback, cap)
        } else {
            minOf(fallback, cap)
        }.coerceAtLeast(0)
    }

    /** Posisi tepi bawah [view] (mis. canvasCard) dalam koordinat [root]. */
    fun bottomInRoot(view: View, root: View): Int {
        val loc = IntArray(2)
        val rootLoc = IntArray(2)
        view.getLocationInWindow(loc)
        root.getLocationInWindow(rootLoc)
        return loc[1] - rootLoc[1] + view.height
    }

    /**
     * Posisi tepi bawah anchor untuk panel bottom-anchored dalam koordinat [root].
     *
     * @param bottomMarginPx margin bawah panel (layout bottom yang menahan panel di dasar).
     */
    fun anchorBottomInRoot(root: View, bottomMarginPx: Int): Int =
        root.height - bottomMarginPx

    /**
     * Terapkan tinggi + reflow yang aman. Tidak menyentuh tinggi bila sudah sama,
     * sehingga tidak memicu loop layout yang tidak perlu.
     */
    fun setHeight(view: View?, heightPx: Int) {
        if (view == null || heightPx < 0) return
        val lp = view.layoutParams ?: return
        if (lp.height != heightPx) {
            lp.height = heightPx
            view.layoutParams = lp
            view.requestLayout()
            view.post { view.requestLayout() }
        }
    }

    /**
     * Gelombang lengkap yang dipakai semua menu: hitung tinggi aman dari posisi
     * nyata canvas lalu terapkan ke [panel].
     *
     * @param panel panel detail yang berada di dasar layar (bottom-anchored).
     * @param root parentLayout / coordinator layout sebagai acuan koordinat.
     * @param canvasCard view yang memuat canvas (canvasCard), untuk diukur.
     * @param bottomMarginPx margin bawah panel.
     */
    fun applyCanvasAwareHeight(
        panel: View?,
        root: View,
        canvasCard: View?,
        bottomMarginPx: Int,
        screenHeightPx: Int,
        density: Float,
    ) {
        if (panel == null || root.height <= 0) return
        val canvasH = canvasCard?.height ?: 0
        var space = 0
        if (canvasCard != null && canvasH > 0) {
            val canvasBottom = bottomInRoot(canvasCard, root)
            space = anchorBottomInRoot(root, bottomMarginPx) - canvasBottom
        }
        val target = safeDetailHeight(space, canvasH, screenHeightPx, density)
        setHeight(panel, target)
    }

    /** Pembungkus [safeDetailHeight] tanpa space yang terukur (fallback murni). */
    fun fallbackHeight(canvasHeightPx: Int, screenHeightPx: Int, density: Float): Int =
        safeDetailHeight(0, canvasHeightPx, screenHeightPx, density)

    fun densityOf(res: Resources): Float = res.displayMetrics.density

    fun screenHeightPx(res: Resources): Int = res.displayMetrics.heightPixels
}