# Exam integrity review dashboard

## Scope
The admin dashboard centralizes the integrity flags already recorded for **ranked competition attempts**. The Android competition exam screen records `app_background` when the app is stopped while a ranked attempt is open and `app_foreground` when the student returns. Other supported flags (such as overlay/copy attempts) are also shown when present.

This is a review queue, not an automatic cheating verdict. Incoming calls, Android permission/system dialogs, accidental gestures, and other legitimate interruptions can create background events. Admins must review context before taking action.

## Admin UI
- Admin route tab: `/admin?tab=exam-integrity`
- Dashboard cards: attempts awaiting review, flagged attempts, leave events, total flags.
- Each attempt shows student, competition, status, score, leave count, total away time, longest away interval, and total flags.
- Event timeline shows each leave/return with timestamps and computed away duration. An unmatched leave is shown as having no return duration.
- Admin can mark an attempt reviewed and optionally save a note. This does not disqualify the student or alter the leaderboard.

## Database
Migration `admin_exam_integrity_dashboard` adds review metadata to `competition_entries` and creates:
- `admin_exam_integrity_report()`: authenticated admin-only report, returns ranked attempts with flags and paired leave/return durations.
- `admin_review_exam_integrity(uuid,text)`: authenticated admin-only review action.

Migration `admin_exam_integrity_review_policy` adds an admin-only UPDATE policy on `competition_entries` so review metadata can be saved under RLS.

## Limits
- This dashboard covers the existing ranked competition integrity event stream. It does not yet claim to track exits from ordinary practice quizzes or the general question-bank exam screen.
- Event logging is best-effort and requires network access at the moment the event is sent.
- The app records app foreground/background lifecycle, not which other app was opened or what the student did while away.
- Away time is calculated from a background event to the next foreground event for the same attempt. If the app never returns, the event remains unmatched.
- No student is automatically disqualified by this feature.
