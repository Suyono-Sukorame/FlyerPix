/**
 * FilterEngine.kt
 *
 * Java/Kotlin wrapper untuk native Filter Engine C++
 *
 * Provides:
 * - Native method declarations untuk semua 8 filter algorithms
 * - Lifecycle management (create/destroy)
 * - Thread pool configuration
 * - SIMD optimization control
 * - Error handling dengan Status enum
 */

package com.flyerpix.editor.filter

import android.graphics.Bitmap
import android.util.Log

/**
 * FilterEngine - Main filter processing coordinator
 *
 * Wraps native C++ FilterEngine untuk penggunaan di Kotlin/Java layer
 *
 * Usage:
 * ```kotlin
 * val engine = FilterEngine.create(threadCount = 4)
 * try {
 *     val result = engine.applyBlur(srcBitmap, dstBitmap, radius = 10.0f, passes = 1)
 *     if (result != FilterStatus.OK) {
 *         Log.e(TAG, "Blur failed: ${result.message}")
 *     }
 * } finally {
 *     engine.destroy()
 * }
 * ```
 */
class FilterEngine private constructor() {
    
    companion object {
        private const val TAG = "FilterEngine"
        
        init {
            System.loadLibrary("flyerpix_engine")
        }
        
        /**
         * Create new FilterEngine instance dengan specified thread count
         *
         * @param threadCount jumlah worker threads (default 4, 0 = auto-detect)
         * @return FilterEngine instance, atau null jika creation gagal
         */
        fun create(threadCount: Int = 4): FilterEngine? {
            return try {
                val engine = FilterEngine()
                val nativePtr = engine.nativeCreate(threadCount)
                if (nativePtr == 0L) {
                    Log.e(TAG, "Failed to create native FilterEngine")
                    return null
                }
                engine.nativePtr = nativePtr
                Log.d(TAG, "FilterEngine created with $threadCount threads")
                engine
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating FilterEngine: ${e.message}")
                null
            }
        }
    }
    
    private var nativePtr: Long = 0
    private var isDestroyed = false
    
    // ========================================================================
    // Lifecycle Management
    // ========================================================================
    
    /**
     * Destroy native FilterEngine dan release resources
     *
     * Must be called ketika selesai menggunakan engine.
     * Calling other methods setelah destroy() akan throw IllegalStateException.
     */
    fun destroy() {
        synchronized(this) {
            if (!isDestroyed && nativePtr != 0L) {
                nativeDestroy()
                nativePtr = 0
                isDestroyed = true
                Log.d(TAG, "FilterEngine destroyed")
            }
        }
    }
    
    /**
     * Check if engine still valid (not destroyed)
     */
    fun isValid(): Boolean = synchronized(this) { !isDestroyed && nativePtr != 0L }
    
    /**
     * Set number of worker threads untuk parallel processing
     *
     * @param threadCount jumlah threads (1-32, typical 2-8 depending on device)
     */
    fun setThreadCount(threadCount: Int) {
        checkNotDestroyed()
        nativeSetThreadCount(threadCount)
        Log.d(TAG, "Thread count set to $threadCount")
    }
    
    /**
     * Get current thread count
     */
    fun getThreadCount(): Int {
        checkNotDestroyed()
        return nativeGetThreadCount()
    }
    
