/**
 * FilterPerformanceMonitor.kt
 *
 * Performance monitoring and benchmarking harness untuk FilterEngine
 *
 * Features:
 * - Execution time profiling (warmup + measured runs)
 * - Memory usage tracking
 * - Throughput calculation (Megapixels/sec)
 * - Statistical analysis (min/max/avg/stddev)
 * - Data export untuk analysis
 */

package com.flyerpix.editor.filter

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.math.sqrt

/**
 * BenchmarkResult - Holds performance metrics for a single filter operation
 */
data class BenchmarkResult(
    val filterName: String,
    val imageSize: Int,  // width * height in pixels
    val threadCount: Int,
    val executionTimes: List<Long>,  // milliseconds
    val memoryBefore: Long,  // bytes
    val memoryAfter: Long
) {
    val avgTime: Double
        get() = executionTimes.average()

    val minTime: Long
        get() = executionTimes.minOrNull() ?: 0L

    val maxTime: Long
        get() = executionTimes.maxOrNull() ?: 0L

    val stddev: Double
        get() {
            val mean = avgTime
            val variance = executionTimes.map { (it - mean) * (it - mean) }.average()
            return sqrt(variance)
        }

    val throughputMpxSec: Double
        get() = (imageSize / 1e6) / (avgTime / 1000.0)

    val memoryDeltaMB: Double
        get() = (memoryAfter - memoryBefore) / (1024.0 * 1024.0)

    override fun toString(): String {
        return """
            Filter: $filterName
            Image Size: $imageSize pixels
            Thread Count: $threadCount
            Execution Times: ${executionTimes.joinToString("ms, ") { "$it" }}ms
            Avg Time: ${"%.2f".format(avgTime)}ms
            Min/Max: ${minTime}ms / ${maxTime}ms
            Std Dev: ${"%.2f".format(stddev)}ms
            Throughput: ${"%.1f".format(throughputMpxSec)} Mpx/sec
            Memory Delta: ${"%.2f".format(memoryDeltaMB)} MB
        """.trimIndent()
    }
}

/**
 * FilterPerformanceMonitor - Main monitoring and benchmarking class
 */
class FilterPerformanceMonitor {
    companion object {
        private const val TAG = "FilterPerfMonitor"
        private const val WARMUP_ITERATIONS = 2
        private const val BENCHMARK_ITERATIONS = 5
    }

    private val results = mutableListOf<BenchmarkResult>()

    // ========================================================================
    // Memory Utilities
    // ========================================================================

    private fun getMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    // ========================================================================
    // Bitmap Creation
    // ========================================================================

    private fun createBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        // Create complex pattern (worst-case for blur)
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

    // ========================================================================
    // Benchmark Methods
    // ========================================================================

    /**
     * Benchmark a filter operation with specified parameters
     */
    private fun benchmarkFilter(
        filterName: String,
        imageSize: Int,
        threadCount: Int,
        operation: () -> FilterStatus
    ): BenchmarkResult {
        Log.d(TAG, "Benchmarking $filterName (size=$imageSize, threads=$threadCount)")

        // Warmup runs (not counted)
        repeat(WARMUP_ITERATIONS) {
            operation()
        }

        System.gc()  // Force garbage collection before measurement
        val memBefore = getMemoryUsageMB()

        // Measured runs
        val times = mutableListOf<Long>()
        repeat(BENCHMARK_ITERATIONS) {
            val start = System.currentTimeMillis()
            operation()
            val end = System.currentTimeMillis()
            times.add(end - start)
        }

        val memAfter = getMemoryUsageMB()

        return BenchmarkResult(
            filterName = filterName,
            imageSize = imageSize,
            threadCount = threadCount,
            executionTimes = times,
            memoryBefore = memBefore * 1024 * 1024,
            memoryAfter = memAfter * 1024 * 1024
        ).also { results.add(it) }
    }

    /**
     * Benchmark Gaussian blur with varying radius
     */
    fun benchmarkBlur(imageSizes: List<Int> = listOf(256, 512, 1024)) {
        Log.i(TAG, "=== Gaussian Blur Benchmark ===")

        val engine = FilterEngine.create(threadCount = 4) ?: return

        for (size in imageSizes) {
            val src = createBitmap(size, size)
            val dst = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            val result = benchmarkFilter(
                "Blur (r=5)",
                size * size,
                4
            ) {
                engine.applyBlur(src, dst, radius = 5.0f, passes = 1)
            }

            Log.i(TAG, result.toString())
            src.recycle()
            dst.recycle()
        }

        engine.destroy()
    }

    /**
     * Benchmark color adjust with various parameters
     */
    fun benchmarkColorAdjust(imageSizes: List<Int> = listOf(256, 512, 1024)) {
        Log.i(TAG, "=== Color Adjust Benchmark ===")

        val engine = FilterEngine.create(threadCount = 4) ?: return

        for (size in imageSizes) {
            val src = createBitmap(size, size)
            val dst = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            val result = benchmarkFilter(
                "Color Adjust",
                size * size,
                4
            ) {
                engine.applyColorAdjust(
                    src, dst,
                    brightness = 0.2f,
                    contrast = 0.3f,
                    saturation = 0.2f,
                    hue = 45.0f
                )
            }

            Log.i(TAG, result.toString())
            src.recycle()
            dst.recycle()
        }

        engine.destroy()
    }

