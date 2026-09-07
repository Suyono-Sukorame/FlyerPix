/**
 * emboss_filter.cpp
 * 
 * Emboss effect implementation dengan angle-based kernel rotation
 * 
 * Features:
 * - Sobel-based emboss kernels (3x3 convolution)
 * - Angle-based kernel rotation (0-360 degrees)
 * - Edge detection + gray blending untuk 3D effect
 * - Parallel processing with thread pool
 */

#include "filter_engine.h"
#include "bitmap.h"
#include "thread_pool.h"
#include <cmath>
#include <vector>
#include <android/log.h>

#define LOG_TAG "EmbossFilter"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// Emboss Kernel Generation & Rotation
// ============================================================================

/**
 * Generate emboss kernel based on angle
 * 
 * Emboss creates 3D effect by:
 * 1. Detecting edges using directional kernel
 * 2. Rotating kernel based on light direction (angle)
 * 3. Combining with original image
 * 
 * Standard emboss kernel (0 degrees - light from top-left):
 * [-2 -1  0]
 * [-1  1  1]
 * [ 0  1  2]
 * 
 * @param angle Light direction in degrees (0-360)
 * @return 3x3 kernel as float array [9 elements]
 */
std::vector<float> EmbossFilter::generateKernel(float angle) {
    // Normalize angle to [0, 360)
    while (angle < 0.0f) angle += 360.0f;
    while (angle >= 360.0f) angle -= 360.0f;
    
    // Convert angle to radians
    float radians = angle * (M_PI / 180.0f);
    float cos_a = std::cos(radians);
    float sin_a = std::sin(radians);
    
    // Base emboss kernel (light from top-left at 0°)
    // This is a simplified Sobel-like kernel
    std::vector<float> base_kernel = {
        -2.0f, -1.0f,  0.0f,
        -1.0f,  1.0f,  1.0f,
         0.0f,  1.0f,  2.0f
    };
    
    // Rotate kernel using 2D rotation matrix
    // For simplicity, we'll use 8 predefined kernels and interpolate
    // This avoids complex 2D rotation while maintaining quality
    
    int octant = static_cast<int>(angle / 45.0f) % 8;
    
    std::vector<float> kernel;
    
    // 8 directional emboss kernels (45-degree increments)
    switch (octant) {
        case 0: // 0° (light from top-left)
            kernel = {
                -2.0f, -1.0f,  0.0f,
                -1.0f,  1.0f,  1.0f,
                 0.0f,  1.0f,  2.0f
            };
            break;
        case 1: // 45° (light from top)
            kernel = {
                -1.0f,  0.0f,  1.0f,
                -2.0f,  1.0f,  2.0f,
                -1.0f,  0.0f,  1.0f
            };
            break;
        case 2: // 90° (light from top-right)
            kernel = {
                 0.0f,  1.0f,  2.0f,
                -1.0f,  1.0f,  1.0f,
                -2.0f, -1.0f,  0.0f
            };
            break;
        case 3: // 135° (light from right)
            kernel = {
                 1.0f,  2.0f,  1.0f,
                 0.0f,  1.0f,  0.0f,
                -1.0f, -2.0f, -1.0f
            };
            break;
        case 4: // 180° (light from bottom-right)
            kernel = {
                 2.0f,  1.0f,  0.0f,
                 1.0f,  1.0f, -1.0f,
                 0.0f, -1.0f, -2.0f
            };
            break;
        case 5: // 225° (light from bottom)
            kernel = {
                 1.0f,  0.0f, -1.0f,
                 2.0f,  1.0f, -2.0f,
                 1.0f,  0.0f, -1.0f
            };
            break;
        case 6: // 270° (light from bottom-left)
            kernel = {
                 0.0f, -1.0f, -2.0f,
                 1.0f,  1.0f, -1.0f,
                 2.0f,  1.0f,  0.0f
            };
            break;
        case 7: // 315° (light from left)
            kernel = {
                -1.0f, -2.0f, -1.0f,
                 0.0f,  1.0f,  0.0f,
                 1.0f,  2.0f,  1.0f
            };
            break;
        default:
            kernel = base_kernel;
    }
    
    // Normalize kernel (sum to 1.0)
    float sum = 0.0f;
    for (float k : kernel) {
        sum += std::abs(k);
    }
    
    if (sum > 0.0f) {
        for (auto& k : kernel) {
            k /= sum;
        }
    }
    
    return kernel;
}

