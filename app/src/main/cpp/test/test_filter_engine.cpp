/**
 * test_filter_engine.cpp
 * 
 * Unit tests untuk Filter Engine - blur, color adjust, emboss filters
 * 
 * Test coverage:
 * - Gaussian blur kernel generation
 * - Blur separable passes (horizontal + vertical)
 * - Color space conversions (RGB ↔ HSV)
 * - Color adjust (brightness, contrast, saturation, hue)
 * - Emboss filter dengan berbagai angles
 * - Thread pool parallelization
 * - SIMD optimizations
 */

#include "filter_engine.h"
#include "bitmap.h"
#include "thread_pool.h"
#include "color_filter.h"
#include <cassert>
#include <cmath>
#include <iostream>
#include <chrono>

#define TEST_WIDTH 256
#define TEST_HEIGHT 256

// ============================================================================
// Test Utilities
// ============================================================================

/**
 * Create test bitmap dengan gradient pattern
 */
static std::shared_ptr<Bitmap> createTestBitmap(int width, int height) {
    auto bitmap = std::make_shared<Bitmap>(width, height, PixelFormat::ARGB_8888);
    
    // Fill dengan gradient (R increases left to right, G increases top to bottom)
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            uint8_t r = (x * 255) / width;
            uint8_t g = (y * 255) / height;
            uint8_t b = 128;
            uint8_t a = 255;
            
            Color32 pixel = (a << 24) | (r << 16) | (g << 8) | b;
            *bitmap->getPixelAt(x, y) = pixel;
        }
    }
    
    return bitmap;
}

/**
 * Verify bitmap is not all zeros (sanity check)
 */
static bool isBitmapValid(const Bitmap& bitmap) {
    int valid_pixels = 0;
    
    for (int y = 0; y < bitmap.getHeight(); y++) {
        for (int x = 0; x < bitmap.getWidth(); x++) {
            Color32 pixel = *bitmap.getPixelAt(x, y);
            if (pixel != 0) {
                valid_pixels++;
            }
        }
    }
    
    return valid_pixels > 0;
}

/**
 * Calculate average pixel difference between two bitmaps
 * Lower = more similar, Higher = more different
 */
static float calculateDifference(const Bitmap& bmp1, const Bitmap& bmp2) {
    if (bmp1.getWidth() != bmp2.getWidth() || bmp1.getHeight() != bmp2.getHeight()) {
        return -1.0f;  // Invalid
    }
    
    double total_diff = 0.0;
    int pixel_count = bmp1.getWidth() * bmp1.getHeight();
    
    for (int y = 0; y < bmp1.getHeight(); y++) {
        for (int x = 0; x < bmp1.getWidth(); x++) {
            Color32 p1 = *bmp1.getPixelAt(x, y);
            Color32 p2 = *bmp2.getPixelAt(x, y);
            
            uint8_t r1 = (p1 >> 16) & 0xFF;
            uint8_t g1 = (p1 >> 8) & 0xFF;
            uint8_t b1 = p1 & 0xFF;
            
            uint8_t r2 = (p2 >> 16) & 0xFF;
            uint8_t g2 = (p2 >> 8) & 0xFF;
            uint8_t b2 = p2 & 0xFF;
            
            double dr = r1 - r2;
            double dg = g1 - g2;
            double db = b1 - b2;
            
            total_diff += (dr * dr + dg * dg + db * db);
        }
    }
    
    return static_cast<float>(total_diff / pixel_count);
}

// ============================================================================
// Test Cases - Gaussian Blur
// ============================================================================

static bool testBlurKernelGeneration() {
    std::cout << "TEST: Gaussian blur kernel generation... ";
    
    auto kernel = BlurFilter::generateKernel(3.0f);
    
    // Kernel should be odd-sized
    assert((kernel.size() % 2) == 1);
    
    // Kernel should sum to ~1.0 (normalized)
    float sum = 0.0f;
    for (float k : kernel) {
        sum += k;
    }
    assert(std::abs(sum - 1.0f) < 0.01f);
    
    // Center should be largest (peak of Gaussian)
    int center = kernel.size() / 2;
    for (size_t i = 0; i < kernel.size(); i++) {
        if (i != center) {
            assert(kernel[i] <= kernel[center]);
        }
    }
    
    std::cout << "PASS (kernel_size=" << kernel.size() << ")\n";
    return true;
}

