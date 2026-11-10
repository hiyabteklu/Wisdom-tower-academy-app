# DRAFT NOTIFICATION: instructions for any agent

**Trigger:** the owner says "draft notification" (or "draft a notification", "send me a draft notification", "put it in my composer").

**What it means:** write a finished push/in-app notification and **save it as a row in the Supabase table `public.notification_drafts`**. It then appears in the admin composer under **"Claude drafts"**, where the owner taps **Load into composer**, picks the audience and time, and presses Send.

**You never send the notification.** You only save the draft.

---

## DO NOT

- Do NOT just paste the text in chat. Do NOT use a "message composer" or copy card. That is not what "draft notification" means. The deliverable is a database row.
- Do NOT send, schedule or broadcast anything. Do NOT touch the send/schedule code or the `notifications` table.
- Do NOT edit or delete older drafts (rows with `status = 'used'` are history).
- Do NOT try to insert from the website or the Android app. The table has no insert policy on purpose; drafts are written only with the Supabase connector (service role).
- Do NOT put secrets, student data or personal phone numbers/emails in a draft.

---

## WHERE (the map)

| What | Where |
|---|---|
| Database | Supabase project **"Wisdom tower academy"**, project id `amieczmpqrvavzjikbdi` (do NOT use the other project, "Ft agritech") |
| Table | `public.notification_drafts` |
| Design doc | `docs/PLAN_NOTIFICATION_DRAFTS.md` |
| Drafts list UI (what the owner sees) | `website-reference/src/components/admin/NotificationDraftsInbox.tsx` |
| Composer that loads a draft | `website-reference/src/components/admin/NotificationComposerV2.tsx` |
| Content validator and block shapes (source of truth) | `website-reference/src/lib/notification-v2.ts` (`sanitizeContent`, `previewFromContent`) |
| Send logic (read only, never change for this task) | `website-reference/src/app/api/notifications/send/route.ts`, `src/lib/notification-audience.ts`, `src/lib/notifications.ts` |
| Admin screen in the app | Profile > Push Broadcasts > Compose |

Needs the **Supabase connector** (execute_sql). The repo itself needs no change to draft a notification.

---

## TABLE COLUMNS

| Column | Value |
|---|---|
| `id` | auto (uuid) |
| `title` | short list label shown in the inbox, e.g. `Freshman: invite your friends, competition soon` |
| `category` | `admin` (default), `material` (new study material, set `url`), or `contest` (competition news) |
| `content` | jsonb, shape below |
| `url` | optional in-app path, e.g. a material path. `null` if none |
| `note` | one line for the owner about what this draft is |
| `status` | `draft` when you insert it. The composer flips it to `used` after a send |
| `created_at` | auto |

---

## CONTENT SHAPE

```json
{
  "v": 1,
  "blocks": [
    { "t": "title",    "text": "Heading shown at the top" },
    { "t": "subtitle", "text": "Optional smaller heading" },
    { "t": "p",        "runs": [ { "text": "Plain text " }, { "b": true, "text": "bold" }, { "hl": true, "text": "highlighted" } ] },
    { "t": "list",     "items": [ [ { "b": true, "text": "Item one" } ], [ { "text": "Item two" } ] ] },
    { "t": "divider" },
    { "t": "callout",  "runs": [ { "b": true, "text": "Key message." }, { "text": " Supporting sentence." } ] }
  ]
}
```

- Allowed block types: `title`, `subtitle`, `p`, `callout`, `list`, `divider`. Nothing else.
- Text goes in `runs` (`p`, `callout`) or `items` (`list`, each item is an array of runs). `title` and `subtitle` use plain `text`.
- This is the same shape the Android app parses. If unsure about limits, read `notification-v2.ts` before inserting. An invalid shape will fail when the owner taps Load.

---

## HOW (steps)

1. Get the message from the owner (who it is for, what to say, any prize/date/link).
2. Look at the last 3 drafts to copy the voice and format:
   ```sql
   select title, category, content, note, status, created_at
   from public.notification_drafts order by created_at desc limit 3;
   ```
3. Write the draft. Voice: calm, clear, short, no hype, no spam-style emoji. The owner sometimes wants English, sometimes Amharic or hybrid Amharic/English; follow what they ask. If a brand/tone file exists in the repo (a `TONE.md` or brand kit), follow it.
4. Insert it (replace the values):
   ```sql
   insert into public.notification_drafts (title, category, content, url, note, status)
   values (
     'Short inbox title',
     'admin',
     '{"v":1,"blocks":[{"t":"title","text":"..."},{"t":"p","runs":[{"text":"..."}]}]}'::jsonb,
     null,
     'one line about this draft',
     'draft'
   )
   returning id, title, status;
   ```
   Use project id `amieczmpqrvavzjikbdi`.
5. Confirm the returned `id` and `status = 'draft'`.
6. Tell the owner (plain words): open **Push Broadcasts**, tap **Refresh** on "Claude drafts", tap **Load into composer**, choose audience and time, press Send.

---

## OWNER'S SIDE (what happens next)

Push Broadcasts > Compose > "Claude drafts (n)" > **Refresh** > **Load into composer** fills title, category, blocks, preview and path. The owner picks Audience and When, then presses Send. After a successful send or schedule the draft becomes `used`.

---

## IF SOMETHING FAILS

- Draft does not show: owner taps **Refresh**. Check the row exists and `status = 'draft'`.
- "Load" fails or the form is empty: the `content` shape is invalid. Compare with `sanitizeContent` in `notification-v2.ts` and fix the row with `update ... where id = '<id>'`.
- Insert fails: check you are on project `amieczmpqrvavzjikbdi` and that the table exists (`docs/PLAN_NOTIFICATION_DRAFTS.md` lists the live Supabase change).
- If you change anything about how drafts work, update `docs/PLAN_NOTIFICATION_DRAFTS.md` and this file in the same commit.
