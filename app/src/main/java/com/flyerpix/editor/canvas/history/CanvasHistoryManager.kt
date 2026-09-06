package com.flyerpix.editor.canvas.history

import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.GradientColor
import java.util.ArrayDeque

/**
 * Snapshot kondisi kanvas pada satu titik waktu (immutable state).
 * Menyimpan seluruh layer hasil kloning independen, background, dan ukuran kanvas.
 */
data class CanvasStateSnapshot(
    val layers: List<CanvasLayer> = emptyList(),
    val background: CanvasBackground = CanvasBackground(),
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080,
    val selectedLayerIndex: Int? = null,
    val actionName: String = "Aksi Kanvas",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun capture(
            layers: List<CanvasLayer>,
            background: CanvasBackground,
            canvasWidth: Int,
            canvasHeight: Int,
            selectedLayer: CanvasLayer? = null,
            actionName: String = "Aksi Kanvas"
        ): CanvasStateSnapshot {
            val clonedLayers = layers.map { it.cloneLayer() }
            val selIndex = if (selectedLayer != null) {
                layers.indexOf(selectedLayer).takeIf { it >= 0 }
            } else null

            return CanvasStateSnapshot(
                layers = clonedLayers,
                background = background.copy(
                    gradient = background.gradient?.let { g ->
                        g.copy(
                            colors = g.colors.clone(),
                            positions = g.positions?.clone()
                        )
                    }
                ),
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                selectedLayerIndex = selIndex,
                actionName = actionName
            )
        }
    }
}

/**
 * Interface dasar untuk Command Pattern riwayat operasi kanvas.
 */
interface CanvasCommand {
    val actionName: String
    fun execute()
    fun undo()
}

/**
 * Command yang menyimpan transisi antara dua state snapshot kanvas.
 */
class SnapshotCommand(
    override val actionName: String,
    val beforeState: CanvasStateSnapshot,
    val afterState: CanvasStateSnapshot,
    private val applyState: (CanvasStateSnapshot) -> Unit
) : CanvasCommand {

    val previousState: CanvasStateSnapshot get() = beforeState
    val newState: CanvasStateSnapshot get() = afterState

    constructor(
        actionName: String,
        previousState: CanvasStateSnapshot,
        newState: CanvasStateSnapshot,
        applyState: (CanvasStateSnapshot) -> Unit,
        unused: Boolean = true
    ) : this(actionName, previousState, newState, applyState)

    override fun execute() {
        applyState(afterState)
    }

    override fun undo() {
        applyState(beforeState)
    }
}

/**
 * Pengelola stack riwayat Undo dan Redo untuk PixelCanvasView (Prompt 50).
 */
class CanvasHistoryManager(
    val maxHistorySize: Int = 30,
    var onHistoryChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null
) {
    val undoStack = ArrayDeque<CanvasCommand>()
    val redoStack = ArrayDeque<CanvasCommand>()

    val undoStackSize: Int get() = undoStack.size
    val redoStackSize: Int get() = redoStack.size

    fun pushCommand(command: CanvasCommand) = recordCommand(command)

    /**
     * Menambahkan aksi perintah baru ke puncak [undoStack].
     * Setiap kali aksi baru dicatat, [redoStack] secara otomatis dibersihkan.
     */
    fun recordCommand(command: CanvasCommand) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeLast() // Buang perintah tertua jika melampaui batas kapasitas memori
        }
        undoStack.push(command)
        redoStack.clear()
        notifyChanged()
    }

    /**
     * Membatalkan aksi terakhir: mengeluarkan command dari [undoStack], menjalankan [CanvasCommand.undo],
     * dan menyimpannya ke [redoStack].
     *
     * @return Command yang berhasil dibatalkan, atau null jika riwayat kosong.
     */
    fun undo(): CanvasCommand? {
        if (undoStack.isEmpty()) return null
        val command = undoStack.pop()
        command.undo()
        redoStack.push(command)
        notifyChanged()
        return command
    }

    /**
     * Mengulang aksi yang sebelumnya dibatalkan: mengeluarkan command dari [redoStack],
     * menjalankan [CanvasCommand.execute], dan mengembalikannya ke [undoStack].
     *
     * @return Command yang berhasil diulang, atau null jika redo stack kosong.
     */
    fun redo(): CanvasCommand? {
        if (redoStack.isEmpty()) return null
        val command = redoStack.pop()
        command.execute()
        undoStack.push(command)
        notifyChanged()
        return command
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        notifyChanged()
    }

    private fun notifyChanged() {
        onHistoryChanged?.invoke(canUndo(), canRedo())
    }
}