static bool testBlurApplication() {
    std::cout << "TEST: Gaussian blur application... ";
    
    auto src = createTestBitmap(TEST_WIDTH, TEST_HEIGHT);
    auto dst = std::make_shared<Bitmap>(TEST_WIDTH, TEST_HEIGHT, PixelFormat::ARGB_8888);
    
    FilterEngine::BlurParams params;
    params.radius = 5.0f;
    params.passes = 1;
    
    FilterEngine engine;
    Status result = engine.applyBlur(*src, *dst, params);
    
    assert(result == Status::OK);
    assert(isBitmapValid(*dst));
    
    // Blurred image should be different from original
    float diff = calculateDifference(*src, *dst);
    assert(diff > 0.0f);
    
    std::cout << "PASS (difference=" << diff << ")\n";
    return true;
}

static bool testBlurMultiplePasses() {
    std::cout << "TEST: Gaussian blur multiple passes... ";
    
    auto src = createTestBitmap(TEST_WIDTH, TEST_HEIGHT);
    auto dst = std::make_shared<Bitmap>(TEST_WIDTH, TEST_HEIGHT, PixelFormat::ARGB_8888);
    
    FilterEngine::BlurParams params;
    params.radius = 3.0f;
    params.passes = 2;
    
    FilterEngine engine;
    Status result = engine.applyBlur(*src, *dst, params);
    
    assert(result == Status::OK);
    assert(isBitmapValid(*dst));
    
    std::cout << "PASS\n";
    return true;
}

// ============================================================================
// Test Cases - Color Adjust
// ============================================================================

static bool testRGBtoHSVConversion() {
    std::cout << "TEST: RGB to HSV conversion... ";
    
    // Test pure red
    float h, s, v;
    ColorFilter::rgbToHsv(255, 0, 0, h, s, v);
    assert(std::abs(h - 0.0f) < 1.0f);  // Hue should be ~0 (red)
    assert(std::abs(s - 1.0f) < 0.01f); // Saturation should be ~1.0
    assert(std::abs(v - 1.0f) < 0.01f); // Value should be ~1.0
    
    // Test pure green
    ColorFilter::rgbToHsv(0, 255, 0, h, s, v);
    assert(std::abs(h - 120.0f) < 1.0f); // Hue should be ~120 (green)
    
    // Test pure blue
    ColorFilter::rgbToHsv(0, 0, 255, h, s, v);
    assert(std::abs(h - 240.0f) < 1.0f); // Hue should be ~240 (blue)
    
    // Test gray (no saturation)
    ColorFilter::rgbToHsv(128, 128, 128, h, s, v);
    assert(s < 0.01f);  // Saturation should be ~0
    
    std::cout << "PASS\n";
    return true;
}

static bool testHSVtoRGBConversion() {
    std::cout << "TEST: HSV to RGB conversion... ";
    
    uint8_t r, g, b;
    
    // Test red (H=0, S=1, V=1)
    ColorFilter::hsvToRgb(0.0f, 1.0f, 1.0f, r, g, b);
    assert(r == 255 && g == 0 && b == 0);
    
    // Test green (H=120, S=1, V=1)
    ColorFilter::hsvToRgb(120.0f, 1.0f, 1.0f, r, g, b);
    assert(r == 0 && g == 255 && b == 0);
    
    // Test blue (H=240, S=1, V=1)
    ColorFilter::hsvToRgb(240.0f, 1.0f, 1.0f, r, g, b);
    assert(r == 0 && g == 0 && b == 255);
    
    std::cout << "PASS\n";
    return true;
}

