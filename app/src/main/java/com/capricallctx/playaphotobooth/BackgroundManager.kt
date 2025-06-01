package com.capricallctx.playaphotobooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import java.io.IOException

class BackgroundManager(private val context: Context) {
    
    private var backgroundImages = mutableListOf<Bitmap>()
    private var currentBackgroundIndex = 0
    
    // Default Burning Man playa background
    private val defaultBackground = 0
    
    init {
        loadBackgroundImages()
    }
    
    private fun loadBackgroundImages() {
        try {
            // Add default procedural background first
            backgroundImages.add(createBurningManBackground(1080, 1920)) // Common phone resolution
            
            // Load background images from assets
            val assetManager = context.assets
            val assetFiles = assetManager.list("") ?: emptyArray()
            
            // Find all bg*.png files
            val backgroundFiles = assetFiles.filter { 
                it.matches(Regex("bg\\d+\\.png", RegexOption.IGNORE_CASE)) 
            }.sorted()
            
            Log.d("BackgroundManager", "Found background files: ${backgroundFiles.joinToString()}")
            
            for (filename in backgroundFiles) {
                try {
                    assetManager.open(filename).use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            backgroundImages.add(bitmap)
                            Log.d("BackgroundManager", "Loaded background: $filename (${bitmap.width}x${bitmap.height})")
                        }
                    }
                } catch (e: IOException) {
                    Log.e("BackgroundManager", "Failed to load background: $filename", e)
                }
            }
            
            Log.d("BackgroundManager", "Total backgrounds loaded: ${backgroundImages.size}")
        } catch (e: Exception) {
            Log.e("BackgroundManager", "Failed to load backgrounds", e)
            // Ensure we always have at least the default background
            if (backgroundImages.isEmpty()) {
                backgroundImages.add(createBurningManBackground(1080, 1920))
            }
        }
    }
    
    fun getCurrentBackground(width: Int, height: Int): Bitmap {
        if (backgroundImages.isEmpty()) {
            return createBurningManBackground(width, height)
        }
        
        val originalBg = backgroundImages[currentBackgroundIndex]
        
        // Scale background to match the required dimensions
        return Bitmap.createScaledBitmap(originalBg, width, height, true)
    }
    
    fun getBackgroundPreview(index: Int, width: Int, height: Int): Bitmap {
        if (index >= backgroundImages.size || index < 0) {
            return createBurningManBackground(width, height)
        }
        
        val originalBg = backgroundImages[index]
        return Bitmap.createScaledBitmap(originalBg, width, height, true)
    }
    
    fun cycleToNextBackground(): Int {
        if (backgroundImages.size > 1) {
            currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundImages.size
        }
        Log.d("BackgroundManager", "Switched to background index: $currentBackgroundIndex")
        return currentBackgroundIndex
    }
    
    fun cycleToPreviousBackground(): Int {
        if (backgroundImages.size > 1) {
            currentBackgroundIndex = if (currentBackgroundIndex == 0) {
                backgroundImages.size - 1
            } else {
                currentBackgroundIndex - 1
            }
        }
        Log.d("BackgroundManager", "Switched to background index: $currentBackgroundIndex")
        return currentBackgroundIndex
    }
    
    fun getCurrentBackgroundIndex(): Int = currentBackgroundIndex
    
    fun getBackgroundCount(): Int = backgroundImages.size
    
    fun getBackgroundName(index: Int): String {
        return when (index) {
            0 -> "Playa Dust"
            else -> "Background ${index + 1}"
        }
    }
    
    private fun createBurningManBackground(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Create a gradient from sandy yellow to orange (simulating playa dust and sunset)
        val paint = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    android.graphics.Color.rgb(255, 218, 142), // Sandy yellow
                    android.graphics.Color.rgb(255, 165, 79),  // Orange
                    android.graphics.Color.rgb(139, 69, 19)    // Brown
                ),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}