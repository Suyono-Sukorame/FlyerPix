# 50 Prompt Bertahap untuk Refaktor Proyek Menjadi "PixelLab - Text on Pictures"

Kumpulan 50 prompt ini dirancang secara terstruktur dan berurutan (*step-by-step*) agar Anda dapat merefaktor proyek **Pixel-Lab** saat ini dari editor foto sederhana menjadi aplikasi desain grafis & tipografi canggih seperti **PixelLab** (oleh App Holdings).

---

## 📑 Daftar Isi Milestone
* **Milestone 1**: Fondasi Arsitektur Canvas & Sistem Layer Non-Destruktif (Prompt 1 – 8)
* **Milestone 2**: Rich Text & Typography Engine (Prompt 9 – 18)
* **Milestone 3**: Efek 3D Teks & Transformasi Perspektif (Prompt 19 – 24)
* **Milestone 4**: Interactive Bounding Box & Transform Handles (Prompt 25 – 30)
* **Milestone 5**: Layer Manager UI / Pengelola Lapisan (Prompt 31 – 36)
* **Milestone 6**: Shapes, Stickers, & Pen Drawing Tool (Prompt 37 – 42)
* **Milestone 7**: Canvas Aspect Ratio & Background Engine (Prompt 43 – 46)
* **Milestone 8**: File Proyek (.PLP), Ultra HD Export & Polish (Prompt 47 – 50)
* **Milestone Revisi**: Authentic PixelLab Floating Layer Panel & 2x2 Grid Action Card (Prompt Revisi 1 – 3)

---

## 🏛️ Milestone 1: Fondasi Arsitektur Canvas & Sistem Layer Non-Destruktif

### Prompt 1: Membuat Arsitektur Abstract Base Layer
> "Saya sedang merefaktor aplikasi Android Pixel-Lab menjadi aplikasi berbasis layer non-destruktif seperti PixelLab. Buatlah kelas abstract `CanvasLayer` di package `com.pixellab.photoeditor.canvas.model`. Kelas ini harus memiliki properti dasar: `id: String`, `x: Float`, `y: Float`, `scale: Float`, `rotation: Float`, `opacity: Int`, `isLocked: Boolean`, `isVisible: Boolean`, dan metode abstract `fun draw(canvas: Canvas, paint: Paint)` serta `fun getBounds(): RectF`. Sertakan juga salinan clone/copy function untuk keperluan duplikasi layer."

### Prompt 2: Membuat Custom Canvas View (`PixelCanvasView`)
> "Gantikan kelas `EditableImageView` dengan membuat custom view baru `PixelCanvasView` turunan dari `View`. View ini harus mengelola daftar layer `val layers = mutableListOf<CanvasLayer>()` dan indeks layer yang sedang aktif `var selectedLayer: CanvasLayer?`. Implementasikan fungsi `onDraw` yang merender semua layer berurutan berdasarkan z-index jika layer `isVisible == true`."

### Prompt 3: Implementasi Single-Touch Selection pada Canvas
> "Di dalam `PixelCanvasView`, implementasikan logika sentuhan `onTouchEvent` untuk mendeteksi layer mana yang di-tap oleh pengguna. Cari layer teratas (z-index paling tinggi) yang titik sentuhnya `(event.x, event.y)` berada di dalam `getBounds()`. Jika ditemukan dan tidak dalam keadaan `isLocked`, jadikan layer tersebut sebagai `selectedLayer` lalu trigger `invalidate()`."

### Prompt 4: Implementasi Drag / Translasi Layer Terpilih
> "Tambahkan kemampuan menggeser (*drag/translate*) layer terpilih pada `PixelCanvasView`. Saat `ACTION_DOWN` mengenai layer terpilih, catat koordinat awal. Pada `ACTION_MOVE`, hitung delta perpindahan `dx` dan `dy`, lalu perbarui nilai `x` dan `y` dari layer tersebut secara mulus."

