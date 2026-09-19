#!/usr/bin/env python3
"""Restore MainActivity.kt before APK build.

Prefers multi-part / single blob if valid; otherwise downloads known-good
source from commit e542 (full WebView shell with offline retry).
"""
import base64
import gzip
import pathlib
import sys
import urllib.request

root = pathlib.Path(__file__).resolve().parents[1]
target = root / "app/src/main/java/com/example/MainActivity.kt"
target.parent.mkdir(parents=True, exist_ok=True)

KNOWN_GOOD = (
    "https://raw.githubusercontent.com/hiyabteklu/Wisdom-tower-academy-app/"
    "e542eeb84d68d003618800f42ae5e5ae6aec02f0/"
    "app/src/main/java/com/example/MainActivity.kt"
)


def write_raw(raw: bytes) -> None:
    target.write_bytes(raw)
    print(f"Restored MainActivity.kt ({len(raw)} bytes)")
    if b"class MainActivity" not in raw:
        print("ERROR: invalid MainActivity", file=sys.stderr)
        sys.exit(1)


def try_blobs() -> bool:
    single = root / "scripts" / "MainActivity.kt.gz.b64"
    if single.exists() and single.stat().st_size > 5000:
        try:
            data = single.read_text().strip()
            try:
                raw = gzip.decompress(base64.urlsafe_b64decode(data))
            except Exception:
                raw = gzip.decompress(base64.b64decode(data))
            if b"class MainActivity" in raw:
                write_raw(raw)
                return True
        except Exception as e:
            print(f"single blob failed: {e}")

    parts = []
    for i in range(6):
        p = root / "scripts" / f"ma_part{i}.b64"
        if not p.exists():
            return False
        parts.append(p.read_text().strip())
    joined = "".join(parts)
    try:
        try:
            raw = gzip.decompress(base64.urlsafe_b64decode(joined))
        except Exception:
            raw = gzip.decompress(base64.b64decode(joined))
        if b"class MainActivity" in raw:
            write_raw(raw)
            return True
    except Exception as e:
        print(f"multi-part blob failed: {e}")
    return False


def from_known_good() -> None:
    print(f"Downloading known-good MainActivity from e542...")
    with urllib.request.urlopen(KNOWN_GOOD, timeout=60) as resp:
        raw = resp.read()
    write_raw(raw)


if try_blobs():
    sys.exit(0)

from_known_good()
