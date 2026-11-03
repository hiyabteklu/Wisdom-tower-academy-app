# SESSION 2026-11-03 B1: taller material cards

Implemented and pushed B1 to main.

- Rebuilt the native hub material rows (Books, Notes, Flashcards, Question Banks, Exams, and other hub lists) as taller glass cards.
- Each card has a type chip, a title (up to two lines), a description (up to three lines), and a footer for file size, question/card count, lock status, or saved-offline status. Existing progress / download control and right-side status icon remain.
- Added optional `description` text to `learning_resources`, included it in the student light-list response and admin-only view, and exposed it in the native Content library editor.
- Existing rows without a description use a useful fallback built from chapter, count, and size; if none exist, the card says to open the material to study.
- The Learning screen's package tiles are a separate component, not the material list card, so they were not changed.

No APK build was run by the agent. Test Day and Night themes, all five core hubs, missing descriptions, saved cards, and progress states on a phone.

Next: F1 — dynamic cached home welcome banner with bundled fallback.
