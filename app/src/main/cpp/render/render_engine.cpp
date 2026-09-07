/**
 * render_engine.cpp
 * 
 * RenderEngine implementation dengan layer composition
 */

#include "render_engine.h"
#include "document.h"
#include "layer.h"
#include "blend_modes.h"
#include <algorithm>

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
    
    // Use dirty detection untuk optimization
    return renderWithDirtyDetection(doc, output);
}

Status RenderEngine::renderRegion(Document* doc, const Rect& region, Bitmap* output) {
    if (!doc || !output) return Status::ERROR_INVALID_PARAM;
    if (region.isEmpty()) return Status::OK;
    
    // Check dirty region untuk optimization
    const Rect& dirtyRegion = doc->getDirtyRegion();
    if (dirtyRegion.isEmpty()) {
        // Nothing to render
        return Status::OK;
    }
    
    // Only render if region intersects dengan dirty area
    if (!region.intersects(dirtyRegion)) {
        return Status::OK;
    }
    
    // Create render context
    auto ctx = std::make_unique<RenderContext>(doc->getWidth(), doc->getHeight());
    
    // Fill output dengan background color
    output->fill(doc->getBackgroundColor());
    
    // Render layers
    Status status = renderLayers(doc, ctx.get());
    if (status != Status::OK) return status;
    
    // Compose layers ke output
    status = composeLayers(ctx.get(), output);
    
    // Clear dirty region
    doc->clearDirty();
    
    return status;
}

Status RenderEngine::renderWithDirtyDetection(Document* doc, Bitmap* output) {
    if (!doc || !output) return Status::ERROR_INVALID_PARAM;
    
    const Rect& dirtyRegion = doc->getDirtyRegion();
    
    // If nothing is dirty, return early
    if (dirtyRegion.isEmpty()) {
        return Status::OK;
    }
    
    // Check if entire canvas is dirty or just part of it
    bool isFullDirty = (dirtyRegion.width() == doc->getWidth() && 
                        dirtyRegion.height() == doc->getHeight());
    
    if (isFullDirty) {
        // Full canvas render - simpler path
        return renderRegion(doc, Rect(0, 0, doc->getWidth(), doc->getHeight()), output);
    }
    
    // Partial dirty region - optimize by only rendering affected area
    // Create render context
    auto ctx = std::make_unique<RenderContext>(doc->getWidth(), doc->getHeight());
    
    // Fill output dengan background color
    output->fill(doc->getBackgroundColor());
    
    // Render layers dengan dirty region awareness
    Status status = renderLayers(doc, ctx.get());
    if (status != Status::OK) return status;
    
    // Compose layers ke output
    status = composeLayers(ctx.get(), output);
    
    // Clear dirty region
    doc->clearDirty();
    
    return status;
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
    if (!doc || !ctx) return Status::ERROR_INVALID_PARAM;
    
    int layerCount = doc->getLayerCount();
    Bitmap* frameBuffer = ctx->getFrameBuffer();
    if (!frameBuffer) return Status::ERROR_INVALID_PARAM;
    
    // Render setiap layer dari belakang ke depan
    for (int i = layerCount - 1; i >= 0; i--) {
        auto layer = doc->getLayer(i);
        if (!layer || !layer->isVisible()) continue;
        
        // Get layer transform dan opacity
        uint8_t opacity = layer->getOpacity();
        if (opacity == 0) continue;  // Skip fully transparent
        
        // Get blend mode
        BlendMode blendMode = layer->getBlendMode();
        
        // Try to get cached bitmap
        Bitmap* cachedBitmap = nullptr;
        if (caching_enabled_) {
            cachedBitmap = cache_->getCachedBitmap(layer.get());
            if (cachedBitmap && !layer->isDirty()) {
                // Use cached - blend ke frame buffer
                blendLayerToBuffer(cachedBitmap, frameBuffer, blendMode, opacity);
                ctx->markRendered(Rect(0, 0, frameBuffer->getWidth(), frameBuffer->getHeight()));
                continue;
            }
        }
        
        // Render layer - get bitmap dari layer
        auto bitmapLayer = std::dynamic_pointer_cast<BitmapLayer>(layer);
        if (bitmapLayer && bitmapLayer->getBitmap()) {
            Bitmap* layerBitmap = bitmapLayer->getBitmap();
            
            // Apply layer transform jika ada
            // For now, simple blit dengan blend mode
            
            // Cache rendered bitmap jika enabled
            if (caching_enabled_) {
                auto cached = std::make_shared<Bitmap>(*layerBitmap);
                cache_->cacheBitmap(layer.get(), cached);
            }
            
            // Blend ke frame buffer
            blendLayerToBuffer(layerBitmap, frameBuffer, blendMode, opacity);
            ctx->markRendered(Rect(0, 0, frameBuffer->getWidth(), frameBuffer->getHeight()));
            
            // Clear layer dirty flag
            layer->clearDirty();
        }
    }
    
    return Status::OK;
}

