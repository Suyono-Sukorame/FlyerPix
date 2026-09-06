package com.flyerpix.editor

import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.history.CanvasCommand
import com.flyerpix.editor.canvas.history.CanvasHistoryManager
import com.flyerpix.editor.canvas.history.CanvasStateSnapshot
import com.flyerpix.editor.canvas.history.SnapshotCommand
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.canvas.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test untuk Undo & Redo History System (Command Pattern) - Prompt 50.
 *
 * Menguji:
 * 1. Initial state pada history manager (undo/redo kosong).
 * 2. Pencatatan snapshot via SnapshotCommand dan pembersihan redo stack.
 * 3. Eksekusi Undo & Redo memulihkan snapshot yang tepat.
 * 4. Branching history: Penambahan aksi baru setelah undo membuang redo stack lama.
 * 5. Batas maksimum history (`maxHistorySize`) memotong riwayat tertua.
 * 6. Immutability: Deep cloning layer pada snapshot menjaga riwayat dari mutasi eksternal.
 * 7. Snapshot menangkap konfigurasi background dan ukuran kanvas.
 * 8. Listener `onHistoryChanged` menerima notifikasi perubahan status canUndo dan canRedo.
 */
class CanvasHistoryTest {

    @Test
    fun `initial state has empty undo and redo stacks and cannot undo or redo`() {
        val historyManager = CanvasHistoryManager()

        assertFalse("Initial state should not be able to undo", historyManager.canUndo())
        assertFalse("Initial state should not be able to redo", historyManager.canRedo())
        assertEquals(0, historyManager.undoStackSize)
        assertEquals(0, historyManager.redoStackSize)
        assertNull(historyManager.undo())
        assertNull(historyManager.redo())
    }

    @Test
    fun `pushing command enables undo and clears redo stack`() {
        val historyManager = CanvasHistoryManager()

        val state0 = CanvasStateSnapshot(layers = emptyList())
        val state1 = CanvasStateSnapshot(layers = listOf(TextLayer(text = "Hello")))

        val command = SnapshotCommand(
            actionName = "Tambah Teks",
            previousState = state0,
            newState = state1,
            applyState = {}
        )

        historyManager.pushCommand(command)

        assertTrue(historyManager.canUndo())
        assertFalse(historyManager.canRedo())
        assertEquals(1, historyManager.undoStackSize)
        assertEquals(0, historyManager.redoStackSize)
    }

    @Test
    fun `undo executes command undo, moves to redo stack, and restores previous snapshot`() {
        val historyManager = CanvasHistoryManager()
        var restoredLayersCount = -1

        val state0 = CanvasStateSnapshot(layers = emptyList())
        val state1 = CanvasStateSnapshot(layers = listOf(TextLayer(text = "Hello")))

        val command = SnapshotCommand(
            actionName = "Tambah Teks",
            previousState = state0,
            newState = state1,
            applyState = { snapshot ->
                restoredLayersCount = snapshot.layers.size
            }
        )

        historyManager.pushCommand(command)

        val undoneCommand = historyManager.undo()
        assertNotNull(undoneCommand)
        assertEquals("Tambah Teks", undoneCommand?.actionName)
        assertEquals(0, restoredLayersCount) // Previous state (empty) dipulihkan
        assertFalse(historyManager.canUndo())
        assertTrue(historyManager.canRedo())
        assertEquals(0, historyManager.undoStackSize)
        assertEquals(1, historyManager.redoStackSize)
    }

    @Test
    fun `redo executes command redo, moves back to undo stack, and restores target snapshot`() {
        val historyManager = CanvasHistoryManager()
        var restoredLayersCount = -1

        val state0 = CanvasStateSnapshot(layers = emptyList())
        val state1 = CanvasStateSnapshot(layers = listOf(TextLayer(text = "Hello")))

        val command = SnapshotCommand(
            actionName = "Tambah Teks",
            previousState = state0,
            newState = state1,
            applyState = { snapshot ->
                restoredLayersCount = snapshot.layers.size
            }
        )

        historyManager.pushCommand(command)
        historyManager.undo()
        assertEquals(0, restoredLayersCount)

        val redoneCommand = historyManager.redo()
        assertNotNull(redoneCommand)
        assertEquals("Tambah Teks", redoneCommand?.actionName)
        assertEquals(1, restoredLayersCount) // New state (1 layer) dipulihkan kembali
        assertTrue(historyManager.canUndo())
        assertFalse(historyManager.canRedo())
        assertEquals(1, historyManager.undoStackSize)
        assertEquals(0, historyManager.redoStackSize)
    }

