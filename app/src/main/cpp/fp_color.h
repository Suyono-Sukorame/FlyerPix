#ifndef FP_COLOR_H
#define FP_COLOR_H

#include <cstdint>

namespace fp {

// Terapkan brightness/contrast/saturation (skala sama dengan adjustment
// PixelCanvasView: brightness 0..100, contrast +/-100, saturation +/-100)
// pada array ARGB8888 in-place.
void applyColorMatrix(int32_t* pixels, int32_t count,
                      float brightness, float contrast, float saturation);

}

#endif // FP_COLOR_H