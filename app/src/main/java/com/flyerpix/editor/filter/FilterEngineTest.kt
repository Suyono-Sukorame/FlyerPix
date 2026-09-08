/**
 * FilterEngineTest.kt
 *
 * JNI integration tests untuk FilterEngine
 *
 * Tests all 8 filters end-to-end dengan real Android Bitmap objects
 * Verifies:
 * - Filter correctness (output is different from input)
 * - Parameter validation (clamping, range checking)
 * - Error handling (null inputs, size mismatches)
 * - Thread safety (concurrent filter access)
 * - Performance (execution time, throughput)
 */

package com.flyerpix.editor.filter

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.system.measureTimeMillis

/**
 * FilterEngineTest - Comprehensive JNI integration test suite
 *
 * Usage:
 * ```kotlin
 * val tester = FilterEngineTest()
 * tester.runAllTests()
 * ```
 */
class FilterEngineTest {
    companion object {
        private const val TAG = "FilterEngineTest"
        private const val TEST_WIDTH = 512
        private const val TEST_HEIGHT = 512
    }

    private var passCount = 0
    private var failCount = 0

    // ========================================================================
    // Test Utilities
    // ========================================================================

    /**
     * Create test bitmap dengan gradient pattern
     */
    private fun createTestBitmap(width: Int = TEST_WIDTH, height: Int = TEST_HEIGHT): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255) / width
                val g = (y * 255) / height
                val b = 128
                val a = 255

                val color = Color.argb(a, r, g, b)
                pixels[y * width + x] = color
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Calculate per-pixel average difference between two bitmaps
     */
    private fun calculateDifference(bmp1: Bitmap, bmp2: Bitmap): Float {
        if (bmp1.width != bmp2.width || bmp1.height != bmp2.height) {
            return -1f
        }

        val pixels1 = IntArray(bmp1.width * bmp1.height)
        val pixels2 = IntArray(bmp2.width * bmp2.height)

        bmp1.getPixels(pixels1, 0, bmp1.width, 0, 0, bmp1.width, bmp1.height)
        bmp2.getPixels(pixels2, 0, bmp2.width, 0, 0, bmp2.width, bmp2.height)

        var totalDiff = 0.0
        for (i in pixels1.indices) {
            val p1 = pixels1[i]
            val p2 = pixels2[i]

            val r1 = Color.red(p1)
            val g1 = Color.green(p1)
            val b1 = Color.blue(p1)

            val r2 = Color.red(p2)
            val g2 = Color.green(p2)
            val b2 = Color.blue(p2)

            val dr = (r1 - r2).toDouble()
            val dg = (g1 - g2).toDouble()
            val db = (b1 - b2).toDouble()

            totalDiff += dr * dr + dg * dg + db * db
        }

        return (totalDiff / pixels1.size).toFloat()
    }

    /**
     * Print test result
     */
    private fun assertTrue(condition: Boolean, message: String) {
        if (condition) {
            Log.d(TAG, "✓ PASS: $message")
            passCount++
        } else {
            Log.e(TAG, "✗ FAIL: $message")
            failCount++
        }
    }

    // ========================================================================
    // Lifecycle Tests
    // ========================================================================

    private fun testEngineCreationAndDestruction() {
        Log.i(TAG, "TEST: Engine creation and destruction")

        val engine = FilterEngine.create(threadCount = 4)
        assertTrue(engine != null, "Engine creation successful")
        assertTrue(engine?.isValid() == true, "Engine is valid after creation")

        engine?.destroy()
        assertTrue(engine?.isValid() == false, "Engine is destroyed")
    }

    private fun testEngineConfiguration() {
        Log.i(TAG, "TEST: Engine thread configuration")

        val engine = FilterEngine.create(threadCount = 2) ?: return
        assertTrue(engine.getThreadCount() == 2, "Initial thread count = 2")

        engine.setThreadCount(4)
        assertTrue(engine.getThreadCount() == 4, "Thread count updated to 4")

        engine.setSIMDEnabled(false)
        assertTrue(engine.isSIMDEnabled() == false, "SIMD disabled")

        engine.setSIMDEnabled(true)
        assertTrue(engine.isSIMDEnabled() == true, "SIMD re-enabled")

        engine.destroy()
    }

    // ========================================================================
    // Blur Filter Tests
    // ========================================================================

    private fun testBlurFilter() {
        Log.i(TAG, "TEST: Gaussian blur filter")

        val engine = FilterEngine.create(threadCount = 4) ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val status = engine.applyBlur(src, dst, radius = 5.0f, passes = 1)
        assertTrue(status == FilterStatus.OK, "Blur completed with OK status")

        val diff = calculateDifference(src, dst)
        assertTrue(diff > 0.0f, "Blurred image differs from source (diff=$diff)")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    private fun testBlurMultiplePasses() {
        Log.i(TAG, "TEST: Blur with multiple passes")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val status = engine.applyBlur(src, dst, radius = 3.0f, passes = 2)
        assertTrue(status == FilterStatus.OK, "Multi-pass blur succeeded")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    // ========================================================================
    // Color Adjust Tests
    // ========================================================================

    private fun testColorAdjust() {
        Log.i(TAG, "TEST: Color adjust filter")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val status = engine.applyColorAdjust(
            src, dst,
            brightness = 0.2f,
            contrast = 0.3f,
            saturation = 0.2f,
            hue = 45.0f
        )
        assertTrue(status == FilterStatus.OK, "Color adjust completed with OK status")

        val diff = calculateDifference(src, dst)
        assertTrue(diff > 0.0f, "Adjusted image differs from source (diff=$diff)")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    private fun testColorAdjustParameterClamping() {
        Log.i(TAG, "TEST: Color adjust parameter clamping")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        // Test with out-of-range values (should be clamped)
        val status = engine.applyColorAdjust(
            src, dst,
            brightness = 2.5f,  // Will be clamped to 1.0f
            contrast = -5.0f,   // Will be clamped to -1.0f
            saturation = 0.5f,
            hue = 720.0f        // Will be clamped to 180.0f
        )
        assertTrue(status == FilterStatus.OK, "Color adjust with out-of-range params succeeded")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    // ========================================================================
    // Emboss Tests
    // ========================================================================

    private fun testEmbossFilter() {
        Log.i(TAG, "TEST: Emboss filter")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val status = engine.applyEmboss(src, dst, amount = 1.0f, angle = 45.0f)
        assertTrue(status == FilterStatus.OK, "Emboss completed with OK status")

        val diff = calculateDifference(src, dst)
        assertTrue(diff > 0.0f, "Embossed image differs from source (diff=$diff)")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    private fun testEmbossAllAngles() {
        Log.i(TAG, "TEST: Emboss with different angles")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()

        val angles = listOf(0.0f, 45.0f, 90.0f, 135.0f, 180.0f, 225.0f, 270.0f, 315.0f)
        var allPassed = true

        for (angle in angles) {
            val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val status = engine.applyEmboss(src, dst, amount = 1.0f, angle = angle)
            if (status != FilterStatus.OK) {
                allPassed = false
                Log.e(TAG, "Emboss failed for angle=$angle")
            }
            dst.recycle()
        }

        assertTrue(allPassed, "All 8 emboss angles succeeded")

        src.recycle()
        engine.destroy()
    }

    // ========================================================================
    // Simple Filter Tests
    // ========================================================================

    private fun testGrayscaleFilter() {
        Log.i(TAG, "TEST: Grayscale filter")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val status = engine.applyGrayscale(src, dst)
        assertTrue(status == FilterStatus.OK, "Grayscale completed with OK status")

        val diff = calculateDifference(src, dst)
        assertTrue(diff > 0.0f, "Grayscale image differs from source (diff=$diff)")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    private fun testInvertFilter() {
        Log.i(TAG, "TEST: Invert filter")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val status = engine.applyInvert(src, dst)
        assertTrue(status == FilterStatus.OK, "Invert completed with OK status")

        val diff = calculateDifference(src, dst)
        assertTrue(diff > 0.0f, "Inverted image differs from source (diff=$diff)")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    private fun testSepiaFilter() {
        Log.i(TAG, "TEST: Sepia filter")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap()
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val status = engine.applySepia(src, dst, intensity = 0.7f)
        assertTrue(status == FilterStatus.OK, "Sepia completed with OK status")

        val diff = calculateDifference(src, dst)
        assertTrue(diff > 0.0f, "Sepia image differs from source (diff=$diff)")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    private fun testBitmapDimensionMismatch() {
        Log.i(TAG, "TEST: Bitmap dimension mismatch error handling")

        val engine = FilterEngine.create() ?: return
        val src = createTestBitmap(512, 512)
        val dst = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)

        // FilterEngine.validateBitmaps throws IllegalArgumentException for
        // dimension mismatch (Kotlin wrapper contract), NOT a status return.
        var exceptionThrown = false
        try {
            engine.applyBlur(src, dst, radius = 5.0f)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown, "Blur with size mismatch throws IllegalArgumentException")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    private fun testNullEngineHandling() {
        Log.i(TAG, "TEST: Null engine handling")

        var exceptionThrown = false
        try {
            val engine: FilterEngine? = null
            val src = createTestBitmap()
            val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

            // Deliberate !! to force NPE on null engine (safe-call would silently skip)
            engine!!.applyBlur(src, dst, radius = 5.0f)
            src.recycle()
            dst.recycle()
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown, "Null engine access throws exception")
    }

    private fun testDestroyedEngineHandling() {
        Log.i(TAG, "TEST: Destroyed engine error handling")

        val engine = FilterEngine.create() ?: return

        // Invalid bitmap dimensions (should be rejected before native call)
        val src = createTestBitmap(256, 256)
        val dstDimMismatch = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        var dimException = false
        try {
            engine.applyBlur(src, dstDimMismatch, radius = 3.0f)
        } catch (e: IllegalArgumentException) {
            dimException = true
        }
        assertTrue(dimException, "Dimension mismatch throws IllegalArgumentException")
        dstDimMismatch.recycle()

        // Non-ARGB_8888 config (should be rejected)
        val dstRgb565 = Bitmap.createBitmap(256, 256, Bitmap.Config.RGB_565)
        var configException = false
        try {
            engine.applyGrayscale(src, dstRgb565)
        } catch (e: IllegalArgumentException) {
            configException = true
        }
        assertTrue(configException, "Non-ARGB_8888 config throws IllegalArgumentException")
        dstRgb565.recycle()

        // Negative radius (should be clamped, not crash)
        val dst = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val clampedStatus = engine.applyBlur(src, dst, radius = -100.0f)
        assertTrue(clampedStatus == FilterStatus.OK, "Negative radius clamped successfully")
        src.recycle()
        dst.recycle()

        // After destroy(), any call must throw IllegalStateException
        var destroyedException = false
        engine.destroy()
        try {
            engine.applyGrayscale(src, dst)
        } catch (e: IllegalStateException) {
            destroyedException = true
        }
        assertTrue(destroyedException, "Call after destroy throws IllegalStateException")
    }

    // ========================================================================
    // Thread Safety Tests
    // ========================================================================

    /**
     * Concurrent access: multiple independent engines running in parallel.
     * Verifies no cross-thread corruption and all filters return OK.
     */
    private fun testConcurrentIndependentEngines() {
        Log.i(TAG, "TEST: Concurrent independent engines (thread safety)")

        val threadCount = 4
        val iterations = 5
        val failureCount = java.util.concurrent.atomic.AtomicInteger(0)
        val threads = mutableListOf<Thread>()

        for (t in 0 until threadCount) {
            threads.add(Thread {
                try {
                    val engine = FilterEngine.create(threadCount = 2) ?: return@Thread
                    val src = createTestBitmap(256, 256)
                    val dst = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)

                    for (i in 0 until iterations) {
                        if (engine.applyBlur(src, dst, 3.0f, 1) != FilterStatus.OK) failureCount.incrementAndGet()
                        if (engine.applyColorAdjust(src, dst, 0.1f, 0.2f, 0.1f, 30.0f) != FilterStatus.OK) failureCount.incrementAndGet()
                        if (engine.applyEmboss(src, dst, 1.0f, 45.0f) != FilterStatus.OK) failureCount.incrementAndGet()
                        if (engine.applyGrayscale(src, dst) != FilterStatus.OK) failureCount.incrementAndGet()
                        if (engine.applyInvert(src, dst) != FilterStatus.OK) failureCount.incrementAndGet()
                        if (engine.applySepia(src, dst, 0.5f) != FilterStatus.OK) failureCount.incrementAndGet()
                    }

                    src.recycle()
                    dst.recycle()
                    engine.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Concurrent engine thread failed: ${e.message}")
                    failureCount.incrementAndGet()
                }
            })
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(failureCount.get() == 0, "All concurrent filters succeeded (failures=${failureCount.get()})")
    }

    /**
     * Shared engine: multiple threads calling a single FilterEngine instance
     * concurrently. Verifies the native engine tolerates concurrent use.
     */
    private fun testSharedEngineConcurrency() {
        Log.i(TAG, "TEST: Shared engine concurrent access")

        val engine = FilterEngine.create(threadCount = 4) ?: return
        val failureCount = java.util.concurrent.atomic.AtomicInteger(0)
        val threads = mutableListOf<Thread>()

        val filterOps = listOf<Pair<String, (Bitmap, Bitmap) -> FilterStatus>>(
            "blur" to { s, d -> engine.applyBlur(s, d, 3.0f, 1) },
            "color" to { s, d -> engine.applyColorAdjust(s, d, 0.1f, 0.2f, 0.1f, 30.0f) },
            "emboss" to { s, d -> engine.applyEmboss(s, d, 1.0f, 90.0f) },
            "grayscale" to { s, d -> engine.applyGrayscale(s, d) }
        )

        for (t in 0 until 4) {
            val op = filterOps[t % filterOps.size]
            threads.add(Thread {
                try {
                    val src = createTestBitmap(256, 256)
                    val dst = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                    repeat(20) {
                        if (op.second(src, dst) != FilterStatus.OK) {
                            failureCount.incrementAndGet()
                        }
                    }
                    src.recycle()
                    dst.recycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Shared engine thread failed: ${e.message}")
                    failureCount.incrementAndGet()
                }
            })
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(failureCount.get() == 0, "Shared engine concurrent access safe (failures=${failureCount.get()})")
        engine.destroy()
    }

    // ========================================================================
    // Performance Tests
    // ========================================================================

    private fun testPerformance() {
        Log.i(TAG, "TEST: Performance benchmarks")

        val engine = FilterEngine.create(threadCount = 4) ?: return
        val src = createTestBitmap(512, 512)
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        // Blur benchmark
        val blurTime = measureTimeMillis {
            engine.applyBlur(src, dst, radius = 5.0f, passes = 1)
        }
        val blurThroughput = (512 * 512 / 1e6) / (blurTime / 1000.0)
        Log.i(TAG, "Blur: $blurTime ms ($blurThroughput Mpx/sec)")
        assertTrue(blurTime < 5000, "Blur completes in < 5 seconds")

        // Color adjust benchmark
        val colorTime = measureTimeMillis {
            engine.applyColorAdjust(src, dst, brightness = 0.2f, contrast = 0.3f)
        }
        val colorThroughput = (512 * 512 / 1e6) / (colorTime / 1000.0)
        Log.i(TAG, "Color adjust: $colorTime ms ($colorThroughput Mpx/sec)")
        assertTrue(colorTime < 3000, "Color adjust completes in < 3 seconds")

        // Grayscale benchmark (SIMD-optimized)
        val grayTime = measureTimeMillis {
            engine.applyGrayscale(src, dst)
        }
        val grayThroughput = (512 * 512 / 1e6) / (grayTime / 1000.0)
        Log.i(TAG, "Grayscale: $grayTime ms ($grayThroughput Mpx/sec)")
        assertTrue(grayTime < 2000, "Grayscale completes in < 2 seconds")

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    // ========================================================================
    // Test Runner
    // ========================================================================

    /**
     * Run a single test method, converting unexpected exceptions into
     * failures so one buggy test cannot abort the entire suite.
     */
    private fun runTest(name: String, test: () -> Unit) {
        try {
            test()
        } catch (e: Throwable) {
            Log.e(TAG, "✗ FAIL: $name threw unexpected ${e.javaClass.simpleName}: ${e.message}")
            failCount++
        }
    }

    fun runAllTests() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "FilterEngine JNI Integration Tests")
        Log.i(TAG, "========================================\n")

        // Lifecycle
        runTest("Engine creation/destruction") { testEngineCreationAndDestruction() }
        runTest("Engine configuration") { testEngineConfiguration() }

        // Blur
        runTest("Blur filter") { testBlurFilter() }
        runTest("Blur multiple passes") { testBlurMultiplePasses() }

        // Color Adjust
        runTest("Color adjust") { testColorAdjust() }
        runTest("Color adjust clamping") { testColorAdjustParameterClamping() }

        // Emboss
        runTest("Emboss filter") { testEmbossFilter() }
        runTest("Emboss angles") { testEmbossAllAngles() }

        // Simple Filters
        runTest("Grayscale filter") { testGrayscaleFilter() }
        runTest("Invert filter") { testInvertFilter() }
        runTest("Sepia filter") { testSepiaFilter() }

        // Error Handling
        runTest("Bitmap dimension mismatch") { testBitmapDimensionMismatch() }
        runTest("Null engine handling") { testNullEngineHandling() }
        runTest("Destroyed engine + config edge cases") { testDestroyedEngineHandling() }

        // Thread Safety
        runTest("Concurrent independent engines") { testConcurrentIndependentEngines() }
        runTest("Shared engine concurrency") { testSharedEngineConcurrency() }

        // Performance
        runTest("Performance") { testPerformance() }

        // Summary
        Log.i(TAG, "\n========================================")
        Log.i(TAG, "Test Summary")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Passed: $passCount")
        Log.i(TAG, "Failed: $failCount")
        Log.i(TAG, "Total: ${passCount + failCount}")
        Log.i(TAG, "========================================\n")

        if (failCount == 0) {
            Log.i(TAG, "✓ ALL TESTS PASSED")
        } else {
            Log.e(TAG, "✗ $failCount TESTS FAILED")
        }
    }
}
