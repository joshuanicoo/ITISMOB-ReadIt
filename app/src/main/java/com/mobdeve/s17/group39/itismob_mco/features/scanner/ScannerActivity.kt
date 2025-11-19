package com.mobdeve.s17.group39.itismob_mco.features.scanner

import android.Manifest
import android.R
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
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

@ExperimentalGetImage
class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ScannerActivityBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isProcessing = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var hasCameraPermission = false
    private var lastProcessedTime = 0L
    private var cameraRetryCount = 0

    companion object {
        const val SCANNED_ISBN_RESULT = "scanned_isbn_result"
        private const val PROCESSING_DELAY_MS = 500L
        private const val MAX_CAMERA_RETRIES = 3
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasCameraPermission = true
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required for scanning", Toast.LENGTH_LONG).show()
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
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
        if (isEmulator()) {
            Toast.makeText(this, "Test Mode Active", Toast.LENGTH_SHORT).show()
            addTestButtons()
        }
        showManualInputOption()
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for")
    }

    private fun addTestButtons() {
        val testLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val testIsbns = listOf(
            "9780141439518" to "Pride and Prejudice",
            "9780439708180" to "Harry Potter 1",
            "9780545010221" to "Harry Potter 2"
        )

        testIsbns.forEach { (isbn, title) ->
            val testButton = Button(this).apply {
                text = title
                setOnClickListener { simulateScan(isbn) }
                setBackgroundColor(resources.getColor(R.color.holo_green_dark, theme))
                setTextColor(resources.getColor(R.color.white, theme))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(20, 10, 20, 10)
                }
            }
            testLayout.addView(testButton)
        }

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            bottomMargin = 50
        }

        (binding.root as? ViewGroup)?.addView(testLayout, layoutParams)
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
                if (cameraProvider?.availableCameraInfos?.isNotEmpty() == true) {
                    bindCameraUseCases()
                } else {
                    handleCameraError()
                    Toast.makeText(this, "No camera available", Toast.LENGTH_LONG).show()
                }
            } catch (exc: Exception) {
                handleCameraError()
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
            .setTargetResolution(Size(1280, 720))
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            cameraRetryCount = 0 // reset on successss
            Toast.makeText(this, "Camera started successfully", Toast.LENGTH_SHORT).show()
        } catch (exc: Exception) {
            handleCameraError()
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
                        .addCameraFilter { cameraInfos ->
                            if (cameraInfos.isNotEmpty()) listOf(cameraInfos[0]) else emptyList()
                        }
                        .build()
                } else {
                    throw IllegalStateException("No cameras available")
                }
            }
        }
    }

    private fun handleCameraError() {
        if (cameraRetryCount < MAX_CAMERA_RETRIES) {
            cameraRetryCount++
            Handler(Looper.getMainLooper()).postDelayed({
                startCamera()
            }, 1000L * cameraRetryCount)
        } else {
            setupTestMode()
            Toast.makeText(this, "Camera initialization failed. Using manual input mode.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showManualInputOption() {
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val manualInput = EditText(this).apply {
            hint = "Enter ISBN (10 or 13 digits)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(50, 20, 50, 20)
            }
        }
        inputLayout.addView(manualInput)

        val submitButton = Button(this).apply {
            text = "Submit ISBN"
            setOnClickListener {
                val isbn = manualInput.text.toString().trim()
                if (isbn.isNotEmpty() && isValidIsbn(isbn)) {
                    simulateScan(isbn)
                } else {
                    Toast.makeText(context, "Please enter valid ISBN", Toast.LENGTH_SHORT).show()
                }
            }
            setBackgroundColor(resources.getColor(R.color.holo_blue_dark, theme))
            setTextColor(resources.getColor(R.color.white, theme))
        }
        inputLayout.addView(submitButton)

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
            topMargin = 100
        }

        (binding.root as? ViewGroup)?.addView(inputLayout, layoutParams)
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < PROCESSING_DELAY_MS || isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        lastProcessedTime = currentTime

        @ExperimentalGetImage
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing = false
            return
        }

        try {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(inputImage)
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing = false
                }
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { barcodeValue ->
                            handleScannedBarcode(barcodeValue)
                            return@addOnSuccessListener
                        }
                    }
                }
        } catch (e: Exception) {
            imageProxy.close()
            isProcessing = false
        }
    }

    private fun handleScannedBarcode(barcodeValue: String) {
        runOnUiThread {
            val cleanIsbn = barcodeValue.replace("[^\\dX]".toRegex(), "")
            if (isValidIsbn(cleanIsbn)) {
                val resultIntent = Intent().apply {
                    putExtra(SCANNED_ISBN_RESULT, cleanIsbn)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                isProcessing = false
                Toast.makeText(this, "Invalid ISBN format: $cleanIsbn", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidIsbn(isbn: String): Boolean {
        val cleanIsbn = isbn.replace("[^\\dX]".toRegex(), "")
        return when (cleanIsbn.length) {
            10 -> isValidIsbn10(cleanIsbn)
            13 -> isValidIsbn13(cleanIsbn)
            else -> false
        }
    }

    private fun isValidIsbn10(isbn: String): Boolean {
        if (isbn.length != 10) return false
        var sum = 0
        for (i in 0 until 9) {
            val digit = isbn[i] - '0'
            if (digit < 0 || digit > 9) return false
            sum += (digit * (10 - i))
        }
        val lastChar = isbn[9]
        sum += if (lastChar == 'X') 10 else (lastChar - '0')
        return sum % 11 == 0
    }

    private fun isValidIsbn13(isbn: String): Boolean {
        if (isbn.length != 13) return false
        var sum = 0
        for (i in isbn.indices) {
            val digit = isbn[i] - '0'
            if (digit < 0 || digit > 9) return false
            sum += digit * if (i % 2 == 0) 1 else 3
        }
        return sum % 10 == 0
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
        cameraProvider?.unbindAll()
    }
}