package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.ExportFormat
import com.flyerpix.editor.canvas.model.ExportQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit test untuk ExportQuality dan ExportFormat pada Ultra HD / 4K Exporter (Prompt 49).
 */
class ExportHighResolutionTest {

    @Test
    fun testExportQualityDefaultSquareCanvas() {
        // 1:1 Square (1080 x 1080)
        val (w, h) = ExportQuality.DEFAULT.calculateDimensions(1080, 1080)
        assertEquals(1080, w)
        assertEquals(1080, h)
    }

    @Test
    fun testExportQualityUltraHD4KResolution() {
        // Kanvas aspek rasio 16:9 dasar (960 x 540) pada Ultra HD 4.0x -> 3840 x 2160 4K
        val (w, h) = ExportQuality.ULTRA_HD.calculateDimensions(960, 540)
        assertEquals(3840, w)
        assertEquals(2160, h)
    }

    @Test
    fun testExportQualityMultipliers() {
        val baseW = 1000
        val baseH = 800

        val (wLow, hLow) = ExportQuality.LOW.calculateDimensions(baseW, baseH)
        assertEquals(500, wLow)
        assertEquals(400, hLow)

        val (wMed, hMed) = ExportQuality.MEDIUM.calculateDimensions(baseW, baseH)
        assertEquals(750, wMed)
        assertEquals(600, hMed)

        val (wHigh, hHigh) = ExportQuality.HIGH.calculateDimensions(baseW, baseH)
        assertEquals(1500, wHigh)
        assertEquals(1200, hHigh)

        val (wVeryHigh, hVeryHigh) = ExportQuality.VERY_HIGH.calculateDimensions(baseW, baseH)
        assertEquals(2000, wVeryHigh)
        assertEquals(1600, hVeryHigh)
    }

    @Test
    fun testExportQualityClampingBounds() {
        // Uji batas minimum 50px
        val (minW, minH) = ExportQuality.LOW.calculateDimensions(20, 20)
        assertEquals(50, minW)
        assertEquals(50, minH)

        // Uji batas maksimum 8192px
        val (maxW, maxH) = ExportQuality.ULTRA_HD.calculateDimensions(4000, 3000)
        assertEquals(8192, maxW)
        assertEquals(8192, maxH)
    }

    @Test
    fun testExportFormatAttributes() {
        assertEquals("png", ExportFormat.PNG.extension)
        assertEquals("image/png", ExportFormat.PNG.mimeType)

        assertEquals("jpg", ExportFormat.JPEG.extension)
        assertEquals("image/jpeg", ExportFormat.JPEG.mimeType)
    }

    @Test
    fun testFromOrdinal() {
        assertEquals(ExportQuality.DEFAULT, ExportQuality.fromOrdinal(0))
        assertEquals(ExportQuality.ULTRA_HD, ExportQuality.fromOrdinal(5))
        assertEquals(ExportQuality.DEFAULT, ExportQuality.fromOrdinal(999)) // fallback
    }
}
