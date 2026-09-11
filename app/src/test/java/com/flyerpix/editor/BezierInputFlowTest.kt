package com.flyerpix.editor

import com.flyerpix.editor.canvas.model.BezierInputFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BezierInputFlowTest {
    @Test
    fun bezierInputFlow_requires_two_points_before_apply() {
        val flow = BezierInputFlow()

        assertFalse(flow.canApply())

        flow.addPoint(100f, 100f)
        assertFalse(flow.canApply())

        flow.addPoint(200f, 180f)
        assertTrue(flow.canApply())
    }
}
