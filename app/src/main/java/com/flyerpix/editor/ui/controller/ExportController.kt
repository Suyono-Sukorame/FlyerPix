package com.flyerpix.editor.ui.controller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.ExportFormat
import com.flyerpix.editor.canvas.model.ExportQuality
import com.flyerpix.editor.canvas.model.TextLayer
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.font.FontManager
import com.flyerpix.editor.project.ProjectModel
import com.flyerpix.editor.project.ProjectSerializer
import com.flyerpix.editor.ui.dialog.ExportImageDialog
import com.flyerpix.editor.ui.dialog.ProjectManagerBottomSheet
import com.flyerpix.editor.ui.dialog.SaveProjectDialog
import java.io.File
import java.io.FileOutputStream

class ExportController(
    private val activity: AppCompatActivity,
    private val binding: ActivityEditorBinding,
    private val canvas: PixelCanvasView,
    private val showSnackbar: (String) -> Unit,
    private val updateCanvasAspectRatio: (Int, Int) -> Unit
) {
    companion object {
        const val REQUEST_WRITE_STORAGE = 99
        const val REQUEST_CAMERA_BG = 100
    }

    var currentProjectName: String = "Untitled"

    /**
     * Callback default untuk membuka file picker .plp eksternal. Di-iwire oleh
     * EditorActivity pada inisialisasi controller sehingga panggilan
     * [showProjectManager] tanpa argumen tetap bisa membuka file picker.
     */
    var onImportExternalRequested: (() -> Unit)? = null

    private val exportExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showExportDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE)
            return
        }
        ExportImageDialog.show(
            context = activity,
            canvasWidth = canvas.canvasWidth,
            canvasHeight = canvas.canvasHeight,
            onExportToGallery = { quality, format, customW, customH ->
                canvas.exportHighResolutionAsync(
                    quality = quality, format = format,
                    customWidth = customW, customHeight = customH,
                    fileName = currentProjectName.takeIf { it.isNotBlank() && it != "Untitled" }
                ) { uri ->
                    if (uri != null) {
                        val (w, h) = if (customW != null && customH != null) Pair(customW, customH)
                        else quality.calculateDimensions(canvas.canvasWidth, canvas.canvasHeight)
                        showSnackbar("Image saved to Gallery (${w} × ${h} px)!")
                    } else showSnackbar("Failed to export image to Gallery")
                }
            },
            onShareRequested = { quality, format, customW, customH ->
                shareImage(quality, format, customW, customH)
            }
        )
    }

    fun shareImage(
        quality: ExportQuality = ExportQuality.DEFAULT,
        format: ExportFormat = ExportFormat.PNG,
        customW: Int? = null,
        customH: Int? = null
    ) {
        exportExecutor.execute {
            try {
                val bmp = canvas.renderOffscreenBitmap(quality, format, customW, customH)
                val cachePath = File(activity.cacheDir, "images").apply { if (!exists()) mkdirs() }
                val file = File(cachePath, "flyerpix_export_${System.currentTimeMillis()}.${format.extension}")
                FileOutputStream(file).use { stream ->
                    bmp.compress(format.compressFormat, 100, stream)
                }
                bmp.recycle()
                val contentUri = FileProvider.getUriForFile(activity, "com.flyerpix.editor", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = format.mimeType
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                mainHandler.post {
                    activity.startActivity(Intent.createChooser(intent, "Share PixelLab Image"))
                }
            } catch (e: Exception) {
                mainHandler.post { showSnackbar("Failed to share image: ${e.localizedMessage}") }
            }
        }
    }

    fun showSaveProjectDialog() {
        SaveProjectDialog.show(
            context = activity,
            pixelCanvasView = canvas,
            defaultProjectName = currentProjectName
        ) { savedName, _ ->
            currentProjectName = savedName
            showSnackbar("Project '$savedName' saved successfully!")
        }
    }

    fun showProjectManager(onImportExternalRequested: (() -> Unit)? = null) {
        ProjectManagerBottomSheet.show(
            fragmentManager = activity.supportFragmentManager,
            onProjectLoaded = { project -> loadProject(project) },
            onImportExternalRequested = onImportExternalRequested ?: this.onImportExternalRequested
        )
    }

    /**
     * Mengekspor proyek `.plp` saat ini ke folder publik **Download/FlyerPix**
     * (MediaStore Q+ / legacy pre-Q) sehingga file terlihat di direktori internal
     * perangkat dan dapat dibuka lewat file manager / SAF picker (Prompt: wire UI).
     */
    fun showExportProjectToDownloads() {
        try {
            val fileName = currentProjectName
                .takeIf { it.isNotBlank() && it != "Untitled" }
                ?.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                ?: "untitled"
            val snapshot = canvas.exportProjectSnapshot(currentProjectName)
            val uri = ProjectSerializer.exportProjectToDownloads(activity, snapshot, fileName)
            if (uri != null) {
                showSnackbar("Project exported to Download/FlyerPix/$fileName.plp!")
            } else {
                showSnackbar("Failed to export .plp to Downloads")
            }
        } catch (e: Exception) {
            showSnackbar("Failed to export .plp: ${e.localizedMessage}")
        }
    }

    fun loadProject(project: ProjectModel) {
        currentProjectName = project.projectName
        FontManager.init(activity)
        project.layers.filterIsInstance<TextLayer>().forEach { layer ->
            layer.fontName?.let { name ->
                FontManager.findFont(name)?.let { layer.typeface = it.typeface }
            }
        }
        canvas.importProjectSnapshot(project)
        updateCanvasAspectRatio(project.canvasWidth, project.canvasHeight)
        showSnackbar("Project '${project.projectName}' loaded successfully!")
    }

    fun importProjectFromUri(uri: Uri) {
        try {
            val json = activity.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (json.isNullOrBlank()) {
                showImportError("File proyek kosong atau tidak dapat dibaca.")
                return
            }
            val project = ProjectSerializer.deserialize(json)
            val fileName = (resolveImportFileName(uri) ?: project.projectName)
                .removeSuffix(ProjectSerializer.FILE_EXTENSION)
                .ifBlank { "Imported" }
            try {
                ProjectSerializer.saveProject(activity, project, fileName)
            } catch (saveError: Exception) {
                showImportError("Gagal menyimpan proyek ke penyimpanan lokal.")
                return
            }
            loadProject(project)
            showSnackbar("Project '${project.projectName}' imported & added to My Projects")
        } catch (e: Exception) {
            showImportError("Format .plp tidak didukung atau rusak.")
        }
    }

    /**
     * Mengambil nama file asli dari URI (MediaStore DISPLAY_NAME, lalu fallback
     * ke segmen path terakhir URI).
     */
    private fun resolveImportFileName(uri: Uri): String? {
        return try {
            activity.contentResolver.query(
                uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        } ?: try {
            uri.lastPathSegment?.substringAfterLast('/')?.let { seg ->
                if (seg.isNotBlank()) seg else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Dialog error yang ramah saat file .plp gagal dibuka / tidak valid. */
    private fun showImportError(message: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            activity, R.style.AppAlertDialog
        )
            .setTitle("File Tidak Dapat Dibuka")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    fun onPermissionResult(requestCode: Int, grantResults: IntArray, onCameraGranted: () -> Unit) {
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) showExportDialog()
            REQUEST_CAMERA_BG -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) onCameraGranted()
        }
    }
}
