package com.KozOnekey.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.rememberHapticFeedback
import com.KozOnekey.models.HeroAnimationType
import com.KozOnekey.theme.BackgroundMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 英雄状态卡片 (Radar Hero Card)
 *
 * 保持纯粹浑然一体的深邃渐变质感，纯净无边框，极简无多余按钮。
 * 右侧动效支持无限数学连续演算 (C-infinity)，杜绝周期重置突兀断裂。
 * 支持长按进入小窗式高度缩放调节，右下角带有仿小窗半透小弧形拖动手柄。
 */
@Composable
fun RadarHeroCard(
    statusText: String = "工作中",
    versionText: String = "版本: 41068-6",
    animationType: HeroAnimationType = HeroAnimationType.CLASSIC_RADAR,
    onAnimationTypeChange: (HeroAnimationType) -> Unit = {},
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    cardHeightDp: Float = 150f,
    onCardHeightChange: (Float) -> Unit = {},
    cardAlpha: Float = 0.75f,
    onCardAlphaChange: (Float) -> Unit = {},
    forceShowControls: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val density = LocalDensity.current
    val cardShape = RoundedCornerShape(28.dp)

    // 高度状态管理（支持 110dp ~ 180dp 范围，当前 180dp 为最大上限）
    var cardHeight by rememberSaveable { mutableFloatStateOf(cardHeightDp) }
    var isResizeMode by remember { mutableStateOf(false) }
    val controlsVisible = isResizeMode || forceShowControls
    // 外部（设置页 / 主页）修改高度时，同步到卡片内部状态
    LaunchedEffect(cardHeightDp) {
        if (kotlin.math.abs(cardHeight - cardHeightDp) > 0.5f) {
            cardHeight = cardHeightDp
        }
    }
    var isDraggingCorner by remember { mutableStateOf(false) }

    // 高度归一化进度 (0f = 110dp 最紧凑模式, 1f = 180dp 全高模式)
    val heightProgress = ((cardHeight - 110f) / (180f - 110f)).coerceIn(0f, 1f)

    // 单调物理时钟（以秒为单位无限连续单调递增，杜绝周期瞬变重置）
    val time by produceState(0f) {
        while (true) {
            withFrameMillis { frameTimeMillis ->
                value = (frameTimeMillis / 1000f)
            }
        }
    }

    val bgGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = cardAlpha),
                Color(0xFF0F172A).copy(alpha = cardAlpha),
                Color(0xFF1E3A8A).copy(alpha = cardAlpha)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF334155).copy(alpha = cardAlpha),
                Color(0xFF1E293B).copy(alpha = cardAlpha),
                Color(0xFF2563EB).copy(alpha = cardAlpha)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // 调整模式下轻微透亮边框，或者半透明模式下的精致微光晶体边框
    val borderModifier = if (isResizeMode) {
        Modifier.border(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xA038BDF8),
                    Color(0x80818CF8),
                    Color(0xA038BDF8)
                )
            ),
            shape = cardShape
        )
    } else if (cardAlpha < 0.98f) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.28f * (1f - cardAlpha * 0.4f)).coerceIn(0.10f, 0.35f)),
                    Color.White.copy(alpha = (0.10f * (1f - cardAlpha * 0.4f)).coerceIn(0.04f, 0.18f))
                )
            ),
            shape = cardShape
        )
    } else {
        Modifier
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(cardHeight.dp)
            .shadow(
                elevation = (8 * cardAlpha).dp,
                shape = cardShape,
                ambientColor = if (isDark) Color(0x35000000).copy(alpha = (0.25f * cardAlpha).coerceIn(0f, 0.25f)) else Color(0x18000000).copy(alpha = (0.15f * cardAlpha).coerceIn(0f, 0.15f)),
                spotColor = Color.Transparent
            )
            .clip(cardShape)
            .background(bgGradient)
            .then(borderModifier)
            .pointerInput(isResizeMode) {
                if (!isResizeMode && !forceShowControls) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.perform(HapticType.HEAVY_CLICK)
                            isResizeMode = true
                        }
                    )
                }
            }
    ) {
        // 右侧艺术动效画布（由设置页面统一控制切换，随高度自然微调居中与缩放）
        AnimatedContent(
            targetState = animationType,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "heroCanvasSwitch",
            modifier = Modifier.fillMaxSize()
        ) { currentType ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (currentType) {
                    HeroAnimationType.CLASSIC_RADAR -> {
                        drawClassicRadar(
                            centerOffset = Offset(
                                size.width * 0.82f,
                                size.height * (0.28f + (1f - heightProgress) * 0.16f)
                            ),
                            time = time,
                            maxR = size.width * (0.50f + 0.15f * heightProgress)
                        )
                    }
                    HeroAnimationType.FLUID_CORE -> {
                        drawFluidCore(
                            center = Offset(
                                size.width * 0.78f,
                                size.height * (0.46f + (1f - heightProgress) * 0.04f)
                            ),
                            time = time,
                            isDark = isDark
                        )
                    }
                    HeroAnimationType.AURORA_WAVES -> {
                        drawAuroraWaves(
                            startX = size.width * 0.40f,
                            endX = size.width + 12f,
                            centerY = size.height * 0.50f,
                            time = time,
                            isDark = isDark
                        )
                    }
                    HeroAnimationType.CORONA_ECLIPSE -> {
                        drawCoronaEclipse(
                            center = Offset(
                                size.width * 0.78f,
                                size.height * (0.46f + (1f - heightProgress) * 0.04f)
                            ),
                            time = time,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // 左侧状态文本与内核版本（随高度平滑适应字号与间距）
        val statusFontSize = (24 + 10 * heightProgress).sp
        val versionFontSize = (12 + 2 * heightProgress).sp
        val verticalPadding = (12 + 10 * heightProgress).dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = verticalPadding),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = statusText,
                color = Color.White,
                fontSize = statusFontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = versionText,
                color = Color(0xCCFFFFFF),
                fontSize = versionFontSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = (2 + 2 * heightProgress).dp)
            )
        }

        // 长按后右下角出现的“类似小窗半透小弧形”角标拖动把手
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.75f),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.75f),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(bottomEnd = 28.dp, topStart = 20.dp))
                    .background(
                        Color.White.copy(alpha = if (isDark) 0.18f else 0.28f)
                    )
                    .pointerInput(density) {
                        detectVerticalDragGestures(
                            onDragStart = { isDraggingCorner = true },
                            onDragEnd = { isDraggingCorner = false },
                            onDragCancel = { isDraggingCorner = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val deltaDp = dragAmount / density.density
                                val newHeight = (cardHeight + deltaDp).coerceIn(110f, 180f)
                                if (kotlin.math.abs(newHeight - cardHeight) >= 6f) {
                                    haptic.perform(HapticType.TICK)
                                }
                                cardHeight = newHeight
                                onCardHeightChange(newHeight)
                            }
                        )
                    }
            ) {
                // 绘制仿小窗悬浮窗经典的 3 道同心半透小弧形把手
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cornerCenter = Offset(size.width, size.height)
                    // 外弧 (半径 28dp)
                    drawArc(
                        color = Color.White.copy(alpha = 0.90f),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(cornerCenter.x - 28.dp.toPx(), cornerCenter.y - 28.dp.toPx()),
                        size = Size(56.dp.toPx(), 56.dp.toPx()),
                        style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // 中弧 (半径 18dp)
                    drawArc(
                        color = Color.White.copy(alpha = 0.68f),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(cornerCenter.x - 18.dp.toPx(), cornerCenter.y - 18.dp.toPx()),
                        size = Size(36.dp.toPx(), 36.dp.toPx()),
                        style = Stroke(width = 2.2f.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // 内弧 (半径 9dp)
                    drawArc(
                        color = Color.White.copy(alpha = 0.45f),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(cornerCenter.x - 9.dp.toPx(), cornerCenter.y - 9.dp.toPx()),
                        size = Size(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(width = 2.0f.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // 调整模式时的 VisionOS 空间悬浮晶态胶囊 (高度提示 · 完成)
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.88f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.88f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            val hudShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .shadow(8.dp, hudShape, ambientColor = Color(0x60000000), spotColor = Color.Transparent)
                    .clip(hudShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xD80F172A),
                                Color(0xE01E293B)
                            )
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.10f))
                        ),
                        shape = hudShape
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. 高度指示
                    Text(
                        text = "↕ ${cardHeight.toInt()}dp",
                        color = Color(0xEEF8FAFC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // 细分界线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(Color.White.copy(alpha = 0.20f))
                    )

                    // 2. 完成退出按钮（设置页常显模式不需要“完成”）
                    if (!forceShowControls) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                                )
                            )
                            .clickable {
                                haptic.perform(HapticType.CLICK)
                                isResizeMode = false
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✓ 完成",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    }
                }
            }
        }
    }

    // 简约纵向流体滑块：长按主页卡片后弹出在右侧卡片外
    AnimatedVisibility(
        visible = controlsVisible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInHorizontally { it },
        exit = fadeOut(tween(180)) +
               shrinkHorizontally(tween(180)) +
               slideOutHorizontally { it }
    ) {
        VerticalLiquidGlassSlider(
            alpha = cardAlpha,
            onAlphaChange = onCardAlphaChange,
            heightDp = cardHeight.dp,
            isDark = isDark
        )
    }
}
}

