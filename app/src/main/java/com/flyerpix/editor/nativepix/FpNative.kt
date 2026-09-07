package com.flyerpix.editor.nativepix

import android.annotation.SuppressLint

/**
 * Akses ke pustaka native FlyerPix (NDK/CMake).
 * Saat ini berisi Gaussian blur 2-pass yang mengubah `pixels` (RGBA8888
 * dari [android.graphics.Bitmap.getPixels]) secara in-place.
 */
object FpNative {

    init {
        System.loadLibrary("flyerpix_native")
    }

    /**
     * Menerapkan Gaussian blur 2-pass pada array piksel RGBA8888.
     *
     * @param pixels array piksel panjang width*height, diubah in-place.
     * @param width lebar bitmap (tanpa padding).
     * @param height tinggi bitmap.
     * @param radius radius blur (>= 1). Sigma dihitung radius/2.
     */
    @SuppressLint("DiscouragedApi")
    external fun blurPixels(pixels: IntArray, width: Int, height: Int, radius: Int)

    /**
     * Menerapkan ColorMatrix adjustment (brightness/contrast/saturation)
     * pada array piksel RGBA8888 secara in-place. Skala nilai sama dengan
     * adjustments di CanvasAdjustment: brightness 0..100, contrast +/-100,
     * saturation +/-100.
     */
    @SuppressLint("DiscouragedApi")
    external fun applyColorMatrix(
        pixels: IntArray,
        brightness: Float,
        contrast: Float,
        saturation: Float,
    )
}