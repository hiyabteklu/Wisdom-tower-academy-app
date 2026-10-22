#!/usr/bin/env python3
"""My Status V2 (S5, both halves) in ONE run, safe to run twice:
1) runs scripts/patch_status_s5_hub_icon.py (gauge icon in the hub header of HubItemListScreen.kt), if that file exists;
2) PackageDetailScreen.kt: the subject cards (Freshman, ECE courses, Grades 9-12, Remedial) call OpenCardWithStatus
   (ui/packages/OpenCardWithStatus.kt) instead of OpenCard, so a small gauge icon sits on the card of a signed-in student."""
import re, subprocess, sys
from pathlib import Path

hub = Path("scripts/patch_status_s5_hub_icon.py")
if hub.exists():
    r = subprocess.run([sys.executable, str(hub)])
    if r.returncode != 0:
        print("ERROR: hub icon patch failed; stopping"); sys.exit(1)

FILE = Path("app/src/main/java/com/wisdomtower/academy/ui/packages/PackageDetailScreen.kt")
src = FILE.read_text(encoding="utf-8")
if "OpenCardWithStatus" in src:
    print("package screen already patched, nothing to do"); sys.exit(0)

rules = [
    (r'OpenCard\(subject\.name, subject\.assetImage\) \{(\s*onOpenSubjectWorkspace\(subject\.name, "Freshman)',
     r'OpenCardWithStatus(subject.name, subject.assetImage, subject.path, isLoggedIn) {\1'),
    (r'OpenCard\(subject\.name, subject\.assetImage\) \{(\s*onOpenSubjectWorkspace\(subject\.name, "Remedial)',
     r'OpenCardWithStatus(subject.name, subject.assetImage, subject.path, isLoggedIn) {\1'),
    (r'OpenCard\(course\.title, course\.assetImage\) \{',
     r'OpenCardWithStatus(course.title, course.assetImage, course.path, isLoggedIn) {'),
    (r'OpenCard\(gSubject\.name, gSubject\.image\) \{',
     r'OpenCardWithStatus(gSubject.name, gSubject.image, gSubject.path, isLoggedIn) {'),
]
for pat, rep in rules:
    found = len(re.findall(pat, src))
    if found != 1:
        print(f"ERROR: pattern {pat[:40]}... found {found} times (need 1); file not changed"); sys.exit(1)
for pat, rep in rules:
    src = re.sub(pat, rep, src)
FILE.write_text(src, encoding="utf-8")
print("patched package screen")
