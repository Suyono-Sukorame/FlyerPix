package com.flyerpix.editor

import android.graphics.Bitmap
import android.graphics.RectF
import com.flyerpix.editor.canvas.history.CanvasHistoryManager
import com.flyerpix.editor.canvas.history.CanvasStateSnapshot
import com.flyerpix.editor.canvas.history.SnapshotCommand
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.ImageLayer
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.ui.dialog.LayerManagerBottomSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk Fitur Merge Layers (Menggabungkan Lapisan) menjadi satu objek ImageLayer (Prompt 36).
 *
 * Memverifikasi:
 * 1. Bounding box gabungan mencakup seluruh rentang koordinat layer yang dicentang.
 * 2. Z-index layer hasil merge menggantikan posisi terendah dari layer yang digabungkan.
 * 3. Penghapusan seluruh layer asal dari daftar kanvas dan penyisipan ImageLayer tunggal.
 * 4. Proteksi validasi: minimal 2 layer harus dipilih untuk penggabungan.
 * 5. Proteksi jika layer tidak terdapat di kanvas.
 * 6. Model ImageLayer (dimensi, cloning, transformasi bounds).
 */
class MergeLayersTest {

    private fun createDummyBitmap(): Bitmap {
        return try {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val field = unsafeClass.getDeclaredField("theUnsafe")
            field.isAccessible = true
            val unsafe = field.get(null)
            val allocate = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
            allocate.invoke(unsafe, Bitmap::class.java) as Bitmap
        } catch (_: Throwable) {
            Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        }
    }

    @Test
    fun `calculateMergedBoundingBox encompasses all constituent layers`() {
        val layer1 = object : CanvasLayer(x = 20f, y = 30f) {
            override fun draw(canvas: android.graphics.Canvas, paint: android.graphics.Paint) {}
            override fun getBounds(): RectF = RectF().apply { left = 20f; top = 30f; right = 150f; bottom = 100f }
            override fun copyLayer(): CanvasLayer = this
        }

        val layer2 = object : CanvasLayer(x = 80f, y = 50f) {
            override fun draw(canvas: android.graphics.Canvas, paint: android.graphics.Paint) {}
            override fun getBounds(): RectF = RectF().apply { left = 80f; top = 10f; right = 240f; bottom = 180f }
            override fun copyLayer(): CanvasLayer = this
        }

        val mergedBounds = LayerManagerBottomSheet.calculateMergedBoundingBox(listOf(layer1, layer2))

        assertEquals(20f, mergedBounds.left, 0.001f)
        assertEquals(10f, mergedBounds.top, 0.001f)
        assertEquals(240f, mergedBounds.right, 0.001f)
        assertEquals(180f, mergedBounds.bottom, 0.001f)
    }

    @Test
    fun `calculateMergedZIndex finds the lowest z-index among selected layers`() {
        val l0 = TextLayer(text = "Layer 0")
        val l1 = TextLayer(text = "Layer 1")
        val l2 = TextLayer(text = "Layer 2")
        val l3 = TextLayer(text = "Layer 3")

        val canvasLayers = listOf<CanvasLayer>(l0, l1, l2, l3)

        // Merge l1 and l3 -> lowest z-index is 1
        val minIndex1 = LayerManagerBottomSheet.calculateMergedZIndex(canvasLayers, listOf(l1, l3))
        assertEquals(1, minIndex1)

        // Merge l2 and l3 -> lowest z-index is 2
        val minIndex2 = LayerManagerBottomSheet.calculateMergedZIndex(canvasLayers, listOf(l2, l3))
        assertEquals(2, minIndex2)

        // Merge l0 and l2 -> lowest z-index is 0
        val minIndex3 = LayerManagerBottomSheet.calculateMergedZIndex(canvasLayers, listOf(l0, l2))
        assertEquals(0, minIndex3)
    }

