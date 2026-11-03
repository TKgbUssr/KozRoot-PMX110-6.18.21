#!/usr/bin/env python3
"""PC-side replica of the app's PatchKo.java (ELF64 aarch64 KASLR slide patcher).

Usage:
    python patch_ko_slide_bprime.py <input.ko> <slide-hex> <output.ko>

Reproduces KozRoot PatchKo.patch():
  - parse ELF64 .symtab
  - for every SHN_ABS symbol whose value is in the kernel image range
    (unsigned value >= 0xffffff8000000000), add (slide - BASE_SLIDE)
  - write the patched copy

BASE_SLIDE is the link-time slide of the offline pre-relocated LKM
(0x1c1c200000).  The app's PatchKo.java uses exactly the same constant.
"""
from __future__ import annotations

import struct
import sys

SHN_ABS = 0xFFF1
KERNEL_ADDR_MIN = 0xFFFFFF8000000000
BASE_SLIDE = 0x1C1C200000


def main(argv: list[str]) -> int:
    if len(argv) != 4:
        print(__doc__)
        return 2
    in_path, slide_s, out_path = argv[1], argv[2], argv[3]
    slide = int(slide_s, 16)

    data = bytearray(open(in_path, "rb").read())
    if len(data) < 0x40 or data[:4] != b"\x7fELF" or data[4] != 2:
        print("bad ELF")
        return 1
    e_shoff = struct.unpack_from("<Q", data, 0x28)[0]
    e_shentsize = struct.unpack_from("<H", data, 0x3A)[0]
    e_shnum = struct.unpack_from("<H", data, 0x3C)[0]
    e_shstrndx = struct.unpack_from("<H", data, 0x3E)[0]

    def shdr(i: int) -> tuple[int, int, int]:
        off = e_shoff + i * e_shentsize
        name, _typ, _flags, _addr, offset, size, _link, _info, _align, entsize = struct.unpack_from(
            "<IIQQQQIIQQ", data, off
        )
        return name, offset, size, entsize

    _, shstr_off, shstr_size, _ = shdr(e_shstrndx)
    shstr = bytes(data[shstr_off:shstr_off + shstr_size])

    sym_off = sym_size = sym_ent = 0
    for i in range(e_shnum):
        name_off, offset, size, entsize = shdr(i)
        end = shstr.find(b"\x00", name_off)
        name = shstr[name_off:end if end >= 0 else len(shstr)]
        if name == b".symtab":
            sym_off, sym_size, sym_ent = offset, size, entsize
    if not sym_off or not sym_ent:
        print("no .symtab")
        return 1

    delta = (slide - BASE_SLIDE) & 0xFFFFFFFFFFFFFFFF
    count = sym_size // sym_ent
    abs_total = shifted = 0
    for i in range(count):
        off = sym_off + i * sym_ent
        if off + 24 > len(data):
            break
        shndx = struct.unpack_from("<H", data, off + 6)[0]
        if shndx != SHN_ABS:
            continue
        abs_total += 1
        value = struct.unpack_from("<Q", data, off + 8)[0]
        if value >= KERNEL_ADDR_MIN:
            struct.pack_into("<Q", data, off + 8, (value + delta) & 0xFFFFFFFFFFFFFFFF)
            shifted += 1

    open(out_path, "wb").write(data)
    print(f"abs={abs_total} shifted={shifted} slide=0x{slide:x} delta=0x{delta:x}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
