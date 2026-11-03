#!/system/bin/sh
# root_stage.sh -- KernelSU-Next v3.4.2 + SUSFS 临时 Root 用户态收尾（由 root_worker2 加载模块后以 root 执行）
PATH=/data/adb/ksu/bin:/system/bin:/system/xbin:/system_ext/bin
export PATH
D=$(dirname "$0")
LOG=$D/root_stage.log
exec >> "$LOG" 2>&1
echo "=== mobile root stage (KernelSU-Next v3.4.2 + SUSFS) $(date) ==="
id
getenforce
grep -i kernelsu /proc/modules
cat /proc/uptime

MGR=$(cat $D/manager_pkg.txt 2>/dev/null | head -1)
[ -n "$MGR" ] || MGR=com.rifsxd.ksunext
KSUD=$D/ksud342
echo "manager pkg: $MGR"

echo "--- ensure module loaded ---"
if ! grep -q '^kernelsu ' /proc/modules 2>/dev/null; then
    echo "worker finit_module 未加载模块，改用 ksud insmod 兜底 ..."
    $KSUD insmod $D/ksu-base.ko 2>&1
    echo "ksud_insmod_rc=$?"
else
    echo "module already loaded"
fi

echo "--- companion module ---"
COMPANION_FILE="susfs_guard_lkm.ko"
if [ -n "$COMPANION_FILE" ] && [ -f "$D/$COMPANION_FILE" ]; then
    COMPANION_MOD="susfs_guard_lkm"
    if grep -q "^$COMPANION_MOD " /proc/modules 2>/dev/null; then
        echo "$COMPANION_FILE already loaded"
    else
        echo "loading $COMPANION_FILE ..."
        $KSUD insmod "$D/$COMPANION_FILE" 2>&1
        echo "companion_insmod_rc=$?"
    fi
else
    echo "no companion module"
fi

echo "--- ensure /data/adb layout ---"
mkdir -p /data/adb/ksu/bin /data/adb/modules 2>/dev/null
cp -f $KSUD /data/adb/ksud 2>/dev/null
chmod 755 /data/adb/ksud
chown root:root /data/adb/ksud 2>/dev/null
ln -sf /data/adb/ksud /data/adb/ksu/bin/su
ln -sf /data/adb/ksud /data/adb/ksu/bin/ksud
chown -h root:root /data/adb/ksu/bin/su /data/adb/ksu/bin/ksud 2>/dev/null

echo "--- late-load ---"
CUR_CTX=$(cat /proc/self/attr/current 2>/dev/null | tr -d '\000\015\012')
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

echo "--- manager register (dynamic manager, if supported) ---"
if /data/adb/ksud manager --help >/dev/null 2>&1; then
    APK=$(pm path "$MGR" 2>/dev/null | head -1 | cut -d: -f2)
    if [ -n "$APK" ] && [ -f "$APK" ]; then
        SIG=$(/data/adb/ksud debug get-sign "$APK" 2>&1)
        echo "get-sign: $SIG"
        SIZE=$(echo "$SIG" | sed -n 's/.*size: \([0-9]*\),.*/\1/p')
        HASH=$(echo "$SIG" | sed -n 's/.*hash: \([0-9a-fA-F]*\)$/\1/p')
        if [ -n "$SIZE" ] && [ -n "$HASH" ]; then
            /data/adb/ksud manager add "$MGR" "$SIZE" "$HASH" 2>&1
            echo "manager add rc=$?"
            /data/adb/ksud manager set-enabled true 2>&1
            echo "set_enabled_rc=$?"
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

echo "--- unload ColorOS anti-root guard ---"
rmmod oplusboot 2>&1; echo "rm_boot=$?"
rmmod oplus_secure_common 2>&1; echo "rm_common=$?"
rmmod oplus_secure_guard_new 2>&1; echo "rm_guard=$?"

echo "--- su entry: kernel hooks only (no real su file) ---"
echo real_su_present=$(ls /system/bin 2>/dev/null | grep -qx su && echo yes || echo no)
timeout 8 su -c id 2>&1
echo "su_test_rc=$?"
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
