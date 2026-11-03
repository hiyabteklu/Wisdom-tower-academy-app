# SESSION 2026-11-03 A2: inquiry quick actions

Implemented and pushed A2 to main.

- Inquiry rows and detail panels now expose Reply first, then Push and Email.
- Reply opens the mail app with a prefilled subject.
- Push first looks up the inquiry email in Academy profiles; it is only sent when a registered Academy account is found.
- Inquiry records contain no phone number or user ID, so Call is intentionally omitted.
- No Supabase schema change. CI/build has not been run by the agent.

Next: C1 — persist material file sizes alongside material records.
