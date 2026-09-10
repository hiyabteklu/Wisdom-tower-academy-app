# Data Status Report (2026-09-10)

## Confirmed Root Cause

When querying the live Supabase project with the **anon key** used by the Android app:

```
GET /rest/v1/learning_resources
```

returns **0 rows** (`content-range: */0`).

- `catalog_items` works correctly and returns all packages.
- `learning_resources` is completely empty for the public/anon role.

This is why the app always shows **“Curriculum Coming Soon”** even for `freshman` and `ece-y3-sem-1`.

## Why Claude reported content earlier

Claude likely used a higher-privilege connection (service role or authenticated session). The rows exist in the database but are **not visible** to the anon key due to Row Level Security (RLS) policies.

## What the app currently does

1. Correctly queries `catalog_items` → packages appear.
2. Correctly queries `learning_resources?package_id=eq.<id>` → receives empty array.
3. Shows the empty state because the array is empty.

## Required fix (Supabase side)

You (or an admin) must update the RLS policy on `learning_resources` so that either:

- The `anon` role can `SELECT` rows where `published = true`, **or**
- Authenticated users (and the Scholar session) receive a proper JWT that passes the current policy.

Until that is fixed on the Supabase dashboard, the Android app cannot display real notes/PDFs/flashcards no matter how many times the client code is changed.

## Temporary client improvements (already planned)

- Clearer empty-state message explaining the auth/RLS issue
- Debug banner showing the exact number of rows returned by the server
- Better handling of Scholar Mode token

---

**Next action for owner:**  
Go to Supabase Dashboard → Authentication / Database → Policies on table `learning_resources` and allow the anon (or authenticated) role to read published content.