### Prompt 5: Multi-Touch Pinch-to-Zoom & Scale Layer
> "Integrasikan `ScaleGestureDetector` ke dalam `PixelCanvasView` agar pengguna bisa memperbesar/memperkecil (*scale*) layer yang sedang dipilih menggunakan gestur cubit dua jari (pinch-to-zoom). Pastikan penskalaan dilakukan dengan titik pusat rotasi/skala di tengah bounding box layer."

### Prompt 6: Multi-Touch Dua Jari untuk Rotasi Layer
> "Buatlah helper class `RotationGestureDetector` di package `com.pixellab.photoeditor.canvas.gesture`. Pasang detector ini pada `PixelCanvasView` agar ketika pengguna memutar dua jari di atas layer terpilih, nilai sudut `rotation` dari layer tersebut ikut berputar secara real-time dan akurat."

### Prompt 7: Canvas State & Background Checkerboard (Transparansi)
> "Buatlah render background kanvas di `PixelCanvasView` yang mendukung mode transparan. Jika background kanvas diset transparan, gambar pola kotak-kotak catur abu-abu putih (*checkerboard pattern*) seperti pada Photoshop atau PixelLab sebelum merender layer-layer di atasnya."

### Prompt 8: Integrasi `PixelCanvasView` ke dalam `activity_editor.xml`
> "Perbarui layout `activity_editor.xml` untuk menggantikan tag custom view lama dengan `<com.pixellab.photoeditor.canvas.PixelCanvasView android:id="@+id/pixelCanvasView" .../>`. Pastikan kanvas berada di tengah layar dengan background card yang rapi dan terisolasi dari bottom sheet kontrol."

---

## 🔤 Milestone 2: Rich Text & Typography Engine

### Prompt 9: Membuat Model `TextLayer`
> "Buatlah data class `TextLayer` turunan dari `CanvasLayer` di `com.pixellab.photoeditor.canvas.model`. Kelas ini harus menyimpan properti: `text: String`, `textSize: Float`, `textColor: Int`, `typeface: Typeface?`, `letterSpacing: Float`, `lineSpacing: Float`, `alignment: Layout.Alignment`, `isBold: Boolean`, `isItalic: Boolean`, `isUnderline: Boolean`. Implementasikan fungsi `draw` menggunakan `StaticLayout` atau `Canvas.drawText`."

### Prompt 10: Dialog Edit Teks Interaktif
> "Buatlah dialog pop-up `EditTextDialog` menggunakan Material Design. Dialog ini muncul ketika layer teks di-double-tap atau tombol 'Edit Teks' ditekan. Sediakan input `EditText` multi-line, tombol 'Batal', tombol 'OK', dan tombol cepat untuk mengubah teks menjadi ALL CAPS atau lowercase."

### Prompt 11: Font Manager Bawaan (10+ Font Populer)
> "Buatlah objek `FontManager` yang memuat font bawaan dari folder `app/src/main/assets/fonts/`. Sediakan font bergaya Modern, Serif, Sans-Serif, Hand-written, dan Bold Display. Buat RecyclerView horizontal di panel kontrol bawah untuk memilih font teks dengan preview visual masing-masing font."

### Prompt 12: Custom Font Loader (.TTF / .OTF dari Memori HP)
> "Tambahkan fitur di `FontManager` dan UI untuk mengizinkan pengguna memilih file font `.ttf` atau `.otf` dari penyimpanan internal HP menggunakan `ActivityResultContracts.GetContent()`. Buat typeface dari URI file tersebut dan simpan ke tab 'My Fonts' agar bisa digunakan pada `TextLayer`."

### Prompt 13: Text Stroke / Outline Engine
> "Tambahkan fitur stroke/garis tepi pada `TextLayer`. Sediakan properti `strokeColor: Int` dan `strokeWidth: Float`. Di method `draw()`, gambar stroke terlebih dahulu menggunakan `Paint.Style.STROKE` lalu timpa di atasnya dengan warna isi teks menggunakan `Paint.Style.FILL`."

