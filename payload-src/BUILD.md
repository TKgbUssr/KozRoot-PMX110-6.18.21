# payload 源码与构建

本目录包含 App `assets` 中两个二进制的源码：

| 文件 | 产物 | 说明 |
|---|---|---|
| `exploit_pmx110_bprime.c` | `exploit_direct` | CVE-2026-64560 写原语 / KASLR stage0 exploit（aarch64，static，约 2.6 MB） |
| `root_worker2_bprime.c` | `root_worker2` | B′-2 worker（b3）：caps 触发 / `/data/adb` 布局 / `finit_module` / `su` 收尾 |

exploit 移植基础来自 [cybermeowfia/CyberMeowfia](https://github.com/cybermeowfia/CyberMeowfia)（Apache-2.0）；
worker 与 PMX110 适配部分为本项目实现。

## 构建（NDK r29 clang / Android API 29）

```sh
# exploit（static）
clang --target=aarch64-linux-android29 -static -O2 -pthread \
    -o exploit_direct exploit_pmx110_bprime.c

# worker（PIE）
clang --target=aarch64-linux-android29 -O2 -pie -fPIE -pthread \
    -o root_worker2 root_worker2_bprime.c
```

产物放入 `app/src/main/assets/`，并同步更新：

- `RootEngine.BASE_ASSET_HASHES`（exploit_direct / root_worker2 的 SHA-256）
- `RootManager` 中与此相关的 asset 校验值

## 当前内置二进制的校验值

```
exploit_direct  sha256 = 2fef478b98553364aea722e61e3a96d0ec302838d63b7f1fd7c9133fe28a69e0
root_worker2    sha256 = d57e6a2c185fb6c73aebf9bb6970127412a1d7003f53917cacfe30a8552969cd  (b3)
```

内核模块（`.ko`）不在此目录：它们来自 KernelSU-Next / KernelSU / SukiSU-Ultra /
FinaSU / BakaSU 上游，本项目做了离线预重定位后放入 `app/src/main/assets/`；
`tools/patch_ko_slide.py` 为 PC 端的模块 slide 平移工具（App 内等价实现见 `PatchKo.java`）。
