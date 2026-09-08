/**
 * filter_engine.cpp
 * 
 * Filter Engine coordinator - main entry point untuk filter operations
 */

#include "filter_engine.h"
#include "bitmap.h"
#include "thread_pool.h"
#include "filter_simd.h"
#include <android/log.h>

#define LOG_TAG "FilterEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// ============================================================================
// FilterEngine Implementation
// ============================================================================

FilterEngine::FilterEngine() {
    LOGD("FilterEngine initialized (threads=%d, SIMD=%s)", 
         thread_count_, simd_enabled_ ? "ON" : "OFF");
}

FilterEngine::~FilterEngine() {
    LOGD("FilterEngine destroyed");
}

Status FilterEngine::applyBlur(const Bitmap& src, Bitmap& dst, const BlurParams& params) {
    LOGD("Applying Gaussian blur (radius=%.1f, passes=%d)", params.radius, params.passes);
    
    // Kotlin wrapper clamps radius ke [0.5, 50.0] — native harus konsisten.
    if (params.radius < 0.5f || params.radius > 50.0f) {
        LOGE("Invalid blur radius: %.1f (valid range: 0.5-50)", params.radius);
        return Status::ERROR_INVALID_PARAM;
    }
    
    // Apply blur multiple passes if requested
    Bitmap temp = src;
    for (int p = 0; p < params.passes; p++) {
        Bitmap pass_dst(src.getWidth(), src.getHeight(), src.getFormat());
        Status result = BlurFilter::applySeparable(temp, pass_dst, params.radius, thread_count_);
        
        if (result != Status::OK) {
            LOGE("Blur pass %d failed", p);
            return result;
        }
        
        temp = pass_dst;
    }
    
    // Copy result to destination
    dst = temp;
    return Status::OK;
}

Status FilterEngine::applyColorAdjust(const Bitmap& src, Bitmap& dst, const ColorAdjustParams& params) {
    LOGD("Applying color adjust (brightness=%.2f, contrast=%.2f, saturation=%.2f, hue=%.1f)",
         params.brightness, params.contrast, params.saturation, params.hue);
    
    return ColorFilter::apply(src, dst, params.brightness, params.contrast, 
                             params.saturation, params.hue, thread_count_);
}

Status FilterEngine::applyEmboss(const Bitmap& src, Bitmap& dst, const EmbossParams& params) {
    LOGD("Applying emboss (amount=%.2f, angle=%.1f)", params.amount, params.angle);
    
    if (params.amount < 0.0f || params.amount > 2.0f) {
        LOGE("Invalid emboss amount: %.2f (valid range: 0-2)", params.amount);
        return Status::ERROR_INVALID_PARAM;
    }
    
    return EmbossFilter::apply(src, dst, params.amount, params.angle, thread_count_);
}

Status FilterEngine::applyGrayscale(const Bitmap& src, Bitmap& dst) {
    LOGD("Applying grayscale");
    
    if (src.getWidth() <= 0 || src.getHeight() <= 0) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    ThreadPool pool(thread_count_);
    int height = src.getHeight();
    int width = src.getWidth();
    int chunk_size = (height + thread_count_ - 1) / thread_count_;
    
    for (int t = 0; t < thread_count_; t++) {
        int y0 = t * chunk_size;
        int y1 = (t == thread_count_ - 1) ? height : (t + 1) * chunk_size;
        
        auto task = [&src, &dst, y0, y1, width]() {
            for (int y = y0; y < y1; y++) {
                // Use SIMD-optimized scanline processing
                const uint32_t* src_scanline = 
                    reinterpret_cast<const uint32_t*>(src.getScanline(y));
                uint32_t* dst_scanline = 
                    reinterpret_cast<uint32_t*>(dst.getScanline(y));
                
                simd_grayscale_scanline(src_scanline, dst_scanline, width);
            }
        };
        
        pool.submit(task);
    }
    
    return pool.waitAll() ? Status::OK : Status::ERROR_RENDERING_FAILED;
}

