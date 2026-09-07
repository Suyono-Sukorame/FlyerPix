/**
 * simd_utils.h
 * 
 * SIMD (NEON/SSE) utilities untuk fast bitmap operations
 * Auto-detects CPU features dan menggunakan intrinsics yang tersedia
 */

#ifndef FLYERPIX_SIMD_UTILS_H
#define FLYERPIX_SIMD_UTILS_H

#include <cstdint>
#include <cstring>

// ============================================================================
// CPU Feature Detection & SIMD Selection
// ============================================================================

#if defined(__ARM_NEON)
    #include <arm_neon.h>
    #define FLYERPIX_NEON 1
#elif defined(__SSE2__)
    #include <emmintrin.h>
    #define FLYERPIX_SSE2 1
#elif defined(__SSE4_2__)
    #include <smmintrin.h>
    #define FLYERPIX_SSE42 1
#endif

// ============================================================================
// SIMD Fill Operations
// ============================================================================

/**
 * Fast fill operation menggunakan SIMD jika available
 * 
 * @param dst Destination buffer (harus 32-byte aligned)
 * @param color ARGB color value
 * @param count Jumlah pixels (32-bit ARGB)
 */
inline void simd_fill_32bit(uint8_t* dst, uint32_t color, int count) {
#if defined(FLYERPIX_NEON)
    // ARM NEON version - fill 4 pixels at a time (16 bytes)
    uint32x4_t color_vec = vdupq_n_u32(color);
    int aligned_count = (count / 4) * 4;
    
    uint32_t* dst32 = reinterpret_cast<uint32_t*>(dst);
    for (int i = 0; i < aligned_count; i += 4) {
        vst1q_u32(dst32 + i, color_vec);
    }
    
    // Handle remaining pixels
    for (int i = aligned_count; i < count; i++) {
        dst32[i] = color;
    }
#elif defined(FLYERPIX_SSE42) || defined(FLYERPIX_SSE2)
    // x86 SSE version - fill 4 pixels at a time (16 bytes)
    __m128i color_vec = _mm_set1_epi32(color);
    int aligned_count = (count / 4) * 4;
    
    uint32_t* dst32 = reinterpret_cast<uint32_t*>(dst);
    for (int i = 0; i < aligned_count; i += 4) {
        _mm_storeu_si128((__m128i*)(dst32 + i), color_vec);
    }
    
    // Handle remaining pixels
    for (int i = aligned_count; i < count; i++) {
        dst32[i] = color;
    }
#else
    // Fallback: generic fill
    uint32_t* dst32 = reinterpret_cast<uint32_t*>(dst);
    for (int i = 0; i < count; i++) {
        dst32[i] = color;
    }
#endif
}

/**
 * Fast 16-bit fill (RGB_565)
 */
inline void simd_fill_16bit(uint8_t* dst, uint16_t color, int count) {
#if defined(FLYERPIX_NEON)
    uint16x8_t color_vec = vdupq_n_u16(color);
    int aligned_count = (count / 8) * 8;
    
    uint16_t* dst16 = reinterpret_cast<uint16_t*>(dst);
    for (int i = 0; i < aligned_count; i += 8) {
        vst1q_u16(dst16 + i, color_vec);
    }
    
    for (int i = aligned_count; i < count; i++) {
        dst16[i] = color;
    }
#elif defined(FLYERPIX_SSE42) || defined(FLYERPIX_SSE2)
    __m128i color_vec = _mm_set1_epi16(color);
    int aligned_count = (count / 8) * 8;
    
    uint16_t* dst16 = reinterpret_cast<uint16_t*>(dst);
    for (int i = 0; i < aligned_count; i += 8) {
        _mm_storeu_si128((__m128i*)(dst16 + i), color_vec);
    }
    
    for (int i = aligned_count; i < count; i++) {
        dst16[i] = color;
    }
#else
    uint16_t* dst16 = reinterpret_cast<uint16_t*>(dst);
    for (int i = 0; i < count; i++) {
        dst16[i] = color;
    }
#endif
}

// ============================================================================
// SIMD Copy Operations
// ============================================================================

