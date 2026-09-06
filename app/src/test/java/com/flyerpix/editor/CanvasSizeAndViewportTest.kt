package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.CanvasSizePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk preset rasio ukuran kanvas dan perhitungan viewport (Prompt 43).
 *
 * Memverifikasi:
 * 1. Definisi preset ukuran standar (1:1, YouTube Thumbnail 16:9, YouTube Banner, Facebook Cover, Custom).
 * 2. Perhitungan rasio aspek dan penyederhanaan FPB (GCD).
 * 3. Perhitungan viewportRect proporsional (letterbox & pillarbox) pada PixelCanvasView.
 * 4. Penyesuaian batas toleransi dan koordinat magnetik snap-to-center berbasis viewport center.
 */
class CanvasSizeAndViewportTest {

    // ── 1. Uji Model CanvasSizePreset ────────────────────────────────────────

    @Test
    fun `standard presets are defined correctly`() {
        val presets = CanvasSizePreset.PRESETS
        assertTrue("Presets must have at least 5 options", presets.size >= 5)

        val square = presets.firstOrNull { it.name.contains("1:1") }
        assertNotNull("1:1 square preset must exist", square)
        assertEquals(1080, square!!.width)
        assertEquals(1080, square.height)

        val ytThumbnail = presets.firstOrNull { it.name.contains("YouTube Thumbnail") }
        assertNotNull("YouTube Thumbnail preset must exist", ytThumbnail)
        assertEquals(1280, ytThumbnail!!.width)
        assertEquals(720, ytThumbnail.height)

        val ytBanner = presets.firstOrNull { it.name.contains("YouTube Channel Banner") }
        assertNotNull("YouTube Channel Banner preset must exist", ytBanner)
        assertEquals(2560, ytBanner!!.width)
        assertEquals(1440, ytBanner.height)

        val fbCover = presets.firstOrNull { it.name.contains("Facebook Cover") }
        assertNotNull("Facebook Cover preset must exist", fbCover)
        assertEquals(820, fbCover!!.width)
        assertEquals(312, fbCover.height)

        val custom = presets.firstOrNull { it.name.contains("Custom") }
        assertNotNull("Custom preset must exist", custom)
    }

    @Test
    fun `aspect ratio calculation and formatting work correctly`() {
        assertEquals("1:1", CanvasSizePreset.getSimplifiedRatio(1080, 1080))
        assertEquals("16:9", CanvasSizePreset.getSimplifiedRatio(1280, 720))
        assertEquals("16:9", CanvasSizePreset.getSimplifiedRatio(1920, 1080))
        assertEquals("16:9", CanvasSizePreset.getSimplifiedRatio(2560, 1440))
        assertEquals("9:16", CanvasSizePreset.getSimplifiedRatio(1080, 1920))
        assertEquals("4:3", CanvasSizePreset.getSimplifiedRatio(1600, 1200))
        assertEquals("205:78", CanvasSizePreset.getSimplifiedRatio(820, 312))
    }

    @Test
    fun `findMatchingPreset identifies preset by resolution`() {
        val preset1080 = CanvasSizePreset.findMatchingPreset(1080, 1080)
        assertNotNull(preset1080)
        assertTrue(preset1080!!.name.contains("1:1"))

        val presetYT = CanvasSizePreset.findMatchingPreset(1280, 720)
        assertNotNull(presetYT)
        assertTrue(presetYT!!.name.contains("Thumbnail"))

        val unknown = CanvasSizePreset.findMatchingPreset(999, 888)
        assertEquals(null, unknown)
    }

    // ── 2. Uji Perhitungan ViewportRect ─────────────────────────────────────

    @Test
    fun `square canvas in square view fills view completely`() {
        val viewW = 1000
        val viewH = 1000
        val canvasW = 1080
        val canvasH = 1080

        val vp = PixelCanvasView.calculateViewportRect(viewW, viewH, canvasW, canvasH)

        assertEquals(0f, vp.left, 0.001f)
        assertEquals(0f, vp.top, 0.001f)
        assertEquals(1000f, vp.right, 0.001f)
        assertEquals(1000f, vp.bottom, 0.001f)
        assertEquals(1000f, vp.right - vp.left, 0.001f)
        assertEquals(1000f, vp.bottom - vp.top, 0.001f)
    }

