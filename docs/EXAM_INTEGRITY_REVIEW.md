# Exam integrity review dashboard

## Where it lives
The review workspace is in the website admin console under **Exam Integrity** at /admin?tab=exam-integrity. There is not currently a native admin screen inside the Android app.

## What is recorded
While a ranked competition attempt is in progress, the Android app records:
- app_background when the app leaves the foreground;
- app_foreground when the student returns, including the measured away duration;
- other supported integrity events such as overlay and copy attempts.

Practice attempts are not included. Recording is asynchronous and should never block or pause the exam.

## Admin view
- The admin tab badge counts distinct students with unreviewed flagged attempts.
- The dashboard shows students, exam/competition, attempt status and score.
- Each student row shows leave count, total away time, longest absence and the activity-flag count.
- The latest four events are visible directly on each row, with timestamps in Ethiopia time (EAT). Longer timelines can be expanded.
- Admins can mark an attempt reviewed and save an optional note.
- Review status is attached to the attempt; it does not automatically disqualify the student.

## Metric definitions
- Leave count is the greater of the recorded background and foreground event counts. This accounts for cases where one side of a lifecycle event pair was not delivered.
- Total away time and longest absence are calculated from the away_ms values sent with return events. A leave without a corresponding return remains visible as an unmatched exit, but its duration cannot be known.
- A background event is not proof of cheating. Calls, notifications, system dialogs, accessibility actions and accidental gestures can also move an app to the background. Admins must review context before disqualification.

## Backend
- public.competition_flags is written through public.report_competition_flag and is admin-readable only.
- public.admin_exam_integrity_report() returns ranked attempts with flags and their event timeline.
- public.admin_review_exam_integrity(uuid, text) records the admin review timestamp and optional note.
- Migration exam_integrity_leave_metrics fixes leave counts for unmatched lifecycle events and derives aggregate away time from return-event durations.

## Verification still required
1. Test a ranked exam on a test account: leave the app, return, and verify both events and duration.
2. Test a simulated missing background event and verify the return still contributes to the leave count.
3. Confirm the tab badge decrements after marking an attempt reviewed.
4. Confirm review notes and EAT timestamps display correctly on the live admin site.
5. Do not treat any flag as automatic proof of cheating.
