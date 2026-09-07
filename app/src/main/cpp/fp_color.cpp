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

// Terapkan noise film-grain (blend abu-abu dengan alpha) lalu vignette
// (gelap radial di tepi) ke array ARGB8888 in-place. Minim meniru
// drawNoiseEffect + drawVignetteEffect milik PixelCanvasView agar snapshot
// blur bisa dibakar bersama efek overlay (draw tunggal, off-UI-thread).
void applyNoiseVignette(int32_t* pixels, int32_t count, int32_t width, int32_t height,
                        int32_t noiseAlpha, uint32_t noiseSeed, bool vignette) {
    if (pixels == nullptr || count <= 0) return;
    if (noiseAlpha <= 0 && !vignette) return;

    const float nAlpha = static_cast<float>(noiseAlpha) / 255.0f;
    const float cx = static_cast<float>(width) * 0.5f;
    const float cy = static_cast<float>(height) * 0.5f;
    const float radius = static_cast<float>(width > height ? width : height) * 0.75f;
    const float rInv = 1.0f / (radius > 0.0f ? radius : 1.0f);
    const float v0 = 0.25f;                       // (0.50)^2
    const float v1 = 0.6084f;                     // (0.78)^2
    const float v2 = 1.0f - v1;
    const uint32_t seed = noiseSeed != 0u ? noiseSeed : 0x9E3779B9u;
    int32_t xc = 0;
    int32_t yc = 0;

    for (int32_t i = 0; i < count; ++i) {
        int32_t p = pixels[i];
        const int32_t alpha = p & 0xFF000000;

        if (noiseAlpha > 0) {
            // Deterministik per-pixel: xorshift32 dari seed + posisi.
            uint32_t s = seed + static_cast<uint32_t>(i);
            s ^= s << 13; s ^= s >> 17; s ^= s << 5;
            const float g = static_cast<float>(s & 0xFF);
            const float inv = 1.0f - nAlpha;
            const int32_t r = static_cast<int32_t>(((p >> 16) & 0xFF) * inv + g * nAlpha + 0.5f);
            const int32_t gg = static_cast<int32_t>(((p >> 8) & 0xFF) * inv + g * nAlpha + 0.5f);
            const int32_t b = static_cast<int32_t>((p & 0xFF) * inv + g * nAlpha + 0.5f);
            p = alpha | (r << 16) | (gg << 8) | b;
        }

        if (vignette) {
            const float dx = static_cast<float>(xc) - cx;
            const float dy = static_cast<float>(yc) - cy;
            const float d = (dx * dx + dy * dy) * rInv * rInv;
            // Stop gradasi (Skia): alpha 0 @ 0.5, 0x52 @ 0.78, 0xA6 @ 1.0,
            // dihitung pada kuadrat jarak ternormalisasi.
            float va;
            if (d <= v0) {
                va = 0.0f;
            } else if (d <= v1) {
                va = 0x52f * ((d - v0) / (v1 - v0));
            } else if (d <= 1.0f) {
                va = 0x52f + (0xA6f - 0x52f) * ((d - v1) / v2);
            } else {
                va = 0xA6f;
            }
            const float inv = 1.0f - va / 255.0f;
            const int32_t r = static_cast<int32_t>(((p >> 16) & 0xFF) * inv + 0.5f);
            const int32_t gg = static_cast<int32_t>(((p >> 8) & 0xFF) * inv + 0.5f);
            const int32_t b = static_cast<int32_t>((p & 0xFF) * inv + 0.5f);
            p = alpha | (r << 16) | (gg << 8) | b;
        }

        pixels[i] = p;
        if (++xc >= width) { xc = 0; yc++; }
    }
}

} // namespace fp