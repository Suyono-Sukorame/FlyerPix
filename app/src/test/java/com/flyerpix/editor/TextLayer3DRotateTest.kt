package com.flyerpix.editor

import android.graphics.Camera
import android.graphics.Matrix
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit test untuk fitur 3D Rotate pada TextLayer.
 *
 * Verifikasi:
 *  1. Properti rotate3DX, rotate3DY, rotate3DZ default = 0f
 *  2. Nilai properti berubah setelah assignment
 *  3. copyLayer() menyalin nilai rotasi 3D
 *  4. Camera API menghasilkan matrix 3D yang tidak identitas saat rotasi ≠ 0
 */
class TextLayer3DRotateTest {

    @Test
    fun `default 3D rotate values are zero`() {
        val layer = TextLayer()
        assertEquals(0f, layer.rotate3DX)
        assertEquals(0f, layer.rotate3DY)
        assertEquals(0f, layer.rotate3DZ)
    }

    @Test
    fun `rotate3DX and rotate3DY can be set in range`() {
        val layer = TextLayer().apply {
            rotate3DX = 45f
            rotate3DY = -30f
            rotate3DZ = 15f
        }
        assertEquals(45f, layer.rotate3DX)
        assertEquals(-30f, layer.rotate3DY)
        assertEquals(15f, layer.rotate3DZ)
    }

    @Test
    fun `copyLayer preserves 3D rotation values`() {
        val original = TextLayer().apply {
            rotate3DX = 60f
            rotate3DY = -90f
            rotate3DZ = 10f
        }
        val copy = original.copyLayer()
        assertEquals(original.rotate3DX, copy.rotate3DX)
        assertEquals(original.rotate3DY, copy.rotate3DY)
        assertEquals(original.rotate3DZ, copy.rotate3DZ)
        assertNotEquals(original.id, copy.id)
    }

    @Test
    fun `extreme rotate values are within slider bounds`() {
        val layer = TextLayer().apply {
            rotate3DX = 180f
            rotate3DY = -180f
            rotate3DZ = 0f
        }
        // Nilai coerceIn pada slider tidak boleh kasih IllegalArgumentException
        val coercedX = layer.rotate3DX.coerceIn(-180f, 180f)
        val coercedY = layer.rotate3DY.coerceIn(-180f, 180f)
        assertEquals(180f, coercedX)
        assertEquals(-180f, coercedY)
    }

    @Test
    fun `camera matrix transforms are non-identity for non-zero rotation`() {
        // Verifikasi Camera API menghasilkan matrix yang valid (tidak identity)
        val camera = Camera()
        val matrix = Matrix()
        camera.save()
        camera.rotateX(45f)
        camera.rotateY(30f)
        camera.getMatrix(matrix)
        camera.restore()

        val identity = Matrix()
        // Matrix dari rotasi 3D tidak boleh sama dengan identity matrix
        assert(!matrix.isIdentity) { "Camera matrix seharusnya bukan identity setelah rotasi 3D" }
    }

    @Test
    fun `reset to zero restores identity-equivalent state`() {
        val layer = TextLayer().apply {
            rotate3DX = 90f
            rotate3DY = -45f
            rotate3DZ = 20f
        }
        layer.rotate3DX = 0f
        layer.rotate3DY = 0f
        layer.rotate3DZ = 0f

        assertEquals(0f, layer.rotate3DX)
        assertEquals(0f, layer.rotate3DY)
        assertEquals(0f, layer.rotate3DZ)
    }
}
