#!/usr/bin/env python3
"""ECE slice E2b: make ECE Year 1 and Year 2 reachable.

1) PackagesData.kt: adds 4 NativePackage entries (ece-y1-sem-1, ece-y1-sem-2, ece-y2-sem-1, ece-y2-sem-2)
   to CATALOG_PACKAGES, just before the Year 3 entries.
2) HomeScreen.kt: adds 2 Home cards ("ECE Year 1", "ECE Year 2") BEFORE the ECE card in HOME_PROGRAMS.

No MainActivity edit. Year 3 entries are not touched. Re-run safe (marker check per file).
Every anchor is checked first; if any needed anchor fails, nothing is written and the script exits 1.
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/ui")
PKG = BASE / "packages" / "PackagesData.kt"
HOME = BASE / "home" / "HomeScreen.kt"

ECE = "/academy/special-packages/electrical-computer-engineering"


def pkg_entry(year: int, sem: int) -> str:
    return (
        "    NativePackage(\n"
        f'        id = "ece-y{year}-sem-{sem}",\n'
        f'        name = "ECE Year {year}: Semester {sem}",\n'
        f'        shortName = "ECE Y{year} S{sem}",\n'
        f'        description = "Electrical and Computer Engineering, Year {year}, Semester {sem}. '
        'Course material written for your department, with the same six learning hubs in every course.",\n'
        f'        path = "{ECE}/y{year}-sem-{sem}",\n'
        f'        assetImage = eceSemesterCover({year}, {sem}),\n'
        '        enrolledLabel = "Special track",\n'
        '        group = "special",\n'
        '        accentColor = WisdomAccentViolet,\n'
        f'        includes = listOf("All Year {year} Semester {sem} engineering courses") + CORE_INCLUDES\n'
        "    ),\n"
    )


def home_card(year: int) -> str:
    return (
        "        ProgramItem(\n"
        f'            id = "ece-y{year}",\n'
        f'            name = "ECE Year {year}",\n'
        f'            path = "{ECE}/y{year}-sem-1",\n'
        f'            assetImage = eceYearCover({year}),\n'
        "            accentColor = WisdomAccentViolet\n"
        "        ),\n"
    )


PKG_ANCHOR = "    // Special packages\n"
PKG_MARK = 'id = "ece-y1-sem-1"'
PKG_ADD = "".join(pkg_entry(y, s) for y in (1, 2) for s in (1, 2))

HOME_ANCHOR = '    get() = listOf(\n        ProgramItem(\n            id = "ece",\n'
HOME_MARK = 'id = "ece-y1",'
HOME_NEW = '    get() = listOf(\n' + home_card(1) + home_card(2) + '        ProgramItem(\n            id = "ece",\n'


def read(p: Path) -> str:
    return p.read_bytes().decode("utf-8")


def write(p: Path, s: str) -> None:
    p.write_bytes(s.encode("utf-8"))


pkg_src = read(PKG)
home_src = read(HOME)
todo = []

if PKG_MARK in pkg_src:
    print("PackagesData.kt: already patched")
else:
    n = pkg_src.count(PKG_ANCHOR)
    if n != 1:
        print(f"ERROR: PackagesData.kt anchor found {n} times"); sys.exit(1)
    todo.append((PKG, pkg_src.replace(PKG_ANCHOR, PKG_ANCHOR + PKG_ADD)))

if HOME_MARK in home_src:
    print("HomeScreen.kt: already patched")
else:
    n = home_src.count(HOME_ANCHOR)
    if n != 1:
        print(f"ERROR: HomeScreen.kt anchor found {n} times"); sys.exit(1)
    todo.append((HOME, home_src.replace(HOME_ANCHOR, HOME_NEW)))

for path, text in todo:
    write(path, text)
    print(f"patched {path.name}")
print("done")
