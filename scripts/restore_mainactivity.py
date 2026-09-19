#!/usr/bin/env python3
import base64, gzip, pathlib, sys
root = pathlib.Path(__file__).resolve().parents[1]
target = root / "app/src/main/java/com/example/MainActivity.kt"
target.parent.mkdir(parents=True, exist_ok=True)

def write_raw(raw: bytes) -> None:
    target.write_bytes(raw)
    print(f"Restored MainActivity.kt ({len(raw)} bytes)")
    if b"class MainActivity" not in raw:
        print("ERROR: invalid MainActivity", file=sys.stderr)
        sys.exit(1)

# Prefer single gzip+b64 blob if present
single = root / "scripts" / "MainActivity.kt.gz.b64"
if single.exists() and single.stat().st_size > 100:
    raw = gzip.decompress(base64.b64decode(single.read_text().strip()))
    write_raw(raw)
    sys.exit(0)

# Fall back to multi-part
parts = []
n = 6 if (root / "scripts" / "ma_part5.b64").exists() else 3
for i in range(n):
    p = root / "scripts" / f"ma_part{i}.b64"
    if not p.exists():
        print(f"MISSING {p}", file=sys.stderr)
        sys.exit(1)
    parts.append(p.read_text().strip())
raw = gzip.decompress(base64.b64decode("".join(parts)))
write_raw(raw)
