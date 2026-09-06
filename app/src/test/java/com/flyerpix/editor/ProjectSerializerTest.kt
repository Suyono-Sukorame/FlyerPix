package com.flyerpix.editor

import android.graphics.Color
import android.graphics.PorterDuff
import com.google.gson.GsonBuilder
import com.flyerpix.editor.canvas.model.*
import com.flyerpix.editor.project.ProjectModel
import com.flyerpix.editor.project.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Unit test untuk ProjectSerializer menggunakan Gson murni (JVM – tanpa Android runtime).
 *
 * Semua konversi DTO dipelajari langsung dari ProjectDto/LayerDto tanpa memanggil
 * android.util.Base64 atau android.graphics.Bitmap, sehingga dapat dijalankan di JVM headless.
 *
 * Pengujian dilakukan pada lapisan DTO (serialisasi/deserialisasi JSON string) dan lapisan
 * domain (roundtrip ProjectModel → JSON → ProjectModel), termasuk penyimpanan/pembacaan file .plp.
 */
class ProjectSerializerTest {

    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    // ─── Helper DTO builders ────────────────────────────────────────────────────

    private fun makeTextLayerDto(
        id: String = "text-1",
        text: String = "Hello World",
        textSize: Float = 64f,
        textColor: Int = Color.WHITE,
        fontName: String? = "Roboto Bold",
        rotation: Float = 15f,
        strokeColor: Int = Color.BLACK,
        strokeWidth: Float = 4f,
        shadowEnabled: Boolean = true,
        shadowColor: Int = Color.BLACK,
        shadowRadius: Float = 8f,
        shadowDx: Float = 4f,
        shadowDy: Float = 4f,
        shadowOpacity: Float = 0.6f,
        isBold: Boolean = true,
        isItalic: Boolean = false,
        alignment: String = "ALIGN_CENTER"
    ) = LayerDto(
        type          = "TEXT",
        id            = id,
        x             = 100f,
        y             = 200f,
        scale         = 1.5f,
        rotation      = rotation,
        opacity       = 200,
        isLocked      = false,
        isVisible     = true,
        blendMode     = "SRC_OVER",
        perspectiveEnabled = false,
        perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
        text          = text,
        textSize      = textSize,
        textColor     = textColor,
        fontName      = fontName,
        isBold        = isBold,
        isItalic      = isItalic,
        isUnderline   = false,
        alignment     = alignment,
        strokeColor   = strokeColor,
        strokeWidth   = strokeWidth,
        shadowEnabled = shadowEnabled,
        shadowColor   = shadowColor,
        shadowRadius  = shadowRadius,
        shadowDx      = shadowDx,
        shadowDy      = shadowDy,
        shadowOpacity = shadowOpacity,
        letterSpacing = 0f,
        lineSpacing   = 0f,
        extrudeEnabled = false,
        extrudeDepth   = 10,
        extrudeColor   = 0xFF333333.toInt(),
        extrudeViewType = "OBLIQUE",
        extrudeAngle   = 45f,
        rotate3DX      = 0f,
        rotate3DY      = 0f,
        rotate3DZ      = 0f,
        curvePercent   = 0,
        gradientEnabled = false,
        textureEnabled  = false,
        textureScale    = 1f,
        textureRotation = 0f,
        embossEnabled   = false,
        embossLightAngle = 45f,
        embossAmbient   = 0.2f,
        embossSpecular  = 8f,
        embossBlurRadius = 3f,
        innerShadowEnabled = false
    )

    private fun makeShapeLayerDto(
        id: String = "shape-1",
        shapeType: String = "ROUNDED_RECTANGLE",
        width: Float = 300f,
        height: Float = 150f,
        fillColor: Int = Color.RED,
        strokeColor: Int = Color.BLUE,
        strokeWidth: Float = 2f,
        cornerRadiusX: Float = 24f,
        cornerRadiusY: Float = 24f
    ) = LayerDto(
        type             = "SHAPE",
        id               = id,
        x                = 50f,
        y                = 50f,
        scale            = 1f,
        rotation         = 0f,
        opacity          = 255,
        blendMode        = "SRC_OVER",
        perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
        shapeType        = shapeType,
        shapeWidth       = width,
        shapeHeight      = height,
        fillColor        = fillColor,
        shapeStrokeColor = strokeColor,
        shapeStrokeWidth = strokeWidth,
        cornerRadiusX    = cornerRadiusX,
        cornerRadiusY    = cornerRadiusY,
        starPoints       = 5,
        starInnerRadiusRatio = 0.4f
    )

