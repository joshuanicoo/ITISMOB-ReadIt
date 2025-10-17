package com.mobdeve.s17.group39.itismob_mco.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mobdeve.s17.group39.itismob_mco.databinding.ScannerActivityBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ScannerActivityBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isProcessing = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var hasCameraPermission = false

    companion object {
        const val SCANNED_ISBN_RESULT = "scanned_isbn_result"
        private const val TAG = "ScannerActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasCameraPermission = true
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required for barcode scanning", Toast.LENGTH_LONG).show()
            setupTestMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScannerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBarcodeScanner()
        setupClickListeners()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasCameraPermission = true
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupBarcodeScanner() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()

        barcodeScanner = BarcodeScanning.getClient(options)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupClickListeners() {
        binding.cancelScanBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupTestMode() {
        Toast.makeText(this, "Test Mode: Use buttons below", Toast.LENGTH_LONG).show()
        addTestButtons()
        showManualInputOption()
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.contains("vbox") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK") ||
                android.os.Build.MANUFACTURER.contains("Genymotion"))
    }

    private fun addTestButtons() {
        val testLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
        }

        val testIsbns = listOf(
            "9780141439518" to "Pride and Prejudice",
            "9780439708180" to "Harry Potter 1",
            "9780545010221" to "Harry Potter 2",
            "9780439358071" to "Harry Potter 3"
        )

        testIsbns.forEach { (isbn, title) ->
            val testButton = android.widget.Button(this).apply {
                text = "$title"
                setOnClickListener {
                    simulateScan(isbn)
                }
                setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, theme))
                setTextColor(resources.getColor(android.R.color.white, theme))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(20, 10, 20, 10)
                }
            }
            testLayout.addView(testButton)
        }

        val layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM
            bottomMargin = 50
        }

        (binding.root as? android.view.ViewGroup)?.addView(testLayout, layoutParams)
    }

    private fun simulateScan(isbn: String) {
        val resultIntent = Intent().apply {
            putExtra(SCANNED_ISBN_RESULT, isbn)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun startCamera() {
        if (!hasCameraPermission) {
            setupTestMode()
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show()
                setupTestMode()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        cameraProvider.unbindAll()

        val cameraSelector = buildCameraSelector(cameraProvider)

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            Toast.makeText(this, "Camera ready for scanning", Toast.LENGTH_SHORT).show()
        } catch (exc: Exception) {
            Toast.makeText(this, "Camera binding failed", Toast.LENGTH_SHORT).show()
            setupTestMode()
        }
    }

    private fun buildCameraSelector(cameraProvider: ProcessCameraProvider): CameraSelector {
        return try {
            CameraSelector.DEFAULT_BACK_CAMERA
        } catch (e: Exception) {
            try {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } catch (e2: Exception) {
                if (cameraProvider.availableCameraInfos.isNotEmpty()) {
                    CameraSelector.Builder()
                        .addCameraFilter { cameraInfos -> cameraInfos }
                        .build()
                } else {
                    throw IllegalStateException("No cameras available")
                }
            }
        }
    }

    private fun showManualInputOption() {
        val inputLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(resources.getColor(android.R.color.background_light, theme))
        }

        val titleText = android.widget.TextView(this).apply {
            text = "Manual ISBN Entry"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.black, theme))
            gravity = android.view.Gravity.CENTER
        }
        inputLayout.addView(titleText)

        val manualInput = android.widget.EditText(this).apply {
            hint = "Enter 10 or 13 digit ISBN..."
            setTextColor(resources.getColor(android.R.color.black, theme))
            setHintTextColor(resources.getColor(android.R.color.darker_gray, theme))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(50, 20, 50, 20)
            }
        }
        inputLayout.addView(manualInput)

        val submitButton = android.widget.Button(this).apply {
            text = "Submit ISBN"
            setOnClickListener {
                val isbn = manualInput.text.toString().trim()
                if (isbn.isNotEmpty()) {
                    if (isValidIsbn(isbn)) {
                        simulateScan(isbn)
                    } else {
                        Toast.makeText(context, "Invalid ISBN format", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please enter an ISBN", Toast.LENGTH_SHORT).show()
                }
            }
            setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, theme))
            setTextColor(resources.getColor(android.R.color.white, theme))
        }
        inputLayout.addView(submitButton)

        val layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP
            topMargin = 100
        }

        (binding.root as? android.view.ViewGroup)?.addView(inputLayout, layoutParams)
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessing) return

        isProcessing = true
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { barcodeValue ->
                            handleScannedBarcode(barcodeValue)
                            imageProxy.close()
                            isProcessing = false
                            return@addOnSuccessListener
                        }
                    }
                    imageProxy.close()
                    isProcessing = false
                }
                .addOnFailureListener { exception ->
                    imageProxy.close()
                    isProcessing = false
                }
        } else {
            imageProxy.close()
            isProcessing = false
        }
    }

    private fun handleScannedBarcode(barcodeValue: String) {
        runOnUiThread {
            val cleanIsbn = barcodeValue.replace("[^\\dX]".toRegex(), "")

            if (isValidIsbn(cleanIsbn)) {
                Toast.makeText(this, "ISBN Found: $cleanIsbn", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply {
                    putExtra(SCANNED_ISBN_RESULT, cleanIsbn)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Invalid ISBN: $cleanIsbn", Toast.LENGTH_SHORT).show()
                isProcessing = false
            }
        }
    }

    private fun isValidIsbn(isbn: String): Boolean {
        return when (isbn.length) {
            10 -> true
            13 -> true
            else -> false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
        cameraProvider?.unbindAll()
    }
}