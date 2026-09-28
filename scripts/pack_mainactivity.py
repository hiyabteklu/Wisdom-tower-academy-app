#!/usr/bin/env python3
"""Pack MainActivity.kt into MainActivity.kt.gz.b64 and ma_part*.b64."""
import base64
import gzip
import math
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/example/MainActivity.kt"
scripts = root / "scripts"

if not src.exists():
    print(f"Error: {src} does not exist")
    exit(1)

raw = src.read_bytes()
compressed = gzip.compress(raw)
b64 = base64.b64encode(compressed).decode("ascii")

single = scripts / "MainActivity.kt.gz.b64"
single.write_text(b64)
print(f"Wrote {single} ({len(b64)} chars, raw {len(raw)} bytes)")

# Split into 6 parts
part_count = 6
chunk_size = math.ceil(len(b64) / part_count)
for i in range(part_count):
    part_content = b64[i * chunk_size : (i + 1) * chunk_size]
    p = scripts / f"ma_part{i}.b64"
    p.write_text(part_content)
    print(f"Wrote {p} ({len(part_content)} chars)")

print("Pack complete!")
