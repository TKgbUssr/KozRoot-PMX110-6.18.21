package com.KozOnekey

import android.content.Context

/** Persistent app preferences (theme / background / hero animation / wallpaper). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("kozroot_prefs", Context.MODE_PRIVATE)

    /** 0 = 跟随系统 / 1 = 浅色 / 2 = 深色（默认） */
    var themeMode: Int
        get() {
            if (sp.contains("theme_mode")) return sp.getInt("theme_mode", 2)
            // 兼容旧版本保存的 dark 布尔值
            if (sp.contains("dark")) return if (sp.getBoolean("dark", true)) 2 else 1
            return 2
        }
        set(v) = sp.edit().putInt("theme_mode", v).apply()

    var backgroundMode: String
        get() = sp.getString("background_mode", "LIQUID_GLASS") ?: "LIQUID_GLASS"
        set(v) = sp.edit().putString("background_mode", v).apply()

    var heroAnimation: String
        get() = sp.getString("hero_animation_v2", "CLASSIC_RADAR") ?: "CLASSIC_RADAR"
        set(v) = sp.edit().putString("hero_animation_v2", v).apply()

    var heroAlpha: Float
        get() = sp.getFloat("hero_alpha", 0.75f)
        set(v) = sp.edit().putFloat("hero_alpha", v).apply()

    var heroCardHeight: Float
        get() = sp.getFloat("hero_card_height", 150f)
        set(v) = sp.edit().putFloat("hero_card_height", v).apply()

    /** 选中的 Root 管理器（RootManager.id，0=KernelSU-Next） */
    var rootManager: Int
        get() = sp.getInt("root_manager", 0)
        set(v) = sp.edit().putInt("root_manager", v).apply()

    /** 上次提权成功使用的管理器（-1 = 无记录） */
    var lastManager: Int
        get() = sp.getInt("last_manager", -1)
        set(v) = sp.edit().putInt("last_manager", v).apply()

    /** 上次提权成功实际注册/使用的管理器包名（SukiSU 有伪装版/正常版之分） */
    var lastManagerPkg: String?
        get() = sp.getString("last_manager_pkg", null)
        set(v) = sp.edit().putString("last_manager_pkg", v).apply()

    var predictiveBack: Boolean
        get() = sp.getBoolean("predictive_back", true)
        set(v) = sp.edit().putBoolean("predictive_back", v).apply()

    var lastSuccessAt: Long
        get() = sp.getLong("last_success_at", 0L)
        set(v) = sp.edit().putLong("last_success_at", v).apply()

    var lastSuccessBootWall: Long
        get() = sp.getLong("last_success_boot_wall", 0L)
        set(v) = sp.edit().putLong("last_success_boot_wall", v).apply()

    var wallpaperUri: String?
        get() = sp.getString("wallpaper_uri", null)
        set(v) = sp.edit().putString("wallpaper_uri", v).apply()

    var wallpaperPreset: String?
        get() = sp.getString("wallpaper_preset", "nebula")
        set(v) = sp.edit().putString("wallpaper_preset", v).apply()

    var wallpaperBlur: Float
        get() = sp.getFloat("wallpaper_blur", 80f)
        set(v) = sp.edit().putFloat("wallpaper_blur", v).apply()

    var wallpaperDim: Float
        get() = sp.getFloat("wallpaper_dim", 0.20f)
        set(v) = sp.edit().putFloat("wallpaper_dim", v).apply()

    var wallpaperScale: String
        get() = sp.getString("wallpaper_scale", "crop") ?: "crop"
        set(v) = sp.edit().putString("wallpaper_scale", v).apply()
}
