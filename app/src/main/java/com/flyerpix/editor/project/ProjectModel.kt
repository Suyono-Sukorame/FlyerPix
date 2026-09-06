package com.flyerpix.editor.project

import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasLayer

/**
 * Model runtime proyek PixelLab — merepresentasikan satu sesi kerja editor.
 *
 * [ProjectModel] digunakan sebagai perantara antara [com.flyerpix.editor.canvas.PixelCanvasView]
 * dan [ProjectSerializer] saat menyimpan atau membuka file `.plp`.
 *
 * @property projectName  Nama proyek yang ditampilkan pada UI dan digunakan sebagai nama file default.
 * @property createdAt    Epoch millis saat proyek pertama kali dibuat.
 * @property updatedAt    Epoch millis terakhir kali proyek disimpan.
 * @property canvasWidth  Lebar kanvas dalam piksel (50–8192).
 * @property canvasHeight Tinggi kanvas dalam piksel (50–8192).
 * @property background   Konfigurasi latar belakang kanvas (Transparan/Solid/Gradasi/Gambar).
 * @property layers       Daftar layer kanvas dari z-index terendah (bawah) ke tertinggi (atas).
 */
data class ProjectModel(
    val projectName: String = "Untitled",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080,
    val background: CanvasBackground = CanvasBackground.solid(-1 /* Color.WHITE */),
    val layers: MutableList<CanvasLayer> = mutableListOf()
)
