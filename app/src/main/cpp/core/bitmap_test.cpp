/**
 * bitmap_test.cpp
 * 
 * Unit tests untuk Bitmap class
 * Compile dengan: g++ -std=c++17 -I../include bitmap_data.cpp bitmap_test.cpp -o bitmap_test
 */

#include "../include/bitmap.h"
#include <cassert>
#include <iostream>
#include <chrono>
#include <iomanip>

// ============================================================================
// Test Utilities
// ============================================================================

void printTest(const char* name, bool passed) {
    std::cout << "[" << (passed ? "PASS" : "FAIL") << "] " << name << std::endl;
    assert(passed);
}

// ============================================================================
// Memory Management Tests
// ============================================================================

void test_bitmap_creation() {
    Bitmap bmp(100, 100, PixelFormat::ARGB_8888);
    printTest("Bitmap creation", bmp.getWidth() == 100 && bmp.getHeight() == 100);
}

void test_bitmap_stride() {
    Bitmap bmp(100, 100, PixelFormat::ARGB_8888);
    // Stride should be aligned to 32 bytes
    int stride = bmp.getStride();
    printTest("Bitmap stride alignment", stride % 32 == 0);
}

void test_bitmap_buffer_size() {
    Bitmap bmp(100, 100, PixelFormat::ARGB_8888);
    size_t expected = 100 * bmp.getStride();
    printTest("Bitmap buffer size", bmp.getBufferSize() == expected);
}

void test_bitmap_copy() {
    Bitmap bmp1(50, 50, PixelFormat::ARGB_8888);
    bmp1.fill(0xFF0000FF);  // Red
    
    Bitmap bmp2 = bmp1;  // Copy
    
    // Check if values match
    bool match = true;
    for (int i = 0; i < bmp1.getPixelCount(); i++) {
        if (*bmp1.getPixelAt(i % 50, i / 50) != *bmp2.getPixelAt(i % 50, i / 50)) {
            match = false;
            break;
        }
    }
    
    printTest("Bitmap copy constructor", match);
}

void test_bitmap_move() {
    Bitmap bmp1(50, 50, PixelFormat::ARGB_8888);
    bmp1.fill(0xFF00FF00);  // Green
    
    Bitmap bmp2 = std::move(bmp1);
    
    // bmp1 should be empty, bmp2 should have data
    printTest("Bitmap move semantics", bmp1.getWidth() == 0 && bmp2.getWidth() == 50);
}

// ============================================================================
// Fill Operations Tests
// ============================================================================

void test_fill_argb() {
    Bitmap bmp(100, 100, PixelFormat::ARGB_8888);
    Color32 color = 0xFF0000FF;  // Red
    bmp.fill(color);
    
    bool all_match = true;
    for (int y = 0; y < bmp.getHeight(); y++) {
        for (int x = 0; x < bmp.getWidth(); x++) {
            if (*bmp.getPixelAt(x, y) != color) {
                all_match = false;
                break;
            }
        }
    }
    
    printTest("Fill ARGB_8888", all_match);
}

void test_fill_rect() {
    Bitmap bmp(100, 100, PixelFormat::ARGB_8888);
    bmp.fill(0xFF000000);  // Black background
    
    Rect rect(10, 10, 50, 50);
    Color32 color = 0xFFFFFFFF;  // White
    bmp.fillRect(rect, color);
    
    // Check rect is filled
    bool rect_match = true;
    for (int y = 10; y < 50; y++) {
        for (int x = 10; x < 50; x++) {
            if (*bmp.getPixelAt(x, y) != color) {
                rect_match = false;
                break;
            }
        }
    }
    
    // Check corners are not filled
    bool corners_ok = *bmp.getPixelAt(0, 0) != color && 
                      *bmp.getPixelAt(99, 99) != color;
    
    printTest("Fill rectangle", rect_match && corners_ok);
}

void test_clear() {
    Bitmap bmp(50, 50, PixelFormat::ARGB_8888);
    bmp.fill(0xFFFFFFFF);
    bmp.clear();
    
    bool all_transparent = true;
    for (int i = 0; i < bmp.getPixelCount(); i++) {
        int y = i / bmp.getWidth();
        int x = i % bmp.getWidth();
        if (*bmp.getPixelAt(x, y) != 0x00000000) {
            all_transparent = false;
            break;
        }
    }
    
    printTest("Clear to transparent", all_transparent);
}

// ============================================================================
// Copy/Blit Operations Tests
// ============================================================================

void test_copy_from() {
    Bitmap src(50, 50, PixelFormat::ARGB_8888);
    src.fill(0xFF0000FF);
    
    Bitmap dst(100, 100, PixelFormat::ARGB_8888);
    dst.fill(0xFF000000);
    
    dst.copyFrom(src, 25, 25);
    
    // Check if source copied to correct position
    bool copied = *dst.getPixelAt(25, 25) == 0xFF0000FF;
    bool background_ok = *dst.getPixelAt(0, 0) == 0xFF000000;
    
    printTest("Copy from", copied && background_ok);
}

