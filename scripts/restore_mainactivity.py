#!/usr/bin/env python3
import base64, gzip, pathlib
root = pathlib.Path(__file__).resolve().parents[1]
parts_dir = root / "scripts"
parts = []
for i in range(4):
    p = parts_dir / f"ma_part{i}.b64"
    if p.exists():
        parts.append(p.read_text().strip())
blob = parts_dir / "MainActivity.kt.gz.b64"
if blob.exists() and not parts:
    parts = [blob.read_text().strip()]
if not parts:
    print("No restore blob; skip")
    raise SystemExit(0)
b64 = "".join(parts)
data = gzip.decompress(base64.b64decode(b64))
target = root / "app/src/main/java/com/example/MainActivity.kt"
target.write_bytes(data)
print(f"Restored MainActivity.kt ({len(data)} bytes)")
