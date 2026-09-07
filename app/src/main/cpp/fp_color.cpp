// fp_color.cpp - ColorMatrix adjustment (brightness/contrast/saturation)
// untuk array ARGB8888 (0xAARRGGBB). Formula disalin dari
// ColorMatrix.setSaturation + matriks contrast/brightness milik PixelCanvasView
// agar hasil konsisten dengan jalur Skia (saveLayer ColorMatrixColorFilter).

#include <cstdint>
#include <algorithm>

namespace fp {

void applyColorMatrix(int32_t* pixels, int32_t count,
                      float brightness, float contrast, float saturation) {
    if (pixels == nullptr || count <= 0) return;

    // Contrast: scale = 1 + contrast/100, pivot 128.
    const float c = 1.0f + contrast / 100.0f;
    const float t = 128.0f * (1.0f - c);
    // Brightness: offset langsung.
    const float b = brightness * 2.55f;
    const float offset = t + b;

    // Saturation standard matrix (ColorMatrix.setSaturation(1 + sat/100)).
    const float s = 1.0f + saturation / 100.0f;
    const float rw = 0.213f + 0.787f * s;
    const float gw = 0.715f - 0.715f * s;
    const float bw = 0.072f - 0.072f * s;
    const float ry = 0.213f - 0.213f * s;
    const float gy = 0.715f + 0.285f * s;
    const float by = 0.072f - 0.072f * s;
    const float rz = 0.213f - 0.213f * s;
    const float gz = 0.715f - 0.715f * s;
    const float bz = 0.072f + 0.928f * s;

    for (int32_t i = 0; i < count; ++i) {
        const int32_t px = pixels[i];
        const float r0 = static_cast<float>((px >> 16) & 0xFF);
        const float g0 = static_cast<float>((px >> 8) & 0xFF);
        const float b0 = static_cast<float>(px & 0xFF);

        // Contrast (diterapkan pertama): r1 = c*R + offset
        const float r1 = c * r0 + offset;
        const float g1 = c * g0 + offset;
        const float b1 = c * b0 + offset;

        // Saturation: out = sat matrix * (r1,g1,b1)
        const float r2 = rw * r1 + gw * g1 + bw * b1;
        const float g2 = ry * r1 + gy * g1 + by * b1;
        const float b2 = rz * r1 + gz * g1 + bz * b1;

        int32_t r = static_cast<int32_t>(r2 + 0.5f);
        int32_t g = static_cast<int32_t>(g2 + 0.5f);
        int32_t bb = static_cast<int32_t>(b2 + 0.5f);
        r = std::min(r, 255); g = std::min(g, 255); bb = std::min(bb, 255);
        r = std::max(r, 0);   g = std::max(g, 0);   bb = std::max(bb, 0);

        pixels[i] = (px & 0xFF000000) | (r << 16) | (g << 8) | bb;
    }
}

} // namespace fp