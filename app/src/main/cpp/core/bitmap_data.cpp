/**
 * bitmap_data.cpp
 * 
 * Bitmap implementation dengan SIMD optimizations
 */

#include "bitmap.h"
#include "simd_utils.h"
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
    // If we own the buffer, unique_ptr will delete it automatically
    // If we don't own it (wrap()), we don't delete - just let it go out of scope
    if (owns_buffer_) {
        data_.reset();
    } else {
        // Release ownership without deleting
        data_.release();
    }
}

Bitmap::Bitmap(const Bitmap& other)
    : width_(other.width_), height_(other.height_), 
      stride_(other.stride_), format_(other.format_),
      bytes_per_pixel_(other.bytes_per_pixel_), owns_buffer_(true) {
    allocate();
    if (other.data_) {
        std::memcpy(data_.get(), other.data_.get(), getBufferSize());
    }
}

Bitmap& Bitmap::operator=(const Bitmap& other) {
    if (this != &other) {
        if (!owns_buffer_) {
            // Wrapped (external) buffer: copy pixels INTO it.
            // Never reallocate or free an external/Android buffer here.
            if (data_ && other.data_) {
                size_t bytes = std::min(getBufferSize(), other.getBufferSize());
                std::memcpy(data_.get(), other.data_.get(), bytes);
            }
            return *this;
        }
        width_ = other.width_;
        height_ = other.height_;
        stride_ = other.stride_;
        format_ = other.format_;
        bytes_per_pixel_ = other.bytes_per_pixel_;
        owns_buffer_ = true;  // Copy always owns its own buffer
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
      data_(std::move(other.data_)),
      owns_buffer_(other.owns_buffer_) {
    other.width_ = 0;
    other.height_ = 0;
}

Bitmap& Bitmap::operator=(Bitmap&& other) noexcept {
    if (this != &other) {
        if (!owns_buffer_) {
            // Wrapped (external) buffer: write other's pixels INTO it.
            // Do not free the external/Android buffer, do not steal the pointer.
            if (data_ && other.data_) {
                size_t bytes = std::min(getBufferSize(), other.getBufferSize());
                std::memcpy(data_.get(), other.data_.get(), bytes);
            }
            return *this;
        }
        width_ = other.width_;
        height_ = other.height_;
        stride_ = other.stride_;
        format_ = other.format_;
        bytes_per_pixel_ = other.bytes_per_pixel_;
        owns_buffer_ = other.owns_buffer_;
        data_ = std::move(other.data_);
        
        other.width_ = 0;
        other.height_ = 0;
    }
    return *this;
}

// ============================================================================
// Factory Methods
// ============================================================================

/**
 * Wrap external pixel buffer into Bitmap without allocation
 */
Bitmap Bitmap::wrap(int width, int height, int stride, uint8_t* buffer, PixelFormat format) {
    // Create using private constructor - non-owning
    return Bitmap(width, height, stride, buffer, format, false);
}

/**
 * Private constructor for wrap() - creates non-owning Bitmap
 */
Bitmap::Bitmap(int width, int height, int stride, uint8_t* buffer, PixelFormat format, bool owning)
    : width_(width), height_(height), stride_(stride), format_(format), owns_buffer_(owning) {
    
    bytes_per_pixel_ = getBytesPerPixel(format);
    
    if (owning) {
        // Normal case - allocate our own buffer
        allocate();
    } else {
        // Wrap mode - point to external buffer without owning it.
        // Adopt the raw pointer. owns_buffer_==false makes every managed
        // path (destructor, copy/move assignment) use release()/memcpy
        // instead of delete[], so the external/Android buffer is never
        // freed by this object.
        data_ = std::unique_ptr<uint8_t[]>(buffer);
    }
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
        // Use SIMD optimized fill
        int pixelCount = (height_ * stride_) / sizeof(Color32);
        simd_fill_32bit(data_.get(), color, pixelCount);
    } else if (format_ == PixelFormat::RGB_565) {
        uint16_t color16 = static_cast<uint16_t>(color);
        int pixelCount = (stride_ * height_) / sizeof(uint16_t);
        simd_fill_16bit(data_.get(), color16, pixelCount);
    } else {
        // GRAY_8 format
        std::memset(data_.get(), color & 0xFF, height_ * stride_);
    }
}

void Bitmap::fillRect(const Rect& rect, Color32 color) {
    if (!data_) return;
    
    int x1 = std::max(0, rect.left);
    int y1 = std::max(0, rect.top);
    int x2 = std::min(width_, rect.right);
    int y2 = std::min(height_, rect.bottom);
    
    if (x1 >= x2 || y1 >= y2) return;
    
    for (int y = y1; y < y2; y++) {
        if (format_ == PixelFormat::ARGB_8888) {
            Color32* scanline = reinterpret_cast<Color32*>(getScanline(y));
            std::fill(scanline + x1, scanline + x2, color);
        } else if (format_ == PixelFormat::RGB_565) {
            uint16_t* scanline = reinterpret_cast<uint16_t*>(getScanline(y));
            std::fill(scanline + x1, scanline + x2, static_cast<uint16_t>(color));
        } else {
            uint8_t* scanline = getScanline(y);
            std::fill(scanline + x1, scanline + x2, static_cast<uint8_t>(color & 0xFF));
        }
    }
}

// ============================================================================
// Conversion & Blitting
// ============================================================================

std::shared_ptr<Bitmap> Bitmap::convert(const Bitmap& src, PixelFormat newFormat) {
    if (src.format_ == newFormat) {
        return std::make_shared<Bitmap>(src);
    }
    
    auto dst = std::make_shared<Bitmap>(src.width_, src.height_, newFormat);
    
    if (src.format_ == PixelFormat::ARGB_8888 && newFormat == PixelFormat::RGB_565) {
        // ARGB_8888 → RGB_565
        const Color32* srcPixel = reinterpret_cast<const Color32*>(src.data_.get());
        uint16_t* dstPixel = reinterpret_cast<uint16_t*>(dst->data_.get());
        
        for (int i = 0; i < src.getPixelCount(); i++) {
            Color32 c = srcPixel[i];
            uint8_t r = (c >> 16) & 0xFF;
            uint8_t g = (c >> 8) & 0xFF;
            uint8_t b = c & 0xFF;
            dstPixel[i] = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3);
        }
    } else if (src.format_ == PixelFormat::RGB_565 && newFormat == PixelFormat::ARGB_8888) {
        // RGB_565 → ARGB_8888
        const uint16_t* srcPixel = reinterpret_cast<const uint16_t*>(src.data_.get());
        Color32* dstPixel = reinterpret_cast<Color32*>(dst->data_.get());
        
        for (int i = 0; i < src.getPixelCount(); i++) {
            uint16_t c = srcPixel[i];
            uint8_t r = ((c >> 11) & 0x1F) << 3;
            uint8_t g = ((c >> 5) & 0x3F) << 2;
            uint8_t b = (c & 0x1F) << 3;
            dstPixel[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    } else if (src.format_ == PixelFormat::ARGB_8888 && newFormat == PixelFormat::GRAY_8) {
        // ARGB_8888 → GRAY_8 (luminance)
        const Color32* srcPixel = reinterpret_cast<const Color32*>(src.data_.get());
        uint8_t* dstPixel = reinterpret_cast<uint8_t*>(dst->data_.get());
        
        for (int i = 0; i < src.getPixelCount(); i++) {
            Color32 c = srcPixel[i];
            uint8_t r = (c >> 16) & 0xFF;
            uint8_t g = (c >> 8) & 0xFF;
            uint8_t b = c & 0xFF;
            // Luminance formula: 0.299*R + 0.587*G + 0.114*B
            dstPixel[i] = (r * 77 + g * 150 + b * 29) >> 8;
        }
    }
    
    return dst;
}

void Bitmap::copyFrom(const Bitmap& src, int dstX, int dstY) {
    if (!data_ || !src.data_) return;
    
    // Compute safe bounds
    int srcW = std::min(src.width_, width_ - dstX);
    int srcH = std::min(src.height_, height_ - dstY);
    
    if (srcW <= 0 || srcH <= 0 || dstX < 0 || dstY < 0) return;
    
    // Copy scanline by scanline with SIMD if possible
    int copyBytes = srcW * bytes_per_pixel_;
    
    // Align to 32-byte boundaries for SIMD
    int alignedBytes = (copyBytes / 32) * 32;
    int remainingBytes = copyBytes % 32;
    
    for (int y = 0; y < srcH; y++) {
        const uint8_t* srcScan = src.getScanline(y);
        uint8_t* dstScan = getScanline(dstY + y) + (dstX * bytes_per_pixel_);
        
        // Copy aligned portion with SIMD
        if (alignedBytes > 0) {
            simd_copy(dstScan, srcScan, alignedBytes);
        }
        
        // Copy remaining bytes
        if (remainingBytes > 0) {
            std::memcpy(dstScan + alignedBytes, srcScan + alignedBytes, remainingBytes);
        }
    }
}

void Bitmap::blitAlpha(const Bitmap& src, int dstX, int dstY, uint8_t alpha) {
    if (!data_ || !src.data_ || format_ != PixelFormat::ARGB_8888) return;
    if (src.format_ != PixelFormat::ARGB_8888) return;
    
    int srcW = std::min(src.width_, width_ - dstX);
    int srcH = std::min(src.height_, height_ - dstY);
    if (srcW <= 0 || srcH <= 0 || dstX < 0 || dstY < 0) return;
    
    float alphaF = alpha / 255.0f;
    
    for (int y = 0; y < srcH; y++) {
        const Color32* srcScan = reinterpret_cast<const Color32*>(src.getScanline(y));
        Color32* dstScan = reinterpret_cast<Color32*>(getScanline(dstY + y));
        
        for (int x = 0; x < srcW; x++) {
            Color32 src_pixel = srcScan[x];
            Color32 dst_pixel = dstScan[dstX + x];
            
            // Extract components
            uint8_t sr = (src_pixel >> 16) & 0xFF;
            uint8_t sg = (src_pixel >> 8) & 0xFF;
            uint8_t sb = src_pixel & 0xFF;
            uint8_t sa = (src_pixel >> 24) & 0xFF;
            
            uint8_t dr = (dst_pixel >> 16) & 0xFF;
            uint8_t dg = (dst_pixel >> 8) & 0xFF;
            uint8_t db = dst_pixel & 0xFF;
            
            // Alpha blend
            uint8_t blended_alpha = static_cast<uint8_t>(sa * alphaF);
            float blend_f = blended_alpha / 255.0f;
            
            uint8_t r = static_cast<uint8_t>(sr * blend_f + dr * (1.0f - blend_f));
            uint8_t g = static_cast<uint8_t>(sg * blend_f + dg * (1.0f - blend_f));
            uint8_t b = static_cast<uint8_t>(sb * blend_f + db * (1.0f - blend_f));
            
            dstScan[dstX + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }
}

void Bitmap::blit(const Bitmap& src, int dstX, int dstY, BlendMode mode) {
    if (mode == BlendMode::NORMAL) {
        copyFrom(src, dstX, dstY);
    } else {
        // TODO: Implement other blend modes
        copyFrom(src, dstX, dstY);
    }
}

// ============================================================================
// Scaling
// ============================================================================

std::shared_ptr<Bitmap> Bitmap::scale(int newWidth, int newHeight) const {
    return scale(newWidth, newHeight, 0);  // Fast (nearest neighbor)
}

std::shared_ptr<Bitmap> Bitmap::scale(int newWidth, int newHeight, int quality) const {
    if (newWidth <= 0 || newHeight <= 0) return nullptr;
    if (newWidth == width_ && newHeight == height_) {
        return std::make_shared<Bitmap>(*this);
    }
    
    auto scaled = std::make_shared<Bitmap>(newWidth, newHeight, format_);
    
    if (quality == 0) {
        // Nearest neighbor scaling (fast)
        float scaleX = static_cast<float>(width_) / newWidth;
        float scaleY = static_cast<float>(height_) / newHeight;
        
        if (format_ == PixelFormat::ARGB_8888) {
            for (int y = 0; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    int srcX = static_cast<int>(x * scaleX);
                    int srcY = static_cast<int>(y * scaleY);
                    
                    // Clamp to valid range
                    srcX = std::min(srcX, width_ - 1);
                    srcY = std::min(srcY, height_ - 1);
                    
                    const Color32* srcPixel = getPixelAt(srcX, srcY);
                    Color32* dstPixel = scaled->getPixelAt(x, y);
                    *dstPixel = *srcPixel;
                }
            }
        } else {
            // Generic scanline copy for other formats
            for (int y = 0; y < newHeight; y++) {
                int srcY = static_cast<int>(y * scaleY);
                srcY = std::min(srcY, height_ - 1);
                
                const uint8_t* srcScan = getScanline(srcY);
                uint8_t* dstScan = scaled->getScanline(y);
                
                for (int x = 0; x < newWidth; x++) {
                    int srcX = static_cast<int>(x * scaleX);
                    srcX = std::min(srcX, width_ - 1);
                    
                    for (int b = 0; b < bytes_per_pixel_; b++) {
                        dstScan[x * bytes_per_pixel_ + b] = 
                            srcScan[srcX * bytes_per_pixel_ + b];
                    }
                }
            }
        }
    } else {
        // Bilinear scaling (higher quality)
        float scaleX = static_cast<float>(width_ - 1) / (newWidth - 1);
        float scaleY = static_cast<float>(height_ - 1) / (newHeight - 1);
        
        if (format_ == PixelFormat::ARGB_8888) {
            for (int y = 0; y < newHeight; y++) {
                float srcY = y * scaleY;
                int y1 = static_cast<int>(srcY);
                int y2 = std::min(y1 + 1, height_ - 1);
                float fy = srcY - y1;
                
                for (int x = 0; x < newWidth; x++) {
                    float srcX = x * scaleX;
                    int x1 = static_cast<int>(srcX);
                    int x2 = std::min(x1 + 1, width_ - 1);
                    float fx = srcX - x1;
                    
                    // Bilinear interpolation
                    Color32 c00 = *getPixelAt(x1, y1);
                    Color32 c10 = *getPixelAt(x2, y1);
                    Color32 c01 = *getPixelAt(x1, y2);
                    Color32 c11 = *getPixelAt(x2, y2);
                    
                    // Extract ARGB components
                    auto lerp_channel = [](uint8_t a, uint8_t b, float t) {
                        return static_cast<uint8_t>(a * (1.0f - t) + b * t);
                    };
                    
                    uint8_t c00_r = (c00 >> 16) & 0xFF, c00_g = (c00 >> 8) & 0xFF, c00_b = c00 & 0xFF;
                    uint8_t c10_r = (c10 >> 16) & 0xFF, c10_g = (c10 >> 8) & 0xFF, c10_b = c10 & 0xFF;
                    uint8_t c01_r = (c01 >> 16) & 0xFF, c01_g = (c01 >> 8) & 0xFF, c01_b = c01 & 0xFF;
                    uint8_t c11_r = (c11 >> 16) & 0xFF, c11_g = (c11 >> 8) & 0xFF, c11_b = c11 & 0xFF;
                    
                    uint8_t r0 = lerp_channel(c00_r, c10_r, fx);
                    uint8_t g0 = lerp_channel(c00_g, c10_g, fx);
                    uint8_t b0 = lerp_channel(c00_b, c10_b, fx);
                    
                    uint8_t r1 = lerp_channel(c01_r, c11_r, fx);
                    uint8_t g1 = lerp_channel(c01_g, c11_g, fx);
                    uint8_t b1 = lerp_channel(c01_b, c11_b, fx);
                    
                    uint8_t r = lerp_channel(r0, r1, fy);
                    uint8_t g = lerp_channel(g0, g1, fy);
                    uint8_t b = lerp_channel(b0, b1, fy);
                    
                    *scaled->getPixelAt(x, y) = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
    }
    
    return scaled;
}