Status FilterEngine::applyInvert(const Bitmap& src, Bitmap& dst) {
    LOGD("Applying invert");
    
    if (src.getWidth() <= 0 || src.getHeight() <= 0) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    ThreadPool pool(thread_count_);
    int height = src.getHeight();
    int width = src.getWidth();
    int chunk_size = (height + thread_count_ - 1) / thread_count_;
    
    for (int t = 0; t < thread_count_; t++) {
        int y0 = t * chunk_size;
        int y1 = (t == thread_count_ - 1) ? height : (t + 1) * chunk_size;
        
        auto task = [&src, &dst, y0, y1, width]() {
            for (int y = y0; y < y1; y++) {
                // Use SIMD-optimized scanline processing
                const uint32_t* src_scanline = 
                    reinterpret_cast<const uint32_t*>(src.getScanline(y));
                uint32_t* dst_scanline = 
                    reinterpret_cast<uint32_t*>(dst.getScanline(y));
                
                simd_invert_scanline(src_scanline, dst_scanline, width);
            }
        };
        
        pool.submit(task);
    }
    
    return pool.waitAll() ? Status::OK : Status::ERROR_RENDERING_FAILED;
}

Status FilterEngine::applySepia(const Bitmap& src, Bitmap& dst, float intensity) {
    LOGD("Applying sepia (intensity=%.2f)", intensity);
    
    if (intensity < 0.0f || intensity > 1.0f) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    if (src.getWidth() <= 0 || src.getHeight() <= 0) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    ThreadPool pool(thread_count_);
    int height = src.getHeight();
    int chunk_size = (height + thread_count_ - 1) / thread_count_;
    
    for (int t = 0; t < thread_count_; t++) {
        int y0 = t * chunk_size;
        int y1 = (t == thread_count_ - 1) ? height : (t + 1) * chunk_size;
        
        auto task = [&src, &dst, y0, y1, intensity]() {
            for (int y = y0; y < y1; y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    Color32 pixel = *src.getPixelAt(x, y);
                    
                    uint8_t a = (pixel >> 24) & 0xFF;
                    uint8_t r = (pixel >> 16) & 0xFF;
                    uint8_t g = (pixel >> 8) & 0xFF;
                    uint8_t b = pixel & 0xFF;
                    
                    // Sepia transform:
                    // R' = R*0.393 + G*0.769 + B*0.189
                    // G' = R*0.349 + G*0.686 + B*0.168
                    // B' = R*0.272 + G*0.534 + B*0.131
                    float sepia_r = r * 0.393f + g * 0.769f + b * 0.189f;
                    float sepia_g = r * 0.349f + g * 0.686f + b * 0.168f;
                    float sepia_b = r * 0.272f + g * 0.534f + b * 0.131f;
                    
                    // Blend with original based on intensity
                    float inv_intensity = 1.0f - intensity;
                    uint8_t out_r = static_cast<uint8_t>(
                        std::min(255.0f, sepia_r * intensity + r * inv_intensity)
                    );
                    uint8_t out_g = static_cast<uint8_t>(
                        std::min(255.0f, sepia_g * intensity + g * inv_intensity)
                    );
                    uint8_t out_b = static_cast<uint8_t>(
                        std::min(255.0f, sepia_b * intensity + b * inv_intensity)
                    );
                    
                    Color32 result = (a << 24) | (out_r << 16) | (out_g << 8) | out_b;
                    *dst.getPixelAt(x, y) = result;
                }
            }
        };
        
        pool.submit(task);
    }
    
    return pool.waitAll() ? Status::OK : Status::ERROR_RENDERING_FAILED;
}

// ============================================================================
// Thread-Safe Configuration Methods
// ============================================================================

void FilterEngine::setThreadCount(int threads) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    if (threads > 0 && threads <= 32) {
        thread_count_ = threads;
        LOGD("Thread count set to %d", thread_count_);
    } else {
        LOGW("Invalid thread count: %d (valid range: 1-32), keeping %d", 
             threads, thread_count_);
    }
}

int FilterEngine::getThreadCount() const {
    std::lock_guard<std::mutex> lock(config_mutex_);
    return thread_count_;
}

void FilterEngine::setSIMDEnabled(bool enabled) {
    std::lock_guard<std::mutex> lock(config_mutex_);
    simd_enabled_ = enabled;
    LOGD("SIMD %s", simd_enabled_ ? "enabled" : "disabled");
}

bool FilterEngine::isSIMDEnabled() const {
    std::lock_guard<std::mutex> lock(config_mutex_);
    return simd_enabled_;
}
