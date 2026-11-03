package com.KozOnekey.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.RootManager
import com.KozOnekey.RootState
import com.KozOnekey.effects.liquidGlass
import com.KozOnekey.theme.BackgroundMode

private fun stepColor(state: Int): Color = when (state) {
    2 -> Color(0xFF10B981)
    3 -> Color(0xFFEF4444)
    1 -> Color(0xFF3B82F6)
    else -> Color(0xFF64748B)
}

private fun stepText(state: Int, extra: String?): String = when (state) {
    2 -> "完成"
    3 -> "失败"
    1 -> "进行中"
    else -> "待处理"
}.let { if (extra.isNullOrEmpty()) it else it + " · " + extra }

@Composable
fun RootScreen(
    state: RootState,
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    onOpenManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let {
        if (it > 20.dp) it else 48.dp
    }
    val logScroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(state.logText) {
        logScroll.animateScrollTo(logScroll.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(statusBarPadding + 10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "提权",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stepColor(state.statusLevel))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = stepColor(state.statusLevel)
                )
                if (state.statusHint.isNotEmpty()) {
                    Text(
                        text = " · " + state.statusHint,
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // 上方行：版本信息 + FinaSU / BakaSU（分别与下方 KernelSU / SukiSU 列对齐）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = "v1.98 · 临时 Root",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            ManagerChip(state, RootManager.FINASU, isDark, Modifier.weight(1f))
            ManagerChip(state, RootManager.BAKASU, isDark, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 下方行：KernelSU-Next / KernelSU / SukiSU-Ultra
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ManagerChip(state, RootManager.KERNELSU_NEXT, isDark, Modifier.weight(1f))
            ManagerChip(state, RootManager.KERNELSU, isDark, Modifier.weight(1f))
            ManagerChip(state, RootManager.SUKISU_ULTRA, isDark, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // progress card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(24.dp),
                    tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                    borderWidth = 0.dp,
                    elevation = 4.dp,
                    isDark = isDark
                )
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "提权进度",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                    Text(
                        text = state.stepsDone.toString() + " / " + state.selectedChain.stageCount(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                }
                for (i in 0 until state.selectedChain.stageCount()) {
                    val st = state.stageStates[i]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(stepColor(st).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (st) {
                                    2 -> Icons.Rounded.CheckCircle
                                    3 -> Icons.Rounded.ErrorOutline
                                    1 -> Icons.Rounded.PlayArrow
                                    else -> Icons.Rounded.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = stepColor(st),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.selectedChain.stageNames[i],
                            fontSize = 12.5.sp,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stepText(st, state.stageExtras[i]),
                            fontSize = 11.5.sp,
                            color = stepColor(st)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // log card (fills the remaining height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .liquidGlass(
                    shape = RoundedCornerShape(24.dp),
                    tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                    borderWidth = 0.dp,
                    elevation = 4.dp,
                    isDark = isDark
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "运行日志",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(state.logText))
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "复制日志",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x33000000) else Color(0x0A000000))
                        .verticalScroll(logScroll)
                        .padding(10.dp)
                ) {
                    Text(
                        text = state.logText.ifEmpty { "等待开始 ..." },
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val promptOpen = state.rooted && state.lastOk == true && state.postRootOpenPrompt && !state.running
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        promptOpen -> if (isDark) Color(0xFF059669) else Color(0xFF10B981)
                        isDark -> Color(0xFF3B82F6)
                        else -> Color(0xFF2563EB)
                    }
                )
                .clickable(enabled = !state.running) {
                    if (promptOpen) state.openManager() else state.start()
                }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    state.running -> "处理中 ..."
                    promptOpen -> "提权成功 · 点击打开管理器"
                    state.rooted -> "重新检测 / 修复"
                    state.lastOk == true -> "再次提权"
                    state.lastOk == false -> "重新尝试"
                    else -> "开始提权"
                },
                color = Color.White,
                fontSize = if (promptOpen) 15.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onOpenManager() }
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "打开 " + state.managerName + " 管理器",
                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}

@Composable
private fun ManagerChip(
    state: RootState,
    m: RootManager,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val selected = state.selectedManager == m
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    if (isDark) Color(0x333B82F6) else Color(0x222563EB)
                } else {
                    if (isDark) Color(0x14FFFFFF) else Color(0x0D000000)
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF3B82F6).copy(alpha = 0.7f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !state.running) { state.selectManager(m) }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = m.displayName,
            fontSize = 9.5.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selected -> Color(0xFF60A5FA)
                isDark -> Color(0xFF94A3B8)
                else -> Color(0xFF64748B)
            }
        )
    }
}
