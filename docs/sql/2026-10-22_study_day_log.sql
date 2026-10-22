-- S6a (2026-10-22): per-day study log for the native My Status weekday chart and daily-goal ring.
-- ALREADY APPLIED to the live project (ref amieczmpqrvavzjikbdi) as migration `study_day_log_and_add_study_seconds`.
-- Kept here as the record; do not run it twice (it is idempotent for the table, but the policies would error).
-- Own rows only (auth.uid() = user_id). The website does not use this table.

create table if not exists public.study_day_log (
  user_id uuid not null references auth.users(id) on delete cascade,
  day date not null,
  seconds integer not null default 0 check (seconds >= 0),
  focus_seconds integer not null default 0 check (focus_seconds >= 0),
  updated_at timestamptz not null default now(),
  primary key (user_id, day)
);

comment on table public.study_day_log is 'Seconds studied per day per student (own rows only). Feeds the native My Status weekday chart and daily-goal ring.';

alter table public.study_day_log enable row level security;

create policy study_day_log_select_own on public.study_day_log
  for select to authenticated using (auth.uid() = user_id);
create policy study_day_log_insert_own on public.study_day_log
  for insert to authenticated with check (auth.uid() = user_id);
create policy study_day_log_update_own on public.study_day_log
  for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- Atomic "add seconds to this day" (one call from the app after a progress save). Clamped to 0..86400 per call and per day.
create or replace function public.add_study_seconds(p_day date, p_seconds integer, p_focus integer default 0)
returns void
language sql
security invoker
set search_path = public
as $$
  insert into public.study_day_log as l (user_id, day, seconds, focus_seconds)
  values (
    auth.uid(),
    p_day,
    least(greatest(coalesce(p_seconds, 0), 0), 86400),
    least(greatest(coalesce(p_focus, 0), 0), 86400)
  )
  on conflict (user_id, day) do update
    set seconds = least(l.seconds + excluded.seconds, 86400),
        focus_seconds = least(l.focus_seconds + excluded.focus_seconds, 86400),
        updated_at = now();
$$;

revoke all on function public.add_study_seconds(date, integer, integer) from public, anon;
grant execute on function public.add_study_seconds(date, integer, integer) to authenticated;
