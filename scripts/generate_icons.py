#!/usr/bin/env python3
from pathlib import Path
import base64, io, urllib.request, sys, subprocess
try:
    from PIL import Image
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pillow", "-q"])
    from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
src = None
b64_path = ROOT / "branding" / "logo.b64"
if b64_path.exists():
    raw = b64_path.read_text().strip()
    if raw and "PLACEHOLDER" not in raw and len(raw) > 100:
        try:
            src = Image.open(io.BytesIO(base64.b64decode(raw))).convert("RGBA")
            print("using logo.b64")
        except Exception as e:
            print("b64 fail", e)
if src is None:
    for url in (
        "https://wisdom-tower-academy.live/images/brand/logo.png",
        "https://raw.githubusercontent.com/hiyabteklu/Wisdom-tower-academy/main/public/images/brand/logo.png",
    ):
        try:
            print("download", url)
            with urllib.request.urlopen(url, timeout=30) as r:
                src = Image.open(io.BytesIO(r.read())).convert("RGBA")
            break
        except Exception as e:
            print("fail", e)
if src is None:
    raise SystemExit("no logo source")

def make_icon(size):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    logo = src.copy()
    logo.thumbnail((int(size * 0.88), int(size * 0.88)), Image.Resampling.LANCZOS)
    canvas.paste(logo, ((size - logo.width) // 2, (size - logo.height) // 2), logo)
    return canvas

res = ROOT / "app" / "src" / "main" / "res"

# Remove vector XML that would clash with PNG adaptive assets
for name in (
    "drawable/ic_launcher_foreground.xml",
    "drawable/ic_launcher_background.xml",
):
    p = res / name
    if p.exists():
        p.unlink()
        print("removed", name)

for folder, size in {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}.items():
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    icon = make_icon(size)
    icon.save(d / "ic_launcher.png", "PNG")
    icon.save(d / "ic_launcher_round.png", "PNG")
    for n in ("ic_launcher.webp", "ic_launcher_round.webp"):
        p = d / n
        if p.exists():
            p.unlink()
    print(folder)

fg_size = 432
fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
logo = src.copy()
logo.thumbnail((int(fg_size * 0.72), int(fg_size * 0.72)), Image.Resampling.LANCZOS)
fg.paste(logo, ((fg_size - logo.width) // 2, (fg_size - logo.height) // 2), logo)
(res / "drawable").mkdir(exist_ok=True)
fg.save(res / "drawable" / "ic_launcher_foreground.png", "PNG")
Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 255)).save(
    res / "drawable" / "ic_launcher_background.png", "PNG"
)
print("ok")