Status RenderEngine::composeLayers(RenderContext* ctx, Bitmap* output) {
    if (!ctx || !output) return Status::ERROR_INVALID_PARAM;
    
    // Get frame buffer dari context
    Bitmap* frameBuffer = ctx->getFrameBuffer();
    if (!frameBuffer) return Status::ERROR_INVALID_PARAM;
    
    // Copy frame buffer ke output
    output->copyFrom(*frameBuffer, 0, 0);
    
    return Status::OK;
}

void RenderEngine::blendLayerToBuffer(
    Bitmap* layerBitmap,
    Bitmap* frameBuffer,
    BlendMode blendMode,
    uint8_t opacity) {
    
    if (!layerBitmap || !frameBuffer) return;
    
    int width = std::min(layerBitmap->getWidth(), frameBuffer->getWidth());
    int height = std::min(layerBitmap->getHeight(), frameBuffer->getHeight());
    
    if (width <= 0 || height <= 0) return;
    
    // Get raw pixel buffers
    Color32* layerPixels = reinterpret_cast<Color32*>(layerBitmap->getBuffer());
    Color32* framePixels = reinterpret_cast<Color32*>(frameBuffer->getBuffer());
    
    if (!layerPixels || !framePixels) return;
    
    int layerStride = layerBitmap->getWidth();
    int frameStride = frameBuffer->getWidth();
    
    // Blend pixel by pixel
    if (simd_enabled_) {
        // Use SIMD-optimized scanline blending
        for (int y = 0; y < height; y++) {
            Color32* layerRow = layerPixels + y * layerStride;
            Color32* frameRow = framePixels + y * frameStride;
            
            apply_blend_mode_scanline_simd(frameRow, layerRow, width, blendMode, opacity);
        }
    } else {
        // Use scalar scanline blending
        for (int y = 0; y < height; y++) {
            Color32* layerRow = layerPixels + y * layerStride;
            Color32* frameRow = framePixels + y * frameStride;
            
            apply_blend_mode_scanline(frameRow, layerRow, width, blendMode, opacity);
        }
    }
}

// ============================================================================
// RenderContext
// ============================================================================

RenderContext::RenderContext(int width, int height) {
    frame_buffer_ = std::make_unique<Bitmap>(width, height);
    frame_buffer_->clear();
}

RenderContext::~RenderContext() {
}

Bitmap* RenderContext::getTempBuffer(int width, int height) {
    if (!temp_buffer_ || temp_buffer_->getWidth() != width || 
        temp_buffer_->getHeight() != height) {
        temp_buffer_ = std::make_unique<Bitmap>(width, height);
    }
    temp_buffer_->clear();
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
        const_cast<RenderCache*>(this)->hit_count_++;
        return it->bitmap.get();
    }
    
    const_cast<RenderCache*>(this)->miss_count_++;
    return nullptr;
}

