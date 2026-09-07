/**
 * dirty_region.cpp
 * 
 * Dirty region tracking untuk optimization re-rendering
 * Advanced dirty region management dan optimization utilities
 */

#include "render_engine.h"
#include <algorithm>

// ============================================================================
// Optimization: Region Coalescing
// ============================================================================

/**
 * Coalesce multiple small dirty regions menjadi satu larger region
 * Untuk mengurangi rendering overhead pada banyak small changes
 */
std::vector<Rect> coalesceRegions(
    const std::vector<Rect>& regions,
    float maxTolerance = 1.1f) {
    
    if (regions.empty()) return {};
    
    std::vector<Rect> result;
    
    for (const auto& region : regions) {
        bool merged = false;
        
        // Try to merge dengan existing result region
        for (auto& existing : result) {
            Rect unionRect = existing.getUnion(region);
            float unionArea = unionRect.getArea();
            float combinedArea = existing.getArea() + region.getArea();
            
            // Merge jika overhead kurang dari tolerance
            if (unionArea <= combinedArea * maxTolerance) {
                existing = unionRect;
                merged = true;
                break;
            }
        }
        
        if (!merged) {
            result.push_back(region);
        }
    }
    
    return result;
}

/**
 * Split large dirty region menjadi tiles untuk better cache locality
 */
std::vector<Rect> splitRegionIntoTiles(
    const Rect& region,
    int tileWidth,
    int tileHeight) {
    
    std::vector<Rect> tiles;
    
    if (region.isEmpty() || tileWidth <= 0 || tileHeight <= 0) {
        return tiles;
    }
    
    for (int y = region.top; y < region.bottom; y += tileHeight) {
        for (int x = region.left; x < region.right; x += tileWidth) {
            int right = std::min(x + tileWidth, region.right);
            int bottom = std::min(y + tileHeight, region.bottom);
            tiles.push_back(Rect(x, y, right, bottom));
        }
    }
    
    return tiles;
}

// ============================================================================
// Performance Monitoring
// ============================================================================

struct DirtyRegionStats {
    int totalUpdates;
    int averageRegionSize;
    float averageDirtyPercentage;
    int maxRegionSize;
};

/**
 * Calculate statistics untuk dirty region tracking
 */
DirtyRegionStats calculateDirtyStats(
    const std::vector<Rect>& dirtyHistory,
    int canvasWidth,
    int canvasHeight) {
    
    DirtyRegionStats stats = {0, 0, 0.0f, 0};
    
    if (dirtyHistory.empty()) {
        return stats;
    }
    
    stats.totalUpdates = dirtyHistory.size();
    
    size_t totalArea = 0;
    int maxArea = 0;
    
    for (const auto& region : dirtyHistory) {
        int area = region.getArea();
        totalArea += area;
        maxArea = std::max(maxArea, area);
    }
    
    stats.averageRegionSize = totalArea / stats.totalUpdates;
    stats.maxRegionSize = maxArea;
    
    if (canvasWidth > 0 && canvasHeight > 0) {
        size_t totalPixels = canvasWidth * canvasHeight;
        stats.averageDirtyPercentage = (totalArea * 100.0f) / 
                                       (totalPixels * stats.totalUpdates);
    }
    
    return stats;
}
