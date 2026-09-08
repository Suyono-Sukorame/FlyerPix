/**
 * FilterReferenceImpl.kt
 *
 * Pure-Kotlin reference (baseline) implementations dari FilterEngine filters.
 *
 * Digunakan untuk benchmarking speedup native vs reference:
 * - Semua method bekerja pada IntArray pixels (ARGB_8888)
 * - Scalar, single-threaded, non-SIMD — disengaja lambat sebagai baseline
 * - Algorithms mirror native C++ implementations
 */

package com.flyerpix.editor.filter

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.pow

/**
 * FilterReferenceImpl - Scalar reference implementations untuk benchmark baseline
 */
object FilterReferenceImpl {

    /**
     * Convert Bitmap ke IntArray pixels (ARGB)
     */
    fun bitmapToPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    fun pixelsToBitmap(bitmap: Bitmap, pixels: IntArray) {
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    // ========================================================================
    // Grayscale Reference  (Y = 0.299R + 0.587G + 0.114B)
    // ========================================================================

    fun grayscale(pixels: IntArray): IntArray {
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val r = (pixels[i] ushr 16) and 0xFF
            val g = (pixels[i] ushr 8) and 0xFF
            val b = pixels[i] and 0xFF
            val a = (pixels[i] ushr 24) and 0xFF

            val y = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            out[i] = (a shl 24) or (y shl 16) or (y shl 8) or y
        }
        return out
    }

    // ========================================================================
    // Invert Reference  (255 - channel)
    // ========================================================================