// -----------------------------------------------------------------------------
// 1. CLASSIC_RADAR: 经典轨道同心圆与卫星雷达（回归图片原版设计）
// -----------------------------------------------------------------------------
private fun DrawScope.drawClassicRadar(
    centerOffset: Offset,
    time: Float,
    maxR: Float
) {
    // 1. 实心内圈圆盘 (Solid central circle)
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = 36.dp.toPx(),
        center = centerOffset
    )

    // 2. 内层经典同心虚线轨道 (Inner dashed ring)
    drawCircle(
        color = Color.White.copy(alpha = 0.15f),
        radius = 70.dp.toPx(),
        center = centerOffset,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    )

    // 3. 外层同心细线连续圆环 (Outer continuous thin ring)
    drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = 115.dp.toPx(),
        center = centerOffset,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 4. 双重无缝扩散脉冲环 (正弦窗平滑加权，永不突变断裂重开)
    for (i in 0 until 2) {
        val p = ((time * 0.28f + i * 0.5f) % 1.0f).let { if (it < 0f) it + 1f else it }
        val alpha = sin(PI * p).toFloat() * 0.25f
        val pulseR = 36.dp.toPx() + (maxR - 36.dp.toPx()) * p
        drawCircle(
            color = Color(0xFF60A5FA).copy(alpha = alpha),
            radius = pulseR,
            center = centerOffset,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // 5. 环绕运行的卫星小圆点 (恒定角速度连续公转)
    val angle = time * 0.85f
    val orbitRadius = 70.dp.toPx()
    val dotX = centerOffset.x + orbitRadius * cos(angle)
    val dotY = centerOffset.y + orbitRadius * sin(angle)

    // 卫星微光晕
    drawCircle(
        color = Color(0xFF60A5FA).copy(alpha = 0.35f),
        radius = 8.dp.toPx(),
        center = Offset(dotX, dotY)
    )
    // 卫星本体
    drawCircle(
        color = Color(0xFF93C5FD),
        radius = 4.dp.toPx(),
        center = Offset(dotX, dotY)
    )
}

// -----------------------------------------------------------------------------
// 2. FLUID_CORE: 液态流光晶核 (上个版本用户好评版本)
// -----------------------------------------------------------------------------
private fun DrawScope.drawFluidCore(
    center: Offset,
    time: Float,
    isDark: Boolean
) {
    // 1. 三重无缝错相水波扩散环（正弦窗包络 sin(pi * p)，两端平滑归零，永不突变断裂重开）
    val rippleCount = 3
    for (i in 0 until rippleCount) {
        val p = ((time * 0.32f + i.toFloat() / rippleCount) % 1.0f).let { if (it < 0f) it + 1f else it }
        val alpha = sin(PI * p).toFloat() * 0.30f
        val r = 26.dp.toPx() + 74.dp.toPx() * p
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = alpha),
            radius = r,
            center = center,
            style = Stroke(width = (1.8f - 0.6f * p).dp.toPx())
        )
    }

    // 2. 偏心慢速李萨如流体轨道环 1（优雅虚线光环，随时间平滑浮动）
    val ring1Center = Offset(
        center.x + (6.dp.toPx() * cos(time * 0.75f)),
        center.y + (4.dp.toPx() * sin(time * 0.95f))
    )
    drawCircle(
        color = Color(0xFF93C5FD).copy(alpha = 0.22f),
        radius = 58.dp.toPx() + (3.dp.toPx() * sin(time * 1.3f)),
        center = ring1Center,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), time * 14f)
        )
    )

    // 3. 广域晶莹流体光环 2（细实线流体环）
    val ring2Center = Offset(
        center.x - (7.dp.toPx() * sin(time * 0.65f)),
        center.y + (5.dp.toPx() * cos(time * 0.85f))
    )
    drawCircle(
        color = Color(0xFFC084FC).copy(alpha = 0.16f),
        radius = 88.dp.toPx() + (2.5.dp.toPx() * cos(time * 1.15f)),
        center = ring2Center,
        style = Stroke(width = 1.4.dp.toPx())
    )

    // 4. 液态有机等离子晶核 (Metaball Liquid Core)
    // 漫射环境辉光
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFF38BDF8).copy(alpha = 0.85f),
            0.35f to Color(0xFF6366F1).copy(alpha = 0.55f),
            0.70f to Color(0xFFA855F7).copy(alpha = 0.22f),
            1.0f to Color.Transparent,
            center = center,
            radius = 64.dp.toPx()
        ),
        radius = 64.dp.toPx(),
        center = center
    )

    // 副液滴 1 (青蓝)
    val drop1Offset = Offset(
        15.dp.toPx() * cos(time * 1.15f),
        12.dp.toPx() * sin(time * 1.35f)
    )
    val drop1Center = Offset(center.x + drop1Offset.x, center.y + drop1Offset.y)
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFF67E8F9).copy(alpha = 0.85f),
            0.55f to Color(0xFF38BDF8).copy(alpha = 0.40f),
            1.0f to Color.Transparent,
            center = drop1Center,
            radius = 25.dp.toPx()
        ),
        radius = 25.dp.toPx(),
        center = drop1Center
    )

    // 副液滴 2 (紫粉)
    val drop2Offset = Offset(
        -13.dp.toPx() * sin(time * 0.95f),
        14.dp.toPx() * cos(time * 1.10f)
    )
    val drop2Center = Offset(center.x + drop2Offset.x, center.y + drop2Offset.y)
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFFE879F9).copy(alpha = 0.75f),
            0.60f to Color(0xFFC084FC).copy(alpha = 0.32f),
            1.0f to Color.Transparent,
            center = drop2Center,
            radius = 22.dp.toPx()
        ),
        radius = 22.dp.toPx(),
        center = drop2Center
    )

    // 主能量晶核 (白炽高光)
    val mainCoreR = 27.dp.toPx() + 2.5.dp.toPx() * sin(time * 2.0f)
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.95f),
            0.30f to Color(0xFFE0F2FE).copy(alpha = 0.90f),
            0.65f to Color(0xFF38BDF8).copy(alpha = 0.75f),
            1.0f to Color(0xFF1D4ED8).copy(alpha = 0.20f),
            center = center,
            radius = mainCoreR
        ),
        radius = mainCoreR,
        center = center
    )

    // 凸透镜液态反光弧 (Top-Left Specular Reflections)
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = mainCoreR * 0.36f,
        center = Offset(center.x - mainCoreR * 0.32f, center.y - mainCoreR * 0.32f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.30f),
        radius = mainCoreR * 0.16f,
        center = Offset(center.x - mainCoreR * 0.48f, center.y - mainCoreR * 0.12f)
    )

    // 5. 旋转液态反光弧 (360°平滑无界演算)
    val arcAngle = (time * 40f) % 360f
    drawArc(
        brush = Brush.sweepGradient(
            listOf(
                Color.Transparent,
                Color(0xFFBAE6FD).copy(alpha = 0.80f),
                Color.White.copy(alpha = 0.95f),
                Color(0xFFDDD6FE).copy(alpha = 0.65f),
                Color.Transparent
            ),
            center = center
        ),
        startAngle = arcAngle,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(center.x - 45.dp.toPx(), center.y - 45.dp.toPx()),
        size = Size(90.dp.toPx(), 90.dp.toPx()),
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
    )

    // 6. 环绕流体微星 (4颗不同相位卫星)
    for (idx in 0 until 4) {
        val pAngle = time * 0.6f + idx * (PI.toFloat() / 2f)
        val pDist = 48.dp.toPx() + 6.dp.toPx() * sin(time * 1.6f + idx * 1.5f)
        val px = center.x + pDist * cos(pAngle)
        val py = center.y + pDist * sin(pAngle)
        val pAlpha = (0.50f + 0.35f * sin(time * 2.5f + idx)).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFBAE6FD).copy(alpha = pAlpha),
            radius = 2.5.dp.toPx(),
            center = Offset(px, py)
        )
    }
}

