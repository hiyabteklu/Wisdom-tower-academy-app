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
    p = root / "scripts" / f"ma_part{i}.b64"
    if not p.exists():
        print(f"MISSING {p}", file=sys.stderr)
        sys.exit(1)
    parts.append(p.read_text().strip())
joined = "".join(parts)
try:
    raw = gzip.decompress(base64.urlsafe_b64decode(joined))
except Exception:
    raw = gzip.decompress(base64.b64decode(joined))
write_raw(raw)
