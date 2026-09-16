#!/usr/bin/env python3
"""Decode branding/logo.b64 and write Android launcher mipmaps + adaptive foreground."""
from pathlib import Path
import base64
import io

try:
    from PIL import Image
except ImportError:
    import subprocess, sys

    subprocess.check_call([sys.executable, "-m", "pip", "install", "pillow", "-q"])
    from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
b64 = (ROOT / "branding" / "logo.b64").read_text().strip()
src = Image.open(io.BytesIO(base64.b64decode(b64))).convert("RGBA")


def make_icon(size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    max_inner = int(size * 0.88)
    logo = src.copy()
    logo.thumbnail((max_inner, max_inner), Image.Resampling.LANCZOS)
    x = (size - logo.width) // 2
    y = (size - logo.height) // 2
    canvas.paste(logo, (x, y), logo)
    return canvas


res = ROOT / "app" / "src" / "main" / "res"
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for folder, size in densities.items():
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    icon = make_icon(size)
    icon.save(d / "ic_launcher.png", "PNG")
    icon.save(d / "ic_launcher_round.png", "PNG")
    for name in ("ic_launcher.webp", "ic_launcher_round.webp"):
        p = d / name
        if p.exists():
            p.unlink()
    print("wrote", folder, size)

fg_size = 432
fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
inner = int(fg_size * 0.72)
logo = src.copy()
logo.thumbnail((inner, inner), Image.Resampling.LANCZOS)
x = (fg_size - logo.width) // 2
y = (fg_size - logo.height) // 2
fg.paste(logo, (x, y), logo)
drawable = res / "drawable"
drawable.mkdir(exist_ok=True)
fg.save(drawable / "ic_launcher_foreground.png", "PNG")
bg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 255))
bg.save(drawable / "ic_launcher_background.png", "PNG")
print("adaptive png ok")
