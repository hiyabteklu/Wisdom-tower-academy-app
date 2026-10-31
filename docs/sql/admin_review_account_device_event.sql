-- Admin-only review marker for a blocked new-device sign-in event.
-- Applied to the live Wisdom Tower Academy Supabase project on 2026-10-31.
-- The caller must be authenticated and pass private.is_admin(); no service-role key is used by the Android client.

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
  if auth.uid() is null or not coalesce(private.is_admin(), false) then
    raise exception 'admin_required' using errcode = '42501';
  end if;

  update public.account_device_events
  set reviewed_at = now(),
      review_note = nullif(left(coalesce(p_review_note, ''), 1000), '')
  where id = p_event_id
    and event_type = 'blocked_new_device';

  return found;
end;
$function$;

revoke all on function public.admin_review_account_device_event(bigint, text) from public, anon;
grant execute on function public.admin_review_account_device_event(bigint, text) to authenticated;
