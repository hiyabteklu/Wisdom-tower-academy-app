# SESSION 2026-10-24: Competition points moved into "Your status"

Not compiled, not seen on a phone. If CI is red, the owner pastes the first line starting with `e: file:///`.

## Owner ask
The Competition points card floated at the top of Profile, loaded later than the rest and pushed the page down. Put it inside My status, make it native, real database data only.

## Cause
`CompetitionPointsCard` called `CompetitionRepository.myPoints()` when it entered the screen and drew nothing until the answer came, then appeared (with `wisdomReveal`) and moved everything below it.

## What changed
- NEW `ui/study/StatusCompetitionPanel.kt`: a panel in the same look as the other status panels. The shell is always drawn. First visit: title, a dash, three grey lines that keep the room. Later visits: a saved copy of the last server answer (SharedPreferences `wt_status_points_cache`, per student) is drawn on the first frame, then replaced by the fresh answer. Offline with a copy: amber line with the save time. Offline with nothing: one line and Retry. Reads again when the internet returns. Shows the 5 newest entries in the server's order, plus "Latest 5 of N entries" when there are more.
- `NativeStatusScreen.kt`: one call `StatusCompetitionPanel()` after "Today & this week" and before "Retention & Exam Accuracy", in the loaded screen and in the offline skeleton.
- `AccountScreen.kt`: the `competition_points` item and its import were removed. Nothing else changed there.
- `CompetitionPointsCard.kt` is no longer used. It is kept in the repo, not deleted.

## Data
Only `get_my_competition_points` (server). No number or title is made up.

## Phone test
- Profile: the gauge is at the top, nothing moves after the page opens.
- My status: the new panel sits between "Today & this week" and "Retention & Exam Accuracy". Open it twice: the second time the numbers are there at once.
- Airplane mode after one online visit: saved numbers with the amber line.
