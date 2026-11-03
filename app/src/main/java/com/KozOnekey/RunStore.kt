package com.KozOnekey

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File

/** Local run-history storage (same format as the previous app version). */
object RunStore {

    data class RunRecord(
        val file: File,
        val time: Long,
        val ok: Boolean,
        val seconds: Long,
        val estimated: Boolean = false
    )

    private fun dir(ctx: Context): File = File(ctx.filesDir, "runs").apply { mkdirs() }

    fun save(ctx: Context, ok: Boolean, seconds: Long, log: String) {
        saveRaw(ctx, System.currentTimeMillis(), ok, seconds, log)
    }

    private fun saveRaw(ctx: Context, time: Long, ok: Boolean, seconds: Long, log: String, estimated: Boolean = false) {
        try {
            val f = File(dir(ctx), time.toString() + ".txt")
            f.writeText(
                "TIME=" + time + "\n" +
                    "RESULT=" + (if (ok) "OK" else "FAIL") + "\n" +
                    "DURATION=" + seconds + "\n" +
                    (if (estimated) "EST=1\n" else "") +
                    "===LOG===\n" + log,
                Charsets.UTF_8
            )
            // keep newest 50
            val all = list(ctx)
            if (all.size > 50) all.drop(50).forEach { it.file.delete() }
        } catch (_: Throwable) {
        }
    }

    /** 运行开始时创建增量日志文件（panic/重启后仍保留） */
    fun newInProgress(ctx: Context, start: Long): File? {
        return try {
            File(dir(ctx), "run_" + start + ".inprogress").apply {
                writeText(
                    "TIME=" + start + "\nRESULT=RUNNING\nDURATION=0\n===LOG===\n",
                    Charsets.UTF_8
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun appendLog(file: File?, line: String) {
        if (file == null) return
        try {
            file.appendText(line, Charsets.UTF_8)
        } catch (_: Throwable) {
        }
    }

    /** App 启动时把上次没跑完（panic/重启/被杀）的日志转成失败记录 */
    fun recoverInterrupted(ctx: Context): Int {
        var n = 0
        try {
            val files = dir(ctx).listFiles { f -> f.isFile && f.name.endsWith(".inprogress") } ?: return 0
            for (f in files) {
                try {
                    val text = f.readText(Charsets.UTF_8)
                    val time = Regex("TIME=(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull()
                        ?: System.currentTimeMillis()
                    val log = text.substringAfter("===LOG===\n", "")
                    // 用最后一条带时间戳的日志估算跑到哪一步（panic 通常在其后几秒）
                    val timeRe = Regex("\\[(\\d{1,2}):(\\d{2}):(\\d{2})\\]")
                    val lastMatch = timeRe.findAll(log).lastOrNull()
                    var est = -1L
                    if (lastMatch != null) {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = time }
                        cal.set(java.util.Calendar.HOUR_OF_DAY, lastMatch.groupValues[1].toInt())
                        cal.set(java.util.Calendar.MINUTE, lastMatch.groupValues[2].toInt())
                        cal.set(java.util.Calendar.SECOND, lastMatch.groupValues[3].toInt())
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        var delta = (cal.timeInMillis - time) / 1000L
                        if (delta < 0) delta += 24 * 3600L
                        if (delta in 0..86400L) est = delta
                    }
                    val note = if (est >= 0) {
                        "\n[记录] 本次运行被中断（panic / 重启 / 进程被杀），用时为中断前最后一条日志的估算。\n"
                    } else {
                        "\n[记录] 本次运行被中断（panic / 重启 / 进程被杀），用时未知；以上为中断前日志。\n"
                    }
                    saveRaw(ctx, time, false, est, log + note, estimated = est >= 0)
                    f.delete()
                    n++
                } catch (_: Throwable) {
                }
            }
            if (n > 0) {
                val all = list(ctx)
                if (all.size > 50) all.drop(50).forEach { it.file.delete() }
            }
        } catch (_: Throwable) {
        }
        return n
    }

    fun list(ctx: Context): List<RunRecord> {
        val files = dir(ctx).listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return emptyList()
        return files.mapNotNull { f ->
            try {
                var time = 0L
                var ok = false
                var seconds = 0L
                var estimated = false
                f.bufferedReader(Charsets.UTF_8).use { r ->
                    while (true) {
                        val line = r.readLine() ?: break
                        when {
                            line.startsWith("TIME=") -> time = line.substring(5).toLongOrNull() ?: 0L
                            line.startsWith("RESULT=") -> ok = line.substring(7) == "OK"
                            line.startsWith("DURATION=") -> seconds = line.substring(9).toLongOrNull() ?: 0L
                            line.startsWith("EST=") -> estimated = line.substring(4) == "1"
                            line.startsWith("===LOG===") -> break
                        }
                    }
                }
                RunRecord(f, time, ok, seconds, estimated)
            } catch (_: Throwable) {
                null
            }
        }.sortedByDescending { it.time }
    }

    fun readLog(r: RunRecord): String {
        return try {
            r.file.readText(Charsets.UTF_8).substringAfter("===LOG===\n", "")
        } catch (_: Throwable) {
            ""
        }
    }

    fun deleteAll(ctx: Context): Int {
        var n = 0
        list(ctx).forEach { if (it.file.delete()) n++ }
        dir(ctx).listFiles { f -> f.isFile && f.name.endsWith(".inprogress") }?.forEach {
            it.delete()
            n++
        }
        return n
    }

    fun exportAll(ctx: Context): Int {
        val records = list(ctx)
        if (records.isEmpty()) return 0
        Shell.exec("mkdir -p /sdcard/Download/KozRoot 2>/dev/null")
        var n = 0
        records.forEach { r ->
            val name = "KozRoot-" + r.file.name
            val bytes = readLog(r).toByteArray(Charsets.UTF_8)
            if (Shell.writeFile("/sdcard/Download/KozRoot/" + name, ByteArrayInputStream(bytes))) n++
        }
        return n
    }

    fun exportOne(ctx: Context, r: RunRecord): Boolean {
        Shell.exec("mkdir -p /sdcard/Download/KozRoot 2>/dev/null")
        val bytes = readLog(r).toByteArray(Charsets.UTF_8)
        return Shell.writeFile("/sdcard/Download/KozRoot/KozRoot-" + r.file.name, ByteArrayInputStream(bytes))
    }
}
