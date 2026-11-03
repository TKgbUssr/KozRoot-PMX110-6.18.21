#!/system/bin/sh
# root_stage_sukisu.sh -- SukiSU-Ultra v4.3.0 (41100, CONFIG_KPM=y) 用户态收尾
# 由 root_worker2 在 finit_module 加载 patched kernelsu.ko 之后以 root 执行
# 工作目录 = 脚本所在目录；ksud 文件名为 ksud342（与 worker 兼容）
PATH=/data/adb/ksu/bin:/system/bin:/system/xbin:/system_ext/bin
export PATH
D=$(dirname "$0")
LOG=$D/root_stage.log
exec >> "$LOG" 2>&1
echo "=== mobile root stage (SukiSU-Ultra v4.3.0) $(date) ==="
id
getenforce
grep -i kernelsu /proc/modules
cat /proc/uptime

MGR=$(cat $D/manager_pkg.txt 2>/dev/null | head -1)
[ -n "$MGR" ] || MGR=pewhkk.neduyw.pgwdfo
KSUD=$D/ksud342
echo "manager pkg: $MGR"

echo "--- ensure module loaded ---"
if ! grep -q '^kernelsu ' /proc/modules 2>/dev/null; then
    echo "worker finit_module 未加载模块，改用 ksud insmod 兜底 ..."
    grep -E '^(Uid|Gid|Cap)' /proc/self/status 2>&1
    $KSUD insmod $D/ksu-base.ko 2>&1
    echo "ksud_insmod_rc=$?"
else
    echo "module already loaded"
fi

echo "--- ksud install (userspace) ---"
$KSUD install 2>&1
echo "install_rc=$?"

mkdir -p /data/adb/ksu/bin /data/adb/modules 2>/dev/null
cp -f $KSUD /data/adb/ksud 2>/dev/null
chmod 755 /data/adb/ksud
chown root:root /data/adb/ksud 2>/dev/null

echo "--- late-load: 初始化用户态/SELinux/KPM/模块挂载 ---"
CUR_CTX=$(cat /proc/self/attr/current 2>/dev/null | tr -d '\0\r\n')
CUR_DOM=$(echo "$CUR_CTX" | cut -d: -f3)
if [ -n "$CUR_DOM" ] && [ "$CUR_DOM" != "ksu" ]; then
    $KSUD sepolicy patch "allow $CUR_DOM ksu process { transition dyntransition }" >/dev/null 2>&1 || true
    $KSUD sepolicy patch "allow $CUR_DOM self process { setcurrent setexec }" >/dev/null 2>&1 || true
    $KSUD sepolicy patch "allow ksu * file { entrypoint execute execute_no_trans }" >/dev/null 2>&1 || true
fi
if [ "$CUR_DOM" = "ksu" ]; then
    $KSUD late-load --package-name "$MGR" 2>&1
    echo "late_load_rc=$?"
elif command -v runcon >/dev/null 2>&1 && runcon u:r:ksu:s0 true 2>/dev/null; then
    runcon u:r:ksu:s0 $KSUD late-load --package-name "$MGR" 2>&1
    echo "late_load_rc=$?"
else
    ORIG_ENF=$(getenforce 2>/dev/null || echo Enforcing)
    setenforce 0 2>/dev/null || true
    $KSUD late-load --package-name "$MGR" 2>&1
    echo "late_load_rc=$?"
    if [ "$ORIG_ENF" = "Enforcing" ]; then
        setenforce 1 2>/dev/null || true
    fi
fi

echo "--- lifecycle hooks ---"
for KCMD in post-fs-data services boot-completed; do
    timeout 30 /data/adb/ksud "$KCMD" 2>&1
    echo "ksud $KCMD rc=$?"
done

echo "--- manager register (SukiSU dynamic manager) ---"
if /data/adb/ksud manager --help >/dev/null 2>&1; then
    APK=$(pm path "$MGR" 2>/dev/null | head -1 | cut -d: -f2)
    if [ -n "$APK" ] && [ -f "$APK" ]; then
        SIG=$(/data/adb/ksud debug get-sign "$APK" 2>&1)
        echo "get-sign: $SIG"
        SIZE=$(echo "$SIG" | sed -n 's/.*size: \([0-9]*\),.*/\1/p')
        HASH=$(echo "$SIG" | sed -n 's/.*hash: \([0-9a-fA-F]*\)$/\1/p')
        echo "manager cert: size=$SIZE hash=$HASH"
        if [ -n "$SIZE" ] && [ -n "$HASH" ]; then
            /data/adb/ksud manager add "$MGR" "$SIZE" "$HASH" 2>&1
            echo "manager add $MGR rc=$?"
            /data/adb/ksud manager set-enabled true 2>&1
            echo "set_enabled_rc=$?"
            echo "NOTE: 本次注册的管理器包为 $MGR；若手机上有其它版本 SukiSU 管理器，请用这个包授权，避免 grant 失效"
        else
            echo "manager cert parse failed"
        fi
    else
        echo "manager APK not found: $MGR"
    fi
