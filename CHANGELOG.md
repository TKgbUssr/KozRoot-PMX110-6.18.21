# CHANGELOG

## v1.99.1

### 新增
- **原版 KernelSU / KernelSU-Next 支持 CI / 自签名管理器**：提权时检测已安装管理器的
  实际签名，把证书哈希与证书长度注入内核模块（`ksu-base*.ko`）后再加载，同包名、
  不同签名的 CI 版管理器也能被内核直接识别（已实测 KernelSU v3.3.1 CI 版）。
- 「管理器连不上？修复关联」：管理器换签名后，应用内自动读取当前 APK 证书并重新登记；
  SukiSU 可热恢复，FinaSU 需重启后重新提权生效。

### 优化
- 管理器证书读取改为 PackageManager 直接读取（DER 长度 + SHA-256），兼容 v2/v3 签名，
  不再依赖只支持 v2 的 `ksud get-sign`。
- 修复关联改为多命令自动探测：SukiSU `manager add` / FinaSU `dynamic set-hash`+`grant` /
  BakaSU `kernel dynamic-manager set` / 兜底 `debug set-manager`。

### 版本
- versionName 1.99.1 / versionCode 105
- APK SHA-256：`7eeae7a6581b0172819a60e957c4d0099e9768dc0f86699eb25f6ca53a275a1d`

## v1.99

### 链路
- 新增 **98260 链路**（比 64560 更快），默认启用；旧内核可在 UI 切回 64560（实验），
  新内核自动隐藏切换、默认 98260。
- 新内核（ColorOS 17.0.1.202 及以后）适配：载荷、管理器 LKM、伴生模块均提供 `-new` 版本。

### 管理器
- 5 套管理器全部适配新内核；管理器自动安装/升级。
- 收尾恢复 SELinux 为 Enforcing，卸载 OPPO guard 模块（不触碰 oplusboot）。

### UI
- 新内核隐藏 98260 链路开关；副标题「98260 比 64560 速度更快」。
- 提权页新增「管理器连不上？修复关联」入口。
- 版本号显示更新。

## v1.98（独立仓库第一版）

### 链路
- 单一「64560 优化链路」（B′-2）：capsA `cred+0x40 V=0x3`（CAP_CHOWN|CAP_DAC_OVERRIDE）
  → worker 在 fsuid=2000 下布局 `/data/adb`；capsB `cred+0x42 V=0x1` 追加
  CAP_SYS_MODULE → worker `finit_module` + `root_stage`。**全程不需要 fsuid=0**（5 阶段）。
- 移除旧 fsuid 链路及其切换 UI（顶部拨钮、旧阶段、`Prefs.rootChain` 等）。

### 管理器
- 支持 5 套：KernelSU-Next、KernelSU、SukiSU-Ultra、FinaSU、BakaSU；
  自动安装/升级管理器，按管理器做 late-load 与 grant。
- 内核模块「离线预重定位」（ABS 符号绑定）+ App 运行时按 KASLR slide 平移（PatchKo）。

### 修复 / 稳定性
- 旧 exploit 进程按 comm 名无法杀掉的问题：`pkill` → `kill -9 $(pidof exploit_direct)`。
- 阶段切换改用 SIGSTOP 冻结，避免进程退出触发的 POSIX timer 清理 panic。
- worker child 存活校验（避免向已释放 cred 写入）、fsuid / slide 合理性校验。
- capsA 命中即切 capsB（`CAPS_LAYOUT_SEEN`），与 `/data/adb` 布局并行。
- **内核残留提示弹窗**：检测到 POSIX timer 残留 / 旧 exploit 进程时，弹出与
  「运行日志 / 开源致谢」同款的液态玻璃弹窗，显示具体数值并给出
  「重启 / 取消执行 / 继续执行」三个选项（重启经 Stellar shell 尝试）。
- 阶段轮询窗口加长（perm 600 s / caps 360 s），避免 attempt 中途被冻结泄漏 timer。

### UI
- 提权页版本行字号放大（9.5sp → 12sp）。
- 移除链路拨钮；进度标题改为「提权进度」；日志显示「链路: 64560 优化链路（5 阶段）」。