    @Test
    fun `consecutive undo and redo traverse state snapshots correctly`() {
        val historyManager = CanvasHistoryManager()
        var currentText = ""

        fun makeCommand(name: String, oldText: String, newText: String): SnapshotCommand {
            return SnapshotCommand(
                actionName = name,
                previousState = CanvasStateSnapshot(layers = listOf(TextLayer(text = oldText))),
                newState = CanvasStateSnapshot(layers = listOf(TextLayer(text = newText))),
                applyState = { snapshot ->
                    currentText = (snapshot.layers.firstOrNull() as? TextLayer)?.text ?: ""
                }
            )
        }

        historyManager.pushCommand(makeCommand("Edit 1", "A", "B"))
        historyManager.pushCommand(makeCommand("Edit 2", "B", "C"))
        historyManager.pushCommand(makeCommand("Edit 3", "C", "D"))

        assertEquals(3, historyManager.undoStackSize)

        // Undo 1: C -> B
        historyManager.undo()
        assertEquals("C", currentText)

        // Undo 2: B -> A
        historyManager.undo()
        assertEquals("B", currentText)

        // Undo 3: A -> original
        historyManager.undo()
        assertEquals("A", currentText)
        assertFalse(historyManager.canUndo())
        assertEquals(3, historyManager.redoStackSize)

        // Redo 1: A -> B
        historyManager.redo()
        assertEquals("B", currentText)

        // Redo 2: B -> C
        historyManager.redo()
        assertEquals("C", currentText)

        // Redo 3: C -> D
        historyManager.redo()
        assertEquals("D", currentText)
        assertFalse(historyManager.canRedo())
        assertEquals(3, historyManager.undoStackSize)
    }

    @Test
    fun `recording new action after undo purges redo stack (branching history)`() {
        val historyManager = CanvasHistoryManager()

        val cmd1 = SnapshotCommand("Action 1", CanvasStateSnapshot(), CanvasStateSnapshot()) {}
        val cmd2 = SnapshotCommand("Action 2", CanvasStateSnapshot(), CanvasStateSnapshot()) {}
        val cmd3 = SnapshotCommand("Action 3", CanvasStateSnapshot(), CanvasStateSnapshot()) {}

        historyManager.pushCommand(cmd1)
        historyManager.pushCommand(cmd2)

        // Undo aksi 2 -> redo stack memiliki 1 item
        historyManager.undo()
        assertEquals(1, historyManager.redoStackSize)
        assertTrue(historyManager.canRedo())

        // User melakukan aksi baru (Action 3) -> cabang baru tercipta, redo stack dibuang
        historyManager.pushCommand(cmd3)
        assertEquals(0, historyManager.redoStackSize)
        assertFalse(historyManager.canRedo())
        assertEquals(2, historyManager.undoStackSize) // cmd1 dan cmd3
    }

    @Test
    fun `history manager respects maxHistorySize and drops oldest entries`() {
        val maxLimit = 5
        val historyManager = CanvasHistoryManager(maxHistorySize = maxLimit)

        for (i in 1..10) {
            val cmd = SnapshotCommand("Action $i", CanvasStateSnapshot(), CanvasStateSnapshot()) {}
            historyManager.pushCommand(cmd)
        }

        assertEquals(maxLimit, historyManager.undoStackSize)

        // Undo berkali-kali hingga habis, harus tepat maxLimit kali
        var undoCount = 0
        while (historyManager.canUndo()) {
            historyManager.undo()
            undoCount++
        }
        assertEquals(maxLimit, undoCount)
    }

