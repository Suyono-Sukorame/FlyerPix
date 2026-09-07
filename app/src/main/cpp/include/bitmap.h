/**
 * bitmap.h
 * 
 * Bitmap & BitmapData classes untuk FlyerPix
 */

#ifndef FLYERPIX_BITMAP_H
#define FLYERPIX_BITMAP_H

#include "flyerpix_types.h"
#include <memory>
#include <cstring>

/**
 * Bitmap - In-memory raster image buffer
 * 
 * Manages pixel data, format conversion, dan memory allocation.
 * Optimized untuk performance dengan aligned allocation.
 */
class Bitmap {
public:
    Bitmap(int width, int height, PixelFormat format = PixelFormat::ARGB_8888);
    ~Bitmap();
    
    // Copyable but expensive - prefer move semantics
    Bitmap(const Bitmap& other);
    Bitmap& operator=(const Bitmap& other);
    
    // Move semantics (efficient)
    Bitmap(Bitmap&& other) noexcept;
    Bitmap& operator=(Bitmap&& other) noexcept;
    
    // ========== Factory Methods ==========
    
    /**
     * Wrap external pixel buffer into Bitmap without allocation
     *
     * Creates a Bitmap that points to external pixel data (e.g., from Android Bitmap).
     * The external buffer MUST remain valid for the lifetime of this Bitmap.
     * The Bitmap will NOT free the buffer on destruction.
     *
     * @param width image width
     * @param height image height
     * @param stride bytes per row (must be >= width * bpp)
     * @param buffer external pixel buffer pointer
     * @param format pixel format (default ARGB_8888)
     * @return Bitmap instance (non-owning, don't delete the buffer)
     */
    static Bitmap wrap(int width, int height, int stride, uint8_t* buffer,
                       PixelFormat format = PixelFormat::ARGB_8888);
    
    /**
     * Check if this Bitmap owns its buffer (vs wrapping external buffer)
     */
    bool ownsBuffer() const { return owns_buffer_; }
    
    // ========== Properties ==========
    
    int getWidth() const { return width_; }
    int getHeight() const { return height_; }
    int getStride() const { return stride_; }
    PixelFormat getFormat() const { return format_; }
    int getBytesPerPixel() const { return bytes_per_pixel_; }
    
    Size getSize() const { return Size(width_, height_); }
    
    // Total pixel count
    int getPixelCount() const { return width_ * height_; }
    
    // Total buffer size in bytes
    size_t getBufferSize() const { return height_ * stride_; }
    
    // ========== Pixel Access ==========
    
    // Get pixel at position (unchecked - caller must validate bounds)
    Color32* getPixelAt(int x, int y) {
        return reinterpret_cast<Color32*>(data_.get() + y * stride_ + x * bytes_per_pixel_);
    }
    
    const Color32* getPixelAt(int x, int y) const {
        return reinterpret_cast<const Color32*>(data_.get() + y * stride_ + x * bytes_per_pixel_);
    }
    
    // Get entire scanline
    uint8_t* getScanline(int y) {
        return data_.get() + y * stride_;
    }
    
    const uint8_t* getScanline(int y) const {
        return data_.get() + y * stride_;
    }
    
    // ========== Buffer Access ==========
    
    // Get raw pixel buffer
    uint8_t* getBuffer() { return data_.get(); }
    const uint8_t* getBuffer() const { return data_.get(); }
    
    // ========== Fill & Clear ==========
    
    // Fill entire bitmap with color
    void fill(Color32 color);
    
    // Fill rectangle with color
    void fillRect(const Rect& rect, Color32 color);
    
    // Clear to transparent (0x00000000)
    void clear() { fill(0x00000000); }
    
    // ========== Conversion ==========
    
    // Convert format in-place or to new bitmap
    static std::shared_ptr<Bitmap> convert(const Bitmap& src, PixelFormat newFormat);
    
    // ========== Blitting ==========
    
    // Copy from another bitmap
    void copyFrom(const Bitmap& src, int dstX = 0, int dstY = 0);
    
    // Blit with alpha blending
    void blitAlpha(const Bitmap& src, int dstX, int dstY, uint8_t alpha = 255);
    
    // Blit with custom blend mode
    void blit(const Bitmap& src, int dstX, int dstY, BlendMode mode);
    
    // ========== Scaling ==========
    
    // Scale to new size (creates new bitmap)
    std::shared_ptr<Bitmap> scale(int newWidth, int newHeight) const;
    
    // Scale to new size with quality parameter (0=fast, 1=high)
    std::shared_ptr<Bitmap> scale(int newWidth, int newHeight, int quality) const;
    
private:
    int width_;
    int height_;
    int stride_;  // Bytes per row (may be > width * bpp for alignment)
    PixelFormat format_;
    int bytes_per_pixel_;
    
    std::unique_ptr<uint8_t[]> data_;
    bool owns_buffer_ = true;  // false if wrapping external buffer
    
    // Private constructor for wrap() factory (non-owning)
    Bitmap(int width, int height, int stride, uint8_t* buffer, PixelFormat format, bool owning);
    
    // Allocate aligned buffer
    void allocate();
    
    // Get stride for width (with alignment)
    static int calculateStride(int width, PixelFormat format);
    
    // Get bytes per pixel for format
    static int getBytesPerPixel(PixelFormat format);
};

#endif // FLYERPIX_BITMAP_H
