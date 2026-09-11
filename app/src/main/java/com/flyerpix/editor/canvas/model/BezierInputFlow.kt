package com.flyerpix.editor.canvas.model

import android.graphics.Color

class BezierInputFlow {
    private val points = mutableListOf<Pair<Float, Float>>()

    fun addPoint(x: Float, y: Float) {
        if (points.isEmpty()) {
            points.add(x to y)
            return
        }

        val last = points.last()
        val dx = x - last.first
        val dy = y - last.second
        if (dx * dx + dy * dy > 1f) {
            points.add(x to y)
        }
    }

    fun clear() {
        points.clear()
    }

    fun canApply(): Boolean = points.size >= 2

    fun buildPenLayer(
        strokeColor: Int = Color.WHITE,
        strokeWidth: Float = 6f
    ): PenLayer? {
        if (!canApply()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        for (point in points) {
            minX = minOf(minX, point.first)
            minY = minOf(minY, point.second)
        }

        val relative = points.map { point ->
            (point.first - minX) to (point.second - minY)
        }
        return PenLayer.fromPoints(relative, closed = false).apply {
            this.strokeColor = strokeColor
            this.strokeWidth = strokeWidth
        }
    }
}
