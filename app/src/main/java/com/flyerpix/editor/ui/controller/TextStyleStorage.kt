package com.flyerpix.editor.ui.controller

import android.content.Context
import android.graphics.Color
import android.text.Layout
import com.flyerpix.editor.canvas.model.ExtrudeViewType
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.font.FontManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Gaya teks tersimpan (style preset) yang di-serialisasi ke SharedPreferences
 * sehingga bertahan antar sesi aplikasi.
 *
 * Bidang yang disimpan setara dengan yang diterapkan oleh "Terapkan Style",
 * mengecualikan bitmap tekstur (tidak dapat disimpan ringan) serta atribut
 * posisi/skala/rotasi global layer.
 */
data class SavedTextStyle(
    var text: String = "",
    var textSize: Float = 64f,
    var textColor: Int = Color.WHITE,
    var fontName: String? = null,
    var letterSpacing: Float = 0f,
    var lineSpacing: Float = 0f,
    var alignment: String = "ALIGN_CENTER",
    var justifyEnabled: Boolean = false,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 0f,
    var shadowEnabled: Boolean = false,
    var shadowColor: Int = Color.BLACK,
    var shadowRadius: Float = 8f,
    var shadowDx: Float = 4f,
    var shadowDy: Float = 4f,
    var shadowOpacity: Float = 0.6f,
    var innerShadowEnabled: Boolean = false,
    var innerShadowColor: Int = Color.BLACK,
    var innerShadowRadius: Float = 6f,
    var innerShadowDx: Float = 0f,
    var innerShadowDy: Float = 4f,
    var innerShadowOpacity: Float = 0.8f,
    var embossEnabled: Boolean = false,
    var embossLightAngle: Float = 45f,
    var embossAmbient: Float = 0.2f,
    var embossSpecular: Float = 8f,
    var embossIntensity: Float = 1f,
    var embossBevel: Float = 3f,
    var neonEnabled: Boolean = false,
    var neonColor: Int = 0xFF00E5FF.toInt(),
    var neonRadius: Float = 12f,
    var neonIntensity: Float = 1f,
    var neonCoreEnabled: Boolean = true,
    var gradientEnabled: Boolean = false,
    var gradient: GradientColor? = null,
    var textureEnabled: Boolean = false,
    var textureScale: Float = 1f,
    var textureRotation: Float = 0f,
    var extrudeEnabled: Boolean = false,
    var extrudeDepth: Int = 10,
    var extrudeColor: Int = 0xFF333333.toInt(),
    var extrudeGradient: GradientColor? = null,
    var extrudeViewType: String = "OBLIQUE",
    var extrudeAngle: Float = 45f,
    var rotate3DX: Float = 0f,
    var rotate3DY: Float = 0f,
    var rotate3DZ: Float = 0f,
    var curvePercent: Int = 0,
    var paddingTop: Float = 0f,
    var paddingBottom: Float = 0f,
    var paddingLeft: Float = 0f,
    var paddingRight: Float = 0f,
    var bgEnabled: Boolean = false,
    var bgColor: Int = Color.BLACK,
    var bgOpacity: Float = 1f,
    var bgPadding: Float = 0f,
    var bgCornerRadius: Float = 0f,
    var reflectionEnabled: Boolean = false,
    var reflectionOpacity: Float = 0.4f,
    var reflectionDistance: Float = 10f,
    var reflectionFade: Float = 0.5f
) {
    companion object {
        fun fromLayer(layer: TextLayer): SavedTextStyle = SavedTextStyle(
            text = layer.text,
            textSize = layer.textSize,
            textColor = layer.textColor,
            fontName = layer.fontName,
            letterSpacing = layer.letterSpacing,
            lineSpacing = layer.lineSpacing,
            alignment = layer.alignment.name,
            justifyEnabled = layer.justifyEnabled,
            isBold = layer.isBold,
            isItalic = layer.isItalic,
            isUnderline = layer.isUnderline,
            isStrikethrough = layer.isStrikethrough,
            strokeColor = layer.strokeColor,
            strokeWidth = layer.strokeWidth,
            shadowEnabled = layer.shadowEnabled,
            shadowColor = layer.shadowColor,
            shadowRadius = layer.shadowRadius,
            shadowDx = layer.shadowDx,
            shadowDy = layer.shadowDy,
            shadowOpacity = layer.shadowOpacity,
            innerShadowEnabled = layer.innerShadowEnabled,
            innerShadowColor = layer.innerShadowColor,
            innerShadowRadius = layer.innerShadowRadius,
            innerShadowDx = layer.innerShadowDx,
            innerShadowDy = layer.innerShadowDy,
            innerShadowOpacity = layer.innerShadowOpacity,
            embossEnabled = layer.embossEnabled,
            embossLightAngle = layer.embossLightAngle,
            embossAmbient = layer.embossAmbient,
            embossSpecular = layer.embossSpecular,
            embossIntensity = layer.embossIntensity,
            embossBevel = layer.embossBevel,
            neonEnabled = layer.neonEnabled,
            neonColor = layer.neonColor,
            neonRadius = layer.neonRadius,
            neonIntensity = layer.neonIntensity,
            neonCoreEnabled = layer.neonCoreEnabled,
            gradientEnabled = layer.gradientEnabled,
            gradient = layer.gradient?.copy(),
            textureEnabled = layer.textureEnabled,
            textureScale = layer.textureScale,
            textureRotation = layer.textureRotation,
            extrudeEnabled = layer.extrudeEnabled,
            extrudeDepth = layer.extrudeDepth,
            extrudeColor = layer.extrudeColor,
            extrudeGradient = layer.extrudeGradient?.copy(),
            extrudeViewType = layer.extrudeViewType.name,
            extrudeAngle = layer.extrudeAngle,
            rotate3DX = layer.rotate3DX,
            rotate3DY = layer.rotate3DY,
            rotate3DZ = layer.rotate3DZ,
            curvePercent = layer.curvePercent,
            paddingTop = layer.paddingTop,
            paddingBottom = layer.paddingBottom,
            paddingLeft = layer.paddingLeft,
            paddingRight = layer.paddingRight,
            bgEnabled = layer.bgEnabled,
            bgColor = layer.bgColor,
            bgOpacity = layer.bgOpacity,
            bgPadding = layer.bgPadding,
            bgCornerRadius = layer.bgCornerRadius,
            reflectionEnabled = layer.reflectionEnabled,
            reflectionOpacity = layer.reflectionOpacity,
            reflectionDistance = layer.reflectionDistance,
            reflectionFade = layer.reflectionFade
        )
    }

    /** Menerapkan seluruh gaya tersimpan ke [target] (setara "Terapkan Style"). */
    fun applyTo(target: TextLayer) {
        target.text = text
        target.textSize = textSize
        target.textColor = textColor
        target.typeface = FontManager.findFont(fontName)?.typeface
        target.fontName = fontName
        target.letterSpacing = letterSpacing
        target.lineSpacing = lineSpacing
        target.alignment = when (alignment) {
            "ALIGN_OPPOSITE" -> Layout.Alignment.ALIGN_OPPOSITE
            "ALIGN_CENTER" -> Layout.Alignment.ALIGN_CENTER
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        target.justifyEnabled = justifyEnabled
        target.isBold = isBold
        target.isItalic = isItalic
        target.isUnderline = isUnderline
        target.isStrikethrough = isStrikethrough
        target.strokeColor = strokeColor
        target.strokeWidth = strokeWidth
        target.shadowEnabled = shadowEnabled
        target.shadowColor = shadowColor
        target.shadowRadius = shadowRadius
        target.shadowDx = shadowDx
        target.shadowDy = shadowDy
        target.shadowOpacity = shadowOpacity
        target.innerShadowEnabled = innerShadowEnabled
        target.innerShadowColor = innerShadowColor
        target.innerShadowRadius = innerShadowRadius
        target.innerShadowDx = innerShadowDx
        target.innerShadowDy = innerShadowDy
        target.innerShadowOpacity = innerShadowOpacity
        target.embossEnabled = embossEnabled
        target.embossLightAngle = embossLightAngle
        target.embossAmbient = embossAmbient
        target.embossSpecular = embossSpecular
        target.embossIntensity = embossIntensity
        target.embossBevel = embossBevel
        target.neonEnabled = neonEnabled
        target.neonColor = neonColor
        target.neonRadius = neonRadius
        target.neonIntensity = neonIntensity
        target.neonCoreEnabled = neonCoreEnabled
        target.gradientEnabled = gradientEnabled
        target.gradient = gradient?.copy()
        target.textureEnabled = textureEnabled
        target.textureScale = textureScale
        target.textureRotation = textureRotation
        target.extrudeEnabled = extrudeEnabled
        target.extrudeDepth = extrudeDepth
        target.extrudeColor = extrudeColor
        target.extrudeGradient = extrudeGradient?.copy()
        target.extrudeViewType = try {
            ExtrudeViewType.valueOf(extrudeViewType)
        } catch (e: IllegalArgumentException) {
            ExtrudeViewType.OBLIQUE
        }
        target.extrudeAngle = extrudeAngle
        target.rotate3DX = rotate3DX
        target.rotate3DY = rotate3DY
        target.rotate3DZ = rotate3DZ
        target.curvePercent = curvePercent
        target.paddingTop = paddingTop
        target.paddingBottom = paddingBottom
        target.paddingLeft = paddingLeft
        target.paddingRight = paddingRight
        target.bgEnabled = bgEnabled
        target.bgColor = bgColor
        target.bgOpacity = bgOpacity
        target.bgPadding = bgPadding
        target.bgCornerRadius = bgCornerRadius
        target.reflectionEnabled = reflectionEnabled
        target.reflectionOpacity = reflectionOpacity
        target.reflectionDistance = reflectionDistance
        target.reflectionFade = reflectionFade
    }
}

/**
 * Penyimpanan persisten style teks ke SharedPreferences (format JSON via Gson).
 */
object TextStyleStorage {
    private const val PREFS = "flyerpix_text_styles"
    private const val KEY = "styles_json"
    private val gson = Gson()

    fun load(context: Context): LinkedHashMap<String, SavedTextStyle> {
        val json = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return LinkedHashMap()
        return try {
            val type = object : TypeToken<LinkedHashMap<String, SavedTextStyle>>() {}.type
            gson.fromJson<LinkedHashMap<String, SavedTextStyle>>(json, type) ?: LinkedHashMap()
        } catch (e: Exception) {
            LinkedHashMap()
        }
    }

    fun save(context: Context, styles: Map<String, SavedTextStyle>) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, gson.toJson(styles))
            .apply()
    }
}