package com.KozOnekey;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import roro.stellar.Stellar;
import roro.stellar.StellarRemoteProcess;

/**
 * Thin wrapper around the Stellar shell bridge.
 *
 * Stellar (independent third-party library, roro.stellar) provides an
 * adb-shell-equivalent (uid 2000) process spawner to third-party apps after
 * the user grants it. We only use it to spawn shell commands -- all the root
 * logic lives in this app and in root_stage.sh.
 */
public final class Shell {

    private Shell() {}

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Context APP;

    /** Must be called once from the Activity so we can restart Stellar if its binder dies. */
    public static void init(Context context) {
        APP = context == null ? null : context.getApplicationContext();
    }

    /** True when the returned text means "the shell channel itself is broken". */
    public static boolean isChannelError(String s) {
        if (s == null) return true;
        return s.contains("EXEC_ERROR")
                || s.contains("WAIT_ERROR")
                || s.contains("NO_PROCESS")
                || s.contains("Service not connected")
                || s.contains("DeadObjectException");
    }

    /**
     * Best-effort recovery when the Stellar service/binder died mid-run: if the
     * binder is gone, bring the Stellar app to the foreground (which restarts
     * its shell server) and wait for it to come back.
     */
    public static boolean recover(long timeoutMs) {
        if (ready()) return true;
        try {
            if (APP != null) {
                Intent it = APP.getPackageManager().getLaunchIntentForPackage(STELLAR_PKG);
                if (it != null) {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    APP.startActivity(it);
                }
            }
        } catch (Throwable ignored) { }
        return waitReady(timeoutMs);
    }

    private static final String STELLAR_PKG = "roro.stellar.manager";

    /** Wait until the Stellar binder is available. */
    public static boolean waitReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (Stellar.INSTANCE.getBinder() != null) return true;
            } catch (Throwable ignored) { }
            try { Thread.sleep(200); } catch (InterruptedException e) { return false; }
        }
        return false;
    }

    public static boolean ready() {
        try {
            return Stellar.INSTANCE.getBinder() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Execute one shell command (through /system/bin/sh -c) and return combined output. */
    public static String exec(String cmd) {
        return execRetry(cmd, 120, 3);
    }

    /**
     * Execute a command, retrying when the shell channel itself is broken.
     * A DeadObjectException / "Service not connected" from Stellar is not a
     * command failure -- it means the shell service died, so we recover and
     * try again instead of reporting a wrong result to the user.
     */
    public static String execRetry(String cmd, int timeoutSec, int attempts) {
        String result = execTimeout(cmd, timeoutSec);
        for (int i = 1; i < attempts && isChannelError(result); i++) {
            recover(8000L);
            try {
                Thread.sleep(600L);
            } catch (InterruptedException e) {
                return result;
            }
            result = execTimeout(cmd, timeoutSec);
        }
        return result;
    }

    /** Execute one shell command with a hard timeout in seconds. */
    public static String execTimeout(String cmd, int timeoutSec) {
        String[] argv = new String[]{"sh", "-c", cmd};
        String[] envp = new String[]{
                "PATH=/data/local/tmp:/sbin:/system/sbin:/system/bin:/system/xbin:/system_ext/bin"
        };
        StellarRemoteProcess proc = null;
        try {
            proc = Stellar.INSTANCE.newProcess(argv, envp, "/data/local/tmp");
            if (proc == null) return "<NO_PROCESS>";
        } catch (Throwable t) {
            return "<EXEC_ERROR: " + t.getMessage() + ">";
        }

        final StellarRemoteProcess p = proc;
        final StringBuilder out = new StringBuilder();
        Thread rd = new Thread(() -> {
            try (InputStream in = p.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.append(new String(buf, 0, n, "UTF-8"));
            } catch (Throwable ignored) { }
        });
        Thread re = new Thread(() -> {
            try (InputStream in = p.getErrorStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.append(new String(buf, 0, n, "UTF-8"));
            } catch (Throwable ignored) { }
        });
        rd.setDaemon(true);
        re.setDaemon(true);
        rd.start();
        re.start();

        try {
            boolean done = p.waitForTimeout(timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
            if (!done) {
                try { p.destroy(); } catch (Throwable ignored) { }
                out.append("\n<TIMEOUT after ").append(timeoutSec).append("s>");
            }
        } catch (Throwable t) {
            out.append("\n<WAIT_ERROR: ").append(t.getMessage()).append(">");
        }
        try { rd.join(2000); re.join(2000); } catch (InterruptedException ignored) { }
        return out.toString();
    }

    /** Read a whole remote file. */
    public static String readFile(String path) {
        return exec("cat " + quote(path) + " 2>/dev/null");
    }

    /**
     * Write a local stream to a remote path, THROUGH the shell process.
     *
     * The app process itself (untrusted_app) is not allowed by SELinux to
     * create files under /data/local/tmp (shell_data_file), so we must pipe the
     * bytes into a `cat > dest` running as shell (uid 2000).
     */
    public static boolean writeFile(String dest, InputStream data) {
        String[] argv = new String[]{"sh", "-c", "cat > " + quote(dest)};
        String[] envp = new String[]{"PATH=/sbin:/system/bin:/system/xbin"};
        StellarRemoteProcess proc;
        try {
            proc = Stellar.INSTANCE.newProcess(argv, envp, "/data/local/tmp");
            if (proc == null) return false;
        } catch (Throwable t) {
            return false;
        }
        try (OutputStream os = proc.getOutputStream()) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = data.read(buf)) > 0) os.write(buf, 0, n);
            os.flush();
        } catch (Throwable t) {
            return false;
        }
        try {
            proc.waitForTimeout(180, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable ignored) { }
        try {
            return proc.exitValue() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String shellQuote(String s) {
        return quote(s);
    }

    private static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    /** Run a callback on the UI thread. */
    public static void ui(Runnable r) {
        MAIN.post(r);
    }
}
