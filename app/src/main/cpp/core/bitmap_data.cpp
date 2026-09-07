/**
 * bitmap_data.cpp
 * 
 * Bitmap implementation
 */

#include "bitmap.h"
#include <cstring>
#include <algorithm>

// ============================================================================
// Bitmap construction & destruction
// ============================================================================

Bitmap::Bitmap(int width, int height, PixelFormat format)
    : width_(width), height_(height), format_(format) {
    bytes_per_pixel_ = getBytesPerPixel(format);
    stride_ = calculateStride(width, format);
    allocate();
}

Bitmap::~Bitmap() {
    data_.reset();
}

Bitmap::Bitmap(const Bitmap& other)
    : width_(other.width_), height_(other.height_), 
      stride_(other.stride_), format_(other.format_),
      bytes_per_pixel_(other.bytes_per_pixel_) {
    allocate();
    if (other.data_) {
        std::memcpy(data_.get(), other.data_.get(), getBufferSize());
    }
}

Bitmap& Bitmap::operator=(const Bitmap& other) {
    if (this != &other) {
        width_ = other.width_;
        height_ = other.height_;
        stride_ = other.stride_;
        format_ = other.format_;
        bytes_per_pixel_ = other.bytes_per_pixel_;
        allocate();
        if (other.data_) {
            std::memcpy(data_.get(), other.data_.get(), getBufferSize());
        }
    }
    return *this;
}

Bitmap::Bitmap(Bitmap&& other) noexcept
    : width_(other.width_), height_(other.height_),
      stride_(other.stride_), format_(other.format_),
      bytes_per_pixel_(other.bytes_per_pixel_),
      data_(std::move(other.data_)) {
    other.width_ = 0;
    other.height_ = 0;
}

Bitmap& Bitmap::operator=(Bitmap&& other) noexcept {
    if (this != &other) {
        width_ = other.width_;
        height_ = other.height_;
        stride_ = other.stride_;
        format_ = other.format_;
        bytes_per_pixel_ = other.bytes_per_pixel_;
        data_ = std::move(other.data_);
        
        other.width_ = 0;
        other.height_ = 0;
    }
    return *this;
}

// ============================================================================
// Private helper methods
// ============================================================================

void Bitmap::allocate() {
    data_.reset(new uint8_t[getBufferSize()]);
    clear();
}

int Bitmap::calculateStride(int width, PixelFormat format) {
    int bpp = getBytesPerPixel(format);
    int stride = width * bpp;
    
    // Align to 32-byte boundary for SIMD optimization
    const int ALIGNMENT = 32;
    return ((stride + ALIGNMENT - 1) / ALIGNMENT) * ALIGNMENT;
}

int Bitmap::getBytesPerPixel(PixelFormat format) {
    switch (format) {
        case PixelFormat::ARGB_8888: return 4;
        case PixelFormat::RGB_565: return 2;
        case PixelFormat::GRAY_8: return 1;
        default: return 4;
    }
}

// ============================================================================
// Fill & Clear operations
// ============================================================================

void Bitmap::fill(Color32 color) {
    if (!data_) return;
    
    if (format_ == PixelFormat::ARGB_8888) {
        Color32* pixel = reinterpret_cast<Color32*>(data_.get());
        int pixelCount = (stride_ * height_) / sizeof(Color32);
        std::fill(pixel, pixel + pixelCount, color);
    }
}

void Bitmap::fillRect(const Rect& rect, Color32 color) {
    if (!data_) return;
    
    int x1 = std::max(0, rect.left);
    int y1 = std::max(0, rect.top);
    int x2 = std::min(width_, rect.right);
    int y2 = std::min(height_, rect.bottom);
    
    for (int y = y1; y < y2; y++) {
        Color32* scanline = reinterpret_cast<Color32*>(getScanline(y));
        std::fill(scanline + x1, scanline + x2, color);
    }
}

// ============================================================================
// Conversion & Blitting
// ============================================================================

std::shared_ptr<Bitmap> Bitmap::convert(const Bitmap& src, PixelFormat newFormat) {
    // TODO: Implement format conversion
    return std::make_shared<Bitmap>(src.width_, src.height_, newFormat);
}

void Bitmap::copyFrom(const Bitmap& src, int dstX, int dstY) {
    // TODO: Implement copy with format handling
}

void Bitmap::blitAlpha(const Bitmap& src, int dstX, int dstY, uint8_t alpha) {
    // TODO: Implement alpha blitting
}

void Bitmap::blit(const Bitmap& src, int dstX, int dstY, BlendMode mode) {
    // TODO: Implement blend mode blitting
}

// ============================================================================
// Scaling
// ============================================================================

std::shared_ptr<Bitmap> Bitmap::scale(int newWidth, int newHeight) const {
    return scale(newWidth, newHeight, 1);
}

std::shared_ptr<Bitmap> Bitmap::scale(int newWidth, int newHeight, int quality) const {
    // TODO: Implement scaling with quality parameter
    return std::make_shared<Bitmap>(newWidth, newHeight, format_);
}
