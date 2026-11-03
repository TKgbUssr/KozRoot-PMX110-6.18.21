# 多管理器说明（v1.98，5 套 Root 体系）

KozRoot 只保留一条链路（**64560 优化链路 / B′-2**），但支持 5 套 KernelSU 系列管理器；
在「提权」页选择管理器后，App 会自动安装/升级对应的管理器 APK、复制对应内核模块与
ksud，并按该管理器完成 late-load 加载与 grant。

## 管理器与内置资产

| 管理器 | 内核模块（主 + 伴生） | ksud | 管理器 APK（包名） | 说明 |
|---|---|---|---|---|
| **KernelSU-Next**（默认） | `ksu-base.ko` + `susfs_guard_lkm.ko` | `ksud342` | `manager.apk`（`com.rifsxd.ksunext`） | KernelSU-Next v3.4.2（33355），带 SUSFS 伴生模块；**优化链路已真机 FULL_PASS** |
| **KernelSU**（官方 v3.3.1） | `ksu-base-ksu.ko` | `ksud-ksu` | `manager-ksu.apk`（`me.weishu.kernelsu`） | 优化链路待逐台回归 |
| **SukiSU-Ultra**（v4.3.0 KPM） | `ksu-base-sukisu.ko` | `ksud-sukisu` | `manager-sukisu.apk`（`com.sukisu.ultra`） | 优化链路待逐台回归 |
| **FinaSU**（v1.3.2） | `ksu-base-fina.ko` + `nomount.ko` | `ksud-fina` | `manager-fina.apk`（`me.yuki.fina`） | 带 NoMount 伴生模块；优化链路待回归 |
| **BakaSU**（v4.2.0-rc3） | `ksu-base-baka.ko` | `ksud-baka` | `manager-baka.apk`（`com.resukisu.resukisu`） | 优化链路待回归 |

管理器的包名、模块/ksud/APK 资产与最低版本号定义在 `RootManager.java`；
所有资产 SHA-256 在启动时校验（`RootEngine` / `RootManager`）。

## 64560 优化链路（B′-2）

| 阶段 | 内容 |
|---|---|
| 1 | SELinux → Permissive（64560 写原语） |
| 2 | 泄露 cred / KASLR slide（worker） |
| 3 | 按 slide 给内核模块做运行时平移 |
| 4 | capsA：`cred+0x40 = 0x3`（CAP_CHOWN / CAP_DAC_OVERRIDE），worker 在 fsuid=2000 下布局 `/data/adb` |
| 5 | capsB：`cred+0x42 = 0x1`（追加 CAP_SYS_MODULE），worker `finit_module` + `root_stage` |

- 两次写原语的值只能是 0–3，因此必须分两次写；顺序不能反（capsA 会覆盖 0x42 字节）。
- worker 自身门控：仅在「布局完成且 caps 为 0x10003」时才加载模块，避免脏状态加载。

## 模块加载方式（重要）

新套件的 `.ko` 原生依赖 `ksud insmod` 做 kallsyms 重定位。KozRoot 为了沿用
`root_worker2` + `finit_module` 链，出厂前对主模块做**离线预重定位**：

- 把未定义内核符号改写为按本机 `vmlinux.elf` 符号 + base-slide `0x1c1c200000` 计算的
  `SHN_ABS` 绝对地址；
- App 运行时再由 `PatchKo` 按**当前开机的 KASLR slide** 平移；
- 伴生模块（`susfs_guard_lkm.ko`、`nomount.ko`）不预重定位，由 `root_stage*.sh`
  以 root 身份通过 `ksud insmod` 加载。

## 运行前/后清理

- 运行前：清理残留进程与 `/data/local/tmp` 临时文件；检测内核 POSIX timer 残留与
  旧 exploit 进程（过多时提示重启）。
- 运行后：关闭管理器旧实例、清理临时文件。
- exploit 进程在阶段切换时用 `SIGSTOP` 冻结（避免进程退出清理触发的 panic），
  重启后自动清空。

## 验证状态（v1.98）

- **优化链路**：PMX110 + KernelSU-Next v3.4.2 真机 FULL_PASS（capsA 后 fsuid=2000
  完成 `/data/adb` 布局；capsB 后 worker 读到 `CapEff=0x10003`；`FINT_MODULE rc=0`、
  模块 Live、`su -c id` = uid=0、`root_stage.ok=ROOT_STAGE_OK`）。
- KernelSU / SukiSU-Ultra / FinaSU / BakaSU 机制同源，但需逐台（每次重启）回归验证。
- App 内（Stellar 通道）端到端待最终验证：引擎逻辑已通过 adb harness 验证。