### Prompt 14: Drop Shadow & Blur Radius
> "Tambahkan efek Drop Shadow pada `TextLayer`. Properti yang dibutuhkan: `shadowColor: Int`, `shadowRadius: Float`, `shadowDx: Float`, `shadowDy: Float`, dan `shadowOpacity: Float`. Terapkan menggunakan `paint.setShadowLayer` dengan toggle switch enable/disable pada panel bawah."

### Prompt 15: Inner Shadow (Bayangan ke Bagian Dalam Teks)
> "Implementasikan efek Inner Shadow pada `TextLayer` menggunakan teknik clipping mask (`canvas.clipRect` / `PorterDuffXfermode`). Bayangan harus muncul di bagian dalam kurva huruf, memberikan kedalaman visual seperti fitur Inner Shadow di PixelLab asli."

### Prompt 16: Efek Emboss & Bevel pada Teks
> "Tambahkan fitur efek Emboss pada `TextLayer`. Gunakan efek pencahayaan sudut (*light angle*), intensitas ambient, dan specular highlight menggunakan `MaskFilter` (`EmbossMaskFilter`) atau shader normal mapping sederhana agar huruf tampak timbul dan bersinar seperti logam/plastik 3D."

### Prompt 17: Linear & Radial Gradient Fill untuk Teks
> "Buat opsi pewarnaan teks menggunakan gradasi warna (`LinearGradient` dan `SweepGradient`). Buat data model `GradientColor` (koleksi warna hex dan stop posisi). Pasang `paint.shader = LinearGradient(...)` di `TextLayer` saat mode gradasi aktif."

### Prompt 18: Texture Masking (Mengisi Teks dengan Foto dari Galeri)
> "Tambahkan fitur 'Texture' pada `TextLayer`. Izinkan pengguna memilih foto dari galeri HP, lalu gunakan gambar tersebut sebagai `BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT)` pada teks. Sediakan kontrol rasio skala tekstur (*scale texture*) dan opsi rotasi tekstur."

---

## 🧊 Milestone 3: Efek 3D Teks & Transformasi Perspektif

### Prompt 19: 3D Text Extrusion (Efek Kedalaman Teks 3D)
> "Implementasikan efek teks 3D (*Extrude*) pada `TextLayer`. Berikan opsi `depth: Int` (1 s/d 50), `depthColor: Int`, dan arah bayangan kedalaman (Oblique / Isometric). Gambar layer teks berulang kali secara bertumpuk dengan offset 1 pixel ke arah sudut kedalaman untuk menghasilkan volume 3D tebal."

### Prompt 20: 3D Rotate (Rotasi Sumbu X dan Y)
> "Tambahkan kemampuan rotasi 3D pada `TextLayer` menggunakan matriks Android `android.graphics.Camera`. Pengguna harus bisa memutar teks pada sumbu X (kemiringan atas-bawah) dan sumbu Y (kemiringan kiri-kanan) secara dinamis menggunakan slider seekbar."

### Prompt 21: Curved / Arc Text (Teks Melingkar)
> "Implementasikan fitur teks melingkar (*Curved Text*) pada `TextLayer`. Buat `Path.addArc` dan gunakan `canvas.drawTextOnPath(text, path, hOffset, vOffset, paint)`. Buat slider dengan rentang -100% (melengkung ke bawah) hingga +100% (melengkung ke atas)."

### Prompt 22: Perspektif Warping pada Layer
> "Tambahkan transformasi perspektif pada layer menggunakan metode `Matrix.setPolyToPoly`. Pengguna dapat menggeser 4 titik sudut untuk memposisikan teks atau stiker agar pas menempel pada bidang miring (seperti teks di tembok miring atau billboard jalanan)."

### Prompt 23: Text Spacing (Spasi Huruf & Baris)
> "Tambahkan kontrol tipografi lanjut pada `TextLayer`: Spasi antar huruf (*Kerning / Letter Spacing*) dan Spasi antar baris (*Line Spacing / Leading*). Pasang slider kontrol di bottom sheet teks dan hubungkan nilainya secara reaktif ke canvas."

