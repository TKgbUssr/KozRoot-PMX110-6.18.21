package com.KozOnekey

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** 校验安装包签名，防止被重新打包 / 二次签名后使用 */
object SignatureCheck {
    // 正式发布证书 SHA-256（大小写不敏感，去掉冒号）
    private const val EXPECTED = "F6EF8BA23DD8FFCEB5B435A6E29A1003C4B08482DBA8D61469D813F71AE58ED8"

    fun verify(ctx: Context): Boolean {
        return try {
            val pm = ctx.packageManager
            val sigs: Array<android.content.pm.Signature>? = if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures
            }
            sigs?.any { sig ->
                val md = MessageDigest.getInstance("SHA-256")
                md.digest(sig.toByteArray()).joinToString("") { "%02X".format(it) } == EXPECTED
            } == true
        } catch (_: Throwable) {
            false
        }
    }
}
