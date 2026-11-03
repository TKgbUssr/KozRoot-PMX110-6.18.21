package com.KozOnekey;

/**
 * 64560 优化链路（B′-2）：两次安全 caps 写。
 *
 * <ul>
 *   <li>capsA：{@code cred+0x40 = 0x3}（CAP_CHOWN | CAP_DAC_OVERRIDE），
 *       worker 在 fsuid=2000 下完成 /data/adb 布局；</li>
 *   <li>capsB：{@code cred+0x42 = 0x1}（追加 CAP_SYS_MODULE），worker 加载 LKM；</li>
 * </ul>
 * 全程不需要 fsuid=0。
 *
 * <p>未来接入其它漏洞链路时：在本枚举里追加一项，并在
 * {@link RootEngine#doFlow()} 里追加对应执行分支即可；当前只保留一条链路。</p>
 */
public enum RootChain {
    CVE_64560_NEW(0, "64560 优化链路", "优化链",
            "B′-2：两次 caps 写（CHOWN/DAC_OVERRIDE + SYS_MODULE），不需要 fsuid=0",
            new String[]{
                    "降 SELinux",
                    "泄露 cred / slide",
                    "KASLR 补丁",
                    "提权 CAP_CHOWN/DAC",
                    "追加 CAP_SYS_MODULE + grant"});

    public final int id;
    /** 日志显示名 */
    public final String displayName;
    /** 进度标题用的短名 */
    public final String shortName;
    /** 说明 */
    public final String description;
    /** UI 阶段名（数量决定 Root 页进度条行数，也约束引擎 stage 下标） */
    public final String[] stageNames;

    RootChain(int id, String displayName, String shortName, String description, String[] stageNames) {
        this.id = id;
        this.displayName = displayName;
        this.shortName = shortName;
        this.description = description;
        this.stageNames = stageNames;
    }

    public int stageCount() {
        return stageNames.length;
    }
}
