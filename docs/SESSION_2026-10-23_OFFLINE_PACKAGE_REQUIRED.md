# SESSION 2026-10-23: "Package required" after being offline (ContentAccess.kt)

Owner report (screenshot, 2026-10-23): content that is free for him (open for everyone, or owned by grant) shows "Package required" after he has been offline for a while or killed and reopened the app. Opening data for 5 seconds makes the lock go away. While the app stays open offline it still works. He noticed it after the security work of the last days.

## Cause (read from `ContentAccess.kt`, not run)
The last ownership answer (`ownership`) and the lock rows (`cache`, `courseRowsCache`) were kept only in memory. When the app process is killed (by the student, or by the phone after a while) and the app is opened with no internet:
- the ownership answer is empty, so nothing is owned;
- the lock rows are empty, so a course the admin set to `open` falls back to the static rule, which is RequirePurchase for freshman, grades, remedial and so on;
- result: owned = false + RequirePurchase = "Package required" on every card.
One successful read refills the memory, which is why a few seconds of data fixes it. The security change did not add this; it exposed it because ownership and locks now come from the server (S-A2, 2026-10-20) instead of a free-mode flag in the code.

## Fix (this commit, `ContentAccess.kt` only)
The last good answers are also saved on the phone (SharedPreferences `wt_content_access_cache`):
- ownership: same student only, at most 30 days old (`DISK_MAX_AGE_MS`);
- lock rows of a course, and the course / package mode: no age limit;
- used only when the server cannot be reached and the memory is empty; `staticAccess` and `staticLocks` (first frame) read the saved copies too, so there is no flash of "Package required".
Nothing is invented: only what the server last said for this student. Different student signed in = the saved ownership is ignored.

## Trade-offs (said plainly)
- A package taken away from a student, or a lock added by the admin, shows its old state until the next online read. The server still checks every open (`open_resource`), so a saved "owned" never gives anything the server refuses.
- Older than 30 days offline with no data: ownership is dropped and paid packages close again.

## Not verified
Not compiled, not seen on a phone. Not checked: whether other screens keep their own copy of ownership (only this file was read).

## Owner phone test
Open the app online and open a free course and one you own. Turn on airplane mode, force-stop the app (Settings, Apps, Force stop), reopen: the same cards open, no "Package required". Then sign in with a different student offline-first: the old student's ownership must not apply.
