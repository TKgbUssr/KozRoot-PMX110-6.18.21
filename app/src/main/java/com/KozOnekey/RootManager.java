package com.KozOnekey;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * 支持的临时 Root 管理器（内核模块 + 用户态 ksud + 管理器 APK 组合）。
 * 所有变体共用同一套 exploit 链：预重定位后的 patched ko 由 root_worker2 finit_module 加载，
 * 随后执行对应 root_stage*.sh 完成用户态、grant 与管理器注册。
 */
public enum RootManager {
    KERNELSU_NEXT(0, "KernelSU-Next", "com.rifsxd.ksunext", new String[]{"com.rifsxd.ksunext", "kqvdie.zeykvw.hpbcyd"}, 33355,
            "ksu-base.ko", "ksud342", "manager.apk", "root_stage.sh", null,
            new String[]{"susfs_guard_lkm.ko"}),
    KERNELSU(1, "KernelSU", "me.weishu.kernelsu", new String[]{"me.weishu.kernelsu"}, 32680,
            "ksu-base-ksu.ko", "ksud-ksu", "manager-ksu.apk", "root_stage_ksu.sh", "ksuinit-ksu",
            new String[]{}),
    SUKISU_ULTRA(2, "SukiSU-Ultra", "com.sukisu.ultra", new String[]{"com.sukisu.ultra", "pewhkk.neduyw.pgwdfo"}, 41100,
            "ksu-base-sukisu.ko", "ksud-sukisu", "manager-sukisu.apk", "root_stage_sukisu.sh", "ksuinit-sukisu",
            new String[]{}),
    FINASU(3, "FinaSU", "me.yuki.fina", new String[]{"me.yuki.fina"}, 33090,
            "ksu-base-fina.ko", "ksud-fina", "manager-fina.apk", "root_stage_fina.sh", null,
            new String[]{"nomount.ko"}),
    BAKASU(4, "BakaSU", "com.resukisu.resukisu", new String[]{"com.resukisu.resukisu"}, 35257,
            "ksu-base-baka.ko", "ksud-baka", "manager-baka.apk", "root_stage_baka.sh", null,
            new String[]{});

    public final int id;
    public final String displayName;
    public final String bearerPackage;      // 默认/内置 APK 的包名
    public final String[] packages;         // 已安装时依次识别
    public final int minVersionCode;
    public final String koAsset;
    public final String ksudAsset;
    public final String managerAsset;
    public final String stageAsset;
    public final String ksuinitAsset;       // 可为 null
    public final String[] extraAssets;      // 伴生模块（susfs_guard_lkm.ko / nomount.ko 等）

    RootManager(int id, String displayName, String bearerPackage, String[] packages, int minVersionCode,
                String koAsset, String ksudAsset, String managerAsset, String stageAsset,
                String ksuinitAsset, String[] extraAssets) {
        this.id = id;
        this.displayName = displayName;
        this.bearerPackage = bearerPackage;
        this.packages = packages;
        this.minVersionCode = minVersionCode;
        this.koAsset = koAsset;
        this.ksudAsset = ksudAsset;
        this.managerAsset = managerAsset;
        this.stageAsset = stageAsset;
        this.ksuinitAsset = ksuinitAsset;
        this.extraAssets = extraAssets;
    }

    public static RootManager fromId(int id) {
        for (RootManager m : values()) {
            if (m.id == id) return m;
        }
        return KERNELSU_NEXT;
    }

    public static RootManager selected(Context ctx) {
        try {
            return fromId(new Prefs(ctx).getRootManager());
        } catch (Throwable t) {
            return KERNELSU_NEXT;
        }
    }

    public static void setSelected(Context ctx, RootManager m) {
        try {
            new Prefs(ctx).setRootManager(m.id);
        } catch (Throwable ignored) {
        }
    }

