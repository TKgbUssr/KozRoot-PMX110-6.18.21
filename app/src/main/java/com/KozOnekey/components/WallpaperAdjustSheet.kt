package com.KozOnekey.components

import android.net.Uri
import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.overscrollHapticFeedback
import com.KozOnekey.effects.rememberHapticFeedback
import com.KozOnekey.models.WallpaperConfig
import kotlin.math.roundToInt

data class PresetWallpaper(
    val id: String,
    val name: String,
    val gradient: List<Color>
)

val PRESET_WALLPAPERS = listOf(
    PresetWallpaper(
        "nebula",
        "赛博星云",
        listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFF701A75), Color(0xFF0284C7))
    ),
    PresetWallpaper(
        "polar",
        "极地液态",
        listOf(Color(0xFF082F49), Color(0xFF0369A1), Color(0xFF06B6D4), Color(0xFF38BDF8), Color(0xFFBAE6FD))
    ),
    PresetWallpaper(
        "ember",
        "暮光流火",
        listOf(Color(0xFF1C1917), Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFBBF24))
    ),
    PresetWallpaper(
        "emerald",
        "翡翠琉璃",
        listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981), Color(0xFF34D399), Color(0xFFA7F3D0))
    )
)

/**
 * 壁纸视觉工坊弹窗 (Wallpaper Visual Studio Dialog)
 *
 * 采用与开源致谢("圈i") 100% 统一的拟物真实液态玻璃与 Windows Aero 磨砂虚化设计语言：
 * 1. 硬件级真实光学模糊折射 (Backdrop Hardware Blur 32dp + Optical Lens)
 * 2. 拟物晶体镜面高光 (Specular Meniscus) 与双层高透晶体描边 (1.2dp 渐变)
 * 3. Windows Aero 上半区 38% 高光斜面反光流光层 (Aero Gloss Sheen)
 * 4. 纯圆形高斯磨砂关闭按钮与微透晶体预设徽章
 * 5. 丝滑物理弹簧缩放淡入淡出动画，支持点击蒙层与系统返回手势
 */
