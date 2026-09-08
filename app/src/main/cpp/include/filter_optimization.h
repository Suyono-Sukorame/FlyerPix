/**
 * filter_optimization.h
 *
 * Critical path optimization configuration untuk FilterEngine
 *
 * Mengoptimalkan:
 * - SIMD kernel sizes dan chunk sizing
 * - Thread pool configuration per device
 * - Cache-aware processing strategy
 * - Memory bandwidth tuning
 */

#ifndef FLYERPIX_FILTER_OPTIMIZATION_H
#define FLYERPIX_FILTER_OPTIMIZATION_H

#include <stdint.h>
#include <android/log.h>

#define LOG_TAG "FilterOptimization"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/**
 * OptimizationProfile - Device-specific optimization configuration
 *
 * Tuned untuk berbagai device characteristics:
 * - CPU core count dan frequency
 * - Cache hierarchy (L1/L2/L3)
 * - Memory bandwidth
 * - Thermal constraints
 */
struct OptimizationProfile {
    // Thread pool configuration
    int optimal_thread_count;      // Ideal thread count untuk device
    int simd_chunk_size;           // Pixels per SIMD batch
    int cache_line_size;           // CPU cache line size (typically 64 bytes)
    
    // Blur optimization
    int blur_kernel_threshold;     // Use separable kernel if radius > threshold
    int blur_tile_height;          // Scanline batch height untuk cache locality
    float blur_simd_benefit;       // Expected SIMD speedup factor
    
    // Color adjust optimization
    int color_batch_size;          // Pixels processed per iteration
    bool color_use_lut;            // Use lookup table for HSV conversion
    
    // Emboss optimization
    int emboss_kernel_size;        // 3x3 or larger
    int emboss_tile_size;          // Tile size untuk convolution
    
    // Memory optimization
    int prefetch_distance;         // Cache prefetch distance (lines ahead)
    bool use_vectorized_io;        // Use SIMD for memory operations
    
    // Thermal throttling
    int max_concurrent_tasks;      // Limit tasks to avoid thermal throttle
    int thermal_backoff_threshold; // Temperature in °C to trigger backoff
};

/**
 * OptimizationProfiler - Runtime device profiling dan auto-tuning
 */
class OptimizationProfiler {
public:
    static OptimizationProfiler& getInstance() {
        static OptimizationProfiler instance;
        return instance;
    }
    
    /**
     * Initialize profiler dan detect device characteristics
     */
    void initialize();
    
    /**
     * Get optimized profile untuk current device
     */
    const OptimizationProfile& getProfile() const {
        return current_profile_;
    }
    
    /**
     * Auto-tune configuration based on runtime benchmarks
     */
    void autoTune();
    
    /**
     * Get number of CPU cores
     */
    int getCpuCoreCount() const;
    
    /**
     * Get L1 cache size dalam bytes
     */
    uint64_t getL1CacheSize() const;
    
    /**
     * Get L2 cache size dalam bytes
     */
    uint64_t getL2CacheSize() const;
    
    /**
     * Get memory bandwidth dalam GB/sec
     */
    float getMemoryBandwidth() const;
    
    /**
     * Profile blur filter dan return optimal radius threshold
     */
    int profileBlurThreshold();
    
    /**
     * Profile color adjust optimization
     */
    void profileColorAdjust();
    
    /**
     * Profile emboss convolution optimal tile size
     */
    int profileEmbossTileSize();
    
    /**
     * Generate detailed optimization report
     */
    const char* generateReport();
    
private:
    OptimizationProfiler() = default;
    ~OptimizationProfiler() = default;
    
    OptimizationProfile current_profile_;
    OptimizationProfile fallback_profile_;
    
    // Detect device CPU characteristics
    void detectCpuCharacteristics();
    
    // Measure memory bandwidth
    void measureMemoryBandwidth();
    
    // Benchmark cache performance
    void benchmarkCacheHierarchy();
    
    // Report buffer for generateReport()
    char report_buffer_[2048];
};

/**
 * SIMD Optimization Constants
 *
 * Tuned untuk ARM NEON (32-bit) dan ARM NEON64 (64-bit)
 */

// SSE/SSE2 equivalent untuk ARM NEON
// Process 4x 32-bit pixels = 16 bytes = 1 cache line
static const int SIMD_PIXEL_BATCH = 4;
static const int SIMD_BATCH_BYTES = 16;

// Cache-optimal tile sizes
static const int CACHE_TILE_HEIGHT = 32;      // 32 scanlines fit in typical L2
static const int CACHE_TILE_WIDTH = 64;       // 64 pixels wide
static const int CACHE_TILE_PIXELS = CACHE_TILE_HEIGHT * CACHE_TILE_WIDTH;  // 2048 px

// Blur optimization thresholds (empirically tuned)
static const int BLUR_SEPARABLE_THRESHOLD = 7;  // Use separable > radius 7
static const int BLUR_FAST_PATH_RADIUS = 3;     // Use optimized path for small radius
static const int BLUR_KERNEL_MAX = 31;          // Limit kernel size

// Color adjust optimization
static const int COLOR_BATCH_PIXELS = 256;      // Process 256 pixels per task
static const int COLOR_LUT_THRESHOLD = 1000;    // Use LUT if processing > 1K pixels

// Emboss convolution
static const int EMBOSS_KERNEL_SIZE = 5;        // 5x5 kernel optimal balance
static const int EMBOSS_TILE_SIZE = 16;         // 16x16 tiles untuk register reuse

// Threading optimization
static const int THREAD_OVERHEAD_PIXELS = 65536;  // Min 64K pixels to justify thread
static const int MIN_WORK_PER_THREAD = 1024;      // Min 1K pixels per thread

// Prefetching
static const int PREFETCH_DISTANCE = 8;         // 8 cache lines ahead

/**
 * Inline optimization utilities
 */

// Calculate optimal chunk height untuk cache-aware processing
inline int calculateOptimalChunkHeight(int total_height, int thread_count) {
    // Target: 32 scanlines per chunk untuk L2 cache locality
    int chunk = (total_height + thread_count - 1) / thread_count;
    
    // Round to cache-friendly boundary (multiple of 32)
    return ((chunk + 31) / 32) * 32;
}

// Calculate optimal tile size for 2D convolution
inline int calculateOptimalTileSize(int image_width, int cache_size_bytes) {
    // Target: fit 2 tiles + kernel overlap in cache
    // Assume ARGB8888 = 4 bytes per pixel
    int tile_pixels = (cache_size_bytes / 4) / 3;  // 3 buffer overhead
    
    // Round to power of 2
    int side = 1;
    while (side * side < tile_pixels) side *= 2;
    
    return side;
}

// Estimate SIMD speedup factor based on filter type
inline float estimateSimdSpeedup(const char* filter_name) {
    // Empirically measured speedup factors
    if (__builtin_strcmp(filter_name, "grayscale") == 0) return 3.5f;    // SIMD: 3-4x
    if (__builtin_strcmp(filter_name, "invert") == 0) return 3.2f;       // SIMD: 3-4x
    if (__builtin_strcmp(filter_name, "sepia") == 0) return 2.8f;        // SIMD: 2-3x
    if (__builtin_strcmp(filter_name, "blur") == 0) return 2.1f;         // SIMD: 2-2.5x
    if (__builtin_strcmp(filter_name, "color_adjust") == 0) return 1.8f; // SIMD: 1.5-2x
    if (__builtin_strcmp(filter_name, "emboss") == 0) return 1.6f;       // SIMD: 1.5-2x
    
    return 1.0f;  // Unknown filter
}

#endif // FLYERPIX_FILTER_OPTIMIZATION_H
