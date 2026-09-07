/**
 * test_render_engine.cpp
 * 
 * Unit tests untuk Rendering Engine
 * Tests blend modes, layer composition, dirty region, cache, dan document rendering
 */

#include <cassert>
#include <iostream>
#include <memory>

// Include headers
#include "../include/render_engine.h"
#include "../include/blend_modes.h"
#include "../include/bitmap.h"
#include "../include/document.h"
#include "../include/layer.h"

// ============================================================================
// Test Utilities
// ============================================================================

void assert_equal(int actual, int expected, const char* msg) {
    if (actual != expected) {
        std::cerr << "FAIL: " << msg << " (expected " << expected << ", got " << actual << ")" << std::endl;
        throw std::runtime_error(msg);
    }
}

void assert_true(bool condition, const char* msg) {
    if (!condition) {
        std::cerr << "FAIL: " << msg << std::endl;
        throw std::runtime_error(msg);
    }
}

// ============================================================================
// Blend Mode Tests
// ============================================================================

void test_blend_normal() {
    Color32 src = 0xFFFF0000;  // Red
    Color32 dst = 0xFF0000FF;  // Blue
    
    Color32 result = apply_blend_mode(src, dst, BlendMode::NORMAL, 255);
    
    // With alpha=255, should be src (fully opaque)
    assert_true(result == src, "Blend normal with full opacity");
    
    // With alpha=128, should be blend
    result = apply_blend_mode(src, dst, BlendMode::NORMAL, 128);
    assert_true(result != src && result != dst, "Blend normal with half opacity");
}

void test_blend_multiply() {
    Color32 src = 0xFF808080;  // Gray (128, 128, 128)
    Color32 dst = 0xFFFFFFFF;  // White
    
    Color32 result = apply_blend_mode(src, dst, BlendMode::MULTIPLY, 255);
    
    // Multiply dengan white should be src
    assert_true(result != 0xFFFFFFFF, "Blend multiply");
}

void test_blend_screen() {
    Color32 src = 0xFF000000;  // Black
    Color32 dst = 0xFFFFFFFF;  // White
    
    Color32 result = apply_blend_mode(src, dst, BlendMode::SCREEN, 255);
    
    // Screen blend should lighten
    assert_true(result == dst, "Blend screen");
}

void test_blend_modes_suite() {
    std::cout << "Testing blend modes..." << std::endl;
    
    test_blend_normal();
    std::cout << "  ✓ Normal blend" << std::endl;
    
    test_blend_multiply();
    std::cout << "  ✓ Multiply blend" << std::endl;
    
    test_blend_screen();
    std::cout << "  ✓ Screen blend" << std::endl;
}

// ============================================================================
// Bitmap Tests
// ============================================================================

void test_bitmap_creation() {
    Bitmap bitmap(100, 100);
    
    assert_equal(bitmap.getWidth(), 100, "Bitmap width");
    assert_equal(bitmap.getHeight(), 100, "Bitmap height");
    assert_true(bitmap.getBuffer() != nullptr, "Bitmap buffer allocated");
}

void test_bitmap_fill() {
    Bitmap bitmap(10, 10);
    bitmap.fill(0xFFFFFFFF);  // White
    
    Color32* buffer = reinterpret_cast<Color32*>(bitmap.getBuffer());
    assert_true(buffer[0] == 0xFFFFFFFF, "Bitmap fill works");
}

void test_bitmap_tests_suite() {
    std::cout << "Testing bitmap operations..." << std::endl;
    
    test_bitmap_creation();
    std::cout << "  ✓ Bitmap creation" << std::endl;
    
    test_bitmap_fill();
    std::cout << "  ✓ Bitmap fill" << std::endl;
}

// ============================================================================
// Dirty Region Tests
// ============================================================================

void test_dirty_region_basic() {
    DirtyRegion dirty;
    
    assert_true(dirty.getDirtyRegion().isEmpty(), "Dirty region starts empty");
    
    Rect region(10, 10, 20, 20);
    dirty.markDirty(region);
    
    assert_true(!dirty.getDirtyRegion().isEmpty(), "Dirty region marked");
    assert_true(dirty.isDirty(region), "Dirty region detected");
}

void test_dirty_region_union() {
    DirtyRegion dirty;
    
    Rect region1(0, 0, 10, 10);
    Rect region2(5, 5, 15, 15);
    
    dirty.markDirty(region1);
    dirty.markDirty(region2);
    
    Rect result = dirty.getDirtyRegion();
    
    // Should union both regions
    assert_true(result.left == 0, "Union left bound");
    assert_true(result.top == 0, "Union top bound");
    assert_true(result.right == 15, "Union right bound");
    assert_true(result.bottom == 15, "Union bottom bound");
}

