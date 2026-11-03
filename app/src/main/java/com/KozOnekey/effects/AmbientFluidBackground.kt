package com.KozOnekey.effects

import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.KozOnekey.components.PRESET_WALLPAPERS
import com.KozOnekey.models.WallpaperConfig
import com.KozOnekey.theme.BackgroundMode
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AmbientFluidBackground(
    mode: BackgroundMode,
    wallpaperConfig: WallpaperConfig,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // 使用单调物理时钟驱动流体运动（永不重置重启，数学上绝对连续 C-infinity，彻底杜绝周期瞬移跳变突兀感）
    val t by produceState(0f) {
        while (true) {
            withFrameMillis { frameTimeMillis ->
                value = (frameTimeMillis / 1000f) * 0.35f
            }
        }
    }

    // Load custom image bitmap asynchronously if URI is present
    val loadedBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = wallpaperConfig.uri) {
        val uri = wallpaperConfig.uri
        value = if (uri != null) {
            runCatching {
                val bmp = if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                bmp?.asImageBitmap()
            }.getOrNull()
        } else {
            null
        }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        when {
            // Case 1: Custom image from user gallery
            mode == BackgroundMode.CUSTOM_WALLPAPER && loadedBitmap != null -> {
                val scale = when (wallpaperConfig.scaleMode) {
                    "fit" -> ContentScale.Fit
                    "zoom" -> ContentScale.FillBounds
                    else -> ContentScale.Crop
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaperConfig.blurRadius > 0f) {
                                renderEffect = RenderEffect.createBlurEffect(
                                    wallpaperConfig.blurRadius,
                                    wallpaperConfig.blurRadius,
                                    Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                ) {
                    Image(
                        bitmap = loadedBitmap!!,
                        contentDescription = "Custom Wallpaper",
                        contentScale = scale,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dim overlay layer
                if (wallpaperConfig.dimAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isDark) Color.Black.copy(alpha = wallpaperConfig.dimAlpha)
                                else Color.White.copy(alpha = (wallpaperConfig.dimAlpha * 0.7f).coerceAtMost(0.6f))
                            )
                    )
                }
            }

            // Case 2: Custom Wallpaper with preset gradient
            mode == BackgroundMode.CUSTOM_WALLPAPER -> {
                val preset = PRESET_WALLPAPERS.find { it.id == wallpaperConfig.presetId } ?: PRESET_WALLPAPERS[0]
                Canvas(modifier = Modifier.fillMaxSize().background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))) {
                    val w = size.width
                    val h = size.height

                    preset.gradient.forEachIndexed { i, c ->
                        val rX = w * (0.3f + (i * 0.2f) % 0.6f) + cos(t + i) * (w * 0.15f)
                        val rY = h * (0.2f + (i * 0.25f) % 0.7f) + sin(t + i) * (h * 0.12f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(c.copy(alpha = if (isDark) 0.65f else 0.45f), Color.Transparent),
                                center = Offset(rX, rY),
                                radius = w * 0.75f
                            ),
                            radius = w * 0.75f,
                            center = Offset(rX, rY)
                        )
                    }
                }

                if (wallpaperConfig.dimAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isDark) Color.Black.copy(alpha = wallpaperConfig.dimAlpha)
                                else Color.White.copy(alpha = (wallpaperConfig.dimAlpha * 0.5f).coerceAtMost(0.5f))
                            )
                    )
                }
            }

            // Case 3: Liquid Glass background with dynamic fluid orbs
            mode == BackgroundMode.LIQUID_GLASS -> {
                val baseBg = if (isDark) Color(0xFF0A0F1D) else Color(0xFFF1F5F9)
                Canvas(modifier = Modifier.fillMaxSize().background(baseBg)) {
                    val w = size.width
                    val h = size.height

                    // Orb 1: Sky / Cyan Blue (Fluid Orb in upper right)
                    val orb1Color = if (isDark) Color(0xFF1E40AF).copy(alpha = 0.55f) else Color(0xFF60A5FA).copy(alpha = 0.48f)
                    val orb1Center = Offset(
                        w * 0.75f + cos(t) * (w * 0.15f),
                        h * 0.22f + sin(t) * (h * 0.08f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to orb1Color,
                            0.55f to orb1Color.copy(alpha = if (isDark) orb1Color.alpha * 0.60f else orb1Color.alpha * 0.50f),
                            1.0f to Color.Transparent,
                            center = orb1Center,
                            radius = w * 0.72f
                        ),
                        radius = w * 0.72f,
                        center = orb1Center
                    )

                    // Orb 2: Lavender / Royal Violet (Fluid Orb in mid left)
                    val orb2Color = if (isDark) Color(0xFF6B21A8).copy(alpha = 0.45f) else Color(0xFFC084FC).copy(alpha = 0.42f)
                    val orb2Center = Offset(
                        w * 0.25f - sin(t * 0.8f) * (w * 0.12f),
                        h * 0.55f + cos(t * 0.8f) * (h * 0.1f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to orb2Color,
                            0.55f to orb2Color.copy(alpha = if (isDark) orb2Color.alpha * 0.60f else orb2Color.alpha * 0.50f),
                            1.0f to Color.Transparent,
                            center = orb2Center,
                            radius = w * 0.82f
                        ),
                        radius = w * 0.82f,
                        center = orb2Center
                    )

                    // Orb 3: Mint / Jade Teal (Fluid Orb flowing right across the bottom navigation dock area)
                    val orb3Color = if (isDark) Color(0xFF065F46).copy(alpha = 0.40f) else Color(0xFF34D399).copy(alpha = 0.44f)
                    val orb3Center = Offset(
                        w * 0.50f + cos(t * 1.1f) * (w * 0.25f),
                        h * 0.83f + sin(t * 1.1f) * (h * 0.08f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to orb3Color,
                            0.55f to orb3Color.copy(alpha = if (isDark) orb3Color.alpha * 0.65f else orb3Color.alpha * 0.52f),
                            1.0f to Color.Transparent,
                            center = orb3Center,
                            radius = w * 0.85f
                        ),
                        radius = w * 0.85f,
                        center = orb3Center
                    )

                    // Orb 4: Rose / Coral Amber (Secondary soft fluid accent at bottom right for rich liquid glass refraction)
                    val orb4Color = if (isDark) Color(0xFF831843).copy(alpha = 0.30f) else Color(0xFFFB7185).copy(alpha = 0.32f)
                    val orb4Center = Offset(
                        w * 0.80f - sin(t * 0.75f) * (w * 0.18f),
                        h * 0.88f - cos(t * 0.75f) * (h * 0.07f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to orb4Color,
                            0.55f to orb4Color.copy(alpha = if (isDark) orb4Color.alpha * 0.60f else orb4Color.alpha * 0.45f),
                            1.0f to Color.Transparent,
                            center = orb4Center,
                            radius = w * 0.75f
                        ),
                        radius = w * 0.75f,
                        center = orb4Center
                    )
                }
            }

            // Case 4: Frosted Gaussian Blur
            mode == BackgroundMode.FROSTED_BLUR -> {
                val baseBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                Canvas(modifier = Modifier.fillMaxSize().background(baseBg)) {
                    val w = size.width
                    val h = size.height

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE)).copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.3f),
                            radius = w * 0.9f
                        ),
                        radius = w * 0.9f,
                        center = Offset(w * 0.5f, h * 0.3f)
                    )
                }
            }

            // Case 5: Acrylic Fluent Material
            mode == BackgroundMode.ACRYLIC -> {
                val bgBrush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF111827), Color(0xFF0B0F19), Color(0xFF030712))
                    } else {
                        listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                    }
                )
                Box(modifier = Modifier.fillMaxSize().background(bgBrush))
            }

            // Case 6: Minimal Clean (Pure OLED Black or Pure Clean White)
            mode == BackgroundMode.MINIMAL_CLEAN -> {
                val solidColor = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
                Box(modifier = Modifier.fillMaxSize().background(solidColor))
            }
        }

        // Foreground content on top of background
        content()
    }
}
