# A4 TEST MATRIX (written 2026-10-20 BEFORE testing; expected results first)

Purpose: prove that lock modes, Paid mode, purchases and grants behave the same in the app lists and on the server.
Rules the app follows (from `ContentAccess.kt` and the database function `content_guard.resource_visible`):
- The MOST SPECIFIC lock row wins: material, chapter, hub, subject (scope), package, then the built-in rule.
- `open`: any signed-in student opens it. `coming_soon` and `locked`: closed for everybody, owners too (same look: lock + Coming soon).
  `require_purchase`: opens only for an owner (purchase, grant, or an all-packages grant) while Paid mode is ON.
- Paid mode = lock row `system:enforce`: `locked` = ON, `open` = OFF. OFF means every signed-in student opens every require_purchase item.
- Signed out on a paid course: the app's Sign-in message (not Package required).
- The two ECE semesters count as ONE package. An all-packages grant (`__all__`) owns everything.
- Server unreachable and no earlier answer: paid packages stay closed. Never opens by mistake.

## Today's lock rows in the database (read 2026-10-20, Paid mode is ON)
| Row | Mode |
|---|---|
| system:enforce | locked (Paid mode ON) |
| package:freshman | require_purchase |
| package:ece-y3-sem-1 | require_purchase |
| package:ece-y3-sem-2 | require_purchase |
| scope:freshman/math-natural | open |
| scope:ece/sem-1/meng3052 | open |
| hub:freshman/math-natural:exams | open |
| hub:ece/sem-1/meng3052:exams | open |
| chapter:freshman/psychology:life-savers:1 | locked |
| item:23eed181-ab12-465c-831d-f4b5490dc6f3 | locked |
| item:a237716b-2bde-44f0-ae3d-c904523409a1 | locked |

No row for the Grade 9 to 12, Entrance, Remedial, COC and UAT packages: the built-in rule applies (paid), GAT and Exit Exam are Coming soon.

## Four test students (use two real accounts and change their rows between rounds)
- S0: signed in, NO purchase and NO grant.
- SG: signed in, a GRANT for `freshman` only (Grants screen).
- SE: signed in, an ENROLLMENT for `ece-y3-sem-1` only (or a grant for it).
- SO: signed OUT.

## Round 1: Paid mode ON (today's state)
| # | Where in the app | S0 | SG (freshman grant) | SE (ECE sem-1) | SO |
|---|---|---|---|---|---|
| 1 | Freshman, a subject with no row of its own (for example Physics) | Package required | opens | Package required | Sign-in message |
| 2 | Freshman Math (Natural), any hub | opens | opens | opens | Sign-in message |
| 3 | Freshman Math (Natural), Exams hub | opens | opens | opens | Sign-in message |
| 4 | Freshman Psychology, Life Savers, Chapter 1 materials | Coming soon (lock) | Coming soon (lock, owner too) | Coming soon (lock) | Sign-in message |
| 5 | Freshman Psychology, Life Savers, Chapter 2 materials | Package required | opens | Package required | Sign-in message |
| 6 | The two materials with an `item:` lock | Coming soon (lock) | Coming soon (lock) | Coming soon (lock) | Sign-in message |
| 7 | ECE Year 3 Sem 1, subject MENG3052 | opens | opens | opens | Sign-in message |
| 8 | ECE Year 3 Sem 1, any other subject | Package required | Package required | opens | Sign-in message |
| 9 | ECE Year 3 Sem 2, any subject | Package required | Package required | opens (the two semesters count as one) | Sign-in message |
| 10 | Grade 9, any subject (no row, built-in paid rule) | Package required | Package required | Package required | Sign-in message |
| 11 | GAT or Exit Exam | Coming soon | Coming soon | Coming soon | Coming soon |

Server check for the same rows (downloads must agree with the lists): in rows 1, 5, 8, 9, 10 the S0 student must be REFUSED when opening a
book or file by hand; in rows 2, 3, 7 it must open. A list that says open while the download refuses (or the other way round) is a bug.

## Round 2: grant and revoke at run time (Paid mode ON)
| # | Action | Expected |
|---|---|---|
| 12 | Admin grants S0 the `freshman` package (Grants screen) | The Grants screen says "<email> now has access to Freshman package". S0 opens row 1 within about 30 seconds of reopening the app (or returning to it). |
| 13 | Admin grants S0 "All packages" | S0 opens rows 1, 5, 8, 9, 10. Rows 4 and 6 (hard locks) stay closed. Row 11 stays Coming soon. |
| 14 | Admin revokes the grant | S0 gets Package required again within about 30 seconds. |
| 15 | Admin sets `package:freshman` to Open | Row 1 opens for S0 with no grant. Set it back to Owners only afterwards. |

## Round 3: Paid mode OFF (set `system:enforce` to open), then set it back ON
| # | Where | S0 | SO |
|---|---|---|---|
| 16 | Rows 1, 5, 8, 9, 10 | open | Sign-in message |
| 17 | Rows 4, 6, 11 (hard locks and Coming soon) | still closed | still closed |
| 18 | Grants screen | shows the yellow note "Paid mode is OFF" | |

## Round 4: network
| # | Situation | Expected |
|---|---|---|
| 19 | S0 on a fresh install, airplane mode, open Freshman Physics | Package required (never opens by mistake) |
| 20 | SG opened Freshman Physics once online, then airplane mode, reopen | still opens (the last answer is reused) |
| 21 | Another student signs in on the same phone | does not inherit the first student's answer |

## How to record the result
One line per row in `docs/A4_RESULTS.md` (to be created at test time): row number, student, what the app showed, PASS or FAIL, and for a FAIL the
package name and the student's e-mail (then the database rows are read directly).
