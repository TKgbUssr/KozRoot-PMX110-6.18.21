package com.KozOnekey.models

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap

data class WallpaperConfig(
    val uri: Uri? = null,
    val presetId: String? = "nebula",
    val blurRadius: Float = 80f,
    val dimAlpha: Float = 0.20f,
    val scaleMode: String = "crop", // crop, fit, zoom
    val bitmap: ImageBitmap? = null
)
