package com.example.foodhub

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewFinder: PreviewView,
    private val onBarcodeScanned: (String) -> Unit
) {
    //thread diverso
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // defuatl , posteriore
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // usiamo il ml
            val imageAnalyzer = setupBarcode()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e("KitchenOS_Logs", "Errore durante il binding della fotocamera", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupBarcode(): ImageAnalysis {
        // lasciato tutti formati , solo ita(EU) .FORMAT_EAT_13
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()

        val scanner = BarcodeScanning.getClient(options)

        // di guardare solo l'ultimo frame
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
            processImage(scanner, imageProxy)
        }

        return imageAnalysis
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImage(scanner: BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { valorLetto ->
                            onBarcodeScanned(valorLetto)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("KitchenOS_Logs", "Errore durante la scansione ML Kit", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    //chiudere thread
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}