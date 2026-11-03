package com.KozOnekey.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.rememberHapticFeedback

/**
 * 与「开源致谢」完全同款的液态玻璃弹窗（同层 overlay + 真实背景 backdrop 虚化折射）：
 * 圆角 26dp、Aero 磨砂、顶部镜面高光、渐变描边、圆形关闭按钮、弹簧缩放进场。
 */
@Composable
fun GlassDialog(
    visible: Boolean,
    isDark: Boolean,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    backdrop: Backdrop? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = rememberHapticFeedback()
    val dialogShape = RoundedCornerShape(26.dp)
    var predictiveScale by remember { mutableFloatStateOf(1f) }

    if (visible) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 与「开源致谢」一致：预测性返回手势（跟手缩小 + 松手关闭）
            PredictiveBackHandler(enabled = true) { progressFlow ->
                try {
                    progressFlow.collect { backEvent ->
                        predictiveScale = 1f - backEvent.progress.coerceIn(0f, 1f) * 0.06f
                    }
                    predictiveScale = 1f
                    onDismiss()
                } catch (_: Exception) {
                    predictiveScale = 1f
                    onDismiss()
                }
            }
        } else {
            BackHandler(enabled = true) { onDismiss() }
        }
    } else {
        predictiveScale = 1f
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            var dialogModifier = Modifier
                .animateEnterExit(
                    enter = scaleIn(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        initialScale = 0.90f
                    ),
                    exit = scaleOut(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        targetScale = 0.90f
                    )
                )
                .padding(horizontal = 20.dp, vertical = 32.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = predictiveScale
                    scaleY = predictiveScale
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // 拦截弹窗内部点击防误触关闭
                }
                .shadow(
                    elevation = 32.dp,
                    shape = dialogShape,
                    ambientColor = if (isDark) Color(0x90000000) else Color(0x35000000),
                    spotColor = Color.Transparent
                )

            // 硬件级 Backdrop Aero 真实光学物理模糊与透镜折射
            if (backdrop != null) {
                dialogModifier = dialogModifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { dialogShape },
                    effects = {
                        blur(32.dp.toPx())
                        lens(14.dp.toPx(), 14.dp.toPx())
                    }
                )
            }

            Box(
                modifier = dialogModifier
                    .clip(dialogShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(Color(0x521E293B), Color(0x320F172A))
                            } else {
                                listOf(Color(0x72FFFFFF), Color(0x45F1F5F9))
                            }
                        )
                    )
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.65f else 0.95f),
                                    Color.White.copy(alpha = if (isDark) 0.15f else 0.35f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 4.dp.toPx()
                            ),
                            size = Size(size.width, 4.dp.toPx())
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.28f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.38f
                            ),
                            size = Size(size.width, size.height * 0.38f)
                        )
                    }
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isDark) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.95f),
                                if (isDark) Color(0x3038BDF8) else Color(0x503B82F6),
                                if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.35f)
                            )
                        ),
                        shape = dialogShape
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(iconTint.copy(alpha = 0.16f))
                                .border(
                                    width = 1.dp,
                                    color = iconTint.copy(alpha = 0.22f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = subtitle,
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x25FFFFFF) else Color(0x15000000))
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color(0x25FFFFFF) else Color(0x12000000),
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.perform(HapticType.CLICK)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "关闭",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

/** 弹窗里的玻璃小按钮 */
@Composable
fun GlassDialogActionButton(
    text: String,
    isDark: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val accent = if (danger) Color(0xFFEF4444) else if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

/** 弹窗里的按钮行 */
@Composable
fun GlassDialogActionRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}
