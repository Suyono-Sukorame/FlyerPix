package com.flyerpix.editor.canvas.model

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import java.io.Serializable
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tipe pola gradasi yang didukung:
 * - [LINEAR] : Gradasi garis lurus dengan sudut putar ([GradientColor.angle]).
 * - [RADIAL] : Gradasi melingkar dari pusat ke tepi teks.
 * - [SWEEP]  : Gradasi menyapu memutar (efek kerucut / spektrum 360°).
 */
enum class GradientType {
    LINEAR,
    RADIAL,
    SWEEP
}

/**
 * Data model untuk opsi pewarnaan gradasi pada [TextLayer].
 *
 * @property colors     Koleksi warna ARGB untuk gradasi (minimal 2 warna).
 * @property positions  Stop posisi relatif (0.0 .. 1.0) tiap warna. Null = distribusi rata.
 * @property type       Tipe gradasi ([GradientType.LINEAR], [GradientType.RADIAL], [GradientType.SWEEP]).
 * @property angle      Sudut rotasi gradasi linier dalam derajat (0°–360°).
 * @property opacity    Transparansi global gradasi (0.0 = tembus, 1.0 = solid), terpisah dari warna.
 * @property name       Nama preset untuk tampilan UI swatch (misal "Sunset", "Ocean").
 */
data class GradientColor(
    var colors: IntArray,
    var positions: FloatArray? = null,
    var type: GradientType = GradientType.LINEAR,
    var angle: Float = 0f,
    var opacity: Float = 1f,
    var name: String = ""
) : Serializable {

    /**
     * Warna efektif setelah dikalikan [opacity] pada kanal alpha.
     * Dipakai saat membuat shader agar seluruh renderer ikut transparan.
     */
    private fun effectiveColors(): IntArray {
        val o = opacity.coerceIn(0f, 1f)
        if (o >= 1f) return colors
        return IntArray(colors.size) { i ->
            val c = colors[i]
            val a = (Color.alpha(c) * o).roundToInt().coerceIn(0, 255)
            (c and 0x00FFFFFF) or (a shl 24)
        }
    }

    /**
     * Menghasilkan instance [Shader] Android yang disesuaikan dengan batas area [bounds] (Prompt 44).
     */
    fun createShader(bounds: RectF): Shader {
        val w = if ((bounds.right - bounds.left) <= 0f) 1f else (bounds.right - bounds.left)
        val h = if ((bounds.bottom - bounds.top) <= 0f) 1f else (bounds.bottom - bounds.top)
        val cx = (bounds.left + bounds.right) / 2f
        val cy = (bounds.top + bounds.bottom) / 2f
        val shaderColors = effectiveColors()

        return when (type) {
            GradientType.LINEAR -> {
                // Hitung koordinat (x0, y0) ke (x1, y1) berdasarkan sudut derajat
                val rad = Math.toRadians(angle.toDouble())
                val r = max(w, h) / 2f
                val dx = (cos(rad) * r).toFloat()
                val dy = (sin(rad) * r).toFloat()
                LinearGradient(
                    cx - dx, cy - dy,
                    cx + dx, cy + dy,
                    shaderColors, positions,
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.RADIAL -> {
                val radius = max(w, h) / 2f
                RadialGradient(
                    cx, cy, max(1f, radius),
                    shaderColors, positions,
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.SWEEP -> {
                SweepGradient(cx, cy, shaderColors, positions)
            }
        }
    }

    /**
     * Menghasilkan instance [Shader] Android yang disesuaikan dengan dimensi teks ([width] & [height]).
     */
    fun createShader(width: Float, height: Float): Shader {
        val rect = RectF().apply {
            left = 0f
            top = 0f
            right = width
            bottom = height
        }
        return createShader(rect)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GradientColor
        if (!colors.contentEquals(other.colors)) return false
        if (positions != null) {
            if (other.positions == null) return false
            if (!positions.contentEquals(other.positions)) return false
        } else if (other.positions != null) return false
        if (type != other.type) return false
        if (angle != other.angle) return false
        if (opacity != other.opacity) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        var result = colors.contentHashCode()
        result = 31 * result + (positions?.contentHashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + angle.hashCode()
        result = 31 * result + opacity.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    companion object {
        /**
         * Koleksi preset gradasi populer bawaan bergaya PixelLab.
         */
        val PRESETS: List<GradientColor> = listOf(
            GradientColor(
                colors = intArrayOf(0xFFFF512F.toInt(), 0xFFDD2476.toInt()),
                name = "Sunset",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFF2193B0.toInt(), 0xFF6DD5ED.toInt()),
                name = "Ocean",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFF11998E.toInt(), 0xFF38EF7D.toInt()),
                name = "Lime",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFF8E2DE2.toInt(), 0xFF4A00E0.toInt()),
                name = "Violet",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFFF7971E.toInt(), 0xFFFFD200.toInt()),
                name = "Golden",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFFFF6A88.toInt(), 0xFFFF99AC.toInt()),
                name = "Candy",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFF00C6FF.toInt(), 0xFF0072FF.toInt()),
                name = "Skyline",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFFF857A6.toInt(), 0xFFFF5858.toInt()),
                name = "Flamingo",
                type = GradientType.LINEAR
            ),
            GradientColor(
                colors = intArrayOf(0xFFFFD200.toInt(), 0xFFFF512F.toInt(), 0xFF7928CA.toInt()),
                positions = floatArrayOf(0f, 0.5f, 1f),
                name = "Sun Radial",
                type = GradientType.RADIAL
            ),
            GradientColor(
                colors = intArrayOf(0xFF00F2FE.toInt(), 0xFF4FACFE.toInt(), 0xFF000000.toInt()),
                positions = floatArrayOf(0f, 0.6f, 1f),
                name = "Neon Pulse",
                type = GradientType.RADIAL
            ),
            GradientColor(
                colors = intArrayOf(
                    0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
                    0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt()
                ),
                name = "Rainbow",
                type = GradientType.SWEEP
            ),
            GradientColor(
                colors = intArrayOf(
                    0xFFE0E0E0.toInt(), 0xFF757575.toInt(),
                    0xFFFFFFFF.toInt(), 0xFF424242.toInt(),
                    0xFFE0E0E0.toInt()
                ),
                name = "Chrome",
                type = GradientType.SWEEP
            )
        )
    }
}
