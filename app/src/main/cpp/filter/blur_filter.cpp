/**
 * blur_filter.cpp
 * 
 * Gaussian blur implementation dengan separable kernel optimization
 * 
 * Technique: Separable Gaussian blur
 * - Split 2D convolution menjadi 2 pass (horizontal + vertical)
 * - Reduce complexity dari O(r²) to O(r) per pixel
 * - Apply dengan thread pool untuk parallel processing
 */

#include "filter_engine.h"
#include "bitmap.h"
#include "thread_pool.h"
#include <cmath>
#include <vector>
#include <android/log.h>

#define LOG_TAG "BlurFilter"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// Gaussian Kernel Generation
// ============================================================================

/**
 * Generate 1D Gaussian kernel untuk blur filter
 * 
 * Gaussian function: G(x) = (1 / (σ√(2π))) * e^(-(x²)/(2σ²))
 * 
 * @param sigma Standard deviation (blur radius)
 * @return Vector of kernel weights (normalized)
 */
std::vector<float> BlurFilter::generateKernel(float sigma) {
    if (sigma <= 0.1f) {
        sigma = 0.1f;  // Minimum sigma
    }
    
    // Calculate kernel radius (3σ covers ~99% of distribution)
    int radius = static_cast<int>(std::ceil(3.0f * sigma));
    if (radius < 1) {
        radius = 1;
    }
    
    int kernel_size = 2 * radius + 1;
    std::vector<float> kernel(kernel_size, 0.0f);
    
    float sigma_sq = sigma * sigma;
    float sigma_sq_2 = 2.0f * sigma_sq;
    float denominator = std::sqrt(2.0f * M_PI) * sigma;
    
    float sum = 0.0f;
    
    // Generate Gaussian values
    for (int i = -radius; i <= radius; i++) {
        float x = i;
        float value = std::exp(-(x * x) / sigma_sq_2) / denominator;
        kernel[i + radius] = value;
        sum += value;
    }
    
    // Normalize kernel (sum to 1.0)
    if (sum > 0.0f) {
        for (auto& k : kernel) {
            k /= sum;
        }
    }
    
    return kernel;
}

// ============================================================================
// Horizontal Blur Pass
// ============================================================================

void BlurFilter::blurHorizontal(
    const Bitmap& src, 
    Bitmap& dst, 
    const std::vector<float>& kernel, 
    int y0, 
    int y1) {
    
    int radius = (kernel.size() - 1) / 2;
    int width = src.getWidth();
    int height = src.getHeight();
    
    // Validate bounds
    if (y0 < 0) y0 = 0;
    if (y1 > height) y1 = height;
    
    for (int y = y0; y < y1; y++) {
        for (int x = 0; x < width; x++) {
            float r = 0.0f, g = 0.0f, b = 0.0f, a = 0.0f;
            float weight_sum = 0.0f;
            
            // Convolve kernel across horizontal neighbors
            for (int i = -radius; i <= radius; i++) {
                int nx = x + i;
                
                // Clamp to image bounds
                if (nx < 0 || nx >= width) {
                    continue;  // Skip out-of-bounds
                }
                
                float weight = kernel[i + radius];
                const Color32* pixel_ptr = src.getPixelAt(nx, y);
                Color32 pixel = *pixel_ptr;
                
                // Extract ARGB
                uint8_t pixel_a = (pixel >> 24) & 0xFF;
                uint8_t pixel_r = (pixel >> 16) & 0xFF;
                uint8_t pixel_g = (pixel >> 8) & 0xFF;
                uint8_t pixel_b = pixel & 0xFF;
                
                r += pixel_r * weight;
                g += pixel_g * weight;
                b += pixel_b * weight;
                a += pixel_a * weight;
                weight_sum += weight;
            }
            
            // Denormalize if needed
            if (weight_sum > 0.0f && weight_sum != 1.0f) {
                r /= weight_sum;
                g /= weight_sum;
                b /= weight_sum;
                a /= weight_sum;
            }
            
            // Clamp to [0, 255]
            uint8_t out_r = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, r)));
            uint8_t out_g = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, g)));
            uint8_t out_b = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, b)));
            uint8_t out_a = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, a)));
            
            Color32 result = (out_a << 24) | (out_r << 16) | (out_g << 8) | out_b;
            *dst.getPixelAt(x, y) = result;
        }
    }
}

// ============================================================================
// Vertical Blur Pass
// ============================================================================

