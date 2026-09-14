package com.flyerpix.editor.canvas.model

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import java.util.UUID

/**
 * TextOnPathLayer - render text along a Bézier path (Phase 5 - killer feature).
 *
 * Renders text following the curve of an underlying path, with full control over:
 * - Text content, size, color, font style
 * - Path offset (start position along path)
 * - Character spacing
 * - Text alignment (left, center, right)
 * - Rotation follow (text rotates with path curve)
 */
data class TextOnPathLayer(
    override var id: String = UUID.randomUUID().toString(),
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var scale: Float = 1f,
    override var rotation: Float = 0f,
    override var opacity: Int = 255,
    override var isLocked: Boolean = false,
    override var isVisible: Boolean = true,
    // ── Text Content ────────────────────────────────────────────────────────
    var text: String = "Text on Path",
    var fontSize: Float = 32f,
    var fontColor: Int = android.graphics.Color.BLACK,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    // ── Path Following ──────────────────────────────────────────────────────
    var pathOffset: Float = 0f,                  // Start position along path (0-1)
    var letterSpacing: Float = 0f,              // Extra space between letters
    var textAlignment: TextAlignment = TextAlignment.CENTER,
    var followPathRotation: Boolean = true,     // Rotate text with curve
    // ── Reference Path ──────────────────────────────────────────────────────
    var referencePath: Path? = null,            // The path to follow
    var referencePathId: String = "",           // ID of PenLayer to reference
    // ── Perspective Warping ────────────────────────────────────────────────
    override var perspectiveEnabled: Boolean = false,
    override var perspectiveCorners: FloatArray = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        0f, 1f
    ),
    // ── Blending Mode ──────────────────────────────────────────────────────
    override var blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC_OVER
) : CanvasLayer(
    id = id,
    x = x,
    y = y,
    scale = scale,
    rotation = rotation,
    opacity = opacity,
    isLocked = isLocked,
    isVisible = isVisible,
    perspectiveEnabled = perspectiveEnabled,
    perspectiveCorners = perspectiveCorners,
    blendMode = blendMode
) {

    companion object {
        // Cache untuk path measurements
        private const val CACHE_SIZE = 256  // Simplifikasi path dengan 256 segments
    }

    // Path measurement cache
    private var cachedPathLength: Float = 0f
    private var cachedSegments: FloatArray? = null

    enum class TextAlignment {
        LEFT, CENTER, RIGHT
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Path Measurement & Text Positioning
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Measure the total length of the reference path.
     */
    fun measurePathLength(): Float {
        if (referencePath == null) return 0f
        
        if (cachedPathLength > 0f) return cachedPathLength
        
        val pathMeasure = android.graphics.PathMeasure(referencePath, false)
        cachedPathLength = pathMeasure.length
        return cachedPathLength
    }

    /**
     * Get a point along the path at a given distance.
     * Returns [Float, Float] = [x, y] position.
     */
    fun getPointAtDistance(distance: Float): Pair<Float, Float> {
        if (referencePath == null) return Pair(0f, 0f)
        
        val pathMeasure = android.graphics.PathMeasure(referencePath, false)
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        
        val clampedDistance = distance.coerceIn(0f, pathMeasure.length)
        pathMeasure.getPosTan(clampedDistance, pos, tan)
        
        return Pair(pos[0], pos[1])
    }

    /**
     * Get position and tangent (angle) at a distance along path.
     * Returns [x, y, angle_radians]
     */
    fun getPosTanAtDistance(distance: Float): FloatArray {
        if (referencePath == null) return floatArrayOf(0f, 0f, 0f)
        
        val pathMeasure = android.graphics.PathMeasure(referencePath, false)
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        
        val clampedDistance = distance.coerceIn(0f, pathMeasure.length)
        pathMeasure.getPosTan(clampedDistance, pos, tan)
        
        // Calculate angle from tangent
        val angle = kotlin.math.atan2(tan[1].toDouble(), tan[0].toDouble()).toFloat()
        
        return floatArrayOf(pos[0], pos[1], angle)
    }

    /**
     * Calculate positions for each character along the path.
     * Returns list of (x, y, angle_rad) for each character.
     */
    fun calculateCharacterPositions(paint: Paint): List<FloatArray> {
        if (text.isEmpty() || referencePath == null) return emptyList()
        
        val positions = mutableListOf<FloatArray>()
        val pathLength = measurePathLength()
        
        if (pathLength <= 0f) return positions
        
        // Measure total text width
        val textWidths = FloatArray(text.length)
        paint.getTextWidths(text, textWidths)
        val totalTextWidth = textWidths.sum() + (letterSpacing * (text.length - 1))
        
        // Calculate start offset based on alignment
        val startDistance = when (textAlignment) {
            TextAlignment.LEFT -> pathOffset * pathLength
            TextAlignment.CENTER -> (pathOffset * pathLength) - (totalTextWidth / 2f)
            TextAlignment.RIGHT -> (pathOffset * pathLength) - totalTextWidth
        }
        
        // Position each character
        var currentDistance = startDistance
        for (i in text.indices) {
            val charPos = getPosTanAtDistance(currentDistance)
            positions.add(charPos)
            
            // Advance by character width + letter spacing
            currentDistance += textWidths[i] + letterSpacing
        }
        
        return positions
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    override fun drawContent(canvas: Canvas, paint: Paint) {
        if (!isVisible || text.isEmpty() || referencePath == null) return
        
        val saveCount = canvas.save()
        
        // Apply layer transformations
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        canvas.rotate(rotation)
        
        // Apply perspective
        val pMat = getPerspectiveMatrix(100f, 100f)  // Dummy dimensions
        if (pMat != null) {
            canvas.concat(pMat)
        }
        
        // Setup paint
        paint.textSize = fontSize
        paint.color = fontColor
        paint.alpha = opacity.coerceIn(0, 255)
        paint.isAntiAlias = true
        paint.isLinearText = true
        paint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            when {
                isBold && isItalic -> android.graphics.Typeface.BOLD_ITALIC
                isBold -> android.graphics.Typeface.BOLD
                isItalic -> android.graphics.Typeface.ITALIC
                else -> android.graphics.Typeface.NORMAL
            }
        )
        
        // Get character positions
        val charPositions = calculateCharacterPositions(paint)
        
        // Draw each character
        for (i in text.indices) {
            if (i >= charPositions.size) break
            
            val posTan = charPositions[i]
            val charX = posTan[0]
            val charY = posTan[1]
            val angle = if (followPathRotation) posTan[2] else 0f
            
            val char = text[i].toString()
            val charWidth = paint.measureText(char)
            
            // Save canvas state for each character
            val charSaveCount = canvas.save()
            
            // Move to character position and rotate
            canvas.translate(charX, charY)
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
            
            // Draw character centered on its position
            canvas.drawText(char, -charWidth / 2f, 0f, paint)
            
            canvas.restoreToCount(charSaveCount)
        }
        
        canvas.restoreToCount(saveCount)
    }

    /**
     * Update the reference path from a PenLayer.
     */
    fun updateReferencePathFromPenLayer(penLayer: PenLayer?) {
        if (penLayer == null) {
            referencePath = null
            referencePathId = ""
            cachedPathLength = 0f
            return
        }
        
        referencePathId = penLayer.id
        
        // Build path from anchors
        val path = Path()
        if (penLayer.anchors.isEmpty()) {
            referencePath = null
            return
        }
        
        // Move to first anchor
        val firstAnchor = penLayer.anchors[0]
        path.moveTo(firstAnchor.x, firstAnchor.y)
        
        // Draw cubic bezier curves between anchors
        var i = 0
        while (i < penLayer.anchors.size - 1) {
            val anchor1 = penLayer.anchors[i]
            val anchor2 = penLayer.anchors[i + 1]
            
            path.cubicTo(
                anchor1.handleOutX, anchor1.handleOutY,
                anchor2.handleInX, anchor2.handleInY,
                anchor2.x, anchor2.y
            )
            
            i++
        }
        
        // Close path if needed
        if (penLayer.isClosed && penLayer.anchors.size >= 2) {
            val lastAnchor = penLayer.anchors.last()
            val firstAnchorAgain = penLayer.anchors[0]
            path.cubicTo(
                lastAnchor.handleOutX, lastAnchor.handleOutY,
                firstAnchorAgain.handleInX, firstAnchorAgain.handleInY,
                firstAnchorAgain.x, firstAnchorAgain.y
            )
        }
        
        referencePath = path
        cachedPathLength = 0f  // Invalidate cache
    }

    /**
     * Get bounding box of text on path (approximate).
     */
    override fun getBounds(): android.graphics.RectF {
        if (text.isEmpty() || referencePath == null) {
            return android.graphics.RectF(x, y, x + 1f, y + 1f)
        }
        
        val paint = Paint()
        paint.textSize = fontSize
        
        // Measure text
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val textWidth = bounds.width().toFloat()
        val textHeight = bounds.height().toFloat()
        
        return android.graphics.RectF(
            x - (textWidth * scale / 2f),
            y - (textHeight * scale / 2f),
            x + (textWidth * scale / 2f),
            y + (textHeight * scale / 2f)
        )
    }

    /**
     * Copy this layer (for undo/serialization).
     */
    override fun copyLayer(): TextOnPathLayer {
        return copy(
            id = id,
            x = x, y = y,
            scale = scale, rotation = rotation,
            opacity = opacity,
            isLocked = isLocked,
            isVisible = isVisible,
            perspectiveEnabled = perspectiveEnabled,
            perspectiveCorners = perspectiveCorners.copyOf(),
            blendMode = blendMode,
            text = text,
            fontSize = fontSize,
            fontColor = fontColor,
            isBold = isBold,
            isItalic = isItalic,
            pathOffset = pathOffset,
            letterSpacing = letterSpacing,
            textAlignment = textAlignment,
            followPathRotation = followPathRotation,
            referencePathId = referencePathId,
            referencePath = referencePath
        )
    }
}