    @Test
    fun `performMergeLogic replaces selected layers with merged ImageLayer at minimum z-index`() {
        val l0 = TextLayer(text = "Layer 0")
        val l1 = TextLayer(text = "Layer 1")
        val l2 = TextLayer(text = "Layer 2")
        val l3 = TextLayer(text = "Layer 3")

        val canvasLayers = mutableListOf<CanvasLayer>(l0, l1, l2, l3)

        val dummyBmp = createDummyBitmap()
        val mergedLayer = ImageLayer(
            x = 50f,
            y = 50f,
            bitmap = dummyBmp,
            layerName = "Merged 1 & 2"
        )

        // Merge l1 and l2
        val success = LayerManagerBottomSheet.performMergeLogic(
            canvasLayers = canvasLayers,
            layersToMerge = listOf(l1, l2),
            mergedLayer = mergedLayer
        )

        assertTrue(success)
        assertEquals(3, canvasLayers.size)
        assertEquals(l0, canvasLayers[0])
        assertEquals(mergedLayer, canvasLayers[1]) // disisipkan pada posisi minimum z-index (1)
        assertEquals(l3, canvasLayers[2])
    }

    @Test
    fun `performMergeLogic when merging bottom and top layers places merged layer at index 0`() {
        val l0 = TextLayer(text = "Bottom")
        val l1 = TextLayer(text = "Middle")
        val l2 = TextLayer(text = "Top")

        val canvasLayers = mutableListOf<CanvasLayer>(l0, l1, l2)

        val dummyBmp = createDummyBitmap()
        val mergedLayer = ImageLayer(
            x = 0f,
            y = 0f,
            bitmap = dummyBmp,
            layerName = "Merged Bottom & Top"
        )

        val success = LayerManagerBottomSheet.performMergeLogic(
            canvasLayers = canvasLayers,
            layersToMerge = listOf(l0, l2),
            mergedLayer = mergedLayer
        )

        assertTrue(success)
        assertEquals(2, canvasLayers.size)
        assertEquals(mergedLayer, canvasLayers[0]) // Terendah adalah index 0
        assertEquals(l1, canvasLayers[1])
    }

    @Test
    fun `performMergeLogic fails when less than 2 layers are provided`() {
        val l0 = TextLayer(text = "Layer 0")
        val l1 = TextLayer(text = "Layer 1")
        val canvasLayers = mutableListOf<CanvasLayer>(l0, l1)

        val dummyBmp = createDummyBitmap()
        val mergedLayer = ImageLayer(bitmap = dummyBmp)

        // Hanya 1 layer
        assertFalse(
            LayerManagerBottomSheet.performMergeLogic(
                canvasLayers = canvasLayers,
                layersToMerge = listOf(l0),
                mergedLayer = mergedLayer
            )
        )

        // 0 layer
        assertFalse(
            LayerManagerBottomSheet.performMergeLogic(
                canvasLayers = canvasLayers,
                layersToMerge = emptyList(),
                mergedLayer = mergedLayer
            )
        )

        // List kanvas tidak boleh berubah
        assertEquals(2, canvasLayers.size)
    }

    @Test
    fun `performMergeLogic fails when layers are not present in canvas`() {
        val l0 = TextLayer(text = "Layer 0")
        val foreign1 = TextLayer(text = "Foreign 1")
        val foreign2 = TextLayer(text = "Foreign 2")
        val canvasLayers = mutableListOf<CanvasLayer>(l0)

        val dummyBmp = createDummyBitmap()
        val mergedLayer = ImageLayer(bitmap = dummyBmp)

        assertFalse(
            LayerManagerBottomSheet.performMergeLogic(
                canvasLayers = canvasLayers,
                layersToMerge = listOf(foreign1, foreign2),
                mergedLayer = mergedLayer
            )
        )
        assertEquals(1, canvasLayers.size)
    }

