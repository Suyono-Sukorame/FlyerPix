/**
 * document.cpp
 * 
 * Document implementation
 */

#include "document.h"
#include "layer.h"
#include "bitmap.h"
#include <algorithm>

Document::Document(int width, int height)
    : width_(width), height_(height), bg_color_(0xFFFFFFFF) {
}

Document::~Document() {
    layers_.clear();
}

void Document::setSize(int width, int height) {
    if (width > 0 && height > 0) {
        width_ = width;
        height_ = height;
        markAllDirty();
    }
}

void Document::addLayer(std::shared_ptr<Layer> layer) {
    if (layer) {
        layers_.push_back(layer);
        markAllDirty();
    }
}

void Document::insertLayer(int index, std::shared_ptr<Layer> layer) {
    if (layer && index >= 0 && index <= (int)layers_.size()) {
        layers_.insert(layers_.begin() + index, layer);
        markAllDirty();
    }
}

void Document::removeLayer(int index) {
    if (index >= 0 && index < (int)layers_.size()) {
        layers_.erase(layers_.begin() + index);
        markAllDirty();
    }
}

std::shared_ptr<Layer> Document::getLayer(int index) const {
    if (index >= 0 && index < (int)layers_.size()) {
        return layers_[index];
    }
    return nullptr;
}

void Document::moveLayer(int fromIndex, int toIndex) {
    if (fromIndex >= 0 && fromIndex < (int)layers_.size() &&
        toIndex >= 0 && toIndex <= (int)layers_.size()) {
        auto layer = layers_[fromIndex];
        layers_.erase(layers_.begin() + fromIndex);
        layers_.insert(layers_.begin() + toIndex, layer);
        markAllDirty();
    }
}

void Document::markDirty(const Rect& region) {
    if (dirty_region_.isEmpty()) {
        dirty_region_ = region;
    } else {
        // Union of rectangles
        int left = std::min(dirty_region_.left, region.left);
        int top = std::min(dirty_region_.top, region.top);
        int right = std::max(dirty_region_.right, region.right);
        int bottom = std::max(dirty_region_.bottom, region.bottom);
        dirty_region_ = Rect(left, top, right, bottom);
    }
}

void Document::markAllDirty() {
    dirty_region_ = Rect(0, 0, width_, height_);
}

Status Document::render(Bitmap* output) {
    if (!output) return Status::ERROR_INVALID_PARAM;
    
    Rect region(0, 0, width_, height_);
    return renderRegion(region, output);
}

Status Document::renderRegion(const Rect& region, Bitmap* output) {
    if (!output) return Status::ERROR_INVALID_PARAM;
    
    // TODO: Implement rendering with layer composition
    // For now, just fill with background color
    output->fill(bg_color_);
    
    return Status::OK;
}
