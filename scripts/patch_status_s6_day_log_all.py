#!/usr/bin/env python3
"""My Status V2, S6b + S6d in ONE run, safe to run twice: first the day log WRITE (patch_status_s6b_day_log_write.py),
then the chart + goal ring wiring (patch_status_s6d_day_panel.py). Run this one script from Actions > Apply pending patches."""
import subprocess, sys
from pathlib import Path

for name in ("patch_status_s6b_day_log_write.py", "patch_status_s6d_day_panel.py"):
    p = Path("scripts") / name
    if not p.exists():
        print(f"ERROR: {p} missing; stopping"); sys.exit(1)
    if subprocess.run([sys.executable, str(p)]).returncode != 0:
        print(f"ERROR: {name} failed; stopping"); sys.exit(1)
print("S6b + S6d applied")
