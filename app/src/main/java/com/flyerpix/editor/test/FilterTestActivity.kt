/**
 * FilterTestActivity.kt
 *
 * On-device test activity untuk FilterEngine
 *
 * Runs comprehensive JNI integration tests dengan UI feedback
 * untuk device-specific testing dan performance profiling.
 */

package com.flyerpix.editor.test

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import com.flyerpix.editor.R
import com.flyerpix.editor.filter.FilterEngineTest
import com.flyerpix.editor.filter.FilterPerformanceMonitor
import com.flyerpix.editor.filter.FilterMemoryProfiler
import com.flyerpix.editor.filter.FilterOptimizationProfiler
import com.flyerpix.editor.filter.FilterEngine
import com.flyerpix.editor.filter.FilterStatus
import android.content.Context
import android.util.Log
import kotlin.concurrent.thread

/**
 * Test Activity - Runs tests on real device with logging
 */
class FilterTestActivity : Activity() {
    companion object {
        private const val TAG = "FilterTestActivity"
    }

    private lateinit var logView: TextView
    private val logs = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple layout with scrollable text view
        logView = TextView(this).apply {
            setTextIsSelectable(true)
            textSize = 10f
            setBackgroundColor(android.graphics.Color.BLACK)
            setTextColor(android.graphics.Color.GREEN)
        }

        setContentView(ScrollView(this).apply {
            addView(logView)
        })

        // Redirect logs to UI
        startLogging()

        // Run tests in background
        thread(start = true) {
            try {
                Log.i(TAG, "Starting FilterEngine tests on device...")
                addLog("=== FilterEngine Device Tests ===\n")

                // Run integration tests
                addLog("\n### Running Integration Tests ###\n")
                val tester = FilterEngineTest()
                tester.runAllTests()

                // Run benchmarks
                addLog("\n### Running Performance Benchmarks ###\n")
                val monitor = FilterPerformanceMonitor()
                monitor.runAllBenchmarks()
                addLog(monitor.getSummaryReport())

                // Run memory profiling
                addLog("\n### Running Memory Profiling ###\n")
                val memoryProfiler = FilterMemoryProfiler(applicationContext)
                memoryProfiler.profileEngineLifecycle()
                val engineForMem = FilterEngine.create(threadCount = 4)
                if (engineForMem != null) {
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        512, 512, android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val dst = android.graphics.Bitmap.createBitmap(
                        512, 512, android.graphics.Bitmap.Config.ARGB_8888
                    )
                    memoryProfiler.profileFilterOperation("Blur (r=5)") {
                        engineForMem.applyBlur(bitmap, dst, radius = 5.0f, passes = 1)
                    }
                    memoryProfiler.profileBitmapAllocation(512, 512)
                    memoryProfiler.stressTestMemory(128, 50)
                    bitmap.recycle(); dst.recycle(); engineForMem.destroy()
                }
                addLog(memoryProfiler.generateLeakReport())

                // Run optimization profiling
                addLog("\n### Running Optimization Profiling ###\n")
                val optimizer = FilterOptimizationProfiler(applicationContext)
                val config = optimizer.detectOptimalConfig()
                addLog(config.toString() + "\n")
                addLog(optimizer.generateReport(config))

                val optEngine = FilterEngine.create(threadCount = config.threadCount)
                if (optEngine != null) {
                    optEngine.setSIMDEnabled(config.simdEnabled)
                    addLog("\nNative optimization report:\n${optEngine.getOptimizationReport()}\n")
                    optEngine.destroy()
                }

                addLog("\n=== All Tests Complete ===\n")
                Log.i(TAG, "Tests completed successfully")

            } catch (e: Exception) {
                addLog("ERROR: ${e.message}\n")
                Log.e(TAG, "Test failed", e)
            }
        }
    }

    private fun startLogging() {
        // Intercept Log calls via LogTree if available
        // For now, use simple approach
        addLog("Device: ${android.os.Build.DEVICE}\n")
        addLog("Model: ${android.os.Build.MODEL}\n")
        addLog("API Level: ${android.os.Build.VERSION.SDK_INT}\n")
        addLog("Brand: ${android.os.Build.BRAND}\n")
        addLog("\n")
    }

    private fun addLog(message: String) {
        logs.add(message)
        runOnUiThread {
            logView.append(message)
            // Auto-scroll to bottom
            logView.post {
                val scroll = logView.parent as? ScrollView
                scroll?.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Save logs to file
        saveLogsToFile()
    }

    private fun saveLogsToFile() {
        try {
            val filename = "filter_tests_${System.currentTimeMillis()}.txt"
            val file = java.io.File(getExternalFilesDir(null), filename)
            file.writeText(logs.joinToString("\n"))
            Log.i(TAG, "Logs saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save logs", e)
        }
    }
}
