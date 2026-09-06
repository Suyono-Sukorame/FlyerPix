package com.flyerpix.editor.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.ActivityStartBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StartActivity : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 0
    private val GALLERY_REQUEST_CODE = 1
    val PERMISSIONS_REQUEST_CAMERA = 99
    lateinit var photoUri : Uri
    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cameraButton.setOnClickListener {
            checkCameraPermission()
        }
        binding.galleryButton.setOnClickListener {
            var pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickPhoto, GALLERY_REQUEST_CODE)
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_REQUEST_CAMERA)
        } else {
            startCameraActivity()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CAMERA -> if (grantResults.isNotEmpty() && grantResults.get(0) == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity()
            }
        }
    }

    private fun startCameraActivity() {
        var takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePicture.resolveActivity(this.packageManager) != null) {
            try {
                val photoFile = createPhotoFile()
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this, "com.flyerpix.editor", photoFile)
                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePicture, CAMERA_REQUEST_CODE)
                }
            } catch (e : Throwable) {
                Log.e("Throwable", e.message ?: "Unknown error")
            }
        }
    }

    private fun createPhotoFile(): File? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val photoFileName = "IMG_" + timestamp + "_"
        val storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(photoFileName, ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, image: Intent?) {
        super.onActivityResult(requestCode, resultCode, image)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                //val bundle = Bundle()
                //bundle.putString("image", photoUri.toString())
                intent = Intent(this, EditorActivity::class.java)
                intent.putExtra("image", photoUri.toString())
                startActivity(intent)
                //NavHostFragment.findNavController(this).navigate(R.id.editorAction, bundle)
            }
            GALLERY_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                val selectedImage = image!!.data
                intent = Intent(this, EditorActivity::class.java)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.putExtra("image", selectedImage!!.toString())
                startActivity(intent)
                //NavHostFragment.findNavController(this).navigate(R.id.editorAction, bundle)
            }
        }
    }
}
