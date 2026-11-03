package com.KozOnekey

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Compose-facing state holder for the root engine + device information.
 * All updates happen on the main thread (RootEngine posts its callbacks there).
 */
class RootState(context: Context) {
    private val ctx = context.applicationContext
    private val ui = Handler(Looper.getMainLooper())

    var statusText by mutableStateOf("检测中 ...")
        private set
    var statusLevel by mutableIntStateOf(0)   // 0 idle / 1 running / 2 ok / 3 fail
        private set
    var statusHint by mutableStateOf("正在检测 Stellar 服务 ...")
        private set
    var running by mutableStateOf(false)
        private set
    var logText by mutableStateOf("")
        private set
    var lastOk by mutableStateOf<Boolean?>(null)
        private set
    var lastSeconds by mutableLongStateOf(0L)
        private set
    var recordsVersion by mutableIntStateOf(0)
        private set
    /** 内核残留提示弹窗 */
    var residueDialogVisible by mutableStateOf(false)
        private set
    var residueTimerCount by mutableIntStateOf(0)
        private set
    var residueExploitCount by mutableIntStateOf(0)
        private set

    val stageStates = mutableStateListOf(0, 0, 0, 0, 0, 0)
    val stageExtras = mutableStateListOf<String?>(null, null, null, null, null, null)

    var devModel by mutableStateOf("读取中 ...")
    var devKernel by mutableStateOf("读取中 ...")
    var devSystem by mutableStateOf("读取中 ...")
    var devSelinux by mutableStateOf("读取中 ...")
    var devFingerprint by mutableStateOf("")
    var stellarConnected by mutableStateOf<Boolean?>(null)
    var stellarInstalled by mutableStateOf<Boolean?>(null)
    var managerPresent by mutableStateOf<Boolean?>(null)
    /** 提权成功后按钮显示“提权成功 · 点击打开管理器”（点过之后恢复“重新检测 / 修复”） */
    var postRootOpenPrompt by mutableStateOf(false)
        private set
    /** 当前选中的管理器（下次提权使用） */
    var selectedManager by mutableStateOf(RootManager.selected(ctx))
        private set
    /** 当前链路（仅保留 64560 优化链路） */
    val selectedChain = RootChain.CVE_64560_NEW
    /** 当前实际生效的管理器（最近一次成功提权 / 已加载模块） */
    var activeManager by mutableStateOf<RootManager?>(null)
        private set

    /** 当前管理器显示名（未提权时显示选中的） */
    val managerName: String get() = (activeManager ?: selectedManager).displayName

    /** 选择管理器（运行中禁止切换） */
    fun selectManager(m: RootManager) {
        if (running) return
        selectedManager = m
        RootManager.setSelected(ctx, m)
        managerPresent = RootManager.installed(ctx, activeManager ?: m)
        if (rooted && activeManager != null && activeManager != m) {
            uiMessage = "当前由 " + activeManager!!.displayName + " 提权；切换管理器需重启后重新提权"
        }
    }

    /** 残留提示弹窗：继续执行 */
    fun residueContinue() {
        residueDialogVisible = false
        engine.decideResidue(RootEngine.RESIDUE_CONTINUE)
    }

    /** 残留提示弹窗：取消执行 */
    fun residueCancel() {
        residueDialogVisible = false
        engine.decideResidue(RootEngine.RESIDUE_CANCEL)
    }

    /** 残留提示弹窗：重启手机（经 Stellar shell 执行） */
    fun residueReboot() {
        residueDialogVisible = false
        engine.decideResidue(RootEngine.RESIDUE_REBOOT)
    }

