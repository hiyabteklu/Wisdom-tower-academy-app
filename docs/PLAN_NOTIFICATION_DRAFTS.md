# Plan: notification drafts inbox (slice DRAFTS-1)

Owner problem: building a notification by hand, block by block, is slow. Claude writes the finished draft and it lands in the composer, preview done.

## Design
- Table `public.notification_drafts` (Wisdom tower academy Supabase project): id, title, category (admin/material/contest), content (jsonb, same `{v:1, blocks}` shape the composer saves), url (material path), note, status (draft/used), created_at.
- RLS on. Admins only (`private.is_admin()`) can select, update, delete. No insert policy: drafts are written by Claude with the Supabase connector (service role), never from the browser.
- Composer (`NotificationComposerV2.tsx`) shows a Drafts list (`NotificationDraftsInbox.tsx`). "Load into composer" runs `sanitizeContent` then `contentToEditor`, so title, category, blocks, preview and path are filled. The admin still picks audience and time and presses Send.
- After a successful send or schedule the draft is marked `used`. Send and schedule logic is unchanged.

## Live Supabase change (record)
- 2026-11-09: migration `notification_drafts_inbox` applied to project amieczmpqrvavzjikbdi (table, 3 RLS policies, 1 index).

## Draft content format (for agents)
`content` = `{"v":1,"blocks":[ {"t":"title","text":"..."}, {"t":"subtitle","text":"..."}, {"t":"p","runs":[{"text":"...","b":true,"hl":true}]}, {"t":"callout","runs":[...]}, {"t":"list","items":[[runs],[runs]]}, {"t":"divider"} ]}`. `b` and `hl` are optional. `url` only for category material, a path from notification-paths.

## Not done
- Not compiled or opened in a browser. Needs a web deploy, then a phone check.
