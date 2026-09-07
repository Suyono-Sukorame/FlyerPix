package com.flyerpix.editor.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import com.flyerpix.editor.R
import com.flyerpix.editor.editableimageview.EditableImageView
import com.flyerpix.editor.editableimageview.EditorTool.PAINT
import com.flyerpix.editor.editableimageview.EditorTool.FIGURE
import com.flyerpix.editor.editableimageview.EditorTool.STICKER
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar

import com.google.android.material.tabs.TabLayout
import com.flyerpix.editor.canvas.PixelCanvasView
import com.flyerpix.editor.canvas.model.ExportFormat
import com.flyerpix.editor.canvas.model.ExportQuality
import com.flyerpix.editor.canvas.model.ImageLayer
import com.flyerpix.editor.canvas.model.StickerItem
import com.flyerpix.editor.databinding.ActivityEditorBinding
import com.flyerpix.editor.project.ProjectModel
import com.flyerpix.editor.project.ProjectSerializer
import com.flyerpix.editor.ui.dialog.ExportImageDialog
import com.flyerpix.editor.ui.dialog.SaveProjectDialog
import com.flyerpix.editor.ui.dialog.ProjectManagerBottomSheet
import com.flyerpix.editor.ui.dialog.ImagePreEditDialog
import com.flyerpix.editor.ui.adapter.AuthenticLayerAdapter
import com.flyerpix.editor.ui.controller.LayerPanelController
import com.flyerpix.editor.ui.controller.ObjectMenuController
import com.flyerpix.editor.ui.controller.ExportController
import com.flyerpix.editor.ui.controller.TextPanelController
import com.flyerpix.editor.ui.controller.CanvasMenuController
import com.flyerpix.editor.ui.controller.EffectsController
import com.flyerpix.editor.ui.controller.FontController
import com.flyerpix.editor.ui.controller.TemplateController
import com.flyerpix.editor.ui.controller.CanvasToolsController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min

class EditorActivity : AppCompatActivity(), TabSticker.TabStickerListener {
    private val PERMISSIONS_REQUEST_CAMERA_BG = 100
    lateinit var pixelCanvasView: PixelCanvasView
    lateinit var toolsBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var binding: ActivityEditorBinding

    // ── Controllers untuk memisahkan tanggung jawab ──────────────────────────
    private lateinit var layerPanel: LayerPanelController
    private lateinit var objectMenu: ObjectMenuController
    private lateinit var exportController: ExportController
    private lateinit var textPanelController: TextPanelController
    private lateinit var canvasMenuController: CanvasMenuController
    private lateinit var effectsController: EffectsController
    private lateinit var fontController: FontController
    private lateinit var templateController: TemplateController
    private lateinit var canvasToolsController: CanvasToolsController


