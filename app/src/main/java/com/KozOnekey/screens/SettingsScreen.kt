package com.KozOnekey.screens

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import com.KozOnekey.components.LiquidGlassSwitch
import com.KozOnekey.components.RadarHeroCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.liquidGlass
import com.KozOnekey.effects.overscrollHapticFeedback
import com.KozOnekey.effects.rememberHapticFeedback
import com.KozOnekey.models.HeroAnimationType
import com.KozOnekey.models.WallpaperConfig
import com.KozOnekey.theme.BackgroundMode

@Composable
fun SettingsScreen(
    currentMode: BackgroundMode,
    onModeChange: (BackgroundMode) -> Unit,
    wallpaperConfig: WallpaperConfig,
    onWallpaperConfigChange: (WallpaperConfig) -> Unit,
    heroAnimationType: HeroAnimationType = HeroAnimationType.FLUID_CORE,
    onHeroAnimationTypeChange: (HeroAnimationType) -> Unit = {},
    heroCardAlpha: Float = 0.75f,
    onHeroCardAlphaChange: (Float) -> Unit = {},
    heroCardHeight: Float = 150f,
    onHeroCardHeightChange: (Float) -> Unit = {},
    isDark: Boolean,
    themeMode: Int = 2,
    onThemeModeChange: (Int) -> Unit = {},
    isPredictiveBackEnabled: Boolean = true,
    onPredictiveBackToggle: (Boolean) -> Unit = {},
    onOpenCreditsDialog: () -> Unit = {},
    isCurrentPage: Boolean = true,
    isWallpaperStudioOpen: Boolean = false,
    onOpenWallpaperStudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    val haptic = rememberHapticFeedback()

    // 二级菜单挂载与动画状态
    var isAppearanceSubMenuOpen by remember { mutableStateOf(false) }
    var isSubMenuMounted by remember { mutableStateOf(false) }

    // 平滑水平位移转场驱动器 (0f: 二级菜单完全展开在原位, screenWidthPx: 二级菜单滑出屏幕右侧)
    val subMenuSlideOffset = remember { Animatable(0f) }

    // 切换到其他页面（如滑动或底栏切换）时，自动退出二级菜单，恢复整洁状态
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && isAppearanceSubMenuOpen) {
            isAppearanceSubMenuOpen = false
            isSubMenuMounted = false
            subMenuSlideOffset.snapTo(0f)
        }
    }

    // 手势与退出动量状态追踪
    var isExitingWithMomentum by remember { mutableStateOf(false) }

    val openSubMenu: () -> Unit = {
        isSubMenuMounted = true
        isAppearanceSubMenuOpen = true
        coroutineScope.launch {
            subMenuSlideOffset.snapTo(screenWidthPx)
            subMenuSlideOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
    }

    val closeSubMenuNormal: () -> Unit = {
        if (!isExitingWithMomentum && isSubMenuMounted) {
            isExitingWithMomentum = true
            coroutineScope.launch {
                subMenuSlideOffset.animateTo(
                    targetValue = screenWidthPx,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
                isAppearanceSubMenuOpen = false
                isSubMenuMounted = false
                isExitingWithMomentum = false
                subMenuSlideOffset.snapTo(0f)
            }
        }
    }

    val isSubMenuBackActive = isAppearanceSubMenuOpen && !isExitingWithMomentum && !isWallpaperStudioOpen && isCurrentPage

    if (isPredictiveBackEnabled && isSubMenuBackActive) {
        PredictiveBackHandler(enabled = isSubMenuBackActive) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    // 原生预测返回手势：1:1 跟随手指平滑水平向右移动，绝不缩放，保持纯正全屏质感
                    val progress = backEvent.progress.coerceIn(0f, 1f)
                    val targetX = progress * (screenWidthPx * 0.70f)
                    subMenuSlideOffset.snapTo(targetX)
                }
                // 手势放手确认返回：无缝衔接当前位移，顺滑飞出右侧屏幕边界，彻底杜绝顿挫
                isExitingWithMomentum = true
                coroutineScope.launch {
                    subMenuSlideOffset.animateTo(
                        targetValue = screenWidthPx,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                    isAppearanceSubMenuOpen = false
                    isSubMenuMounted = false
                    isExitingWithMomentum = false
                    subMenuSlideOffset.snapTo(0f)
                }
            } catch (e: CancellationException) {
                // 手势拉回取消返回：自然平滑弹簧复位到 0f 原位
                coroutineScope.launch {
                    subMenuSlideOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    )
                }
            }
        }
    } else if (isSubMenuBackActive) {
        // 关闭预测手势时的经典即时返回支持
        BackHandler(enabled = isSubMenuBackActive) {
            closeSubMenuNormal()
        }
    }

    val mainScrollState = rememberScrollState()
    val appearanceScrollState = rememberScrollState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let {
        if (it > 20.dp) it else 48.dp
    }

    // 动态计算水平滑动视差位移与渐变参数（保持 1.0f 满屏比例，绝不缩放）
    val currentSlideX = subMenuSlideOffset.value.coerceIn(0f, screenWidthPx)
    val slideFraction = if (screenWidthPx > 0f) (currentSlideX / screenWidthPx).coerceIn(0f, 1f) else 0f

    val subMenuOffsetX = currentSlideX
    val subMenuAlpha = (1f - slideFraction * 0.4f).coerceIn(0f, 1f)

    // 仅在二级菜单发生滑动视差过渡时进行轻微视差偏移，二级菜单完全展开后归零且底层不渲染，杜绝跨页面穿透
    val mainOffsetX = if (isSubMenuMounted && slideFraction > 0.001f) {
        -((screenWidthPx * 0.28f) * (1f - slideFraction))
    } else {
        0f
    }
    val mainAlpha = if (isSubMenuMounted) slideFraction.coerceIn(0f, 1f) else 1f

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        // =========================================================================
        // 【Layer 0: 一级主菜单（设置）】
        // 始终保留在底层，当二级菜单平滑滑开时，呈现自然视差位移与显隐过渡
        // =========================================================================
        if (!isSubMenuMounted || slideFraction > 0.001f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = if (isSubMenuMounted) mainOffsetX else 0f
                        alpha = mainAlpha
                    }
                .overscrollHapticFeedback(haptic)
                .verticalScroll(mainScrollState)
                .padding(horizontal = 20.dp)
        ) {
                    Spacer(modifier = Modifier.height(statusBarPadding + 26.dp))
                    Text(
                        text = "设置",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )

                    Text(
                        text = "KozRoot 系统首选项与全局配置",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                    )

                    // 1. “界面与个性化” 二级菜单入口卡片
                    Text(
                        text = "视觉与个性化",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val entryCardShape = RoundedCornerShape(22.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = entryCardShape,
                                tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                                borderWidth = 0.dp,
                                elevation = 4.dp,
                                isDark = isDark
                            )
                            .clickable {
                                haptic.perform(HapticType.CLICK)
                                openSubMenu()
                            }
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2563EB), Color(0xFF38BDF8))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = "个性化",
                                        tint = Color.White,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "界面与个性化",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "自定义壁纸、材质背景、外观与卡片动效",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "进入",
                                tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // 2. 关于 KozRoot 与设备（保持留在外层设置界面不变）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "关于 KozRoot 与设备",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                        )

                        // 独立 "圈 i" 开源致谢按键
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0x351E293B) else Color(0x40FFFFFF)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            if (isDark) Color(0x5094A3B8) else Color(0x90FFFFFF),
                                            if (isDark) Color(0x2038BDF8) else Color(0x303B82F6)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.perform(HapticType.CLICK)
                                    onOpenCreditsDialog()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "开源致谢",
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(24.dp),
                                tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                                borderWidth = 1.2.dp,
                                elevation = 4.dp,
                                isDark = isDark
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DiagRow("外观架构", "Lumina Caustics · by Kozmiztar", isDark)
                            HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000))
                            DiagRow("图形管线", "Jetpack Compose + AGSL Shaders", isDark)
                            HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000))
                            DiagRow("模糊折射", "Kyant0/Backdrop + RenderEffect", isDark)
                            HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000))
                            DiagRow("Android 系统", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", isDark)
                            HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000))
                            DiagRow("设备型号", Build.MODEL, isDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(140.dp))
            }
        }

        // =========================================================================
        // 【Layer 1: 二级菜单（界面与个性化）】
        // 全屏平滑水平位移（绝不缩放，保持原有纯正清透液态玻璃质感与状态栏一体化）
        // =========================================================================
        if (isSubMenuMounted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = subMenuOffsetX
                        alpha = subMenuAlpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 拦截卡片空白处的点击，杜绝穿透到底层主设置
                    }
                    .overscrollHapticFeedback(haptic)
                    .verticalScroll(appearanceScrollState)
                    .padding(horizontal = 20.dp)
            ) {
                    Spacer(modifier = Modifier.height(statusBarPadding + 26.dp))
                    // 二级菜单顶部导航栏 (返回键 + 标题)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.perform(HapticType.CLICK)
                                closeSubMenuNormal()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "返回设置",
                                tint = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "界面与个性化",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Section 1: 自定义背景与相册壁纸
                    Text(
                        text = "自定义背景与相册壁纸",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val wallpaperCardShape = RoundedCornerShape(22.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = wallpaperCardShape,
                                tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                                borderWidth = 0.dp,
                                elevation = 4.dp,
                                isDark = isDark
                            )
                            .clickable {
                                haptic.perform(HapticType.CLICK)
                                onOpenWallpaperStudio()
                            }
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = "Wallpaper",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "壁纸视觉工坊",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (wallpaperConfig.uri != null) "已使用本地照片 (点击卡片调节)"
                                        else "选择相册图片或调整模糊/景深",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "进入壁纸视觉工坊",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 2: 材质与背景模式
                    Text(
                        text = "材质与背景模式",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactOptionTile(
                                title = BackgroundMode.LIQUID_GLASS.getDisplayTitle(isDark),
                                icon = Icons.Rounded.WaterDrop,
                                isSelected = currentMode == BackgroundMode.LIQUID_GLASS,
                                onClick = { onModeChange(BackgroundMode.LIQUID_GLASS) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            CompactOptionTile(
                                title = BackgroundMode.FROSTED_BLUR.getDisplayTitle(isDark),
                                icon = Icons.Rounded.AutoAwesome,
                                isSelected = currentMode == BackgroundMode.FROSTED_BLUR,
                                onClick = { onModeChange(BackgroundMode.FROSTED_BLUR) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactOptionTile(
                                title = BackgroundMode.ACRYLIC.getDisplayTitle(isDark),
                                icon = Icons.Rounded.ColorLens,
                                isSelected = currentMode == BackgroundMode.ACRYLIC,
                                onClick = { onModeChange(BackgroundMode.ACRYLIC) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            CompactOptionTile(
                                title = BackgroundMode.MINIMAL_CLEAN.getDisplayTitle(isDark),
                                icon = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                isSelected = currentMode == BackgroundMode.MINIMAL_CLEAN,
                                onClick = { onModeChange(BackgroundMode.MINIMAL_CLEAN) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactOptionTile(
                                title = BackgroundMode.CUSTOM_WALLPAPER.getDisplayTitle(isDark),
                                icon = Icons.Rounded.Image,
                                isSelected = currentMode == BackgroundMode.CUSTOM_WALLPAPER,
                                onClick = { onModeChange(BackgroundMode.CUSTOM_WALLPAPER) },
                                isDark = isDark,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 3: 外观 (深浅色模式)
                    Text(
                        text = "外观",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactOptionTile(
                            title = "深色模式",
                            icon = Icons.Rounded.DarkMode,
                            isSelected = themeMode == 2,
                            onClick = { onThemeModeChange(2) },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        CompactOptionTile(
                            title = "浅色模式",
                            icon = Icons.Rounded.LightMode,
                            isSelected = themeMode == 1,
                            onClick = { onThemeModeChange(1) },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        CompactOptionTile(
                            title = "跟随系统",
                            icon = Icons.Rounded.BrightnessAuto,
                            isSelected = themeMode == 0,
                            onClick = { onThemeModeChange(0) },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 4: 头图（与主页同步，控件常显）
                    Text(
                        text = "头图",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Text(
                        text = "与主页同步：拖右下角弧形把手改高度，拖右侧滑杆改透明度",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    RadarHeroCard(
                        statusText = "提权成功",
                        versionText = "头图预览",
                        animationType = heroAnimationType,
                        onAnimationTypeChange = onHeroAnimationTypeChange,
                        backgroundMode = currentMode,
                        isDark = isDark,
                        cardHeightDp = heroCardHeight,
                        onCardHeightChange = onHeroCardHeightChange,
                        cardAlpha = heroCardAlpha,
                        onCardAlphaChange = onHeroCardAlphaChange,
                        forceShowControls = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 5: 状态卡片动效风格
                    Text(
                        text = "状态卡片动效风格",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactOptionTile(
                                title = HeroAnimationType.CLASSIC_RADAR.badgeName,
                                icon = Icons.Rounded.TrackChanges,
                                isSelected = heroAnimationType == HeroAnimationType.CLASSIC_RADAR,
                                onClick = { onHeroAnimationTypeChange(HeroAnimationType.CLASSIC_RADAR) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            CompactOptionTile(
                                title = HeroAnimationType.FLUID_CORE.badgeName,
                                icon = Icons.Rounded.BlurOn,
                                isSelected = heroAnimationType == HeroAnimationType.FLUID_CORE,
                                onClick = { onHeroAnimationTypeChange(HeroAnimationType.FLUID_CORE) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactOptionTile(
                                title = HeroAnimationType.AURORA_WAVES.badgeName,
                                icon = Icons.Rounded.Waves,
                                isSelected = heroAnimationType == HeroAnimationType.AURORA_WAVES,
                                onClick = { onHeroAnimationTypeChange(HeroAnimationType.AURORA_WAVES) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            CompactOptionTile(
                                title = HeroAnimationType.CORONA_ECLIPSE.badgeName,
                                icon = Icons.Rounded.WbSunny,
                                isSelected = heroAnimationType == HeroAnimationType.CORONA_ECLIPSE,
                                onClick = { onHeroAnimationTypeChange(HeroAnimationType.CORONA_ECLIPSE) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 5: 手势与系统交互
                    Text(
                        text = "手势与系统交互",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val gestureCardShape = RoundedCornerShape(22.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = gestureCardShape,
                                tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                                borderWidth = 0.dp,
                                elevation = 4.dp,
                                isDark = isDark
                            )
                            .clickable {
                                haptic.perform(HapticType.CLICK)
                                onPredictiveBackToggle(!isPredictiveBackEnabled)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPredictiveBackEnabled) {
                                            Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB)))
                                        } else {
                                            Brush.linearGradient(
                                                if (isDark) listOf(Color(0xFF334155), Color(0xFF1E293B))
                                                else listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "原生预测性返回手势",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "支持 Android 系统级侧滑缩放与窗口视差返回",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            LiquidGlassSwitch(
                                checked = isPredictiveBackEnabled,
                                onCheckedChange = {
                                    onPredictiveBackToggle(it)
                                },
                                isDark = isDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(140.dp))
        }
    }
}
}

// Clean, symmetric, perfectly aligned Theme selection tile
@Composable
fun CompactOptionTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val haptic = rememberHapticFeedback()

    val bgColor = if (isSelected) {
        if (isDark) Color(0x451E3A8A) else Color(0x2E3B82F6)
    } else {
        if (isDark) Color(0x281E293B) else Color(0x38FFFFFF)
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .shadow(
                elevation = if (isSelected) 4.dp else 1.dp,
                shape = cardShape,
                ambientColor = if (isDark) Color(0x35000000) else Color(0x12000000),
                spotColor = Color.Transparent
            )
            .clip(cardShape)
            .background(bgColor)
            .clickable {
                haptic.perform(HapticType.CLICK)
                onClick()
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) {
                    if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                } else {
                    if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                },
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) {
                    if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                } else {
                    if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                },
                maxLines = 1
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f, fill = false),
            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
        )
    }
}
