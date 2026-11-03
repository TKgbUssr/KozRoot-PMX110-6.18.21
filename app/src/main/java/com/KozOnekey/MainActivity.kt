package com.KozOnekey

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.KozOnekey.effects.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Job
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.KozOnekey.components.LiquidGlassBottomBar
import com.KozOnekey.components.LiquidGlassStatusBar
import com.KozOnekey.components.OpenSourceCreditsDialog
import com.KozOnekey.components.WallpaperAdjustDialog
import com.KozOnekey.effects.AmbientFluidBackground
import com.KozOnekey.models.HeroAnimationType
import com.KozOnekey.models.WallpaperConfig
import com.KozOnekey.RootState
import com.KozOnekey.RunStore
import com.KozOnekey.components.GlassDialog
import com.KozOnekey.components.GlassDialogActionButton
import com.KozOnekey.components.GlassDialogActionRow
import com.KozOnekey.screens.durationText
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import com.KozOnekey.Shell
import com.KozOnekey.screens.HomeScreen
import com.KozOnekey.screens.LogsScreen
import com.KozOnekey.screens.RootScreen
import com.KozOnekey.screens.SettingsScreen
import com.KozOnekey.theme.BackgroundMode
import com.KozOnekey.theme.LiquidGlassTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.KozOnekey.effects.createProgressiveTopBlurEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * =============================================================================
 * 【液态玻璃 UI 模板架构核心 - MainActivity】
 * =============================================================================
 * 本模板提供了一套完整的旗舰级 Android 现代 UI 解决方案，主要包含以下核心系统：
 *
 * 1. 【顶部状态栏分离式无级高斯渐弱磨砂 (Separable Progressive Gaussian Blur)】：
 *    - 核心实现位于 [ProgressiveBlur.kt]：拆分为水平 + 垂直两个 1D 线性采样 Pass；
 *    - 彻底杜绝稀疏采样导致的“散光/近视重影”，提供真正连续平滑的高斯磨砂扩散；
 *    - 默认衰减区高度为 statusBarPadding + 38dp，与下方标题（statusBarPadding + 26dp）留出自然空间。
 *
 * 2. 【苹果 iOS 18 风格液态玻璃悬浮底栏 (Floating Liquid Glass Bottom Bar)】：
 *    - 核心实现位于 [LiquidGlassBottomBar.kt]；
 *    - 支持手指长按横向滑动无级切换（Scrubbing），页面与滑块药丸实时 1:1 跟随；
 *    - 内置物理齿轮滴答触觉反馈 (Haptic Feedback) 与双向连续联动。
 *
 * 3. 【零重组原生硬件运动模糊页面容器 (HorizontalPager)】：
 *    - 在 RenderNode 绘制层 (graphicsLayer) 直接根据滑动速度施加定向高斯运动模糊，60/120 FPS 丝滑满帧。
 *
 * -----------------------------------------------------------------------------
 * 🛠️【如何套用替换为您自己的内容】:
 * 1. 替换页面内容：在下方 `HorizontalPager` 的 `when (pageIndex)` 中替换各页面组件；
 * 2. 增减标签页数：
 *    - 修改 `rememberPagerState(pageCount = { 4 })` 中的页数；
 *    - 修改 [LiquidGlassBottomBar.kt] 内的 `items` 列表（添加/减少 NavItem 图标与文字）；
 * 3. 调整顶部虚化范围：
 *    - 调整 `fadeZoneHeight = statusBarPadding + 38.dp` 与 `maxRadiusPx`；
 *    - 子页面标题顶部统一留白：`Spacer(modifier = Modifier.height(statusBarPadding + 26.dp))`。
 * =============================================================================
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SignatureCheck.verify(applicationContext)) {
            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B0F19)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "非官方版本，已停止运行",
                        color = Color(0xFFE2E8F0),
                        fontSize = 18.sp
                    )
                }
            }
            return
        }
        Shell.init(applicationContext)
        // 沉浸式边到边布局
        enableEdgeToEdge()

        setContent {
            val appContext = LocalContext.current
            val rootState = remember { RootState(appContext) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { rootState.refresh() }

            // 提权运行中按返回键：回到桌面/后台继续跑，不退出 App
            BackHandler(enabled = rootState.running) {
                this@MainActivity.moveTaskToBack(true)
            }

            // ---------- persistent preferences ----------
            val prefs = remember { Prefs(appContext) }
            var bannerText by remember { mutableStateOf<String?>(null) }
            var selectedLogRecord by remember { mutableStateOf<RunStore.RunRecord?>(null) }
            var displayedLogRecord by remember { mutableStateOf<RunStore.RunRecord?>(null) }
            var showClearRecordsConfirm by remember { mutableStateOf(false) }
            val showBanner: (String) -> Unit = { msg -> bannerText = msg }
            LaunchedEffect(bannerText) {
                if (bannerText != null) {
                    delay(2400)
                    bannerText = null
                }
            }
            LaunchedEffect(rootState.uiMessage) {
                rootState.uiMessage?.let {
                    showBanner(it)
                    rootState.clearUiMessage()
                }
            }
            // 系统与应用主题状态
            val systemDark = isSystemInDarkTheme()
            // 默认深色；用户可切换浅色或跟随系统
            var themeMode by remember { mutableIntStateOf(prefs.themeMode) }
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }

            // 动态响应深浅色切换，确保浅色模式下状态栏文字为深色，深色模式下为浅色
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }
            var backgroundMode by remember {
                mutableStateOf(
                    runCatching { BackgroundMode.valueOf(prefs.backgroundMode) }
                        .getOrDefault(BackgroundMode.LIQUID_GLASS)
                )
            }
            var wallpaperConfig by remember {
                mutableStateOf(
                    WallpaperConfig(
                        uri = prefs.wallpaperUri?.let { Uri.parse(it) },
                        presetId = prefs.wallpaperPreset,
                        blurRadius = prefs.wallpaperBlur,
                        dimAlpha = prefs.wallpaperDim,
                        scaleMode = prefs.wallpaperScale
                    )
                )
            }
            var heroAnimationType by remember {
                mutableStateOf(
                    runCatching { HeroAnimationType.valueOf(prefs.heroAnimation) }
                        .getOrDefault(HeroAnimationType.CLASSIC_RADAR)
                )
            }
            var heroCardAlpha by remember { mutableFloatStateOf(prefs.heroAlpha) }
            var heroCardHeight by remember { mutableFloatStateOf(prefs.heroCardHeight) }
            var isPredictiveBackEnabled by remember { mutableStateOf(prefs.predictiveBack) }

            LaunchedEffect(themeMode) { prefs.themeMode = themeMode }
            LaunchedEffect(backgroundMode) { prefs.backgroundMode = backgroundMode.name }
            LaunchedEffect(heroAnimationType) { prefs.heroAnimation = heroAnimationType.name }
            LaunchedEffect(heroCardAlpha) { prefs.heroAlpha = heroCardAlpha }
            LaunchedEffect(heroCardHeight) { prefs.heroCardHeight = heroCardHeight }
            LaunchedEffect(isPredictiveBackEnabled) { prefs.predictiveBack = isPredictiveBackEnabled }
            // 旧版本存的是 content:// URI，重启后可能失去读取权限：复制到私有目录再保存
            LaunchedEffect(Unit) {
                val wc = wallpaperConfig
                if (wc.uri != null && wc.uri.scheme == "content") {
                    val copied = withContext(Dispatchers.IO) {
                        runCatching {
                            val dir = java.io.File(appContext.filesDir, "wallpaper").apply { mkdirs() }
                            val f = java.io.File(dir, "custom_wallpaper.img")
                            appContext.contentResolver.openInputStream(wc.uri)?.use { input ->
                                f.outputStream().use { output -> input.copyTo(output) }
                            }
                            Uri.fromFile(f)
                        }.getOrNull()
                    }
                    if (copied != null) {
                        wallpaperConfig = wc.copy(uri = copied)
                    }
                }
            }
            LaunchedEffect(wallpaperConfig) {
                prefs.wallpaperUri = wallpaperConfig.uri?.toString()
                prefs.wallpaperPreset = wallpaperConfig.presetId
                prefs.wallpaperBlur = wallpaperConfig.blurRadius
                prefs.wallpaperDim = wallpaperConfig.dimAlpha
                prefs.wallpaperScale = wallpaperConfig.scaleMode
            }
            var isCreditsDialogOpen by remember { mutableStateOf(false) }
            var isWallpaperStudioOpen by remember { mutableStateOf(false) }

            val appAeroBlurRadius by animateFloatAsState(
                targetValue = if (isCreditsDialogOpen || isWallpaperStudioOpen || selectedLogRecord != null || showClearRecordsConfirm || rootState.residueDialogVisible) 36f else 0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "appAeroBlur"
            )

            // 4个核心模块选项卡 Pager 控制器
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
            val coroutineScope = rememberCoroutineScope()

            // 离开设置页时，自动关闭可能开启的弹窗
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != 3) {
                    isWallpaperStudioOpen = false
                    isCreditsDialogOpen = false
                }
            }

            // 跨选项卡物理全景飞越导航控制器（越过中间每一个页面平滑飞掠，绝无跳帧）
            var pageFlightJob by remember { mutableStateOf<Job?>(null) }
            var flightStartPage by remember { mutableFloatStateOf(0f) }
            var flightTargetPage by remember { mutableFloatStateOf(0f) }
            var isFlightActive by remember { mutableStateOf(false) }

            val navigateFlyToPage: (Int) -> Unit = { targetIndex ->
                pageFlightJob?.cancel()
                val startFloat = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val targetFloat = targetIndex.toFloat()
                val distance = abs(targetFloat - startFloat)
                if (distance > 0.005f) {
                    flightStartPage = startFloat
                    flightTargetPage = targetFloat
                    isFlightActive = true

                    // 距离自适应物理飞跃时长（1页约 270ms，3页约 410ms）：轻盈利落，帧帧相扣绝不跳页
                    val duration = (200 + distance * 70).toInt().coerceIn(220, 420)
                    val durationNanos = duration * 1_000_000L
                    val easing = FastOutSlowInEasing

                    pageFlightJob = coroutineScope.launch {
                        try {
                            val startTime = withFrameNanos { it }
                            while (true) {
                                val frameTime = withFrameNanos { it }
                                val playTime = (frameTime - startTime).coerceAtLeast(0L)
                                val fraction = (playTime.toFloat() / durationNanos).coerceIn(0f, 1f)
                                val eased = easing.transform(fraction)
                                val currentVal = startFloat + (targetFloat - startFloat) * eased
                                val targetPage = kotlin.math.round(currentVal).toInt().coerceIn(0, 3)
                                val offset = (currentVal - targetPage).coerceIn(-0.5f, 0.5f)
                                pagerState.scrollToPage(targetPage, offset)
                                if (fraction >= 1f) break
                            }
                            pagerState.scrollToPage(targetIndex, 0f)
                        } finally {
                            isFlightActive = false
                        }
                    }
                }
            }

            // Android 原生预测性返回手势：在子页面返回首页（Tab 0）
            if (isPredictiveBackEnabled && pagerState.currentPage > 0 && !isCreditsDialogOpen && !isWallpaperStudioOpen && selectedLogRecord == null && !showClearRecordsConfirm && !rootState.residueDialogVisible) {
                PredictiveBackHandler(enabled = true) { progressFlow ->
                    try {
                        progressFlow.collect {
                            // 收集系统原生预测返回进度
                        }
                        navigateFlyToPage(0)
                    } catch (e: Exception) {
                        // 手势撤销或中断
                    }
                }
            }

            // 采用 derivedStateOf 仅使底栏滑块响应浮点偏移，彻底避免全屏幕 Root 发生每帧重复重组
            val continuousPage by remember {
                derivedStateOf {
                    pagerState.currentPage + pagerState.currentPageOffsetFraction
                }
            }

            // 捕获页面内容真实图层，提供底层内容到液态玻璃底栏的高保真物理折射与模糊
            val contentBackdrop = rememberLayerBackdrop()

            // 计算顶部状态栏无感渐弱高斯虚化参数与硬件滤镜
            val density = LocalDensity.current
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let {
                if (it > 20.dp) it else 48.dp
            }
            val fadeZoneHeight = statusBarPadding + 38.dp
            val fadeHeightPx = with(density) { fadeZoneHeight.toPx() }
            val maxRadiusPx = with(density) { 18.dp.toPx() }

            val screenWidthPx = androidx.compose.ui.platform.LocalContext.current
                .resources.displayMetrics.widthPixels.toFloat()

            // 分离式（水平+垂直）可变半径高斯磨砂：逐像素连续采样，无离散重影
            val progressiveTopBlurEffect = remember(fadeHeightPx, maxRadiusPx, screenWidthPx) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && fadeHeightPx > 0f) {
                    createProgressiveTopBlurEffect(
                        fadeHeightPx = fadeHeightPx,
                        maxRadiusPx = maxRadiusPx,
                        widthPx = screenWidthPx
                    ).asComposeRenderEffect()
                } else null
            }

            LiquidGlassTheme(darkTheme = isDark) {
                // 环境流体全屏动态背景
                AmbientFluidBackground(
                    mode = backgroundMode,
                    wallpaperConfig = wallpaperConfig,
                    isDark = isDark
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 主界面图层（含全页面 Pager 与液态玻璃底栏），弹窗开启时实施全屏虚化，日常实施顶部渐弱虚化
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        if (appAeroBlurRadius > 0.5f) {
                                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                                appAeroBlurRadius,
                                                appAeroBlurRadius,
                                                android.graphics.Shader.TileMode.CLAMP
                                            ).asComposeRenderEffect()
                                        } else {
                                            renderEffect = progressiveTopBlurEffect
                                        }
                                    }
                                }
                        ) {
                        // -----------------------------------------------------------------
                        // 【主屏水平滑动页面容器 (HorizontalPager)】
                        // 承载 4 大页面，并在 Layer 绘制阶段直接叠加原生硬件加速的横向运动模糊滤镜
                        // -----------------------------------------------------------------
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 4, // 预载全量 4 个页面，跨页面（如 0 到 3）跳转时彻底消除临时重组与测量卡顿
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(contentBackdrop)
                                .graphicsLayer {
                                    // Android 12+ (API 31+) 原生硬件加速动态运动模糊渲染
                                    // 彻底消除卡顿优化：在 RenderNode 绘制层直接读取滑动进度，不触发任何 Compose 重组
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (pagerState.isScrollInProgress || isFlightActive)) {
                                        val currentPos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                        val rawBlur: Float = if (isFlightActive) {
                                            // 点击底栏跨页飞掠：以全程物理位移解算正弦钟形包络曲线（在中点速度最高处模糊最浓烈，两端平滑收敛）
                                            val flightSpan = abs(flightTargetPage - flightStartPage)
                                            val progress = if (flightSpan > 0.001f) {
                                                ((currentPos - flightStartPage) / (flightTargetPage - flightStartPage)).coerceIn(0f, 1f)
                                            } else 0f
                                            val bell = sin(progress * Math.PI.toFloat()).coerceIn(0f, 1f)
                                            bell * 22f // 飞跃时拥有强烈的流动感与动态模糊
                                        } else {
                                            // 用户手指直接横向滑屏
                                            val bell = sin(abs(pagerState.currentPageOffsetFraction) * Math.PI.toFloat()).coerceIn(0f, 1f)
                                            bell * 16f
                                        }

                                        if (rawBlur > 0.8f) {
                                            // 0.5dp 精度量化步长，避免极微小的浮点波动引起 Skia 着色器频繁重编
                                            val quantizedBlurX = ((rawBlur * 2f).toInt() / 2f).coerceIn(1f, 26f)
                                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                                quantizedBlurX,
                                                0.01f, // 垂直方向设为极小值 0.01f（必须大于0避免系统抛出异常），呈现纯正横向定向光学运动模糊
                                                android.graphics.Shader.TileMode.CLAMP
                                            ).asComposeRenderEffect()
                                        } else {
                                            renderEffect = null
                                        }
                                    } else {
                                        renderEffect = null
                                    }
                                }
                        ) { pageIndex ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            ) {
                                when (pageIndex) {
                                    0 -> HomeScreen(
                                        backgroundMode = backgroundMode,
                                        isDark = isDark,
                                        state = rootState,
                                        heroAnimationType = heroAnimationType,
                                        onHeroAnimationTypeChange = { heroAnimationType = it },
                                        heroCardAlpha = heroCardAlpha,
                                        onHeroCardAlphaChange = { heroCardAlpha = it },
                                        heroCardHeight = heroCardHeight,
                                        onHeroCardHeightChange = { heroCardHeight = it },
                                        onNavigateToRoot = { navigateFlyToPage(1) },
                                        onNavigateToLogs = { navigateFlyToPage(2) },
                                        onOpenManager = {
                                            rootState.openManager()
                                        },
                                        onOpenStellar = {
                                            if (rootState.stellarConnected != true) {
                                                if (rootState.stellarInstalled == true) {
                                                    openApp(appContext, "roro.stellar.manager")
                                                } else {
                                                    showBanner("请先安装 Stellar")
                                                }
                                            }
                                        },
                                        onShowBanner = showBanner
                                    )
                                    1 -> RootScreen(
                                        state = rootState,
                                        backgroundMode = backgroundMode,
                                        isDark = isDark,
                                        onOpenManager = {
                                            rootState.openManager()
                                        }
                                    )
                                    2 -> LogsScreen(
                                        state = rootState,
                                        backgroundMode = backgroundMode,
                                        isDark = isDark,
                                        onOpenLog = {
                                            displayedLogRecord = it
                                            selectedLogRecord = it
                                        },
                                        onRequestClear = { showClearRecordsConfirm = true }
                                    )
                                    3 -> SettingsScreen(
                                        currentMode = backgroundMode,
                                        onModeChange = { backgroundMode = it },
                                        wallpaperConfig = wallpaperConfig,
                                        onWallpaperConfigChange = { wallpaperConfig = it },
                                        heroAnimationType = heroAnimationType,
                                        onHeroAnimationTypeChange = { heroAnimationType = it },
                                        heroCardAlpha = heroCardAlpha,
                                        onHeroCardAlphaChange = { heroCardAlpha = it },
                                        heroCardHeight = heroCardHeight,
                                        onHeroCardHeightChange = { heroCardHeight = it },
                                        isDark = isDark,
                                        themeMode = themeMode,
                                        onThemeModeChange = { themeMode = it },
                                        isPredictiveBackEnabled = isPredictiveBackEnabled,
                                        onPredictiveBackToggle = { isPredictiveBackEnabled = it },
                                        onOpenCreditsDialog = { isCreditsDialogOpen = true },
                                        isCurrentPage = (pagerState.currentPage == 3),
                                        isWallpaperStudioOpen = isWallpaperStudioOpen,
                                        onOpenWallpaperStudio = { isWallpaperStudioOpen = true }
                                    )
                                }
                            }
                        }

                        // -----------------------------------------------------------------
                        // 【悬浮苹果液态玻璃底栏 (Floating Liquid Glass Bottom Bar)】
                        // 支持长按滑动、真实机械触觉反馈与连续联动
                        // -----------------------------------------------------------------
                        LiquidGlassBottomBar(
                            selectedIndex = pagerState.currentPage,
                            selectedFraction = continuousPage,
                            onTabSelected = { targetIndex ->
                                navigateFlyToPage(targetIndex)
                            },
                            // 底栏长按/拖拽时实时向 Pager 传递无级连续坐标，让页面跟着手指一起动
                            onScrubbing = { floatPage ->
                                pageFlightJob?.cancel()
                                coroutineScope.launch {
                                    val nearest = kotlin.math.round(floatPage).toInt().coerceIn(0, 3)
                                    val pageOffset = (floatPage - nearest).coerceIn(-0.5f, 0.5f)
                                    pagerState.scrollToPage(nearest, pageOffset)
                                }
                            },
                            // 拖拽释放时自然吸附回弹至目标选项卡
                            onScrubRelease = { finalTab ->
                                navigateFlyToPage(finalTab)
                            },
                            backgroundMode = backgroundMode,
                            isDark = isDark,
                            backdrop = contentBackdrop,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )

                        // -----------------------------------------------------------------
                        // 【顶部悬浮液态玻璃状态栏透景条 (Liquid Glass Status Bar)】
                        // 覆盖状态栏区域，对经过其下方的滑动页面卡片实施真实硬件高斯虚化与光学透镜折射
                        // -----------------------------------------------------------------
                        LiquidGlassStatusBar(
                            isDark = isDark,
                            backdrop = contentBackdrop,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                        // 轻提示条：单实例、不堆叠、自动消失（替代原生 Toast）
                        AnimatedVisibility(
                            visible = bannerText != null,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 104.dp, start = 20.dp, end = 20.dp)
                        ) {
                            BannerView(text = bannerText ?: "", isDark = isDark)
                        }
                        }

                        // 【最顶层弹窗图层 (Top-Level Modal Layer)】
                        // 处于全屏最上方，绝对覆盖底栏、页面与一切背景
                        OpenSourceCreditsDialog(
                            isOpen = isCreditsDialogOpen,
                            isDark = isDark,
                            backdrop = contentBackdrop,
                            onDismiss = { isCreditsDialogOpen = false }
                        )

                        WallpaperAdjustDialog(
                            isOpen = isWallpaperStudioOpen,
                            currentConfig = wallpaperConfig,
                            isDark = isDark,
                            backdrop = contentBackdrop,
                            onDismiss = { isWallpaperStudioOpen = false },
                            onApply = { newConfig ->
                                wallpaperConfig = newConfig
                                backgroundMode = BackgroundMode.CUSTOM_WALLPAPER
                            },
                            onReset = {
                                wallpaperConfig = WallpaperConfig()
                                backgroundMode = BackgroundMode.LIQUID_GLASS
                            }
                        )

                        // ---- 运行日志详情（与开源致谢同款：同层 overlay + backdrop 虚化） ----
                        val logDialogRec = displayedLogRecord
                        if (logDialogRec != null) {
                            val rec = logDialogRec
                            val logText = remember(rec) { RunStore.readLog(rec) }
                            GlassDialog(
                                visible = selectedLogRecord != null,
                                isDark = isDark,
                                icon = if (rec.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                iconTint = if (rec.ok) Color(0xFF10B981) else Color(0xFFEF4444),
                                title = if (rec.ok) "提权成功" else "提权失败",
                                subtitle = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                    .format(java.util.Date(rec.time)) + " · " + durationText(rec.seconds, rec.estimated),
                                onDismiss = { selectedLogRecord = null },
                                backdrop = contentBackdrop
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(360.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDark) Color(0x33000000) else Color(0x0A000000))
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = logText,
                                        fontSize = 10.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassDialogActionRow {
                                    GlassDialogActionButton("复制", isDark) {
                                        val cm = appContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                            as android.content.ClipboardManager
                                        cm.setPrimaryClip(android.content.ClipData.newPlainText("KozRoot-log", logText))
                                    }
                                    GlassDialogActionButton("导出", isDark) {
                                        ShareUtil.shareText(appContext, "KozRoot 运行日志", logText)
                                    }
                                    GlassDialogActionButton("删除", isDark, danger = true) {
                                        rec.file.delete()
                                        rootState.refreshRecords()
                                        selectedLogRecord = null
                                    }
                                }
                            }
                        }

                        // ---- 清除运行记录确认（同款玻璃窗口） ----
                        GlassDialog(
                            visible = showClearRecordsConfirm,
                            isDark = isDark,
                            icon = Icons.Rounded.DeleteOutline,
                            iconTint = Color(0xFFEF4444),
                            title = "清除运行记录",
                            subtitle = "此操作不可恢复",
                            onDismiss = { showClearRecordsConfirm = false },
                            backdrop = contentBackdrop
                        ) {
                            Text(
                                text = "确定删除全部提权历史吗？",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassDialogActionRow {
                                GlassDialogActionButton("取消", isDark) { showClearRecordsConfirm = false }
                                GlassDialogActionButton("删除", isDark, danger = true) {
                                    RunStore.deleteAll(appContext)
                                    rootState.refreshRecords()
                                    showClearRecordsConfirm = false
                                }
                            }
                        }

                        // ---- 内核残留提示（与日志/致谢同款液态玻璃弹窗） ----
                        GlassDialog(
                            visible = rootState.residueDialogVisible,
                            isDark = isDark,
                            icon = Icons.Rounded.WarningAmber,
                            iconTint = Color(0xFFF59E0B),
                            title = "内核状态提示",
                            subtitle = "检测到上次运行残留，成功率可能降低",
                            onDismiss = { rootState.residueCancel() },
                            backdrop = contentBackdrop
                        ) {
                            Text(
                                text = "POSIX timer 残留：" + rootState.residueTimerCount,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "旧 exploit 进程：" + rootState.residueExploitCount,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "正常应分别小于 1000 和 0。残留会明显降低竞态命中率、增加 panic 风险，建议重启手机后再执行。",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassDialogActionRow {
                                GlassDialogActionButton("重启", isDark) { rootState.residueReboot() }
                                GlassDialogActionButton("取消执行", isDark) { rootState.residueCancel() }
                                GlassDialogActionButton("继续执行", isDark, danger = true) { rootState.residueContinue() }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openApp(ctx: Context, pkg: String): Boolean {
    return try {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            true
        } else {
            false
        }
    } catch (_: Throwable) {
        false
    }
}

@Composable
private fun BannerView(text: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                tintColor = if (isDark) Color(0xE61E293B) else Color(0xF2FFFFFF),
                borderWidth = 0.dp,
                elevation = 8.dp,
                isDark = isDark
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )
        }
    }
}