### Prompt 24: Blending Modes (Multiply, Screen, Overlay)
> "Tambahkan properti `blendMode: PorterDuff.Mode` pada `CanvasLayer`. Sediakan pilihan blending mode populer di UI (Normal, Multiply, Screen, Overlay, Darken, Lighten) menggunakan `paint.xfermode = PorterDuffXfermode(mode)`."

---

## 🔲 Milestone 4: Interactive Bounding Box & Transform Handles

### Prompt 25: Menggambar Bounding Box Seleksi
> "Di `PixelCanvasView`, ketika ada layer yang aktif (`selectedLayer != null`), gambar kotak pembatas putus-putus (*dashed rectangle*) di sekeliling layer tersebut beserta margin padding secukupnya."

### Prompt 26: Menambahkan 4 Tombol Handle Sudut Interaktif
> "Gambar 4 ikon tombol handle di setiap sudut Bounding Box:
> 1. Sudut kanan bawah: Handle Scale / Resize (ikon panah diagonal).
> 2. Sudut kiri bawah: Handle Rotate (ikon panah melingkar).
> 3. Sudut kanan atas: Handle Delete (ikon silang merah).
> 4. Sudut kiri atas: Handle Duplicate (ikon copy)."

### Prompt 27: Sentuhan pada Handle Scale & Resize
> "Di `PixelCanvasView.onTouchEvent`, deteksi jika titik sentuh pengguna mengenai area tombol handle kanan bawah. Jika tersentuh, ubah state menjadi `DRAGGING_SCALE_HANDLE`, dan saat jari digerakkan menjauh/mendekat dari titik pusat layer, sesuaikan nilai `scale` atau `textSize` secara proporsional."

### Prompt 28: Sentuhan pada Handle Rotate
> "Di `PixelCanvasView.onTouchEvent`, deteksi jika pengguna menyentuh handle kiri bawah. Hitung sudut antara titik pusat layer dengan koordinat sentuhan jari menggunakan `Math.atan2`, dan perbarui nilai `layer.rotation` secara mulus."

### Prompt 29: Handle Delete & Duplicate
> "Implementasikan aksi instan ketika handle delete (kanan atas) ditekan: hapus layer dari `layers`, set `selectedLayer = null`, dan redraw canvas. Ketika handle duplicate (kiri atas) ditekan: kloning layer tersebut dengan offset `(x + 30, y + 30)` dan langsung jadikan layer baru tersebut sebagai layer terpilih."

### Prompt 30: Snap-to-Center & Grid Guidelines
> "Tambahkan garis panduan magnetik (*Snap Guidelines*). Ketika layer digeser mendekati garis tengah horizontal atau vertikal kanvas (toleransi 5dp), kunci posisi layer ke tengah secara otomatis dan tampilkan garis panduan berwarna biru cyan di layar."

---

## 📑 Milestone 5: Layer Manager UI / Pengelola Lapisan

### Prompt 31: Membuat Drawer / Floating Dialog Layer Manager
> "Buat sebuah panel pop-up samping atau bottom sheet `LayerManagerBottomSheet` yang menampilkan daftar semua layer yang sedang aktif di kanvas dari atas ke bawah (sesuai urutan z-index terbalik)."

### Prompt 32: Item Layout untuk Layer Manager
> "Desain file layout `item_layer.xml` untuk baris daftar layer. Layout harus memuat: preview icon jenis layer (Teks/Gambar/Bentuk), label nama layer, tombol toggle Mata (Visibilitas), tombol toggle Gembok (Kunci), tombol Hapus, dan handle drag untuk mengubah urutan."

### Prompt 33: Drag-and-Drop Reorder Z-Index dengan `ItemTouchHelper`
> "Pasang `ItemTouchHelper` pada RecyclerView di `LayerManagerBottomSheet`. Ketika pengguna men-drag baris layer ke atas atau ke bawah, tukar posisi objek layer di dalam list `layers` pada `PixelCanvasView`, lalu panggil `invalidate()` agar tumpukan di kanvas langsung berubah."

