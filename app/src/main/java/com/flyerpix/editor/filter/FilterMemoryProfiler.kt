/**
 * FilterMemoryProfiler.kt
 *
 * Memory profiling dan leak detection untuk FilterEngine
 *
 * Tracks:
 * - Heap memory usage before/after filter operations
 * - Bitmap allocation/deallocation
 * - Native memory usage
 * - Potential memory leaks via repeated operations
 * - GC behavior
 */

package com.flyerpix.editor.filter

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Debug
import android.util.Log
import kotlin.math.abs

/**
 * MemorySnapshot - Captures memory state at a point in time
 */
data class MemorySnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val totalMemory: Long,     // Total memory allocated to JVM (bytes)
    val freeMemory: Long,      // Free memory in JVM (bytes)
    val usedMemory: Long,      // Used memory = total - free
    val maxMemory: Long,       // Max memory JVM can use
    val nativeMemory: Long,    // Native heap size (bytes)
    val pssMemory: Long        // PSS (Proportional Set Size) in KB
) {
    val usedPercent: Float
        get() = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f

    override fun toString(): String {
        return """
            Timestamp: $timestamp
            Total: ${formatBytes(totalMemory)}
            Free: ${formatBytes(freeMemory)}
            Used: ${formatBytes(usedMemory)} (${"%.1f".format(usedPercent)}%)
            Max: ${formatBytes(maxMemory)}
            Native: ${formatBytes(nativeMemory)}
            PSS: ${formatBytes(pssMemory * 1024)}
        """.trimIndent()
    }

    companion object {
        fun capture(context: Context?): MemorySnapshot {
            val runtime = Runtime.getRuntime()
            val total = runtime.totalMemory()
            val free = runtime.freeMemory()
            val used = total - free
            val max = runtime.maxMemory()

            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val pss = memInfo.totalPss.toLong()

            return MemorySnapshot(
                totalMemory = total,
                freeMemory = free,
                usedMemory = used,
                maxMemory = max,
                nativeMemory = pss,  // Use PSS as proxy for native
                pssMemory = pss
            )
        }

        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
}

/**
 * MemoryProfile - Tracks memory before/after operation
 */
data class MemoryProfile(
    val operationName: String,
    val snapshotBefore: MemorySnapshot,
    val snapshotAfter: MemorySnapshot,
    val gcCount: Int = 0,
    val notes: String = ""
) {
    val memoryDelta: Long
        get() = snapshotAfter.usedMemory - snapshotBefore.usedMemory

    val memoryDeltaMB: Double
        get() = memoryDelta / (1024.0 * 1024.0)

    val leakSuspicion: Boolean
        get() = memoryDelta > 5 * 1024 * 1024  // > 5MB increase is suspicious

    override fun toString(): String {
        val deltaStr = if (memoryDelta >= 0) "+${formatBytes(memoryDelta)}" else formatBytes(memoryDelta)
        val warning = if (leakSuspicion) " ⚠️ POTENTIAL LEAK" else ""

        return """
            Operation: $operationName$warning
            Memory Delta: $deltaStr
            Before: ${snapshotBefore.usedMemory / (1024 * 1024)} MB / ${snapshotBefore.maxMemory / (1024 * 1024)} MB
            After: ${snapshotAfter.usedMemory / (1024 * 1024)} MB / ${snapshotAfter.maxMemory / (1024 * 1024)} MB
            GC Runs: $gcCount
            Notes: $notes
        """.trimIndent()
    }

    companion object {
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes == 0L -> "0 B"
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
            }
        }
    }
}

/**
 * FilterMemoryProfiler - Main memory profiling and leak detection
 */
class FilterMemoryProfiler(private val context: Context?) {
    companion object {
        private const val TAG = "FilterMemoryProfiler"
        private const val LEAK_THRESHOLD_MB = 5.0  // 5MB increase = suspicious
    }

    private val profiles = mutableListOf<MemoryProfile>()

    // ========================================================================
    // Profiling Methods
    // ========================================================================

    /**
     * Profile a filter operation for memory leaks
     */
    fun profileFilterOperation(
        operationName: String,
        operation: () -> FilterStatus
    ): MemoryProfile {
        Log.d(TAG, "Profiling: $operationName")

        // Force GC and capture baseline
        System.gc()
        val snapBefore = MemorySnapshot.capture(context)

        // Run operation
        val status = operation()

        // Capture after
        val snapAfter = MemorySnapshot.capture(context)

        val profile = MemoryProfile(
            operationName = operationName,
            snapshotBefore = snapBefore,
            snapshotAfter = snapAfter,
            notes = "Status: $status"
        )

        profiles.add(profile)
        Log.i(TAG, profile.toString())

        if (profile.leakSuspicion) {
            Log.w(TAG, "⚠️ MEMORY LEAK SUSPECTED: ${profile.operationName}")
        }

        return profile
    }

