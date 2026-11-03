package com.KozOnekey.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.KozOnekey.effects.HapticType
import com.KozOnekey.effects.rememberDeviceTilt
import com.KozOnekey.effects.rememberHapticFeedback
import com.KozOnekey.theme.BackgroundMode
import kotlin.math.roundToInt

/**
 * 底部导航条目数据结构
 * @param title 标签名称
 * @param icon 矢量图标
 * @param badgeCount 角标未读数字
 */
data class NavItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * 苹果 iOS 18 风格 1:1 液态玻璃（Liquid Glass）交互浮动底栏
 *
 * 核心技术与物理光学特性：
 * 1. 【长按 / 拖拽连续平滑滑动切换 (Scrubbing)】：
 *    支持手指按住底栏横向滑动，滑动过程中实时传递连续浮点进度，上方页面与滑块药丸完全无缝实时跟随。
 * 2. 【真实力反馈 (Tactile Haptic Feedback)】：
 *    - 按下一瞬：触发确认触感 (CLICK / HEAVY_CLICK)
 *    - 拖动划过每个选项卡分界线时：触发物理齿轮般的清脆滴答 (TICK)
 *    - 释放吸附时：触发轻微吸附回弹震感
 * 3. 【真实渲染液态玻璃 (Real-rendered Liquid Glass)】：
 *    - 针对之前“过于透明看不清”进行彻底重构：采用高折射晶体基板，在浅色与深色下均具有清晰立体的轮廓与层次
 *    - 1.5dp 双层菲涅尔高折射渐变边框（顶沿纯白聚光弦线 -> 侧沿微透晶体 -> 底沿环境折射光）
 *    - 顶沿弧面液滴镜面弦线（Specular Meniscus Rim 3.5dp 高光带）
 *    - 左右两翼弧面圆角处呈现纳米级微色散晶体微光（Prismatic Dispersion Sheen）
 *    - 随手机物理倾角（陀螺仪传感器）流转的内部焦散光斑（Caustic Tilt Gleam）
 * 4. 【液态张力微形变 (Liquid Squash & Stretch)】：
 *    手指按压与滑动拖拽时，滑块药丸产生自然的果冻表面张力微拉伸。
 * 5. 【完全无遮罩】：底部保持悬浮纯净，背景与列表内容从下方自由透射。
 */
