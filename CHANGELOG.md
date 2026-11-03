# CHANGELOG

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
