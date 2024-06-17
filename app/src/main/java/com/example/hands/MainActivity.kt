package com.example.hands

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    uri?.let { mediaUri ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(
                                contentResolver, mediaUri
                            )
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(
                                contentResolver, mediaUri
                            )
                        }.copy(Bitmap.Config.ARGB_8888, true)?.let { bitmap ->
                            val mpImage = BitmapImageBuilder(bitmap).build()

                            val result = gestureRecognizer?.recognize(mpImage)

                            if (result != null) {
                                withContext(Dispatchers.Main) {
                                    if (result.gestures().isNotEmpty()) {
                                        if (result.gestures()[0].isNotEmpty()) {
                                            tv_result.text = result.gestures()[0][0].categoryName()
                                        }
                                    }

                                    if (result.handednesses().isNotEmpty()) {
                                        if (result.handednesses()[0].isNotEmpty()) {
                                            tv_result.append(result.handednesses()[0][0].displayName())
                                        }
                                    }

                                }
                            }

                        }

                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tv_result = findViewById(R.id.tv_result)
        btn_select = findViewById(R.id.btn_select)

        gestureRecognizer = GestureRecognizer.createFromOptions(this, options)

        btn_select.setOnClickListener{
            getContent.launch(arrayOf("image/*"))
        }

    }
}