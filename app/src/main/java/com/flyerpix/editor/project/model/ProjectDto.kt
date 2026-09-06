package com.flyerpix.editor.project.model

/**
 * Root DTO representasi file proyek PixelLab (.plp).
 *
 * Semua field bertipe primitif/String agar serialisasi Gson berjalan aman
 * di JVM maupun unit test tanpa memerlukan Android runtime.
 *
 * @property schemaVersion Versi format file .plp untuk kompatibilitas ke depan.
 * @property projectName   Nama proyek yang ditampilkan pada UI.
 * @property createdAt     Epoch millis saat proyek pertama kali dibuat.
 * @property updatedAt     Epoch millis terakhir kali proyek disimpan.
 * @property canvasWidth   Lebar kanvas dalam piksel (logis).
 * @property canvasHeight  Tinggi kanvas dalam piksel (logis).
 * @property background    Konfigurasi latar belakang kanvas.
 * @property layers        Daftar DTO layer (dari z-index terendah ke tertinggi).
 */
data class ProjectDto(
    val schemaVersion: Int = 1,
    val projectName: String = "Untitled",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080,
    val background: CanvasBackgroundDto = CanvasBackgroundDto(),
    val layers: List<LayerDto> = emptyList()
)

// ─── Background ───────────────────────────────────────────────────────────────

/**
 * DTO konfigurasi latar belakang kanvas.
 *
 * @property mode       Salah satu dari: "TRANSPARENT", "SOLID_COLOR", "GRADIENT", "IMAGE".
 * @property solidColor Warna ARGB latar belakang solid (sebagai Int).
 * @property gradient   Konfigurasi gradasi; null jika mode bukan GRADIENT.
 * @property imageBase64 Gambar latar belakang dikodekan sebagai Base64 PNG; null jika mode bukan IMAGE.
 */
data class CanvasBackgroundDto(
    val mode: String = "SOLID_COLOR",
    val solidColor: Int = -1,        // Color.WHITE = -1
    val gradient: GradientColorDto? = null,
    val imageBase64: String? = null
)

/**
 * DTO konfigurasi warna gradasi.
 *
 * @property type     Salah satu dari: "LINEAR", "RADIAL", "SWEEP".
 * @property colors   Daftar warna ARGB titik henti gradasi.
 * @property positions Daftar posisi relatif (0.0–1.0) per titik henti; null = merata.
 * @property angle    Sudut gradasi linear dalam derajat (0–360).
 */
data class GradientColorDto(
    val type: String = "LINEAR",
    val colors: List<Int> = emptyList(),
    val positions: List<Float>? = null,
    val angle: Float = 0f
)

// ─── Layer ────────────────────────────────────────────────────────────────────

/**
 * DTO polimorfik yang merepresentasikan satu layer kanvas dari salah satu tipe:
 * TEXT, IMAGE, SHAPE, STICKER, ARROW, PEN.
 *
 * Field yang tidak relevan untuk tipe layer tertentu bernilai null / default.
 */