### Prompt 34: Fitur Lock & Hide Per-Layer
> "Hubungkan tombol mata dan gembok di `item_layer.xml` ke state layer:
> * Tombol Mata: Toggle `layer.isVisible`. Jika false, sembunyikan dari kanvas.
> * Tombol Gembok: Toggle `layer.isLocked`. Jika true, layer tidak bisa diseleksi atau digeser di kanvas utama."

### Prompt 35: Fitur Reorder 'Bring to Front' & 'Send to Back'
> "Tambahkan tombol cepat di toolbar bawah: 'To Front' (memindahkan layer aktif ke posisi paling atas tumpukan) dan 'To Back' (memindahkan layer aktif tepat di atas background kanvas)."

### Prompt 36: Fitur Merge Layers (Menggabungkan Lapisan)
> "Tambahkan mode 'Merge' di Layer Manager. Izinkan pengguna mencentang checkbox pada 2 layer atau lebih, lalu klik tombol 'Gabungkan'. Gabungkan layer-layer yang dicentang tersebut menjadi satu objek `ImageLayer` tunggal di kanvas."

---

## 🔺 Milestone 6: Shapes, Stickers, & Pen Drawing Tool

### Prompt 37: Membuat Model `ShapeLayer`
> "Buatlah data class `ShapeLayer` turunan dari `CanvasLayer` di package model. Model ini mendukung beragam bentuk: Persegi (*Rectangle*), Persegi Sudut Tumpul (*Rounded Rectangle* dengan radius sudut dinamis), Lingkaran (*Circle*), Segitiga, dan Bintang. Sediakan properti warna isi, warna garis tepi (*stroke*), dan ketebalan garis tepi."

### Prompt 38: Arrow & Line Layer
> "Buat kelas `ArrowLayer` untuk menggambar panah penunjuk (seperti fitur panah di PixelLab). Sediakan opsi kepala panah di awal/akhir, ketebalan batang panah, warna, dan gaya panah (lurus atau melengkung)."

### Prompt 39: Bézier Curve Pen Tool
> "Buat kelas `PenLayer` dan mode interaktif pen tool. Pengguna dapat meletakkan titik-titik jangkar (*anchor points*) dan menarik garis kurva Bézier halus untuk menggambar bentuk kustom di atas kanvas."

### Prompt 40: Sticker & Emoji Picker
> "Tambahkan dialog pemilihan stiker dan emoji bawaan. Saat sebuah stiker atau emoji dipilih, buat objek `StickerLayer` (atau `TextLayer` berukuran besar untuk emoji) dan letakkan tepat di tengah kanvas."

### Prompt 41: Color Palette Picker Komprehensif
> "Ganti Spectrum Color Picker lama dengan custom Dialog Color Picker yang mendukung:
> 1. Tab Warna Solid (preset grid warna + slider HSV).
> 2. Tab Gradasi (pemilih arah gradasi linear dan 2 titik warna).
> 3. Hex code input (misal: `#FF5722`)."

### Prompt 42: Color Eyedropper Tool (Pipet Warna)
> "Implementasikan fitur Eyedropper (Pipet Pengambil Warna). Ketika tombol pipet diaktifkan, tampilkan kaca pembesar lingkaran (*magnifier*) di bawah jari pengguna yang membaca warna pixel bitmap tepat di titik sentuh, lalu tetapkan warna tersebut ke elemen yang sedang diedit."

---

## 🖼️ Milestone 7: Canvas Aspect Ratio & Background Engine

### Prompt 43: Preset Rasio Ukuran Kanvas
> "Tambahkan dialog 'Ukuran Gambar' (*Image Size*) yang menyediakan preset ukuran resolusi kanvas standar:
> * Persegi 1:1 (Instagram Post / Profil: 1080x1080)
> * YouTube Thumbnail (16:9: 1280x720)
> * YouTube Channel Banner (2560x1440)
> * Facebook Cover (820x312)
> * Custom Width & Height dalam pixel.
> Sesuaikan viewport kanvas di `PixelCanvasView` agar rasio ukuran tersebut tampil proporsional di layar."

