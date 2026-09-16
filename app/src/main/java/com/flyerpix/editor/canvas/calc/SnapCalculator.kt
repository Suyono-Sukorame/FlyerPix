package com.flyerpix.editor.canvas.calc

/**
 * Kalkulasi murni (stateless) untuk mekanik magnetic snap-to-center
 * beserta garis panduan (Prompt 30, 43).
 */
object SnapCalculator {

    /**
     * Hasil perhitungan penempelan magnetik (Snap Guidelines).
     */
    data class SnapResult(
        val snappedX: Float,
        val snappedY: Float,
        val isSnappedX: Boolean,
        val isSnappedY: Boolean,
        val guideX: Float? = null,
        val guideY: Float? = null
    )

    /**
     * Menghitung posisi kunci magnetik (Snap Guidelines) ke titik tengah kanvas
     * secara otomatis ketika layer berada dalam rentang toleransi (Prompt 30, 43).
     *
     * @param layerX Posisi X layer saat ini.
     * @param layerY Posisi Y layer saat ini.
     * @param layerWidth Lebar unwarped layer.
     * @param layerHeight Tinggi unwarped layer.
     * @param canvasWidth Lebar kanvas atau lebar viewport.
     * @param canvasHeight Tinggi kanvas atau tinggi viewport.
     * @param tolerance Jarak toleransi snap (misalnya 5dp dalam piksel).
     * @param canvasCenterX Titik tengah horizontal target snap (default: canvasWidth / 2f).
     * @param canvasCenterY Titik tengah vertikal target snap (default: canvasHeight / 2f).
     * @return [SnapResult] yang berisi koordinat ter-snap dan flag apakah snap aktif.
     */
    fun calculate(
        layerX: Float,
        layerY: Float,
        layerWidth: Float,
        layerHeight: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        tolerance: Float,
        canvasCenterX: Float = canvasWidth / 2f,
        canvasCenterY: Float = canvasHeight / 2f
    ): SnapResult {
        val layerCenterX = layerX + layerWidth / 2f
        val layerCenterY = layerY + layerHeight / 2f

        val snapX = kotlin.math.abs(layerCenterX - canvasCenterX) <= tolerance
        val snapY = kotlin.math.abs(layerCenterY - canvasCenterY) <= tolerance

        val newX = if (snapX) canvasCenterX - layerWidth / 2f else layerX
        val newY = if (snapY) canvasCenterY - layerHeight / 2f else layerY

        return SnapResult(newX, newY, snapX, snapY)
    }

    /**
     * Menghitung snap berdasarkan bounding box aktual layer, sehingga akurat untuk
     * layer yang sudah diskalakan atau dirotasi.
     *
     * Target magnet mencakup tepi kanvas kiri/kanan/atas/bawah, garis tengah kanvas,
     * serta tepi/tengah layer lain ([peerXTargets]/[peerYTargets]). Toleransi tepi
     * ([edgeTolerance]) default sama dengan [tolerance] agar pemanggil lama tetap
     * kompatibel, tetapi UI memakai toleransi tepi yang lebih besar supaya magnet
     * sisi terasa sama kuatnya dengan magnet tengah.
     */
    fun calculateWithEdges(
        layerX: Float,
        layerY: Float,
        boundsLeft: Float,
        boundsTop: Float,
        boundsRight: Float,
        boundsBottom: Float,
        tolerance: Float,
        canvasLeft: Float,
        canvasTop: Float,
        canvasRight: Float,
        canvasBottom: Float,
        edgeTolerance: Float = tolerance,
        peerXTargets: FloatArray = FloatArray(0),
        peerYTargets: FloatArray = FloatArray(0)
    ): SnapResult {
        val boundsCenterX = (boundsLeft + boundsRight) / 2f
        val boundsCenterY = (boundsTop + boundsBottom) / 2f
        val canvasCenterX = (canvasLeft + canvasRight) / 2f
        val canvasCenterY = (canvasTop + canvasBottom) / 2f

        // Tepi kanvas tidak lagi ditawarkan bila layer lebih besar dari kanvas pada
        // sumbu tersebut, karena menempelkan satu tepi justru melepas tepi lainnya.
        val allowCanvasEdgeX = (boundsRight - boundsLeft) <= (canvasRight - canvasLeft)
        val allowCanvasEdgeY = (boundsBottom - boundsTop) <= (canvasBottom - canvasTop)

        val snapX = resolveAxisSnap(
            boundsStart = boundsLeft,
            boundsCenter = boundsCenterX,
            boundsEnd = boundsRight,
            canvasStart = canvasLeft,
            canvasCenter = canvasCenterX,
            canvasEnd = canvasRight,
            allowCanvasEdge = allowCanvasEdgeX,
            centerTolerance = tolerance,
            edgeTolerance = edgeTolerance,
            peerTargets = peerXTargets
        )
        val snapY = resolveAxisSnap(
            boundsStart = boundsTop,
            boundsCenter = boundsCenterY,
            boundsEnd = boundsBottom,
            canvasStart = canvasTop,
            canvasCenter = canvasCenterY,
            canvasEnd = canvasBottom,
            allowCanvasEdge = allowCanvasEdgeY,
            centerTolerance = tolerance,
            edgeTolerance = edgeTolerance,
            peerTargets = peerYTargets
        )

        return SnapResult(
            snappedX = layerX + (snapX?.delta ?: 0f),
            snappedY = layerY + (snapY?.delta ?: 0f),
            isSnappedX = snapX != null,
            isSnappedY = snapY != null,
            guideX = snapX?.target,
            guideY = snapY?.target
        )
    }

    private data class SnapDelta(val delta: Float, val target: Float)

    /**
     * Memilih target snap terdekat pada satu sumbu.
     *
     * Tepi kanvas & layer lain dievaluasi lebih dulu sehingga menang saat jaraknya
     * seri dengan garis tengah kanvas; garis tengah hanya terpakai bila benar-benar
     * lebih dekat (mencegah center "merampas" magnet tepi).
     */
    private fun resolveAxisSnap(
        boundsStart: Float,
        boundsCenter: Float,
        boundsEnd: Float,
        canvasStart: Float,
        canvasCenter: Float,
        canvasEnd: Float,
        allowCanvasEdge: Boolean,
        centerTolerance: Float,
        edgeTolerance: Float,
        peerTargets: FloatArray
    ): SnapDelta? {
        var best: SnapDelta? = null
        var bestDistance = Float.POSITIVE_INFINITY

        fun consider(source: Float, target: Float, limit: Float) {
            val delta = target - source
            val distance = kotlin.math.abs(delta)
            if (distance <= limit && distance < bestDistance) {
                best = SnapDelta(delta, target)
                bestDistance = distance
            }
        }

        if (allowCanvasEdge) {
            consider(boundsStart, canvasStart, edgeTolerance)
            consider(boundsEnd, canvasEnd, edgeTolerance)
        }
        for (peer in peerTargets) {
            if (peer.isFinite()) {
                consider(boundsStart, peer, edgeTolerance)
                consider(boundsCenter, peer, edgeTolerance)
                consider(boundsEnd, peer, edgeTolerance)
            }
        }

        consider(boundsCenter, canvasCenter, centerTolerance)

        return best
    }
}
