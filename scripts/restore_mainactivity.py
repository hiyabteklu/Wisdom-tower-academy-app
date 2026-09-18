#!/usr/bin/env python3
"""Restore MainActivity.kt from known-good commit (pairs with simple exit dialog)."""
import pathlib
import urllib.request

root = pathlib.Path(__file__).resolve().parents[1]
target = root / "app" / "src" / "main" / "java" / "com" / "example" / "MainActivity.kt"
url = (
    "https://raw.githubusercontent.com/hiyabteklu/Wisdom-tower-academy-app/"
    "b598a3d65251ff069a3a09378bb83debf01115cb/"
    "app/src/main/java/com/example/MainActivity.kt"
)
print("Downloading", url)
data = urllib.request.urlopen(url, timeout=60).read()
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(data)
print(f"Restored MainActivity.kt ({len(data)} bytes)")
