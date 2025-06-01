package com.capricallctx.playaphotobooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        photos = loadPhotosFromStorage(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Photo Gallery") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No photos yet!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Take some photos to see them here",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photoFile ->
                    PhotoGridItem(
                        photoFile = photoFile,
                        onClick = { selectedPhoto = photoFile }
                    )
                }
            }
        }
    }

    selectedPhoto?.let { photo ->
        PhotoDetailDialog(
            photoFile = photo,
            onDismiss = { selectedPhoto = null }
        )
    }
}

@Composable
private fun PhotoGridItem(
    photoFile: File,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoFile) {
        bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun PhotoDetailDialog(
    photoFile: File,
    onDismiss: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(photoFile) {
        bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo Options") },
        text = {
            Column {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Photo detail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = { shareViaBluetooth(context, photoFile) },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share via Bluetooth",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Bluetooth",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = { sharePhoto(context, photoFile) },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun loadPhotosFromStorage(context: Context): List<File> {
    val directory = context.getExternalFilesDir(null)
    return directory?.listFiles { file ->
        file.name.startsWith("playa_photo_") && file.name.endsWith(".jpg")
    }?.sortedByDescending { it.lastModified() } ?: emptyList()
}

private fun shareViaBluetooth(context: Context, photoFile: File) {
    try {
        val bluetoothManager = BluetoothManager(context)

        if (!bluetoothManager.isBluetoothAvailable()) {
            android.widget.Toast.makeText(
                context,
                "Bluetooth not available",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Enable discoverability and start sharing
        if (context is androidx.activity.ComponentActivity) {
            bluetoothManager.enableDiscoverability(context)
        }

        android.widget.Toast.makeText(
            context,
            "Bluetooth sharing enabled. Other devices can now connect to receive this photo.",
            android.widget.Toast.LENGTH_LONG
        ).show()

        // In a real implementation, you'd handle the async sharing in a coroutine
        // For now, we'll just show instructions
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Failed to enable Bluetooth sharing: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}


private fun sharePhoto(context: Context, photoFile: File) {
    try {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "image/jpeg"
            putExtra(
                android.content.Intent.EXTRA_STREAM,
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
            )
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Photo"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Failed to share photo",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