@Composable
fun LiquidGlassBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backgroundMode: BackgroundMode,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    selectedFraction: Float = selectedIndex.toFloat(),
    onScrubbing: ((Float) -> Unit)? = null,
    onScrubRelease: ((Int) -> Unit)? = null,
    backdrop: Backdrop? = null
) {
    // -------------------------------------------------------------------------
    // 🛠️【模板配置项：底部导航栏标签定义】
    // 在此处自由增加、删除或修改选项卡：
    // - title: 标签文本
    // - icon: 图标（支持任何 ImageVector）
    // - badgeCount: 未读徽标计数（0 则不显示）
    // *注意：若修改选项数量，请同步修改 MainActivity 中 Pager 的 pageCount。
    // -------------------------------------------------------------------------
    val items = remember {
        listOf(
            NavItem("主页", Icons.Rounded.Home),
            NavItem("提权", Icons.Rounded.Bolt),
            NavItem("日志", Icons.Rounded.ReceiptLong),
            NavItem("设置", Icons.Rounded.Settings)
        )
    }

    // 触觉马达管理器（硬件震动反馈）
    val haptic = rememberHapticFeedback()
    // 设备物理倾角传感器数据（用于卡片镜面光照流转）
    val tilt = rememberDeviceTilt()

    // 交互拖拽状态跟踪
    var isDragging by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var currentHoverIndex by remember { mutableIntStateOf(selectedIndex) }

    // 胶囊液态果冻微拉伸形变（手指拖拽受表面张力影响横向微拉伸、纵向微压缩）
    val liquidScaleX by animateFloatAsState(
        targetValue = if (isDragging) 1.06f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "liquidScaleX"
    )
    val liquidScaleY by animateFloatAsState(
        targetValue = if (isDragging) 0.94f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "liquidScaleY"
    )

    // 外形几何尺寸：标准 64dp 高度，圆润饱满
    val dockHeight = 64.dp
    val dockShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // 外层 Liquid Glass 真实晶体底盘
        var dockModifier = Modifier
            // 柔和环境漫反射投影，营造悬浮于壁纸之上的高档立体感
            .shadow(
                elevation = 16.dp,
                shape = dockShape,
                ambientColor = if (isDark) Color(0x40000000) else Color(0x20000000),
                spotColor = if (isDark) Color(0x60000000) else Color(0x301E293B)
            )

        // 真实光学透镜折射与模糊（Kyant0 AndroidLiquidGlass drawBackdrop）
        if (backdrop != null) {
            dockModifier = dockModifier.drawBackdrop(
                backdrop = backdrop,
                shape = { dockShape },
                effects = {
                    blur(24.dp.toPx())
                    lens(12.dp.toPx(), 12.dp.toPx())
                }
            )
        }

        dockModifier = dockModifier
            .clip(dockShape)
            // 真实液态玻璃高密度晶体基底：采用高密度磨砂晶体，既保留微透反光与透镜折射，又彻底防止底层高对比度开关/文字透出遮挡未展开图标
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xF61E293B), // 高密度深海岩蓝晶体（96%不透明度，彻底阻隔背景列表控件透射干扰）
                            Color(0xF0151E2E),
                            Color(0xEA0F172A)
                        )
                    } else {
                        listOf(
                            Color(0xF8FFFFFF), // 97%高透白玉晶体板，清爽通透无杂色
                            Color(0xF0F8FAFC),
                            Color(0xEAE2E8F0)
                        )
                    }
                )
            )

        BoxWithConstraints(
            modifier = dockModifier
                // 绘制高阶物理光学层（镜面顶弦反射、边缘色散、随角度移动的光斑）
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // 1. 顶沿物理弧面镜面反光（Specular Meniscus Rim: 3.5dp 纤细白光弦线）
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.22f else 0.95f),
                                Color.White.copy(alpha = if (isDark) 0.08f else 0.40f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 3.5f
                        ),
                        size = Size(w, 3.5f)
                    )

                    // 2. 左右两侧胶囊圆弧透镜色散微光（Chromatic Prismatic Sheen）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isDark) Color(0x2060A5FA) else Color(0x3538BDF8)),
                                Color.Transparent
                            ),
                            center = Offset(h * 0.45f, h * 0.5f),
                            radius = h * 0.75f
                        ),
                        radius = h * 0.75f,
                        center = Offset(h * 0.45f, h * 0.5f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isDark) Color(0x20A78BFA) else Color(0x30818CF8)),
                                Color.Transparent
                            ),
                            center = Offset(w - h * 0.45f, h * 0.5f),
                            radius = h * 0.75f
                        ),
                        radius = h * 0.75f,
                        center = Offset(w - h * 0.45f, h * 0.5f)
                    )

                    // 3. 随手机物理多角度倾角（左右、前后、对角）流转的内部高光焦散光斑
                    val gleamX = w * (0.5f + tilt.roll * 0.35f + tilt.diagonal1 * 0.15f)
                    val gleamY = h * (0.5f + tilt.pitch * 0.25f + tilt.diagonal2 * 0.15f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isDark) Color(0x2838BDF8) else Color.White.copy(alpha = 0.35f)),
                                Color.Transparent
                            ),
                            center = Offset(gleamX, gleamY),
                            radius = w * 0.35f
                        ),
                        radius = w * 0.35f,
                        center = Offset(gleamX, gleamY)
                    )
                }
                // 1.5dp 高折射双阶晶体边框（解决之前过于透明看不清轮廓的问题，极具玻璃棱镜雕刻质感）
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.98f),
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.50f),
                            if (isDark) Color(0x2038BDF8) else Color(0x252563EB)
                        )
                    ),
                    shape = dockShape
                )
                .height(dockHeight)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                // 触控交互处理：统一手势状态机，完美融合轻点触按、长按拖拽（Scrubbing）与全行程触觉反馈
                .pointerInput(items.size) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val sectionWidth = size.width / items.size.toFloat()
                        val startIndex = (down.position.x / sectionWidth).toInt().coerceIn(0, items.size - 1)
                        haptic.perform(HapticType.CLICK)

                        var isDraggingLocal = false
                        var currentHover = startIndex
                        currentHoverIndex = startIndex

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                if (isDraggingLocal) {
                                    isDragging = false
                                    haptic.perform(HapticType.TICK)
                                    onScrubRelease?.invoke(currentHover)
                                    onTabSelected(currentHover)
                                } else {
                                    // 确定轻触点按切换
                                    onTabSelected(startIndex)
                                }
                                break
                            } else if (change.pressed) {
                                val dx = kotlin.math.abs(change.position.x - down.position.x)
                                if (!isDraggingLocal && dx > viewConfiguration.touchSlop) {
                                    isDraggingLocal = true
                                    isDragging = true
                                    haptic.perform(HapticType.HEAVY_CLICK)
                                }

                                if (isDraggingLocal) {
                                    change.consume()
                                    touchX = change.position.x.coerceIn(0f, size.width.toFloat())
                                    val floatPage = ((touchX - sectionWidth * 0.5f) / sectionWidth).coerceIn(0f, (items.size - 1).toFloat())
                                    val hovered = (touchX / sectionWidth).toInt().coerceIn(0, items.size - 1)

                                    // 划过选项卡分界线时触发机械力反馈滴答
                                    if (hovered != currentHover) {
                                        currentHover = hovered
                                        currentHoverIndex = hovered
                                        haptic.perform(HapticType.TICK)
                                    }
                                    // 连续同步浮点坐标给页面容器
                                    onScrubbing?.invoke(floatPage)
                                }
                            }
                        }
                        isDragging = false
                    }
                }
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / items.size.toFloat()

            // 计算滑块横向位移：拖拽中紧随手指，松手后以自然弹簧插值跟随选中项
            val activeTabOffsetAnim by animateDpAsState(
                targetValue = tabWidth * selectedFraction,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "activeTabOffsetAnim"
            )

            val currentPillOffset = if (isDragging) {
                // 拖拽时滑块中心对齐手指位置
                ((touchX / (constraints.maxWidth.toFloat())) * totalWidth.value).dp - (tabWidth * 0.5f)
            } else {
                activeTabOffsetAnim
            }

            // 【苹果 iOS 18 签名级滑动晶体透镜药丸滑块（The Floating Glass Pill）】
            val pillShape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .offset(x = currentPillOffset.coerceIn(0.dp, totalWidth - tabWidth))
                    .width(tabWidth)
                    .height(52.dp)
                    .scale(scaleX = liquidScaleX, scaleY = liquidScaleY)
                    // 立体凸起投影，让滑块在底座上形成高亮浮动透镜效果
                    .shadow(
                        elevation = 6.dp,
                        shape = pillShape,
                        ambientColor = Color(0x18000000),
                        spotColor = if (isDark) Color(0x50000000) else Color(0x251E293B)
                    )
                    .clip(pillShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0x35FFFFFF), // 深色：冷月冰晶微透滑块，去除刺眼贴图感
                                    Color(0x18FFFFFF)
                                )
                            } else {
                                listOf(
                                    Color(0xFFFFFFFF), // 浅色：纯白无瑕立体凸面透镜
                                    Color(0xFFF8FAFC)
                                )
                            }
                        )
                    )
                    .drawBehind {
                        // 选态药丸顶部镜面微反光（深色模式下调柔）
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.30f else 0.98f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 3.0f
                            ),
                            size = Size(size.width, 3.0f)
                        )
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.28f else 0.98f),
                                if (isDark) Color(0x2038BDF8) else Color.White.copy(alpha = 0.40f)
                            )
                        ),
                        shape = pillShape
                    )
            )

            // 选项卡图标与文本排版层
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    // 只要当前进度接近该项，就高亮该项
                    val isHoveredOrSelected = if (isDragging) {
                        currentHoverIndex == index
                    } else {
                        selectedIndex == index
                    }

                    // iOS 经典电光蓝配色：选中有强烈对比度，未选中为高识别度灰蓝，保证任何环境下清爽易读
                    val contentColor by animateColorAsState(
                        targetValue = if (isHoveredOrSelected) {
                            if (isDark) Color(0xFFFFFFFF) else Color(0xFF007AFF)
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "contentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.height(30.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // 图标与角标容器：固定 32dp 宽 x 30dp 高，为右上角角标预留完整绘制空间，绝不被裁切且决不造成整行跳动
                            Box(
                                modifier = Modifier.size(width = 32.dp, height = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(21.dp)
                                )

                                if (item.badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 1.dp, y = 1.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(
                                                if (isHoveredOrSelected) {
                                                    if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
                                                } else {
                                                    if (isDark) Color(0xFF475569) else Color(0xFF64748B)
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                            .padding(horizontal = 4.dp, vertical = 0.5.dp)
                                    ) {
                                        Text(
                                            text = item.badgeCount.toString(),
                                            color = Color.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 9.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // 展开标签文字动画（高度严格锁定 30dp，纯横向展开收缩，配合无超调阻尼，彻底杜绝上下跳动）
                            AnimatedVisibility(
                                visible = isHoveredOrSelected,
                                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                        expandHorizontally(
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            ),
                                            expandFrom = Alignment.Start
                                        ),
                                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                       shrinkHorizontally(
                                           animationSpec = spring(
                                               stiffness = Spring.StiffnessMedium,
                                               dampingRatio = Spring.DampingRatioNoBouncy
                                           ),
                                           shrinkTowards = Alignment.Start
                                       )
                            ) {
                                Row(
                                    modifier = Modifier.height(30.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = item.title,
                                        color = contentColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp,
                                        maxLines = 1,
                                        softWrap = false
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
