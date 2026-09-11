package com.example.foodhub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.torchvision.TensorImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.exp

class ImageScannerManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewFinder: PreviewView
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var module: org.pytorch.Module? = null
    private var labels = listOf<String>()

    private var imageCapture: ImageCapture? = null

    init {
        module = LiteModuleLoader.load(assetFilePath(context, "model2.ptl"))
        labels = context.assets.open("model2.txt").bufferedReader().readLines()
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e("KitchenOS_Logs", "Errore durante il binding della fotocamera (Image Labeling)", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(onResult: (Bitmap, List<String>) -> Unit) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val originalBitmap = image.toBitmap()
                val rotationDegrees = image.imageInfo.rotationDegrees

                cameraExecutor.execute {
                    try {
                        val rotatedBitmap = rotateBitmap(originalBitmap, rotationDegrees)
                        val croppedBitmap = cropToGreenSquare(rotatedBitmap)
                        val resizedBitmap = Bitmap.createScaledBitmap(
                            croppedBitmap,
                            224,
                            224,
                            true
                        )
                        salvaBitmapPerDebug(resizedBitmap, context)

                        val results = processImage(resizedBitmap)

                        ContextCompat.getMainExecutor(context).execute {
                            onResult(resizedBitmap, results)
                        }
                    } catch (exc: Exception) {
                        Log.e("KitchenOS_Logs", "Errore durante l'analisi", exc)
                    } finally {
                        image.close()
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("KitchenOS_Logs", "Errore scatto foto", exception)
            }
        })
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImage(bitmap: Bitmap): List<String> {

        val tensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            floatArrayOf(0.485f, 0.456f, 0.406f),
            floatArrayOf(0.229f, 0.224f, 0.225f)
        )

        val outputTensor = module!!.forward(IValue.from(tensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray

        val expScores = scores.map { exp(it.toDouble()) }
        val sumExp = expScores.sum()
        val probabilities = expScores.map { (it / sumExp).toFloat() }

        val top5 = probabilities
            .mapIndexed { index, prob -> index to prob }
            .sortedByDescending { it.second }
            .take(5)

        top5.forEach {
            Log.e("KitchenOS_Logs", "index=${it.first}, prob=${it.second}, label=${labels[it.first]}")
        }

        return top5.map { (index, prob) ->
            "${labels[index]} : %.2f%%".format(prob * 100)
        }

    }

    private fun cropToGreenSquare(bitmap: Bitmap): Bitmap {
        val shortSide = minOf(bitmap.width, bitmap.height)

        val cropSide = (shortSide * 0.5).toInt()

        val xOffset = (bitmap.width - cropSide) / 2
        val yOffset = (bitmap.height - cropSide) / 2

        return Bitmap.createBitmap(bitmap, xOffset, yOffset, cropSide, cropSide)
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = java.io.File(context.filesDir, assetName)
        context.assets.open(assetName).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    private fun salvaBitmapPerDebug(bitmap: Bitmap, context: Context) {
        val file = java.io.File(context.getExternalFilesDir(null), "debug_android.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Log.d("KitchenOS_Logs", "Immagine salvata in: ${file.absolutePath}")
    }
}