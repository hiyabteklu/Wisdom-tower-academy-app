# SESSION 2026-10-22 BACK FIX: hub list back jumps two steps

> Repo is PUBLIC: no secrets, no student data.

BUG (owner, weeks old): from a hub item list (books / exams / flashcards) the first back does nothing, the second jumps to the package, the subject list is never seen.

CAUSE (read from code, not yet reproduced on a device): `PackageDetailScreen` has `BackHandler(enabled = stream != null || eceSemester != null)`. It stays composed under the study workspace (`WisdomPushLayer` overlay) and is registered EARLIER than the workspace handler. While the workspace has a view open its own handler wins. On the course page (view == null) the workspace handler is off, so the package handler answered first: it cleared the ECE semester / Entrance stream BEHIND the overlay (nothing visible), and the next back closed the workspace and showed the package's first level.

FIX (one patch run): `scripts/patch_back_workspace_over_package.py` (marker `BACKFIX-S7`, safe twice, tested on a synthetic copy of the real MainActivity): `PackageDetailScreen(backEnabled: Boolean = true)`; MainActivity passes `backEnabled = activeWorkspace == null`. Now back goes: item list -> course page (hub cards) -> subject list (ECE semester courses / Entrance subjects) -> semester or stream cards -> package list.

UNSURE: grades / Freshman / Remedial packages have no drill-down, so their package handler is already off; if the double step still happens there, the next suspect is the Learning tab entry (`onOpenHub` opens the workspace straight into a hub via `initialHub`; back then shows the hub cards, then closes the workspace to the Learning tab, which is by design). Report which package and which entry point if it persists.

NOT compiled. Owner: Actions > Apply pending patches > script `scripts/patch_back_workspace_over_package.py`, branch `fix/back-workspace-over-package`, green build, merge PR, test.
