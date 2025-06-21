package com.capricallctx.playaphotobooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.capricallctx.playaphotobooth.data.ConfigurationRepository
import com.capricallctx.playaphotobooth.ui.theme.PlayaPhotoboothTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayaPhotoboothTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoboothApp()
                }
            }
        }
    }
}

@Composable
fun PhotoboothApp() {
    val context = LocalContext.current
    val configurationRepository = remember { ConfigurationRepository(context) }
    val configuration by configurationRepository.configuration.collectAsState()
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "landing"
    ) {
        composable("landing") {
            LandingScreen(
                onTakeSelfiesClick = {
                    navController.navigate("camera")
                },
                onSettingsClick = {
                    navController.navigate("configuration")
                }
            )
        }
        
        composable("configuration") {
            ConfigurationScreen(
                configuration = configuration,
                onConfigurationChanged = { newConfig ->
                    configurationRepository.updateConfiguration(newConfig)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("camera") {
            CameraScreen(
                configuration = configuration,
                onNavigateToGallery = {
                    navController.navigate("gallery")
                }
            )
        }
        
        composable("gallery") {
            GalleryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}