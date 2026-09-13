package com.flyerpix.editor.canvas.model

/**
 * Enum untuk catalog semua effect types yang tersedia di Unified Effects System.
 * 
 * Setiap effect type memiliki:
 * - Technical name (enum constant)
 * - Human-readable name (untuk UI display)
 * - Category grouping (untuk organized UI panels)
 * 
 * Created: Prompt 1 - Architecture Foundation
 * Updated: Incremental (as new effects added)
 */
enum class LayerEffectType(
    val humanReadableName: String,
    val category: EffectCategory
) {
    // ── Basic Effects ────────────────────────────────────────────────────
    OPACITY("Opacity", EffectCategory.BASIC),
    DROP_SHADOW("Drop Shadow", EffectCategory.BASIC),
    BLUR("Blur", EffectCategory.BASIC),
    
    // ── 3D Effects ───────────────────────────────────────────────────────
    EMBOSS("Emboss / Bevel", EffectCategory.THREE_D),
    EXTRUDE_3D("3D Extrusion", EffectCategory.THREE_D),
    SHADOW_3D("3D Long Shadow", EffectCategory.THREE_D),
    ROTATE_3D("3D Rotation", EffectCategory.THREE_D),
    
    // ── Special Effects ──────────────────────────────────────────────────
    NEON("Neon / Glow", EffectCategory.SPECIAL),
    INNER_SHADOW("Inner Shadow", EffectCategory.SPECIAL),
    REFLECTION("Reflection", EffectCategory.SPECIAL),
    
    // ── Fill Effects ─────────────────────────────────────────────────────
    GRADIENT("Gradient Fill", EffectCategory.FILL),
    TEXTURE("Texture Fill", EffectCategory.FILL),
    
    // ── Advanced Effects ─────────────────────────────────────────────────
    BACKGROUND("Background Layer", EffectCategory.ADVANCED),
    CURVE("Curve / Arc Path", EffectCategory.ADVANCED),
    
    // ── Blend Modes ──────────────────────────────────────────────────────
    BLEND_MODE("Blend Mode", EffectCategory.BLEND);
    
    /**
     * Check apakah effect ini applicable untuk specific layer type.
     * Beberapa effects mungkin tidak make sense untuk certain layer types.
     * 
     * Default: All effects applicable untuk all layers.
     * Override di future jika ada restrictions.
     */
    fun isApplicableFor(layer: CanvasLayer): Boolean {
        // Default: semua effects applicable untuk semua layers
        // Future: bisa add conditional logic jika perlu
        // Contoh: CURVE might not make sense untuk ImageLayer
        return true
    }
    
    companion object {
        /**
         * Get all effects untuk specific category.
         */
        fun forCategory(category: EffectCategory): List<LayerEffectType> {
            return values().filter { it.category == category }
        }
        
        /**
         * Get human-readable names untuk UI dropdown/picker.
         */
        fun getAllNames(): List<String> {
            return values().map { it.humanReadableName }
        }
    }
}

/**
 * Effect categories untuk UI organization.
 * Digunakan untuk group effects dalam tabs/sections di unified effect panel.
 */
enum class EffectCategory(val displayName: String) {
    BASIC("Basic"),
    THREE_D("3D Effects"),
    SPECIAL("Special Effects"),
    FILL("Fill Effects"),
    ADVANCED("Advanced"),
    BLEND("Blending");
    
    companion object {
        /**
         * Get display names untuk UI tabs.
         */
        fun getAllDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}