    /**
     * Benchmark emboss with different angles
     */
    fun benchmarkEmboss(imageSizes: List<Int> = listOf(256, 512, 1024)) {
        Log.i(TAG, "=== Emboss Benchmark ===")

        val engine = FilterEngine.create(threadCount = 4) ?: return

        for (size in imageSizes) {
            val src = createBitmap(size, size)
            val dst = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            val result = benchmarkFilter(
                "Emboss (45°)",
                size * size,
                4
            ) {
                engine.applyEmboss(src, dst, amount = 1.0f, angle = 45.0f)
            }

            Log.i(TAG, result.toString())
            src.recycle()
            dst.recycle()
        }

        engine.destroy()
    }

    /**
     * Benchmark simple filters (grayscale, invert, sepia)
     */
    fun benchmarkSimpleFilters(imageSize: Int = 512) {
        Log.i(TAG, "=== Simple Filters Benchmark ===")

        val engine = FilterEngine.create(threadCount = 4) ?: return
        val src = createBitmap(imageSize, imageSize)
        val dst = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)

        // Grayscale
        val grayResult = benchmarkFilter(
            "Grayscale (SIMD)",
            imageSize * imageSize,
            4
        ) {
            engine.applyGrayscale(src, dst)
        }
        Log.i(TAG, grayResult.toString())

        // Invert
        val invertResult = benchmarkFilter(
            "Invert (SIMD)",
            imageSize * imageSize,
            4
        ) {
            engine.applyInvert(src, dst)
        }
        Log.i(TAG, invertResult.toString())

        // Sepia
        val sepiaResult = benchmarkFilter(
            "Sepia",
            imageSize * imageSize,
            4
        ) {
            engine.applySepia(src, dst, intensity = 0.7f)
        }
        Log.i(TAG, sepiaResult.toString())

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    /**
     * Benchmark parallel scaling (1, 2, 4, 8 threads)
     */
    fun benchmarkParallelScaling(imageSize: Int = 512) {
        Log.i(TAG, "=== Parallel Scaling Benchmark ===")

        val threadCounts = listOf(1, 2, 4, 8)
        val scalingResults = mutableListOf<BenchmarkResult>()

        for (threads in threadCounts) {
            val engine = FilterEngine.create(threadCount = threads) ?: continue
            val src = createBitmap(imageSize, imageSize)
            val dst = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)

            val result = benchmarkFilter(
                "Blur (parallel)",
                imageSize * imageSize,
                threads
            ) {
                engine.applyBlur(src, dst, radius = 5.0f, passes = 1)
            }

            scalingResults.add(result)
            Log.i(TAG, result.toString())

            src.recycle()
            dst.recycle()
            engine.destroy()
        }

