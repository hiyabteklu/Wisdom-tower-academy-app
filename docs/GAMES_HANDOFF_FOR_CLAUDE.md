# GAMES HANDOFF FOR CLAUDE

> Plan: `docs/GAMES_NATIVE_PLAN.md`. Repo PUBLIC.

## Files created or changed
- `app/src/main/java/com/wisdomtower/academy/data/games/GameQuestion.kt` — model + parse + fair shuffle
- `app/src/main/java/com/wisdomtower/academy/data/games/GameQuestionLoader.kt` — getResource door + LRU cache
- `app/src/main/java/com/wisdomtower/academy/data/games/GameStore.kt` — SharedPreferences progress
- `app/src/main/assets/notes/game_question.html` — setCard, hidden, reveal, solution, CSS shake/entrance
- `app/src/main/java/com/wisdomtower/academy/ui/games/common/GameQuestionCard.kt` — WebView wrapper (CompetitionQuestionView pattern)
- `docs/GAMES_HANDOFF_FOR_CLAUDE.md` — this file

## Phase status
- G0 Recon: DONE
- G1 Data kit: DONE
- G2 Question card: DONE
- G3–G9: open

## UNSURE
(none yet)

## Known risks
- Plan file on main may still be a short stub from G0; section 15 signatures are in prior handoff commits. Parent plan SHA: 61a2b34.

## CI errors seen (Claude fills this)
(none yet)

## Compile notes for Claude
- GameQuestionCard: same WebView setup as CompetitionQuestionView (blockNetworkLoads, WTBridge, JSONObject.quote).
- game_question.html loads katex + richtext from same assets/notes/ relative paths as competition.html.
- GameCardState.revealCorrect null → JSON null reveal; non-null → {correct, solution}.