    fun invert(pixels: IntArray): IntArray {
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val r = (pixels[i] ushr 16) and 0xFF
            val g = (pixels[i] ushr 8) and 0xFF
            val b = pixels[i] and 0xFF
            val a = (pixels[i] ushr 24) and 0xFF

            out[i] = (a shl 24) or ((255 - r) shl 16) or ((255 - g) shl 8) or (255 - b)
        }
        return out
    }

    // ========================================================================
    // Sepia Reference  (classic sepia matrix)
    // ========================================================================

    fun sepia(pixels: IntArray, intensity: Float = 0.7f): IntArray {
        val out = IntArray(pixels.size)
        val t = intensity.coerceIn(0.0f, 1.0f)
        val invT = 1.0f - t

        for (i in pixels.indices) {
            val r = (pixels[i] ushr 16) and 0xFF
            val g = (pixels[i] ushr 8) and 0xFF
            val b = pixels[i] and 0xFF
            val a = (pixels[i] ushr 24) and 0xFF

            val sr = (0.393 * r + 0.769 * g + 0.189 * b).toInt()
            val sg = (0.349 * r + 0.686 * g + 0.168 * b).toInt()
            val sb = (0.272 * r + 0.534 * g + 0.131 * b).toInt()

            val or = (t * sr + invT * r).toInt().coerceIn(0, 255)
            val og = (t * sg + invT * g).toInt().coerceIn(0, 255)
            val ob = (t * sb + invT * b).toInt().coerceIn(0, 255)

            out[i] = (a shl 24) or (or shl 16) or (og shl 8) or ob
        }
        return out
    }

    // ========================================================================
    // Blur Reference  (separable 2-pass Gaussian)
    // ========================================================================

    fun gaussianKernel(radius: Int): FloatArray {
        val size = radius * 2 + 1
        val kernel = FloatArray(size)
        var sum = 0.0f

        val sigma = (radius / 2.0f).coerceAtLeast(0.5f)
        for (i in 0 until size) {
            val x = i - radius
            kernel[i] = Math.exp(-(x * x).toDouble() / (2.0 * sigma * sigma)).toFloat()
            sum += kernel[i]
        }
        for (i in 0 until size) kernel[i] /= sum
        return kernel
    }

    fun blur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val kernel = gaussianKernel(radius)
        val size = radius * 2 + 1

        // Horizontal pass
        val hPass = IntArray(pixels.size)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var rAcc = 0.0; var gAcc = 0.0; var bAcc = 0.0
                for (k in 0 until size) {
                    val sx = (x + k - radius).coerceIn(0, width - 1)
                    val p = pixels[row + sx]
                    val kr = (p ushr 16) and 0xFF
                    val kg = (p ushr 8) and 0xFF
                    val kb = p and 0xFF
                    val w = kernel[k].toDouble()
                    rAcc += kr * w; gAcc += kg * w; bAcc += kb * w
                }
                hPass[row + x] = ((rAcc.toInt().coerceIn(0, 255)) shl 16) or
                        ((gAcc.toInt().coerceIn(0, 255)) shl 8) or
                        (bAcc.toInt().coerceIn(0, 255)) or 0xFF000000.toInt()
            }
        }

        // Vertical pass
        val out = IntArray(pixels.size)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var rAcc = 0.0; var gAcc = 0.0; var bAcc = 0.0
                for (k in 0 until size) {
                    val sy = (y + k - radius).coerceIn(0, height - 1)
                    val p = hPass[sy * width + x]
                    val kr = (p ushr 16) and 0xFF
                    val kg = (p ushr 8) and 0xFF
                    val kb = p and 0xFF
                    val w = kernel[k].toDouble()
                    rAcc += kr * w; gAcc += kg * w; bAcc += kb * w
                }
                out[row + x] = ((rAcc.toInt().coerceIn(0, 255)) shl 16) or
                        ((gAcc.toInt().coerceIn(0, 255)) shl 8) or
                        (bAcc.toInt().coerceIn(0, 255)) or 0xFF000000.toInt()
            }
        }
        return out
    }

    // ========================================================================
    // Color Adjust Reference  (brightness, contrast, saturation)
    // ========================================================================

    fun colorAdjust(
        pixels: IntArray,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        hue: Float = 0.0f
    ): IntArray {
        val out = IntArray(pixels.size)

        // Brightness: factor 0.2 = +51 offset (mirrors native clamp [-1,1])
        val brightnessOffset = brightness * 255.0f
        // Contrast factor: [-1,1] mapped to scale [0.5, 1.5]
        val contrastFactor = 1.0f + contrast * 0.5f
        val satFactor = 1.0f + saturation

        for (i in pixels.indices) {
            val r = (pixels[i] ushr 16) and 0xFF
            val g = (pixels[i] ushr 8) and 0xFF
            val b = pixels[i] and 0xFF
            val a = (pixels[i] ushr 24) and 0xFF

            var cr = r + brightnessOffset
            cr = (cr - 128.0f) * contrastFactor + 128.0f

            // Saturation via weighted luminance
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            var satR = lum + (cr - lum) * satFactor
            var satG = lum + (g - lum) * satFactor
            var satB = lum + (b - lum) * satFactor

            satR = satR.coerceIn(0f, 255f).toInt().toFloat()
            satG = satG.coerceIn(0f, 255f).toInt().toFloat()
            satB = satB.coerceIn(0f, 255f).toInt().toFloat()

            out[i] = (a shl 24) or (satR.toInt() shl 16) or (satG.toInt() shl 8) or satB.toInt()
        }
        return out
    }

    // ========================================================================
    // Emboss Reference  (3x3 directional Sobel-like kernel)
    // ========================================================================

    fun emboss(pixels: IntArray, width: Int, height: Int, amount: Float, angleDeg: Float): IntArray {
        val out = IntArray(pixels.size)
        val t = amount.coerceIn(0.0f, 2.0f)

        val rad = Math.toRadians(angleDeg.toDouble())
        val kx = Math.cos(rad)
        val ky = Math.sin(rad)

        // 3x3 kernel: gradient along lighting direction
        val kernel = Array(3) { DoubleArray(3) }
        for (dy in -1..1) {
            for (dx in -1..1) {
                kernel[dy + 1][dx + 1] = dx * kx + dy * ky
            }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                var rAcc = 0.0; var gAcc = 0.0; var bAcc = 0.0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val sx = (x + dx).coerceIn(0, width - 1)
                        val sy = (y + dy).coerceIn(0, height - 1)
                        val p = pixels[sy * width + sx]
                        val w = kernel[dy + 1][dx + 1]
                        rAcc += ((p ushr 16) and 0xFF) * w
                        gAcc += ((p ushr 8) and 0xFF) * w
                        bAcc += (p and 0xFF) * w
                    }
                }
                val r = (128 + t * rAcc).toInt().coerceIn(0, 255)
                val g = (128 + t * gAcc).toInt().coerceIn(0, 255)
                val b = (128 + t * bAcc).toInt().coerceIn(0, 255)
                out[y * width + x] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
        return out
    }
}