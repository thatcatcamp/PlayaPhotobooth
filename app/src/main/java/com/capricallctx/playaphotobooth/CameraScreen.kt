package com.capricallctx.playaphotobooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateToGallery: () -> Unit
) {
    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        CameraContent(onNavigateToGallery = onNavigateToGallery)
    } else {
        PermissionDeniedContent(onRequestPermission = { cameraPermissionState.launchPermissionRequest() })
    }
}

@Composable
private fun CameraContent(onNavigateToGallery: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedPhoto by remember { mutableStateOf<Bitmap?>(null) }

    // Background management
    val backgroundManager = remember { BackgroundManager(context) }
    var currentBackgroundIndex by remember { mutableStateOf(0) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(previewView) {
        if (previewView != null) {
            startCamera(context, lifecycleOwner, previewView!!) { cam, capture ->
                camera = cam
                imageCapture = capture
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    previewView = preview
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Background cycling controls
                if (backgroundManager.getBackgroundCount() > 1) {
                    Row {
                        FloatingActionButton(
                            onClick = {
                                currentBackgroundIndex = backgroundManager.cycleToPreviousBackground()
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text("←", style = MaterialTheme.typography.headlineSmall)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Background info
                        Card(
                            modifier = Modifier.padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        ) {
                            Text(
                                text = backgroundManager.getBackgroundName(currentBackgroundIndex),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FloatingActionButton(
                            onClick = {
                                currentBackgroundIndex = backgroundManager.cycleToNextBackground()
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text("→", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                FloatingActionButton(
                    onClick = onNavigateToGallery,
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.AccountBox, contentDescription = "Gallery")
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!isCapturing && imageCapture != null) {
                            isCapturing = true
                            capturePhoto(
                                context = context,
                                imageCapture = imageCapture!!,
                                executor = cameraExecutor
                            ) { bitmap ->
                                capturedPhoto = bitmap
                                isCapturing = false
                            }
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = if (isCapturing) Color.Gray else MaterialTheme.colorScheme.primary
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountBox,
                            contentDescription = "Capture",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        // Show captured photo with background replacement
        capturedPhoto?.let { bitmap ->
            PhotoPreviewDialog(
                bitmap = bitmap,
                backgroundManager = backgroundManager,
                selectedBackgroundIndex = currentBackgroundIndex,
                onDismiss = { capturedPhoto = null },
                onSave = { processedBitmap ->
                    savePhotoToGallery(context, processedBitmap)
                    capturedPhoto = null
                }
            )
        }
    }
}

@Composable
private fun PhotoPreviewDialog(
    bitmap: Bitmap,
    backgroundManager: BackgroundManager,
    selectedBackgroundIndex: Int,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(true) }

    LaunchedEffect(bitmap, selectedBackgroundIndex) {
        processedBitmap = processBackgroundReplacement(bitmap, backgroundManager, selectedBackgroundIndex)
        isProcessing = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo Preview") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isProcessing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Processing background...")
                        }
                    }
                    processedBitmap != null -> {
                        Image(
                            bitmap = processedBitmap!!.asImageBitmap(),
                            contentDescription = "Processed photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Text("Failed to process image")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { processedBitmap?.let { onSave(it) } },
                enabled = !isProcessing && processedBitmap != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera permission is required to take photos",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

private suspend fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onCameraReady: (Camera, ImageCapture) -> Unit
) = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            onCameraReady(camera, imageCapture)
            continuation.resume(Unit)
        } catch (exc: Exception) {
            Log.e("CameraScreen", "Use case binding failed", exc)
            continuation.resume(Unit)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                onImageCaptured(bitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Handle rotation if needed (front camera might be mirrored)
    val matrix = Matrix()
    matrix.preScale(-1.0f, 1.0f) // Mirror horizontally for front camera

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private suspend fun processBackgroundReplacement(
    originalBitmap: Bitmap,
    backgroundManager: BackgroundManager,
    selectedBackgroundIndex: Int
): Bitmap? = suspendCoroutine { continuation ->
    val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .build()

    val segmenter = Segmentation.getClient(options)
    val inputImage = InputImage.fromBitmap(originalBitmap, 0)

    segmenter.process(inputImage)
        .addOnSuccessListener { segmentationMask ->
            try {
                Log.d("CameraScreen", "Segmentation successful, mask width: ${segmentationMask.width}, height: ${segmentationMask.height}")
                Log.d("CameraScreen", "Original bitmap width: ${originalBitmap.width}, height: ${originalBitmap.height}")

                val processedBitmap = replaceBackgroundImproved(originalBitmap, segmentationMask, backgroundManager, selectedBackgroundIndex)
                continuation.resume(processedBitmap)
            } catch (e: Exception) {
                Log.e("CameraScreen", "Background replacement failed", e)
                continuation.resume(originalBitmap)
            }
        }
        .addOnFailureListener { e ->
            Log.e("CameraScreen", "Segmentation failed", e)
            continuation.resume(originalBitmap)
        }
}

private fun replaceBackground(
    originalBitmap: Bitmap,
    mask: SegmentationMask,
    backgroundManager: BackgroundManager,
    selectedBackgroundIndex: Int
): Bitmap {
    val width = originalBitmap.width
    val height = originalBitmap.height

    // Get the selected background from BackgroundManager
    val backgroundBitmap = backgroundManager.getBackgroundPreview(selectedBackgroundIndex, width, height)

    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Get original image pixels
    val originalPixels = IntArray(width * height)
    originalBitmap.getPixels(originalPixels, 0, width, 0, 0, width, height)

    // Get background pixels
    val backgroundPixels = IntArray(width * height)
    backgroundBitmap.getPixels(backgroundPixels, 0, width, 0, 0, width, height)

    // Get mask data - ensure buffer is rewound to start
    val maskBuffer = mask.buffer
    maskBuffer.rewind()
    val maskArray = ByteArray(maskBuffer.remaining())
    maskBuffer.get(maskArray)

    // Create result pixels array
    val resultPixels = IntArray(width * height)

    // ML Kit mask dimensions might be different from image dimensions
    val maskWidth = mask.width
    val maskHeight = mask.height

    Log.d("CameraScreen", "Mask dimensions: ${maskWidth}x${maskHeight}, Image dimensions: ${width}x${height}")
    Log.d("CameraScreen", "Mask array size: ${maskArray.size}, Expected: ${maskWidth * maskHeight}")

    // Debug mask values
    val sampleMaskValues = mutableListOf<Int>()
    for (i in 0 until minOf(100, maskArray.size)) {
        sampleMaskValues.add(maskArray[i].toInt() and 0xFF)
    }
    Log.d("CameraScreen", "Sample mask values: $sampleMaskValues")

    // Count mask value distribution
    var zeroCount = 0
    var lowCount = 0  // 1-127
    var highCount = 0 // 128-255
    for (i in maskArray.indices) {
        val value = maskArray[i].toInt() and 0xFF
        when {
            value == 0 -> zeroCount++
            value < 128 -> lowCount++
            else -> highCount++
        }
    }
    Log.d("CameraScreen", "Mask value distribution - Zero: $zeroCount, Low (1-127): $lowCount, High (128-255): $highCount")

    // Try different interpretation: ML Kit might use float format or different byte order
    try {
        // Method 1: Direct pixel mapping without scaling if dimensions match
        if (maskWidth == width && maskHeight == height && maskArray.size == width * height) {
            Log.d("CameraScreen", "Using direct pixel mapping")
            for (i in originalPixels.indices) {
                val maskValue = maskArray[i].toInt() and 0xFF
                // Try both conventions: high value = person vs low value = person
                val isPersonHighValue = maskValue > 128
                val isPersonLowValue = maskValue < 128

                // Log a few samples to see which makes more sense
                if (i < 10) {
                    Log.d("CameraScreen", "Pixel $i: maskValue=$maskValue, highValue=$isPersonHighValue, lowValue=$isPersonLowValue")
                }

                // For now, try the inverted approach since the original wasn't working
                resultPixels[i] = if (maskValue < 128) {  // LOW values = person
                    originalPixels[i]
                } else {  // HIGH values = background
                    backgroundPixels[i]
                }
            }
        } else {
            // Method 2: Bilinear interpolation for scaling
            Log.d("CameraScreen", "Using scaled mapping with bilinear interpolation")
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val imageIndex = y * width + x

                    // Use bilinear interpolation for better scaling
                    val maskXFloat = x.toFloat() * (maskWidth - 1) / (width - 1)
                    val maskYFloat = y.toFloat() * (maskHeight - 1) / (height - 1)

                    val maskX1 = maskXFloat.toInt().coerceIn(0, maskWidth - 1)
                    val maskY1 = maskYFloat.toInt().coerceIn(0, maskHeight - 1)
                    val maskX2 = (maskX1 + 1).coerceIn(0, maskWidth - 1)
                    val maskY2 = (maskY1 + 1).coerceIn(0, maskHeight - 1)

                    // Get four corner values
                    val index1 = maskY1 * maskWidth + maskX1
                    val index2 = maskY1 * maskWidth + maskX2
                    val index3 = maskY2 * maskWidth + maskX1
                    val index4 = maskY2 * maskWidth + maskX2

                    val val1 = if (index1 < maskArray.size) maskArray[index1].toInt() and 0xFF else 0
                    val val2 = if (index2 < maskArray.size) maskArray[index2].toInt() and 0xFF else 0
                    val val3 = if (index3 < maskArray.size) maskArray[index3].toInt() and 0xFF else 0
                    val val4 = if (index4 < maskArray.size) maskArray[index4].toInt() and 0xFF else 0

                    // Simple average for interpolation
                    val avgMaskValue = (val1 + val2 + val3 + val4) / 4

                    resultPixels[imageIndex] = if (avgMaskValue < 128) {  // LOW values = person
                        originalPixels[imageIndex]
                    } else {  // HIGH values = background
                        backgroundPixels[imageIndex]
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Error in mask processing, using fallback", e)
        // Fallback: return original image
        originalBitmap.getPixels(resultPixels, 0, width, 0, 0, width, height)
    }

    // Set the result pixels to the bitmap
    resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)

    return resultBitmap
}

private fun replaceBackgroundImproved(
    originalBitmap: Bitmap,
    mask: SegmentationMask,
    backgroundManager: BackgroundManager,
    selectedBackgroundIndex: Int
): Bitmap {
    val width = originalBitmap.width
    val height = originalBitmap.height

    // Get the selected background from BackgroundManager
    val backgroundBitmap = backgroundManager.getBackgroundPreview(selectedBackgroundIndex, width, height)

    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Get original image pixels
    val originalPixels = IntArray(width * height)
    originalBitmap.getPixels(originalPixels, 0, width, 0, 0, width, height)

    // Get background pixels
    val backgroundPixels = IntArray(width * height)
    backgroundBitmap.getPixels(backgroundPixels, 0, width, 0, 0, width, height)

    // Get mask data - ensure buffer is properly positioned
    val maskBuffer = mask.buffer
    maskBuffer.rewind()
    val maskArray = FloatArray(maskBuffer.remaining() / 4) // ML Kit uses float values
    maskBuffer.asFloatBuffer().get(maskArray)

    // ML Kit mask dimensions
    val maskWidth = mask.width
    val maskHeight = mask.height

    Log.d("CameraScreen", "Improved mask dimensions: ${maskWidth}x${maskHeight}, Image dimensions: ${width}x${height}")
    Log.d("CameraScreen", "Mask array size: ${maskArray.size}, Expected: ${maskWidth * maskHeight}")

    // Debug mask values
    val sampleMaskValues = mutableListOf<Float>()
    for (i in 0 until minOf(20, maskArray.size)) {
        sampleMaskValues.add(maskArray[i])
    }
    Log.d("CameraScreen", "Sample mask values (float): $sampleMaskValues")

    // Create result pixels array
    val resultPixels = IntArray(width * height)

    // Apply mask with proper float handling
    for (y in 0 until height) {
        for (x in 0 until width) {
            val imageIndex = y * width + x

            // Scale coordinates to mask dimensions with proper bounds checking
            val maskX = (x.toFloat() * maskWidth / width).toInt().coerceIn(0, maskWidth - 1)
            val maskY = (y.toFloat() * maskHeight / height).toInt().coerceIn(0, maskHeight - 1)
            val maskIndex = maskY * maskWidth + maskX

            // Get mask confidence as float (0.0 to 1.0)
            val maskValue = if (maskIndex < maskArray.size) {
                maskArray[maskIndex]
            } else {
                0.0f
            }

            // Use threshold of 0.5 for person detection
            // Higher values = more likely to be person
            resultPixels[imageIndex] = if (maskValue > 0.5f) {
                originalPixels[imageIndex]  // Person pixel
            } else {
                backgroundPixels[imageIndex]  // Background pixel
            }
        }
    }

    // Set the result pixels to the bitmap
    resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)

    return resultBitmap
}

private fun savePhotoToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "playa_photo_${System.currentTimeMillis()}.jpg"
        val file = File(context.getExternalFilesDir(null), filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        Log.d("CameraScreen", "Photo saved: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e("CameraScreen", "Failed to save photo", e)
    }
}
