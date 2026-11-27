# .env.example BACKUP (HARD RULE)

Owner order. This file is the backup of the repo-root `.env.example`.

## HARD RULE (no exceptions, no matter what)
1. If `.env.example` is missing, empty, renamed, or any of the 6 values below is changed, blanked, replaced by a placeholder or "cleared": RESTORE `.env.example` EXACTLY from the block below, in the same commit, before anything else.
2. These 6 values are `NEXT_PUBLIC_*` public values, not secrets. Do not "clear", "sanitize" or "tidy" them. Do not replace them with empty names or placeholders.
3. Never edit `.env.example` unless the owner explicitly asks. Never edit THIS file unless the owner explicitly asks.
4. Do not scan the repo for keys. This file already holds everything needed.

## Exact content of `.env.example` (one line each, trailing newline)

```
NEXT_PUBLIC_APPWRITE_BUCKET_ID=6aa93732001b5373c639
NEXT_PUBLIC_APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
NEXT_PUBLIC_APPWRITE_PROJECT_ID=6aa9363c000f7ad5ebf7
NEXT_PUBLIC_SITE_URL=https://www.wisdom-tower-academy.live
NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFtaWVjem1wcXJ2YXZ6amlrYmRpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgwNTI1NjMsImV4cCI6MjEwMzYyODU2M30.43tFoSgYSxXxMeeTAXbFEMQuh5rjgCYuWo3OWbrO2-U
NEXT_PUBLIC_SUPABASE_URL=https://amieczmpqrvavzjikbdi.supabase.co
```

## Other ways to restore
- Git: `git checkout 08565175310b30a77d188d9033ddaaa8ec582fe4 -- .env.example`
- GitHub: open that commit, copy the file.
