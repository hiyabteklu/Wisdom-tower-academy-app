#!/usr/bin/env python3
"""E2c-a: ownership ids for ECE Year 1 and Year 2 (4 packages: ece-y1-sem-1/2, ece-y2-sem-1/2).

Owner decision 2026-11-23: ONE purchase opens BOTH semesters of a year (same as Year 3).

What it changes (3 files, folder data/content):
  ContentAccess.kt   : a Year 1/2 path unlocks its own package (before the Year 3 branch, so Year 3 ownership
                       never opens Years 1 and 2); lock scope `ece/y1-sem-1/<course>` for Year 1/2 course paths;
                       ownership helper `ownsPackageId`: owning ece-yN-sem-1 OR ece-yN-sem-2 opens both semesters
                       of that SAME year (N = 1, 2, 3). Year 3 behaves exactly as before; years never mix.
  PackageCheckout.kt : scope `ece/y1-sem-1/...` sells package `ece-y1-sem-1` (same for the other three).
  PaymentMethods.kt  : display names of the 4 packages.
Not touched: Supabase, prices, AdminPackages.kt. The server function content_guard.resource_visible needs the same
per-year rule (separate owner-approved step, see docs/ECE_STATUS.md).
Run twice is safe (marker check). Every anchor must match exactly once, else nothing is written.
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/data/content")

OWNS_OLD_TAIL = (
    '        return unlockIds.any { id ->\n'
    '            id in access.packages ||\n'
    '                (id.startsWith("ece-y3-sem-") && access.packages.any { it.startsWith("ece-y3-sem-") })\n'
    '        }\n'
)
OWNS_NEW_TAIL = '        return unlockIds.any { id -> ownsPackageId(id, access.packages) }\n'

OWNS_FROM_OLD = (
    '        if (!access.enforced) return true\n'
    '        if (ALL_PACKAGES in access.packages) return true\n'
) + OWNS_OLD_TAIL
OWNS_FROM_NEW = (
    '        if (!access.enforced) return true\n'
    '        if (ALL_PACKAGES in access.packages) return true\n'
) + OWNS_NEW_TAIL

OWNS_PAID_OLD = (
    '        if (!signedIn || access == null) return false\n'
    '        if (ALL_PACKAGES in access.packages) return true\n'
) + OWNS_OLD_TAIL
OWNS_PAID_NEW = (
    '        if (!signedIn || access == null) return false\n'
    '        if (ALL_PACKAGES in access.packages) return true\n'
) + OWNS_NEW_TAIL

HELPER_ANCHOR = '    /** The last answer for the student who is signed in now (memory, then the phone), without any network call. */\n'
HELPER_NEW = (
    '    private val RE_ECE_YEAR_PREFIX = Regex("^ece-y[123]-sem-")\n'
    '\n'
    '    /**\n'
    '     * Owns [id], or (ECE Years 1 to 3) owns ANY semester package of the SAME year: one purchase opens both\n'
    '     * semesters. Years never mix (owning Year 3 does not open Year 1 or 2).\n'
    '     */\n'
    '    private fun ownsPackageId(id: String, packages: Set<String>): Boolean {\n'
    '        if (id in packages) return true\n'
    '        val year = RE_ECE_YEAR_PREFIX.find(id)?.value ?: return false\n'
    '        return packages.any { it.startsWith(year) }\n'
    '    }\n'
    '\n'
)

# (file, marker that exists only after the patch, [(anchor, new text, "before" | "after" | "replace")])
EDITS = [
    (
        BASE / "ContentAccess.kt",
        "RE_ECE_YEARS",
        [
            (
                '        base.contains("/special-packages/electrical-computer-engineering") ||\n',
                '        base.contains("/special-packages/") && base.contains("/y1-sem-1") -> listOf("ece-y1-sem-1")\n'
                '        base.contains("/special-packages/") && base.contains("/y1-sem-2") -> listOf("ece-y1-sem-2")\n'
                '        base.contains("/special-packages/") && base.contains("/y2-sem-1") -> listOf("ece-y2-sem-1")\n'
                '        base.contains("/special-packages/") && base.contains("/y2-sem-2") -> listOf("ece-y2-sem-2")\n',
                "before",
            ),
            (
                '    private val RE_ECE = Regex("/special-packages/(?:electrical-computer-engineering|ece)/(sem-[12])/([^/]+)")\n',
                '    private val RE_ECE_YEARS = Regex("/special-packages/(?:electrical-computer-engineering|ece)/(y[12]-sem-[12])/([^/]+)")\n',
                "after",
            ),
            (
                '            scope = "ece/${ece.groupValues[1]}/${ece.groupValues[2]}"\n        }\n',
                '        val eceYear = RE_ECE_YEARS.find(base)\n'
                '        if (eceYear != null) {\n'
                '            if (pkg == null) pkg = "ece-${eceYear.groupValues[1]}"\n'
                '            scope = "ece/${eceYear.groupValues[1]}/${eceYear.groupValues[2]}"\n'
                '        }\n',
                "after",
            ),
            (OWNS_FROM_OLD, OWNS_FROM_NEW, "replace"),
            (OWNS_PAID_OLD, OWNS_PAID_NEW, "replace"),
            (HELPER_ANCHOR, HELPER_NEW, "before"),
        ],
    ),
    (
        BASE / "PackageCheckout.kt",
        '"y1-sem-1" -> "ece-y1-sem-1"',
        [
            (
                '                "sem-2" -> "ece-y3-sem-2"\n',
                '                "y1-sem-1" -> "ece-y1-sem-1"\n'
                '                "y1-sem-2" -> "ece-y1-sem-2"\n'
                '                "y2-sem-1" -> "ece-y2-sem-1"\n'
                '                "y2-sem-2" -> "ece-y2-sem-2"\n',
                "after",
            ),
        ],
    ),
    (
        BASE / "PaymentMethods.kt",
        '"ece-y1-sem-1" -> "ECE Year 1',
        [
            (
                '        "ece-y3-sem-2" -> "ECE Year 3: Semester 2"\n',
                '        "ece-y1-sem-1" -> "ECE Year 1: Semester 1"\n'
                '        "ece-y1-sem-2" -> "ECE Year 1: Semester 2"\n'
                '        "ece-y2-sem-1" -> "ECE Year 2: Semester 1"\n'
                '        "ece-y2-sem-2" -> "ECE Year 2: Semester 2"\n',
                "after",
            ),
        ],
    ),
]

plans = []
for path, mark, edits in EDITS:
    if not path.exists():
        print(f"ERROR: {path} not found"); sys.exit(1)
    src = path.read_text(encoding="utf-8")
    if mark in src:
        print(f"already patched: {path.name}")
        continue
    for anchor, _, _ in edits:
        n = src.count(anchor)
        if n != 1:
            print(f"ERROR: {path.name}: anchor found {n} times: {anchor.strip()[:70]}")
            sys.exit(1)
    plans.append((path, src, edits))

for path, src, edits in plans:
    for anchor, add, where in edits:
        if where == "replace":
            src = src.replace(anchor, add)
        else:
            src = src.replace(anchor, (add + anchor) if where == "before" else (anchor + add))
    path.write_text(src, encoding="utf-8")
    print(f"patched: {path.name}")
