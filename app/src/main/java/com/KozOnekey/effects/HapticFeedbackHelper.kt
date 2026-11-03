package com.KozOnekey.effects

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity

/**
 * 触觉反馈类型定义
 */
enum class HapticType {
    TICK,       // 齿轮微滴答（左右滑动划过选项卡分界线、撞击边界回弹时触发）
    CLICK,      // 明确按下确认反馈（按钮、选项卡轻点、卡片点击、开关切换）
    HEAVY_CLICK // 沉浸重反馈（长按激活液态拖拽、删除确认）
}

/**
 * 封装 Android 触觉马达振动管理器
 * 优先调用线性马达系统级预置震感（VibrationEffect.EFFECT_TICK / EFFECT_CLICK），
 * 兼具 View 级触觉反馈回退，提供媲美 iOS Taptic Engine 的清脆力反馈。
 */
class HapticFeedbackHelper(
    private val context: Context,
    private val view: View
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * 触发指定强度的物理力反馈
     */
    fun perform(type: HapticType) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                val effect = when (type) {
                    HapticType.TICK -> {
                        // 细微轻脆滴答（用于划过 Dock 标签、滚动撞击顶/底边缘）
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        } else {
                            VibrationEffect.createOneShot(10, 40)
                        }
                    }
                    HapticType.CLICK -> {
                        // 标准轻触反馈（开关、按钮、卡片轻点）
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        } else {
                            VibrationEffect.createOneShot(20, 80)
                        }
                    }
                    HapticType.HEAVY_CLICK -> {
                        // 饱满力反馈（用于长按捕获 Dock、危险删除操作）
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                        } else {
                            VibrationEffect.createOneShot(35, 160)
                        }
                    }
                }
                vibrator.vibrate(effect)
            } else {
                // 降级使用 View 级触觉
                when (type) {
                    HapticType.TICK -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    HapticType.CLICK -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    HapticType.HEAVY_CLICK -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
        } catch (_: Exception) {
            // 容错降级
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}

/**
 * 记忆获取触觉马达管理器实例
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        HapticFeedbackHelper(context, view)
    }
}

/**
 * 边界撞击触觉反馈修饰符 (Overscroll / Boundary Hit Haptics)
 * 监听界面滚动，当用户向上滑动撞击到底部、或向下拉动撞击到顶部边缘时，
 * 触发真实的物理碰撞力反馈，模拟撞击到弹力边界的触感。
 */
@Composable
fun Modifier.overscrollHapticFeedback(haptic: HapticFeedbackHelper = rememberHapticFeedback()): Modifier {
    var hitBoundary by remember { mutableStateOf(false) }

    val connection = remember(haptic) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 当用户手指在屏幕上拖拽，且内容已到达边缘无法继续滚动（available.y != 0 撞击边界）
                if (source == NestedScrollSource.UserInput) {
                    val isHitting = kotlin.math.abs(available.y) > 6f
                    if (isHitting) {
                        if (!hitBoundary) {
                            haptic.perform(HapticType.TICK)
                            hitBoundary = true
                        }
                    } else {
                        hitBoundary = false
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                hitBoundary = false
                // 当快速滑动抛掷 (Fling) 撞击边缘时，available.y 不为 0，触发清脆撞击震感
                if (kotlin.math.abs(available.y) > 80f) {
                    haptic.perform(HapticType.TICK)
                }
                return Velocity.Zero
            }
        }
    }
    return this.nestedScroll(connection)
}
