# KozRoot

作者：**Kozmiztar / kgb玉米**

KozRoot 是一个针对 **OPPO Find X10 Pro Max（PMX110，MT6995，Android 17 / ColorOS 17）**
的**临时一键 root** 工具：利用 CVE-2026-64560 在内核中完成凭据提权，加载 KernelSU
系列内核模块（LKM），重启后失效。

本项目是 KozRoot 的**独立仓库**：内置全部运行所需 assets 与源码说明，不依赖外部目录。

## 特性

- 包名 `com.KozOnekey`，应用名 **KozRoot**，版本 v1.98
- 页面：主页（设备信息/状态/提权控制）、提权（5 步进度 + 实时日志 + 管理器选择）、
  日志（运行记录查看/复制/导出/清除）、设置（界面与个性化）
- **64560 优化链路**（B′-2）：两次安全 caps 写
  - capsA：`cred+0x40 = 0x3`（CAP_CHOWN | CAP_DAC_OVERRIDE），worker 在 fsuid=2000 下布局 `/data/adb`
  - capsB：`cred+0x42 = 0x1`（追加 CAP_SYS_MODULE），worker `finit_module` + `root_stage`
  - **全程不需要 fsuid=0**，共 5 个阶段
- **多管理器（5 套）**：KernelSU-Next v3.4.2（+SUSFS）/ 官方 KernelSU v3.3.1 /
  SukiSU-Ultra v4.3.0/ FinaSU v1.3.2（+NoMount）/ BakaSU v4.2.0-rc3；
  可在提权页切换，自动安装/升级管理器，按管理器做 late-load 与 grant
- 内核模块「离线预重定位」+ 运行时按 KASLR slide 平移（`PatchKo`）
- 稳定性措施：exploit 进程状态清理（冻结而非 SIGKILL）、worker child 存活校验、
  fsuid / slide 合理性校验、内核残留提示弹窗（显示 POSIX timer / 旧 exploit 数值，
  可选「重启 / 取消执行 / 继续执行」）
- 仅支持 PMX110 + 指定内核；临时 root，重启失效

## 仓库结构

```
app/                 Android 应用（Kotlin/Compose）
  src/main/assets/   内置 payload：exploit_direct、root_worker2、5 套 .ko / ksud /
                     管理器 APK / root_stage*.sh / susfs_guard_lkm.ko / nomount.ko
  src/main/java/…    RootEngine / RootState / RootChain / RootManager / PatchKo / Shell …
docs/                多管理器说明
payload-src/         exploit 与 worker 源码 + 构建说明（BUILD.md）
tools/               PC 端模块 slide 平移脚本
NOTICE               第三方组件与致谢
CHANGELOG.md         版本记录
LICENSE              GPL-3.0
```

## 构建

要求：JDK 21、Android SDK（compileSdk 37）、NDK（仅重建 payload 时需要）。

```sh
cp local.properties.example local.properties   # 填入本机 sdk.dir
./gradlew :app:assembleRelease
```

- release 已开启 R8 混淆与资源压缩；签名请使用**自己的** keystore（不要提交到仓库）。
- 产物位于 `app/build/outputs/apk/release/`。
- 重新生成 `app/src/main/assets/` 中的 payload / 内核模块的流程见
  [`payload-src/BUILD.md`](payload-src/BUILD.md) 与 `tools/`。

## 致谢与第三方组件

详见 [`NOTICE`](NOTICE)。要点：

- 内核模块来源：KernelSU-Next（官方 v3.4.2）、KernelSU（官方）、SukiSU-Ultra、
  FinaSU、BakaSU；按各自上游许可证发布，本项目仅做离线符号预重定位与运行时平移。
- CVE-2026-64560 实现思路参考 OneKeyJB / 星苒鸭（GPL-3.0）；
- exploit 移植基础 CyberMeowfia（Apache-2.0）；
- shell 权限桥 Stellar（GPL-3.0）；
- UI 基于 LiquidGlass UI 模板适配。

## 免责声明

本工具仅供个人学习与技术研究使用，请勿用于商业用途或倒卖；使用可能带来设备不稳定、
数据丢失等风险，请自行评估并承担全部后果。

本项目采用 GPL-3.0（见 [`LICENSE`](LICENSE)）。
