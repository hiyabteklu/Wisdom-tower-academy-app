# SESSION 2026-11-03 A1: scholar quick actions

Implemented and pushed A1 to main.

- Scholar rows now have Push, Call, and Email actions. Missing phone/email actions are hidden.
- The profile panel also exposes the same actions.
- Push reuses the existing notification API and targets the selected scholar's user ID.
- A zero-device response is shown clearly.
- Call opens the dialer only; it never calls automatically.
- Email opens the mail app with the scholar's address.

No Supabase changes. CI/build has not been run by the agent.

Next: A2 — inquiry quick actions, Reply first.
