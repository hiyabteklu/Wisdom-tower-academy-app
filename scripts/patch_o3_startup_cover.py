#!/usr/bin/env python3
"""O3 (docs/ONBOARDING_STARTUP_PLAN.md): full-screen start-up cover in MainActivity.kt.

Owner bug (2026-10-23): at app start the custom loader spins on an EMPTY screen while the top bar and the bottom nav are
already visible, and taps already reach the tabs and the WebView underneath.

Cause (verified in code): `MainScreen` called `onReady()` in its first LaunchedEffect, so the system splash was released
at once, and the only loader is drawn INSIDE the Scaffold content (zIndex 95), under/around the bars. It has no touch
blocking either.

What this patch does (4 edits, all in MainActivity.kt):
1. import for `pointerInput` (needed to swallow touches).
2. `onReady()` now runs after the first frame has gone through (`withFrameNanos`), not before. The cover is composed in the
   very first composition (it is visible from the start because `isInitialLoading` starts true), so the first frame the
   student sees already has the cover.
3. A full-screen COVER as the LAST child of the root Box of `MainScreen` (above top bar, bottom nav, overlays): the chosen
   app background (`appBackdrop()`, same look as the old start-up loader, owner decision BG-LOADING), a BIG loader
   (160 dp, the same gear rings), and a pointer handler that consumes every touch. Visible while `isInitialLoading`;
   `isInitialLoading` already ends when the first page rendered or at the existing 3.5 s fail-safe, so the cover can never
   stay. Fades out in 220 ms; no enter animation (it must be there from the first frame).
4. After onboarding (first run) the WebView did not exist yet, so the same empty gap showed again: when the student
   finishes onboarding `isInitialLoading` is set to true again, which brings the cover back until the first page renders
   (the 3.5 s fail-safe applies again).

Not touched: the old in-content loader (still used for tab switching and the study loader), the onboarding screen, the
system splash theme. Safe to run twice (marker check). Every anchor must match exactly once, otherwise nothing is written.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "O3: start-up cover"

EDITS = []


def edit(anchor, replacement):
    EDITS.append((anchor, replacement))


# 1. import ------------------------------------------------------------------------------------------------------------
edit(
    "import androidx.compose.ui.zIndex\n",
    "import androidx.compose.ui.zIndex\n"
    "import androidx.compose.ui.input.pointer.pointerInput\n",
)

# 2. release the system splash after the first frame, not before ------------------------------------------------------
edit(
    "        FcmTokenRegistrar.checkAndRegisterToken(context)\n"
    "        onReady()\n"
    "        delay(MIN_SPLASH_DISPLAY_MS)\n",
    "        FcmTokenRegistrar.checkAndRegisterToken(context)\n"
    "        // O3: start-up cover. The cover is composed in the first composition; wait for one frame, then release the\n"
    "        // system splash, so the first frame the student sees already has the cover over the bars.\n"
    "        androidx.compose.runtime.withFrameNanos { }\n"
    "        onReady()\n"
    "        delay(MIN_SPLASH_DISPLAY_MS)\n",
)

# 3. the cover itself: last child of the root Box ----------------------------------------------------------------------
COVER = (
    "\n"
    "        // O3: start-up cover (marker: " + MARK + "). Full screen, above the top bar, the bottom nav and every overlay.\n"
    "        // Draws the chosen app background, a big loader, and swallows ALL touches so nothing underneath can be tapped\n"
    "        // or scrolled while the app starts. Ends when isInitialLoading ends (first page rendered, or the 3.5 s\n"
    "        // fail-safe), then fades out in 220 ms. No enter animation: it must be there from the first frame.\n"
    "        AnimatedVisibility(\n"
    "            visible = isInitialLoading,\n"
    "            enter = androidx.compose.animation.EnterTransition.None,\n"
    "            exit = fadeOut(tween(220, easing = FastOutSlowInEasing)),\n"
    "            modifier = Modifier\n"
    "                .fillMaxSize()\n"
    "                .zIndex(1000f)\n"
    "        ) {\n"
    "            Box(\n"
    "                modifier = Modifier\n"
    "                    .fillMaxSize()\n"
    "                    .appBackdrop()\n"
    "                    .pointerInput(Unit) {\n"
    "                        awaitPointerEventScope {\n"
    "                            while (true) {\n"
    "                                val event = awaitPointerEvent()\n"
    "                                event.changes.forEach { it.consume() }\n"
    "                            }\n"
    "                        }\n"
    "                    },\n"
    "                contentAlignment = Alignment.Center\n"
    "            ) {\n"
    "                FuturisticGearRings(\n"
    "                    size = 160.dp,\n"
    "                    brandSize = 92.dp,\n"
    "                    numTicks = 16,\n"
    "                    tickInnerRatio = 0.68f,\n"
    "                    tickOuterRatio = 0.88f,\n"
    "                    tickWidth = 4.dp\n"
    "                )\n"
    "            }\n"
    "        }\n"
)
END_ANCHOR = (
    "                    activity?.finish()\n"
    "                }\n"
    "            )\n"
    "        }\n"
    "    }\n"
    "}\n"
)
edit(
    END_ANCHOR,
    "                    activity?.finish()\n"
    "                }\n"
    "            )\n"
    "        }\n"
    + COVER +
    "    }\n"
    "}\n",
)

# 4. bring the cover back after onboarding (the WebView does not exist before that) ------------------------------------
edit(
    "                setOnboardingCompleted(context)\n"
    "                showOnboarding = false\n"
    "                requestNotificationPermission()\n",
    "                setOnboardingCompleted(context)\n"
    "                showOnboarding = false\n"
    "                // O3: start-up cover again until the first page renders (fail-safe 3.5 s applies again)\n"
    "                isInitialLoading = true\n"
    "                requestNotificationPermission()\n",
)

# ---------------------------------------------------------------------------------------------------------------------
src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)

out = src
for i, (anchor, replacement) in enumerate(EDITS):
    n = out.count(anchor)
    if n != 1:
        print(f"ERROR: edit #{i} anchor found {n} times (need exactly 1); file not changed")
        print(anchor[:200])
        sys.exit(1)
    out = out.replace(anchor, replacement)

FILE.write_text(out, encoding="utf-8")
print(f"patched ({len(EDITS)} edits)")