    private fun makeArrowLayerDto(
        id: String = "arrow-1",
        arrowStyle: String = "CURVED",
        stemLength: Float = 400f,
        curveBend: Float = 60f
    ) = LayerDto(
        type         = "ARROW",
        id           = id,
        x            = 200f,
        y            = 300f,
        scale        = 1f,
        rotation     = 30f,
        opacity      = 255,
        blendMode    = "MULTIPLY",
        perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
        arrowStyle   = arrowStyle,
        stemWidth    = 8f,
        stemLength   = stemLength,
        angle        = 0f,
        headEnabled  = true,
        headSize     = 35f,
        headColor    = Color.YELLOW,
        headFilled   = true,
        tailEnabled  = true,
        tailSize     = 20f,
        tailColor    = Color.GREEN,
        tailFilled   = false,
        curveBend    = curveBend
    )

    private fun makePenLayerDto(
        id: String = "pen-1",
        isClosed: Boolean = true
    ) = LayerDto(
        type           = "PEN",
        id             = id,
        x              = 0f,
        y              = 0f,
        scale          = 1f,
        rotation       = 0f,
        opacity        = 255,
        blendMode      = "SRC_OVER",
        perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
        anchors        = listOf(
            AnchorPointDto("a1", 100f, 100f, 80f, 120f, 120f, 80f, "SMOOTH"),
            AnchorPointDto("a2", 300f, 100f, 280f, 80f, 320f, 120f, "CORNER"),
            AnchorPointDto("a3", 200f, 250f, 180f, 250f, 220f, 250f, "SMOOTH")
        ),
        isClosed       = isClosed,
        penFillColor   = Color.CYAN,
        fillEnabled    = true,
        penStrokeColor = Color.MAGENTA,
        penStrokeWidth = 3f
    )

    // ─── Test: CanvasBackgroundDto JSON roundtrip ───────────────────────────────

    @Test
    fun `test background solid color dto roundtrip`() {
        val dto = CanvasBackgroundDto(
            mode       = "SOLID_COLOR",
            solidColor = Color.BLUE,
            gradient   = null,
            imageBase64 = null
        )
        val json  = gson.toJson(dto)
        val back  = gson.fromJson(json, CanvasBackgroundDto::class.java)

        assertEquals("SOLID_COLOR", back.mode)
        assertEquals(Color.BLUE,    back.solidColor)
        assertNull(back.gradient)
        assertNull(back.imageBase64)
    }

