package com.KozOnekey

import android.content.Context
import android.content.Intent

/** 调用安卓系统分享/导出面板 */
object ShareUtil {
    fun shareText(ctx: Context, subject: String, text: String) {
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(send, "导出日志").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(chooser)
        } catch (_: Throwable) {
        }
    }
}