    /**
     * Profile repeated filter operations to detect leaks
     */
    fun profileRepeatedOperations(
        operationName: String,
        repetitions: Int = 10,
        operation: () -> FilterStatus
    ): List<MemoryProfile> {
        Log.i(TAG, "Profiling repeated operations: $operationName ($repetitions times)")

        val results = mutableListOf<MemoryProfile>()

        for (i in 1..repetitions) {
            val profile = profileFilterOperation("$operationName (run $i/$repetitions)", operation)
            results.add(profile)

            // Check for growth trend
            if (i > 1) {
                val prev = results[i - 2]
                if (profile.memoryDelta > LEAK_THRESHOLD_MB && prev.memoryDelta > LEAK_THRESHOLD_MB) {
                    Log.w(TAG, "⚠️ CONTINUOUS MEMORY GROWTH DETECTED")
                }
            }

            Thread.sleep(100)  // Small delay between runs
        }

        return results
    }

    /**
     * Profile bitmap lifecycle
     */
    fun profileBitmapAllocation(width: Int, height: Int): Long {
        System.gc()
        val before = MemorySnapshot.capture(context)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val allocSize = bitmap.byteCount.toLong()

        val after = MemorySnapshot.capture(context)
        val measured = after.usedMemory - before.usedMemory

        Log.i(
            TAG,
            "Bitmap allocation: ${width}x${height} = $allocSize bytes " +
                    "(measured: $measured bytes, diff: ${abs(allocSize - measured)} bytes)"
        )

        bitmap.recycle()
        return allocSize
    }

    /**
     * Profile bitmap deallocation
     */
    fun profileBitmapDeallocation(width: Int, height: Int): Boolean {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bitmapSize = bitmap.byteCount.toLong()

        System.gc()
        val before = MemorySnapshot.capture(context)

        bitmap.recycle()

        System.gc()
        val after = MemorySnapshot.capture(context)

        val freed = before.usedMemory - after.usedMemory
        val leakage = abs(freed - bitmapSize)
        val leakPercent = (leakage.toFloat() / bitmapSize.toFloat()) * 100f

        val isLeaking = leakPercent > 10f  // > 10% unfreed = leak

        Log.i(
            TAG,
            "Bitmap deallocation: allocated=$bitmapSize, freed=$freed " +
                    "(leakage: $leakage bytes, ${"%.1f".format(leakPercent)}%)" +
                    if (isLeaking) " ⚠️ POSSIBLE LEAK" else ""
        )

        return isLeaking
    }

    /**
     * Profile engine lifecycle
     */
    fun profileEngineLifecycle() {
        Log.i(TAG, "=== Engine Lifecycle Profiling ===")

        System.gc()
        val baseline = MemorySnapshot.capture(context)

        // Create engine
        val engine = FilterEngine.create(threadCount = 4)

        val afterCreate = MemorySnapshot.capture(context)
        Log.i(TAG, "After create: +${(afterCreate.usedMemory - baseline.usedMemory) / 1024} KB")

        // Destroy engine
        engine?.destroy()

        System.gc()
        val afterDestroy = MemorySnapshot.capture(context)

        val leaked = afterDestroy.usedMemory - baseline.usedMemory
        Log.i(TAG, "After destroy: ${if (leaked > 0) "+" else ""}${leaked / 1024} KB")

        if (leaked > 100 * 1024) {  // > 100KB leak
            Log.w(TAG, "⚠️ ENGINE LIFECYCLE LEAK DETECTED")
        }
    }

    /**
     * Stress test: allocate many bitmaps
     */
    fun stressTestMemory(size: Int = 256, iterations: Int = 100) {
        Log.i(TAG, "=== Stress Test: $iterations × ${size}x${size} bitmaps ===")

        System.gc()
        val baseline = MemorySnapshot.capture(context)

        try {
            for (i in 1..iterations) {
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                bitmap.recycle()

                if (i % 10 == 0) {
                    val current = MemorySnapshot.capture(context)
                    val used = current.usedMemory - baseline.usedMemory
                    Log.d(TAG, "Iteration $i: used +${used / (1024 * 1024)} MB")
                }
            }

            System.gc()
            val final = MemorySnapshot.capture(context)
            val totalUsed = final.usedMemory - baseline.usedMemory

            Log.i(TAG, "Final memory delta: ${totalUsed / (1024 * 1024)} MB")

            if (totalUsed > 10 * 1024 * 1024) {  // > 10MB leak
                Log.w(TAG, "⚠️ STRESS TEST DETECTED MEMORY LEAK")
            } else {
                Log.i(TAG, "✓ Stress test passed")
            }

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError during stress test", e)
        }
    }

    // ========================================================================
    // Analysis
    // ========================================================================

    /**
     * Generate leak report
     */
    fun generateLeakReport(): String {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("Memory Leak Analysis Report\n")
        sb.append("========================================\n\n")

        var totalDelta = 0L
        var suspiciousCount = 0

        for (profile in profiles) {
            totalDelta += profile.memoryDelta
            if (profile.leakSuspicion) {
                suspiciousCount++
                sb.append("⚠️ ${profile.operationName}\n")
                sb.append("   Delta: +${profile.memoryDeltaMB}MB\n\n")
            }
        }

        sb.append("Summary:\n")
        sb.append("Total operations: ${profiles.size}\n")
        sb.append("Suspicious leaks: $suspiciousCount\n")
        sb.append("Total memory delta: +${totalDelta / (1024 * 1024)} MB\n")

        return sb.toString()
    }

    /**
     * Get all profiles
     */
    fun getProfiles(): List<MemoryProfile> = profiles.toList()

    /**
     * Clear profiles
     */
    fun clear() {
        profiles.clear()
    }
}
