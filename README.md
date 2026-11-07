# KozRoot

作者：**Kozmiztar / kgb玉米**

KozRoot 是一个针对 **OPPO Find X10 Pro Max（PMX110，MT6995，Android 17 / ColorOS 17）**
的**临时一键 root** 工具：利用内核漏洞完成凭据提权，加载 KernelSU 系列内核模块（LKM），
重启后失效。

本项目是 KozRoot 的**独立仓库**：内置全部运行所需 assets 与源码说明，不依赖外部目录。

## 下载

最新版本：**v1.99.1**

- [Releases](https://github.com/TKgbUssr/KozRoot-PMX110-6.18.21/releases)
- 文件：`KozRoot-v1.99.1-PMX110.apk`
- SHA-256：`7eeae7a6581b0172819a60e957c4d0099e9768dc0f86699eb25f6ca53a275a1d`

## 特性

- 包名 `com.KozOnekey`，应用名 **KozRoot**，版本 v1.99.1
- 页面：主页（设备信息/状态/提权控制）、提权（5 步进度 + 实时日志 + 管理器选择）、
  日志（运行记录查看/复制/导出/清除）、设置（界面与个性化）
- **98260 链路（默认）**：比 64560 更快；旧内核可在 UI 切回 64560（实验）链路，
  新内核自动隐藏切换、默认 98260
- **多管理器（5 套）**：KernelSU-Next v3.4.2（+SUSFS）/ 官方 KernelSU v3.3.1 /
  SukiSU-Ultra v4.3.0 / FinaSU v1.3.2（+NoMount）/ BakaSU v4.2.0-rc3；
  可在提权页切换，自动安装/升级管理器
- **CI / 自签名管理器支持**：提权时检测已安装管理器的实际签名，把证书哈希与证书
  长度注入内核模块后再加载，原版 KernelSU / KernelSU-Next 也能识别同包名、不同签名的
  CI 版管理器（已实测）
- **「管理器连不上？修复关联」**：管理器换签名导致连不上时，应用内一键读取当前
  APK 证书并重新登记；SukiSU 可热恢复，FinaSU 需重启后重新提权
- 收尾自动恢复 SELinux 为 **Enforcing**，并卸载 OPPO guard 模块（不触碰 oplusboot）
- 内核模块「离线预重定位」+ 运行时按 KASLR slide 平移（`PatchKo`）
- 仅支持 PMX110 + 指定内核；临时 root，重启失效

## 适配内核

| 版本 | 内核 |
|---|---|
| ColorOS 17.0.1.109 及以前 | `6.18.21-android17-5-gf957accaf563-ab15926176-4k` |
| ColorOS 17.0.1.202 及以后 | `6.18.21-android17-5-g7fcbffcad3e1-ab16353812-4k` |

## 仓库结构

```
app/                 Android 应用（Kotlin/Compose）
  src/main/assets/   内置 payload：pc98260、veil、exec_helper、root_stage_m6.sh、
                     repair_worker.sh、5 套 .ko / ksud / 管理器 APK / root_stage*.sh
  src/main/java/…    RootEngine / RootState / RootChain / RootManager / Chain98260 / PatchKo / Shell …
docs/                多管理器说明
payload-src/         exploit 与 worker 源码 + 构建说明（BUILD.md）
tools/               PC 端模块 slide 平移/发布脚本
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
- exploit 移植基础 CyberMeowfia（Apache-2.0）；
- shell 权限桥 Stellar（GPL-3.0）；
- UI 基于 LiquidGlass UI 模板适配。

## 免责声明

本工具仅供个人学习与技术研究使用，请勿用于商业用途或倒卖；使用可能带来设备不稳定、
数据丢失等风险，请自行评估并承担全部后果。

本项目采用 GPL-3.0（见 [`LICENSE`](LICENSE)）。