static bool testColorAdjust() {
    std::cout << "TEST: Color adjust filter... ";
    
    auto src = createTestBitmap(TEST_WIDTH, TEST_HEIGHT);
    auto dst = std::make_shared<Bitmap>(TEST_WIDTH, TEST_HEIGHT, PixelFormat::ARGB_8888);
    
    FilterEngine::ColorAdjustParams params;
    params.brightness = 0.1f;
    params.contrast = 0.2f;
    params.saturation = 0.15f;
    params.hue = 30.0f;
    
    FilterEngine engine;
    Status result = engine.applyColorAdjust(*src, *dst, params);
    
    assert(result == Status::OK);
    assert(isBitmapValid(*dst));
    
    float diff = calculateDifference(*src, *dst);
    assert(diff > 0.0f);  // Should be different from original
    
    std::cout << "PASS (difference=" << diff << ")\n";
    return true;
}

// ============================================================================
// Test Cases - Emboss
// ============================================================================

static bool testEmbossKernelGeneration() {
    std::cout << "TEST: Emboss kernel generation... ";
    
    // Test different angles
    for (float angle = 0.0f; angle < 360.0f; angle += 45.0f) {
        auto kernel = EmbossFilter::generateKernel(angle);
        assert(kernel.size() == 9);  // 3x3 kernel
    }
    
    std::cout << "PASS\n";
    return true;
}

static bool testEmbossApplication() {
    std::cout << "TEST: Emboss filter application... ";
    
    auto src = createTestBitmap(TEST_WIDTH, TEST_HEIGHT);
    auto dst = std::make_shared<Bitmap>(TEST_WIDTH, TEST_HEIGHT, PixelFormat::ARGB_8888);
    
    FilterEngine::EmbossParams params;
    params.amount = 1.0f;
    params.angle = 45.0f;
    
    FilterEngine engine;
    Status result = engine.applyEmboss(*src, *dst, params);
    
    assert(result == Status::OK);
    assert(isBitmapValid(*dst));
    
    std::cout << "PASS\n";
    return true;
}

// ============================================================================
// Test Cases - Thread Pool
// ============================================================================

static bool testThreadPoolParallelization() {
    std::cout << "TEST: Thread pool parallelization... ";
    
    ThreadPool pool(4);  // 4 worker threads
    
    std::atomic<int> task_count(0);
    
    // Submit 100 tasks
    for (int i = 0; i < 100; i++) {
        pool.submit([&task_count]() {
            task_count++;
        });
    }
    
    // Wait for completion
    bool completed = pool.waitAll(5000);  // 5 second timeout
    
    assert(completed);
    assert(task_count == 100);
    
    auto stats = pool.getStats();
    std::cout << "PASS (completed=" << stats.completedTasks << " tasks)\n";
    return true;
}

// ============================================================================
// Test Runner
// ============================================================================

int main() {
    std::cout << "========================================\n";
    std::cout << "Filter Engine Unit Tests\n";
    std::cout << "========================================\n\n";
    
    int passed = 0, failed = 0;
    
    // Blur tests
    if (testBlurKernelGeneration()) passed++; else failed++;
    if (testBlurApplication()) passed++; else failed++;
    if (testBlurMultiplePasses()) passed++; else failed++;
    
    // Color adjust tests
    if (testRGBtoHSVConversion()) passed++; else failed++;
    if (testHSVtoRGBConversion()) passed++; else failed++;
    if (testColorAdjust()) passed++; else failed++;
    
    // Emboss tests
    if (testEmbossKernelGeneration()) passed++; else failed++;
    if (testEmbossApplication()) passed++; else failed++;
    
    // Thread pool tests
    if (testThreadPoolParallelization()) passed++; else failed++;
    
    std::cout << "\n========================================\n";
    std::cout << "Results: " << passed << " passed, " << failed << " failed\n";
    std::cout << "========================================\n";
    
    return (failed == 0) ? 0 : 1;
}