    @Test
    fun `16 by 9 widescreen canvas in square view is letterboxed vertically`() {
        val viewW = 1000
        val viewH = 1000
        val canvasW = 1600
        val canvasH = 900 // Rasio 16:9

        val vp = PixelCanvasView.calculateViewportRect(viewW, viewH, canvasW, canvasH)

        // Lebar penuh (1000), tinggi = 1000 / (16/9) = 562.5
        assertEquals(0f, vp.left, 0.001f)
        assertEquals(1000f, vp.right, 0.001f)
        assertEquals(1000f, vp.right - vp.left, 0.001f)

        assertEquals(562.5f, vp.bottom - vp.top, 0.01f)
        val expectedTop = (1000f - 562.5f) / 2f // 218.75
        assertEquals(expectedTop, vp.top, 0.01f)
        assertEquals(expectedTop + 562.5f, vp.bottom, 0.01f)
    }

    @Test
    fun `9 by 16 portrait canvas in square view is pillarboxed horizontally`() {
        val viewW = 1000
        val viewH = 1000
        val canvasW = 900
        val canvasH = 1600 // Rasio 9:16

        val vp = PixelCanvasView.calculateViewportRect(viewW, viewH, canvasW, canvasH)

        // Tinggi penuh (1000), lebar = 1000 * (9/16) = 562.5
        assertEquals(0f, vp.top, 0.001f)
        assertEquals(1000f, vp.bottom, 0.001f)
        assertEquals(1000f, vp.bottom - vp.top, 0.001f)

        assertEquals(562.5f, vp.right - vp.left, 0.01f)
        val expectedLeft = (1000f - 562.5f) / 2f // 218.75
        assertEquals(expectedLeft, vp.left, 0.01f)
        assertEquals(expectedLeft + 562.5f, vp.right, 0.01f)
    }

    @Test
    fun `invalid or zero dimensions return empty viewport rect`() {
        val vpZero = PixelCanvasView.calculateViewportRect(0, 1000, 1080, 1080)
        assertEquals(0f, vpZero.right - vpZero.left, 0.001f)
        assertEquals(0f, vpZero.bottom - vpZero.top, 0.001f)

        val vpNegative = PixelCanvasView.calculateViewportRect(1000, -500, 1080, 1080)
        assertEquals(0f, vpNegative.right - vpNegative.left, 0.001f)
        assertEquals(0f, vpNegative.bottom - vpNegative.top, 0.001f)
    }

    // ── 3. Uji Snap-to-Center dengan Custom Center (Viewport) ───────────────

    @Test
    fun `snap-to-center respects custom viewport center coordinates`() {
        // Misalkan kanvas letterbox memiliki pusat viewport di X = 500, Y = 300
        val viewportCenterX = 500f
        val viewportCenterY = 300f
        val layerW = 100f
        val layerH = 100f
        val tolerance = 10f

        // Layer ditempatkan di dekat pusat viewport: X = 452 (center = 502, delta = 2 <= 10)
        // Y = 251 (center = 301, delta = 1 <= 10)
        val snapResult = PixelCanvasView.calculateSnapToCenter(
            layerX = 452f,
            layerY = 251f,
            layerWidth = layerW,
            layerHeight = layerH,
            canvasWidth = 1000f,
            canvasHeight = 600f,
            tolerance = tolerance,
            canvasCenterX = viewportCenterX,
            canvasCenterY = viewportCenterY
        )

        assertTrue(snapResult.isSnappedX)
        assertTrue(snapResult.isSnappedY)
        // Posisi baru harus tepat mengunci ke tengah viewport: 500 - 50 = 450, 300 - 50 = 250
        assertEquals(450f, snapResult.snappedX, 0.001f)
        assertEquals(250f, snapResult.snappedY, 0.001f)
    }
}
