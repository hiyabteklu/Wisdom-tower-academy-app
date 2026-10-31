-- Device security review queue.
-- Applied to the Wisdom Tower Academy Supabase project as migration
-- device_event_review_queue.

alter table public.account_device_events
  add column if not exists reviewed_at timestamptz,
  add column if not exists reviewed_by uuid references auth.users(id) on delete set null,
  add column if not exists review_note text;

create index if not exists account_device_events_pending_review_idx
  on public.account_device_events (created_at desc)
  where event_type = 'blocked_new_device' and reviewed_at is null;

drop function if exists public.admin_list_account_device_events();

create function public.admin_list_account_device_events()
returns table(
  event_id bigint,
  user_id uuid,
  email text,
  full_name text,
  device_label text,
  event_type text,
  created_at timestamptz,
  reviewed_at timestamptz,
  review_note text
)
language plpgsql
stable
security definer
set search_path = ''
as $function$
begin
  if not coalesce(private.is_admin(), false) then
    raise exception 'admin_required' using errcode = '42501';
  end if;

  return query
  select e.id, e.user_id, u.email,
         coalesce(p.full_name, nullif(trim(coalesce(p.first_name, '') || ' ' || coalesce(p.last_name, '')), ''), ''),
         e.device_label, e.event_type, e.created_at, e.reviewed_at, e.review_note
  from public.account_device_events e
  left join auth.users u on u.id = e.user_id
  left join public.profiles p on p.id = e.user_id
  order by e.created_at desc
  limit 200;
end;
$function$;

revoke all on function public.admin_list_account_device_events() from public, anon;
grant execute on function public.admin_list_account_device_events() to authenticated, service_role;

create or replace function public.admin_count_pending_device_alerts()
returns integer
language plpgsql
stable
security definer
set search_path = ''
as $function$
declare
  v_count integer;
begin
  if not coalesce(private.is_admin(), false) then
    raise exception 'admin_required' using errcode = '42501';
  end if;

  select count(*)::integer
    into v_count
  from public.account_device_events
  where event_type = 'blocked_new_device'
    and reviewed_at is null;

  return coalesce(v_count, 0);
end;
$function$;

revoke all on function public.admin_count_pending_device_alerts() from public, anon;
grant execute on function public.admin_count_pending_device_alerts() to authenticated, service_role;

create or replace function public.admin_review_account_device_event(
  p_event_id bigint,
  p_review_note text default null
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $function$
begin
  if not coalesce(private.is_admin(), false) then
    raise exception 'admin_required' using errcode = '42501';
  end if;

  update public.account_device_events
  set reviewed_at = now(),
      reviewed_by = auth.uid(),
      review_note = nullif(left(coalesce(p_review_note, ''), 1000), '')
  where id = p_event_id
    and event_type = 'blocked_new_device';

  return found;
end;
$function$;

revoke all on function public.admin_review_account_device_event(bigint, text) from public, anon;
grant execute on function public.admin_review_account_device_event(bigint, text) to authenticated, service_role;
