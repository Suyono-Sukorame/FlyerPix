package com.flyerpix.editor.canvas.model

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.EmbossMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Representasi layer teks kaya (Rich Text) pada kanvas PixelLab.
 *
 * Mendukung pipeline render multi-pass:
 *  1. Stroke / Outline     — [strokeColor], [strokeWidth]
 *  2. Drop Shadow          — via [Paint.setShadowLayer] ([shadowEnabled])
 *  3. Text Fill            — [textColor], [GradientColor], atau Texture Masking ([BitmapShader])
 *  4. Inner Shadow overlay — two-bitmap PorterDuff technique ([innerShadowEnabled])
 *  5. Neon / Glow          — blur mask offscreen ([neonEnabled]) (menggantikan Fill)
 *  6. Emboss / Bevel       — [EmbossMaskFilter] via offscreen software bitmap ([embossEnabled])
 *     (ketika aktif, Emboss **menggantikan** pass Fill; Drop Shadow & Inner Shadow tidak diterapkan)
 *  7. Curved / Arc Text    — [curvePercent] ≠ 0 → drawTextOnPath ([Path.addArc])
 */
data class TextLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    var text: String = "New Text",
    var richTextSpans: MutableList<RichTextSpan> = mutableListOf(),
    var textSize: Float = 64f,
    var textColor: Int = Color.WHITE,
    var typeface: Typeface? = null,
    /** Nama font yang dipilih dari [com.flyerpix.editor.font.FontManager] (misal: "Roboto Bold").
     *  Digunakan untuk serialisasi proyek .plp agar font dapat direkonstruksi saat membuka kembali proyek. */
    var fontName: String? = null,
    var letterSpacing: Float = 0f,
    var lineSpacing: Float = 0f,
    var alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var justifyEnabled: Boolean = false,
    var wrapTextEnabled: Boolean = false,
    var wrapWidth: Float = 0f,
    // ── Padding Bounding Box ──────────────────────────────────────────────
    var paddingTop: Float = 0f,
    var paddingBottom: Float = 0f,
    var paddingLeft: Float = 0f,
    var paddingRight: Float = 0f,
    // ── Text Background ────────────────────────────────────────────────────
    override var bgEnabled: Boolean = false,
    override var bgColor: Int = Color.BLACK,
    override var bgOpacity: Float = 1f,
    override var bgPadding: Float = 0f,
    override var bgCornerRadius: Float = 0f,
    // ── Reflection ─────────────────────────────────────────────────────────
    override var reflectionEnabled: Boolean = false,
    override var reflectionOpacity: Float = 0.4f,
    override var reflectionDistance: Float = 10f,
    override var reflectionFade: Float = 0.5f,
    // ── Stroke / Outline ─────────────────────────────────────────────────────
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 0f,
    // ── Drop Shadow ──────────────────────────────────────────────────────────
    override var shadowEnabled: Boolean = false,
    override var shadowColor: Int = Color.BLACK,
    override var shadowRadius: Float = 8f,
    override var shadowDx: Float = 4f,
    override var shadowDy: Float = 4f,
    override var shadowOpacity: Float = 0.6f,
    // ── Inner Shadow ─────────────────────────────────────────────────────────
    override var innerShadowEnabled: Boolean = false,
    override var innerShadowColor: Int = Color.BLACK,
    override var innerShadowRadius: Float = 6f,
    override var innerShadowDx: Float = 0f,
    override var innerShadowDy: Float = 4f,
    override var innerShadowOpacity: Float = 0.8f,
    // ── Emboss / Bevel ───────────────────────────────────────────────────────
    override var embossEnabled: Boolean = false,
    override var embossLightAngle: Float = 45f,   // derajat 0–360 (arah cahaya)
    override var embossAmbient: Float = 0.2f,     // 0.0–1.0  (cahaya ambient)
    override var embossSpecular: Float = 8f,      // 0–20     (kilap specular / bevel)
    override var embossIntensity: Float = 1f,     // 0–2.5    (penguat kontras cahaya: highlight & bayangan)
    override var embossBevel: Float = 3f,         // 0.5–12   (ketebalan/lebar relief bevel)
    // ── Gradient Fill ────────────────────────────────────────────────────────
    override var gradientEnabled: Boolean = false,
    override var gradient: GradientColor? = null,
    // ── Texture Masking ──────────────────────────────────────────────────────
    override var textureBitmap: Bitmap? = null,
    override var textureEnabled: Boolean = false,
    override var textureScale: Float = 1.0f,
    override var textureRotation: Float = 0f,
    // ── 3D Extrusion ─────────────────────────────────────────────────────────
    override var extrudeEnabled: Boolean = false,
    override var extrudeDepth: Int = 10,                 // 1 s/d 50
    override var extrudeColor: Int = 0xFF333333.toInt(), // Warna sisi kedalaman 3D
    override var extrudeGradient: GradientColor? = null, // Gradasi sisi kedalaman 3D (menggantikan warna solid)
    override var extrudeViewType: ExtrudeViewType = ExtrudeViewType.OBLIQUE,
    override var extrudeAngle: Float = 45f,              // 0° - 360° arah kedalaman
    // ── 3D Shadow ────────────────────────────────────────────────────────────
    override var shadow3DEnabled: Boolean = false,
    override var shadow3DDepth: Int = 12,                // 1 s/d 50 (ketebalan bayangan)
    override var shadow3DColor: Int = 0xB3000000.toInt(),// Warna bayangan 3D (ARGB, default hitam semi-transparan)
    override var shadow3DViewType: ExtrudeViewType = ExtrudeViewType.OBLIQUE,
    override var shadow3DAngle: Float = 45f,             // 0° - 360° arah bayangan
    override var shadow3DBlur: Float = 0f,               // 0–40 (kelembutan ujung bayangan)
    override var shadow3DOpacity: Float = 0.6f,          // 0.0–1.0 (kegelapan bayangan)
    // ── 3D Rotate (Rotasi Sumbu X dan Y) ──────────────────────────────────
    override var rotate3DX: Float = 0f,                  // Kemiringan atas-bawah (-180° s/d 180°)
    override var rotate3DY: Float = 0f,                  // Kemiringan kiri-kanan (-180° s/d 180°)
    override var rotate3DZ: Float = 0f,                  // Rotasi 3D sumbu Z (-180° s/d 180°)
    // ── Neon / Glow ────────────────────────────────────────────────────────
    override var neonEnabled: Boolean = false,
    override var neonColor: Int = 0xFF00E5FF.toInt(),    // Warna cahaya neon
    override var neonRadius: Float = 12f,                // 1–40     (sebaran/blur lingkaran cahaya)
    override var neonIntensity: Float = 1f,              // 0.1–2    (kekuatan/opacity cahaya)
    override var neonCoreEnabled: Boolean = true,        // true=isi teks terang; false=hollow neon
    // ── Curved / Arc Text ────────────────────────────────────────────────────
    override var curvePercent: Int = 0,                  // -100 (bawah) s/d +100 (atas), 0 = lurus
    // ── Perspective Warping ──────────────────────────────────────────────────
    override var perspectiveEnabled: Boolean = false,
    override var perspectiveCorners: FloatArray = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        0f, 1f
    ),
    // ── Blending Mode ────────────────────────────────────────────────────────
    override var blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC_OVER
) : CanvasLayer(id, x, y, scale, rotation, opacity, isLocked, isVisible, perspectiveEnabled, perspectiveCorners, blendMode) {

    @Transient
    private var cachedTextPaint: TextPaint? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Paint helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Menghitung vektor arah geseran per pixel untuk efek 3D Extrusion.
     */
    fun getExtrudeVector(): Pair<Float, Float> {
        return if (extrudeViewType == ExtrudeViewType.ISOMETRIC) {
            val rad = Math.toRadians(30.0)
            Pair(cos(rad).toFloat(), (sin(rad) * 0.58).toFloat())
        } else {
            val rad = Math.toRadians(extrudeAngle.toDouble())
            Pair(cos(rad).toFloat(), sin(rad).toFloat())
        }
    }

    /**
     * Menghitung vektor arah geseran per pixel untuk efek 3D Shadow.
     * Mengikuti pola [getExtrudeVector]: OBLIQUE memakai [shadow3DAngle],
     * ISOMETRIC memakai proyeksi tetap 30°.
     */
    fun getShadow3DVector(): Pair<Float, Float> {
        return if (shadow3DViewType == ExtrudeViewType.ISOMETRIC) {
            val rad = Math.toRadians(30.0)
            Pair(cos(rad).toFloat(), (sin(rad) * 0.58).toFloat())
        } else {
            val rad = Math.toRadians(shadow3DAngle.toDouble())
            Pair(cos(rad).toFloat(), sin(rad).toFloat())
        }
    }

    /**
     * Menghasilkan [BitmapShader] dari [textureBitmap] dengan transformasi skala & rotasi.
     */
    fun createTextureShader(): Shader? {
        val bmp = textureBitmap ?: return null
        if (bmp.isRecycled) return null

        val matrix = Matrix().apply {
            val s = if (textureScale <= 0f) 0.1f else textureScale
            postScale(s, s)
            val bw = bmp.width * s
            val bh = bmp.height * s
            postRotate(textureRotation, bw / 2f, bh / 2f)
        }

        return BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            setLocalMatrix(matrix)
        }
    }

    /**
     * Mengonfigurasi TextPaint untuk satu pass render.
     *
     * @param style  [Paint.Style.FILL] untuk isi teks, [Paint.Style.STROKE] untuk outline.
     * @param color  Warna untuk pass ini.
     */
    private fun obtainTextPaint(
        style: Paint.Style = Paint.Style.FILL,
        color: Int = textColor
    ): TextPaint {
        val paint = cachedTextPaint
            ?: TextPaint(Paint.ANTI_ALIAS_FLAG).also { cachedTextPaint = it }
        paint.textSize = textSize
        paint.color = color
        paint.alpha = opacity.coerceIn(0, 255)
        paint.letterSpacing = letterSpacing
        paint.isUnderlineText = isUnderline
        paint.isStrikeThruText = isStrikethrough
        paint.style = style
        paint.shader = null

        when (style) {
            Paint.Style.STROKE -> {
                paint.strokeWidth = strokeWidth
                paint.strokeJoin = Paint.Join.ROUND
                paint.strokeCap = Paint.Cap.ROUND
                paint.clearShadowLayer()            // stroke tidak mendapat shadow
            }
            else -> {
                // Drop shadow diterapkan hanya pada pass FILL
                if (shadowEnabled && shadowRadius > 0f) {
                    val a = (shadowOpacity.coerceIn(0f, 1f) * 255).toInt()
                    paint.setShadowLayer(shadowRadius, shadowDx, shadowDy,
                        (shadowColor and 0x00FFFFFF) or (a shl 24))
                } else {
                    paint.clearShadowLayer()
                }
            }
        }

        var ts = Typeface.NORMAL
        if (isBold && isItalic) ts = Typeface.BOLD_ITALIC
        else if (isBold)        ts = Typeface.BOLD
        else if (isItalic)      ts = Typeface.ITALIC
        paint.typeface = Typeface.create(typeface ?: Typeface.DEFAULT, ts)
        return paint
    }

    /**
     * Membuat TextPaint "mask" — crisp, solid black, tanpa shadow/blur.
     * Digunakan untuk offscreen bitmap pada algoritma Inner Shadow.
     */
    private fun buildMaskPaint(): TextPaint {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = textSize
        p.color = Color.BLACK
        p.style = Paint.Style.FILL
        p.letterSpacing = letterSpacing
        p.isStrikeThruText = isStrikethrough
        var ts = Typeface.NORMAL
        if (isBold && isItalic) ts = Typeface.BOLD_ITALIC
        else if (isBold)        ts = Typeface.BOLD
        else if (isItalic)      ts = Typeface.ITALIC
        p.typeface = Typeface.create(typeface ?: Typeface.DEFAULT, ts)
        return p
    }

    private fun styledText(paint: TextPaint): CharSequence {
        val content = if (text.isEmpty()) " " else text
        if (richTextSpans.isEmpty() || paint.style != Paint.Style.FILL) return content

        val styled = SpannableString(content)
        richTextSpans.forEach { span ->
            val start = span.start.coerceIn(0, content.length)
            val end = span.end.coerceIn(start, content.length)
            if (end <= start) return@forEach
            if (paint.color == textColor) {
                span.color?.let { styled.setSpan(ForegroundColorSpan(it), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            }
            span.textSize?.let { styled.setSpan(AbsoluteSizeSpan(it.toInt().coerceAtLeast(1), false), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            if (span.isBold && span.isItalic) styled.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            else if (span.isBold) styled.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            else if (span.isItalic) styled.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (span.isUnderline) styled.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (span.isStrikethrough) styled.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return styled
    }

    private fun createLayout(paint: TextPaint): StaticLayout {
        val content = styledText(paint)
        val lines = content.split("\n")
        val useWrap = wrapTextEnabled && wrapWidth > 0f
        val layoutWidth = if (useWrap) {
            max(1, wrapWidth.roundToInt())
        } else {
            var maxLineWidth = 0f
            for (line in lines) {
                val width = paint.measureText(line.toString())
                if (width > maxLineWidth) maxLineWidth = width
            }
            max(1, ceil(maxLineWidth).toInt() + 4)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = StaticLayout.Builder.obtain(content, 0, content.length, paint, layoutWidth)
                .setAlignment(alignment)
                .setLineSpacing(lineSpacing, 1.0f)
                .setIncludePad(true)
            if (useWrap) {
                builder.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                builder.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            }
            if (justifyEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
            }
            builder.build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(content, paint, layoutWidth, alignment, 1.0f, lineSpacing, true)
        }
    }

    /**
     * Mengukur lebar alami teks (baris terpanjang) tanpa pembungkusan.
     * Dipakai saat wrap baru diaktifkan supaya teks langsung membungkus
     * pada lebar isinya saat ini.
     */
    fun measureNaturalWidth(): Float {
        val paint = obtainTextPaint(Paint.Style.FILL, textColor)
        val lines = text.split("\n")
        var maxLineWidth = 0f
        for (line in lines) {
            val width = paint.measureText(line.trimEnd())
            if (width > maxLineWidth) maxLineWidth = width
        }
        return maxLineWidth.coerceAtLeast(1f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render pipeline
    // ─────────────────────────────────────────────────────────────────────────

    override fun draw(canvas: Canvas, paint: Paint) {
        val fillPaint = obtainTextPaint(Paint.Style.FILL, textColor)
        val layout    = createLayout(fillPaint)
        val w = layout.width.toFloat()
        val h = layout.height.toFloat()

        // Padding bounding box (mengelilingi teks tanpa mengubah posisi huruf)
        val padL = paddingLeft.coerceAtLeast(0f)
        val padT = paddingTop.coerceAtLeast(0f)
        val padR = paddingRight.coerceAtLeast(0f)
        val padB = paddingBottom.coerceAtLeast(0f)
        val pw = w + padL + padR
        val ph = h + padT + padB
        // Sumbu putar: pusat kotak padding (rigid, konsisten dgn getBounds/handle)
        val cx = pw / 2f
        val cy = ph / 2f

        // ── Pasang Shader: Prioritas Tekstur > Gradasi > Solid Color ───────────
        if (textureEnabled && textureBitmap != null && !textureBitmap!!.isRecycled) {
            fillPaint.shader = createTextureShader()
        } else if (gradientEnabled && gradient != null) {
            fillPaint.shader = gradient?.createShader(w, h)
        } else {
            fillPaint.shader = null
        }

        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale, cx, cy)
        canvas.rotate(rotation, cx, cy)

        // ── 3D Rotate (Rotasi Sumbu X dan Y via Camera) ──────────────────────
        if (rotate3DX != 0f || rotate3DY != 0f || rotate3DZ != 0f) {
            val camera = Camera()
            val matrix3D = Matrix()
            camera.save()
            camera.rotateX(rotate3DX)
            camera.rotateY(rotate3DY)
            if (rotate3DZ != 0f) camera.rotateZ(rotate3DZ)
            camera.getMatrix(matrix3D)
            camera.restore()

            matrix3D.preTranslate(-cx, -cy)
            matrix3D.postTranslate(cx, cy)

            canvas.concat(matrix3D)
        }

        // ── Perspective Warping (Matrix.setPolyToPoly) ─────────────────────────
        if (perspectiveEnabled) {
            getPerspectiveMatrix(pw, ph)?.let { pMatrix ->
                canvas.concat(pMatrix)
            }
        }

        // ── Background: persegi membulat di belakang teks, mengikuti kotak padding ──
        if (bgEnabled) {
            drawBackground(canvas, pw, ph)
        }

        // ── Isi teks & efek: digeser ke dalam kotak padding ────────────────────
        canvas.save()
        canvas.translate(padL, padT)

        // ── Pass 0: 3D Shadow (Bayangan Dalam/3D) ────────────────────────────
        if (shadow3DEnabled && shadow3DDepth > 0) {
            draw3DShadow(canvas, fillPaint)
        }

        // ── Pass 1: 3D Text Extrusion (Volume Lapisan Kedalaman 3D) ───────────
        if (extrudeEnabled && extrudeDepth > 0) {
            draw3DExtrusion(canvas, fillPaint)
        }

        // ── Pass 1: Stroke / Outline ──────────────────────────────────────────
        if (strokeWidth > 0f) {
            if (curvePercent != 0) {
                drawCurvedText(canvas, obtainTextPaint(Paint.Style.STROKE, strokeColor))
            } else {
                createLayout(obtainTextPaint(Paint.Style.STROKE, strokeColor)).draw(canvas)
            }
        }

        // ── Pass 2 & 3: Fill / Inner Shadow / Emboss ─────────────────────────────
        if (curvePercent != 0) {
            // Mode Curved Text: gunakan drawTextOnPath sebagai ganti StaticLayout
            drawCurvedText(canvas, fillPaint)
        } else {
            when {
                neonEnabled        -> drawNeonEffect(canvas, layout)
                embossEnabled      -> drawEmbossEffect(canvas, layout)
                innerShadowEnabled -> drawFillWithInnerShadow(canvas, layout)
                else               -> layout.draw(canvas)
            }
        }

        // ── Reflection: pantulan vertikal teks di bawah ───────────────────────
        if (reflectionEnabled) {
            drawReflection(canvas, layout, fillPaint, h)
        }

        canvas.restore()
        canvas.restore()
    }

    /** Menggambar latar belakang persegi membulat di belakang teks. */
    private fun drawBackground(canvas: Canvas, pw: Float, ph: Float) {
        val radius = bgCornerRadius.coerceAtLeast(0f)
        val pPad = bgPadding.coerceAtLeast(0f)
        val rect = RectF(-pPad, -pPad, pw + pPad, ph + pPad)
        val alpha = (bgOpacity.coerceIn(0f, 1f) * 255).toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            this.alpha = alpha
        }
        canvas.drawRoundRect(rect, radius, radius, p)
    }

    /** Menggambar refleksi/pantulan teks yang dicerminkan vertikal di bawah teks. */
    private fun drawReflection(canvas: Canvas, layout: StaticLayout, fillPaint: TextPaint, textH: Float) {
        val lw = layout.width
        val lh = layout.height
        val pad = 8
        val bw = lw + pad * 2
        val bh = lh + pad * 2

        // Bitmap teks solid (bersih dari shadow, agar pantulan tajam)
        val reflectPaint = TextPaint(fillPaint).apply {
            clearShadowLayer()
            style = Paint.Style.FILL
        }
        val textBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        Canvas(textBmp).run {
            translate(pad.toFloat(), pad.toFloat())
            createLayout(reflectPaint).draw(this)
        }

        // Balik vertikal
        val flip = Matrix().apply { preScale(1f, -1f) }
        val refBmp = Bitmap.createBitmap(textBmp, 0, 0, bw, bh, flip, true)
        textBmp.recycle()

        val alpha = (reflectionOpacity.coerceIn(0f, 1f) * 255).toInt()
        val top = textH + reflectionDistance.coerceAtLeast(0f) - pad
        val bottom = top + bh

        val sc = canvas.saveLayer(-pad.toFloat(), textH, lw + pad.toFloat(), bottom, null)
        canvas.drawBitmap(
            refBmp,
            -pad.toFloat(),
            top.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
        )

        if (reflectionFade > 0f) {
            val fadePct = reflectionFade.coerceIn(0f, 1f)
            val grad = android.graphics.LinearGradient(
                0f, top.toFloat(), 0f, bottom,
                Color.argb(alpha, 255, 255, 255),
                Color.argb((alpha * (1f - fadePct)).toInt(), 255, 255, 255),
                android.graphics.Shader.TileMode.CLAMP
            )
            val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = grad
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
            canvas.drawRect(0f, top.toFloat(), lw.toFloat(), bottom, gp)
        }
        canvas.restoreToCount(sc)
        refBmp.recycle()
    }

    /**
     * Menggambar teks melengkung menggunakan [Path.addArc] + [Canvas.drawTextOnPath].
     *
     * **Algoritma:**
     * - Radius lingkaran dihitung dari lebar teks dan `|curvePercent|`.
     *   Semakin kecil `|curvePercent|`, semakin besar radius → kurva lebih landai.
     * - Nilai positif → teks melengkung **ke atas** (arc atas lingkaran).
     * - Nilai negatif → teks melengkung **ke bawah** (arc bawah lingkaran).
     * - `hOffset` menggeser teks di sepanjang path agar terpusat di arc.
     *
     * @param canvas Canvas target
     * @param paint  TextPaint yang sudah dikonfigurasi (fill / stroke / gradient / texture)
     */
    private fun drawCurvedText(canvas: Canvas, paint: TextPaint) {
        val content = if (text.isEmpty()) " " else text
        val pct = curvePercent.coerceIn(-100, 100)
        if (pct == 0) return

        // Ukur lebar string aktual
        val measuredWidth = paint.measureText(content)

        // Radius arc: |curvePercent|=100 → radius = setengah lebar teks (kurva ketat)
        //             |curvePercent|=1   → radius sangat besar (hampir lurus)
        val absRatio = abs(pct) / 100f          // 0.01 – 1.0
        val radius   = measuredWidth / (2f * absRatio).coerceAtLeast(0.01f)

        // Arc: mulai & sweep angle
        // Untuk kurva ke atas (pct > 0): arc bagian atas (180° → sweep ke kiri)
        // Untuk kurva ke bawah (pct < 0): arc bagian bawah (0° → sweep ke kanan)
        val sweepAngle = (measuredWidth / radius) * (180f / Math.PI.toFloat()) * absRatio
        val cx = measuredWidth / 2f

        val path = Path()
        if (pct > 0) {
            // Melengkung ke atas: pusat lingkaran di BAWAH baseline teks
            val cy = radius
            val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val startAngle = 180f + (180f - sweepAngle) / 2f
            path.addArc(oval, startAngle, sweepAngle)
        } else {
            // Melengkung ke bawah: pusat lingkaran di ATAS baseline teks
            val cy = -radius + paint.textSize
            val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val startAngle = -(180f - sweepAngle) / 2f
            path.addArc(oval, startAngle, sweepAngle)
        }

        canvas.drawTextOnPath(content, path, 0f, 0f, paint)
    }

    /**
     * Menggambar efek 3D Extrusion dengan menumpuk layer teks berulang kali (1 s/d [extrudeDepth])
     * dengan pergeseran 1 pixel ke arah sudut kedalaman ([getExtrudeVector]).
     */
    private fun draw3DExtrusion(canvas: Canvas, basePaint: TextPaint) {
        val (dirX, dirY) = getExtrudeVector()
        val depthPaint = TextPaint(basePaint).apply {
            alpha = opacity.coerceIn(0, 255)
            clearShadowLayer()
        }
        val depthLayout = createLayout(depthPaint)

        val grad = extrudeGradient
        if (grad != null) {
            // Ukuran tumpukan depth didasarkan pada layout teks itu sendiri
            depthPaint.shader = grad.createShader(depthLayout.width.toFloat(), depthLayout.height.toFloat())
        } else {
            depthPaint.color = extrudeColor
            depthPaint.shader = null
        }

        // Gambar bertumpuk dari lapisan terdalam (belakang) ke depan
        for (d in extrudeDepth downTo 1) {
            canvas.save()
            canvas.translate(dirX * d, dirY * d)
            depthLayout.draw(canvas)
            canvas.restore()
        }
    }

    /**
     * Menggambar efek 3D Shadow: tumpukan layer bayangan tebal (1 s/d [shadow3DDepth])
     * bergeser 1 pixel ke arah [getShadow3DVector], sehingga membentuk bayangan
     * dalam/solid di belakang teks (berbeda dari Drop Shadow yang soft & bergeser pendek).
     *
     * Bila [shadow3DBlur] > 0, lapisan terjauh diblur lewat offscreen software bitmap
     * (BlurMaskFilter) sehingga ujung bayangan tampak lebih lembut.
     */
    private fun draw3DShadow(canvas: Canvas, basePaint: TextPaint) {
        val depth = shadow3DDepth.coerceIn(1, 50)
        val alpha = (shadow3DOpacity.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        val blur = shadow3DBlur.coerceIn(0f, 40f)
        val (dirX, dirY) = getShadow3DVector()

        val shadowPaint = TextPaint(basePaint).apply {
            clearShadowLayer()
            color = shadow3DColor
            this.alpha = alpha
            shader = null
        }

        if (blur <= 0f) {
            val shadowLayout = createLayout(shadowPaint)
            // Tumpukan bayangan solid — lapisan terdalam (belakang) digambar dulu
            for (d in depth downTo 1) {
                canvas.save()
                canvas.translate(dirX * d, dirY * d)
                shadowLayout.draw(canvas)
                canvas.restore()
            }
            return
        }

        // Mode blur: render ke offscreen software bitmap (BlurMaskFilter tidak
        // berfungsi pada canvas hardware). Lapisan terjauh di-blur, sisanya solid.
        val blurredPaint = TextPaint(shadowPaint).apply {
            maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        val solidLayout = createLayout(shadowPaint)
        val blurredLayout = createLayout(blurredPaint)
        val pad = (ceil(depth.toFloat()) + blur + 8).toInt()
        val bw = solidLayout.width + pad * 2
        val bh = solidLayout.height + pad * 2

        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val bc = Canvas(bmp).apply { translate(pad.toFloat(), pad.toFloat()) }

        bc.save()
        bc.translate(dirX * depth, dirY * depth)
        blurredLayout.draw(bc)
        bc.restore()

        for (d in depth - 1 downTo 1) {
            bc.save()
            bc.translate(dirX * d, dirY * d)
            solidLayout.draw(bc)
            bc.restore()
        }

        canvas.drawBitmap(
            bmp,
            -pad.toFloat(),
            -pad.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        bmp.recycle()
    }

    /**
     * Menggambar efek Emboss / Bevel menggunakan [EmbossMaskFilter].
     *
     * **Mengapa offscreen bitmap?**
     * [EmbossMaskFilter] hanya bekerja pada software-rendered [Canvas].
     * Karena [PixelCanvasView] adalah hardware-accelerated, filter dijalankan
     * pada canvas offscreen (Bitmap → Canvas), kemudian hasilnya di-blit
     * ke canvas utama melalui `drawBitmap` biasa.
     *
     * **Parameter cahaya:**
     * Vektor arah cahaya dihitung dari [embossLightAngle] (derajat):
     * ```
     * direction = [cos(θ), sin(θ), 0.5]
     * ```
     * - [embossAmbient]      — intensitas cahaya ambient (0.0–1.0)
     * - [embossSpecular]     — kilap specular / highlight bevel (0–20)
     * - [embossIntensity]    — penguat kontras (0–2.5): menaikkan specular &
     *                          menurunkan ambient → bayangan makin dalam,
     *                          highlight makin terang.
     * - [embossBevel]        — ketebalan/kehalusan relief (0.5–12)
     */
    private fun drawEmbossEffect(canvas: Canvas, layout: StaticLayout) {
        val lw  = layout.width
        val lh  = layout.height
        val pad = (embossBevel * 2 + 4).toInt()
        val bw  = lw + pad * 2
        val bh  = lh + pad * 2

        // Keep a solid text base so the relief never makes glyph interiors disappear.
        layout.draw(canvas)

        // Offscreen software bitmap — EmbossMaskFilter bekerja di sini
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val bmpCanvas = Canvas(bmp)  // Canvas ini adalah software-rendered

        // Hitung vektor arah cahaya dari sudut derajat
        val rad = Math.toRadians(embossLightAngle.toDouble()).toFloat()
        val lightDir = floatArrayOf(cos(rad), sin(rad), 0.5f)

        // Terapkan embossIntensity: naikkan specular, turunkan ambient agar kontras
        val intensity = embossIntensity.coerceIn(0f, 2.5f)
        val ambient   = (embossAmbient.coerceIn(0f, 1f) / (1f + intensity * 0.5f)).coerceIn(0f, 1f)
        val specular  = embossSpecular.coerceAtLeast(0.1f) * (0.6f + intensity)

        val embossPaint = buildMaskPaint().apply {
            color      = textColor
            // Blend the relief over the crisp base instead of replacing it.
            alpha      = (opacity.coerceIn(0, 255) * 0.72f).roundToInt()
            if (textureEnabled && textureBitmap != null && !textureBitmap!!.isRecycled) {
                shader = createTextureShader()
            } else if (gradientEnabled && gradient != null) {
                shader = gradient?.createShader(lw.toFloat(), lh.toFloat())
            }
            @Suppress("DEPRECATION")
            val embossFilter = EmbossMaskFilter(
                lightDir,
                ambient,
                specular,
                embossBevel.coerceIn(0.5f, 12f)
            )
            maskFilter = embossFilter
        }

        bmpCanvas.translate(pad.toFloat(), pad.toFloat())
        createLayout(embossPaint).draw(bmpCanvas)

        // Blit offscreen bitmap ke canvas hardware utama
        canvas.drawBitmap(
            bmp,
            -pad.toFloat(),
            -pad.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        bmp.recycle()
    }

    /**
     * Menggambar efek Neon / Glow menggunakan offscreen software bitmap:
     *
     *  1. Glow luar (blur lebar, alpha tipis) → kedalaman cahaya.
     *  2. Glow inti (blur rapat, alpha [neonIntensity]) → lingkaran cahaya.
     *  3. Bila [neonCoreEnabled], teks inti digambar dengan [textColor]
     *     (versi neon "hidup"); bila mati → hollow (hanya tabung cahaya).
     *
     * [BlurMaskFilter] hanya bekerja di canvas software, jadi mask dibuat di
     * bitmap terpisah lalu di-blit ke canvas hardware utama.
     */
    private fun drawNeonEffect(canvas: Canvas, layout: StaticLayout) {
        val radius = neonRadius.coerceIn(1f, 40f)
        val pad = (radius + 8).toInt()
        val bw = layout.width + pad * 2
        val bh = layout.height + pad * 2

        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val bc = Canvas(bmp)
        bc.translate(pad.toFloat(), pad.toFloat())

        val glowAlpha = (neonIntensity.coerceIn(0.1f, 2f) * 255).toInt().coerceIn(0, 255)

        // Lapis luar: sebaran lebar untuk kedalaman cahaya
        val outerAlpha = glowAlpha / 2
        if (outerAlpha > 0) {
            val outerPaint = buildMaskPaint().apply {
                color = neonColor
                alpha = outerAlpha
                maskFilter = BlurMaskFilter(radius * 1.9f, BlurMaskFilter.Blur.NORMAL)
            }
            createLayout(outerPaint).draw(bc)
        }

        // Lapis inti: blur rapat dengan kekuatan neonIntensity
        val glowPaint = buildMaskPaint().apply {
            color = neonColor
            alpha = glowAlpha
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        createLayout(glowPaint).draw(bc)

        // Inti teks (opsional): teks terang di tengah cahaya
        if (neonCoreEnabled) {
            val corePaint = buildMaskPaint().apply {
                color = textColor
                alpha = opacity.coerceIn(0, 255)
            }
            createLayout(corePaint).draw(bc)
        }

        canvas.drawBitmap(
            bmp,
            -pad.toFloat(),
            -pad.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        bmp.recycle()
    }

    /**
     * Menggambar teks fill beserta efek Inner Shadow menggunakan teknik dua offscreen bitmap:
     *
     *  • **Bitmap A** (`maskBmp`)   — bentuk teks crisp (alpha mask) tanpa blur.
     *  • **Bitmap B** (`shadowBmp`) — shadow diblur + digeser, lalu di-clip ke Bitmap A
     *                                 menggunakan [PorterDuff.Mode.DST_IN].
     *
     * Bitmap B kemudian di-overlay pada teks yang sudah dirender menggunakan
     * [PorterDuff.Mode.SRC_ATOP] di dalam sebuah `saveLayer` sehingga shadow
     * hanya muncul di bagian **dalam** kurva huruf.
     *
     * > **Catatan:** [BlurMaskFilter] bekerja pada software-rendered [Canvas]
     * > (canvas offscreen bitmap), bukan hardware canvas. Karena kita membuat
     * > Bitmap terpisah, filter ini berfungsi di semua API.
     */
    private fun drawFillWithInnerShadow(canvas: Canvas, layout: StaticLayout) {
        val lw  = layout.width
        val lh  = layout.height
        // Tambahkan padding supaya blur tidak terpotong di tepi bitmap
        val pad = (innerShadowRadius + abs(innerShadowDx) + abs(innerShadowDy) + 4f).toInt()
        val bw  = lw + pad * 2
        val bh  = lh + pad * 2

        // ── Bitmap A: Text alpha mask (crisp, hitam solid) ────────────────────
        val maskBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        Canvas(maskBmp).run {
            translate(pad.toFloat(), pad.toFloat())
            createLayout(buildMaskPaint()).draw(this)
        }

        // ── Bitmap B: Shadow (blur + offset) → clip ke Bitmap A ───────────────
        val shadowBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        Canvas(shadowBmp).run {
            val alpha  = (innerShadowOpacity.coerceIn(0f, 1f) * 255).toInt()
            val argb   = (innerShadowColor and 0x00FFFFFF) or (alpha shl 24)
            val blurPaint = buildMaskPaint().apply {
                color      = argb
                maskFilter = BlurMaskFilter(
                    innerShadowRadius.coerceAtLeast(1f),
                    BlurMaskFilter.Blur.NORMAL
                )
            }
            // Offset shadow ke arah (dx, dy): bayangan muncul di sisi berlawanan
            translate(pad + innerShadowDx, pad + innerShadowDy)
            createLayout(blurPaint).draw(this)
        }

        // Clip: hanya pertahankan shadow di mana Bitmap A (teks) ada
        Canvas(shadowBmp).drawBitmap(
            maskBmp, 0f, 0f,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
        )

        // ── Composite: fill + inner shadow overlay dalam saveLayer ─────────────
        //   saveLayer membuat "permukaan sementara" sehingga SRC_ATOP membaca
        //   piksel destination dengan benar di canvas hardware-accelerated.
        val sc = canvas.saveLayer(
            -pad.toFloat(), -pad.toFloat(),
            lw + pad.toFloat(), lh + pad.toFloat(),
            null
        )

        // DST: gambar teks fill (drop shadow via setShadowLayer sudah terpasang)
        layout.draw(canvas)

        // SRC_ATOP: overlay inner shadow — hanya di atas piksel teks
        canvas.drawBitmap(
            shadowBmp,
            -pad.toFloat(), -pad.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            }
        )

        canvas.restoreToCount(sc)

        // Bebaskan memori segera setelah selesai render
        maskBmp.recycle()
        shadowBmp.recycle()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bounds & Copy
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUnwarpedDimensions(): Pair<Float, Float> {
        val layout = createLayout(obtainTextPaint())
        val lw = layout.width.toFloat()
        val lh = layout.height.toFloat()
        val padL = paddingLeft.coerceAtLeast(0f)
        val padT = paddingTop.coerceAtLeast(0f)
        val padR = paddingRight.coerceAtLeast(0f)
        val padB = paddingBottom.coerceAtLeast(0f)
        return if (lw > 0f && lh > 0f) {
            Pair(lw + padL + padR, lh + padT + padB)
        } else {
            // Fallback estimasi ukuran (e.g. saat pengujian headless JVM)
            val estWidth = (if (text.isEmpty()) 1 else text.length) * textSize * 0.6f
            Pair(max(20f, estWidth + padL + padR), max(20f, textSize + padT + padB))
        }
    }

    override fun getLayerTransformMatrix(w: Float, h: Float): Matrix {
        val matrix = Matrix()
        if (rotate3DX != 0f || rotate3DY != 0f || rotate3DZ != 0f) {
            val camera = Camera()
            val matrix3D = Matrix()
            camera.save()
            camera.rotateX(rotate3DX)
            camera.rotateY(rotate3DY)
            if (rotate3DZ != 0f) camera.rotateZ(rotate3DZ)
            camera.getMatrix(matrix3D)
            camera.restore()

            matrix3D.preTranslate(-w / 2f, -h / 2f)
            matrix3D.postTranslate(w / 2f, h / 2f)
            matrix.postConcat(matrix3D)
        }
        matrix.postTranslate(x, y)
        matrix.postScale(scale, scale, x + w / 2f, y + h / 2f)
        matrix.postRotate(rotation, x + w / 2f, y + h / 2f)
        return matrix
    }

    override fun getBounds(): RectF {
        val layout = createLayout(obtainTextPaint())
        val w = (layout.width + paddingLeft + paddingRight).toFloat()
        val h = (layout.height + paddingTop + paddingBottom).toFloat()
        val rect = RectF(0f, 0f, w, h)

        if (extrudeEnabled && extrudeDepth > 0) {
            val (dirX, dirY) = getExtrudeVector()
            val totalDx = dirX * extrudeDepth
            val totalDy = dirY * extrudeDepth
            if (totalDx > 0) rect.right += totalDx else rect.left += totalDx
            if (totalDy > 0) rect.bottom += totalDy else rect.top += totalDy
        }

        val matrix = Matrix()
        if (perspectiveEnabled) {
            getPerspectiveMatrix(w, h)?.let { pMatrix ->
                matrix.postConcat(pMatrix)
            }
        }

        if (rotate3DX != 0f || rotate3DY != 0f || rotate3DZ != 0f) {
            val camera = Camera()
            val matrix3D = Matrix()
            camera.save()
            camera.rotateX(rotate3DX)
            camera.rotateY(rotate3DY)
            if (rotate3DZ != 0f) camera.rotateZ(rotate3DZ)
            camera.getMatrix(matrix3D)
            camera.restore()

            matrix3D.preTranslate(-w / 2f, -h / 2f)
            matrix3D.postTranslate(w / 2f, h / 2f)
            matrix.postConcat(matrix3D)
        }

        matrix.postTranslate(x, y)
        matrix.postScale(scale, scale, x + w / 2f, y + h / 2f)
        matrix.postRotate(rotation, x + w / 2f, y + h / 2f)
        matrix.mapRect(rect)
        return rect
    }

    override fun contentBlurSignature(): Int {
        var h = hashCode()
        h = h * 31 + (blendExtra?.ordinal ?: -1)
        textureBitmap?.let { bmp ->
            h = h * 31 + System.identityHashCode(bmp)
            h = h * 31 + bmp.generationId
        }
        return h
    }

    override fun copyLayer(): TextLayer = this.copy(
        id = UUID.randomUUID().toString(),
        x  = this.x + 30f,
        y  = this.y + 30f,
        gradient = this.gradient?.copy(),
        extrudeGradient = this.extrudeGradient?.copy(),
        richTextSpans = this.richTextSpans.map { it.copy() }.toMutableList(),
        textureBitmap = this.textureBitmap,
        perspectiveCorners = this.perspectiveCorners.clone(),
        blendMode = this.blendMode
    ).also { it.blendExtra = this.blendExtra }
}
