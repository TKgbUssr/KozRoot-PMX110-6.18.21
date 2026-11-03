package com.KozOnekey.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.RootState
import com.KozOnekey.components.QuickStatsRow
import com.KozOnekey.components.RadarHeroCard
import com.KozOnekey.components.SpecItem
import com.KozOnekey.components.SystemSpecCard
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.liquidGlass
import com.KozOnekey.effects.overscrollHapticFeedback
import com.KozOnekey.effects.rememberHapticFeedback
import com.KozOnekey.models.HeroAnimationType
import com.KozOnekey.theme.BackgroundMode

@Composable
fun HomeScreen(
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    state: RootState,
    heroAnimationType: HeroAnimationType = HeroAnimationType.CLASSIC_RADAR,
    onHeroAnimationTypeChange: (HeroAnimationType) -> Unit = {},
    heroCardAlpha: Float = 0.75f,
    onHeroCardAlphaChange: (Float) -> Unit = {},
    heroCardHeight: Float = 150f,
    onHeroCardHeightChange: (Float) -> Unit = {},
    onNavigateToRoot: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenStellar: () -> Unit,
    onShowBanner: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableIntStateOf(2) }

    val scrollState = rememberScrollState()
    val haptic = rememberHapticFeedback()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let {
        if (it > 20.dp) it else 48.dp
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .overscrollHapticFeedback(haptic)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(statusBarPadding + 14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KozRoot",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "临时 Root · " + state.managerName,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
                IconButton(
                    onClick = {
                        haptic.perform(HapticType.CLICK)
                        onOpenManager()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = "打开管理器",
                        tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            RadarHeroCard(
                statusText = state.statusText,
                versionText = "v1.98 · 临时 Root" + if (state.statusHint.isNotEmpty()) " · " + state.statusHint else "",
                animationType = heroAnimationType,
                onAnimationTypeChange = onHeroAnimationTypeChange,
                backgroundMode = backgroundMode,
                isDark = isDark,
                cardHeightDp = heroCardHeight,
                onCardHeightChange = onHeroCardHeightChange,
                cardAlpha = heroCardAlpha,
                onCardAlphaChange = onHeroCardAlphaChange,
                // 只“旁听”单击（Initial 阶段、不消费事件），保留卡片原有的长按 / 拖动 / 缩放
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            val downTime = down.uptimeMillis
                            val downPos = down.position
                            var isTap = true
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUpIgnoreConsumed()) {
                                    val dt = change.uptimeMillis - downTime
                                    if (dt !in 0L..350L) isTap = false
                                    break
                                }
                                if ((change.position - downPos).getDistance() > viewConfiguration.touchSlop) {
                                    isTap = false
                                    break
                                }
                            }
                            if (isTap) {
                                if (state.stellarConnected == true) {
                                    onShowBanner("Stellar 已连接")
                                } else {
                                    onShowBanner("正在打开 Stellar ...")
                                    onOpenStellar()
                                }
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Spacer(modifier = Modifier.height(16.dp))

            QuickStatsRow(
                superuserCount = state.stepsDone,
                moduleCount = state.recordCount,
                label1 = "已完成步骤",
                label2 = "运行记录",
                backgroundMode = backgroundMode,
                isDark = isDark,
                onSuperuserClick = onNavigateToRoot,
                onModuleClick = onNavigateToLogs
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = selectedMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "modeSwitch"
            ) { mode ->
                if (mode == 2) {
                    SystemSpecCard(
                        backgroundMode = backgroundMode,
                        isDark = isDark,
                        onCopy = onShowBanner,
                        specs = listOf(
                            SpecItem("设备型号", "OPPO " + state.devModel),
                            SpecItem("内核版本", state.devKernel),
                            SpecItem("系统版本", state.devSystem),
                            SpecItem("SELinux", state.devSelinux),
                            SpecItem("Stellar 通道", when (state.stellarConnected) {
                                true -> "已连接"
                                false -> "未连接"
                                null -> "检测中 ..."
                            }),
                            SpecItem(state.managerName + " 管理器", when (state.managerPresent) {
                                true -> "已安装"
                                false -> "未安装"
                                null -> "检测中 ..."
                            })
                        )
                    )
                } else {
                    RootControlsCard(
                        state = state,
                        backgroundMode = backgroundMode,
                        isDark = isDark,
                        onStart = onNavigateToRoot,
                        onOpenManager = onOpenManager,
                        onOpenStellar = onOpenStellar
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                        .clickable {
                            haptic.perform(HapticType.CLICK)
                            selectedMode = if (selectedMode == 1) 2 else 1
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedMode/2",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedMode == 2) "系统规格" else "提权控制",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun RootControlsCard(
    state: RootState,
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    onStart: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenStellar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)
    val haptic = rememberHapticFeedback()
    val running = state.running

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = cardShape,
                tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                borderWidth = 0.dp,
                elevation = 4.dp,
                isDark = isDark
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "提权控制",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )

            InfoRow(
                icon = Icons.Rounded.Link,
                title = "Stellar shell 通道",
                subtitle = when (state.stellarConnected) {
                    true -> "已连接，shell 权限就绪"
                    false -> "未连接，点击打开 Stellar"
                    null -> "检测中 ..."
                },
                valueColor = if (state.stellarConnected == true) Color(0xFF10B981) else Color(0xFFF59E0B),
                isDark = isDark,
                onClick = onOpenStellar
            )

            InfoRow(
                icon = Icons.Rounded.AdminPanelSettings,
                title = state.managerName + " 管理器",
                subtitle = when (state.managerPresent) {
                    true -> "已安装，点击打开"
                    false -> "未安装，运行时会自动安装"
                    null -> "检测中 ..."
                },
                valueColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                isDark = isDark,
                onClick = onOpenManager
            )

            InfoRow(
                icon = Icons.Rounded.Security,
                title = "SELinux",
                subtitle = "当前模式：" + state.devSelinux,
                valueColor = if (state.devSelinux == "Permissive") Color(0xFFF59E0B) else Color(0xFF10B981),
                isDark = isDark,
                onClick = {}
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.perform(HapticType.CLICK)
                        if (!running) onStart()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        running -> "处理中 ..."
                        state.rooted -> "重新检测 / 修复"
                        state.lastOk == true -> "再次提权"
                        state.lastOk == false -> "重新尝试"
                        else -> "开始提权"
                    },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = valueColor
                )
            }
        }
    }
}
