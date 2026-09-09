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
                        "TemplateTest navBefore=$before navAfter=$after expected=${R.id.nav_presets}" +
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
            onCanvasChanged = { updateCanvasCardMargin() },
            onEffectSettingsOpenChanged = { effectSettingsOpen ->
                val density = resources.displayMetrics.density
                val offset = (56 * density).toInt()
                animateNavTranslation(if (effectSettingsOpen) offset else 0)
                val collapsedH = (107 * density).toInt()
                if (effectSettingsOpen) {
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
                        .forEach { it.animateLayoutHeight(collapsedH) }
                } else {
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
                        .forEach {
                            it.animateLayoutHeight(collapsedH)
                            it.animateLayoutMarginBottom((56 * density).toInt())
                        }
                }
                updateCanvasCardMargin()
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
            { showSnackbar(it) }
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
            onPanelChanged = { updateCanvasCardMargin() },
            onShapeCreated = { shape -> shapePanelController.showShapeSettings(shape) }
        )
        objectMenu.initialize()
        objectMenu.onDetailExpandedChanged = { setDetailExpanded(objectMenu.activeTag.isNotEmpty()) }

        // Tap shape di kanvas (bukan drag) => navigasikan ke halaman Objek lalu buka Shape Settings
        pixelCanvasView.onShapeTapRequested = { shape ->
            binding.bottomNavigation.selectedItemId = R.id.nav_object
            objectMenu.select(ObjectMenuController.OBJ_SHAPES)
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
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
                        .forEach { it.animateLayoutHeight(collapsedH) }
                } else {
                    listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
                        .forEach {
                            it.animateLayoutHeight(collapsedH)
                            it.animateLayoutMarginBottom((56 * density).toInt())
                        }
                }
                updateCanvasCardMargin()
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
        Snackbar.make(binding.parentLayout, "Canvas size changed to $width × $height px ($ratio)", Snackbar.LENGTH_SHORT).show()
    }

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

        if (menuId != R.id.nav_text && textPanelController.isEffectSettingsOpen()) {
            textPanelController.cancelEffectSettings()
        }
        
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

        // Tutup panel Shape Settings jika pindah ke halaman selain Objek
        if (menuId != R.id.nav_object && ::shapePanelController.isInitialized) {
            shapePanelController.hideShapeSettings()
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
                    showSnackbar("Add a text layer first via + Add → Text")
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
            top.tvTopZoomLabel.alpha = if (percent == 100) 0.9f else 1f
        }

        updateZoomLabel()

        top.btnTopZoom.setOnClickListener {
            val expanded = top.tvTopZoomLabel.visibility != View.VISIBLE
            val visibility = if (expanded) View.VISIBLE else View.GONE
            top.btnTopZoomOut.visibility = visibility
            top.tvTopZoomLabel.visibility = visibility
            top.btnTopZoomIn.visibility = visibility
            top.btnTopZoom.contentDescription = if (expanded) {
                "Reset zoom to 100 percent"
            } else {
                "Show zoom controls"
            }
            if (expanded) {
                updateZoomLabel()
            } else {
                pixelCanvasView.resetZoom()
                updateZoomLabel()
                showSnackbar("Zoom set to 100%")
            }
        }

        top.btnTopZoomOut.setOnClickListener {
            pixelCanvasView.zoomOut()
            updateZoomLabel()
        }

        top.btnTopZoomIn.setOnClickListener {
            pixelCanvasView.zoomIn()
            updateZoomLabel()
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

        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)

        // Editing Operations
        popup.menu.add(0, 1, 0, "Edit Text")
        popup.menu.add(0, 2, 1, "Copy")
        popup.menu.add(0, 3, 2, "Size")
        popup.menu.add(0, 4, 3, "Rotate")
        popup.menu.add(0, 5, 4, "Alignment")
        popup.menu.add(0, 6, 5, "Color")

        // Z-Order Operations
        popup.menu.add(0, 7, 6, "To Front")
        popup.menu.add(0, 8, 7, "To Back")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    // Edit Teks
                    showEditTextDialog(textLayer)
                }
                2 -> {
                    // Copy text layer
                    pixelCanvasView.runRecordedAction("Copy Text") {
                        val copiedLayer = textLayer.copyLayer() as com.flyerpix.editor.canvas.model.TextLayer
                        copiedLayer.x += 30f
                        copiedLayer.y += 30f
                        pixelCanvasView.addLayer(copiedLayer)
                    }
                    pixelCanvasView.invalidate()
                    showSnackbar("Text layer copied")
                }
                3 -> {
                    // Size - switch to text panel
                    binding.bottomNavigation.selectedItemId = R.id.nav_text
                    showSnackbar("Adjust text size with the slider in the Text panel")
                }
                4 -> {
                    // Rotate - switch to text panel
                    binding.bottomNavigation.selectedItemId = R.id.nav_text
                    showSnackbar("Adjust text rotation with the slider in the Text panel")
                }
                5 -> {
                    // Alignment menu
                    showTextAlignmentMenu(anchor, textLayer)
                }
                6 -> {
                    // Color - switch to text panel
                    binding.bottomNavigation.selectedItemId = R.id.nav_text
                    showSnackbar("Choose text color in the Text panel")
                }
                7 -> {
                    // To Front
                    pixelCanvasView.runRecordedAction("Bring to Front") {
                        pixelCanvasView.bringLayerToFront(textLayer)
                    }
                    pixelCanvasView.invalidate()
                    showSnackbar("Layer brought to front")
                }
                8 -> {
                    // To Back
                    pixelCanvasView.runRecordedAction("Send to Back") {
                        pixelCanvasView.sendLayerToBack(textLayer)
                    }
                    pixelCanvasView.invalidate()
                    showSnackbar("Layer sent to back")
                }
            }
            true
        }
        popup.show()
    }

    /**
     * Show text alignment menu
     */
    private fun showTextAlignmentMenu(anchor: View, textLayer: com.flyerpix.editor.canvas.model.TextLayer) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)

        val alignments = listOf("Left", "Center", "Right")
        val alignValues = listOf(
            android.text.Layout.Alignment.ALIGN_NORMAL,
            android.text.Layout.Alignment.ALIGN_CENTER,
            android.text.Layout.Alignment.ALIGN_OPPOSITE
        )

        alignments.forEachIndexed { index, name ->
            popup.menu.add(0, index + 1, index, name)
        }

        popup.setOnMenuItemClickListener { item ->
            val align = alignValues.getOrNull(item.itemId - 1)
            if (align != null) {
                // Note: TextLayer doesn't have alignment, but we can add it as future feature
                showSnackbar("Alignment: ${alignments[item.itemId - 1]}")
            }
            true
        }
        popup.show()
    }

    private fun showTopAddMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Text")
        popup.menu.add(0, 2, 1, "Today's Date")
        popup.menu.add(0, 3, 2, "Sticker")
        popup.menu.add(0, 4, 3, "Shape")
        popup.menu.add(0, 5, 4, "From Gallery")
        popup.menu.add(0, 6, 5, "Free Draw")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> pixelCanvasView.addTextLayer("New Text")
                2 -> {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    pixelCanvasView.addTextLayer(date)
                }
                3 -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_object
                    objectMenu.select(ObjectMenuController.OBJ_STICKER)
                }
                4 -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_object
                    objectMenu.select(ObjectMenuController.OBJ_SHAPES)
                }
                5 -> preEditImageLauncher.launch("image/*")
                6 -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_object
                    objectMenu.select(ObjectMenuController.OBJ_DRAW)
                }
            }
            true
        }
        popup.show()
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
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Use image from gallery")
        popup.menu.add(0, 2, 1, "Use image from camera")
        popup.menu.add(0, 3, 2, "Export image")
        popup.menu.add(0, 4, 3, "Image size")
        popup.menu.add(0, 5, 4, "Background")
        popup.menu.add(0, 6, 5, "Clear canvas")
        popup.menu.add(0, 7, 6, "Save Project")
        popup.menu.add(0, 8, 7, "Open Project (.plp)")

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
    // dengan menggesernya keluar saat detail dibuka. Tinggi panel dihitung dari
    // ruang kosong di bawah canvas (PanelHeightManager) agar tidak menutupinya.
    // ────────────────────────────────────────────────────────────────────────

    private var navTranslationAnimator: android.animation.ValueAnimator? = null
    private val panelHeightAnimators = HashMap<View, android.animation.ValueAnimator>()

    /**
     * Mengekspansi panel detail (object/canvas/effects) dan menggeser keluar
     * bottom nav saat `expanded == true`; kembali normal saat `false`.
     *
     * Panel tertutup:   tinggi 107dp, marginBottom 56dp (di atas nav).
     * Panel terbuka:    tinggi dari ruang bawah canvas, marginBottom 0dp
     *                   (nav disembunyikan).
     */
    private fun setDetailExpanded(expanded: Boolean) {
        val density = resources.displayMetrics.density
        val collapsedH = (107 * density).toInt()
        val collapsedMargin = (56 * density).toInt()

        if (!expanded) {
            // Kontraksi: semua panel kembali ke ukuran default & nav dipanggil kembali.
            listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
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
        }

        // Tinggi expanded dihitung dinamis dari ruang kosong di bawah canvas
        // (PanelHeightManager), sehingga detail TIDAK menutupi canvas. Minimum
        // tidak di bawah tinggi collapsed agar tool strip tetap utuh.
        val expandedH = activePanel.let {
            val space = PanelHeightManager.anchorBottomInRoot(binding.parentLayout, 0) -
                PanelHeightManager.bottomInRoot(binding.canvasCard, binding.parentLayout)
            PanelHeightManager.safeDetailHeight(
                availableBelowCanvasPx = space,
                canvasHeightPx = pixelCanvasView.height,
                screenHeightPx = resources.displayMetrics.heightPixels,
                density = density,
            ).let { if (it >= collapsedH) it else collapsedH }
        }

        val navOffset = (56 * density).toInt()
        listOf(binding.objectMenuPanel, binding.canvasMenuPanel, binding.effectsMenuPanel)
            .filter { it != activePanel }
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
