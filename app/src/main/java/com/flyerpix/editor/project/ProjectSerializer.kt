package com.flyerpix.editor.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.Layout
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.flyerpix.editor.canvas.model.*
import com.flyerpix.editor.project.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * Serializer dan deserializer format proyek PixelLab (.plp).
 *
 * Format .plp adalah file teks JSON yang menyimpan:
 *  - Metadata proyek (nama, waktu buat, waktu update, versi schema).
 *  - Konfigurasi kanvas (lebar, tinggi, latar belakang: Transparan/Solid/Gradasi/Gambar).
 *  - Array seluruh layer kanvas (TextLayer, ImageLayer, ShapeLayer, StickerLayer,
 *    ArrowLayer, PenLayer) beserta semua atributnya.
 *
 * File disimpan di folder privat aplikasi: `context.filesDir/projects/<nama>.plp`.
 *
 * ### Penggunaan
 * ```kotlin
 * val project = canvas.exportProjectSnapshot("My Design")
 * val file = ProjectSerializer.saveProject(context, project, "my_design")
 *
 * val loaded = ProjectSerializer.loadProject(file)
 * canvas.importProjectSnapshot(loaded)
 * ```
 */
object ProjectSerializer {

    // ── Konstanta ──────────────────────────────────────────────────────────────

    const val FILE_EXTENSION = ".plp"
    const val PROJECTS_DIR = "projects"
    private const val SCHEMA_VERSION = 1

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    // ── Public API: File I/O ───────────────────────────────────────────────────

