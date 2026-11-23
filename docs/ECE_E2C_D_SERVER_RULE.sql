-- E2c-d: one purchase opens BOTH semesters of an ECE year (Years 1 to 5), server side.
-- Applied to the live project "Wisdom tower academy" on 2026-11-23 (owner approved) as migration
-- `ece_one_purchase_both_semesters_resource_visible`.
--
-- Only content_guard.resource_visible changed: the old Year 3 rule
--   (v_pkg like 'ece-y3-sem-%' and package_id like 'ece-y3-sem-%')
-- became a same-year rule: prefix 'ece-y1-sem-', 'ece-y2-sem-' or 'ece-y3-sem-' of the package being opened must be
-- the prefix of an owned package. Years never mix. Year 3 results are identical (checked with a truth table of 15
-- package pairs before applying: only same-year Year 1/2 semester pairs changed from false to true).
-- Not changed: content_guard.effective_mode (lock rows stay per course scope), my_package_access, RLS, prices.
--
-- UNDO (puts back the exact Year 3 only rule): run the same CREATE OR REPLACE with the two conditions
--   (e.package_id in (v_pkg, '__all__') or (v_pkg like 'ece-y3-sem-%' and e.package_id like 'ece-y3-sem-%'))
--   (g.package_id in (v_pkg, '__all__') or (v_pkg like 'ece-y3-sem-%' and g.package_id like 'ece-y3-sem-%'))
-- and without the v_ece variable.

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
  -- ECE years 1 to 3: one purchase opens both semesters of the SAME year (prefix such as 'ece-y1-sem-'); years never mix
  v_ece  text := substring(v_pkg from '^ece-y[1-5]-sem-');
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
        and (e.package_id in (v_pkg, '__all__') or (v_ece is not null and starts_with(e.package_id, v_ece)))
    ) or exists (
      select 1 from public.access_grants g
      where (g.user_id = v_uid or (g.email is not null and lower(g.email) = v_mail))
        and (g.package_id in (v_pkg, '__all__') or (v_ece is not null and starts_with(g.package_id, v_ece)))
    );
  end if;
  return false;
end;
$function$;
