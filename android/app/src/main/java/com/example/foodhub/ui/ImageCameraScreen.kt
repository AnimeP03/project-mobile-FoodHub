package com.example.foodhub.ui

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.foodhub.ImageScannerManager

enum class CameraState { CAMERA, LOADING, RESULT }

@Composable
fun ImageCameraScreen(
    onLabelsDetected: (List<String>) -> Unit,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val manager = remember { ImageScannerManager(context, lifecycleOwner, previewView) }

    var currentState by remember { mutableStateOf(CameraState.CAMERA) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val detectedIngredients = remember { mutableStateListOf<String>() }

    DisposableEffect(Unit) {
        manager.startCamera()

        onDispose {
            manager.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentState) {
            CameraState.CAMERA -> {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .border(3.dp, Color.Green)
                )

                Button(
                    onClick = onCloseClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Text("Chiudi")
                }

                Button(
                    onClick = {
                        currentState = CameraState.LOADING
                        manager.takePhoto { bitmap, labels ->
                            capturedBitmap = bitmap
                            detectedIngredients.clear()
                            detectedIngredients.addAll(labels)
                            currentState = CameraState.RESULT
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                ) {
                    Text("SCATTA FOTO")
                }
            }

            CameraState.LOADING -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.Green)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analisi del cibo in corso...", color = Color.White)
                }
            }

            CameraState.RESULT -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    capturedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto scattata",
                            modifier = Modifier
                                .size(250.dp)
                                .border(4.dp, Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Risultati:", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (detectedIngredients.isEmpty()) {
                        Text("Nessun ingrediente riconosciuto.", color = Color.Red, fontSize = 18.sp)
                    } else {
                        detectedIngredients.forEach { cibo ->
                            Text("- $cibo", color = Color.Green, fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(onClick = {
                        currentState = CameraState.CAMERA
                    }) {
                        Text("RIPROVA")
                    }
                }
            }
        }
    }
}