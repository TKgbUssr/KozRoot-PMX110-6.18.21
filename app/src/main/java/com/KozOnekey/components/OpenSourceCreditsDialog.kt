package com.KozOnekey.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.rememberHapticFeedback

private data class OpenSourceLib(
    val name: String,
    val author: String,
    val description: String,
    val license: String,
    val icon: ImageVector,
    val tint: Color
)

/**
 * 开源致谢对话框 (Open Source Acknowledgements Dialog)
 *
 * 采用 100% 拟物真实液态玻璃 (Liquid Glass) 与 Windows Aero 磨砂虚化设计语言：
 * 1. 硬件级真实光学模糊折射 (Backdrop Hardware Blur 32dp + Optical Lens)：透视底层页面细节并实时虚化
 * 2. 彻底杜绝文字冲突，底层文字与图标在 Aero 玻璃后方被柔和雾化成光斑
 * 3. 拟物晶体镜面高光 (Specular Meniscus) 与双层高透玻璃描边
 * 4. 丝滑弹簧缩放淡入淡出动画，支持点击蒙层与系统返回手势
 */
@Composable
fun OpenSourceCreditsDialog(
    isOpen: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop? = null
) {
    var predictiveScale by remember { mutableFloatStateOf(1f) }

    if (isOpen) {
        PredictiveBackHandler(enabled = true) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    predictiveScale = 1f - backEvent.progress.coerceIn(0f, 1f) * 0.06f
                }
                predictiveScale = 1f
                onDismiss()
            } catch (e: Exception) {
                predictiveScale = 1f
            }
        }
    } else {
        predictiveScale = 1f
    }

    val haptic = rememberHapticFeedback()
    val dialogShape = RoundedCornerShape(26.dp)

    val libraries = remember {
        listOf(
            OpenSourceLib(
                name = "AndroidLiquidGlass",
                author = "Kyant0",
                description = "硬件级光学透镜折射与动态虚化着色器",
                license = "Apache 2.0",
                icon = Icons.Rounded.WaterDrop,
                tint = Color(0xFF38BDF8)
            ),
            OpenSourceLib(
                name = "Jetpack Compose UI",
                author = "Google / AOSP",
                description = "现代声明式图形管线与原生物理弹簧动效",
                license = "Apache 2.0",
                icon = Icons.Rounded.Code,
                tint = Color(0xFF10B981)
            ),
            OpenSourceLib(
                name = "AGSL & RenderEffect",
                author = "AOSP",
                description = "硬件加速着色器与动态高斯模糊渲染管线",
                license = "Apache 2.0",
                icon = Icons.Rounded.Layers,
                tint = Color(0xFF8B5CF6)
            ),
            OpenSourceLib(
                name = "Kotlin Coroutines",
                author = "JetBrains",
                description = "陀螺仪物理采样协程调度并发引擎",
                license = "Apache 2.0",
                icon = Icons.Rounded.Favorite,
                tint = Color(0xFFEF4444)
            ),
            OpenSourceLib(
                name = "OneKeyJB（星苒鸭）",
                author = "星苒鸭",
                description = "CVE-2026-64560 临时 Root 实现思路参考（late-load 收尾等）",
                license = "GPL-3.0",
                icon = Icons.Rounded.AutoAwesome,
                tint = Color(0xFFF59E0B)
            ),
            OpenSourceLib(
                name = "KernelSU 系列内核模块 / 管理器",
                author = "KernelSU-Next / KernelSU / SukiSU-Ultra / FinaSU / BakaSU",
                description = "内置 LKM（离线预重定位 + 运行时 KASLR 平移）与管理器 APK",
                license = "GPL-2.0 / GPL-3.0（以各上游为准）",
                icon = Icons.Rounded.Layers,
                tint = Color(0xFF38BDF8)
            ),
            OpenSourceLib(
                name = "CVE-2026-64560 exploit",
                author = "cybermeowfia",
                description = "漏洞利用源码基础（UAF / cross-cache / pipe_buffer）",
                license = "Apache 2.0",
                icon = Icons.Rounded.Layers,
                tint = Color(0xFF10B981)
            ),
            OpenSourceLib(
                name = "Stellar",
                author = "roro2239",
                description = "shell 权限桥（无线调试 / Root 启动服务）",
                license = "GPL-3.0",
                icon = Icons.Rounded.WaterDrop,
                tint = Color(0xFF38BDF8)
            )
        )
    }

    AnimatedVisibility(
        visible = isOpen,
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
                ) { /* 拦截弹窗内部点击防误触关闭 */ }
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
                                listOf(
                                    Color(0x521E293B), // 高透烟熏蓝晶体 (32% 不透明度)
                                    Color(0x320F172A)  // 深层夜空半透明微透底 (20% 不透明度)
                                )
                            } else {
                                listOf(
                                    Color(0x72FFFFFF), // 高透白玉磨砂晶体
                                    Color(0x45F1F5F9)  // 通透冰晶微透层
                                )
                            }
                        )
                    )
                    .drawBehind {
                        // 1. 顶部晶体镜面高光 (Top Specular Meniscus)
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

                        // 2. Windows Aero 标志性上半区流光斜面反光 (Aero Gloss Sheen Overlay)
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
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with icon and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x3038BDF8) else Color(0x180284C7))
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) Color(0x4038BDF8) else Color(0x250284C7),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "开源致谢",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Open Source Acknowledgements",
                                    fontSize = 10.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        // Close icon (圆形高斯磨砂关闭按钮)
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

                    Text(
                        text = "KozRoot · 作者 Kozmiztar / kgb玉米。特此向以下项目致谢：\n\n免责声明：本工具仅供个人学习与技术研究使用，请勿用于商业用途或倒卖；使用可能带来设备不稳定、数据丢失等风险，请自行评估并承担全部后果。",
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Library cards (微透晶体列表项 - 详情完整展示无截断)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        libraries.forEach { lib ->
                            val itemShape = RoundedCornerShape(14.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(itemShape)
                                    .background(
                                        if (isDark) Color(0x20FFFFFF) else Color(0x50FFFFFF)
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                if (isDark) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.85f),
                                                if (isDark) Color(0x1238BDF8) else Color(0x253B82F6)
                                            )
                                        ),
                                        shape = itemShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(lib.tint.copy(alpha = if (isDark) 0.22f else 0.15f))
                                            .border(
                                                width = 1.dp,
                                                color = lib.tint.copy(alpha = 0.30f),
                                                shape = RoundedCornerShape(9.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = lib.icon,
                                            contentDescription = null,
                                            tint = lib.tint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // 第一行：项目名称（左） + 授权协议徽章（右）
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lib.name,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                            )

                                            // 许可徽章
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(lib.tint.copy(alpha = if (isDark) 0.20f else 0.14f))
                                                    .border(
                                                        width = 0.8.dp,
                                                        color = lib.tint.copy(alpha = 0.40f),
                                                        shape = RoundedCornerShape(5.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = lib.license,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = lib.tint,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(1.dp))

                                        // 第二行：作者归属
                                        Text(
                                            text = "by ${lib.author}",
                                            fontSize = 10.5.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        // 第三行：项目技术详情完整描述（无截断、自然换行）
                                        Text(
                                            text = lib.description,
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
