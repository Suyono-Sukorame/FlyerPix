/**
 * document.h
 * 
 * Document model untuk FlyerPix - container untuk canvas dan layers
 */

#ifndef FLYERPIX_DOCUMENT_H
#define FLYERPIX_DOCUMENT_H

#include "flyerpix_types.h"
#include <string>
#include <vector>
#include <memory>

// Forward declaration
class Layer;
class Bitmap;

/**
 * Document - Root container untuk canvas dan layers
 * 
 * Tanggung jawab:
 * - Manage canvas size dan properties
 * - Manage layer collection
 * - Handle dirty region tracking
 * - Coordinate rendering
 */
class Document {
public:
    Document(int width, int height);
    ~Document();
    
    // ========== Canvas Properties ==========
    
    int getWidth() const { return width_; }
    int getHeight() const { return height_; }
    Size getSize() const { return Size(width_, height_); }
    
    void setSize(int width, int height);
    
    // Canvas background color (ARGB)
    Color32 getBackgroundColor() const { return bg_color_; }
    void setBackgroundColor(Color32 color) { bg_color_ = color; }
    
    // ========== Layer Management ==========
    
    // Add layer (takes ownership)
    void addLayer(std::shared_ptr<Layer> layer);
    
    // Insert layer at specific index
    void insertLayer(int index, std::shared_ptr<Layer> layer);
    
    // Remove layer by index
    void removeLayer(int index);
    
    // Get layer by index
    std::shared_ptr<Layer> getLayer(int index) const;
    
    // Get total layer count
    int getLayerCount() const { return layers_.size(); }
    
    // Move layer to different position
    void moveLayer(int fromIndex, int toIndex);
    
    // ========== Dirty Region Tracking ==========
    
    // Mark region as dirty (needs re-render)
    void markDirty(const Rect& region);
    
    // Mark entire canvas as dirty
    void markAllDirty();
    
    // Get accumulated dirty region
    const Rect& getDirtyRegion() const { return dirty_region_; }
    
    // Clear dirty region (call after rendering)
    void clearDirty() { dirty_region_ = Rect(); }
    
    // ========== Rendering ==========
    
    // Render entire document to bitmap
    Status render(Bitmap* output);
    
    // Render specific region to bitmap
    Status renderRegion(const Rect& region, Bitmap* output);
    
private:
    int width_;
    int height_;
    Color32 bg_color_ = 0xFFFFFFFF;  // White background
    
    std::vector<std::shared_ptr<Layer>> layers_;
    
    Rect dirty_region_;  // Accumulated dirty region
};

#endif // FLYERPIX_DOCUMENT_H
