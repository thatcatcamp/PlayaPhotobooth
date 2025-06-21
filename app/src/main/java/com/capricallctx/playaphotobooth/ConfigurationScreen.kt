package com.capricallctx.playaphotobooth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.capricallctx.playaphotobooth.data.ConfigurationDefaults
import com.capricallctx.playaphotobooth.data.CustomBackground
import com.capricallctx.playaphotobooth.data.PhotoboothConfiguration
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    configuration: PhotoboothConfiguration,
    onConfigurationChanged: (PhotoboothConfiguration) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var campName by remember { mutableStateOf(configuration.campName) }
    var customBackgrounds by remember { mutableStateOf(configuration.customBackgrounds) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (customBackgrounds.size < ConfigurationDefaults.MAX_CUSTOM_BACKGROUNDS) {
                val newBackground = CustomBackground(
                    id = UUID.randomUUID().toString(),
                    imageUri = it,
                    tagline = ""
                )
                customBackgrounds = customBackgrounds + newBackground
                onConfigurationChanged(
                    configuration.copy(
                        campName = campName,
                        customBackgrounds = customBackgrounds
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Camp Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = campName,
                            onValueChange = { 
                                campName = it
                                onConfigurationChanged(
                                    configuration.copy(
                                        campName = campName,
                                        customBackgrounds = customBackgrounds
                                    )
                                )
                            },
                            label = { Text("Enter your camp name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Custom Backgrounds",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${customBackgrounds.size}/${ConfigurationDefaults.MAX_CUSTOM_BACKGROUNDS}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add custom backgrounds with taglines for your photobooth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            itemsIndexed(customBackgrounds) { index, background ->
                CustomBackgroundItem(
                    background = background,
                    onTaglineChanged = { newTagline ->
                        val updatedBackgrounds = customBackgrounds.toMutableList()
                        updatedBackgrounds[index] = background.copy(tagline = newTagline)
                        customBackgrounds = updatedBackgrounds
                        onConfigurationChanged(
                            configuration.copy(
                                campName = campName,
                                customBackgrounds = customBackgrounds
                            )
                        )
                    },
                    onDelete = {
                        customBackgrounds = customBackgrounds.filterIndexed { i, _ -> i != index }
                        onConfigurationChanged(
                            configuration.copy(
                                campName = campName,
                                customBackgrounds = customBackgrounds
                            )
                        )
                    }
                )
            }

            if (customBackgrounds.size < ConfigurationDefaults.MAX_CUSTOM_BACKGROUNDS) {
                item {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Custom Background")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomBackgroundItem(
    background: CustomBackground,
    onTaglineChanged: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background ${background.id.take(8)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete background",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            background.imageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = background.tagline,
                onValueChange = onTaglineChanged,
                label = { Text("Tagline or saying") },
                placeholder = { Text("Enter a fun tagline for this background") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}