package com.flyerpix.editor.canvas.model

import java.io.Serializable

/**
 * Model data yang merepresentasikan preset ukuran dan resolusi kanvas standar di PixelLab (Prompt 43).
 *
 * @param name Nama tampilan preset (e.g. "YouTube Thumbnail (16:9)").
 * @param width Lebar kanvas dalam satuan piksel (px).
 * @param height Tinggi kanvas dalam satuan piksel (px).
 * @param description Deskripsi peruntukan preset (e.g. "Instagram Post, Foto Profil (1:1)").
 */
data class CanvasSizePreset(
    val name: String,
    val width: Int,
    val height: Int,
    val description: String = ""
) : Serializable {

    /**
     * Mengembalikan rasio aspek dalam bentuk teks sederhana (e.g. "1:1", "16:9", "205:78").
     */
    val aspectRatioText: String
        get() {
            val gcdVal = gcd(width, height)
            val rw = width / gcdVal
            val rh = height / gcdVal
            return "$rw:$rh"
        }

    /**
     * Nilai float perbandingan lebar dibagi tinggi (w / h).
     */
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1f

    override fun toString(): String = "$name ($width × $height)"

    companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * Menghitung faktor pembagi terbesar (Greatest Common Divisor) untuk menyederhanakan pecahan rasio.
         */
        fun gcd(a: Int, b: Int): Int {
            var x = if (a < 0) -a else a
            var y = if (b < 0) -b else b
            while (y != 0) {
                val temp = y
                y = x % y
                x = temp
            }
            return if (x == 0) 1 else x
        }

        /**
         * Menghasilkan teks rasio yang disederhanakan dari sembarang dimensi lebar dan tinggi.
         */
        fun formatAspectRatio(w: Int, h: Int): String {
            if (w <= 0 || h <= 0) return "1:1"
            val gcdVal = gcd(w, h)
            val rw = w / gcdVal
            val rh = h / gcdVal
            val ratioFloat = w.toFloat() / h.toFloat()
            val floatStr = String.format(java.util.Locale.US, "%.2f", ratioFloat)
            return "$rw:$rh ($floatStr)"
        }

        /**
         * Mengembalikan teks rasio sederhana tanpa desimal dalam format "rw:rh" (e.g. "1:1", "16:9").
         */
        fun getSimplifiedRatio(w: Int, h: Int): String {
            if (w <= 0 || h <= 0) return "1:1"
            val gcdVal = gcd(w, h)
            return "${w / gcdVal}:${h / gcdVal}"
        }

        /**
         * Mencari preset standar yang memiliki resolusi tepat sesuai [w] dan [h].
         */
        fun findMatchingPreset(w: Int, h: Int): CanvasSizePreset? {
            return PRESETS.filter { it.name != "Custom" }.firstOrNull { it.width == w && it.height == h }
        }

        /**
         * Daftar preset ukuran resolusi standar kanvas PixelLab (Prompt 43).
         */
        val PRESETS = listOf(
            CanvasSizePreset(
                name = "Custom",
                width = 1080,
                height = 1080,
                description = "Tentukan ukuran lebar & tinggi kustom bebas"
            ),
            CanvasSizePreset(
                name = "Persegi 1:1 (Instagram / Profil)",
                width = 1080,
                height = 1080,
                description = "Format feed Instagram, avatar profil WhatsApp/IG"
            ),
            CanvasSizePreset(
                name = "YouTube Thumbnail (16:9)",
                width = 1280,
                height = 720,
                description = "Sampul video standar YouTube resolusi HD 720p"
            ),
            CanvasSizePreset(
                name = "YouTube Channel Banner",
                width = 2560,
                height = 1440,
                description = "Header latar belakang banner saluran YouTube (16:9)"
            ),
            CanvasSizePreset(
                name = "Facebook Cover",
                width = 820,
                height = 312,
                description = "Foto sampul header halaman / grup Facebook"
            ),
            CanvasSizePreset(
                name = "Twitter / X Header",
                width = 1500,
                height = 500,
                description = "Banner header profil akun Twitter/X (3:1)"
            ),
            CanvasSizePreset(
                name = "Instagram Story / TikTok (9:16)",
                width = 1080,
                height = 1920,
                description = "Video pendek vertikal Story, Reels, TikTok (9:16)"
            )
        )
    }
}
