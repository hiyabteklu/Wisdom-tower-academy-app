# SESSION 2026-10-24: Match random arrivals, Defense one-gun cockpit

Nothing here was compiled or run on a phone by the agent. If CI is red, the owner pastes the first line starting with `e: file:///` and that is fixed first.

## Owner asks
1. Match: after a correct pair the same two slots always got the next question and answer. Make it unpredictable.
2. Defense: two guns (a big tower with a crystal that did nothing, a small turret that shot). Make one real gun. Answers 2x2, symmetric on both sides. Make both games look less like a kids HTML page.

## What changed
### Match (`MatchEngine.kt`, `MatchScreen.kt`)
- A matched pair leaves two EMPTY slots (faint pulsing outline). Nothing refills them at once.
- The screen asks the engine for one card at a time, 0.6 to 2.8 s apart (0.25 to 0.9 s when nothing on the board can be matched). The card is any card of the deck (a term or a meaning, any pair) and it takes a random empty slot.
- The first board is still full whole pairs, in random order, and the pairs themselves are shuffled each game.
- Safety: 4 times in 10 the new card is the partner of a card on the board. The last empty slot is never filled in a way that leaves no matchable pair. Every card in the deck has its partner on the board or in the deck.
- Easy hint shows only when the right partner is on the board.
- Cards got a glass look (lighter top, darker bottom, cyan edge).
- Removed from the engine: `lastNewCardIds`, the pair-in-same-slots refill. New: `slots`, `canSpawn`, `hasMatchOnBoard()`, `spawnNext()`.

### Defense (`DefenseCockpit.kt`, `DefenseWall.kt`, `WisdomDefenseScreen.kt`)
- The tower and crystal on the wall are gone. `DefenseWall.kt` draws only the wall (arch, body, battlements, lamps) plus a scrolling floor grid (skipped on low-end phones). `drawDefenseEmpire` in `DefenseArt.kt` is no longer called (left in the file).
- One cannon in the middle of the cockpit: base plate, housing, core, barrel with energy rings and muzzle brake. The barrel is taller than the cockpit and reaches over the wall. It turns toward the nearest enemy (max 25 degrees), kicks back and flashes on a shot. Every shot starts at the muzzle (`defenseCannonTip`).
- Answers: A over B on the left, C over D on the right. Same size, mirrored cut corners, letter chip on the outer edge. Long press still shows the full answer.
- Power-up chips moved from bottom centre to bottom left so the barrel does not cover them.
- Question panel has a gradient and a glowing edge.
- Shared numbers between screen and cockpit: `COCKPIT_TOP_PADDING` (screen uses it as the cockpit top padding), `CANNON_OVERHANG`, `CANNON_PIVOT_Y`, `CANNON_BARREL_LEN` (private in the cockpit file). Change them together or the shot will not leave the muzzle.
- The 2x2 fallback cards for screens narrower than 320 dp show no cannon (the shot still starts at the same point).

## Phone test checklist
- Match: match a pair; two outlines appear; cards come one by one, not together, in different places, some are terms and some meanings. Finish a whole set (Easy, Medium, Hard). Check the board never stays stuck.
- Defense: the barrel is visible above the cockpit, turns, kicks. The shot leaves the muzzle and flies up the lane. Nothing odd at the wall. Answers are two and two. Long press on an answer. Power-up chips are visible at the bottom left (Medium, Hard).
- Low-end phone and reduce-motion: no grid, no pulse, no kick.

## Known risks
- The barrel and the shot are drawn outside their Canvas bounds. If a phone clips them, the barrel top is cut and the shot starts at the field edge.
- Barrel proportions (84 dp long, cockpit 146 dp) are a first guess. Adjust after seeing it.
