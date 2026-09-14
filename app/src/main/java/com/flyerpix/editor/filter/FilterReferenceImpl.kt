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
import kotlin.math.abs
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
    // Extended Color Adjust Reference (Prompt 01)
    //
    // Mirror C++ ColorFilter::adjustPixelAdvanced / applyExtended.
    // Semua parameter baru bernilai netral => output identik dengan input
    // (dan identik dengan colorAdjust() legacy pipeline).
    // ========================================================================

    private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)

    private fun clamp255(v: Float): Int = v.toInt().coerceIn(0, 255)

    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val cmax = maxOf(rf, gf, bf)
        val cmin = minOf(rf, gf, bf)
        val delta = cmax - cmin

        val v = cmax
        val s = if (cmax > 0f) delta / cmax else 0f

        var h = 0f
        if (delta == 0f) {
            h = 0f
        } else if (cmax == rf) {
            h = 60f * (((gf - bf) / delta) % 6f)
        } else if (cmax == gf) {
            h = 60f * (((bf - rf) / delta) + 2f)
        } else {
            h = 60f * (((rf - gf) / delta) + 4f)
        }
        h = if (h < 0f) h + 360f else h
        return floatArrayOf(h, s, v)
    }

    private fun hsvToRgb(hIn: Float, s: Float, v: Float): Triple<Int, Int, Int> {
        var h = hIn
        while (h < 0f) h += 360f
        while (h >= 360f) h -= 360f

        val c = v * s
        val hh = h / 60f
        val x = c * (1f - abs(hh % 2f - 1f))

        val (rf, gf, bf) = when {
            hh >= 0f && hh < 1f -> Triple(c, x, 0f)
            hh >= 1f && hh < 2f -> Triple(x, c, 0f)
            hh >= 2f && hh < 3f -> Triple(0f, c, x)
            hh >= 3f && hh < 4f -> Triple(0f, x, c)
            hh >= 4f && hh < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val m = v - c
        return Triple(
            ((rf + m) * 255f).toInt().coerceIn(0, 255),
            ((gf + m) * 255f).toInt().coerceIn(0, 255),
            ((bf + m) * 255f).toInt().coerceIn(0, 255)
        )
    }

    fun colorAdjustExtended(
        pixels: IntArray,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        hue: Float,
        exposure: Float = 0f,
        highlights: Float = 0f,
        shadows: Float = 0f,
        temperature: Float = 0f,
        tint: Float = 0f,
        gamma: Float = 1f,
        vibrance: Float = 0f
    ): IntArray {
        val out = IntArray(pixels.size)

        val cb = brightness.coerceIn(-1f, 1f)
        val cc = contrast.coerceIn(-1f, 1f)
        val cs = saturation.coerceIn(-1f, 1f)
        val ce = exposure.coerceIn(-1f, 1f)
        val chi = highlights.coerceIn(-1f, 1f)
        val csh = shadows.coerceIn(-1f, 1f)
        val ct = temperature.coerceIn(-1f, 1f)
        val ctn = tint.coerceIn(-1f, 1f)
        val cg = gamma.coerceIn(0.1f, 4f)
        val cv = vibrance.coerceIn(-1f, 1f)

        for (i in pixels.indices) {
            val argb = pixels[i]
            val a = (argb ushr 24) and 0xFF
            val r = (argb ushr 16) and 0xFF
            val g = (argb ushr 8) and 0xFF
            val b = argb and 0xFF

            val hsv = rgbToHsv(r, g, b)
            var h = hsv[0]
            var s = hsv[1]
            var v = hsv[2]

            // Exposure: multiplicative in stops
            v = clamp01(v * 2f.pow(ce))

            // Highlights / Shadows: smooth weight curves
            val shadowW = 1f - smoothstep(0f, 0.5f, v)
            val hiW = smoothstep(0.5f, 1f, v)
            v = if (csh > 0f) clamp01(v + csh * shadowW * (1f - v))
            else clamp01(v + csh * shadowW * v)
            v = if (chi > 0f) clamp01(v + chi * hiW * (1f - v))
            else clamp01(v + chi * hiW * v)

            // Brightness (legacy)
            v = clamp01(v + cb)

            // Contrast (legacy)
            v = clamp01(0.5f + (v - 0.5f) * (1f + cc))

            // Gamma: power curve centered pada luma
            v = clamp01(v).pow(1f / cg)

            // Saturation (legacy)
            s = clamp01(s * (1f + cs))

            // Vibrance: selective saturation
            s = clamp01(s * (1f + cv * (1f - s)))

            // Hue (legacy): rotate warna
            h = (h + hue) % 360f
            if (h < 0f) h += 360f

            val rgb = hsvToRgb(h, s, v)
            var outR = rgb.first
            var outG = rgb.second
            var outB = rgb.third

            // White balance: temperature + tint
            val tb = ct * 0.15f
            val tn = ctn * 0.15f
            outR = clamp255(outR * (1f + tb + tn))
            outG = clamp255(outG * (1f - tn))
            outB = clamp255(outB * (1f - tb + tn))

            out[i] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
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