// -----------------------------------------------------------------------------
// 3. AURORA_WAVES: 极光弥散薄纱流光 (Northern Lights Ethereal Veils)
// -----------------------------------------------------------------------------
private fun DrawScope.drawAuroraWaves(
    startX: Float,
    endX: Float,
    centerY: Float,
    time: Float,
    isDark: Boolean
) {
    val totalWidth = endX - startX
    if (totalWidth <= 0f) return

    val fadeWidth = 40.dp.toPx()
    val step = 4.dp.toPx()

    // -------------------------------------------------------------------------
    // Ribbon 1: 青蓝极光轻纱 (Cyan-Blue Silk Veil)
    // -------------------------------------------------------------------------
    fun wave1Y(x: Float): Float {
        val relX = x - startX
        val taper = (relX / fadeWidth).coerceIn(0f, 1f)
        val undulation = 13.dp.toPx() * sin(relX * 0.014f - time * 1.15f) +
                5.dp.toPx() * cos(relX * 0.022f + time * 0.75f)
        return centerY - 4.dp.toPx() + undulation * taper
    }

    val veilHeight1 = 26.dp.toPx()
    val veilPath1 = Path().apply {
        var x = startX
        var first = true
        while (x <= endX) {
            val y = wave1Y(x)
            if (first) {
                moveTo(x, y)
                first = false
            } else {
                lineTo(x, y)
            }
            x += step
        }
        x = endX
        while (x >= startX) {
            val taper = ((x - startX) / fadeWidth).coerceIn(0f, 1f)
            val y = wave1Y(x) + veilHeight1 * taper
            lineTo(x, y)
            x -= step
        }
        close()
    }

    drawPath(
        path = veilPath1,
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.20f to Color(0xFF06B6D4).copy(alpha = 0.35f),
            0.65f to Color(0xFF3B82F6).copy(alpha = 0.28f),
            1.0f to Color(0xFF6366F1).copy(alpha = 0.15f),
            startX = startX,
            endX = endX
        )
    )

    // 上缘高光线
    val crestPath1 = Path().apply {
        var first = true
        var x = startX
        while (x <= endX) {
            val y = wave1Y(x)
            if (first) {
                moveTo(x, y)
                first = false
            } else {
                lineTo(x, y)
            }
            x += step
        }
    }
    drawPath(
        path = crestPath1,
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.25f to Color(0xFF67E8F9).copy(alpha = 0.65f),
            0.75f to Color(0xFF93C5FD).copy(alpha = 0.50f),
            1.0f to Color.Transparent,
            startX = startX,
            endX = endX
        ),
        style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round)
    )

    // -------------------------------------------------------------------------
    // Ribbon 2: 幻紫洋红交融薄纱 (Violet-Magenta Counter-Veil)
    // -------------------------------------------------------------------------
    fun wave2Y(x: Float): Float {
        val relX = x - startX
        val taper = (relX / fadeWidth).coerceIn(0f, 1f)
        val undulation = 14.dp.toPx() * sin(relX * 0.011f - time * 0.90f + 1.4f) +
                4.dp.toPx() * cos(relX * 0.020f + time * 0.6f)
        return centerY + 3.dp.toPx() + undulation * taper
    }

    val veilHeight2 = 24.dp.toPx()
    val veilPath2 = Path().apply {
        var x = startX
        var first = true
        while (x <= endX) {
            val y = wave2Y(x)
            if (first) {
                moveTo(x, y)
                first = false
            } else {
                lineTo(x, y)
            }
            x += step
        }
        x = endX
        while (x >= startX) {
            val taper = ((x - startX) / fadeWidth).coerceIn(0f, 1f)
            val y = wave2Y(x) + veilHeight2 * taper
            lineTo(x, y)
            x -= step
        }
        close()
    }

    drawPath(
        path = veilPath2,
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.30f to Color(0xFF8B5CF6).copy(alpha = 0.30f),
            0.70f to Color(0xFFEC4899).copy(alpha = 0.22f),
            1.0f to Color.Transparent,
            startX = startX,
            endX = endX
        )
    )

    // 下缘高光流线
    val crestPath2 = Path().apply {
        var first = true
        var x = startX
        while (x <= endX) {
            val y = wave2Y(x) + veilHeight2 * ((x - startX) / fadeWidth).coerceIn(0f, 1f)
            if (first) {
                moveTo(x, y)
                first = false
            } else {
                lineTo(x, y)
            }
            x += step
        }
    }
    drawPath(
        path = crestPath2,
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.35f to Color(0xFFC084FC).copy(alpha = 0.55f),
            0.80f to Color(0xFFF472B6).copy(alpha = 0.40f),
            1.0f to Color.Transparent,
            startX = startX,
            endX = endX
        ),
        style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round)
    )
}

