package com.KozOnekey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * ELF64 (aarch64) KASLR slide patcher for the KernelSU(-Next) kernel module.
 *
 * The module carries absolute kernel addresses bound at build time against a
 * fixed link-time slide. At runtime the slide differs per boot, so every
 * absolute symbol inside the kernel address range must be shifted by
 * (current_slide - base_slide).
 *
 * Same transformation as tools/patch_ko_slide.py, implemented in Java so the
 * on-device app can do it without a PC.
 */
public final class PatchKo {

    private static final int SHN_ABS = 0xfff1;
    private static final long KERNEL_ADDR_MIN = 0xffffff8000000000L; // signed: negative
    private static final long BASE_SLIDE = 0x1c1c200000L;

    private PatchKo() {}

    public static String patch(File inFile, long slide, File outFile) throws IOException {
        byte[] data = Files.readAllBytes(inFile.toPath());
        if (data.length < 0x40 || (data[0] & 0xff) != 0x7f
                || data[1] != 'E' || data[2] != 'L' || data[3] != 'F'
                || data[4] != 2) {
            return "bad ELF";
        }
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        long e_shoff = bb.getLong(0x28);
        int e_shentsize = bb.getShort(0x3a) & 0xffff;
        int e_shnum = bb.getShort(0x3c) & 0xffff;
        int e_shstrndx = bb.getShort(0x3e) & 0xffff;

        long shstrOff = 0, shstrSize = 0;
        {
            long o = e_shoff + (long) e_shstrndx * e_shentsize;
            shstrOff = bb.getLong((int) o + 24);
            shstrSize = bb.getLong((int) o + 32);
        }

        long symOff = 0, symSize = 0, symEnt = 0;
        for (int i = 0; i < e_shnum; i++) {
            int so = (int) (e_shoff + (long) i * e_shentsize);
            int nmOff = bb.getInt(so);
            long start = shstrOff + (nmOff & 0xffffffffL);
            int end = (int) start;
            long lim = Math.min(data.length, shstrOff + shstrSize);
            while (end < lim && data[end] != 0) end++;
            String nm = new String(data, (int) start, Math.max(0, end - (int) start),
                    StandardCharsets.US_ASCII);
            if (".symtab".equals(nm)) {
                symOff = bb.getLong(so + 24);
                symSize = bb.getLong(so + 32);
                symEnt = bb.getLong(so + 56);
            }
        }
        if (symOff == 0 || symEnt == 0) return "no .symtab";

        long delta = slide - BASE_SLIDE;
        long count = symSize / symEnt;
        int absTotal = 0, shifted = 0;
        for (long i = 0; i < count; i++) {
            int so = (int) (symOff + i * symEnt);
            if (so + 24 > data.length) break;
            int shndx = bb.getShort(so + 6) & 0xffff;
            if (shndx != SHN_ABS) continue;
            absTotal++;
            long value = bb.getLong(so + 8);
            if (value >= KERNEL_ADDR_MIN && value < 0) {
                long nv = value + delta;
                bb.putLong(so + 8, nv);
                shifted++;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(data);
        }
        return "abs=" + absTotal + " shifted=" + shifted + " delta=0x" + Long.toHexString(delta);
    }

    private static void putLong(ByteBuffer bb, int off, long v) { bb.putLong(off, v); }
}