        // Calculate speedup
        Log.i(TAG, "\nParallel Speedup Analysis:")
        val baseline = scalingResults[0].avgTime
        for (result in scalingResults) {
            val speedup = baseline / result.avgTime
            val efficiency = (speedup / result.threadCount) * 100
            Log.i(
                TAG,
                "${"%-2d".format(result.threadCount)} threads: ${"%.2f".format(speedup)}x speedup " +
                        "(${"%.1f".format(efficiency)}% efficiency)"
            )
        }
    }

    /**
     * SIMD optimization analysis
     */
    fun benchmarkSIMDOptimization(imageSize: Int = 512) {
        Log.i(TAG, "=== SIMD Optimization Analysis ===")

        val src = createBitmap(imageSize, imageSize)
        val dst = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)

        // With SIMD
        val engineSIMD = FilterEngine.create(threadCount = 4) ?: return
        engineSIMD.setSIMDEnabled(true)

        val simdResult = benchmarkFilter(
            "Grayscale (SIMD ON)",
            imageSize * imageSize,
            4
        ) {
            engineSIMD.applyGrayscale(src, dst)
        }
        Log.i(TAG, simdResult.toString())
        engineSIMD.destroy()

        System.gc()

        // Without SIMD
        val engineNoSIMD = FilterEngine.create(threadCount = 4) ?: return
        engineNoSIMD.setSIMDEnabled(false)

        val noSIMDResult = benchmarkFilter(
            "Grayscale (SIMD OFF)",
            imageSize * imageSize,
            4
        ) {
            engineNoSIMD.applyGrayscale(src, dst)
        }
        Log.i(TAG, noSIMDResult.toString())
        engineNoSIMD.destroy()

        // Calculate SIMD benefit
        val benefit = noSIMDResult.avgTime / simdResult.avgTime
        Log.i(TAG, "\nSIMD Speedup: ${"%.2f".format(benefit)}x")

        src.recycle()
        dst.recycle()
    }

    /**
     * Benchmark vs scalar reference implementations.
     *
     * Runs the same filter through:
     *  - Pure-Kotlin scalar reference (FilterReferenceImpl)
     *  - Native C++ engine (SIMD + thread pool)
     * and computes measured speedup = reference_time / native_time.
     */
    fun benchmarkVsReference(imageSize: Int = 512) {
        Log.i(TAG, "=== Benchmark vs Reference Implementation ===")
        Log.i(TAG, "Image: ${imageSize}x${imageSize} (${imageSize * imageSize} px)")

        val engine = FilterEngine.create(threadCount = 4) ?: return
        val src = createBitmap(imageSize, imageSize)
        val dst = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
        val pixels = FilterReferenceImpl.bitmapToPixels(src)

        data class RefBench(
            val name: String,
            val ref: () -> Unit,
            val native: () -> FilterStatus
        )

        val benchmarks = listOf(
            RefBench("Grayscale", { FilterReferenceImpl.grayscale(pixels) }, { engine.applyGrayscale(src, dst) }),
            RefBench("Invert", { FilterReferenceImpl.invert(pixels) }, { engine.applyInvert(src, dst) }),
            RefBench("Sepia", { FilterReferenceImpl.sepia(pixels, 0.7f) }, { engine.applySepia(src, dst, 0.7f) }),
            RefBench("Blur (r=5)", { FilterReferenceImpl.blur(pixels, imageSize, imageSize, 5) }, { engine.applyBlur(src, dst, 5.0f, 1) }),
            RefBench(
                "Color Adjust",
                { FilterReferenceImpl.colorAdjust(pixels, 0.2f, 0.3f, 0.2f) },
                { engine.applyColorAdjust(src, dst, 0.2f, 0.3f, 0.2f, 45.0f) }
            ),
            RefBench(
                "Emboss",
                { FilterReferenceImpl.emboss(pixels, imageSize, imageSize, 1.0f, 45.0f) },
                { engine.applyEmboss(src, dst, 1.0f, 45.0f) }
            )
        )

        // Warmup native path
        repeat(WARMUP_ITERATIONS) {
            engine.applyGrayscale(src, dst)
        }

        Log.i(TAG, "${"%-16s".format("Filter")} ${"%-12s".format("Reference")} ${"%-12s".format("Native")} Speedup")
        Log.i(TAG, "${"----------------".padEnd(16)} ${"------------".padEnd(12)} ${"------------".padEnd(12)} -------")

        for (b in benchmarks) {
            // Reference timing (scalar Kotlin)
            var refTotal = 0L
            repeat(BENCHMARK_ITERATIONS) {
                val start = System.nanoTime()
                b.ref()
                refTotal += (System.nanoTime() - start) / 1_000_000
            }
            val refAvg = refTotal.toDouble() / BENCHMARK_ITERATIONS

            // Native timing
            var nativeTotal = 0L
            repeat(BENCHMARK_ITERATIONS) {
                val start = System.nanoTime()
                val status = b.native()
                if (status != FilterStatus.OK) Log.w(TAG, "${b.name}: native returned $status")
                nativeTotal += (System.nanoTime() - start) / 1_000_000
            }
            val nativeAvg = nativeTotal.toDouble() / BENCHMARK_ITERATIONS

            val speedup = if (nativeAvg > 0) refAvg / nativeAvg else 0.0
            Log.i(
                TAG,
                "${"%-16s".format(b.name)} ${"%-10s".format("%.2fms".format(refAvg))} " +
                        "${"%-10s".format("%.2fms".format(nativeAvg))} ${"%.1fx".format(speedup)}"
            )
        }

        src.recycle()
        dst.recycle()
        engine.destroy()
    }

    // ========================================================================
    // Results Export
    // ========================================================================

    /**
     * Get all benchmark results
     */
    fun getResults(): List<BenchmarkResult> = results.toList()

    /**
     * Get summary report
     */
    fun getSummaryReport(): String {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("Filter Performance Summary\n")
        sb.append("========================================\n\n")

        for (result in results) {
            sb.append("${result.filterName}:\n")
            sb.append("  Image Size: ${result.imageSize} px\n")
            sb.append("  Avg Time: ${"%.2f".format(result.avgTime)}ms\n")
            sb.append("  Throughput: ${"%.1f".format(result.throughputMpxSec)} Mpx/sec\n")
            sb.append("  Memory: ${"%.2f".format(result.memoryDeltaMB)} MB\n")
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Clear all results
     */
    fun clear() {
        results.clear()
    }

    /**
     * Run all benchmarks
     */
    fun runAllBenchmarks() {
        Log.i(TAG, "Starting comprehensive benchmark suite...")

        benchmarkSimpleFilters()
        benchmarkBlur()
        benchmarkColorAdjust()
        benchmarkEmboss()
        benchmarkParallelScaling()
        benchmarkSIMDOptimization()
        benchmarkVsReference()

        Log.i(TAG, getSummaryReport())
    }
}
