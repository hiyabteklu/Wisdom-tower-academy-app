-- Prioritize every unreviewed blocked-device event in the admin queue,
-- followed by the latest 200 other audit events.
-- Applied as migration prioritize_pending_device_review_events.

create or replace function public.admin_list_account_device_events()
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
  with pending as (
    select e.id, e.user_id, u.email,
           coalesce(p.full_name, nullif(trim(coalesce(p.first_name, '') || ' ' || coalesce(p.last_name, '')), ''), '') as full_name,
           e.device_label, e.event_type, e.created_at, e.reviewed_at, e.review_note,
           true as needs_review
    from public.account_device_events e
    left join auth.users u on u.id = e.user_id
    left join public.profiles p on p.id = e.user_id
    where e.event_type = 'blocked_new_device'
      and e.reviewed_at is null
  ),
  recent_history as (
    select e.id, e.user_id, u.email,
           coalesce(p.full_name, nullif(trim(coalesce(p.first_name, '') || ' ' || coalesce(p.last_name, '')), ''), '') as full_name,
           e.device_label, e.event_type, e.created_at, e.reviewed_at, e.review_note,
           false as needs_review
    from public.account_device_events e
    left join auth.users u on u.id = e.user_id
    left join public.profiles p on p.id = e.user_id
    where not (e.event_type = 'blocked_new_device' and e.reviewed_at is null)
    order by e.created_at desc
    limit 200
  ),
  selected as (
    select * from pending
    union all
    select * from recent_history
  )
  select s.id, s.user_id, s.email, s.full_name, s.device_label,
         s.event_type, s.created_at, s.reviewed_at, s.review_note
  from selected s
  order by s.needs_review desc, s.created_at desc;
end;
$function$;

revoke all on function public.admin_list_account_device_events() from public, anon;
grant execute on function public.admin_list_account_device_events() to authenticated, service_role;
