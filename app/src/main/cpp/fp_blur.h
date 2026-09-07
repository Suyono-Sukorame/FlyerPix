#ifndef FP_BLUR_H
#define FP_BLUR_H

#include <cstdint>

namespace fp {

// Gaussian blur 2-pass in-place untuk buffer RGBA8888.
// `stride` = byte per baris (width*4 bila tanpa padding).
void gaussianBlurRGBA(uint8_t* buffer, int32_t width, int32_t height,
                      int32_t stride, int radius);

}

#endif // FP_BLUR_H