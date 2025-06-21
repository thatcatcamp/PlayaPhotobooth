package com.capricallctx.playaphotobooth.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigurationRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "photobooth_config", Context.MODE_PRIVATE
    )
    
    private val _configuration = MutableStateFlow(loadConfiguration())
    val configuration: StateFlow<PhotoboothConfiguration> = _configuration.asStateFlow()
    
    fun updateConfiguration(newConfiguration: PhotoboothConfiguration) {
        _configuration.value = newConfiguration
        saveConfiguration(newConfiguration)
    }
    
    private fun loadConfiguration(): PhotoboothConfiguration {
        val campName = sharedPreferences.getString("camp_name", ConfigurationDefaults.DEFAULT_CAMP_NAME) 
            ?: ConfigurationDefaults.DEFAULT_CAMP_NAME
        
        val customBackgrounds = mutableListOf<CustomBackground>()
        
        for (i in 0 until ConfigurationDefaults.MAX_CUSTOM_BACKGROUNDS) {
            val id = sharedPreferences.getString("bg_${i}_id", null)
            val uriString = sharedPreferences.getString("bg_${i}_uri", null)
            val tagline = sharedPreferences.getString("bg_${i}_tagline", null)
            
            if (id != null && uriString != null && tagline != null) {
                customBackgrounds.add(
                    CustomBackground(
                        id = id,
                        imageUri = Uri.parse(uriString),
                        tagline = tagline
                    )
                )
            }
        }
        
        return PhotoboothConfiguration(
            campName = campName,
            customBackgrounds = customBackgrounds
        )
    }
    
    private fun saveConfiguration(configuration: PhotoboothConfiguration) {
        sharedPreferences.edit {
            putString("camp_name", configuration.campName)
            
            // Clear all existing background entries
            for (i in 0 until ConfigurationDefaults.MAX_CUSTOM_BACKGROUNDS) {
                remove("bg_${i}_id")
                remove("bg_${i}_uri")
                remove("bg_${i}_tagline")
            }
            
            // Save current backgrounds
            configuration.customBackgrounds.forEachIndexed { index, background ->
                if (index < ConfigurationDefaults.MAX_CUSTOM_BACKGROUNDS) {
                    putString("bg_${index}_id", background.id)
                    putString("bg_${index}_uri", background.imageUri?.toString())
                    putString("bg_${index}_tagline", background.tagline)
                }
            }
        }
    }
}