    /**
     * Mengembalikan direktori privat tempat menyimpan proyek.
     * Direktori dibuat otomatis jika belum ada.
     */
    fun getProjectsDirectory(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Menyimpan [project] ke file `.plp` di direktori privat.
     *
     * @param context   Context aplikasi.
     * @param project   Model proyek runtime yang akan disimpan.
     * @param fileName  Nama file tanpa ekstensi (karakter tidak valid akan diganti '_').
     * @return          File `.plp` yang berhasil disimpan.
     * @throws IOException jika gagal menulis file.
     */
    fun saveProject(context: Context, project: ProjectModel, fileName: String): File {
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(getProjectsDirectory(context), "$safeFileName$FILE_EXTENSION")
        val json = serialize(project)
        file.writeText(json, Charsets.UTF_8)
        return file
    }

    /**
     * Memuat proyek dari file `.plp`.
     *
     * @param file  File `.plp` yang akan dibaca.
     * @return      [ProjectModel] hasil deserialisasi.
     * @throws IOException jika file tidak dapat dibaca.
     * @throws IllegalArgumentException jika format file tidak valid.
     */
    fun loadProject(file: File): ProjectModel {
        if (!file.exists()) throw IOException("File tidak ditemukan: ${file.absolutePath}")
        val json = file.readText(Charsets.UTF_8)
        return deserialize(json)
    }

    /**
     * Mengembalikan daftar semua file `.plp` yang tersimpan, diurutkan berdasarkan
     * waktu modifikasi terbaru terlebih dahulu.
     */
    fun listProjects(context: Context): List<File> {
        val dir = getProjectsDirectory(context)
        return dir.listFiles { f -> f.extension == "plp" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Menghapus file proyek `.plp` berdasarkan nama file (tanpa ekstensi).
     *
     * @return true jika berhasil dihapus, false jika file tidak ada.
     */
    fun deleteProject(context: Context, fileName: String): Boolean {
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(getProjectsDirectory(context), "$safeFileName$FILE_EXTENSION")
        return file.exists() && file.delete()
    }

    // ── Public API: Serialization ──────────────────────────────────────────────

    /**
     * Mengkonversi [ProjectModel] ke string JSON format `.plp`.
     */
    fun serialize(project: ProjectModel): String {
        val dto = toDto(project)
        return gson.toJson(dto)
    }

    /**
     * Mengkonversi string JSON `.plp` ke [ProjectModel].
     *
     * @throws IllegalArgumentException jika format JSON tidak valid atau versi schema tidak dikenali.
     */
    fun deserialize(json: String): ProjectModel {
        val dto = try {
            gson.fromJson(json, ProjectDto::class.java)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Format file .plp tidak valid: ${e.message}", e)
        } ?: throw IllegalArgumentException("File .plp kosong atau tidak dapat dibaca.")

        if (dto.schemaVersion > SCHEMA_VERSION) {
            throw IllegalArgumentException(
                "File .plp menggunakan versi schema ${dto.schemaVersion}, " +
                "tetapi aplikasi ini hanya mendukung hingga versi $SCHEMA_VERSION. " +
                "Silakan perbarui aplikasi."
            )
        }
        return fromDto(dto)
    }

    // ── Konversi Model → DTO ───────────────────────────────────────────────────

    private fun toDto(project: ProjectModel): ProjectDto = ProjectDto(
        schemaVersion = SCHEMA_VERSION,
        projectName   = project.projectName,
        createdAt     = project.createdAt,
        updatedAt     = System.currentTimeMillis(),
        canvasWidth   = project.canvasWidth,
        canvasHeight  = project.canvasHeight,
        background    = backgroundToDto(project.background),
        layers        = project.layers.map { layerToDto(it) }
    )

    private fun backgroundToDto(bg: CanvasBackground): CanvasBackgroundDto = CanvasBackgroundDto(
        mode       = bg.mode.name,
        solidColor = bg.solidColor,
        gradient   = bg.gradient?.let { gradientToDto(it) },
        imageBase64 = bg.imageBitmap?.let { bitmapToBase64(it) }
    )

    private fun gradientToDto(g: GradientColor): GradientColorDto = GradientColorDto(
        type      = g.type.name,
        colors    = g.colors.toList(),
        positions = g.positions?.toList(),
        angle     = g.angle
    )

    private fun cornersToString(corners: FloatArray): String =
        corners.joinToString(",") { it.toString() }

    private fun blendModeToString(mode: PorterDuff.Mode): String = mode.name

    private fun alignmentToString(alignment: Layout.Alignment): String = alignment.name

    private fun layerToDto(layer: CanvasLayer): LayerDto = when (layer) {
        is TextLayer    -> textLayerToDto(layer)
        is ImageLayer   -> imageLayerToDto(layer)
        is ShapeLayer   -> shapeLayerToDto(layer)
        is StickerLayer -> stickerLayerToDto(layer)
        is ArrowLayer   -> arrowLayerToDto(layer)
        is PenLayer     -> penLayerToDto(layer)
        else            -> throw IllegalArgumentException(
            "Tipe layer tidak dikenali: ${layer::class.simpleName}"
        )
    }

    private fun baseLayerFields(layer: CanvasLayer): LayerDto = LayerDto(
        type               = "",  // akan dioverride di tiap fungsi spesifik
        id                 = layer.id,
        x                  = layer.x,
        y                  = layer.y,
        scale              = layer.scale,
        rotation           = layer.rotation,
        opacity            = layer.opacity,
        isLocked           = layer.isLocked,
        isVisible          = layer.isVisible,
        blendMode          = blendModeToString(layer.blendMode),
        perspectiveEnabled = layer.perspectiveEnabled,
        perspectiveCorners = cornersToString(layer.perspectiveCorners)
    )

    private fun textLayerToDto(l: TextLayer): LayerDto = baseLayerFields(l).copy(
        type                = "TEXT",
        text                = l.text,
        textSize            = l.textSize,
        textColor           = l.textColor,
        fontName            = l.fontName,
        letterSpacing       = l.letterSpacing,
        lineSpacing         = l.lineSpacing,
        alignment           = alignmentToString(l.alignment),
        isBold              = l.isBold,
        isItalic            = l.isItalic,
        isUnderline         = l.isUnderline,
        isStrikethrough     = l.isStrikethrough,
        justifyEnabled      = l.justifyEnabled,
        paddingTop          = l.paddingTop,
        paddingRight        = l.paddingRight,
        paddingBottom       = l.paddingBottom,
        paddingLeft         = l.paddingLeft,
        bgEnabled           = l.bgEnabled,
        bgColor             = l.bgColor,
        bgOpacity           = l.bgOpacity,
        bgPadding           = l.bgPadding,
        bgCornerRadius      = l.bgCornerRadius,
        reflectionEnabled   = l.reflectionEnabled,
        reflectionOpacity   = l.reflectionOpacity,
        reflectionDistance  = l.reflectionDistance,
        reflectionFade      = l.reflectionFade,
        strokeColor         = l.strokeColor,
        strokeWidth         = l.strokeWidth,
        shadowEnabled       = l.shadowEnabled,
        shadowColor         = l.shadowColor,
        shadowRadius        = l.shadowRadius,
        shadowDx            = l.shadowDx,
        shadowDy            = l.shadowDy,
        shadowOpacity       = l.shadowOpacity,
        innerShadowEnabled  = l.innerShadowEnabled,
        innerShadowColor    = l.innerShadowColor,
        innerShadowRadius   = l.innerShadowRadius,
        innerShadowDx       = l.innerShadowDx,
        innerShadowDy       = l.innerShadowDy,
        innerShadowOpacity  = l.innerShadowOpacity,
        embossEnabled       = l.embossEnabled,
        embossLightAngle    = l.embossLightAngle,
        embossAmbient       = l.embossAmbient,
        embossSpecular      = l.embossSpecular,
        embossBlurRadius    = l.embossBlurRadius,
        gradientEnabled     = l.gradientEnabled,
        textGradient        = l.gradient?.let { gradientToDto(it) },
        textureEnabled      = l.textureEnabled,
        textureScale        = l.textureScale,
        textureRotation     = l.textureRotation,
        textureBase64       = l.textureBitmap?.let { bitmapToBase64(it) },
        extrudeEnabled      = l.extrudeEnabled,
        extrudeDepth        = l.extrudeDepth,
        extrudeColor        = l.extrudeColor,
        extrudeViewType     = l.extrudeViewType.name,
        extrudeAngle        = l.extrudeAngle,
        rotate3DX           = l.rotate3DX,
        rotate3DY           = l.rotate3DY,
        rotate3DZ           = l.rotate3DZ,
        curvePercent        = l.curvePercent
    )

    private fun imageLayerToDto(l: ImageLayer): LayerDto = baseLayerFields(l).copy(
        type         = "IMAGE",
        bitmapBase64 = bitmapToBase64(l.bitmap),
        layerName    = l.layerName
    )

    private fun stickerLayerToDto(l: StickerLayer): LayerDto = baseLayerFields(l).copy(
        type         = "STICKER",
        bitmapBase64 = bitmapToBase64(l.stickerBitmap),
        layerName    = l.stickerName
    )

    private fun shapeLayerToDto(l: ShapeLayer): LayerDto = baseLayerFields(l).copy(
        type                = "SHAPE",
        shapeType           = l.shapeType.name,
        shapeWidth          = l.width,
        shapeHeight         = l.height,
        fillColor           = l.fillColor,
        shapeStrokeColor    = l.strokeColor,
        shapeStrokeWidth    = l.strokeWidth,
        cornerRadiusX       = l.cornerRadiusX,
        cornerRadiusY       = l.cornerRadiusY,
        starPoints          = l.starPoints,
        starInnerRadiusRatio = l.starInnerRadiusRatio
    )

    private fun arrowLayerToDto(l: ArrowLayer): LayerDto = baseLayerFields(l).copy(
        type         = "ARROW",
        arrowStyle   = l.arrowStyle.name,
        stemWidth    = l.stemWidth,
        stemLength   = l.stemLength,
        angle        = l.angle,
        headEnabled  = l.headEnabled,
        headSize     = l.headSize,
        headColor    = l.headColor,
        headFilled   = l.headFilled,
        tailEnabled  = l.tailEnabled,
        tailSize     = l.tailSize,
        tailColor    = l.tailColor,
        tailFilled   = l.tailFilled,
        curveBend    = l.curveBend
    )

    private fun penLayerToDto(l: PenLayer): LayerDto = baseLayerFields(l).copy(
        type           = "PEN",
        anchors        = l.anchors.map { a ->
            AnchorPointDto(
                id          = a.id,
                x           = a.x,
                y           = a.y,
                handleInX   = a.handleInX,
                handleInY   = a.handleInY,
                handleOutX  = a.handleOutX,
                handleOutY  = a.handleOutY,
                type        = a.type.name
            )
        },
        isClosed       = l.isClosed,
        penFillColor   = l.fillColor,
        fillEnabled    = l.fillEnabled,
        penStrokeColor = l.strokeColor,
        penStrokeWidth = l.strokeWidth
    )

    // ── Konversi DTO → Model ───────────────────────────────────────────────────

    private fun fromDto(dto: ProjectDto): ProjectModel = ProjectModel(
        projectName  = dto.projectName,
        createdAt    = dto.createdAt,
        updatedAt    = dto.updatedAt,
        canvasWidth  = dto.canvasWidth.coerceIn(50, 8192),
        canvasHeight = dto.canvasHeight.coerceIn(50, 8192),
        background   = backgroundFromDto(dto.background),
        layers       = dto.layers.mapNotNull { safeLayerFromDto(it) }.toMutableList()
    )

    private fun backgroundFromDto(dto: CanvasBackgroundDto): CanvasBackground {
        val mode = try {
            CanvasBackgroundMode.valueOf(dto.mode)
        } catch (e: IllegalArgumentException) {
            CanvasBackgroundMode.SOLID_COLOR
        }
        return CanvasBackground(
            mode        = mode,
            solidColor  = dto.solidColor,
            gradient    = dto.gradient?.let { gradientFromDto(it) },
            imageBitmap = dto.imageBase64?.let { base64ToBitmap(it) }
        )
    }

    private fun gradientFromDto(dto: GradientColorDto): GradientColor {
        val type = try {
            GradientType.valueOf(dto.type)
        } catch (e: IllegalArgumentException) {
            GradientType.LINEAR
        }
        return GradientColor(
            colors    = dto.colors.toIntArray(),
            positions = dto.positions?.toFloatArray(),
            type      = type,
            angle     = dto.angle
        )
    }

    private fun cornersFromString(csv: String): FloatArray {
        return try {
            csv.split(",").map { it.trim().toFloat() }.toFloatArray().let {
                if (it.size == 8) it
                else floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
            }
        } catch (e: NumberFormatException) {
            floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
        }
    }

    private fun blendModeFromString(name: String): PorterDuff.Mode {
        return try {
            PorterDuff.Mode.valueOf(name)
        } catch (e: IllegalArgumentException) {
            PorterDuff.Mode.SRC_OVER
        }
    }

    private fun alignmentFromString(name: String?): Layout.Alignment {
        return when (name) {
            "ALIGN_OPPOSITE" -> Layout.Alignment.ALIGN_OPPOSITE
            "ALIGN_CENTER"   -> Layout.Alignment.ALIGN_CENTER
            else             -> Layout.Alignment.ALIGN_NORMAL
        }
    }

    /** Membungkus deserialisasi satu layer agar error pada satu layer tidak menghentikan seluruh proyek. */
    private fun safeLayerFromDto(dto: LayerDto): CanvasLayer? = try {
        layerFromDto(dto)
    } catch (e: Exception) {
        null // Layer tidak valid dilewati; proyek tetap dimuat sebagian
    }

    private fun layerFromDto(dto: LayerDto): CanvasLayer {
        val blendMode          = blendModeFromString(dto.blendMode)
        val perspectiveCorners = cornersFromString(dto.perspectiveCorners)

        return when (dto.type) {
            "TEXT" -> TextLayer(
                id                 = dto.id,
                x                  = dto.x,
                y                  = dto.y,
                scale              = dto.scale,
                rotation           = dto.rotation,
                opacity            = dto.opacity,
                isLocked           = dto.isLocked,
                isVisible          = dto.isVisible,
                text               = dto.text ?: "",
                textSize           = dto.textSize ?: 64f,
                textColor          = dto.textColor ?: Color.WHITE,
                fontName           = dto.fontName,
                letterSpacing      = dto.letterSpacing ?: 0f,
                lineSpacing        = dto.lineSpacing ?: 0f,
                alignment          = alignmentFromString(dto.alignment),
                isBold             = dto.isBold ?: false,
                isItalic           = dto.isItalic ?: false,
                isUnderline        = dto.isUnderline ?: false,
                isStrikethrough    = dto.isStrikethrough ?: false,
                justifyEnabled     = dto.justifyEnabled ?: false,
                paddingTop         = dto.paddingTop ?: 0f,
                paddingRight       = dto.paddingRight ?: 0f,
                paddingBottom      = dto.paddingBottom ?: 0f,
                paddingLeft        = dto.paddingLeft ?: 0f,
                bgEnabled          = dto.bgEnabled ?: false,
                bgColor            = dto.bgColor ?: Color.BLACK,
                bgOpacity          = dto.bgOpacity ?: 1f,
                bgPadding          = dto.bgPadding ?: 0f,
                bgCornerRadius     = dto.bgCornerRadius ?: 0f,
                reflectionEnabled  = dto.reflectionEnabled ?: false,
                reflectionOpacity  = dto.reflectionOpacity ?: 0.4f,
                reflectionDistance = dto.reflectionDistance ?: 10f,
                reflectionFade     = dto.reflectionFade ?: 0.5f,
                strokeColor        = dto.strokeColor ?: Color.BLACK,
                strokeWidth        = dto.strokeWidth ?: 0f,
                shadowEnabled      = dto.shadowEnabled ?: false,
                shadowColor        = dto.shadowColor ?: Color.BLACK,
                shadowRadius       = dto.shadowRadius ?: 8f,
                shadowDx           = dto.shadowDx ?: 4f,
                shadowDy           = dto.shadowDy ?: 4f,
                shadowOpacity      = dto.shadowOpacity ?: 0.6f,
                innerShadowEnabled = dto.innerShadowEnabled ?: false,
                innerShadowColor   = dto.innerShadowColor ?: Color.BLACK,
                innerShadowRadius  = dto.innerShadowRadius ?: 6f,
                innerShadowDx      = dto.innerShadowDx ?: 0f,
                innerShadowDy      = dto.innerShadowDy ?: 4f,
                innerShadowOpacity = dto.innerShadowOpacity ?: 0.8f,
                embossEnabled      = dto.embossEnabled ?: false,
                embossLightAngle   = dto.embossLightAngle ?: 45f,
                embossAmbient      = dto.embossAmbient ?: 0.2f,
                embossSpecular     = dto.embossSpecular ?: 8f,
                embossBlurRadius   = dto.embossBlurRadius ?: 3f,
                gradientEnabled    = dto.gradientEnabled ?: false,
                gradient           = dto.textGradient?.let { gradientFromDto(it) },
                textureEnabled     = dto.textureEnabled ?: false,
                textureScale       = dto.textureScale ?: 1f,
                textureRotation    = dto.textureRotation ?: 0f,
                textureBitmap      = dto.textureBase64?.let { base64ToBitmap(it) },
                extrudeEnabled     = dto.extrudeEnabled ?: false,
                extrudeDepth       = dto.extrudeDepth ?: 10,
                extrudeColor       = dto.extrudeColor ?: 0xFF333333.toInt(),
                extrudeViewType    = dto.extrudeViewType?.let {
                    try { ExtrudeViewType.valueOf(it) } catch (e: Exception) { ExtrudeViewType.OBLIQUE }
                } ?: ExtrudeViewType.OBLIQUE,
                extrudeAngle       = dto.extrudeAngle ?: 45f,
                rotate3DX          = dto.rotate3DX ?: 0f,
                rotate3DY          = dto.rotate3DY ?: 0f,
                rotate3DZ          = dto.rotate3DZ ?: 0f,
                curvePercent       = dto.curvePercent ?: 0,
                perspectiveEnabled = dto.perspectiveEnabled,
                perspectiveCorners = perspectiveCorners,
                blendMode          = blendMode
            )

            "IMAGE" -> {
                val bitmap = dto.bitmapBase64?.let { base64ToBitmap(it) }
                    ?: throw IllegalArgumentException("ImageLayer tanpa bitmapBase64")
                ImageLayer(
                    id                 = dto.id,
                    x                  = dto.x,
                    y                  = dto.y,
                    scale              = dto.scale,
                    rotation           = dto.rotation,
                    opacity            = dto.opacity,
                    isLocked           = dto.isLocked,
                    isVisible          = dto.isVisible,
                    perspectiveEnabled = dto.perspectiveEnabled,
                    perspectiveCorners = perspectiveCorners,
                    blendMode          = blendMode,
                    bitmap             = bitmap,
                    layerName          = dto.layerName ?: "Image Layer"
                )
            }

            "STICKER" -> {
                val bitmap = dto.bitmapBase64?.let { base64ToBitmap(it) }
                    ?: throw IllegalArgumentException("StickerLayer tanpa bitmapBase64")
                StickerLayer(
                    id                 = dto.id,
                    x                  = dto.x,
                    y                  = dto.y,
                    scale              = dto.scale,
                    rotation           = dto.rotation,
                    opacity            = dto.opacity,
                    isLocked           = dto.isLocked,
                    isVisible          = dto.isVisible,
                    stickerBitmap      = bitmap,
                    stickerName        = dto.layerName ?: "Sticker",
                    perspectiveEnabled = dto.perspectiveEnabled,
                    perspectiveCorners = perspectiveCorners,
                    blendMode          = blendMode
                )
            }

            "SHAPE" -> ShapeLayer(
                id                  = dto.id,
                x                   = dto.x,
                y                   = dto.y,
                scale               = dto.scale,
                rotation            = dto.rotation,
                opacity             = dto.opacity,
                isLocked            = dto.isLocked,
                isVisible           = dto.isVisible,
                shapeType           = dto.shapeType?.let {
                    try { ShapeType.valueOf(it) } catch (e: Exception) { ShapeType.RECTANGLE }
                } ?: ShapeType.RECTANGLE,
                width               = dto.shapeWidth ?: 200f,
                height              = dto.shapeHeight ?: 200f,
                fillColor           = dto.fillColor ?: Color.WHITE,
                strokeColor         = dto.shapeStrokeColor ?: Color.BLACK,
                strokeWidth         = dto.shapeStrokeWidth ?: 0f,
                cornerRadiusX       = dto.cornerRadiusX ?: 20f,
                cornerRadiusY       = dto.cornerRadiusY ?: 20f,
                starPoints          = dto.starPoints ?: 5,
                starInnerRadiusRatio = dto.starInnerRadiusRatio ?: 0.4f,
                perspectiveEnabled  = dto.perspectiveEnabled,
                perspectiveCorners  = perspectiveCorners,
                blendMode           = blendMode
            )

            "ARROW" -> ArrowLayer(
                id                 = dto.id,
                x                  = dto.x,
                y                  = dto.y,
                scale              = dto.scale,
                rotation           = dto.rotation,
                opacity            = dto.opacity,
                isLocked           = dto.isLocked,
                isVisible          = dto.isVisible,
                arrowStyle         = dto.arrowStyle?.let {
                    try { ArrowStyle.valueOf(it) } catch (e: Exception) { ArrowStyle.STRAIGHT }
                } ?: ArrowStyle.STRAIGHT,
                stemWidth          = dto.stemWidth ?: 6f,
                stemLength         = dto.stemLength ?: 300f,
                angle              = dto.angle ?: 0f,
                headEnabled        = dto.headEnabled ?: true,
                headSize           = dto.headSize ?: 30f,
                headColor          = dto.headColor ?: Color.WHITE,
                headFilled         = dto.headFilled ?: true,
                tailEnabled        = dto.tailEnabled ?: false,
                tailSize           = dto.tailSize ?: 24f,
                tailColor          = dto.tailColor ?: Color.WHITE,
                tailFilled         = dto.tailFilled ?: true,
                curveBend          = dto.curveBend ?: 0f,
                perspectiveEnabled = dto.perspectiveEnabled,
                perspectiveCorners = perspectiveCorners,
                blendMode          = blendMode
            )

            "PEN" -> PenLayer(
                id                 = dto.id,
                x                  = dto.x,
                y                  = dto.y,
                scale              = dto.scale,
                rotation           = dto.rotation,
                opacity            = dto.opacity,
                isLocked           = dto.isLocked,
                isVisible          = dto.isVisible,
                anchors            = (dto.anchors ?: emptyList()).map { a ->
                    AnchorPoint(
                        id          = a.id,
                        x           = a.x,
                        y           = a.y,
                        handleInX   = a.handleInX,
                        handleInY   = a.handleInY,
                        handleOutX  = a.handleOutX,
                        handleOutY  = a.handleOutY,
                        type        = try {
                            AnchorType.valueOf(a.type)
                        } catch (e: Exception) {
                            AnchorType.SMOOTH
                        }
                    )
                }.toMutableList(),
                isClosed           = dto.isClosed ?: false,
                fillColor          = dto.penFillColor ?: Color.TRANSPARENT,
                fillEnabled        = dto.fillEnabled ?: false,
                strokeColor        = dto.penStrokeColor ?: Color.WHITE,
                strokeWidth        = dto.penStrokeWidth ?: 4f,
                perspectiveEnabled = dto.perspectiveEnabled,
                perspectiveCorners = perspectiveCorners,
                blendMode          = blendMode
            )

            else -> throw IllegalArgumentException("Tipe layer tidak dikenali: '${dto.type}'")
        }
    }

    // ── Bitmap Encoding/Decoding ───────────────────────────────────────────────

    /**
     * Mengkodekan [bitmap] ke string Base64 PNG (lossless).
     * Bitmap yang sudah di-recycle akan menghasilkan null.
     */
    internal fun bitmapToBase64(bitmap: Bitmap): String? {
        if (bitmap.isRecycled) return null
        return try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mendekodekan string Base64 PNG menjadi [Bitmap].
     */
    internal fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
