/**
 * filter_optimization.cpp
 *
 * Runtime device profiling dan auto-tuning untuk optimal filter performance
 */

#include "filter_optimization.h"
#include "bitmap.h"
#include "filter_simd.h"
#include <chrono>
#include <cstring>
#include <cstdio>
#include <algorithm>
#include <cinttypes>
#include <unistd.h>
#include <fstream>

// ============================================================================
// CPU Detection
// ============================================================================

int OptimizationProfiler::getCpuCoreCount() const {
    int cores = sysconf(_SC_NPROCESSORS_ONLN);
    LOGD("Detected %d CPU cores", cores);
    return cores;
}

uint64_t OptimizationProfiler::getL1CacheSize() const {
    // ARM L1: typically 32KB per core
    uint64_t l1 = sysconf(_SC_LEVEL1_DCACHE_SIZE);
    if (l1 <= 0) l1 = 32 * 1024;  // Fallback
    return l1;
}

uint64_t OptimizationProfiler::getL2CacheSize() const {
    // ARM L2: typically 256KB-512KB per core
    uint64_t l2 = sysconf(_SC_LEVEL2_CACHE_SIZE);
    if (l2 <= 0) l2 = 256 * 1024;  // Fallback
    return l2;
}

float OptimizationProfiler::getMemoryBandwidth() const {
    // Typical bandwidth:
    // - Snapdragon 888 (LPDDR5): 51.2 GB/sec
    // - Snapdragon 870 (LPDDR5): 51.2 GB/sec
    // - Snapdragon 855 (LPDDR4X): 34.1 GB/sec
    // Fallback to conservative 25.6 GB/sec
    return 25.6f;
}

// ============================================================================
// Initialization
// ============================================================================

void OptimizationProfiler::initialize() {
    LOGI("=== FilterEngine Optimization Profiler ===");
    
    // Set default fallback profile (C++17 compatible field-by-field init)
    fallback_profile_.optimal_thread_count = 4;
    fallback_profile_.simd_chunk_size = 256;
    fallback_profile_.cache_line_size = 64;
    fallback_profile_.blur_kernel_threshold = 7;
    fallback_profile_.blur_tile_height = 32;
    fallback_profile_.blur_simd_benefit = 2.1f;
    fallback_profile_.color_batch_size = 256;
    fallback_profile_.color_use_lut = true;
    fallback_profile_.emboss_kernel_size = 5;
    fallback_profile_.emboss_tile_size = 16;
    fallback_profile_.prefetch_distance = 8;
    fallback_profile_.use_vectorized_io = true;
    fallback_profile_.max_concurrent_tasks = 8;
    fallback_profile_.thermal_backoff_threshold = 80;  // 80°C
    
    current_profile_ = fallback_profile_;
    
    // Detect device characteristics
    detectCpuCharacteristics();
    
    // Measure memory characteristics
    measureMemoryBandwidth();
    
    // Benchmark cache hierarchy
    benchmarkCacheHierarchy();
    
    LOGI("Optimization profile initialized successfully");
}

void OptimizationProfiler::detectCpuCharacteristics() {
    int cores = getCpuCoreCount();
    uint64_t l1 = getL1CacheSize();
    uint64_t l2 = getL2CacheSize();
    
    LOGD("CPU Characteristics:");
    LOGD("  Cores: %d", cores);
    LOGD("  L1 Cache: %" PRIu64 " KB", l1 / 1024);
    LOGD("  L2 Cache: %" PRIu64 " KB", l2 / 1024);
    
    // Tune thread count based on cores
    // Empirically: threads = (cores - 1) to avoid contention with main thread
    current_profile_.optimal_thread_count = std::max(1, cores - 1);
    
    // Cap at 8 threads (diminishing returns beyond)
    current_profile_.optimal_thread_count = std::min(current_profile_.optimal_thread_count, 8);
    
    // Tune SIMD chunk size based on cache
    // Target: fit 4 chunks in L1 (4 chunks = 4 SIMD_BATCH_BYTES each)
    current_profile_.simd_chunk_size = l1 / (4 * 16);  // 16 bytes per SIMD batch
    current_profile_.simd_chunk_size = std::max(64, current_profile_.simd_chunk_size);
}

