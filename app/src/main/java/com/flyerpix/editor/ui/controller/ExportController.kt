package com.flyerpix.editor.ui.controller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.ExportFormat
import com.flyerpix.editor.canvas.model.ExportQuality
import com.flyerpix.editor.databinding.ActivityEditorBinding
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
                        showSnackbar("Gambar berhasil disimpan ke Galeri (${w} × ${h} px)!")
                    } else showSnackbar("Gagal mengekspor gambar ke Galeri")
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
                    activity.startActivity(Intent.createChooser(intent, "Bagikan Gambar PixelLab"))
                }
            } catch (e: Exception) {
                mainHandler.post { showSnackbar("Gagal membagikan gambar: ${e.localizedMessage}") }
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
            showSnackbar("Proyek '$savedName' berhasil disimpan!")
        }
    }

    fun showProjectManager() {
        ProjectManagerBottomSheet.show(
            fragmentManager = activity.supportFragmentManager,
            onProjectLoaded = { project -> loadProject(project) },
            onImportExternalRequested = { /* handled by Activity launcher */ }
        )
    }

    fun loadProject(project: ProjectModel) {
        currentProjectName = project.projectName
        canvas.importProjectSnapshot(project)
        updateCanvasAspectRatio(project.canvasWidth, project.canvasHeight)
        showSnackbar("Proyek '${project.projectName}' berhasil dimuat!")
    }

    fun importProjectFromUri(uri: Uri) {
        try {
            val json = activity.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (json.isNullOrBlank()) { showSnackbar("File proyek kosong atau tidak dapat dibaca"); return }
            loadProject(ProjectSerializer.deserialize(json))
        } catch (e: Exception) {
            showSnackbar("Gagal memuat file proyek: ${e.localizedMessage}")
        }
    }

    fun onPermissionResult(requestCode: Int, grantResults: IntArray, onCameraGranted: () -> Unit) {
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) showExportDialog()
            REQUEST_CAMERA_BG -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) onCameraGranted()
        }
    }
}