    @Test
    fun `state snapshot deep clones layers so subsequent layer mutations do not corrupt past snapshots`() {
        val originalTextLayer = TextLayer(text = "Original Text").apply {
            x = 100f
            y = 200f
            textColor = 0xFF0000
        }

        val snapshot = CanvasStateSnapshot.capture(
            layers = listOf(originalTextLayer),
            background = com.flyerpix.editor.canvas.model.CanvasBackground(
                mode = com.flyerpix.editor.canvas.model.CanvasBackgroundMode.SOLID_COLOR,
                solidColor = 0xFFFFFF
            ),
            canvasWidth = 1080,
            canvasHeight = 1080,
            selectedLayer = originalTextLayer
        )

        // Mutasi objek originalTextLayer setelah di-snapshot
        originalTextLayer.text = "Mutated Text"
        originalTextLayer.x = 999f
        originalTextLayer.y = 888f
        originalTextLayer.textColor = 0x00FF00

        // Snapshot harus tetap mempertahankan nilai asli saat dicapture
        val snapshotLayer = snapshot.layers.first() as TextLayer
        assertEquals("Original Text", snapshotLayer.text)
        assertEquals(100f, snapshotLayer.x, 0.001f)
        assertEquals(200f, snapshotLayer.y, 0.001f)
        assertEquals(0xFF0000, snapshotLayer.textColor)
    }

    @Test
    fun `state snapshot captures and restores background type and canvas dimension attributes`() {
        val shapeLayer = ShapeLayer(shapeType = ShapeType.ROUNDED_RECTANGLE).apply {
            width = 300f
            height = 150f
            cornerRadiusX = 20f
            cornerRadiusY = 20f
        }

        val snapshot = CanvasStateSnapshot.capture(
            layers = listOf(shapeLayer),
            background = com.flyerpix.editor.canvas.model.CanvasBackground(
                mode = com.flyerpix.editor.canvas.model.CanvasBackgroundMode.TRANSPARENT
            ),
            canvasWidth = 1920,
            canvasHeight = 1080,
            selectedLayer = null
        )

        assertEquals(com.flyerpix.editor.canvas.model.CanvasBackgroundMode.TRANSPARENT, snapshot.background.mode)
        assertEquals(1920, snapshot.canvasWidth)
        assertEquals(1080, snapshot.canvasHeight)
        assertNull(snapshot.selectedLayerIndex)
        assertEquals(1, snapshot.layers.size)

        val restoredShape = snapshot.layers.first() as ShapeLayer
        assertEquals(ShapeType.ROUNDED_RECTANGLE, restoredShape.shapeType)
        assertEquals(300f, restoredShape.width, 0.001f)
        assertEquals(20f, restoredShape.cornerRadiusX, 0.001f)
        assertEquals(20f, restoredShape.cornerRadiusY, 0.001f)
    }

    @Test
    fun `onHistoryChanged callback correctly reports canUndo and canRedo transitions`() {
        val historyManager = CanvasHistoryManager()
        var lastCanUndo = false
        var lastCanRedo = false

        historyManager.onHistoryChanged = { canUndo, canRedo ->
            lastCanUndo = canUndo
            lastCanRedo = canRedo
        }

        val cmd = SnapshotCommand("Action", CanvasStateSnapshot(), CanvasStateSnapshot()) {}

        // Push -> canUndo true, canRedo false
        historyManager.pushCommand(cmd)
        assertTrue(lastCanUndo)
        assertFalse(lastCanRedo)

        // Undo -> canUndo false, canRedo true
        historyManager.undo()
        assertFalse(lastCanUndo)
        assertTrue(lastCanRedo)

        // Redo -> canUndo true, canRedo false
        historyManager.redo()
        assertTrue(lastCanUndo)
        assertFalse(lastCanRedo)

        // Clear -> canUndo false, canRedo false
        historyManager.clear()
        assertFalse(lastCanUndo)
        assertFalse(lastCanRedo)
    }
}