    @Test
    fun `merge batch is recorded as a single undoable and redoable history action`() {
        val l0 = TextLayer(text = "L0")
        val l1 = TextLayer(text = "L1")
        val l2 = TextLayer(text = "L2")

        val canvasLayers = mutableListOf<CanvasLayer>(l0, l1, l2)
        val dummyBmp = createDummyBitmap()
        val mergedLayer = ImageLayer(
            x = 50f,
            y = 50f,
            bitmap = dummyBmp,
            layerName = "Merged Layer"
        )

        val background = CanvasBackground()
        val history = CanvasHistoryManager()
        var currentLayers: List<CanvasLayer> = emptyList()

        // 1. Snapshot sebelum merge: seluruh layer asli tercatat
        val beforeState = CanvasStateSnapshot.capture(
            layers = canvasLayers,
            background = background,
            canvasWidth = 1080,
            canvasHeight = 1080,
            selectedLayer = l1,
            actionName = "Gabung Layer"
        )

        // 2. Eksekusi engine penggabungan (l1 & l2 -> mergedLayer)
        assertTrue(
            LayerManagerBottomSheet.performMergeLogic(
                canvasLayers = canvasLayers,
                layersToMerge = listOf(l1, l2),
                mergedLayer = mergedLayer
            )
        )
        assertEquals(2, canvasLayers.size)

        val afterState = CanvasStateSnapshot.capture(
            layers = canvasLayers,
            background = background,
            canvasWidth = 1080,
            canvasHeight = 1080,
            selectedLayer = mergedLayer,
            actionName = "Gabung Layer"
        )

        // 3. Batch action dicatat sebagai SATU command di CanvasHistoryManager
        val command = SnapshotCommand(
            actionName = "Gabung Layer",
            beforeState = beforeState,
            afterState = afterState,
            applyState = { snapshot ->
                currentLayers = snapshot.layers.map { it.copyLayer() }
            }
        )
        history.recordCommand(command)
        assertTrue(history.canUndo())
        assertFalse(history.canRedo())

        // 4. Undo: seluruh layer asli dikembalikan dengan urutan z-index yang sama
        history.undo()
        assertTrue(history.canRedo())
        assertEquals(3, currentLayers.size)
        assertEquals(
            listOf("L0", "L1", "L2"),
            currentLayers.map { (it as TextLayer).text }
        )

        // 5. Redo: ImageLayer hasil gabungan dipulihkan kembali (batch yang sama)
        history.redo()
        assertTrue(history.canUndo())
        assertFalse(history.canRedo())
        assertEquals(2, currentLayers.size)
        assertEquals("L0", (currentLayers[0] as TextLayer).text)
        assertEquals("Merged Layer", (currentLayers[1] as ImageLayer).layerName)
        assertEquals(50f, currentLayers[1].x, 0.001f)
        assertEquals(50f, currentLayers[1].y, 0.001f)
    }

    @Test
    fun `imageLayer copyLayer creates exact clone with unique id`() {
        val dummyBmp = createDummyBitmap()
        val original = ImageLayer(
            x = 120f,
            y = 200f,
            scale = 1.5f,
            rotation = 45f,
            opacity = 180,
            bitmap = dummyBmp,
            layerName = "Custom Image"
        )

        val clone = original.copyLayer()

        assertEquals(original.x, clone.x, 0.001f)
        assertEquals(original.y, clone.y, 0.001f)
        assertEquals(original.scale, clone.scale, 0.001f)
        assertEquals(original.rotation, clone.rotation, 0.001f)
        assertEquals(original.opacity, clone.opacity)
        assertEquals(original.layerName, clone.layerName)
        assertEquals(original.bitmap, clone.bitmap)
        assertFalse(original.id == clone.id) // ID baru yang unik
    }

    @Test
    fun `imageLayer containsCanvasPoint validates touch within bounds`() {
        val dummyBmp = createDummyBitmap()
        val layer = object : ImageLayer(
            x = 100f,
            y = 100f,
            scale = 1f,
            rotation = 0f,
            bitmap = dummyBmp,
            layerName = "Test"
        ) {
            override fun getUnwarpedDimensions(): Pair<Float, Float> = Pair(200f, 150f)
        }

        // Dalam bounds (100..300, 100..250)
        assertTrue(layer.containsCanvasPoint(150f, 150f))
        assertTrue(layer.containsCanvasPoint(100f, 100f))
        assertTrue(layer.containsCanvasPoint(300f, 250f))

        // Luar bounds
        assertFalse(layer.containsCanvasPoint(50f, 150f))
        assertFalse(layer.containsCanvasPoint(350f, 150f))
        assertFalse(layer.containsCanvasPoint(150f, 50f))
        assertFalse(layer.containsCanvasPoint(150f, 300f))
    }
}