// -----------------------------------------------------------------------------
// 4. CORONA_ECLIPSE: 日冕偏心光晕 (Corona Halo)
// -----------------------------------------------------------------------------
private fun DrawScope.drawCoronaEclipse(
    center: Offset,
    time: Float,
    isDark: Boolean
) {
    val eclipseR = 26.dp.toPx()

    // 1. 日冕微风多层辐射辉光
    val breathe = 1.0f + 0.05f * sin(time * 0.85f)
    val flareDriftX = center.x + (3.dp.toPx() * cos(time * 0.6f))
    val flareDriftY = center.y + (2.dp.toPx() * sin(time * 0.75f))
    val flareCenter = Offset(flareDriftX, flareDriftY)

    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFF60A5FA).copy(alpha = 0.45f),
            0.35f to Color(0xFF818CF8).copy(alpha = 0.30f),
            0.70f to Color(0xFFC084FC).copy(alpha = 0.12f),
            1.0f to Color.Transparent,
            center = flareCenter,
            radius = eclipseR * 2.5f * breathe
        ),
        radius = eclipseR * 2.5f * breathe,
        center = flareCenter
    )

    // 2. 有机等离子色球层光晕 (Harmonic Chromosphere Flare Lobes)
    val numLobes = 48
    val coronaPath = Path().apply {
        for (i in 0 until numLobes) {
            val angle = (i.toFloat() / numLobes) * 2f * PI.toFloat()
            val wobble = 1.0f +
                    0.08f * sin(3f * angle + time * 1.4f) +
                    0.05f * cos(5f * angle - time * 0.9f)
            val r = eclipseR * 1.35f * wobble
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = coronaPath,
        brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.65f to Color(0xFF38BDF8).copy(alpha = 0.35f),
            0.88f to Color(0xFFF59E0B).copy(alpha = 0.20f),
            1.0f to Color.Transparent,
            center = center,
            radius = eclipseR * 1.55f
        )
    )

    // 3. 高能色球层金曜晕斑 (Chromosphere Amber-Gold Rim Glow)
    drawCircle(
        brush = Brush.radialGradient(
            0.75f to Color.Transparent,
            0.90f to Color(0xFFFDE047).copy(alpha = 0.35f),
            0.98f to Color(0xFF38BDF8).copy(alpha = 0.50f),
            1.0f to Color.Transparent,
            center = center,
            radius = eclipseR * 1.18f
        ),
        radius = eclipseR * 1.18f,
        center = center
    )

    // 4. 超柔和日冕微澜
    for (i in 0 until 2) {
        val p = ((time * 0.22f + i * 0.5f) % 1.0f).let { if (it < 0f) it + 1f else it }
        val alpha = sin(PI * p).toFloat() * 0.12f
        val r = eclipseR + 32.dp.toPx() * p
        drawCircle(
            color = Color(0xFF93C5FD).copy(alpha = alpha),
            radius = r,
            center = center,
            style = Stroke(width = (1.5f - 0.5f * p).dp.toPx())
        )
    }

    // 5. 深邃凹面黑曜天体圆盘
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0x650F172A),
            0.85f to Color(0x85020617),
            1.0f to Color(0xAA020617),
            center = center,
            radius = eclipseR
        ),
        radius = eclipseR,
        center = center
    )

    // 6. 引力透镜棱镜折射光谱弧
    val sweepStartAngle = (time * 36f) % 360f
    drawArc(
        brush = Brush.sweepGradient(
            listOf(
                Color.Transparent,
                Color(0xFF38BDF8).copy(alpha = 0.70f),
                Color(0xFF818CF8).copy(alpha = 0.60f),
                Color(0xFFF472B6).copy(alpha = 0.65f),
                Color(0xFFFBBF24).copy(alpha = 0.50f),
                Color.Transparent
            ),
            center = center
        ),
        startAngle = sweepStartAngle,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(center.x - eclipseR * 1.02f, center.y - eclipseR * 1.02f),
        size = Size(eclipseR * 2.04f, eclipseR * 2.04f),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )
}