data class LayerDto(
    // ── Discriminator ───────────────────────────────────────────────────────
    /** Tipe layer: "TEXT" | "IMAGE" | "SHAPE" | "STICKER" | "ARROW" | "PEN" */
    val type: String,

    // ── Properti umum CanvasLayer ────────────────────────────────────────────
    val id: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Int = 255,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val blendMode: String = "SRC_OVER",
    val perspectiveEnabled: Boolean = false,
    /** 8 float perspektif corners dikodekan sebagai CSV "x0,y0,x1,y1,..." */
    val perspectiveCorners: String = "0,0,1,0,1,1,0,1",

    // ── TextLayer ────────────────────────────────────────────────────────────
    val text: String? = null,
    val textSize: Float? = null,
    val textColor: Int? = null,
    val fontName: String? = null,
    val letterSpacing: Float? = null,
    val lineSpacing: Float? = null,
    /** "ALIGN_NORMAL" | "ALIGN_OPPOSITE" | "ALIGN_CENTER" */
    val alignment: String? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val isUnderline: Boolean? = null,
    val isStrikethrough: Boolean? = null,
    val justifyEnabled: Boolean? = null,
    val paddingTop: Float? = null,
    val paddingRight: Float? = null,
    val paddingBottom: Float? = null,
    val paddingLeft: Float? = null,
    val bgEnabled: Boolean? = null,
    val bgColor: Int? = null,
    val bgOpacity: Float? = null,
    val bgPadding: Float? = null,
    val bgCornerRadius: Float? = null,
    val reflectionEnabled: Boolean? = null,
    val reflectionOpacity: Float? = null,
    val reflectionDistance: Float? = null,
    val reflectionFade: Float? = null,
    val strokeColor: Int? = null,
    val strokeWidth: Float? = null,
    val shadowEnabled: Boolean? = null,
    val shadowColor: Int? = null,
    val shadowRadius: Float? = null,
    val shadowDx: Float? = null,
    val shadowDy: Float? = null,
    val shadowOpacity: Float? = null,
    val innerShadowEnabled: Boolean? = null,
    val innerShadowColor: Int? = null,
    val innerShadowRadius: Float? = null,
    val innerShadowDx: Float? = null,
    val innerShadowDy: Float? = null,
    val innerShadowOpacity: Float? = null,
    val embossEnabled: Boolean? = null,
    val embossLightAngle: Float? = null,
    val embossAmbient: Float? = null,
    val embossSpecular: Float? = null,
    val embossBlurRadius: Float? = null,
    val gradientEnabled: Boolean? = null,
    val textGradient: GradientColorDto? = null,
    val textureEnabled: Boolean? = null,
    val textureScale: Float? = null,
    val textureRotation: Float? = null,
    /** Bitmap tekstur dikodekan Base64 PNG; null jika tidak ada. */
    val textureBase64: String? = null,
    val extrudeEnabled: Boolean? = null,
    val extrudeDepth: Int? = null,
    val extrudeColor: Int? = null,
    /** "OBLIQUE" | "ISOMETRIC" */
    val extrudeViewType: String? = null,
    val extrudeAngle: Float? = null,
    val rotate3DX: Float? = null,
    val rotate3DY: Float? = null,
    val rotate3DZ: Float? = null,
    val curvePercent: Int? = null,

    // ── ImageLayer / StickerLayer ─────────────────────────────────────────────
    /** Bitmap layer gambar/stiker dikodekan Base64 PNG. */
    val bitmapBase64: String? = null,
    val layerName: String? = null,

    // ── ShapeLayer ───────────────────────────────────────────────────────────
    /** "RECTANGLE" | "ROUNDED_RECTANGLE" | "CIRCLE" | "TRIANGLE" | "STAR" */
    val shapeType: String? = null,
    val shapeWidth: Float? = null,
    val shapeHeight: Float? = null,
    val fillColor: Int? = null,
    val shapeStrokeColor: Int? = null,
    val shapeStrokeWidth: Float? = null,
    val cornerRadiusX: Float? = null,
    val cornerRadiusY: Float? = null,
    val starPoints: Int? = null,
    val starInnerRadiusRatio: Float? = null,

    // ── ArrowLayer ───────────────────────────────────────────────────────────
    /** "STRAIGHT" | "CURVED" */
    val arrowStyle: String? = null,
    val stemWidth: Float? = null,
    val stemLength: Float? = null,
    val angle: Float? = null,
    val headEnabled: Boolean? = null,
    val headSize: Float? = null,
    val headColor: Int? = null,
    val headFilled: Boolean? = null,
    val tailEnabled: Boolean? = null,
    val tailSize: Float? = null,
    val tailColor: Int? = null,
    val tailFilled: Boolean? = null,
    val curveBend: Float? = null,
    val arrowColor: Int? = null,

    // ── PenLayer ─────────────────────────────────────────────────────────────
    val anchors: List<AnchorPointDto>? = null,
    val isClosed: Boolean? = null,
    val penFillColor: Int? = null,
    val fillEnabled: Boolean? = null,
    val penStrokeColor: Int? = null,
    val penStrokeWidth: Float? = null
)

/**
 * DTO titik anchor Bézier untuk PenLayer.
 */
data class AnchorPointDto(
    val id: String,
    val x: Float,
    val y: Float,
    val handleInX: Float,
    val handleInY: Float,
    val handleOutX: Float,
    val handleOutY: Float,
    /** "SMOOTH" | "CORNER" */
    val type: String
)
