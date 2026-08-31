package com.example.momentra.ui.shell.empty.group

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicBoolean
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.momentra.ui.theme.PlusJakartaSans
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun GroupJoinQrScanner(
    onCode: (String) -> Unit,
    onDismiss: () -> Unit,
    onCompanyCode: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) onDismiss()
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }
    val accepted = remember { AtomicBoolean(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF131313)),
        ) {
            if (granted) {
                CameraPreview(
                    onRaw = { raw ->
                        val company = com.example.momentra.ui.shell.empty.business.CompanyJoinLink.parse(raw)
                        if (company != null && onCompanyCode != null) {
                            if (accepted.compareAndSet(false, true)) onCompanyCode(company)
                            return@CameraPreview
                        }
                        val code = GroupJoinLink.parse(raw) ?: return@CameraPreview
                        if (accepted.compareAndSet(false, true)) onCode(code)
                    },
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    "Scan to join",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    fontSize = 20.sp,
                )
                Text(
                    "Point the camera at a Momentra invite QR.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .border(2.dp, Color(0xFFFF7A3D), RoundedCornerShape(24.dp)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 28.dp, vertical = 12.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Close scanner"
                    },
            ) {
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun CameraPreview(onRaw: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val onRawState = rememberUpdatedState(onRaw)
    val analyzer = remember { QrAnalyzer { onRawState.value(it) } }

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

private class QrAnalyzer(
    private val onRaw: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

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
