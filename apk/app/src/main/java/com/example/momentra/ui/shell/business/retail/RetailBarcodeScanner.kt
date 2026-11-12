package com.example.momentra.ui.shell.business.retail

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.momentra.ui.shell.components.MomentraFullscreenDialog
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RetailRose
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Retail invoice / SKU barcode + QR scanner. Returns the raw payload — callers match catalogue
 * or fill receive lines. Distinct from [com.example.momentra.ui.shell.empty.group.GroupJoinQrScanner]
 * which only accepts Momentra invite links.
 */
@Composable
internal fun RetailBarcodeScanner(
    title: String = "Scan barcode",
    subtitle: String = "Point at a product barcode, QR, or supplier invoice code",
    onCode: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var scanError by remember { mutableStateOf<String?>(null) }
    var decodingUpload by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }
    val accepted = remember { AtomicBoolean(false) }

    fun acceptRaw(raw: String): Boolean {
        val code = raw.trim()
        if (code.isBlank()) return false
        if (!accepted.compareAndSet(false, true)) return false
        onCode(code)
        return true
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            decodingUpload = true
            scanError = null
            val raw = withContext(Dispatchers.IO) {
                decodeRetailBarcodeFromUri(context, uri)
            }
            decodingUpload = false
            when {
                raw.isNullOrBlank() -> scanError = "No barcode found in that image."
                acceptRaw(raw) -> Unit
                else -> scanError = "Could not read that code."
            }
        }
    }

    MomentraFullscreenDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF131313)),
        ) {
            if (granted) {
                RetailCameraPreview(
                    onRaw = { raw -> acceptRaw(raw) },
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(20.dp),
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    fontSize = 20.sp,
                )
                Text(
                    when {
                        decodingUpload -> "Reading code from photo…"
                        !granted -> "Camera permission is off — upload a barcode photo instead."
                        else -> subtitle
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                scanError?.let {
                    Text(
                        it,
                        color = Color(0xFFF87171),
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .border(2.dp, RetailRose.rose, RoundedCornerShape(24.dp)),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(RetailRose.rose.copy(alpha = 0.9f))
                        .clickable(enabled = !decodingUpload) {
                            pickMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Upload barcode from gallery"
                        },
                ) {
                    Text(
                        "Upload",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 15.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close scanner"
                        },
                ) {
                    Text(
                        "Cancel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

private fun retailBarcodeOptions(): BarcodeScannerOptions =
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_DATA_MATRIX,
        )
        .build()

private suspend fun decodeRetailBarcodeFromUri(context: android.content.Context, uri: Uri): String? {
    val bitmap = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull() ?: return null
    val image = InputImage.fromBitmap(bitmap, 0)
    val scanner = BarcodeScanning.getClient(retailBarcodeOptions())
    return try {
        scanner.process(image).await().firstOrNull()?.rawValue
    } finally {
        scanner.close()
    }
}

@Composable
private fun RetailCameraPreview(onRaw: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val onRawState = rememberUpdatedState(onRaw)
    val analyzer = remember { RetailBarcodeAnalyzer { onRawState.value(it) } }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { useCase ->
                        useCase.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                },
                ContextCompat.getMainExecutor(ctx),
            )
            previewView
        },
    )
}

private class RetailBarcodeAnalyzer(
    private val onRaw: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(retailBarcodeOptions())

    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstOrNull()?.rawValue
                if (!raw.isNullOrBlank()) onRaw(raw)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        scanner.close()
    }
}
