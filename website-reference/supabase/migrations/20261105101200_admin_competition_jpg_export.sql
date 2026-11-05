create or replace function public.admin_competition_export(p_competition_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  c public.competitions%rowtype;
  v_exam_title text;
  v_subject_scope text;
  v_top jsonb;
  v_count integer;
begin
  if auth.uid() is null or not (select private.is_admin()) then
    raise exception 'admin_only';
  end if;

  select * into c
  from public.competitions
  where id = p_competition_id;

  if not found then
    raise exception 'competition_not_found';
  end if;

  if private.competition_phase(c) <> 'closed' then
    return jsonb_build_object('status', 'not_closed');
  end if;

  select r.title, r.scope_path
    into v_exam_title, v_subject_scope
  from public.learning_resources r
  where r.id = c.source_resource_id;

  with ranked as (
    select
      row_number() over (
        order by e.score desc, e.time_taken_ms asc nulls last,
                 e.submitted_at asc nulls last, e.id
      )::integer as rank,
      coalesce(nullif(trim(p.full_name), ''), 'Student') as name,
      e.score,
      e.total_count as total,
      e.time_taken_ms,
      e.submitted_at
    from public.competition_entries e
    left join public.profiles p on p.id = e.user_id
    where e.competition_id = c.id
      and e.mode = 'ranked'
      and e.status <> 'in_progress'
      and not e.disqualified
      and e.score is not null
      and e.total_count is not null
  )
  select
    count(*)::integer,
    coalesce(
      jsonb_agg(
        jsonb_build_object(
          'rank', rank,
          'name', name,
          'score', score,
          'total', total,
          'time_taken_ms', time_taken_ms,
          'submitted_at', submitted_at
        ) order by rank
      ) filter (where rank <= 3),
      '[]'::jsonb
    )
  into v_count, v_top
  from ranked;

  if v_count < 3 then
    return jsonb_build_object(
      'status', 'insufficient_results',
      'available_results', v_count
    );
  end if;

  return jsonb_build_object(
    'status', 'ok',
    'competition_title', c.title,
    'exam_title', coalesce(v_exam_title, c.title),
    'subject_scope', v_subject_scope,
    'event_date', c.starts_at,
    'top', v_top
  );
end;
$function$;

revoke all on function public.admin_competition_export(uuid) from public, anon;
grant execute on function public.admin_competition_export(uuid) to authenticated, service_role;
