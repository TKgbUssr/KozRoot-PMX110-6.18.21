package com.KozOnekey.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.effects.acrylicMaterial
import com.KozOnekey.effects.frostedGlass
import com.KozOnekey.effects.liquidGlass
import com.KozOnekey.theme.BackgroundMode

data class SpecItem(
    val title: String,
    val value: String
)

@Composable
fun SystemSpecCard(
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    specs: List<SpecItem> = emptyList(),
    onCopy: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)

    val deviceModel = Build.MODEL.ifBlank { "PMX110" }

    val specs = specs.ifEmpty {
        listOf(
            SpecItem("设备型号", deviceModel),
            SpecItem("内核版本", System.getProperty("os.version") ?: "-"),
            SpecItem("SELinux 状态", "未知")
        )
    }

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
            .fillMaxWidth()
            .then(cardModifier)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column {
            specs.forEachIndexed { index, spec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCopy("已复制: " + spec.value)
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = spec.title,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Text(
                        text = spec.value,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = if (spec.value.contains(".") || spec.value.contains("-")) FontFamily.Monospace else FontFamily.SansSerif,
                        textAlign = TextAlign.End,
                        lineHeight = 18.sp
                    )
                }

                if (index < specs.lastIndex) {
                    HorizontalDivider(
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000),
                        thickness = 0.8.dp
                    )
                }
            }
        }
    }
}
