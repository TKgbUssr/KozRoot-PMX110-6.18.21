package com.KozOnekey.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.rememberHapticFeedback
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 苹果 VisionOS / iOS 控制中心级别极致奢华液态玻璃光柱滑块 (Liquid Glass Control Capsule Slider)
 *
 * 核心高级设计特性：
 * 1. 【一体化微晶光柱腔体 (Glass Prism Chamber)】：
 *    54dp 饱满圆润胶囊形态，深层光学内阴影与精密仪器刻度微点，透出高奢晶体质感。
 * 2. 【电光琉璃流体充盈 (Luminous Fluid Radiance)】：
 *    多阶天蓝/深蓝琉璃渐变，液位顶端带有表面张力光柱光弧（Meniscus Glow Bead）与上沿镜面反射光弧。
 * 3. 【悬浮嵌壁微晶球 (Embedded Glass Pearl)】：
 *    左侧立体微凸透镜珍珠光圈，水滴图标通透生动，即使被液体覆盖依然具有折射反差。
 * 4. 【全域触控微弹簧动效与高光 HUD 数字 (Fluid HUD Pill)】：
 *    手指按压时微幅扩张并伴随发光强化，杜绝轨道内部随意堆砌生硬文字；右侧为精致透光发光百分比胶囊。
 * 5. 【机械触觉反馈 (Tick Feedback)】：每滑动 5% 产生清脆利落的机械齿轮力反馈。
 */
@Composable
fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0.10f..1.0f,
    icon: ImageVector = Icons.Rounded.Opacity
) {
    val haptic = rememberHapticFeedback()

    var isInteracting by remember { mutableStateOf(false) }
    var lastHapticStep by remember { mutableFloatStateOf(value) }

    // 交互时的轻微动态缩放与发光强化
    val interactionScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.015f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "sliderInteractionScale"
    )

    val sliderHeight by animateDpAsState(
        targetValue = if (isInteracting) 56.dp else 52.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "sliderHeight"
    )

    val capsuleShape = RoundedCornerShape(26.dp)
    val clampedFraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(sliderHeight)
            .scale(interactionScale)
            .shadow(
                elevation = if (isInteracting) 12.dp else 4.dp,
                shape = capsuleShape,
                ambientColor = Color(0x35000000),
                spotColor = if (isDark) Color(0x5038BDF8) else Color(0x352563EB)
            )
            .clip(capsuleShape)
            // 晶体轨道基底：深层玻璃折射与光晕
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color(0x401E293B),
                        Color(0x280F172A)
                    ) else listOf(
                        Color(0x50FFFFFF),
                        Color(0x30E2E8F0)
                    )
                )
            )
            // 双阶菲涅尔光学折射边框：顶沿高光弦线 -> 底沿环境光
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.40f),
                        if (isDark) Color(0x2538BDF8) else Color(0x303B82F6)
                    )
                ),
                shape = capsuleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isInteracting = true
                        val totalW = size.width.toFloat()
                        if (totalW > 0f) {
                            val newFraction = (offset.x / totalW).coerceIn(0f, 1f)
                            val computed = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                            haptic.perform(HapticType.TICK)
                            onValueChange(computed)
                        }
                        tryAwaitRelease()
                        isInteracting = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isInteracting = true },
                    onDragEnd = { isInteracting = false },
                    onDragCancel = { isInteracting = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val totalW = size.width.toFloat()
                        if (totalW > 0f) {
                            val newFraction = (change.position.x / totalW).coerceIn(0f, 1f)
                            val computed = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                            if (abs(computed - lastHapticStep) >= 0.05f) {
                                haptic.perform(HapticType.TICK)
                                lastHapticStep = computed
                            }
                            onValueChange(computed)
                        }
                    }
                )
            }
    ) {
        val totalWidth = maxWidth
        val liquidFillWidth = totalWidth * clampedFraction

        // 1. 轨道背景精密仪表微刻度点（25%, 50%, 75%）
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tickSteps = listOf(0.25f, 0.50f, 0.75f)
            tickSteps.forEach { fraction ->
                val x = size.width * fraction
                drawCircle(
                    color = (if (isDark) Color.White else Color(0xFF64748B)).copy(alpha = 0.16f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(x, size.height * 0.5f)
                )
            }
        }

        // 2. 内部多层高光流体充盈层
        if (clampedFraction > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(liquidFillWidth)
                    .clip(capsuleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = if (isDark) listOf(
                                Color(0xFF1E3A8A).copy(alpha = 0.85f),
                                Color(0xFF2563EB).copy(alpha = 0.90f),
                                Color(0xFF0284C7).copy(alpha = 0.92f),
                                Color(0xFF38BDF8).copy(alpha = 0.96f)
                            ) else listOf(
                                Color(0xFF2563EB).copy(alpha = 0.80f),
                                Color(0xFF3B82F6).copy(alpha = 0.88f),
                                Color(0xFF60A5FA).copy(alpha = 0.95f)
                            )
                        )
                    )
                    .drawBehind {
                        // 上沿镜面反射光弧
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.35f)
                                )
                            ),
                            start = Offset(0f, 1.2.dp.toPx()),
                            end = Offset(size.width, 1.2.dp.toPx()),
                            strokeWidth = 1.8.dp.toPx()
                        )

                        // 流体表面张力前沿发光柱 (Liquid Meniscus Leading Edge)
                        drawLine(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color(0xFFBAE6FD).copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.60f)
                                )
                            ),
                            start = Offset(size.width - 1.5.dp.toPx(), 4.dp.toPx()),
                            end = Offset(size.width - 1.5.dp.toPx(), size.height - 4.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
            )
        }

        // 3. 悬浮嵌壁微晶球 (左侧) 与动态 HUD 百分比发光胶囊 (右侧)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 水滴立体微晶珍珠
            val pearlShape = CircleShape
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = pearlShape,
                        ambientColor = Color(0x30000000),
                        spotColor = Color(0x4038BDF8)
                    )
                    .clip(pearlShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (clampedFraction > 0.15f) 0.28f else 0.15f),
                                Color.White.copy(alpha = if (clampedFraction > 0.15f) 0.14f else 0.06f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.60f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        shape = pearlShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Slider Lens",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            // 右侧发光数字 HUD 胶囊
            val percentValue = (value * 100).roundToInt()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isInteracting) {
                            Color(0x550F172A)
                        } else {
                            Color(0x35000000)
                        }
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                if (isInteracting) Color(0xFFBAE6FD) else Color.White.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$percentValue%",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

