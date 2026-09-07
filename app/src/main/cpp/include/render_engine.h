/**
 * render_engine.h
 * 
 * Rendering engine dengan layer caching dan dirty region detection
 * 
 * Arsitektur:
 * - RenderEngine: Main coordinator
 * - RenderCache: Layer cache management
 * - RenderContext: Per-frame rendering context
 * - DirtyRegion: Dirty region tracking
 */

#ifndef FLYERPIX_RENDER_ENGINE_H
#define FLYERPIX_RENDER_ENGINE_H

#include "flyerpix_types.h"
#include "bitmap.h"
#include <memory>

// Forward declarations
class Document;
class Layer;
class RenderContext;
class RenderCache;

/**
 * RenderEngine - Main rendering coordinator
 * 
 * Tanggung jawab:
 * - Coordinate layer rendering
 * - Manage render cache
 * - Track dirty regions
 * - Optimize rendering passes
 */
class RenderEngine {
public:
    RenderEngine();
    ~RenderEngine();
    
    // ========== Rendering ==========
    
    // Render document to bitmap with caching & dirty detection
    Status render(Document* doc, Bitmap* output);
    
    // Render specific region (optimized for partial updates)
    Status renderRegion(Document* doc, const Rect& region, Bitmap* output);
    
    // ========== Cache Management ==========
    
    // Clear all layer caches
    void clearCache();
    
    // Get cache statistics
    struct CacheStats {
        int totalCachedLayers = 0;
        size_t totalCacheMemory = 0;
        int hitCount = 0;
        int missCount = 0;
    };
    
    CacheStats getCacheStats() const;
    
    // ========== Performance Options ==========
    
    // Enable/disable layer caching
    void setCachingEnabled(bool enabled) { caching_enabled_ = enabled; }
    
    // Set max cache memory (in bytes)
    void setMaxCacheMemory(size_t bytes) { max_cache_memory_ = bytes; }
    
    // Enable/disable SIMD optimizations
    void setSIMDEnabled(bool enabled) { simd_enabled_ = enabled; }
    
private:
    std::unique_ptr<RenderCache> cache_;
    std::unique_ptr<RenderContext> render_ctx_;
    
    bool caching_enabled_ = true;
    bool simd_enabled_ = true;
    size_t max_cache_memory_ = 100 * 1024 * 1024;  // 100MB default
    
    // Internal rendering passes
    Status renderLayers(Document* doc, RenderContext* ctx);
    Status composeLayers(RenderContext* ctx, Bitmap* output);
};

/**
 * RenderContext - Per-frame rendering context
 * 
 * Contains temporary buffers and state for current render pass
 */
class RenderContext {
public:
    explicit RenderContext(int width, int height);
    ~RenderContext();
    
    // Get current frame bitmap
    Bitmap* getFrameBuffer() { return frame_buffer_.get(); }
    
    // Get layer-specific temp buffer
    Bitmap* getTempBuffer(int width, int height);
    
    // Mark region as rendered
    void markRendered(const Rect& region);
    
    // Get union of all rendered regions
    Rect getRenderedRegion() const { return rendered_region_; }
    
private:
    std::unique_ptr<Bitmap> frame_buffer_;
    std::unique_ptr<Bitmap> temp_buffer_;
    Rect rendered_region_;
};

/**
 * RenderCache - Intelligent layer caching system
 * 
 * Features:
 * - Per-layer bitmap caching
 * - Cache invalidation tracking
 * - Memory pressure handling
 * - LRU eviction
 */
class RenderCache {
public:
    explicit RenderCache(size_t maxMemory = 100 * 1024 * 1024);
    ~RenderCache();
    
    // ========== Cache Operations ==========
    
    // Get cached bitmap for layer (nullptr if not cached)
    Bitmap* getCachedBitmap(const Layer* layer) const;
    
    // Store bitmap for layer
    void cacheBitmap(const Layer* layer, std::shared_ptr<Bitmap> bitmap);
    
    // Invalidate cache for layer
    void invalidate(const Layer* layer);
    
    // Invalidate all cache
    void clearAll();
    
    // ========== Statistics ==========
    
    size_t getCurrentMemoryUsage() const { return current_memory_; }
    size_t getMaxMemory() const { return max_memory_; }
    int getCacheHitCount() const { return hit_count_; }
    int getCacheMissCount() const { return miss_count_; }
    
    // ========== Memory Management ==========
    
    // Evict least-recently-used entries if needed
    void evictIfNeeded(size_t requiredMemory);
    
private:
    struct CacheEntry {
        const Layer* layer;
        std::shared_ptr<Bitmap> bitmap;
        size_t memoryUsage;
        uint64_t lastAccessTime;
    };
    
    std::vector<CacheEntry> cache_entries_;
    size_t max_memory_;
    size_t current_memory_ = 0;
    int hit_count_ = 0;
    int miss_count_ = 0;
    
    uint64_t getCurrentTime() const;
};

/**
 * DirtyRegion - Efficient dirty region tracking
 * 
 * Tracks which regions of canvas need re-rendering
 */
class DirtyRegion {
public:
    DirtyRegion();
    
    // Mark region as dirty
    void markDirty(const Rect& region);
    
    // Mark all as dirty
    void markAllDirty(int canvasWidth, int canvasHeight);
    
    // Get dirty region
    const Rect& getDirtyRegion() const { return dirty_region_; }
    
    // Get dirty region count (for optimization decisions)
    int getDirtyRegionCount() const { return dirty_count_; }
    
    // Clear dirty region
    void clear() { 
        dirty_region_ = Rect(); 
        dirty_count_ = 0;
    }
    
    // Check if region is dirty
    bool isDirty(const Rect& region) const;
    
private:
    Rect dirty_region_;
    int dirty_count_ = 0;
};

#endif // FLYERPIX_RENDER_ENGINE_H