    private val texturePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bmp = textPanelController.decodeBitmapFromUri(uri)
            if (bmp != null) {
                textPanelController.applyTextureBitmap(bmp)
            } else {
                showSnackbar("Gagal memuat gambar tekstur.")
            }
        }
    }

    private val customFontLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            fontController.handleCustomFontResult(uri)
        }
    }

    // ── Background Image Launchers (Prompt 45) ──────────────────────────────

    private var cameraPhotoUri: Uri? = null

    private val bgGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bmp = textPanelController.decodeBitmapFromUri(uri)
            if (bmp != null) {
                pixelCanvasView.setImageBackground(bmp)
                showSnackbar("Gambar latar belakang berhasil diterapkan!")
            } else {
                showSnackbar("Gagal memuat gambar dari galeri.")
            }
        }
    }

    private val bgCameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && cameraPhotoUri != null) {
            val bmp = textPanelController.decodeBitmapFromUri(cameraPhotoUri!!)
            if (bmp != null) {
                pixelCanvasView.setImageBackground(bmp)
                showSnackbar("Foto kamera berhasil dijadikan latar belakang!")
            } else {
                showSnackbar("Gagal memuat foto dari kamera.")
            }
        }
    }

    // ── Pre-Edit Gambar (Foto / Galeri → Sesuaikan → menjadi Layer) ───────────

    private val preEditImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bmp = textPanelController.decodeBitmapFromUri(uri)
            if (bmp != null) {
                showImagePreEdit(bmp)
            } else {
                showSnackbar("Gagal memuat gambar dari galeri.")
            }
        }
    }

    private fun showImagePreEdit(bitmap: Bitmap) {
        ImagePreEditDialog.show(supportFragmentManager, bitmap) { result ->
            if (result != null) {
                addImageAsLayer(result)
                showSnackbar("Gambar berhasil ditambahkan!")
            }
        }
    }

    private fun addImageAsLayer(bitmap: Bitmap) {
        val viewW = if (pixelCanvasView.width > 0) pixelCanvasView.width.toFloat() else 1080f
        val viewH = if (pixelCanvasView.height > 0) pixelCanvasView.height.toFloat() else 1080f
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()
        var scale = 1f
        if (bmpW > viewW * 0.9f || bmpH > viewH * 0.9f) {
            scale = min((viewW * 0.9f) / bmpW, (viewH * 0.9f) / bmpH)
        }
        val layer = ImageLayer(bitmap = bitmap, scale = scale, layerName = "Gambar")
        layer.x = (viewW - bmpW * scale) / 2f
        layer.y = (viewH - bmpH * scale) / 2f
        pixelCanvasView.addLayer(layer)
    }

    // ── Project Management State & Launcher (Prompt 48) ───────────────────────

    private val openProjectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            exportController.importProjectFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        this.title = ""
        pixelCanvasView = binding.pixelCanvasView

        // ── Profiling: baca flag debug via system property ─────────────────
        // `adb shell setprop debug.flyerpix_profile 1` → aktifkan timer onDraw.
        // Polling kecil tiap 500ms cukup karena flag hanya untuk sesi debug.
        val profileRunnable = object : Runnable {
            override fun run() {
                val enabled = readSystemProperty("debug.flyerpix_profile") == "1"
                if (enabled != PixelCanvasView.profileEnabled) {
                    PixelCanvasView.profileEnabled = enabled
                    android.util.Log.d("FlyerPixProfile", "profileEnabled=$enabled")
                }
                binding.pixelCanvasView.postDelayed(this, 500)
            }
        }
        binding.pixelCanvasView.postDelayed(profileRunnable, 500)

        // ── Stress test otomatis (debug) ────────────────────────────────────
        // `adb shell setprop debug.flyerpix_stress 10` → tambah 10 layer lalu
        // aktifkan blur+adjust, render beberapa detik, cetak frame-time.
        val stressCount = readSystemProperty("debug.flyerpix_stress").toIntOrNull() ?: -1
        if (stressCount >= 0) binding.pixelCanvasView.post { runRenderStressTest(stressCount) }

        // ── Inisialisasi Controllers ────────────────────────────────────────
        initializeControllers()

        // ── Legacy Initializations ───────────────────────────────────────────
        // Text Editor ala PixelLab: pasang controller PALING AWAL agar berlari PALING
        // AKHIR pada rantai onLayerSelectedListener → visibilitas panel selalu final.

        // Dialog edit teks interaktif saat double-tap layer teks
        pixelCanvasView.onTextLayerDoubleTapListener = { textLayer ->
            showEditTextDialog(textLayer)
        }

        setImage()
        initializeSave()
        initializeAuthenticTopBar()
        initializeTabLayout()
        initializeViewPager()
        initializeBottomSheetBehavior()
        initializeBottomNavigationView()
    }

    /**
     * Inisialisasi semua controller untuk memisahkan tanggung jawab.
     * Menggunakan Opsi A - Minimal: Extract ke Helper/Controller Class.
     */
    private fun initializeControllers() {
        // Export Controller
        exportController = ExportController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) },
            { w, h -> updateCanvasAspectRatio(w, h) }
        )

        // Text Panel Controller - Mengelola semua kontrol text editor
        textPanelController = TextPanelController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) },
            onShowMenu = { showMenu(it) },
            onEditTextRequested = { showEditTextDialog(it) },
            onCanvasChanged = { updateCanvasCardMargin() }
        )
        textPanelController.initialize()
        textPanelController.setTexturePickerLauncher(texturePickerLauncher)

        // Font Controller - Mengelola font picker dan custom fonts
        fontController = FontController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) }
        )
        fontController.initialize()
        fontController.setCustomFontLauncher(customFontLauncher)

        // Canvas Menu Controller - Mengelola menu canvas (background, size, dll)
        canvasMenuController = CanvasMenuController(
            this,
            binding,
            pixelCanvasView,
            supportFragmentManager,
            { showSnackbar(it) },
            { w, h -> updateCanvasAspectRatio(w, h) }
        )
        canvasMenuController.initialize()
        canvasMenuController.setBgGalleryLauncher(bgGalleryLauncher)
        canvasMenuController.setOnCameraRequested { checkCameraPermissionForBackground() }
        canvasMenuController.onDetailExpandedChanged = { setDetailExpanded(canvasMenuController.activeTag.isNotEmpty()) }

        // Effects Controller - Mengelola efek kanvas
        effectsController = EffectsController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) }
        )
        effectsController.initialize()
        effectsController.onDetailExpandedChanged = { setDetailExpanded(it) }

        // Template Controller - Mengelola template presets
        templateController = TemplateController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) },
            onProjectOpen = { exportController.showProjectManager() },
            onProjectSave = { exportController.showSaveProjectDialog() }
        )
        templateController.initialize()

        // Akhiri phase init TextPanelController SETELAH template default diterapkan.
        // Keduanya di-post ke queue yang sama (pixelCanvasView) → FIFO, post ini
        // dijalankan setelah applyDefaultTemplateIfNeeded, sehingga auto-switch
        // ke menu Text tidak menimpa menu Presets saat pertama membuka aplikasi.
        pixelCanvasView.post { textPanelController.finishInitialization() }

        // Layer Panel Controller
        layerPanel = LayerPanelController(
            binding,
            pixelCanvasView,
            { showSnackbar(it) },
            { showEditTextDialog(it) }
        )
        layerPanel.initialize()

        // Object Menu Controller
        objectMenu = ObjectMenuController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) },
            onGalleryRequested = { preEditImageLauncher.launch("image/*") },
            onCameraRequested = { checkCameraPermissionForBackground() },
            onPanelChanged = { updateCanvasCardMargin() }
        )
        objectMenu.initialize()
        objectMenu.onDetailExpandedChanged = { setDetailExpanded(objectMenu.activeTag.isNotEmpty()) }

        // Canvas Tools Controller - Mengelola eyedropper, crop, dan palette
        // DISABLED: Fitur ini belum diperlukan, di-disable untuk menghindari bug FAB
        canvasToolsController = CanvasToolsController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) }
        )
        // canvasToolsController.initialize()  // DISABLED
    }

    fun showEditTextDialog(textLayer: com.flyerpix.editor.canvas.model.TextLayer) {
        com.flyerpix.editor.ui.dialog.EditTextDialog.show(this, textLayer.text) { newText ->
            pixelCanvasView.runRecordedAction("Ubah Teks") {
                textLayer.text = newText
            }
            pixelCanvasView.invalidate()
        }
    }

    private fun initializeSave() {
        binding.saveFab.setOnClickListener {
            exportController.showExportDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.clear_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_background -> {
                val sheet = com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.show(supportFragmentManager, pixelCanvasView)
                sheet.onBackgroundImageRequested = { source ->
                    when (source) {
                        com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.Source.GALLERY -> {
                            bgGalleryLauncher.launch("image/*")
                        }
                        com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.Source.CAMERA -> {
                            checkCameraPermissionForBackground()
                        }
                    }
                }
            }
            R.id.action_image_size -> {
                canvasMenuController.showImageSizeDialog()
            }
            R.id.action_layers -> {
                com.flyerpix.editor.ui.dialog.LayerManagerBottomSheet.show(supportFragmentManager, pixelCanvasView)
            }
            R.id.action_save_project -> {
                exportController.showSaveProjectDialog()
            }
            R.id.action_open_project -> {
                exportController.showProjectManager()
            }
            R.id.clear -> pixelCanvasView.clearLayers()
        }
        return true
    }

    /**
     * Memperbarui resolusi kanvas dan rasio aspek kontainer canvasCard di MotionLayout (Prompt 43).
     */
    private fun updateCanvasAspectRatio(width: Int, height: Int) {
        pixelCanvasView.setCanvasSize(width, height)
        val ratio = "$width:$height"
        binding.motionLayout.getConstraintSet(R.id.start)?.setDimensionRatio(R.id.canvasCard, ratio)
        binding.motionLayout.getConstraintSet(R.id.end)?.setDimensionRatio(R.id.canvasCard, ratio)
        binding.motionLayout.requestLayout()
        Snackbar.make(binding.parentLayout, "Ukuran kanvas diubah ke $width × $height px ($ratio)", Snackbar.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (layerPanel.isOpen) {
            layerPanel.close()
            return
        }
        if (canvasToolsController.isCropActive()) {
            canvasToolsController.exitCropMode()
            return
        }
        if (canvasToolsController.isEyedropperActive()) {
            canvasToolsController.disableEyedropper()
            return
        }
        if (binding.paletteFab.isExpanded) {
            binding.paletteFab.isExpanded = false
        }
        if (binding.bottomNavigation.selectedItemId != R.id.nav_presets) {
            binding.bottomNavigation.selectedItemId = R.id.nav_presets
        } else if (saveMode) {
            exitSaveMode()
        } else {
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
            materialAlertDialogBuilder.setMessage(R.string.lose_work)
            materialAlertDialogBuilder.setPositiveButton(R.string.yes) { _, _ ->
                super.onBackPressed()
            }
            materialAlertDialogBuilder.setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            val materialAlertDialog = materialAlertDialogBuilder.create()
            materialAlertDialog.show()
        }
    }

    // ── TabStickerListener ──────────────────────────────────────────────────

    override fun onStickerSelected(stickerItem: StickerItem) {
        pixelCanvasView.addEmojiLayer(stickerItem.emoji)
    }

    /**
     * Controller tunggal untuk seluruh halaman menu bawah (UX ala Presets).
     * Menjamin hanya SATU halaman tampil dalam satu waktu, dan toolsBottomSheet
     * lama (dictator tumpang-tindih) selalu dipensiunkan.
     */
    private fun showMenu(menuId: Int) {
        // Hide all FABs immediately to prevent flash effect when switching menus
        // DISABLED: FAB features tidak diperlukan saat ini
        // binding.paletteFab.visibility = View.GONE
        // binding.eyedropperFab.visibility = View.GONE
        // binding.cropFab.visibility = View.GONE
        
        val pages = listOf(
            R.id.nav_presets to binding.bottomControlPanelContainer,
            R.id.nav_text to binding.textEditorBar,
            R.id.nav_object to binding.objectMenuPanel,
            R.id.nav_canvas to binding.canvasMenuPanel,
            R.id.nav_effects to binding.effectsMenuPanel
        )
        if (pages.none { it.first == menuId }) return
        pages.forEach { (id, page) ->
            page.visibility = if (id == menuId) View.VISIBLE else View.GONE
        }

        textPanelController.isPageOpen = menuId == R.id.nav_text
        textPanelController.pagePinnedByNav = textPanelController.isPageOpen

        // Sheet lama dipensiunkan: tak pernah boleh muncul lagi di atas kanvas.
        binding.toolsBottomSheet.visibility = View.GONE
        if (::toolsBottomSheetBehavior.isInitialized) {
            toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        if (textPanelController.isPageOpen) {
            val layer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (layer == null || layer.isLocked) {
                val unlocked = pixelCanvasView.layers.lastOrNull {
                    it is com.flyerpix.editor.canvas.model.TextLayer && !it.isLocked
                }
                if (unlocked != null) {
                    pixelCanvasView.selectedLayer = unlocked
                } else {
                    showSnackbar("Tambahkan layer teks dahulu via + Tambah → Teks")
                }
            }
            textPanelController.refreshUI()
        } else {
            textPanelController.hideStripAndPanels()
        }

        if (menuId == R.id.nav_object) {
            objectMenu.refreshUI()
        } else {
            objectMenu.deselect()
        }

        if (menuId == R.id.nav_canvas) {
            canvasMenuController.refreshUI()
        } else {
            canvasMenuController.deselect()
        }

        if (menuId == R.id.nav_effects) {
            effectsController.refreshUI()
        } else {
            effectsController.deselect()
        }
        
        // Update FAB visibility berdasarkan menu yang aktif
        // DISABLED: CanvasToolsController tidak di-initialize
        // canvasToolsController.updateContextFabVisibility()
        
        updateCanvasCardMargin()
    }

    private fun initializeBottomNavigationView() {
        // Halaman awal: Presets (default dari XML), sheet dipensiunkan.
        binding.toolsBottomSheet.visibility = View.GONE
        textPanelController.isPageOpen = false
        textPanelController.pagePinnedByNav = false

        binding.bottomNavigation.setOnItemSelectedListener { menuItem: MenuItem ->
            showMenu(menuItem.itemId)
            true
        }
        
        // Initialize dengan menu default (Presets) untuk set FAB visibility dengan benar
        showMenu(R.id.nav_presets)
    }

    private fun initializeAuthenticTopBar() {
        val top = binding.topBarInclude

        top.btnTopAdd.setOnClickListener { v ->
            showTopAddMenu(v)
        }

        top.btnTopSave.setOnClickListener { v ->
            showTopSaveMenu(v)
        }

        top.btnTopShare.setOnClickListener {
            exportController.showExportDialog()
        }

        top.btnTopQuotes.setOnClickListener {
            com.flyerpix.editor.ui.dialog.QuotesDialog.show(supportFragmentManager) { quote ->
                pixelCanvasView.addTextLayer(quote)
                showSnackbar("Kutipan berhasil ditambahkan!")
            }
        }

        top.btnTopOverflow.setOnClickListener { v ->
            showTopOverflowMenu(v)
        }

        // Undo & Redo History System (Command Pattern)
        top.btnTopUndo.alpha = if (pixelCanvasView.canUndo()) 1.0f else 0.4f
        top.btnTopUndo.isEnabled = pixelCanvasView.canUndo()
        top.btnTopRedo.alpha = if (pixelCanvasView.canRedo()) 1.0f else 0.4f
        top.btnTopRedo.isEnabled = pixelCanvasView.canRedo()

        pixelCanvasView.onHistoryStateChangedListener = { canUndo, canRedo ->
            top.btnTopUndo.alpha = if (canUndo) 1.0f else 0.4f
            top.btnTopUndo.isEnabled = canUndo
            top.btnTopRedo.alpha = if (canRedo) 1.0f else 0.4f
            top.btnTopRedo.isEnabled = canRedo
            layerPanel.refresh()
        }

        top.btnTopUndo.setOnClickListener {
            val success = pixelCanvasView.undo()
            if (success) {
                showSnackbar("Tindakan dibatalkan (Undo)")
            } else {
                showSnackbar("Tidak ada riwayat untuk di-undo")
            }
        }

        top.btnTopRedo.setOnClickListener {
            val success = pixelCanvasView.redo()
            if (success) {
                showSnackbar("Tindakan diulangi (Redo)")
            } else {
                showSnackbar("Tidak ada riwayat untuk di-redo")
            }
        }

        top.btnTopZoom.setOnClickListener {
            showSnackbar("Gunakan dua jari (pinch) untuk memperbesar/memperkecil kanvas")
        }

        top.btnTopGrid.setOnClickListener {
            pixelCanvasView.isGridEnabled = !pixelCanvasView.isGridEnabled
            pixelCanvasView.invalidate()
            showSnackbar(if (pixelCanvasView.isGridEnabled) "Grid panduan aktif" else "Grid panduan dinonaktifkan")
        }

        top.btnTopLayers.setOnClickListener {
            layerPanel.toggle()
        }
    }

    private fun showTopAddMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Teks")
        popup.menu.add(0, 2, 1, "Tanggal Sekarang")
        popup.menu.add(0, 3, 2, "Stiker")
        popup.menu.add(0, 4, 3, "Bentuk")
        popup.menu.add(0, 5, 4, "Dari Galeri")
        popup.menu.add(0, 6, 5, "Gambar Bebas")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> pixelCanvasView.addTextLayer("New Text")
                2 -> {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    pixelCanvasView.addTextLayer(date)
                }
                3 -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_object
                }
                4 -> {
                    showSnackbar("Menu Bentuk hadir lewat halaman Objek pada rilis berikutnya")
                }
                5 -> preEditImageLauncher.launch("image/*")
                6 -> {
                    showSnackbar("Gambar Bebas hadir lewat halaman Objek pada rilis berikutnya")
                }
            }
            true
        }
        popup.show()
    }

    private fun showTopSaveMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Simpan sebagai Proyek")
        popup.menu.add(0, 2, 1, "Simpan sebagai Gambar")
        popup.menu.add(0, 3, 2, "Buka Proyek (.plp)")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> exportController.showSaveProjectDialog()
                2 -> exportController.showExportDialog()
                3 -> exportController.showProjectManager()
            }
            true
        }
        popup.show()
    }

    private fun shareCanvasImage() {
        exportController.shareImage()
    }

    private fun showTopOverflowMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Gunakan gambar dari galeri")
        popup.menu.add(0, 2, 1, "Gunakan gambar dari kamera")
        popup.menu.add(0, 3, 2, "Ekspor gambar")
        popup.menu.add(0, 4, 3, "Ukuran gambar")
        popup.menu.add(0, 5, 4, "Latar belakang")
        popup.menu.add(0, 6, 5, "Bersihkan kanvas")
        popup.menu.add(0, 7, 6, "Simpan Proyek")
        popup.menu.add(0, 8, 7, "Buka Proyek (.plp)")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> bgGalleryLauncher.launch("image/*")
                2 -> checkCameraPermissionForBackground()
                3 -> exportController.showExportDialog()
                4 -> canvasMenuController.showImageSizeDialog()
                5 -> {
                    val sheet = com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.show(supportFragmentManager, pixelCanvasView)
                    sheet.onBackgroundImageRequested = { src ->
                        when (src) {
                            com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.Source.GALLERY -> bgGalleryLauncher.launch("image/*")
                            com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.Source.CAMERA -> checkCameraPermissionForBackground()
                        }
                    }
                }
                6 -> pixelCanvasView.clearLayers()
                7 -> exportController.showSaveProjectDialog()
                8 -> exportController.showProjectManager()
            }
            true
        }
        popup.show()
    }

    private var saveMode = false

    private fun enterSaveMode() {
        binding.bottomNavigation.visibility = View.GONE
        binding.bottomControlPanelContainer.visibility = View.GONE
        binding.objectMenuPanel.visibility = View.GONE
        binding.canvasMenuPanel.visibility = View.GONE
        binding.effectsMenuPanel.visibility = View.GONE
        binding.textEditorBar.visibility = View.GONE
        binding.toolbarConfirmCancel.visibility = View.GONE
        binding.paletteFab.visibility = View.GONE
        binding.eyedropperFab.visibility = View.GONE
        binding.cropFab.visibility = View.GONE
        binding.saveFab.visibility = View.VISIBLE
        binding.topBarInclude.root.visibility = View.GONE
        binding.nameTextInputLayout.visibility = View.VISIBLE
        toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        binding.motionLayout.transitionToEnd()
        saveMode = true
    }

    private fun exitSaveMode() {
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.bottomNavigation.translationY = 0f
        binding.saveFab.visibility = View.GONE
        binding.topBarInclude.root.visibility = View.VISIBLE
        binding.nameTextInputLayout.visibility = View.GONE
        val current = binding.bottomNavigation.selectedItemId
        val known = listOf(R.id.nav_presets, R.id.nav_text, R.id.nav_object, R.id.nav_canvas, R.id.nav_effects).contains(current)
        showMenu(if (known) current else R.id.nav_presets)
        // updateContextFabVisibility() sudah dipanggil di dalam showMenu(), tidak perlu double call
        binding.motionLayout.transitionToStart()
        saveMode = false
    }

    private fun checkWriteExternalStoragePermission() {
        exportController.showExportDialog()
    }

    private fun saveImage() {
        exportController.showExportDialog()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        exportController.onPermissionResult(requestCode, grantResults) {
            launchCameraForBackground()
        }
    }

    // ── Camera for Background Image (Prompt 45) ─────────────────────────────

    private fun checkCameraPermissionForBackground() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA_BG)
        } else {
            launchCameraForBackground()
        }
    }

    private fun launchCameraForBackground() {
        val photoFile = createBackgroundPhotoFile() ?: return
        cameraPhotoUri = androidx.core.content.FileProvider.getUriForFile(
            this, "com.flyerpix.editor", photoFile
        )
        bgCameraLauncher.launch(cameraPhotoUri!!)
    }

    private fun createBackgroundPhotoFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("BG_${timestamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun showSnackbar(message : String){
        Snackbar.make(binding.parentLayout, message, Snackbar.LENGTH_LONG).addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (saveMode) exitSaveMode()
            }
        }).show()
    }

    private fun initializeBottomSheetBehavior() {
        toolsBottomSheetBehavior = BottomSheetBehavior.from(binding.toolsBottomSheet)
        toolsBottomSheetBehavior.setBottomSheetCallback(createBottomSheetCallback())
    }

    private fun createBottomSheetCallback(): BottomSheetBehavior.BottomSheetCallback? {
        return object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEXT EDITOR (UX ala PixelLab): Tool Strip + Property Panel Kontekstual
    // ═══════════════════════════════════════════════════════════════════════

    // ────────────────────────────────────────────────────────────────────────
    // DETAIL EXPAND: menyembunyikan bottom nav saat detail dibuka agar
    // jendela konten lebih lebar, dan memunculkannya kembali saat ditutup.
    // ────────────────────────────────────────────────────────────────────────

    private var navTranslationAnimator: android.animation.ValueAnimator? = null
    private val panelHeightAnimators = HashMap<View, android.animation.ValueAnimator>()

    /**
     * Mengekspansi panel detail (object/canvas/effects) dan menggeser keluar
     * bottom nav saat `expanded == true`; kembali normal saat `false`.
     *
     * Panel tertutup:   tinggi 107dp, marginBottom 56dp (di atas nav).
     * Panel terbuka:    tinggi 163dp, marginBottom 0dp (nav disembunyikan).
     */
    private fun setDetailExpanded(expanded: Boolean) {
        val density = resources.displayMetrics.density
        val collapsedH = (107 * density).toInt()
        val expandedH = (163 * density).toInt()
        val collapsedMargin = (56 * density).toInt()

        if (!expanded) {
            // Kontraksi: semua panel kembali ke ukuran default & nav dipanggil kembali.
            listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
                .filter { it != null }
                .forEach { panel ->
                    panel.animateLayoutHeight(collapsedH)
                    panel.animateLayoutMarginBottom(collapsedMargin)
                }
            animateNavTranslation(0)
            updateCanvasCardMargin()
            return
        }

        val activePanel = when (binding.bottomNavigation.selectedItemId) {
            R.id.nav_object -> binding.objectMenuPanel
            R.id.nav_canvas -> binding.canvasMenuPanel
            R.id.nav_effects -> binding.effectsMenuPanel
            else -> return
        } ?: return

        val navOffset = (56 * density).toInt()
        listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
            .filter { it != null && it != activePanel }
            .forEach { panel -> panel.animateLayoutHeight(collapsedH) }
        activePanel.animateLayoutHeight(expandedH)
        activePanel.animateLayoutMarginBottom(0)
        animateNavTranslation(navOffset)
        updateCanvasCardMargin()
    }

    private fun View.animateLayoutHeight(target: Int) {
        panelHeightAnimators[this]?.cancel()
        val lp = layoutParams
        val start = lp?.height ?: 0
        val anim = android.animation.ValueAnimator.ofInt(start, target).apply {
            duration = 220
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                val l = layoutParams ?: return@addUpdateListener
                l.height = animatedValue as Int
                layoutParams = l
            }
        }
        panelHeightAnimators[this] = anim
        anim.start()
    }

    private fun View.animateLayoutMarginBottom(target: Int) {
        val lp = layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return
        val start = lp.bottomMargin
        val anim = android.animation.ValueAnimator.ofInt(start, target).apply {
            duration = 220
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                val l = layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return@addUpdateListener
                l.bottomMargin = animatedValue as Int
                layoutParams = l
            }
        }
        anim.start()
    }

    private fun animateNavTranslation(target: Int) {
        val nav = binding.bottomNavigation
        val start = nav.translationY.toInt()
        navTranslationAnimator?.cancel()
        navTranslationAnimator = android.animation.ValueAnimator.ofInt(start, target).apply {
            duration = 220
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                nav.translationY = (animatedValue as Int).toFloat()
            }
        }
        navTranslationAnimator?.start()
    }

    private fun readSystemProperty(key: String): String = try {
        val clazz = Class.forName("android.os.SystemProperties")
        clazz.getMethod("get", String::class.java).invoke(null, key) as String
    } catch (t: Throwable) {
        ""
    }

    /**
     * Stress test rendering: tambah N layer gambar + efek sesuai mode, lalu render
     * berulang. Baca frame-time per segmen via profiler onDraw (aktifkan
     * `debug.flyerpix_profile`). Dipicu `setprop debug.flyerpix_stress N`.
     * Mode: `debug.flyerpix_stressmode` 0=all, 1=layer saja, 2=blur+adjust, 3=blur saja.
     */
    private fun runRenderStressTest(layerCount: Int) {
        val mode = readSystemProperty("debug.flyerpix_stressmode").toIntOrNull() ?: 0
        val colors = intArrayOf(
            android.graphics.Color.rgb(230, 57, 70),
            android.graphics.Color.rgb(23, 105, 255),
            android.graphics.Color.rgb(105, 56, 239),
            android.graphics.Color.rgb(24, 200, 245),
            android.graphics.Color.rgb(46, 125, 50)
        )
        val size = 256
        for (i in 0 until layerCount) {
            val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(bmp).drawCircle(
                size / 2f, size / 2f, size / 2f - 4f,
                android.graphics.Paint().apply { color = colors[i % colors.size] }
            )
            val layer = com.flyerpix.editor.canvas.model.ImageLayer(bitmap = bmp, scale = 0.6f, layerName = "Stress#$i")
            layer.x = (60 + (i % 8) * 40).toFloat()
            layer.y = (80 + (i % 8) * 40).toFloat()
            pixelCanvasView.addLayer(layer)
        }
        if (mode != 1) {
            pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BRIGHTNESS, 25f)
            pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.CONTRAST, 30f)
            pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.SATURATION, 40f)
            if (mode == 0 || mode == 2) {
                pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, 12f)
                pixelCanvasView.toggleEffect(PixelCanvasView.CanvasEffect.FILTER)
            } else if (mode == 3) {
                pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, 12f)
            }
        }

        android.util.Log.d("FlyerPixProfile", "StressTest mode=$mode memulai dengan $layerCount layer")
        val jiggle = readSystemProperty("debug.flyerpix_stressjiggle") == "1"
        val jiggleLayer = if (jiggle) pixelCanvasView.layers.firstOrNull() else null
        var frames = 0
        val start = System.nanoTime()
        val renderer = object : Runnable {
            override fun run() {
                // Jiggle: ubah x layer tiap frame agar signature blur berubah
                // (menguji jalur rebuild, bukan cache hit).
                jiggleLayer?.let {
                    it.x = it.x + (frames % 3 - 2) * 2f
                }
                pixelCanvasView.invalidate()
                frames++
                val elapsed = (System.nanoTime() - start) / 1_000_000_000
                if (elapsed < 6) binding.pixelCanvasView.postOnAnimation(this) else {
                    android.util.Log.d(
                        "FlyerPixProfile",
                        "StressTest selesai: $frames frame dalam ${elapsed}s, layers=${pixelCanvasView.layers.size}, blurRebuilds=${pixelCanvasView.blurRebuildCount}"
                    )
                }
            }
        }
        binding.pixelCanvasView.postOnAnimation(renderer)
    }

    private fun updateCanvasCardMargin() {
        // Kesimpulan verifikasi empiris (09/2026): margin bawah canvasCard
        // diabaikan MotionLayout pada perangkat ini di semua cara mutasi
        // (layoutParams, clone/applyTo, setConstraintSet, atau scene_01).
        // Seluruh halaman menu bawah dibatasi setinggi ≤ ~595px sehingga kanvas
        // (dasar ≤ ~1651px) tidak pernah tertutup — fungsi ini hanya penjaga
        // agar MotionLayout melakukan pemerataan ulang.
        binding.canvasCard.post { binding.motionLayout.requestLayout() }
    }


    private fun initializeViewPager() {
        val pagerAdapter = ToolsViewPagerAdapter(supportFragmentManager, binding.toolsTabLayout.tabCount)
        binding.toolsViewPager.adapter = pagerAdapter
        binding.toolsViewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(binding.toolsTabLayout))
    }

    private fun initializeTabLayout() {
        binding.toolsTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.toolsViewPager.currentItem = tab!!.position
            }
        })
    }

    private fun setImage() {
        val imageExtra = intent.getStringExtra("image")
        if (!imageExtra.isNullOrEmpty()) {
            val uri = Uri.parse(imageExtra)
            val bmp = textPanelController.decodeBitmapFromUri(uri)
            if (bmp != null) {
                showImagePreEdit(bmp)
            }
        }
    }

    private class ToolsViewPagerAdapter(fm: FragmentManager, var tabsNum: Int) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            var fragment: Fragment? = null
            if (position == PAINT) {
                fragment = TabPaint()
            } else if (position == FIGURE) {
                fragment = TabFigure()
            } else if (position == STICKER) {
                fragment = TabSticker()
            }
            return fragment!!
        }

        override fun getCount(): Int {
            return tabsNum
        }
    }
}