else
    echo "no dynamic manager support, skip"
fi

echo "--- features ---"
/data/adb/ksud feature list 2>&1
/data/adb/ksud feature set su_compat 1 2>&1
echo "set_su_compat_rc=$?"
/data/adb/ksud feature set adb_root 1 2>&1
echo "set_adb_root_rc=$?"

echo "--- ensure /data/adb layout (kernel KSUD_PATH=/data/adb/ksud) ---"
if ! cmp -s /data/adb/ksud $KSUD 2>/dev/null; then
    cp -f $KSUD /data/adb/ksud
    echo "ksud replaced -> payload"
else
    echo "ksud already payload"
fi
chmod 755 /data/adb/ksud
chown root:root /data/adb/ksud 2>/dev/null
ln -sf /data/adb/ksud /data/adb/ksu/bin/su
ln -sf /data/adb/ksud /data/adb/ksu/bin/ksud
chown -h root:root /data/adb/ksu/bin/su /data/adb/ksu/bin/ksud 2>/dev/null
if [ -f $D/ksuinit ]; then
    cp -f $D/ksuinit /data/adb/ksu/bin/ksuinit
    chmod 755 /data/adb/ksu/bin/ksuinit
    chown root:root /data/adb/ksu/bin/ksuinit 2>/dev/null
fi
ls -la /data/adb /data/adb/ksu /data/adb/ksu/bin 2>&1

echo "--- unload ColorOS anti-root guard (blocks ksud execve) ---"
rmmod oplusboot 2>&1; echo "rm_boot=$?"
rmmod oplus_secure_common 2>&1; echo "rm_common=$?"
rmmod oplus_secure_guard_new 2>&1; echo "rm_guard=$?"
echo "--- modules after rmmod ---"
grep -iE 'oplus_secure_guard_new|oplus_secure_common|oplusboot' /proc/modules 2>&1

echo "--- su entry: kernel hooks only (no real su file) ---"
echo real_su_present=$(ls /system/bin 2>/dev/null | grep -qx su && echo yes || echo no)
/data/adb/ksud feature set su_compat 1 2>&1
/data/adb/ksud feature list 2>&1 | head -8
/data/adb/ksud debug info 2>&1 | head -20

echo "--- patch Termux su wrapper ---"
TS=/data/data/com.termux/files/usr/bin/su
if [ -f "$TS" ]; then
    if grep -q '/system_ext/bin/su' "$TS" 2>/dev/null; then
        echo "termux su already patched"
    else
        awk '
        /^for p in \/debug_ramdisk\/su / {
            print "for p in /system_ext/bin/su /debug_ramdisk/su /sbin/su /system/sbin/su /system/bin/su /system/xbin/su /su/bin/su /magisk/.core/bin/su";
            next;
        }
        { print }
        ' "$TS" > "$TS.tmp" && cat "$TS.tmp" > "$TS" && rm -f "$TS.tmp"
        echo "termux su patched"
    fi
    grep -n 'for p in' "$TS" 2>&1
else
    echo "termux not installed, skip"
fi

echo "--- su self test (as current root) ---"
timeout 8 su -c id 2>&1
echo "su_test_rc=$?"
echo "--- helper self test (optional) ---"
if [ -x /data/adb/pmx110/app_grant ]; then
    /data/adb/pmx110/app_grant --size 2>&1
    echo "helper_rc=$?"
else
    echo "no pmx110 helper, skip"
fi

echo "--- id ---"
id > $D/root_id.txt
CUR_UID=$(id -u)
if [ "$CUR_UID" = "0" ] && [ -x /data/adb/ksud ]; then
    echo ROOT_STAGE_OK > $D/root_stage.ok
    echo "LAYOUT_OK"
else
    echo ROOT_STAGE_FAIL > $D/root_stage.ok
    echo "LAYOUT_FAIL uid=$CUR_UID"
fi
chmod 644 $D/root_id.txt $D/root_stage.ok $LOG 2>/dev/null
echo "=== mobile root stage end ==="
