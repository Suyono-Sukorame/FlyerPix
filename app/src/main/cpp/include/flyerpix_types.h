/**
 * flyerpix_types.h
 * 
 * Core type definitions untuk FlyerPix C++ Engine
 * Digunakan oleh Document, Render, Bitmap, dan Filter modules
 */

#ifndef FLYERPIX_TYPES_H
#define FLYERPIX_TYPES_H

#include <cstdint>
#include <vector>
#include <memory>
#include <algorithm>

// ============================================================================
// Color & Pixel Formats
// ============================================================================

/** 32-bit ARGB color (A=MSB, R, G, B=LSB) */
using Color32 = uint32_t;

/** 8-bit grayscale value */
using Gray8 = uint8_t;

/** Pixel format untuk bitmap */
enum class PixelFormat {
    ARGB_8888,      // 32-bit: ARGB
    RGB_565,        // 16-bit: RGB
    GRAY_8          // 8-bit: Grayscale
};

// ============================================================================
// Geometry Types
// ============================================================================

/** 2D Integer Point */
struct Point {
    int x = 0;
    int y = 0;
    
    Point() = default;
    Point(int x_, int y_) : x(x_), y(y_) {}
    
    bool operator==(const Point& other) const {
        return x == other.x && y == other.y;
    }
};

/** 2D Float Point untuk transformations */
struct PointF {
    float x = 0.0f;
    float y = 0.0f;
    
    PointF() = default;
    PointF(float x_, float y_) : x(x_), y(y_) {}
};

/** Rectangle (integer) */
struct Rect {
    int left = 0;
    int top = 0;
    int right = 0;
    int bottom = 0;
    
    Rect() = default;
    Rect(int l, int t, int r, int b) 
        : left(l), top(t), right(r), bottom(b) {}
    
    int width() const { return right - left; }
    int height() const { return bottom - top; }
    bool isEmpty() const { return width() <= 0 || height() <= 0; }
    
    size_t getArea() const {
        if (isEmpty()) return 0;
        return width() * height();
    }
    
    bool intersects(const Rect& other) const {
        return left < other.right && right > other.left &&
               top < other.bottom && bottom > other.top;
    }
    
    Rect getIntersection(const Rect& other) const {
        if (!intersects(other)) {
            return Rect(0, 0, 0, 0);
        }
        int l = std::max(left, other.left);
        int t = std::max(top, other.top);
        int r = std::min(right, other.right);
        int b = std::min(bottom, other.bottom);
        return Rect(l, t, r, b);
    }
    
    Rect getUnion(const Rect& other) const {
        if (isEmpty()) return other;
        if (other.isEmpty()) return *this;
        int l = std::min(left, other.left);
        int t = std::min(top, other.top);
        int r = std::max(right, other.right);
        int b = std::max(bottom, other.bottom);
        return Rect(l, t, r, b);
    }
};

/** Rectangle (float) */
struct RectF {
    float left = 0.0f;
    float top = 0.0f;
    float right = 0.0f;
    float bottom = 0.0f;
    
    RectF() = default;
    RectF(float l, float t, float r, float b)
        : left(l), top(t), right(r), bottom(b) {}
    
    float width() const { return right - left; }
    float height() const { return bottom - top; }
};

/** 2D Size */
struct Size {
    int width = 0;
    int height = 0;
    
    Size() = default;
    Size(int w, int h) : width(w), height(h) {}
};

// ============================================================================
// Transform Matrix (4x4 for 2D transforms)
// ============================================================================

struct Matrix4 {
    float m[16] = {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    };
    
    Matrix4() = default;
    
    // Identity
    static Matrix4 identity();
    
    // Transformations
    static Matrix4 translate(float tx, float ty);
    static Matrix4 scale(float sx, float sy);
    static Matrix4 rotate(float angle);  // angle in degrees
    
    // Matrix multiplication
    Matrix4 operator*(const Matrix4& other) const;
};

// ============================================================================
// Blend Modes
// ============================================================================

enum class BlendMode {
    NORMAL,         // Alpha blend (default)
    MULTIPLY,       // Multiply blend
    SCREEN,         // Screen blend
    OVERLAY,        // Overlay blend
    ADD,            // Additive blend
    SUBTRACT,       // Subtractive blend
    LIGHTEN,        // Lighten blend
    DARKEN          // Darken blend
};

// ============================================================================
// Filter Types
// ============================================================================

enum class FilterType {
    BLUR,           // Gaussian blur
    COLOR_ADJUST,   // Brightness, contrast, saturation
    EMBOSS,         // Emboss effect
    GRAYSCALE,      // Convert to grayscale
    INVERT,         // Invert colors
    SEPIA           // Sepia tone
};

// ============================================================================
// Layer Flags
// ============================================================================

struct LayerFlags {
    uint32_t visible : 1;
    uint32_t locked : 1;
    uint32_t dirty : 1;
    uint32_t reserved : 29;
    
    LayerFlags() : visible(1), locked(0), dirty(1), reserved(0) {}
};

// ============================================================================
// Transform Data
// ============================================================================

struct Transform {
    float translateX = 0.0f;
    float translateY = 0.0f;
    float scaleX = 1.0f;
    float scaleY = 1.0f;
    float rotation = 0.0f;      // degrees
    float skewX = 0.0f;
    float skewY = 0.0f;
    float opacity = 1.0f;
    
    // TODO: Implement matrix conversion
    // Matrix4 toMatrix() const;
};

// ============================================================================
// Status Codes
// ============================================================================

enum class Status {
    OK = 0,
    ERROR_INVALID_PARAM = -1,
    ERROR_OUT_OF_MEMORY = -2,
    ERROR_FILE_NOT_FOUND = -3,
    ERROR_UNSUPPORTED_FORMAT = -4,
    ERROR_RENDERING_FAILED = -5
};

#endif // FLYERPIX_TYPES_H
