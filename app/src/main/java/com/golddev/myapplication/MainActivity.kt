package com.golddev.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.FileProvider
import coil.api.load
import coil.size.Scale
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*

private const val FILE_PROVIDER_AUTHORITY = "com.golddev.myapplication.fileprovider"
private const val CACHE_FOLDER = ".CoilExifRotationTest"

class MainActivity : AppCompatActivity() {

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start_camera_button.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                val file = createCacheImageFile()
                val fileUri = FileProvider.getUriForFile(applicationContext, FILE_PROVIDER_AUTHORITY, file)
                imageUri = fileUri
                putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                if (Build.VERSION.SDK_INT >= 23) {
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            }
            // check whether the user has a camera app before starting in order to prevent crashes
            intent.resolveActivity(packageManager)?.also {
                startActivityForResult(intent, 10)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
            image_view.load(imageUri) {
                scale(Scale.FIT)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createCacheImageFile(): File {
        val directory = File(applicationContext.cacheDir, CACHE_FOLDER)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File.createTempFile(UUID.randomUUID().toString(), ".jpg", directory)
    }
}
