package com.capricallctx.playaphotobooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import java.io.IOException

data class BackgroundTextOverlay(
    val legend: String,
    val message: String
)

class BackgroundManager(val context: Context) {

    private var backgroundImages = mutableListOf<Bitmap>()
    var currentBackgroundIndex = 0

    private val backgroundTextOverlays = mapOf(
        0 to BackgroundTextOverlay("BURNING MAN 2025", "Tomorrow, Today."),
        1 to BackgroundTextOverlay("BURNING MAN 2025", "Tomorrow, Today."),
        2 to BackgroundTextOverlay("BURNING MAN 2025", "Tomorrow, Today."),
        3 to BackgroundTextOverlay("BURNING MAN 2025", "I pooped today!")
    )


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


    fun getBackgroundCount(): Int = backgroundImages.size

    fun getBackgroundName(index: Int): String {
        return when (index) {
            0 -> "Playa Dust"
            1 -> "The Burn"
            2 -> "OMG MUD"
            3 -> "Poop Today"
            else -> "Background ${index + 1}"
        }
    }

    fun getBackgroundTextOverlay(index: Int): BackgroundTextOverlay {
        return backgroundTextOverlays[index] ?: BackgroundTextOverlay("BURNING MAN 2025", "Tomorrow, Today.")
    }

    fun getBackgroundWithTextOverlay(index: Int, width: Int, height: Int): Bitmap {
        val backgroundBitmap = getBackgroundPreview(index, width, height)
        val textOverlay = getBackgroundTextOverlay(index)
        return addTextOverlayToBitmap(backgroundBitmap, textOverlay)
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

    private fun addTextOverlayToBitmap(bitmap: Bitmap, textOverlay: BackgroundTextOverlay): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width
        val height = result.height

        // Calculate font sizes based on image dimensions
        val legendTextSize = (width * 0.08f).coerceAtLeast(24f) // 8% of width, minimum 24px
        val messageTextSize = (width * 0.05f).coerceAtLeast(18f) // 5% of width, minimum 18px
        val padding = (width * 0.05f).coerceAtLeast(20f) // 5% of width, minimum 20px

        // Legend (top) text setup
        val legendPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = legendTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Message (bottom) text setup
        val messagePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = messageTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Background paint for text readability
        val backgroundPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            alpha = 180 // Semi-transparent
        }

        // Calculate text bounds
        val legendBounds = Rect()
        legendPaint.getTextBounds(textOverlay.legend, 0, textOverlay.legend.length, legendBounds)
        val messageBounds = Rect()
        messagePaint.getTextBounds(textOverlay.message, 0, textOverlay.message.length, messageBounds)

        // Calculate header block height (legend + message + padding)
        val legendHeight = legendBounds.height()
        val messageHeight = messageBounds.height()
        val headerBlockHeight = padding + legendHeight + padding + messageHeight + padding

        // Draw background rectangle for entire header block
        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            headerBlockHeight,
            backgroundPaint
        )

        // Draw legend text at top
        val legendY = padding + legendHeight
        canvas.drawText(
            textOverlay.legend,
            width / 2f,
            legendY,
            legendPaint
        )

        // Draw message text directly under legend
        val messageY = legendY + padding + messageHeight
        canvas.drawText(
            textOverlay.message,
            width / 2f,
            messageY,
            messagePaint
        )

        return result
    }
}
