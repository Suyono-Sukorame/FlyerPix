/**
 * color_filter.cpp
 * 
 * Color adjustment implementation (brightness, contrast, saturation, hue)
 * 
 * Features:
 * - RGB ↔ HSV color space conversion
 * - Per-pixel brightness, contrast, saturation, hue adjustment
 * - Parallel processing with thread pool
 * - Optimized for performance
 */

#include "filter_engine.h"
#include "bitmap.h"
#include "thread_pool.h"
#include <cmath>
#include <algorithm>
#include <memory>
#include <android/log.h>

#define LOG_TAG "ColorFilter"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// Color Space Conversions
// ============================================================================

/**
 * Convert RGB to HSV (Hue, Saturation, Value)
 * 
 * Input: R, G, B in range [0, 255]
 * Output: H in range [0, 360), S in range [0, 1], V in range [0, 1]
 */
void ColorFilter::rgbToHsv(uint8_t r, uint8_t g, uint8_t b, float& h, float& s, float& v) {
    float rf = r / 255.0f;
    float gf = g / 255.0f;
    float bf = b / 255.0f;
    
    float cmax = std::max({rf, gf, bf});
    float cmin = std::min({rf, gf, bf});
    float delta = cmax - cmin;
    
    // Calculate Value (brightness)
    v = cmax;
    
    // Calculate Saturation
    if (cmax > 0.0f) {
        s = delta / cmax;
    } else {
        s = 0.0f;
    }
    
    // Calculate Hue
    if (delta == 0.0f) {
        h = 0.0f;
    } else if (cmax == rf) {
        h = 60.0f * std::fmod((gf - bf) / delta, 6.0f);
    } else if (cmax == gf) {
        h = 60.0f * (((bf - rf) / delta) + 2.0f);
    } else {
        h = 60.0f * (((rf - gf) / delta) + 4.0f);
    }
    
    // Normalize hue to [0, 360)
    if (h < 0.0f) {
        h += 360.0f;
    }
}

/**
 * Convert HSV to RGB
 * 
 * Input: H in range [0, 360), S in range [0, 1], V in range [0, 1]
 * Output: R, G, B in range [0, 255]
 */
void ColorFilter::hsvToRgb(float h, float s, float v, uint8_t& r, uint8_t& g, uint8_t& b) {
    // Normalize hue to [0, 360)
    while (h < 0.0f) h += 360.0f;
    while (h >= 360.0f) h -= 360.0f;
    
    float c = v * s;
    float hh = h / 60.0f;
    float x = c * (1.0f - std::abs(std::fmod(hh, 2.0f) - 1.0f));
    
    float rf = 0.0f, gf = 0.0f, bf = 0.0f;
    
    if (hh >= 0.0f && hh < 1.0f) {
        rf = c; gf = x; bf = 0.0f;
    } else if (hh >= 1.0f && hh < 2.0f) {
        rf = x; gf = c; bf = 0.0f;
    } else if (hh >= 2.0f && hh < 3.0f) {
        rf = 0.0f; gf = c; bf = x;
    } else if (hh >= 3.0f && hh < 4.0f) {
        rf = 0.0f; gf = x; bf = c;
    } else if (hh >= 4.0f && hh < 5.0f) {
        rf = x; gf = 0.0f; bf = c;
    } else {
        rf = c; gf = 0.0f; bf = x;
    }
    
    float m = v - c;
    r = static_cast<uint8_t>((rf + m) * 255.0f);
    g = static_cast<uint8_t>((gf + m) * 255.0f);
    b = static_cast<uint8_t>((bf + m) * 255.0f);
}

// ============================================================================
// Per-Pixel Color Adjustment
// ============================================================================

/**
 * Adjust single pixel dengan brightness, contrast, saturation, hue
 */
Color32 ColorFilter::adjustPixel(
    Color32 pixel, 
    float brightness, 
    float contrast, 
    float saturation, 
    float hue) {
    
    // Extract ARGB
    uint8_t a = (pixel >> 24) & 0xFF;
    uint8_t r = (pixel >> 16) & 0xFF;
    uint8_t g = (pixel >> 8) & 0xFF;
    uint8_t b = pixel & 0xFF;
    
    // Convert RGB to HSV
    float h, s, v;
    rgbToHsv(r, g, b, h, s, v);
    
    // Apply brightness (adjust V)
    v = std::min(1.0f, std::max(0.0f, v + brightness));
    
    // Apply contrast (adjust V around midpoint)
    v = 0.5f + (v - 0.5f) * (1.0f + contrast);
    v = std::min(1.0f, std::max(0.0f, v));
    
    // Apply saturation
    s = std::min(1.0f, std::max(0.0f, s * (1.0f + saturation)));
    
    // Apply hue (rotate in color wheel)
    h = std::fmod(h + hue, 360.0f);
    if (h < 0.0f) h += 360.0f;
    
    // Convert HSV back to RGB
    hsvToRgb(h, s, v, r, g, b);
    
    // Reconstruct color with original alpha
    return (a << 24) | (r << 16) | (g << 8) | b;
}

// ============================================================================
// Color Adjust Implementation
// ============================================================================

Status ColorFilter::apply(
    const Bitmap& src, 
    Bitmap& dst, 
    float brightness, 
    float contrast, 
    float saturation, 
    float hue, 
    int threadCount,
    ThreadPool* pool) {
    
    LOGD("Applying color adjust (B=%.2f, C=%.2f, S=%.2f, H=%.1f)", 
         brightness, contrast, saturation, hue);
    
    // Validate input
    if (src.getWidth() <= 0 || src.getHeight() <= 0) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    // Clamp parameters
    brightness = std::min(1.0f, std::max(-1.0f, brightness));
    contrast = std::min(1.0f, std::max(-1.0f, contrast));
    saturation = std::min(1.0f, std::max(-1.0f, saturation));
    // Hue kan unlimited
    
    // If all adjustments are zero, just copy
    if (brightness == 0.0f && contrast == 0.0f && saturation == 0.0f && hue == 0.0f) {
        dst.copyFrom(src);
        return Status::OK;
    }
    
    // Gunakan persistent pool jika disediakan; fallback buat pool lokal.
    // Pool lokal hanya dibuat (spawn thread) saat param pool == nullptr.
    std::unique_ptr<ThreadPool> owned_pool;
    ThreadPool* active_pool = pool;
    if (!active_pool) {
        owned_pool = std::make_unique<ThreadPool>(threadCount);
        active_pool = owned_pool.get();
    }
    
    // Process in parallel
    int height = src.getHeight();
    int chunk_size = (height + threadCount - 1) / threadCount;
    
    for (int t = 0; t < threadCount; t++) {
        int y0 = t * chunk_size;
        int y1 = (t == threadCount - 1) ? height : (t + 1) * chunk_size;
        
        auto task = [&src, &dst, y0, y1, brightness, contrast, saturation, hue]() {
            for (int y = y0; y < y1; y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    Color32 pixel = *src.getPixelAt(x, y);
                    Color32 adjusted = adjustPixel(pixel, brightness, contrast, saturation, hue);
                    *dst.getPixelAt(x, y) = adjusted;
                }
            }
        };
        
        active_pool->submit(task);
    }
    
    return active_pool->waitAll() ? Status::OK : Status::ERROR_RENDERING_FAILED;
}
