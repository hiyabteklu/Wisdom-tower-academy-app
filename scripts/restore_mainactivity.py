#!/usr/bin/env python3
"""Restore MainActivity.kt from gzip+base64 blob."""
import base64, gzip, pathlib
root = pathlib.Path(__file__).resolve().parents[1]
blob = root / "scripts" / "MainActivity.kt.gz.b64"
target = root / "app/src/main/java/com/example/MainActivity.kt"
if not blob.exists():
    print("No blob; skip")
    raise SystemExit(0)
data = gzip.decompress(base64.b64decode(blob.read_text().strip()))
target.write_bytes(data)
print(f"Restored MainActivity.kt ({len(data)} bytes)")
