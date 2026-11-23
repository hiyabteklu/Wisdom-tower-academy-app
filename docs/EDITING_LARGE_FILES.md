# EDITING LARGE FILES (one rule, no exceptions)

> Owner rule: never stop at "the file is too big". Never resend a file over 10 KB.

## Which method
| File | Method |
|---|---|
| Under 10 KB, or a NEW file | `push_files` with the whole file. |
| Over 10 KB (`PackagesData.kt`, `HomeScreen.kt`, `MainActivity.kt`, ...) | **Patch script** in `scripts/`, run by the owner with the GitHub Action. |
| `.github/workflows/*` | Chat tool gets 403. Give the owner the text to paste. |

## Patch script, short version
1. Read ONLY the lines near the edit. If you cannot read part of a file, `get_file_contents` it once and copy a short exact anchor (1 to 3 lines). Do not retype the file.
2. Write `scripts/<name>.py` from the template. Each anchor must match exactly once, else exit 1 and change nothing. Re-run safe (marker check). Standard library only.
3. If you have no bash tool, write `UNTESTED` in the log; CI on the patch branch is the test.
4. Push the script (a small new file, always possible).
5. Log it in ONE line in the log table of `docs/ECE_STATUS.md` (script name + branch name). Do NOT touch `docs/PENDING_PATCHES.md` (old, 14 KB, retired).
6. Tell the owner: GitHub > Actions > Apply pending patches > Run workflow > script name + new branch name > wait for green build > test on phone > merge the PR.

## Template
```python
#!/usr/bin/env python3
"""What this patch does and why."""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/ui/packages/PackagesData.kt")
ANCHOR = "    exact line(s) from the file, with indentation\n"
MARK = "text that exists only after the patch"
ADD = "    new line(s)\n"

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched"); sys.exit(0)
if src.count(ANCHOR) != 1:
    print(f"ERROR: anchor found {src.count(ANCHOR)} times"); sys.exit(1)
FILE.write_text(src.replace(ANCHOR, ANCHOR + ADD), encoding="utf-8")
print("patched")
```
Several edits: check every anchor first, write once at the end, change nothing if any anchor fails.

Old `scripts/patch_*.py` files are already applied: do not read or list them.
