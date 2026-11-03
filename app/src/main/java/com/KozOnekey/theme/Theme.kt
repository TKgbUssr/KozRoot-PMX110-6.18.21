package com.KozOnekey.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class BackgroundMode(
    val darkTitle: String,
    val lightTitle: String,
    val subtitle: String
) {
    LIQUID_GLASS("KozRoot 风格", "KozRoot 风格", "纯拟物光学高光折射与物理透景"),
    FROSTED_BLUR("高斯毛玻璃", "高斯毛玻璃", "经典深层高斯模糊与弥散辉光"),
    ACRYLIC("亚克力", "亚克力", "微噪点纹理与透光亚克力层"),
    MINIMAL_CLEAN("极简纯黑 (OLED)", "极简素白 (Pure)", "高对比度纯净背景"),
    CUSTOM_WALLPAPER("自定义壁纸", "自定义壁纸", "个性化相册图片与参数微调");

    fun getDisplayTitle(isDark: Boolean): String = if (isDark) darkTitle else lightTitle
}

private val DarkColorScheme = darkColorScheme(
    primary = OceanBluePrimary,
    onPrimary = Color.White,
    secondary = CyberPurplePrimary,
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = GlassDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = GlassDarkCard,
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBluePrimary,
    onPrimary = Color.White,
    secondary = CyberPurplePrimary,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = GlassWhiteLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
