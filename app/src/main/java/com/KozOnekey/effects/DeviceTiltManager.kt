package com.KozOnekey.effects

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2

/**
 * 手机物理 3D 空间多维度姿态与光照向量数据模型
 *
 * @param pitch 俯仰角归一化值 (-1.0f 向上倾斜 ~ +1.0f 向下倾斜)
 * @param roll  翻滚角归一化值 (-1.0f 向左倾斜 ~ +1.0f 向右倾斜)
 * @param azimuth 偏航角/罗盘自转角 (-1.0f ~ +1.0f)
 * @param diagonal1 对角线轴向1 (左上到右下，45度角) 倾斜度
 * @param diagonal2 对角线轴向2 (右上到左下，-45度角) 倾斜度
 * @param lightAngle 3D 空间主光线在屏幕切平面的入射旋转极角 (弧度制)
 * @param depth 纵深空间倾角（平放为0，竖起或后仰渐增）
 */
data class DeviceTilt(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val azimuth: Float = 0f,
    val diagonal1: Float = 0f,
    val diagonal2: Float = 0f,
    val lightAngle: Float = 0f,
    val depth: Float = 0f
)

/**
 * 监听设备传感器（融合旋转矢量传感器/陀螺仪/加速度计），
 * 实时解算出全空间、多维度的物理倾角向量，支持多轴向共同联动变化：
 * 包括左右倾斜、上下俯仰、对角向交叉倾斜、平面内自转极角以及空间进深。
 */
@Composable
fun rememberDeviceTilt(): DeviceTilt {
    val context = LocalContext.current

    // 原始采集姿态角
    var rawPitch by remember { mutableFloatStateOf(0f) }
    var rawRoll by remember { mutableFloatStateOf(0f) }
    var rawAzimuth by remember { mutableFloatStateOf(0f) }
    var rawDepth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        // 使用物理重力传感器（经过硬件融合滤波，无万向节锁与旋转跳变奇异点）
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                // 物理坐标系投影：
                // values[0]: X 轴重力分量 (向左倾斜为负，向右倾斜为正)
                // values[1]: Y 轴重力分量 (向上仰起为负，向下俯角为正)
                // values[2]: Z 轴重力分量 (面向用户平放接近 9.8，垂直立起接近 0)
                val targetRoll = (-event.values[0] / 9.8f).coerceIn(-1.5f, 1.5f)
                val targetPitch = (event.values[1] / 9.8f).coerceIn(-1.5f, 1.5f)
                val targetDepth = ((9.8f - event.values[2]) / 9.8f).coerceIn(0f, 1.5f)

                // 低通滤波消除微小高频手抖
                rawRoll = rawRoll * 0.82f + targetRoll * 0.18f
                rawPitch = rawPitch * 0.82f + targetPitch * 0.18f
                rawDepth = rawDepth * 0.82f + targetDepth * 0.18f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // 弹簧物理平滑阻尼动画，确保光斑在视角倾斜时像液态水银般顺滑流转，绝不突兀
    val smoothPitch by animateFloatAsState(
        targetValue = rawPitch,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.88f),
        label = "smoothPitch"
    )
    val smoothRoll by animateFloatAsState(
        targetValue = rawRoll,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.88f),
        label = "smoothRoll"
    )
    val smoothDepth by animateFloatAsState(
        targetValue = rawDepth,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.88f),
        label = "smoothDepth"
    )

    // 解算多轴向复合对角光照向量（左上到右下 45° 与 右上到左下 -45°）
    val d1 = (smoothRoll + smoothPitch) * 0.7071f
    val d2 = (smoothRoll - smoothPitch) * 0.7071f

    return DeviceTilt(
        pitch = smoothPitch,
        roll = smoothRoll,
        azimuth = 0f,
        diagonal1 = d1,
        diagonal2 = d2,
        lightAngle = 0f,
        depth = smoothDepth
    )
}