### Prompt 44: Background Color & Gradient Kanvas
> "Implementasikan pengaturan latar belakang kanvas independen pada `PixelCanvasView`. Pengguna dapat memilih: Background Transparan, Warna Solid, atau Warna Gradasi tanpa perlu membuat layer persegi manual."

### Prompt 45: Background Image dari Kamera & Galeri
> "Buat fitur 'From Gallery' dan 'From Camera' untuk latar belakang kanvas. Gambar yang dipilih akan otomatis di-crop atau disesuaikan (*fit/fill*) dengan rasio kanvas yang sedang aktif, berperan sebagai background statis di lapisan terbawah."

### Prompt 46: Fitur Crop Canvas
> "Implementasikan fitur 'Crop Canvas' yang memungkinkan pengguna memotong batas area kerja kanvas tanpa merusak objek layer yang ada di dalamnya."

---

## 💾 Milestone 8: File Proyek (.PLP), Ultra HD Export & Polish

### Prompt 47: Serialization & Deserialization Format Proyek (.PLP)
> "Buat kelas `ProjectSerializer` menggunakan Gson/Kotlinx Serialization. Format ini menyimpan seluruh konfigurasi kanvas (lebar, tinggi, background) dan array semua layer (koordinat, teks, font, warna, rotasi) ke dalam satu file teks berekstensi `.plp` di folder privat aplikasi."

### Prompt 48: Fitur 'Save Project' dan 'Open Project'
> "Tambahkan opsi menu 'Simpan Proyek' dan 'Buka Proyek'. Saat membuka proyek `.plp`, parse file JSON tersebut, rekonstruksi objek-objek layer ke dalam `PixelCanvasView`, dan muat ulang tampilan kanvas persis seperti saat terakhir disimpan."

### Prompt 49: Ultra HD / 4K Off-Screen Canvas Exporter
> "Buat fungsi `exportHighResolution(quality: ExportQuality)` di `PixelCanvasView`. Fungsi ini harus membuat bitmap off-screen pada memori sesuai resolusi asli kanvas (misal: 1920x1080 atau 3840x2160 Ultra HD), merender seluruh layer tanpa ada handle seleksi, dan menyimpannya ke galeri via `MediaStore API` dengan format PNG atau JPG."

### Prompt 50: Undo & Redo History System (Command Pattern)
> "Terapkan arsitektur Command Pattern untuk mengelola riwayat Undo dan Redo pada `PixelCanvasView`. Setiap aksi pengguna (mengubah posisi, mengganti teks, menambah/menghapus layer, mengubah warna) dicatat sebagai state snapshot di stack `undoStack`. Hubungkan tombol Undo dan Redo di toolbar atas untuk berpindah antar riwayat state dengan lancar."

---

## 🔄 Milestone Revisi: Authentic PixelLab Floating Layer Panel (Berdasarkan Referensi UI Asli)

### Prompt Revisi 1: Authentic Top-Left Floating Layer Panel & Toggle State
> "Gantikan atau kembangkan modal BottomSheet pengelola lapisan dengan panel pop-up melayang di sisi kiri atas (persis seperti tampilan PixelLab asli pada tangkapan layar referensi). Panel ini muncul tepat di bawah toolbar atas menutupi sekitar 50% lebar layar kanvas dengan latar belakang kanvas teredupkan (*scrim dimming*). Ketika tombol icon lapisan (`btnTopLayers`) di toolbar atas diklik, tampilkan lingkaran latar aktif (*highlight circle*) dan buka panel ini secara mulus. Menekan tombol `btnTopLayers` kembali atau menekan area kanvas di luar panel akan menutup panel dan menonaktifkan highlight circle."

