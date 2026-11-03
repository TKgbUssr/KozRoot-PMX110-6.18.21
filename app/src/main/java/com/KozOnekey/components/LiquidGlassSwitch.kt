package com.KozOnekey.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.rememberHapticFeedback

/**
 * 液态玻璃模糊半透拨钮开关 (Liquid Glass Translucent Switch)
 *
 * 专为液态玻璃主题定制的高级拨钮：
 * 1. 【有触感的实时连续物理拖拽 (Haptic Continuous Draggable)】：
 *    支持手指按住滑块 1:1 连续拖拽移动，越过 50% 临界分界线时触发清脆机械滴答力反馈 (Haptic TICK)，
 *    松手时智能吸附并触发确认触觉反馈 (Haptic CLICK)。
 * 2. 【模糊半透微晶轨道与无级辉光过渡 (Progressive Glowing Track)】：
 *    拖拽过程中背景颜色与菲涅尔晶体描边根据拖动比例实时插值无缝渐变，既通透又富有灵动光感。
 * 3. 【悬浮水滴立体玻璃珠 (Liquid Lens Thumb)】：
 *    滑块呈高光圆润的微凸透镜珠，带有上沿高光弦线与折射层次；
 *    拖拽拉扯过程中伴随自然的果冻液态表面张力形变 (Squash & Stretch)。
 */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    enabled: Boolean = true
) {
    val haptic = rememberHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    // 开关几何规格：标准胶囊比例 50dp x 28dp
    val switchWidth = 50.dp
    val switchHeight = 28.dp
    val cornerRadius = 14.dp
    val thumbDiameter = 22.dp
    val trackShape = RoundedCornerShape(cornerRadius)

    // 滑块位移距离：左端 3dp，右端 25dp，总有效行程 22dp
    val minOffset = 3.dp
    val maxOffset = 25.dp
    val travelRange = 22.dp

    val minOffsetPx = with(density) { minOffset.toPx() }
    val maxOffsetPx = with(density) { maxOffset.toPx() }
    val travelRangePx = maxOffsetPx - minOffsetPx

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(if (checked) maxOffsetPx else minOffsetPx) }
    var hasCrossedThreshold by remember { mutableStateOf(checked) }

    // 外部 checked 变更或拖拽结束后，位置实时与目标状态对齐
    LaunchedEffect(checked, isDragging) {
        if (!isDragging) {
            dragOffsetPx = if (checked) maxOffsetPx else minOffsetPx
            hasCrossedThreshold = checked
        }
    }

    // 弹簧平滑位移：拖拽中以极高刚度跟随手指，释放/点按时呈现弹性果冻阻尼回弹
    val animatedOffset by animateDpAsState(
        targetValue = if (isDragging) with(density) { dragOffsetPx.toDp() } else if (checked) maxOffset else minOffset,
        animationSpec = if (isDragging) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        },
        label = "switchThumbOffset"
    )

    // 当前归一化进度（0.0f 为未选中，1.0f 为选中）
    val progress = if (isDragging) {
        ((dragOffsetPx - minOffsetPx) / travelRangePx).coerceIn(0f, 1f)
    } else {
        if (checked) 1f else 0f
    }

    // 拖动与按压拉扯时的果冻液态表面张力微拉伸
    val scaleX by animateFloatAsState(
        targetValue = if (isDragging) 1.15f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "switchThumbScaleX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isDragging) 0.88f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "switchThumbScaleY"
    )

    // 轨道根据当前进度实时动态线性插值微光颜色
    val dynamicTrackBg = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0x22FFFFFF), Color(0x9038BDF8), progress),
                lerp(Color(0x10FFFFFF), Color(0xB01D4ED8), progress)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0x40FFFFFF), Color(0xA060A5FA), progress),
                lerp(Color(0x18CBD5E1), Color(0xC02563EB), progress)
            )
        )
    }

    // 轨道高折射晶体描边随进度通透发光
    val dynamicBorder = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0x45FFFFFF), Color(0xE0BAE6FD), progress),
                lerp(Color(0x15FFFFFF), Color(0x9038BDF8), progress)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0x80FFFFFF), Color(0xE0BAE6FD), progress),
                lerp(Color(0x25000000), Color(0x9038BDF8), progress)
            )
        )
    }

    Box(
        modifier = modifier
            .width(switchWidth)
            .height(switchHeight)
            .clip(trackShape)
            .shadow(
                elevation = (1f + progress * 3f).dp,
                shape = trackShape,
                ambientColor = if (progress > 0.5f) Color(0x303B82F6) else Color(0x10000000),
                spotColor = if (progress > 0.5f) Color(0x502563EB) else Color(0x20000000)
            )
            .background(dynamicTrackBg)
            .border(
                width = 1.dp,
                brush = dynamicBorder,
                shape = trackShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                // 点击即时切换
                haptic.perform(HapticType.CLICK)
                onCheckedChange(!checked)
            }
            .pointerInput(enabled, checked) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragOffsetPx = if (checked) maxOffsetPx else minOffsetPx
                        hasCrossedThreshold = checked
                    },
                    onDragEnd = {
                        val finalChecked = (dragOffsetPx - minOffsetPx) >= (travelRangePx / 2f)
                        if (finalChecked != checked) {
                            haptic.perform(HapticType.CLICK)
                            onCheckedChange(finalChecked)
                        } else {
                            haptic.perform(HapticType.TICK)
                        }
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (dragOffsetPx + dragAmount).coerceIn(minOffsetPx, maxOffsetPx)
                        dragOffsetPx = newOffset
                        val currentP = ((newOffset - minOffsetPx) / travelRangePx).coerceIn(0f, 1f)
                        val isOverHalf = currentP >= 0.5f
                        if (isOverHalf != hasCrossedThreshold) {
                            hasCrossedThreshold = isOverHalf
                            haptic.perform(HapticType.TICK) // 跨过 50% 分界线时呈现清脆机械齿轮触感反馈
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 滑动水滴玻璃珠
        val thumbShape = CircleShape
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .size(thumbDiameter)
                .scale(scaleX = scaleX, scaleY = scaleY)
                .shadow(
                    elevation = 4.dp,
                    shape = thumbShape,
                    ambientColor = Color(0x25000000),
                    spotColor = Color(0x40000000)
                )
                .clip(thumbShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            if (isDark) Color(0xFFE2E8F0) else Color(0xFFF8FAFC)
                        )
                    )
                )
                .drawBehind {
                    // 水滴滑块顶部高光弧
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = size.width * 0.28f,
                        center = Offset(size.width * 0.40f, size.height * 0.35f)
                    )
                }
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0x50CBD5E1)
                        )
                    ),
                    shape = thumbShape
                )
        )
    }
}