    /** 打开当前生效（或选中）的管理器 */
    fun openManager() {
        postRootOpenPrompt = false
        val target = activeManager ?: selectedManager
        // 已提权过：优先打开本次实际注册的管理器包（SukiSU 伪装版/正常版可能同时存在）
        val savedPkg = Prefs(ctx).lastManagerPkg
        val pkg = if (activeManager != null && savedPkg != null &&
            RootManager.isPackageInstalled(ctx, savedPkg)
        ) {
            savedPkg
        } else {
            RootManager.installedPackage(ctx, target)
        }
        if (pkg == null) {
            uiMessage = "请先安装 " + target.displayName + " 管理器"
            return
        }
        try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } else {
                uiMessage = target.displayName + " 管理器无法打开"
            }
        } catch (_: Throwable) {
            uiMessage = target.displayName + " 管理器无法打开"
        }
    }
    var rooted by mutableStateOf(false)
        private set
    var selinuxPermissiveAfterRoot by mutableStateOf(false)
    var uiMessage by mutableStateOf<String?>(null)
        private set

    fun clearUiMessage() {
        uiMessage = null
    }

    fun refreshRecords() {
        recordsVersion++
    }

    val stepsDone: Int get() = (0 until selectedChain.stageCount()).count { stageStates[it] == 2 }
    val recordCount: Int get() = RunStore.list(ctx).size

    private var logFile: java.io.File? = null

    init {
        // 把上次 panic / 重启时没来得及保存的运行日志恢复成失败记录
        if (RunStore.recoverInterrupted(ctx) > 0) {
            recordsVersion = 1
        }
    }

    private val engine = RootEngine(ctx, object : RootEngine.Listener {
        override fun onLog(line: String) {
            logText += line
            RunStore.appendLog(logFile, line)
        }

        override fun onStage(idx: Int, state: Int, extra: String?) {
            if (idx in 0..5) {
                stageStates[idx] = state
                stageExtras[idx] = extra
            }
        }

        override fun onStagesReset() {
            for (i in 0..5) {
                stageStates[i] = 0
                stageExtras[i] = null
            }
        }

        override fun onStatus(state: Int, text: String) {
            statusLevel = state
            statusText = when (state) {
                2 -> "提权成功"
                3 -> when {
                    text.contains("Stellar") -> "未连接"
                    text.contains("内核") -> "内核不符"
                    text.contains("设备") -> "设备不符"
                    text.contains("payload") -> "文件失败"
                    else -> "失败"
                }
                1 -> "运行中"
                else -> text
            }
            statusHint = when (state) {
                2 -> "重启失效"
                3 -> "看日志"
                1 -> "执行中"
                else -> "等待"
            }
        }

        override fun onState(r: Boolean) {
            running = r
        }

        override fun onResidueWarning(timerActive: Int, exploitCount: Int) {
            residueTimerCount = timerActive
            residueExploitCount = exploitCount
            residueDialogVisible = true
        }

        override fun onFinished(ok: Boolean, log: String, seconds: Long) {
            residueDialogVisible = false
            lastOk = ok
            lastSeconds = seconds
            logText = log
            statusLevel = if (ok) 2 else 3
            statusText = if (ok) "提权成功" else "失败"
            statusHint = if (ok) "重启失效" else "看日志"
            if (ok) {
                rooted = true
                postRootOpenPrompt = true
                activeManager = startingManager
                val usedPkg = RootManager.installedPackage(ctx, startingManager)
                    ?: startingManager.bearerPackage
                Prefs(ctx).lastManagerPkg = usedPkg
                RootManager.setLast(ctx, startingManager)
                managerPresent = RootManager.installed(ctx, startingManager)
                val now = System.currentTimeMillis()
                val prefs = Prefs(ctx)
                prefs.lastSuccessAt = now
                prefs.lastSuccessBootWall = now - android.os.SystemClock.elapsedRealtime()
            }
            RunStore.save(ctx, ok, seconds, log)
            try {
                logFile?.delete()
            } catch (_: Throwable) {
            }
            logFile = null
            recordsVersion++
            if (ok) {
                uiMessage = "提权成功"
                Thread {
                    val se = try {
                        Shell.exec("getenforce").trim()
                    } catch (_: Throwable) {
                        ""
                    }
                    if (se.contains("Permissive")) {
                        ui.post {
                            selinuxPermissiveAfterRoot = true
                            uiMessage = "若 SELinux 为宽容，请手动恢复"
                        }
                    }
                }.start()
            } else {
                uiMessage = "提权失败"
            }
        }
    })

    private var startingManager: RootManager = RootManager.KERNELSU_NEXT

    fun start() {        if (running) return
        // 已经有可用 root：不再重跑 exploit，只清理残留 + 检测状态 / 修复管理器
        if (rooted) {
            val act = activeManager
            if (act != null && act != selectedManager) {
                uiMessage = "当前由 " + act.displayName + " 提权；切换管理器需重启后重新提权"
            }
            recheckAndRepair()
            return
        }
        if (!Shell.ready()) {
            if (stellarInstalled == false) {
                statusLevel = 3
                statusText = "未安装"
                statusHint = "请先安装"
                uiMessage = "请先安装 Stellar"
                return
            }
            statusLevel = 3
            statusText = "未连接"
            statusHint = "去开 Stellar"
            logText += "[" + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date()) + "] ✗ Stellar 未连接，正在打开 Stellar ...\n"
            logText += "请在 Stellar 里用「无线调试 / Root 启动」把服务启动起来，并授权 KozRoot；\n"
            logText += "启动服务后回到本应用，状态会自动刷新，再点一次「开始提权」。\n"
            try {
                val intent = ctx.packageManager.getLaunchIntentForPackage("roro.stellar.manager")
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                }
            } catch (_: Throwable) {
            }
            return
        }
        lastOk = null
        logText = ""
        rooted = false
        postRootOpenPrompt = false
        selinuxPermissiveAfterRoot = false
        logFile = RunStore.newInProgress(ctx, System.currentTimeMillis())
        startingManager = selectedManager
        engineLogCleanup()
        for (i in 0..5) {
            stageStates[i] = 0
            stageExtras[i] = null
        }
        engine.start(selectedManager)
    }

    private fun engineLogCleanup() {
        // engine clears its own buffer on start; compose state is reset here
        logText = ""
    }

    private fun appendLogLine(line: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val l = "[" + ts + "] " + line + "\n"
        logText += l
        RunStore.appendLog(logFile, l)
    }

    private fun copyAssetToWork(name: String): Boolean {
        return try {
            ctx.assets.open(name).use { input ->
                Shell.writeFile("/data/local/tmp/koznroot/" + name, input)
            }
        } catch (_: Throwable) {
            false
        }
    }

    /** 已有 root：清理残留、检测真实现状；若管理器被卸载则自动安装 */
    private fun recheckAndRepair() {
        if (!Shell.ready()) {
            uiMessage = "Stellar 未连接"
            return
        }
        running = true
        statusLevel = 1
        statusText = "检测中"
        statusHint = "清理并检查"
        logText = ""
        logFile = RunStore.newInProgress(ctx, System.currentTimeMillis())
        Thread {
            try {
                appendLogLine("---- 清理残留进程 ----")
                Shell.exec("mkdir -p /data/local/tmp/koznroot; touch /data/local/tmp/koznroot/rkw_stop 2>/dev/null")
                Thread.sleep(600L)
                Shell.exec("pkill -9 root_worker2 2>/dev/null; pkill -9 RKW 2>/dev/null; kill -STOP \$(pidof exploit_direct) 2>/dev/null")
                Shell.exec("su -c 'pkill -9 -f root_worker2; kill -STOP \$(pidof exploit_direct) 2>/dev/null; pkill -9 -x RKW' 2>/dev/null")
                Shell.exec("rm -f /data/local/tmp/koznroot/rkw_stop /data/local/tmp/koznroot/rkw_done /data/local/tmp/koznroot/rkw_cmd 2>/dev/null")
                appendLogLine("清理完成")

                appendLogLine("---- 检测本机状态 ----")
                val mod = Shell.exec("grep -ci kernelsu /proc/modules 2>/dev/null").trim().toIntOrNull() ?: 0
                val su = Shell.exec("su -c id 2>&1").trim()
                appendLogLine("内核模块: " + (if (mod > 0) "已加载" else "未加载"))
                appendLogLine("su: " + su)
                val isRoot = mod > 0 || su.contains("uid=0(root)")

                appendLogLine("---- 当前管理器检测 ----")
                val act = activeManager ?: RootManager.active(ctx)
                appendLogLine("当前生效管理器: " + (act?.displayName ?: "未知"))
                appendLogLine("已选择(下次使用): " + selectedManager.displayName)
                var anyMgr = false
                for (m in RootManager.values()) {
                    val p = RootManager.installedPackage(ctx, m)
                    if (p != null) {
                        anyMgr = true
                        val ver = Shell.exec(
                            "dumpsys package " + p + " 2>/dev/null | grep -m1 versionName"
                        ).trim().replace("versionName=", "")
                        appendLogLine("已安装: " + m.displayName + " " + ver + " (" + p + ")")
                    }
                }
                if (!anyMgr) appendLogLine("已安装: 无")
                val savedPkg = Prefs(ctx).lastManagerPkg
                if (savedPkg != null && RootManager.isPackageInstalled(ctx, savedPkg)) {
                    appendLogLine("上次注册的管理器包: " + savedPkg)
                }
                val sukiPkgs = listOf("pewhkk.neduyw.pgwdfo", "com.sukisu.ultra")
                    .filter { RootManager.isPackageInstalled(ctx, it) }
                if (sukiPkgs.size > 1) {
                    appendLogLine("提示: 同时安装了多个 SukiSU 管理器；请用本次注册的那个授权（见上）")
                }
                val ksudInfoRaw = Shell.exec(
                    "if [ -x /data/adb/ksud ]; then /data/adb/ksud debug info 2>&1 | head -12; " +
                        "else echo 'KSUS_HIDDEN'; fi"
                ).trim()
                val ksudInfo = ksudInfoRaw.split("\n").filter {
                    it.isNotBlank() && !it.startsWith("sh:") &&
                        !it.contains("inaccessible") && !it.contains("not found") &&
                        !it.contains("Permission denied") && it != "KSUS_HIDDEN"
                }
                if (ksudInfo.isEmpty()) {
                    appendLogLine("ksud: 当前 shell 无法访问（SELinux/未授权），已跳过")
                } else {
                    appendLogLine("ksud: " + ksudInfo.joinToString(" | "))
                }
                val dynRaw = Shell.exec("/data/adb/ksud manager list 2>&1 | head -8").trim()
                val dyn = dynRaw.split("\n").filter {
                    it.isNotBlank() && !it.startsWith("sh:") &&
                        !it.contains("inaccessible") && !it.contains("not found") &&
                        !it.contains("Permission denied")
                }
                if (dyn.isNotEmpty() && !dynRaw.contains("unknown") &&
                    !dynRaw.contains("No such") && !dynRaw.contains("unrecognized")
                ) {
                    appendLogLine("动态管理器: " + dyn.joinToString(" | "))
                } else {
                    appendLogLine("动态管理器: 当前 shell 无法访问（SELinux/未授权），已跳过")
                }
                if (act != null && RootManager.installedPackage(ctx, act) == null) {
                    appendLogLine("⚠ 当前生效的 " + act.displayName + " 管理器未安装（下方会自动安装）")
                }

                val mgr = activeManager ?: selectedManager
                appendLogLine("当前管理器: " + mgr.displayName)
                var mgrPkg = RootManager.installedPackage(ctx, mgr)
                var mgrOk = mgrPkg != null
                if (!mgrOk) {
                    appendLogLine(mgr.displayName + " 管理器不在，自动安装 ...")
                    if (copyAssetToWork(mgr.managerAsset)) {
                        Shell.exec("chmod 644 /data/local/tmp/koznroot/" + mgr.managerAsset)
                        appendLogLine(Shell.exec("pm install -r /data/local/tmp/koznroot/" + mgr.managerAsset + " 2>&1").trim())
                        mgrOk = mgr.packages.any { p -> Shell.exec("pm path " + p + " 2>/dev/null").contains("package:") }
                    } else {
                        appendLogLine("✗ 管理器安装包写入失败")
                    }
                } else {
                    appendLogLine(mgr.displayName + " 管理器已安装")
                }
                appendLogLine("---- 完成 ----")

                ui.post {
                    rooted = isRoot
                    managerPresent = mgrOk
                    running = false
                    statusLevel = if (isRoot) 2 else 3
                    statusText = if (isRoot) "提权成功" else "未 ROOT"
                    statusHint = if (isRoot) "重启失效" else "看日志"
                    uiMessage = when {
                        isRoot && !mgrOk -> "提权正常，管理器安装失败"
                        isRoot -> "提权正常，已清理并检查"
                        else -> "未检测到 root"
                    }
                    logFile?.delete()
                    logFile = null
                }
            } catch (_: Throwable) {
                ui.post {
                    running = false
                    statusLevel = 3
                    statusText = "失败"
                    statusHint = "看日志"
                    uiMessage = "检测失败"
                    logFile?.delete()
                    logFile = null
                }
            }
        }.start()
    }

    fun refresh() {
        Thread {
            // Never call Shell.exec() when the Stellar binder is absent: its retry
            // logic would auto-launch Stellar (the app would "jump" there on start).
            val ready = Shell.ready()
            val model = Build.MODEL
            val kernel = System.getProperty("os.version") ?: "-"
            val rel = Build.VERSION.RELEASE
            val sdk = Build.VERSION.SDK_INT
            val se = if (ready) Shell.exec("getenforce").trim().ifEmpty { "未知" } else "未检测"
            val mgr = RootManager.installed(ctx, RootManager.active(ctx))
            // 判断当前是否仍然 root：
            //   1) Stellar 可用 -> 看内核模块 + su 是否真的 uid=0
            //   2) Stellar 不可用 -> 同一次开机的成功记录（本 App 提权过）视为仍 root
            val bootWall = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
            val prefs = Prefs(ctx)
            val sameBoot = prefs.lastSuccessAt > 0L &&
                kotlin.math.abs(prefs.lastSuccessBootWall - bootWall) < 5000L
            var isRooted = false
            if (ready) {
                val mod = Shell.exec("grep -ci kernelsu /proc/modules 2>/dev/null").trim()
                    .toIntOrNull() ?: 0
                val su = Shell.exec("su -c id 2>&1").trim()
                isRooted = su.contains("uid=0(root)") || (mod > 0 && !su.contains("EXEC_ERROR"))
            } else {
                isRooted = sameBoot
            }
            val stellarApk = try {
                ctx.packageManager.getLaunchIntentForPackage("roro.stellar.manager") != null
            } catch (_: Throwable) {
                false
            }
            val activeMgr = if (isRooted) RootManager.active(ctx) else null
            val selectedMgr = RootManager.selected(ctx)
            val mgrPresentNow = RootManager.installed(ctx, activeMgr ?: selectedMgr)
            ui.post {
                devModel = model
                devKernel = kernel
                devSystem = "Android " + rel + " · SDK " + sdk
                devSelinux = se
                devFingerprint = Build.FINGERPRINT
                stellarConnected = ready
                stellarInstalled = stellarApk
                activeManager = activeMgr
                selectedManager = selectedMgr
                managerPresent = mgrPresentNow
                rooted = isRooted
                if (isRooted) {
                    for (i in 0..5) {
                        stageStates[i] = 2
                    }
                }
                refreshIdleStatus()
            }
        }.start()
    }

    private fun refreshIdleStatus() {
        if (running || lastOk != null) return
        if (rooted) {
            statusLevel = 2
            statusText = "提权成功"
            statusHint = "重启失效"
            return
        }
        when (stellarConnected) {
            true -> {
                statusLevel = 0
                statusText = "就绪"
                statusHint = "可以开始"
            }
            false -> {
                statusLevel = 3
                statusText = "未连接"
                statusHint = "请先连接"
            }
            else -> {
                statusLevel = 0
                statusText = "检测中"
                statusHint = "检测中"
            }
        }
    }
}
