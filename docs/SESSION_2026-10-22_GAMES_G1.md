# SESSION 2026-10-22 GAMES G1: level numbers and engines

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/GAMES_COOL_PLAN.md` (progress section is NOT ticked, to save a whole-file resend; this note is the status: G1 DONE, next G2).

DONE (one commit on `main`, NOT compiled, NOT run on a device). Nothing visible changes yet: no screen passes a level, and MEDIUM behaves as before.
- NEW `data/games/GameLevel.kt`: `GameLevel` (EASY/MEDIUM/HARD, `fromId`), `DefenseTuning` and `MatchTuning` (with `forLevel`), and `GameLevelPool.pick` (optional item tags, section 3 of the plan; no caller yet).
- `WisdomDefenseEngine.kt` (whole file resent): NEW last constructor parameter `tuning: DefenseTuning = MEDIUM`. Uses `tuning.speedScale`, `wrongLunge`, `armoredGolem`, `boss`, `hintAfterWrong`, `scoreMultiplier`. New read-only members: `level`, `hint`, `score`. New fields on `WisdomEnemy`: `armor`, `boss` (defaults, so the screen compiles unchanged). `killed` now counts every correct answer (an armor hit too), so the progress bar still reaches `questionsLeft` at the start. Spawning counts HITS needed, not enemies, so every enemy stays killable; the Hard boss takes exactly the last 3 questions (only in sets of 8+). `hearts` and `maxEnemies` are NOT read from the tuning by the engine: the SCREEN passes `maxHearts = tuning.hearts, maxEnemies = tuning.maxEnemies` (G2).
- `MatchEngine.kt` (whole file resent): NEW last constructor parameter `tuning: MatchTuning = MEDIUM`; `fillInitialBoard` takes `tuning.pairsOnBoard` pairs (was 4). New read-only `level`.

CHANGED vs the plan table: Medium keeps up to 4 enemies at once (= today), Easy 2, Hard 5. Score multiplier Medium is 1.5 as in the plan.

NOT DONE here (moved to G2): reading the optional `level` key in `GameSetsRepository.parseSet` (needs a whole-file resend of a 300-line file; nothing waits for tags).

UNSURE / not compiled: (1) no `WisdomEvent` was added on purpose (a new subclass could break an exhaustive `when` in the screen), so an armor hit shows no effect yet; slice W2 adds one. (2) With armor, a correct answer that only cracks armor still plays the screen's normal correct-shot sound/effect and the enemy stays: expected. (3) `values()` is used instead of `entries` (older Kotlin).

NEXT: G2 (level picker screen + hub wiring + Defense passes the tuning to the engine and shows `engine.hint`). Owner has nothing to do for G1 (CI only).
