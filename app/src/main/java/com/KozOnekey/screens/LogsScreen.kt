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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.KozOnekey.RunStore
import com.KozOnekey.ShareUtil
import com.KozOnekey.RootState
import com.KozOnekey.components.GlassDialog
import com.KozOnekey.effects.liquidGlass
import com.KozOnekey.theme.BackgroundMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun durationText(seconds: Long, estimated: Boolean = false): String = when {
    seconds < 0 -> "已中断（用时未知）"
    estimated -> "约 " + seconds + " 秒（中断前）"
    else -> "用时 " + seconds + " 秒"
}

@Composable
fun LogsScreen(
    state: RootState,
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    onOpenLog: (RunStore.RunRecord) -> Unit = {},
    onRequestClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let {
        if (it > 20.dp) it else 48.dp
    }
    // reading recordsVersion keeps the list fresh whenever a new run finishes
    val version = state.recordsVersion
    val records = remember(version) { RunStore.list(ctx) }
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.US) }

    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(statusBarPadding + 26.dp))
        Text(
            text = "运行日志",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "共 " + records.size + " 条记录 · 自动保存在本机",
            fontSize = 12.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionChip(
                icon = Icons.Rounded.Download,
                text = "导出全部",
                isDark = isDark,
                onClick = {
                    val all = records.joinToString("\n\n") { "==== " + fmt.format(Date(it.time)) + " ====\n" + RunStore.readLog(it) }
                    ShareUtil.shareText(ctx, "KozRoot 全部运行日志", all)
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            ActionChip(
                icon = Icons.Rounded.DeleteOutline,
                text = "清除记录",
                isDark = isDark,
                danger = true,
                onClick = { onRequestClear() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(26.dp),
                        tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                        borderWidth = 0.dp,
                        elevation = 4.dp,
                        isDark = isDark
                    )
                    .padding(vertical = 34.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "暂无运行记录",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                    )
                    Text(
                        text = "完成一次提权后，这里会显示结果和完整日志",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        } else {
            records.forEach { rec ->
                LogCard(
                    record = rec,
                    timeText = fmt.format(Date(rec.time)),
                    isDark = isDark,
                    onClick = { onOpenLog(rec) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(150.dp))
    }
}

@Composable
private fun LogCard(
    record: RunStore.RunRecord,
    timeText: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val accent = if (record.ok) Color(0xFF10B981) else Color(0xFFEF4444)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
                borderWidth = 0.dp,
                elevation = 3.dp,
                isDark = isDark
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (record.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (record.ok) "提权成功" else "提权失败",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Text(
                text = timeText + " · " + durationText(record.seconds, record.estimated),
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isDark: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val accent = if (danger) Color(0xFFEF4444) else (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB))
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}
