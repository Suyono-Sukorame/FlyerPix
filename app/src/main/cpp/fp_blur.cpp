// fp_blur.cpp - Approximasi Gaussian blur dengan 3 pass box blur O(1) per
// piksel (running-sum, bebas radius) untuk buffer RGBA8888. Tidak bergantung
// pada Android / JNI — murni fungsi inti.

#include <cstdint>
#include <cstring>
#include <vector>

namespace fp {

// Satu pass box blur: sinkronisasi 4 kanal dengan running sum.
// `len` = jumlah piksel pada arah yang diblur, `pitch` = byte antar piksel
// tetangga pada arah tersebut. Bekerja in-place tiap line: tulis dst.
//
// Setiap output menggunakan tepat (2*radius+1) sample dengan clamping ke
// tepi (edge pixel digandakan) sehingga divisor konstan — memungkinkan
// running sum + reciprocal tanpa pembagian per piksel.
static void boxBlurLine1D(const uint8_t* src, uint8_t* dst, int32_t len,
                          int32_t pitch, int32_t radius, float recip) {
    if (len <= 0 || radius <= 0) return;

    const auto clampIdx = [len](int32_t i) -> int32_t {
        if (i < 0) return 0;
        if (i >= len) return len - 1;
        return i;
    };

    auto pxAt = [src, pitch](int32_t idx) -> const uint8_t* {
        return src + static_cast<int64_t>(idx) * pitch;
    };

    // Inisialisasi jendela sum untuk x=0: slot j=-radius..radius → src[clamp(j)].
    int32_t r = 0, g = 0, b = 0, a = 0;
    for (int32_t j = -radius; j <= radius; ++j) {
        const uint8_t* p = pxAt(clampIdx(j));
        r += p[0]; g += p[1]; b += p[2]; a += p[3];
    }

    // Integer sums aman (maks ~49*255 << 2^31); output pakai mul reciprocal.
    for (int32_t x = 0; x < len; ++x) {
        uint8_t* d = dst + static_cast<int64_t>(x) * pitch;
        d[0] = static_cast<uint8_t>(r * recip + 0.5f);
        d[1] = static_cast<uint8_t>(g * recip + 0.5f);
        d[2] = static_cast<uint8_t>(b * recip + 0.5f);
        d[3] = static_cast<uint8_t>(a * recip + 0.5f);

        if (x == len - 1) break;
        const uint8_t* pAdd = pxAt(clampIdx(x + radius));      // slot baru masuk
        const uint8_t* pRem = pxAt(clampIdx(x - radius));      // slot lama keluar
        r += pAdd[0] - pRem[0];
        g += pAdd[1] - pRem[1];
        b += pAdd[2] - pRem[2];
        a += pAdd[3] - pRem[3];
    }
}

// Terapkan satu pass box blur ke semua baris (horizontal).
static void boxBlurHorizontal(const uint8_t* src, uint8_t* dst, int32_t width,
                              int32_t height, int32_t srcStride, int32_t dstStride,
                              int32_t radius, float recip) {
    for (int32_t y = 0; y < height; ++y) {
        boxBlurLine1D(src + static_cast<int64_t>(y) * srcStride,
                      dst + static_cast<int64_t>(y) * dstStride,
                      width, 4, radius, recip);
    }
}

// Terapkan satu pass box blur ke semua kolom (vertical).
static void boxBlurVertical(const uint8_t* src, uint8_t* dst, int32_t width,
                            int32_t height, int32_t srcStride, int32_t dstStride,
                            int32_t radius, float recip) {
    for (int32_t x = 0; x < width; ++x) {
        // Satu kolom = leret spanning beberapa baris, pitch = srcStride.
        boxBlurLine1D(src + x * 4, dst + x * 4, height, srcStride, radius, recip);
    }
}

// Approximasi Gaussian dengan 3 pass box blur (horizontal×3 lalu vertical×3).
// Kompleksitas O(N * passes) dan tidak bergantung pada radius.
// `radius` dipakai sebagai basis; box radius = max(1, radius/2) agar total
// kekaburan serupa radius maksimal namun lebih merata.
void gaussianBlurRGBA(uint8_t* buffer, int32_t width, int32_t height,
                      int32_t stride, int radius) {
    if (buffer == nullptr || width <= 0 || height <= 0 || radius <= 0) return;

    const int32_t boxR = (radius > 2) ? radius / 2 : 1;
    const float recip = 1.0f / (2.0f * boxR + 1.0f);

    std::vector<uint8_t> tmp(static_cast<size_t>(stride) * height);

    // 6 sweep ping-pong: buffer → tmp → buffer → tmp → ... → buffer (hasil di buffer).
    uint8_t* cur = buffer;
    uint8_t* nxt = tmp.data();
    for (int i = 0; i < 3; ++i) {
        boxBlurHorizontal(cur, nxt, width, height, stride, stride, boxR, recip);
        boxBlurVertical(nxt, cur, width, height, stride, stride, boxR, recip);
    }
}

} // namespace fp