### Prompt Revisi 2: Redesain Kartu Lapisan dengan Grid Aksi 2x2 (Lock, Edit, Hide, Delete)
> "Rancang layout item layer (`item_layer_authentic.xml`) menjadi kartu putih rapi bergaris batas (*border*) kotak khas PixelLab:
> * **Sisi Kiri**: Titik-titik handle drag (*dot matrix* `::`) di sebelah kiri untuk reorder urutan z-index, diikuti teks preview/nama lapisan (misal: 'New Text').
> * **Sisi Kanan (Kotak Grid 2x2 Bergaris Pembatas Rapi)**:
>   - **Atas-Kiri**: Tombol Gembok (*Lock*) untuk mengunci/membuka layer agar tidak bisa digeser tidak sengaja.
>   - **Atas-Kanan**: Tombol Pensil (*Edit*) untuk langsung membuka dialog ubah teks (`EditTextDialog`) atau panel opsi layer.
>   - **Bawah-Kiri**: Tombol Mata (*Visibility/Hide*) untuk menampilkan atau menyembunyikan layer dari kanvas.
>   - **Bawah-Kanan**: Tombol Tempat Sampah (*Delete*) untuk menghapus layer terpilih dari kanvas."

### Prompt Revisi 3: Integrasi Interaktif, Drag-to-Reorder & Undo/Redo Synchronizer
> "Hubungkan kartu item layer baru pada panel melayang dengan `ItemTouchHelper` untuk drag-and-drop urutan z-index secara real-time. Hubungkan keempat tombol pada grid 2x2 (Lock, Edit, Visibility, Delete) langsung ke `PixelCanvasView` dengan pencatatan snapshot riwayat pada `CanvasHistoryManager` (Prompt 50), sehingga setiap perubahan (termasuk penghapusan, penyembunyian, atau pengeditan teks melalui panel layer) dapat di-undo dan di-redo dengan lancar."

### Prompt Revisi 4: Bilah Kontrol Footer & Transisi Mode Batch Selection (SS 1 ➔ SS 2)
> "Tambahkan bilah footer kontrol di bagian bawah panel layer melayang dengan dua status tampilan transisi interaktif:
> 1. **Tampilan Normal (SS 1)**: Menampilkan tombol bulat putih toggle mode seleksi ganda (`ic_layer_batch_select` dengan simbol `✓=` dan `✗=`) di sebelah kiri, serta tombol kapsul hijau/teal dengan ikon panah 4-arah (`✥`) di sebelah kanan.
> 2. **Transisi ke Mode Batch (SS 2)**: Ketika tombol toggle diklik:
>    - **Pada Kartu Lapisan**: Sembunyikan grid aksi 2x2 dan tampilkan **Kotak Centang (Checkbox `⬜`)** di sebelah kanan kartu untuk menandai layer yang ingin diproses secara massal, dilengkapi garis highlight biru di batas bawah kartu.
>    - **Pada Bilah Footer Bawah**: Beralih ke 5 tombol aksi melingkar rapi:
>      - **Tombol 1 (Hapus `🗑`)**: Menghapus seluruh layer yang dicentang sekaligus dari kanvas dengan pencatatan riwayat undo/redo.
>      - **Tombol 2 (Edit / Properti `📝`)**: Membuka dialog/menu aksi massal bagi layer terpilih.
>      - **Tombol 3 (Gabungkan / Merge `⧉`)**: Menyatukan layer-layer yang dicentang.
>      - **Tombol 4 (Centang / Selesai `✓`)**: Menutup mode seleksi batch dan mengembalikan kartu layer ke mode normal (Grid 2x2 & SS 1).
>      - **Tombol 5 (Geser Bersama `✥`)**: Menggeser posisi semua layer terpilih secara bersamaan.
> 3. Menghubungkan tombol Centang (`✓`) untuk menutup mode batch dan tombol Hapus (`🗑`) untuk mengeksekusi penghapusan massal."

### Prompt Revisi 5: Merge Layers Engine & Batch Action Synchronizer
> "Implementasikan fungsionalitas penggabungan lapisan (*Merge Layers*) pada `PixelCanvasView` ketika tombol merge (`⧉`) ditekan pada layer yang dicentang. Fungsi ini merender layer terpilih secara off-screen menjadi satu objek `ImageLayer` tunggal, menghapus layer-layer asli dari stack, menyisipkan layer gabungan baru pada z-index yang sesuai, dan mencatat snapshot pada `CanvasHistoryManager` sehingga proses penggabungan lapisan dapat di-undo dan di-redo secara sempurna."

