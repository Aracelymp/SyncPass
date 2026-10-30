package com.rojas.syncpass.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.theme.BrandGradient
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.theme.SuccessGreen
import com.rojas.syncpass.ui.theme.WarningAmber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(
    container: AppContainer,
    session: Session,
    onBack: () -> Unit,
    onOpenEvent: (Long) -> Unit
) {
    val viewModel: ScannerViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ScannerViewModel(container.attendanceRepository, session) }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var manualOpen by remember { mutableStateOf(false) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF12081D))
    ) {
        if (granted && state.success == null) {
            CameraPreview(
                enabled = !state.busy,
                onCode = viewModel::onCode,
                modifier = Modifier.fillMaxSize()
            )
            // Dark focus frame and guidance.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .border(3.dp, OnBrand, RoundedCornerShape(30.dp))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = OnBrand)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Escanear QR", style = MaterialTheme.typography.titleLarge, color = OnBrand)
                    Text(
                        "Acredita tu asistencia al evento",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBrand.copy(alpha = 0.75f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!granted) {
                    CameraPermissionCard(
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                } else if (state.busy) {
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = OnBrand)
                        Text("Validando asistencia…", color = OnBrand, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (state.success == null) {
                    Text(
                        "Apunta la cámara al código QR del evento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnBrand,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(onClick = { manualOpen = true }) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, tint = OnBrand)
                    Spacer(Modifier.width(8.dp))
                    Text("Ingresar código manualmente", color = OnBrand)
                }
            }
        }

        if (state.success != null) {
            val success = state.success!!
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .background(OnBrand, RoundedCornerShape(26.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(SuccessGreen.copy(alpha = 0.14f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Text("¡Asistencia registrada!", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        success.attendance.eventTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    if (!success.online) {
                        Row(
                            modifier = Modifier
                                .background(WarningAmber.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.WifiOff, null, tint = WarningAmber, modifier = Modifier.size(17.dp))
                            Text(
                                "Guardado en el dispositivo. Se sincronizará al recuperar conexión.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.reset()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Escanear otro")
                        }
                        Button(
                            onClick = { onOpenEvent(success.eventId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver evento")
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )
    }

    if (manualOpen) {
        var manualCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manualOpen = false },
            title = { Text("Código manual") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Escribe el token del evento o pega el contenido completo del QR.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text("Código o token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualCode.isNotBlank()) {
                            viewModel.onCode(manualCode)
                            manualOpen = false
                        }
                    }
                ) { Text("Validar") }
            },
            dismissButton = {
                TextButton(onClick = { manualOpen = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CameraPermissionCard(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = OnBrand, modifier = Modifier.size(36.dp))
        Text("Permiso de cámara requerido", color = OnBrand, style = MaterialTheme.typography.titleSmall)
        Text(
            "SyncPass usa la cámara solo para leer el código QR del evento.",
            color = OnBrand.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRequest) { Text("Permitir cámara") }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    enabled: Boolean,
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabledState = rememberUpdatedState(enabled)
    val callbackState = rememberUpdatedState(onCode)
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val locked = remember { AtomicBoolean(false) }

    LaunchedEffect(enabled) {
        if (enabled) locked.set(false)
    }

    DisposableEffect(Unit) {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null || !enabledState.value || locked.get()) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val value = barcodes.firstOrNull()?.rawValue
                            if (!value.isNullOrBlank() && locked.compareAndSet(false, true)) {
                                callbackState.value(value)
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (_: Exception) {
                // The permission can be revoked or the camera busy; the manual entry still works.
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching {
                if (providerFuture.isDone) providerFuture.get().unbindAll()
            }
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