@Composable
fun WallpaperAdjustDialog(
    isOpen: Boolean,
    currentConfig: WallpaperConfig,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onApply: (WallpaperConfig) -> Unit,
    onReset: () -> Unit,
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember(currentConfig, isOpen) { mutableStateOf<Uri?>(currentConfig.uri) }
    var selectedPreset by remember(currentConfig, isOpen) { mutableStateOf<String?>(currentConfig.presetId) }
    var blurRadius by remember(currentConfig, isOpen) { mutableFloatStateOf(currentConfig.blurRadius) }
    var dimAlpha by remember(currentConfig, isOpen) { mutableFloatStateOf(currentConfig.dimAlpha) }
    var scaleMode by remember(currentConfig, isOpen) { mutableStateOf(currentConfig.scaleMode) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                // 把相册图片复制到 App 私有目录，避免 content:// 授权在重启后失效
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(context.filesDir, "wallpaper").apply { mkdirs() }
                        val f = File(dir, "custom_wallpaper.img")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            f.outputStream().use { output -> input.copyTo(output) }
                        }
                        Uri.fromFile(f)
                    }.getOrNull()
                }
                selectedUri = saved ?: uri
                selectedPreset = null
            }
        }
    }

    val dialogShape = RoundedCornerShape(26.dp)
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp.dp - 64.dp).coerceAtLeast(320.dp)

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
                .padding(horizontal = 20.dp, vertical = 24.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                        .verticalScroll(scrollState)
                        .overscrollHapticFeedback(haptic)
                ) {
                    // Header with icon and close button (统一圈i风格)
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
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "壁纸视觉工坊",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Wallpaper Visual Studio",
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
                        text = "定制系统级纯拟物光学折射、磨砂景深模糊与动态壁纸遮罩：",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wallpaper Source Selection Buttons (微透晶体按键)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val pickShape = RoundedCornerShape(14.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(pickShape)
                                .background(if (isDark) Color(0x20FFFFFF) else Color(0x50FFFFFF))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            if (isDark) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.85f),
                                            if (isDark) Color(0x1238BDF8) else Color(0x253B82F6)
                                        )
                                    ),
                                    shape = pickShape
                                )
                                .clickable {
                                    haptic.perform(HapticType.CLICK)
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = "Gallery",
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedUri != null) "已选本地照片" else "选取本地相册",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                )
                            }
                        }

                        val resetShape = RoundedCornerShape(14.dp)
                        Box(
                            modifier = Modifier
                                .weight(0.75f)
                                .clip(resetShape)
                                .background(if (isDark) Color(0x15FFFFFF) else Color(0x30FFFFFF))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.60f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = resetShape
                                )
                                .clickable {
                                    haptic.perform(HapticType.CLICK)
                                    selectedUri = null
                                    selectedPreset = "nebula"
                                    blurRadius = 80f
                                    dimAlpha = 0.20f
                                    scaleMode = "crop"
                                    onReset()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset",
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "重置默认",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset Theme Swatches
                    Text(
                        text = "高级流体预设",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_WALLPAPERS.forEach { preset ->
                            val isSelected = selectedPreset == preset.id && selectedUri == null
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Brush.linearGradient(preset.gradient))
                                    .clickable {
                                        haptic.perform(HapticType.CLICK)
                                        selectedPreset = preset.id
                                        selectedUri = null
                                    }
                                    .border(
                                        width = if (isSelected) 2.2.dp else 0.8.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0x35FFFFFF),
                                        shape = RoundedCornerShape(13.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scale / Fit Mode Switcher
                    Text(
                        text = "裁剪与对齐方式",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("crop" to "居中裁切", "fit" to "等比适应", "zoom" to "特写放大").forEach { (mode, label) ->
                            val isSelected = scaleMode == mode
                            val itemShape = RoundedCornerShape(11.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(itemShape)
                                    .background(
                                        if (isSelected) {
                                            if (isDark) Color(0xFF0284C7) else Color(0xFF2563EB)
                                        } else {
                                            if (isDark) Color(0x18FFFFFF) else Color(0x35FFFFFF)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) Color(0x6038BDF8) else Color(0x20FFFFFF),
                                        shape = itemShape
                                    )
                                    .clickable {
                                        haptic.perform(HapticType.CLICK)
                                        scaleMode = mode
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Blur Intensity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "磨砂景深模糊度 (Blur)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                        Text(
                            text = "${blurRadius.roundToInt()} px",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }

                    Slider(
                        value = blurRadius,
                        onValueChange = { blurRadius = it },
                        valueRange = 0f..160f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            activeTrackColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            inactiveTrackColor = if (isDark) Color(0x30FFFFFF) else Color(0x20000000)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dim / Darkness Overlay Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "暗色遮罩亮度 (Overlay Dim)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                        Text(
                            text = "${(dimAlpha * 100).roundToInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }

                    Slider(
                        value = dimAlpha,
                        onValueChange = { dimAlpha = it },
                        valueRange = 0f..0.85f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            activeTrackColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            inactiveTrackColor = if (isDark) Color(0x30FFFFFF) else Color(0x20000000)
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Apply Button (Aero 拟物微透晶体大按钮)
                    val applyShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = applyShape,
                                ambientColor = if (isDark) Color(0x400284C7) else Color(0x302563EB),
                                spotColor = Color.Transparent
                            )
                            .clip(applyShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF0284C7),
                                        Color(0xFF2563EB)
                                    )
                                )
                            )
                            .drawBehind {
                                // 顶部晶体高光
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = 2.dp.toPx()
                                    ),
                                    size = Size(size.width, 2.dp.toPx())
                                )
                            }
                            .clickable {
                                haptic.perform(HapticType.CLICK)
                                onApply(
                                    WallpaperConfig(
                                        uri = selectedUri,
                                        presetId = selectedPreset,
                                        blurRadius = blurRadius,
                                        dimAlpha = dimAlpha,
                                        scaleMode = scaleMode
                                    )
                                )
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "保存并应用壁纸",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
