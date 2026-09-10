package com.flyerpix.editor.ui.dialog

import android.content.Context
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType

/**
 * Entri recent: warna solid atau gradasi.
 */
sealed class RecentEntry {
    data class Solid(val color: Int) : RecentEntry()
    data class Gradient(val gradient: GradientColor) : RecentEntry()
}

/**
 * Penyimpanan terpadu untuk "Recent Color" (solid + gradasi).
 *
 * Menyimpan maksimal [MAX_RECENT] entri terbaru (paling baru di depan), dipakai
 * bersama oleh tab Solid, tab Gradient, dan strip recent di halaman 3D Text /
 * 3D Shadow. Format persisten:
 *  - solid   : `s,<argb>`
 *  - gradasi : `g|<#ARGBC1-#ARGBC2>|<TYPE>|<ANGLE>|<NAME>`
 */
class ColorRecents(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS = "color_recent"
        private const val KEY = "entries"
        const val MAX_RECENT = 8

        // Legacy store dari SolidColorEditorView (migrasi satu kali).
        private const val LEGACY_PREFS = "color_picker"
        private const val LEGACY_KEY = "solid_color_recents"
    }

    fun recents(): List<RecentEntry> {
        migrateIfNeeded()
        val raw = prefs.getString(KEY, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { decode(it) }
    }

    fun solids(): List<Int> = recents()
        .filterIsInstance<RecentEntry.Solid>()
        .map { it.color }

    fun pushSolid(color: Int) {
        insert(RecentEntry.Solid(color))
    }

    fun pushGradient(gradient: GradientColor) {
        insert(RecentEntry.Gradient(gradient))
    }

    private fun insert(entry: RecentEntry) {
        val current = recents()
        val filtered = current.filter { !sameEntry(it, entry) }
        val updated = (listOf(entry) + filtered).take(MAX_RECENT)
        prefs.edit().putString(KEY, updated.joinToString(";") { encode(it) }).apply()
    }

    private fun sameEntry(a: RecentEntry, b: RecentEntry): Boolean = when {
        a is RecentEntry.Solid && b is RecentEntry.Solid -> a.color == b.color
        a is RecentEntry.Gradient && b is RecentEntry.Gradient ->
            a.gradient.colors.contentEquals(b.gradient.colors) &&
                a.gradient.type == b.gradient.type &&
                a.gradient.angle == b.gradient.angle
        else -> false
    }

    fun isSelectedSolid(entry: RecentEntry, color: Int): Boolean =
        entry is RecentEntry.Solid && entry.color == color

    fun isSelectedGradient(entry: RecentEntry, g: GradientColor?): Boolean =
        entry is RecentEntry.Gradient && g != null &&
            entry.gradient.colors.contentEquals(g.colors) &&
            entry.gradient.type == g.type &&
            entry.gradient.angle == g.angle

    private fun encode(entry: RecentEntry): String = when (entry) {
        is RecentEntry.Solid -> "s,${entry.color}"
        is RecentEntry.Gradient -> {
            val g = entry.gradient
            val colorsHex = g.colors.joinToString("-") { String.format("#%08X", it) }
            "g|$colorsHex|${g.type.name}|${g.angle}|${g.name}"
        }
    }

    private fun decode(raw: String): RecentEntry? {
        if (raw.startsWith("s,")) {
            val color = raw.substring(2).toIntOrNull()
            return color?.let { RecentEntry.Solid(it) }
        }
        if (raw.startsWith("g|")) {
            val parts = raw.substring(2).split("|")
            if (parts.size < 4) return null
            val colors = parts[0].split("-").mapNotNull { hexColor(it) }
            if (colors.isEmpty()) return null
            val type = runCatching { GradientType.valueOf(parts[1]) }
                .getOrDefault(GradientType.LINEAR)
            val angle = parts[2].toFloatOrNull() ?: 0f
            val name = parts.getOrElse(3) { "Custom" }
            return RecentEntry.Gradient(
                GradientColor(colors = colors.toIntArray(), type = type, angle = angle, name = name)
            )
        }
        return null
    }

    private fun hexColor(hex: String): Int? =
        runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()

    /** Migrasi satu kali dari store lama solid_color_recents. */
    private fun migrateIfNeeded() {
        if (prefs.contains(KEY)) return
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val raw = legacy.getString(LEGACY_KEY, "").orEmpty()
        if (raw.isBlank()) return
        val solids = raw.split(",").mapNotNull { it.toIntOrNull() }
        val entries = solids.map { RecentEntry.Solid(it) }.toMutableList()
        if (entries.isNotEmpty()) {
            prefs.edit().putString(KEY, entries.joinToString(";") { encode(it) }).apply()
            legacy.edit().remove(LEGACY_KEY).apply()
        }
    }
}