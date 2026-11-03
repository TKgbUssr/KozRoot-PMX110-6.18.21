package com.KozOnekey.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 顶部状态栏渐弱高斯磨砂模糊 (Separable Progressive Gaussian Blur)
 *
 * 为什么不用稀疏泊松盘采样：
 * 32 个固定偏移点覆盖 ~60px 半径时，文字的每条锐利边缘会被复制成 32 个离散“分身”，
 * 视觉上就是“近视重影 / 看着晕”。
 *
 * 现方案（与 chrisbanes/haze 同源思路）：
 * - 拆分为 水平 + 垂直 两个一维高斯 pass，用 RenderEffect.createChainEffect 串联；
 * - 每个 pass 在 3σ 范围内逐像素连续采样（线性采样优化：一次 eval 合并相邻两像素权重），
 *   得到真正连续的高斯漫射 —— 无离散分身、无重影，呈纯正磨砂质感；
 * - 每个像素的模糊半径由其 y 坐标决定：顶部最强，向下 smoothstep 平滑衰减至 0，无可见边界；
 * - 越界采样直接丢弃并重新归一化权重，顶部/左右边缘不会出现变暗、发白或发脏。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun createProgressiveTopBlurEffect(
    fadeHeightPx: Float,
    maxRadiusPx: Float,
    widthPx: Float
): RenderEffect {
    fun pass(vertical: Boolean): RenderEffect {
        val shader = RuntimeShader(makeBlurAgsl(vertical)).apply {
            setFloatUniform("fadeHeight", fadeHeightPx)
            setFloatUniform("maxRadius", maxRadiusPx)
            setFloatUniform("boundsWidth", widthPx)
        }
        return RenderEffect.createRuntimeShaderEffect(shader, "content")
    }
    // 先水平 (inner)，再垂直 (outer)
    return RenderEffect.createChainEffect(pass(vertical = true), pass(vertical = false))
}

private fun makeBlurAgsl(vertical: Boolean): String {
    val dir = if (vertical) "float2(0.0, 1.0)" else "float2(1.0, 0.0)"
    return """
        uniform shader content;
        uniform float fadeHeight;
        uniform float maxRadius;
        uniform float boundsWidth;

        float gaussian(float x, float sigma) {
            return exp(-(x * x) / (2.0 * sigma * sigma));
        }

        half4 main(float2 coord) {
            if (coord.y >= fadeHeight) {
                return content.eval(coord);
            }

            // 顶部最强，向下 smoothstep 平滑收敛到 0
            float t = clamp(1.0 - coord.y / fadeHeight, 0.0, 1.0);
            float progress = t * t * (3.0 - 2.0 * t);
            float radius = maxRadius * progress;
            if (radius < 1.0) {
                return content.eval(coord);
            }

            float sigma = max(radius * 0.5, 0.8);
            float extent = sigma * 3.0;
            float2 dir = $dir;

            float4 result = float4(content.eval(coord));
            float weightSum = 1.0;

            // 常量上限 + break（AGSL 要求循环边界为常量）
            for (float i = 1.0; i < 240.0; i += 2.0) {
                if (i >= extent) { break; }

                // 线性采样：一次 eval 合并 i 与 i+1 两个像素的高斯权重
                float wL = gaussian(i, sigma);
                float wH = gaussian(i + 1.0, sigma);
                float w = wL + wH;
                float2 d = dir * (i + wH / w);

                float2 c1 = coord - d;
                if (c1.x >= 0.0 && c1.y >= 0.0) {
                    result += w * float4(content.eval(c1));
                    weightSum += w;
                }
                float2 c2 = coord + d;
                if (c2.x <= boundsWidth) {
                    result += w * float4(content.eval(c2));
                    weightSum += w;
                }
            }

            return half4(result / weightSum);
        }
    """.trimIndent()
}
