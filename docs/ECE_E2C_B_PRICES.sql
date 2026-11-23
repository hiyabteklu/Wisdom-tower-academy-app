-- ECE Year 1 and 2 price rows (slice E2c-b). NOT RUN. Owner says go first.
-- Why rows: the app has no built-in price. PackagePrices.kt reads public.catalog_items (id, price_etb, active=true).
-- Owner price: 300 ETB for every ECE package, Years 1 to 5. The database value wins; change price_etb any time.
-- Rows are inserted with active = false, so the website and the app show nothing yet.
-- When Year 1 / Year 2 are built and phone-tested, flip them live with the last statement.
-- Year 3 rows (ece-y3-sem-1, ece-y3-sem-2) already exist at 300. Year 4 and 5 rows come with slices E4 to E7.
-- Columns checked 2026-11-23: includes is text[]; price_etb is numeric.

-- 1) TEST FIRST (nothing is saved): run this block alone.
begin;
insert into public.catalog_items (id, name, short_name, description, price_etb, href, image, includes, enrolled_label, group_key, active, sort_order) values
 ('ece-y1-sem-1','ECE Year 1 — Semester 1','ECE Y1 S1','Electrical & Computer Engineering 1st year — first semester only.',300,'/academy/special-packages/electrical-computer-engineering/y1-sem-1','/images/special-packages/ece-sem-1.jpg',array['Semester 1','Learning hubs per course','300 ETB one-time'],'Special track','special',false,130),
 ('ece-y1-sem-2','ECE Year 1 — Semester 2','ECE Y1 S2','Electrical & Computer Engineering 1st year — second semester only.',300,'/academy/special-packages/electrical-computer-engineering/y1-sem-2','/images/special-packages/ece-sem-2.jpg',array['Semester 2','Learning hubs per course','300 ETB one-time'],'Special track','special',false,140),
 ('ece-y2-sem-1','ECE Year 2 — Semester 1','ECE Y2 S1','Electrical & Computer Engineering 2nd year — first semester only.',300,'/academy/special-packages/electrical-computer-engineering/y2-sem-1','/images/special-packages/ece-sem-1.jpg',array['Semester 1','Learning hubs per course','300 ETB one-time'],'Special track','special',false,150),
 ('ece-y2-sem-2','ECE Year 2 — Semester 2','ECE Y2 S2','Electrical & Computer Engineering 2nd year — second semester only.',300,'/academy/special-packages/electrical-computer-engineering/y2-sem-2','/images/special-packages/ece-sem-2.jpg',array['Semester 2','Learning hubs per course','300 ETB one-time'],'Special track','special',false,160)
on conflict (id) do nothing;
select id, price_etb, active from public.catalog_items where id like 'ece-y%' order by id;
rollback;

-- 2) APPLY: same insert as above, but end with commit instead of rollback.

-- 3) GO LIVE (only after the phone test of Years 1 and 2):
-- update public.catalog_items set active = true, updated_at = now() where id in ('ece-y1-sem-1','ece-y1-sem-2','ece-y2-sem-1','ece-y2-sem-2');
