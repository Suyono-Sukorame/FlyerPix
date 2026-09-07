/**
 * layer.cpp
 * 
 * Layer implementation
 */

#include "layer.h"
#include "bitmap.h"

// ============================================================================
// Base Layer class
// ============================================================================

Layer::Layer(const std::string& name, Type type, int width, int height)
    : name_(name), type_(type), width_(width), height_(height) {
}

Layer::~Layer() {
}

Rect Layer::getBounds() const {
    return Rect(x_, y_, x_ + width_, y_ + height_);
}

// ============================================================================
// BitmapLayer class
// ============================================================================

BitmapLayer::BitmapLayer(const std::string& name, int width, int height)
    : Layer(name, Type::BITMAP, width, height) {
    bitmap_ = std::make_shared<Bitmap>(width, height);
}

void BitmapLayer::setBitmap(std::shared_ptr<Bitmap> bitmap) {
    if (bitmap) {
        bitmap_ = bitmap;
        width_ = bitmap->getWidth();
        height_ = bitmap->getHeight();
        dirty_ = true;
    }
}

Status BitmapLayer::render(RenderContext* ctx) {
    // TODO: Implement rendering to context
    return Status::OK;
}

// ============================================================================
// TextLayer class
// ============================================================================

TextLayer::TextLayer(const std::string& name, const std::string& text)
    : Layer(name, Type::TEXT, 0, 0), text_(text) {
}

Status TextLayer::render(RenderContext* ctx) {
    // TODO: Implement text rendering to context
    return Status::OK;
}

// ============================================================================
// Matrix4 transformations
// ============================================================================

Matrix4 Matrix4::identity() {
    Matrix4 m;
    m.m[0] = 1;  m.m[1] = 0;  m.m[2] = 0;  m.m[3] = 0;
    m.m[4] = 0;  m.m[5] = 1;  m.m[6] = 0;  m.m[7] = 0;
    m.m[8] = 0;  m.m[9] = 0;  m.m[10] = 1; m.m[11] = 0;
    m.m[12] = 0; m.m[13] = 0; m.m[14] = 0; m.m[15] = 1;
    return m;
}

Matrix4 Matrix4::translate(float tx, float ty) {
    Matrix4 m = identity();
    m.m[12] = tx;
    m.m[13] = ty;
    return m;
}

Matrix4 Matrix4::scale(float sx, float sy) {
    Matrix4 m = identity();
    m.m[0] = sx;
    m.m[5] = sy;
    return m;
}

Matrix4 Matrix4::rotate(float angle) {
    Matrix4 m = identity();
    float rad = angle * 3.14159265359f / 180.0f;  // Convert to radians
    float c = cosf(rad);
    float s = sinf(rad);
    m.m[0] = c;  m.m[1] = s;
    m.m[4] = -s; m.m[5] = c;
    return m;
}

Matrix4 Matrix4::operator*(const Matrix4& other) const {
    Matrix4 result;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            result.m[i * 4 + j] = 0;
            for (int k = 0; k < 4; k++) {
                result.m[i * 4 + j] += m[i * 4 + k] * other.m[k * 4 + j];
            }
        }
    }
    return result;
}