void test_blit_alpha() {
    Bitmap src(50, 50, PixelFormat::ARGB_8888);
    src.fill(0xFF0000FF);  // Red
    
    Bitmap dst(100, 100, PixelFormat::ARGB_8888);
    dst.fill(0xFF00FF00);  // Green
    
    dst.blitAlpha(src, 25, 25, 128);  // 50% alpha
    
    // Check if pixel at blit position is blended
    Color32 result = *dst.getPixelAt(50, 50);
    
    // With 50% alpha blend, result should be close to both colors
    bool blended = (result & 0x00FF0000) > 0 && (result & 0x0000FF00) > 0;
    
    printTest("Alpha blit", blended);
}

// ============================================================================
// Format Conversion Tests
// ============================================================================

void test_convert_argb_to_rgb565() {
    Bitmap src(50, 50, PixelFormat::ARGB_8888);
    src.fill(0xFFFF0000);  // Red
    
    auto dst = Bitmap::convert(src, PixelFormat::RGB_565);
    
    printTest("Convert ARGB_8888 to RGB_565", 
              dst != nullptr && dst->getFormat() == PixelFormat::RGB_565);
}

void test_convert_argb_to_gray() {
    Bitmap src(50, 50, PixelFormat::ARGB_8888);
    src.fill(0xFFFFFFFF);  // White
    
    auto dst = Bitmap::convert(src, PixelFormat::GRAY_8);
    
    printTest("Convert ARGB_8888 to GRAY_8",
              dst != nullptr && dst->getFormat() == PixelFormat::GRAY_8);
}

// ============================================================================
// Scaling Tests
// ============================================================================

void test_scale_nearest() {
    Bitmap src(100, 100, PixelFormat::ARGB_8888);
    src.fill(0xFF0000FF);
    
    auto scaled = src.scale(50, 50, 0);  // Nearest neighbor
    
    printTest("Scale nearest neighbor",
              scaled != nullptr && scaled->getWidth() == 50 && scaled->getHeight() == 50);
}

void test_scale_bilinear() {
    Bitmap src(100, 100, PixelFormat::ARGB_8888);
    src.fill(0xFF0000FF);
    
    auto scaled = src.scale(200, 200, 1);  // Bilinear
    
    printTest("Scale bilinear",
              scaled != nullptr && scaled->getWidth() == 200 && scaled->getHeight() == 200);
}

// ============================================================================
// Benchmark Tests
// ============================================================================

void benchmark_fill() {
    Bitmap bmp(4000, 4000, PixelFormat::ARGB_8888);
    Color32 color = 0xFF0000FF;
    
    auto start = std::chrono::high_resolution_clock::now();
    bmp.fill(color);
    auto end = std::chrono::high_resolution_clock::now();
    
    auto duration_ms = std::chrono::duration<double, std::milli>(end - start).count();
    
    std::cout << std::fixed << std::setprecision(2);
    std::cout << "[BENCHMARK] Fill 4000x4000: " << duration_ms << " ms" << std::endl;
}

void benchmark_copy() {
    Bitmap src(2000, 2000, PixelFormat::ARGB_8888);
    src.fill(0xFF0000FF);
    
    Bitmap dst(4000, 4000, PixelFormat::ARGB_8888);
    
    auto start = std::chrono::high_resolution_clock::now();
    dst.copyFrom(src, 1000, 1000);
    auto end = std::chrono::high_resolution_clock::now();
    
    auto duration_ms = std::chrono::duration<double, std::milli>(end - start).count();
    
    std::cout << std::fixed << std::setprecision(2);
    std::cout << "[BENCHMARK] Copy 2000x2000: " << duration_ms << " ms" << std::endl;
}

void benchmark_scale() {
    Bitmap src(2000, 2000, PixelFormat::ARGB_8888);
    src.fill(0xFF0000FF);
    
    auto start = std::chrono::high_resolution_clock::now();
    auto scaled = src.scale(1000, 1000, 0);  // Nearest neighbor
    auto end = std::chrono::high_resolution_clock::now();
    
    auto duration_ms = std::chrono::duration<double, std::milli>(end - start).count();
    
    std::cout << std::fixed << std::setprecision(2);
    std::cout << "[BENCHMARK] Scale 2000x2000 → 1000x1000: " << duration_ms << " ms" << std::endl;
}

// ============================================================================
// Main Test Runner
// ============================================================================

int main() {
    std::cout << "========== FlyerPix Bitmap Unit Tests ==========" << std::endl;
    
    std::cout << "\n--- Memory Management Tests ---" << std::endl;
    test_bitmap_creation();
    test_bitmap_stride();
    test_bitmap_buffer_size();
    test_bitmap_copy();
    test_bitmap_move();
    
    std::cout << "\n--- Fill Operations Tests ---" << std::endl;
    test_fill_argb();
    test_fill_rect();
    test_clear();
    
    std::cout << "\n--- Copy/Blit Operations Tests ---" << std::endl;
    test_copy_from();
    test_blit_alpha();
    
    std::cout << "\n--- Format Conversion Tests ---" << std::endl;
    test_convert_argb_to_rgb565();
    test_convert_argb_to_gray();
    
    std::cout << "\n--- Scaling Tests ---" << std::endl;
    test_scale_nearest();
    test_scale_bilinear();
    
    std::cout << "\n--- Benchmarks ---" << std::endl;
    benchmark_fill();
    benchmark_copy();
    benchmark_scale();
    
    std::cout << "\n========== All tests passed! ==========" << std::endl;
    
    return 0;
}
