package com.KozOnekey.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

/**
 * 顶部系统状态栏区域容器
 */
@Composable
fun LiquidGlassStatusBar(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let {
        if (it > 20.dp) it else 48.dp
    }
    val totalHeight = statusBarPadding + 44.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
    )
}
