package com.example.foodhub.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.foodhub.BarcodeScannerManager

@Composable
fun BarcodeCameraScreen(
    onBarcodeScanned: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    // chiamava troppe funzioni
    var lastScannedCode by remember { mutableStateOf("") }

    val manager = remember {
        BarcodeScannerManager(
            context,
            lifecycleOwner,
            previewView,
            onBarcodeScanned = { codice ->
                if (codice != lastScannedCode) {
                    lastScannedCode = codice
                    onBarcodeScanned(codice)
                }
            }
        )
    }


    DisposableEffect(Unit) {
        manager.startCamera()

        onDispose {
            manager.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = onCloseClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text("Chiudi")
        }
    }
}