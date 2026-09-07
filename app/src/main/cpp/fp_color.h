#ifndef FP_COLOR_H
#define FP_COLOR_H

#include <cstdint>

namespace fp {

// Terapkan brightness/contrast/saturation (skala sama dengan adjustment
// PixelCanvasView: brightness 0..100, contrast +/-100, saturation +/-100)
// pada array ARGB8888 in-place.
void applyColorMatrix(int32_t* pixels, int32_t count,
                      float brightness, float contrast, float saturation);

// Terapkan noise film-grain (blend abu-abu seed deterministik, alpha) lalu
// vignette (gelap radial) ke array ARGB8888 in-place. `noiseAlpha` 0 = tanpa
// noise; `vignette` false = tanpa vignette.
void applyNoiseVignette(int32_t* pixels, int32_t count, int32_t width, int32_t height,
                        int32_t noiseAlpha, uint32_t noiseSeed, bool vignette);

}

#endif // FP_COLOR_H