void BlurFilter::blurVertical(
    const Bitmap& src, 
    Bitmap& dst, 
    const std::vector<float>& kernel, 
    int x0, 
    int x1) {
    
    int radius = (kernel.size() - 1) / 2;
    int width = src.getWidth();
    int height = src.getHeight();
    
    // Validate bounds
    if (x0 < 0) x0 = 0;
    if (x1 > width) x1 = width;
    
    for (int x = x0; x < x1; x++) {
        for (int y = 0; y < height; y++) {
            float r = 0.0f, g = 0.0f, b = 0.0f, a = 0.0f;
            float weight_sum = 0.0f;
            
            // Convolve kernel across vertical neighbors
            for (int i = -radius; i <= radius; i++) {
                int ny = y + i;
                
                // Clamp to image bounds
                if (ny < 0 || ny >= height) {
                    continue;  // Skip out-of-bounds
                }
                
                float weight = kernel[i + radius];
                const Color32* pixel_ptr = src.getPixelAt(x, ny);
                Color32 pixel = *pixel_ptr;
                
                // Extract ARGB
                uint8_t pixel_a = (pixel >> 24) & 0xFF;
                uint8_t pixel_r = (pixel >> 16) & 0xFF;
                uint8_t pixel_g = (pixel >> 8) & 0xFF;
                uint8_t pixel_b = pixel & 0xFF;
                
                r += pixel_r * weight;
                g += pixel_g * weight;
                b += pixel_b * weight;
                a += pixel_a * weight;
                weight_sum += weight;
            }
            
            // Denormalize if needed
            if (weight_sum > 0.0f && weight_sum != 1.0f) {
                r /= weight_sum;
                g /= weight_sum;
                b /= weight_sum;
                a /= weight_sum;
            }
            
            // Clamp to [0, 255]
            uint8_t out_r = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, r)));
            uint8_t out_g = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, g)));
            uint8_t out_b = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, b)));
            uint8_t out_a = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, a)));
            
            Color32 result = (out_a << 24) | (out_r << 16) | (out_g << 8) | out_b;
            *dst.getPixelAt(x, y) = result;
        }
    }
}

// ============================================================================
// Separable Blur Implementation (OPTIMIZED - 2 passes)
// ============================================================================

Status BlurFilter::applySeparable(
    const Bitmap& src, 
    Bitmap& dst, 
    float radius, 
    int threadCount) {
    
    if (radius <= 0.0f) {
        // No blur, just copy
        dst.copyFrom(src);
        return Status::OK;
    }
    
    LOGD("Applying separable Gaussian blur (radius=%.1f, threads=%d)", radius, threadCount);
    
    // Validate input
    if (src.getWidth() <= 0 || src.getHeight() <= 0) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    // Ensure destination has same size
    if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    // Generate Gaussian kernel
    auto kernel = generateKernel(radius);
    LOGD("Generated Gaussian kernel (size=%zu, sigma=%.2f)", kernel.size(), radius);
    
    // Create intermediate buffer untuk horizontal pass
    Bitmap horizontal_buf(src.getWidth(), src.getHeight(), src.getFormat());
    
    // ========== PASS 1: Horizontal Blur ==========
    {
        ThreadPool pool(threadCount);
        int height = src.getHeight();
        int chunk_size = (height + threadCount - 1) / threadCount;
        
        for (int t = 0; t < threadCount; t++) {
            int y0 = t * chunk_size;
            int y1 = (t == threadCount - 1) ? height : (t + 1) * chunk_size;
            
            auto task = [&src, &horizontal_buf, &kernel, y0, y1]() {
                blurHorizontal(src, horizontal_buf, kernel, y0, y1);
            };
            
            pool.submit(task);
        }
        
        if (!pool.waitAll(5000)) {
            LOGE("Horizontal blur pass timeout");
            return Status::ERROR_RENDERING_FAILED;
        }
    }
    
    LOGD("Horizontal blur pass complete");
    
    // ========== PASS 2: Vertical Blur ==========
    {
        ThreadPool pool(threadCount);
        int width = src.getWidth();
        int chunk_size = (width + threadCount - 1) / threadCount;
        
        for (int t = 0; t < threadCount; t++) {
            int x0 = t * chunk_size;
            int x1 = (t == threadCount - 1) ? width : (t + 1) * chunk_size;
            
            auto task = [&horizontal_buf, &dst, &kernel, x0, x1]() {
                blurVertical(horizontal_buf, dst, kernel, x0, x1);
            };
            
            pool.submit(task);
        }
        
        if (!pool.waitAll(5000)) {
            LOGE("Vertical blur pass timeout");
            return Status::ERROR_RENDERING_FAILED;
        }
    }
    
    LOGD("Vertical blur pass complete");
    return Status::OK;
}

// ============================================================================
// Full 2D Convolution Implementation (SLOWER but support varying kernels)
// ============================================================================

Status BlurFilter::apply(
    const Bitmap& src, 
    Bitmap& dst, 
    float radius, 
    int threadCount) {
    
    if (radius <= 0.0f) {
        dst.copyFrom(src);
        return Status::OK;
    }
    
    // For Gaussian blur, use separable optimization
    // (Full 2D convolution would be slower)
    return applySeparable(src, dst, radius, threadCount);
}