void RenderCache::cacheBitmap(const Layer* layer, std::shared_ptr<Bitmap> bitmap) {
    if (!bitmap) return;
    
    // Remove existing cache untuk layer ini
    auto it = std::find_if(cache_entries_.begin(), cache_entries_.end(),
        [layer](const CacheEntry& e) { return e.layer == layer; });
    
    if (it != cache_entries_.end()) {
        current_memory_ -= it->memoryUsage;
        cache_entries_.erase(it);
    }
    
    // Add entry baru
    size_t memoryUsage = bitmap->getBufferSize();
    
    // Evict jika perlu
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
    static uint64_t counter = 0;
    return ++counter;
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

// ============================================================================
// Advanced RenderCache Features
// ============================================================================

float RenderCache::getCacheHitRate() const {
    int total = hit_count_ + miss_count_;
    if (total == 0) return 0.0f;
    return (hit_count_ * 100.0f) / total;
}

int RenderCache::getCachedLayerCount() const {
    return cache_entries_.size();
}

std::vector<RenderCache::CacheEntryInfo> RenderCache::getCacheEntries() const {
    std::vector<CacheEntryInfo> entries;
    
    for (const auto& entry : cache_entries_) {
        entries.push_back({
            entry.memoryUsage,
            entry.lastAccessTime
        });
    }
    
    return entries;
}

void RenderCache::setMaxMemory(size_t bytes) {
    max_memory_ = bytes;
    // Evict if necessary
    evictIfNeeded(0);
}

void RenderCache::compact() {
    // Sort by access time dan remove least recently used
    // until we're at 80% of max memory
    size_t targetMemory = (max_memory_ * 80) / 100;
    
    while (current_memory_ > targetMemory && !cache_entries_.empty()) {
        auto lruIt = std::min_element(
            cache_entries_.begin(),
            cache_entries_.end(),
            [](const CacheEntry& a, const CacheEntry& b) {
                return a.lastAccessTime < b.lastAccessTime;
            });
        
        if (lruIt != cache_entries_.end()) {
            current_memory_ -= lruIt->memoryUsage;
            cache_entries_.erase(lruIt);
        }
    }
}

RenderCache::MemoryBreakdown RenderCache::getMemoryBreakdown() const {
    MemoryBreakdown breakdown = {0, 0, 0.0f};
    
    breakdown.totalUsed = current_memory_;
    breakdown.totalAvailable = max_memory_;
    
    if (max_memory_ > 0) {
        breakdown.percentUsed = (current_memory_ * 100.0f) / max_memory_;
    }
    
    return breakdown;
}

float RenderCache::estimateCacheEfficiency() const {
    // Efficiency based on hit rate
    float hitRate = getCacheHitRate();
    
    // Adjust based on memory utilization
    float memoryUtilization = 0.0f;
    if (max_memory_ > 0) {
        memoryUtilization = current_memory_ / (float)max_memory_;
    }
    
    // Score = hit rate * memory utilization factor
    // Ideal: high hit rate with good memory usage
    float score = (hitRate / 100.0f);
    
    // Penalize if memory not being used (below 20%)
    if (memoryUtilization < 0.2f) {
        score *= 0.5f;
    }
    // Penalize if too much memory used (above 95%)
    else if (memoryUtilization > 0.95f) {
        score *= 0.8f;
    }
    
    return std::max(0.0f, std::min(1.0f, score));
}

std::vector<std::pair<const Layer*, size_t>> RenderCache::getCachedLayersList() const {
    std::vector<std::pair<const Layer*, size_t>> list;
    
    for (const auto& entry : cache_entries_) {
        list.push_back({entry.layer, entry.memoryUsage});
    }
    
    // Sort by memory usage (largest first)
    std::sort(list.begin(), list.end(),
        [](const auto& a, const auto& b) {
            return a.second > b.second;
        });
    
    return list;
}

void RenderCache::resetStats() {
    hit_count_ = 0;
    miss_count_ = 0;
}

// ============================================================================
// Advanced RenderContext Features
// ============================================================================

size_t RenderContext::estimateMemoryUsage() const {
    size_t usage = 0;
    
    if (frame_buffer_) {
        usage += frame_buffer_->getBufferSize();
    }
    
    if (temp_buffer_) {
        usage += temp_buffer_->getBufferSize();
    }
    
    return usage;
}

RenderContext::ContextStats RenderContext::getStats() const {
    ContextStats stats;
    
    if (frame_buffer_) {
        stats.frameBufferSize = frame_buffer_->getBufferSize();
        stats.frameBufferWidth = frame_buffer_->getWidth();
        stats.frameBufferHeight = frame_buffer_->getHeight();
    }
    
    if (temp_buffer_) {
        stats.tempBufferSize = temp_buffer_->getBufferSize();
        stats.tempBufferWidth = temp_buffer_->getWidth();
        stats.tempBufferHeight = temp_buffer_->getHeight();
    }
    
    stats.renderedRegion = rendered_region_;
    stats.totalMemoryUsage = estimateMemoryUsage();
    
    return stats;
}

Status RenderContext::resizeFrameBuffer(int width, int height) {
    if (width <= 0 || height <= 0) {
        return Status::ERROR_INVALID_PARAM;
    }
    
    frame_buffer_ = std::make_unique<Bitmap>(width, height);
    frame_buffer_->clear();
    rendered_region_ = Rect();
    
    return Status::OK;
}

void RenderContext::optimizeMemory() {
    // Memory optimization - can release temp buffer if not needed
    // Keep for now - might be reused in render pass
}

void RenderContext::clearRenderedRegion() {
    rendered_region_ = Rect();
}
