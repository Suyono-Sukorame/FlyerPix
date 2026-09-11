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
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.flyerpix.editor.R
import com.flyerpix.editor.editableimageview.EditableImageView
import com.flyerpix.editor.editableimageview.EditorTool.PAINT
import com.flyerpix.editor.editableimageview.EditorTool.FIGURE
import kotlinx.coroutines.launch
import com.flyerpix.editor.editableimageview.EditorTool.STICKER
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import androidx.constraintlayout.widget.ConstraintSet

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
import com.flyerpix.editor.ui.controller.ObjectPanelController
import com.flyerpix.editor.ui.controller.ExportController
import com.flyerpix.editor.ui.controller.TextPanelController
import com.flyerpix.editor.ui.controller.CanvasMenuController
import com.flyerpix.editor.ui.controller.EffectsController
import com.flyerpix.editor.ui.controller.FontController
import com.flyerpix.editor.ui.controller.TemplateController
import com.flyerpix.editor.ui.controller.CanvasToolsController
import com.flyerpix.editor.ui.controller.ShapePanelController
import com.flyerpix.editor.ui.controller.PanelHeightManager
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
    private lateinit var objectPanelController: ObjectPanelController
    private lateinit var exportController: ExportController
    private lateinit var textPanelController: TextPanelController
    private lateinit var canvasMenuController: CanvasMenuController
    private lateinit var effectsController: EffectsController
    private lateinit var fontController: FontController
    private lateinit var templateController: TemplateController
    private lateinit var canvasToolsController: CanvasToolsController
    private lateinit var shapePanelController: ShapePanelController

    // ── Kanvas dimensi ──────────────────────────────────────────────────────
    private var canvasRatioW = 1
    private var canvasRatioH = 1


    private val texturePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Show loading indicator
            showLoadingDialog("Loading texture image...")
            
            lifecycleScope.launch {
                try {
                    val bmp = textPanelController.decodeBitmapFromUriAsync(uri, maxSize = 2048)
                    dismissLoadingDialog()
                    
                    if (bmp != null) {
                        textPanelController.applyTextureBitmap(bmp)
                    } else {
                        showSnackbar("Failed to load texture image.")
                    }
                } catch (e: Exception) {
                    dismissLoadingDialog()
                    showSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    private val customFontLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            fontController.handleCustomFontResults(uris)
        }
    }

    private val folderFontLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            fontController.handleFolderFontResult(uri)
        }
    }

    // ── Background Image Launchers (Prompt 45) ──────────────────────────────

    private var cameraPhotoUri: Uri? = null

    private val bgGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            showLoadingDialog("Loading background image...")
            
            lifecycleScope.launch {
                try {
                    val bmp = textPanelController.decodeBitmapFromUriAsync(uri, maxSize = 2048)
                    dismissLoadingDialog()
                    
                    if (bmp != null) {
                        pixelCanvasView.setImageBackground(bmp)
                        showSnackbar("Background image applied successfully!")
                    } else {
                        showSnackbar("Failed to load image from gallery.")
                    }
                } catch (e: Exception) {
                    dismissLoadingDialog()
                    showSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    private val bgCameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && cameraPhotoUri != null) {
            showLoadingDialog("Processing photo...")
            
            lifecycleScope.launch {
                try {
                    val bmp = textPanelController.decodeBitmapFromUriAsync(cameraPhotoUri!!, maxSize = 2048)
                    dismissLoadingDialog()
                    
                    if (bmp != null) {
                        pixelCanvasView.setImageBackground(bmp)
                        showSnackbar("Camera photo applied as background!")
                    } else {
                        showSnackbar("Failed to load camera photo.")
                    }
                } catch (e: Exception) {
                    dismissLoadingDialog()
                    showSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    // ── Pre-Edit Gambar (Foto / Galeri → Sesuaikan → menjadi Layer) ───────────

    private val preEditImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            showLoadingDialog("Loading image...")
            
            lifecycleScope.launch {
                try {
                    val bmp = textPanelController.decodeBitmapFromUriAsync(uri, maxSize = 2048)
                    dismissLoadingDialog()
                    
                    if (bmp != null) {
                        showImagePreEdit(bmp)
                    } else {
                        showSnackbar("Failed to load image from gallery.")
                    }
                } catch (e: Exception) {
                    dismissLoadingDialog()
                    showSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    private fun showImagePreEdit(bitmap: Bitmap) {
        ImagePreEditDialog.show(supportFragmentManager, bitmap) { result ->
            if (result != null) {
                addImageAsLayer(result)
                showSnackbar("Image added successfully!")
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
        val layer = ImageLayer(bitmap = bitmap, scale = scale, layerName = "Image")
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

        // ── Template smoke test (debug): `adb shell setprop debug.flyerpix_template 1`
        binding.pixelCanvasView.postDelayed({
            if (readSystemProperty("debug.flyerpix_template") == "1" && ::templateController.isInitialized) {
                templateController.applyTemplateByTitle("3D")
                val before = binding.bottomNavigation.selectedItemId
                binding.pixelCanvasView.postDelayed({
                    val after = binding.bottomNavigation.selectedItemId
                    android.util.Log.d(
                        "FlyerPixProfile",
                        "TemplateTest navBefore=$before navAfter=$after expected=${R.id.nav_home}" +
                            " selected=${pixelCanvasView.selectedLayer?.javaClass?.simpleName}"
                    )
                }, 600)
            }
        }, 2000)

        // ── Export smoke test (debug): `adb shell setprop debug.flyerpix_export 1`
        binding.pixelCanvasView.postDelayed({
            if (readSystemProperty("debug.flyerpix_export") == "1") {
                val format =
                    if (readSystemProperty("debug.flyerpix_exportformat") == "jpeg")
                        com.flyerpix.editor.canvas.model.ExportFormat.JPEG
                    else com.flyerpix.editor.canvas.model.ExportFormat.PNG
                binding.pixelCanvasView.exportHighResolutionAsync(
                    quality = com.flyerpix.editor.canvas.model.ExportQuality.ULTRA_HD,
                    format = format
                ) { uri ->
                    android.util.Log.d(
                        "FlyerPixProfile",
                        "ExportTest uri=${uri != null} ms=${binding.pixelCanvasView.shareExportMillis}"
                    )
                }
            }
        }, 2000)

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

        // ── Jaga kanvas utuh: tiap perubahan layout panel bawah, sesuaikan
        //    band yang direservasi sehingga kanvas mengecil & tidak pernah
        //    tertutup oleh menu (design A: auto-shrink canvas).
        if (::binding.isInitialized) {
            binding.parentLayout.viewTreeObserver.addOnGlobalLayoutListener {
                fitCanvasToOpenPanels()
            }
        }
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
            onFontRequested = { fontController.openFontPicker(it) },
            onCanvasChanged = { fitCanvasToOpenPanels() },
            onEffectSettingsOpenChanged = { effectSettingsOpen ->
                val density = resources.displayMetrics.density
                val offset = (56 * density).toInt()
                animateNavTranslation(if (effectSettingsOpen) offset else 0)
                val collapsedH = (107 * density).toInt()
                if (effectSettingsOpen) {
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel, binding.editContainerPanel)
                        .forEach { it.animateLayoutHeight(collapsedH) }
                } else {
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel, binding.editContainerPanel)
                        .forEach {
                            it.animateLayoutHeight(collapsedH)
                            it.animateLayoutMarginBottom((56 * density).toInt())
                        }
                }
                fitCanvasToOpenPanels()
            }
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
        fontController.setFolderFontLauncher(folderFontLauncher)

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

        // Shape Panel Controller - Mengelola Shape Settings panel (Initialize first)
        shapePanelController = ShapePanelController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) },
            onCanvasChanged = { fitCanvasToOpenPanels() }
        )

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
            onPanelChanged = { fitCanvasToOpenPanels() },
            onShapeCreated = { shape -> shapePanelController.showShapeSettings(shape) }
        )
        objectMenu.initialize()
        objectMenu.onDetailExpandedChanged = { setDetailExpanded(objectMenu.activeTag.isNotEmpty()) }

        // Tap shape di kanvas (bukan drag) => navigasikan ke tab Edit lalu buka Shape Settings
        pixelCanvasView.onShapeTapRequested = { shape ->
            binding.bottomNavigation.selectedItemId = R.id.nav_edit
            shapePanelController.showShapeSettings(shape)
        }

        // Object Panel Controller - Mengelola efek properti objek (Shape/Image/etc)
        objectPanelController = ObjectPanelController(
            this,
            binding,
            pixelCanvasView,
            { showSnackbar(it) },
            onEffectSettingsOpenChanged = { effectSettingsOpen ->
                val density = resources.displayMetrics.density
                val offset = (56 * density).toInt()
                animateNavTranslation(if (effectSettingsOpen) offset else 0)
                val collapsedH = (107 * density).toInt()
                if (effectSettingsOpen) {
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel, binding.editContainerPanel)
                        .forEach { it.animateLayoutHeight(collapsedH) }
                } else {
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel, binding.editContainerPanel)
                        .forEach {
                            it.animateLayoutHeight(collapsedH)
                            it.animateLayoutMarginBottom((56 * density).toInt())
                        }
                }
                fitCanvasToOpenPanels()
            }
        )
        objectPanelController.initialize()

        // Sambungkan tombol ✓/✕ Effect Settings (halaman yang sama dgn Text) ke ObjectPanelController
        textPanelController.onObjectEffectApply = { objectPanelController.applyEffectSettings() }
        textPanelController.onObjectEffectCancel = { objectPanelController.cancelEffectSettings() }
        textPanelController.isObjectEffectSettingsOpen = { objectPanelController.isEffectSettingsOpen() }

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
        pixelCanvasView.setTextEditMode(true)
        val dialog = com.flyerpix.editor.ui.dialog.EditTextDialog.show(
            this,
            textLayer.text,
            textLayer.richTextSpans
        ) { newText, spans ->
            pixelCanvasView.runRecordedAction("Edit Text") {
                textLayer.text = newText
                textLayer.richTextSpans = spans.toMutableList()
            }
            pixelCanvasView.invalidate()
        }
        dialog.setOnDismissListener {
            pixelCanvasView.setTextEditMode(false)
        }
    }

    private fun initializeSave() {
        binding.saveFab.setOnClickListener {
            exportController.showExportDialog()
        }
    }

    /**
     * Dialog "New Project": bersihkan kanvas dengan konfirmasi. Dipanggil dari
     * menu overflow top-bar (aksi cepat tab Home dipindah ke sana agar panel
     * Home tidak menutupi editor kanvas).
     */
    private fun showNewProjectDialog() {
        MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
            .setTitle("New Project")
            .setMessage("Start a new project? Current canvas will be cleared.")
            .setPositiveButton(R.string.yes) { _, _ ->
                pixelCanvasView.runWithLayerSelectSuppressed {
                    pixelCanvasView.clearLayers()
                }
                pixelCanvasView.invalidate()
                showSnackbar("New project started")
            }
            .setNegativeButton(R.string.no, null)
            .show()
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
        canvasRatioW = width
        canvasRatioH = height
        val ratio = canvasDimRatio()
        binding.motionLayout.getConstraintSet(R.id.start)?.setDimensionRatio(R.id.canvasCard, ratio)
        binding.motionLayout.getConstraintSet(R.id.end)?.setDimensionRatio(R.id.canvasCard, ratio)
        binding.motionLayout.requestLayout()
        fitCanvasToOpenPanels()
        Snackbar.make(binding.parentLayout, "Canvas size changed to $width × $height px ($ratio)", Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Rasio kanvas dengan dimensi yang "langka" sebagai acuan:
     * - Portrait (tinggi > lebar, mis. 9:16) → prefix "H," → tinggi dibatasi oleh
     *   margin bawah (area panel), lebar mengikuti. Margin bawah JADI berefek:
     *   kanvas mengecil saat panel bawah terbuka.
     * - Landscape / square → prefix "W," (proporsi square: lebar acuan, margin
     *   bawah tidak mengganggu).
     */
    private fun canvasDimRatio(): String =
        if (canvasRatioH > canvasRatioW) "H,${canvasRatioW}:${canvasRatioH}"
        else "W,${canvasRatioW}:${canvasRatioH}"

    override fun onBackPressed() {
        if (textPanelController.isEffectSettingsOpen()) {
            textPanelController.cancelEffectSettings()
            return
        }
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
        if (binding.bottomNavigation.selectedItemId != R.id.nav_home) {
            binding.bottomNavigation.selectedItemId = R.id.nav_home
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
        // Tutup halaman Effect Settings (teks maupun objek) saat pindah tab Edit.
        if (menuId != R.id.nav_edit && textPanelController.isEffectSettingsOpen()) {
            textPanelController.cancelEffectSettings()
        }
        if (menuId != R.id.nav_edit && objectPanelController.isEffectSettingsOpen()) {
            objectPanelController.cancelEffectSettings()
        }

        val pages = listOf(
            R.id.nav_home to binding.bottomControlPanelContainer,
            R.id.nav_add to binding.objectMenuPanel,
            R.id.nav_edit to binding.editContainerPanel,
            R.id.nav_canvas to binding.canvasMenuPanel,
            R.id.nav_effects to binding.effectsMenuPanel
        )
        if (pages.none { it.first == menuId }) return
        pages.forEach { (id, page) ->
            page.visibility = if (id == menuId) View.VISIBLE else View.GONE
        }

        // Tutup panel Shape Settings jika pindah ke halaman selain Add/Edit
        if (menuId != R.id.nav_add && menuId != R.id.nav_edit && ::shapePanelController.isInitialized) {
            shapePanelController.hideShapeSettings()
        }

        // Tab Edit bersifat kontekstual: mode bergantung tipe layer yang terpilih.
        val selected = pixelCanvasView.selectedLayer
        val isTextMode = menuId == R.id.nav_edit &&
            selected is com.flyerpix.editor.canvas.model.TextLayer && !selected.isLocked
        textPanelController.isPageOpen = isTextMode
        textPanelController.pagePinnedByNav = false
        applyEditContextVisuals(menuId)

        // Sheet lama dipensiunkan: tak pernah boleh muncul lagi di atas kanvas.
        binding.toolsBottomSheet.visibility = View.GONE
        if (::toolsBottomSheetBehavior.isInitialized) {
            toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        if (textPanelController.isPageOpen) {
            textPanelController.refreshUI()
        } else {
            textPanelController.hideStripAndPanels()
        }

        if (menuId == R.id.nav_add) {
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

        fitCanvasToOpenPanels()
    }

    /**
     * Atur tampilan tab Edit sesuai tipe layer terpilih:
     * - TextLayer → panel properti teks + tool strip teks.
     * - Layer objek lain → tool strip properti objek.
     * - Tidak ada / terkunci → empty state.
     */
    private fun applyEditContextVisuals(menuId: Int) {
        if (menuId != R.id.nav_edit) return
        val selected = pixelCanvasView.selectedLayer
        val isText = selected is com.flyerpix.editor.canvas.model.TextLayer
        val usable = selected != null && !selected.isLocked
        binding.editEmptyHint.visibility = if (usable) View.GONE else View.VISIBLE
        binding.editObjectBar.visibility = if (usable && !isText) View.VISIBLE else View.GONE
        binding.textEditorBar.visibility = if (usable && isText) View.VISIBLE else View.GONE
        if (usable && !isText) {
            binding.objectPropertyStripInclude.objectToolStripScroll.visibility = View.VISIBLE
        }
    }

    private fun initializeBottomNavigationView() {
        // Halaman awal: Home (default dari XML), sheet dipensiunkan.
        binding.toolsBottomSheet.visibility = View.GONE
        textPanelController.isPageOpen = false
        textPanelController.pagePinnedByNav = false

        binding.bottomNavigation.setOnItemSelectedListener { menuItem: MenuItem ->
            showMenu(menuItem.itemId)
            true
        }
        
        // Initialize dengan menu default (Home) untuk set FAB visibility dengan benar
        showMenu(R.id.nav_home)
    }

    private fun initializeAuthenticTopBar() {
        val top = binding.topBarInclude

        // Apply stylish Impact font to FlyerPix badge
        val badgeTypeface = android.graphics.Typeface.createFromAsset(assets, "fonts/impact.ttf")
        top.tvPixelLabBadge.typeface = badgeTypeface

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
                showSnackbar("Quote added successfully!")
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
                showSnackbar("Action undone (Undo)")
            } else {
                showSnackbar("No history to undo")
            }
        }

        top.btnTopRedo.setOnClickListener {
            val success = pixelCanvasView.redo()
            if (success) {
                showSnackbar("Action redone (Redo)")
            } else {
                showSnackbar("No history to redo")
            }
        }

        fun updateZoomLabel() {
            val percent = (pixelCanvasView.zoomLevel * 100f).toInt()
            top.tvTopZoomLabel.text = "$percent%"
            top.tvTopZoomLabel.alpha = if (percent == 100) 0.85f else 1f
        }

        fun applyZoomModeUi() {
            val active = pixelCanvasView.isEditorZoomMode
            top.tvTopZoomLabel.visibility = if (active) View.VISIBLE else View.GONE
            top.btnTopZoom.setBackgroundResource(
                if (active) R.drawable.bg_mode_pill_active else R.drawable.bg_mode_pill
            )
            top.btnTopZoom.imageTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(if (active) R.color.light_text_primary else android.R.color.white, theme)
            )
            top.btnTopZoom.contentDescription =
                if (active) "Exit zoom mode" else "Enter zoom mode"
        }

        updateZoomLabel()
        applyZoomModeUi()

        // Sinkronkan label persentase setiap kali canvasZoom berubah (pinch maupun reset).
        pixelCanvasView.onZoomChangedListener = { _ -> updateZoomLabel() }

        // PILL = saklar Edit Mode <-> Zoom Mode. Nilai zoom terakhir dipertahankan.
        top.btnTopZoom.setOnClickListener {
            val entering = !pixelCanvasView.isEditorZoomMode
            pixelCanvasView.setEditorZoomMode(entering)
            applyZoomModeUi()
            if (entering) {
                updateZoomLabel()
                showSnackbar("Zoom mode: pinch pada kanvas utk memperbesar. Tap % utk reset 100%")
            } else {
                showSnackbar("Edit mode")
            }
        }

        // Badge persentase di header (menampilkan % zoom) = tap utk reset zoom ke 100%
        top.tvTopZoomLabel.setOnClickListener {
            if (pixelCanvasView.isEditorZoomMode) {
                pixelCanvasView.resetZoom()
                updateZoomLabel()
                showSnackbar("Zoom reset to 100%")
            }
        }

        top.btnTopGrid.setOnClickListener {
            pixelCanvasView.isGridEnabled = !pixelCanvasView.isGridEnabled
            pixelCanvasView.invalidate()
            showSnackbar(if (pixelCanvasView.isGridEnabled) "Guide grid enabled" else "Guide grid disabled")
        }

        top.btnTopLayers.setOnClickListener {
            layerPanel.toggle()
        }

        // ── Context-Aware Header: Show/Hide Badge & Edit/Delete Buttons ──
        val prevLayerListener = pixelCanvasView.onLayerSelectedListener
        pixelCanvasView.onLayerSelectedListener = { layer ->
            prevLayerListener?.invoke(layer)
            updateHeaderForLayer(layer)
        }

        // Initial state
        updateHeaderForLayer(pixelCanvasView.selectedLayer)

        // Edit Text button
        top.btnTopEditText.setOnClickListener {
            showTextEditMenu(it)
        }

        // Delete Text button
        top.btnTopDeleteText.setOnClickListener {
            val textLayer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
            if (textLayer != null) {
                pixelCanvasView.runRecordedAction("Delete Text") {
                    pixelCanvasView.removeLayer(textLayer)
                }
                pixelCanvasView.invalidate()
                showSnackbar("Text layer deleted")
            }
        }
    }

    /**
     * Update header badge & text edit buttons berdasarkan layer yang sedang dipilih
     */
    private fun updateHeaderForLayer(layer: com.flyerpix.editor.canvas.model.CanvasLayer?) {
        val top = binding.topBarInclude

        if (layer is com.flyerpix.editor.canvas.model.TextLayer) {
            // Text editing mode: Show edit & delete buttons
            top.tvPixelLabBadge.visibility = View.GONE
            top.btnTopEditText.visibility = View.VISIBLE
            top.btnTopDeleteText.visibility = View.VISIBLE
        } else {
            // Photo/default mode: Show FlyerPix badge
            top.tvPixelLabBadge.visibility = View.VISIBLE
            top.btnTopEditText.visibility = View.GONE
            top.btnTopDeleteText.visibility = View.GONE
        }
    }

    /**
     * Show context menu untuk text layer editing operations
     */
    private fun showTextEditMenu(anchor: View) {
        val textLayer = pixelCanvasView.selectedLayer as? com.flyerpix.editor.canvas.model.TextLayer
        if (textLayer == null) {
            showSnackbar("No text layer selected")
            return
        }

        val density = resources.displayMetrics.density
        val iconSize = (48 * density).toInt()
        val pad = (12 * density).toInt()

        fun makeIcon(resId: Int, tooltip: String, onClick: () -> Unit): android.widget.ImageView {
            val icon = android.widget.ImageView(this).apply {
                setImageResource(resId)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    resources.getColor(R.color.light_text_primary, theme)
                )
                layoutParams = android.widget.FrameLayout.LayoutParams(iconSize, iconSize)
                    .apply { setMargins(pad / 2, pad / 2, pad / 2, pad / 2) }
                background = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                    this@EditorActivity, R.drawable.bg_quick_toolbar
                )?.apply { mutate(); alpha = 60 }
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                isClickable = true
                isFocusable = true
                contentDescription = tooltip
                androidx.appcompat.widget.TooltipCompat.setTooltipText(this, tooltip)
                setOnClickListener {
                    popup?.dismiss()
                    onClick()
                }
            }
            return icon
        }

        val row1 = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        val row2 = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        // Row 1: Edit Text, Copy, Size, Rotate
        row1.addView(makeIcon(R.drawable.ic_edit_24px, "Edit Text") {
            showEditTextDialog(textLayer)
        })
        row1.addView(makeIcon(R.drawable.ic_copy_24px, "Copy") {
            pixelCanvasView.runRecordedAction("Copy Text") {
                val copiedLayer = textLayer.copyLayer() as com.flyerpix.editor.canvas.model.TextLayer
                copiedLayer.x += 30f
                copiedLayer.y += 30f
                pixelCanvasView.addLayer(copiedLayer)
            }
            pixelCanvasView.invalidate()
            showSnackbar("Text layer copied")
        })
        row1.addView(makeIcon(R.drawable.ic_size_24px, "Size") {
            binding.bottomNavigation.selectedItemId = R.id.nav_edit
            showSnackbar("Adjust text size with the slider in the Edit panel")
        })
        row1.addView(makeIcon(R.drawable.ic_rotate_right_24px, "Rotate") {
            binding.bottomNavigation.selectedItemId = R.id.nav_edit
            showSnackbar("Adjust text rotation with the slider in the Edit panel")
        })

        // Row 2: Alignment, Color, To Front, To Back
        row2.addView(makeIcon(R.drawable.ic_align_left_24, "Alignment") {
            showAlignmentSubPopup(anchor, textLayer)
        })
        row2.addView(makeIcon(R.drawable.ic_sharp_palette_24px, "Color") {
            binding.bottomNavigation.selectedItemId = R.id.nav_edit
            showSnackbar("Choose text color in the Edit panel")
        })
        row2.addView(makeIcon(R.drawable.ic_bring_to_front_24px, "To Front") {
            pixelCanvasView.runRecordedAction("Bring to Front") {
                pixelCanvasView.bringLayerToFront(textLayer)
            }
            pixelCanvasView.invalidate()
            showSnackbar("Layer brought to front")
        })
        row2.addView(makeIcon(R.drawable.ic_send_to_back_24px, "To Back") {
            pixelCanvasView.runRecordedAction("Send to Back") {
                pixelCanvasView.sendLayerToBack(textLayer)
            }
            pixelCanvasView.invalidate()
            showSnackbar("Layer sent to back")
        })

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = 4 * density
            }
            elevation = 8 * density
            addView(row1)
            addView(row2)
        }

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val popupW = (48 * 4 + 12 * 3) * density
        var x = location[0] + anchor.width / 2 - (popupW / 2).toInt()
        val screenW = resources.displayMetrics.widthPixels
        val margin = (4 * density).toInt()
        x = x.coerceIn(margin, screenW - popupW.toInt() - margin)

        popup = android.widget.PopupWindow(
            container,
            popupW.toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 8 * density
            showAtLocation(anchor, android.view.Gravity.TOP or android.view.Gravity.START,
                x, location[1] + anchor.height + 4)
        }
    }

    private var popup: android.widget.PopupWindow? = null

    /**
     * Popup kecil untuk pilihan perataan teks (Left/Center/Right) — dibuka dari
     * ikon Alignment pada grid edit teks.
     */
    private fun showAlignmentSubPopup(anchor: View, textLayer: com.flyerpix.editor.canvas.model.TextLayer) {
        val density = resources.displayMetrics.density
        val iconSize = (44 * density).toInt()
        val pad = (10 * density).toInt()
        val alignments = listOf("Left", "Center", "Right")
        val alignmentIcons = listOf(
            R.drawable.ic_align_left_24, R.drawable.ic_align_center_24, R.drawable.ic_align_right_24
        )
        val alignmentValues = listOf(
            android.text.Layout.Alignment.ALIGN_NORMAL,
            android.text.Layout.Alignment.ALIGN_CENTER,
            android.text.Layout.Alignment.ALIGN_OPPOSITE
        )

        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        for (i in 0..2) {
            val icon = android.widget.ImageView(this).apply {
                setImageResource(alignmentIcons[i])
                imageTintList = android.content.res.ColorStateList.valueOf(
                    resources.getColor(R.color.light_text_primary, theme)
                )
                layoutParams = android.widget.FrameLayout.LayoutParams(iconSize, iconSize)
                    .apply { setMargins(pad / 2, pad / 2, pad / 2, pad / 2) }
                background = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                    this@EditorActivity, R.drawable.bg_quick_toolbar
                )?.apply { mutate(); alpha = 60 }
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                isClickable = true
                isFocusable = true
                contentDescription = alignments[i]
                androidx.appcompat.widget.TooltipCompat.setTooltipText(this, alignments[i])
                setOnClickListener {
                    val align = alignmentValues.getOrNull(i)
                    if (align != null) {
                        // Note: TextLayer belum punya properti alignment — snackbar saja
                        showSnackbar("Alignment: ${alignments[i]}")
                    }
                }
            }
            row.addView(icon)
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = 4 * density
            }
            elevation = 10 * density
            addView(row)
        }

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val popupW = (44 * 3 + 10 * 2) * density
        var x = location[0] + anchor.width / 2 - (popupW / 2).toInt()
        val screenW = resources.displayMetrics.widthPixels
        val margin = (4 * density).toInt()
        x = x.coerceIn(margin, screenW - popupW.toInt() - margin)

        android.widget.PopupWindow(
            container,
            popupW.toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 10 * density
            showAtLocation(anchor, android.view.Gravity.TOP or android.view.Gravity.START,
                x, location[1] + anchor.height + 4)
        }
    }

    private fun showTopAddMenu(anchor: View) {
        CompactPopupMenu(
            this,
            anchor,
            listOf("Text", "Today's Date", "Sticker", "Shape", "From Gallery", "Free Draw")
        ) { index, _ ->
            when (index) {
                0 -> pixelCanvasView.addTextLayer("New Text")
                1 -> {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    pixelCanvasView.addTextLayer(date)
                }
                2 -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_add
                    objectMenu.select(ObjectMenuController.OBJ_STICKER)
                }
                3 -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_add
                    objectMenu.select(ObjectMenuController.OBJ_SHAPES)
                }
                4 -> preEditImageLauncher.launch("image/*")
                5 -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_add
                    objectMenu.select(ObjectMenuController.OBJ_DRAW)
                }
            }
        }.show()
    }

    private fun showTopSaveMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Save as Project")
        popup.menu.add(0, 2, 1, "Save as Image")
        popup.menu.add(0, 3, 2, "Open Project (.plp)")

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
        // Hanya item unik: Export/Save/Open sudah ada di menu popup Save (ketas
        // dan tidak diduplikasi di sini agar popup tidak memenuhi layar).
        CompactPopupMenu(
            this,
            anchor,
            listOf(
                "Use image from gallery",
                "Use image from camera",
                "Image size",
                "Background",
                "Clear canvas",
                "New Project"
            )
        ) { index, _ ->
            when (index) {
                0 -> bgGalleryLauncher.launch("image/*")
                1 -> checkCameraPermissionForBackground()
                2 -> canvasMenuController.showImageSizeDialog()
                3 -> {
                    val sheet = com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.show(supportFragmentManager, pixelCanvasView)
                    sheet.onBackgroundImageRequested = { src ->
                        when (src) {
                            com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.Source.GALLERY -> bgGalleryLauncher.launch("image/*")
                            com.flyerpix.editor.ui.dialog.CanvasBackgroundBottomSheet.Source.CAMERA -> checkCameraPermissionForBackground()
                        }
                    }
                }
                4 -> pixelCanvasView.clearLayers()
                5 -> showNewProjectDialog()
            }
        }.show()
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
        binding.canvasCard.post { fitCanvasToOpenPanels() }
    }

    private fun exitSaveMode() {
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.bottomNavigation.translationY = 0f
        binding.saveFab.visibility = View.GONE
        binding.topBarInclude.root.visibility = View.VISIBLE
        binding.nameTextInputLayout.visibility = View.GONE
        val current = binding.bottomNavigation.selectedItemId
        val known = listOf(R.id.nav_home, R.id.nav_add, R.id.nav_edit, R.id.nav_canvas, R.id.nav_effects).contains(current)
        showMenu(if (known) current else R.id.nav_home)
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

    // ── Loading Dialog untuk async operations ───────────────────────────────
    
    private var loadingDialog: android.app.ProgressDialog? = null
    
    private fun showLoadingDialog(message: String) {
        dismissLoadingDialog() // Dismiss any existing
        loadingDialog = android.app.ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }
    
    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun initializeBottomSheetBehavior() {
        toolsBottomSheetBehavior = BottomSheetBehavior.from(binding.toolsBottomSheet)
        toolsBottomSheetBehavior.addBottomSheetCallback(createBottomSheetCallback()!!)
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
    // DETAIL EXPAND: memperluas panel detail halaman menu; bottom nav dihindari
    // dengan menggesernya keluar saat detail dibuka. Tinggi panel = natural konten
    // (Design A); kanvas di-fit-ulang agar tidak pernah tertutup.
    // ────────────────────────────────────────────────────────────────────────

    private var navTranslationAnimator: android.animation.ValueAnimator? = null
    private val panelHeightAnimators = HashMap<View, android.animation.ValueAnimator>()
    private var isDetailExpanded = false

    /**
     * Mengekspansi panel detail (object/canvas/effects) dan menggeser keluar
     * bottom nav saat `expanded == true`; kembali normal saat `false`.
     *
     * Paradigma Design A: tinggi panel TIDAK lagi diukur dari ruang sisa di bawah
     * kanvas (yang untuk flyer 9:16 nyaris nol). Panel memakai tinggi natural dari
     * kontennya, dan kanvas justru mengecil di-fit di atas panel lewat
     * [fitCanvasToOpenPanels].
     */
    private fun setDetailExpanded(expanded: Boolean) {
        isDetailExpanded = expanded
        val density = resources.displayMetrics.density
        val collapsedH = (107 * density).toInt()
        val collapsedMargin = (56 * density).toInt()
        val navOffset = (56 * density).toInt()

        if (!expanded) {
            // Kontraksi: semua panel kembali ke ukuran default & nav dipanggil kembali.
            // Kanvas DIPERTAHANKAN kecil selama panel menyusut agar tidak tertutup,
            // lalu di-fit-kan ulang ke ruang baru setelah animasi selesai.
            listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel, binding.editContainerPanel)
                .forEach { panel ->
                    panel.animateLayoutHeight(collapsedH)
                    panel.animateLayoutMarginBottom(collapsedMargin)
                }
            animateNavTranslation(0)
            binding.root.postDelayed({ fitCanvasToOpenPanels() }, 320)
            return
        }

        // Tab Edit memakai tinggi natural (wrap_content + maxHeight), tidak perlu ekspansi.
        if (binding.bottomNavigation.selectedItemId == R.id.nav_edit) return

        val activePanel = when (binding.bottomNavigation.selectedItemId) {
            R.id.nav_add -> binding.objectMenuPanel
            R.id.nav_canvas -> binding.canvasMenuPanel
            R.id.nav_effects -> binding.effectsMenuPanel
            else -> return
        }

        // Tinggi expanded diambil dari tinggi natural konten (bukan ruang sisa
        // di bawah kanvas), di-clamp agar masih menyisakan ruang untuk kanvas.
        listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel, binding.editContainerPanel)
            .filter { it != activePanel }
            .forEach { panel -> panel.animateLayoutHeight(collapsedH) }
        animateNavTranslation(navOffset)

        activePanel.post {
            if (!isDetailExpanded) return@post
            val expandedH = contentBasedExpandedHeight(activePanel)
            activePanel.animateLayoutHeight(expandedH)
            activePanel.animateLayoutMarginBottom(0)
            // Pre-fit kanvas ke boundary akhir panel SEBELUM animasi berjalan,
            // sehingga panel tidak pernah sempat menutupi kanvas.
            fitCanvasAbove(binding.parentLayout.height - expandedH)
        }
        // Koreksi final dari posisi nyata setelah layout/animasi selesai.
        binding.root.postDelayed({ fitCanvasToOpenPanels() }, 320)
    }

    /** Tinggi natural panel detail sesuai kontennya, di-clamp terhadap kapasitas layar. */
    private fun contentBasedExpandedHeight(panel: View): Int {
        val density = resources.displayMetrics.density
        val screenH = resources.displayMetrics.heightPixels
        val cap = (screenH * 0.46f).toInt()
        val collapsedH = (107 * density).toInt()

        val contentScroll = when (panel) {
            binding.objectMenuPanel -> binding.objectContentPanel
            binding.canvasMenuPanel -> binding.canvasContentPanel
            binding.effectsMenuPanel -> binding.effectContentPanel
            else -> null
        }
        var contentH = 0
        if (contentScroll != null && contentScroll.childCount > 0) {
            val child = contentScroll.getChildAt(0)
            if (child != null && child.visibility != View.GONE) {
                if (contentScroll.width > 0) {
                    child.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(contentScroll.width, android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                    )
                    contentH = child.measuredHeight + child.paddingTop + child.paddingBottom
                } else if (child.height > 0) {
                    contentH = child.height + child.paddingTop + child.paddingBottom
                }
            }
        }
        if (contentH <= 0) return cap

        val stripH = when (panel) {
            binding.objectMenuPanel -> binding.objectToolStripInclude.root.height
            binding.canvasMenuPanel -> binding.canvasToolStripInclude.root.height
            binding.effectsMenuPanel -> binding.effectToolStripInclude.root.height
            else -> 0
        }
        val natural = contentH + (if (stripH > 0) stripH else (62 * density).toInt())
        return natural.coerceIn(collapsedH, cap)
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
     * Mode: `debug.flyerpix_stressmode` 0=all, 1=layer saja, 2=blur+adjust, 3=blur saja,
     * 4=blur+noise+vignette.
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
            } else if (mode == 4) {
                pixelCanvasView.setAdjustment(PixelCanvasView.CanvasAdjustment.BLUR, 12f)
                pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.NOISE, true)
                pixelCanvasView.setEffectEnabled(PixelCanvasView.CanvasEffect.VIGNETTE, true)
            }
        }

        android.util.Log.d("FlyerPixProfile", "StressTest mode=$mode starting with $layerCount layers")
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
                        "StressTest finished: $frames frames in ${elapsed}s, layers=${pixelCanvasView.layers.size}, blurRebuilds=${pixelCanvasView.blurRebuildCount}"
                    )
                }
            }
        }
        binding.pixelCanvasView.postOnAnimation(renderer)
    }

