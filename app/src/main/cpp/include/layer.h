/**
 * layer.h
 * 
 * Layer model untuk FlyerPix - individual editable element
 */

#ifndef FLYERPIX_LAYER_H
#define FLYERPIX_LAYER_H

#include "flyerpix_types.h"
#include <string>
#include <memory>
#include <vector>

// Forward declaration
class Bitmap;
class RenderContext;

/**
 * Layer - Representasi individual layer dalam canvas
 * 
 * Tipe layer yang didukung:
 * - Bitmap Layer: Raster image
 * - Text Layer: Text dengan font & styling
 * - Shape Layer: Vector shapes (rectangle, circle, dll)
 */
class Layer {
public:
    enum class Type {
        BITMAP,
        TEXT,
        SHAPE,
        GROUP
    };
    
    Layer(const std::string& name, Type type, int width, int height);
    virtual ~Layer();
    
    // ========== Properties ==========
    
    const std::string& getName() const { return name_; }
    void setName(const std::string& name) { name_ = name; dirty_ = true; }
    
    Type getType() const { return type_; }
    
    int getX() const { return x_; }
    int getY() const { return y_; }
    int getWidth() const { return width_; }
    int getHeight() const { return height_; }
    
    void setPosition(int x, int y) { 
        x_ = x; 
        y_ = y; 
        dirty_ = true; 
    }
    
    // ========== Transform ==========
    
    Transform& getTransform() { return transform_; }
    const Transform& getTransform() const { return transform_; }
    void setTransform(const Transform& t) { transform_ = t; dirty_ = true; }
    
    // ========== Opacity & Blending ==========
    
    float getOpacity() const { return opacity_; }
    void setOpacity(float o) { 
        opacity_ = o > 1.0f ? 1.0f : (o < 0.0f ? 0.0f : o);
        dirty_ = true; 
    }
    
    BlendMode getBlendMode() const { return blend_mode_; }
    void setBlendMode(BlendMode mode) { 
        blend_mode_ = mode; 
        dirty_ = true; 
    }
    
    // ========== Visibility & State ==========
    
    bool isVisible() const { return flags_.visible; }
    void setVisible(bool v) { 
        flags_.visible = v ? 1 : 0;
        dirty_ = true;
    }
    
    bool isLocked() const { return flags_.locked; }
    void setLocked(bool l) { flags_.locked = l ? 1 : 0; }
    
    bool isDirty() const { return dirty_; }
    void markDirty() { dirty_ = true; }
    void clearDirty() { dirty_ = false; }
    
    // ========== Content Access ==========
    
    // Get layer bitmap (untuk BITMAP layer)
    virtual Bitmap* getBitmap() { return nullptr; }
    virtual const Bitmap* getBitmap() const { return nullptr; }
    
    // Set layer bitmap
    virtual void setBitmap(std::shared_ptr<Bitmap> bitmap) {}
    
    // ========== Rendering ==========
    
    // Render layer ke context
    virtual Status render(RenderContext* ctx) = 0;
    
    // Get bounding box
    virtual Rect getBounds() const;
    
protected:
    std::string name_;
    Type type_;
    
    int x_ = 0;
    int y_ = 0;
    int width_;
    int height_;
    
    Transform transform_;
    float opacity_ = 1.0f;
    BlendMode blend_mode_ = BlendMode::NORMAL;
    LayerFlags flags_;
    bool dirty_ = true;
};

/**
 * BitmapLayer - Layer yang berisi raster image
 */
class BitmapLayer : public Layer {
public:
    BitmapLayer(const std::string& name, int width, int height);
    
    Bitmap* getBitmap() override { return bitmap_.get(); }
    const Bitmap* getBitmap() const override { return bitmap_.get(); }
    void setBitmap(std::shared_ptr<Bitmap> bitmap) override;
    
    Status render(RenderContext* ctx) override;
    
private:
    std::shared_ptr<Bitmap> bitmap_;
};

/**
 * TextLayer - Layer yang berisi text dengan styling
 */
class TextLayer : public Layer {
public:
    TextLayer(const std::string& name, const std::string& text);
    
    const std::string& getText() const { return text_; }
    void setText(const std::string& text) { 
        text_ = text; 
        dirty_ = true; 
    }
    
    const std::string& getFontName() const { return font_name_; }
    void setFontName(const std::string& name) { 
        font_name_ = name; 
        dirty_ = true; 
    }
    
    float getFontSize() const { return font_size_; }
    void setFontSize(float size) { 
        font_size_ = size; 
        dirty_ = true; 
    }
    
    Color32 getTextColor() const { return text_color_; }
    void setTextColor(Color32 color) { 
        text_color_ = color; 
        dirty_ = true; 
    }
    
    Status render(RenderContext* ctx) override;
    
private:
    std::string text_;
    std::string font_name_;
    float font_size_ = 24.0f;
    Color32 text_color_ = 0xFF000000;  // Black
};

#endif // FLYERPIX_LAYER_H
