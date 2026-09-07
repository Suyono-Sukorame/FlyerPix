/**
 * filter_engine.h
 * 
 * Filter Engine untuk FlyerPix dengan parallel processing
 * 
 * Supported Filters:
 * - Gaussian Blur (dengan radius control)
 * - Color Adjust (brightness, contrast, saturation)
 * - Emboss (3D effect)
 * - Grayscale, Invert, Sepia
 */

#ifndef FLYERPIX_FILTER_ENGINE_H
#define FLYERPIX_FILTER_ENGINE_H

#include "flyerpix_types.h"
#include "bitmap.h"
#include <memory>

// Forward declaration
class Bitmap;

/**
 * FilterEngine - Main filter processing coordinator
 * 
 * Features:
 * - Multi-threaded filter processing
 * - SIMD optimizations
 * - In-place filtering atau output to new bitmap
 * - Filter chaining
 */
class FilterEngine {
public:
    FilterEngine();
    ~FilterEngine();
    
    // ========== Blur Filter ==========
    
    struct BlurParams {
        float radius = 5.0f;      // Blur radius (1-50)
        int passes = 1;            // Number of blur passes
    };
    
    // Apply Gaussian blur to bitmap
    Status applyBlur(const Bitmap& src, Bitmap& dst, const BlurParams& params);
    
    // In-place blur
    Status applyBlur(Bitmap& bitmap, const BlurParams& params) {
        Bitmap temp = bitmap;  // Copy
        return applyBlur(temp, bitmap, params);
    }
    
    // ========== Color Adjust Filter ==========
    
    struct ColorAdjustParams {
        float brightness = 0.0f;   // -1.0 to 1.0
        float contrast = 0.0f;     // -1.0 to 1.0
        float saturation = 0.0f;   // -1.0 to 1.0
        float hue = 0.0f;          // -180 to 180 degrees
    };
    
    Status applyColorAdjust(const Bitmap& src, Bitmap& dst, const ColorAdjustParams& params);
    
    Status applyColorAdjust(Bitmap& bitmap, const ColorAdjustParams& params) {
        Bitmap temp = bitmap;
        return applyColorAdjust(temp, bitmap, params);
    }
    
    // ========== Emboss Filter ==========
    
    struct EmbossParams {
        float amount = 1.0f;       // 0.0 to 2.0
        float angle = 45.0f;       // 0-360 degrees
    };
    
    Status applyEmboss(const Bitmap& src, Bitmap& dst, const EmbossParams& params);
    
    Status applyEmboss(Bitmap& bitmap, const EmbossParams& params) {
        Bitmap temp = bitmap;
        return applyEmboss(temp, bitmap, params);
    }
    
    // ========== Simple Filters ==========
    
    // Convert to grayscale
    Status applyGrayscale(const Bitmap& src, Bitmap& dst);
    
    // Invert colors
    Status applyInvert(const Bitmap& src, Bitmap& dst);
    
    // Sepia tone effect
    Status applySepia(const Bitmap& src, Bitmap& dst, float intensity = 1.0f);
    
    // ========== Performance ==========
    
    // Set number of threads untuk multi-threaded filters
    void setThreadCount(int threads) { thread_count_ = threads; }
    
    // Enable/disable SIMD optimizations
    void setSIMDEnabled(bool enabled) { simd_enabled_ = enabled; }
    
private:
    int thread_count_ = 4;
    bool simd_enabled_ = true;
    
    // Helper functions
    static inline void blendPixel(Color32& dst, Color32 src, uint8_t alpha);
    static inline Color32 blurPixel(const Bitmap& src, int x, int y, float radius);
    static inline Color32 adjustColor(Color32 pixel, const ColorAdjustParams& params);
};

/**
 * BlurFilter - Specialized Gaussian blur implementation
 */
class BlurFilter {
public:
    BlurFilter();
    
    // Apply Gaussian blur dengan kernel optimization
    static Status apply(const Bitmap& src, Bitmap& dst, float radius, int threadCount = 4);
    
    // Apply separable Gaussian blur (faster)
    static Status applySeparable(const Bitmap& src, Bitmap& dst, float radius, int threadCount = 4);
    
private:
    // Generate Gaussian kernel
    static std::vector<float> generateKernel(float sigma);
    
    // Horizontal blur pass
    static void blurHorizontal(const Bitmap& src, Bitmap& dst, const std::vector<float>& kernel, int y0, int y1);
    
    // Vertical blur pass
    static void blurVertical(const Bitmap& src, Bitmap& dst, const std::vector<float>& kernel, int x0, int x1);
};

/**
 * ColorFilter - Color adjustment implementation
 */
class ColorFilter {
public:
    // Apply color adjustments dengan parallel processing
    static Status apply(const Bitmap& src, Bitmap& dst, float brightness, float contrast, 
                       float saturation, float hue, int threadCount = 4);
    
private:
    // RGB to HSV conversion
    static void rgbToHsv(uint8_t r, uint8_t g, uint8_t b, float& h, float& s, float& v);
    
    // HSV to RGB conversion
    static void hsvToRgb(float h, float s, float v, uint8_t& r, uint8_t& g, uint8_t& b);
    
    // Apply adjustment to single pixel
    static Color32 adjustPixel(Color32 pixel, float brightness, float contrast, 
                              float saturation, float hue);
};

/**
 * EmbossFilter - Emboss effect implementation
 */
class EmbossFilter {
public:
    // Apply emboss effect
    static Status apply(const Bitmap& src, Bitmap& dst, float amount, float angle, int threadCount = 4);
    
private:
    // Generate emboss kernel based on angle
    static std::vector<float> generateKernel(float angle);
};

#endif // FLYERPIX_FILTER_ENGINE_H