    @Test
    fun `test background transparent dto roundtrip`() {
        val dto = CanvasBackgroundDto(mode = "TRANSPARENT", solidColor = -1)
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, CanvasBackgroundDto::class.java)
        assertEquals("TRANSPARENT", back.mode)
    }

    @Test
    fun `test background gradient dto roundtrip`() {
        val gradDto = GradientColorDto(
            type      = "LINEAR",
            colors    = listOf(Color.RED, Color.BLUE, Color.GREEN),
            positions = listOf(0f, 0.5f, 1f),
            angle     = 45f
        )
        val dto = CanvasBackgroundDto(mode = "GRADIENT", solidColor = -1, gradient = gradDto)
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, CanvasBackgroundDto::class.java)

        assertNotNull(back.gradient)
        assertEquals("LINEAR",          back.gradient!!.type)
        assertEquals(3,                 back.gradient!!.colors.size)
        assertEquals(Color.RED,         back.gradient!!.colors[0])
        assertEquals(45f,               back.gradient!!.angle, 0.001f)
        assertEquals(listOf(0f, 0.5f, 1f), back.gradient!!.positions)
    }

    // ─── Test: TextLayer DTO roundtrip ─────────────────────────────────────────

    @Test
    fun `test text layer dto roundtrip preserves all basic text properties`() {
        val dto  = makeTextLayerDto()
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertEquals("TEXT",         back.type)
        assertEquals("text-1",       back.id)
        assertEquals("Hello World",  back.text)
        assertEquals(64f,            back.textSize!!, 0.001f)
        assertEquals(Color.WHITE,    back.textColor!!)
        assertEquals("Roboto Bold",  back.fontName)
        assertEquals(15f,            back.rotation, 0.001f)
        assertEquals(200,            back.opacity)
        assertEquals("ALIGN_CENTER", back.alignment)
        assertEquals(true,           back.isBold)
        assertEquals(false,          back.isItalic)
    }

    @Test
    fun `test text layer dto preserves stroke and shadow properties`() {
        val dto  = makeTextLayerDto(strokeWidth = 6f, shadowEnabled = true, shadowRadius = 12f)
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertEquals(6f,   back.strokeWidth!!, 0.001f)
        assertEquals(true, back.shadowEnabled)
        assertEquals(12f,  back.shadowRadius!!, 0.001f)
        assertEquals(4f,   back.shadowDx!!, 0.001f)
        assertEquals(4f,   back.shadowDy!!, 0.001f)
    }

    @Test
    fun `test text layer dto preserves 3D properties`() {
        val dto = LayerDto(
            type          = "TEXT",
            id            = "t3d",
            blendMode     = "SRC_OVER",
            perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
            text          = "3D Text",
            textSize      = 72f,
            textColor     = Color.RED,
            extrudeEnabled = true,
            extrudeDepth   = 20,
            extrudeColor   = 0xFF555555.toInt(),
            extrudeViewType = "ISOMETRIC",
            extrudeAngle   = 30f,
            rotate3DX      = 15f,
            rotate3DY      = -10f,
            rotate3DZ      = 5f,
            curvePercent   = 50
        )
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertTrue(back.extrudeEnabled!!)
        assertEquals(20,           back.extrudeDepth)
        assertEquals("ISOMETRIC",  back.extrudeViewType)
        assertEquals(30f,          back.extrudeAngle!!, 0.001f)
        assertEquals(15f,          back.rotate3DX!!, 0.001f)
        assertEquals(-10f,         back.rotate3DY!!, 0.001f)
        assertEquals(5f,           back.rotate3DZ!!, 0.001f)
        assertEquals(50,           back.curvePercent)
    }

    @Test
    fun `test text layer dto with gradient fill`() {
        val gradDto = GradientColorDto(
            type      = "RADIAL",
            colors    = listOf(Color.YELLOW, Color.MAGENTA),
            positions = null,
            angle     = 0f
        )
        val dto = LayerDto(
            type           = "TEXT",
            id             = "tg1",
            blendMode      = "SRC_OVER",
            perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
            text           = "Gradient Text",
            textSize       = 80f,
            textColor      = Color.WHITE,
            gradientEnabled = true,
            textGradient   = gradDto
        )
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertTrue(back.gradientEnabled!!)
        assertNotNull(back.textGradient)
        assertEquals("RADIAL",    back.textGradient!!.type)
        assertEquals(2,           back.textGradient!!.colors.size)
        assertEquals(Color.YELLOW, back.textGradient!!.colors[0])
    }

    // ─── Test: ShapeLayer DTO roundtrip ────────────────────────────────────────

    @Test
    fun `test shape layer rounded rectangle dto roundtrip`() {
        val dto  = makeShapeLayerDto()
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertEquals("SHAPE",              back.type)
        assertEquals("ROUNDED_RECTANGLE",  back.shapeType)
        assertEquals(300f,                 back.shapeWidth!!, 0.001f)
        assertEquals(150f,                 back.shapeHeight!!, 0.001f)
        assertEquals(Color.RED,            back.fillColor)
        assertEquals(Color.BLUE,           back.shapeStrokeColor)
        assertEquals(2f,                   back.shapeStrokeWidth!!, 0.001f)
        assertEquals(24f,                  back.cornerRadiusX!!, 0.001f)
    }

    @Test
    fun `test shape layer star dto roundtrip`() {
        val dto = LayerDto(
            type                = "SHAPE",
            id                  = "star-1",
            blendMode           = "SRC_OVER",
            perspectiveCorners  = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
            shapeType           = "STAR",
            shapeWidth          = 200f,
            shapeHeight         = 200f,
            fillColor           = Color.YELLOW,
            shapeStrokeColor    = Color.BLACK,
            shapeStrokeWidth    = 0f,
            starPoints          = 8,
            starInnerRadiusRatio = 0.35f
        )
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertEquals("STAR",   back.shapeType)
        assertEquals(8,        back.starPoints)
        assertEquals(0.35f,    back.starInnerRadiusRatio!!, 0.001f)
    }

    // ─── Test: ArrowLayer DTO roundtrip ────────────────────────────────────────

    @Test
    fun `test arrow layer curved dto roundtrip`() {
        val dto  = makeArrowLayerDto()
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertEquals("ARROW",   back.type)
        assertEquals("CURVED",  back.arrowStyle)
        assertEquals(400f,      back.stemLength!!, 0.001f)
        assertEquals(60f,       back.curveBend!!, 0.001f)
        assertEquals(true,      back.headEnabled)
        assertEquals(35f,       back.headSize!!, 0.001f)
        assertEquals(true,      back.tailEnabled)
        assertEquals("MULTIPLY", back.blendMode)
    }

    @Test
    fun `test arrow layer straight dto roundtrip`() {
        val dto  = makeArrowLayerDto(arrowStyle = "STRAIGHT", curveBend = 0f)
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertEquals("STRAIGHT", back.arrowStyle)
        assertEquals(0f, back.curveBend!!, 0.001f)
    }

    // ─── Test: PenLayer DTO roundtrip ──────────────────────────────────────────

    @Test
    fun `test pen layer dto roundtrip preserves all anchors`() {
        val dto  = makePenLayerDto()
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertEquals("PEN",         back.type)
        assertTrue(back.isClosed!!)
        assertNotNull(back.anchors)
        assertEquals(3,             back.anchors!!.size)

        val a1 = back.anchors!![0]
        assertEquals("a1",          a1.id)
        assertEquals(100f,          a1.x, 0.001f)
        assertEquals(100f,          a1.y, 0.001f)
        assertEquals(80f,           a1.handleInX, 0.001f)
        assertEquals("SMOOTH",      a1.type)

        val a2 = back.anchors!![1]
        assertEquals("CORNER",      a2.type)
    }

    @Test
    fun `test open pen layer dto roundtrip`() {
        val dto  = makePenLayerDto(isClosed = false)
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertFalse(back.isClosed!!)
        assertEquals(Color.CYAN,    back.penFillColor)
        assertEquals(Color.MAGENTA, back.penStrokeColor)
        assertEquals(3f,            back.penStrokeWidth!!, 0.001f)
    }

    // ─── Test: ProjectDto JSON roundtrip ───────────────────────────────────────

    @Test
    fun `test project dto with multiple layers roundtrip`() {
        val dto = ProjectDto(
            schemaVersion = 1,
            projectName   = "Test Project",
            createdAt     = 1000L,
            updatedAt     = 2000L,
            canvasWidth   = 1920,
            canvasHeight  = 1080,
            background    = CanvasBackgroundDto(mode = "SOLID_COLOR", solidColor = Color.WHITE),
            layers        = listOf(
                makeTextLayerDto(id = "t1"),
                makeShapeLayerDto(id = "s1"),
                makeArrowLayerDto(id = "a1"),
                makePenLayerDto(id = "p1")
            )
        )

        val json = gson.toJson(dto)
        val back = gson.fromJson(json, ProjectDto::class.java)

        assertEquals(1,              back.schemaVersion)
        assertEquals("Test Project", back.projectName)
        assertEquals(1000L,          back.createdAt)
        assertEquals(1920,           back.canvasWidth)
        assertEquals(1080,           back.canvasHeight)
        assertEquals(4,              back.layers.size)
        assertEquals("TEXT",         back.layers[0].type)
        assertEquals("SHAPE",        back.layers[1].type)
        assertEquals("ARROW",        back.layers[2].type)
        assertEquals("PEN",          back.layers[3].type)
    }

    @Test
    fun `test project dto canvas dimensions roundtrip`() {
        val dto  = ProjectDto(canvasWidth = 2160, canvasHeight = 3840)
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, ProjectDto::class.java)

        assertEquals(2160, back.canvasWidth)
        assertEquals(3840, back.canvasHeight)
    }

    // ─── Test: Perspective corners serialization ────────────────────────────────

    @Test
    fun `test perspective corners csv format roundtrip`() {
        val corners = floatArrayOf(-0.15f, -0.1f, 1.15f, -0.1f, 1f, 1f, 0f, 1f)
        val csv     = corners.joinToString(",") { it.toString() }

        val dto  = LayerDto(type = "SHAPE", id = "sp", blendMode = "SRC_OVER",
            perspectiveEnabled = true, perspectiveCorners = csv,
            shapeType = "RECTANGLE", shapeWidth = 100f, shapeHeight = 100f)
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)

        assertTrue(back.perspectiveEnabled)
        val parts = back.perspectiveCorners.split(",").map { it.trim().toFloat() }
        assertEquals(8, parts.size)
        assertEquals(-0.15f, parts[0], 0.001f)
        assertEquals(-0.1f,  parts[1], 0.001f)
    }

    // ─── Test: blendMode serialization ─────────────────────────────────────────

    @Test
    fun `test blend mode string roundtrip for known modes`() {
        val modes = listOf("SRC_OVER", "MULTIPLY", "SCREEN", "OVERLAY", "DARKEN", "LIGHTEN", "ADD")
        for (mode in modes) {
            val dto  = LayerDto(type = "SHAPE", id = "bm", blendMode = mode,
                perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0",
                shapeType = "CIRCLE", shapeWidth = 100f, shapeHeight = 100f)
            val json = gson.toJson(dto)
            val back = gson.fromJson(json, LayerDto::class.java)
            assertEquals("blendMode '$mode' tidak tepat", mode, back.blendMode)
        }
    }

    // ─── Test: File I/O .plp ───────────────────────────────────────────────────

    @Test
    fun `test project serializes to valid json string`() {
        val dto  = ProjectDto(
            projectName  = "Save Test",
            canvasWidth  = 1080,
            canvasHeight = 1080,
            layers       = listOf(makeTextLayerDto())
        )
        val json = gson.toJson(dto)

        assertTrue(json.contains("\"projectName\""))
        assertTrue(json.contains("\"schemaVersion\""))
        assertTrue(json.contains("\"canvasWidth\""))
        assertTrue(json.contains("\"Save Test\""))
        assertTrue(json.contains("\"TEXT\""))
        assertTrue(json.contains("\"Hello World\""))
    }

    @Test
    fun `test save and load plp file to temp directory`() {
        val tmpDir = createTempDir()
        try {
            val dto = ProjectDto(
                projectName   = "File Test",
                canvasWidth   = 1280,
                canvasHeight  = 720,
                background    = CanvasBackgroundDto(mode = "TRANSPARENT"),
                layers        = listOf(
                    makeTextLayerDto(id = "f1", text = "Saved Layer"),
                    makeShapeLayerDto(id = "f2")
                )
            )

            // Tulis ke file .plp
            val json = gson.toJson(dto)
            val plpFile = File(tmpDir, "file_test.plp")
            plpFile.writeText(json, Charsets.UTF_8)

            // Baca kembali
            val loaded = gson.fromJson(plpFile.readText(Charsets.UTF_8), ProjectDto::class.java)

            assertEquals("File Test",    loaded.projectName)
            assertEquals(1280,           loaded.canvasWidth)
            assertEquals(720,            loaded.canvasHeight)
            assertEquals("TRANSPARENT",  loaded.background.mode)
            assertEquals(2,              loaded.layers.size)
            assertEquals("Saved Layer",  loaded.layers[0].text)
            assertEquals("SHAPE",        loaded.layers[1].type)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `test empty layers list serializes and deserializes correctly`() {
        val dto  = ProjectDto(layers = emptyList())
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, ProjectDto::class.java)
        assertEquals(0, back.layers.size)
    }

    @Test
    fun `test gradient background multi-stop roundtrip`() {
        val gradDto = GradientColorDto(
            type      = "SWEEP",
            colors    = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW),
            positions = listOf(0f, 0.33f, 0.66f, 1f),
            angle     = 90f
        )
        val bgDto = CanvasBackgroundDto(
            mode     = "GRADIENT",
            solidColor = -1,
            gradient = gradDto
        )
        val json = gson.toJson(bgDto)
        val back = gson.fromJson(json, CanvasBackgroundDto::class.java)

        assertNotNull(back.gradient)
        assertEquals("SWEEP",   back.gradient!!.type)
        assertEquals(4,         back.gradient!!.colors.size)
        assertEquals(Color.GREEN, back.gradient!!.colors[1])
        assertEquals(0.33f,     back.gradient!!.positions!![1], 0.001f)
        assertEquals(90f,       back.gradient!!.angle, 0.001f)
    }

    @Test
    fun `test schema version is written to json`() {
        val dto  = ProjectDto(schemaVersion = 1)
        val json = gson.toJson(dto)
        assertTrue(json.contains("\"schemaVersion\": 1"))
    }

    @Test
    fun `test layer with null optional fields is valid`() {
        // Hanya field wajib (type) yang diisi; semua optional null
        val dto  = LayerDto(type = "TEXT", id = "min", blendMode = "SRC_OVER",
            perspectiveCorners = "0.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0")
        val json = gson.toJson(dto)
        val back = gson.fromJson(json, LayerDto::class.java)
        assertEquals("TEXT", back.type)
        assertNull(back.text)
        assertNull(back.fontName)
    }
}
