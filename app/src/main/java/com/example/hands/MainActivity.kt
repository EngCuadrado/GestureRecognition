package com.example.hands

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Declaración de vistas y variables necesarias
    private lateinit var tv_result: TextView  // TextView para mostrar el resultado del reconocimiento de gestos
    private lateinit var btn_select: Button   // Botón para seleccionar una imagen desde el almacenamiento
    private lateinit var btn_capture: Button  // Botón para capturar una imagen desde la cámara
    private lateinit var iv_preview: ImageView  // ImageView para mostrar la imagen seleccionada o capturada

    // Configuración para el reconocimiento de gestos usando Mediapipe
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
    var gestureRecognizer: GestureRecognizer? =
        null  // Objeto GestureRecognizer para reconocimiento de gestos

    // Lanzadores para obtener contenido de documentos y capturar imágenes
    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { analyzeImageUri(it) }  // Cuando se obtiene un URI de imagen, analizar la imagen
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let { analyzeBitmap(it) }  // Cuando se captura una imagen, analizar el Bitmap
        }

    // Lanzador para solicitar permisos
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var tts: TextToSpeech  // Instancia de TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Habilitar el modo de borde a borde (edge-to-edge)

        setContentView(R.layout.activity_main)  // Establecer el layout de la actividad

        // Inicialización de las vistas desde el layout
        tv_result = findViewById(R.id.tv_result)
        btn_select = findViewById(R.id.btn_select)
        btn_capture = findViewById(R.id.btn_capture)
        iv_preview = findViewById(R.id.iv_preview)

        // Registro del lanzador para solicitar permisos de cámara
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Si se concede el permiso, lanzar la captura de imagen
                takePicture.launch(null)
            } else {
                // Manejar aquí el escenario donde se deniega el permiso de cámara
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

        // Inicialización de TextToSpeech
        tts = TextToSpeech(this, this)

        // Creación del objeto GestureRecognizer utilizando las opciones configuradas
        gestureRecognizer = GestureRecognizer.createFromOptions(this, options)

        // Configuración de listeners para los botones de seleccionar y capturar imagen
        btn_select.setOnClickListener {
            // Al hacer clic en el botón de seleccionar, lanzar la actividad para seleccionar una imagen
            getContent.launch(arrayOf("image/*"))
        }

        btn_capture.setOnClickListener {
            // Al hacer clic en el botón de capturar, validar y solicitar permiso de cámara
            validateCameraPermission()
        }
    }

    // Función para validar y solicitar permiso de cámara
    private fun validateCameraPermission() {
        try {
            // Verificar si el permiso de cámara ya está concedido
            val cameraPermissionStatus = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            // Si el permiso de cámara está concedido, lanzar la captura de imagen
            if (cameraPermissionStatus) {
                takePicture.launch(null)
            } else {
                // Si el permiso de cámara no está concedido, solicitarlo al usuario
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Función para analizar una imagen desde un URI obtenido al seleccionar una imagen
    private fun analyzeImageUri(uri: Uri) {
        lifecycleScope.launch {
            // Cargar y decodificar la imagen desde el URI en un hilo de fondo (Dispatchers.IO)
            val bitmap = withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }.copy(
                    Bitmap.Config.ARGB_8888,
                    true
                )  // Copiar el bitmap para asegurar la mutabilidad
            }
            analyzeBitmap(bitmap)  // Llamar a la función para analizar el Bitmap obtenido
        }
    }

    // Función para analizar un Bitmap capturado desde la cámara
    private fun analyzeBitmap(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.Main) {
            // Mostrar el Bitmap capturado en el ImageView en el hilo principal
            iv_preview.setImageBitmap(bitmap)

            // Analizar el Bitmap utilizando Mediapipe en un hilo de fondo (Dispatchers.IO)
            withContext(Dispatchers.IO) {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result =
                    gestureRecognizer?.recognize(mpImage)  // Realizar el reconocimiento de gestos
                withContext(Dispatchers.Main) {
                    result?.let {
                        // Procesar el resultado del reconocimiento de gestos
                        if (it.gestures().isNotEmpty() && it.gestures()[0].isNotEmpty()) {
                            var gestureName = it.gestures()[0][0].categoryName()
                            gestureName = gestureName.replace('_', ' ')
                            tv_result.text = gestureName
                            speakOut(gestureName)
                        }
                        if (it.handednesses().isNotEmpty() && it.handednesses()[0].isNotEmpty()) {
                            tv_result.append(" " + it.handednesses()[0][0].displayName())
                        }
                    }
                }
            }
        }
    }


    // Función para reproducir el texto en tv_result utilizando TTS
    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onDestroy() {
        // Liberar recursos de TextToSpeech cuando se destruya la actividad
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Configurar idioma (opcional)
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language is not supported")
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }
}
