/**
 * render_engine.cpp
 * 
 * RenderEngine implementation
 */

#include "render_engine.h"
#include "document.h"
#include "layer.h"

// ============================================================================
// RenderEngine
// ============================================================================

RenderEngine::RenderEngine() {
    cache_ = std::make_unique<RenderCache>(max_cache_memory_);
}

RenderEngine::~RenderEngine() {
}

Status RenderEngine::render(Document* doc, Bitmap* output) {
    if (!doc || !output) return Status::ERROR_INVALID_PARAM;
    
    Rect region(0, 0, doc->getWidth(), doc->getHeight());
    return renderRegion(doc, region, output);
}

Status RenderEngine::renderRegion(Document* doc, const Rect& region, Bitmap* output) {
    if (!doc || !output) return Status::ERROR_INVALID_PARAM;
    
    // TODO: Implement actual rendering with layer composition
    // For now, just fill with background
    output->fill(doc->getBackgroundColor());
    doc->clearDirty();
    
    return Status::OK;
}

void RenderEngine::clearCache() {
    if (cache_) {
        cache_->clearAll();
    }
}

RenderEngine::CacheStats RenderEngine::getCacheStats() const {
    CacheStats stats;
    if (cache_) {
        stats.totalCacheMemory = cache_->getCurrentMemoryUsage();
        stats.hitCount = cache_->getCacheHitCount();
        stats.missCount = cache_->getCacheMissCount();
    }
    return stats;
}

Status RenderEngine::renderLayers(Document* doc, RenderContext* ctx) {
    // TODO: Implement layer rendering
    return Status::OK;
}

Status RenderEngine::composeLayers(RenderContext* ctx, Bitmap* output) {
    // TODO: Implement layer composition
    return Status::OK;
}

// ============================================================================
// RenderContext
// ============================================================================

RenderContext::RenderContext(int width, int height) {
    frame_buffer_ = std::make_unique<Bitmap>(width, height);
}

RenderContext::~RenderContext() {
}

Bitmap* RenderContext::getTempBuffer(int width, int height) {
    if (!temp_buffer_ || temp_buffer_->getWidth() != width || 
        temp_buffer_->getHeight() != height) {
        temp_buffer_ = std::make_unique<Bitmap>(width, height);
    }
    return temp_buffer_.get();
}

void RenderContext::markRendered(const Rect& region) {
    if (rendered_region_.isEmpty()) {
        rendered_region_ = region;
    } else {
        int left = std::min(rendered_region_.left, region.left);
        int top = std::min(rendered_region_.top, region.top);
        int right = std::max(rendered_region_.right, region.right);
        int bottom = std::max(rendered_region_.bottom, region.bottom);
        rendered_region_ = Rect(left, top, right, bottom);
    }
}

// ============================================================================
// RenderCache
// ============================================================================

RenderCache::RenderCache(size_t maxMemory)
    : max_memory_(maxMemory) {
}

RenderCache::~RenderCache() {
    clearAll();
}

Bitmap* RenderCache::getCachedBitmap(const Layer* layer) const {
    auto it = std::find_if(cache_entries_.begin(), cache_entries_.end(),
        [layer](const CacheEntry& e) { return e.layer == layer; });
    
    if (it != cache_entries_.end()) {
        const_cast<RenderCache*>(this)->miss_count_++;
        return it->bitmap.get();
    }
    
    const_cast<RenderCache*>(this)->hit_count_++;
    return nullptr;
}

void RenderCache::cacheBitmap(const Layer* layer, std::shared_ptr<Bitmap> bitmap) {
    if (!bitmap) return;
    
    // Remove existing cache for this layer
    auto it = std::find_if(cache_entries_.begin(), cache_entries_.end(),
        [layer](const CacheEntry& e) { return e.layer == layer; });
    
    if (it != cache_entries_.end()) {
        current_memory_ -= it->memoryUsage;
        cache_entries_.erase(it);
    }
    
    // Add new cache entry
    size_t memoryUsage = bitmap->getBufferSize();
    
    // Evict if needed
    evictIfNeeded(memoryUsage);
    
    cache_entries_.push_back({layer, bitmap, memoryUsage, getCurrentTime()});
    current_memory_ += memoryUsage;
}

void RenderCache::invalidate(const Layer* layer) {
    auto it = std::find_if(cache_entries_.begin(), cache_entries_.end(),
        [layer](const CacheEntry& e) { return e.layer == layer; });
    
    if (it != cache_entries_.end()) {
        current_memory_ -= it->memoryUsage;
        cache_entries_.erase(it);
    }
}

void RenderCache::clearAll() {
    cache_entries_.clear();
    current_memory_ = 0;
}

void RenderCache::evictIfNeeded(size_t requiredMemory) {
    while (current_memory_ + requiredMemory > max_memory_ && !cache_entries_.empty()) {
        // Find LRU entry
        auto lruIt = std::min_element(cache_entries_.begin(), cache_entries_.end(),
            [](const CacheEntry& a, const CacheEntry& b) {
                return a.lastAccessTime < b.lastAccessTime;
            });
        
        current_memory_ -= lruIt->memoryUsage;
        cache_entries_.erase(lruIt);
    }
}

uint64_t RenderCache::getCurrentTime() const {
    // TODO: Implement proper timestamp
    return 0;
}

// ============================================================================
// DirtyRegion
// ============================================================================

DirtyRegion::DirtyRegion() {
}

void DirtyRegion::markDirty(const Rect& region) {
    if (dirty_region_.isEmpty()) {
        dirty_region_ = region;
    } else {
        int left = std::min(dirty_region_.left, region.left);
        int top = std::min(dirty_region_.top, region.top);
        int right = std::max(dirty_region_.right, region.right);
        int bottom = std::max(dirty_region_.bottom, region.bottom);
        dirty_region_ = Rect(left, top, right, bottom);
    }
    dirty_count_++;
}

void DirtyRegion::markAllDirty(int canvasWidth, int canvasHeight) {
    dirty_region_ = Rect(0, 0, canvasWidth, canvasHeight);
    dirty_count_ = 1;
}

bool DirtyRegion::isDirty(const Rect& region) const {
    return dirty_region_.intersects(region);
}
