-- Applied live as migration fix_admin_security_rpc_types_and_review_privilege (2026-10-31).
-- 1) auth.users.email is varchar(255) but RETURNS TABLE declares text -> Postgres 42804
--    "structure of query does not match function result type". Fixed by casting to text.
-- 2) admin_review_exam_integrity UPDATEs competition_entries, but role authenticated only has SELECT
--    on that table -> 42501 "permission denied for table competition_entries". Fixed by making the
--    function SECURITY DEFINER (admin check kept inside) instead of granting UPDATE to all students.

create or replace function public.admin_list_account_devices()
 returns table(user_id uuid, email text, full_name text, device_label text, registered_at timestamptz, last_seen_at timestamptz, session_id uuid)
 language plpgsql stable security definer
 set search_path to ''
as $function$
begin
  if not coalesce(private.is_admin(), false) then
    raise exception 'admin_required' using errcode = '42501';
  end if;
  return query
  select d.user_id, u.email::text, coalesce(p.full_name, p.first_name, '')::text, d.device_label::text,
         d.registered_at, d.last_seen_at, d.session_id
  from public.account_devices d
  join auth.users u on u.id = d.user_id
  left join public.profiles p on p.id = d.user_id
  order by d.last_seen_at desc;
end;
$function$;

-- admin_list_account_device_events: same shape as before, with ::text casts on email, full_name,
-- device_label, event_type, review_note in both the `pending` and `recent_history` CTEs.
-- (Full body is in the live database: select pg_get_functiondef('public.admin_list_account_device_events'::regproc);)

create or replace function public.admin_review_exam_integrity(p_entry_id uuid, p_review_note text default null)
 returns boolean
 language plpgsql security definer
 set search_path to ''
as $function$
begin
  if auth.uid() is null or not coalesce(private.is_admin(), false) then
    raise exception 'admin_only';
  end if;
  update public.competition_entries
  set integrity_reviewed_at = now(),
      integrity_reviewed_by = auth.uid(),
      integrity_review_note = nullif(left(coalesce(p_review_note, ''), 1000), '')
  where id = p_entry_id and mode = 'ranked';
  return found;
end;
$function$;

revoke all on function public.admin_review_exam_integrity(uuid, text) from public, anon;
grant execute on function public.admin_review_exam_integrity(uuid, text) to authenticated;