/**
     * Design A - Auto-shrink canvas (pola Canva/PicsArt).
     *
     * Satu-satunya titik sinkronisasi semua menu bawah: setiap kali salah satu
     * panel bawah berubah (buka/tutup/resize), kanvas di-fit-kan ulang agar:
     *   - Selalu tampil UTUH (tanpa tertutup) dengan rasio tetap 9:16 dll,
     *   - Menaati ruang di atas panel paling tinggi yang sedang tampil,
     *   - Kembali ke ukuran penuh saat tidak ada panel bawah.
     *
     * Posisi panel dibaca dari koordinat NYATA (getLocationInWindow), sehingga
     * otomatis mengikuti animasi nav/panel dan margin yang sedang berubah.
     */
    private var canvasFitPending = false
    private var lastFullRatio: String? = null

    private fun fitCanvasToOpenPanels(attempt: Int = 0) {
        if (canvasFitPending) return
        val root = binding.parentLayout
        canvasFitPending = true
        binding.root.post {
            canvasFitPending = false
            if (root.width <= 0 || root.height <= 0) return@post
            // Tunggu transisi MotionLayout (save mode) selesai agar ukuran kanvas
            // yang kita set tidak di-overwrite mid-transition.
            if (binding.motionLayout.progress != 0f) {
                if (attempt < 30) {
                    binding.root.postDelayed({ fitCanvasToOpenPanels(attempt + 1) }, 60)
                }
                return@post
            }
            val panels = visibleBottomPanels()
            if (panels.isEmpty()) {
                restoreFullCanvas()
                return@post
            }
            val boundary = panels
                .asSequence()
                .filter { it.height > 0 }
                .map { PanelHeightManager.topInRoot(it, root) }
                .minOrNull()

            if (boundary == null) {
                // Panel belum terukur (baru tampil). Coba lagi sebentar.
                if (attempt < 12) {
                    binding.root.postDelayed({ fitCanvasToOpenPanels(attempt + 1) }, 80)
                } else {
                    restoreFullCanvas()
                }
            } else {
                // Jika kanvas ukuran penuh (rasio flyer) SUDAH muat di atas panel
                // paling tinggi, tidak perlu mengecilkannya (mis. carousel presets
                // yang pendek). Mengecil hanya saat panel benar-benar menutupi kanvas.
                val density = resources.displayMetrics.density
                val topMarginPx = (8 * density).toInt()
                val gapPx = (8 * density).toInt()
                val motionTop = PanelHeightManager.topInRoot(binding.motionLayout, root)
                val naturalH = (root.width.toLong() *
                    kotlin.math.max(1, pixelCanvasView.canvasHeight) /
                    kotlin.math.max(1, pixelCanvasView.canvasWidth)).toInt().coerceAtLeast(1)
                val naturalCanvasBottom = motionTop + topMarginPx + naturalH
                if (naturalCanvasBottom + gapPx <= boundary) {
                    restoreFullCanvas()
                } else {
                    fitCanvasAbove(boundary)
                }
            }
        }
    }

    /** Panel-panel bawah yang sedang menempati ruang layar. */
    private fun visibleBottomPanels(): List<View> = listOf(
        binding.bottomControlPanelContainer,
        binding.editContainerPanel,
        binding.objectMenuPanel,
        binding.canvasMenuPanel,
        binding.effectsMenuPanel,
        binding.shapeSettingsPanel.root,
        binding.effectSettingsInclude.root,
        binding.composeThreeDSheetContainer,
    ).filter { it.visibility == View.VISIBLE }

    /** Fit kanvas (rasio flyer) agar muat di atas tepi atas panel [boundaryTopPx]. */
    private fun fitCanvasAbove(boundaryTopPx: Int) {
        val root = binding.parentLayout
        val density = resources.displayMetrics.density
        val topMargin = (8 * density).toInt()
        val gap = (8 * density).toInt()
        // Kanvas berada di dalam motionLayout yang dimulai di bawah top bar, sehingga
        // batas atas panel dipetakan dahulu ke koordinat relative motionLayout.
        val motionTop = PanelHeightManager.topInRoot(binding.motionLayout, root)
        val panelTopInMotion = boundaryTopPx - motionTop
        val (w, h) = PanelHeightManager.fitCanvasSize(
            topBoundaryPx = panelTopInMotion,
            topMarginPx = topMargin,
            gapPx = gap,
            screenWidthPx = root.width,
            ratioW = pixelCanvasView.canvasWidth,
            ratioH = pixelCanvasView.canvasHeight,
        )
        if (w <= 0 || h <= 0) return
        applyCanvasSize(w, h, topMargin, binding.motionLayout.height)
    }

    /**
     * Terapkan ukuran kanvas ke canvasCard. Dua jalur agar robust:
     *  1) layoutParams eksplisit (primer, deterministik karena tidak ada rasio).
     *  2) ConstraintSet start (jaga-jaga & agar MotionLayout konsisten).
     */
    private fun applyCanvasSize(w: Int, h: Int, topMarginPx: Int, parentHeightPx: Int) {
        val bottomMargin = (parentHeightPx - topMarginPx - h).coerceAtLeast(0)
        val lp = binding.canvasCard.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        // Idempoten: skip bila ukuran & margin sudah persis sama. Tanpa guard ini
        // setCanvasSize + requestLayout tiap pass layout → feedback-loop tak berujung.
        if (lp != null && lp.width == w && lp.height == h &&
            lp.topMargin == topMarginPx && lp.bottomMargin == bottomMargin
        ) {
            return
        }
        val cs = binding.motionLayout.getConstraintSet(R.id.start)
        if (cs != null) {
            cs.constrainWidth(R.id.canvasCard, w)
            cs.constrainHeight(R.id.canvasCard, h)
            cs.setVerticalBias(R.id.canvasCard, 0f)
            cs.setHorizontalBias(R.id.canvasCard, 0.5f)
            cs.setMargin(R.id.canvasCard, ConstraintSet.START, 0)
            cs.setMargin(R.id.canvasCard, ConstraintSet.END, 0)
            cs.setMargin(R.id.canvasCard, ConstraintSet.TOP, topMarginPx)
            cs.setMargin(R.id.canvasCard, ConstraintSet.BOTTOM, bottomMargin)
        }
        if (lp != null) {
            lp.width = w
            lp.height = h
            lp.topMargin = topMarginPx
            lp.bottomMargin = bottomMargin
            lp.leftMargin = 0
            lp.rightMargin = 0
            binding.canvasCard.layoutParams = lp
        }
        binding.motionLayout.requestLayout()
    }

    /** Kembalikan kanvas ke ukuran penuh (rasio lewat constraint set asli scene_01). */
    private fun restoreFullCanvas() {
        val density = resources.displayMetrics.density
        val topMargin = (8 * density).toInt()
        val bottomMargin = (56 * density).toInt()
        val ratio = "${pixelCanvasView.canvasWidth}:${pixelCanvasView.canvasHeight}"
        // Idempoten: kanvas sudah full & rasio masih sama → tidak perlu requestLayout.
        // Mencegah feedback-loop layout yang sama seperti applyCanvasSize.
        val lp = binding.canvasCard.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (lp != null && lp.width == 0 && lp.height == 0 &&
            lp.topMargin == topMargin && lp.bottomMargin == bottomMargin &&
            lastFullRatio == ratio
        ) {
            return
        }
        val cs = binding.motionLayout.getConstraintSet(R.id.start)
        if (cs != null) {
            cs.constrainWidth(R.id.canvasCard, 0)
            cs.constrainHeight(R.id.canvasCard, 0)
            cs.setDimensionRatio(R.id.canvasCard, ratio)
            cs.setVerticalBias(R.id.canvasCard, 0.5f)
            cs.setHorizontalBias(R.id.canvasCard, 0.5f)
            cs.setMargin(R.id.canvasCard, ConstraintSet.START, 0)
            cs.setMargin(R.id.canvasCard, ConstraintSet.END, 0)
            cs.setMargin(R.id.canvasCard, ConstraintSet.TOP, topMargin)
            cs.setMargin(R.id.canvasCard, ConstraintSet.BOTTOM, bottomMargin)
        }
        if (lp != null) {
            lp.width = 0
            lp.height = 0
            lp.topMargin = topMargin
            lp.bottomMargin = bottomMargin
            lp.leftMargin = 0
            lp.rightMargin = 0
            binding.canvasCard.layoutParams = lp
        }
        lastFullRatio = ratio
        binding.motionLayout.requestLayout()
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

    @Suppress("DEPRECATION")
    private class ToolsViewPagerAdapter(fm: FragmentManager, var tabsNum: Int) : androidx.fragment.app.FragmentPagerAdapter(fm) {
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
