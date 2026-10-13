import sys, os
from PIL import Image, ImageDraw, ImageFilter
import numpy as np
src, out_res, preview = sys.argv[1], sys.argv[2], sys.argv[3]
im = Image.open(src).convert("RGB").crop((65, 61, 1189, 1176)).resize((1024, 1024), Image.LANCZOS)
N = 1024 * 108 // 76            # 108dp の中に絵を 76dp で置く（見える範囲は中央 72dp。丸型でもスマホの絵が欠けないように）
canvas = Image.new("RGB", (N, N), (0, 0, 0)); off = (N - 1024) // 2
canvas.paste(im, (off, off))
# 絵の外側（白い角と余白）を「埋める対象」にする
mask = Image.new("L", (N, N), 0)
m2 = Image.new("L", (1024, 1024), 255)
a = np.asarray(im).astype(int)
white = Image.fromarray(((a.min(axis=2) > 235)).astype(np.uint8) * 255).copy()
for c in [(0, 0), (1023, 0), (0, 1023), (1023, 1023)]:
    ImageDraw.floodfill(white, c, 128)
corner = (np.asarray(white) == 128)
m2a = np.where(corner, 0, 255).astype(np.uint8)
m2 = Image.fromarray(m2a).filter(ImageFilter.MinFilter(5))
mask.paste(m2, (off, off))
known = np.asarray(mask) > 0
img = np.asarray(canvas).astype(float); img[~known] = 0
w = known.astype(float)
# 周りの色を少しずつにじませて埋める（簡易インペイント）
def box1(x, r, ax):
    p = np.pad(x, [(r + 1, r) if k == ax else (0, 0) for k in range(2)], mode="edge")
    c = np.cumsum(p, axis=ax)
    n = x.shape[ax]
    return (np.take(c, range(2 * r + 1, 2 * r + 1 + n), axis=ax) - np.take(c, range(0, n), axis=ax)) / (2 * r + 1)
def box(x, r): return box1(box1(x, r, 0), r, 1)
cur = img.copy(); curw = w.copy()
for r in [4, 8, 16, 32, 64, 96]:
    for _ in range(3):
        b = np.stack([box(cur[..., i] * curw, r) for i in range(3)], -1)
        bw = box(curw, r)
        fill = b / np.maximum(bw, 1e-6)[..., None]
        newly = (~known) & (bw > 1e-4)
        cur = np.where(known[..., None], img, np.where(newly[..., None], fill * 1.0, 0))
        curw = np.where(known | newly, 1.0, 0.0)
cur = np.where(known[..., None], img, cur)
fg = Image.fromarray(np.clip(cur, 0, 255).astype(np.uint8))
# 境目をなじませる
soft = Image.fromarray((known * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))
fg = Image.composite(canvas, fg.filter(ImageFilter.GaussianBlur(3)), soft)
for name, dp in [("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4)]:
    d = os.path.join(out_res, f"mipmap-{name}"); os.makedirs(d, exist_ok=True)
    s = int(108 * dp)
    fg.resize((s, s), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
avg = np.asarray(fg).reshape(-1, 3).mean(0).astype(int)
print("avg #%02X%02X%02X" % tuple(avg))
# プレビュー：円・角丸・正方形のマスク
P = 432; f = fg.resize((P, P), Image.LANCZOS); vis = int(P * 72 / 108); o = (P - vis) // 2
sheet = Image.new("RGB", (P * 3 + 80, P + 40), (40, 40, 48))
for i, shape in enumerate(["circle", "squircle", "square"]):
    m = Image.new("L", (P, P), 0); dr = ImageDraw.Draw(m)
    box = (o, o, o + vis, o + vis)
    if shape == "circle": dr.ellipse(box, 255)
    elif shape == "squircle": dr.rounded_rectangle(box, vis // 3, 255)
    else: dr.rectangle(box, 255)
    sheet.paste(f, (20 + i * (P + 20), 20), m)
sheet.save(preview)
