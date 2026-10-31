-- Exam integrity admin dashboard (live migrations: admin_exam_integrity_dashboard,
-- admin_exam_integrity_review_policy, fix_unmatched_exam_exit_duration_v2).
-- This is the consolidated final SQL for a fresh environment.

alter table public.competition_entries
  add column if not exists integrity_reviewed_at timestamptz,
  add column if not exists integrity_reviewed_by uuid references auth.users(id) on delete set null,
  add column if not exists integrity_review_note text;

drop policy if exists compe_admin_integrity_review_update on public.competition_entries;
create policy compe_admin_integrity_review_update on public.competition_entries
  for update to authenticated
  using ((select private.is_admin()))
  with check ((select private.is_admin()));

create or replace function public.admin_exam_integrity_report()
returns jsonb
language plpgsql
security invoker
set search_path = public, private
as $$
declare v_report jsonb;
begin
  if auth.uid() is null or not private.is_admin() then
    raise exception 'admin_only';
  end if;

  select coalesce(jsonb_agg(row_data order by flagged_at desc), '[]'::jsonb)
    into v_report
  from (
    select jsonb_build_object(
      'entry_id', e.id,
      'user_id', e.user_id,
      'student_name', coalesce(p.full_name, nullif(concat_ws(' ', p.first_name, p.last_name), ''), 'Student'),
      'email', p.email,
      'student_id', p.student_id_number,
      'competition_title', c.title,
      'mode', e.mode,
      'attempt_status', e.status,
      'started_at', e.started_at,
      'submitted_at', e.submitted_at,
      'score', e.score,
      'total_count', e.total_count,
      'flag_count', f.flag_count,
      'leave_count', f.leave_count,
      'total_away_ms', f.total_away_ms,
      'longest_away_ms', f.longest_away_ms,
      'last_flag_at', f.flagged_at,
      'reviewed_at', e.integrity_reviewed_at,
      'review_note', e.integrity_review_note,
      'events', f.events
    ) as row_data, f.flagged_at
    from public.competition_entries e
    join public.competitions c on c.id = e.competition_id
    left join public.profiles p on p.id = e.user_id
    join lateral (
      select
        count(*)::int as flag_count,
        count(*) filter (where x.kind = 'app_background')::int as leave_count,
        coalesce(sum(x.away_ms) filter (where x.kind = 'app_background'), 0)::bigint as total_away_ms,
        coalesce(max(x.away_ms) filter (where x.kind = 'app_background'), 0)::bigint as longest_away_ms,
        max(x.created_at) as flagged_at,
        jsonb_agg(jsonb_build_object(
          'id', x.id, 'kind', x.kind, 'detail', x.detail,
          'created_at', x.created_at, 'away_ms', x.away_ms
        ) order by x.created_at asc) as events
      from (
        select cf.id, cf.kind, cf.detail, cf.created_at,
          case when cf.kind = 'app_background' then (
            select case when min(cf2.created_at) is null then null
              else greatest(0, floor(extract(epoch from (min(cf2.created_at) - cf.created_at)) * 1000))::bigint
            end
            from public.competition_flags cf2
            where cf2.entry_id = cf.entry_id
              and cf2.kind = 'app_foreground'
              and cf2.created_at > cf.created_at
          ) else null end as away_ms
        from public.competition_flags cf
        where cf.entry_id = e.id
      ) x
    ) f on true
    where e.mode = 'ranked' and f.flag_count > 0
  ) report_rows;

  return v_report;
end;
$$;

create or replace function public.admin_review_exam_integrity(
  p_entry_id uuid,
  p_review_note text default null
)
returns boolean
language plpgsql
security invoker
set search_path = public, private
as $$
begin
  if auth.uid() is null or not private.is_admin() then
    raise exception 'admin_only';
  end if;

  update public.competition_entries
  set integrity_reviewed_at = now(),
      integrity_reviewed_by = auth.uid(),
      integrity_review_note = nullif(left(coalesce(p_review_note, ''), 1000), '')
  where id = p_entry_id and mode = 'ranked';

  return found;
end;
$$;

revoke all on function public.admin_exam_integrity_report() from public, anon;
revoke all on function public.admin_review_exam_integrity(uuid, text) from public, anon;
grant execute on function public.admin_exam_integrity_report() to authenticated;
grant execute on function public.admin_review_exam_integrity(uuid, text) to authenticated;
