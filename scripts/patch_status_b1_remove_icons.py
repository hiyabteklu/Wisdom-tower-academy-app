#!/usr/bin/env python3
"""My Status V2, slice B1 (docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md): REMOVES the small gauge icon that slice S5 put
(1) in the one-line hub header of HubItemListScreen.kt (marker MYSTATUS-S5) and
(2) on the subject cards of PackageDetailScreen.kt (OpenCardWithStatus -> back to OpenCard).
Each part does nothing if it was never applied, so it is safe to run twice or on a branch that never had S5.
Files ui/study/StatusSubjectIcon.kt and ui/packages/OpenCardWithStatus.kt stay (unused, harmless; B2 reuses the loader)."""
import re, sys
from pathlib import Path

HUB = Path("app/src/main/java/com/wisdomtower/academy/ui/study/HubItemListScreen.kt")
PKG = Path("app/src/main/java/com/wisdomtower/academy/ui/packages/PackageDetailScreen.kt")

HUB_WITH_ICON = '''            item(key = "hub_list_header") {
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
HUB_ORIGINAL = '''            item(key = "hub_list_header") {
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

def part_hub():
    if not HUB.exists():
        print("hub: file not found, skipped"); return True
    src = HUB.read_text(encoding="utf-8")
    if "MYSTATUS-S5" not in src:
        print("hub: icon not present, nothing to do"); return True
    n = src.count(HUB_WITH_ICON)
    if n != 1:
        print(f"ERROR hub: icon block found {n} times (need 1); file not changed"); return False
    HUB.write_text(src.replace(HUB_WITH_ICON, HUB_ORIGINAL), encoding="utf-8")
    print("hub: icon removed"); return True

def part_pkg():
    if not PKG.exists():
        print("package: file not found, skipped"); return True
    src = PKG.read_text(encoding="utf-8")
    if "OpenCardWithStatus" not in src:
        print("package: icon not present, nothing to do"); return True
    pat = re.compile(r'OpenCardWithStatus\(([^,()]+), ([^,()]+), [A-Za-z]+\.path, isLoggedIn\) \{')
    out, n = pat.subn(r'OpenCard(\1, \2) {', src)
    if "OpenCardWithStatus" in out:
        print("ERROR package: an OpenCardWithStatus call has an unexpected shape; file not changed"); return False
    PKG.write_text(out, encoding="utf-8")
    print(f"package: {n} cards back to OpenCard"); return True

ok_hub = part_hub()
ok_pkg = part_pkg()
sys.exit(0 if ok_hub and ok_pkg else 1)
