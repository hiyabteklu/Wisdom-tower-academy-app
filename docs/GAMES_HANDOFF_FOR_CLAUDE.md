# GAMES HANDOFF FOR CLAUDE

## Files created or changed
- data/games: GameQuestion.kt, GameQuestionLoader.kt, GameStore.kt
- assets/notes/game_question.html
- ui/games/common: GameQuestionCard.kt, GameTheme.kt, GameClock.kt, GameAudio.kt, GameHaptics.kt, GameParticles.kt, GameButtons.kt, GamePauseOverlay.kt

## Phase status
- G0 DONE, G1 DONE, G2 DONE, G3 Feel kit DONE
- G4–G9 open

## UNSURE
(none yet)

## Known risks
- GameAudio silent on init failure (by design).
- Plan stub on main; full plan parent 61a2b34.

## CI errors seen (Claude fills this)
(none yet)

## Compile notes
- GameClock: androidx.lifecycle.compose.LocalLifecycleOwner
- GameButtons: clickable, background, border, clip, padding each imported
