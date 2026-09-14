package com.flyerpix.editor.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FilterEngineTestExtended - JVM unit test untuk extended color adjustment
 * (Prompt 01): exposure, highlights, shadows, temperature, tint, gamma, vibrance + hue.
 *
 * Tidak memerlukan native library: test berjalan pada pure-Kotlin reference
 * [FilterReferenceImpl.colorAdjustExtended] yang men-mirror
 * ColorFilter::adjustPixelAdvanced / applyExtended di C++.
 */
class FilterEngineTestExtended {

    companion object {
        private const val WIDTH = 64
        private const val HEIGHT = 64
    }

    // ========================================================================
    // Test Utilities
    // ========================================================================

    private fun createTestPixels(width: Int = WIDTH, height: Int = HEIGHT): IntArray {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255) / width
                val g = (y * 255) / height
                val b = 128
                val a = 255
                pixels[y * width + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return pixels
    }

    private fun averageChannel(pixels: IntArray, shift: Int): Double {
        var sum = 0.0
        for (p in pixels) {
            sum += ((p ushr shift) and 0xFF)
        }
        return sum / pixels.size
    }

    private fun averageLuma(pixels: IntArray): Double {
        var sum = 0.0
        for (p in pixels) {
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            sum += 0.299 * r + 0.587 * g + 0.114 * b
        }
        return sum / pixels.size
    }

    private fun neutral(pixels: IntArray): IntArray {
        return FilterReferenceImpl.colorAdjustExtended(pixels, 0f, 0f, 0f, 0f)
    }

    // ========================================================================
    // Kompatibilitas: nilai netral = identik dengan input
    // ========================================================================

    @Test
    fun `parameter netral menghasilkan output identik dengan input`() {
        val src = createTestPixels()
        val out = neutral(src)
        assertNearIdentity(src, out, "netral")
    }

    // ========================================================================
    // Setiap parameter mengubah pixel (output vs input)
    // ========================================================================