    /**
     * Enable atau disable SIMD optimizations (ARM NEON, x86 SSE)
     *
     * @param enabled true untuk enable SIMD, false untuk scalar fallback
     */
    fun setSIMDEnabled(enabled: Boolean) {
        checkNotDestroyed()
        nativeSetSIMDEnabled(enabled)
        Log.d(TAG, "SIMD ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if SIMD optimizations are enabled
     */
    fun isSIMDEnabled(): Boolean {
        checkNotDestroyed()
        return nativeIsSIMDEnabled()
    }
    
    // ========================================================================
    // Optimization Profiling
    // ========================================================================
    
    /**
     * Run runtime auto-tuning dan return optimization report
     *
     * Detects device characteristics (CPU cores, cache sizes) and
     * tunes thread pool + SIMD settings untuk optimal performance.
     *
     * @return multi-line String berisi detected profile dan settings
     */
    fun getOptimizationReport(): String {
        checkNotDestroyed()
        return nativeGetOptimizationReport()
    }
    
    // ========================================================================
    // Blur Filter
    // ========================================================================
    
    /**
     * Apply Gaussian blur filter ke bitmap
     *
     * @param srcBitmap source bitmap (not modified)
     * @param dstBitmap destination bitmap untuk hasil
     * @param radius blur radius dalam pixels (0.5f - 50.0f, typical 1-15)
     * @param passes jumlah blur passes untuk cumulative effect (1-3)
     * @return FilterStatus.OK jika sukses, error code sebaliknya
     */
    fun applyBlur(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        radius: Float = 5.0f,
        passes: Int = 1
    ): FilterStatus {
        checkNotDestroyed()
        validateBitmaps(srcBitmap, dstBitmap)
        
        val radiusClamped = radius.coerceIn(0.5f, 50.0f)
        val passesClamped = passes.coerceIn(1, 3)
        
        Log.d(TAG, "Applying blur: radius=$radiusClamped, passes=$passesClamped")
        
        val statusCode = nativeApplyBlur(srcBitmap, dstBitmap, radiusClamped, passesClamped)
        return FilterStatus.fromCode(statusCode)
    }
    
    // ========================================================================
    // Color Adjust Filter
    // ========================================================================
    
    /**
     * Apply color adjustment filter (brightness, contrast, saturation, hue)
     *
     * @param srcBitmap source bitmap
     * @param dstBitmap destination bitmap
     * @param brightness -1.0f to +1.0f (negative=darker, positive=brighter)
     * @param contrast -1.0f to +1.0f (negative=less contrast, positive=more)
     * @param saturation -1.0f to +1.0f (negative=less saturated, positive=more)
     * @param hue -180f to +180f degrees (color rotation)
     * @return FilterStatus.OK jika sukses
     */
    fun applyColorAdjust(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        brightness: Float = 0.0f,
        contrast: Float = 0.0f,
        saturation: Float = 0.0f,
        hue: Float = 0.0f
    ): FilterStatus {
        checkNotDestroyed()
        validateBitmaps(srcBitmap, dstBitmap)
        
        val brightnessClamped = brightness.coerceIn(-1.0f, 1.0f)
        val contrastClamped = contrast.coerceIn(-1.0f, 1.0f)
        val saturationClamped = saturation.coerceIn(-1.0f, 1.0f)
        val hueClamped = hue.coerceIn(-180f, 180f)
        
        Log.d(TAG, "Applying color adjust: B=$brightnessClamped, C=$contrastClamped, S=$saturationClamped, H=$hueClamped")
        
        val statusCode = nativeApplyColorAdjust(
            srcBitmap, dstBitmap,
            brightnessClamped, contrastClamped, saturationClamped, hueClamped
        )
        return FilterStatus.fromCode(statusCode)
    }
    
    // ========================================================================
    // Emboss Filter
    // ========================================================================
    
    /**
     * Apply emboss filter untuk 3D effect
     *
     * @param srcBitmap source bitmap
     * @param dstBitmap destination bitmap
     * @param amount intensity dari emboss effect (0.0f - 2.0f, typical 0.5-1.5)
     * @param angle direction dari lighting dalam degrees (0-360)
     *        0°=dari kanan, 45°=dari kanan-atas, 90°=dari atas, dll
     * @return FilterStatus.OK jika sukses
     */
    fun applyEmboss(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        amount: Float = 1.0f,
        angle: Float = 45.0f
    ): FilterStatus {
        checkNotDestroyed()
        validateBitmaps(srcBitmap, dstBitmap)
        
        val amountClamped = amount.coerceIn(0.0f, 2.0f)
        val angleClamped = (angle % 360.0f + 360.0f) % 360.0f  // Normalize to 0-360
        
        Log.d(TAG, "Applying emboss: amount=$amountClamped, angle=$angleClamped")
        
        val statusCode = nativeApplyEmboss(srcBitmap, dstBitmap, amountClamped, angleClamped)
        return FilterStatus.fromCode(statusCode)
    }
    
    // ========================================================================
    // Simple Filters (Grayscale, Invert, Sepia)
    // ========================================================================
    
    /**
     * Convert bitmap ke grayscale using standard luminosity formula
     *
     * @param srcBitmap source bitmap
     * @param dstBitmap destination bitmap
     * @return FilterStatus.OK jika sukses
     */
    fun applyGrayscale(srcBitmap: Bitmap, dstBitmap: Bitmap): FilterStatus {
        checkNotDestroyed()
        validateBitmaps(srcBitmap, dstBitmap)
        
        Log.d(TAG, "Applying grayscale")
        
        val statusCode = nativeApplyGrayscale(srcBitmap, dstBitmap)
        return FilterStatus.fromCode(statusCode)
    }
    
    /**
     * Invert colors dalam bitmap
     *
     * @param srcBitmap source bitmap
     * @param dstBitmap destination bitmap
     * @return FilterStatus.OK jika sukses
     */
    fun applyInvert(srcBitmap: Bitmap, dstBitmap: Bitmap): FilterStatus {
        checkNotDestroyed()
        validateBitmaps(srcBitmap, dstBitmap)
        
        Log.d(TAG, "Applying invert")
        
        val statusCode = nativeApplyInvert(srcBitmap, dstBitmap)
        return FilterStatus.fromCode(statusCode)
    }
    
    /**
     * Apply sepia tone effect
     *
     * @param srcBitmap source bitmap
     * @param dstBitmap destination bitmap
     * @param intensity strength dari sepia effect (0.0f - 1.0f, typical 0.7)
     * @return FilterStatus.OK jika sukses
     */
    fun applySepia(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        intensity: Float = 0.7f
    ): FilterStatus {
        checkNotDestroyed()
        validateBitmaps(srcBitmap, dstBitmap)
        
        val intensityClamped = intensity.coerceIn(0.0f, 1.0f)
        
        Log.d(TAG, "Applying sepia: intensity=$intensityClamped")
        
        val statusCode = nativeApplySepia(srcBitmap, dstBitmap, intensityClamped)
        return FilterStatus.fromCode(statusCode)
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private fun checkNotDestroyed() {
        if (isDestroyed || nativePtr == 0L) {
            throw IllegalStateException("FilterEngine has been destroyed")
        }
    }
    
    private fun validateBitmaps(src: Bitmap, dst: Bitmap) {
        if (src.width != dst.width || src.height != dst.height) {
            throw IllegalArgumentException(
                "Source and destination bitmaps must have same dimensions. " +
                "Got: src=${src.width}x${src.height}, dst=${dst.width}x${dst.height}"
            )
        }
        
        if (src.config != Bitmap.Config.ARGB_8888 || dst.config != Bitmap.Config.ARGB_8888) {
            throw IllegalArgumentException(
                "Both bitmaps must be ARGB_8888 format"
            )
        }
    }
    
    // ========================================================================
    // Native Method Declarations
    // ========================================================================
    
    private external fun nativeCreate(threadCount: Int): Long
    
    private external fun nativeDestroy()
    
    private external fun nativeSetThreadCount(threadCount: Int)
    
    private external fun nativeGetThreadCount(): Int
    
    private external fun nativeSetSIMDEnabled(enabled: Boolean)
    
    private external fun nativeIsSIMDEnabled(): Boolean
    
    private external fun nativeGetOptimizationReport(): String
    
    private external fun nativeApplyBlur(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        radius: Float,
        passes: Int
    ): Int
    
    private external fun nativeApplyColorAdjust(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        hue: Float
    ): Int
    
    private external fun nativeApplyEmboss(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        amount: Float,
        angle: Float
    ): Int
    
    private external fun nativeApplyGrayscale(srcBitmap: Bitmap, dstBitmap: Bitmap): Int
    
    private external fun nativeApplyInvert(srcBitmap: Bitmap, dstBitmap: Bitmap): Int
    
    private external fun nativeApplySepia(
        srcBitmap: Bitmap,
        dstBitmap: Bitmap,
        intensity: Float
    ): Int
}

/**
 * FilterStatus - Hasil dari filter operation
 *
 * Mirrors C++ Status enum dengan human-readable error messages
 */
enum class FilterStatus(val code: Int, val message: String) {
    OK(0, "Filter applied successfully"),
    ERROR_INVALID_PARAM(-1, "Invalid filter parameters"),
    ERROR_OUT_OF_MEMORY(-2, "Out of memory"),
    ERROR_FILE_NOT_FOUND(-3, "File not found"),
    ERROR_UNSUPPORTED_FORMAT(-4, "Unsupported bitmap format"),
    ERROR_RENDERING_FAILED(-5, "Filter rendering failed"),
    ERROR_UNKNOWN(-999, "Unknown error");
    
    companion object {
        fun fromCode(code: Int): FilterStatus {
            return values().find { it.code == code } ?: ERROR_UNKNOWN
        }
    }
}
