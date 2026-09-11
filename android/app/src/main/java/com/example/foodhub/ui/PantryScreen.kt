package com.example.foodhub.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Camera
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.foodhub.data.model.IngredientStatus
import com.example.foodhub.data.model.PantryUiItem
import com.example.foodhub.data.model.PantryUiState
import com.example.foodhub.data.model.ScanResultState
import com.example.foodhub.viewmodel.PantryViewModel


enum class CameraMode{
    CLOSED,BARCODE,IMAGE
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(viewModel: PantryViewModel) {
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    val context = LocalContext.current

    var pendingCameraMode by remember { mutableStateOf(CameraMode.CLOSED) }
    var cameraMode by remember { mutableStateOf(CameraMode.CLOSED) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraMode = pendingCameraMode
        } else {
            Toast.makeText(context, "Permesso fotocamera necessario", Toast.LENGTH_SHORT).show()
        }
        pendingCameraMode = CameraMode.CLOSED
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (cameraMode) {
        CameraMode.BARCODE -> {
            BarcodeCameraScreen(
                onBarcodeScanned = { barcodeVal ->
                    cameraMode = CameraMode.CLOSED
                    viewModel.processScannedBarcode(barcodeVal)
                },
                onCloseClick = {
                    cameraMode = CameraMode.CLOSED
                }
            )
        }

        CameraMode.IMAGE -> {
            ImageCameraScreen(
                onLabelsDetected = { labels ->
                    cameraMode = CameraMode.CLOSED
                    viewModel.processImageLabels(labels)
                },
                onCloseClick = {
                    cameraMode = CameraMode.CLOSED
                }
            )
        }
        CameraMode.CLOSED -> {
            Scaffold(
                topBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        CenterAlignedTopAppBar(
                            title = { Text("La mia Dispensa", fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                },
                bottomBar = {
                    PantrySmartActions(
                        onBarcodeClick = {
                            val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

                            if (isGranted) {
                                cameraMode = CameraMode.BARCODE
                            } else {
                                pendingCameraMode = CameraMode.BARCODE
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onImageScannerClick = {
                            val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

                            if (isGranted) {
                                cameraMode = CameraMode.IMAGE
                            } else {
                                pendingCameraMode = CameraMode.IMAGE
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }
            ) { paddingValues ->
                when (uiState) {
                    is PantryUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues), contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is PantryUiState.Error -> {
                        val msg = (uiState as PantryUiState.Error).message
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues), contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Errore: $msg")
                        }
                    }

                    is PantryUiState.Success -> {
                        val items = (uiState as PantryUiState.Success).items
                        val groupedItems = items.groupBy { it.status }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {

                            groupedItems[IngredientStatus.TO_BUY]?.let { toBuyList ->
                                stickyHeader {
                                    CategoryHeader(
                                        "Lista della Spesa (Mancanti)",
                                        MaterialTheme.colorScheme.errorContainer,
                                        MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                items(toBuyList) { item -> PantryItemRow(item) }
                            }

                            groupedItems[IngredientStatus.IN_USE]?.let { inUseList ->
                                stickyHeader {
                                    CategoryHeader(
                                        "In dispensa e nel Piano",
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                items(inUseList) { item -> PantryItemRow(item) }
                            }

                            groupedItems[IngredientStatus.UNUSED]?.let { unusedList ->
                                stickyHeader {
                                    CategoryHeader(
                                        "In dispensa (Non usati nel piano)",
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(unusedList) { item -> PantryItemRow(item) }
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nessun ingrediente in dispensa",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                ScanResultDialog(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PantryItemRow(item: PantryUiItem) {
    val statusColor = when (item.status) {
        IngredientStatus.IN_USE -> Color(0xFF4CAF50)
        IngredientStatus.UNUSED -> Color(0xFF9E9E9E)
        IngredientStatus.TO_BUY -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = "https://spoonacular.com/cdn/ingredients_100x100/${item.image}"
            AsyncImage(
                model = imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "${item.amount} ${item.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )
        }
    }
}

@Composable
fun PantrySmartActions(onBarcodeClick: () -> Unit, onImageScannerClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onBarcodeClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Barcode", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onImageScannerClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Scanner Immagine")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanner Immagine", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScanResultDialog(
    viewModel: PantryViewModel
) {
    val scanState by viewModel.scanState.collectAsState()

    when (val state = scanState) {
        is ScanResultState.Hidden -> { /*niente*/ }
        is ScanResultState.Loading -> {
            AlertDialog(
                onDismissRequest = {  },
                confirmButton = {},
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            )
        }
        is ScanResultState.ShowForm -> {
            var name by remember { mutableStateOf(state.originalName) }
            var amount by remember { mutableStateOf(state.amountInStock.toString()) }
            var unit by remember { mutableStateOf(state.unit) }

            AlertDialog(
                onDismissRequest = { viewModel.dismissScanDialog() },
                title = { Text(if (state.isError) "Prodotto non trovato" else "Conferma Prodotto") },
                text = {
                    Column {
                        if (state.isError && state.errorMessage != null) {
                            Text(
                                text = state.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (!state.image.isNullOrEmpty()) {
                            val imageUrl = if (state.image.startsWith("http")) state.image else "https://spoonacular.com/cdn/ingredients_100x100/${state.image}"
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Immagine prodotto",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome prodotto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Quantità") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unità (es. kg, l, pz)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedAmount = amount.toFloatOrNull() ?: 0f
                            viewModel.confirmAndSaveIngredient(
                                id = state.id,
                                userid = state.userid,
                                name = name,
                                amount = parsedAmount,
                                unit = unit,
                                imageUrl = state.image
                            )
                        }
                    ) {
                        Text("Salva in dispensa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissScanDialog() }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}