    @Test
    fun `exposure mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = 0.5f)
        assertDiffers(src, out, "exposure")
    }

    @Test
    fun `highlights mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, highlights = 0.5f)
        assertDiffers(src, out, "highlights")
    }

    @Test
    fun `shadows mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, shadows = 0.5f)
        assertDiffers(src, out, "shadows")
    }

    @Test
    fun `temperature mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, temperature = 0.5f)
        assertDiffers(src, out, "temperature")
    }

    @Test
    fun `tint mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, tint = 0.5f)
        assertDiffers(src, out, "tint")
    }

    @Test
    fun `gamma mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, gamma = 0.5f)
        assertDiffers(src, out, "gamma")
    }

    @Test
    fun `vibrance mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, vibrance = 0.5f)
        assertDiffers(src, out, "vibrance")
    }

    @Test
    fun `hue mengubah pixel`() {
        val src = createTestPixels()
        val out = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 90f)
        assertDiffers(src, out, "hue")
    }

    // ========================================================================
    // Perilaku arah (monotonicity / warmth)
    // ========================================================================

    @Test
    fun `exposure +1 menghasilkan image lebih terang daripada exposure 0`() {
        val src = createTestPixels()
        val base = neutral(src)
        val boosted = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = 1f)
        assertTrue(
            "exposure=+1 harus lebih terang: ${averageLuma(boosted)} > ${averageLuma(base)}",
            averageLuma(boosted) > averageLuma(base)
        )
    }

    @Test
    fun `temperature +1 menghasilkan pixel lebih merah dan kurang biru`() {
        val src = createTestPixels()
        val base = neutral(src)
        val warm = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, temperature = 1f)
        assertTrue(
            "temperature=+1 harus lebih merah: ${averageChannel(warm, 16)} > ${averageChannel(base, 16)}",
            averageChannel(warm, 16) > averageChannel(base, 16)
        )
        assertTrue(
            "temperature=+1 harus kurang biru: ${averageChannel(warm, 0)} < ${averageChannel(base, 0)}",
            averageChannel(warm, 0) < averageChannel(base, 0)
        )
    }

    @Test
    fun `gamma diatas 1 terang, gamma dibawah 1 gelap`() {
        val src = createTestPixels()
        val bright = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, gamma = 2.0f)
        val dark = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, gamma = 0.5f)
        val base = neutral(src)
        assertTrue(
            "gamma > 1 harus lebih terang (midtone lift): ${averageLuma(bright)} > ${averageLuma(base)}",
            averageLuma(bright) > averageLuma(base)
        )
        assertTrue(
            "gamma < 1 harus lebih gelap: ${averageLuma(dark)} < ${averageLuma(base)}",
            averageLuma(dark) < averageLuma(base)
        )
    }

    @Test
    fun `tint +1 menambah kanal magenta (r+b) dan mengurangi hijau`() {
        val src = createTestPixels()
        val base = neutral(src)
        val magenta = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, tint = 1f)
        val baseMagenta = averageChannel(base, 16) + averageChannel(base, 0)
        val outMagenta = averageChannel(magenta, 16) + averageChannel(magenta, 0)
        assertTrue(
            "tint=+1 harus menambah r+b: $outMagenta > $baseMagenta",
            outMagenta > baseMagenta
        )
        assertTrue(
            "tint=+1 harus mengurangi hijau: ${averageChannel(magenta, 8)} < ${averageChannel(base, 8)}",
            averageChannel(magenta, 8) < averageChannel(base, 8)
        )
    }

    @Test
    fun `exposure monotonik - +0-5 lebih terang dari 0 dan +1 lebih terang dari +0-5`() {
        val src = createTestPixels()
        val base = neutral(src)
        val half = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = 0.5f)
        val full = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = 1f)
        assertTrue(averageLuma(full) > averageLuma(half))
        assertTrue(averageLuma(half) > averageLuma(base))
    }

    // ========================================================================
    // Clamping: nilai di luar rentang tidak crash dan hasil masuk akal
    // ========================================================================

    @Test
    fun `nilai out-of-range tidak crash dan hasil selalu dalam rentang channel`() {
        val src = createTestPixels()

        val extremePipelines = listOf<IntArray>(
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = 50f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = -50f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, highlights = 99f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, shadows = -99f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, temperature = 100f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, tint = -100f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, gamma = 0.001f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, gamma = 100f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, vibrance = 50f),
            FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 720f)
        )

        for (result in extremePipelines) {
            assertEquals("Ukuran output harus sama", src.size, result.size)
            for (p in result) {
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                assertTrue("Channel RGB harus dalam 0..255 (got r=$r,g=$g,b=$b)", r <= 255 && g <= 255 && b <= 255)
            }
        }
    }

    @Test
    fun `out-of-range disamakan dengan nilai clamped yang setara`() {
        val src = createTestPixels()

        val exposureClamped = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = 50f)
        val exposureNormal = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, exposure = 1f)
        assertTrue("exposure=50 harus setara dengan exposure=1", exposureClamped.contentEquals(exposureNormal))

        val gammaClamped = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, gamma = 0.0001f)
        val gammaNormal = FilterReferenceImpl.colorAdjustExtended(src, 0f, 0f, 0f, 0f, gamma = 0.1f)
        assertTrue("gamma=0.0001 harus setara dengan gamma=0.1", gammaClamped.contentEquals(gammaNormal))
    }

    // ========================================================================
    // Helper assertion
    // ========================================================================

    private fun assertDiffers(src: IntArray, out: IntArray, paramName: String) {
        var differs = false
        for (i in src.indices) {
            if (src[i] != out[i]) {
                differs = true
                break
            }
        }
        assertTrue("$paramName harus mengubah pixel (output != input)", differs)
    }

    /**
     * Verifikasi bahwa pipeline dengan seluruh parameter netral menghasilkan
     * output yang praktis identik dengan input. Karena konversi HSV<->RGB
     * menggunakan float, perbedaan maksimal 1 level per channel bisa terjadi
     * (juga berlaku pada implementasi C++ native).
     */
    private fun assertNearIdentity(src: IntArray, out: IntArray, label: String) {
        assertEquals("Ukuran output harus sama", src.size, out.size)
        var maxDiff = 0
        for (i in src.indices) {
            val rIn = (src[i] ushr 16) and 0xFF
            val gIn = (src[i] ushr 8) and 0xFF
            val bIn = src[i] and 0xFF
            val rOut = (out[i] ushr 16) and 0xFF
            val gOut = (out[i] ushr 8) and 0xFF
            val bOut = out[i] and 0xFF
            maxDiff = maxOf(
                maxDiff,
                kotlin.math.abs(rIn - rOut),
                kotlin.math.abs(gIn - gOut),
                kotlin.math.abs(bIn - bOut)
            )
        }
        assertTrue(
            "$label harus identik dalam toleransi (maxDiff=$maxDiff, ekspektasi <= 1)",
            maxDiff <= 1
        )
    }
}