void OptimizationProfiler::measureMemoryBandwidth() {
    // Simple bandwidth measurement: sequential read
    LOGI("Measuring memory bandwidth...");
    
    const int TEST_SIZE = 64 * 1024 * 1024;  // 64 MB
    auto* test_buffer = new uint8_t[TEST_SIZE];
    
    // Fill buffer
    std::memset(test_buffer, 0xAA, TEST_SIZE);
    
    // Timed sequential read
    auto start = std::chrono::high_resolution_clock::now();
    volatile uint64_t sum = 0;
    for (int i = 0; i < TEST_SIZE; i += 64) {  // Read per cache line
        sum += test_buffer[i];
    }
    auto end = std::chrono::high_resolution_clock::now();
    
    auto duration_ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    float bandwidth_gb_s = (TEST_SIZE / (1024.0f * 1024.0f * 1024.0f)) / (duration_ms / 1000.0f);
    
    LOGD("Memory Bandwidth: %.2f GB/sec", bandwidth_gb_s);
    
    delete[] test_buffer;
}

void OptimizationProfiler::benchmarkCacheHierarchy() {
    LOGI("Benchmarking cache hierarchy...");
    
    // Test different working set sizes
    const int sizes[] = {
        1024,                // 1 KB (L1 hit)
        8 * 1024,            // 8 KB (L1 boundary)
        256 * 1024,          // 256 KB (L2 hit)
        2 * 1024 * 1024      // 2 MB (L3/memory)
    };
    
    for (int size : sizes) {
        auto* buffer = new int[size / sizeof(int)];
        
        auto start = std::chrono::high_resolution_clock::now();
        volatile int sum = 0;
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < size / sizeof(int); j += 16) {
                sum += buffer[j];  // Sequential access
            }
        }
        auto end = std::chrono::high_resolution_clock::now();
        
        auto duration_us = std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();
        LOGD("Cache test %d KB: %.3f us/iteration", size / 1024, duration_us / 1000.0f);
        
        delete[] buffer;
    }
}

// ============================================================================
// Auto-tuning
// ============================================================================

void OptimizationProfiler::autoTune() {
    LOGI("Starting auto-tune profiling...");
    
    // Profile blur
    int blur_threshold = profileBlurThreshold();
    current_profile_.blur_kernel_threshold = blur_threshold;
    LOGI("Blur separable threshold tuned to: %d", blur_threshold);
    
    // Profile color adjust
    profileColorAdjust();
    LOGI("Color adjust batch size: %d", current_profile_.color_batch_size);
    
    // Profile emboss
    int emboss_tile = profileEmbossTileSize();
    current_profile_.emboss_tile_size = emboss_tile;
    LOGI("Emboss tile size tuned to: %d", emboss_tile);
    
    LOGI("Auto-tune complete");
}

int OptimizationProfiler::profileBlurThreshold() {
    // Test blur at different radii and find breakeven point
    // where separable convolution becomes faster
    
    LOGD("Profiling blur convolution strategy...");
    
    // For now, return conservative default
    // In real deployment, this would benchmark blur speeds
    return BLUR_SEPARABLE_THRESHOLD;
}

void OptimizationProfiler::profileColorAdjust() {
    LOGD("Profiling color adjust optimization...");
    
    // Determine if LUT is beneficial
    // LUT size = 256^3 = 16M entries (too large), so use per-channel LUT
    // Actually, HSV conversion is fast enough, disable LUT
    current_profile_.color_use_lut = false;
}

int OptimizationProfiler::profileEmbossTileSize() {
    // Find optimal tile size for emboss convolution
    LOGD("Profiling emboss tile size...");
    
    // Conservative: use 16x16 tiles
    // Can be tuned to 8x8 or 32x32 depending on cache
    return EMBOSS_TILE_SIZE;
}

// ============================================================================
// Reporting
// ============================================================================

const char* OptimizationProfiler::generateReport() {
    std::snprintf(report_buffer_, sizeof(report_buffer_),
        "=== FilterEngine Optimization Report ===\n"
        "Thread Count: %d\n"
        "SIMD Chunk Size: %d pixels\n"
        "Cache Line Size: %d bytes\n"
        "Blur Separable Threshold: %d\n"
        "Blur Tile Height: %d\n"
        "Color Batch Size: %d\n"
        "Color LUT Enabled: %s\n"
        "Emboss Tile Size: %dx%d\n"
        "Prefetch Distance: %d lines\n"
        "Vectorized I/O: %s\n"
        "Max Concurrent Tasks: %d\n"
        "Thermal Backoff: %d°C\n",
        current_profile_.optimal_thread_count,
        current_profile_.simd_chunk_size,
        current_profile_.cache_line_size,
        current_profile_.blur_kernel_threshold,
        current_profile_.blur_tile_height,
        current_profile_.color_batch_size,
        current_profile_.color_use_lut ? "YES" : "NO",
        current_profile_.emboss_tile_size,
        current_profile_.emboss_tile_size,
        current_profile_.prefetch_distance,
        current_profile_.use_vectorized_io ? "YES" : "NO",
        current_profile_.max_concurrent_tasks,
        current_profile_.thermal_backoff_threshold
    );
    
    return report_buffer_;
}