/**
 * 液态玻璃 VisionOS 浮动透镜分段选择胶囊 (Liquid Glass Gliding Lens Segmented Bar)
 *
 * 经典高奢设计：
 * 1. 【弹簧透镜滑动滑块 (Animated Gliding Lens)】：滑块在预设档位间如液态水银般平滑飞跃滑动。
 * 2. 【多态视觉触感 (Tactile Preset Capsules)】：20% 极透、50% 通透、75% 微透、100% 经典，无论滑块处于任意数值，都有清脆平滑的吸附与切换体验。
 */
@Composable
fun LiquidGlassSegmentedBar(
    currentAlpha: Float,
    onAlphaSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val haptic = rememberHapticFeedback()

    val options = remember {
        listOf(
            0.20f to "极透 20%",
            0.50f to "通透 50%",
            0.75f to "微透 75%",
            1.00f to "经典 100%"
        )
    }

    val barShape = RoundedCornerShape(20.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(barShape)
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color(0x351E293B),
                        Color(0x200F172A)
                    ) else listOf(
                        Color(0x50FFFFFF),
                        Color(0x30E2E8F0)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.75f),
                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.20f)
                    )
                ),
                shape = barShape
            )
            .padding(3.dp)
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / options.size

        // 计算当前数值与哪个选项最匹配（阈值 0.08f）
        val matchedIndex = options.indexOfFirst { abs(currentAlpha - it.first) < 0.08f }

        // 动画滑动透镜位置
        if (matchedIndex != -1) {
            val targetOffset = tabWidth * matchedIndex
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = "lensOffset"
            )

            val lensShape = RoundedCornerShape(17.dp)
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 6.dp,
                        shape = lensShape,
                        ambientColor = Color(0x30000000),
                        spotColor = Color(0x5038BDF8)
                    )
                    .clip(lensShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.95f),
                                Color(0xFF1D4ED8).copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.75f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        shape = lensShape
                    )
                    .drawBehind {
                        // 透镜上沿高光弦线
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.2f)
                                )
                            ),
                            start = Offset(0f, 1.dp.toPx()),
                            end = Offset(size.width, 1.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
            )
        }

        // 文字选项层
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, (alphaVal, title) ->
                val isSelected = index == matchedIndex

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.perform(HapticType.CLICK)
                            onAlphaSelected(alphaVal)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) {
                            Color.White
                        } else if (isDark) {
                            Color(0xFF94A3B8)
                        } else {
                            Color(0xFF475569)
                        }
                    )
                }
            }
        }
    }
}
