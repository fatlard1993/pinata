#!/usr/bin/env python3
"""Generate pinata's mod icon: a rainbow jeb_ sheep dressed as a fringed pinata,
hanging from its rope, drawn on a 32px grid and scaled 4x nearest to 128x128.

Pure-stdlib PNG writer, deterministic, same approach as poopsmith's
generate_textures.py.
"""

import os, random, struct, zlib

def write_png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in r) for r in pixels)
    def chunk(t, d):
        c = t + d
        return struct.pack(">I", len(d)) + c + struct.pack(">I", zlib.crc32(c))
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))

CLEAR = (0, 0, 0, 0)
OUTLINE = (0x24, 0x1C, 0x22, 0xFF)
ROPE = (0xA8, 0x86, 0x52, 0xFF); ROPE_DARK = (0x75, 0x5A, 0x33, 0xFF)
FACE = (0xD9, 0xBC, 0xA3, 0xFF); FACE_DARK = (0xB8, 0x98, 0x7E, 0xFF); EYE = (0x1A, 0x14, 0x14, 0xFF)
LEG = (0xD9, 0xBC, 0xA3, 0xFF)
# fringe bands top to bottom: (base, shade)
BANDS = [((0xE8, 0x3B, 0x3B, 255), (0xB5, 0x24, 0x2A, 255)),
         ((0xF2, 0x8C, 0x28, 255), (0xC4, 0x68, 0x16, 255)),
         ((0xF5, 0xD8, 0x2F, 255), (0xC9, 0xAC, 0x1C, 255)),
         ((0x5C, 0xC9, 0x4A, 255), (0x3A, 0x9A, 0x33, 255)),
         ((0x3F, 0xA8, 0xE8, 255), (0x2A, 0x7E, 0xBC, 255)),
         ((0x9B, 0x5C, 0xE0, 255), (0x74, 0x3C, 0xB0, 255))]

N = 32
g = [[CLEAR] * N for _ in range(N)]

def fringe(x0, x1, y0, band_h, first_band):
    for row in range((len(BANDS) - first_band) * band_h):
        y = y0 + row
        b = first_band + row // band_h
        base, shade = BANDS[b]
        for x in range(x0, x1 + 1):
            # last row of each band hangs in teeth over the next band
            if row % band_h == band_h - 1 and (x % 2) and b + 1 < len(BANDS):
                g[y][x] = BANDS[b + 1][1]
            else:
                g[y][x] = shade if (row % band_h == band_h - 1) else base

# body
fringe(4, 24, 10, 2, 0)
# head: fringe cap, face below
for y in range(7, 17):
    for x in range(22, 30):
        g[y][x] = FACE
fringe(22, 29, 7, 1, 3)  # purple-ish cap rows (green, blue, purple)
for y in range(10, 17):
    g[y][22] = FACE_DARK
g[11][27] = EYE; g[11][28] = EYE
g[14][29] = FACE_DARK

# outline everything opaque
out = [r[:] for r in g]
for y in range(N):
    for x in range(N):
        if g[y][x][3] == 0 and any(0 <= x + dx < N and 0 <= y + dy < N and g[y + dy][x + dx][3]
                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            out[y][x] = OUTLINE
g = out
# legs go on after the outline pass so each keeps its own and the gaps stay open
HOOF = (0x4A, 0x40, 0x3C, 0xFF)
for lx in (5, 10, 16, 21):
    for y in range(22, 27):
        g[y][lx - 1] = OUTLINE
        g[y][lx + 2] = OUTLINE
        if y == 22:
            continue
        g[y][lx] = LEG
        g[y][lx + 1] = FACE_DARK
    g[26][lx] = HOOF
    g[26][lx + 1] = HOOF
    for x in range(lx - 1, lx + 3):
        g[27][x] = OUTLINE
# rope from the top edge down to the back
for y in range(0, 9):
    g[y][14] = ROPE if y % 2 else ROPE_DARK
# confetti
rng = random.Random(7)
for (x, y) in [(4, 4), (26, 2), (8, 29), (19, 30), (29, 22), (2, 16), (20, 4), (27, 28)]:
    g[y][x] = BANDS[rng.randrange(len(BANDS))][0]

write_png(os.path.join(os.path.dirname(__file__), "src/main/resources/assets/pinata/icon.png"),
          [[g[y // 4][x // 4] for x in range(128)] for y in range(128)])
