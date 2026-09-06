package com.flyerpix.editor.canvas.model

/**
 * Tipe proyeksi arah kedalaman efek 3D Extrusion pada [TextLayer]:
 * - [OBLIQUE]   : Proyeksi miring dengan sudut putar dinamis (0° - 360°).
 * - [ISOMETRIC] : Proyeksi isometrik dengan sudut proyeksi 30° standar grafis 3D.
 */
enum class ExtrudeViewType {
    OBLIQUE,
    ISOMETRIC
}
