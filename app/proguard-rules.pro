# ---- 保持跨进程/反射相关类（Stellar binder 协议） ----
-keep class roro.stellar.** { *; }
-keep class com.stellar.** { *; }
-keepclassmembers enum * { *; }
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }

# ---- 混淆/压缩（针对 UI 与业务代码） ----
-repackageclasses 'k'
-allowaccessmodification
-overloadaggressively
-renamesourcefileattribute k
-dontwarn **