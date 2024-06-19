package com.example.hands

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tv_result: TextView
    private lateinit var btn_select: Button
    private lateinit var btn_capture: Button
    private lateinit var iv_preview: ImageView

    val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("gesture_recognizer.task")
    val baseOptions = baseOptionsBuilder.build()

    val optionsBuilder =
        GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinHandDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setRunningMode(RunningMode.IMAGE)

    val options = optionsBuilder.build()
    var gestureRecognizer: GestureRecognizer? = null

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { analyzeImageUri(it) }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let { analyzeBitmap(it) }
        }


    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tv_result = findViewById(R.id.tv_result)
        btn_select = findViewById(R.id.btn_select)
        btn_capture = findViewById(R.id.btn_capture)
        iv_preview = findViewById(R.id.iv_preview)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

        gestureRecognizer = GestureRecognizer.createFromOptions(this, options)

        btn_select.setOnClickListener {
            getContent.launch(arrayOf("image/*"))
        }

        btn_capture.setOnClickListener {
            validateCameraPermission()
        }
    }

    private fun validateCameraPermission() {
        try {
            val cameraPermissionStatus = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            // Check permission before launching intent
            if (cameraPermissionStatus) {
                takePicture.launch(null)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun analyzeImageUri(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }.copy(Bitmap.Config.ARGB_8888, true)
            }
            analyzeBitmap(bitmap)
        }
    }

    private fun analyzeBitmap(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.Main) {
            iv_preview.setImageBitmap(bitmap)
            withContext(Dispatchers.IO) {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = gestureRecognizer?.recognize(mpImage)
                withContext(Dispatchers.Main) {
                    result?.let {
                        if (it.gestures().isNotEmpty() && it.gestures()[0].isNotEmpty()) {
                            tv_result.text = it.gestures()[0][0].categoryName()
                        }
                        if (it.handednesses().isNotEmpty() && it.handednesses()[0].isNotEmpty()) {
                            tv_result.append(" " + it.handednesses()[0][0].displayName())
                        }
                    }
                }
            }
        }
    }
}
