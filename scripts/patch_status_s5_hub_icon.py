#!/usr/bin/env python3
"""My Status V2 (S5): puts the small gauge icon (StatusSubjectIcon, ui/study/StatusSubjectIcon.kt) on the right of the
one-line hub header of HubItemListScreen.kt, shown only when the student is signed in. Tapping it opens the same subject
status card as "Your status". One anchor (the header item), safe to run twice."""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/ui/study/HubItemListScreen.kt")
MARK = "MYSTATUS-S5"
ANCHOR = '''            item(key = "hub_list_header") {
                Text(
                    text = hubName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WisdomMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
'''
NEW = '''            item(key = "hub_list_header") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = hubName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WisdomMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (loggedIn) StatusSubjectIcon(scopePath = scopePath, title = courseTitle) // MYSTATUS-S5
                }
            }
'''

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do"); sys.exit(0)
n = src.count(ANCHOR)
if n != 1:
    print(f"ERROR: anchor found {n} times (need exactly 1); file not changed"); sys.exit(1)
FILE.write_text(src.replace(ANCHOR, NEW), encoding="utf-8")
print("patched")
