/**
 * blend_modes.h
 * 
 * Blend mode declarations
 */

#ifndef FLYERPIX_BLEND_MODES_H
#define FLYERPIX_BLEND_MODES_H

#include "flyerpix_types.h"

// ============================================================================
// Single Pixel Blending
// ============================================================================

/**
 * Apply blend mode untuk single pixel
 * 
 * @param src Source pixel color (ARGB_8888)
 * @param dst Destination pixel color (ARGB_8888)
 * @param mode Blend mode to apply
 * @param alpha Opacity (0-255)
 * @return Blended pixel color
 */
Color32 apply_blend_mode(Color32 src, Color32 dst, BlendMode mode, uint8_t alpha);

// ============================================================================
// Scanline Blending (Multiple Pixels)
// ============================================================================

/**
 * Apply blend mode untuk scanline (optimized untuk SIMD)
 * 
 * @param dst Destination scanline (modified in-place)
 * @param src Source scanline
 * @param count Jumlah pixels
 * @param mode Blend mode
 * @param alpha Opacity
 */
void apply_blend_mode_scanline(
    Color32* dst,
    const Color32* src,
    int count,
    BlendMode mode,
    uint8_t alpha
);

/**
 * Apply blend mode dengan SIMD optimization jika tersedia
 * Falls back ke scalar jika SIMD tidak available
 */
void apply_blend_mode_scanline_simd(
    Color32* dst,
    const Color32* src,
    int count,
    BlendMode mode,
    uint8_t alpha
);

#endif // FLYERPIX_BLEND_MODES_H
