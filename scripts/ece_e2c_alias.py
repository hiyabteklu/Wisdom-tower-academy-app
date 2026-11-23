#!/usr/bin/env python3
"""E2c-c: alias known Year 1/2 ECE courses to their freshman source content.

The app owns Years 1 and 2 separately. This only changes content lookup:
ece/y1-sem-N/<ece-slug> is read from freshman/<freshman-slug>. Unknown
Year 2 engineering courses keep their original ECE scope and stay empty.

Run once through the patch branch Action. Every anchor must match exactly
once; otherwise the script exits before writing.
"""
import sys
from pathlib import Path

CONTENT = Path("app/src/main/java/com/wisdomtower/academy/data/content")

REPO_ANCHOR = '''    private val ECE_SCOPE = Regex("^ece/(sem-[12])/([^/]+)$")
'''

REPO_ADD = '''    private val ECE_SCOPE = Regex("^ece/(sem-[12])/([^/]+)$")
    private val ECE_YEAR_SCOPE = Regex("^ece/(y[12]-sem-[12])/([^/]+)$")
    private val ECE_FRESHMAN_ALIASES = mapOf(
        "flen1011" to "english-1",
        "loct1011" to "logic",
        "math1011" to "math-natural",
        "phys1011" to "physics",
        "gees1011" to "geography",
        "psch1011" to "psychology",
        "spsc1011" to "physical-fitness",
        "flen1012" to "english-2",
        "anth1012" to "anthropology",
        "math1042" to "applied-math-1",
        "mgmt1012" to "entrepreneurship",
        "emte1012" to "emerging-technology",
        "mcie1012" to "civics",
        "eceg1052" to "cpp-programming",
        "hist1012" to "history",
        "snie1012" to "inclusiveness",
        "gltr1012" to "global-trends",
    )

'''

edits = [
    (
        CONTENT / "LearningContentRepository.kt",
        "ECE_YEAR_SCOPE",
        [
            (REPO_ANCHOR, REPO_ADD, "replace"),
            (
                '''            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                setOf(scopePath, "ece/$other/${ece.groupValues[2]}")
            }
''',
                '''            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                setOf(scopePath, "ece/$other/${ece.groupValues[2]}")
            }
            ECE_YEAR_SCOPE.matchEntire(scopePath)?.let { year ->
                ECE_FRESHMAN_ALIASES[year.groupValues[2]]?.let { freshman ->
                    setOf("freshman/$freshman")
                }
            } ?: setOf(scopePath)
''',
                "replace",
            ),
            (
                '''            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                "in.(\"$scopePath\",\"ece/$other/${ece.groupValues[2]}\")"
            }
            scopePath == "freshman/math-natural" || scopePath == "freshman/mathematics" ->
                "in.(\"freshman/math-natural\",\"freshman/mathematics\")"
            else -> "eq.$scopePath"
''',
                '''            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                "in.(\"$scopePath\",\"ece/$other/${ece.groupValues[2]}\")"
            }
            scopePath == "freshman/math-natural" || scopePath == "freshman/mathematics" ->
                "in.(\"freshman/math-natural\",\"freshman/mathematics\")"
            ECE_YEAR_SCOPE.matchEntire(scopePath)?.let { year ->
                ECE_FRESHMAN_ALIASES[year.groupValues[2]]?.let { freshman ->
                    "eq.freshman/$freshman"
                }
            } ?: "eq.$scopePath"
''',
                "replace",
            ),
        ],
    ),
]

if __name__ == "__main__":
    plans = []
    for path, marker, file_edits in edits:
        if not path.exists():
            print(f"ERROR: {path} not found")
            sys.exit(1)
        src = path.read_text(encoding="utf-8")
        if marker in src:
            print(f"already patched: {path.name}")
            continue
        for anchor, _, _ in file_edits:
            count = src.count(anchor)
            if count != 1:
                print(f"ERROR: {path.name}: anchor found {count} times")
                sys.exit(1)
        plans.append((path, src, file_edits))

    for path, src, file_edits in plans:
        for anchor, replacement, where in file_edits:
            if where == "replace":
                src = src.replace(anchor, replacement)
            elif where == "before":
                src = src.replace(anchor, replacement + anchor)
            else:
                src = src.replace(anchor, anchor + replacement)
        path.write_text(src, encoding="utf-8")
        print(f"patched: {path.name}")
