package com.KozOnekey.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.rememberHapticFeedback
import kotlin.math.roundToInt

/**
 * 极简高端纵向流体透明度滑块 (Vertical Liquid Glass Slider)
 *
 * 弹出在卡片右侧外部，极简、精致、无多余装饰，支持触控与拖拽平滑调节卡片透明度。
 * 遵循 iOS 控制中心 / VisionOS 空间悬浮胶囊设计语言。
 */
@Composable
fun VerticalLiquidGlassSlider(
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    heightDp: Dp,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    minAlpha: Float = 0.15f,
    maxAlpha: Float = 1.0f
) {
    val haptic = rememberHapticFeedback()
    val capsuleShape = RoundedCornerShape(20.dp)

    var isDragging by remember { mutableStateOf(false) }
    var lastHapticStep by remember { mutableFloatStateOf(alpha) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sliderDragScale"
    )

    val percent = (alpha * 100).roundToInt()

    // 胶囊外壳 (宽度 40dp，高度与卡片严格一致)
    Box(
        modifier = modifier
            .width(42.dp)
            .height(heightDp)
            .scale(animatedScale)
            .shadow(
                elevation = 10.dp,
                shape = capsuleShape,
                ambientColor = if (isDark) Color(0x50000000) else Color(0x20000000),
                spotColor = Color.Transparent
            )
            .clip(capsuleShape)
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xE60F172A),
                            Color(0xF01E293B),
                            Color(0xE60F172A)
                        )
                    } else {
                        listOf(
                            Color(0xE6F8FAFC),
                            Color(0xF0E2E8F0),
                            Color(0xE6F1F5F9)
                        )
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.65f),
                        Color.White.copy(alpha = if (isDark) 0.10f else 0.25f)
                    )
                ),
                shape = capsuleShape
            )
            .pointerInput(minAlpha, maxAlpha) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    down.consume()
                    val h = size.height.toFloat()
                    if (h > 0f) {
                        val rawFraction = (1f - (down.position.y / h)).coerceIn(0f, 1f)
                        val targetAlpha = (minAlpha + rawFraction * (maxAlpha - minAlpha)).coerceIn(minAlpha, maxAlpha)
                        haptic.perform(HapticType.CLICK)
                        onAlphaChange(targetAlpha)
                        lastHapticStep = targetAlpha
                    }
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            break
                        }
                        change.consume()
                        if (h > 0f) {
                            val rawFraction = (1f - (change.position.y / h)).coerceIn(0f, 1f)
                            val targetAlpha = (minAlpha + rawFraction * (maxAlpha - minAlpha)).coerceIn(minAlpha, maxAlpha)
                            if (kotlin.math.abs(targetAlpha - lastHapticStep) >= 0.05f) {
                                haptic.perform(HapticType.TICK)
                                lastHapticStep = targetAlpha
                            }
                            onAlphaChange(targetAlpha)
                        }
                    }
                    isDragging = false
                }
            }
    ) {
        // 画布绘制充盈液柱、刻度微线、液面光晕
        val fillProgress = ((alpha - minAlpha) / (maxAlpha - minAlpha)).coerceIn(0f, 1f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val fillHeight = h * fillProgress

            // 1. 刻度参考微细线 (25%, 50%, 75%)
            val tickSteps = listOf(0.25f, 0.5f, 0.75f)
            tickSteps.forEach { stepProgress ->
                val tickY = h * (1f - stepProgress)
                drawLine(
                    color = Color.White.copy(alpha = if (isDark) 0.18f else 0.30f),
                    start = Offset(w * 0.32f, tickY),
                    end = Offset(w * 0.68f, tickY),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 2. 纵向液体填充 (从底端向上充盈)
            if (fillHeight > 0f) {
                val topY = h - fillHeight
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8), // 顶部高光亮青
                            Color(0xFF2563EB), // 中段活力深蓝
                            Color(0xFF1D4ED8)  // 底座深邃皇家蓝
                        ),
                        startY = topY,
                        endY = h
                    ),
                    topLeft = Offset(0f, topY),
                    size = Size(w, fillHeight)
                )

                // 3. 液面张力反光边缘线 (Meniscus Highlight)
                drawLine(
                    color = Color(0xFFE0F2FE),
                    start = Offset(2.dp.toPx(), topY),
                    end = Offset(w - 2.dp.toPx(), topY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. 顶部柔和玻璃内阴影与反光
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(w * 0.5f, 16.dp.toPx()),
                    radius = 24.dp.toPx()
                ),
                radius = 24.dp.toPx(),
                center = Offset(w * 0.5f, 16.dp.toPx())
            )
        }

        // 内部极简视觉排版：顶部半透图标，底部实时百分比
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部水滴透明度图标
            Icon(
                imageVector = Icons.Rounded.Opacity,
                contentDescription = "透明度",
                tint = if (fillProgress > 0.82f) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
                modifier = Modifier.size(17.dp)
            )

            // 底部百分比数字
            Text(
                text = "$percent%",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (fillProgress < 0.22f) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)) else Color.White,
                letterSpacing = (-0.3).sp
            )
        }
    }
}
