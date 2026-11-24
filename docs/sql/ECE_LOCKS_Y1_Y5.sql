-- ECE Years 1, 2, 4, 5: package locks (NOT RUN. Owner approval needed: live users).
-- Why: content_guard.effective_mode returns 'open' when no lock row matches. Years 1, 2, 4, 5 have no
-- `package:<id>` row today (checked read-only 2026-11-24), so any content added under them would be FREE.
-- Year 3 has `package:ece-y3-sem-1` / `package:ece-y3-sem-2` = require_purchase; this gives the other 8 the same.
-- Safe to run twice (skips a key that already exists). Does not touch any existing row, price or catalog flag.
-- The same can be done by hand in the admin app: Locks > each ECE package > Require purchase.
-- Test first: wrap in begin; ... rollback; and check the count, then run for real.

insert into public.content_locks (lock_key, mode, label, updated_by)
select v.lock_key, 'require_purchase', v.label, 'ece-y1-y5-setup'
from (values
  ('package:ece-y1-sem-1', 'ECE Year 1: Semester 1'),
  ('package:ece-y1-sem-2', 'ECE Year 1: Semester 2'),
  ('package:ece-y2-sem-1', 'ECE Year 2: Semester 1'),
  ('package:ece-y2-sem-2', 'ECE Year 2: Semester 2'),
  ('package:ece-y4-sem-1', 'ECE Year 4: Semester 1'),
  ('package:ece-y4-sem-2', 'ECE Year 4: Semester 2'),
  ('package:ece-y5-sem-1', 'ECE Year 5: Semester 1'),
  ('package:ece-y5-sem-2', 'ECE Year 5: Semester 2')
) as v(lock_key, label)
where not exists (select 1 from public.content_locks c where c.lock_key = v.lock_key);

-- Check (expect 8 rows, all require_purchase):
-- select lock_key, mode from public.content_locks where lock_key like 'package:ece-y%' order by lock_key;
-- Undo:
-- delete from public.content_locks where updated_by = 'ece-y1-y5-setup';
