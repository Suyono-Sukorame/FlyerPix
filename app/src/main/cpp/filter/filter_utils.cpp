/**
 * filter_utils.cpp
 * 
 * Filter utility functions dan advanced coordination logic
 * 
 * Features:
 * - Filter chaining (apply multiple filters in sequence)
 * - Filter performance profiling
 * - Memory optimization utilities
 * - Progress tracking untuk long-running operations
 */

#include "filter_engine.h"
#include "bitmap.h"
#include "thread_pool.h"
#include <android/log.h>
#include <chrono>
#include <vector>

#define LOG_TAG "FilterUtils"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// Filter Chaining Infrastructure
// ============================================================================

/**
 * Simple filter chain executor untuk apply multiple filters
 * 
 * Contoh usage:
 *   vector<FilterOperation> chain = {
 *       {BLUR, blur_params},
 *       {COLOR_ADJUST, color_params},
 *       {EMBOSS, emboss_params}
 *   };
 *   executeFilterChain(src, dst, chain, engine);
 */

struct FilterOperation {
    FilterType type;
    // Union untuk hold different parameter types
    union Params {
        FilterEngine::BlurParams blur_params;
        FilterEngine::ColorAdjustParams color_params;
        FilterEngine::EmbossParams emboss_params;
        
        Params() {}  // Empty constructor untuk union
        ~Params() {}
    } params;
    
    FilterOperation() = default;
};

/**
 * Execute filter chain dengan optimization:
 * - Avoid unnecessary buffer copies
 * - Reuse intermediate buffers
 * - Profile execution time per filter
 */
Status executeFilterChain(
    const Bitmap& src,
    Bitmap& dst,
    const std::vector<FilterOperation>& operations,
    FilterEngine* engine) {
    
    if (!engine || operations.empty()) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    LOGD("Executing filter chain with %zu operations", operations.size());
    
    // Create intermediate buffer untuk ping-pong processing
    Bitmap intermediate(src.getWidth(), src.getHeight(), src.getFormat());
    
    // Start dengan source bitmap
    Bitmap* current_src = const_cast<Bitmap*>(&src);
    Bitmap* current_dst = &intermediate;
    
    auto start_time = std::chrono::high_resolution_clock::now();
    
    for (size_t i = 0; i < operations.size(); i++) {
        const auto& op = operations[i];
        
        LOGD("Applying filter %zu/%zu (type=%d)", i + 1, operations.size(), static_cast<int>(op.type));
        
        Status result = Status::ERROR_RENDERING_FAILED;
        
        // Apply appropriate filter berdasarkan type
        switch (op.type) {
            case FilterType::BLUR:
                result = engine->applyBlur(*current_src, *current_dst, op.params.blur_params);
                break;
            case FilterType::COLOR_ADJUST:
                result = engine->applyColorAdjust(*current_src, *current_dst, op.params.color_params);
                break;
            case FilterType::EMBOSS:
                result = engine->applyEmboss(*current_src, *current_dst, op.params.emboss_params);
                break;
            case FilterType::GRAYSCALE:
                result = engine->applyGrayscale(*current_src, *current_dst);
                break;
            case FilterType::INVERT:
                result = engine->applyInvert(*current_src, *current_dst);
                break;
            case FilterType::SEPIA:
                result = engine->applySepia(*current_src, *current_dst, 1.0f);
                break;
            default:
                LOGE("Unknown filter type: %d", static_cast<int>(op.type));
                return Status::ERROR_INVALID_PARAM;
        }
        
        if (result != Status::OK) {
            LOGE("Filter %zu failed with status %d", i, static_cast<int>(result));
            return result;
        }
        
        // Swap buffers untuk next iteration
        std::swap(current_src, current_dst);
    }
    
    // Copy final result to destination
    if (current_src != &src) {
        dst.copyFrom(*current_src);
    }
    
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
        end_time - start_time).count();
    
    LOGD("Filter chain complete (total time: %lld ms)", duration);
    
    return Status::OK;
}

// ============================================================================
// Performance Profiling
// ============================================================================

/**
 * Profile single filter execution
 */
struct FilterProfile {
    FilterType filter_type;
    long long execution_time_ms;
    size_t pixels_processed;
    float pixels_per_ms;  // Throughput metric
};

FilterProfile profileFilter(
    const Bitmap& src,
    FilterEngine* engine,
    FilterType filter_type) {
    
    FilterProfile profile = {filter_type, 0, 0, 0.0f};
    
    if (!engine) {
        return profile;
    }
    
    Bitmap dst(src.getWidth(), src.getHeight(), src.getFormat());
    
    auto start = std::chrono::high_resolution_clock::now();
    
    // Apply filter
    Status result = Status::ERROR_RENDERING_FAILED;
    switch (filter_type) {
        case FilterType::GRAYSCALE:
            result = engine->applyGrayscale(src, dst);
            break;
        case FilterType::INVERT:
            result = engine->applyInvert(src, dst);
            break;
        case FilterType::SEPIA:
            result = engine->applySepia(src, dst, 1.0f);
            break;
        // Add more filter types as needed
        default:
            LOGD("Filter profiling not supported for type %d", static_cast<int>(filter_type));
            return profile;
    }
    
    auto end = std::chrono::high_resolution_clock::now();
    
    if (result == Status::OK) {
        profile.execution_time_ms = 
            std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
        profile.pixels_processed = src.getWidth() * src.getHeight();
        
        if (profile.execution_time_ms > 0) {
            profile.pixels_per_ms = static_cast<float>(profile.pixels_processed) / 
                                   profile.execution_time_ms;
        }
        
        LOGD("Filter profiling: type=%d, time=%lld ms, throughput=%.1f Mpx/ms",
             static_cast<int>(filter_type), profile.execution_time_ms,
             profile.pixels_per_ms / 1e6f);
    }
    
    return profile;
}

// ============================================================================
// Memory Optimization
// ============================================================================

/**
 * Estimate memory required untuk filter operation
 * 
 * Helps dengan pre-allocation dan OOM prevention
 */
size_t estimateFilterMemory(
    const Bitmap& bitmap,
    FilterType filter_type) {
    
    size_t base_size = bitmap.getBufferSize();
    
    // Add overhead berdasarkan filter type
    switch (filter_type) {
        case FilterType::BLUR:
            // Blur needs intermediate buffer (double) + kernel storage
            return base_size * 2 + 1024;  // 1KB for kernel
        
        case FilterType::COLOR_ADJUST:
            // Color adjust needs minimal overhead
            return base_size + 128;
        
        case FilterType::EMBOSS:
            // Emboss needs 3x3 kernel processing overhead
            return base_size + 512;
        
        case FilterType::GRAYSCALE:
        case FilterType::INVERT:
        case FilterType::SEPIA:
            // Simple per-pixel operations
            return base_size + 64;
        
        default:
            return base_size;
    }
}