// ============================================================================
// 3x3 Convolution Helper
// ============================================================================

/**
 * Apply 3x3 kernel convolution pada single pixel
 * 
 * @param src Source bitmap
 * @param x Center pixel x coordinate
 * @param y Center pixel y coordinate
 * @param kernel 3x3 kernel (9 floats)
 * @return Convolved color value
 */
static float convolvePixel(
    const Bitmap& src,
    int x,
    int y,
    const std::vector<float>& kernel) {
    
    float result = 0.0f;
    
    // Apply 3x3 kernel
    for (int ky = -1; ky <= 1; ky++) {
        for (int kx = -1; kx <= 1; kx++) {
            int nx = x + kx;
            int ny = y + ky;
            
            // Clamp to image bounds
            nx = std::max(0, std::min(nx, src.getWidth() - 1));
            ny = std::max(0, std::min(ny, src.getHeight() - 1));
            
            Color32 pixel = *src.getPixelAt(nx, ny);
            
            // Extract grayscale value
            uint8_t r = (pixel >> 16) & 0xFF;
            uint8_t g = (pixel >> 8) & 0xFF;
            uint8_t b = pixel & 0xFF;
            
            float gray = 0.299f * r + 0.587f * g + 0.114f * b;
            
            // Apply kernel weight
            int kernel_idx = (ky + 1) * 3 + (kx + 1);
            result += gray * kernel[kernel_idx] / 255.0f;
        }
    }
    
    return result;
}

// ============================================================================
// Emboss Effect Implementation
// ============================================================================

Status EmbossFilter::apply(
    const Bitmap& src, 
    Bitmap& dst, 
    float amount, 
    float angle, 
    int threadCount) {
    
    LOGD("Applying emboss effect (amount=%.2f, angle=%.1f°)", amount, angle);
    
    // Validate input
    if (src.getWidth() <= 0 || src.getHeight() <= 0) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    // Clamp amount
    amount = std::min(2.0f, std::max(0.0f, amount));
    
    // No emboss if amount is 0
    if (amount == 0.0f) {
        dst.copyFrom(src);
        return Status::OK;
    }
    
    // Generate emboss kernel based on angle
    auto kernel = generateKernel(angle);
    
    LOGD("Generated emboss kernel (angle=%.1f°, amount=%.2f)", angle, amount);
    
    // Process in parallel
    ThreadPool pool(threadCount);
    int height = src.getHeight();
    int chunk_size = (height + threadCount - 1) / threadCount;
    
    for (int t = 0; t < threadCount; t++) {
        int y0 = t * chunk_size;
        int y1 = (t == threadCount - 1) ? height : (t + 1) * chunk_size;
        
        auto task = [&src, &dst, &kernel, y0, y1, amount]() {
            for (int y = y0; y < y1; y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    // Get original pixel
                    Color32 orig_pixel = *src.getPixelAt(x, y);
                    uint8_t orig_a = (orig_pixel >> 24) & 0xFF;
                    uint8_t orig_r = (orig_pixel >> 16) & 0xFF;
                    uint8_t orig_g = (orig_pixel >> 8) & 0xFF;
                    uint8_t orig_b = orig_pixel & 0xFF;
                    
                    // Apply emboss kernel convolution
                    float emboss_value = convolvePixel(src, x, y, kernel);
                    
                    // Gray value (neutral baseline)
                    float gray_base = 128.0f / 255.0f;
                    
                    // Blend emboss effect with original
                    // Emboss enhances contrast while preserving hue
                    float factor = gray_base + emboss_value * amount;
                    factor = std::min(1.0f, std::max(0.0f, factor));
                    
                    // Apply effect to RGB channels
                    uint8_t out_r = static_cast<uint8_t>(orig_r * factor);
                    uint8_t out_g = static_cast<uint8_t>(orig_g * factor);
                    uint8_t out_b = static_cast<uint8_t>(orig_b * factor);
                    
                    Color32 result = (orig_a << 24) | (out_r << 16) | (out_g << 8) | out_b;
                    *dst.getPixelAt(x, y) = result;
                }
            }
        };
        
        pool.submit(task);
    }
    
    return pool.waitAll() ? Status::OK : Status::ERROR_RENDERING_FAILED;
}
