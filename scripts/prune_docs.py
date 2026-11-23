#!/usr/bin/env python3
"""Delete old logs and built plans from docs/ and a few root docs.
Keeps only the files in KEEP / KEEP_PREFIX. Subfolders of docs/ are untouched.
Safe to run twice. Git history keeps every deleted file."""
from pathlib import Path

KEEP = {
    "ECE_STATUS.md", "EDITING_LARGE_FILES.md", "APPLY_PATCHES_WORKFLOW.md",
    "PENDING_PATCHES.md", "SUPABASE_NOTES.md", "BUILD_PITFALLS.md",
    "BUILD_PITFALLS_M7_NOTE.md", "RELEASE_WORKFLOW.md", "CONTENT_SHAPES.md",
    "OPEN_QUESTIONS.md", "PARKED_2026-10-21.md", "ECE_EXPANSION_SLICES.md",
    "SESSION_2026-11-23_ECE_EXPANSION_START.md", "AGENT_QUOTA_PROTOCOL.md",
}
KEEP_PREFIX = ("OWNER_RULE_",)
ROOT_DELETE = ["STATUS.md", "DRAFT_NOTIFICATION.md", "WEBSITE_CHANGES.md"]

n = 0
for f in sorted(Path("docs").iterdir()):
    if not f.is_file():
        continue
    if f.name in KEEP or f.name.startswith(KEEP_PREFIX):
        continue
    f.unlink()
    n += 1
for name in ROOT_DELETE:
    p = Path(name)
    if p.is_file():
        p.unlink()
        n += 1
print(f"deleted {n} files")
