package com.KozOnekey.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃（Liquid Glass）视觉修饰符
 *
 * 核心渲染特性：
 * 1. 深度通透的磨砂微透层（无边框，极简通透）
 * 2. 依据物理传感器姿态（Pitch & Roll）实时驱动光带反射与弦线折射（告别重复循环突兀动画）
 * 3. 晶体弧面弦线高光（Specular Meniscus Rim）模拟光线折射
 * 4. 浮雕级柔和微漫射阴影
 *
 * @param shape 卡片圆角几何外形
 * @param tintColor 玻璃本体底色通透度
 * @param borderColor 可选边缘描边色（默认透明无边）
 * @param borderWidth 边框粗细（默认为 0.dp，实现完全无边的通透效果）
 * @param elevation 投影高度
 * @param blurRadius 硬件模糊半径
 * @param shimmer 是否启用随设备物理倾角流转的高光
 * @param isDark 是否处于深色模式（深色模式下严格调暗反光强度，杜绝塑料贴图感）
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(26.dp),
    tintColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 0.dp,
    elevation: Dp = 4.dp,
    blurRadius: Float = 0f,
    shimmer: Boolean = true,
    isDark: Boolean = false
): Modifier {
    // 读取手机多轴向 3D 空间倾角（左右、前后、对角线、空间极角）
    val tilt = rememberDeviceTilt()

    val actualTintColor = tintColor ?: if (isDark) Color(0x281E293B) else Color(0x35FFFFFF)
    val actualBorderColor = borderColor ?: if (isDark) Color(0x3064748B) else Color(0x60FFFFFF)

    var mod = this
        // 1. 底层环境遮蔽柔和投影
        // 彻底根除中心方框伪影：将 spotColor 置为 Transparent，完全消除点光源本影(Umbra)多边形投射，
        // 仅保留完全沿圆角周向外扩的环境泛光阴影 (Ambient Shadow)，既恢复浓郁毛玻璃立体层次，又绝无内部矩形伪影！
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isDark) Color(0x38000000) else Color(0x15000000),
            spotColor = Color.Transparent
        )
        // 2. 视口裁切至卡片几何形状
        .clip(shape)
        // 3. 通透磨砂微透基底
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    actualTintColor,
                    actualTintColor.copy(alpha = (actualTintColor.alpha * 0.90f).coerceAtMost(1f))
                )
            ),
            shape = shape
        )
        // 4. 多角度物理晶体光学折射层（支持 360° 全方位自转、对角倾斜联动）
        .drawBehind {
            if (shimmer) {
                val w = size.width
                val h = size.height

                // 结合左右(roll)、俯仰(pitch)与对角线轴向(diagonal)解算出光斑焦点坐标
                val lightX = w * (0.5f + tilt.roll * 0.38f + tilt.diagonal1 * 0.12f)
                val lightY = h * (0.5f + tilt.pitch * 0.38f + tilt.diagonal2 * 0.12f)

                // 物理光学平滑反光扫带：
                // 保持优雅的对角主反光倾角（约 -38°），随左右与俯仰产生平滑的连续倾斜响应，
                // 彻底杜绝使用 atan2 造成的象限跳变、奇异点与 90 度倒下旋转
                val skew = (tilt.roll * 0.22f - tilt.pitch * 0.15f)
                val dirX = 0.85f
                val dirY = 0.52f + skew * 0.20f
                val bandSpan = (w + h) * 0.55f

                // 反光适度调柔微透：相比之前进一步下调透明度，呈现极度通透、晶莹润泽但毫无贴图感的微光
                val mainAlpha = if (isDark) 0.055f else 0.13f
                val causticAlpha = if (isDark) 0.036f else 0.085f
                val subAlpha = if (isDark) 0.022f else 0.05f

                // 主光束高光扫带（随手机物理多角度倾角与空间旋转自由流转，全角度连续丝滑）
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            (if (isDark) Color(0x0E38BDF8) else Color.White.copy(alpha = mainAlpha * 0.6f)),
                            (if (isDark) Color(0x1860A5FA) else Color.White.copy(alpha = mainAlpha)),
                            (if (isDark) Color(0x0C38BDF8) else Color.White.copy(alpha = mainAlpha * 0.4f)),
                            Color.Transparent
                        ),
                        start = Offset(lightX - dirX * bandSpan, lightY - dirY * bandSpan),
                        end = Offset(lightX + dirX * bandSpan, lightY + dirY * bandSpan)
                    ),
                    size = size
                )

                // 副轴反向视差焦散光斑（营造多角度折射下的晶体立体进深感）
                val subX = w * (0.5f - tilt.diagonal2 * 0.20f)
                val subY = h * (0.5f - tilt.diagonal1 * 0.20f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDark) Color(0x1038BDF8) else Color.White.copy(alpha = subAlpha)),
                            Color.Transparent
                        ),
                        center = Offset(subX, subY),
                        radius = (w * 0.45f).coerceAtLeast(80f)
                    ),
                    radius = (w * 0.45f).coerceAtLeast(80f),
                    center = Offset(subX, subY)
                )

                // 晶体弧面菲涅尔聚光焦点（Radial Caustic Glow）
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isDark) Color(0x1638BDF8) else Color.White.copy(alpha = causticAlpha)),
                            Color.Transparent
                        ),
                        center = Offset(lightX, lightY),
                        radius = (w * 0.40f).coerceAtLeast(80f)
                    ),
                    radius = (w * 0.40f).coerceAtLeast(80f),
                    center = Offset(lightX, lightY)
                )
            }

            // 5. 顶沿极细镜面反射弦线（深色模式下严格调柔，消除过亮刺眼贴图感）
            val topAlpha = if (isDark) {
                (0.06f + tilt.pitch * 0.02f).coerceIn(0.04f, 0.09f)
            } else {
                (0.22f + tilt.pitch * 0.07f).coerceIn(0.12f, 0.32f)
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isDark) Color(0x2038BDF8) else Color.White.copy(alpha = topAlpha)),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 3.0f
                ),
                size = Size(size.width, 3.0f)
            )
        }

    // 仅在明确指定描边粗细大于 0 时才绘制边框，保证默认纯净无边
    if (borderWidth > 0.dp) {
        mod = mod.border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    actualBorderColor,
                    actualBorderColor.copy(alpha = (actualBorderColor.alpha * 0.5f).coerceAtLeast(0.1f)),
                    actualBorderColor.copy(alpha = 0.15f)
                )
            ),
            shape = shape
        )
    }

    return mod
}

/**
 * 经典高斯磨砂玻璃修饰符
 */
@Composable
fun Modifier.frostedGlass(
    shape: Shape = RoundedCornerShape(26.dp),
    tintColor: Color = Color(0x35FFFFFF),
    borderColor: Color = Color(0x60FFFFFF),
    borderWidth: Dp = 0.dp,
    elevation: Dp = 4.dp,
    blurRadius: Float = 0f
): Modifier {
    var mod = this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x16000000),
            spotColor = Color.Transparent
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    tintColor,
                    tintColor.copy(alpha = (tintColor.alpha * 0.94f).coerceAtMost(1f))
                )
            ),
            shape = shape
        )

    if (borderWidth > 0.dp) {
        mod = mod.border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.2f)
                )
            ),
            shape = shape
        )
    }

    return mod
}

/**
 * 亚克力 (Fluent Material) 材质修饰符
 */
@Composable
fun Modifier.acrylicMaterial(
    shape: Shape = RoundedCornerShape(26.dp),
    tintColor: Color = Color(0xF0F8FAFC),
    borderColor: Color = Color(0x40FFFFFF),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x22000000),
            spotColor = Color.Transparent
        )
        .clip(shape)
        .background(tintColor)
        .border(borderWidth, borderColor, shape)
}