    /** 上次提权成功使用的管理器；没有记录则返回 null */
    public static RootManager last(Context ctx) {
        try {
            int id = new Prefs(ctx).getLastManager();
            return id < 0 ? null : fromId(id);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setLast(Context ctx, RootManager m) {
        try {
            new Prefs(ctx).setLastManager(m.id);
        } catch (Throwable ignored) {
        }
    }

    /** 当前实际生效的管理器：上次成功记录优先，其次“只装了一个”就推断它，最后退回选中项 */
    public static RootManager active(Context ctx) {
        RootManager last = last(ctx);
        if (last != null) return last;
        int count = 0;
        RootManager only = null;
        for (RootManager m : values()) {
            if (installed(ctx, m)) {
                count++;
                only = m;
            }
        }
        if (count == 1) return only;
        return selected(ctx);
    }

    public static String installedPackage(Context ctx, RootManager m) {
        for (String p : m.packages) {
            if (isPackageInstalled(ctx, p)) return p;
        }
        return null;
    }

    public static boolean installed(Context ctx, RootManager m) {
        return installedPackage(ctx, m) != null;
    }

    public static boolean isPackageInstalled(Context ctx, String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static int installedVersionCode(Context ctx, String pkg) {
        try {
            return (int) ctx.getPackageManager().getPackageInfo(pkg, 0).getLongVersionCode();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 所有已安装管理器包名（用于运行结束统一关闭旧实例） */
    public static String[] installedPackages(Context ctx) {
        StringBuilder sb = new StringBuilder();
        for (RootManager m : values()) {
            String p = installedPackage(ctx, m);
            if (p != null) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(p);
            }
        }
        return sb.length() == 0 ? new String[0] : sb.toString().split(" ");
    }

    /** payload 资产 SHA-256（引擎在运行前校验，防止被替换） */
    public String[][] assetHashes() {
        switch (this) {
            case KERNELSU_NEXT:
                return new String[][]{
                        {"ksu-base.ko", "f0060f9ba38c367ba844bf5838c9946346d82c03c08111d7fddf60f21f406666"},
                        {"ksud342", "2771a2ae989b522b5d70f3288ad5a2f379488962d09c1fe11e13a745eba89362"},
                        {"manager.apk", "774a5511f7034a4930e881114e202319cbdc34c5aaaa5c7c6b6d2902364e4c58"},
                        {"root_stage.sh", "de853f3404694b7e85d6baa33df46567d917d03e4eb47f97840e2a5c4ff20476"},
                        {"susfs_guard_lkm.ko", "a8f209fd41a67b56c80424f9a8fed53d16dc2838f35682a14287bf98e8c7d8c2"}};
            case KERNELSU:
                return new String[][]{
                        {"ksu-base-ksu.ko", "f2215414cadbaf93d963f1b34700e8208caa96e2bec8609981bf763307bb8832"},
                        {"ksud-ksu", "049491e2b4cc0a6c913d3920cb90b8c94c9e8ca96c0ee92818293304973bfb1b"},
                        {"manager-ksu.apk", "b4a059ace71de2fa65cc7f12b1c58d55fc23ab25e057ee4230ce779742abe450"},
                        {"root_stage_ksu.sh", "d8ee9c3f597a3691729f5ff6e4ba03a8bd04998e41202560e1b5852bd5d08796"},
                        {"ksuinit-ksu", "365c66e5435a9ecdb3dbb23ceda2d9253568defeee3682af95220087094eff5d"}};
            case SUKISU_ULTRA:
                return new String[][]{
                        {"ksu-base-sukisu.ko", "da9972110cf9885da87a365898365512df58d3da70d32c027d0da2f6dc642870"},
                        {"ksud-sukisu", "e9fcad8189397d5c96965819ae1f184557c7a51701fed183a1e0190242a89501"},
                        {"manager-sukisu.apk", "a928eb57ad34b105233f92dcd55e54dd883482e417970276329a7d0f0979e212"},
                        {"root_stage_sukisu.sh", "2364f289c53b529c1c0145f2ab1f1e4b464bb19b952467189e21845dc70da31a"},
                        {"ksuinit-sukisu", "c7034b996c739734ecefe4357397549443725830c3e395ee4e2dd7913a4c2b26"}};
            case FINASU:
                return new String[][]{
                        {"ksu-base-fina.ko", "d65b24b15a16fae8d7017bca13a4f8154d5d7a3069a4f26e7c16b824a9f33772"},
                        {"ksud-fina", "8967e8b309293335354734600ae0725a6d3d9a81a0382907f656d376f52dff29"},
                        {"manager-fina.apk", "367e28a646d4f448218ea1fa5d1228fcdd5fb20623c5e22eecb3ea3462fb19e1"},
                        {"root_stage_fina.sh", "210d8a98eae33e137f984bbe84176c8acde390e565d97bff232124280827eec8"},
                        {"nomount.ko", "ca66d280fbbc74844301f0f01eaf3baeaf7f64e7b2aeb412e18b5e4416b0ae9f"}};
            case BAKASU:
                return new String[][]{
                        {"ksu-base-baka.ko", "de89180db77fd9be365f631aefbbdf0012ff7319354e010d55c05e88bff56f2a"},
                        {"ksud-baka", "e80c601e1ff09f90bfed933db5693fe01f6fcfb748a0206a08e74423892b60ff"},
                        {"manager-baka.apk", "3f64434ce06c046ac06ba7dd8a7bc8f3735fcbfc6e172e33c4e1d537aab8082b"},
                        {"root_stage_baka.sh", "465f59abd93d8c59732d3dac7d71c035fcb48cb391d9bd1544b3fb57ff64c7d3"}};
            default:
                return new String[0][];
        }
    }
}
