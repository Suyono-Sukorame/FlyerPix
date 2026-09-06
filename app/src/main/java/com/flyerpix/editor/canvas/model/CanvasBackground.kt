package com.flyerpix.editor.canvas.model

import android.graphics.Bitmap
import android.graphics.Color
import java.io.Serializable

/**
 * Tipe mode latar belakang kanvas independen PixelLab (Prompt 44).
 */
enum class CanvasBackgroundMode {
    /** Latar belakang transparan dengan pola kotak-kotak catur (checkerboard). */
    TRANSPARENT,

    /** Latar belakang warna solid tunggal. */
    SOLID_COLOR,

    /** Latar belakang gradasi multi-warna (Linear, Radial, Sweep). */
    GRADIENT,

    /** Latar belakang gambar dari Galeri atau Kamera (Prompt 45). */
    IMAGE
}

/**
 * Konfigurasi independen latar belakang kanvas [com.flyerpix.editor.canvas.PixelCanvasView] (Prompt 44).
 *
 * @param mode Mode aktif latar belakang kanvas.
 * @param solidColor Nilai ARGB warna latar belakang saat mode [CanvasBackgroundMode.SOLID_COLOR].
 * @param gradient Konfigurasi gradasi saat mode [CanvasBackgroundMode.GRADIENT].
 * @param imageBitmap Bitmap gambar latar belakang saat mode [CanvasBackgroundMode.IMAGE].
 */
data class CanvasBackground(
    var mode: CanvasBackgroundMode = CanvasBackgroundMode.SOLID_COLOR,
    var solidColor: Int = Color.WHITE,
    var gradient: GradientColor? = null,
    var imageBitmap: Bitmap? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * Membuat konfigurasi latar belakang transparan.
         */
        fun transparent(): CanvasBackground = CanvasBackground(
            mode = CanvasBackgroundMode.TRANSPARENT
        )

        /**
         * Membuat konfigurasi latar belakang warna solid.
         */
        fun solid(color: Int): CanvasBackground = CanvasBackground(
            mode = CanvasBackgroundMode.SOLID_COLOR,
            solidColor = color
        )

        /**
         * Membuat konfigurasi latar belakang gradasi.
         */
        fun gradient(gradient: GradientColor): CanvasBackground = CanvasBackground(
            mode = CanvasBackgroundMode.GRADIENT,
            gradient = gradient
        )

        /**
         * Membuat konfigurasi latar belakang gambar.
         */
        fun image(bitmap: Bitmap): CanvasBackground = CanvasBackground(
            mode = CanvasBackgroundMode.IMAGE,
            imageBitmap = bitmap
        )
    }
}
