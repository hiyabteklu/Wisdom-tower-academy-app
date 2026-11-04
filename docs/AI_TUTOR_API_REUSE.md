# AI tutor shared service — reuse policy

Updated: 2026-11-04

## The existing AI API is reusable

The app's shared tutor service is **not reserved for the Notes screen**. Other app features may use it when the owner decides where AI is needed.

- Server endpoint: `/api/ai/tutor` in the connected Wisdom Tower website deployment.
- Android client: `app/src/main/java/com/wisdomtower/academy/data/content/AiTutorRepository.kt`.
- Website route reference in this repository: `website-reference/src/app/api/ai/tutor/route.ts` (the deployed website repository remains the runtime source of truth).
- Request: conversation `messages` plus optional `courseContext`.
- Response: streamed answer chunks, optional follow-up suggestions, then completion.
- The API keys stay on the server. App clients must call the existing route through the repository; never embed provider keys in Android or send them from the client.
- The route currently tries Gemini and Groq model candidates in sequence. Provider/model availability, quotas, rate limits and any billing are controlled by those providers and the deployed server configuration.

## Meaning of “free to use elsewhere”

The existing integration can be reused by other app features without creating a new AI endpoint or treating the Notes tutor as its exclusive owner. This does **not** guarantee that provider inference is free of charge or unlimited. Check provider terms, current quotas, deployment environment variables and runtime logs before enabling a high-volume feature.

## Integration guidance for future features

1. Reuse `AiTutorRepository.stream(history, courseContext)` for conversational, streamed tutoring.
2. Supply only the minimum relevant context needed for that feature. Do not send student personal data unless a separately reviewed feature requires it.
3. Keep credentials server-side and call `/api/ai/tutor`; do not create another client-side key path.
4. For one-shot explanations or summaries, first assess the existing `/api/ai/explain` route and its cache/usage rules rather than routing everything through chat.
5. Add feature-specific limits, caching and abuse protection where appropriate; the tutor route's current implementation does not establish a universal free-use entitlement.

The owner will identify the next places in the app where AI should be used. This document records the reusable service and its boundaries; it does not activate AI in any other feature.
