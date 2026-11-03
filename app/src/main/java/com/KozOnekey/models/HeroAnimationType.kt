package com.KozOnekey.models

enum class HeroAnimationType(
    val title: String,
    val subtitle: String,
    val badgeName: String
) {
    CLASSIC_RADAR(
        title = "经典同心雷达",
        subtitle = "轨道卫星与平滑脉冲",
        badgeName = "经典雷达"
    ),
    FLUID_CORE(
        title = "液态能量晶核",
        subtitle = "多相水波与流体光核",
        badgeName = "流体晶核"
    ),
    AURORA_WAVES(
        title = "极光律动流线",
        subtitle = "半透明正弦丝滑流动",
        badgeName = "极光流线"
    ),
    CORONA_ECLIPSE(
        title = "日冕偏心光晕",
        subtitle = "色球层曜斑与微澜辉光",
        badgeName = "日冕光晕"
    );

    fun next(): HeroAnimationType {
        val all = entries
        val nextIdx = (ordinal + 1) % all.size
        return all[nextIdx]
    }
}
