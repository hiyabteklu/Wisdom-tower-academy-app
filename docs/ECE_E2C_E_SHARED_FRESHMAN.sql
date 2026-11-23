-- E2c-e: ECE Year 1/2 owners open the SHARED freshman rows (ONE copy of content, no duplicates), server side.
-- Applied to the live project "Wisdom tower academy" on 2026-11-23 (owner chose the alias plan: one content, separate ownership)
-- as migration `ece_y1_y2_owners_open_shared_freshman_rows`.
--
-- Only content_guard.resource_visible changed (on top of E2c-d). New variable v_shared: when the ROW package is 'freshman'
-- and its scope is one of the freshman subjects reused by ECE, an owner of ANY 'ece-y1-sem-*' (Year 1 subjects) or
-- ANY 'ece-y2-sem-*' (inclusiveness, global-trends) opens it, in enrollments and access_grants. Years never mix.
-- NOT shared (stay freshman-only): biology, chemistry, economics, math-social.
-- Not changed: effective_mode, open_resource_v2, my_package_access, RLS, prices, locks.
--
-- TEST (rolled back, 6 sample users x 6 sample rows, before and after): Year 1 owner gained freshman/english-1 only;
-- Year 2 owner gained inclusiveness and global-trends only; biology and economics stayed locked for ECE owners;
-- Year 3 owner unchanged (still opens Year 3, still no freshman); freshman-only owner unchanged (opens freshman,
-- NOT Year 3 rows); user with no purchase still locked everywhere. A first test run was invalid (it picked free sample rows
-- with an item-level open lock): sample rows must have effective_mode = 'require_purchase'.
--
-- TO ADD A SHARED SUBJECT LATER: add its scope to the matching list below and run the whole CREATE OR REPLACE again.
-- UNDO (back to E2c-d): run docs/ECE_E2C_D_SERVER_RULE.sql again.

CREATE OR REPLACE FUNCTION content_guard.resource_visible(p_package text, p_scope text, p_hub text, p_chapter integer, p_id uuid)
 RETURNS boolean
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public'
AS $function$
declare
  v_uid  uuid := auth.uid();
  v_mail text := lower(coalesce(auth.jwt() ->> 'email', ''));
  v_pkg  text := coalesce(p_package, '');
  v_mode text := content_guard.effective_mode(p_package, p_scope, p_hub, p_chapter, p_id);
  v_ece  text := substring(v_pkg from '^ece-y[123]-sem-');
  v_shared text := case
    when v_pkg = 'freshman' and coalesce(p_scope, '') in (
      'freshman/english-1','freshman/logic','freshman/math-natural','freshman/mathematics','freshman/physics',
      'freshman/geography','freshman/psychology','freshman/physical-fitness','freshman/english-2',
      'freshman/anthropology','freshman/applied-math-1','freshman/entrepreneurship',
      'freshman/emerging-technology','freshman/civics','freshman/cpp-programming','freshman/history')
      then 'ece-y1-sem-'
    when v_pkg = 'freshman' and coalesce(p_scope, '') in ('freshman/inclusiveness','freshman/global-trends')
      then 'ece-y2-sem-'
    else null end;
begin
  if v_mode = 'open' then return true; end if;
  if v_mode = 'require_purchase' then
    if v_uid is null then return false; end if;
    if not exists (select 1 from public.content_locks where lock_key = 'system:enforce' and mode = 'locked') then
      return true;
    end if;
    return exists (
      select 1 from public.enrollments e
      where (e.user_id = v_uid or (e.email is not null and lower(e.email) = v_mail))
        and (e.package_id in (v_pkg, '__all__') or (v_ece is not null and starts_with(e.package_id, v_ece))
             or (v_shared is not null and starts_with(e.package_id, v_shared)))
    ) or exists (
      select 1 from public.access_grants g
      where (g.user_id = v_uid or (g.email is not null and lower(g.email) = v_mail))
        and (g.package_id in (v_pkg, '__all__') or (v_ece is not null and starts_with(g.package_id, v_ece))
             or (v_shared is not null and starts_with(g.package_id, v_shared)))
    );
  end if;
  return false;
end;
$function$;