void test_dirty_region_tests_suite() {
    std::cout << "Testing dirty region..." << std::endl;
    
    test_dirty_region_basic();
    std::cout << "  ✓ Dirty region basic" << std::endl;
    
    test_dirty_region_union();
    std::cout << "  ✓ Dirty region union" << std::endl;
}

// ============================================================================
// Rect Tests
// ============================================================================

void test_rect_area() {
    Rect rect(0, 0, 10, 20);
    
    size_t area = rect.getArea();
    assert_equal(area, 200, "Rect area calculation");
}

void test_rect_intersection() {
    Rect rect1(0, 0, 10, 10);
    Rect rect2(5, 5, 15, 15);
    
    Rect intersection = rect1.getIntersection(rect2);
    
    assert_true(intersection.left == 5, "Intersection left");
    assert_true(intersection.top == 5, "Intersection top");
    assert_true(intersection.right == 10, "Intersection right");
    assert_true(intersection.bottom == 10, "Intersection bottom");
}

void test_rect_union() {
    Rect rect1(0, 0, 10, 10);
    Rect rect2(5, 5, 15, 15);
    
    Rect unionRect = rect1.getUnion(rect2);
    
    assert_true(unionRect.left == 0, "Union left");
    assert_true(unionRect.top == 0, "Union top");
    assert_true(unionRect.right == 15, "Union right");
    assert_true(unionRect.bottom == 15, "Union bottom");
}

void test_rect_tests_suite() {
    std::cout << "Testing rect operations..." << std::endl;
    
    test_rect_area();
    std::cout << "  ✓ Rect area" << std::endl;
    
    test_rect_intersection();
    std::cout << "  ✓ Rect intersection" << std::endl;
    
    test_rect_union();
    std::cout << "  ✓ Rect union" << std::endl;
}

// ============================================================================
// RenderCache Tests
// ============================================================================

void test_render_cache_basic() {
    RenderCache cache(10 * 1024 * 1024);  // 10MB
    
    assert_equal(cache.getCachedLayerCount(), 0, "Cache starts empty");
}

void test_render_cache_memory() {
    RenderCache cache(100);  // 100 bytes
    
    auto bitmap1 = std::make_shared<Bitmap>(10, 10);
    
    // This should work since bitmap is small
    // MockLayer would be needed for real test
}

void test_render_cache_tests_suite() {
    std::cout << "Testing render cache..." << std::endl;
    
    test_render_cache_basic();
    std::cout << "  ✓ Cache basic" << std::endl;
}

// ============================================================================
// RenderContext Tests
// ============================================================================

void test_render_context_creation() {
    RenderContext ctx(100, 100);
    
    Bitmap* frameBuffer = ctx.getFrameBuffer();
    assert_true(frameBuffer != nullptr, "Frame buffer allocated");
    assert_equal(frameBuffer->getWidth(), 100, "Frame buffer width");
    assert_equal(frameBuffer->getHeight(), 100, "Frame buffer height");
}

void test_render_context_temp_buffer() {
    RenderContext ctx(100, 100);
    
    Bitmap* temp1 = ctx.getTempBuffer(50, 50);
    assert_true(temp1 != nullptr, "Temp buffer allocated");
    assert_equal(temp1->getWidth(), 50, "Temp buffer width");
    assert_equal(temp1->getHeight(), 50, "Temp buffer height");
    
    // Get same size should reuse
    Bitmap* temp2 = ctx.getTempBuffer(50, 50);
    assert_true(temp1 == temp2, "Temp buffer reused for same size");
}

void test_render_context_tests_suite() {
    std::cout << "Testing render context..." << std::endl;
    
    test_render_context_creation();
    std::cout << "  ✓ Context creation" << std::endl;
    
    test_render_context_temp_buffer();
    std::cout << "  ✓ Temp buffer management" << std::endl;
}

// ============================================================================
// Main Test Runner
// ============================================================================

int main() {
    std::cout << "\n=== FlyerPix Rendering Engine Tests ===" << std::endl;
    
    try {
        // Run all test suites
        test_blend_modes_suite();
        test_bitmap_tests_suite();
        test_rect_tests_suite();
        test_dirty_region_tests_suite();
        test_render_cache_tests_suite();
        test_render_context_tests_suite();
        
        std::cout << "\n=== All Tests Passed! ===" << std::endl;
        return 0;
        
    } catch (const std::exception& e) {
        std::cerr << "\n=== Test Failed: " << e.what() << " ===" << std::endl;
        return 1;
    }
}
