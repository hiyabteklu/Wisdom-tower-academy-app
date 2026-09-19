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

parts = []
for i in range(6):
    if i == 2:
        a = root / "scripts" / "ma_part2a.b64"
        b = root / "scripts" / "ma_part2b.b64"
        if a.exists() and b.exists():
            parts.append(a.read_text().strip() + b.read_text().strip())
            continue
    p = root / "scripts" / f"ma_part{i}.b64"
    if not p.exists():
        print(f"MISSING {p}", file=sys.stderr)
        sys.exit(1)
    parts.append(p.read_text().strip())
raw = gzip.decompress(base64.b64decode("".join(parts)))
write_raw(raw)
