package com.KozOnekey.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.acrylicMaterial
import com.KozOnekey.effects.frostedGlass
import com.KozOnekey.effects.liquidGlass
import com.KozOnekey.effects.rememberHapticFeedback
import com.KozOnekey.theme.BackgroundMode

@Composable
fun QuickStatsRow(
    superuserCount: Int = 0,
    moduleCount: Int = 0,
    label1: String = "已完成步骤",
    label2: String = "运行记录",
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    onSuperuserClick: () -> Unit = {},
    onModuleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatCard(
            count = superuserCount.toString(),
            label = label1,
            backgroundMode = backgroundMode,
            isDark = isDark,
            onClick = onSuperuserClick,
            modifier = Modifier.weight(0.66f)
        )

        StatCard(
            count = moduleCount.toString(),
            label = label2,
            backgroundMode = backgroundMode,
            isDark = isDark,
            onClick = onModuleClick,
            modifier = Modifier.weight(1.34f)
        )
    }
}

@Composable
fun StatCard(
    count: String,
    label: String,
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    val haptic = rememberHapticFeedback()

    val cardModifier = when (backgroundMode) {
        BackgroundMode.LIQUID_GLASS -> Modifier.liquidGlass(
            shape = cardShape,
            tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
            borderWidth = 0.dp,
            elevation = 4.dp,
            shimmer = true,
            isDark = isDark
        )
        BackgroundMode.FROSTED_BLUR -> Modifier.frostedGlass(
            shape = cardShape,
            tintColor = if (isDark) Color(0x301E2330) else Color(0x40FFFFFF),
            borderWidth = 0.dp,
            elevation = 4.dp
        )
        BackgroundMode.ACRYLIC -> Modifier.acrylicMaterial(
            shape = cardShape,
            tintColor = if (isDark) Color(0x401E293B) else Color(0x50F8FAFC)
        )
        BackgroundMode.MINIMAL_CLEAN -> Modifier
            .background(
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                shape = cardShape
            )
            .clip(cardShape)
        else -> Modifier.liquidGlass(
            shape = cardShape,
            tintColor = if (isDark) Color(0x351E293B) else Color(0x40FFFFFF),
            borderWidth = 0.dp,
            elevation = 4.dp,
            shimmer = true,
            isDark = isDark
        )
    }

    Box(
        modifier = modifier
            .height(100.dp)
            .then(cardModifier)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6))
            ) {
                haptic.perform(HapticType.CLICK)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = count,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
