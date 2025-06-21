package com.capricallctx.playaphotobooth.data

import android.net.Uri

data class CustomBackground(
    val id: String = "",
    val imageUri: Uri? = null,
    val tagline: String = ""
)

data class PhotoboothConfiguration(
    val campName: String = "CAT Camp",
    val customBackgrounds: List<CustomBackground> = emptyList()
)

object ConfigurationDefaults {
    const val MAX_CUSTOM_BACKGROUNDS = 4
    const val DEFAULT_CAMP_NAME = "CAT Camp"
}