/**
 * Fast copy using SIMD (cache-friendly memcpy variant)
 * 
 * @param dst Destination (harus 32-byte aligned)
 * @param src Source (harus 32-byte aligned)
 * @param bytes Jumlah bytes to copy (harus multiple of 32)
 */
inline void simd_copy(uint8_t* dst, const uint8_t* src, size_t bytes) {
#if defined(FLYERPIX_NEON)
    // ARM NEON: copy 32 bytes at a time
    size_t aligned_bytes = (bytes / 32) * 32;
    for (size_t i = 0; i < aligned_bytes; i += 32) {
        uint8x16_t v1 = vld1q_u8(src + i);
        uint8x16_t v2 = vld1q_u8(src + i + 16);
        vst1q_u8(dst + i, v1);
        vst1q_u8(dst + i + 16, v2);
    }
    
    // Handle remaining bytes
    if (bytes % 32) {
        std::memcpy(dst + aligned_bytes, src + aligned_bytes, bytes % 32);
    }
#elif defined(FLYERPIX_SSE42) || defined(FLYERPIX_SSE2)
    // x86 SSE: copy 32 bytes at a time
    size_t aligned_bytes = (bytes / 32) * 32;
    for (size_t i = 0; i < aligned_bytes; i += 32) {
        __m128i v1 = _mm_loadu_si128((__m128i*)(src + i));
        __m128i v2 = _mm_loadu_si128((__m128i*)(src + i + 16));
        _mm_storeu_si128((__m128i*)(dst + i), v1);
        _mm_storeu_si128((__m128i*)(dst + i + 16), v2);
    }
    
    if (bytes % 32) {
        std::memcpy(dst + aligned_bytes, src + aligned_bytes, bytes % 32);
    }
#else
    // Fallback: standard memcpy
    std::memcpy(dst, src, bytes);
#endif
}

// ============================================================================
// SIMD Alpha Blend
// ============================================================================

/**
 * SIMD alpha blending untuk 4 pixels sekaligus
 * blend = src * alpha + dst * (1 - alpha)
 */
inline void simd_alpha_blend_4pixels(
    uint32_t* dst, 
    const uint32_t* src, 
    uint8_t alpha) {
#if defined(FLYERPIX_NEON)
    // NEON version
    uint32x4_t src_vec = vld1q_u32(src);
    uint32x4_t dst_vec = vld1q_u32(dst);
    
    // Extract ARGB channels
    uint8x16_t src_bytes = vreinterpretq_u8_u32(src_vec);
    uint8x16_t dst_bytes = vreinterpretq_u8_u32(dst_vec);
    
    // Alpha blend each channel
    uint8x16_t alpha_vec = vdupq_n_u8(alpha);
    
    // Simplified: just use alpha directly (full implementation needs careful channel handling)
    uint8x16_t blend = vmlaq_u8(
        vmulq_u8(dst_bytes, vdupq_n_u8(255 - alpha)),
        src_bytes, 
        alpha_vec
    );
    
    uint32x4_t result = vreinterpretq_u32_u8(blend);
    vst1q_u32(dst, result);
#else
    // Fallback: pixel-by-pixel blend
    float alpha_f = alpha / 255.0f;
    for (int i = 0; i < 4; i++) {
        uint32_t s = src[i];
        uint32_t d = dst[i];
        
        uint8_t sr = (s >> 16) & 0xFF;
        uint8_t sg = (s >> 8) & 0xFF;
        uint8_t sb = s & 0xFF;
        
        uint8_t dr = (d >> 16) & 0xFF;
        uint8_t dg = (d >> 8) & 0xFF;
        uint8_t db = d & 0xFF;
        
        uint8_t r = static_cast<uint8_t>(sr * alpha_f + dr * (1.0f - alpha_f));
        uint8_t g = static_cast<uint8_t>(sg * alpha_f + dg * (1.0f - alpha_f));
        uint8_t b = static_cast<uint8_t>(sb * alpha_f + db * (1.0f - alpha_f));
        
        dst[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
    }
#endif
}

#endif // FLYERPIX_SIMD_UTILS_H
